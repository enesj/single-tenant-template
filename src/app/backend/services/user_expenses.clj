(ns app.backend.services.user-expenses
  "User-facing expense services with user_id filtering.
   These services enforce that users can only access their own expenses."
  (:require
    [app.domain.expenses.services.expenses :as admin-expenses]
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
                                          [:= :user_id user-id]
                                          [:is :deleted_at nil]]})
                     {:builder-fn rs/as-unqualified-lower-maps})]
      (when existing
        (admin-expenses/update-expense! db expense-id updates)))))

(defn soft-delete-user-expense!
  "Soft delete a user's own expense. Returns deleted expense or nil if not found/unauthorized."
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
                                          [:= :user_id user-id]
                                          [:is :deleted_at nil]]})
                     {:builder-fn rs/as-unqualified-lower-maps})]
      (when existing
        (admin-expenses/soft-delete-expense! db expense-id)))))

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
                                          [:p.type :payer_type]]
                                 :from [[:expenses :e]]
                                 :left-join [[:suppliers :s] [:= :s.id :e.supplier_id]
                                             [:payers :p] [:= :p.id :e.payer_id]]
                                 :where [:and
                                         [:= :e.id expense-id]
                                         [:= :e.user_id user-id]
                                         [:is :e.deleted_at nil]]})
                    {:builder-fn rs/as-unqualified-lower-maps})
          items (when expense
                  (jdbc/execute!
                    db
                    (sql/format {:select [:*]
                                 :from [:expense_items]
                                 :where [:= :expense_id expense-id]
                                 :order-by [[:created_at :asc]]})
                    {:builder-fn rs/as-unqualified-lower-maps}))]
      (when expense
        (assoc expense :items items)))))

(defn list-user-expenses
  "List expenses for a specific user with common filters.
   opts: :from, :to, :supplier-id, :payer-id, :is-posted?, :limit, :offset, :order-dir."
  [db user-id {:keys [from to supplier-id payer-id is-posted? limit offset order-dir]
               :or {limit 50 offset 0 order-dir :desc}}]
  (let [user-id (ensure-uuid user-id)]
    (when-not user-id
      (throw (ex-info "user-id is required" {})))
    (let [base-where (cond-> [:and
                              [:= :e.user_id user-id]
                              [:is :e.deleted_at nil]]
                       from (conj [:>= :e.purchased_at from])
                       to (conj [:<= :e.purchased_at to])
                       supplier-id (conj [:= :e.supplier_id supplier-id])
                       payer-id (conj [:= :e.payer_id payer-id])
                       (some? is-posted?) (conj [:= :e.is_posted (boolean is-posted?)]))
          query {:select [[:e.*]
                          [:s.display_name :supplier_display_name]
                          [:s.normalized_key :supplier_normalized_key]
                          [:p.label :payer_label]
                          [:p.type :payer_type]]
                 :from [[:expenses :e]]
                 :left-join [[:suppliers :s] [:= :s.id :e.supplier_id]
                             [:payers :p] [:= :p.id :e.payer_id]]
                 :where base-where
                 :order-by [[:e.purchased_at order-dir]]
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
                              [:= :user_id user-id]
                              [:is :deleted_at nil]]
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
                                                 [:is :deleted_at nil]
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
                           [:is :deleted_at nil]
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
                              [:is :e.deleted_at nil]
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
