(ns app.domain.backend.expenses.handlers.user-receipts
  "User-facing (non-admin) receipt review and approval handlers.

  These endpoints are mounted under /api/v1/expenses and require an authenticated user.

  Responsibilities:
  - list receipts belonging to the current user
  - fetch a receipt detail
  - approve an extracted receipt and create an expense (receipt status → posted)"
  (:require
    [app.domain.backend.expenses.services.receipts :as receipts]
    [app.template.backend.utils.adapters.database :as db-adapter]
    [cheshire.core :as json]
    [clojure.string :as str]
    [ring.util.response :as response]
    [taoensso.timbre :as log])
  (:import
    [java.util UUID]))

(defn- to-app
  [data]
  (-> data
    db-adapter/convert-pg-objects
    db-adapter/convert-db-keys->app-keys))

(defn- json-response
  [data status]
  (-> (response/response (json/generate-string data))
    (response/content-type "application/json")
    (response/status status)))

(defn- unauthorized-response
  ([] (unauthorized-response "Authentication required"))
  ([message]
   (json-response {:error message} 401)))

(defn- try-parse-uuid
  [x]
  (when x
    (try
      (UUID/fromString (str x))
      (catch Exception _ nil))))

(defn- get-user-id
  "Extract user-id from request session and normalize to UUID.

  Accepts either UUID objects or string UUIDs; returns nil if missing/invalid."
  [request]
  (let [raw-id (or (get-in request [:session :auth-session :user :id])
                 (get-in request [:session :user :id])
                 (get-in request [:identity :id]))]
    (cond
      (instance? UUID raw-id) raw-id
      :else (try-parse-uuid raw-id))))

(defn- get-user-role
  "Extract user role from request session and normalize to string (e.g. \"admin\")."
  [request]
  (let [role (or (get-in request [:session :auth-session :user :role])
               (get-in request [:session :user :role])
               (get-in request [:identity :role]))]
    (cond
      (keyword? role) (name role)
      (string? role) role
      :else nil)))

(defn- parse-long-param
  [params k default-val]
  (if-let [v (get params k)]
    (try
      (Long/parseLong (str v))
      (catch Exception _ default-val))
    default-val))

(defn- parse-status-param
  [status-param]
  (cond
    (vector? status-param) status-param
    (seq? status-param) (vec status-param)
    (string? status-param)
    (let [s (str/trim status-param)]
      (if (str/includes? s ",")
        (->> (str/split s #",") (map str/trim) (remove str/blank?) vec)
        s))
    :else nil))

(defn- read-json-body
  [request]
  (or
    (:body-params request)
    (when-let [body (:body request)]
      (try
        (json/parse-string (slurp body) true)
        (catch Exception _ nil)))))

(defn- with-error-handling
  [handler-fn error-message]
  (fn [request]
    (try
      (handler-fn request)
      (catch clojure.lang.ExceptionInfo e
        (let [{:keys [status] :as data} (ex-data e)
              status (or status 500)
              message (or (ex-message e) error-message)]
          (when (= status 500)
            (log/error e error-message data))
          (json-response (cond-> {:error message}
                           (seq (dissoc data :status)) (assoc :details (dissoc data :status)))
            status)))
      (catch Exception e
        (log/error e error-message)
        (json-response {:error error-message} 500)))))

(defn list-receipts-handler
  "GET /api/v1/expenses/receipts

  Query params:
  - status (optional, string or comma-separated)
  - limit (default 50)
  - offset (default 0)
  - order_dir (default desc)"
  [db]
  (with-error-handling
    (fn [request]
      (if-let [user-id (get-user-id request)]
        (let [role (get-user-role request)
              qp (:query-params request)
              status (parse-status-param (or (:status qp) (get qp "status")))
              opts {:status status
                    :limit (parse-long-param qp :limit 50)
                    :offset (parse-long-param qp :offset 0)
                    :order-dir (keyword (or (:order_dir qp) (:order-dir qp) "desc"))}
              rows (if (= "admin" role)
                     (receipts/list-receipts db opts)
                     (receipts/list-user-receipts db user-id opts))]
          (json-response {:data (to-app rows)
                          :limit (:limit opts)
                          :offset (:offset opts)}
            200))
        (unauthorized-response)))
    "Failed to list receipts"))

(defn get-receipt-handler
  "GET /api/v1/expenses/receipts/:id"
  [db]
  (with-error-handling
    (fn [request]
      (if-let [user-id (get-user-id request)]
        (let [role (get-user-role request)]
          (if-let [id (try-parse-uuid (get-in request [:path-params :id]))]
            (if-let [receipt (if (= "admin" role)
                               (receipts/get-receipt db id)
                               (receipts/get-user-receipt db user-id id))]
              (json-response {:data (to-app receipt)} 200)
              (json-response {:error "Receipt not found"} 404))
            (json-response {:error "Invalid id"} 400)))
        (unauthorized-response)))
    "Failed to fetch receipt"))

(defn approve-receipt-handler
  "POST /api/v1/expenses/receipts/:id/approve

  Body: expense form payload (supplier_id, payer_id, purchased_at, total_amount, currency, notes, items)

  Returns {:data {:expense ... :receipt ...}}"
  [db]
  (with-error-handling
    (fn [request]
      (if-let [user-id (get-user-id request)]
        (let [role (get-user-role request)]
          (if-let [id (try-parse-uuid (get-in request [:path-params :id]))]
            (let [body (or (read-json-body request) {})
                  expense (if (= "admin" role)
                            (receipts/approve-and-post-for-user-any! db user-id id body)
                            (receipts/approve-and-post-for-user! db user-id id body))
                  receipt (if (= "admin" role)
                            (receipts/get-receipt db id)
                            (receipts/get-user-receipt db user-id id))]
              (json-response {:data {:expense (to-app expense)
                                     :receipt (to-app receipt)}}
                200))
            (json-response {:error "Invalid id"} 400)))
        (unauthorized-response)))
    "Failed to approve receipt"))
