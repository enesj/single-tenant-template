(ns app.domain.backend.expenses.routes.article-aliases
  "Admin API routes for article aliases."
  (:require
    [app.domain.backend.expenses.routes.route-configs :as configs]
    [app.domain.backend.expenses.routes.routes-factory :as factory]
    [app.domain.backend.expenses.services.article-aliases :as article-aliases]
    [app.template.backend.routes.admin.utils :as utils]))

(defn routes [db]
  (let [config (-> configs/article-alias-config
                 (factory/register-entity-routes!))
        base-routes (factory/build-standard-routes db config)]
    (conj base-routes
      ["/:id/related-records"
       {:get (fn [request]
               ((utils/with-error-handling
                  (fn [req]
                    (let [alias-id (utils/parse-uuid-custom (get-in req [:path-params :id]))
                          qp (:query-params req)
                          related-type (or (get qp "type") (:type qp))
                          limit (utils/parse-int-param qp :limit 100)]
                      (when-not alias-id
                        (throw (ex-info "Invalid article alias id" {:status 400})))
                      (let [rows (article-aliases/list-related-records
                                   db alias-id
                                   {:type related-type :limit limit})]
                        (utils/success-response {:related-records (factory/to-app rows)}))))
                  "Failed to list article alias related records")
                request))}])))
