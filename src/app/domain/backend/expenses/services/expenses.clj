(ns app.domain.backend.expenses.services.expenses
  "Expense creation/update with line items."
  (:require
    [app.domain.backend.expenses.services.price-history :as price-history]
    [clojure.string :as str]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.time Instant LocalDate LocalDateTime OffsetDateTime ZoneId ZoneOffset]
    [java.util Date UUID]))

;; ============================================================================
;; Helpers
;; ============================================================================

(defn- require-keys!
  [m ks]
  (doseq [k ks]
    (when-not (get m k)
      (throw (ex-info (str (name k) " is required")
               {:status 400
                :field k
                :data m})))))

(defn- blank->nil
  [v]
  (cond
    (nil? v) nil
    (and (string? v) (str/blank? v)) nil
    :else v))

(defn- parse-uuid!
  [field v]
  (let [v (blank->nil v)]
    (cond
      (nil? v) nil
      (instance? UUID v) v
      :else
      (try
        (UUID/fromString (str v))
        (catch IllegalArgumentException _
          (throw (ex-info (str "Invalid " (name field))
                   {:status 400
                    :field field
                    :value v})))))))

(defn- parse-bool
  "Parse boolean value. Named parse-bool to avoid shadowing clojure.core/parse-boolean."
  [v]
  (let [v (blank->nil v)]
    (cond
      (nil? v) nil
      (instance? Boolean v) v
      (string? v) (Boolean/parseBoolean v)
      :else (boolean v))))

(defn- parse-bigdec!
  [field v]
  (let [v (blank->nil v)]
    (cond
      (nil? v) nil
      (instance? java.math.BigDecimal v) v
      (number? v) (bigdec v)
      (string? v)
      (try
        (bigdec v)
        (catch Exception _
          (throw (ex-info (str "Invalid " (name field))
                   {:status 400
                    :field field
                    :value v}))))
      :else
      (throw (ex-info (str "Invalid " (name field))
               {:status 400
                :field field
                :value v})))))

(defn- parse-instant!
  [field v]
  (let [v (blank->nil v)]
    (cond
      (nil? v) nil
      (instance? Instant v) v
      (instance? Date v) (.toInstant ^Date v)
      (number? v) (Instant/ofEpochMilli (long v))
      (string? v)
      (or
        (try
          (Instant/parse v)
          (catch Exception _ nil))
        (try
          (-> (OffsetDateTime/parse v) .toInstant)
          (catch Exception _ nil))
        ;; Support HTML `datetime-local` values like "2025-12-12T12:34".
        (try
          (-> (LocalDateTime/parse v)
            (.atZone (ZoneId/systemDefault))
            .toInstant)
          (catch Exception _ nil))
        (try
          (-> (LocalDate/parse v) (.atStartOfDay ZoneOffset/UTC) .toInstant)
          (catch Exception _ nil))
        (throw (ex-info (str "Invalid " (name field))
                 {:status 400
                  :field field
                  :value v})))
      :else
      (throw (ex-info (str "Invalid " (name field))
               {:status 400
                :field field
                :value v})))))

(defn- update-if-present
  "Update map key only when it exists (avoids inserting optional columns with nil values)."
  [m k f]
  (if (contains? m k)
    (update m k f)
    m))

(defn- normalize-expense-item
  [item]
  (-> item
    (update-if-present :id #(parse-uuid! :id %))
    (update-if-present :article_id #(parse-uuid! :article_id %))
    (update-if-present :qty #(parse-bigdec! :qty %))
    (update-if-present :unit_price #(parse-bigdec! :unit_price %))
    (update-if-present :line_total #(parse-bigdec! :line_total %))))

(defn- normalize-expense-data
  [expense-data]
  (-> expense-data
    (update-if-present :supplier_id #(parse-uuid! :supplier_id %))
    (update-if-present :payer_id #(parse-uuid! :payer_id %))
    (update-if-present :user_id #(parse-uuid! :user_id %))
    (update-if-present :receipt_id #(parse-uuid! :receipt_id %))
    (update-if-present :purchased_at #(parse-instant! :purchased_at %))
    (update-if-present :total_amount #(parse-bigdec! :total_amount %))
    (update-if-present :is_posted parse-bool)))

;; ============================================================================
;; Core
;; ============================================================================

(defn create-expense!
  "Create an expense and its line items. Returns expense with :items.

  NOTE: The generic admin CRUD route factory calls create fns as (f db body).
  For expenses, the request body includes :items, so we support a 2-arity
  wrapper that delegates to the core 3-arity implementation."
  ([db {:keys [items] :as body}]
   (let [expense-data (normalize-expense-data (dissoc body :items))
         items* (mapv normalize-expense-item (or items []))]
     (create-expense! db expense-data items*)))
  ([db expense-data items]
   (let [expense-data (normalize-expense-data expense-data)
         items (mapv normalize-expense-item (or items []))]
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
                              {:id (UUID/randomUUID)
                               :expense_id expense-id
                               :raw_label raw_label
                               :article_id article_id
                               :qty qty
                               :unit_price unit_price
                               :line_total line_total})
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
         (assoc expense :items inserted-items))))))

(declare get-expense-with-items)

(defn update-expense!
  "Update expense fields and optionally line items (when :items is provided). Returns expense with :items."
  [db id {:keys [items] :as body}]
  (let [updates* (-> body
                   (dissoc :items)
                   normalize-expense-data
                   (select-keys [:supplier_id :payer_id :purchased_at :total_amount :currency :notes :is_posted])
                   (update-if-present :currency #(when % [:cast % :currency]))
                   (assoc :updated_at [:now]))
        sql-map {:update :expenses
                 :set updates*
                 :where [:and
                         [:= :id id]
                         [:is :deleted_at nil]]
                 :returning [:*]}]
    (jdbc/with-transaction [tx db]
      (when-let [expense (jdbc/execute-one!
                           tx
                           (sql/format sql-map)
                           {:builder-fn rs/as-unqualified-lower-maps})]
        (when (contains? body :items)
          (let [existing-ids (->> (jdbc/execute!
                                    tx
                                    (sql/format {:select [:id]
                                                 :from [:expense_items]
                                                 :where [:= :expense_id id]})
                                    {:builder-fn rs/as-unqualified-lower-maps})
                               (map :id)
                               (into #{}))
                items* (mapv normalize-expense-item (or items []))
                update-items (filterv (fn [{item-id :id}]
                                        (and item-id (contains? existing-ids item-id)))
                               items*)
                keep-ids (into #{} (map :id update-items))
                insert-items (filterv (fn [{item-id :id}]
                                        (not (and item-id (contains? existing-ids item-id))))
                               items*)
                delete-where (if (seq keep-ids)
                               [:and
                                [:= :expense_id id]
                                [:not [:in :id keep-ids]]]
                               [:= :expense_id id])]
            (jdbc/execute! tx (sql/format {:delete-from :expense_items
                                           :where delete-where}))
            (doseq [item update-items]
              (require-keys! item [:raw_label :line_total])
              (jdbc/execute! tx
                (sql/format {:update :expense_items
                             :set (select-keys item [:raw_label :article_id :qty :unit_price :line_total])
                             :where [:and
                                     [:= :id (:id item)]
                                     [:= :expense_id id]]})))
            (let [item-rows (map (fn [{:keys [raw_label article_id qty unit_price line_total] :as item}]
                                   (require-keys! item [:raw_label :line_total])
                                   {:id (UUID/randomUUID)
                                    :expense_id id
                                    :raw_label raw_label
                                    :article_id article_id
                                    :qty qty
                                    :unit_price unit_price
                                    :line_total line_total})
                              insert-items)
                  inserted-items (if (seq item-rows)
                                   (jdbc/execute!
                                     tx
                                     (sql/format {:insert-into :expense_items
                                                  :values item-rows
                                                  :returning [:*]})
                                     {:builder-fn rs/as-unqualified-lower-maps})
                                   [])]
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
                        :observed_at (:purchased_at expense)}))))))
        (get-expense-with-items tx id)))))

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

(defn get-expense
  "Get an expense (with items). Wrapper expected by the generic admin routes factory."
  [db id]
  (get-expense-with-items db id))

(defn delete-expense!
  "Delete an expense. Wrapper expected by the generic admin routes factory.
  Currently implemented as a soft delete."
  [db id]
  (soft-delete-expense! db id))

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
