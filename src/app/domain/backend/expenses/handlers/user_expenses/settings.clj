(ns app.domain.backend.expenses.handlers.user-expenses.settings
  "User expense export and delete-all handlers.

   Legacy GET/PUT /api/v1/expenses/settings handlers were removed after the
   frontend migrated to the profile/settings hierarchy."
  (:require
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [app.shared.adapters.database :as db-adapter]
    [app.template.backend.security.privacy-subject :as privacy-subject]
    [clojure.string :as str]
    [next.jdbc :as jdbc]
    [taoensso.timbre :as log]))

(def power-user-roles
  "Roles allowed to execute danger-zone actions (delete-all)."
  #{"admin" "owner"})

(defn- load-export-expenses
  "Load expenses for the profile export action.

   When `tenant-id` is present, export the full tenant dataset; otherwise
   fall back to the requesting user's own expenses. User-scoped exports match
   subject_ref rows and legacy user_id rows during migration."
  [db user-id tenant-id]
  (let [subject-ref (privacy-subject/user-subject-ref user-id)]
    (->> (if tenant-id
           (jdbc/execute! db
             ["SELECT e.*, s.display_name as supplier_name, p.label as payer_label
             FROM expenses e
             LEFT JOIN suppliers s ON e.supplier_id = s.id
             LEFT JOIN payers p ON e.payer_id = p.id
             WHERE e.tenant_id = ?
             ORDER BY e.purchased_at DESC
             LIMIT 1000"
              tenant-id])
           (jdbc/execute! db
             ["SELECT e.*, s.display_name as supplier_name, p.label as payer_label
             FROM expenses e
             LEFT JOIN suppliers s ON e.supplier_id = s.id
             LEFT JOIN payers p ON e.payer_id = p.id
             WHERE (e.subject_ref = ? OR e.user_id = ?)
             ORDER BY e.purchased_at DESC
             LIMIT 1000"
              subject-ref user-id]))
      (map db-adapter/to-app))))

(defn- delete-all-expenses!
  "Delete all expenses for the active profile danger-zone scope.

   When `tenant-id` is present, delete the full tenant dataset; otherwise
   fall back to the requesting user's own expenses. User-scoped deletes match
   subject_ref rows and legacy user_id rows during migration."
  [tx user-id tenant-id]
  (if tenant-id
    (do
      (jdbc/execute-one!
        tx
        ["UPDATE receipts
          SET expense_id = NULL,
              status = CASE WHEN status = 'posted'::receipt_status THEN 'extracted'::receipt_status ELSE status END
          WHERE expense_id IN (SELECT id FROM expenses WHERE tenant_id = ?)"
         tenant-id])
      (jdbc/execute-one!
        tx
        ["DELETE FROM expenses WHERE tenant_id = ?"
         tenant-id]))
    (let [subject-ref (privacy-subject/user-subject-ref user-id)]
      (jdbc/execute-one!
        tx
        ["UPDATE receipts
          SET expense_id = NULL,
              status = CASE WHEN status = 'posted'::receipt_status THEN 'extracted'::receipt_status ELSE status END
          WHERE expense_id IN (SELECT id FROM expenses WHERE subject_ref = ? OR user_id = ?)"
         subject-ref user-id])
      (jdbc/execute-one!
        tx
        ["DELETE FROM expenses WHERE subject_ref = ? OR user_id = ?"
         subject-ref user-id]))))

(defn export-expenses-handler
  "GET /api/v1/profile/export - export expenses as CSV/PDF.
   Used by the profile page danger-zone actions."
  [db]
  (fn [request]
    (if-let [user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request h/expenses-read-roles "Role assignment required")]
        forbidden
        (let [params (:query-params request)
              format (or (h/get-param params :format) "csv")
              tenant-id (h/get-tenant-id request)]
          (try
            (let [expenses (load-export-expenses db user-id tenant-id)]
              (if (= format "csv")
                (let [header "id,purchased_at,supplier,payer,total_amount,currency,notes\n"
                      rows (->> expenses
                             (map (fn [e]
                                    (str/join ","
                                      [(str (:expenses/id e))
                                       (str (:expenses/purchased-at e))
                                       (str "\"" (or (:supplier-name e) "") "\"")
                                       (str "\"" (or (:payer-label e) "") "\"")
                                       (str (:expenses/total-amount e))
                                       (str (:expenses/currency e))
                                       (str "\"" (str/replace (or (:expenses/notes e) "") "\"" "\"\"") "\"")])))
                             (str/join "\n"))
                      csv-content (str header rows)]
                  {:status 200
                   :headers {"Content-Type" "text/csv"
                             "Content-Disposition" "attachment; filename=\"expenses.csv\""}
                   :body csv-content})
                (h/json-response {:message "PDF export not yet implemented"
                                  :format format
                                  :count (count expenses)}
                  200)))
            (catch Exception e
              (log/error e "Failed to export expenses")
              (h/json-response {:error "Export failed"} 500)))))
      (h/unauthorized-response))))

(defn delete-all-expenses-handler
  "DELETE /api/v1/profile/all - permanently delete all expenses in scope.
   Requires admin/owner role and confirmation token."
  [db]
  (fn [request]
    (if-let [user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request power-user-roles
                           "Only admin/owner can delete all expenses")]
        forbidden
        (let [params (or (:body-params request)
                       (:query-params request)
                       {})
              confirmation (or (h/get-param params :confirmation)
                             (h/get-param params :token))
              tenant-id (h/get-tenant-id request)]
          (if (= confirmation "DELETE_ALL_EXPENSES")
            (try
              (jdbc/with-transaction [tx db]
                (let [result (delete-all-expenses! tx user-id tenant-id)]
                  (log/warn "Power user deleted all expenses"
                    {:user-id user-id
                     :tenant-id tenant-id
                     :scope (if tenant-id :tenant :user)
                     :affected (:next.jdbc/update-count result)})
                  (h/json-response {:data {:deleted-count (or (:next.jdbc/update-count result) 0)}})))
              (catch Exception e
                (log/error e "Failed to delete all expenses")
                (h/json-response {:error "Delete failed"} 500)))
            (h/json-response {:error "Invalid confirmation token"} 400))))
      (h/unauthorized-response))))
