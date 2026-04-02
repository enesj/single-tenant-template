(ns app.domain.backend.expenses.handlers.user-expenses.summary
  "Dashboard and summary handlers for user expenses."
  (:require
    [app.domain.backend.expenses.services.user-expenses :as user-expenses]
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [taoensso.timbre :as log]))

(defn expense-summary-handler
  "Handler factory for getting user expense summary."
  [db]
  (fn [request]
    (if-let [user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request h/expenses-read-roles "Role assignment required")]
        forbidden
        (let [tenant-id (h/get-tenant-id request)]
          (try
            (let [params (:query-params request)
                  days-back (or (some-> (h/get-param params :days_back) parse-long) 30)
                  uid (when-not (h/tenant-elevated? request) user-id)
                  summary (user-expenses/get-user-expense-summary db tenant-id uid {:days-back days-back
                                                                                    :from (h/get-param params :from)
                                                                                    :to (h/get-param params :to)
                                                                                    :supplier-id (h/get-param params :supplier_id)
                                                                                    :expense-category-id (h/get-param params :expense_category_id)})]
              (h/json-response {:data summary}))
            (catch Exception e
              (log/error e "Error getting expense summary"
                {:user-id user-id
                 :query-params (:query-params request)
                 :message (.getMessage e)})
              (h/json-response {:error "Failed to get expense summary"} 500)))))
      (h/unauthorized-response))))

(defn spending-by-month-handler
  "Handler factory for getting user spending by month."
  [db]
  (fn [request]
    (if-let [user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request h/expenses-read-roles "Role assignment required")]
        forbidden
        (let [tenant-id (h/get-tenant-id request)]
          (try
            (let [params (:query-params request)
                  months-back (or (some-> (:months_back params) parse-long) 6)
                  uid (when-not (h/tenant-elevated? request) user-id)
                  spending (user-expenses/get-user-spending-by-month db tenant-id uid {:months-back months-back})]
              (h/json-response {:data spending}))
            (catch Exception e
              (log/error e "Error getting spending by month"
                {:user-id user-id
                 :query-params (:query-params request)
                 :message (.getMessage e)})
              (h/json-response {:error "Failed to get spending by month"} 500)))))
      (h/unauthorized-response))))

(defn spending-by-supplier-handler
  "Handler factory for getting user spending by supplier."
  [db]
  (fn [request]
    (if-let [user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request h/expenses-read-roles "Role assignment required")]
        forbidden
        (let [tenant-id (h/get-tenant-id request)]
          (try
            (let [params (:query-params request)
                  opts {:from (:from params)
                        :to (:to params)
                        :limit (or (some-> (:limit params) parse-long) 10)}
                  uid (when-not (h/tenant-elevated? request) user-id)
                  spending (user-expenses/get-user-spending-by-supplier db tenant-id uid opts)]
              (h/json-response {:data spending}))
            (catch Exception e
              (log/error e "Error getting spending by supplier")
              (h/json-response {:error "Failed to get spending by supplier"} 500)))))
      (h/unauthorized-response))))
