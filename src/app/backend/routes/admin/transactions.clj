(ns app.backend.routes.admin.transactions
  "Admin transaction management handlers"
  (:require
    [app.backend.routes.admin.utils :as utils]
    [app.backend.services.admin :as admin-service]
    [app.shared.adapters.database :as db-adapter]
    [taoensso.timbre :as log]))

(defn get-transaction-overview-handler
  "Handler for transaction monitoring overview"
  [db]
  (utils/with-error-handling
    (fn [_request]
      (log/info "📊 Getting transaction overview")
      (let [overview (admin-service/get-transaction-overview db)]
        (-> overview
          db-adapter/convert-pg-objects
          db-adapter/convert-db-keys->app-keys
          utils/json-response)))
    "Failed to get transaction overview"))

(defn get-transaction-trends-handler
  "Handler for transaction trends and analytics"
  [db]
  (utils/with-error-handling
    (fn [request]
      (let [params (:params request)
            period (keyword (or (:period params) "month"))
            months (utils/parse-int-param params :months 12)
            trends (admin-service/get-transaction-trends db {:period period :months months})]
        (-> trends
          db-adapter/convert-pg-objects
          db-adapter/convert-db-keys->app-keys
          utils/json-response)))
    "Failed to get transaction trends"))

(defn get-suspicious-transactions-handler
  "Handler for suspicious transaction detection"
  [db]
  (utils/with-error-handling
    (fn [request]
      (let [params (:params request)
            pagination (utils/extract-pagination-params params)
            suspicious (admin-service/get-suspicious-transactions db pagination)]
        (-> suspicious
          db-adapter/convert-pg-objects
          db-adapter/convert-db-keys->app-keys
          utils/json-response)))
    "Failed to get suspicious transactions"))

;; Route definitions
(defn routes
  "Transaction monitoring route definitions"
  [db]
  [""
   ["/overview" {:get (get-transaction-overview-handler db)}]
   ["/trends" {:get (get-transaction-trends-handler db)}]
   ["/suspicious" {:get (get-suspicious-transactions-handler db)}]])
