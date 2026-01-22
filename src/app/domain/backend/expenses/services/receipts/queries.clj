(ns app.domain.backend.expenses.services.receipts.queries
  "Receipt query operations: get, list, delete."
  (:require
    [app.domain.backend.expenses.services.expenses :as expenses]
    [app.domain.backend.expenses.services.receipts.storage :as storage]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]))

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
          (when (expenses/get-expense tx expense-id)
            (throw (ex-info "Cannot delete a receipt linked to an expense"
                     {:status 409
                      :id receipt-id
                      :expense-id expense-id})))))
      (storage/delete-receipt-file! receipt)
      (jdbc/execute-one!
        tx
        (sql/format {:delete-from :receipts
                     :where [:= :id receipt-id]
                     :returning [:*]})
        {:builder-fn rs/as-unqualified-lower-maps}))))

(defn- build-status-query-helpers
  "Build SQL helpers for status filtering with mismatch detection."
  []
  (let [lines-total-sql
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

        effective-status-sql (str
                               "case when (" mismatch-sql ") then 'review_required'::receipt_status else status end")]
    {:lines-total-sql lines-total-sql
     :mismatch-sql mismatch-sql
     :mismatch-clause mismatch-clause
     :not-mismatch-clause not-mismatch-clause
     :effective-status-sql effective-status-sql}))

(defn- build-status-clause
  "Build WHERE clause for status filtering."
  [status {:keys [mismatch-clause not-mismatch-clause]}]
  (cond
    (string? status)
    (case status
      "review_required" [:or
                         [:= :status (storage/receipt-status-cast "review_required")]
                         mismatch-clause]
      "extracted" [:and
                   [:= :status (storage/receipt-status-cast "extracted")]
                   not-mismatch-clause]
      [:= :status (storage/receipt-status-cast status)])

    (sequential? status)
    (let [sset (set status)
          base [:in :status (mapv storage/receipt-status-cast status)]
          want-review? (contains? sset "review_required")
          want-extracted? (contains? sset "extracted")]
      (cond
        (and want-review? (not want-extracted?)) [:or base mismatch-clause]
        (and want-extracted? (not want-review?)) [:and base not-mismatch-clause]
        :else base))

    :else nil))

(defn list-receipts
  "List receipts with optional status filter.

  Returns a lightweight projection for list views (detail endpoints return
  raw_extract_json / parsed_markdown, etc.)."
  [db {:keys [status limit offset order-dir]
       :or {limit 50 offset 0 order-dir :desc}}]
  (let [helpers (build-status-query-helpers)
        {:keys [lines-total-sql effective-status-sql]} helpers
        status-clause (build-status-clause status helpers)

        query (cond-> {:select [:id
                                :original_filename
                                [[:raw effective-status-sql] :status]
                                :supplier_guess
                                :total_amount_guess
                                [[:raw lines-total-sql] :lines_total_amount_guess]
                                :currency_guess
                                :payer_id
                                [[:raw "coalesce((raw_extract_json->>'refine_pending')::boolean, false)"] :refine_pending]
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

        helpers (build-status-query-helpers)
        {:keys [lines-total-sql effective-status-sql]} helpers
        status-clause (build-status-clause status helpers)

        where-clause (cond
                       status-clause [:and visibility-clause status-clause]
                       :else visibility-clause)

        query {:select [:id
                        :original_filename
                        [[:raw effective-status-sql] :status]
                        :supplier_guess
                        :total_amount_guess
                        [[:raw lines-total-sql] :lines_total_amount_guess]
                        :currency_guess
                        :payer_id
                        [[:raw "coalesce((raw_extract_json->>'refine_pending')::boolean, false)"] :refine_pending]
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

(defn list-pending-for-processing
  "Receipts that are ready to process (uploaded or failed-but-retry)."
  ([db]
   (list-pending-for-processing db nil))
  ([db {:keys [limit]}]
   (let [statuses (mapv storage/receipt-status-cast ["uploaded" "parsing" "parsed" "extracting"])
         query (cond-> {:select [:*]
                        :from [:receipts]
                        :where [:in :status statuses]
                        :order-by [[:created_at :asc]]}
                 (some? limit) (assoc :limit limit))]
     (jdbc/execute!
       db
       (sql/format query)
       {:builder-fn rs/as-unqualified-lower-maps}))))
