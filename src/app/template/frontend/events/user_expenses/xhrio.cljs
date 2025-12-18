(ns app.template.frontend.events.user-expenses.xhrio
  (:require
    [app.admin.frontend.adapters.core :as admin-core]
    [app.admin.frontend.utils.http :as admin-http]
    [app.template.frontend.api.http :as http]))

(defn admin-context?
  [db]
  (some? (admin-core/admin-token db)))

(defn xhrio
  "Build an :http-xhrio config that uses admin endpoints when an admin token exists."
  [db {:keys [method uri admin-uri params on-success on-failure]}]
  (let [req {:method method
             :uri uri
             :params params
             :on-success on-success
             :on-failure on-failure}]
    (if (admin-context? db)
      (admin-http/admin-request (assoc req :uri admin-uri))
      (http/api-request req))))

