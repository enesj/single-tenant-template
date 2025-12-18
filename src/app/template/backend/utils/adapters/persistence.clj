(ns app.template.backend.utils.adapters.persistence
  "Shared database persistence utilities, transactions, and audit logging."
  (:require
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [taoensso.timbre :as log]))

(defn with-admin-transaction
  "Execute a function within an admin transaction with RLS bypass and audit logging."
  ([db f]
   (with-admin-transaction db f nil))
  ([db f audit-context]
   (jdbc/with-transaction [tx db]
     (try
       (jdbc/execute-one! tx ["SET LOCAL app.bypass_rls = true"])
       (let [result (f tx)]
         (when audit-context
           (let [{:keys [admin-id action entity-type entity-id]} audit-context]
             (when (and admin-id action)
               (try
                 (jdbc/execute-one! tx
                   ["INSERT INTO admin_audit_log (admin_id, action, entity_type, entity_id, timestamp)
                    VALUES (?, ?, ?, ?, NOW())"
                    admin-id action entity-type entity-id])
                 (catch Exception e
                   (log/warn "Failed to log audit info" (.getMessage e)))))))
         result)
       (catch Exception e
         (log/error e "Admin transaction failed" audit-context)
         (throw e))))))

(defn execute-admin-query
  "Execute a query with admin-specific error handling and result normalization.
   Accepts HoneySQL maps, next.jdbc sql-params vectors, or raw SQL strings.
   Note: Normalization must be passed as an argument or handled by the caller."
  [db query normalize-fn & [options]]
  (let [{:keys [single? audit-context bypass-rls?]
         :or {single? false bypass-rls? true}} options

        formatted-query (cond
                          (vector? query) query
                          (string? query) [query]
                          (map? query) (sql/format query)
                          :else query)

        execute-fn (fn [tx]
                     (when bypass-rls?
                       (jdbc/execute-one! tx ["SET LOCAL app.bypass_rls = true"]))

                     (let [raw-result (if single?
                                        (jdbc/execute-one! tx formatted-query)
                                        (jdbc/execute! tx formatted-query))]
                       (normalize-fn raw-result)))]

    (if audit-context
      (with-admin-transaction db execute-fn audit-context)
      (jdbc/with-transaction [tx db]
        (execute-fn tx)))))
