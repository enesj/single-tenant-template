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

(defn bulk-delete-audit-logs-handler
  "Delete multiple audit logs (admin action)"
  [db]
  (utils/with-error-handling
    (fn [request]
      (let [admin (:admin request)
            body (:body request)
            ids (or (:ids body) [])
            ;; Parse UUIDs
            parsed-ids (keep utils/parse-uuid-custom ids)]
        (log/info "Admin" (:email admin) "attempting to bulk delete" (count parsed-ids) "audit logs")

        (if (empty? parsed-ids)
          (utils/error-response "No valid IDs provided" :status 400)
          (let [result (next-jdbc/with-transaction [tx db]
                         ;; Set admin bypass context
                         (next-jdbc/execute-one! tx ["SET LOCAL app.bypass_rls = true"])
                         ;; Execute the bulk delete using ANY for array matching
                         (next-jdbc/execute-one! tx
                           [(str "DELETE FROM audit_logs WHERE id = ANY(?::uuid[])")
                            (into-array String (map str parsed-ids))]))
                deleted-count (:next.jdbc/update-count result)]
            (log/info "Successfully bulk deleted" deleted-count "audit logs by admin" (:email admin))
            (utils/log-admin-action "bulk_delete_audit_logs" (:id admin)
              "audit_logs" nil {:count deleted-count :ids (map str parsed-ids)})
            (utils/success-response {:message (str deleted-count " audit logs deleted successfully")
                                     :deleted-count deleted-count})))))
    "Failed to bulk delete audit logs"))

;; Route definitions
(defn routes
  "Audit logging route definitions"
  [db]
  [""
   ["" {:get (get-audit-logs-handler db)}]
   ["/bulk" {:delete (bulk-delete-audit-logs-handler db)}]
   ["/:id" {:delete (delete-audit-log-handler db)}]])
