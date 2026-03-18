(ns app.domain.backend.expenses.routes.stores
  "Admin API routes for stores (supplier locations)."
  (:require
    [app.domain.backend.expenses.routes.route-configs :as configs]
    [app.domain.backend.expenses.routes.routes-factory :as factory]
    [app.domain.backend.expenses.services.stores.related :as stores-related]
    [app.template.backend.routes.admin.utils :as utils]))

(defn routes [db]
  (let [config (-> configs/store-config
                 (factory/register-entity-routes!))
        base-routes (factory/build-extended-routes db config)]
    (into base-routes
      [["/:id/related-records"
        {:get (fn [request]
                ((utils/with-error-handling
                   (fn [req]
                     (let [store-id (utils/parse-uuid-custom (get-in req [:path-params :id]))
                           qp (:query-params req)
                           related-type (or (get qp "type") (:type qp))
                           limit (utils/parse-int-param qp :limit 100)]
                       (when-not store-id
                         (throw (ex-info "Invalid store id" {:status 400})))
                       (let [rows (stores-related/list-related-records
                                    db store-id
                                    {:type related-type :limit limit})]
                         (utils/success-response {:related-records (factory/to-app rows)}))))
                   "Failed to list store related records")
                 request))}]
       ["/:id/related-records/counts"
        {:get (fn [request]
                ((utils/with-error-handling
                   (fn [req]
                     (let [store-id (utils/parse-uuid-custom (get-in req [:path-params :id]))]
                       (when-not store-id
                         (throw (ex-info "Invalid store id" {:status 400})))
                       (utils/success-response {:counts (stores-related/count-all-related db store-id)})))
                   "Failed to count store related records")
                 request))}]])))
