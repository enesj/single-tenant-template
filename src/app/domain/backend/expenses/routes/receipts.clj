(ns app.domain.backend.expenses.routes.receipts
  "Admin API routes for receipt ingestion and approval."
  (:require
    [app.domain.backend.expenses.integrations.mistral-ocr :as mistral-ocr]
    [app.domain.backend.expenses.services.receipts :as receipts]
    [app.domain.backend.expenses.services.suppliers :as suppliers]
    [app.domain.backend.expenses.workers.receipt-ocr :as receipt-ocr]
    [app.template.backend.routes.admin.utils :as utils]
    [app.template.backend.utils.adapters.database :as db-adapter]
    [clojure.string :as str]
    [taoensso.timbre :as log]))

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

(defn- parse-money [v]
  (cond
    (nil? v) nil
    (instance? java.math.BigDecimal v) v
    (number? v) (bigdec v)
    (string? v)
    (let [s (-> v
              str/trim
              ;; keep digits/decimal separators/minus
              (str/replace #"[^0-9,\.\-]" ""))
          s (cond
              ;; both separators present → treat commas as thousands
              (and (str/includes? s ",") (str/includes? s ".")) (str/replace s "," "")
              ;; only comma present → treat comma as decimal separator
              (str/includes? s ",") (str/replace s "," ".")
              :else s)]
      (try
        (bigdec s)
        (catch Exception _ nil)))
    :else nil))

(defn- lines-total-amount-guess
  "Sum extracted line totals (from raw_extract_json.extraction.items[].line_total).

  Returns a BigDecimal or nil when no parseable line totals exist."
  [receipt]
  (let [items (get-in receipt [:raw-extract-json :extraction :items])]
    (when (seq items)
      (let [{:keys [sum count]}
            (reduce
              (fn [{:keys [sum count]} item]
                (if-let [line-total (parse-money (:line-total item))]
                  {:sum (+ sum line-total) :count (inc count)}
                  {:sum sum :count count}))
              {:sum 0M :count 0}
              items)]
        (when (pos? (long count))
          sum)))))

(defn- enrich-receipt-for-detail
  [db receipt]
  (let [supplier-guess (some-> (:supplier-guess receipt) str/trim not-empty)
        normalized-key (when supplier-guess
                         (suppliers/normalize-supplier-key supplier-guess))
        supplier (when normalized-key
                   (suppliers/find-by-normalized-key db normalized-key))
        supplier-app (some-> supplier to-app (select-keys [:id :display-name :normalized-key]))
        lines-total (lines-total-amount-guess receipt)
        total (:total-amount-guess receipt)
        total-equals-lines? (when (and (some? total) (some? lines-total))
                              (zero? (compare total lines-total)))]
    (cond-> (assoc receipt :supplier-guess-has-supplier? (boolean supplier))
      supplier-app (assoc :supplier-guess-supplier supplier-app)
      (some? lines-total) (assoc :lines-total-amount-guess lines-total)
      (some? total-equals-lines?) (assoc :total-guess-equals-lines-total-guess? total-equals-lines?))))

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
          (utils/success-response {:receipt (->> receipt to-app (enrich-receipt-for-detail db))})
          (utils/error-response "Receipt not found" :status 404))
        (utils/error-response "Invalid id" :status 400)))
    "Failed to fetch receipt"))

(defn delete-receipt-handler [db]
  (utils/with-error-handling
    (fn [request]
      (if-let [id (utils/parse-uuid-custom (get-in request [:path-params :id]))]
        (if-let [deleted (receipts/delete-receipt! db id)]
          ;; Return JSON to keep the frontend XHR pipeline happy (empty bodies can fail JSON parsing).
          (utils/success-response {:deleted true
                                   :receipt (to-app deleted)})
          (utils/error-response "Receipt not found" :status 404))
        (utils/error-response "Invalid id" :status 400)))
    "Failed to delete receipt"))

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
          (let [expense (receipts/approve-and-post! db id body)
                receipt (receipts/get-receipt db id)]
            (utils/success-response {:expense (to-app expense)
                                     :receipt (to-app receipt)}))
          (utils/error-response "Invalid id" :status 400))))
    "Failed to approve receipt"))

;; ---------------------------------------------------------------------------
;; OCR Handlers (UI-triggered)
;; ---------------------------------------------------------------------------

(defn ocr-single-receipt-handler
  "Trigger OCR for a single receipt (POST /:id/ocr).
  Resets the receipt and processes it asynchronously."
  [db app-config]
  (utils/with-error-handling
    (fn [request]
      (if-let [id (utils/parse-uuid-custom (get-in request [:path-params :id]))]
        (if-let [_receipt (receipts/get-receipt db id)]
          (let [{:keys [enabled? api-key]} (mistral-ocr/build-config app-config)]
            (cond
              (not enabled?)
              (utils/error-response "Receipt OCR is disabled (set MISTRAL_OCR_ENABLED=true to enable)" :status 409)

              (not (seq api-key))
              (utils/error-response "Receipt OCR is not configured (missing MISTRAL_API_KEY)" :status 409)

              :else
              (do
                ;; Run OCR asynchronously
                (future
                  (try
                    (log/info "Starting OCR for receipt" {:receipt-id id :source :admin-ui})
                    (receipt-ocr/process-receipts-by-ids! db app-config [id])
                    (log/info "Completed OCR for receipt" {:receipt-id id :source :admin-ui})
                    (catch Exception e
                      (log/error e "OCR failed for receipt" {:receipt-id id}))))
                ;; Return immediately with 202 Accepted
                (utils/json-response {:success true
                                      :data {:queued true
                                             :receipt_ids [(str id)]}}
                  :status 202))))
          (utils/error-response "Receipt not found" :status 404))
        (utils/error-response "Invalid id" :status 400)))
    "Failed to trigger OCR"))

(defn- parse-receipt-ids
  "Parse receipt_ids from request body. Accepts :receipt_ids or :receipt-ids."
  [body]
  (let [ids (or (:receipt_ids body) (:receipt-ids body))]
    (when (sequential? ids)
      (->> ids
        (map utils/parse-uuid-custom)
        (filter some?)
        vec))))

(defn ocr-batch-receipts-handler
  "Trigger OCR for multiple receipts (POST /ocr).
  Body: {:receipt_ids [...]}"
  [db app-config]
  (utils/with-error-handling
    (fn [request]
      (let [body (:body request)
            receipt-ids (parse-receipt-ids body)]
        (if (empty? receipt-ids)
          (utils/error-response "receipt_ids is required and must be a non-empty array" :status 400)
          (let [{:keys [enabled? api-key]} (mistral-ocr/build-config app-config)]
            (cond
              (not enabled?)
              (utils/error-response "Receipt OCR is disabled (set MISTRAL_OCR_ENABLED=true to enable)" :status 409)

              (not (seq api-key))
              (utils/error-response "Receipt OCR is not configured (missing MISTRAL_API_KEY)" :status 409)

              :else
              (do
                ;; Run OCR asynchronously
                (future
                  (try
                    (log/info "Starting batch OCR" {:receipt-ids receipt-ids :source :admin-ui})
                    (receipt-ocr/process-receipts-by-ids! db app-config receipt-ids)
                    (log/info "Completed batch OCR" {:receipt-ids receipt-ids :source :admin-ui})
                    (catch Exception e
                      (log/error e "Batch OCR failed" {:receipt-ids receipt-ids}))))
                ;; Return immediately with 202 Accepted
                (utils/json-response {:success true
                                      :data {:queued true
                                             :receipt_ids (mapv str receipt-ids)}}
                  :status 202)))))))
    "Failed to trigger batch OCR"))

(defn routes
  "Admin receipts routes. Mounted under /admin/api/expenses/receipts."
  [db & [app-config]]
  ["/receipts"
   ["" {:get (list-receipts-handler db)
        :post (upload-receipt-handler db)}]
   ["/pending" {:get (list-pending-handler db)}]
   ["/ocr" {:post (ocr-batch-receipts-handler db app-config)}]
   ["/:id" {:get (get-receipt-handler db)
            :delete (delete-receipt-handler db)}]
   ["/:id/status" {:post (update-status-handler db)}]
   ["/:id/retry" {:post (retry-receipt-handler db)}]
   ["/:id/fail" {:post (fail-receipt-handler db)}]
   ["/:id/extraction" {:post (save-extraction-handler db)}]
   ["/:id/approve" {:post (approve-and-post-handler db)}]
   ["/:id/ocr" {:post (ocr-single-receipt-handler db app-config)}]])
