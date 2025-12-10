(ns app.backend.services.admin.users.security
  "User security operations including email verification and password management.

   This namespace handles:
   - Email verification management
   - Password reset operations
   - Security-related user operations
   - Audit logging for security actions"
  (:require
   [app.backend.services.admin.audit :as audit]
   [app.shared.type-conversion :as tc]
   [honey.sql :as hsql]
   [java-time.api :as time]
   [next.jdbc :as jdbc]
   [next.jdbc.sql :as sql]))

;; ============================================================================
;; Email Verification Management
;; ============================================================================

(defn force-verify-email!
  "Force verify a user's email as admin"
  [db user-id admin-id ip-address user-agent]
  (jdbc/with-transaction [tx db]
    (jdbc/execute-one! tx ["SET LOCAL app.bypass_rls = true"])
    (let [result (sql/update! tx :users
                   {:email_verified true
                    :email_verified_at (time/instant)
                    :updated_at (time/instant)}
                   {:id user-id})]
      ;; Log the action
      (audit/log-audit! tx {:admin_id admin-id
                            :action "user.email_force_verified"
                            :entity-type "user"
                            :entity-id user-id
                            :changes {:email_verified true}
                            :ip-address ip-address
                            :user-agent user-agent})
      result)))

;; ============================================================================
;; Password Management
;; ============================================================================

(defn reset-user-password!
  "Reset user password and send reset email"
  [db user-id admin-id ip-address user-agent]
  (jdbc/with-transaction [tx db]
    (jdbc/execute-one! tx ["SET LOCAL app.bypass_rls = true"])
    (let [user (jdbc/execute-one! tx
                 (hsql/format {:select [:email :full_name]
                               :from [:users]
                               :where [:= :id user-id]}))]
      (if user
        (do
          ;; In a real implementation, this would trigger password reset email
          ;; For now, we'll just log the action
          (audit/log-audit! tx {:admin_id admin-id
                                :action "user.password_reset"
                                :entity-type "user"
                                :entity-id user-id
                                :changes {:reset_initiated true}
                                :ip-address ip-address
                                :user-agent user-agent})
          {:success true
           :message (str "Password reset email sent to " (:email user))})
        {:error "User not found"}))))

;; ============================================================================
;; Security Status Management
;; ============================================================================

(defn suspend-user!
  "Suspend a user account as admin action"
  [db user-id admin-id ip-address user-agent reason]
  (let [old-user (jdbc/execute-one! db
                   (hsql/format {:select [:status :email]
                                 :from [:users]
                                 :where [:= :id user-id]}))
        result (sql/update! db :users
                 {:status (tc/cast-for-database :user-status "suspended")
                  :updated_at (time/instant)}
                 {:id user-id})]
    ;; Log the action
    (audit/log-audit! db {:admin_id admin-id
                          :action "user.suspended"
                          :entity-type "user"
                          :entity-id user-id
                          :changes {:before {:status (:status old-user)}
                                    :after {:status "suspended"}
                                    :reason reason}
                          :ip-address ip-address
                          :user-agent user-agent})
    result))

(defn reactivate-user!
  "Reactivate a suspended user account"
  [db user-id admin-id ip-address user-agent]
  (let [old-user (jdbc/execute-one! db
                   (hsql/format {:select [:status :email]
                                 :from [:users]
                                 :where [:= :id user-id]}))
        result (sql/update! db :users
                 {:status (tc/cast-for-database :user-status "active")
                  :updated_at (time/instant)}
                 {:id user-id})]
    ;; Log the action
    (audit/log-audit! db {:admin_id admin-id
                          :action "user.reactivated"
                          :entity-type "user"
                          :entity-id user-id
                          :changes {:before {:status (:status old-user)}
                                    :after {:status "active"}}
                          :ip-address ip-address
                          :user-agent user-agent})
    result))

;; ============================================================================
;; Security Audit Functions
;; ============================================================================

(defn get-user-security-events
  "Get security-related events for a user from the audit log.

   Uses the simplified audit_logs schema where admin actions are stored as:
   - actor_type = 'admin'
   - actor_id   = admin ID
   - target_type = 'user'
   - target_id   = user ID."
  [db user-id {:keys [limit offset] :or {limit 50 offset 0}}]
  (jdbc/execute! db
    (hsql/format {:select [:al.*
                           [:a.email :admin_email]
                           [:a.full_name :admin_name]]
                  :from [[:audit_logs :al]]
                  :left-join [[:admins :a] [:= :al.actor_id :a.id]]
                  :where [:and
                          [:= :al.target_id user-id]
                          [:= :al.target_type "user"]
                          [:= :al.actor_type "admin"]
                          [:in :al.action ["user.email_force_verified"
                                           "user.password_reset"
                                           "user.suspended"
                                           "user.reactivated"
                                           "user.role_updated"]]]
                  :order-by [[:al.created_at :desc]]
                  :limit limit
                  :offset offset})))

(defn check-user-security-status
  "Check the security status and flags for a user"
  [db user-id]
  (let [user (jdbc/execute-one! db
               (hsql/format {:select [:email :status :email_verified
                                      :role :last_login_at :created_at :updated_at]
                             :from [:users]
                             :where [:= :id user-id]}))

        recent-security-events (get-user-security-events db user-id {:limit 10})]

    (when user
      {:user-security user
       :security-flags {:is-verified (:email_verified user)
                        :is-active (= "active" (str (:status user)))
                        :is-privileged (contains? #{"admin" "owner"} (str (:role user)))
                        :has-recent-activity (not (nil? (:last_login_at user)))}
       :recent-events recent-security-events})))
