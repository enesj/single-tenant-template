(ns app.template.backend.routes.admin.settings-io
  "Admin settings I/O backed by DB-persisted runtime config.

   All mutable frontend config reads come from the runtime store
   (`frontend_runtime_configs` table). Source-controlled EDN files are
   no longer consulted at request time — they are consumed only once by
   the bootstrap path that seeds fresh environments."
  (:require
    [app.shared.specs.entities :as entities-spec]
    [app.shared.specs.form-fields :as form-fields-spec]
    [app.shared.specs.table-columns :as table-columns-spec]
    [app.shared.specs.view-options :as view-options-spec]
    [clojure.edn :as edn]
    [clojure.string :as str]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]
    [taoensso.timbre :as log])
  (:import
    [java.util UUID]))

(def ^:private admin-scope "admin")
(def ^:private user-scope "user")

;; ---------------------------------------------------------------------------
;; Validation
;; ---------------------------------------------------------------------------

(defn- validate-or-throw!
  [label validation extra-data]
  (when-not (:valid? validation)
    (log/error (str "Attempted to write invalid " label " data")
      (merge extra-data (dissoc validation :valid?)))
    (throw (ex-info (str "Invalid " label " data")
             (merge {:status 400}
               extra-data
               (dissoc validation :valid?))))))

(defn- warn-on-invalid
  "Log a warning when validation fails, but do not throw."
  [label validation]
  (when-not (:valid? validation)
    (log/warn (str label " validation issues")
      (dissoc validation :valid?))))

;; ---------------------------------------------------------------------------
;; Runtime store — read / write
;; ---------------------------------------------------------------------------

(defn- read-runtime-override
  "Read a runtime config blob from the database.

   Returns an EDN map or {} when no row exists."
  [db scope config-key]
  (try
    (let [row (jdbc/execute-one!
                db
                (sql/format {:select [:config_edn]
                             :from [:frontend_runtime_configs]
                             :where [:and
                                     [:= :scope scope]
                                     [:= :config_key (name config-key)]]})
                {:builder-fn rs/as-unqualified-lower-maps})]
      (if-let [config-edn (:config_edn row)]
        (or (edn/read-string config-edn) {})
        {}))
    (catch Exception e
      (log/error e "Failed to read runtime frontend config"
        {:scope scope :config-key config-key})
      (throw (ex-info "Failed to read persisted runtime config"
               {:status 500
                :scope scope
                :config-key config-key})))))

(defn- write-runtime-override!
  "Persist a runtime config blob in the database via upsert."
  [db scope config-key data]
  (try
    (jdbc/execute-one!
      db
      [(str
         "INSERT INTO frontend_runtime_configs "
         "(id, scope, config_key, config_edn, created_at, updated_at) "
         "VALUES (?, ?, ?, ?, NOW(), NOW()) "
         "ON CONFLICT (scope, config_key) DO UPDATE SET "
         "config_edn = EXCLUDED.config_edn, updated_at = NOW()")
       (UUID/randomUUID)
       scope
       (name config-key)
       (pr-str data)])
    data
    (catch Exception e
      (log/error e "Failed to write runtime frontend config"
        {:scope scope :config-key config-key})
      (throw (ex-info "Failed to persist runtime config"
               {:status 500
                :scope scope
                :config-key config-key})))))

;; ---------------------------------------------------------------------------
;; Table-columns transforms (legacy promotion + retired column pruning)
;; ---------------------------------------------------------------------------

(def ^:private legacy-admin-default-visible-columns
  {:admins ["id"
            "email"
            "full-name"
            "role"
            "status"
            "last-login-at"
            "created-at"
            "updated-at"]
   :users ["id" "status" "created-at"]})

(defn- promote-legacy-admin-table-columns-override
  "Upgrade legacy persisted admin table-column defaults for users/admins.

   This is intentionally narrow: we only rewrite the exact historic defaults that
   shipped before the new all-columns-visible behavior. Future intentional admin
   edits to different subsets are preserved as-is."
  [table-columns]
  (reduce-kv
    (fn [acc entity-key legacy-defaults]
      (let [available (get-in acc [entity-key :available-columns])
            defaults (get-in acc [entity-key :default-visible-columns])]
        (if (and (seq available)
              (= (vec defaults) (vec legacy-defaults)))
          (assoc-in acc [entity-key :default-visible-columns] (vec available))
          acc)))
    (or table-columns {})
    legacy-admin-default-visible-columns))

(defn- canonical-config-id
  [value]
  (some-> value
    name
    str/trim
    not-empty
    (str/replace "_" "-")))

(defn- retired-id-set
  [ids]
  (into #{} (keep canonical-config-id) ids))

(defn- prune-retired-id-list
  [xs retired-ids]
  (let [retired (retired-id-set retired-ids)]
    (->> (or xs [])
      (remove #(contains? retired (canonical-config-id %)))
      vec)))

(defn- prune-retired-id-map
  [m retired-ids]
  (let [retired (retired-id-set retired-ids)]
    (reduce-kv
      (fn [acc k v]
        (if (contains? retired (canonical-config-id k))
          acc
          (assoc acc k v)))
      {}
      (or m {}))))

(def ^:private retired-expense-config-ids
  #{"is-posted"})

(def ^:private retired-admin-table-columns
  {:article-aliases #{"confidence"}
   :users #{"avatar-url"}
   :expenses retired-expense-config-ids})

(def ^:private retired-user-table-columns
  {:expenses retired-expense-config-ids})

(defn- remove-retired-table-columns
  [entity-config retired-columns]
  (let [entity-config (or entity-config {})]
    (cond-> entity-config
      (contains? entity-config :available-columns)
      (update :available-columns prune-retired-id-list retired-columns)

      (contains? entity-config :default-visible-columns)
      (update :default-visible-columns prune-retired-id-list retired-columns)

      (contains? entity-config :filterable-columns)
      (update :filterable-columns prune-retired-id-list retired-columns)

      (contains? entity-config :sortable-columns)
      (update :sortable-columns prune-retired-id-list retired-columns)

      (contains? entity-config :always-visible)
      (update :always-visible prune-retired-id-list retired-columns)

      (contains? entity-config :visible-columns)
      (update :visible-columns prune-retired-id-list retired-columns)

      (contains? entity-config :computed-fields)
      (update :computed-fields prune-retired-id-map retired-columns)

      (contains? entity-config :column-config)
      (update :column-config prune-retired-id-map retired-columns)

      (contains? entity-config :column-metadata)
      (update :column-metadata prune-retired-id-map retired-columns))))

(defn- retire-table-columns
  [table-columns retired-columns-by-entity]
  (reduce-kv
    (fn [acc entity-key retired-columns]
      (if (contains? acc entity-key)
        (update acc entity-key remove-retired-table-columns retired-columns)
        acc))
    (or table-columns {})
    retired-columns-by-entity))

(defn- retire-admin-table-columns
  [table-columns]
  (retire-table-columns table-columns retired-admin-table-columns))

(defn- retire-user-table-columns
  [table-columns]
  (retire-table-columns table-columns retired-user-table-columns))

(def ^:private legacy-admin-email-column "email-masked")
(def ^:private canonical-admin-email-column "email")

(defn- normalize-admin-email-column-id
  [value]
  (let [value-name (some-> value name str/trim)]
    (if (= legacy-admin-email-column value-name)
      (if (keyword? value)
        :email
        canonical-admin-email-column)
      value)))

(defn- normalize-admin-email-column-list
  [columns]
  (->> (or columns [])
    (map normalize-admin-email-column-id)
    (remove nil?)
    distinct
    vec))

(defn- normalize-admin-email-column-map
  [m]
  (reduce-kv
    (fn [acc k v]
      (assoc acc (normalize-admin-email-column-id k) v))
    {}
    (or m {})))

(defn- normalize-admin-email-columns
  [table-columns]
  (update (or table-columns {}) :admins
    (fn [entity-config]
      (cond-> (or entity-config {})
        true (update :available-columns normalize-admin-email-column-list)
        true (update :default-visible-columns normalize-admin-email-column-list)
        true (update :filterable-columns normalize-admin-email-column-list)
        true (update :sortable-columns normalize-admin-email-column-list)
        true (update :always-visible normalize-admin-email-column-list)
        true (update :computed-fields normalize-admin-email-column-map)
        true (update :column-config normalize-admin-email-column-map)
        true (update :column-metadata normalize-admin-email-column-map)))))

(def ^:private payer-type-filter-options
  [{:value "system" :label "system"}
   {:value "custom" :label "custom"}])

(def ^:private legacy-user-payer-type-columns
  [:payer_type_label "payer_type_label" :payer_type "payer_type"])

(defn- referenced-user-payer-type-column
  [entity-config]
  (or
    (some #(when (contains? (or (:computed-fields entity-config) {}) %)
             %)
      legacy-user-payer-type-columns)
    (let [referenced-columns (into #{}
                               (mapcat #(or (get entity-config %) []))
                               [:available-columns
                                :default-visible-columns
                                :filterable-columns
                                :sortable-columns
                                :always-visible])]
      (some referenced-columns legacy-user-payer-type-columns))))

(defn- ensure-user-payer-type-select-filter
  [table-columns]
  (if-let [payer-type-column (some-> (get table-columns :payers)
                               referenced-user-payer-type-column)]
    (let [current-field (get-in table-columns [:payers :computed-fields payer-type-column])
          current-label (some-> (:label current-field) str str/trim not-empty)]
      (assoc-in table-columns [:payers :computed-fields payer-type-column]
                (cond-> (assoc (or current-field {})
                          :type "select"
                          :options payer-type-filter-options)
                  (nil? current-label) (assoc :label "Payer Type"))))
    (or table-columns {})))

(defn- normalize-user-table-columns
  [table-columns]
  (-> (or table-columns {})
    ensure-user-payer-type-select-filter
    retire-user-table-columns))

(def ^:private retired-admin-form-fields
  {:expenses retired-expense-config-ids})

(def ^:private retired-user-form-fields
  {:expenses retired-expense-config-ids})

(defn- remove-retired-form-fields
  [entity-config retired-fields]
  (let [entity-config (or entity-config {})]
    (cond-> entity-config
      (contains? entity-config :create-fields)
      (update :create-fields prune-retired-id-list retired-fields)

      (contains? entity-config :edit-fields)
      (update :edit-fields prune-retired-id-list retired-fields)

      (contains? entity-config :field-config)
      (update :field-config prune-retired-id-map retired-fields)

      (contains? entity-config :removed-fields)
      (update :removed-fields
        (fn [removed-fields]
          (let [removed-fields (or removed-fields {})]
            (cond-> removed-fields
              (contains? removed-fields :create-fields)
              (update :create-fields prune-retired-id-list retired-fields)

              (contains? removed-fields :edit-fields)
              (update :edit-fields prune-retired-id-list retired-fields))))))))

(defn- retire-form-fields
  [form-fields retired-fields-by-entity]
  (reduce-kv
    (fn [acc entity-key retired-fields]
      (if (contains? acc entity-key)
        (update acc entity-key remove-retired-form-fields retired-fields)
        acc))
    (or form-fields {})
    retired-fields-by-entity))

(defn- retire-admin-form-fields
  [form-fields]
  (retire-form-fields form-fields retired-admin-form-fields))

(defn- retire-user-form-fields
  [form-fields]
  (retire-form-fields form-fields retired-user-form-fields))

(def ^:private retired-admin-view-option-columns
  {:expenses retired-expense-config-ids})

(def ^:private retired-user-view-option-columns
  {:expenses retired-expense-config-ids})

(defn- remove-retired-view-option-columns
  [entity-config retired-columns]
  (let [entity-config (or entity-config {})]
    (cond-> entity-config
      (contains? entity-config :column-defaults)
      (update :column-defaults prune-retired-id-map retired-columns)

      (contains? entity-config :column-locks)
      (update :column-locks prune-retired-id-map retired-columns))))

(defn- retire-view-options
  [view-options retired-columns-by-entity]
  (reduce-kv
    (fn [acc entity-key retired-columns]
      (if (contains? acc entity-key)
        (update acc entity-key remove-retired-view-option-columns retired-columns)
        acc))
    (or view-options {})
    retired-columns-by-entity))

(defn- retire-admin-view-options
  [view-options]
  (retire-view-options view-options retired-admin-view-option-columns))

(defn- retire-user-view-options
  [view-options]
  (retire-view-options view-options retired-user-view-option-columns))

;; ---------------------------------------------------------------------------
;; Admin config — read / write
;; ---------------------------------------------------------------------------

(defn read-view-options
  "Read admin view-options from the runtime store."
  [db]
  (let [data (-> (read-runtime-override db admin-scope :view-options)
               retire-admin-view-options)]
    (warn-on-invalid "admin view-options"
      (view-options-spec/validate-view-options-strict data))
    data))

(defn write-view-options!
  "Persist admin view-options in the runtime store."
  [db view-options]
  (let [sanitized-view-options (retire-admin-view-options view-options)
        validation (view-options-spec/validate-view-options-strict sanitized-view-options)]
    (validate-or-throw! "view-options" validation
      {:errors (:errors validation)
       :nested-locks-errors (:nested-locks-errors validation)})
    (write-runtime-override! db admin-scope :view-options sanitized-view-options)))

(defn read-form-fields
  "Read admin form-fields from the runtime store."
  [db]
  (let [data (-> (read-runtime-override db admin-scope :form-fields)
               form-fields-spec/sanitize-form-fields
               retire-admin-form-fields)]
    (warn-on-invalid "admin form-fields"
      (form-fields-spec/validate-form-fields-strict data))
    data))

(defn write-form-fields!
  "Persist admin form-fields in the runtime store."
  [db form-fields]
  (let [sanitized-form-fields (-> form-fields
                                form-fields-spec/sanitize-form-fields
                                retire-admin-form-fields)
        validation (form-fields-spec/validate-form-fields-strict sanitized-form-fields)]
    (validate-or-throw! "form-fields" validation
      {:errors (:errors validation)
       :warnings (:warnings validation)})
    (write-runtime-override! db admin-scope :form-fields sanitized-form-fields)))

(defn read-table-columns
  "Read admin table-columns from the runtime store."
  [db]
  (-> (read-runtime-override db admin-scope :table-columns)
    normalize-admin-email-columns
    promote-legacy-admin-table-columns-override
    retire-admin-table-columns
    (doto (->> (table-columns-spec/validate-table-columns-strict)
            (warn-on-invalid "admin table-columns")))))

(defn write-table-columns!
  "Persist admin table-columns in the runtime store."
  [db table-columns]
  (let [sanitized-table-columns (-> table-columns
                                  normalize-admin-email-columns
                                  retire-admin-table-columns)
        validation (table-columns-spec/validate-table-columns-strict sanitized-table-columns)]
    (validate-or-throw! "table-columns" validation
      {:errors (:errors validation)
       :warnings (:warnings validation)})
    (write-runtime-override! db admin-scope :table-columns sanitized-table-columns)))

;; ---------------------------------------------------------------------------
;; User config — read / write
;; ---------------------------------------------------------------------------

(defn read-user-entities
  "Read user-facing entities from the runtime store."
  [db]
  (let [data (read-runtime-override db user-scope :entities)]
    (warn-on-invalid "user entities"
      (entities-spec/validate-user-entities data))
    data))

(defn write-user-entities!
  [db entities]
  (let [validation (entities-spec/validate-user-entities entities)]
    (validate-or-throw! "user entities" validation
      {:errors (:errors validation)}))
  (write-runtime-override! db user-scope :entities entities))

(defn read-user-view-options
  "Read user-facing view-options from the runtime store."
  [db]
  (let [data (-> (read-runtime-override db user-scope :view-options)
               retire-user-view-options)]
    (warn-on-invalid "user view-options"
      (view-options-spec/validate-view-options-strict data))
    data))

(defn write-user-view-options!
  [db view-options]
  (let [sanitized-view-options (retire-user-view-options view-options)
        validation (view-options-spec/validate-view-options-strict sanitized-view-options)]
    (validate-or-throw! "user view-options" validation
      {:errors (:errors validation)
       :nested-locks-errors (:nested-locks-errors validation)})
    (write-runtime-override! db user-scope :view-options sanitized-view-options)))

(defn read-user-form-fields
  "Read user-facing form-fields from the runtime store."
  [db]
  (let [data (-> (read-runtime-override db user-scope :form-fields)
               form-fields-spec/sanitize-form-fields
               retire-user-form-fields)]
    (warn-on-invalid "user form-fields"
      (form-fields-spec/validate-form-fields-strict data))
    data))

(defn write-user-form-fields!
  [db form-fields]
  (let [sanitized-form-fields (-> form-fields
                                form-fields-spec/sanitize-form-fields
                                retire-user-form-fields)
        validation (form-fields-spec/validate-form-fields-strict sanitized-form-fields)]
    (validate-or-throw! "user form-fields" validation
      {:errors (:errors validation)
       :warnings (:warnings validation)})
    (write-runtime-override! db user-scope :form-fields sanitized-form-fields)))

(defn read-user-table-columns
  "Read user-facing table-columns from the runtime store."
  [db]
  (let [data (-> (read-runtime-override db user-scope :table-columns)
               normalize-user-table-columns)]
    (warn-on-invalid "user table-columns"
      (table-columns-spec/validate-table-columns-strict data))
    data))

(defn write-user-table-columns!
  [db table-columns]
  (let [sanitized-table-columns (normalize-user-table-columns table-columns)
        validation (table-columns-spec/validate-table-columns-strict sanitized-table-columns)]
    (validate-or-throw! "user table-columns" validation
      {:errors (:errors validation)
       :warnings (:warnings validation)})
    (write-runtime-override! db user-scope :table-columns sanitized-table-columns)))