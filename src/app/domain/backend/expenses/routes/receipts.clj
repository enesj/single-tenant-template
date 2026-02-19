(ns app.domain.backend.expenses.routes.receipts
  "Admin API routes for receipt ingestion and approval."
  (:require
    [app.domain.backend.expenses.integrations.ocr-provider :as ocr-provider]
    [app.domain.backend.expenses.services.receipts.approval :as receipt-approval]
    [app.domain.backend.expenses.services.receipts.image-preprocess :as image-preprocess]
    [app.domain.backend.expenses.services.receipts.queries :as receipt-queries]
    [app.domain.backend.expenses.services.receipts.status :as receipt-status]
    [app.domain.backend.expenses.services.receipts.storage :as receipt-storage]
    [app.domain.backend.expenses.services.suppliers :as suppliers]
    [app.domain.backend.expenses.workers.receipt-ocr.core :as receipt-ocr]
    [app.template.backend.routes.admin.utils :as utils]
    [app.shared.adapters.database :as shared-db]
    [clojure.string :as str]
    [ring.util.response :as response]))

(def ^:private to-app shared-db/to-app)

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
                (if-let [line-total (parse-money (or (:line-total item) (:line_total item)))]
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
        abs-dec (fn [d] (if (neg? d) (- d) d))
        total-equals-lines? (when (and (some? total) (some? lines-total))
                              (<= (abs-dec (- total lines-total)) 0.01M))
        effective-status (let [status (:status receipt)]
                           (if (and (= "extracted" status)
                                 (false? total-equals-lines?))
                             "review_required"
                             status))]
    (cond-> (assoc receipt
              :supplier-guess-has-supplier? (boolean supplier)
              :status effective-status)
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
                  :order-dir (keyword (or (:order-dir qp) "desc"))
                  :order-by (or (:order-by qp) (get qp "order-by"))}
            results (receipt-queries/list-receipts db opts)]
        (utils/success-response {:receipts (to-app results)})))
    "Failed to list receipts"))

(defn list-pending-handler [db]
  (utils/with-error-handling
    (fn [_]
      (utils/success-response
        {:receipts (to-app (receipt-queries/list-pending-for-processing db))}))
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
          (let [result (receipt-storage/upload-receipt! db body)]
            (utils/success-response
              (-> result
                (update :receipt to-app)))))))
    "Failed to upload receipt"))

(defn get-receipt-handler [db]
  (utils/with-error-handling
    (fn [request]
      (if-let [id (utils/parse-uuid-custom (get-in request [:path-params :id]))]
        (if-let [receipt (receipt-queries/get-receipt db id)]
          (let [receipt0 (->> receipt to-app (enrich-receipt-for-detail db))
                content-type* (some-> (:content-type receipt0) str/trim not-empty)
                inferred-content-type (or content-type*
                                        (receipt-storage/infer-content-type (:original-filename receipt0))
                                        (receipt-storage/infer-content-type (:storage-key receipt0)))
                receipt* (cond-> receipt0
                           (and (nil? content-type*) (seq inferred-content-type))
                           (assoc :content-type inferred-content-type))
                download-url (when (receipt-storage/resolve-local-receipt-file (:storage-key receipt*))
                               (str "/admin/api/expenses/receipts/" id "/download"))]
            (utils/success-response
              {:receipt (cond-> receipt*
                          download-url (assoc :download-url download-url))}))
          (utils/error-response "Receipt not found" :status 404))
        (utils/error-response "Invalid id" :status 400)))
    "Failed to fetch receipt"))

(defn- truthy-param?
  [value]
  (let [value* (some-> value str str/lower-case str/trim)]
    (contains? #{"1" "true" "yes"} value*)))

(defn- safe-filename
  [value fallback]
  (let [value* (some-> value
                 str
                 (str/replace #"[\r\n\"]" "")
                 str/trim)]
    (if (seq value*) value* fallback)))

(defn download-receipt-handler [db]
  (utils/with-error-handling
    (fn [request]
      (if-let [id (utils/parse-uuid-custom (get-in request [:path-params :id]))]
        (if-let [receipt (receipt-queries/get-receipt db id)]
          (let [receipt-app (to-app receipt)
                file (receipt-storage/resolve-local-receipt-file (:storage-key receipt-app))]
            (if-not file
              (utils/error-response "Receipt file not found" :status 404)
              (let [qp (:query-params request)
                    download? (truthy-param? (or (:download qp) (get qp "download")))
                    disposition (if download? "attachment" "inline")
                    format (some-> (or (:format qp) (get qp "format") (:as qp) (get qp "as")) str str/trim str/lower-case)
                    want-jpeg? (contains? #{"jpg" "jpeg"} format)
                    preview? (truthy-param? (or (:preview qp) (get qp "preview")))
                    filename (safe-filename (:original-filename receipt-app) (str "receipt-" id))
                    content-type (or (some-> (:content-type receipt-app) str/trim not-empty)
                                   (receipt-storage/infer-content-type (:original-filename receipt-app))
                                   (receipt-storage/infer-content-type (:storage-key receipt-app))
                                   "application/octet-stream")
                    ct* (some-> content-type str str/trim str/lower-case)
                    heic? (or (contains? #{"image/heic" "image/heif"} ct*)
                            (contains? #{"image/heic" "image/heif"}
                              (some-> (receipt-storage/infer-content-type filename) str str/trim str/lower-case)))]
                (if (or (and want-jpeg? heic?) preview?)
                  (let [{:keys [bytes preprocessed?]} (image-preprocess/prepare-for-preview {:path (.getPath file)
                                                                                             :content-type content-type
                                                                                             :filename filename})]
                    (if preprocessed?
                      (let [filename* (safe-filename (if (re-find #"\.[A-Za-z0-9]+$" filename)
                                                       (str/replace filename #"\.[A-Za-z0-9]+$" ".jpg")
                                                       (str filename ".jpg"))
                                        (str "receipt-" id ".jpg"))]
                        (-> (response/response bytes)
                          (response/content-type "image/jpeg")
                          (response/header "Content-Disposition" (str disposition "; filename=\"" filename* "\""))
                          (response/header "Cache-Control" "private, max-age=0, no-store")))
                      (-> (response/file-response (.getPath file))
                        (response/content-type content-type)
                        (response/header "Content-Disposition" (str disposition "; filename=\"" filename "\""))
                        (response/header "Cache-Control" "private, max-age=0, no-store"))))
                  (-> (response/file-response (.getPath file))
                    (response/content-type content-type)
                    (response/header "Content-Disposition" (str disposition "; filename=\"" filename "\""))
                    (response/header "Cache-Control" "private, max-age=0, no-store"))))))
          (utils/error-response "Receipt not found" :status 404))
        (utils/error-response "Invalid id" :status 400)))
    "Failed to download receipt"))

(defn delete-receipt-handler [db]
  (utils/with-error-handling
    (fn [request]
      (if-let [id (utils/parse-uuid-custom (get-in request [:path-params :id]))]
        (if-let [deleted (receipt-queries/delete-receipt! db id)]
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
              {:receipt (to-app (receipt-status/update-status! db id new-status))}))
          (utils/error-response "Invalid id" :status 400))))
    "Failed to update receipt status"))

(defn retry-receipt-handler [db]
  (utils/with-error-handling
    (fn [request]
      (if-let [id (utils/parse-uuid-custom (get-in request [:path-params :id]))]
        (utils/success-response
          {:receipt (to-app (receipt-status/retry-extraction! db id))})
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
              {:receipt (to-app (receipt-status/mark-failed! db id message details))}))
          (utils/error-response "Invalid id" :status 400))))
    "Failed to mark receipt failed"))

(defn save-extraction-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [body (:body request)]
        (if-let [id (utils/parse-uuid-custom (get-in request [:path-params :id]))]
          (utils/success-response
            {:receipt (to-app (receipt-status/store-extraction-results! db id body))})
          (utils/error-response "Invalid id" :status 400))))
    "Failed to store extraction results"))

(defn approve-and-post-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [body (:body request)]
        (if-let [id (utils/parse-uuid-custom (get-in request [:path-params :id]))]
          (let [expense (receipt-approval/approve-and-post! db id body)
                receipt (receipt-queries/get-receipt db id)]
            (utils/success-response {:expense (to-app expense)
                                     :receipt (to-app receipt)}))
          (utils/error-response "Invalid id" :status 400))))
    "Failed to approve receipt"))

(defn save-review-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [body (:body request)]
        (if-let [id (utils/parse-uuid-custom (get-in request [:path-params :id]))]
          (let [receipt (receipt-approval/save-review! db id body)]
            (utils/success-response {:receipt (to-app receipt)}))
          (utils/error-response "Invalid id" :status 400))))
    "Failed to save receipt review"))

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
        (if-let [_receipt (receipt-queries/get-receipt db id)]
          (let [{:keys [enabled? api-key] :as ocr-cfg}
                (ocr-provider/build-provider app-config)]
            (cond
              (not enabled?)
              (utils/error-response (ocr-provider/disabled-message ocr-cfg) :status 409)

              (not (seq api-key))
              (utils/error-response (ocr-provider/missing-api-key-message ocr-cfg) :status 409)

              :else
              (do
                (receipt-ocr/queue-ui-ocr! db app-config [id] {:source :admin-ui})
                (utils/json-response {:success true
                                      :data {:queued true
                                             :receipt_ids [(str id)]}}
                  :status 202))))
          (utils/error-response "Receipt not found" :status 404))
        (utils/error-response "Invalid id" :status 400)))
    "Failed to trigger OCR"))

(defn routes
  "Admin receipts routes. Mounted under /admin/api/expenses/receipts."
  [db & [app-config]]
  ["/receipts"
   ["" {:get (list-receipts-handler db)
        :post (upload-receipt-handler db)}]
   ["/pending" {:get (list-pending-handler db)}]
   ["/:id/download" {:get (download-receipt-handler db)}]
   ["/:id" {:get (get-receipt-handler db)
            :delete (delete-receipt-handler db)}]
   ["/:id/status" {:post (update-status-handler db)}]
   ["/:id/retry" {:post (retry-receipt-handler db)}]
   ["/:id/fail" {:post (fail-receipt-handler db)}]
   ["/:id/extraction" {:post (save-extraction-handler db)}]
   ["/:id/review" {:post (save-review-handler db)}]
   ["/:id/approve" {:post (approve-and-post-handler db)}]
   ["/:id/ocr" {:post (ocr-single-receipt-handler db app-config)}]])
