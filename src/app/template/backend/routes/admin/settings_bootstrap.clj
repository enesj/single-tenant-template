(ns app.template.backend.routes.admin.settings-bootstrap
  "Bootstrap runtime config rows for fresh environments.

   Reads source-controlled EDN defaults and seeds them into
   `frontend_runtime_configs` for channels that have no runtime row yet.
   Idempotent — existing rows are never overwritten."
  (:require
    [app.domain.backend.registry :as domain-registry]
    [app.shared.frontend-config.io :as frontend-config-io]
    [app.shared.frontend-config.template-user :as template-user]
    [app.shared.specs.entities :as entities-spec]
    [app.shared.specs.form-fields :as form-fields-spec]
    [app.shared.specs.table-columns :as table-columns-spec]
    [app.shared.specs.view-options :as view-options-spec]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]
    [taoensso.timbre :as log]))

;; ---------------------------------------------------------------------------
;; Admin-owned EDN paths (same as historical settings_io.clj)
;; ---------------------------------------------------------------------------

(def ^:private admin-view-options-path "src/app/admin/frontend/config/view-options.edn")
(def ^:private admin-form-fields-path "src/app/admin/frontend/config/form-fields.edn")
(def ^:private admin-table-columns-path "src/app/admin/frontend/config/table-columns.edn")

(def ^:private validators
  {"admin" {:view-options view-options-spec/validate-view-options-strict
            :form-fields form-fields-spec/validate-form-fields-strict
            :table-columns table-columns-spec/validate-table-columns-strict}
   "user" {:entities entities-spec/validate-user-entities
           :view-options view-options-spec/validate-view-options-strict
           :form-fields form-fields-spec/validate-form-fields-strict
           :table-columns table-columns-spec/validate-table-columns-strict}})

;; ---------------------------------------------------------------------------
;; Helpers
;; ---------------------------------------------------------------------------

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

(defn- read-edn-default!
  "Read a bootstrap EDN source. Invalid/unreadable/missing EDN aborts bootstrap.

   Nil paths still resolve to {} so optional domain bundles can remain absent
   without blocking startup."
  [scope config-key path]
  (if-not path
    {}
    (let [resource-candidates (->> [path
                                    (str/replace-first path #"^src/" "")
                                    (str/replace-first path #"^resources/" "")
                                    (str/replace-first path #"^config/" "")
                                    (str/replace-first path #"^vendor/" "")]
                                distinct)
          source-exists? (or (.exists (io/file path))
                           (some io/resource resource-candidates))]
      (when-not source-exists?
        (throw (ex-info "Bootstrap: missing EDN default"
                 {:scope scope
                  :config config-key
                  :path path})))
      (frontend-config-io/read-edn-or-throw
        path
        {:log-message "Bootstrap: failed to read EDN default"
         :log-context {:scope scope
                       :config config-key
                       :path path}}))))

(defn- validate-defaults!
  [scope config-key data]
  (when-let [validate-fn (get-in validators [scope config-key])]
    (let [validation (validate-fn data)]
      (when-not (:valid? validation)
        (log/error "Bootstrap: invalid frontend config defaults"
          {:scope scope
           :config-key config-key
           :errors (:errors validation)
           :warnings (:warnings validation)
           :nested-locks-errors (:nested-locks-errors validation)})
        (throw (ex-info "Bootstrap: invalid frontend config defaults"
                 {:scope scope
                  :config-key config-key
                  :errors (:errors validation)
                  :warnings (:warnings validation)
                  :nested-locks-errors (:nested-locks-errors validation)})))))
  data)

(defn- row-exists?
  "Check whether a runtime config row already exists for scope + config-key."
  [db scope config-key]
  (let [row (jdbc/execute-one!
              db
              (sql/format {:select [:%count.*]
                           :from [:frontend_runtime_configs]
                           :where [:and
                                   [:= :scope scope]
                                   [:= :config_key (name config-key)]]})
              {:builder-fn rs/as-unqualified-lower-maps})]
    (pos? (or (:count row) 0))))

(defn- seed-channel!
  "Insert a runtime config row when none exists. No-op when a row is present."
  [db scope config-key data]
  (if (row-exists? db scope config-key)
    (log/debug "Bootstrap: channel already seeded" {:scope scope :config-key config-key})
    (do
      (jdbc/execute-one!
        db
        [(str
           "INSERT INTO frontend_runtime_configs "
           "(id, scope, config_key, config_edn, created_at, updated_at) "
           "VALUES (gen_random_uuid(), ?, ?, ?, NOW(), NOW()) "
           "ON CONFLICT (scope, config_key) DO NOTHING")
         scope
         (name config-key)
         (pr-str data)])
      (log/info "Bootstrap: seeded runtime config" {:scope scope :config-key config-key}))))

;; ---------------------------------------------------------------------------
;; Default assembly
;; ---------------------------------------------------------------------------

(defn- admin-defaults
  "Merge domain + admin defaults for a given config-key."
  [config-key]
  (let [domain-paths (domain-registry/get-admin-ui-config-paths)
        domain-configs (map #(read-edn-default! "admin" config-key (get % config-key))
                         domain-paths)
        admin-path (case config-key
                     :view-options admin-view-options-path
                     :form-fields admin-form-fields-path
                     :table-columns admin-table-columns-path)
        admin-config (read-edn-default! "admin" config-key admin-path)
        merged (apply deep-merge (concat domain-configs [admin-config]))]
    (validate-defaults! "admin" config-key merged)))

(defn- user-config-paths-of
  "Collect user-facing config paths for a given config-key."
  [config-key]
  (let [all-path-maps (into []
                        (remove nil?)
                        [template-user/paths
                         (domain-registry/primary-user-ui-config-paths)])]
    (->> all-path-maps
      (keep #(get % config-key))
      vec)))

(defn- user-defaults
  "Merge template + domain user defaults for a given config-key."
  [config-key]
  (let [paths (user-config-paths-of config-key)
        merged (apply deep-merge (map #(read-edn-default! "user" config-key %) paths))]
    (validate-defaults! "user" config-key merged)))

;; ---------------------------------------------------------------------------
;; Public API
;; ---------------------------------------------------------------------------

(defn bootstrap-runtime-configs!
  "Ensure all mutable frontend config channels have runtime rows.

   Reads source-controlled EDN defaults and seeds them into the DB for any
   channel that does not yet have a row. Existing rows are never overwritten.

   Channels seeded:
     admin/view-options, admin/form-fields, admin/table-columns
     user/entities, user/view-options, user/form-fields, user/table-columns

   Safe to call on every startup — idempotent.
   Bootstrap fails closed: unreadable or invalid defaults abort startup rather
   than seeding an empty runtime snapshot."
  [db]
  (try
    (log/info "Bootstrap: checking runtime frontend config channels...")

    ;; Admin channels
    (seed-channel! db "admin" :view-options (admin-defaults :view-options))
    (seed-channel! db "admin" :form-fields (admin-defaults :form-fields))
    (seed-channel! db "admin" :table-columns (admin-defaults :table-columns))

    ;; User channels
    (seed-channel! db "user" :entities (user-defaults :entities))
    (seed-channel! db "user" :view-options (user-defaults :view-options))
    (seed-channel! db "user" :form-fields (user-defaults :form-fields))
    (seed-channel! db "user" :table-columns (user-defaults :table-columns))

    (log/info "Bootstrap: runtime frontend config check complete")
    :ok
    (catch Exception e
      (log/error e "Bootstrap: failed to seed runtime frontend configs")
      (throw (ex-info "Bootstrap: failed to seed runtime frontend configs" {} e)))))
