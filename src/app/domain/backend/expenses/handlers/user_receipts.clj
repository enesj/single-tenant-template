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
    [app.domain.backend.expenses.services.receipts :as receipts]
    [app.domain.backend.expenses.services.suppliers :as suppliers]
    [app.domain.backend.expenses.workers.receipt-ocr :as receipt-ocr]
    [app.template.backend.utils.adapters.database :as db-adapter]
    [cheshire.core :as json]
    [clojure.string :as str]
    [ring.util.response :as response]
    [taoensso.timbre :as log])
  (:import
    [java.util UUID]))

(defn- to-app
  [data]
  (-> data
    db-adapter/convert-pg-objects
    db-adapter/convert-db-keys->app-keys))

(defn- json-response
  [data status]
  (-> (response/response (json/generate-string data))
    (response/content-type "application/json")
    (response/status status)))

(defn- unauthorized-response
  ([] (unauthorized-response "Authentication required"))
  ([message]
   (json-response {:error message} 401)))

(defn- try-parse-uuid
  [x]
  (when x
    (try
      (UUID/fromString (str x))
      (catch Exception _ nil))))

(defn- get-user-id
  "Extract user-id from request session and normalize to UUID.

  Accepts either UUID objects or string UUIDs; returns nil if missing/invalid."
  [request]
  (let [raw-id (or (get-in request [:session :auth-session :user :id])
                 (get-in request [:session :user :id])
                 (get-in request [:identity :id]))]
    (cond
      (instance? UUID raw-id) raw-id
      :else (try-parse-uuid raw-id))))

(defn- get-user-role
  "Extract user role from request session and normalize to string (e.g. \"admin\")."
  [request]
  (let [role (or (get-in request [:session :auth-session :user :role])
               (get-in request [:session :user :role])
               (get-in request [:identity :role]))]
    (cond
      (keyword? role) (name role)
      (string? role) role
      :else nil)))

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

(defn- read-json-body
  [request]
  (or
    (:body-params request)
    (when-let [body (:body request)]
      (try
        (json/parse-string (slurp body) true)
        (catch Exception _ nil)))))

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
          (json-response (cond-> {:error message}
                           (seq (dissoc data :status)) (assoc :details (dissoc data :status)))
            status)))
      (catch Exception e
        (log/error e error-message)
        (json-response {:error error-message} 500)))))

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
      (if-let [user-id (get-user-id request)]
        (let [role (get-user-role request)
              qp (:query-params request)
              status (parse-status-param (or (:status qp) (get qp "status")))
              opts {:status status
                    :limit (parse-long-param qp :limit 50)
                    :offset (parse-long-param qp :offset 0)
                    :order-dir (keyword (or (:order_dir qp) (:order-dir qp) "desc"))}
              rows (if (= "admin" role)
                     (receipts/list-receipts db opts)
                     (receipts/list-user-receipts db user-id opts))]
          (json-response {:data (to-app rows)
                          :limit (:limit opts)
                          :offset (:offset opts)}
            200))
        (unauthorized-response)))
    "Failed to list receipts"))

(defn get-receipt-handler
  "GET /api/v1/expenses/receipts/:id"
  [db]
  (with-error-handling
    (fn [request]
      (if-let [user-id (get-user-id request)]
        (let [role (get-user-role request)]
          (if-let [id (try-parse-uuid (get-in request [:path-params :id]))]
            (if-let [receipt (if (= "admin" role)
                               (receipts/get-receipt db id)
                               (receipts/get-user-receipt db user-id id))]
              (let [receipt-app (to-app receipt)]
                (json-response {:data (enrich-receipt-for-detail db receipt-app)} 200))
              (json-response {:error "Receipt not found"} 404))
            (json-response {:error "Invalid id"} 400)))
        (unauthorized-response)))
    "Failed to fetch receipt"))

(defn delete-receipt-handler
  "DELETE /api/v1/expenses/receipts/:id

  Returns JSON to keep frontend XHR pipelines (which may expect JSON) happy."
  [db]
  (with-error-handling
    (fn [request]
      (if-let [user-id (get-user-id request)]
        (let [role (get-user-role request)]
          (if-let [id (try-parse-uuid (get-in request [:path-params :id]))]
            (if (= "admin" role)
              (if-let [deleted (receipts/delete-receipt! db id)]
                (json-response {:data {:deleted true
                                       :receipt (to-app deleted)}}
                  200)
                (json-response {:error "Receipt not found"} 404))
              ;; Regular users can only delete receipts visible to them (owned or unassigned).
              (if-not (receipts/get-user-receipt db user-id id)
                (json-response {:error "Receipt not found"} 404)
                (if-let [deleted (receipts/delete-receipt! db id)]
                  (json-response {:data {:deleted true
                                         :receipt (to-app deleted)}}
                    200)
                  (json-response {:error "Receipt not found"} 404))))
            (json-response {:error "Invalid id"} 400)))
        (unauthorized-response)))
    "Failed to delete receipt"))

(defn approve-receipt-handler
  "POST /api/v1/expenses/receipts/:id/approve

  Body: expense form payload (supplier_id, payer_id, purchased_at, total_amount, currency, notes, items)

  Returns {:data {:expense ... :receipt ...}}"
  [db]
  (with-error-handling
    (fn [request]
      (if-let [user-id (get-user-id request)]
        (let [role (get-user-role request)]
          (if-let [id (try-parse-uuid (get-in request [:path-params :id]))]
            (let [body (or (read-json-body request) {})
                  expense (if (= "admin" role)
                            (receipts/approve-and-post-for-user-any! db user-id id body)
                            (receipts/approve-and-post-for-user! db user-id id body))
                  receipt (if (= "admin" role)
                            (receipts/get-receipt db id)
                            (receipts/get-user-receipt db user-id id))]
              (json-response {:data {:expense (to-app expense)
                                     :receipt (to-app receipt)}}
                200))
            (json-response {:error "Invalid id"} 400)))
        (unauthorized-response)))
    "Failed to approve receipt"))

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
      (if-let [user-id (get-user-id request)]
        (let [role (get-user-role request)]
          (if-let [id (try-parse-uuid (get-in request [:path-params :id]))]
            ;; Check access: admin can OCR any, users only their own
            (let [receipt (if (= "admin" role)
                            (receipts/get-receipt db id)
                            (receipts/get-user-receipt db user-id id))]
              (if receipt
                (let [{:keys [enabled? api-key]} (mistral-ocr/build-config app-config)]
                  (cond
                    (not enabled?)
                    (json-response {:error "Receipt OCR is disabled (set MISTRAL_OCR_ENABLED=true to enable)"}
                      409)

                    (not (seq api-key))
                    (json-response {:error "Receipt OCR is not configured (missing MISTRAL_API_KEY)"}
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
                      (json-response {:data {:queued true
                                             :receipt_ids [(str id)]}}
                        202))))
                (json-response {:error "Receipt not found"} 404)))
            (json-response {:error "Invalid id"} 400)))
        (unauthorized-response)))
    "Failed to trigger OCR"))

(defn- parse-receipt-ids-from-body
  "Parse receipt_ids from request body. Accepts :receipt_ids or :receipt-ids."
  [body]
  (let [ids (or (:receipt_ids body) (:receipt-ids body))]
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
      (if-let [user-id (get-user-id request)]
        (let [role (get-user-role request)
              body (or (read-json-body request) {})
              all-ids (parse-receipt-ids-from-body body)]
          (if (empty? all-ids)
            (json-response {:error "receipt_ids is required and must be a non-empty array"} 400)
            ;; Filter to only receipts the user can access
            (let [accessible-ids (if (= "admin" role)
                                   all-ids
                                   (->> all-ids
                                     (filter #(receipts/get-user-receipt db user-id %))
                                     vec))]
              (if (empty? accessible-ids)
                (json-response {:error "No accessible receipts found"} 404)
                (let [{:keys [enabled? api-key]} (mistral-ocr/build-config app-config)]
                  (cond
                    (not enabled?)
                    (json-response {:error "Receipt OCR is disabled (set MISTRAL_OCR_ENABLED=true to enable)"}
                      409)

                    (not (seq api-key))
                    (json-response {:error "Receipt OCR is not configured (missing MISTRAL_API_KEY)"}
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
                      (json-response {:data {:queued true
                                             :receipt_ids (mapv str accessible-ids)}}
                        202))))))))
        (unauthorized-response)))
    "Failed to trigger batch OCR"))
