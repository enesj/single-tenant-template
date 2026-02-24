(ns app.domain.backend.expenses.routes.suppliers
  "Admin API routes for expense suppliers."
  (:require
    [app.domain.backend.expenses.routes.route-configs :as configs]
    [app.domain.backend.expenses.routes.routes-factory :as factory]
    [app.domain.backend.expenses.services.suppliers :as suppliers]
    [app.template.backend.routes.admin.utils :as utils]))

(defn routes [db]
  (let [config (-> configs/supplier-config
                 (factory/register-entity-routes!))
        base-routes (factory/build-extended-routes db config)]
    (conj base-routes
      ["/:id/related-records"
       {:get (fn [request]
               ((utils/with-error-handling
                  (fn [req]
                    (let [supplier-id (utils/parse-uuid-custom (get-in req [:path-params :id]))
                          qp (:query-params req)
                          related-type (or (get qp "type") (:type qp))
                          limit (utils/parse-int-param qp :limit 100)]
                      (when-not supplier-id
                        (throw (ex-info "Invalid supplier id" {:status 400})))
                      (let [rows (suppliers/list-related-records
                                   db supplier-id
                                   {:type related-type :limit limit})]
                        (utils/success-response {:related-records (factory/to-app rows)}))))
                  "Failed to list supplier related records")
                request))}])))
