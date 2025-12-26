(ns app.domain.frontend.expenses.events.user-expenses.xhrio
  "HTTP request helper for user-expenses that switches between user and admin endpoints."
  (:require
    [app.admin.frontend.adapters.core :as admin-core]
    [app.admin.frontend.utils.http :as admin-http]
    [app.template.frontend.api.http :as http]))

(defn admin-context?
  [db]
  (admin-core/admin-context? db))

(defn xhrio
  "Build an :http-xhrio config that uses admin endpoints when the runtime indicates
  an admin UI context (typically /admin routes)."
  [db {:keys [method uri admin-uri params body format response-format headers timeout on-success on-failure]}]
  (let [req (cond-> {:method method
                     :uri uri
                     :on-success on-success
                     :on-failure on-failure}
              (some? params) (assoc :params params)
              (some? body) (assoc :body body)
              format (assoc :format format)
              response-format (assoc :response-format response-format)
              headers (assoc :headers headers)
              timeout (assoc :timeout timeout))]
    (if (admin-context? db)
      (admin-http/admin-request (assoc req :uri admin-uri))
      (http/api-request req))))
