(ns app.domain.backend.expenses.services.user-expenses
  "User-facing expense services with user_id filtering.
   These services enforce that users can only access their own expenses."
  (:require
    [app.domain.backend.expenses.services.exchange-rates :as exchange-rates]
    [app.domain.backend.expenses.services.expenses :as admin-expenses]
    [app.shared.model-naming :as model-naming]
    [app.shared.query-builders :as shared-qb]
    [app.shared.type-conversion :as type-conv]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]
    [taoensso.timbre :as log])
  (:import
    [java.time Instant LocalDate]
    [java.util UUID]))

;; ============================================================================
;; User Expense CRUD with user_id filtering
;; ============================================================================

(defn- ensure-uuid
  "Coerce a UUID or UUID string into a UUID instance. Returns nil if input is nil.
   Throws IllegalArgumentException for invalid UUID strings to surface earlier."
  [v]
  (cond
    (nil? v) nil
    (instance? UUID v) v
    :else (UUID/fromString (str v))))

(defn create-user-expense!
  "Create an expense for a specific user. Returns expense with :items.

   `tenant-id` scopes the expense to a specific tenant.

   Currency conversion (Phase 2):
   - When currency is non-BAM, fetches the daily exchange rate and populates
     original_amount, bam_amount, exchange_rate, and rate_fetched_at.
   - When currency is BAM (or nil/default), original_amount = bam_amount = total_amount."
  ([db tenant-id user-id expense-data items]
   (create-user-expense! db tenant-id user-id expense-data items nil))
  ([db tenant-id user-id expense-data items app-config]
   (let [user-id (ensure-uuid user-id)
         tenant-id (ensure-uuid tenant-id)
         currency (or (:currency expense-data) "BAM")
         total-amount (some-> (:total_amount expense-data) bigdec)]
     (when-not user-id
       (throw (ex-info "user-id is required" {:data expense-data})))
     (log/debug "Creating expense for user" {:user-id user-id :tenant-id tenant-id :currency currency})
     (let [expense-data (cond-> (assoc expense-data
                                  :user_id user-id
                                  :created_by user-id
                                  :currency currency)
                          tenant-id (assoc :tenant_id tenant-id))
           ;; Currency conversion: populate original_amount, bam_amount, exchange_rate
           expense-data (if (and total-amount (not= currency "BAM"))
                          (let [rate-date (LocalDate/now)
                                ;; Ensure rates are cached for today
                                _ (when app-config
                                    (try
                                      (exchange-rates/ensure-daily-rates!
                                        db (exchange-rates/build-config app-config))
                                      (catch Exception e
                                        (log/warn e "Failed to ensure daily rates" {:currency currency}))))
                                rate (exchange-rates/get-conversion-rate db currency rate-date)
                                bam-amount (if rate
                                             (.setScale (* total-amount rate) 2 java.math.RoundingMode/HALF_UP)
                                             total-amount)]
                            (cond-> (assoc expense-data
                                      :original_amount total-amount
                                      :bam_amount bam-amount)
                              rate (assoc :exchange_rate rate
                                     :rate_fetched_at (Instant/now))))
                          ;; BAM: all amounts are the same, no rate needed
                          (cond-> expense-data
                            total-amount (assoc :original_amount total-amount
                                           :bam_amount total-amount)))]
       (admin-expenses/create-expense! db expense-data items)))))

(defn update-user-expense!
  "Update expense fields for a user's own expense. Returns updated expense or nil if not found/unauthorized.
   `tenant-id` scopes the ownership check and update to a specific tenant."
  [db tenant-id user-id expense-id updates]
  (let [user-id (ensure-uuid user-id)
        tenant-id (ensure-uuid tenant-id)]
    (when-not user-id
      (throw (ex-info "user-id is required" {:expense-id expense-id})))
    ;; First verify ownership (scoped by tenant when present)
    (let [ownership-where (cond-> [:and
                                   [:= :id expense-id]
                                   [:= :user_id user-id]]
                            tenant-id (conj [:= :tenant_id tenant-id]))
          existing (jdbc/execute-one!
                     db
                     (sql/format {:select [:id :user_id]
                                  :from [:expenses]
                                  :where ownership-where})
                     {:builder-fn rs/as-unqualified-lower-maps})]
      (when existing
        (admin-expenses/update-expense! db expense-id updates tenant-id)))))

(defn delete-user-expenses!
  "Hard delete multiple expenses owned by a user.

  Returns a result map:
  - :deleted-count
  - :deleted-ids
  - :not-found-ids (includes ids not owned by the user or missing)

  `tenant-id` scopes the ownership check and delete to a specific tenant."
  [db tenant-id user-id expense-ids]
  (let [user-id (ensure-uuid user-id)
        tenant-id (ensure-uuid tenant-id)
        ids (->> (or expense-ids [])
              (map ensure-uuid)
              (remove nil?)
              vec)]
    (when-not user-id
      (throw (ex-info "user-id is required" {:expense-ids expense-ids})))
    (if (empty? ids)
      {:deleted-count 0
       :deleted-ids []
       :not-found-ids []}
      (jdbc/with-transaction [tx db]
        ;; Only delete expenses that are owned by the user (scoped by tenant).
        (let [ownership-where (cond-> [:and
                                       [:= :user_id user-id]
                                       [:in :id ids]]
                                tenant-id (conj [:= :tenant_id tenant-id]))
              owned-rows (jdbc/execute!
                           tx
                           (sql/format {:select [:id]
                                        :from [:expenses]
                                        :where ownership-where})
                           {:builder-fn rs/as-unqualified-lower-maps})
              owned-ids (mapv :id owned-rows)
              deleted-ids (reduce
                            (fn [acc expense-id]
                              (if-let [deleted (admin-expenses/delete-expense! tx expense-id tenant-id)]
                                (conj acc (:id deleted))
                                acc))
                            []
                            owned-ids)
              deleted-set (set deleted-ids)
              not-found-ids (->> ids
                              (remove deleted-set)
                              vec)]
          {:deleted-count (count deleted-ids)
           :deleted-ids deleted-ids
           :not-found-ids not-found-ids})))))

(defn batch-update-user-expenses!
  "Batch update multiple expenses owned by a user.

  `items` is a collection of maps, each containing:
  - :id (UUID or UUID string)
  - any updatable expense columns (snake_case keys)

  Only the following keys are applied:
  :supplier_id :payer_id :expense_category_id :purchased_at :total_amount :currency :notes :is_posted :receipt_id

  `tenant-id` scopes the ownership checks and updates to a specific tenant.

  Returns:
  {:updated n
   :results [<updated-expense> ...]
   :errors  [{:id <uuid-or-raw> :error <msg>} ...]}"
  [db tenant-id user-id items]
  (let [user-id (ensure-uuid user-id)
        tenant-id (ensure-uuid tenant-id)
        allowed-keys #{:supplier_id :payer_id :expense_category_id :purchased_at :total_amount :currency :notes :is_posted :receipt_id}
        items (vec (or items []))
        try-uuid type-conv/try-parse-uuid]
    (when-not user-id
      (throw (ex-info "user-id is required" {:items-count (count items)})))
    (if (empty? items)
      {:updated 0
       :results []
       :errors []}
      (jdbc/with-transaction [tx db]
        (let [{:keys [results errors]}
              (reduce
                (fn [acc item]
                  (let [expense-id (try-uuid (:id item))
                        updates (-> item
                                  (dissoc :id
                                    :created_at :updated_at
                                    :created-at :updated-at)
                                  (select-keys allowed-keys))]
                    (cond
                      (nil? expense-id)
                      (update acc :errors conj {:id (:id item) :error "Invalid expense id"})

                      (empty? updates)
                      (update acc :errors conj {:id expense-id :error "No updatable fields provided"})

                      :else
                      (try
                        (if-let [expense (update-user-expense! tx tenant-id user-id expense-id updates)]
                          (update acc :results conj expense)
                          (update acc :errors conj {:id expense-id :error "Expense not found or access denied"}))
                        (catch Exception e
                          (log/error e "Batch update failed for expense" {:expense-id expense-id :user-id user-id})
                          (update acc :errors conj {:id expense-id :error "Failed to update expense"}))))))
                {:results [] :errors []}
                items)]
          {:updated (count results)
           :results results
           :errors errors})))))

(defn get-user-expense-with-items
  "Get an expense with items. When user-id is non-nil, restricts to that user's expenses.
   `tenant-id` scopes the query to a specific tenant."
  [db tenant-id user-id expense-id]
  (let [user-id (ensure-uuid user-id)
        tenant-id (ensure-uuid tenant-id)]
    (let [where (cond-> [:and
                         [:= :e.id expense-id]]
                  user-id (conj [:= :e.user_id user-id])
                  tenant-id (conj [:= :e.tenant_id tenant-id]))
          expense (jdbc/execute-one!
                    db
                    (sql/format {:select [[:e.*]
                                          [:s.display_name :supplier_display_name]
                                          [:s.normalized_key :supplier_normalized_key]
                                          [:p.label :payer_label]
                                          [:p.payer_type_id :payer_type_id]
                                          [:ec.name :expense_category_name]]
                                 :from [[:expenses :e]]
                                 :left-join [[:suppliers :s] [:= :s.id :e.supplier_id]
                                             [:payers :p] [:= :p.id :e.payer_id]
                                             [:expense_categories :ec] [:= :ec.id :e.expense_category_id]]
                                 :where where})
                    {:builder-fn rs/as-unqualified-lower-maps})
          items (when expense
                  (jdbc/execute!
                    db
                    (sql/format {:select [[:ei.*]
                                          [:aa.raw_label :raw_label]
                                          [:aa.raw_label_normalized :raw_label_normalized]
                                          [:a.canonical_name :article_canonical_name]]
                                 :from [[:expense_items :ei]]
                                 :left-join [[:article_aliases :aa] [:= :aa.id :ei.alias_id]
                                             [:articles :a] [:= :a.id :aa.article_id]]
                                 :where [:= :ei.expense_id expense-id]
                                 :order-by [[:ei.created_at :asc]]})
                    {:builder-fn rs/as-unqualified-lower-maps}))]
      (when expense
        (assoc expense :items items)))))

(def ^:private expense-text-filter-columns
  "Mapping from text filter keys to SQL column identifiers for expenses."
  {:supplier-display-name  :s.display_name
   :store-display-name     :st.display_name
   :expense-category-name  :ec.name
   :payer-label            :p.label
   :currency               :e.currency
   :notes                  :e.notes})

(def ^:private allowed-user-expenses-order-by
  "Allowlisted sort keys for `list-user-expenses`.

  Keys are app keywords (kebab-case). Values are HoneySQL identifiers."
  {:purchased-at :e.purchased_at
   :expense-date :e.purchased_at
   :created-at :e.created_at
   :updated-at :e.updated_at
   :total-amount :e.total_amount
   :currency :e.currency
   :supplier-display-name :s.display_name
   :store-display-name :st.display_name
   :payer-label :p.label
   :payer-type :pt.label
   :expense-category-name :ec.name
   :is-posted :e.is_posted})

(defn list-user-expenses
  "List expenses for a specific user with common filters.

  opts:
  - :from, :to
  - :supplier-id, :payer-id, :is-posted?
  - :supplier-display-name, :store-display-name, :expense-category-name,
    :payer-label, :currency, :notes  (text ILIKE filters)
  - :limit, :offset
  - :order-by (app keyword)
  - :order-dir (:asc/:desc)

  `tenant-id` scopes the query to a specific tenant."
  [db tenant-id user-id {:keys [from to supplier-id payer-id is-posted?
                                supplier-display-name store-display-name
                                expense-category-name payer-label currency notes
                                limit offset order-by order-dir]
                         :or {limit 50 offset 0 order-dir :desc}}]
  (let [user-id (ensure-uuid user-id)
        tenant-id (ensure-uuid tenant-id)
        order-by* (model-naming/ensure-app-keyword order-by)
        order-col (get allowed-user-expenses-order-by order-by* :e.purchased_at)
        order-dir* (shared-qb/normalize-order-direction order-dir {:default :desc})]
    (let [base-where (cond-> [:and]
                       user-id (conj [:= :e.user_id user-id])
                       tenant-id (conj [:= :e.tenant_id tenant-id])
                       from (conj [:>= :e.purchased_at from])
                       to (conj [:<= :e.purchased_at to])
                       supplier-id (conj [:= :e.supplier_id supplier-id])
                       payer-id (conj [:= :e.payer_id payer-id])
                       (some? is-posted?) (conj [:= :e.is_posted (boolean is-posted?)]))
          query (-> {:select [[:e.*]
                              [:s.display_name :supplier_display_name]
                              [:st.display_name :store_display_name]
                              [:s.normalized_key :supplier_normalized_key]
                              [:p.label :payer_label]
                              [:pt.label :payer_type]
                              [:ec.name :expense_category_name]
                              [{:select [[[:count :ei.id]]]
                                :from [[:expense_items :ei]]
                                :where [:= :ei.expense_id :e.id]} :item_count]]
                     :from [[:expenses :e]]
                     :left-join [[:suppliers :s] [:= :s.id :e.supplier_id]
                                 [:stores :st] [:= :st.id :e.store_id]
                                 [:payers :p] [:= :p.id :e.payer_id]
                                 [:payer_types :pt] [:= :pt.id :p.payer_type_id]
                                 [:expense_categories :ec] [:= :ec.id :e.expense_category_id]]
                     :where base-where
                     :order-by [[order-col order-dir*]
                                [:e.id :asc]]
                     :limit limit
                     :offset offset}
                  (shared-qb/apply-text-filters expense-text-filter-columns
                    {:supplier-display-name supplier-display-name
                     :store-display-name store-display-name
                     :expense-category-name expense-category-name
                     :payer-label payer-label
                     :currency currency
                     :notes notes}))]
      (jdbc/execute! db (sql/format query) {:builder-fn rs/as-unqualified-lower-maps}))))

(defn count-user-expenses
  "Count total expenses for a specific user with optional filters.
   When user-id is nil, counts all tenant expenses.
   `tenant-id` scopes the count to a specific tenant.
   Text filters (supplier-display-name, etc.) require LEFT JOINs."
  [db tenant-id user-id {:keys [from to supplier-id payer-id is-posted?
                                supplier-display-name store-display-name
                                expense-category-name payer-label currency notes]}]
  (let [user-id (ensure-uuid user-id)
        tenant-id (ensure-uuid tenant-id)
        text-filters {:supplier-display-name supplier-display-name
                      :store-display-name store-display-name
                      :expense-category-name expense-category-name
                      :payer-label payer-label
                      :currency currency
                      :notes notes}
        has-text-filters? (shared-qb/has-text-filters?
                            (keys expense-text-filter-columns) text-filters)]
    (let [base-where (cond-> [:and]
                       user-id (conj [:= (if has-text-filters? :e.user_id :user_id) user-id])
                       tenant-id (conj [:= (if has-text-filters? :e.tenant_id :tenant_id) tenant-id])
                       from (conj [:>= (if has-text-filters? :e.purchased_at :purchased_at) from])
                       to (conj [:<= (if has-text-filters? :e.purchased_at :purchased_at) to])
                       supplier-id (conj [:= (if has-text-filters? :e.supplier_id :supplier_id) supplier-id])
                       payer-id (conj [:= (if has-text-filters? :e.payer_id :payer_id) payer-id])
                       (some? is-posted?) (conj [:= (if has-text-filters? :e.is_posted :is_posted) (boolean is-posted?)]))
          query (if has-text-filters?
                  (-> {:select [[[:count :*] :total]]
                       :from [[:expenses :e]]
                       :left-join [[:suppliers :s] [:= :s.id :e.supplier_id]
                                   [:stores :st] [:= :st.id :e.store_id]
                                   [:payers :p] [:= :p.id :e.payer_id]
                                   [:expense_categories :ec] [:= :ec.id :e.expense_category_id]]
                       :where base-where}
                    (shared-qb/apply-text-filters expense-text-filter-columns text-filters))
                  {:select [[[:count :*] :total]]
                   :from [:expenses]
                   :where base-where})]
      (:total (jdbc/execute-one! db (sql/format query)
                {:builder-fn rs/as-unqualified-lower-maps})))))

;; ============================================================================
;; User Dashboard Aggregations
;; ============================================================================

(defn get-user-expense-summary
  "Get summary statistics for expenses.
   When user-id is nil, aggregates all tenant expenses.
   Returns: {:total-expenses N :total-amount M :currency-totals {...} :recent-count N}
   `tenant-id` scopes the summary to a specific tenant."
  [db tenant-id user-id {:keys [days-back] :or {days-back 30}}]
  (let [user-id (ensure-uuid user-id)
        tenant-id (ensure-uuid tenant-id)]
    (let [;; Total count of all expenses
          total-expenses (count-user-expenses db tenant-id user-id {})

          ;; Total amount by currency
          currency-where (cond-> [:and
                                  [:= :is_posted true]]
                           user-id (conj [:= :user_id user-id])
                           tenant-id (conj [:= :tenant_id tenant-id]))
          currency-totals (jdbc/execute!
                            db
                            (sql/format {:select [:currency
                                                  [[:sum :total_amount] :total]]
                                         :from [:expenses]
                                         :where currency-where
                                         :group-by [:currency]})
                            {:builder-fn rs/as-unqualified-lower-maps})

          ;; Recent expenses count (last N days)
          recent-date (java.time.Instant/now)
          recent-count (count-user-expenses db tenant-id user-id
                         {:from (.minus recent-date (java.time.Duration/ofDays days-back))})]

      {:total-expenses total-expenses
       :currency-totals (into {} (map (juxt :currency :total) currency-totals))
       :recent-count recent-count
       :days-back days-back})))

(defn get-user-spending-by-month
  "Get monthly spending aggregation for a user.
   Returns list of {:month \"YYYY-MM\" :currency C :total N}
   `tenant-id` scopes the query to a specific tenant."
  [db tenant-id user-id {:keys [months-back] :or {months-back 6}}]
  (let [user-id (ensure-uuid user-id)
        tenant-id (ensure-uuid tenant-id)]
    (let [where (cond-> [:and
                         [:= :is_posted true]
                         [:>= :purchased_at
                          [:raw (format "NOW() - INTERVAL '%d months'" months-back)]]]
                  user-id (conj [:= :user_id user-id])
                  tenant-id (conj [:= :tenant_id tenant-id]))]
      (jdbc/execute!
        db
        (sql/format {:select [[[:to_char :purchased_at [:inline "YYYY-MM"]] :month]
                              :currency
                              [[:sum :total_amount] :total]]
                     :from [:expenses]
                     :where where
                     :group-by [[:to_char :purchased_at [:inline "YYYY-MM"]] :currency]
                     :order-by [[[:to_char :purchased_at [:inline "YYYY-MM"]] :desc]]})
        {:builder-fn rs/as-unqualified-lower-maps}))))

(defn get-user-spending-by-supplier
  "Get spending by supplier for a user.
   Returns list of {:supplier_id UUID :supplier_name S :total N :currency C}
   `tenant-id` scopes the query to a specific tenant."
  [db tenant-id user-id {:keys [from to limit] :or {limit 10}}]
  (let [user-id (ensure-uuid user-id)
        tenant-id (ensure-uuid tenant-id)]
    (let [base-where (cond-> [:and
                              [:= :e.is_posted true]
                              [:is-not :e.supplier_id nil]]
                       user-id (conj [:= :e.user_id user-id])
                       tenant-id (conj [:= :e.tenant_id tenant-id])
                       from (conj [:>= :e.purchased_at from])
                       to (conj [:<= :e.purchased_at to]))]
      (jdbc/execute!
        db
        (sql/format {:select [:e.supplier_id
                              [:s.display_name :supplier_name]
                              :e.currency
                              [[:sum :e.total_amount] :total]
                              [[:count :*] :expense_count]]
                     :from [[:expenses :e]]
                     :left-join [[:suppliers :s] [:= :s.id :e.supplier_id]]
                     :where base-where
                     :group-by [:e.supplier_id :s.display_name :e.currency]
                     :order-by [[[:sum :e.total_amount] :desc]]
                     :limit limit})
        {:builder-fn rs/as-unqualified-lower-maps}))))
