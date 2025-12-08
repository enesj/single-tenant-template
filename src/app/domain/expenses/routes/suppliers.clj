(ns app.domain.expenses.routes.suppliers
  "Admin API routes for expense suppliers."
  (:require
    [app.backend.routes.admin.utils :as utils]
    [app.domain.expenses.services.suppliers :as suppliers]
    [app.shared.adapters.database :as db-adapter]))

(defn- to-app
  "Convert DB rows to API-friendly maps."
  [data]
  (-> data
      db-adapter/convert-pg-objects
      db-adapter/convert-db-keys->app-keys))

(defn list-suppliers-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [qp (:query-params request)
            opts {:search (:search qp)
                  :limit (utils/parse-int-param qp :limit 100)
                  :offset (utils/parse-int-param qp :offset 0)
                  :order-by (keyword (or (:order-by qp) "display_name"))
                  :order-dir (keyword (or (:order-dir qp) "asc"))}
            results (suppliers/list-suppliers db opts)]
        (utils/success-response {:suppliers (to-app results)})))
    "Failed to list suppliers"))

(defn create-supplier-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [body (:body request)]
        (if-not (:display_name body)
          (utils/error-response "display_name is required" :status 400)
          (let [supplier (suppliers/create-supplier! db body)]
            (utils/success-response {:supplier (to-app supplier)})))))
    "Failed to create supplier"))

(defn get-supplier-handler [db]
  (utils/with-error-handling
    (fn [request]
      (if-let [id (utils/parse-uuid-custom (get-in request [:path-params :id]))]
        (if-let [supplier (suppliers/get-supplier db id)]
          (utils/success-response {:supplier (to-app supplier)})
          (utils/error-response "Supplier not found" :status 404))
        (utils/error-response "Invalid id" :status 400)))
    "Failed to fetch supplier"))

(defn update-supplier-handler [db]
  (utils/with-error-handling
    (fn [request]
      (if-let [id (utils/parse-uuid-custom (get-in request [:path-params :id]))]
        (let [updated (suppliers/update-supplier! db id (:body request))]
          (if updated
            (utils/success-response {:supplier (to-app updated)})
            (utils/error-response "Supplier not found" :status 404)))
        (utils/error-response "Invalid id" :status 400)))
    "Failed to update supplier"))

(defn delete-supplier-handler [db]
  (utils/with-error-handling
    (fn [request]
      (if-let [id (utils/parse-uuid-custom (get-in request [:path-params :id]))]
        (if (suppliers/delete-supplier! db id)
          (utils/success-response {:deleted true})
          (utils/error-response "Supplier not found or in use" :status 404))
        (utils/error-response "Invalid id" :status 400)))
    "Failed to delete supplier"))

(defn search-suppliers-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [qp (:query-params request)
            query (:q qp)
            limit (utils/parse-int-param qp :limit 10)
            results (suppliers/search-suppliers db query {:limit limit})]
        (utils/success-response {:suppliers (to-app results)})))
    "Failed to search suppliers"))

(defn count-suppliers-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [search (get-in request [:query-params :search])
            total (suppliers/count-suppliers db search)]
        (utils/success-response {:total total})))
    "Failed to count suppliers"))

(defn routes [db]
  ["/suppliers"
   ["" {:get (list-suppliers-handler db)
        :post (create-supplier-handler db)}]
   ["/count" {:get (count-suppliers-handler db)}]
   ["/search" {:get (search-suppliers-handler db)}]
   ["/:id" {:get (get-supplier-handler db)
            :put (update-supplier-handler db)
            :delete (delete-supplier-handler db)}]])
