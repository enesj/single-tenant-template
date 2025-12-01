(ns app.shared.field-specs
  (:require
    #?(:clj [taoensso.timbre :as log]
       :cljs [taoensso.timbre :as log])
    [app.shared.field-types :as field-types]
    [app.shared.model-naming :as model-naming]
    [app.shared.validation.metadata :as validation-meta]
    [clojure.string :as str]))

(def excluded-fields #{:id :created_at :updated_at :tenant_id})

;; Fields to exclude from forms but keep in tables
;; Fields to exclude from forms but keep in tables
(def form-excluded-fields #{:id :created_at :updated_at :tenant_id :owner_id})

;; Load models data
;; Runtime models metadata ------------------------------------------------

(defn- compute-types-map
  "Build a lookup of `type-key → props` for every enum type declared in the
  models metadata. Works both for EDN-loaded maps (keyword keys) and the
  JSON-encoded `[k v]` vector form where keys and type names are strings. It
  ensures the resulting map is always keyed by **keywords**, so downstream code
  can reliably query e.g. `:property-type` regardless of data source."
  [md]
  (let [md-map (cond
                 (map? md) md
                 (vector? md) (into {} md)
                 :else {})]
    (->> md-map
      vals
      (mapcat (fn [table-def]
                ;; Each :types entry is [type-name base-type props]
                ;; where `type-name` may be string when coming from JSON.
                (map (fn [[type-name _ props]]
                       [(keyword type-name) props])
                  (:types table-def))))
      (into {}))))

(defn- field-name->label
  "Convert field name to human readable label"
  [field-name]
  (-> (name field-name)
    (str/split #"_id" 0)
    first
    (str/replace #"_" " ")
    str/capitalize))

(defn- find-first-unique-field
  "Choose a sensible display column for a referenced entity so that foreign-key
  selects show human-readable labels.

  Preference order:
  1. `:name` column if present.
  2. `:full_name` column if present (for users).
  3. First column with a `:unique` constraint.
  4. First non-FK, non-excluded, VARCHAR/TEXT column.
  5. Fallback `:id`."
  [entity-name models-data]
  ;; models-data can be a map (EDN) or a vector of `[k v]` pairs (JSON). Normalize.
  (let [md (cond
             (map? models-data) models-data
             (vector? models-data) (into {} (map (fn [[k v]] [(keyword k) v]) models-data))
             :else {})
        ;; Handle both string and keyword keys in models-data
        entity-key (if (contains? md entity-name)
                     entity-name
                     (name entity-name))
        fields (get-in md [entity-key :fields])
        ;; Helper to coerce any field name (str/kw) → keyword
        kw (fn [x] (if (keyword? x) x (keyword x)))
        fnames (map (comp kw first) fields)
        ;; Check for common human-readable field names in order of preference
        explicit (cond
                   (some #{:name} fnames) :name
                   (some #{:full_name} fnames) :full_name
                   (some #{:title} fnames) :title
                   (some #{:description} fnames) :description
                   :else nil)
        unique-col (some (fn [[fname _ constraints]]
                           (when (:unique constraints) (kw fname)))
                     fields)
        readable? (fn [[fname ftype constraints]]
                    (let [t (kw (if (vector? ftype) (first ftype) ftype))]
                      (and (#{:text :varchar} t)
                        (nil? (:foreign-key constraints))
                        (not (excluded-fields (kw fname))))))
        readable-col (some (fn [f]
                             (when (readable? f) (kw (first f))))
                       fields)
        choice (or explicit unique-col readable-col :id)]
    choice))

(defn- create-field-type
  "Create appropriate field type handler based on field definition.
  Accepts keyword OR string representations so that data coming from JSON
  (where keywords are converted to strings in values) works the same way as EDN-loaded data."
  [field-type constraints models-data types-map]
  (let [;; Determine base type, normalising any string to keyword
        raw-base-type (cond
                        (vector? field-type) (first field-type) ;; e.g. [:varchar n] OR ["enum" "property-type"]
                        (= field-type :serial) :integer
                        :else field-type)
        base-type (if (keyword? raw-base-type) raw-base-type (keyword raw-base-type))
        foreign-key (:foreign-key constraints)]
    (cond
      ;; 1. Foreign keys (integer + :foreign-key) become ForeignKeyField
      (and (#{:integer :uuid :bigint} base-type) foreign-key)
      (let [entity-name (let [fk (if (keyword? foreign-key) foreign-key (keyword foreign-key))] (keyword (namespace fk)))
            unique-field (find-first-unique-field entity-name models-data)]
        (field-types/->ForeignKeyField entity-name unique-field))

      ;; 2. Enumerated values [:enum <type>] become EnumField. <type> may arrive as string -> keyword
      (and (= base-type :enum) (vector? field-type))
      (let [enum-type-raw (second field-type)
            enum-type (if (keyword? enum-type-raw) enum-type-raw (keyword enum-type-raw))
            choices (get-in types-map [enum-type :choices])]
        (field-types/->EnumField choices))

      ;; 3. Fallback to BasicField
      :else
      (field-types/->BasicField base-type))))

(defn- process-field
  "Process a single field definition with validation and admin metadata support"
  ([field-def models-data types-map]
   (process-field field-def models-data types-map excluded-fields))
  ([field-def models-data types-map exclusion-set]
   (let [[field-name field-type constraints] field-def
         ;; Try both keyword and string/symbol versions
         field-name-kw (keyword field-name)
         excluded? (or (exclusion-set field-name) (exclusion-set field-name-kw))]
     (when-not excluded?
       (let [field-type-handler (create-field-type field-type constraints models-data types-map)
             type-info (field-types/get-input-type field-type-handler)
             options (field-types/get-options field-type-handler types-map)
             default-value (field-types/get-default-value field-type-handler)
             field-label (field-name->label field-name)

             ;; Extract admin metadata from constraints
             admin-meta (:admin constraints)

             ;; DEBUG: Log admin metadata extraction
             ;;_ (when admin-meta
             ;;    (log/info "DEBUG field-specs: Field" field-name "has admin metadata:" admin-meta))

             ;; Generate validation specification from metadata
             validation-spec (validation-meta/generate-field-validation-spec
                               field-name field-type constraints field-label)

             ;; Create base field spec
             base-spec (cond-> {:id (name (model-naming/db-keyword->app (keyword field-name)))
                                :label field-label
                                :default-value default-value}
                         type-info (merge type-info)
                         (false? (get-in constraints [:null])) (assoc :required true)
                         (:unique constraints) (assoc :validate-server? :unique :unique true)
                         options (assoc :options options)
                         ;; NEW: Include admin metadata if present
                         admin-meta (assoc :admin admin-meta))

             ;; Merge validation metadata into field spec
             final-spec (validation-meta/merge-field-validation base-spec validation-spec)]

             ;; DEBUG: Log final field spec with admin metadata
             ;;_ (log/info "DEBUG field-specs: Final spec for" field-name ":"
             ;;    {:id (:id final-spec)
             ;;     :label (:label final-spec)
             ;;     :admin (:admin final-spec)})]

         final-spec)))))

(defn- process-computed-field
  "Process a computed field definition from models metadata"
  [field-name field-def]
  (let [admin-meta (:admin field-def)
        field-type (:type field-def :string)
        label (:label field-def (field-name->label field-name))]
    ;; Debug log
    (log/info "Processing computed field:" field-name "with admin meta:" admin-meta)
    {:id (name field-name)
     :label label
     :type field-type
     :admin (merge {:visible-in-table? true
                    :filterable? true
                    :sortable? true}
              admin-meta)}))

(defn entity-specs
  "Return a map of `entity-key → vector-of-field-defs` built from the models metadata.
   Works whether `md` is a normal Clojure map or the `[k v]` vector form coming from
   EDN/JSON serialisation. Fields are sorted by :display-order from admin metadata."
  ([md]
   (entity-specs md excluded-fields))
  ([md exclusion-set]
   (let [md-map (cond
                  (map? md) md
                  (vector? md) (into {} md)
                  :else {})
         types-map (compute-types-map md-map)]
     (reduce-kv
       (fn [acc entity-key entity-def]
         (let [fields (:fields entity-def)
               computed-fields (:computed-fields entity-def)
               regular-field-definitions (->> fields
                                           (keep #(process-field % md-map types-map exclusion-set)))
               computed-field-definitions (->> computed-fields
                                            (map (fn [[field-name field-def]]
                                                   (process-computed-field field-name field-def))))
               all-field-definitions (concat regular-field-definitions computed-field-definitions)
               field-definitions (->> all-field-definitions
                                   ;; Debug: log final field definitions
                                   (doall (map (fn [field-spec]
                                                 (log/info "Final field spec:" (:id field-spec) "admin:" (:admin field-spec))
                                                 field-spec)))
                                   ;; Sort by display-order from admin metadata
                                   (sort-by (fn [field-spec]
                                              (get-in field-spec [:admin :display-order] 1000)))
                                   vec)]
           (assoc acc (keyword entity-key) field-definitions)))
       {}
       md-map))))

(defn form-entity-specs
  "Return entity specs for forms with form-specific field exclusions"
  [md]
  (entity-specs md form-excluded-fields))
