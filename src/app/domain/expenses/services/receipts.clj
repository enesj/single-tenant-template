(ns app.domain.expenses.services.receipts
  "Receipt upload, status transitions, and approval workflow."
  (:require
    [app.domain.expenses.services.expenses :as expenses]
    [buddy.core.codecs :as codecs]
    [buddy.core.hash :as hash]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.util UUID]))

(def ^:private approvable-status? #{"extracted" "review_required"})

;; ============================================================================
;; Helpers
;; ============================================================================

(defn compute-file-hash
  "Compute SHA-256 hex digest for uploaded file bytes."
  [bytes]
  (some-> bytes hash/sha256 codecs/bytes->hex))

(defn check-duplicate
  "Return existing receipt with the same file_hash, if any."
  [db file-hash]
  (when file-hash
    (jdbc/execute-one!
      db
      (sql/format {:select [:*]
                   :from [:receipts]
                   :where [:= :file_hash file-hash]
                   :limit 1})
      {:builder-fn rs/as-unqualified-lower-maps})))

;; ============================================================================
;; CRUD / status management
;; ============================================================================

(defn upload-receipt!
  "Insert a new receipt record. Expects at least :storage_key and either
   :file_hash or :bytes to hash. Returns {:duplicate? bool :receipt {...}}."
  [db {:keys [storage_key file_hash bytes original_filename content_type file_size] :as data}]
  (let [hash (or file_hash (compute-file-hash bytes))]
    (when-not storage_key
      (throw (ex-info "storage_key is required" {:data data})))
    (when-not hash
      (throw (ex-info "file_hash or bytes required" {:data data})))

    (if-let [existing (check-duplicate db hash)]
      {:duplicate? true :receipt existing}
      (let [row {:id (UUID/randomUUID)
                 :storage_key storage_key
                 :file_hash hash
                 :original_filename original_filename
                 :content_type content_type
                 :file_size file_size
                 :status "uploaded"}
            sql-map {:insert-into :receipts
                     :values [(update row :status #(vector :cast % :receipt_status))]
                     :returning [:*]}]
        {:duplicate? false
         :receipt (jdbc/execute-one! db (sql/format sql-map) {:builder-fn rs/as-unqualified-lower-maps})}))))

(defn update-status!
  "Update receipt status (and optional extra fields)."
  [db receipt-id new-status & [extra]]
  (jdbc/execute-one!
    db
    (sql/format {:update :receipts
                 :set (merge {:status [:cast new-status :receipt_status]
                              :updated_at [:now]}
                             extra)
                 :where [:= :id receipt-id]
                 :returning [:*]})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn mark-failed!
  "Mark receipt as failed with message/details."
  [db receipt-id message & [details]]
  (update-status! db receipt-id "failed"
                  (merge {:error_message message}
                         (when details {:error_details details})
                         {:retry_count [:+ :retry_count 1]})))

(defn retry-extraction!
  "Reset receipt for re-processing and increment retry_count."
  [db receipt-id]
  (update-status! db receipt-id "uploaded" {:retry_count [:+ :retry_count 1]}))

(defn store-extraction-results!
  "Persist extraction/parse payloads and guesses."
  [db receipt-id {:keys [raw_parse_json raw_extract_json parsed_markdown supplier_guess
                        total_amount_guess currency_guess purchased_at_guess payment_hints]}]
  (jdbc/execute-one!
    db
    (sql/format {:update :receipts
                 :set {:raw_parse_json raw_parse_json
                       :raw_extract_json raw_extract_json
                       :parsed_markdown parsed_markdown
                       :supplier_guess supplier_guess
                       :total_amount_guess total_amount_guess
                       :currency_guess currency_guess
                       :purchased_at_guess purchased_at_guess
                       :payment_hints payment_hints
                       :updated_at [:now]}
                 :where [:= :id receipt-id]
                 :returning [:*]})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn get-receipt
  [db receipt-id]
  (jdbc/execute-one!
    db
    (sql/format {:select [:*]
                 :from [:receipts]
                 :where [:= :id receipt-id]})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn list-receipts
  "List receipts with optional status filter."
  [db {:keys [status limit offset order-dir] :or {limit 50 offset 0 order-dir :desc}}]
  (let [status-clause (cond
                        (string? status) [:= :status status]
                        (seq status) [:in :status status]
                        :else nil)
        query (cond-> {:select [:*]
                       :from [:receipts]
                       :order-by [[:created_at order-dir]]
                       :limit limit
                       :offset offset}
                status-clause (assoc :where status-clause))]
    (jdbc/execute! db (sql/format query) {:builder-fn rs/as-unqualified-lower-maps})))

(defn list-pending-for-processing
  "Receipts that are ready to process (uploaded or failed-but-retry)."
  [db]
  (jdbc/execute!
    db
    (sql/format {:select [:*]
                 :from [:receipts]
                 :where [:in :status ["uploaded" "parsing" "parsed" "extracting"]]
                 :order-by [[:created_at :asc]]})
    {:builder-fn rs/as-unqualified-lower-maps}))

;; ============================================================================
;; Approval / posting
;; ============================================================================

(defn approve-and-post!
  "Create an expense from a receipt and update status → posted.
   review-data expects keys for expenses/create-expense! including :supplier_id,
   :payer_id, :purchased_at, :total_amount, :currency, :notes, :items."
  [db receipt-id review-data]
  (jdbc/with-transaction [tx db]
    (let [receipt (get-receipt tx receipt-id)]
      (when-not receipt
        (throw (ex-info "Receipt not found" {:status 404 :id receipt-id})))
      (when-not (approvable-status? (:status receipt))
        (throw (ex-info "Receipt not in approvable status"
                 {:status 409 :id receipt-id :current-status (:status receipt)})))

      (let [expense (expenses/create-expense!
                      tx
                      (merge {:receipt_id receipt-id
                              :currency (or (:currency review-data) (:currency_guess receipt) "BAM")}
                             review-data)
                      (:items review-data))]
        (update-status! tx receipt-id "posted" {:expense_id (:id expense)})
        expense))))
