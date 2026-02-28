(ns app.domain.backend.expenses.services.receipts.queries
  "Receipt query operations: get, list, delete."
  (:require
    [app.domain.backend.expenses.services.expenses :as expenses]
    [app.domain.backend.expenses.services.receipts.storage :as storage]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]))

(defn get-receipt
  "Get a receipt by id. Optional `tenant-id` scopes to a specific tenant."
  ([db receipt-id] (get-receipt db receipt-id nil))
  ([db receipt-id tenant-id]
   (let [where (if tenant-id
                 [:and [:= :id receipt-id] [:= :tenant_id tenant-id]]
                 [:= :id receipt-id])]
     (jdbc/execute-one!
       db
       (sql/format {:select [:*]
                    :from [:receipts]
                    :where where})
       {:builder-fn rs/as-unqualified-lower-maps}))))

(defn get-receipt-refine-context
  "Load canonical supplier/store context for a receipt.

  Returns (keys are snake_case to match DB conventions):
  {:supplier_id uuid
   :supplier_key string
   :supplier_name string
   :store_id uuid
   :store_key string
   :store_display_name string
   :store_address string}

  Values can be nil when the receipt has not been mapped to canonical supplier/store
  rows yet (e.g. alias is still unmapped).

  Returns nil when the receipt does not exist."
  [db receipt-id]
  (when receipt-id
    (jdbc/execute-one!
      db
      (sql/format
        {:select [[[:coalesce :sup_from_store.id :sup_from_alias.id] :supplier_id]
                  [[:coalesce :sup_from_store.normalized_key :sup_from_alias.normalized_key] :supplier_key]
                  [[:coalesce :sup_from_store.display_name :sup_from_alias.display_name] :supplier_name]
                  [:st.id :store_id]
                  [:st.normalized_key :store_key]
                  [:st.display_name :store_display_name]
                  [:st.address :store_address]]
         :from [[:receipts :r]]
         :left-join [[:store_aliases :sta] [:= :sta.id :r.store_alias_id]
                     [:stores :st] [:= :st.id :sta.store_id]
                     [:suppliers :sup_from_store] [:= :sup_from_store.id :st.supplier_id]
                     [:supplier_aliases :sa] [:= :sa.id :r.supplier_alias_id]
                     [:suppliers :sup_from_alias] [:= :sup_from_alias.id :sa.supplier_id]]
         :where [:= :r.id receipt-id]
         :limit 1})
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn delete-receipt!
  "Hard-delete a receipt and return the deleted row.

  Safety rules:
  - Disallow deleting receipts that have already been posted / linked to an expense.

  Also deletes the associated receipt image file from disk.

  Returns the deleted receipt row (map) or nil if the receipt did not exist.
  Optional `tenant-id` scopes the delete to a specific tenant."
  ([db receipt-id] (delete-receipt! db receipt-id nil))
  ([db receipt-id tenant-id]
   (jdbc/with-transaction [tx db]
     (when-let [receipt (get-receipt tx receipt-id tenant-id)]
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
       (let [where (if tenant-id
                     [:and [:= :id receipt-id] [:= :tenant_id tenant-id]]
                     [:= :id receipt-id])]
         (jdbc/execute-one!
           tx
           (sql/format {:delete-from :receipts
                        :where where
                        :returning [:*]})
           {:builder-fn rs/as-unqualified-lower-maps}))))))

(defn- build-status-query-helpers
  "Build SQL helpers for status filtering with mismatch detection.

  Receipts with refine_pending=true are excluded from the mismatch condition so
  the effective status stays as the raw DB status while Cerebras refinement is
  running. Once refinement completes the flag is cleared and the mismatch check
  applies normally."
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
          " and abs((" lines-total-sql ") - total_amount_guess) > 0.01"
          " and not coalesce((raw_extract_json->>'refine_pending')::boolean, false)")

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

    (nil? status) nil

    :else (throw (ex-info "status filter must be a string, sequential, or nil"
                   {:status status :type (type status)}))))

(defn- build-receipts-where-clause
  "Build WHERE clause for receipt list/count queries.

  When `user-id` is provided, visibility is scoped to:
  - receipts owned by the user, or
  - receipts with no owner (`user_id` is nil).

  When `tenant-id` is provided, scopes to that tenant."
  [status user-id helpers & {:keys [tenant-id]}]
  (let [status-clause (build-status-clause status helpers)
        visibility-clause (when user-id
                            [:or
                             [:= :user_id user-id]
                             [:is :user_id nil]])
        tenant-clause (when tenant-id
                        [:= :tenant_id tenant-id])
        clauses (remove nil? [tenant-clause visibility-clause status-clause])]
    (case (count clauses)
      0 nil
      1 (first clauses)
      (into [:and] clauses))))

(def ^:private sortable-receipt-columns
  "Whitelist mapping client-supplied column names to ORDER BY expressions.
  :status uses the effective-status SQL expression so sorting matches
  the displayed computed status."
  {"original_filename" :original_filename
   "original-filename" :original_filename
   "supplier_guess" :supplier_guess
   "supplier-guess" :supplier_guess
   "total_amount_guess" :total_amount_guess
   "total-amount-guess" :total_amount_guess
   "total-display" :total_amount_guess
   "created_at" :created_at
   "created-at" :created_at
   "updated_at" :updated_at
   "updated-at" :updated_at})

(defn list-receipts
  "List receipts with optional status filter.

  Returns a lightweight projection for list views (detail endpoints return
  raw_extract_json / parsed_markdown, etc.).
  Optional :tenant-id in opts scopes to a specific tenant."
  [db {:keys [status tenant-id limit offset order-dir order-by]
       :or {limit 50 offset 0 order-dir :desc}}]
  (let [helpers (build-status-query-helpers)
        {:keys [lines-total-sql effective-status-sql]} helpers
        where-clause (build-receipts-where-clause status nil helpers :tenant-id tenant-id)
        order-col (if (= (some-> order-by name) "status")
                    [:raw effective-status-sql]
                    (or (get sortable-receipt-columns (some-> order-by name)) :created_at))
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
                       :order-by [[order-col order-dir]]
                       :limit limit
                       :offset offset}
                where-clause (assoc :where where-clause))]
    (jdbc/execute! db (sql/format query) {:builder-fn rs/as-unqualified-lower-maps})))

(defn list-user-receipts
  "List receipts visible to a specific user.

  Visibility rules:
  - receipts owned by `user-id`
  - receipts with no `user_id` (unassigned/admin-uploaded)

  Supports optional status filter.
  Optional :tenant-id in opts scopes to a specific tenant.

  Returns a lightweight projection for list views (detail endpoints return
  raw_extract_json / parsed_markdown, etc.)."
  [db user-id {:keys [status tenant-id limit offset order-dir order-by]
               :or {limit 50 offset 0 order-dir :desc}}]
  (when-not user-id
    (throw (ex-info "user-id is required" {:status 400})))
  (let [helpers (build-status-query-helpers)
        {:keys [lines-total-sql effective-status-sql]} helpers
        where-clause (build-receipts-where-clause status user-id helpers :tenant-id tenant-id)
        order-col (if (= (some-> order-by name) "status")
                    [:raw effective-status-sql]
                    (or (get sortable-receipt-columns (some-> order-by name)) :created_at))
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
               :order-by [[order-col order-dir]]
               :limit limit
               :offset offset}]
    (jdbc/execute! db (sql/format query) {:builder-fn rs/as-unqualified-lower-maps})))

(defn count-receipts
  "Count receipts using the same status filter semantics as `list-receipts`.
   Optional :tenant-id in opts scopes to a specific tenant."
  [db {:keys [status tenant-id]}]
  (let [helpers (build-status-query-helpers)
        where-clause (build-receipts-where-clause status nil helpers :tenant-id tenant-id)
        query (cond-> {:select [[[:count :*] :total]]
                       :from [:receipts]}
                where-clause (assoc :where where-clause))
        row (jdbc/execute-one! db (sql/format query) {:builder-fn rs/as-unqualified-lower-maps})]
    (long (or (:total row) 0))))

(defn count-user-receipts
  "Count receipts visible to `user-id` using the same filters as `list-user-receipts`.
   Optional :tenant-id in opts scopes to a specific tenant."
  [db user-id {:keys [status tenant-id]}]
  (when-not user-id
    (throw (ex-info "user-id is required" {:status 400})))
  (let [helpers (build-status-query-helpers)
        where-clause (build-receipts-where-clause status user-id helpers :tenant-id tenant-id)
        query {:select [[[:count :*] :total]]
               :from [:receipts]
               :where where-clause}
        row (jdbc/execute-one! db (sql/format query) {:builder-fn rs/as-unqualified-lower-maps})]
    (long (or (:total row) 0))))

(defn list-receipts-page
  "List receipts with pagination metadata for server-backed pagination."
  [db opts]
  (let [opts* (merge {:limit 50 :offset 0} opts)
        rows (list-receipts db opts*)
        total (count-receipts db opts*)]
    {:rows rows
     :total total
     :limit (:limit opts*)
     :offset (:offset opts*)}))

(defn list-user-receipts-page
  "List user-visible receipts with pagination metadata."
  [db user-id opts]
  (let [opts* (merge {:limit 50 :offset 0} opts)
        rows (list-user-receipts db user-id opts*)
        total (count-user-receipts db user-id opts*)]
    {:rows rows
     :total total
     :limit (:limit opts*)
     :offset (:offset opts*)}))

(defn get-user-receipt
  "Fetch a single receipt visible to `user-id`.

  Visibility rules:
  - receipts owned by `user-id`
  - receipts with no `user_id` (unassigned/admin-uploaded)

  Returns nil when not found or not visible.
  Optional `tenant-id` scopes to a specific tenant."
  ([db user-id receipt-id] (get-user-receipt db user-id receipt-id nil))
  ([db user-id receipt-id tenant-id]
   (when-not user-id
     (throw (ex-info "user-id is required" {:status 400})))
   (let [receipt (get-receipt db receipt-id tenant-id)]
     (when (and receipt
             (or (= user-id (:user_id receipt))
               (nil? (:user_id receipt))))
       receipt))))

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
