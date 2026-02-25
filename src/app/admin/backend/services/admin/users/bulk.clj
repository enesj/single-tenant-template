(ns app.admin.backend.services.admin.users.bulk
  "Bulk user ops; keep batch validation and side effects centralized."
  (:require
    [app.admin.backend.services.admin.audit :as audit]
    [app.admin.backend.services.admin.users.validation :as validation]
    [app.shared.data :as shared-data]
    [app.shared.adapters.database :as shared-db]
    [app.shared.adapters.normalization :as norm]
    [app.template.backend.utils.adapters.persistence :as persist]
    [app.shared.type-conversion :as tc]
    [clojure.string :as str]
    [honey.sql :as hsql]
    [java-time.api :as time]
    [next.jdbc :as jdbc]
    [taoensso.timbre :as log]))

;; ============================================================================
;; Bulk User Operations
;; ============================================================================

(defn- bulk-update-field!
  "Generic scaffold for bulk-updating a single user field.

  `field-key`    — DB column keyword (e.g. :status, :role)
  `cast-type`    — type keyword passed to tc/cast-for-database
  `audit-action` — string recorded in the audit log
  `msg-suffix`   — appended to the success message after the count"
  [db user-ids new-value admin-id ip-address user-agent
   field-key cast-type audit-action msg-suffix]
  (try
    (jdbc/with-transaction [tx db]
      (jdbc/execute-one! tx ["SET LOCAL app.bypass_rls = true"])
      (let [current-users (when (seq user-ids)
                            (jdbc/execute! tx
                              (hsql/format {:select [:id :email field-key]
                                            :from [:users]
                                            :where [:in :id user-ids]})))
            _result (when (seq user-ids)
                      (jdbc/execute! tx
                        (hsql/format {:update :users
                                      :set {field-key (tc/cast-for-database cast-type new-value)}
                                      :where [:in :id user-ids]})))]
        (doseq [user current-users]
          (audit/log-audit! tx {:admin_id admin-id
                                :action audit-action
                                :entity-type "user"
                                :entity-id (:id user)
                                :changes {:before {field-key (get user field-key)}
                                          :after {field-key new-value}}
                                :ip-address ip-address
                                :user-agent user-agent}))
        {:success true
         :updated_count (count current-users)
         :message (str "Updated " (count current-users) " users " msg-suffix)}))
    (catch Exception e
      (log/error e (str "Failed to bulk update user " (name field-key))
        {:user_ids user-ids field-key new-value})
      {:error (.getMessage e)})))

(defn bulk-update-user-status!
  "Bulk update user status for multiple users."
  [db user-ids new-status admin-id ip-address user-agent]
  (bulk-update-field! db user-ids new-status admin-id ip-address user-agent
    :status :user-status "user.bulk_status_updated" (str "to " new-status)))

(defn bulk-update-user-role!
  "Bulk update user role for multiple users."
  [db user-ids new-role admin-id ip-address user-agent]
  (bulk-update-field! db user-ids new-role admin-id ip-address user-agent
    :role :user-role "user.bulk_role_updated" (str "to role " new-role)))

;; ============================================================================
;; Data Export
;; ============================================================================

(def ^:private export-user-config validation/user-normalization-config)

(defn- export-users-query
  [user-ids]
  (cond-> {:select [:u.id :u.email :u.full_name :u.role :u.status
                    :u.email_verified :u.auth_provider :u.created_at :u.last_login_at
                    [:t.name :tenant_name] [:t.slug :tenant_slug]]
           :from [[:users :u]]
           :join [[:tenants :t] [:= :u.tenant_id :t.id]]
           :order-by [[:u.created_at :desc]]}
    (seq user-ids) (assoc :where [:in :u.id user-ids])
    (not (seq user-ids)) (assoc :limit 10000)))

(defn export-users-csv
  "Export users data as CSV"
  [db user-ids]
  (try
    (let [users (persist/execute-admin-query
                  db
                  (export-users-query user-ids)
                  (fn [raw]
                    (-> raw
                      shared-db/convert-pg-objects
                      (norm/normalize-admin-result export-user-config))))

          ;; Convert to CSV format
          csv-headers "ID,Email,Full Name,Role,Status,Email Verified,Auth Provider,Created At,Last Login,Tenant Name,Tenant Slug"
          csv-rows (map (fn [user]
                          (str (:id user) ","
                            (or (:email user) "") ","
                            (or (:full-name user) "") ","
                            (or (:role user) "") ","
                            (or (:status user) "") ","
                            (if (:email-verified user) "Yes" "No") ","
                            (or (:auth-provider user) "") ","
                            (or (:created-at user) "") ","
                            (or (:last-login-at user) "") ","
                            (or (:tenant-name user) "") ","
                            (or (:tenant-slug user) ""))) users)
          csv-content (str csv-headers "\n" (str/join "\n" csv-rows))]

      {:success true
       :content csv-content
       :filename (str "users-export-" (.format (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd-HHmm")
                                        (java.time.LocalDateTime/now)) ".csv")})
    (catch Exception e
      (log/error e "Failed to export users" {:user_ids user-ids})
      {:error (.getMessage e)})))

;; ============================================================================
;; User Impersonation
;; ============================================================================

(defn- user-row->safe-session-user
  "Convert a DB user row into the shape we store in `:session/:auth-session`.

  Notes:
  - We *must* use unqualified keys (e.g. :id, :email) because downstream
    middleware/access helpers look up `[:session :auth-session :user :id]`.
  - Never include secrets/sensitive values (e.g. password hashes) in the
    session cookie.
  - Session data is cookie-backed in this template, so keep it small."
  [user-row]
  (let [m (shared-db/convert-pg-objects user-row)
        ;; Drop any table namespaces (e.g. :users/id -> :id)
        plain (into {} (map (fn [[k v]] [(keyword (name k)) v])) m)]
    (-> plain
      ;; Strip sensitive fields (support a few naming variants defensively)
      (dissoc :password_hash :password-hash)
      ;; Ensure cookie-safe serialization
      shared-data/sanitize-for-serialization)))

(defn create-user-impersonation-session!
  "Create an impersonation session for admin to access user account"
  [db user-id admin-id ip-address user-agent]
  (try
    (jdbc/with-transaction [tx db]
      (jdbc/execute-one! tx ["SET LOCAL app.bypass_rls = true"])
      (let [user (jdbc/execute-one! tx
                   (hsql/format {:select [:*]
                                 :from [:users]
                                 :where [:and [:= :id user-id]
                                         [:= :status (tc/cast-for-database :user-status "active")]]}))
            admin (jdbc/execute-one! tx
                    (hsql/format {:select [:*]
                                  :from [:admins]
                                  :where [:= :id admin-id]}))
            now (time/instant)]

        (if (and user admin)
          (let [session-user (user-row->safe-session-user user)
                auth-session {:user session-user
                              :provider "impersonation"
                              ;; Useful for debugging and auditing UI behavior.
                              :impersonated-by (str admin-id)
                              :impersonated-at (str now)}]

            ;; Log the impersonation
            (audit/log-audit! tx {:admin_id admin-id
                                  :action "user.impersonated"
                                  :entity-type "user"
                                  :entity-id user-id
                                  :changes {:impersonation true}
                                  :ip-address ip-address
                                  :user-agent user-agent})

            {:success true
             ;; Returned so the route handler can set the Ring session.
             :auth-session auth-session
             :redirect-url "/dashboard"})

          (do
            (when (nil? user)
              (log/warn "Impersonation failed: user not found or not active"
                {:user-id user-id :admin-id admin-id}))
            (when (nil? admin)
              (log/warn "Impersonation failed: admin not found"
                {:user-id user-id :admin-id admin-id}))
            {:error "User or admin not found, or user not active"}))))
    (catch Exception e
      (log/error e "Failed to create impersonation session" {:user_id user-id :admin_id admin-id})
      {:error (.getMessage e)})))
