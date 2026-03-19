(ns app.domain.backend.expenses.services.tenant-settings
  "Service for per-tenant settings (one row per tenant).

   Manages: email notifications toggle.
   Provisioned automatically when a new tenant is created."
  (:require
    [app.shared.adapters.database :as db-adapter]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.util UUID]))

(defn get-tenant-settings
  "Return the tenant settings row for `tenant-id`, or nil if not provisioned."
  [db tenant-id]
  (when-not (instance? UUID tenant-id)
    (throw (ex-info "tenant-id must be a UUID" {:tenant-id tenant-id})))
  (-> (jdbc/execute-one!
        db
        (sql/format {:select [:*]
                     :from [:tenant_settings]
                     :where [:= :tenant_id tenant-id]})
        {:builder-fn rs/as-unqualified-lower-maps})
    db-adapter/to-app))

(defn update-tenant-settings!
  "Partial update of tenant settings. Only provided keys are changed.

   Accepted keys:
   - :email-notifications (boolean)"
  [db tenant-id updates]
  (when-not (instance? UUID tenant-id)
    (throw (ex-info "tenant-id must be a UUID" {:tenant-id tenant-id})))
  (let [allowed-keys #{:email-notifications}
        filtered (select-keys updates allowed-keys)]
    (when (empty? filtered)
      (throw (ex-info "No valid update keys provided" {:updates updates})))
    (when (and (contains? filtered :email-notifications)
            (not (boolean? (:email-notifications filtered))))
      (throw (ex-info "email-notifications must be boolean"
               {:email-notifications (:email-notifications filtered)})))
    (let [db-updates (cond-> {:updated_at [:now]}
                       (contains? filtered :email-notifications)
                       (assoc :email_notifications (:email-notifications filtered)))]
      (-> (jdbc/execute-one!
            db
            (sql/format {:update :tenant_settings
                         :set db-updates
                         :where [:= :tenant_id tenant-id]
                         :returning [:*]})
            {:builder-fn rs/as-unqualified-lower-maps})
        db-adapter/to-app))))

(defn provision-tenant-settings!
  "Create default tenant settings for a new tenant.
   No-op if settings already exist (idempotent)."
  [db tenant-id]
  (when-not (instance? UUID tenant-id)
    (throw (ex-info "tenant-id must be a UUID" {:tenant-id tenant-id})))
  (-> (jdbc/execute-one!
        db
        [(str "INSERT INTO tenant_settings (id, tenant_id, email_notifications) "
           "VALUES (?, ?, true) "
           "ON CONFLICT (tenant_id) DO NOTHING "
           "RETURNING *")
         (UUID/randomUUID)
         tenant-id]
        {:builder-fn rs/as-unqualified-lower-maps})
    db-adapter/to-app))

(comment
  ;; (require '[app.domain.backend.expenses.services.tenant-settings :as ts] :reload)
  ;; (ts/get-tenant-settings db tenant-id)
  ;; (ts/update-tenant-settings! db tenant-id {:email-notifications false})
  :rcf)
