(ns app.domain.backend.expenses.services.dashboard
  "Dashboard aggregation queries for the workspace dashboard.

   All queries are tenant-scoped via tenant_id. Monetary aggregations use
   the pre-computed bam_amount column for cross-currency consistency."
  (:require
    [app.domain.backend.expenses.services.article-aliases :as article-aliases]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]
    [taoensso.timbre :as log])
  (:import
    [java.util UUID]))

(defn- ensure-uuid [v]
  (cond
    (instance? UUID v) v
    (string? v) (try (UUID/fromString v) (catch Exception _ nil))
    :else nil))

(defn- query
  "Execute a HoneySQL query map, returning unqualified maps."
  [db sql-map]
  (jdbc/execute! db (sql/format sql-map)
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- query-one [db sql-map]
  (first (query db sql-map)))

;; ---------------------------------------------------------------------------
;; Period summary (30d / 6m with comparison)
;; ---------------------------------------------------------------------------

(defn- period-summary
  "Compute expense count + total bam_amount for a given interval,
   plus the same stats for the preceding interval of equal length."
  [db tenant-id interval-expr]
  (let [tid (ensure-uuid tenant-id)]
    (query-one db
      {:select [[[:count :*] :expense_count]
                [[:coalesce [:sum :bam_amount] [:inline 0]] :total_spend_bam]
                ;; Previous period for comparison
                [[:count
                  [:case
                   [:< :purchased_at [:raw (str "NOW() - " interval-expr)]]
                   [:inline 1]]]
                 :prev_count]
                [[:coalesce
                  [:sum
                   [:case
                    [:< :purchased_at [:raw (str "NOW() - " interval-expr)]]
                    :bam_amount]]
                  [:inline 0]]
                 :prev_spend_bam]]
       :from :expenses
       :where [:and
               [:= :tenant_id tid]
               [:>= :purchased_at
                [:raw (str "NOW() - " interval-expr " * 2")]]]})))

(defn get-period-summary-30d
  "Summary for last 30 days with comparison to previous 30 days."
  [db tenant-id]
  (period-summary db tenant-id "INTERVAL '30 days'"))

(defn get-period-summary-6m
  "Summary for last 6 months with comparison to previous 6 months."
  [db tenant-id]
  (period-summary db tenant-id "INTERVAL '6 months'"))

;; ---------------------------------------------------------------------------
;; Monthly trend (6 calendar months)
;; ---------------------------------------------------------------------------

(defn get-monthly-trend
  "Returns up to 6 rows, one per calendar month (most recent first).
   Each row has :month_label, :total_bam, :expense_count."
  [db tenant-id]
  (let [tid (ensure-uuid tenant-id)]
    (query db
      {:select [[[:to_char
                  [:date_trunc [:inline "month"] :purchased_at]
                  [:inline "Mon YY"]]
                 :month_label]
                [[:coalesce [:sum :bam_amount] [:inline 0]] :total_bam]
                [[:count :*] :expense_count]]
       :from :expenses
       :where [:and
               [:= :tenant_id tid]
               [:>= :purchased_at [:raw "DATE_TRUNC('month', NOW()) - INTERVAL '5 months'"]]]
       :group-by [[:date_trunc [:inline "month"] :purchased_at]]
       :order-by [[[:date_trunc [:inline "month"] :purchased_at] :desc]]})))

;; ---------------------------------------------------------------------------
;; Top 3: Suppliers, Stores (by total spend)
;; ---------------------------------------------------------------------------

(defn get-top-suppliers
  "Top N suppliers by total bam_amount in the last 6 months."
  [db tenant-id & [{:keys [limit] :or {limit 3}}]]
  (let [tid (ensure-uuid tenant-id)]
    (query db
      {:select [:s.display_name
                [[:coalesce [:sum :e.bam_amount] [:inline 0]] :total_spend_bam]
                [[:count :*] :expense_count]]
       :from [[:expenses :e]]
       :join [[:suppliers :s] [:= :s.id :e.supplier_id]]
       :where [:and
               [:= :e.tenant_id tid]
               [:is-not :e.supplier_id nil]
               [:>= :e.purchased_at [:raw "NOW() - INTERVAL '6 months'"]]]
       :group-by [:e.supplier_id :s.display_name]
       :order-by [[[:sum :e.bam_amount] :desc]]
       :limit limit})))

(defn get-top-stores
  "Top N stores by total bam_amount in the last 6 months."
  [db tenant-id & [{:keys [limit] :or {limit 3}}]]
  (let [tid (ensure-uuid tenant-id)]
    (query db
      {:select [:st.display_name
                [[:coalesce [:sum :e.bam_amount] [:inline 0]] :total_spend_bam]
                [[:count :*] :expense_count]]
       :from [[:expenses :e]]
       :join [[:stores :st] [:= :st.id :e.store_id]]
       :where [:and
               [:= :e.tenant_id tid]
               [:is-not :e.store_id nil]
               [:>= :e.purchased_at [:raw "NOW() - INTERVAL '6 months'"]]]
       :group-by [:e.store_id :st.display_name]
       :order-by [[[:sum :e.bam_amount] :desc]]
       :limit limit})))

;; ---------------------------------------------------------------------------
;; Top 3: Articles (by frequency)
;; ---------------------------------------------------------------------------

(defn get-top-articles
  "Top N articles by purchase count (expense_items rows) in the last 6 months.
   Shows avg unit_price and the most common currency."
  [db tenant-id & [{:keys [limit] :or {limit 3}}]]
  (let [tid (ensure-uuid tenant-id)]
    (query db
      {:select [:a.canonical_name
                [[:count :*] :purchase_count]
                [[:round [:avg :ei.unit_price] [:inline 2]] :avg_unit_price]
                [[:raw "MODE() WITHIN GROUP (ORDER BY e.currency)"] :currency]]
       :from [[:expense_items :ei]]
       :join [[:expenses :e] [:= :e.id :ei.expense_id]
              [:articles :a] [:= :a.id :ei.article_id]]
       :where [:and
               [:= :e.tenant_id tid]
               [:is-not :ei.article_id nil]
               [:>= :e.purchased_at [:raw "NOW() - INTERVAL '6 months'"]]]
       :group-by [:ei.article_id :a.canonical_name]
       :order-by [[[:count :*] :desc]]
       :limit limit})))

;; ---------------------------------------------------------------------------
;; Price changes (30d avg vs previous 90d avg)
;; ---------------------------------------------------------------------------

(defn get-price-changes
  "Top N articles with largest price change between recent (30d) and baseline (90d) windows.
   Requires at least 2 observations in each window."
  [db tenant-id & [{:keys [limit min-obs] :or {limit 3 min-obs 2}}]]
  (let [tid (ensure-uuid tenant-id)]
    (query db
      {:with [[:recent
               {:select [:ei.article_id
                         [[:avg :ei.unit_price] :recent_avg]
                         [[:count :*] :recent_count]
                         [[:raw "MODE() WITHIN GROUP (ORDER BY e.currency)"] :currency]]
                :from [[:expense_items :ei]]
                :join [[:expenses :e] [:= :e.id :ei.expense_id]]
                :where [:and
                        [:= :e.tenant_id tid]
                        [:is-not :ei.article_id nil]
                        [:is-not :ei.unit_price nil]
                        [:>= :e.purchased_at [:raw "NOW() - INTERVAL '30 days'"]]]
                :group-by [:ei.article_id]}]
              [:baseline
               {:select [:ei.article_id
                         [[:avg :ei.unit_price] :baseline_avg]
                         [[:count :*] :baseline_count]]
                :from [[:expense_items :ei]]
                :join [[:expenses :e] [:= :e.id :ei.expense_id]]
                :where [:and
                        [:= :e.tenant_id tid]
                        [:is-not :ei.article_id nil]
                        [:is-not :ei.unit_price nil]
                        [:>= :e.purchased_at [:raw "NOW() - INTERVAL '120 days'"]]
                        [:< :e.purchased_at [:raw "NOW() - INTERVAL '30 days'"]]]
                :group-by [:ei.article_id]}]]
       :select [:a.canonical_name
                :b.baseline_avg
                :r.recent_avg
                [[:raw "ROUND(((r.recent_avg - b.baseline_avg) / NULLIF(b.baseline_avg, 0)) * 100, 1)"]
                 :change_percent]
                :r.currency]
       :from [[:recent :r]]
       :join [[:baseline :b] [:= :b.article_id :r.article_id]
              [:articles :a] [:= :a.id :r.article_id]]
       :where [:and
               [:>= :r.recent_count min-obs]
               [:>= :b.baseline_count min-obs]]
       :order-by [[[:raw "ABS((r.recent_avg - b.baseline_avg) / NULLIF(b.baseline_avg, 0))"] :desc]]
       :limit limit})))

;; ---------------------------------------------------------------------------
;; Category breakdown (last 6 months)
;; ---------------------------------------------------------------------------

(defn get-category-breakdown
  "Spending grouped by expense_category. NULL category grouped as 'Nekategorizirano'.
   Returns each category's share as a percentage of total."
  [db tenant-id]
  (let [tid (ensure-uuid tenant-id)]
    (query db
      {:with [[:cat_totals
               {:select [[[:coalesce :ec.name [:inline "Nekategorizirano"]] :category_name]
                         [[:coalesce [:sum :e.bam_amount] [:inline 0]] :total_spend_bam]
                         [[:count :*] :expense_count]]
                :from [[:expenses :e]]
                :left-join [[:expense_categories :ec] [:= :ec.id :e.expense_category_id]]
                :where [:and
                        [:= :e.tenant_id tid]
                        [:>= :e.purchased_at [:raw "NOW() - INTERVAL '6 months'"]]]
                :group-by [[:coalesce :ec.name [:inline "Nekategorizirano"]]]}]]
       :select [:category_name
                :total_spend_bam
                :expense_count
                [[:raw "ROUND(total_spend_bam * 100.0 / NULLIF(SUM(total_spend_bam) OVER (), 0), 1)"]
                 :percentage]]
       :from :cat_totals
       :order-by [[:total_spend_bam :desc]]})))

;; ---------------------------------------------------------------------------
;; Biggest expense (current calendar month)
;; ---------------------------------------------------------------------------

(defn get-biggest-expense
  "Largest single expense by bam_amount in the current calendar month."
  [db tenant-id]
  (let [tid (ensure-uuid tenant-id)]
    (query-one db
      {:select [:e.total_amount :e.currency :e.bam_amount :e.purchased_at
                [:s.display_name :supplier_name]
                [:st.display_name :store_name]]
       :from [[:expenses :e]]
       :left-join [[:suppliers :s] [:= :s.id :e.supplier_id]
                   [:stores :st] [:= :st.id :e.store_id]]
       :where [:and
               [:= :e.tenant_id tid]
               [:>= :e.purchased_at [:raw "DATE_TRUNC('month', NOW())"]]]
       :order-by [[:e.bam_amount :desc-nulls-last]]
       :limit 1})))

;; ---------------------------------------------------------------------------
;; Spending averages
;; ---------------------------------------------------------------------------

(defn get-spending-averages
  "Daily avg (30d), weekly avg (30d), monthly avg (6m) — all in BAM."
  [db tenant-id]
  (let [tid (ensure-uuid tenant-id)]
    (query-one db
      {:select [[[:coalesce
                  [:raw "SUM(CASE WHEN purchased_at >= NOW() - INTERVAL '30 days' THEN bam_amount ELSE 0 END) / 30.0"]
                  [:inline 0]]
                 :daily_avg_30d]
                [[:coalesce
                  [:raw "SUM(CASE WHEN purchased_at >= NOW() - INTERVAL '30 days' THEN bam_amount ELSE 0 END) / (30.0/7)"]
                  [:inline 0]]
                 :weekly_avg_30d]
                [[:coalesce
                  [:raw "SUM(bam_amount) / 6.0"]
                  [:inline 0]]
                 :monthly_avg_6m]]
       :from :expenses
       :where [:and
               [:= :tenant_id tid]
               [:>= :purchased_at [:raw "NOW() - INTERVAL '6 months'"]]]})))

;; ---------------------------------------------------------------------------
;; Team overview
;; ---------------------------------------------------------------------------

(defn get-team-overview
  "Member count and role breakdown for a tenant."
  [db tenant-id]
  (let [tid (ensure-uuid tenant-id)
        roles (query db
                {:select [:role [[:count :*] :count]]
                 :from :tenant_memberships
                 :where [:and
                         [:= :tenant_id tid]
                         [:= :status [:cast "active" :membership_status]]]
                 :group-by [:role]})
        tenant (query-one db
                 {:select [:name]
                  :from :tenants
                  :where [:= :id tid]})]
    {:total-members (reduce + 0 (map :count roles))
     :role-breakdown (mapv (fn [{:keys [role count]}]
                             {:role (if (keyword? role) (name role) (str role))
                              :count count})
                       roles)
     :tenant-name (:name tenant)}))

;; ---------------------------------------------------------------------------
;; Admin-only: pending count + unmapped alias count
;; ---------------------------------------------------------------------------

(defn get-pending-count
  "Count of expenses that are not yet posted (is_posted = false)."
  [db tenant-id]
  (let [tid (ensure-uuid tenant-id)]
    (:count
     (query-one db
       {:select [[[:count :*] :count]]
        :from :expenses
        :where [:and
                [:= :tenant_id tid]
                [:= :is_posted false]]}))))

(defn get-unmapped-alias-count
  "Count unmapped article aliases in the active tenant.
   Reuses the same tenant-aware semantics as the unmapped-aliases management page."
  [db tenant-id]
  (let [tid (ensure-uuid tenant-id)]
    (article-aliases/count-unmapped-aliases db
      (cond-> {}
        tid (assoc :tenant-id tid)))))

;; ---------------------------------------------------------------------------
;; Composite: full dashboard data
;; ---------------------------------------------------------------------------

(defn get-dashboard-data
  "Compute all dashboard data in one call. Admin-only fields are skipped
   when `power-user?` is false."
  [db tenant-id power-user?]
  (let [safe (fn [label f]
               (try (f)
                 (catch Exception e
                   (log/warn "Dashboard query failed" {:label label :error (.getMessage e)})
                   nil)))
        summary-30d (safe :summary-30d #(get-period-summary-30d db tenant-id))
        summary-6m  (safe :summary-6m #(get-period-summary-6m db tenant-id))
        trend       (safe :trend #(get-monthly-trend db tenant-id))
        suppliers   (safe :suppliers #(get-top-suppliers db tenant-id))
        stores      (safe :stores #(get-top-stores db tenant-id))
        articles    (safe :articles #(get-top-articles db tenant-id))
        prices      (safe :prices #(get-price-changes db tenant-id))
        categories  (safe :categories #(get-category-breakdown db tenant-id))
        biggest     (safe :biggest #(get-biggest-expense db tenant-id))
        averages    (safe :averages #(get-spending-averages db tenant-id))
        team        (safe :team #(get-team-overview db tenant-id))]
    (cond-> {:last-30-days     summary-30d
             :last-6-months    summary-6m
             :monthly-trend    (when trend
                                 (mapv (fn [row idx]
                                         (assoc row :month_index idx))
                                   trend (range)))
             :top-suppliers    (vec (or suppliers []))
             :top-stores       (vec (or stores []))
             :top-articles     (vec (or articles []))
             :price-changes    (when prices
                                 (mapv (fn [{:keys [change_percent] :as row}]
                                         (assoc row :direction
                                           (if (pos? (or change_percent 0)) "up" "down")))
                                   prices))
             :category-breakdown (vec (or categories []))
             :biggest-expense  biggest
             :averages         averages
             :team             team}
      power-user? (assoc :pending-count (safe :pending #(get-pending-count db tenant-id))
                    :unmapped-alias-count (safe :unmapped #(get-unmapped-alias-count db tenant-id))))))

(comment
  ;; REPL testing
  ;; (require 'app.domain.backend.expenses.services.dashboard :reload)
  ;; (def db (:db-pool (system.core/system)))
  ;; (def tid "your-tenant-id-here")
  ;; (get-dashboard-data db tid true)
  :rcf)
