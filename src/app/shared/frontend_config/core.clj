(ns app.shared.frontend-config.core
  "Shared helpers for frontend config validation and syncing.

  This namespace is intentionally Clojure-only (used by bb tasks and tests)."
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.set :as set]
    [clojure.string :as str]
    [app.shared.specs.entities :as entities-spec]
    [app.shared.specs.form-fields :as form-fields-spec]
    [app.shared.specs.table-columns :as table-columns-spec]
    [app.shared.specs.view-options :as view-options-spec]))

;; =============================================================================
;; Config discovery
;; =============================================================================

(def ^:private standard-config-files
  {:entities "entities.edn"
   :view-options "view-options.edn"
   :form-fields "form-fields.edn"
   :table-columns "table-columns.edn"})

(defn- file-exists?
  [path]
  (.exists (io/file path)))

(defn- canonicalize-name
  [s]
  (some-> s (str/replace "_" "-")))

(defn normalize-id
  "Return a canonical string identifier for keywords/strings.

  Normalizes separators so '_' and '-' are treated as equivalent."
  [x]
  (cond
    (keyword? x) (canonicalize-name (name x))
    (string? x) (canonicalize-name x)
    :else nil))

(defn normalize-entity-id
  [x]
  (normalize-id x))

(defn normalize-field-id
  [x]
  (normalize-id x))

(defn read-edn-file
  [path]
  (try
    (edn/read-string (slurp path))
    (catch Exception e
      (throw (ex-info "Failed to read EDN"
               {:path path
                :cause (.getMessage e)}
               e)))))

(defn discover-domain-names
  "Return domain names under root that contain a config/ directory.

  Options:
  - :only [\"expenses\" ...] include only these domains
  - :skip [\"expenses\" ...] exclude these domains"
  ([root]
   (discover-domain-names root {}))
  ([root {:keys [only skip]}]
   (let [root-dir (io/file root)
         only-set (set (map str only))
         skip-set (set (map str skip))]
     (if-not (.exists root-dir)
       []
       (->> (.listFiles root-dir)
         (filter #(.isDirectory %))
         (map #(.getName %))
         (filter (fn [name]
                   (-> (io/file root-dir name "config") .isDirectory)))
         (filter (fn [name]
                   (or (empty? only-set) (contains? only-set name))))
         (remove (fn [name]
                   (contains? skip-set name)))
         sort
         vec)))))

(defn config-bundles
  "Discover admin + domain config bundles.

  Options:
  - :admin-root   (default src/app/admin/frontend/config)
  - :domain-root  (default src/app/domain/frontend)
  - :only         (domain allowlist)
  - :skip         (domain denylist)"
  ([] (config-bundles {}))
  ([{:keys [admin-root domain-root only skip]}]
   (let [admin-root (or admin-root "src/app/admin/frontend/config")
         domain-root (or domain-root "src/app/domain/frontend")
         admin-paths (into {}
                       (for [[kind filename] standard-config-files
                             :let [path (str (io/file admin-root filename))]
                             :when (file-exists? path)]
                         [kind path]))
         admin-bundle (when (seq admin-paths)
                        {:scope :admin
                         :domain nil
                         :paths admin-paths})
         domains (discover-domain-names domain-root {:only only :skip skip})
         domain-bundles (for [domain domains
                              :let [config-dir (str (io/file domain-root domain "config"))
                                    paths (into {}
                                            (for [[kind filename] standard-config-files
                                                  :let [path (str (io/file config-dir filename))]
                                                  :when (file-exists? path)]
                                              [kind path]))]
                              :when (seq paths)]
                          {:scope :domain
                           :domain domain
                           :paths paths})]
     (vec (concat (when admin-bundle [admin-bundle]) domain-bundles)))))

(defn load-bundles
  [bundles]
  (mapv (fn [bundle]
          (assoc bundle :data
            (into {}
              (for [[kind path] (:paths bundle)]
                [kind (read-edn-file path)]))))
    bundles))

;; =============================================================================
;; DB schema index
;; =============================================================================

(defn models-index
  "Load resources/db/models.edn and return a normalized schema index.

  Output:
  {:entities #{\"users\" ...}
   :entity->fields
   {\"users\" {:raw [\"id\" \"email\" ...]
             :canonical #{\"id\" \"email\" ...}
             :raw-by-canonical {\"id\" \"id\" ...}}}}
  "
  ([] (models-index "resources/db/models.edn"))
  ([path]
   (let [data (read-edn-file path)
         entities (keys data)
         entity->fields
         (reduce
           (fn [acc entity]
             (let [raw-fields (->> (get-in data [entity :fields])
                                (map first)
                                (map name)
                                vec)
                   canonical (set (map normalize-field-id raw-fields))
                   raw-by-canonical (zipmap (map normalize-field-id raw-fields) raw-fields)]
               (assoc acc
                 (normalize-entity-id entity)
                 {:raw raw-fields
                  :canonical canonical
                  :raw-by-canonical raw-by-canonical})))
           {}
           entities)]
     {:entities (set (map normalize-entity-id entities))
      :entity->fields entity->fields})))

;; =============================================================================
;; Allowlist handling
;; =============================================================================

(defn- normalize-allowlist-fields
  [fields]
  (->> fields
    (map normalize-field-id)
    (remove nil?)
    set))

(defn normalize-allowlist
  "Normalize an EDN allowlist into a map of canonical entity -> set of canonical fields.

  Supported formats:
  - map of entity -> collection of fields
  - set of fields (global allowlist for all entities)

  Use :* or :all as a global allowlist key in the map format."
  [allowlist]
  (cond
    (nil? allowlist) {:* #{}}

    (set? allowlist)
    {:* (normalize-allowlist-fields allowlist)}

    (map? allowlist)
    (reduce-kv
      (fn [acc entity fields]
        (let [entity* (normalize-entity-id entity)
              key* (if (#{"*" "all"} entity*) :* entity*)
              fields-coll (cond
                            (nil? fields) []
                            (set? fields) fields
                            (sequential? fields) fields
                            :else [fields])
              fields* (normalize-allowlist-fields fields-coll)]
          (update acc key* (fnil set/union #{}) fields*)))
      {:* #{}}
      allowlist)

    :else
    (throw (ex-info "Allowlist must be a map or set" {:type (type allowlist)}))))

(defn- allowed-fields
  [{:keys [schema-index entity computed allowlist include-computed?]}]
  (set/union
    (get-in schema-index [:entity->fields entity :canonical] #{})
    (when include-computed? (get computed entity #{}))
    (get allowlist :* #{})
    (get allowlist entity #{})))

;; =============================================================================
;; Field extraction helpers
;; =============================================================================

(def ^:private table-columns-list-keys
  [:available-columns :default-visible-columns :filterable-columns :sortable-columns :always-visible])

(def ^:private form-fields-list-keys
  [:create-fields :edit-fields :required-fields])

(defn- normalize-values
  [xs]
  (->> xs
    (map normalize-field-id)
    (remove nil?)
    set))

(defn- collect-field-ids
  [cfg list-keys extra-keys]
  (let [list-ids (mapcat #(get cfg % []) list-keys)
        extra-ids (mapcat #(keys (get cfg %)) extra-keys)]
    (concat list-ids extra-ids)))

(defn- table-column-ids
  [cfg]
  (collect-field-ids cfg table-columns-list-keys [:column-config]))

(defn- form-field-ids
  [cfg]
  (collect-field-ids cfg form-fields-list-keys [:field-config]))

(defn- view-option-column-ids
  [cfg]
  (concat (keys (:column-defaults cfg)) (keys (:column-locks cfg))))

(defn- computed-fields-by-entity
  [table-columns]
  (into {}
    (for [[entity cfg] table-columns]
      [(normalize-entity-id entity)
       (->> (:computed-fields cfg)
         keys
         (map normalize-field-id)
         (remove nil?)
         set)])))

;; =============================================================================
;; Semantic validation
;; =============================================================================

(defn- spec-validate
  [kind scope data]
  (case kind
    :entities (if (= :domain scope)
                (entities-spec/validate-user-entities data)
                (entities-spec/validate-admin-entities-strict data))
    :form-fields (form-fields-spec/validate-form-fields-strict data)
    :table-columns (table-columns-spec/validate-table-columns-strict data)
    :view-options (view-options-spec/validate-view-options-strict data)
    {:valid? false :errors [(str "Unknown config kind: " kind)]}))

(defn- unknown-values
  [values allowed]
  (->> values
    (filter (fn [v]
              (let [normalized (normalize-field-id v)]
                (not (contains? allowed normalized)))))
    vec))

(defn- semantic-issues-table-columns
  [data schema-index computed allowlist]
  (reduce-kv
    (fn [acc entity cfg]
      (let [entity* (normalize-entity-id entity)
            allowed (allowed-fields {:schema-index schema-index
                                     :entity entity*
                                     :computed computed
                                     :allowlist allowlist
                                     :include-computed? true})]
        (if-not (contains? (:entities schema-index) entity*)
          (update acc :unknown-entities conj entity)
          (let [unknown (into {}
                          (for [k table-columns-list-keys
                                :let [vals (get cfg k)
                                      unknown* (when (seq vals)
                                                 (unknown-values vals allowed))]
                                :when (seq unknown*)]
                            [k unknown*]))
                unknown-config (let [vals (keys (:column-config cfg))
                                     unknown* (when (seq vals)
                                                (unknown-values vals allowed))]
                                 (when (seq unknown*)
                                   {:column-config unknown*}))
                unknown (merge unknown unknown-config)
                available (normalize-values (:available-columns cfg))
                db-order (get-in schema-index [:entity->fields entity* :raw] [])
                missing (->> db-order
                          (remove (fn [f]
                                    (contains? available (normalize-field-id f))))
                          vec)
                acc (if (seq unknown)
                      (update acc :unknown-fields assoc entity unknown)
                      acc)
                acc (if (seq missing)
                      (update acc :missing-fields assoc entity missing)
                      acc)]
            acc))))
    {:unknown-entities []
     :unknown-fields {}
     :missing-fields {}}
    data))

(defn- semantic-issues-form-fields
  [data schema-index allowlist]
  (reduce-kv
    (fn [acc entity cfg]
      (let [entity* (normalize-entity-id entity)
            allowed (allowed-fields {:schema-index schema-index
                                     :entity entity*
                                     :computed {}
                                     :allowlist allowlist
                                     :include-computed? false})]
        (if-not (contains? (:entities schema-index) entity*)
          (update acc :unknown-entities conj entity)
          (let [unknown (into {}
                          (for [k form-fields-list-keys
                                :let [vals (get cfg k)
                                      unknown* (when (seq vals)
                                                 (unknown-values vals allowed))]
                                :when (seq unknown*)]
                            [k unknown*]))
                unknown-config (let [vals (keys (:field-config cfg))
                                     unknown* (when (seq vals)
                                                (unknown-values vals allowed))]
                                 (when (seq unknown*)
                                   {:field-config unknown*}))
                unknown (merge unknown unknown-config)
                acc (if (seq unknown)
                      (update acc :unknown-fields assoc entity unknown)
                      acc)]
            acc))))
    {:unknown-entities []
     :unknown-fields {}
     :missing-fields {}}
    data))

(defn- semantic-issues-view-options
  [data schema-index computed allowlist]
  (reduce-kv
    (fn [acc entity cfg]
      (let [entity* (normalize-entity-id entity)
            allowed (allowed-fields {:schema-index schema-index
                                     :entity entity*
                                     :computed computed
                                     :allowlist allowlist
                                     :include-computed? true})]
        (if-not (contains? (:entities schema-index) entity*)
          (update acc :unknown-entities conj entity)
          (let [unknown-defaults (unknown-values (keys (:column-defaults cfg)) allowed)
                unknown-locks (unknown-values (keys (:column-locks cfg)) allowed)
                unknown (cond-> {}
                          (seq unknown-defaults) (assoc :column-defaults unknown-defaults)
                          (seq unknown-locks) (assoc :column-locks unknown-locks))
                acc (if (seq unknown)
                      (update acc :unknown-fields assoc entity unknown)
                      acc)]
            acc))))
    {:unknown-entities []
     :unknown-fields {}
     :missing-fields {}}
    data))

(defn- semantic-issues-entities
  [data schema-index]
  (let [unknown (->> (keys data)
                  (remove #(contains? (:entities schema-index)
                             (normalize-entity-id %)))
                  vec)]
    {:unknown-entities unknown
     :unknown-fields {}
     :missing-fields {}}))

(defn validate-bundles
  "Validate all config bundles. Returns per-file results.

  Each result contains:
  {:scope :admin|:domain
   :domain \"expenses\"|nil
   :kind :entities|:form-fields|:table-columns|:view-options
   :path \"...\"
   :valid? boolean
   :errors []
   :warnings []
   :semantic {:unknown-entities [] :unknown-fields {} :missing-fields {}}}
  "
  [bundles schema-index allowlist]
  (let [allowlist (normalize-allowlist allowlist)]
    (mapcat
      (fn [bundle]
        (let [data (:data bundle)
              computed (computed-fields-by-entity (:table-columns data))]
          (for [[kind value] data
                :let [spec-result (spec-validate kind (:scope bundle) value)
                      semantic (if (:valid? spec-result)
                                 (case kind
                                   :entities (semantic-issues-entities value schema-index)
                                   :form-fields (semantic-issues-form-fields value schema-index allowlist)
                                   :table-columns (semantic-issues-table-columns value schema-index computed allowlist)
                                   :view-options (semantic-issues-view-options value schema-index computed allowlist)
                                   {:unknown-entities [] :unknown-fields {} :missing-fields {}})
                                 {:unknown-entities [] :unknown-fields {} :missing-fields {}})
                      semantic-errors? (or (seq (:unknown-entities semantic))
                                         (seq (:unknown-fields semantic)))
                      warnings (vec (concat (:warnings spec-result)
                                      (when (seq (:missing-fields semantic))
                                        [{:missing-fields (:missing-fields semantic)}])))
                      errors (vec (concat (:errors spec-result)
                                    (when semantic-errors?
                                      [(str "Schema alignment issues: "
                                         (pr-str (select-keys semantic
                                                   [:unknown-entities :unknown-fields])))])))]]
            (merge bundle
              {:kind kind
               :path (get-in bundle [:paths kind])
               :valid? (and (:valid? spec-result) (not semantic-errors?))
               :errors errors
               :warnings warnings
               :semantic semantic}))))
      bundles)))

;; =============================================================================
;; Sync planning
;; =============================================================================

(defn- infer-id-style
  [values]
  (let [values (filter #(or (string? %) (keyword? %)) values)
        names (map name values)
        dash-count (count (filter #(str/includes? % "-") names))
        underscore-count (count (filter #(str/includes? % "_") names))]
    {:separator (cond
                  (and (pos? dash-count) (>= dash-count underscore-count)) "-"
                  (pos? underscore-count) "_"
                  :else "_")
     :type (cond
             (some string? values) :string
             (some keyword? values) :keyword
             :else :string)}))

(defn- format-field-id
  [raw-name {:keys [separator type]}]
  (let [formatted (if (= separator "-")
                    (str/replace raw-name "_" "-")
                    raw-name)]
    (if (= type :keyword)
      (keyword formatted)
      formatted)))

(defn- append-missing
  [existing missing style]
  (let [existing (vec existing)
        additions (map #(format-field-id % style) missing)]
    (into existing additions)))

(defn- plan-table-columns
  [data schema-index computed allowlist]
  (reduce-kv
    (fn [acc entity cfg]
      (let [entity* (normalize-entity-id entity)
            allowed (allowed-fields {:schema-index schema-index
                                     :entity entity*
                                     :computed computed
                                     :allowlist allowlist
                                     :include-computed? true})]
        (if-not (contains? (:entities schema-index) entity*)
          (update acc :unknown-entities conj entity)
          (let [style (infer-id-style (mapcat #(get cfg % []) table-columns-list-keys))
                removed (into {}
                          (for [k table-columns-list-keys
                                :let [vals (get cfg k)
                                      unknown* (when (seq vals)
                                                 (unknown-values vals allowed))]
                                :when (seq unknown*)]
                            [k unknown*]))
                removed-config (let [vals (keys (:column-config cfg))
                                     unknown* (when (seq vals)
                                                (unknown-values vals allowed))]
                                 (when (seq unknown*)
                                   {:column-config unknown*}))
                removed (merge removed removed-config)
                filtered (into {}
                           (for [k table-columns-list-keys
                                 :when (contains? cfg k)
                                 :let [vals (get cfg k)
                                       keep* (->> (or vals [])
                                               (remove (set (get removed k)))
                                               vec)]]
                             [k keep*]))
                available-present? (contains? cfg :available-columns)
                available (get cfg :available-columns [])
                available-normalized (normalize-values available)
                db-order (get-in schema-index [:entity->fields entity* :raw] [])
                missing (->> db-order
                          (remove (fn [f]
                                    (contains? available-normalized (normalize-field-id f))))
                          vec)
                available* (when (or (seq missing) available-present?)
                             (append-missing (get filtered :available-columns available)
                               missing
                               style))
                column-config (:column-config cfg)
                column-config* (when (map? column-config)
                                 (apply dissoc column-config (get removed :column-config)))
                updates (cond-> {}
                          (and (some? available*)
                            (not= available available*))
                          (assoc :available-columns available*)

                          (and (contains? filtered :default-visible-columns)
                            (not= (:default-visible-columns cfg)
                              (:default-visible-columns filtered)))
                          (assoc :default-visible-columns (:default-visible-columns filtered))

                          (and (contains? filtered :filterable-columns)
                            (not= (:filterable-columns cfg)
                              (:filterable-columns filtered)))
                          (assoc :filterable-columns (:filterable-columns filtered))

                          (and (contains? filtered :sortable-columns)
                            (not= (:sortable-columns cfg)
                              (:sortable-columns filtered)))
                          (assoc :sortable-columns (:sortable-columns filtered))

                          (and (contains? filtered :always-visible)
                            (not= (:always-visible cfg)
                              (:always-visible filtered)))
                          (assoc :always-visible (:always-visible filtered))

                          (and (map? column-config)
                            (not= column-config column-config*))
                          (assoc :column-config column-config*)

                          (seq removed)
                          (assoc :removed-fields
                            (merge (:removed-fields cfg) removed)))
                summary (cond-> {}
                          (seq missing) (assoc :add-available missing)
                          (seq removed) (assoc :removed removed))]
            (cond-> acc
              (seq updates) (update :updates assoc entity updates)
              (seq summary) (update :summary assoc entity summary))))))
    {:updates {}
     :summary {}
     :unknown-entities []}
    data))

(defn- plan-form-fields
  [data schema-index allowlist]
  (reduce-kv
    (fn [acc entity cfg]
      (let [entity* (normalize-entity-id entity)
            allowed (allowed-fields {:schema-index schema-index
                                     :entity entity*
                                     :computed {}
                                     :allowlist allowlist
                                     :include-computed? false})]
        (if-not (contains? (:entities schema-index) entity*)
          (update acc :unknown-entities conj entity)
          (let [removed (into {}
                          (for [k form-fields-list-keys
                                :let [vals (get cfg k)
                                      unknown* (when (seq vals)
                                                 (unknown-values vals allowed))]
                                :when (seq unknown*)]
                            [k unknown*]))
                removed-config (let [vals (keys (:field-config cfg))
                                     unknown* (when (seq vals)
                                                (unknown-values vals allowed))]
                                 (when (seq unknown*)
                                   {:field-config unknown*}))
                removed (merge removed removed-config)
                filtered (into {}
                           (for [k form-fields-list-keys
                                 :when (contains? cfg k)
                                 :let [vals (get cfg k)
                                       keep* (->> (or vals [])
                                               (remove (set (get removed k)))
                                               vec)]]
                             [k keep*]))
                field-config (:field-config cfg)
                field-config* (when (map? field-config)
                                (apply dissoc field-config (get removed :field-config)))
                updates (cond-> {}
                          (and (contains? filtered :create-fields)
                            (not= (:create-fields cfg)
                              (:create-fields filtered)))
                          (assoc :create-fields (:create-fields filtered))

                          (and (contains? filtered :edit-fields)
                            (not= (:edit-fields cfg)
                              (:edit-fields filtered)))
                          (assoc :edit-fields (:edit-fields filtered))

                          (and (contains? filtered :required-fields)
                            (not= (:required-fields cfg)
                              (:required-fields filtered)))
                          (assoc :required-fields (:required-fields filtered))

                          (and (map? field-config)
                            (not= field-config field-config*))
                          (assoc :field-config field-config*)

                          (seq removed)
                          (assoc :removed-fields
                            (merge (:removed-fields cfg) removed)))
                summary (when (seq removed)
                          {:removed removed})]
            (cond-> acc
              (seq updates) (update :updates assoc entity updates)
              (seq summary) (update :summary assoc entity summary))))))
    {:updates {}
     :summary {}
     :unknown-entities []}
    data))

(defn- plan-view-options
  [data schema-index computed allowlist]
  (reduce-kv
    (fn [acc entity cfg]
      (let [entity* (normalize-entity-id entity)
            allowed (allowed-fields {:schema-index schema-index
                                     :entity entity*
                                     :computed computed
                                     :allowlist allowlist
                                     :include-computed? true})]
        (if-not (contains? (:entities schema-index) entity*)
          (update acc :unknown-entities conj entity)
          (let [unknown-defaults (unknown-values (keys (:column-defaults cfg)) allowed)
                unknown-locks (unknown-values (keys (:column-locks cfg)) allowed)
                removed (cond-> {}
                          (seq unknown-defaults) (assoc :column-defaults unknown-defaults)
                          (seq unknown-locks) (assoc :column-locks unknown-locks))
                column-defaults (:column-defaults cfg)
                column-locks (:column-locks cfg)
                updates (cond-> {}
                          (and (map? column-defaults) (seq unknown-defaults))
                          (assoc :column-defaults (apply dissoc column-defaults unknown-defaults))

                          (and (map? column-locks) (seq unknown-locks))
                          (assoc :column-locks (apply dissoc column-locks unknown-locks))

                          (seq removed)
                          (assoc :removed-fields
                            (merge (:removed-fields cfg) removed)))
                summary (when (seq removed)
                          {:removed removed})]
            (cond-> acc
              (seq updates) (update :updates assoc entity updates)
              (seq summary) (update :summary assoc entity summary))))))
    {:updates {}
     :summary {}
     :unknown-entities []}
    data))

(defn plan-sync
  "Compute sync patches per bundle. Returns a vector of file patches."
  [bundles schema-index allowlist]
  (let [allowlist (normalize-allowlist allowlist)]
    (mapcat
      (fn [bundle]
        (let [data (:data bundle)
              computed (computed-fields-by-entity (:table-columns data))]
          (for [[kind value] data
                :let [plan (case kind
                             :table-columns (plan-table-columns value schema-index computed allowlist)
                             :form-fields (plan-form-fields value schema-index allowlist)
                             :view-options (plan-view-options value schema-index computed allowlist)
                             :entities {:updates {} :summary {} :unknown-entities []})
                      has-changes? (or (seq (:updates plan))
                                     (seq (:unknown-entities plan)))]]
            (merge bundle
              {:kind kind
               :path (get-in bundle [:paths kind])
               :updates (:updates plan)
               :summary (:summary plan)
               :unknown-entities (:unknown-entities plan)
               :has-changes? has-changes?}))))
      bundles)))
