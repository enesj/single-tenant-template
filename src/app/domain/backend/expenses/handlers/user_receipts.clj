(ns app.domain.backend.expenses.handlers.user-receipts
  "User-facing (non-admin) receipt review and approval handlers.

  These endpoints are mounted under /api/v1/expenses and require an authenticated user.

  Responsibilities:
  - list receipts belonging to the current user
  - fetch a receipt detail
  - approve an extracted receipt and create an expense (receipt status → posted)
  - trigger OCR for receipts (single and batch)"
  (:require
    [app.domain.backend.expenses.integrations.ocr-provider :as ocr-provider]
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [app.domain.backend.expenses.services.receipts.approval :as receipt-approval]
    [app.domain.backend.expenses.services.receipts.image-preprocess :as image-preprocess]
    [app.domain.backend.expenses.services.receipts.queries :as receipt-queries]
    [app.domain.backend.expenses.services.receipts.storage :as receipt-storage]
    [app.domain.backend.expenses.services.supplier-aliases :as supplier-aliases]
    [app.domain.backend.expenses.services.suppliers :as suppliers]
    [app.domain.backend.expenses.services.user-expense-settings :as user-settings]
    [app.domain.backend.expenses.workers.receipt-ocr.core :as receipt-ocr]
    [app.shared.adapters.database :as shared-db]
    [clojure.string :as str]
    [ring.util.response :as response]
    [taoensso.timbre :as log]))

(def ^:private to-app shared-db/to-app)

(def ^:private try-parse-uuid h/try-parse-uuid)

(def ^:private receipts-read-roles
  #{"viewer" "member" "admin" "owner"})
(def ^:private receipts-write-roles
  #{"member" "admin" "owner"})

(defn- parse-long-param
  [params k default-val]
  (if-let [v (h/get-param params k)]
    (try
      (Long/parseLong (str v))
      (catch Exception _ default-val))
    default-val))

(defn- parse-status-param
  [status-param]
  (cond
    (vector? status-param) status-param
    (seq? status-param) (vec status-param)
    (string? status-param)
    (let [s (str/trim status-param)]
      (if (str/includes? s ",")
        (->> (str/split s #",") (map str/trim) (remove str/blank?) vec)
        s))
    :else nil))

(defn- parse-money
  [v]
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

(defn- with-error-handling
  [handler-fn error-message]
  (fn [request]
    (try
      (handler-fn request)
      (catch clojure.lang.ExceptionInfo e
        (let [{:keys [status] :as data} (ex-data e)
              status (or status 500)
              message (or (ex-message e) error-message)]
          (when (= status 500)
            (log/error e error-message data))
          (h/json-response (cond-> {:error message}
                             (seq (dissoc data :status)) (assoc :details (dissoc data :status)))
            status)))
      (catch Exception e
        (log/error e error-message)
        (h/json-response {:error error-message} 500)))))

(defn list-receipts-handler
  "GET /api/v1/expenses/receipts

  Query params:
  - status (optional, string or comma-separated)
  - limit (default 50)
  - offset (default 0)
  - order-dir (default desc)
  - original-filename (text filter, ILIKE)
  - supplier-guess (text filter, ILIKE)"
  [db]
  (with-error-handling
    (fn [request]
      (if-let [user-id (h/get-user-id request)]
        (if-let [forbidden (h/ensure-role request receipts-read-roles "Role assignment required")]
          forbidden
          (let [tenant-id (h/get-tenant-id request)
                qp (:query-params request)
                status (parse-status-param (or (:status qp) (get qp "status")))
                text-filters (h/extract-text-filter-params qp
                               [:original-filename :supplier-guess])
                opts (cond-> (merge {:status status
                                     :limit (parse-long-param qp :limit 50)
                                     :offset (parse-long-param qp :offset 0)
                                     :order-dir (keyword (or (:order-dir qp) (get qp "order-dir") "desc"))
                                     :order-by (or (:order-by qp) (get qp "order-by"))}
                               text-filters)
                       tenant-id (assoc :tenant-id tenant-id))
                {:keys [rows total limit offset]}
                (if (h/tenant-elevated? request)
                  (receipt-queries/list-receipts-page db opts)
                  (receipt-queries/list-user-receipts-page db user-id opts))]
            (h/json-response {:data (to-app rows)
                              :total total
                              :limit limit
                              :offset offset}
              200)))
        (h/unauthorized-response)))
    "Failed to list receipts"))

(defn get-receipt-handler
  "GET /api/v1/expenses/receipts/:id"
  [db]
  (with-error-handling
    (fn [request]
      (if-let [user-id (h/get-user-id request)]
        (if-let [forbidden (h/ensure-role request receipts-read-roles "Role assignment required")]
          forbidden
          (let [tenant-id (h/get-tenant-id request)]
            (if-let [id (h/try-parse-uuid (get-in request [:path-params :id]))]
              (if-let [receipt (if (h/tenant-elevated? request)
                                 (receipt-queries/get-receipt db id)
                                 (receipt-queries/get-user-receipt db user-id id tenant-id))]
                (let [receipt0 (to-app receipt)
                      content-type* (some-> (:content-type receipt0) str/trim not-empty)
                      inferred-content-type (or content-type*
                                              (receipt-storage/infer-content-type (:original-filename receipt0))
                                              (receipt-storage/infer-content-type (:storage-key receipt0)))
                      receipt0 (cond-> receipt0
                                 (and (nil? content-type*) (seq inferred-content-type))
                                 (assoc :content-type inferred-content-type))
                      download-url (when (receipt-storage/resolve-local-receipt-file (:storage-key receipt0))
                                     (str "/api/v1/expenses/receipts/" id "/download"))
                      receipt-app (cond-> (enrich-receipt-for-detail db receipt0)
                                    download-url (assoc :download-url download-url))]
                  (h/json-response {:data receipt-app} 200))
                (h/json-response {:error "Receipt not found"} 404))
              (h/json-response {:error "Invalid id"} 400))))
        (h/unauthorized-response)))
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

(defn download-receipt-handler
  "GET /api/v1/expenses/receipts/:id/download"
  [db]
  (with-error-handling
    (fn [request]
      (if-let [user-id (h/get-user-id request)]
        (if-let [forbidden (h/ensure-role request receipts-read-roles "Role assignment required")]
          forbidden
          (let [tenant-id (h/get-tenant-id request)]
            (if-let [id (try-parse-uuid (get-in request [:path-params :id]))]
              (if-let [receipt (if (h/tenant-elevated? request)
                                 (receipt-queries/get-receipt db id)
                                 (receipt-queries/get-user-receipt db user-id id tenant-id))]
                (let [receipt-app (to-app receipt)
                      file (receipt-storage/resolve-local-receipt-file (:storage-key receipt-app))]
                  (if-not file
                    (h/json-response {:error "Receipt file not found"} 404)
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
                (h/json-response {:error "Receipt not found"} 404))
              (h/json-response {:error "Invalid id"} 400))))
        (h/unauthorized-response)))
    "Failed to download receipt"))

(defn batch-delete-receipts-handler
  "DELETE /api/v1/expenses/receipts/batch

  Batch delete receipts visible to the current user.

  Allowed roles: member/admin/owner.

  Expects JSON body like:
  {:ids [<uuid> ...]}

  Returns:
  {:data {:deleted-count n :deleted-ids [...] :errors [...]}}"
  [db]
  (with-error-handling
    (fn [request]
      (if-let [user-id (h/get-user-id request)]
        (if-let [forbidden (h/ensure-role request receipts-write-roles
                             "Only members, admins, and owners can delete receipts")]
          forbidden
          (let [tenant-id (h/get-tenant-id request)
                body (h/read-body-params request)
                raw-ids (or (:ids body)
                          (:receipt_ids body)
                          (:receipt-ids body)
                          (:receiptIds body)
                          [])
                ids (->> raw-ids (map try-parse-uuid) (filter some?) vec)]
            (cond
              (empty? raw-ids)
              (h/json-response {:error "No receipt ids provided"} 400)

              (empty? ids)
              (h/json-response {:error "One or more receipt ids are invalid"} 400)

              :else
              (let [deleted-ids (atom [])
                    errors (atom [])]
                (doseq [id ids]
                  (try
                    (cond
                      (h/tenant-elevated? request)
                      (if-let [_deleted (receipt-queries/delete-receipt! db id)]
                        (swap! deleted-ids conj (str id))
                        (swap! errors conj {:id (str id)
                                            :error "not found"}))

                      (not (receipt-queries/get-user-receipt db user-id id tenant-id))
                      (swap! errors conj {:id (str id)
                                          :error "not found"})

                      :else
                      (if-let [_deleted (receipt-queries/delete-receipt! db id tenant-id)]
                        (swap! deleted-ids conj (str id))
                        (swap! errors conj {:id (str id)
                                            :error "not found"})))
                    (catch Exception e
                      (swap! errors conj {:id (str id)
                                          :error (.getMessage e)}))))

                (h/json-response {:data {:deleted-count (count @deleted-ids)
                                         :deleted-ids (vec @deleted-ids)
                                         :errors (vec @errors)}})))))
        (h/unauthorized-response)))
    "Failed to batch delete receipts"))

(defn approve-receipt-handler
  "POST /api/v1/expenses/receipts/:id/approve

  Body: expense form payload (supplier_id, payer_id, purchased_at, total_amount, currency, notes, items)

  Returns {:data {:expense ... :receipt ...}}"
  [db]
  (with-error-handling
    (fn [request]
      (if-let [user-id (h/get-user-id request)]
        (if-let [forbidden (h/ensure-role request receipts-write-roles "Only members, admins, and owners can approve receipts")]
          forbidden
          (let [tenant-id (h/get-tenant-id request)]
            (if-let [id (try-parse-uuid (get-in request [:path-params :id]))]
              (let [body (h/read-body-params request)
                    expense (if (h/tenant-elevated? request)
                              (receipt-approval/approve-and-post-for-user-any! db user-id id body :tenant-id tenant-id)
                              (receipt-approval/approve-and-post-for-user! db user-id id body :tenant-id tenant-id))
                    receipt (if (h/tenant-elevated? request)
                              (receipt-queries/get-receipt db id)
                              (receipt-queries/get-user-receipt db user-id id tenant-id))]
                ;; Sticky default: update default payer if it changed
                (try
                  (user-settings/update-sticky-default-payer! db tenant-id user-id
                    (or (:payer_id body) (:payer-id body)))
                  (catch Exception e
                    (log/warn e "Failed to update sticky default payer after receipt approval")))
                (h/json-response {:data {:expense (to-app expense)
                                         :receipt (to-app receipt)}}
                  200))
              (h/json-response {:error "Invalid id"} 400))))
        (h/unauthorized-response)))
    "Failed to approve receipt"))

(defn save-receipt-review-handler
  "POST /api/v1/expenses/receipts/:id/review

  Body: receipt review payload (supplier_id, purchased_at, total_amount, currency, items)

  Persists reviewed values without creating an expense.

  Returns {:data {:receipt ...}}"
  [db]
  (with-error-handling
    (fn [request]
      (if-let [user-id (h/get-user-id request)]
        (if-let [forbidden (h/ensure-role request receipts-write-roles "Only members, admins, and owners can review receipts")]
          forbidden
          (let [tenant-id (h/get-tenant-id request)]
            (if-let [id (try-parse-uuid (get-in request [:path-params :id]))]
              (let [body (h/read-body-params request)
                    accessible? (if (h/tenant-elevated? request)
                                  (some? (receipt-queries/get-receipt db id))
                                  (some? (receipt-queries/get-user-receipt db user-id id tenant-id)))]
                (if-not accessible?
                  (h/json-response {:error "Receipt not found"} 404)
                  (do
                    (receipt-approval/save-review! db id body)
                    (let [receipt (if (h/tenant-elevated? request)
                                    (receipt-queries/get-receipt db id)
                                    (receipt-queries/get-user-receipt db user-id id tenant-id))
                          receipt-app (to-app receipt)
                          download-url (when (receipt-storage/resolve-local-receipt-file (:storage-key receipt-app))
                                         (str "/api/v1/expenses/receipts/" id "/download"))
                          receipt-app (cond-> (enrich-receipt-for-detail db receipt-app)
                                        download-url (assoc :download-url download-url))]
                      (h/json-response {:data {:receipt receipt-app}} 200)))))
              (h/json-response {:error "Invalid id"} 400))))
        (h/unauthorized-response)))
    "Failed to save receipt review"))

;; ---------------------------------------------------------------------------
;; OCR Handlers (UI-triggered)
;; ---------------------------------------------------------------------------

(defn ocr-single-receipt-handler
  "POST /api/v1/expenses/receipts/:id/ocr

  Trigger OCR for a single receipt. Admin users can OCR any receipt;
  regular users can only OCR their own receipts."
  [db app-config]
  (with-error-handling
    (fn [request]
      (if-let [user-id (h/get-user-id request)]
        (if-let [forbidden (h/ensure-role request receipts-write-roles "Only members, admins, and owners can run OCR")]
          forbidden
          (let [tenant-id (h/get-tenant-id request)]
            (if-let [id (try-parse-uuid (get-in request [:path-params :id]))]
              ;; Check access: admin can OCR any, users only their own
              (let [receipt (if (h/tenant-elevated? request)
                              (receipt-queries/get-receipt db id)
                              (receipt-queries/get-user-receipt db user-id id tenant-id))]
                (if receipt
                  (let [{:keys [enabled? api-key] :as ocr-cfg}
                        (ocr-provider/build-provider app-config)]
                    (cond
                      (not enabled?)
                      (h/json-response {:error (ocr-provider/disabled-message ocr-cfg)}
                        409)

                      (not (seq api-key))
                      (h/json-response {:error (ocr-provider/missing-api-key-message ocr-cfg)}
                        409)

                      :else
                      (do
                        (receipt-ocr/queue-ui-ocr! db app-config [id] {:source :user-ui :user-id user-id})
                        (h/json-response {:data {:queued true
                                                 :receipt_ids [(str id)]}}
                          202))))
                  (h/json-response {:error "Receipt not found"} 404)))
              (h/json-response {:error "Invalid id"} 400))))
        (h/unauthorized-response)))
    "Failed to trigger OCR"))

(defn ocr-batch-receipts-handler
  "POST /api/v1/expenses/receipts/ocr

  Trigger OCR for multiple receipts. Admin users can OCR any receipts;
  regular users can only OCR their own receipts.

  Expects JSON body:
  {:receipt_ids [<uuid> ...]}

  Returns:
  {:data {:queued true :receipt_ids [<uuid-string> ...]}}"
  [db app-config]
  (with-error-handling
    (fn [request]
      (if-let [user-id (h/get-user-id request)]
        (if-let [forbidden (h/ensure-role request receipts-write-roles "Only members, admins, and owners can run OCR")]
          forbidden
          (let [tenant-id (h/get-tenant-id request)
                body (h/read-body-params request)
                raw-ids (or (:receipt_ids body)
                          (:receipt-ids body)
                          (:receiptIds body)
                          (:ids body)
                          [])
                ids (->> raw-ids
                      (map try-parse-uuid)
                      (filter some?)
                      distinct
                      vec)]
            (cond
              (empty? raw-ids)
              (h/json-response {:error "No receipt ids provided"} 400)

              (empty? ids)
              (h/json-response {:error "One or more receipt ids are invalid"} 400)

              :else
              (let [accessible-ids (->> ids
                                     (filter (fn [id]
                                               (some? (if (h/tenant-elevated? request)
                                                        (receipt-queries/get-receipt db id)
                                                        (receipt-queries/get-user-receipt db user-id id tenant-id)))))
                                     vec)
                    accessible-id-set (set accessible-ids)
                    skipped-ids (->> ids
                                  (remove accessible-id-set)
                                  (map str)
                                  vec)]
                (if (empty? accessible-ids)
                  (h/json-response {:error "No accessible receipts found"} 404)
                  (let [{:keys [enabled? api-key] :as ocr-cfg}
                        (ocr-provider/build-provider app-config)]
                    (cond
                      (not enabled?)
                      (h/json-response {:error (ocr-provider/disabled-message ocr-cfg)}
                        409)

                      (not (seq api-key))
                      (h/json-response {:error (ocr-provider/missing-api-key-message ocr-cfg)}
                        409)

                      :else
                      (do
                        (receipt-ocr/queue-ui-ocr! db app-config accessible-ids {:source :user-ui :user-id user-id})
                        (h/json-response
                          {:data (cond-> {:queued true
                                          :receipt_ids (mapv str accessible-ids)}
                                   (seq skipped-ids) (assoc :skipped_receipt_ids skipped-ids))}
                          202)))))))))
        (h/unauthorized-response)))
    "Failed to trigger batch OCR"))
