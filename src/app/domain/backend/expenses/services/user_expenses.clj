(ns app.domain.backend.expenses.services.user-expenses
  "User-facing expense services with user_id filtering.
   These services enforce that users can only access their own expenses."
  (:require
    [app.domain.backend.expenses.services.expenses :as admin-expenses]
    [app.shared.model-naming :as model-naming]
    [app.shared.query-builders :as shared-qb]
    [app.shared.type-conversion :as type-conv]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]
    [taoensso.timbre :as log])
  (:import
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
  "Create an expense for a specific user. Returns expense with :items."
  [db user-id expense-data items]
  (let [user-id (ensure-uuid user-id)]
    (when-not user-id
      (throw (ex-info "user-id is required" {:data expense-data})))
    (log/debug "Creating expense for user" {:user-id user-id})
    (admin-expenses/create-expense! db (assoc expense-data :user_id user-id) items)))

(defn update-user-expense!
  "Update expense fields for a user's own expense. Returns updated expense or nil if not found/unauthorized."
  [db user-id expense-id updates]
  (let [user-id (ensure-uuid user-id)]
    (when-not user-id
      (throw (ex-info "user-id is required" {:expense-id expense-id})))
    ;; First verify ownership
    (let [existing (jdbc/execute-one!
                     db
                     (sql/format {:select [:id :user_id]
                                  :from [:expenses]
                                  :where [:and
                                          [:= :id expense-id]
                                          [:= :user_id user-id]]})
                     {:builder-fn rs/as-unqualified-lower-maps})]
      (when existing
        (admin-expenses/update-expense! db expense-id updates)))))

(defn delete-user-expense!
  "Hard delete a user's own expense. Returns deleted expense or nil if not found/unauthorized."
  [db user-id expense-id]
  (let [user-id (ensure-uuid user-id)]
    (when-not user-id
      (throw (ex-info "user-id is required" {:expense-id expense-id})))
    ;; First verify ownership
    (let [existing (jdbc/execute-one!
                     db
                     (sql/format {:select [:id :user_id]
                                  :from [:expenses]
                                  :where [:and
                                          [:= :id expense-id]
                                          [:= :user_id user-id]]})

                     {:builder-fn rs/as-unqualified-lower-maps})]
      (when existing
        (admin-expenses/delete-expense! db expense-id)))))

(defn delete-user-expenses!
  "Hard delete multiple expenses owned by a user.

  Returns a result map:
  - :deleted-count
  - :deleted-ids
  - :not-found-ids (includes ids not owned by the user or missing)"
  [db user-id expense-ids]
  (let [user-id (ensure-uuid user-id)
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
        ;; Only delete expenses that are owned by the user.
        (let [owned-rows (jdbc/execute!
                           tx
                           (sql/format {:select [:id]
                                        :from [:expenses]
                                        :where [:and
                                                [:= :user_id user-id]
                                                [:in :id ids]]})
                           {:builder-fn rs/as-unqualified-lower-maps})
              owned-ids (mapv :id owned-rows)
              deleted-ids (reduce
                            (fn [acc expense-id]
                              (if-let [deleted (admin-expenses/delete-expense! tx expense-id)]
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

  Returns:
  {:updated n
   :results [<updated-expense> ...]
   :errors  [{:id <uuid-or-raw> :error <msg>} ...]}"
  [db user-id items]
  (let [user-id (ensure-uuid user-id)
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
                        (if-let [expense (update-user-expense! tx user-id expense-id updates)]
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
  "Get a user's own expense with items. Returns nil if not found or not owned by user."
  [db user-id expense-id]
  (let [user-id (ensure-uuid user-id)]
    (when-not user-id
      (throw (ex-info "user-id is required" {:expense-id expense-id})))
    (let [expense (jdbc/execute-one!
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
                                 :where [:and
                                         [:= :e.id expense-id]
                                         [:= :e.user_id user-id]]})
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
  - :limit, :offset
  - :order-by (app keyword)
  - :order-dir (:asc/:desc)"
  [db user-id {:keys [from to supplier-id payer-id is-posted? limit offset order-by order-dir]
               :or {limit 50 offset 0 order-dir :desc}}]
  (let [user-id (ensure-uuid user-id)
        order-by* (model-naming/ensure-app-keyword order-by)
        order-col (get allowed-user-expenses-order-by order-by* :e.purchased_at)
        order-dir* (shared-qb/normalize-order-direction order-dir {:default :desc})]
    (when-not user-id
      (throw (ex-info "user-id is required" {})))
    (let [base-where (cond-> [:and
                              [:= :e.user_id user-id]]
                       from (conj [:>= :e.purchased_at from])
                       to (conj [:<= :e.purchased_at to])
                       supplier-id (conj [:= :e.supplier_id supplier-id])
                       payer-id (conj [:= :e.payer_id payer-id])
                       (some? is-posted?) (conj [:= :e.is_posted (boolean is-posted?)]))
          query {:select [[:e.*]
                          [:s.display_name :supplier_display_name]
                      [:st.display_name :store_display_name]
                          [:s.normalized_key :supplier_normalized_key]
                          [:p.label :payer_label]
                          [:pt.label :payer_type]
                          [:ec.name :expense_category_name]]
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
                 :offset offset}]
      (jdbc/execute! db (sql/format query) {:builder-fn rs/as-unqualified-lower-maps}))))

(defn count-user-expenses
  "Count total expenses for a specific user with optional filters."
  [db user-id {:keys [from to supplier-id payer-id is-posted?]}]
  (let [user-id (ensure-uuid user-id)]
    (when-not user-id
      (throw (ex-info "user-id is required" {})))
    (let [base-where (cond-> [:and
                              [:= :user_id user-id]]
                       from (conj [:>= :purchased_at from])
                       to (conj [:<= :purchased_at to])
                       supplier-id (conj [:= :supplier_id supplier-id])
                       payer-id (conj [:= :payer_id payer-id])
                       (some? is-posted?) (conj [:= :is_posted (boolean is-posted?)]))
          query {:select [[[:count :*] :total]]
                 :from [:expenses]
                 :where base-where}]
      (:total (jdbc/execute-one! db (sql/format query)
                {:builder-fn rs/as-unqualified-lower-maps})))))

;; ============================================================================
;; User Dashboard Aggregations
;; ============================================================================

(defn get-user-expense-summary
  "Get summary statistics for a user's expenses.
   Returns: {:total-expenses N :total-amount M :currency-totals {...} :recent-count N}"
  [db user-id {:keys [days-back] :or {days-back 30}}]
  (let [user-id (ensure-uuid user-id)]
    (when-not user-id
      (throw (ex-info "user-id is required" {})))
    (let [;; Total count of all expenses
          total-expenses (count-user-expenses db user-id {})

          ;; Total amount by currency
          currency-totals (jdbc/execute!
                            db
                            (sql/format {:select [:currency
                                                  [[:sum :total_amount] :total]]
                                         :from [:expenses]
                                         :where [:and
                                                 [:= :user_id user-id]
                                                 [:= :is_posted true]]
                                         :group-by [:currency]})
                            {:builder-fn rs/as-unqualified-lower-maps})

          ;; Recent expenses count (last N days)
          recent-date (java.time.Instant/now)
          recent-count (count-user-expenses db user-id
                         {:from (.minus recent-date (java.time.Duration/ofDays days-back))})]

      {:total-expenses total-expenses
       :currency-totals (into {} (map (juxt :currency :total) currency-totals))
       :recent-count recent-count
       :days-back days-back})))

(defn get-user-spending-by-month
  "Get monthly spending aggregation for a user.
   Returns list of {:month \"YYYY-MM\" :currency C :total N}"
  [db user-id {:keys [months-back] :or {months-back 6}}]
  (let [user-id (ensure-uuid user-id)]
    (when-not user-id
      (throw (ex-info "user-id is required" {})))
    (jdbc/execute!
      db
      (sql/format {:select [[[:to_char :purchased_at [:inline "YYYY-MM"]] :month]
                            :currency
                            [[:sum :total_amount] :total]]
                   :from [:expenses]
                   :where [:and
                           [:= :user_id user-id]
                           [:= :is_posted true]
                           [:>= :purchased_at
                            [:raw (format "NOW() - INTERVAL '%d months'" months-back)]]]
                   :group-by [[:to_char :purchased_at [:inline "YYYY-MM"]] :currency]
                   :order-by [[[:to_char :purchased_at [:inline "YYYY-MM"]] :desc]]})
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn get-user-spending-by-supplier
  "Get spending by supplier for a user.
   Returns list of {:supplier_id UUID :supplier_name S :total N :currency C}"
  [db user-id {:keys [from to limit] :or {limit 10}}]
  (let [user-id (ensure-uuid user-id)]
    (when-not user-id
      (throw (ex-info "user-id is required" {})))
    (let [base-where (cond-> [:and
                              [:= :e.user_id user-id]
                              [:= :e.is_posted true]]
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
