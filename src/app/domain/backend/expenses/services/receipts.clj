(ns app.domain.backend.expenses.services.receipts
  "Receipt upload, status transitions, and approval workflow."
  (:require
    [app.domain.backend.expenses.services.expenses :as expenses]
    [app.domain.backend.expenses.services.suppliers :as suppliers]
    [buddy.core.codecs :as codecs]
    [buddy.core.hash :as hash]
    [cheshire.core :as json]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]
    [taoensso.timbre :as log])
  (:import
    [java.nio.file Files]
    [java.time Instant LocalDate LocalDateTime OffsetDateTime ZoneId ZoneOffset]
    [java.util UUID]))

(def ^:private approvable-status? #{"extracted" "review_required"})

(def ^:private allowed-currencies #{"BAM" "EUR" "USD"})

(defn- blank->nil
  [v]
  (cond
    (nil? v) nil
    (and (string? v) (str/blank? v)) nil
    :else v))

(defn- parse-instant!
  "Parse `v` as an Instant.

  Accepts ISO-8601 instants, OffsetDateTime strings, HTML `datetime-local`
  strings (e.g. \"2025-12-12T12:34\"), LocalDate strings, epoch millis.

  Throws ex-info {:status 400 :field field} on invalid input."
  [field v]
  (let [v (blank->nil v)]
    (cond
      (nil? v) nil
      (instance? Instant v) v
      (number? v) (Instant/ofEpochMilli (long v))
      (string? v)
      (or
        (try
          (Instant/parse v)
          (catch Exception _ nil))
        (try
          (-> (OffsetDateTime/parse v) .toInstant)
          (catch Exception _ nil))
        ;; Support HTML `datetime-local` values like "2025-12-12T12:34".
        (try
          (-> (LocalDateTime/parse v)
            (.atZone (ZoneId/systemDefault))
            .toInstant)
          (catch Exception _ nil))
        (try
          (-> (LocalDate/parse v)
            (.atStartOfDay ZoneOffset/UTC)
            .toInstant)
          (catch Exception _ nil))
        (throw (ex-info (str "Invalid " (name field))
                 {:status 400
                  :field field
                  :value v})))
      :else
      (throw (ex-info (str "Invalid " (name field))
               {:status 400
                :field field
                :value v})))))

(defn- normalize-currency!
  "Normalize/validate currency string.

  Returns an uppercase currency code (e.g. \"BAM\") or nil.
  Throws ex-info {:status 400 :field :currency} when non-blank but invalid."
  [currency]
  (let [currency* (some-> currency blank->nil str str/trim str/upper-case)]
    (cond
      (nil? currency*) nil
      (contains? allowed-currencies currency*) currency*
      :else (throw (ex-info "Invalid currency"
                     {:status 400
                      :field :currency
                      :value currency})))))

(defn- try-parse-uuid
  [v]
  (try
    (when (some? v)
      (UUID/fromString (str v)))
    (catch Exception _
      nil)))

(defn- parse-money
  "Coerce user/JSON numeric values into BigDecimal.

  Accepts numbers, BigDecimal, or numeric-ish strings. Returns nil when unparsable."
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

(defn- lines-total
  [items]
  (when (sequential? items)
    (let [totals (keep (fn [item]
                         (parse-money (or (:line_total item) (:line-total item))))
                   items)]
      (when (seq totals)
        (reduce + 0M totals)))))

;; Forward declares (helps static analysis tools when functions are defined later in this file)
(declare get-receipt)
(declare jsonb-value receipt-status-cast)

(defn save-review!
  "Persist reviewed receipt values without posting an expense.

  - Updates raw_extract_json.extraction.items to reviewed items
  - Updates supplier_guess/total_amount_guess/currency_guess/purchased_at_guess
  - Optionally flips status review_required → extracted when totals match

  Returns the updated receipt row."
  [db receipt-id {:keys [supplier_id purchased_at total_amount currency items] :as review-data}]
  (jdbc/with-transaction [tx db]
    (let [receipt (get-receipt tx receipt-id)]
      (when-not receipt
        (throw (ex-info "Receipt not found" {:status 404 :id receipt-id})))
      (when-not (approvable-status? (:status receipt))
        (throw (ex-info "Receipt not in approvable status"
                 {:status 409 :id receipt-id :current-status (:status receipt)})))

      (let [supplier-uuid (try-parse-uuid supplier_id)
            supplier (when supplier-uuid (suppliers/get-supplier tx supplier-uuid))
            supplier-guess (or (some-> supplier :display_name str/trim not-empty)
                             (:supplier_guess review-data)
                             (:supplier-guess review-data))
            purchased-at* (parse-instant! :purchased_at purchased_at)
            currency* (normalize-currency! currency)
            total* (parse-money total_amount)
            lines* (lines-total items)
            abs-dec (fn [d] (if (neg? d) (- d) d))
            ;; IMPORTANT: keep `total_amount_guess` stable (it's the OCR/extraction guess).
            ;; When a user edits line items and saves a review, we store the reviewed items,
            ;; but we do NOT overwrite the original total guess.
            guess-total (:total_amount_guess receipt)
            totals-match? (when (and (some? guess-total) (some? lines*))
                            (<= (abs-dec (- guess-total lines*)) 0.01M))
            new-status (if (and (= "review_required" (:status receipt)) (true? totals-match?))
                         "extracted"
                         (:status receipt))]

        (when-not supplier-uuid
          (throw (ex-info "supplier_id is required" {:status 400 :field :supplier_id})))
        (when-not (some? purchased-at*)
          (throw (ex-info "purchased_at is required" {:status 400 :field :purchased_at})))
        (when-not (seq items)
          (throw (ex-info "items is required" {:status 400 :field :items})))
        (when-not (some? total*)
          (throw (ex-info "total_amount is required" {:status 400 :field :total_amount})))

        (jdbc/execute-one!
          tx
          (sql/format
            {:update :receipts
             :set {:raw_extract_json
                   [:call :jsonb_set
                    [:call :coalesce :raw_extract_json [:cast "{}" :jsonb]]
                ;; jsonb_set expects a `text[]` path; use a typed array literal.
                    [:raw "'{extraction,items}'::text[]"]
                    (jsonb-value items)
                    true]
                   :supplier_guess supplier-guess
                   :currency_guess (when currency* [:cast currency* :currency])
                   :purchased_at_guess purchased-at*
                   :status (receipt-status-cast new-status)
                   :updated_at [:now]}
             :where [:= :id receipt-id]
             :returning [:*]})
          {:builder-fn rs/as-unqualified-lower-maps})))))

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

(defn- jsonb-value
  "Convert a Clojure value into a HoneySQL expression that writes JSONB.

  Accepts either a Clojure value (map/vector/etc) or a pre-encoded JSON string."
  [x]
  (cond
    (nil? x) nil
    ;; Already a HoneySQL expression
    (and (vector? x) (= :cast (first x))) x
    (string? x) [:cast x :jsonb]
    :else [:cast (json/generate-string x) :jsonb]))

(defn- receipt-status-cast [status]
  (when status
    [:cast status :receipt_status]))

(def ^:private local-receipt-storage-base-dir
  (io/file "upload" "stripes"))

(defn resolve-local-receipt-file
  "Return a java.io.File for a local receipt storage key (relative to `upload/stripes/`).

  Returns nil when:
  - storage-key is blank
  - the resolved file does not exist

  Throws ex-info with :status 400 when the path is unsafe (escapes base dir)."
  [storage-key]
  (let [k (some-> storage-key str/trim not-empty)]
    (when k
      (try
        (let [base-path (.toPath local-receipt-storage-base-dir)
              resolved (.normalize (.resolve base-path k))]
          (when-not (.startsWith resolved base-path)
            (throw (ex-info "Unsafe storage_key path" {:status 400 :storage_key k})))
          (let [f (.toFile resolved)]
            (when (.exists f) f)))
        (catch clojure.lang.ExceptionInfo e
          (throw (ex-info (ex-message e)
                   (assoc (ex-data e) :status 400)
                   e)))))))

;; File move functionality removed - receipts stay in upload/stripes/
;; Status is tracked via database receipts.status column

(defn- delete-receipt-file!
  "Delete the receipt file from disk if it exists.
   
   Handles both regular files (upload/stripes/<storage_key>) and exported files
   (upload/stripes/exported/<storage_key>).

   Logs errors but does not throw - deletion should succeed even if file cleanup fails."
  [receipt]
  (when-let [storage-key (:storage_key receipt)]
    (try
      (when-let [f (resolve-local-receipt-file storage-key)]
        (Files/deleteIfExists (.toPath f))
        (log/info "Deleted receipt file" {:receipt-id (:id receipt)
                                          :storage-key storage-key
                                          :path (.getAbsolutePath f)}))
      (catch Exception e
        (log/warn e "Failed to delete receipt file" {:receipt-id (:id receipt)
                                                     :storage-key storage-key})))))

;; File move functionality removed - receipts stay in upload/stripes/
;; Status is tracked via database receipts.status column

;; ============================================================================
;; CRUD / status management
;; ============================================================================

(defn upload-receipt!
  "Insert a new receipt record. Expects at least :storage_key and either
   :file_hash or :bytes to hash. Returns {:duplicate? bool :receipt {...}}."
  [db {:keys [user_id storage_key file_hash bytes original_filename content_type file_size] :as data}]
  (let [hash (or file_hash (compute-file-hash bytes))]
    (when-not storage_key
      (throw (ex-info "storage_key is required" {:data data})))
    (when-not hash
      (throw (ex-info "file_hash or bytes required" {:data data})))

    (if-let [existing (check-duplicate db hash)]
      {:duplicate? true :receipt existing}
      (let [row {:id (UUID/randomUUID)
                 :user_id user_id
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

(defn claim-status!
  "Atomically transition receipt from one status to another.
  If `from-status` is a collection, it will match any of them.
  this also reclaims receipts already in to-status when their `updated_at` is
  older than the lease.

  Returns the updated receipt row, or nil when not claimed."
  ([db receipt-id from-status to-status]
   (claim-status! db receipt-id from-status to-status nil))
  ([db receipt-id from-status to-status {:keys [lease-seconds] :or {lease-seconds 900}}]
   (let [from-coll (if (coll? from-status) from-status [from-status])
         from* (map receipt-status-cast from-coll)
         to* (receipt-status-cast to-status)]
     (jdbc/execute-one!
       db
       (sql/format
         {:update :receipts
          :set {:status to*
                :updated_at [:now]}
          :where [:and
                  [:= :id receipt-id]
                  [:or
                   [:in :status from*]
                   [:and
                    [:= :status to*]
                    [:< :updated_at [:raw (format "NOW() - INTERVAL '%s seconds'" (long lease-seconds))]]]]]
          :returning [:*]})
       {:builder-fn rs/as-unqualified-lower-maps}))))

(defn claim-for-parsing!
  "Claim a receipt for parsing (uploaded → parsing)."
  [db receipt-id & [opts]]
  (claim-status! db receipt-id "uploaded" "parsing" opts))

(defn claim-for-extracting!
  "Claim a receipt for extraction (parsed or uploaded → extracting)."
  [db receipt-id & [opts]]
  (claim-status! db receipt-id ["parsed" "uploaded"] "extracting" opts))

(defn mark-failed!
  "Mark receipt as failed with message/details."
  [db receipt-id message & [details]]
  (update-status! db receipt-id "failed"
    (merge {:error_message message
            :retry_count [:+ :retry_count 1]}
      (when details {:error_details (jsonb-value details)}))))

(defn retry-extraction!
  "Reset receipt for re-processing and increment retry_count."
  [db receipt-id]
  (update-status! db receipt-id "uploaded" {:retry_count [:+ :retry_count 1]}))

(defn reset-for-ocr!
  "Force-reset a receipt for OCR re-processing.

  Unlike `retry-extraction!`, this also clears OCR-related fields so the
  receipt can be cleanly re-parsed/extracted. Use when the user explicitly
  requests a fresh OCR pass.

  Clears: error_message, error_details, raw_parse_json, raw_extract_json,
          parsed_markdown, supplier_guess, total_amount_guess, currency_guess,
          purchased_at_guess

  Increments: retry_count

  Returns the updated receipt row."
  [db receipt-id]
  (jdbc/execute-one!
    db
    (sql/format {:update :receipts
                 :set {:status (receipt-status-cast "uploaded")
                       :error_message nil
                       :error_details nil
                       :raw_parse_json nil
                       :raw_extract_json nil
                       :parsed_markdown nil
                       :supplier_guess nil
                       :total_amount_guess nil
                       :currency_guess nil
                       :purchased_at_guess nil
                       :retry_count [:+ :retry_count 1]
                       :updated_at [:now]}
                 :where [:= :id receipt-id]
                 :returning [:*]})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn store-extraction-results!
  "Persist extraction/parse payloads and guesses.

  Updates only fields present in the input map (so callers can PATCH-like update
  without wiping other columns)."
  [db receipt-id {:keys [raw_parse_json raw_extract_json parsed_markdown supplier_guess
                         total_amount_guess currency_guess purchased_at_guess]
                  :as data}]
  (let [set-map (cond-> {:updated_at [:now]}
                  (contains? data :raw_parse_json) (assoc :raw_parse_json (jsonb-value raw_parse_json))
                  (contains? data :raw_extract_json) (assoc :raw_extract_json (jsonb-value raw_extract_json))
                  (contains? data :parsed_markdown) (assoc :parsed_markdown parsed_markdown)
                  (contains? data :supplier_guess) (assoc :supplier_guess supplier_guess)
                  (contains? data :total_amount_guess) (assoc :total_amount_guess total_amount_guess)
                  (contains? data :currency_guess) (assoc :currency_guess (when currency_guess [:cast currency_guess :currency]))
                  (contains? data :purchased_at_guess) (assoc :purchased_at_guess purchased_at_guess))]
    (jdbc/execute-one!
      db
      (sql/format {:update :receipts
                   :set set-map
                   :where [:= :id receipt-id]
                   :returning [:*]})
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn get-receipt
  [db receipt-id]
  (jdbc/execute-one!
    db
    (sql/format {:select [:*]
                 :from [:receipts]
                 :where [:= :id receipt-id]})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn delete-receipt!
  "Hard-delete a receipt and return the deleted row.

  Safety rules:
  - Disallow deleting receipts that have already been posted / linked to an expense.

  Also deletes the associated receipt image file from disk.

  Returns the deleted receipt row (map) or nil if the receipt did not exist."
  [db receipt-id]
  (jdbc/with-transaction [tx db]
    (when-let [receipt (get-receipt tx receipt-id)]
      (when (= "posted" (:status receipt))
        (throw (ex-info "Cannot delete a posted receipt"
                 {:status 409
                  :id receipt-id
                  :current-status (:status receipt)})))
      (let [expense-id (:expense_id receipt)]
        (when expense-id
          ;; Self-heal stale links: if the linked expense is already soft-deleted (or missing),
          ;; allow deleting the receipt.
          (when (expenses/get-expense tx expense-id)
            (throw (ex-info "Cannot delete a receipt linked to an expense"
                     {:status 409
                      :id receipt-id
                      :expense-id expense-id})))))
      ;; Delete the file from disk before deleting the database record
      (delete-receipt-file! receipt)
      (jdbc/execute-one!
        tx
        (sql/format {:delete-from :receipts
                     :where [:= :id receipt-id]
                     :returning [:*]})
        {:builder-fn rs/as-unqualified-lower-maps}))))

(defn list-receipts
  "List receipts with optional status filter.

  Returns a lightweight projection for list views (detail endpoints return
  raw_extract_json / parsed_markdown, etc.)."
  [db {:keys [status limit offset order-dir]
       :or {limit 50 offset 0 order-dir :desc}}]
  (let [;; Sum extracted line totals (supports both line_total and line-total keys).
        lines-total-sql
        (str
          "(select sum("
          "nullif(replace(regexp_replace(coalesce(item->>'line_total', item->>'line-total',''), '[^0-9,.-]', '', 'g'), ',', '.'), '')::numeric"
          ") from jsonb_array_elements(coalesce(raw_extract_json->'extraction'->'items','[]'::jsonb)) as item)")

        mismatch-sql
        (str
          "status = 'extracted'::receipt_status"
          " and total_amount_guess is not null"
          " and " lines-total-sql " is not null"
          " and abs((" lines-total-sql ") - total_amount_guess) > 0.01")

        mismatch-clause [:raw mismatch-sql]
        not-mismatch-clause [:raw (str "not (" mismatch-sql ")")]

        ;; Treat extracted-but-mismatched receipts as review_required for UX.
        status-clause (cond
                        (string? status)
                        (case status
                          "review_required" [:or
                                             [:= :status (receipt-status-cast "review_required")]
                                             mismatch-clause]
                          "extracted" [:and
                                       [:= :status (receipt-status-cast "extracted")]
                                       not-mismatch-clause]
                          [:= :status (receipt-status-cast status)])

                        (sequential? status)
                        (let [sset (set status)
                              base [:in :status (mapv receipt-status-cast status)]
                              want-review? (contains? sset "review_required")
                              want-extracted? (contains? sset "extracted")]
                          (cond
                            (and want-review? (not want-extracted?)) [:or base mismatch-clause]
                            (and want-extracted? (not want-review?)) [:and base not-mismatch-clause]
                            :else base))

                        :else nil)

        effective-status-sql (str
                               "case when (" mismatch-sql ") then 'review_required'::receipt_status else status end")

        query (cond-> {:select [:id
                                :original_filename
                                [[:raw effective-status-sql] :status]
                                :supplier_guess
                                :total_amount_guess
                                [[:raw lines-total-sql] :lines_total_amount_guess]
                                :currency_guess
                                :created_at
                                :updated_at]
                       :from [:receipts]
                       :order-by [[:created_at order-dir]]
                       :limit limit
                       :offset offset}
                status-clause (assoc :where status-clause))]
    (jdbc/execute! db (sql/format query) {:builder-fn rs/as-unqualified-lower-maps})))

(defn list-user-receipts
  "List receipts visible to a specific user.

  Visibility rules:
  - receipts owned by `user-id`
  - receipts with no `user_id` (unassigned/admin-uploaded)

  Supports optional status filter.

  Returns a lightweight projection for list views (detail endpoints return
  raw_extract_json / parsed_markdown, etc.)."
  [db user-id {:keys [status limit offset order-dir]
               :or {limit 50 offset 0 order-dir :desc}}]
  (when-not user-id
    (throw (ex-info "user-id is required" {:status 400})))
  (let [visibility-clause [:or
                           [:= :user_id user-id]
                           [:is :user_id nil]]

        ;; Sum extracted line totals (supports both line_total and line-total keys).
        lines-total-sql
        (str
          "(select sum("
          "nullif(replace(regexp_replace(coalesce(item->>'line_total', item->>'line-total',''), '[^0-9,.-]', '', 'g'), ',', '.'), '')::numeric"
          ") from jsonb_array_elements(coalesce(raw_extract_json->'extraction'->'items','[]'::jsonb)) as item)")

        mismatch-sql
        (str
          "status = 'extracted'::receipt_status"
          " and total_amount_guess is not null"
          " and " lines-total-sql " is not null"
          " and abs((" lines-total-sql ") - total_amount_guess) > 0.01")

        mismatch-clause [:raw mismatch-sql]
        not-mismatch-clause [:raw (str "not (" mismatch-sql ")")]

        ;; Treat extracted-but-mismatched receipts as review_required for UX.
        status-clause (cond
                        (string? status)
                        (case status
                          "review_required" [:or
                                             [:= :status (receipt-status-cast "review_required")]
                                             mismatch-clause]
                          "extracted" [:and
                                       [:= :status (receipt-status-cast "extracted")]
                                       not-mismatch-clause]
                          [:= :status (receipt-status-cast status)])

                        (sequential? status)
                        (let [sset (set status)
                              base [:in :status (mapv receipt-status-cast status)]
                              want-review? (contains? sset "review_required")
                              want-extracted? (contains? sset "extracted")]
                          (cond
                            (and want-review? (not want-extracted?)) [:or base mismatch-clause]
                            (and want-extracted? (not want-review?)) [:and base not-mismatch-clause]
                            :else base))

                        :else nil)

        where-clause (cond
                       status-clause [:and visibility-clause status-clause]
                       :else visibility-clause)

        effective-status-sql (str
                               "case when (" mismatch-sql ") then 'review_required'::receipt_status else status end")

        query {:select [:id
                        :original_filename
                        [[:raw effective-status-sql] :status]
                        :supplier_guess
                        :total_amount_guess
                        [[:raw lines-total-sql] :lines_total_amount_guess]
                        :currency_guess
                        :created_at
                        :updated_at]
               :from [:receipts]
               :where where-clause
               :order-by [[:created_at order-dir]]
               :limit limit
               :offset offset}]
    (jdbc/execute! db (sql/format query) {:builder-fn rs/as-unqualified-lower-maps})))

(defn get-user-receipt
  "Fetch a single receipt visible to `user-id`.

  Visibility rules:
  - receipts owned by `user-id`
  - receipts with no `user_id` (unassigned/admin-uploaded)

  Returns nil when not found or not visible."
  [db user-id receipt-id]
  (when-not user-id
    (throw (ex-info "user-id is required" {:status 400})))
  (let [receipt (get-receipt db receipt-id)]
    (when (and receipt
            (or (= user-id (:user_id receipt))
              (nil? (:user_id receipt))))
      receipt)))

(defn approve-and-post-for-user!
  "Create an expense from a receipt for a specific user and update status → posted.

  Receipt must be visible to the user:
  - owned by user-id, OR
  - unassigned (user_id is NULL)

  If the receipt is unassigned, it is claimed by setting :user_id to user-id.

  review-data expects keys for expenses/create-expense! including :supplier_id,
  :payer_id, :purchased_at, :total_amount, :currency, :notes, :items."
  [db user-id receipt-id review-data]
  (jdbc/with-transaction [tx db]
    (let [receipt (get-user-receipt tx user-id receipt-id)]
      (when-not receipt
        ;; Use 404 to avoid leaking existence of other users' receipts.
        (throw (ex-info "Receipt not found" {:status 404 :id receipt-id})))
      (when-not (approvable-status? (:status receipt))
        (throw (ex-info "Receipt not in approvable status"
                 {:status 409 :id receipt-id :current-status (:status receipt)})))

      (let [expense (expenses/create-expense!
                      tx
                      (merge {:receipt_id receipt-id
                              :user_id user-id
                              :currency (or (:currency review-data) (:currency_guess receipt) "BAM")}
                        review-data)
                      (:items review-data))
            claim? (nil? (:user_id receipt))
            extra (cond-> {:expense_id (:id expense)}
                    claim? (assoc :user_id user-id))]
        (update-status! tx receipt-id "posted" extra)
        expense))))

(defn approve-and-post-for-user-any!
  "Create an expense from a receipt as a user, without enforcing receipt ownership.

  Intended for user-role admins in the user UI who can process any receipt.

  If the receipt is unassigned (`user_id` is NULL), it is claimed by setting :user_id to user-id.

  review-data expects keys for expenses/create-expense! including :supplier_id,
  :payer_id, :purchased_at, :total_amount, :currency, :notes, :items."
  [db user-id receipt-id review-data]
  (when-not user-id
    (throw (ex-info "user-id is required" {:status 400})))
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
                              :user_id user-id
                              :currency (or (:currency review-data) (:currency_guess receipt) "BAM")}
                        review-data)
                      (:items review-data))
            claim? (nil? (:user_id receipt))
            extra (cond-> {:expense_id (:id expense)}
                    claim? (assoc :user_id user-id))]
        (update-status! tx receipt-id "posted" extra)
        expense))))

(defn list-pending-for-processing
  "Receipts that are ready to process (uploaded or failed-but-retry)."
  [db]
  (let [statuses (mapv receipt-status-cast ["uploaded" "parsing" "parsed" "extracting"])]
    (jdbc/execute!
      db
      (sql/format {:select [:*]
                   :from [:receipts]
                   :where [:in :status statuses]
                   :order-by [[:created_at :asc]]})
      {:builder-fn rs/as-unqualified-lower-maps})))

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
                      (:items review-data))
            extra {:expense_id (:id expense)}]
        (update-status! tx receipt-id "posted" extra)
        expense))))
