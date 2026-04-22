(ns app.domain.backend.expenses.services.receipts.approval
  "Receipt review, approval, and posting workflows."
  (:require
    [app.domain.backend.expenses.services.expenses :as expenses]
    [app.domain.backend.expenses.services.receipts.parsing :as parsing]
    [app.domain.backend.expenses.services.receipts.queries :as queries]
    [app.domain.backend.expenses.services.receipts.status :as status]
    [app.domain.backend.expenses.services.receipts.storage :as storage]
    [app.domain.backend.expenses.services.supplier-aliases :as supplier-aliases]
    [app.domain.backend.expenses.services.suppliers :as suppliers]
    [cheshire.core :as json]
    [clojure.string :as str]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]
    [taoensso.timbre :as log]))

(defn- extract-ocr-items
  "Parse OCR extraction items from a receipt's raw_extract_json.
   Returns the extraction items vector or nil."
  [receipt]
  (try
    (when-let [raw (:raw_extract_json receipt)]
      (let [parsed (cond
                     (string? raw) (json/parse-string raw true)
                     (map? raw) raw
                     (instance? org.postgresql.util.PGobject raw)
                     (json/parse-string (.getValue ^org.postgresql.util.PGobject raw) true)
                     :else nil)
            items (get-in parsed [:extraction :items])]
        (when (seq items)
          (vec items))))
    (catch Exception e
      (log/warn e "Failed to parse OCR items from receipt"
        {:receipt-id (:id receipt)})
      nil)))

(defn- extract-ocr-item-prices
  "Return a vector of OCR unit prices (BigDecimal or nil) indexed by position."
  [ocr-items]
  (when (seq ocr-items)
    (mapv (fn [item]
            (some-> (or (:unit_price item) (:unit-price item))
              bigdec))
      ocr-items)))

(defn- preserve-ocr-item-unit
  "Fill missing approved/reviewed item units from OCR extraction items by position.

  This keeps existing UI behavior intact while preventing hidden OCR metadata
  like `:unit` from being dropped when the review form does not expose it."
  [items ocr-items]
  (if (seq ocr-items)
    (vec
      (map-indexed
        (fn [idx item]
          (let [item-unit (some-> (or (:unit item) (:item_unit item) (:item-unit item))
                            str
                            str/trim
                            not-empty)
                ocr-unit (some-> (or (:unit (get ocr-items idx))
                                   (:item_unit (get ocr-items idx))
                                   (:item-unit (get ocr-items idx)))
                           str
                           str/trim
                           not-empty)]
            (cond-> item
              (and (nil? item-unit) ocr-unit) (assoc :unit ocr-unit))))
        items))
    (vec items)))

(defn- mark-price-modified
  "Compare approved items against OCR-extracted prices by position.
   Returns items with :price_modified flag added to each."
  [approved-items ocr-prices]
  (if (seq ocr-prices)
    (vec
      (map-indexed
        (fn [idx item]
          (let [ocr-price (get ocr-prices idx)
                approved-price (some-> (or (:unit_price item) (:unit-price item))
                                 bigdec)
                modified (or (nil? ocr-price)
                           (nil? approved-price)
                           (not= (.compareTo ^java.math.BigDecimal approved-price
                                   ^java.math.BigDecimal ocr-price)
                             0))]
            (assoc item :price_modified modified)))
        approved-items))
    ;; No OCR prices to compare — treat all as unmodified (auto-post path)
    (mapv #(assoc % :price_modified false) approved-items)))

(defn- parse-raw-extract-json
  [raw]
  (try
    (cond
      (map? raw) raw
      (string? raw) (json/parse-string raw true)
      (instance? org.postgresql.util.PGobject raw)
      (json/parse-string (.getValue ^org.postgresql.util.PGobject raw) true)
      :else {})
    (catch Exception e
      (log/warn e "Failed to parse receipt raw_extract_json while saving review"
        {:raw-type (some-> raw class str)})
      {})))

(defn- reviewed-raw-extract-json
  [receipt items total]
  (-> (parse-raw-extract-json (:raw_extract_json receipt))
    (assoc-in [:extraction :items] items)
    (assoc-in [:extraction :totals :total] total)))

(def ^:private receipt-currency "BAM")

(defn- enforce-receipt-bam
  "Receipts always persist BAM, while still validating any explicit
   client-supplied currency value."
  [review-data]
  (parsing/normalize-currency! (:currency review-data))
  (assoc review-data :currency receipt-currency))

(defn save-review!
  "Persist reviewed receipt values without posting an expense.

  - Updates raw_extract_json.extraction.items to reviewed items
  - Updates raw_extract_json.extraction.totals.total and total_amount_guess to the reviewed total
  - Updates supplier_guess/supplier_alias_id/currency_guess/purchased_at_guess
  - Flips status review_required → extracted once all required fields are provided
    (supplier, purchased_at, items, total_amount). Totals alignment is a soft
    quality check handled by the effective-status layer in the API response, not
    a hard gate here.

  Returns the updated receipt row."
  [db receipt-id {:keys [supplier_id purchased_at total_amount items] :as review-data}]
  (jdbc/with-transaction [tx db]
    (let [review-data* (enforce-receipt-bam review-data)
          receipt (queries/get-receipt tx receipt-id)]
      (when-not receipt
        (throw (ex-info "Receipt not found" {:status 404 :id receipt-id})))
      (when-not (parsing/approvable-status? (:status receipt))
        (throw (ex-info "Receipt not in approvable status"
                 {:status 409 :id receipt-id :current-status (:status receipt)})))

      (let [supplier-uuid (parsing/try-parse-uuid supplier_id)
            get-supplier (:get suppliers/service)
            supplier (when supplier-uuid (get-supplier tx supplier-uuid))
            supplier-guess (or (some-> supplier :display_name str/trim not-empty)
                             (:supplier_guess review-data)
                             (:supplier-guess review-data))
            supplier-alias-id (try
                                (when-not (str/blank? (some-> supplier-guess str))
                                  (:id (supplier-aliases/find-or-create-alias! tx supplier-guess)))
                                (catch Exception e
                                  (log/warn e "Failed to find-or-create supplier alias during review"
                                    {:receipt-id receipt-id :supplier-guess supplier-guess})
                                  nil))
            _ (when (and supplier-alias-id supplier-uuid)
                (try
                  (supplier-aliases/map-alias-to-supplier! tx supplier-alias-id supplier-uuid 100)
                  (catch Exception e
                    (log/warn e "Failed to map alias to supplier during review"
                      {:receipt-id receipt-id
                       :supplier-alias-id supplier-alias-id
                       :supplier-uuid supplier-uuid})
                    nil)))
            purchased-at* (parsing/parse-instant! :purchased_at purchased_at)
            currency* (:currency review-data*)
            total* (parsing/parse-money total_amount)]

        (when-not supplier-uuid
          (throw (ex-info "supplier_id is required" {:status 400 :field :supplier_id})))
        (when-not (some? purchased-at*)
          (throw (ex-info "purchased_at is required" {:status 400 :field :purchased_at})))
        (when-not (seq items)
          (throw (ex-info "items is required" {:status 400 :field :items})))
        (when-not (some? total*)
          (throw (ex-info "total_amount is required" {:status 400 :field :total_amount})))

        (let [items* (preserve-ocr-item-unit items (extract-ocr-items receipt))
              new-status (if (= "review_required" (:status receipt))
                           "extracted"
                           (:status receipt))]
          (jdbc/execute-one!
            tx
            (sql/format
              {:update :receipts
               :set {:raw_extract_json (storage/jsonb-value (reviewed-raw-extract-json receipt items* total*))
                     :supplier_guess supplier-guess
                     :supplier_alias_id supplier-alias-id
                     :currency_guess (when currency* [:cast currency* :currency])
                     :purchased_at_guess purchased-at*
                     :total_amount_guess total*
                     :status (storage/receipt-status-cast new-status)}
               :where [:= :id receipt-id]
               :returning [:*]})
            {:builder-fn rs/as-unqualified-lower-maps}))))))

(defn approve-and-post!
  "Create an expense from a receipt and update status → posted.
   review-data expects keys for expenses/create-expense! including :supplier_id,
   :payer_id, :purchased_at, :total_amount, :currency, :notes, :items.

   store_id is resolved automatically from the receipt's store_alias_id when
   not explicitly provided in review-data.

   Reads tenant_id from the receipt row and includes it in the expense data.
   Sets created_by from the receipt's user_id (admin path has no acting user)."
  [db receipt-id review-data]
  (jdbc/with-transaction [tx db]
    (let [review-data* (enforce-receipt-bam review-data)
          receipt (queries/get-receipt tx receipt-id)]
      (when-not receipt
        (throw (ex-info "Receipt not found" {:status 404 :id receipt-id})))
      (when-not (parsing/approvable-status? (:status receipt))
        (throw (ex-info "Receipt not in approvable status"
                 {:status 409 :id receipt-id :current-status (:status receipt)})))

      (let [context    (queries/get-receipt-refine-context tx receipt-id)
            store-id   (:store_id context)
            tenant-id  (:tenant_id receipt)
            ocr-items  (extract-ocr-items receipt)
            ocr-prices (extract-ocr-item-prices ocr-items)
          items      (-> (:items review-data*)
                         (preserve-ocr-item-unit ocr-items)
                         (mark-price-modified ocr-prices))
            base       (cond-> {:receipt_id receipt-id
                                :created_by (:user_id receipt)
                  :currency   receipt-currency}
                         store-id  (assoc :store_id store-id)
                         tenant-id (assoc :tenant_id tenant-id))
            expense    (expenses/create-expense!
                         tx
              (merge base review-data*)
                         items)
            extra      {:expense_id (:id expense)}]
        (status/update-status! tx receipt-id "posted" extra)
        expense))))

(defn approve-and-post-for-user!
  "Create an expense from a receipt for a specific user and update status → posted.

  Receipt must be visible to the user:
  - owned by user-id, OR
  - unassigned (user_id is NULL)

  If the receipt is unassigned, it is claimed by setting :user_id to user-id.

  store_id is resolved automatically from the receipt's store_alias_id when
  not explicitly provided in review-data. tenant_id is read from the receipt row.

  review-data expects keys for expenses/create-expense! including :supplier_id,
  :payer_id, :purchased_at, :total_amount, :currency, :notes, :items."
  [db user-id receipt-id review-data & {:keys [tenant-id]}]
  (jdbc/with-transaction [tx db]
    (let [review-data* (enforce-receipt-bam review-data)
          receipt (queries/get-user-receipt tx user-id receipt-id tenant-id)]
      (when-not receipt
        (throw (ex-info "Receipt not found" {:status 404 :id receipt-id})))
      (when-not (parsing/approvable-status? (:status receipt))
        (throw (ex-info "Receipt not in approvable status"
                 {:status 409 :id receipt-id :current-status (:status receipt)})))

      (let [context      (queries/get-receipt-refine-context tx receipt-id)
            store-id     (:store_id context)
            receipt-tid  (:tenant_id receipt)
            ocr-items    (extract-ocr-items receipt)
            ocr-prices   (extract-ocr-item-prices ocr-items)
          items        (-> (:items review-data*)
                           (preserve-ocr-item-unit ocr-items)
                           (mark-price-modified ocr-prices))
            base         (cond-> {:receipt_id receipt-id
                                  :user_id    user-id
                                  :created_by user-id
                    :currency   receipt-currency}
                           store-id    (assoc :store_id store-id)
                           receipt-tid (assoc :tenant_id receipt-tid))
            expense      (expenses/create-expense!
                           tx
                (merge base review-data*)
                           items)
            claim?       (nil? (:user_id receipt))
            extra        (cond-> {:expense_id (:id expense)}
                           claim? (assoc :user_id user-id))]
        (status/update-status! tx receipt-id "posted" extra)
        expense))))

(defn approve-and-post-for-user-any!
  "Create an expense from a receipt as a user, without enforcing receipt ownership.

  Intended for user-role admins in the user UI who can process any receipt.

  If the receipt is unassigned (`user_id` is NULL), it is claimed by setting :user_id to user-id.

  store_id is resolved automatically from the receipt's store_alias_id when
  not explicitly provided in review-data. tenant_id is read from the receipt row.

  review-data expects keys for expenses/create-expense! including :supplier_id,
  :payer_id, :purchased_at, :total_amount, :currency, :notes, :items."
  [db user-id receipt-id review-data & {:keys [tenant-id]}]
  (when-not user-id
    (throw (ex-info "user-id is required" {:status 400})))
  (jdbc/with-transaction [tx db]
    (let [review-data* (enforce-receipt-bam review-data)
          receipt (queries/get-receipt tx receipt-id tenant-id)]
      (when-not receipt
        (throw (ex-info "Receipt not found" {:status 404 :id receipt-id})))
      (when-not (parsing/approvable-status? (:status receipt))
        (throw (ex-info "Receipt not in approvable status"
                 {:status 409 :id receipt-id :current-status (:status receipt)})))

      (let [context      (queries/get-receipt-refine-context tx receipt-id)
            store-id     (:store_id context)
            receipt-tid  (:tenant_id receipt)
            ocr-items    (extract-ocr-items receipt)
            ocr-prices   (extract-ocr-item-prices ocr-items)
          items        (-> (:items review-data*)
                           (preserve-ocr-item-unit ocr-items)
                           (mark-price-modified ocr-prices))
            base         (cond-> {:receipt_id receipt-id
                                  :user_id    user-id
                                  :created_by user-id
                    :currency   receipt-currency}
                           store-id    (assoc :store_id store-id)
                           receipt-tid (assoc :tenant_id receipt-tid))
            expense      (expenses/create-expense!
                           tx
                (merge base review-data*)
                           items)
            claim?       (nil? (:user_id receipt))
            extra        (cond-> {:expense_id (:id expense)}
                           claim? (assoc :user_id user-id))]
        (status/update-status! tx receipt-id "posted" extra)
        expense))))

;; ---------------------------------------------------------------------------
;; Update posted receipt + linked expense
;; ---------------------------------------------------------------------------

(defn- update-receipt-metadata!
  "Update receipt metadata columns to reflect edited review data."
  [tx receipt review-data supplier supplier-alias-id]
  (let [review-data* (enforce-receipt-bam review-data)
        purchased-at* (parsing/parse-instant! :purchased_at (:purchased_at review-data*))
        currency* (:currency review-data*)
        total* (parsing/parse-money (:total_amount review-data*))
        items* (preserve-ocr-item-unit
                 (or (:items review-data*) [])
                 (extract-ocr-items receipt))
        supplier-guess (or (some-> supplier :display_name str/trim not-empty)
                         (:supplier_guess review-data*)
                         (:supplier-guess review-data*))]
    (jdbc/execute-one!
      tx
      (sql/format
        {:update :receipts
         :set {:raw_extract_json (storage/jsonb-value
                                   (reviewed-raw-extract-json receipt items* total*))
               :supplier_guess supplier-guess
               :supplier_alias_id supplier-alias-id
               :currency_guess (when currency* [:cast currency* :currency])
               :purchased_at_guess purchased-at*
               :total_amount_guess total*}
         :where [:= :id (:id receipt)]
         :returning [:*]})
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn update-posted-receipt!
  "Update a posted receipt AND its linked expense in a single transaction.

  Validates:
  - Receipt exists and is in 'posted' status
  - Receipt has a linked expense_id

  Updates:
  - Receipt metadata (supplier_guess, total_amount_guess, currency_guess, etc.)
  - Linked expense (supplier_id, payer_id, purchased_at, total_amount, currency, notes, items)

  Returns {:expense <updated-expense> :receipt <updated-receipt>}."
  [db receipt-id review-data & {:keys [tenant-id]}]
  (jdbc/with-transaction [tx db]
    (let [review-data* (enforce-receipt-bam review-data)
          receipt (queries/get-receipt tx receipt-id)
          _ (when-not receipt
              (throw (ex-info "Receipt not found" {:status 404 :id receipt-id})))
          _ (when-not (= "posted" (:status receipt))
              (throw (ex-info "Receipt is not in posted status"
                       {:status 409 :id receipt-id :current-status (:status receipt)})))
          expense-id (:expense_id receipt)
          _ (when-not expense-id
              (throw (ex-info "Posted receipt has no linked expense"
                       {:status 409 :id receipt-id})))

          ;; Resolve supplier
          supplier-uuid (parsing/try-parse-uuid (:supplier_id review-data*))
          get-supplier (:get suppliers/service)
          supplier (when supplier-uuid (get-supplier tx supplier-uuid))
          supplier-guess (or (some-> supplier :display_name str/trim not-empty)
                           (:supplier_guess review-data*)
                           (:supplier-guess review-data*))
          supplier-alias-id (try
                              (when-not (str/blank? (some-> supplier-guess str))
                                (:id (supplier-aliases/find-or-create-alias! tx supplier-guess)))
                              (catch Exception e
                                (log/warn e "Failed to find-or-create supplier alias during posted update"
                                  {:receipt-id receipt-id :supplier-guess supplier-guess})
                                nil))
          _ (when (and supplier-alias-id supplier-uuid)
              (try
                (supplier-aliases/map-alias-to-supplier! tx supplier-alias-id supplier-uuid 100)
                (catch Exception e
                  (log/warn e "Failed to map alias to supplier during posted update"
                    {:receipt-id receipt-id
                     :supplier-alias-id supplier-alias-id
                     :supplier-uuid supplier-uuid})
                  nil)))

          ;; Update receipt metadata
          updated-receipt (update-receipt-metadata!
                            tx receipt review-data* supplier supplier-alias-id)

          ;; Update linked expense (including items)
          updated-expense (expenses/update-expense! tx expense-id review-data* tenant-id {:allow-linked-expense-update? true})]
      {:expense updated-expense
       :receipt updated-receipt})))
