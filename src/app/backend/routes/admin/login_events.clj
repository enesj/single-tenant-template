(ns app.backend.routes.admin.login-events
  "Admin login events handlers"
  (:require
    [app.backend.routes.admin.utils :as utils]
    [app.backend.services.monitoring.login-events :as login-monitoring]
    [taoensso.timbre :as log]))

(defn get-login-events-handler
  "Get login events for admins and users with optional filtering."
  [db]
  (utils/with-error-handling
    (fn [request]
      (let [params (:params request)
            pagination (utils/extract-pagination-params params
                         :default-limit 100)
            principal-type (some-> (:principal-type params) keyword)
            success (utils/parse-boolean-param params :success)
            options (merge pagination
                      {:principal-type principal-type
                       :success? success})
            events (login-monitoring/list-login-events db options)]
        (log/info "ADMIN LOGIN EVENTS: fetched events" {:count (count events)})
        (utils/json-response {:events events})))
    "Failed to retrieve login events"))

(defn routes
  "Login events route definitions"
  [db]
  [""
   ["" {:get (get-login-events-handler db)}]])
