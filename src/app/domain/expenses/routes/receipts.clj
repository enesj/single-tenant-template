(ns app.domain.expenses.routes.receipts
  "Admin API routes for receipt ingestion and approval."
  (:require
    [app.backend.routes.admin.utils :as utils]
    [app.domain.expenses.services.receipts :as receipts]
    [app.shared.adapters.database :as db-adapter]
    [clojure.string :as str]))

(defn- to-app [data]
  (-> data
      db-adapter/convert-pg-objects
      db-adapter/convert-db-keys->app-keys))

(defn- parse-status-param [status-param]
  (cond
    (vector? status-param) status-param
    (seq? status-param) (vec status-param)
    (string? status-param)
    (let [s (str/trim status-param)]
      (if (str/includes? s ",")
        (->> (str/split s #",") (map str/trim) (remove str/blank?) vec)
        s))
    :else nil))

(defn list-receipts-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [qp (:query-params request)
            status (parse-status-param (:status qp))
            opts {:status status
                  :limit (utils/parse-int-param qp :limit 50)
                  :offset (utils/parse-int-param qp :offset 0)
                  :order-dir (keyword (or (:order-dir qp) "desc"))}
            results (receipts/list-receipts db opts)]
        (utils/success-response {:receipts (to-app results)})))
    "Failed to list receipts"))

(defn list-pending-handler [db]
  (utils/with-error-handling
    (fn [_]
      (utils/success-response
        {:receipts (to-app (receipts/list-pending-for-processing db))}))
    "Failed to list pending receipts"))

(defn upload-receipt-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [body (:body request)
            {:keys [storage_key file_hash bytes]} body]
        (cond
          (nil? storage_key) (utils/error-response "storage_key is required" :status 400)
          (and (nil? file_hash) (nil? bytes)) (utils/error-response "file_hash or bytes is required" :status 400)
          :else
          (let [result (receipts/upload-receipt! db body)]
            (utils/success-response
              (-> result
                  (update :receipt to-app)))))))
    "Failed to upload receipt"))

(defn get-receipt-handler [db]
  (utils/with-error-handling
    (fn [request]
      (if-let [id (utils/parse-uuid-custom (get-in request [:path-params :id]))]
        (if-let [receipt (receipts/get-receipt db id)]
          (utils/success-response {:receipt (to-app receipt)})
          (utils/error-response "Receipt not found" :status 404))
        (utils/error-response "Invalid id" :status 400)))
    "Failed to fetch receipt"))

(defn update-status-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [new-status (or (get-in request [:body :status])
                           (get-in request [:body :new_status]))]
        (if-let [id (utils/parse-uuid-custom (get-in request [:path-params :id]))]
          (if-not new-status
            (utils/error-response "status is required" :status 400)
            (utils/success-response
              {:receipt (to-app (receipts/update-status! db id new-status))}))
          (utils/error-response "Invalid id" :status 400))))
    "Failed to update receipt status"))

(defn retry-receipt-handler [db]
  (utils/with-error-handling
    (fn [request]
      (if-let [id (utils/parse-uuid-custom (get-in request [:path-params :id]))]
        (utils/success-response
          {:receipt (to-app (receipts/retry-extraction! db id))})
        (utils/error-response "Invalid id" :status 400)))
    "Failed to retry receipt"))

(defn fail-receipt-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [body (:body request)
            message (:message body)
            details (:details body)]
        (if-let [id (utils/parse-uuid-custom (get-in request [:path-params :id]))]
          (if-not message
            (utils/error-response "message is required" :status 400)
            (utils/success-response
              {:receipt (to-app (receipts/mark-failed! db id message details))}))
          (utils/error-response "Invalid id" :status 400))))
    "Failed to mark receipt failed"))

(defn save-extraction-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [body (:body request)]
        (if-let [id (utils/parse-uuid-custom (get-in request [:path-params :id]))]
          (utils/success-response
            {:receipt (to-app (receipts/store-extraction-results! db id body))})
          (utils/error-response "Invalid id" :status 400))))
    "Failed to store extraction results"))

(defn approve-and-post-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [body (:body request)]
        (if-let [id (utils/parse-uuid-custom (get-in request [:path-params :id]))]
          (let [expense (receipts/approve-and-post! db id body)]
            (utils/success-response {:expense (to-app expense)}))
          (utils/error-response "Invalid id" :status 400))))
    "Failed to approve receipt"))

(defn routes [db]
  ["/receipts"
   ["" {:get (list-receipts-handler db)
        :post (upload-receipt-handler db)}]
   ["/pending" {:get (list-pending-handler db)}]
   ["/:id" {:get (get-receipt-handler db)}]
   ["/:id/status" {:post (update-status-handler db)}]
   ["/:id/retry" {:post (retry-receipt-handler db)}]
   ["/:id/fail" {:post (fail-receipt-handler db)}]
   ["/:id/extraction" {:post (save-extraction-handler db)}]
   ["/:id/approve" {:post (approve-and-post-handler db)}]])
