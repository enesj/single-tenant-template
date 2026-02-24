(ns app.domain.backend.expenses.routes.manufacturers
  "Admin API routes for manufacturers."
  (:require
    [app.domain.backend.expenses.routes.route-configs :as configs]
    [app.domain.backend.expenses.routes.routes-factory :as factory]
    [app.domain.backend.expenses.services.manufacturers :as manufacturers]
    [app.template.backend.routes.admin.utils :as utils]))

(defn routes [db]
  (let [config (-> configs/manufacturer-config
                 (factory/register-entity-routes!))
        base-routes (factory/build-extended-routes db config)]
    (conj base-routes
      ["/:id/related-records"
       {:get (fn [request]
               ((utils/with-error-handling
                  (fn [req]
                    (let [manufacturer-id (utils/parse-uuid-custom (get-in req [:path-params :id]))
                          qp (:query-params req)
                          related-type (or (get qp "type") (:type qp))
                          limit (utils/parse-int-param qp :limit 100)]
                      (when-not manufacturer-id
                        (throw (ex-info "Invalid manufacturer id" {:status 400})))
                      (let [rows (manufacturers/list-related-records
                                   db manufacturer-id
                                   {:type related-type :limit limit})]
                        (utils/success-response {:related-records (factory/to-app rows)}))))
                  "Failed to list manufacturer related records")
                request))}])))
