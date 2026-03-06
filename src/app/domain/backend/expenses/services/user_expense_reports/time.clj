(ns app.domain.backend.expenses.services.user-expense-reports.time
  (:require
    [app.domain.backend.expenses.services.user-expense-reports.shared :as shared]
    [clojure.set :as set]))

(defn day-of-week
  "Spending pattern grouped by day-of-week (ISO: 1=Mon .. 7=Sun)."
  [db user-id opts]
  (let [user-id (shared/ensure-uuid user-id)
        rows (shared/query-many
               db
               {:select [[[:raw "extract(isodow from e.purchased_at)::int"] :iso_day_of_week]
                         :e.currency
                         [[:sum :e.total_amount] :total_amount]
                         [[:count :*] :expense_count]]
                :from [[:expenses :e]]
                :where (shared/base-where user-id opts)
                :group-by [[:raw "extract(isodow from e.purchased_at)::int"] :e.currency]
                :order-by [[:e.currency :asc]
                           [[:raw "extract(isodow from e.purchased_at)::int"] :asc]]})]
    (shared/enrich-with-all-weekdays rows (:currency opts))))

(defn monthly-comparison
  "Compare two months and return deltas by currency and supplier."
  [db user-id {:keys [month-a month-b] :as opts}]
  (let [user-id (shared/ensure-uuid user-id)
        range-a (shared/month->range month-a)
        range-b (shared/month->range month-b)
        opts-a (assoc opts :from (:from range-a) :to (:to range-a))
        opts-b (assoc opts :from (:from range-b) :to (:to range-b))
        summary-a (shared/summary-by-currency db user-id opts-a)
        summary-b (shared/summary-by-currency db user-id opts-b)
        supplier-a (shared/supplier-breakdown-by-currency db user-id opts-a)
        supplier-b (shared/supplier-breakdown-by-currency db user-id opts-b)
        summary-map-a (into {} (map (juxt :currency identity) summary-a))
        summary-map-b (into {} (map (juxt :currency identity) summary-b))
        all-currencies (sort (set/union (set (keys summary-map-a))
                               (set (keys summary-map-b))))
        supplier-map-a (into {} (map (juxt (fn [row] [(:supplier_id row) (:currency row)]) identity) supplier-a))
        supplier-map-b (into {} (map (juxt (fn [row] [(:supplier_id row) (:currency row)]) identity) supplier-b))
        supplier-keys (set/union (set (keys supplier-map-a))
                        (set (keys supplier-map-b)))
        currency-comparison (mapv
                              (fn [currency]
                                (let [a (get summary-map-a currency)
                                      b (get summary-map-b currency)
                                      a-total (shared/bd (:total_amount a))
                                      b-total (shared/bd (:total_amount b))
                                      delta (- b-total a-total)
                                      delta-pct (when (pos? (compare a-total 0M))
                                                  (* 100.0 (/ (double delta) (double a-total))))]
                                  {:currency currency
                                   :month_a_total a-total
                                   :month_b_total b-total
                                   :delta_amount delta
                                   :delta_percent delta-pct
                                   :month_a_count (long (or (:expense_count a) 0))
                                   :month_b_count (long (or (:expense_count b) 0))
                                   :delta_count (- (long (or (:expense_count b) 0))
                                                  (long (or (:expense_count a) 0)))}))
                              all-currencies)
        supplier-comparison (->> supplier-keys
                              (map (fn [[supplier-id currency]]
                                     (let [a (get supplier-map-a [supplier-id currency])
                                           b (get supplier-map-b [supplier-id currency])
                                           a-total (shared/bd (:total_amount a))
                                           b-total (shared/bd (:total_amount b))
                                           delta (- b-total a-total)
                                           supplier-name (or (:supplier_name a) (:supplier_name b))]
                                       {:supplier_id supplier-id
                                        :supplier_name supplier-name
                                        :currency currency
                                        :month_a_total a-total
                                        :month_b_total b-total
                                        :delta_amount delta
                                        :delta_count (- (long (or (:expense_count b) 0))
                                                       (long (or (:expense_count a) 0)))})))
                              (sort-by (fn [row]
                                         [(- (double (shared/bd (:delta_amount row))))
                                          (:supplier_name row)]))
                              vec)]
    {:month_a month-a
     :month_b month-b
     :by_currency currency-comparison
     :by_supplier supplier-comparison}))

(defn size-distribution
  "Expense-size distribution in deterministic buckets."
  [db user-id opts]
  (let [user-id (shared/ensure-uuid user-id)
        rows (shared/query-many
               db
               {:select [[[:raw shared/size-bucket-case-sql] :bucket_key]
                         :e.currency
                         [[:sum :e.total_amount] :total_amount]
                         [[:count :*] :expense_count]]
                :from [[:expenses :e]]
                :where (shared/base-where user-id opts)
                :group-by [[:raw shared/size-bucket-case-sql] :e.currency]
                :order-by [[:e.currency :asc]
                           [[:raw shared/size-bucket-case-sql] :asc]]})
        filled (shared/fill-size-buckets rows (:currency opts))
        bucket-meta (into {} (map (juxt :bucket-key identity) shared/size-bucket-template))]
    (mapv (fn [row]
            (let [meta (get bucket-meta (:bucket_key row))]
              (merge row
                {:bucket_label (:bucket-label meta)
                 :sort_order (:sort-order meta)})))
      filled)))

(defn daily-heatmap
  "Daily heatmap aggregation by date and currency."
  [db user-id opts]
  (let [user-id (shared/ensure-uuid user-id)]
    (shared/query-many
      db
      {:select [[[:raw "to_char(e.purchased_at, 'YYYY-MM-DD')"] :day]
                [[:raw "extract(isodow from e.purchased_at)::int"] :iso_day_of_week]
                :e.currency
                [[:sum :e.total_amount] :total_amount]
                [[:count :*] :expense_count]]
       :from [[:expenses :e]]
       :where (shared/base-where user-id opts)
       :group-by [[:raw "to_char(e.purchased_at, 'YYYY-MM-DD')"]
                  [:raw "extract(isodow from e.purchased_at)::int"]
                  :e.currency]
       :order-by [[[:raw "to_char(e.purchased_at, 'YYYY-MM-DD')"] :asc]
                  [:e.currency :asc]]})))
