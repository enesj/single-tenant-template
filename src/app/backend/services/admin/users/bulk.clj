(ns app.backend.services.admin.users.bulk
  "Admin bulk user operations and data export.

   This namespace handles:
   - Bulk status updates for multiple users
   - Bulk role updates for multiple users
   - CSV export functionality for user data
   - User impersonation capabilities for admin access"
  (:require
    [app.backend.services.admin.audit :as audit]
    [app.backend.services.admin.auth :as auth]
    [app.shared.adapters.database :as db-adapter]
    [app.shared.type-conversion :as tc]
    [clojure.string :as str]
    [honey.sql :as hsql]
    [java-time.api :as time]
    [next.jdbc :as jdbc]
    [taoensso.timbre :as log])
  (:import
    [java.util UUID]))

;; ============================================================================
;; Bulk User Operations
;; ============================================================================

(defn bulk-update-user-status!
  "Bulk update user status for multiple users"
  [db user-ids new-status admin-id ip-address user-agent]
  (try
    (jdbc/with-transaction [tx db]
      (jdbc/execute-one! tx ["SET LOCAL app.bypass_rls = true"])
      (let [;; Get current users for audit log
            current-users (when (seq user-ids)
                            (jdbc/execute! tx
                              (hsql/format {:select [:id :email :status]
                                            :from [:users]
                                            :where [:in :id user-ids]})))

            ;; Update all users
            _result (when (seq user-ids)
                      (jdbc/execute! tx
                        (hsql/format {:update :users
                                      :set {:status (tc/cast-for-database :user-status new-status)
                                            :updated_at (time/instant)}
                                      :where [:in :id user-ids]})))]

        ;; Log audit entries for each user
        (doseq [user current-users]
          (audit/log-audit! tx {:admin_id admin-id
                                :action "user.bulk_status_updated"
                                :entity-type "user"
                                :entity-id (:id user)
                                :changes {:before {:status (:status user)}
                                          :after {:status new-status}}
                                :ip-address ip-address
                                :user-agent user-agent}))

        {:success true
         :updated_count (count current-users)
         :message (str "Updated " (count current-users) " users to " new-status)}))
    (catch Exception e
      (log/error e "Failed to bulk update user status" {:user_ids user-ids :status new-status})
      {:error (.getMessage e)})))

(defn bulk-update-user-role!
  "Bulk update user role for multiple users"
  [db user-ids new-role admin-id ip-address user-agent]
  (try
    (jdbc/with-transaction [tx db]
      (jdbc/execute-one! tx ["SET LOCAL app.bypass_rls = true"])
      (let [;; Get current users for audit log
            current-users (when (seq user-ids)
                            (jdbc/execute! tx
                              (hsql/format {:select [:id :email :role]
                                            :from [:users]
                                            :where [:in :id user-ids]})))

            ;; Update all users
            _result (when (seq user-ids)
                      (jdbc/execute! tx
                        (hsql/format {:update :users
                                      :set {:role (tc/cast-for-database :user-role new-role)
                                            :updated_at (time/instant)}
                                      :where [:in :id user-ids]})))]

        ;; Log audit entries for each user
        (doseq [user current-users]
          (audit/log-audit! tx {:admin_id admin-id
                                :action "user.bulk_role_updated"
                                :entity-type "user"
                                :entity-id (:id user)
                                :changes {:before {:role (:role user)}
                                          :after {:role new-role}}
                                :ip-address ip-address
                                :user-agent user-agent}))

        {:success true
         :updated_count (count current-users)
         :message (str "Updated " (count current-users) " users to role " new-role)}))
    (catch Exception e
      (log/error e "Failed to bulk update user role" {:user_ids user-ids :role new-role})
      {:error (.getMessage e)})))

;; ============================================================================
;; Data Export
;; ============================================================================

(defn export-users-csv
  "Export users data as CSV"
  [db user-ids]
  (try
    (let [users (if (seq user-ids)
                  (jdbc/execute! db
                    (hsql/format {:select [:u.id :u.email :u.full_name :u.role :u.status
                                           :u.email_verified :u.auth_provider :u.created_at :u.last_login_at
                                           [:t.name :tenant_name] [:t.slug :tenant_slug]]
                                  :from [[:users :u]]
                                  :join [[:tenants :t] [:= :u.tenant_id :t.id]]
                                  :where [:in :u.id user-ids]
                                  :order-by [[:u.created_at :desc]]}))
                  (jdbc/execute! db
                    (hsql/format {:select [:u.id :u.email :u.full_name :u.role :u.status
                                           :u.email_verified :u.auth_provider :u.created_at :u.last_login_at
                                           [:t.name :tenant_name] [:t.slug :tenant_slug]]
                                  :from [[:users :u]]
                                  :join [[:tenants :t] [:= :u.tenant_id :t.id]]
                                  :order-by [[:u.created_at :desc]]
                                  :limit 10000}))) ;; Reasonable limit

          ;; Convert to CSV format
          csv-headers "ID,Email,Full Name,Role,Status,Email Verified,Auth Provider,Created At,Last Login,Tenant Name,Tenant Slug"
          csv-rows (map (fn [user]
                          (str (:id user) ","
                            (or (:email user) "") ","
                            (or (:full_name user) "") ","
                            (or (:role user) "") ","
                            (or (:status user) "") ","
                            (if (:email_verified user) "Yes" "No") ","
                            (or (:auth_provider user) "") ","
                            (or (:created_at user) "") ","
                            (or (:last_login_at user) "") ","
                            (or (:tenant_name user) "") ","
                            (or (:tenant_slug user) ""))) users)
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
            normalized-user (when user
                              (db-adapter/normalize-admin-result
                                user {:prefixes ["users-" "user-"]
                                      :namespaces #{"users"}
                                      :id-fields #{}}))
            tenant-id (or (:tenant-id normalized-user)
                          ;; Fallbacks for unexpected shapes (pre-normalization)
                        (:tenant_id user)
                        (:tenant-id user)
                        (:users/tenant_id user)
                        (:users/tenant-id user))]

        (if (and user admin tenant-id)
          (let [;; Create impersonation session
                session-token (auth/generate-session-token)
                session-id (UUID/randomUUID)
                now (time/instant)
                expires-at (time/plus now (time/hours 2)) ;; Shorter session for impersonation

                ;; Store impersonation session
                _ (jdbc/execute-one! tx
                    (hsql/format {:insert-into :user_sessions
                                  :values [{:id session-id
                                            :tenant_id tenant-id
                                            :user_id user-id
                                            :token session-token
                                            :ip_address (when ip-address [:cast ip-address :inet])
                                            :user_agent user-agent
                                            :impersonated_by admin-id
                                            :expires_at expires-at
                                            :created_at now}]}))]

            ;; Log the impersonation
            (audit/log-audit! tx {:admin_id admin-id
                                  :action "user.impersonated"
                                  :entity-type "user"
                                  :entity-id user-id
                                  :changes {:impersonation_session session-id}
                                  :ip-address ip-address
                                  :user-agent user-agent})

            {:success true
             :session_token session-token
             :user user
             :redirect_url "/app/dashboard"})
          (do
            (when (and user (nil? tenant-id))
              (log/error "Impersonation failed: resolved tenant_id is nil for user"
                {:user-id user-id :admin-id admin-id :user user}))
            {:error "User or admin not found, user not active, or missing tenant context"}))))
    (catch Exception e
      (log/error e "Failed to create impersonation session" {:user_id user-id :admin_id admin-id})
      {:error (.getMessage e)})))
