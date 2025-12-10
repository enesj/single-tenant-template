(ns app.backend.handlers.user-expenses
  "API handlers for user-facing expense endpoints.
   All handlers extract user-id from session and enforce user-based filtering."
  (:require
    [app.backend.routes.admin.utils :as utils]
    [app.backend.services.user-expenses :as user-expenses]
    [cheshire.core :as json]
    [taoensso.timbre :as log])
  (:import
    [java.util UUID]))

;; ============================================================================
;; Helpers
;; ============================================================================

(declare try-parse-uuid)

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
                    :is-posted? (utils/parse-boolean-param params :is_posted)
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
                  updates (select-keys body [:supplier_id :payer_id :purchased_at :total_amount :currency :notes :is_posted])]
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
