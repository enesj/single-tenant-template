(ns app.domain.backend.expenses.handlers.user-expenses.settings
  "User expense settings and export handlers.

   NOTE: Settings storage is currently stubbed - returns defaults.
   TODO: Add user_expense_settings table or JSONB column to users table."
  (:require
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [clojure.string :as str]
    [next.jdbc :as jdbc]
    [next.jdbc.sql :as sql]
    [taoensso.timbre :as log]))

;; ---------------------------------------------------------------------------
;; Role definitions
;; ---------------------------------------------------------------------------

(def power-user-roles
  "Roles allowed to execute danger-zone actions (delete-all)."
  #{"admin" "owner"})

;; ---------------------------------------------------------------------------
;; Settings handlers (stub implementation)
;; ---------------------------------------------------------------------------

(defn get-settings-handler
  "GET /api/v1/expenses/settings - fetch user settings.
   Currently returns default values until settings storage is implemented."
  [_db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (h/json-response
        {:default_currency "BAM"
         :default_payer_id nil
         :notifications_enabled true})
      (h/unauthorized-response))))

(defn update-settings-handler
  "PUT /api/v1/expenses/settings - update user settings.
   Currently a no-op stub that returns the input."
  [_db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (let [body (h/read-body-params request)]
        (log/info "Settings update request (stub)" {:settings body})
        (h/json-response body))
      (h/unauthorized-response))))

;; ---------------------------------------------------------------------------
;; Export handler
;; ---------------------------------------------------------------------------

(defn export-expenses-handler
  "GET /api/v1/expenses/export - export expenses as CSV/PDF.
   Returns a download URL or the data directly."
  [db]
  (fn [request]
    (if-let [user-id (h/get-user-id request)]
      (let [params (:query-params request)
            format (or (h/get-param params :format) "csv")]
        (try
          (let [expenses (jdbc/execute! db
                           ["SELECT e.*, s.display_name as supplier_name, p.label as payer_label
                             FROM expenses e
                             LEFT JOIN suppliers s ON e.supplier_id = s.id
                             LEFT JOIN payers p ON e.payer_id = p.id
                             WHERE e.user_id = ? AND e.deleted_at IS NULL
                             ORDER BY e.purchased_at DESC
                             LIMIT 1000"
                            user-id])]
            (if (= format "csv")
              ;; Return CSV data directly
              (let [header "id,purchased_at,supplier,payer,total_amount,currency,notes\n"
                    rows (->> expenses
                           (map (fn [e]
                                  (str/join ","
                                    [(str (:expenses/id e))
                                     (str (:expenses/purchased_at e))
                                     (str "\"" (or (:supplier_name e) "") "\"")
                                     (str "\"" (or (:payer_label e) "") "\"")
                                     (str (:expenses/total_amount e))
                                     (str (:expenses/currency e))
                                     (str "\"" (str/replace (or (:expenses/notes e) "") "\"" "\"\"") "\"")])))
                           (str/join "\n"))
                    csv-content (str header rows)]
                {:status 200
                 :headers {"Content-Type" "text/csv"
                           "Content-Disposition" "attachment; filename=\"expenses.csv\""}
                 :body csv-content})
              ;; For PDF, return stub message
              (h/json-response {:message "PDF export not yet implemented"
                                :format format
                                :count (count expenses)} 200)))
          (catch Exception e
            (log/error e "Failed to export expenses")
            (h/json-response {:error "Export failed"} 500))))
      (h/unauthorized-response))))

;; ---------------------------------------------------------------------------
;; Delete-all handler (danger zone, admin/owner only)
;; ---------------------------------------------------------------------------

(defn delete-all-expenses-handler
  "DELETE /api/v1/expenses/all - permanently delete all user expenses.
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
                             (h/get-param params :token))]
          (if (= confirmation "DELETE_ALL_EXPENSES")
            (try
              ;; Soft-delete all expenses for user
              (let [result (jdbc/execute-one! db
                             ["UPDATE expenses
                               SET deleted_at = NOW()
                               WHERE user_id = ? AND deleted_at IS NULL"
                              user-id])]
                (log/warn "User deleted all expenses" {:user-id user-id
                                                       :affected (:next.jdbc/update-count result)})
                (h/json-response {:success true
                                  :deleted_count (or (:next.jdbc/update-count result) 0)}))
              (catch Exception e
                (log/error e "Failed to delete all expenses")
                (h/json-response {:error "Delete failed"} 500)))
            (h/json-response {:error "Invalid confirmation token"} 400))))
      (h/unauthorized-response))))
