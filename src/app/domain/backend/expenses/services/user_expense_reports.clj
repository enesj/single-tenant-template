(ns app.domain.backend.expenses.services.user-expense-reports
  "User-scoped reporting queries for expense analytics."
  (:require
    [clojure.set :as set]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.math BigDecimal]
    [java.time YearMonth ZoneOffset]
    [java.util UUID]))

(def ^:private day-of-week-template
  [{:iso_day_of_week 1 :day_key "mon" :day_label "Monday"}
   {:iso_day_of_week 2 :day_key "tue" :day_label "Tuesday"}
   {:iso_day_of_week 3 :day_key "wed" :day_label "Wednesday"}
   {:iso_day_of_week 4 :day_key "thu" :day_label "Thursday"}
   {:iso_day_of_week 5 :day_key "fri" :day_label "Friday"}
   {:iso_day_of_week 6 :day_key "sat" :day_label "Saturday"}
   {:iso_day_of_week 7 :day_key "sun" :day_label "Sunday"}])

(def ^:private size-bucket-template
  [{:bucket-key "lt_10" :bucket-label "Under 10" :sort-order 1}
   {:bucket-key "10_25" :bucket-label "10 to 24.99" :sort-order 2}
   {:bucket-key "25_50" :bucket-label "25 to 49.99" :sort-order 3}
   {:bucket-key "50_100" :bucket-label "50 to 99.99" :sort-order 4}
   {:bucket-key "100_200" :bucket-label "100 to 199.99" :sort-order 5}
   {:bucket-key "gte_200" :bucket-label "200 and above" :sort-order 6}])

(def ^:private size-bucket-case-sql
  "CASE
     WHEN e.total_amount < 10 THEN 'lt_10'
     WHEN e.total_amount >= 10 AND e.total_amount < 25 THEN '10_25'
     WHEN e.total_amount >= 25 AND e.total_amount < 50 THEN '25_50'
     WHEN e.total_amount >= 50 AND e.total_amount < 100 THEN '50_100'
     WHEN e.total_amount >= 100 AND e.total_amount < 200 THEN '100_200'
     ELSE 'gte_200'
   END")

(defn- bd
  [v]
  (cond
    (instance? BigDecimal v) v
    (nil? v) 0M
    :else (bigdec v)))

(defn- ensure-uuid
  [v]
  (cond
    (nil? v) nil
    (instance? UUID v) v
    :else (UUID/fromString (str v))))

(defn- query-many
  [db query]
  (jdbc/execute! db (sql/format query) {:builder-fn rs/as-unqualified-lower-maps}))

(defn- query-one
  [db query]
  (jdbc/execute-one! db (sql/format query) {:builder-fn rs/as-unqualified-lower-maps}))

(defn- base-where
  [user-id {:keys [from to currency supplier-id payer-id expense-category-id]}]
  (cond-> [:and
           [:= :e.user_id user-id]
           [:= :e.is_posted true]]
    from (conj [:>= :e.purchased_at from])
    to (conj [:<= :e.purchased_at to])
    (seq currency) (conj [:= :e.currency [:cast currency :currency]])
    supplier-id (conj [:= :e.supplier_id supplier-id])
    payer-id (conj [:= :e.payer_id payer-id])
    expense-category-id (conj [:= :e.expense_category_id expense-category-id])))

(defn- item-base-where
  [user-id {:keys [category-id subcategory-id manufacturer-id] :as opts}]
  (cond-> (base-where user-id opts)
    category-id (conj [:= :c.id category-id])
    subcategory-id (conj [:= :sc.id subcategory-id])
    manufacturer-id (conj [:= :m.id manufacturer-id])))

(defn- month->range
  [month]
  (let [month* (YearMonth/parse month)
        from (-> month* (.atDay 1) (.atStartOfDay ZoneOffset/UTC) (.toInstant))
        to (-> month* (.plusMonths 1) (.atDay 1) (.atStartOfDay ZoneOffset/UTC) (.toInstant))]
    {:from from
     :to to}))

(defn- enrich-with-all-weekdays
  [rows currency]
  (let [rows-by-key (into {} (map (juxt (fn [row] [(:currency row) (int (:iso_day_of_week row))]) identity) rows))
        currencies (cond
                     (seq currency) [currency]
                     (seq rows) (sort (distinct (keep :currency rows)))
                     :else [])]
    (mapv (fn [curr]
            {:currency curr
             :days (mapv (fn [{:keys [iso_day_of_week day_key day_label]}]
                           (let [base {:iso_day_of_week iso_day_of_week
                                       :day_key day_key
                                       :day_label day_label
                                       :total_amount 0M
                                       :expense_count 0}]
                             (merge base (get rows-by-key [curr iso_day_of_week]))))
                     day-of-week-template)})
      currencies)))

(defn- fill-size-buckets
  [rows currency]
  (let [rows-by-key (into {} (map (juxt (fn [row] [(:currency row) (:bucket_key row)]) identity) rows))
        currencies (cond
                     (seq currency) [currency]
                     (seq rows) (sort (distinct (keep :currency rows)))
                     :else [])]
    (mapcat (fn [curr]
              (map (fn [{:keys [bucket-key bucket-label sort-order]}]
                     (let [base {:currency curr
                                 :bucket_key bucket-key
                                 :bucket_label bucket-label
                                 :sort_order sort-order
                                 :total_amount 0M
                                 :expense_count 0}]
                       (merge base (get rows-by-key [curr bucket-key]))))
                size-bucket-template))
      currencies)))

(defn- uncategorized-row
  [currency]
  {:category_key "uncategorized"
   :category_name "Uncategorized"
   :currency currency
   :total_amount 0M
   :line_count 0})

(defn- ensure-uncategorized
  [rows currency]
  (let [currencies (cond
                     (seq currency) [currency]
                     (seq rows) (sort (distinct (keep :currency rows)))
                     :else [])]
    (reduce
      (fn [acc curr]
        (if (some (fn [row]
                    (and (= curr (:currency row))
                      (= "uncategorized" (:category_key row))))
              acc)
          acc
          (conj acc (uncategorized-row curr))))
      (vec rows)
      currencies)))

(defn- allocation-with-percentages
  [rows]
  (let [rows-by-currency (group-by :currency rows)
        totals-by-currency (into {}
                             (map (fn [[curr rs]]
                                    [curr (reduce (fn [acc {:keys [total_amount]}]
                                                    (+ acc (bd total_amount)))
                                            0M
                                            rs)]))
                             rows-by-currency)]
    (mapv (fn [row]
            (let [currency (:currency row)
                  total (get totals-by-currency currency 0M)
                  row-total (bd (:total_amount row))
                  pct (if (pos? (compare total 0M))
                        (* 100.0 (/ (double row-total) (double total)))
                        0.0)]
              (assoc row :allocation_pct pct)))
      rows)))

(defn- summary-by-currency
  [db user-id opts]
  (query-many
    db
    {:select [:e.currency
              [[:sum :e.total_amount] :total_amount]
              [[:count :*] :expense_count]]
     :from [[:expenses :e]]
     :where (base-where user-id opts)
     :group-by [:e.currency]
     :order-by [[:e.currency :asc]]}))

(defn- supplier-breakdown-by-currency
  [db user-id opts]
  (query-many
    db
    {:select [:e.supplier_id
              [:s.display_name :supplier_name]
              :e.currency
              [[:sum :e.total_amount] :total_amount]
              [[:count :*] :expense_count]]
     :from [[:expenses :e]]
     :left-join [[:suppliers :s] [:= :s.id :e.supplier_id]]
     :where (base-where user-id opts)
     :group-by [:e.supplier_id :s.display_name :e.currency]
     :order-by [[[:sum :e.total_amount] :desc]
                [:s.display_name :asc]]}))

(defn get-user-supplier-deep-dive
  "Supplier deep-dive report for a specific supplier.

  Returns:
  {:supplier-id ..
   :supplier-name ..
   :summary [...]
   :trend [...]
   :top-aliases [...]}"
  [db user-id {:keys [supplier-id alias-limit] :or {alias-limit 10} :as opts}]
  (let [user-id (ensure-uuid user-id)
        supplier-id (ensure-uuid supplier-id)]
    (when-not user-id
      (throw (ex-info "user-id is required" {:status 400})))
    (when-not supplier-id
      (throw (ex-info "supplier-id is required" {:status 400})))
    (let [opts* (assoc opts :supplier-id supplier-id)
          supplier-name (some-> (query-one db {:select [:display_name]
                                               :from [:suppliers]
                                               :where [:= :id supplier-id]
                                               :limit 1})
                          :display_name)
          summary (summary-by-currency db user-id opts*)
          trend (query-many
                  db
                  {:select [[[:to_char :e.purchased_at [:inline "YYYY-MM"]] :month]
                            :e.currency
                            [[:sum :e.total_amount] :total_amount]
                            [[:count :*] :expense_count]]
                   :from [[:expenses :e]]
                   :where (base-where user-id opts*)
                   :group-by [[:to_char :e.purchased_at [:inline "YYYY-MM"]] :e.currency]
                   :order-by [[[:to_char :e.purchased_at [:inline "YYYY-MM"]] :asc]
                              [:e.currency :asc]]})
          top-aliases (query-many
                        db
                        {:select [[:aa.id :alias_id]
                                  [[:raw "COALESCE(aa.raw_label, a.canonical_name, 'Unmapped item')"] :alias_label]
                                  [:a.canonical_name :article_canonical_name]
                                  :e.currency
                                  [[:sum :ei.line_total] :total_amount]
                                  [[:count :*] :line_count]]
                         :from [[:expense_items :ei]]
                         :join [[:expenses :e] [:= :e.id :ei.expense_id]]
                         :left-join [[:article_aliases :aa] [:= :aa.id :ei.alias_id]
                                     [:articles :a] [:= :a.id :aa.article_id]
                                     [:subcategories :sc] [:= :sc.id :a.subcategory_id]
                                     [:categories :c] [:= :c.id :sc.category_id]
                                     [:manufacturers :m] [:= :m.id :a.manufacturer_id]]
                         :where (item-base-where user-id opts*)
                         :group-by [:aa.id
                                    [:raw "COALESCE(aa.raw_label, a.canonical_name, 'Unmapped item')"]
                                    :a.canonical_name
                                    :e.currency]
                         :order-by [[[:sum :ei.line_total] :desc]
                                    [[:raw "COALESCE(aa.raw_label, a.canonical_name, 'Unmapped item')"] :asc]]
                         :limit (-> (or alias-limit 10) long (max 1) (min 100))})]
      {:supplier-id supplier-id
       :supplier-name supplier-name
       :summary (vec summary)
       :trend (vec trend)
       :top-aliases (vec top-aliases)})))

(defn get-user-day-of-week-spending-pattern
  "Spending pattern grouped by day-of-week (ISO: 1=Mon .. 7=Sun)."
  [db user-id opts]
  (let [user-id (ensure-uuid user-id)]
    (when-not user-id
      (throw (ex-info "user-id is required" {:status 400})))
    (let [rows (query-many
                 db
                 {:select [[[:raw "extract(isodow from e.purchased_at)::int"] :iso_day_of_week]
                           :e.currency
                           [[:sum :e.total_amount] :total_amount]
                           [[:count :*] :expense_count]]
                  :from [[:expenses :e]]
                  :where (base-where user-id opts)
                  :group-by [[:raw "extract(isodow from e.purchased_at)::int"] :e.currency]
                  :order-by [[:e.currency :asc]
                             [[:raw "extract(isodow from e.purchased_at)::int"] :asc]]})]
      (enrich-with-all-weekdays rows (:currency opts)))))

(defn get-user-top-item-spending
  "Top product/item spending by alias label."
  [db user-id {:keys [limit] :as opts}]
  (let [user-id (ensure-uuid user-id)
        limit* (-> (or limit 20) long (max 1) (min 100))]
    (when-not user-id
      (throw (ex-info "user-id is required" {:status 400})))
    (query-many
      db
      {:select [[:aa.id :alias_id]
                [[:raw "COALESCE(aa.raw_label, a.canonical_name, 'Unmapped item')"] :alias_label]
                [:a.canonical_name :article_canonical_name]
                :e.currency
                [[:sum :ei.line_total] :total_amount]
                [[:sum :ei.qty] :qty_total]
                [[:count :*] :line_count]]
       :from [[:expense_items :ei]]
       :join [[:expenses :e] [:= :e.id :ei.expense_id]]
       :left-join [[:article_aliases :aa] [:= :aa.id :ei.alias_id]
                   [:articles :a] [:= :a.id :aa.article_id]
                   [:subcategories :sc] [:= :sc.id :a.subcategory_id]
                   [:categories :c] [:= :c.id :sc.category_id]
                   [:manufacturers :m] [:= :m.id :a.manufacturer_id]]
       :where (item-base-where user-id opts)
       :group-by [:aa.id
                  [:raw "COALESCE(aa.raw_label, a.canonical_name, 'Unmapped item')"]
                  :a.canonical_name
                  :e.currency]
       :order-by [[[:sum :ei.line_total] :desc]
                  [[:raw "COALESCE(aa.raw_label, a.canonical_name, 'Unmapped item')"] :asc]]
       :limit limit*})))

(defn get-user-monthly-comparison
  "Compare two months and return deltas by currency and supplier."
  [db user-id {:keys [month-a month-b] :as opts}]
  (let [user-id (ensure-uuid user-id)]
    (when-not user-id
      (throw (ex-info "user-id is required" {:status 400})))
    (let [range-a (month->range month-a)
          range-b (month->range month-b)
          opts-a (assoc opts :from (:from range-a) :to (:to range-a))
          opts-b (assoc opts :from (:from range-b) :to (:to range-b))
          summary-a (summary-by-currency db user-id opts-a)
          summary-b (summary-by-currency db user-id opts-b)
          supplier-a (supplier-breakdown-by-currency db user-id opts-a)
          supplier-b (supplier-breakdown-by-currency db user-id opts-b)
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
                                        a-total (bd (:total_amount a))
                                        b-total (bd (:total_amount b))
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
                                             a-total (bd (:total_amount a))
                                             b-total (bd (:total_amount b))
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
                                           [(- (double (bd (:delta_amount row))))
                                            (:supplier_name row)]))
                                vec)]
      {:month_a month-a
       :month_b month-b
       :by_currency currency-comparison
       :by_supplier supplier-comparison})))

(defn get-user-expense-size-distribution
  "Expense-size distribution in deterministic buckets."
  [db user-id opts]
  (let [user-id (ensure-uuid user-id)]
    (when-not user-id
      (throw (ex-info "user-id is required" {:status 400})))
    (let [rows (query-many
                 db
                 {:select [[[:raw size-bucket-case-sql] :bucket_key]
                           :e.currency
                           [[:sum :e.total_amount] :total_amount]
                           [[:count :*] :expense_count]]
                  :from [[:expenses :e]]
                  :where (base-where user-id opts)
                  :group-by [[:raw size-bucket-case-sql] :e.currency]
                  :order-by [[:e.currency :asc]
                             [[:raw size-bucket-case-sql] :asc]]})
          filled (fill-size-buckets rows (:currency opts))
          bucket-meta (into {} (map (juxt :bucket-key identity) size-bucket-template))]
      (mapv (fn [row]
              (let [meta (get bucket-meta (:bucket_key row))]
                (merge row
                  {:bucket_label (:bucket-label meta)
                   :sort_order (:sort-order meta)})))
        filled))))

(defn get-user-daily-heatmap
  "Daily heatmap aggregation by date and currency."
  [db user-id opts]
  (let [user-id (ensure-uuid user-id)]
    (when-not user-id
      (throw (ex-info "user-id is required" {:status 400})))
    (query-many
      db
      {:select [[[:raw "to_char(e.purchased_at, 'YYYY-MM-DD')"] :day]
                [[:raw "extract(isodow from e.purchased_at)::int"] :iso_day_of_week]
                :e.currency
                [[:sum :e.total_amount] :total_amount]
                [[:count :*] :expense_count]]
       :from [[:expenses :e]]
       :where (base-where user-id opts)
       :group-by [[:raw "to_char(e.purchased_at, 'YYYY-MM-DD')"]
                  [:raw "extract(isodow from e.purchased_at)::int"]
                  :e.currency]
       :order-by [[[:raw "to_char(e.purchased_at, 'YYYY-MM-DD')"] :asc]
                  [:e.currency :asc]]})))

(defn get-user-category-allocation
  "Category allocation based on expense items with uncategorized bucket support."
  [db user-id opts]
  (let [user-id (ensure-uuid user-id)]
    (when-not user-id
      (throw (ex-info "user-id is required" {:status 400})))
    (let [rows (query-many
                 db
                 {:select [[[:raw "COALESCE(c.id::text, 'uncategorized')"] :category_key]
                           [[:raw "COALESCE(c.name, 'Uncategorized')"] :category_name]
                           :e.currency
                           [[:sum :ei.line_total] :total_amount]
                           [[:count :*] :line_count]]
                  :from [[:expense_items :ei]]
                  :join [[:expenses :e] [:= :e.id :ei.expense_id]]
                  :left-join [[:article_aliases :aa] [:= :aa.id :ei.alias_id]
                              [:articles :a] [:= :a.id :aa.article_id]
                              [:subcategories :sc] [:= :sc.id :a.subcategory_id]
                              [:categories :c] [:= :c.id :sc.category_id]
                              [:manufacturers :m] [:= :m.id :a.manufacturer_id]]
                  :where (item-base-where user-id opts)
                  :group-by [[:raw "COALESCE(c.id::text, 'uncategorized')"]
                             [:raw "COALESCE(c.name, 'Uncategorized')"]
                             :e.currency]
                  :order-by [[[:sum :ei.line_total] :desc]
                             [[:raw "COALESCE(c.name, 'Uncategorized')"] :asc]]})
          rows* (-> rows
                  (ensure-uncategorized (:currency opts))
                  allocation-with-percentages)]
      (vec rows*))))
