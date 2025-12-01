(ns app.backend.services.admin.users.deletion
  "User deletion operations with comprehensive validation and audit trails.

   This namespace handles:
   - Complex user deletion logic
   - Business constraint validation
   - Audit trail maintenance
   - Force deletion capabilities"
  (:require
    [app.backend.services.admin.audit :as audit]
    [app.backend.services.admin.users.validation :as validation]
    [honey.sql :as hsql]
    [java-time.api :as time]
    [next.jdbc :as jdbc]
    [taoensso.timbre :as log]))

;; ============================================================================
;; User Deletion
;; ============================================================================

(defn delete-user!
  "Delete user with comprehensive validation, audit logging, and error handling.
   Runs inside a transaction with RLS bypass enabled.

   Options:
   - force-delete: Skip some business logic constraints (default: false)
   - dry-run: Only validate and return impact without deletion (default: false)"
  [db user-id admin-id ip-address user-agent & {:keys [force-delete dry-run]
                                                :or {force-delete false dry-run false}}]

  (jdbc/with-transaction [tx db]
    ;; Enable RLS bypass for admin operations
    (jdbc/execute-one! tx ["SET LOCAL app.bypass_rls = true"])

    (try
      ;; Check constraints (this will throw if constraints fail)
      (let [constraint-check (if force-delete
                              ;; In force mode, skip some business logic constraints
                               (try
                                 (validation/check-user-deletion-constraints tx user-id)
                                 (catch clojure.lang.ExceptionInfo e
                                   (let [reason (-> e ex-data :reason)]
                                     (if (contains? #{:extensive-dependencies :active-privileged-user} reason)
                                      ;; Override these constraints in force mode
                                       (do
                                         (log/warn "Force deleting user despite constraint:" (ex-message e))
                                         {:user (jdbc/execute-one! tx
                                                  (hsql/format {:select [:u.*
                                                                         [:t.name :tenant_name]
                                                                         [:t.slug :tenant_slug]]
                                                                :from [[:users :u]]
                                                                :join [[:tenants :t] [:= :u.tenant_id :t.id]]
                                                                :where [:= :u.id user-id]}))
                                          :can-delete true
                                          :force-override true
                                          :impact-summary {}})
                                      ;; Re-throw constraints that can't be overridden
                                       (throw e)))))
                              ;; Normal mode - all constraints apply
                               (validation/check-user-deletion-constraints tx user-id))
            user (:user constraint-check)
            impact (:impact-summary constraint-check)]

        (log/info "User deletion validation passed:"
          "user-id:" user-id
          "user-email:" (:email user)
          "impact:" impact
          "force-delete:" force-delete
          "dry-run:" dry-run)

        (if dry-run
          ;; Dry run - return impact analysis without deletion
          {:success true
           :dry-run true
           :user user
           :impact-summary impact
           :message "Deletion impact analysis completed - no changes made"}

          ;; Perform actual deletion
          (do
            ;; Pre-deletion audit log
            (audit/log-audit! tx {:admin_id admin-id
                                  :action "user.delete_initiated"
                                  :entity-type "user"
                                  :entity-id user-id
                                  :changes {:user-details (select-keys user [:email :full_name :role :status :tenant_id])
                                            :impact-summary impact
                                            :force-delete force-delete}
                                  :ip-address ip-address
                                  :user-agent user-agent})

            ;; Perform the deletion (CASCADE and SET NULL will be handled by DB constraints)
            (let [delete-result (jdbc/execute-one! tx
                                  (hsql/format {:delete-from :users
                                                :where [:= :id user-id]}))]

              (when (= 0 (get delete-result :next.jdbc/update-count 0))
                (throw (ex-info "User deletion failed - no rows affected"
                         {:status 500 :user-id user-id})))

              ;; Final audit log for successful deletion
              (audit/log-audit! tx {:admin_id admin-id
                                    :action "user.deleted"
                                    :entity-type "user"
                                    :entity-id user-id
                                    :changes {:deleted-user (select-keys user [:email :full_name :role :status :tenant_id])
                                              :impact-summary impact
                                              :deletion-timestamp (time/instant)}
                                    :ip-address ip-address
                                    :user-agent user-agent})

              (log/info "✅ User deleted successfully:"
                "user-id:" user-id
                "email:" (:email user)
                "admin-id:" admin-id)

              {:success true
               :user user
               :impact-summary impact
               :message (str "User " (:email user) " deleted successfully")
               :deleted-at (time/instant)}))))

      (catch clojure.lang.ExceptionInfo e
        ;; Re-throw our custom exceptions with additional context
        (let [error-data (ex-data e)]
          (log/error "User deletion validation failed:"
            "user-id:" user-id
            "admin-id:" admin-id
            "error:" (ex-message e)
            "error-data:" error-data)
          (throw e)))

      (catch Exception e
        ;; Handle unexpected database errors
        (log/error e "Unexpected error during user deletion:"
          "user-id:" user-id
          "admin-id:" admin-id)
        (throw (ex-info "Database error during user deletion"
                 {:status 500
                  :user-id user-id
                  :original-error (.getMessage e)}
                 e))))))
