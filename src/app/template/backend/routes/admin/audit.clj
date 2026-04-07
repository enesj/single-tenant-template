(ns app.template.backend.routes.admin.audit
  "Admin audit log handlers"
  (:require
    [app.template.backend.security.email :as email-privacy]
    [app.template.backend.routes.admin.utils :as utils]
    [app.admin.backend.services.admin.audit :as audit-service]
    [app.shared.adapters.database :as shared-db]
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
            sort (utils/extract-sort-params params)
            filters {:admin-id (when (:admin-id params)
                                 (utils/parse-uuid-custom (:admin-id params)))
                     :entity-type (:entity-type params)
                     :entity-id (when (:entity-id params)
                                  (utils/parse-uuid-custom (:entity-id params)))
                     :action (:action params)}
            {:keys [logs total limit offset]}
            (audit-service/get-audit-logs-page db (merge filters pagination sort))]
        ;; Convert any remaining PostgreSQL objects for JSON serialization
        (utils/json-response {:logs (shared-db/convert-pg-objects logs)
                              :total total
                              :limit limit
                              :offset offset})))
    "Failed to retrieve audit logs"))

(defn bulk-delete-audit-logs-handler
  "Delete multiple audit logs (admin action)"
  [db]
  (utils/with-error-handling
    (fn [request]
      (let [admin (:admin request)
            admin-id (:id admin)
            body (:body request)
            ids (or (:ids body) [])
            ;; Parse UUIDs
            parsed-ids (keep utils/parse-uuid-custom ids)]
        (log/info "Admin attempting to bulk delete audit logs"
          {:admin-id admin-id
           :admin-ref (email-privacy/admin-ref admin-id)
           :count (count parsed-ids)})

        (if (empty? parsed-ids)
          (utils/error-response "No valid IDs provided" :status 400)
          (let [result (next-jdbc/with-transaction [tx db]
                         ;; Set admin bypass context
                         (next-jdbc/execute-one! tx ["SET LOCAL app.bypass_rls = true"])
                         ;; Execute the bulk delete using ANY for array matching
                         (next-jdbc/execute-one! tx
                           ["DELETE FROM audit_logs WHERE id = ANY(?::uuid[])"
                            (into-array String (map str parsed-ids))]))
                deleted-count (:next.jdbc/update-count result)]
            (log/info "Successfully bulk deleted audit logs"
              {:admin-id admin-id
               :admin-ref (email-privacy/admin-ref admin-id)
               :deleted-count deleted-count})
            (utils/log-admin-action "bulk_delete_audit_logs" admin-id
              "audit_logs" nil {:count deleted-count :ids (map str parsed-ids)})
            (utils/success-response {:message (str deleted-count " audit logs deleted successfully")
                                     :deleted-count deleted-count})))))
    "Failed to bulk delete audit logs"))

(defn get-unread-api-failures-handler
  "Get the count of unacknowledged external API failure audit entries"
  [db]
  (utils/with-error-handling
    (fn [request]
      (let [admin (:admin request)
            admin-id (:id admin)
            count (audit-service/get-unread-api-failure-count db admin-id)]
        (utils/json-response {:unread-count count})))
    "Failed to get unread API failure count"))

(defn acknowledge-api-failures-handler
  "Acknowledge/dismiss the API failure notification badge"
  [db]
  (utils/with-error-handling
    (fn [request]
      (let [admin (:admin request)
            admin-id (:id admin)]
        (audit-service/acknowledge-api-failures! db admin-id)
        (utils/success-response {:message "API failures acknowledged"})))
    "Failed to acknowledge API failures"))

;; Route definitions
(defn routes
  "Audit logging route definitions"
  [db]
  [""
   ["" {:get (get-audit-logs-handler db)}]
   ["/batch" {:delete (bulk-delete-audit-logs-handler db)}]
   ["/api-failures/unread" {:get (get-unread-api-failures-handler db)}]
   ["/api-failures/acknowledge" {:post (acknowledge-api-failures-handler db)}]])
