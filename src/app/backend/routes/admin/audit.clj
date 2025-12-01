(ns app.backend.routes.admin.audit
  "Admin audit log handlers"
  (:require
    [app.backend.routes.admin.utils :as utils]
    [app.backend.services.admin.audit :as audit-service]
    [app.shared.adapters.database :as db-adapter]
    [next.jdbc :as next-jdbc]
    [taoensso.timbre :as log]))

(defn get-audit-logs-handler
  "Get audit logs with filtering"
  [db]
  (utils/with-error-handling
    (fn [request]
      (let [params (:params request)
            pagination (utils/extract-pagination-params params
                         :default-limit 100)
            filters {:admin-id (when (:admin-id params)
                                 (utils/parse-uuid-custom (:admin-id params)))
                     :entity-type (:entity-type params)
                     :entity-id (when (:entity-id params)
                                  (utils/parse-uuid-custom (:entity-id params)))
                     :action (:action params)}
            logs (audit-service/get-audit-logs db (merge filters pagination))]
        ;; Convert any remaining PostgreSQL objects for JSON serialization
        (utils/json-response {:logs (db-adapter/convert-pg-objects logs)})))
    "Failed to retrieve audit logs"))

(defn delete-audit-log-handler
  "Delete audit log (admin action)"
  [db]
  (utils/with-error-handling
    (fn [request]
      (utils/handle-uuid-request request :id
        (fn [audit-id request]
          (let [admin (:admin request)]
            (log/info "Admin" (:email admin) "attempting to delete audit log" audit-id)

            ;; Execute delete with admin context - set RLS bypass
            (let [result (next-jdbc/with-transaction [tx db]
                           ;; Set admin bypass context
                           (next-jdbc/execute-one! tx ["SET LOCAL app.bypass_rls = true"])
                           ;; Execute the delete
                           (next-jdbc/execute-one! tx
                             ["DELETE FROM audit_logs WHERE id = ?::uuid" audit-id]))]
              (if (> (:next.jdbc/update-count result) 0)
                (do
                  (log/info "Successfully deleted audit log" audit-id "by admin" (:email admin))
                  (utils/log-admin-action "delete_audit_log" (:id admin)
                    "audit_log" audit-id {})
                  (utils/success-response {:message "Audit log deleted successfully"}))
                (utils/error-response "Audit log not found" :status 404)))))))
    "Failed to delete audit log"))

;; Route definitions
(defn routes
  "Audit logging route definitions"
  [db]
  [""
   ["" {:get (get-audit-logs-handler db)}]
   ["/:id" {:delete (delete-audit-log-handler db)}]])
