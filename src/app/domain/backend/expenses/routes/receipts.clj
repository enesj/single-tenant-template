(ns app.domain.backend.expenses.routes.receipts
  "Admin API routes for receipt ingestion and approval."
  (:require
    [app.domain.backend.expenses.integrations.ocr-provider :as ocr-provider]
    [app.domain.backend.expenses.privacy :as privacy]
    [app.domain.backend.expenses.services.expenses :as expenses]
    [app.domain.backend.expenses.services.receipts.approval :as receipt-approval]
    [app.domain.backend.expenses.services.receipts.image-preprocess :as image-preprocess]
    [app.domain.backend.expenses.services.receipts.queries :as receipt-queries]
    [app.domain.backend.expenses.services.receipts.status :as receipt-status]
    [app.domain.backend.expenses.services.receipts.storage :as receipt-storage]
    [app.domain.backend.expenses.services.supplier-aliases :as supplier-aliases]
    [app.domain.backend.expenses.services.suppliers :as suppliers]
    [app.domain.backend.expenses.workers.receipt-ocr.core :as receipt-ocr]
    [app.shared.adapters.database :as shared-db]
    [app.template.backend.routes.admin.utils :as utils]
    [clojure.string :as str]
    [ring.util.response :as response]
    [taoensso.timbre :as log]))

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

(defn- resolve-supplier-via-alias
  "Resolve supplier through the receipt's supplier_alias mapping.
  Returns a supplier row or nil."
  [db receipt]
  (when-let [alias-id (:supplier-alias-id receipt)]
    (try
      (when-let [alias-row (supplier-aliases/get-alias db alias-id)]
        (when-let [supplier-id (:supplier_id alias-row)]
          ((:get suppliers/service) db supplier-id)))
      (catch Exception _ nil))))

(defn- enrich-receipt-for-detail
  [db receipt]
  (let [supplier-guess (some-> (:supplier-guess receipt) str/trim not-empty)
        supplier (or (resolve-supplier-via-alias db receipt)
                   (when-let [normalized-key (when supplier-guess
                                               (suppliers/normalize-supplier-key supplier-guess))]
                     (suppliers/find-by-normalized-key db normalized-key)))
        supplier-app (some-> supplier to-app (select-keys [:id :display-name :normalized-key]))
        lines-total (lines-total-amount-guess receipt)
        total (:total-amount-guess receipt)
        abs-dec (fn [d] (if (neg? d) (- d) d))
        total-equals-lines? (when (and (some? total) (some? lines-total))
                              (<= (abs-dec (- total lines-total)) 0.01M))
        refine-pending? (true? (get-in receipt [:raw-extract-json :refine-pending]))
        effective-status (let [status (:status receipt)]
                           (if (and (= "extracted" status)
                                 (false? total-equals-lines?)
                                 (not refine-pending?))
                             "review_required"
                             status))]
    (cond-> (assoc receipt
              :supplier-guess-has-supplier? (boolean supplier)
              :status effective-status)
      supplier-app (assoc :supplier-guess-supplier supplier-app)
      (some? lines-total) (assoc :lines-total-amount-guess lines-total)
      (some? total-equals-lines?) (assoc :total-guess-equals-lines-total-guess? total-equals-lines?))))

(defn- enrich-with-linked-expense
  "When a receipt is posted and has a linked expense, attach the expense
  (with items) under :linked-expense so the frontend can pre-fill the edit form."
  [db receipt-app]
  (if (and (= "posted" (:status receipt-app))
        (some? (or (:expense-id receipt-app) (:expense_id receipt-app))))
    (let [expense-id (or (:expense-id receipt-app) (:expense_id receipt-app))]
      (try
        (if-let [expense (expenses/get-expense-with-items db expense-id)]
          (assoc receipt-app :linked-expense (privacy/admin-expense-view expense))
          receipt-app)
        (catch Exception e
          (log/warn e "Failed to enrich posted receipt with linked expense"
            {:receipt-id (:id receipt-app) :expense-id expense-id})
          receipt-app)))
    receipt-app))

(def ^:private receipt-text-filter-keys
  [:original-filename :supplier-guess :created-by-name])

(def ^:private receipt-date-range-fields
  [:purchased-at-guess :created-at :updated-at])

(defn- extract-total-amount-range-filters
  [get-qp]
  (let [min-value (or (some-> (get-qp :total-amount-guess-min) parse-money)
                    (some-> (get-qp :total-display-min) parse-money))
        max-value (or (some-> (get-qp :total-amount-guess-max) parse-money)
                    (some-> (get-qp :total-display-max) parse-money))]
    (cond-> {}
      (some? min-value) (assoc :total-amount-guess-min min-value)
      (some? max-value) (assoc :total-amount-guess-max max-value))))

(defn list-receipts-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [qp (:query-params request)
            get-qp (fn [k] (or (get qp k) (get qp (name k))))
            status (parse-status-param (get-qp :status))
            text-filters (reduce (fn [acc k]
                                   (if-let [v (get-qp k)]
                                     (assoc acc k v)
                                     acc))
                           {}
                           receipt-text-filter-keys)
            amount-filters (extract-total-amount-range-filters get-qp)
            date-filters (utils/extract-date-range-params qp receipt-date-range-fields)
            show-purged? (boolean (utils/parse-boolean-param qp :show-purged))
            sort-opts (utils/extract-sort-params qp)
            opts (merge {:status status
                         :show-purged? show-purged?
                         :limit (utils/parse-int-param qp :limit 50)
                         :offset (utils/parse-int-param qp :offset 0)}
                   sort-opts
                   text-filters
                   amount-filters
                   date-filters)
            {:keys [rows total purged-total]} (receipt-queries/list-receipts-page db opts)]
        (utils/success-response {:receipts (privacy/admin-receipts-view rows)
                                 :total total
                                 :purged-total purged-total})))
    "Failed to list receipts"))

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
              {:receipt (privacy/admin-receipt-view
                          (enrich-with-linked-expense
                            db
                            (cond-> receipt*
                              download-url (assoc :download-url download-url))))}))
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
                                   :receipt (privacy/admin-receipt-view deleted)})
          (utils/error-response "Receipt not found" :status 404))
        (utils/error-response "Invalid id" :status 400)))
    "Failed to delete receipt"))

(defn approve-and-post-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [body (:body request)]
        (if-let [id (utils/parse-uuid-custom (get-in request [:path-params :id]))]
          (let [expense (receipt-approval/approve-and-post! db id body)
                receipt (receipt-queries/get-receipt db id)]
            (utils/success-response {:expense (privacy/admin-expense-view expense)
                                     :receipt (privacy/admin-receipt-view receipt)}))
          (utils/error-response "Invalid id" :status 400))))
    "Failed to approve receipt"))

(defn save-review-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [body (:body request)]
        (if-let [id (utils/parse-uuid-custom (get-in request [:path-params :id]))]
          (let [receipt (receipt-approval/save-review! db id body)]
            (utils/success-response {:receipt (privacy/admin-receipt-view receipt)}))
          (utils/error-response "Invalid id" :status 400))))
    "Failed to save receipt review"))

(defn update-posted-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [body (:body request)]
        (if-let [id (utils/parse-uuid-custom (get-in request [:path-params :id]))]
          (let [{:keys [expense receipt]} (receipt-approval/update-posted-receipt! db id body)]
            (utils/success-response {:expense (privacy/admin-expense-view expense)
                                     :receipt (privacy/admin-receipt-view receipt)}))
          (utils/error-response "Invalid id" :status 400))))
    "Failed to update posted receipt"))

(defn ocr-batch-handler
  "Admin batch OCR trigger. Expects body {:receipt_ids [uuid ...]}.
  Skips ids for missing receipts; blocks ids already linked to expenses."
  [db app-config]
  (utils/with-error-handling
    (fn [request]
      (let [body (:body request)
            raw-ids (or (:receipt_ids body)
                      (:receipt-ids body)
                      (:ids body)
                      [])
            ids (->> raw-ids
                  (map utils/parse-uuid-custom)
                  (filter some?)
                  distinct
                  vec)]
        (cond
          (empty? raw-ids)
          (utils/error-response "No receipt ids provided" :status 400)

          (empty? ids)
          (utils/error-response "One or more receipt ids are invalid" :status 400)

          :else
          (let [found-receipts (->> ids
                                 (keep (fn [id] (receipt-queries/get-receipt db id)))
                                 vec)
                found-id-set (set (map :id found-receipts))
                skipped-ids (->> ids (remove found-id-set) (map str) vec)
                grouped (group-by #(boolean (receipt-status/linked-expense-id db (:id %)))
                          found-receipts)
                blocked-receipts (vec (get grouped true []))
                allowed-receipts (vec (get grouped false []))
                blocked-ids (mapv (comp str :id) blocked-receipts)
                allowed-ids (mapv :id allowed-receipts)]
            (cond
              (and (empty? found-receipts) (empty? blocked-ids))
              (utils/error-response "No accessible receipts found" :status 404)

              (and (empty? allowed-ids) (seq blocked-ids))
              (utils/error-response
                "One or more receipts are already linked to expenses. Unlink them first before reparsing"
                :status 409
                :details {:blocked_receipt_ids blocked-ids})

              :else
              (let [{:keys [enabled? api-key] :as ocr-cfg}
                    (ocr-provider/build-provider app-config)]
                (cond
                  (not enabled?)
                  (utils/error-response (ocr-provider/disabled-message ocr-cfg) :status 409)

                  (not (seq api-key))
                  (utils/error-response (ocr-provider/missing-api-key-message ocr-cfg) :status 409)

                  :else
                  (let [admin-id (utils/get-admin-id request)]
                    (receipt-ocr/queue-ui-ocr! db app-config allowed-ids
                      {:source :admin-ui :admin-id admin-id})
                    (utils/json-response
                      (cond-> {:success true
                               :queued true
                               :receipt_ids (mapv str allowed-ids)}
                        (seq skipped-ids) (assoc :skipped_receipt_ids skipped-ids)
                        (seq blocked-ids) (assoc :blocked_receipt_ids blocked-ids))
                      :status 202)))))))))
    "Failed to trigger batch OCR"))

(defn routes
  "Admin receipts routes. Mounted under /admin/api/expenses/receipts."
  [db & [app-config]]
  ["/receipts"
   ["" {:get (list-receipts-handler db)}]
   ["/ocr" {:post (ocr-batch-handler db app-config)}]
   ["/:id/download" {:get (download-receipt-handler db)}]
   ["/:id" {:get (get-receipt-handler db)
            :delete (delete-receipt-handler db)}]
   ["/:id/review" {:post (save-review-handler db)}]
   ["/:id/approve" {:post (approve-and-post-handler db)}]
   ["/:id/update-posted" {:post (update-posted-handler db)}]])
