(ns app.domain.expenses.routes.payers
  "Admin API routes for payers (payment sources)."
  (:require
    [app.backend.routes.admin.utils :as utils]
    [app.domain.expenses.services.payers :as payers]
    [app.shared.adapters.database :as db-adapter]))

(defn- to-app [data]
  (-> data
      db-adapter/convert-pg-objects
      db-adapter/convert-db-keys->app-keys))

(defn list-payers-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [qp (:query-params request)
            opts {:type (:type qp)}
            results (payers/list-payers db opts)]
        (utils/success-response {:payers (to-app results)})))
    "Failed to list payers"))

(defn create-payer-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [body (:body request)
            data (update body :type #(when % (name %)))]
        (cond
          (nil? (:type data)) (utils/error-response "type is required" :status 400)
          (nil? (:label data)) (utils/error-response "label is required" :status 400)
          :else
          (let [payer (payers/create-payer! db data)]
            (utils/success-response {:payer (to-app payer)})))))
    "Failed to create payer"))

(defn get-payer-handler [db]
  (utils/with-error-handling
    (fn [request]
      (if-let [id (utils/parse-uuid-custom (get-in request [:path-params :id]))]
        (if-let [payer (payers/get-payer db id)]
          (utils/success-response {:payer (to-app payer)})
          (utils/error-response "Payer not found" :status 404))
        (utils/error-response "Invalid id" :status 400)))
    "Failed to fetch payer"))

(defn update-payer-handler [db]
  (utils/with-error-handling
    (fn [request]
      (if-let [id (utils/parse-uuid-custom (get-in request [:path-params :id]))]
        (let [updates (update (:body request) :type #(when % (name %)))
              updated (payers/update-payer! db id updates)]
          (if updated
            (utils/success-response {:payer (to-app updated)})
            (utils/error-response "Payer not found" :status 404)))
        (utils/error-response "Invalid id" :status 400)))
    "Failed to update payer"))

(defn delete-payer-handler [db]
  (utils/with-error-handling
    (fn [request]
      (if-let [id (utils/parse-uuid-custom (get-in request [:path-params :id]))]
        (if (payers/delete-payer! db id)
          (utils/success-response {:deleted true})
          (utils/error-response "Payer not found or in use" :status 404))
        (utils/error-response "Invalid id" :status 400)))
    "Failed to delete payer"))

(defn set-default-handler [db]
  (utils/with-error-handling
    (fn [request]
      (if-let [id (utils/parse-uuid-custom (get-in request [:path-params :id]))]
        (let [updated (payers/set-default-payer! db id)]
          (utils/success-response {:payer (to-app updated)}))
        (utils/error-response "Invalid id" :status 400)))
    "Failed to set default payer"))

(defn get-default-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [type (or (get-in request [:path-params :type])
                     (get-in request [:query-params :type]))]
        (if type
          (if-let [payer (payers/get-default-payer db type)]
            (utils/success-response {:payer (to-app payer)})
            (utils/success-response {:payer nil}))
          (utils/error-response "type is required" :status 400))))
    "Failed to fetch default payer"))

(defn suggest-payer-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [qp (:query-params request)
            hints {:method (:method qp)
                   :card_last4 (:card_last4 qp)}
            suggestion (payers/suggest-payer db hints)]
        (utils/success-response {:payer (to-app suggestion)})))
    "Failed to suggest payer"))

(defn count-payers-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [type (get-in request [:query-params :type])
            total (payers/count-payers db type)]
        (utils/success-response {:total total})))
    "Failed to count payers"))

(defn routes [db]
  ["/payers"
   ["" {:get (list-payers-handler db)
        :post (create-payer-handler db)}]
   ["/count" {:get (count-payers-handler db)}]
   ["/suggest" {:get (suggest-payer-handler db)}]
   ["/default/:type" {:get (get-default-handler db)}]
   ["/:id" {:get (get-payer-handler db)
            :put (update-payer-handler db)
            :delete (delete-payer-handler db)}]
   ["/:id/default" {:post (set-default-handler db)}]])
