(ns app.backend.services.admin
  "Admin services - refactored into focused namespaces.

   This namespace serves as a compatibility layer that delegates to the new
   focused admin service namespaces:

   - auth: Authentication and session management
   - audit: Audit logging and compliance tracking
   - dashboard: Dashboard statistics and metrics
   - users: User management and operations
   - users.bulk: Bulk user operations and export
   - monitoring.transactions: Transaction monitoring
   - monitoring.integrations: Integration monitoring

   All functions maintain the same signature for backward compatibility."
  (:require
    [app.backend.services.admin.audit :as audit]
    [app.backend.services.admin.auth :as auth]
    [app.backend.services.admin.dashboard :as dashboard]
    [app.backend.services.admin.monitoring.integrations :as monitoring-integrations]
    [app.backend.services.admin.monitoring.transactions :as monitoring-transactions]
    [app.backend.services.admin.users :as users]
    [app.backend.services.admin.users.bulk :as users-bulk]))

;; ============================================================================
;; Authentication & Session Management (auth namespace)
;; ============================================================================

(defn generate-session-token []
  (auth/generate-session-token))

(defn hash-password [password]
  (auth/hash-password password))

(defn verify-password [password stored-hash]
  (auth/verify-password password stored-hash))

(defn migrate-admin-password! [db admin-id password]
  (auth/migrate-admin-password! db admin-id password))

(defn create-admin! [db admin-data]
  (auth/create-admin! db admin-data))

(defn find-admin-by-email [db email]
  (auth/find-admin-by-email db email))

(defn verify-bcrypt-password [password password-hash]
  (auth/verify-bcrypt-password password password-hash))

(defn verify-sha256-password [password password-hash]
  (auth/verify-sha256-password password password-hash))

(defn authenticate-admin [db email password]
  (auth/authenticate-admin db email password))

(defn create-admin-session! [db admin-id ip-address user-agent]
  (auth/create-admin-session! db admin-id ip-address user-agent))

(defn get-admin-by-session [db token]
  (auth/get-admin-by-session db token))

(defn update-session-activity! [db token]
  (auth/update-session-activity! db token))

(defn invalidate-session! [db token]
  (auth/invalidate-session! db token))

(defn invalidate-all-admin-sessions! [db admin-id]
  (auth/invalidate-all-admin-sessions! db admin-id))

;; ============================================================================
;; Audit Logging (audit namespace)
;; ============================================================================

(defn log-audit! [db audit-data]
  (audit/log-audit! db audit-data))

(defn get-audit-logs [db filters]
  (audit/get-audit-logs db filters))

;; ============================================================================
;; Dashboard Statistics (dashboard namespace)
;; ============================================================================

(defn get-dashboard-stats [db]
  (dashboard/get-dashboard-stats db))

(defn get-advanced-dashboard-data [db]
  (dashboard/get-advanced-dashboard-data db))

;; ============================================================================
;; User Management (users namespace)
;; ============================================================================

(defn list-all-users [db filters]
  (users/list-all-users db filters))

(defn update-user! [db user-id updates admin-id ip-address user-agent]
  (users/update-user! db user-id updates admin-id ip-address user-agent))

(defn get-user-details [db user-id]
  (users/get-user-details db user-id))

(defn update-user-role! [db user-id new-role admin-id ip-address user-agent]
  (users/update-user-role! db user-id new-role admin-id ip-address user-agent))

(defn force-verify-email! [db user-id admin-id ip-address user-agent]
  (users/force-verify-email! db user-id admin-id ip-address user-agent))

(defn reset-user-password! [db user-id admin-id ip-address user-agent]
  (users/reset-user-password! db user-id admin-id ip-address user-agent))

(defn get-user-activity [db user-id filters]
  (users/get-user-activity db user-id filters))

(defn search-users-advanced [db filters]
  (users/search-users-advanced db filters))

(defn create-user! [db user-data admin-id ip-address user-agent]
  (users/create-user! db user-data admin-id ip-address user-agent))

(defn delete-user!
  "Delete user with comprehensive validation and audit logging"
  [db user-id admin-id ip-address user-agent & {:keys [force-delete dry-run]
                                                :or {force-delete false dry-run false}}]
  (users/delete-user! db user-id admin-id ip-address user-agent
    :force-delete force-delete :dry-run dry-run))

(defn check-users-deletion-constraints-batch
  "Batch-check deletion constraints for multiple users"
  [db user-ids]
  (users/check-users-deletion-constraints-batch db user-ids))

;; ============================================================================
;; Bulk User Operations (users.bulk namespace)
;; ============================================================================

(defn bulk-update-user-status! [db user-ids new-status admin-id ip-address user-agent]
  (users-bulk/bulk-update-user-status! db user-ids new-status admin-id ip-address user-agent))

(defn bulk-update-user-role! [db user-ids new-role admin-id ip-address user-agent]
  (users-bulk/bulk-update-user-role! db user-ids new-role admin-id ip-address user-agent))

(defn export-users-csv [db user-ids]
  (users-bulk/export-users-csv db user-ids))

(defn create-user-impersonation-session! [db user-id admin-id ip-address user-agent]
  (users-bulk/create-user-impersonation-session! db user-id admin-id ip-address user-agent))


;; ============================================================================
;; Transaction Monitoring (monitoring.transactions namespace)
;; ============================================================================

(defn get-transaction-overview [db]
  (monitoring-transactions/get-transaction-overview db))

(defn get-transaction-trends [db filters]
  (monitoring-transactions/get-transaction-trends db filters))

(defn get-suspicious-transactions [db filters]
  (monitoring-transactions/get-suspicious-transactions db filters))

;; ============================================================================
;; Integration Monitoring (monitoring.integrations namespace)
;; ============================================================================

(defn get-integration-overview [db]
  (monitoring-integrations/get-integration-overview db))

(defn get-integration-performance [db filters]
  (monitoring-integrations/get-integration-performance db filters))

(defn get-webhook-status [db filters]
  (monitoring-integrations/get-webhook-status db filters))
