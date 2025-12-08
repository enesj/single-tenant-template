(ns app.domain.expenses.routes.articles
  "Admin API routes for articles, aliases, and price history."
  (:require
    [app.backend.routes.admin.utils :as utils]
    [app.domain.expenses.services.articles :as articles]
    [app.domain.expenses.services.price-history :as price-history]
    [app.shared.adapters.database :as db-adapter]))

(defn- to-app [data]
  (-> data
    db-adapter/convert-pg-objects
    db-adapter/convert-db-keys->app-keys))

(defn list-articles-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [qp (:query-params request)
            opts {:search (:search qp)
                  :limit (utils/parse-int-param qp :limit 50)
                  :offset (utils/parse-int-param qp :offset 0)
                  :order-by (keyword (or (:order-by qp) "canonical_name"))
                  :order-dir (keyword (or (:order-dir qp) "asc"))}
            rows (articles/list-articles db opts)]
        (utils/success-response {:articles (to-app rows)})))
    "Failed to list articles"))

(defn create-article-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [body (:body request)]
        (if-not (:canonical_name body)
          (utils/error-response "canonical_name is required" :status 400)
          (utils/success-response {:article (to-app (articles/create-article! db body))}))))
    "Failed to create article"))

(defn get-article-handler [db]
  (utils/with-error-handling
    (fn [request]
      (if-let [id (utils/parse-uuid-custom (get-in request [:path-params :id]))]
        (if-let [article (articles/get-article db id)]
          (utils/success-response {:article (to-app article)})
          (utils/error-response "Article not found" :status 404))
        (utils/error-response "Invalid id" :status 400)))
    "Failed to fetch article"))

(defn update-article-handler [db]
  (utils/with-error-handling
    (fn [request]
      (if-let [id (utils/parse-uuid-custom (get-in request [:path-params :id]))]
        (if-let [article (articles/update-article! db id (:body request))]
          (utils/success-response {:article (to-app article)})
          (utils/error-response "Article not found" :status 404))
        (utils/error-response "Invalid id" :status 400)))
    "Failed to update article"))

(defn delete-article-handler [db]
  (utils/with-error-handling
    (fn [request]
      (if-let [id (utils/parse-uuid-custom (get-in request [:path-params :id]))]
        (if (articles/delete-article! db id)
          (utils/success-response {:deleted true})
          (utils/error-response "Article not found" :status 404))
        (utils/error-response "Invalid id" :status 400)))
    "Failed to delete article"))

(defn create-alias-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [article-id (utils/parse-uuid-custom (get-in request [:path-params :id]))
            body (:body request)
            supplier-id (utils/parse-uuid-custom (:supplier_id body))
            raw-label (:raw_label body)
            confidence (:confidence body)]
        (cond
          (nil? article-id) (utils/error-response "Invalid article id" :status 400)
          (nil? supplier-id) (utils/error-response "supplier_id is required" :status 400)
          (nil? raw-label) (utils/error-response "raw_label is required" :status 400)
          :else
          (let [alias (articles/create-alias! db supplier-id raw-label article-id {:confidence confidence})]
            (utils/success-response {:alias (to-app alias)})))))
    "Failed to create alias"))

(defn list-unmapped-items-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [qp (:query-params request)
            limit (utils/parse-int-param qp :limit 50)
            offset (utils/parse-int-param qp :offset 0)
            rows (articles/list-unmapped-items db {:limit limit :offset offset})]
        (utils/success-response {:items (to-app rows)})))
    "Failed to list unmapped items"))

(defn map-item-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [item-id (utils/parse-uuid-custom (get-in request [:path-params :item-id]))
            body (:body request)
            article-id (utils/parse-uuid-custom (:article_id body))
            create-alias? (boolean (:create_alias? body))]
        (cond
          (nil? item-id) (utils/error-response "Invalid item id" :status 400)
          (nil? article-id) (utils/error-response "article_id is required" :status 400)
          :else
          (let [updated (articles/map-item-to-article! db item-id article-id {:create-alias? create-alias?})]
            (utils/success-response {:item (to-app updated)})))))
    "Failed to map item to article"))

(defn price-history-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [article-id (utils/parse-uuid-custom (get-in request [:path-params :id]))
            qp (:query-params request)
            supplier-id (utils/parse-uuid-custom (:supplier_id qp))
            limit (utils/parse-int-param qp :limit 50)]
        (if article-id
          (utils/success-response
            {:observations (to-app (price-history/get-price-history db article-id {:supplier-id supplier-id :limit limit}))})
          (utils/error-response "Invalid article id" :status 400))))
    "Failed to fetch price history"))

(defn latest-prices-handler [db]
  (utils/with-error-handling
    (fn [request]
      (if-let [article-id (utils/parse-uuid-custom (get-in request [:path-params :id]))]
        (utils/success-response
          {:prices (to-app (price-history/get-latest-prices db article-id))})
        (utils/error-response "Invalid article id" :status 400)))
    "Failed to fetch latest prices"))

(defn price-comparison-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [article-id (utils/parse-uuid-custom (get-in request [:path-params :id]))
            qp (:query-params request)
            from (:from qp)
            limit (utils/parse-int-param qp :limit 100)]
        (if article-id
          (utils/success-response
            {:observations (to-app (price-history/get-price-comparison db article-id {:from from :limit limit}))})
          (utils/error-response "Invalid article id" :status 400))))
    "Failed to fetch price comparison"))

(defn routes [db]
  ["/articles"
   ["" {:get (list-articles-handler db)
        :post (create-article-handler db)}]
   ["/unmapped-items" {:get (list-unmapped-items-handler db)}]
   ["/items/:item-id/map" {:post (map-item-handler db)}]
   ["/:id" {:get (get-article-handler db)
            :put (update-article-handler db)
            :delete (delete-article-handler db)}]
   ["/:id/aliases" {:post (create-alias-handler db)}]
   ["/:id/price-history" {:get (price-history-handler db)}]
   ["/:id/latest-prices" {:get (latest-prices-handler db)}]
   ["/:id/compare" {:get (price-comparison-handler db)}]])
