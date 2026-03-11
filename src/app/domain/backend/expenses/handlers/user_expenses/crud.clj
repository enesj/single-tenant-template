(ns app.domain.backend.expenses.handlers.user-expenses.crud
  "CRUD handlers for user expenses."
  (:require
    [app.domain.backend.expenses.services.user-expenses :as user-expenses]
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [cheshire.core :as json]
    [taoensso.timbre :as log]))

(defn list-expenses-handler
  "Handler factory for listing user's expenses."
  [db]
  (fn [request]
    (if-let [user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request h/expenses-read-roles "Role assignment required")]
        forbidden
        (let [tenant-id (h/get-tenant-id request)]
          (try
            (let [params (:query-params request)
                  order-by (h/parse-order-by params)
                  order-dir (h/parse-order-dir params)
                  opts (cond-> {:from (h/get-param params :from)
                                :to (h/get-param params :to)
                                :supplier-id (h/try-parse-uuid (h/get-param params :supplier_id))
                                :payer-id (h/try-parse-uuid (h/get-param params :payer_id))
                                :is-posted? (h/parse-boolean-param params :is_posted)
                                :limit (or (some-> (h/get-param params :limit) parse-long) 50)
                                :offset (or (some-> (h/get-param params :offset) parse-long) 0)}
                         order-by (assoc :order-by order-by)
                         order-dir (assoc :order-dir order-dir))
                  uid (when-not (h/tenant-elevated? request) user-id)
                  expenses (user-expenses/list-user-expenses db tenant-id uid opts)
                  total (user-expenses/count-user-expenses db tenant-id uid opts)]
              (h/json-response {:data expenses
                                :total total
                                :limit (:limit opts)
                                :offset (:offset opts)}))
            (catch Exception e
              (log/error e "Error listing user expenses"
                {:user-id user-id
                 :query-params (:query-params request)
                 :path-params (:path-params request)
                 :message (.getMessage e)})
              (h/json-response {:error "Failed to list expenses"} 500)))))
      (h/unauthorized-response))))

(defn get-expense-handler
  "Handler factory for getting a single user expense."
  [db]
  (fn [request]
    (if-let [user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request h/expenses-read-roles "Role assignment required")]
        forbidden
        (let [tenant-id (h/get-tenant-id request)
              expense-id (or (h/try-parse-uuid (get-in request [:path-params :id]))
                           (h/try-parse-uuid (get-in request [:parameters :path :id])))]
          (if expense-id
            (try
              (let [uid (when-not (h/tenant-elevated? request) user-id)
                    expense (user-expenses/get-user-expense-with-items db tenant-id uid expense-id)]
                (if expense
                  (h/json-response {:data expense})
                  (h/not-found-response "Expense not found")))
              (catch Exception e
                (log/error e "Error getting user expense" {:expense-id expense-id})
                (h/json-response {:error "Failed to get expense"} 500)))
            (h/json-response {:error "Invalid expense ID"} 400))))
      (h/unauthorized-response))))

(defn create-expense-handler
  "Handler factory for creating a user expense."
  [db]
  (fn [request]
    (if-let [user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request h/expenses-write-roles "Only members, admins, and owners can create expenses")]
        forbidden
        (let [tenant-id (h/get-tenant-id request)]
          (try
            (let [body (or (:body-params request) (json/parse-string (slurp (:body request)) true))
                  expense-data (select-keys body [:supplier_id :store_id :payer_id :expense_category_id :article_id :purchased_at :total_amount :currency :notes :is_posted :receipt_id])
                  items (or (:items body) [])]
              (log/debug "Creating user expense" {:user-id user-id :tenant-id tenant-id :expense-data expense-data})
              (let [expense (user-expenses/create-user-expense! db tenant-id user-id expense-data items)]
                (h/json-response {:data expense} 201)))
            (catch clojure.lang.ExceptionInfo e
              (log/warn "Validation error creating expense" {:error (ex-message e) :data (ex-data e)})
              (h/json-response {:error (ex-message e)} 400))
            (catch Exception e
              (log/error e "Error creating user expense")
              (h/json-response {:error "Failed to create expense"} 500)))))
      (h/unauthorized-response))))

(defn update-expense-handler
  "Handler factory for updating a user expense."
  [db]
  (fn [request]
    (if-let [user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request h/expenses-write-roles "Only members, admins, and owners can modify expenses")]
        forbidden
        (let [tenant-id (h/get-tenant-id request)
              expense-id (or (h/try-parse-uuid (get-in request [:path-params :id]))
                           (h/try-parse-uuid (get-in request [:parameters :path :id])))]
          (if expense-id
            (try
              (let [body (h/read-body-params request)
                    updates (select-keys body [:supplier_id :store_id :payer_id :expense_category_id :purchased_at :total_amount :currency :notes :is_posted :items])]
                (if-let [expense (user-expenses/update-user-expense! db tenant-id user-id expense-id updates)]
                  (h/json-response {:data expense})
                  (h/not-found-response "Expense not found or access denied")))
              (catch clojure.lang.ExceptionInfo e
                (log/warn "Validation error updating expense" {:error (ex-message e) :data (ex-data e)})
                (h/json-response {:error (ex-message e)} 400))
              (catch Exception e
                ;; NOTE: Our dev timbre output fn does not always print throwable stack traces.
                ;; Include message + class explicitly so 500s are debuggable from logs.
                (log/error "Error updating user expense"
                  {:expense-id expense-id
                   :exception (str (class e))
                   :message (.getMessage e)})
                (h/json-response {:error "Failed to update expense"} 500)))
            (h/json-response {:error "Invalid expense ID"} 400))))
      (h/unauthorized-response))))
