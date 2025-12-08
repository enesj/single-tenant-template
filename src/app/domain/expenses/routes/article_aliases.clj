(ns app.domain.expenses.routes.article-aliases
  "Admin API routes for article aliases."
  (:require
    [app.backend.routes.admin.utils :as utils]
    [app.domain.expenses.services.article-aliases :as aliases]
    [app.shared.adapters.database :as db-adapter]))

(defn- to-app [data]
  (-> data
    db-adapter/convert-pg-objects
    db-adapter/convert-db-keys->app-keys))

(defn list-aliases-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [qp (:query-params request)
            opts {:limit (utils/parse-int-param qp :limit 50)
                  :offset (utils/parse-int-param qp :offset 0)
                  :supplier-id (utils/parse-uuid-custom (:supplier_id qp))
                  :article-id (utils/parse-uuid-custom (:article_id qp))
                  :search (:search qp)
                  :order-by (keyword (or (:order-by qp) "created_at"))
                  :order-dir (keyword (or (:order-dir qp) "desc"))}
            rows (aliases/list-aliases db opts)]
        (utils/success-response {:article-aliases (to-app rows)})))
    "Failed to list article aliases"))

(defn create-alias-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [body (:body request)
            supplier-id (utils/parse-uuid-custom (:supplier_id body))
            article-id (utils/parse-uuid-custom (:article_id body))
            raw-label (:raw_label body)
            confidence (:confidence body)]
        (cond
          (nil? supplier-id) (utils/error-response "supplier_id is required" :status 400)
          (nil? article-id) (utils/error-response "article_id is required" :status 400)
          (nil? raw-label) (utils/error-response "raw_label is required" :status 400)
          :else
          (let [alias (aliases/create-alias! db {:supplier_id supplier-id
                                                 :article_id article-id
                                                 :raw_label raw-label
                                                 :confidence confidence})
                full (aliases/get-alias db (:id alias))]
            (utils/success-response {:article-alias (to-app (or full alias))})))))
    "Failed to create alias"))

(defn get-alias-handler [db]
  (utils/with-error-handling
    (fn [request]
      (if-let [id (utils/parse-uuid-custom (get-in request [:path-params :id]))]
        (if-let [alias (aliases/get-alias db id)]
          (utils/success-response {:article-alias (to-app alias)})
          (utils/error-response "Alias not found" :status 404))
        (utils/error-response "Invalid id" :status 400)))
    "Failed to fetch alias"))

(defn update-alias-handler [db]
  (utils/with-error-handling
    (fn [request]
      (if-let [id (utils/parse-uuid-custom (get-in request [:path-params :id]))]
        (if-let [alias (aliases/update-alias! db id (:body request))]
          (utils/success-response {:article-alias (to-app alias)})
          (utils/error-response "Alias not found" :status 404))
        (utils/error-response "Invalid id" :status 400)))
    "Failed to update alias"))

(defn delete-alias-handler [db]
  (utils/with-error-handling
    (fn [request]
      (if-let [id (utils/parse-uuid-custom (get-in request [:path-params :id]))]
        (if (aliases/delete-alias! db id)
          (utils/success-response {:deleted true})
          (utils/error-response "Alias not found" :status 404))
        (utils/error-response "Invalid id" :status 400)))
    "Failed to delete alias"))

(defn routes [db]
  ["/article-aliases"
   ["" {:get (list-aliases-handler db)
        :post (create-alias-handler db)}]
   ["/:id" {:get (get-alias-handler db)
            :put (update-alias-handler db)
            :delete (delete-alias-handler db)}]])
