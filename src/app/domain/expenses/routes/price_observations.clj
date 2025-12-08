(ns app.domain.expenses.routes.price-observations
  "Admin API routes for price observations."
  (:require
    [app.backend.routes.admin.utils :as utils]
    [app.domain.expenses.services.price-observations :as price-observations]
    [app.shared.adapters.database :as db-adapter]))

(defn- to-app [data]
  (-> data
    db-adapter/convert-pg-objects
    db-adapter/convert-db-keys->app-keys))

(defn list-price-observations-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [qp (:query-params request)
            opts {:limit (utils/parse-int-param qp :limit 50)
                  :offset (utils/parse-int-param qp :offset 0)
                  :article-id (utils/parse-uuid-custom (:article_id qp))
                  :supplier-id (utils/parse-uuid-custom (:supplier_id qp))
                  :order-by (keyword (or (:order-by qp) "observed_at"))
                  :order-dir (keyword (or (:order-dir qp) "desc"))}
            rows (price-observations/list-price-observations db opts)]
        (utils/success-response {:price-observations (to-app rows)})))
    "Failed to list price observations"))

(defn create-price-observation-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [body (:body request)
            article-id (utils/parse-uuid-custom (:article_id body))
            supplier-id (utils/parse-uuid-custom (:supplier_id body))
            expense-item-id (utils/parse-uuid-custom (:expense_item_id body))
            observed-at (:observed_at body)
            qty (:qty body)
            unit-price (:unit_price body)
            line-total (:line_total body)
            currency (:currency body)]
        (cond
          (nil? article-id) (utils/error-response "article_id is required" :status 400)
          (nil? supplier-id) (utils/error-response "supplier_id is required" :status 400)
          (nil? line-total) (utils/error-response "line_total is required" :status 400)
          :else
          (let [obs (price-observations/create-price-observation!
                      db {:article_id article-id
                          :supplier_id supplier-id
                          :expense_item_id expense-item-id
                          :observed_at observed-at
                          :qty qty
                          :unit_price unit-price
                          :line_total line-total
                          :currency currency})
                full (price-observations/get-price-observation db (:id obs))]
            (utils/success-response {:price-observation (to-app (or full obs))})))))
    "Failed to create price observation"))

(defn get-price-observation-handler [db]
  (utils/with-error-handling
    (fn [request]
      (if-let [id (utils/parse-uuid-custom (get-in request [:path-params :id]))]
        (if-let [obs (price-observations/get-price-observation db id)]
          (utils/success-response {:price-observation (to-app obs)})
          (utils/error-response "Price observation not found" :status 404))
        (utils/error-response "Invalid id" :status 400)))
    "Failed to fetch price observation"))

(defn update-price-observation-handler [db]
  (utils/with-error-handling
    (fn [request]
      (if-let [id (utils/parse-uuid-custom (get-in request [:path-params :id]))]
        (if-let [obs (price-observations/update-price-observation! db id (:body request))]
          (utils/success-response {:price-observation (to-app obs)})
          (utils/error-response "Price observation not found" :status 404))
        (utils/error-response "Invalid id" :status 400)))
    "Failed to update price observation"))

(defn delete-price-observation-handler [db]
  (utils/with-error-handling
    (fn [request]
      (if-let [id (utils/parse-uuid-custom (get-in request [:path-params :id]))]
        (if (price-observations/delete-price-observation! db id)
          (utils/success-response {:deleted true})
          (utils/error-response "Price observation not found" :status 404))
        (utils/error-response "Invalid id" :status 400)))
    "Failed to delete price observation"))

(defn routes [db]
  ["/price-observations"
   ["" {:get (list-price-observations-handler db)
        :post (create-price-observation-handler db)}]
   ["/:id" {:get (get-price-observation-handler db)
            :put (update-price-observation-handler db)
            :delete (delete-price-observation-handler db)}]])
