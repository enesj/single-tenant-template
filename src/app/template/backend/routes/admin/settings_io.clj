(ns app.template.backend.routes.admin.settings-io
  "Admin settings I/O backed by DB-persisted runtime overrides.

   Source-controlled EDN files remain the defaults for both admin-owned and
   domain-owned UI config. Runtime saves are stored in the database and merged
   over those defaults on read so deployed environments do not depend on a
   writable application filesystem."
  (:require
    [app.domain.backend.registry :as domain-registry]
    [app.shared.frontend-config.io :as frontend-config-io]
    [app.shared.specs.entities :as entities-spec]
    [app.shared.specs.form-fields :as form-fields-spec]
    [app.shared.specs.table-columns :as table-columns-spec]
    [app.shared.specs.view-options :as view-options-spec]
    [clojure.edn :as edn]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]
    [taoensso.timbre :as log])
  (:import
    [java.util UUID]))

;; Admin-owned UI config paths (source-controlled defaults)
(def ^:private view-options-path "src/app/admin/frontend/config/view-options.edn")
(def ^:private form-fields-path "src/app/admin/frontend/config/form-fields.edn")
(def ^:private table-columns-path "src/app/admin/frontend/config/table-columns.edn")

(def ^:private admin-scope "admin")
(def ^:private user-scope "user")

(defn- read-domain-admin-configs
  "Read and merge all domain admin config files of a given type.
   config-key is one of :view-options, :form-fields, :table-columns."
  [config-key]
  (let [domain-admin-config-paths (domain-registry/get-admin-ui-config-paths)]
    (reduce
      (fn [acc domain-paths]
        (merge acc
          (frontend-config-io/read-edn-or-empty
            (get domain-paths config-key)
            {:log-message "Failed to read domain admin config EDN"
             :log-context {:scope :admin-settings
                           :config config-key}})))
      {}
      domain-admin-config-paths)))

;; User-facing (domain-owned) UI config defaults come from the primary domain.
(defn- get-user-config-paths []
  (domain-registry/primary-user-ui-config-paths))

(defn- user-entities-path []
  (get (get-user-config-paths) :entities))

(defn- user-view-options-path []
  (get (get-user-config-paths) :view-options))

(defn- user-form-fields-path []
  (get (get-user-config-paths) :form-fields))

(defn- user-table-columns-path []
  (get (get-user-config-paths) :table-columns))

(defn- validate-or-throw!
  [label validation extra-data]
  (when-not (:valid? validation)
    (log/error (str "Attempted to write invalid " label " data")
      (merge extra-data (dissoc validation :valid?)))
    (throw (ex-info (str "Invalid " label " data")
             (merge {:status 400}
               extra-data
               (dissoc validation :valid?))))))

(defn- read-runtime-override
  "Read a runtime override blob from the database.

   Returns an EDN map or {} when no override exists."
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
  "Persist a runtime override blob in the database via upsert."
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

(defn- read-default-config-or-throw
  [path config-key scope label validate-fn]
  (try
    (let [data (frontend-config-io/read-edn-or-throw path
                 {:log-message (str "Failed to read " label " EDN")
                  :log-context {:scope scope
                                :config config-key}})
          validation (validate-fn data)]
      (when-not (:valid? validation)
        (log/warn (str label " validation issues")
          {:scope scope
           :config config-key
           :errors (:errors validation)
           :warnings (:warnings validation)}))
      data)
    (catch Exception e
      (log/error e (str "Failed to read " label)
        {:scope scope :config config-key :path path})
      (throw (ex-info (str "Failed to read " label)
               {:status 500
                :scope scope
                :config config-key})))))

(defn- merge-and-validate
  [label validate-fn & maps]
  (let [merged (apply merge maps)
        validation (validate-fn merged)]
    (when-not (:valid? validation)
      (log/warn (str label " validation issues")
        {:errors (:errors validation)
         :warnings (:warnings validation)}))
    merged))

(defn read-view-options
  "Read admin view-options defaults and merge persisted runtime overrides."
  [db]
  (let [admin-data (read-default-config-or-throw
                     view-options-path
                     :view-options
                     :admin-settings
                     "admin view-options"
                     view-options-spec/validate-view-options-strict)
        domain-data (read-domain-admin-configs :view-options)
        runtime-override (read-runtime-override db admin-scope :view-options)]
    (merge-and-validate
      "merged view-options"
      view-options-spec/validate-view-options-strict
      domain-data
      admin-data
      runtime-override)))

(defn write-view-options!
  "Persist admin view-options runtime overrides in the database."
  [db view-options]
  (let [validation (view-options-spec/validate-view-options-strict view-options)]
    (validate-or-throw! "view-options" validation
      {:errors (:errors validation)
       :nested-locks-errors (:nested-locks-errors validation)}))
  (write-runtime-override! db admin-scope :view-options view-options))

(defn read-form-fields
  "Read admin form-fields defaults and merge persisted runtime overrides."
  [db]
  (let [admin-data (read-default-config-or-throw
                     form-fields-path
                     :form-fields
                     :admin-settings
                     "admin form-fields"
                     form-fields-spec/validate-form-fields-strict)
        domain-data (read-domain-admin-configs :form-fields)
        runtime-override (read-runtime-override db admin-scope :form-fields)]
    (merge-and-validate
      "merged form-fields"
      form-fields-spec/validate-form-fields-strict
      domain-data
      admin-data
      runtime-override)))

(defn write-form-fields!
  "Persist admin form-fields runtime overrides in the database."
  [db form-fields]
  (let [validation (form-fields-spec/validate-form-fields-strict form-fields)]
    (validate-or-throw! "form-fields" validation
      {:errors (:errors validation)
       :warnings (:warnings validation)}))
  (write-runtime-override! db admin-scope :form-fields form-fields))

(defn read-table-columns
  "Read admin table-columns defaults and merge persisted runtime overrides."
  [db]
  (let [admin-data (read-default-config-or-throw
                     table-columns-path
                     :table-columns
                     :admin-settings
                     "admin table-columns"
                     table-columns-spec/validate-table-columns-strict)
        domain-data (read-domain-admin-configs :table-columns)
        runtime-override (read-runtime-override db admin-scope :table-columns)]
    (merge-and-validate
      "merged table-columns"
      table-columns-spec/validate-table-columns-strict
      domain-data
      admin-data
      runtime-override)))

(defn write-table-columns!
  "Persist admin table-columns runtime overrides in the database."
  [db table-columns]
  (let [validation (table-columns-spec/validate-table-columns-strict table-columns)]
    (validate-or-throw! "table-columns" validation
      {:errors (:errors validation)
       :warnings (:warnings validation)}))
  (write-runtime-override! db admin-scope :table-columns table-columns))

(defn read-user-entities
  "Read user-facing entities defaults and merge persisted runtime overrides."
  [db]
  (let [defaults (read-default-config-or-throw
                   (user-entities-path)
                   :entities
                   :user-ui-config
                   "user entities"
                   entities-spec/validate-user-entities)
        runtime-override (read-runtime-override db user-scope :entities)]
    (merge-and-validate
      "merged user entities"
      entities-spec/validate-user-entities
      defaults
      runtime-override)))

(defn write-user-entities!
  [db entities]
  (let [validation (entities-spec/validate-user-entities entities)]
    (validate-or-throw! "user entities" validation
      {:errors (:errors validation)}))
  (write-runtime-override! db user-scope :entities entities))

(defn read-user-view-options
  "Read user-facing view-options defaults and merge persisted runtime overrides."
  [db]
  (let [defaults (read-default-config-or-throw
                   (user-view-options-path)
                   :view-options
                   :user-ui-config
                   "user view-options"
                   view-options-spec/validate-view-options-strict)
        runtime-override (read-runtime-override db user-scope :view-options)]
    (merge-and-validate
      "merged user view-options"
      view-options-spec/validate-view-options-strict
      defaults
      runtime-override)))

(defn write-user-view-options!
  [db view-options]
  (let [validation (view-options-spec/validate-view-options-strict view-options)]
    (validate-or-throw! "user view-options" validation
      {:errors (:errors validation)
       :nested-locks-errors (:nested-locks-errors validation)}))
  (write-runtime-override! db user-scope :view-options view-options))

(defn read-user-form-fields
  "Read user-facing form-fields defaults and merge persisted runtime overrides."
  [db]
  (let [defaults (read-default-config-or-throw
                   (user-form-fields-path)
                   :form-fields
                   :user-ui-config
                   "user form-fields"
                   form-fields-spec/validate-form-fields-strict)
        runtime-override (read-runtime-override db user-scope :form-fields)]
    (merge-and-validate
      "merged user form-fields"
      form-fields-spec/validate-form-fields-strict
      defaults
      runtime-override)))

(defn write-user-form-fields!
  [db form-fields]
  (let [validation (form-fields-spec/validate-form-fields-strict form-fields)]
    (validate-or-throw! "user form-fields" validation
      {:errors (:errors validation)
       :warnings (:warnings validation)}))
  (write-runtime-override! db user-scope :form-fields form-fields))

(defn read-user-table-columns
  "Read user-facing table-columns defaults and merge persisted runtime overrides."
  [db]
  (let [defaults (read-default-config-or-throw
                   (user-table-columns-path)
                   :table-columns
                   :user-ui-config
                   "user table-columns"
                   table-columns-spec/validate-table-columns-strict)
        runtime-override (read-runtime-override db user-scope :table-columns)]
    (merge-and-validate
      "merged user table-columns"
      table-columns-spec/validate-table-columns-strict
      defaults
      runtime-override)))

(defn write-user-table-columns!
  [db table-columns]
  (let [validation (table-columns-spec/validate-table-columns-strict table-columns)]
    (validate-or-throw! "user table-columns" validation
      {:errors (:errors validation)
       :warnings (:warnings validation)}))
  (write-runtime-override! db user-scope :table-columns table-columns))
