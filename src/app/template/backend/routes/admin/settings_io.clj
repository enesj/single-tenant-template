(ns app.template.backend.routes.admin.settings-io
  "Admin settings I/O backed by DB-persisted runtime overrides.

   Source-controlled EDN files remain the defaults for both admin-owned and
   domain-owned UI config. Runtime saves are stored in the database and merged
   over those defaults on read so deployed environments do not depend on a
   writable application filesystem."
  (:require
    [app.domain.backend.registry :as domain-registry]
    [app.shared.frontend-config.io :as frontend-config-io]
    [app.shared.frontend-config.template-user :as template-user]
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

;; User-facing UI config defaults come from the template-user bundle plus the
;; primary enabled domain. Template defaults load first; domain defaults win.
(defn- get-user-config-paths []
  (into []
    (remove nil?)
    [template-user/paths
     (domain-registry/primary-user-ui-config-paths)]))

(defn- user-config-paths-of
  [config-key]
  (->> (get-user-config-paths)
    (keep #(get % config-key))
    vec))

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

(defn- deep-merge
  [& ms]
  (letfn [(merge-entry [a b]
            (merge-with
              (fn [x y]
                (if (and (map? x) (map? y))
                  (merge-entry x y)
                  y))
              (or a {})
              (or b {})))]
    (reduce merge-entry {} ms)))

(defn- merge-and-validate
  [label validate-fn & maps]
  (let [merged (apply deep-merge maps)
        validation (validate-fn merged)]
    (when-not (:valid? validation)
      (log/warn (str label " validation issues")
        {:errors (:errors validation)
         :warnings (:warnings validation)}))
    merged))

(defn- overlay-runtime-snapshot-and-validate
  "Overlay a persisted runtime snapshot over merged defaults.

   Runtime config rows are written from full-editor payloads, so each top-level
   entity key should replace the default entity map wholesale. That preserves
   explicit removals of nested keys (for example clearing a :display-locks entry
   that exists in source-controlled defaults) across reloads."
  [label validate-fn defaults runtime-snapshot]
  (let [merged (merge (or defaults {}) (or runtime-snapshot {}))
        validation (validate-fn merged)]
    (when-not (:valid? validation)
      (log/warn (str label " validation issues")
        {:errors (:errors validation)
         :warnings (:warnings validation)}))
    merged))

(defn- read-user-default-config
  [config-key label validate-fn]
  (apply merge-and-validate
    (concat
      [(str "merged " label) validate-fn]
      (map (fn [path]
             (read-default-config-or-throw path config-key :user-ui-config label validate-fn))
        (user-config-paths-of config-key)))))

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

(def ^:private retired-admin-table-columns
  {:article-aliases #{"confidence"}
   :users #{"avatar-url"}})

(defn- remove-retired-admin-columns
  [entity-config retired-columns]
  (let [retired-columns (set retired-columns)
        retired-keys (into #{} (keep #(when (string? %) (keyword %))) retired-columns)
        prune-columns (fn [columns]
                        (->> (or columns [])
                          (remove retired-columns)
                          vec))
        prune-map (fn [m]
                    (reduce dissoc (or m {}) retired-keys))]
    (cond-> (or entity-config {})
      true (update :available-columns prune-columns)
      true (update :default-visible-columns prune-columns)
      true (update :filterable-columns prune-columns)
      true (update :sortable-columns prune-columns)
      true (update :always-visible prune-columns)
      true (update :computed-fields prune-map)
      true (update :column-config prune-map)
      true (update :column-metadata prune-map))))

(defn- retire-admin-table-columns
  [table-columns]
  (reduce-kv
    (fn [acc entity-key retired-columns]
      (if (contains? acc entity-key)
        (update acc entity-key remove-retired-admin-columns retired-columns)
        acc))
    (or table-columns {})
    retired-admin-table-columns))

(defn read-view-options
  "Read admin view-options defaults and overlay persisted runtime snapshots."
  [db]
  (let [defaults (merge-and-validate
                   "merged default view-options"
                   view-options-spec/validate-view-options-strict
                   (read-domain-admin-configs :view-options)
                   (read-default-config-or-throw
                     view-options-path
                     :view-options
                     :admin-settings
                     "admin view-options"
                     view-options-spec/validate-view-options-strict))
        runtime-override (read-runtime-override db admin-scope :view-options)]
    (overlay-runtime-snapshot-and-validate
      "merged view-options"
      view-options-spec/validate-view-options-strict
      defaults
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
  "Read admin form-fields defaults and overlay persisted runtime snapshots."
  [db]
  (let [defaults (merge-and-validate
                   "merged default form-fields"
                   form-fields-spec/validate-form-fields-strict
                   (read-domain-admin-configs :form-fields)
                   (read-default-config-or-throw
                     form-fields-path
                     :form-fields
                     :admin-settings
                     "admin form-fields"
                     form-fields-spec/validate-form-fields-strict))
        runtime-override (read-runtime-override db admin-scope :form-fields)]
    (overlay-runtime-snapshot-and-validate
      "merged form-fields"
      form-fields-spec/validate-form-fields-strict
      defaults
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
  "Read admin table-columns defaults and overlay persisted runtime snapshots."
  [db]
  (let [defaults (merge-and-validate
                   "merged default table-columns"
                   table-columns-spec/validate-table-columns-strict
                   (read-domain-admin-configs :table-columns)
                   (read-default-config-or-throw
                     table-columns-path
                     :table-columns
                     :admin-settings
                     "admin table-columns"
                     table-columns-spec/validate-table-columns-strict))
        runtime-override (some-> (read-runtime-override db admin-scope :table-columns)
                           promote-legacy-admin-table-columns-override)
        merged (overlay-runtime-snapshot-and-validate
                 "merged table-columns"
                 table-columns-spec/validate-table-columns-strict
                 defaults
                 runtime-override)]
    (retire-admin-table-columns merged)))

(defn write-table-columns!
  "Persist admin table-columns runtime overrides in the database."
  [db table-columns]
  (let [sanitized-table-columns (retire-admin-table-columns table-columns)
        validation (table-columns-spec/validate-table-columns-strict sanitized-table-columns)]
    (validate-or-throw! "table-columns" validation
      {:errors (:errors validation)
       :warnings (:warnings validation)})
    (write-runtime-override! db admin-scope :table-columns sanitized-table-columns)))

(defn read-user-entities
  "Read user-facing entities defaults and overlay persisted runtime snapshots."
  [db]
  (let [defaults (read-user-default-config
                   :entities
                   "user entities"
                   entities-spec/validate-user-entities)
        runtime-override (read-runtime-override db user-scope :entities)]
    (overlay-runtime-snapshot-and-validate
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
  "Read user-facing view-options defaults and overlay persisted runtime snapshots."
  [db]
  (let [defaults (read-user-default-config
                   :view-options
                   "user view-options"
                   view-options-spec/validate-view-options-strict)
        runtime-override (read-runtime-override db user-scope :view-options)]
    (overlay-runtime-snapshot-and-validate
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
  "Read user-facing form-fields defaults and overlay persisted runtime snapshots."
  [db]
  (let [defaults (read-user-default-config
                   :form-fields
                   "user form-fields"
                   form-fields-spec/validate-form-fields-strict)
        runtime-override (read-runtime-override db user-scope :form-fields)]
    (overlay-runtime-snapshot-and-validate
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
  "Read user-facing table-columns defaults and overlay persisted runtime snapshots."
  [db]
  (let [defaults (read-user-default-config
                   :table-columns
                   "user table-columns"
                   table-columns-spec/validate-table-columns-strict)
        runtime-override (read-runtime-override db user-scope :table-columns)]
    (overlay-runtime-snapshot-and-validate
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
