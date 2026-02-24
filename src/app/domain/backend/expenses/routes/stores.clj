(ns app.domain.backend.expenses.routes.stores
  "Admin API routes for stores (supplier locations)."
  (:require
    [app.domain.backend.expenses.routes.route-configs :as configs]
    [app.domain.backend.expenses.routes.routes-factory :as factory]
    [app.domain.backend.expenses.services.stores :as stores]
    [app.template.backend.routes.admin.utils :as utils]))

(defn routes [db]
  (let [config (-> configs/store-config
                 (factory/register-entity-routes!))
        base-routes (factory/build-extended-routes db config)]
    (conj base-routes
      ["/:id/related-records"
       {:get (fn [request]
               ((utils/with-error-handling
                  (fn [req]
                    (let [store-id (utils/parse-uuid-custom (get-in req [:path-params :id]))
                          qp (:query-params req)
                          related-type (or (get qp "type") (:type qp))
                          limit (utils/parse-int-param qp :limit 100)]
                      (when-not store-id
                        (throw (ex-info "Invalid store id" {:status 400})))
                      (let [rows (stores/list-related-records
                                   db store-id
                                   {:type related-type :limit limit})]
                        (utils/success-response {:related-records (factory/to-app rows)}))))
                  "Failed to list store related records")
                request))}])))
