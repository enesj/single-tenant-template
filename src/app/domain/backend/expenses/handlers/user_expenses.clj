(ns app.domain.backend.expenses.handlers.user-expenses
  "API handlers for user-facing expense endpoints.

   These endpoints are mounted by the template API under /api/v1/expenses.
   All handlers extract user-id from the session and enforce user-based filtering."
  (:require
    [app.domain.backend.expenses.services.user-expenses :as user-expenses]
    [cheshire.core :as json]
    [taoensso.timbre :as log])
  (:import
    [java.util UUID]))

;; ============================================================================
;; Helpers
;; ============================================================================

(declare try-parse-uuid)

(defn- parse-boolean-param
  "Parse boolean parameter from query params map (string values)."
  [params k]
  (when-let [val (get params k)]
    (Boolean/parseBoolean (str val))))

(defn- get-user-id
  "Extract user-id from request session and normalize to UUID.
   Accepts either UUID objects or string UUIDs; returns nil if missing/invalid."
  [request]
  (let [raw-id (or (get-in request [:session :auth-session :user :id])
                 (get-in request [:session :user :id]))]
    (cond
      (instance? UUID raw-id) raw-id
      :else (try-parse-uuid raw-id))))

(defn- try-parse-uuid
  "Parse a UUID from string, returns nil if invalid."
  [s]
  (when s
    (try
      (UUID/fromString (str s))
      (catch Exception _ nil))))

(defn- json-response
  "Create a JSON response with the given body and status."
  ([body] (json-response body 200))
  ([body status]
   {:status status
    :headers {"Content-Type" "application/json"}
    :body (json/generate-string body)}))

(defn- unauthorized-response
  "Return 401 unauthorized response."
  ([] (unauthorized-response "Authentication required"))
  ([message]
   (json-response {:error message} 401)))

(defn- not-found-response
  "Return 404 not found response."
  ([] (not-found-response "Resource not found"))
  ([message]
   (json-response {:error message} 404)))

;; ============================================================================
;; Handler Factories
;; ============================================================================

(defn list-expenses-handler
  "Handler factory for listing user's expenses."
  [db]
  (fn [request]
    (if-let [user-id (get-user-id request)]
      (try
        (let [params (:query-params request)
              opts {:from (:from params)
                    :to (:to params)
                    :supplier-id (try-parse-uuid (:supplier_id params))
                    :payer-id (try-parse-uuid (:payer_id params))
                    :is-posted? (parse-boolean-param params :is_posted)
                    :limit (or (some-> (:limit params) parse-long) 50)
                    :offset (or (some-> (:offset params) parse-long) 0)
                    :order-dir (keyword (or (:order_dir params) "desc"))}
              expenses (user-expenses/list-user-expenses db user-id opts)
              total (user-expenses/count-user-expenses db user-id opts)]
          (json-response {:data expenses
                          :total total
                          :limit (:limit opts)
                          :offset (:offset opts)}))
        (catch Exception e
          (log/error e "Error listing user expenses"
            {:user-id user-id
             :query-params (:query-params request)
             :path-params (:path-params request)
             :message (.getMessage e)})
          (json-response {:error "Failed to list expenses"} 500)))
      (unauthorized-response))))

(defn get-expense-handler
  "Handler factory for getting a single user expense."
  [db]
  (fn [request]
    (if-let [user-id (get-user-id request)]
      (let [expense-id (or (try-parse-uuid (get-in request [:path-params :id]))
                         (try-parse-uuid (get-in request [:parameters :path :id])))]
        (if expense-id
          (try
            (if-let [expense (user-expenses/get-user-expense-with-items db user-id expense-id)]
              (json-response {:data expense})
              (not-found-response "Expense not found"))
            (catch Exception e
              (log/error e "Error getting user expense" {:expense-id expense-id})
              (json-response {:error "Failed to get expense"} 500)))
          (json-response {:error "Invalid expense ID"} 400)))
      (unauthorized-response))))

(defn create-expense-handler
  "Handler factory for creating a user expense."
  [db]
  (fn [request]
    (if-let [user-id (get-user-id request)]
      (try
        (let [body (or (:body-params request) (json/parse-string (slurp (:body request)) true))
              expense-data (select-keys body [:supplier_id :payer_id :purchased_at :total_amount :currency :notes :is_posted :receipt_id])
              items (or (:items body) [])]
          (log/debug "Creating user expense" {:user-id user-id :expense-data expense-data})
          (let [expense (user-expenses/create-user-expense! db user-id expense-data items)]
            (json-response {:data expense} 201)))
        (catch clojure.lang.ExceptionInfo e
          (log/warn "Validation error creating expense" {:error (ex-message e) :data (ex-data e)})
          (json-response {:error (ex-message e)} 400))
        (catch Exception e
          (log/error e "Error creating user expense")
          (json-response {:error "Failed to create expense"} 500)))
      (unauthorized-response))))

(defn update-expense-handler
  "Handler factory for updating a user expense."
  [db]
  (fn [request]
    (if-let [user-id (get-user-id request)]
      (let [expense-id (or (try-parse-uuid (get-in request [:path-params :id]))
                         (try-parse-uuid (get-in request [:parameters :path :id])))]
        (if expense-id
          (try
            (let [body (or (:body-params request) (json/parse-string (slurp (:body request)) true))
                  updates (select-keys body [:supplier_id :payer_id :purchased_at :total_amount :currency :notes :is_posted :items])]
              (if-let [expense (user-expenses/update-user-expense! db user-id expense-id updates)]
                (json-response {:data expense})
                (not-found-response "Expense not found or access denied")))
            (catch Exception e
              (log/error e "Error updating user expense" {:expense-id expense-id})
              (json-response {:error "Failed to update expense"} 500)))
          (json-response {:error "Invalid expense ID"} 400)))
      (unauthorized-response))))

(defn delete-expense-handler
  "Handler factory for deleting (soft delete) a user expense."
  [db]
  (fn [request]
    (if-let [user-id (get-user-id request)]
      (let [expense-id (or (try-parse-uuid (get-in request [:path-params :id]))
                         (try-parse-uuid (get-in request [:parameters :path :id])))]
        (if expense-id
          (try
            (if-let [expense (user-expenses/soft-delete-user-expense! db user-id expense-id)]
              (json-response {:data expense :message "Expense deleted"})
              (not-found-response "Expense not found or access denied"))
            (catch Exception e
              (log/error e "Error deleting user expense" {:expense-id expense-id})
              (json-response {:error "Failed to delete expense"} 500)))
          (json-response {:error "Invalid expense ID"} 400)))
      (unauthorized-response))))

;; ============================================================================
;; Dashboard/Summary Handlers
;; ============================================================================

(defn expense-summary-handler
  "Handler factory for getting user expense summary."
  [db]
  (fn [request]
    (if-let [user-id (get-user-id request)]
      (try
        (let [params (:query-params request)
              days-back (or (some-> (:days_back params) parse-long) 30)
              summary (user-expenses/get-user-expense-summary db user-id {:days-back days-back})]
          (json-response {:data summary}))
        (catch Exception e
          (log/error e "Error getting expense summary"
            {:user-id user-id
             :query-params (:query-params request)
             :message (.getMessage e)})
          (json-response {:error "Failed to get expense summary"} 500)))
      (unauthorized-response))))

(defn spending-by-month-handler
  "Handler factory for getting user spending by month."
  [db]
  (fn [request]
    (if-let [user-id (get-user-id request)]
      (try
        (let [params (:query-params request)
              months-back (or (some-> (:months_back params) parse-long) 6)
              spending (user-expenses/get-user-spending-by-month db user-id {:months-back months-back})]
          (json-response {:data spending}))
        (catch Exception e
          (log/error e "Error getting spending by month"
            {:user-id user-id
             :query-params (:query-params request)
             :message (.getMessage e)})
          (json-response {:error "Failed to get spending by month"} 500)))
      (unauthorized-response))))

(defn spending-by-supplier-handler
  "Handler factory for getting user spending by supplier."
  [db]
  (fn [request]
    (if-let [user-id (get-user-id request)]
      (try
        (let [params (:query-params request)
              opts {:from (:from params)
                    :to (:to params)
                    :limit (or (some-> (:limit params) parse-long) 10)}
              spending (user-expenses/get-user-spending-by-supplier db user-id opts)]
          (json-response {:data spending}))
        (catch Exception e
          (log/error e "Error getting spending by supplier")
          (json-response {:error "Failed to get spending by supplier"} 500)))
      (unauthorized-response))))

;; ============================================================================
;; Reference Data Handlers (Suppliers, Payers)
;; ============================================================================

(defn- normalize-role
  [role]
  (cond
    (keyword? role) (name role)
    (string? role) role
    :else nil))

(defn- get-user-role
  [request]
  (normalize-role
    (or (get-in request [:session :auth-session :user :role])
      (get-in request [:session :user :role]))))

(def ^:private reference-data-read-roles
  #{"viewer" "member" "admin" "owner"})

(def ^:private reference-data-write-roles
  #{"member" "admin" "owner"})

(defn- forbidden-response
  ([] (forbidden-response "Forbidden"))
  ([message]
   (json-response {:error message} 403)))

(defn- ensure-role
  [request allowed-roles message]
  (let [role (get-user-role request)]
    (when-not (contains? allowed-roles role)
      (forbidden-response (or message "Forbidden")))))

(defn list-suppliers-handler
  "Handler factory for listing suppliers available to users."
  [db]
  (fn [request]
    (if-let [_user-id (get-user-id request)]
      (if-let [forbidden (ensure-role request reference-data-read-roles "Role assignment required")]
        forbidden
        (try
          (let [params (:query-params request)
                limit (or (some-> (:limit params) parse-long) 100)
                offset (or (some-> (:offset params) parse-long) 0)
                ;; Use the suppliers service
                suppliers-svc (requiring-resolve 'app.domain.backend.expenses.services.suppliers/list-suppliers)
                suppliers (suppliers-svc db {:limit limit :offset offset})]
            (json-response {:data suppliers}))
          (catch Exception e
            (log/error e "Error listing suppliers")
            (json-response {:error "Failed to list suppliers"} 500))))
      (unauthorized-response))))

(defn list-payers-handler
  "Handler factory for listing payers available to users."
  [db]
  (fn [request]
    (if-let [_user-id (get-user-id request)]
      (if-let [forbidden (ensure-role request reference-data-read-roles "Role assignment required")]
        forbidden
        (try
          (let [params (:query-params request)
                limit (or (some-> (:limit params) parse-long) 100)
                offset (or (some-> (:offset params) parse-long) 0)
                ;; Use the payers service
                payers-svc (requiring-resolve 'app.domain.backend.expenses.services.payers/list-payers)
                payers (payers-svc db {:limit limit :offset offset})]
            (json-response {:data payers}))
          (catch Exception e
            (log/error e "Error listing payers")
            (json-response {:error "Failed to list payers"} 500))))
      (unauthorized-response))))

(defn- read-json-body
  [request]
  (or (:body-params request)
    (json/parse-string (slurp (:body request)) true)))

(defn create-supplier-handler
  "Handler factory for creating a supplier (shared catalog).

  Allowed roles: member/admin."
  [db]
  (fn [request]
    (if-let [_user-id (get-user-id request)]
      (if-let [forbidden (ensure-role request reference-data-write-roles "Only members and admins can modify suppliers")]
        forbidden
        (try
          (let [body (read-json-body request)
                supplier-data (select-keys body [:display_name :address :tax_id])
                create-supplier! (requiring-resolve 'app.domain.backend.expenses.services.suppliers/create-supplier!)
                supplier (create-supplier! db supplier-data)]
            (json-response {:data supplier} 201))
          (catch clojure.lang.ExceptionInfo e
            (log/warn "Validation error creating supplier" {:error (ex-message e) :data (ex-data e)})
            (json-response {:error (ex-message e)} 400))
          (catch Exception e
            (log/error e "Error creating supplier")
            (json-response {:error "Failed to create supplier"} 500))))
      (unauthorized-response))))

(defn update-supplier-handler
  "Handler factory for updating a supplier (shared catalog).

  Allowed roles: member/admin."
  [db]
  (fn [request]
    (if-let [_user-id (get-user-id request)]
      (if-let [forbidden (ensure-role request reference-data-write-roles "Only members and admins can modify suppliers")]
        forbidden
        (let [supplier-id (or (try-parse-uuid (get-in request [:path-params :id]))
                            (try-parse-uuid (get-in request [:parameters :path :id])))]
          (if supplier-id
            (try
              (let [body (read-json-body request)
                    updates (select-keys body [:display_name :address :tax_id])
                    update-supplier! (requiring-resolve 'app.domain.backend.expenses.services.suppliers/update-supplier!)
                    supplier (update-supplier! db supplier-id updates)]
                (if supplier
                  (json-response {:data supplier})
                  (not-found-response "Supplier not found")))
              (catch clojure.lang.ExceptionInfo e
                (log/warn "Validation error updating supplier" {:error (ex-message e) :data (ex-data e)})
                (json-response {:error (ex-message e)} 400))
              (catch Exception e
                (log/error e "Error updating supplier" {:supplier-id supplier-id})
                (json-response {:error "Failed to update supplier"} 500)))
            (json-response {:error "Invalid supplier ID"} 400))))
      (unauthorized-response))))

(defn delete-supplier-handler
  "Handler factory for deleting a supplier (shared catalog).

  Allowed roles: member/admin."
  [db]
  (fn [request]
    (if-let [_user-id (get-user-id request)]
      (if-let [forbidden (ensure-role request reference-data-write-roles "Only members and admins can modify suppliers")]
        forbidden
        (let [supplier-id (or (try-parse-uuid (get-in request [:path-params :id]))
                            (try-parse-uuid (get-in request [:parameters :path :id])))]
          (if supplier-id
            (try
              (let [delete-supplier! (requiring-resolve 'app.domain.backend.expenses.services.suppliers/delete-supplier!)
                    deleted? (boolean (delete-supplier! db supplier-id))]
                (if deleted?
                  (json-response {:success true})
                  (not-found-response "Supplier not found")))
              (catch Exception e
                (log/error e "Error deleting supplier" {:supplier-id supplier-id})
                (json-response {:error "Failed to delete supplier"} 500)))
            (json-response {:error "Invalid supplier ID"} 400))))
      (unauthorized-response))))

(defn create-payer-handler
  "Handler factory for creating a payer (shared catalog).

  Allowed roles: member/admin."
  [db]
  (fn [request]
    (if-let [_user-id (get-user-id request)]
      (if-let [forbidden (ensure-role request reference-data-write-roles "Only members and admins can modify payers")]
        forbidden
        (try
          (let [body (read-json-body request)
                payer-data (-> (select-keys body [:label :type :is_default])
                             (cond-> (contains? body :is_default)
                               (update :is_default boolean)))
                create-payer! (requiring-resolve 'app.domain.backend.expenses.services.payers/create-payer!)
                payer (create-payer! db payer-data)]
            (json-response {:data payer} 201))
          (catch clojure.lang.ExceptionInfo e
            (log/warn "Validation error creating payer" {:error (ex-message e) :data (ex-data e)})
            (json-response {:error (ex-message e)} 400))
          (catch Exception e
            (log/error e "Error creating payer")
            (json-response {:error "Failed to create payer"} 500))))
      (unauthorized-response))))

(defn update-payer-handler
  "Handler factory for updating a payer (shared catalog).

  Allowed roles: member/admin."
  [db]
  (fn [request]
    (if-let [_user-id (get-user-id request)]
      (if-let [forbidden (ensure-role request reference-data-write-roles "Only members and admins can modify payers")]
        forbidden
        (let [payer-id (or (try-parse-uuid (get-in request [:path-params :id]))
                         (try-parse-uuid (get-in request [:parameters :path :id])))]
          (if payer-id
            (try
              (let [body (read-json-body request)
                    updates (-> (select-keys body [:label :type :is_default])
                              (cond-> (contains? body :is_default)
                                (update :is_default boolean)))
                    update-payer! (requiring-resolve 'app.domain.backend.expenses.services.payers/update-payer!)
                    payer (update-payer! db payer-id updates)]
                (if payer
                  (json-response {:data payer})
                  (not-found-response "Payer not found")))
              (catch clojure.lang.ExceptionInfo e
                (log/warn "Validation error updating payer" {:error (ex-message e) :data (ex-data e)})
                (json-response {:error (ex-message e)} 400))
              (catch Exception e
                (log/error e "Error updating payer" {:payer-id payer-id})
                (json-response {:error "Failed to update payer"} 500)))
            (json-response {:error "Invalid payer ID"} 400))))
      (unauthorized-response))))

(defn delete-payer-handler
  "Handler factory for deleting a payer (shared catalog).

  Allowed roles: member/admin."
  [db]
  (fn [request]
    (if-let [_user-id (get-user-id request)]
      (if-let [forbidden (ensure-role request reference-data-write-roles "Only members and admins can modify payers")]
        forbidden
        (let [payer-id (or (try-parse-uuid (get-in request [:path-params :id]))
                         (try-parse-uuid (get-in request [:parameters :path :id])))]
          (if payer-id
            (try
              (let [delete-payer! (requiring-resolve 'app.domain.backend.expenses.services.payers/delete-payer!)
                    deleted? (boolean (delete-payer! db payer-id))]
                (if deleted?
                  (json-response {:success true})
                  (not-found-response "Payer not found")))
              (catch Exception e
                (log/error e "Error deleting payer" {:payer-id payer-id})
                (json-response {:error "Failed to delete payer"} 500)))
            (json-response {:error "Invalid payer ID"} 400))))
      (unauthorized-response))))
