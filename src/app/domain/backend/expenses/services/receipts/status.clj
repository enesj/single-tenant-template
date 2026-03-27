(ns app.domain.backend.expenses.services.receipts.status
  "Receipt status transitions, claims, and extraction result storage."
  (:require
    [app.domain.backend.expenses.services.receipts.storage :as storage]
    [cheshire.core :as json]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]))

(defn update-status!
  "Update receipt status (and optional extra fields)."
  [db receipt-id new-status & [extra]]
  (jdbc/execute-one!
    db
    (sql/format {:update :receipts
                 :set (merge {:status [:cast new-status :receipt_status]}
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
         from* (map storage/receipt-status-cast from-coll)
         to* (storage/receipt-status-cast to-status)]
     (jdbc/execute-one!
       db
       (sql/format
         {:update :receipts
          :set {:status to*}
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
      (when details {:error_details (storage/jsonb-value details)}))))

(defn retry-extraction!
  "Reset receipt for re-processing and increment retry_count."
  [db receipt-id]
  (update-status! db receipt-id "uploaded" {:retry_count [:+ :retry_count 1]}))

(defn linked-expense-id
  "Return the linked expense id for a receipt, if any.
   Checks the receipt pointer first, then falls back to expenses.receipt_id for
   defensive consistency when data drift exists."
  [db receipt-id]
  (let [receipt (jdbc/execute-one!
                  db
                  (sql/format {:select [:expense_id]
                               :from [:receipts]
                               :where [:= :id receipt-id]
                               :limit 1})
                  {:builder-fn rs/as-unqualified-lower-maps})]
    (or (:expense_id receipt)
      (:id (jdbc/execute-one!
             db
             (sql/format {:select [:id]
                          :from [:expenses]
                          :where [:= :receipt_id receipt-id]
                          :limit 1})
             {:builder-fn rs/as-unqualified-lower-maps})))))

(defn ensure-reset-for-ocr-allowed!
  "Reject OCR reset when a receipt is already linked to an expense.
   The caller must explicitly unlink the expense first."
  [db receipt-id]
  (when-let [expense-id (linked-expense-id db receipt-id)]
    (throw (ex-info "Receipt already linked to an expense. Unlink it first before reparsing"
             {:status 409
              :field :receipt_id
              :receipt-id receipt-id
              :expense-id expense-id}))))

(defn reset-for-ocr!
  "Force-reset a receipt for OCR re-processing.

  Unlike `retry-extraction!`, this also clears OCR-related fields so the
  receipt can be cleanly re-parsed/extracted. Use when the user explicitly
  requests a fresh OCR pass.

  Linked receipts are not eligible for reparse; callers must explicitly unlink
  the expense first.

  Clears: error_message, error_details, raw_extract_json,
          parsed_markdown, supplier_guess, total_amount_guess, currency_guess,
          purchased_at_guess

  Increments: retry_count

  Returns the updated receipt row."
  [db receipt-id]
  (ensure-reset-for-ocr-allowed! db receipt-id)
  (jdbc/execute-one!
    db
    (sql/format {:update :receipts
                 :set {:status (storage/receipt-status-cast "uploaded")
                       :error_message nil
                       :error_details nil
                       :raw_extract_json nil
                       :parsed_markdown nil
                       :supplier_guess nil
                       :supplier_alias_id nil
                       :store_guess nil
                       :store_alias_id nil
                       :total_amount_guess nil
                       :currency_guess nil
                       :purchased_at_guess nil
                       :retry_count [:+ :retry_count 1]}
                 :where [:= :id receipt-id]
                 :returning [:*]})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn store-extraction-results!
  "Persist extraction/parse payloads and guesses.

  Updates only fields present in the input map (so callers can PATCH-like update
  without wiping other columns)."
  [db receipt-id {:keys [raw_extract_json parsed_markdown
                         supplier_guess supplier_alias_id
                         store_guess store_alias_id
                         total_amount_guess currency_guess purchased_at_guess]
                  :as data}]
  (let [set-map (cond-> {}
                  (contains? data :raw_extract_json) (assoc :raw_extract_json (storage/jsonb-value raw_extract_json))
                  (contains? data :parsed_markdown) (assoc :parsed_markdown parsed_markdown)
                  (contains? data :supplier_guess) (assoc :supplier_guess supplier_guess)
                  (contains? data :supplier_alias_id) (assoc :supplier_alias_id supplier_alias_id)
                  (contains? data :store_guess) (assoc :store_guess store_guess)
                  (contains? data :store_alias_id) (assoc :store_alias_id store_alias_id)
                  (contains? data :total_amount_guess) (assoc :total_amount_guess total_amount_guess)
                  (contains? data :currency_guess) (assoc :currency_guess (when currency_guess [:cast currency_guess :currency]))
                  (contains? data :purchased_at_guess) (assoc :purchased_at_guess purchased_at_guess))]
    (if (seq set-map)
      (jdbc/execute-one!
        db
        (sql/format {:update :receipts
                     :set set-map
                     :where [:= :id receipt-id]
                     :returning [:*]})
        {:builder-fn rs/as-unqualified-lower-maps})
      (jdbc/execute-one!
        db
        (sql/format {:select [:*]
                     :from [:receipts]
                     :where [:= :id receipt-id]
                     :limit 1})
        {:builder-fn rs/as-unqualified-lower-maps}))))

(defn store-receipt-refine-context!
  "Persist receipt refine context into `receipts.raw_extract_json` under key `receipt_refine/context`.

  This is for observability/debugging. It should not change extraction/refine behavior.

  Returns {:id <receipt-id>} when updated, otherwise nil."
  [db receipt-id context]
  (when (and db receipt-id (map? context))
    (jdbc/execute-one!
      db
      [(str "update receipts "
         "set raw_extract_json = jsonb_set("
         "coalesce(raw_extract_json, '{}'::jsonb), "
         "'{receipt_refine/context}', "
         "?::jsonb, true) "
         "where id = ? "
         "returning id")
       (json/generate-string context)
       receipt-id]
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn clear-refine-pending!
  "Clear the `raw_extract_json.refine_pending` flag.

  This is primarily used to prevent the UI from staying in a perpetual
  refining/polling state when the optional LLM refine step is skipped,
  fails, or times out.

  Note: This does not change the receipt status (e.g. review_required stays
  review_required)."
  [db receipt-id]
  (jdbc/execute-one!
    db
    (sql/format
      {:update :receipts
       :set {:raw_extract_json [:raw "jsonb_set(coalesce(raw_extract_json, '{}'::jsonb), '{refine_pending}', 'false'::jsonb, true)"]}
       :where [:= :id receipt-id]
       :returning [:id]})
    {:builder-fn rs/as-unqualified-lower-maps}))
