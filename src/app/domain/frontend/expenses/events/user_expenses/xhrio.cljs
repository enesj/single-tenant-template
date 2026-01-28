(ns app.domain.frontend.expenses.events.user-expenses.xhrio
  "HTTP request helper for user-expenses (user API only)."
  (:require
    [app.template.frontend.api.http :as http]))

(defn xhrio
  [db {:keys [method uri params body format response-format headers timeout on-success on-failure]}]
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
    (http/api-request req)))
