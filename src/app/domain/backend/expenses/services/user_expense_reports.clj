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

(defn- id-filter-clause
  [column value]
  (cond
    (nil? value) nil
    (sequential? value) (let [values (vec (remove nil? value))]
                          (when (seq values)
                            [:in column values]))
    :else [:= column value]))

(defn- base-where
  [user-id {:keys [from to currency supplier-id payer-id expense-category-id]}]
  (let [supplier-clause (id-filter-clause :e.supplier_id supplier-id)
        payer-clause (id-filter-clause :e.payer_id payer-id)
        expense-category-clause (id-filter-clause :e.expense_category_id expense-category-id)]
    (cond-> [:and
             [:= :e.user_id user-id]
             [:= :e.is_posted true]]
      from (conj [:>= :e.purchased_at from])
      to (conj [:<= :e.purchased_at to])
      (seq currency) (conj [:= :e.currency [:cast currency :currency]])
      supplier-clause (conj supplier-clause)
      payer-clause (conj payer-clause)
      expense-category-clause (conj expense-category-clause))))

(defn- item-base-where
  [user-id {:keys [category-id subcategory-id manufacturer-id] :as opts}]
  (let [category-clause (id-filter-clause :c.id category-id)
        subcategory-clause (id-filter-clause :sc.id subcategory-id)
        manufacturer-clause (id-filter-clause :m.id manufacturer-id)]
    (cond-> (base-where user-id opts)
      category-clause (conj category-clause)
      subcategory-clause (conj subcategory-clause)
      manufacturer-clause (conj manufacturer-clause))))

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
   :subcategory_key "uncategorized"
   :subcategory_name "Uncategorized"
   :currency currency
   :total_amount 0M
   :subcategory_total_amount 0M
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

(defn get-user-top-suppliers
  "Top suppliers ranked by total spending grouped by currency."
  [db user-id {:keys [limit] :as opts}]
  (let [user-id (ensure-uuid user-id)
        limit* (-> (or limit 20) long (max 1) (min 100))]
    (when-not user-id
      (throw (ex-info "user-id is required" {:status 400})))
    (let [supplier-rows (supplier-breakdown-by-currency db user-id opts)
          totals-by-currency (->> (summary-by-currency db user-id opts)
                               (map (juxt :currency (comp bd :total_amount)))
                               (into {}))]
      (->> supplier-rows
        (map (fn [row]
               (let [supplier-total (bd (:total_amount row))
                     grand-total (get totals-by-currency (:currency row) 0M)
                     share-pct (if (pos? (compare grand-total 0M))
                                 (* 100.0 (/ (double supplier-total) (double grand-total)))
                                 0.0)]
                 (assoc row :share_pct share-pct))))
        (sort-by (fn [row]
                   [(- (double (bd (:total_amount row))))
                    (or (:supplier_name row) "")
                    (str (:supplier_id row))
                    (or (:currency row) "")]))
        (take limit*)
        vec))))

(defn get-user-supplier-stores
  "Store-level supplier breakdown grouped by currency."
  [db user-id {:keys [supplier-id limit] :as opts}]
  (let [user-id (ensure-uuid user-id)
        supplier-id (ensure-uuid supplier-id)
        limit* (-> (or limit 20) long (max 1) (min 100))]
    (when-not user-id
      (throw (ex-info "user-id is required" {:status 400})))
    (when-not supplier-id
      (throw (ex-info "supplier-id is required" {:status 400})))
    (let [opts* (assoc opts :supplier-id supplier-id)
          totals-by-currency (->> (summary-by-currency db user-id opts*)
                               (map (juxt :currency (comp bd :total_amount)))
                               (into {}))
          resolved-store-id-sql
          [:raw
           "COALESCE(
              e.store_id,
              sa_direct.store_id,
              (
                SELECT sa_fallback.store_id
                FROM receipts r_fallback
                LEFT JOIN store_aliases sa_fallback ON sa_fallback.id = r_fallback.store_alias_id
                WHERE r_fallback.expense_id = e.id
                  AND sa_fallback.store_id IS NOT NULL
                ORDER BY COALESCE(r_fallback.updated_at, r_fallback.created_at) DESC,
                         r_fallback.id DESC
                LIMIT 1
              )
            )"]
          store-name-sql [:raw "COALESCE(st.display_name, 'Unmapped store')"]
          rows (query-many
                 db
                 {:select [[resolved-store-id-sql :store_id]
                           [store-name-sql :store_name]
                           [:ci.name :city_name]
                           :e.currency
                           [[:sum :e.total_amount] :total_amount]
                           [[:count :*] :expense_count]]
                  :from [[:expenses :e]]
                  :left-join [[:receipts :r_direct] [:= :r_direct.id :e.receipt_id]
                              [:store_aliases :sa_direct] [:= :sa_direct.id :r_direct.store_alias_id]
                              [:stores :st] [:= :st.id resolved-store-id-sql]
                              [:cities :ci] [:= :ci.id :st.city_id]]
                  :where (base-where user-id opts*)
                  :group-by [resolved-store-id-sql store-name-sql :ci.name :e.currency]
                  :order-by [[[:sum :e.total_amount] :desc]
                             [store-name-sql :asc]
                             [:e.currency :asc]]
                  :limit limit*})]
      (->> rows
        (mapv (fn [row]
                (let [currency-total (get totals-by-currency (:currency row) 0M)
                      row-total (bd (:total_amount row))
                      share-pct (if (pos? (compare currency-total 0M))
                                  (* 100.0 (/ (double row-total) (double currency-total)))
                                  0.0)]
                  (assoc row :share_pct share-pct))))))))

(defn get-user-supplier-monthly-trends
  "Month-by-month supplier totals for the top suppliers in scope."
  [db user-id {:keys [limit] :as opts}]
  (let [user-id (ensure-uuid user-id)
        limit* (-> (or limit 10) long (max 1) (min 50))]
    (when-not user-id
      (throw (ex-info "user-id is required" {:status 400})))
    (let [top-supplier-ids (->> (query-many
                                  db
                                  {:select [:e.supplier_id
                                            [[:sum :e.total_amount] :total_amount]]
                                   :from [[:expenses :e]]
                                   :where (conj (base-where user-id opts)
                                            [:is-not :e.supplier_id nil])
                                   :group-by [:e.supplier_id]
                                   :order-by [[[:sum :e.total_amount] :desc]
                                              [:e.supplier_id :asc]]
                                   :limit limit*})
                             (keep :supplier_id)
                             vec)]
      (if (empty? top-supplier-ids)
        []
        (->> (query-many
               db
               {:select [:e.supplier_id
                         [:s.display_name :supplier_name]
                         [[:to_char :e.purchased_at [:inline "YYYY-MM"]] :month]
                         :e.currency
                         [[:sum :e.total_amount] :total_amount]
                         [[:count :*] :expense_count]]
                :from [[:expenses :e]]
                :left-join [[:suppliers :s] [:= :s.id :e.supplier_id]]
                :where (conj (base-where user-id opts)
                         [:in :e.supplier_id top-supplier-ids])
                :group-by [:e.supplier_id
                           :s.display_name
                           [:to_char :e.purchased_at [:inline "YYYY-MM"]]
                           :e.currency]
                :order-by [[[:to_char :e.purchased_at [:inline "YYYY-MM"]] :asc]
                           [[:sum :e.total_amount] :desc]
                           [:s.display_name :asc]
                           [:e.currency :asc]]})
          vec)))))

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
                                  ;; NOTE: `:line_count` is a legacy API field name kept for backward compatibility.
                                  ;; It now counts distinct posted receipts (e.id), not individual item lines; do not rename without
                                  ;; coordinating a breaking API change with all clients.
                                  [[:count [:distinct :e.id]] :line_count]]
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
                ;; Keep :line_count for API compatibility; it now counts distinct posted receipts (e.id).
                [[:count [:distinct :e.id]] :line_count]]
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

(defn- option-row
  [row]
  (let [id (:id row)
        name (:name row)
        category-id (:category_id row)]
    (when (and id (some? name))
      (cond-> {:id (str id)
               :name (str name)}
        category-id (assoc :category_id (str category-id))))))

(defn- query-option-list
  [db query]
  (->> (query-many db query)
    (keep option-row)
    vec))

(defn get-user-report-filter-options
  "Filter options with available DB data for the current report scope.

  Each option list ignores its own active filter, so users can keep adding values
  in the same dropdown without options disappearing."
  [db user-id opts]
  (let [user-id (ensure-uuid user-id)]
    (when-not user-id
      (throw (ex-info "user-id is required" {:status 400})))
    (let [item-joins [[:expenses :e] [:= :e.id :ei.expense_id]]
          item-left-joins [[:article_aliases :aa] [:= :aa.id :ei.alias_id]
                           [:articles :a] [:= :a.id :aa.article_id]
                           [:subcategories :sc] [:= :sc.id :a.subcategory_id]
                           [:categories :c] [:= :c.id :sc.category_id]
                           [:manufacturers :m] [:= :m.id :a.manufacturer_id]]
          suppliers (query-option-list
                      db
                      {:select [[:e.supplier_id :id]
                                [[:coalesce :s.display_name [:inline "Unknown supplier"]] :name]]
                       :from [[:expenses :e]]
                       :left-join [[:suppliers :s] [:= :s.id :e.supplier_id]]
                       :where (conj (base-where user-id (dissoc opts :supplier-id))
                                [:is-not :e.supplier_id nil])
                       :group-by [:e.supplier_id :s.display_name]
                       :order-by [[:s.display_name :asc]]})
          expense-categories (query-option-list
                               db
                               {:select [[:e.expense_category_id :id]
                                         [[:coalesce :ec.name [:inline "Uncategorized"]] :name]]
                                :from [[:expenses :e]]
                                :left-join [[:expense_categories :ec] [:= :ec.id :e.expense_category_id]]
                                :where (conj (base-where user-id (dissoc opts :expense-category-id))
                                         [:is-not :e.expense_category_id nil])
                                :group-by [:e.expense_category_id :ec.name]
                                :order-by [[:ec.name :asc]]})
          categories (query-option-list
                       db
                       {:select [[:c.id :id]
                                 [[:coalesce :c.name [:inline "Uncategorized"]] :name]]
                        :from [[:expense_items :ei]]
                        :join item-joins
                        :left-join item-left-joins
                        :where (conj (item-base-where user-id (dissoc opts :category-id))
                                 [:is-not :c.id nil])
                        :group-by [:c.id :c.name]
                        :order-by [[:c.name :asc]]})
          subcategories (query-option-list
                          db
                          {:select [[:sc.id :id]
                                    [[:coalesce :sc.name [:inline "Uncategorized"]] :name]
                                    [:sc.category_id :category_id]]
                           :from [[:expense_items :ei]]
                           :join item-joins
                           :left-join item-left-joins
                           :where (conj (item-base-where user-id (dissoc opts :subcategory-id))
                                    [:is-not :sc.id nil])
                           :group-by [:sc.id :sc.name :sc.category_id]
                           :order-by [[:sc.name :asc]]})
          manufacturers (query-option-list
                          db
                          {:select [[:m.id :id]
                                    [[:coalesce :m.display_name [:inline "Unknown manufacturer"]] :name]]
                           :from [[:expense_items :ei]]
                           :join item-joins
                           :left-join item-left-joins
                           :where (conj (item-base-where user-id (dissoc opts :manufacturer-id))
                                    [:is-not :m.id nil])
                           :group-by [:m.id :m.display_name]
                           :order-by [[:m.display_name :asc]]})]
      {:suppliers suppliers
       :categories categories
       :subcategories subcategories
       :expense-categories expense-categories
       :manufacturers manufacturers})))

(defn get-user-category-allocation
  "Category allocation based on expense items with uncategorized bucket support.

  Includes `:subcategory_name` + `:subcategory_total_amount` to support expanded
  subcategory spend breakdown in the reports UI."
  [db user-id opts]
  (let [user-id (ensure-uuid user-id)]
    (when-not user-id
      (throw (ex-info "user-id is required" {:status 400})))
    (let [category-key-sql [:raw "COALESCE(c.id::text, 'uncategorized')"]
          category-name-sql [:raw "COALESCE(c.name, 'Uncategorized')"]
          subcategory-key-sql [:raw "COALESCE(sc.id::text, 'uncategorized')"]
          subcategory-name-sql [:raw "COALESCE(sc.name, 'Uncategorized')"]
          joins [[:expenses :e] [:= :e.id :ei.expense_id]]
          left-joins [[:article_aliases :aa] [:= :aa.id :ei.alias_id]
                      [:articles :a] [:= :a.id :aa.article_id]
                      [:subcategories :sc] [:= :sc.id :a.subcategory_id]
                      [:categories :c] [:= :c.id :sc.category_id]
                      [:manufacturers :m] [:= :m.id :a.manufacturer_id]]
          where-clause (item-base-where user-id opts)
          category-totals-query {:select [[category-key-sql :category_key]
                                          [category-name-sql :category_name]
                                          :e.currency
                                          [[:sum :ei.line_total] :total_amount]
                                          ;; Keep :line_count for API compatibility; it counts distinct posted receipts (e.id).
                                          [[:count [:distinct :e.id]] :line_count]]
                                 :from [[:expense_items :ei]]
                                 :join joins
                                 :left-join left-joins
                                 :where where-clause
                                 :group-by [category-key-sql
                                            category-name-sql
                                            :e.currency]}
          subcategory-totals-query {:select [[category-key-sql :category_key]
                                             :e.currency
                                             [subcategory-key-sql :subcategory_key]
                                             [subcategory-name-sql :subcategory_name]
                                             [[:sum :ei.line_total] :subcategory_total_amount]]
                                    :from [[:expense_items :ei]]
                                    :join joins
                                    :left-join left-joins
                                    :where where-clause
                                    :group-by [category-key-sql
                                               :e.currency
                                               subcategory-key-sql
                                               subcategory-name-sql]}
          rows (query-many
                 db
                 {:with [[:category_totals category-totals-query]
                         [:subcategory_totals subcategory-totals-query]]
                  :select [[:ct.category_key :category_key]
                           [:ct.category_name :category_name]
                           [:ct.currency :currency]
                           [:ct.total_amount :total_amount]
                           [:ct.line_count :line_count]
                           [:st.subcategory_key :subcategory_key]
                           [:st.subcategory_name :subcategory_name]
                           [[:coalesce :st.subcategory_total_amount 0M] :subcategory_total_amount]]
                  :from [[:category_totals :ct]]
                  :left-join [[:subcategory_totals :st]
                              [:and
                               [:= :st.category_key :ct.category_key]
                               [:= :st.currency :ct.currency]]]
                  :order-by [[:ct.total_amount :desc]
                             [:ct.category_name :asc]
                             [:st.subcategory_total_amount :desc]
                             [:st.subcategory_name :asc]]})
          rows* (ensure-uncategorized rows (:currency opts))
          category-rows (->> rows*
                          (reduce (fn [acc row]
                                    (assoc acc
                                      [(:category_key row) (:currency row)]
                                      {:category_key (:category_key row)
                                       :currency (:currency row)
                                       :total_amount (or (:total_amount row) 0M)}))
                            {})
                          vals
                          allocation-with-percentages)
          allocation-by-category (into {}
                                   (map (fn [row]
                                          [[(:category_key row) (:currency row)]
                                           (:allocation_pct row)]))
                                   category-rows)]
      (->> rows*
        (mapv (fn [row]
                (assoc row
                  :allocation_pct (or (get allocation-by-category
                                        [(:category_key row) (:currency row)])
                                    0.0))))))))
