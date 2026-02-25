(ns app.domain.backend.expenses.workers.receipt-ocr.extraction.review
  (:require
    [app.domain.backend.expenses.services.receipts.approval :as receipt-approval]
    [app.domain.backend.expenses.services.receipts.queries :as receipt-queries]
    [app.domain.backend.expenses.services.receipts.status :as receipt-status]
    [app.domain.backend.expenses.services.user-expense-settings :as user-expense-settings]
    [taoensso.timbre :as log]))

(defn- resolve-payer-id
  [db {:keys [payer_id user_id]}]
  (or payer_id
    (when user_id
      (-> (user-expense-settings/get-user-expense-settings db user_id)
        user-expense-settings/effective-settings
        :default-payer-id))))

(defn- resolve-purchased-at
  [{:keys [purchased_at_guess created_at]}]
  (or purchased_at_guess created_at))

(defn- build-review-data
  [supplier-id store-id payer-id receipt extraction {:keys [default-currency]}]
  (let [purchased-at (resolve-purchased-at receipt)
        total-amount (:total_amount_guess receipt)
        currency (or (:currency_guess receipt) default-currency "BAM")
        items (vec (or (:items extraction) []))
        notes (str "Extracted from receipt: "
                (or (:original_filename receipt)
                  (:storage_key receipt)
                  "receipt"))]
    (cond-> {:supplier_id supplier-id
             :payer_id payer-id
             :purchased_at purchased-at
             :total_amount total-amount
             :currency currency
             :notes notes
             :items items}
      store-id (assoc :store_id store-id))))

(defn mark-review-required!
  [db receipt-id message]
  (receipt-status/update-status!
    db
    receipt-id
    "review_required"
    {:error_message message
     :error_details nil}))

(defn auto-approve-extracted-receipt!
  [db receipt-id extraction supplier-id store-id opts]
  (when-let [receipt (receipt-queries/get-receipt db receipt-id)]
    (when (and (= "extracted" (:status receipt))
            (nil? (:expense_id receipt)))
      (let [payer-id (resolve-payer-id db receipt)
            review-data (build-review-data supplier-id store-id payer-id receipt extraction opts)
            {:keys [purchased_at total_amount items]} review-data]
        (cond
          (nil? payer-id)
          {:status "review_required"
           :error (mark-review-required! db receipt-id "Payer is required to auto-post receipt.")}

          (nil? purchased_at)
          {:status "review_required"
           :error (mark-review-required! db receipt-id "Purchase date is required to auto-post receipt.")}

          (nil? total_amount)
          {:status "review_required"
           :error (mark-review-required! db receipt-id "Total amount is required to auto-post receipt.")}

          (empty? items)
          {:status "review_required"
           :error (mark-review-required! db receipt-id "Line items are required to auto-post receipt.")}

          :else
          (try
            (if-let [user-id (:user_id receipt)]
              (receipt-approval/approve-and-post-for-user! db user-id receipt-id review-data)
              (receipt-approval/approve-and-post! db receipt-id review-data))
            (log/info "Auto-posted extracted receipt" {:receipt-id receipt-id})
            {:status "posted"}
            (catch Exception e
              (log/warn e "Failed to auto-post extracted receipt" {:receipt-id receipt-id})
              {:status "review_required"
               :error (mark-review-required! db receipt-id (or (.getMessage e) "Auto-post failed"))})))))))
