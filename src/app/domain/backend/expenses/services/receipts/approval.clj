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
    [clojure.string :as str]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]
    [taoensso.timbre :as log]))

(defn save-review!
  "Persist reviewed receipt values without posting an expense.

  - Updates raw_extract_json.extraction.items to reviewed items
  - Updates supplier_guess/total_amount_guess/currency_guess/purchased_at_guess
  - Optionally flips status review_required → extracted when totals match

  Returns the updated receipt row."
  [db receipt-id {:keys [supplier_id purchased_at total_amount currency items] :as review-data}]
  (jdbc/with-transaction [tx db]
    (let [receipt (queries/get-receipt tx receipt-id)]
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
                 ;; User selected a canonical supplier during review: this is a strong
                 ;; signal that the raw supplier label should map to that supplier.
                 ;; We intentionally overwrite mappings here (user intent), unlike
                 ;; automated OCR flows which only map when unmapped.
                (try
                  (supplier-aliases/map-alias-to-supplier! tx supplier-alias-id supplier-uuid 100)
                  (catch Exception e
                    (log/warn e "Failed to map alias to supplier during review"
                      {:receipt-id receipt-id
                       :supplier-alias-id supplier-alias-id
                       :supplier-uuid supplier-uuid})
                    nil)))
            purchased-at* (parsing/parse-instant! :purchased_at purchased_at)
            currency* (parsing/normalize-currency! currency)
            total* (parsing/parse-money total_amount)
            lines* (parsing/lines-total items)
            abs-dec (fn [d] (if (neg? d) (- d) d))
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
                    [:raw "'{extraction,items}'::text[]"]
                    (storage/jsonb-value items)
                    true]
                   :supplier_guess supplier-guess
                   :supplier_alias_id supplier-alias-id
                   :currency_guess (when currency* [:cast currency* :currency])
                   :purchased_at_guess purchased-at*
                   :status (storage/receipt-status-cast new-status)}
             :where [:= :id receipt-id]
             :returning [:*]})
          {:builder-fn rs/as-unqualified-lower-maps})))))

(defn approve-and-post!
  "Create an expense from a receipt and update status → posted.
   review-data expects keys for expenses/create-expense! including :supplier_id,
   :payer_id, :purchased_at, :total_amount, :currency, :notes, :items.

   store_id is resolved automatically from the receipt's store_alias_id when
   not explicitly provided in review-data.

   Reads tenant_id from the receipt row and includes it in the expense data."
  [db receipt-id review-data]
  (jdbc/with-transaction [tx db]
    (let [receipt (queries/get-receipt tx receipt-id)]
      (when-not receipt
        (throw (ex-info "Receipt not found" {:status 404 :id receipt-id})))
      (when-not (parsing/approvable-status? (:status receipt))
        (throw (ex-info "Receipt not in approvable status"
                 {:status 409 :id receipt-id :current-status (:status receipt)})))

      (let [context   (queries/get-receipt-refine-context tx receipt-id)
            store-id  (:store_id context)
            tenant-id (:tenant_id receipt)
            base      (cond-> {:receipt_id receipt-id
                               :currency   (or (:currency review-data) (:currency_guess receipt) "BAM")}
                        store-id  (assoc :store_id store-id)
                        tenant-id (assoc :tenant_id tenant-id))
            expense   (expenses/create-expense!
                        tx
                        (merge base review-data)
                        (:items review-data))
            extra     {:expense_id (:id expense)}]
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
    (let [receipt (queries/get-user-receipt tx user-id receipt-id tenant-id)]
      (when-not receipt
        (throw (ex-info "Receipt not found" {:status 404 :id receipt-id})))
      (when-not (parsing/approvable-status? (:status receipt))
        (throw (ex-info "Receipt not in approvable status"
                 {:status 409 :id receipt-id :current-status (:status receipt)})))

      (let [context      (queries/get-receipt-refine-context tx receipt-id)
            store-id     (:store_id context)
            receipt-tid  (:tenant_id receipt)
            base         (cond-> {:receipt_id receipt-id
                                  :user_id    user-id
                                  :currency   (or (:currency review-data) (:currency_guess receipt) "BAM")}
                           store-id    (assoc :store_id store-id)
                           receipt-tid (assoc :tenant_id receipt-tid))
            expense      (expenses/create-expense!
                           tx
                           (merge base review-data)
                           (:items review-data))
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
    (let [receipt (queries/get-receipt tx receipt-id tenant-id)]
      (when-not receipt
        (throw (ex-info "Receipt not found" {:status 404 :id receipt-id})))
      (when-not (parsing/approvable-status? (:status receipt))
        (throw (ex-info "Receipt not in approvable status"
                 {:status 409 :id receipt-id :current-status (:status receipt)})))

      (let [context      (queries/get-receipt-refine-context tx receipt-id)
            store-id     (:store_id context)
            receipt-tid  (:tenant_id receipt)
            base         (cond-> {:receipt_id receipt-id
                                  :user_id    user-id
                                  :currency   (or (:currency review-data) (:currency_guess receipt) "BAM")}
                           store-id    (assoc :store_id store-id)
                           receipt-tid (assoc :tenant_id receipt-tid))
            expense      (expenses/create-expense!
                           tx
                           (merge base review-data)
                           (:items review-data))
            claim?       (nil? (:user_id receipt))
            extra        (cond-> {:expense_id (:id expense)}
                           claim? (assoc :user_id user-id))]
        (status/update-status! tx receipt-id "posted" extra)
        expense))))
