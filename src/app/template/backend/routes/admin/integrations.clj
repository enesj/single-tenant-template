(ns app.template.backend.routes.admin.integrations
  "Admin integration management handlers"
  (:require
    [app.template.backend.routes.admin.utils :as utils]
    [app.admin.backend.services.admin.monitoring.integrations :as admin-integrations]
    [app.template.backend.utils.adapters.database :as db-adapter]
    [taoensso.timbre :as log]))

(defn get-integration-overview-handler
  "Handler for integration monitoring overview"
  [db]
  (utils/with-error-handling
    (fn [_request]
      (log/info "🔗 Getting integration overview")
      (let [overview (admin-integrations/get-integration-overview db)]
        (-> overview
          db-adapter/convert-pg-objects
          db-adapter/convert-db-keys->app-keys
          utils/json-response)))
    "Failed to get integration overview"))

(defn get-integration-performance-handler
  "Handler for integration performance metrics"
  [db]
  (utils/with-error-handling
    (fn [request]
      (let [params (:params request)
            period (keyword (or (:period params) "hour"))
            hours (utils/parse-int-param params :hours 24)
            performance (admin-integrations/get-integration-performance db {:period period :hours hours})]
        (-> performance
          db-adapter/convert-pg-objects
          db-adapter/convert-db-keys->app-keys
          utils/json-response)))
    "Failed to get integration performance"))

(defn get-webhook-status-handler
  "Handler for webhook status and delivery metrics"
  [db]
  (utils/with-error-handling
    (fn [request]
      (let [params (:params request)
            pagination (utils/extract-pagination-params params)
            webhook-status (admin-integrations/get-webhook-status db pagination)]
        (-> webhook-status
          db-adapter/convert-pg-objects
          db-adapter/convert-db-keys->app-keys
          utils/json-response)))
    "Failed to get webhook status"))

;; Route definitions
(defn routes
  "Integration monitoring route definitions"
  [db]
  [""
   ["/overview" {:get (get-integration-overview-handler db)}]
   ["/performance" {:get (get-integration-performance-handler db)}]
   ["/webhooks" {:get (get-webhook-status-handler db)}]])
