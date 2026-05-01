(ns app.admin.frontend.specs.generic
  "Enhanced generic spec generation utilities for admin frontend using vector-based configuration.

   This namespace provides utilities for generating entity and form specs from
   vector-based UI configuration with intelligent fallbacks to models.edn data.

   Key benefits:
   - Clean separation: UI configuration separate from database schema
   - Vector-based: Explicit column ordering and visibility
   - Flexible: User preferences layer over default configurations
   - Maintainable: Configuration-driven approach eliminates hardcoded customizations"
  {:clj-kondo/ignore [:unused-namespace]}
  (:require
    [app.admin.frontend.config.loader :as config-loader]
    [app.admin.frontend.specs.form-components :as form-components]
    [app.admin.frontend.utils.vector-config :as vector-config]
    [app.shared.field-specs :as field-specs]
    [app.shared.keywords :as kw]
    [app.shared.model-naming :as model-naming]
    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.settings.resolver :as resolver]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

(defn- create-column-spec-from-config
  "Create a column spec from vector configuration"
  [locale column-key column-config computed-fields column-metadata base-field-spec]
  (let [computed-field (resolver/lookup-column-entry computed-fields column-key)
        label-override (resolver/resolve-column-label-override locale {:column-metadata column-metadata} column-key)
        resolved-type (or (:type column-config)
                        (:type computed-field)
                        (:type base-field-spec)
                        :text)
        resolved-input-type (or (:input-type column-config)
                              (:input-type computed-field)
                              (:input-type base-field-spec))
        resolved-options (or (:options column-config)
                           (:options computed-field)
                           (:options base-field-spec))
        base-spec {:id column-key
                   :label (or label-override
                            (:label computed-field)
                            (:label base-field-spec)
                            (-> (name column-key)
                              (str/replace #"[_-]" " ")))
                   :type resolved-type}
        width-config (when-let [width (:width column-config)]
                       {:width width})
        formatter-config (when-let [formatter (:formatter column-config)]
                           {:formatter formatter})
        input-type-config (when resolved-input-type
                            {:input-type resolved-input-type})
        options-config (when resolved-options
                         {:options resolved-options})
        computed-config (when computed-field
                          {:computed true
                           :dependencies (:dependencies computed-field)})
        always-visible-config (when (:always-visible column-config)
                                {:always-visible true})]
    (merge base-spec
      width-config
      formatter-config
      input-type-config
      options-config
      computed-config
      always-visible-config)))

(def ^:private stable-receipt-status-options
  [{:value "extracted" :label "Extracted"}
   {:value "review_required" :label "Review required"}
   {:value "posted" :label "Posted"}
   {:value "failed" :label "Failed"}])

(defn- receipt-status-column?
  [entity-keyword column-key]
  (and (= :receipts (model-naming/ensure-app-keyword entity-keyword))
    (= :status (model-naming/ensure-app-keyword column-key))))

(defn- stabilize-receipt-status-column
  [entity-keyword column-key column-spec]
  (cond-> column-spec
    (receipt-status-column? entity-keyword column-key)
    (merge {:type "select"
            :input-type "select"
            :options stable-receipt-status-options})))

;; Read from Re-frame DB with route-aware fallback via resolver.
(defn- generate-admin-entity-spec-from-db
  "Generate admin entity spec using vector-based configuration from Re-frame DB.

   Uses resolver/resolve-config-source for route-aware admin/domain fallback."
  [db entity-keyword]
  (let [admin-route? (paths/admin-route? db)
        table-config (resolver/resolve-config-source
                       admin-route?
                       (get-in db [:admin :config :table-columns entity-keyword])
                       (get-in db [:domain :config :table-columns entity-keyword]))]
    (if table-config
      (let [{:keys [available-columns computed-fields column-config always-visible column-metadata]} table-config
            locale (or (:locale db) :bs)
            model-fields (or (get-in db [:entities :specs (model-naming/ensure-app-keyword entity-keyword)])
                           [])
            fields-by-id (into {}
                           (map (fn [field]
                                  [(:id field) field]))
                           model-fields)
            always-visible-set (->> (or always-visible [])
                                 (keep model-naming/ensure-app-keyword)
                                 set)
            column-specs (mapv (fn [column-key]
                                 (let [normalized-column (model-naming/ensure-app-keyword column-key)
                                       specific-config (or (resolver/lookup-column-entry column-config column-key) {})
                                       base-field-spec (resolver/lookup-column-entry fields-by-id column-key)
                                       is-always-visible (contains? always-visible-set normalized-column)]
                                   (stabilize-receipt-status-column
                                     entity-keyword
                                     column-key
                                     (create-column-spec-from-config
                                       locale
                                       column-key
                                       (assoc specific-config :always-visible is-always-visible)
                                       computed-fields
                                       column-metadata
                                       base-field-spec))))
                           available-columns)]
        {:id entity-keyword
         :fields column-specs
         :vector-config table-config})
      ;; No config found in DB – only log after configs have been loaded
      (do
        (when (:admin/config-loaded? db)
          (log/warn "No table config found for entity:" entity-keyword "available entities:" (keys (get-in db [:admin :config :table-columns]))))
        nil))))

;; Admin-specific entity specs subscription - reads from Re-frame DB
;; Now returns the full entity-spec map instead of just fields vector
(rf/reg-sub
  :admin/entity-specs-by-name
  (fn [db [_ entity-name]]
    (generate-admin-entity-spec-from-db db entity-name)))

;; =============================================================================
;; Admin Form Entity Specs - form-fields.edn based configuration
;; =============================================================================

(defn- field-type->input-type
  "Map form-fields.edn :type to field spec format"
  [field-type]
  (case field-type
    :email "email"
    :password "password"
    :text "text"
    :string "text"
    :textarea "textarea"
    :select "select"
    :number "number"
    :checkbox "checkbox"
    :boolean "checkbox"
    :date "date"
    :datetime "datetime-local"
    :datetime-local "datetime-local"
    :uuid "text"
    "text"))

(defn- normalize-select-options
  "Convert options to the format expected by select component: [{:value :label}]"
  [options]
  (when (seq options)
    (mapv (fn [opt]
            (cond
              ;; Already in correct format
              (map? opt)
              (let [v (:value opt)
                    v-str (cond
                            (keyword? v) (name v)
                            (symbol? v) (name v)
                            (string? v) v
                            (nil? v) ""
                            :else (str v))
                    l (:label opt)
                    l-str (cond
                            (string? l) l
                            (keyword? l) (name l)
                            (symbol? l) (name l)
                            (nil? l) (let [base (-> v-str
                                                  (str/replace "-" " ")
                                                  (str/replace "_" " "))]
                                       (if (re-matches #"[A-Z0-9]{2,}" base)
                                         base
                                         (str/capitalize base)))
                            :else (str l))]
                (assoc opt :value v-str :label l-str))

              :else
              (let [v-str (cond
                            (keyword? opt) (name opt)
                            (symbol? opt) (name opt)
                            (string? opt) opt
                            (nil? opt) ""
                            :else (str opt))]
                {:value v-str
                 :label (let [base (-> v-str
                                     (str/replace "-" " ")
                                     (str/replace "_" " "))]
                          (if (re-matches #"[A-Z0-9]{2,}" base)
                            base
                            (str/capitalize base)))})))
      options)))

(defn- ->kw
  "Coerce an identifier into the canonical app keyword.

  Admin UI config is transported over JSON, so collections like
  :create-fields / :edit-fields often arrive as strings.

  This function also normalizes snake_case identifiers into kebab-case
  (e.g. \"manufacturer_id\" -> :manufacturer-id) so form specs match
  the normalized entity keys used across the app."
  [x]
  (model-naming/ensure-app-keyword x))

(defn- normalize-field-type
  "Normalize a form-fields.edn :type value that may arrive as a string from JSON."
  [t]
  (cond
    (keyword? t) t
    (string? t) (keyword t)
    (symbol? t) (keyword (name t))
    :else t))

(defn- cfg-get
  "Get a value from a config map that may have keyword keys (EDN) or string keys
  (JSON). Also supports underscore variants (e.g. \"min_length\")."
  [m k]
  (when (map? m)
    (cond
      (keyword? k)
      (let [k-name (name k)
            k-us (str/replace k-name "-" "_")]
        (or (get m k)
          (get m k-name)
          (get m k-us)))

      (string? k)
      (let [k-kw (keyword k)
            k-kebab (keyword (str/replace k "_" "-"))]
        (or (get m k)
          (get m k-kw)
          (get m k-kebab)))

      :else
      (get m k))))

(defn- build-field-spec-from-config
  "Build a field spec from form-fields.edn field configuration.

  Supports keys: :type, :label, :label-key, :options, :placeholder, :default,
  :min-length, :max-length, :validation, :min, :max, :step

  :options supports two shapes:
  - Static options (e.g. [:a :b] or [{:value \"a\" :label \"A\"} ...])
  - Foreign-key options vector: [entity-name label-field]
    (e.g. [:suppliers :display_name])"
  [locale entity-key field-key field-config _editing?]
  (let [component (form-components/get-form-field-component entity-key field-key)
        field-type (normalize-field-type (or (cfg-get field-config :type) :text))
        base {:id field-key
              :label (resolver/resolve-field-label
                       locale
                       field-key
                       {:label (cfg-get field-config :label)
                        :label-key (cfg-get field-config :label-key)})
              :type field-type
              :input-type (field-type->input-type field-type)}
        options (cfg-get field-config :options)
        foreign-key-options? (and (vector? options)
                               (= 2 (count options))
                               (every? #(or (keyword? %) (string? %) (symbol? %)) options))
        normalized-options (when (some? options)
                             (if foreign-key-options?
                               (mapv ->kw options)
                               (or (normalize-select-options options) [])))
        with-options (if (some? options)
                       (assoc base :options normalized-options)
                       base)
        placeholder (cfg-get field-config :placeholder)
        with-placeholder (if placeholder
                           (assoc with-options :placeholder placeholder)
                           with-options)
        with-validation (cond-> with-placeholder
                          (cfg-get field-config :min-length) (assoc :min-length (cfg-get field-config :min-length))
                          (cfg-get field-config :max-length) (assoc :max-length (cfg-get field-config :max-length))
                          (cfg-get field-config :validation) (assoc :validation (cfg-get field-config :validation))

                          ;; Numeric field constraints
                          (cfg-get field-config :min) (assoc :min (cfg-get field-config :min))
                          (cfg-get field-config :max) (assoc :max (cfg-get field-config :max))
                          (cfg-get field-config :step) (assoc :step (cfg-get field-config :step))

                          ;; Default value for field initialization
                          (cfg-get field-config :default) (assoc :default (cfg-get field-config :default)
                                                            :default-value (cfg-get field-config :default)))
        with-component (if component
                         (assoc with-validation :component component)
                         with-validation)]
    with-component))

(def ^:private user-route-form-exclusions
  "Fields that should never be editable on user routes.

  These commonly trigger generic lookups (e.g. :users) and are typically
  system-managed."
  #{:user-id :user_id :tenant-id :tenant_id})

(defn- sanitize-user-route-form-spec
  "Remove sensitive/system-managed fields from a form entity spec on user routes."
  [spec]
  (when (sequential? spec)
    (->> spec
      (remove (fn [field]
                (contains? user-route-form-exclusions (->kw (:id field)))))
      vec)))

(defn- generate-form-entity-spec-from-config
  "Generate a form entity spec from a form-fields.edn-style config map.

  The config is expected to have keys like :create-fields/:edit-fields/:batch-edit-fields
  and optionally :required-fields and :field-config.

  `mode-or-editing` accepts nil/false for create mode, true for edit mode,
  and :batch-edit for batch updates."
  [locale entity-keyword form-config mode-or-editing]
  (when (map? form-config)
    (let [{:keys [create-fields edit-fields batch-edit-fields required-fields field-config]} form-config
          mode (cond
                 (= mode-or-editing :batch-edit) :batch-edit
                 (true? mode-or-editing) :edit
                 :else :create)
          fields-source (case mode
             :batch-edit (cond
                 (contains? form-config :batch-edit-fields) batch-edit-fields
                 (seq edit-fields) edit-fields
                 :else create-fields)
                          :edit edit-fields
                          create-fields)
          ;; Config files are read server-side as EDN, but delivered client-side via JSON.
          ;; JSON arrays preserve values as strings, so we normalize identifiers here.
          fields-to-show (mapv ->kw fields-source)
          required-set (set (map ->kw required-fields))
          ;; Normalize field-config keys to canonical app keywords so lookups are stable
          ;; across EDN (keyword keys) and JSON (string keys).
          field-config (let [fc (or field-config {})]
                         (if (map? fc)
                           (into {}
                             (map (fn [[k v]] [(->kw k) v]))
                             fc)
                           {}))]
      (when (seq fields-to-show)
        (mapv (fn [field-key]
                (let [field-key (->kw field-key)
                      config (or (get field-config field-key) {})
                      spec (build-field-spec-from-config locale entity-keyword field-key config mode-or-editing)]
                  (if (contains? required-set field-key)
                    (assoc spec :required true)
                    spec)))
          fields-to-show)))))

;; Override the template :form-entity-specs/by-name subscription for both admin + user routes.
;;
;; Admin routes:
;;   - prefer [:admin :config :form-fields]
;; User routes:
;;   - prefer [:domain :config :form-fields]
;;   - if we fall back to models-data, strip system-managed fields that frequently trigger
;;     blocked generic lookups (e.g. /api/v1/entities/users).
;;
;; The subscription accepts an optional second parameter for mode:
;; - omitted / false => create mode
;; - true => edit mode
;; - :batch-edit => batch edit mode
;; Usage: [:form-entity-specs/by-name :users], [:form-entity-specs/by-name :users true],
;;        [:form-entity-specs/by-name :expenses :batch-edit].
(rf/reg-sub
  :form-entity-specs/by-name
  (fn [db [_ entity-name mode-or-editing]]
    (let [entity-name (-> entity-name
                        ->kw
                        model-naming/db-keyword->app)
          admin-route? (paths/admin-route? db)
          locale (if admin-route?
                   :en
                   (or (:locale db) :bs))
          form-config (resolver/resolve-config-source
                        admin-route?
                        (get-in db [:admin :config :form-fields entity-name])
                        (get-in db [:domain :config :form-fields entity-name]))
          spec-from-config (when form-config
                             (generate-form-entity-spec-from-config locale entity-name form-config mode-or-editing))
          spec-from-models (when-let [md (:models-data db)]
                             (get (field-specs/form-entity-specs md) entity-name))
          spec-from-models* (if admin-route?
                              spec-from-models
                              (sanitize-user-route-form-spec spec-from-models))]
      (or spec-from-config
        spec-from-models*))))
