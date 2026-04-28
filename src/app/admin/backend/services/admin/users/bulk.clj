(ns app.admin.backend.services.admin.users.bulk
  "Bulk user ops; keep batch validation and side effects centralized."
  (:require
    [app.admin.backend.services.admin.audit :as audit]
    [app.admin.backend.services.admin.users.validation :as validation]
    [app.shared.adapters.database :as shared-db]
    [app.shared.adapters.normalization :as norm]
    [app.template.backend.security.email :as email-privacy]
    [app.template.backend.utils.adapters.persistence :as persist]
    [app.shared.type-conversion :as tc]
    [clojure.string :as str]
    [honey.sql :as hsql]
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
                              (hsql/format {:select [:id field-key]
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
  (cond-> {:select [:u.id
                    :u.status
                    :u.email_verified
                    :u.auth_provider
                    :u.created_at
                    :u.last_login_at]
           :from [[:users :u]]
           :order-by [[:u.created_at :desc]]}
    (seq user-ids) (assoc :where [:in :u.id user-ids])
    (not (seq user-ids)) (assoc :limit 10000)))

(defn export-users-csv
  "Export pseudonymous user data as CSV.

  The export intentionally omits raw emails, encrypted email persistence fields,
  full names, and tenant relationship fields. Routine admin CSV exports should
  not create a portable identity dump."
  [db user-ids]
  (try
    (let [users (persist/execute-admin-query
                  db
                  (export-users-query user-ids)
                  (fn [raw]
                    (->> (-> raw
                           shared-db/convert-pg-objects
                           (norm/normalize-admin-result export-user-config))
                      (mapv #(dissoc %
                               :email
                               :email-ciphertext
                               :email-lookup-hash
                               :email-key-version
                               :full-name
                               :tenant-name
                               :tenant-slug)))))

          ;; Convert to CSV format. Keep this intentionally pseudonymous.
          csv-headers "User Ref,Status,Email Verified,Auth Provider,Created At,Last Login"
          csv-rows (map (fn [user]
                          (str (email-privacy/user-ref (:id user)) ","
                            (or (:status user) "") ","
                            (if (:email-verified user) "Yes" "No") ","
                            (or (:auth-provider user) "") ","
                            (or (:created-at user) "") ","
                            (or (:last-login-at user) ""))) users)
          csv-content (str csv-headers "\n" (str/join "\n" csv-rows))]

      {:success true
       :content csv-content
       :filename (str "users-export-" (.format (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd-HHmm")
                                        (java.time.LocalDateTime/now)) ".csv")})
    (catch Exception e
      (log/error e "Failed to export users" {:user_ids user-ids})
      {:error (.getMessage e)})))

