(ns app.backend.routes.admin.dashboard
  "Admin dashboard handlers"
  (:require
    [app.backend.middleware.rate-limiting :as rate-limiting]
    [app.backend.routes.admin.utils :as utils]
    [app.backend.services.admin :as admin-service]
    [app.shared.adapters.database :as db-adapter]
    [taoensso.timbre :as log]))

(defn stats-handler
  "Get dashboard statistics"
  [db]
  (utils/with-error-handling
    (fn [_request]
      (let [stats (admin-service/get-dashboard-stats db)
            converted-stats (-> stats
                              db-adapter/convert-pg-objects
                              db-adapter/convert-db-keys->app-keys)]
        (utils/json-response converted-stats)))
    "Failed to retrieve dashboard statistics"))

(defn advanced-dashboard-handler
  "Get advanced dashboard with computed metrics and business intelligence"
  [db]
  (utils/with-error-handling
    (fn [_request]
      (let [data (admin-service/get-advanced-dashboard-data db)]
        (utils/json-response data)))
    "Failed to retrieve advanced dashboard data"))

(defn clear-rate-limits-handler
  "Handler to clear all rate limiting data for development and testing."
  [_db]
  (fn [request]
    (try
      (log/info "Admin clearing rate limits" {:admin-id (get-in request [:session :admin-id])})

      ;; Clear the rate limits
      (rate-limiting/clear-rate-limits!)

      ;; Get stats after clearing
      (let [stats (rate-limiting/get-rate-limit-stats)]
        (log/info "Rate limits cleared successfully" {:stats stats})
        (utils/success-response
          {:message "Rate limits cleared successfully"
           :stats stats}))

      (catch Exception e
        (log/error e "Failed to clear rate limits")
        (utils/error-response "Failed to clear rate limits" 500)))))

;; Route definitions
(defn routes
  "Dashboard route definitions"
  [db]
  [""
   ["" {:get (stats-handler db)}]  ; This handles /admin/api/dashboard
   ["/advanced-dashboard" {:get (advanced-dashboard-handler db)}]
   ["/clear-rate-limits" {:post (clear-rate-limits-handler db)}]])
