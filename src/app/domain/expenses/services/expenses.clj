(ns app.domain.expenses.services.expenses
  "Expense creation/update with line items."
  (:require
    [app.domain.expenses.services.price-history :as price-history]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.util UUID]))

;; ============================================================================
;; Helpers
;; ============================================================================

(defn- require-keys!
  [m ks]
  (doseq [k ks]
    (when-not (get m k)
      (throw (ex-info (str (name k) " is required") {:data m})))))

;; ============================================================================
;; Core
;; ============================================================================

(defn create-expense!
  "Create an expense and its line items. Returns expense with :items."
  [db expense-data items]
  (require-keys! expense-data [:supplier_id :payer_id :purchased_at :total_amount])
  (jdbc/with-transaction [tx db]
    (let [expense-id (UUID/randomUUID)
          expense-row (-> expense-data
                        (select-keys [:user_id :receipt_id :supplier_id :payer_id :purchased_at :total_amount :currency :notes :is_posted])
                        (assoc :id expense-id)
                        (update :currency #(when % [:cast % :currency]))
                        (update :is_posted #(if (nil? %) true (boolean %))))
          expense-sql (sql/format {:insert-into :expenses
                                   :values [expense-row]
                                   :returning [:*]})
          expense (jdbc/execute-one! tx expense-sql {:builder-fn rs/as-unqualified-lower-maps})
          item-rows (map (fn [{:keys [raw_label article_id qty unit_price line_total] :as item}]
                           (require-keys! item [:raw_label :line_total])
                           (merge {:id (UUID/randomUUID)
                                   :expense_id expense-id
                                   :raw_label raw_label
                                   :article_id article_id
                                   :qty qty
                                   :unit_price unit_price
                                   :line_total line_total}
                             {}))
                      items)
          inserted-items (if (seq item-rows)
                           (jdbc/execute! tx
                             (sql/format {:insert-into :expense_items
                                          :values item-rows
                                          :returning [:*]})
                             {:builder-fn rs/as-unqualified-lower-maps})
                           [])]
      ;; Record price observations when article + supplier present
      (doseq [item inserted-items]
        (when (and (:article_id item) (:supplier_id expense))
          (price-history/record-observation!
            tx {:article_id (:article_id item)
                :supplier_id (:supplier_id expense)
                :expense_item_id (:id item)
                :qty (:qty item)
                :unit_price (:unit_price item)
                :line_total (:line_total item)
                :currency (:currency expense)
                :observed_at (:purchased_at expense)})))
      (assoc expense :items inserted-items))))

(defn update-expense!
  "Update expense fields (not items). Returns updated expense."
  [db id updates]
  (let [updates* (-> updates
                   (select-keys [:supplier_id :payer_id :purchased_at :total_amount :currency :notes :is_posted])
                   (update :currency #(when % [:cast % :currency]))
                   (assoc :updated_at [:now]))
        sql-map {:update :expenses
                 :set updates*
                 :where [:and
                         [:= :id id]
                         [:is :deleted_at nil]]
                 :returning [:*]}]
    (jdbc/execute-one! db (sql/format sql-map) {:builder-fn rs/as-unqualified-lower-maps})))

(defn soft-delete-expense!
  "Soft delete expense."
  [db id]
  (jdbc/execute-one!
    db
    (sql/format {:update :expenses
                 :set {:deleted_at [:now]}
                 :where [:= :id id]
                 :returning [:*]})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn get-expense-with-items
  [db id]
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
                               :where [:and [:= :e.id id]
                                       [:is :e.deleted_at nil]]})
                  {:builder-fn rs/as-unqualified-lower-maps})
        items (jdbc/execute!
                db
                (sql/format {:select [:*]
                             :from [:expense_items]
                             :where [:= :expense_id id]
                             :order-by [[:created_at :asc]]})
                {:builder-fn rs/as-unqualified-lower-maps})]
    (when expense
      (assoc expense :items items))))

(defn list-expenses
  "List expenses with common filters.
   opts: :from, :to, :supplier-id, :payer-id, :is-posted?, :limit, :offset."
  [db {:keys [from to supplier-id payer-id is-posted? limit offset order-dir]
       :or {limit 50 offset 0 order-dir :desc}}]
  (let [base-where (cond-> [:and
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
    (jdbc/execute! db (sql/format query) {:builder-fn rs/as-unqualified-lower-maps})))

(defn count-expenses
  "Count total expenses with optional filters."
  [db {:keys [from to supplier-id payer-id is-posted?]}]
  (let [base-where (cond-> [:and [:is :deleted_at nil]]
                     from (conj [:>= :purchased_at from])
                     to (conj [:<= :purchased_at to])
                     supplier-id (conj [:= :supplier_id supplier-id])
                     payer-id (conj [:= :payer_id payer-id])
                     (some? is-posted?) (conj [:= :is_posted (boolean is-posted?)]))
        query {:select [[[:count :*] :total]]
               :from [:expenses]
               :where base-where}]
    (:total (jdbc/execute-one! db (sql/format query)
              {:builder-fn rs/as-unqualified-lower-maps}))))

(defn create-from-receipt!
  "Create an expense tied to a receipt. Delegates to create-expense! then returns expense."
  [db receipt-id expense-data items]
  (create-expense! db (assoc expense-data :receipt_id receipt-id) items))
