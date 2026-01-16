(ns app.domain.backend.expenses.handlers.user-receipts
  "User-facing (non-admin) receipt review and approval handlers.

  These endpoints are mounted under /api/v1/expenses and require an authenticated user.

  Responsibilities:
  - list receipts belonging to the current user
  - fetch a receipt detail
  - approve an extracted receipt and create an expense (receipt status → posted)
  - trigger OCR for receipts (single or batch)"
  (:require
    [app.domain.backend.expenses.integrations.mistral-ocr :as mistral-ocr]
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [app.domain.backend.expenses.services.receipts.approval :as receipt-approval]
    [app.domain.backend.expenses.services.receipts.queries :as receipt-queries]
    [app.domain.backend.expenses.services.receipts.storage :as receipt-storage]
    [app.domain.backend.expenses.services.suppliers :as suppliers]
    [app.domain.backend.expenses.workers.receipt-ocr.core :as receipt-ocr]
    [app.template.backend.utils.adapters.database :as db-adapter]
    [clojure.string :as str]
    [ring.util.response :as response]
    [taoensso.timbre :as log]))

(def ^:private to-app db-adapter/to-app)

(def ^:private try-parse-uuid h/try-parse-uuid)

(def ^:private receipts-read-roles h/receipts-read-roles)
(def ^:private receipts-write-roles h/receipts-write-roles)

(defn- parse-long-param
  [params k default-val]
  (if-let [v (get params k)]
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
  - order_dir (default desc)"
  [db]
  (with-error-handling
    (fn [request]
      (if-let [user-id (h/get-user-id request)]
        (if-let [forbidden (h/ensure-role request receipts-read-roles "Role assignment required")]
          forbidden
          (let [role (h/get-user-role request)
                qp (:query-params request)
                status (parse-status-param (or (:status qp) (get qp "status")))
                    opts {:status status
                      :limit (parse-long-param qp :limit 50)
                      :offset (parse-long-param qp :offset 0)
                      :order-dir (keyword (or (:order_dir qp) "desc"))}
                rows (if (= "admin" role)
                       (receipt-queries/list-receipts db opts)
                       (receipt-queries/list-user-receipts db user-id opts))]
            (h/json-response {:data (to-app rows)
                              :limit (:limit opts)
                              :offset (:offset opts)}
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
          (let [role (h/get-user-role request)]
            (if-let [id (h/try-parse-uuid (get-in request [:path-params :id]))]
              (if-let [receipt (if (= "admin" role)
                                 (receipt-queries/get-receipt db id)
                                 (receipt-queries/get-user-receipt db user-id id))]
                (let [receipt-app (to-app receipt)
                      download-url (when (receipt-storage/resolve-local-receipt-file (:storage-key receipt-app))
                         (str "/api/v1/expenses/receipts/" id "/download"))
                      receipt-app (cond-> (enrich-receipt-for-detail db receipt-app)
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
          (let [role (h/get-user-role request)]
            (if-let [id (try-parse-uuid (get-in request [:path-params :id]))]
              (if-let [receipt (if (= "admin" role)
                                 (receipt-queries/get-receipt db id)
                                 (receipt-queries/get-user-receipt db user-id id))]
                (let [receipt-app (to-app receipt)
                      file (receipt-storage/resolve-local-receipt-file (:storage-key receipt-app))]
                  (if-not file
                    (h/json-response {:error "Receipt file not found"} 404)
                    (let [qp (:query-params request)
                          download? (truthy-param? (or (:download qp) (get qp "download")))
                          disposition (if download? "attachment" "inline")
                          filename (safe-filename (:original-filename receipt-app) (str "receipt-" id))
                          content-type (or (:content-type receipt-app) "application/octet-stream")]
                      (-> (response/file-response (.getPath file))
                        (response/content-type content-type)
                        (response/header "Content-Disposition" (str disposition "; filename=\"" filename "\""))
                        (response/header "Cache-Control" "private, max-age=0, no-store")))))
                (h/json-response {:error "Receipt not found"} 404))
              (h/json-response {:error "Invalid id"} 400))))
        (h/unauthorized-response)))
    "Failed to download receipt"))

(defn delete-receipt-handler
  "DELETE /api/v1/expenses/receipts/:id

  Returns JSON to keep frontend XHR pipelines (which may expect JSON) happy."
  [db]
  (with-error-handling
    (fn [request]
      (if-let [user-id (h/get-user-id request)]
        (if-let [forbidden (h/ensure-role request receipts-write-roles "Only members, admins, and owners can delete receipts")]
          forbidden
          (let [role (h/get-user-role request)]
            (if-let [id (try-parse-uuid (get-in request [:path-params :id]))]
              (if (= "admin" role)
                (if-let [deleted (receipt-queries/delete-receipt! db id)]
                  (h/json-response {:data {:deleted true
                                           :receipt (to-app deleted)}}
                    200)
                  (h/json-response {:error "Receipt not found"} 404))
                ;; Regular users can only delete receipts visible to them (owned or unassigned).
                (if-not (receipt-queries/get-user-receipt db user-id id)
                  (h/json-response {:error "Receipt not found"} 404)
                  (if-let [deleted (receipt-queries/delete-receipt! db id)]
                    (h/json-response {:data {:deleted true
                                             :receipt (to-app deleted)}}
                      200)
                    (h/json-response {:error "Receipt not found"} 404))))
              (h/json-response {:error "Invalid id"} 400))))
        (h/unauthorized-response)))
    "Failed to delete receipt"))

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
          (let [role (h/get-user-role request)]
            (if-let [id (try-parse-uuid (get-in request [:path-params :id]))]
              (let [body (h/read-body-params request)
                    expense (if (= "admin" role)
                              (receipt-approval/approve-and-post-for-user-any! db user-id id body)
                              (receipt-approval/approve-and-post-for-user! db user-id id body))
                    receipt (if (= "admin" role)
                              (receipt-queries/get-receipt db id)
                              (receipt-queries/get-user-receipt db user-id id))]
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
          (let [role (h/get-user-role request)]
            (if-let [id (try-parse-uuid (get-in request [:path-params :id]))]
              (let [body (h/read-body-params request)
                    accessible? (if (= "admin" role)
                                  (some? (receipt-queries/get-receipt db id))
                                  (some? (receipt-queries/get-user-receipt db user-id id)))]
                (if-not accessible?
                  (h/json-response {:error "Receipt not found"} 404)
                  (do
                    (receipt-approval/save-review! db id body)
                    (let [receipt (if (= "admin" role)
                                    (receipt-queries/get-receipt db id)
                                    (receipt-queries/get-user-receipt db user-id id))
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
          (let [role (h/get-user-role request)]
            (if-let [id (try-parse-uuid (get-in request [:path-params :id]))]
              ;; Check access: admin can OCR any, users only their own
              (let [receipt (if (= "admin" role)
                              (receipt-queries/get-receipt db id)
                              (receipt-queries/get-user-receipt db user-id id))]
                (if receipt
                  (let [{:keys [enabled? api-key]} (mistral-ocr/build-config app-config)]
                    (cond
                      (not enabled?)
                      (h/json-response {:error "Receipt OCR is disabled (set MISTRAL_OCR_ENABLED=true to enable)"}
                        409)

                      (not (seq api-key))
                      (h/json-response {:error "Receipt OCR is not configured (missing MISTRAL_API_KEY)"}
                        409)

                      :else
                      (do
                        ;; Run OCR asynchronously
                        (future
                          (try
                            (log/info "Starting OCR for receipt" {:receipt-id id :user-id user-id :source :user-ui})
                            (receipt-ocr/process-receipts-by-ids! db app-config [id])
                            (log/info "Completed OCR for receipt" {:receipt-id id :source :user-ui})
                            (catch Exception e
                              (log/error e "OCR failed for receipt" {:receipt-id id}))))
                        ;; Return immediately with 202 Accepted
                        (h/json-response {:data {:queued true
                                     :receipt_ids [(str id)]}}
                          202))))
                      (h/json-response {:error "Receipt not found"} 404)))
                    (h/json-response {:error "Invalid id"} 400))))
                (h/unauthorized-response)))
    "Failed to trigger OCR"))

(defn- parse-receipt-ids-from-body
  "Parse receipt_ids from request body."
  [body]
  (let [ids (:receipt_ids body)]
    (when (sequential? ids)
      (->> ids
        (map try-parse-uuid)
        (filter some?)
        vec))))

(defn ocr-batch-receipts-handler
  "POST /api/v1/expenses/receipts/ocr

  Trigger OCR for multiple receipts. Admin users can OCR any receipts;
  regular users can only OCR their own receipts.

  Body: {:receipt_ids [...]}"
  [db app-config]
  (with-error-handling
    (fn [request]
      (if-let [user-id (h/get-user-id request)]
        (if-let [forbidden (h/ensure-role request receipts-write-roles "Only members, admins, and owners can run OCR")]
          forbidden
          (let [role (h/get-user-role request)
                body (h/read-body-params request)
                all-ids (parse-receipt-ids-from-body body)]
            (if (empty? all-ids)
              (h/json-response {:error "receipt_ids is required and must be a non-empty array"} 400)
              ;; Filter to only receipts the user can access
              (let [accessible-ids (if (= "admin" role)
                                     all-ids
                                     (->> all-ids
                                       (filter #(receipt-queries/get-user-receipt db user-id %))
                                       vec))]
                (if (empty? accessible-ids)
                  (h/json-response {:error "No accessible receipts found"} 404)
                  (let [{:keys [enabled? api-key]} (mistral-ocr/build-config app-config)]
                    (cond
                      (not enabled?)
                      (h/json-response {:error "Receipt OCR is disabled (set MISTRAL_OCR_ENABLED=true to enable)"}
                        409)

                      (not (seq api-key))
                      (h/json-response {:error "Receipt OCR is not configured (missing MISTRAL_API_KEY)"}
                        409)

                      :else
                      (do
                        ;; Run OCR asynchronously
                        (future
                          (try
                            (log/info "Starting batch OCR" {:receipt-ids accessible-ids :user-id user-id :source :user-ui})
                            (receipt-ocr/process-receipts-by-ids! db app-config accessible-ids)
                            (log/info "Completed batch OCR" {:receipt-ids accessible-ids :source :user-ui})
                            (catch Exception e
                              (log/error e "Batch OCR failed" {:receipt-ids accessible-ids}))))
                        ;; Return immediately with 202 Accepted
                        (h/json-response {:data {:queued true
                                     :receipt_ids (mapv str accessible-ids)}}
                          202)))))))))
                (h/unauthorized-response)))
    "Failed to trigger batch OCR"))
