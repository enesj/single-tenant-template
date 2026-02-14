(ns app.domain.backend.expenses.routes.articles
  "Admin API routes for articles plus mapping/alias helper endpoints used by the admin UX."
  (:require
    [app.domain.backend.expenses.routes.route-configs :as configs]
    [app.domain.backend.expenses.routes.routes-factory :as factory]
    [app.domain.backend.expenses.services.article-aliases :as aliases]
    [app.domain.backend.expenses.services.articles :as articles]
    [app.template.backend.routes.admin.utils :as utils]))

(defn routes
  [db]
  (let [config (-> configs/article-config
                 (factory/register-entity-routes!))
        [route-path list-route id-route] (factory/build-standard-routes db config)]
    ;; IMPORTANT: Specific routes must come BEFORE wildcard routes like "/:id".
    ;; Otherwise requests like "/unmapped-items" can be incorrectly matched by "/:id".
    [route-path
     list-route

     ["/unmapped-aliases"
      {:get (fn [request]
              ((utils/with-error-handling
                 (fn [req]
                   (let [qp (:query-params req)
                         supplier-id (utils/parse-uuid-custom
                                       (or (get qp "supplier-id")
                                         (get qp "supplier_id")
                                         (:supplier-id qp)
                                         (:supplier_id qp)))
                         limit (utils/parse-int-param qp :limit 50)
                         offset (utils/parse-int-param qp :offset 0)
                         rows (aliases/list-unmapped-aliases
                                db
                                (cond-> {:limit limit :offset offset}
                                  supplier-id (assoc :supplier-id supplier-id)))]
                     (utils/success-response {:unmapped-aliases (factory/to-app rows)})))
                 "Failed to list unmapped aliases")
               request))}]

     ["/aliases/:alias-id/map"
      {:post (fn [request]
               ((utils/with-error-handling
                  (fn [req]
                    (let [alias-id (utils/parse-uuid-custom (get-in req [:path-params :alias-id]))
                          body (factory/read-json-body req)
                          article-id (utils/parse-uuid-custom (or (:article-id body) (:article_id body)))]
                      (when-not alias-id
                        (throw (ex-info "Invalid alias-id" {:status 400})))
                      (when-not article-id
                        (throw (ex-info "Invalid article-id" {:status 400})))
                      (let [updated (aliases/map-alias-to-article! db alias-id article-id)]
                        (utils/success-response {:article-alias (factory/to-app updated)}))))
                  "Failed to map alias to article")
                request))}]

     ["/:id/aliases"
      {:post (fn [request]
               ((utils/with-error-handling
                  (fn [req]
                    (let [article-id (utils/parse-uuid-custom (get-in req [:path-params :id]))
                          body (factory/read-json-body req)
                          supplier-id (utils/parse-uuid-custom (or (:supplier-id body) (:supplier_id body)))
                          raw-labels (or (:raw-labels body) (:raw_labels body) (:raw-labels-text body) (:raw_labels_text body))
                          allow-reassign? (boolean (or (:allow-reassign? body) (:allow_reassign? body)))]
                      (when-not article-id
                        (throw (ex-info "Invalid article id" {:status 400})))
                      (when-not supplier-id
                        (throw (ex-info "Invalid supplier-id" {:status 400})))
                      (let [result (articles/batch-create-aliases!
                                     db
                                     {:supplier-id supplier-id
                                      :article-id article-id
                                      :raw-labels raw-labels
                                      :allow-reassign? allow-reassign?})]
                        (utils/success-response {:result (factory/to-app result)}))))
                  "Failed to batch-create article aliases")
                request))}]

     ["/:id/related-records"
      {:get (fn [request]
              ((utils/with-error-handling
                 (fn [req]
                   (let [article-id (utils/parse-uuid-custom (get-in req [:path-params :id]))
                         qp (:query-params req)
                         related-type (or (get qp "type") (:type qp))
                         limit (utils/parse-int-param qp :limit 100)]
                     (when-not article-id
                       (throw (ex-info "Invalid article id" {:status 400})))
                     (let [rows (articles/list-related-records
                                  db
                                  article-id
                                  {:type related-type
                                   :limit limit})]
                       (utils/success-response {:related-records (factory/to-app rows)}))))
                 "Failed to list article related records")
               request))}]

     id-route]))
