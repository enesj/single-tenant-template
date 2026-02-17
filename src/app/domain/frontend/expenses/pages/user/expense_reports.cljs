(ns app.domain.frontend.expenses.pages.user.expense-reports
  "User-facing expense reports and analytics page."
  (:require
    [app.template.frontend.components.button :refer [button]]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-state]]
    [uix.re-frame :refer [use-subscribe]]))

;; ========================================================================
;; Formatting helpers
;; ========================================================================

(defn ->number
  [value]
  (cond
    (number? value) value
    (string? value) (let [parsed (js/parseFloat value)]
                      (when-not (js/isNaN parsed) parsed))
    :else nil))

(defn format-money
  [amount currency]
  (let [amount* (->number amount)
        currency* (or (some-> currency name)
                    (some-> currency str)
                    "USD")]
    (if (nil? amount*)
      "—"
      (try
        (.toLocaleString (js/Number amount*) "en-US"
          #js {:style "currency"
               :currency currency*
               :minimumFractionDigits 2
               :maximumFractionDigits 2})
        (catch :default _
          (str currency* " " (.toFixed (js/Number amount*) 2)))))))

(defn format-amount-only
  [amount]
  (let [amount* (->number amount)]
    (if (nil? amount*)
      "—"
      (try
        (.toLocaleString (js/Number amount*) "en-US"
          #js {:minimumFractionDigits 2
               :maximumFractionDigits 2})
        (catch :default _
          (.toFixed (js/Number amount*) 2))))))

(defn format-percent
  [value]
  (let [n (->number value)]
    (if (nil? n)
      "—"
      (str (.toFixed (js/Number n) 1) "%"))))

(defn format-int
  [value]
  (let [n (->number value)]
    (if (nil? n)
      "0"
      (.toLocaleString (js/Number n) "en-US"))))

(defn month-label
  [month]
  (let [[year month-part] (some-> month str (str/split #"-"))
        month-idx (some-> month-part js/parseInt)
        short-month (get ["" "Jan" "Feb" "Mar" "Apr" "May" "Jun"
                          "Jul" "Aug" "Sep" "Oct" "Nov" "Dec"]
                      month-idx
                      month)]
    (if (and year month-part)
      (str short-month " " year)
      (or month "—"))))

(defn supplier-options
  [rows]
  (->> (or rows [])
    (reduce
      (fn [acc row]
        (let [supplier-id (some-> (:supplier_id row) str)
              supplier-name (or (:supplier_name row) "Unknown supplier")]
          (if (seq supplier-id)
            (if (contains? acc supplier-id)
              acc
              (assoc acc supplier-id supplier-name))
            acc)))
      {})
    (mapv (fn [[id name]] {:id id :name name}))
    (sort-by :name)))

(defn month-options
  [by-month month-a month-b]
  (let [months (->> (or by-month [])
                 (keep :month)
                 (map str)
                 distinct
                 sort
                 reverse
                 vec)
        selected (->> [month-a month-b]
                   (keep #(some-> % str/trim))
                   (remove str/blank?)
                   vec)]
    (vec (distinct (concat selected months)))))

(defn aggregate-day-pattern
  [groups]
  (->> (or groups [])
    (mapcat (fn [group] (or (:days group) [])))
    (reduce
      (fn [acc row]
        (let [iso-day (int (or (:iso_day_of_week row) 0))
              base (get acc iso-day {:iso_day_of_week iso-day
                                     :day_key (:day_key row)
                                     :day_label (:day_label row)
                                     :total_amount 0
                                     :expense_count 0})]
          (assoc acc iso-day
            (-> base
              (update :total_amount + (or (->number (:total_amount row)) 0))
              (update :expense_count + (or (->number (:expense_count row)) 0))))))
      {})
    vals
    (sort-by :iso_day_of_week)
    vec))

(defn aggregate-size-buckets
  [rows]
  (->> (or rows [])
    (reduce
      (fn [acc row]
        (let [bucket-key (:bucket_key row)
              base (get acc bucket-key {:bucket_key bucket-key
                                        :bucket_label (:bucket_label row)
                                        :sort_order (or (:sort_order row) 0)
                                        :total_amount 0
                                        :expense_count 0})]
          (assoc acc bucket-key
            (-> base
              (update :total_amount + (or (->number (:total_amount row)) 0))
              (update :expense_count + (or (->number (:expense_count row)) 0))))))
      {})
    vals
    (sort-by :sort_order)
    vec))

(defn aggregate-heatmap
  [rows]
  (->> (or rows [])
    (reduce
      (fn [acc row]
        (let [day (:day row)
              base (get acc day {:day day
                                 :iso_day_of_week (:iso_day_of_week row)
                                 :total_amount 0
                                 :expense_count 0})]
          (assoc acc day
            (-> base
              (update :total_amount + (or (->number (:total_amount row)) 0))
              (update :expense_count + (or (->number (:expense_count row)) 0))))))
      {})
    vals
    (sort-by :day)
    vec))

(defn aggregate-category-allocation
  [rows]
  (let [aggregated (->> (or rows [])
                     (reduce
                       (fn [acc row]
                         (let [category-key (:category_key row)
                               base (get acc category-key {:category_key category-key
                                                           :category_name (:category_name row)
                                                           :total_amount 0
                                                           :line_count 0})]
                           (assoc acc category-key
                             (-> base
                               (update :total_amount + (or (->number (:total_amount row)) 0))
                               (update :line_count + (or (->number (:line_count row)) 0))))))
                       {})
                     vals
                     vec)
        total (reduce + 0 (map #(or (->number (:total_amount %)) 0) aggregated))]
    (->> aggregated
      (mapv
        (fn [row]
          (let [row-total (or (->number (:total_amount row)) 0)
                allocation-pct (if (pos? total) (* 100 (/ row-total total)) 0)]
            (assoc row :allocation_pct allocation-pct))))
      (sort-by :total_amount >)
      vec)))

(defn bucket-range
  [bucket-key]
  (case bucket-key
    "lt_10" [nil 10]
    "10_25" [10 25]
    "25_50" [25 50]
    "50_100" [50 100]
    "100_200" [100 200]
    "gte_200" [200 nil]
    [nil nil]))

(defn in-bucket?
  [amount bucket-key]
  (let [[min-value max-value] (bucket-range bucket-key)
        amount* (or (->number amount) 0)]
    (and (or (nil? min-value) (<= min-value amount*))
      (or (nil? max-value) (< amount* max-value)))))

(defn filter-top-items-by-bucket
  [rows bucket-key]
  (if (str/blank? (str (or bucket-key "")))
    (vec (or rows []))
    (->> (or rows [])
      (filter #(in-bucket? (:total_amount %) bucket-key))
      vec)))

(defn heat-intensity-class
  [ratio]
  (cond
    (>= ratio 0.8) "bg-primary text-primary-content shadow-sm scale-105 font-bold"
    (>= ratio 0.55) "bg-primary/80 text-primary-content font-semibold"
    (>= ratio 0.3) "bg-primary/40 text-base-content font-medium"
    (pos? ratio) "bg-primary/10 text-base-content"
    :else "bg-base-100 text-base-content/30 hover:bg-base-200 transition-colors"))

(defn default-sort-direction
  [sort-type]
  (if (= sort-type :number) :desc :asc))

(defn toggle-sort-config
  [current-sort column sort-type]
  (if (= column (:column current-sort))
    (update current-sort :direction #(if (= % :asc) :desc :asc))
    {:column column
     :direction (default-sort-direction sort-type)
     :type sort-type}))

(defn blank-sort-value?
  [value]
  (or (nil? value)
    (and (string? value) (str/blank? value))))

(defn compare-sort-values
  [left right sort-type]
  (cond
    (and (blank-sort-value? left) (blank-sort-value? right)) 0
    (blank-sort-value? left) 1
    (blank-sort-value? right) -1
    (= sort-type :number)
    (let [left-num (->number left)
          right-num (->number right)]
      (cond
        (and (nil? left-num) (nil? right-num)) 0
        (nil? left-num) 1
        (nil? right-num) -1
        :else (compare left-num right-num)))
    :else
    (compare (-> left str str/lower-case)
      (-> right str str/lower-case))))

(defn sort-rows-by-config
  [rows {:keys [column direction type]}]
  (let [direction-multiplier (if (= direction :desc) -1 1)]
    (->> (or rows [])
      (map-indexed (fn [idx row] {:idx idx :row row}))
      (sort (fn [a b]
              (let [cmp (compare-sort-values
                          (get (:row a) column)
                          (get (:row b) column)
                          type)]
                (if (zero? cmp)
                  (compare (:idx a) (:idx b))
                  (* direction-multiplier cmp)))))
      (mapv :row))))

(defn sortable-column-label
  [label sort-config column]
  (let [active? (= column (:column sort-config))
        indicator (if (= :asc (:direction sort-config)) "↑" "↓")]
    (if active?
      (str label " " indicator)
      label)))

(defui stat-card [{:keys [title value subtitle icon loading?]}]
  ($ :div {:class "bg-white rounded-xl shadow-sm border border-base-200/60 p-6 flex flex-col justify-between h-full transition-all duration-300 hover:shadow-lg hover:shadow-primary/5 hover:border-primary/20 hover:-translate-y-0.5"}
    ($ :div {:class "flex items-start justify-between mb-2"}
      ($ :div
        ($ :p {:class "text-sm font-bold text-base-content/50 tracking-wider uppercase"} title)
        (if loading?
          ($ :div {:class "h-8 w-32 bg-base-200 rounded animate-pulse mt-2"})
          ($ :p {:class "text-3xl font-extrabold mt-1 tracking-tight text-base-content bg-gradient-to-br from-base-content to-base-content/70 bg-clip-text"} value)))
      (when icon
        ($ :div {:class "p-3 bg-primary/5 rounded-2xl text-primary ring-1 ring-primary/10 shadow-sm"}
          ($ :span {:class "text-2xl"} icon))))
    (when subtitle
      ($ :div {:class "mt-4 pt-4 border-t border-base-100/60"}
        ($ :p {:class "text-xs font-medium text-base-content/50 flex items-center gap-1.5"}
          ($ :span {:class "w-1.5 h-1.5 rounded-full bg-primary/60 shadow-[0_0_8px_rgba(var(--p)/0.4)]"})
          subtitle)))))

(defui section-shell [{:keys [title subtitle loading? error children header-actions]}]
  ($ :section {:class "bg-white rounded-2xl shadow-sm border border-base-200/80 overflow-hidden flex flex-col h-full transition-all hover:border-base-300"}
    ($ :div {:class "px-6 py-4 border-b border-base-100 flex items-center justify-between bg-gradient-to-r from-base-50/80 via-base-50/40 to-white"}
      ($ :div
        ($ :h3 {:class "font-bold text-lg text-base-content/90 flex items-center gap-2"}
          ($ :span {:class "w-1 h-5 bg-primary/40 rounded-full"})
          title)
        (when subtitle
          ($ :p {:class "text-xs text-base-content/50 mt-0.5 font-medium pl-3"} subtitle)))
      ($ :div {:class "flex items-center gap-3"}
        header-actions
        (when loading?
          ($ :span {:class "ds-loading ds-loading-spinner ds-loading-sm text-primary"}))))
    ($ :div {:class "p-6 flex-1"}
      (if (seq error)
        ($ :div {:class "ds-alert ds-alert-error text-sm rounded-xl shadow-sm"}
          ($ :span (str error)))
        ($ :div {:class "space-y-4 h-full"}
          children)))))

(defui expense-reports-page []
  (let [summary (or (use-subscribe [:user-expenses/summary]) {})
        summary-loading? (boolean (use-subscribe [:user-expenses/summary-loading?]))
        by-month (or (use-subscribe [:user-expenses/by-month]) [])
        by-supplier (or (use-subscribe [:user-expenses/by-supplier]) [])
        categories-data (or (use-subscribe [:user-expenses/categories]) [])
        subcategories-data (or (use-subscribe [:user-expenses/subcategories]) [])
        expense-categories-data (or (use-subscribe [:user-expenses/expense-categories]) [])
        manufacturers-data (or (use-subscribe [:user-expenses/manufacturers]) [])
        template-categories (or (use-subscribe [:app.template.frontend.subs.entity/entities :categories]) [])
        template-subcategories (or (use-subscribe [:app.template.frontend.subs.entity/entities :subcategories]) [])
        template-expense-categories (or (use-subscribe [:app.template.frontend.subs.entity/entities :expense-categories]) [])
        template-manufacturers (or (use-subscribe [:app.template.frontend.subs.entity/entities :manufacturers]) [])

        reports-filters (or (use-subscribe [:user-expenses/reports-filters]) {})
        months-back (or (:months-back reports-filters) 6)
        selected-supplier-id (:supplier-id reports-filters)
        selected-category-id (:category-id reports-filters)
        selected-subcategory-id (:subcategory-id reports-filters)
        selected-expense-category-id (:expense-category-id reports-filters)
        selected-manufacturer-id (:manufacturer-id reports-filters)
        selected-day-of-week (:day-of-week reports-filters)
        selected-category-key (:category-key reports-filters)
        selected-bucket-key (:amount-bucket reports-filters)
        selected-day (:selected-day reports-filters)
        month-a (:month-a reports-filters)
        month-b (:month-b reports-filters)
        show-uncategorized? (not= false (:show-uncategorized? reports-filters))

        [trend-sort set-trend-sort!] (use-state {:column :total_amount
                                                 :direction :desc
                                                 :type :number})
        [alias-sort set-alias-sort!] (use-state {:column :total_amount
                                                 :direction :desc
                                                 :type :number})
        [top-items-sort set-top-items-sort!] (use-state {:column :total_amount
                                                         :direction :desc
                                                         :type :number})
        [monthly-currency-sort set-monthly-currency-sort!] (use-state {:column :delta_amount
                                                                       :direction :desc
                                                                       :type :number})

        supplier-deep-dive (use-subscribe [:user-expenses/report-supplier-deep-dive])
        supplier-deep-dive-loading? (boolean (use-subscribe [:user-expenses/report-supplier-deep-dive-loading?]))
        supplier-deep-dive-error (use-subscribe [:user-expenses/report-supplier-deep-dive-error])

        day-of-week-data (or (use-subscribe [:user-expenses/report-day-of-week]) [])
        day-of-week-loading? (boolean (use-subscribe [:user-expenses/report-day-of-week-loading?]))
        day-of-week-error (use-subscribe [:user-expenses/report-day-of-week-error])

        top-items-data (or (use-subscribe [:user-expenses/report-top-items]) [])
        top-items-loading? (boolean (use-subscribe [:user-expenses/report-top-items-loading?]))
        top-items-error (use-subscribe [:user-expenses/report-top-items-error])

        monthly-comparison (or (use-subscribe [:user-expenses/report-monthly-comparison]) {})
        monthly-comparison-loading? (boolean (use-subscribe [:user-expenses/report-monthly-comparison-loading?]))
        monthly-comparison-error (use-subscribe [:user-expenses/report-monthly-comparison-error])

        size-distribution-data (or (use-subscribe [:user-expenses/report-size-distribution]) [])
        size-distribution-loading? (boolean (use-subscribe [:user-expenses/report-size-distribution-loading?]))
        size-distribution-error (use-subscribe [:user-expenses/report-size-distribution-error])

        daily-heatmap-data (or (use-subscribe [:user-expenses/report-daily-heatmap]) [])
        daily-heatmap-loading? (boolean (use-subscribe [:user-expenses/report-daily-heatmap-loading?]))
        daily-heatmap-error (use-subscribe [:user-expenses/report-daily-heatmap-error])

        category-allocation-data (or (use-subscribe [:user-expenses/report-category-allocation]) [])
        category-allocation-loading? (boolean (use-subscribe [:user-expenses/report-category-allocation-loading?]))
        category-allocation-error (use-subscribe [:user-expenses/report-category-allocation-error])

        suppliers* (supplier-options by-supplier)
        categories* (->> (if (seq categories-data) categories-data template-categories)
                      (keep (fn [row]
                              (let [id (some-> (:id row) str)
                                    name* (some-> (:name row) str str/trim)]
                                (when (and (seq id) (seq name*))
                                  {:id id :name name*}))))
                      (sort-by (comp str/lower-case :name))
                      vec)
        subcategories* (->> (if (seq subcategories-data) subcategories-data template-subcategories)
                         (keep (fn [row]
                                 (let [id (some-> (:id row) str)
                                       name* (some-> (:name row) str str/trim)]
                                   (when (and (seq id) (seq name*))
                                     {:id id :name name*}))))
                         (sort-by (comp str/lower-case :name))
                         vec)
        expense-categories* (->> (if (seq expense-categories-data) expense-categories-data template-expense-categories)
                              (keep (fn [row]
                                      (let [id (some-> (:id row) str)
                                            name* (some-> (:name row) str str/trim)]
                                        (when (and (seq id) (seq name*))
                                          {:id id :name name*}))))
                              (sort-by (comp str/lower-case :name))
                              vec)
        manufacturers* (->> (if (seq manufacturers-data) manufacturers-data template-manufacturers)
                         (keep (fn [row]
                                 (let [id (some-> (:id row) str)
                                       name* (some-> (or (:display_name row)
                                                       (:display-name row)
                                                       (:name row))
                                               str
                                               str/trim)]
                                   (when (and (seq id) (seq name*))
                                     {:id id :name name*}))))
                         (sort-by (comp str/lower-case :name))
                         vec)
        supplier-name-by-id (into {} (map (juxt :id :name) suppliers*))
        selected-supplier-name (or (get supplier-name-by-id (some-> selected-supplier-id str))
                                 (:supplier-name supplier-deep-dive)
                                 "All suppliers")

        month-options* (month-options by-month month-a month-b)

        day-pattern (aggregate-day-pattern day-of-week-data)
        day-pattern-max (apply max (cons 0 (map #(or (->number (:total_amount %)) 0) day-pattern)))

        top-items-filtered (filter-top-items-by-bucket top-items-data selected-bucket-key)
        top-items-visible (vec (take 20 (sort-rows-by-config top-items-filtered top-items-sort)))

        monthly-by-currency-rows (or (:by_currency monthly-comparison) [])
        monthly-by-currency (sort-rows-by-config monthly-by-currency-rows monthly-currency-sort)
        monthly-by-supplier (or (:by_supplier monthly-comparison) [])

        size-buckets (aggregate-size-buckets size-distribution-data)
        size-buckets-max (apply max (cons 0 (map #(or (->number (:total_amount %)) 0) size-buckets)))

        heatmap-rows-raw (aggregate-heatmap daily-heatmap-data)
        heatmap-rows (if selected-day-of-week
                       (->> heatmap-rows-raw
                         (filter #(= selected-day-of-week (:iso_day_of_week %)))
                         vec)
                       heatmap-rows-raw)
        heatmap-visible (->> heatmap-rows (take-last 84) vec)
        heatmap-max (apply max (cons 0 (map #(or (->number (:total_amount %)) 0) heatmap-visible)))
        selected-heatmap-day (first (filter #(= selected-day (:day %)) heatmap-visible))

        category-rows-raw (aggregate-category-allocation category-allocation-data)
        category-rows (->> category-rows-raw
                        (filter (fn [row]
                                  (if show-uncategorized?
                                    true
                                    (not= "uncategorized" (:category_key row)))))
                        vec)
        category-max (apply max (cons 0 (map #(or (->number (:total_amount %)) 0) category-rows)))

        deep-dive-summary (or (:summary supplier-deep-dive) [])
        deep-dive-trend-rows (or (:trend supplier-deep-dive) [])
        deep-dive-trend (sort-rows-by-config deep-dive-trend-rows trend-sort)
        deep-dive-top-aliases-rows (or (:top-aliases supplier-deep-dive) [])
        deep-dive-top-aliases (sort-rows-by-config deep-dive-top-aliases-rows alias-sort)

        currency-totals (or (:currency-totals summary) {})
        primary-currency-key (or
                               (some #(when (contains? currency-totals %) %) [:BAM :USD :EUR "BAM" "USD" "EUR"])
                               (first (keys currency-totals)))
        primary-currency (if (keyword? primary-currency-key)
                           (name primary-currency-key)
                           (some-> primary-currency-key str))
        total-sum (reduce + 0 (map #(or (->number %) 0) (vals currency-totals)))
        total-expenses (or (->number (:total-expenses summary)) 0)
        average-spend (when (pos? total-expenses) (/ total-sum total-expenses))]

    ($ :div {:class "min-h-screen bg-base-100 pb-12"}
      ($ :header {:class "bg-white/80 backdrop-blur-xl border-b border-base-200 sticky top-0 z-30 shadow-sm transition-all"}
        ($ :div {:class "max-w-7xl mx-auto px-4 sm:px-6 py-4"}
          ($ :div {:class "flex flex-col xl:flex-row xl:items-start xl:justify-between gap-6"}
            ($ :div {:class "flex-shrink-0 space-y-1"}
              ($ :div {:class "flex items-center gap-2 mb-1"}
                ($ :span {:class "w-2 h-2 rounded-full bg-primary animate-pulse"})
                ($ :span {:class "text-xs font-bold text-primary uppercase tracking-widest"} "Analytics Dashboard"))
              ($ :h1 {:class "text-2xl sm:text-3xl font-black text-base-content tracking-tight"} "Expense Reports")
              ($ :p {:class "text-sm text-base-content/60 max-w-md font-medium"}
                "Start by selecting a time range and filters below to analyze spending patterns."))

            ($ :div {:class "w-full rounded-2xl border border-base-200 bg-white/50 p-4 sm:p-6 space-y-4 shadow-sm backdrop-blur-sm"}
              ($ :div {:class "flex items-center justify-between mb-3"}
                ($ :div {:class "flex items-center gap-2"}
                  ($ :span {:class "text-xs font-extrabold uppercase text-base-content/40 tracking-wider"} "Global Filters")
                  (when (or selected-supplier-id selected-category-id selected-subcategory-id
                          selected-expense-category-id selected-manufacturer-id)
                    ($ :span {:class "ds-badge bg-primary/10 text-primary border-primary/20 font-bold ds-badge-xs uppercase tracking-wide"} "Active")))

                ($ :div {:class "flex items-center gap-2"}
                  ($ button {:id "btn-reports-refresh"
                             :btn-type :ghost
                             :size :sm
                             :class "text-base-content/60 hover:text-primary hover:bg-primary/5 font-medium transition-colors"
                             :on-click #(rf/dispatch [:user-expenses/reports-refresh])}
                    "Refresh Data")

                  (when (or selected-supplier-id selected-category-id selected-subcategory-id
                          selected-expense-category-id selected-manufacturer-id)
                    ($ button {:id "btn-reports-clear-local-filters"
                               :btn-type :ghost
                               :size :sm
                               :class "text-error/70 hover:text-error hover:bg-error/10 font-medium transition-colors"
                               :on-click #(rf/dispatch [:user-expenses/reports-clear-local-filters])}
                      "Clear All filters"))))

              ($ :div {:class "grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-3"}
                ;; Range - Prominent first
                ($ :div {:class "space-y-1.5 col-span-1 sm:col-span-2 lg:col-span-1 xl:col-span-1"}
                  ($ :label {:class "text-[10px] font-bold text-base-content/50 uppercase tracking-widest px-0.5" :for "reports-filter-months-back"} "Time Range")
                  ($ :select {:id "reports-filter-months-back"
                              :class "ds-select ds-select-sm ds-select-bordered w-full bg-white font-medium focus:border-primary focus:ring-2 focus:ring-primary/20 transition-all rounded-lg text-xs shadow-sm"
                              :value months-back
                              :on-change #(rf/dispatch [:user-expenses/reports-set-filter
                                                        :months-back
                                                        (js/parseInt (.. % -target -value) 10)])}
                    ($ :option {:value 3} "Last 3 months")
                    ($ :option {:value 6} "Last 6 months")
                    ($ :option {:value 12} "Last 12 months")
                    ($ :option {:value 24} "Last 24 months")))

                ($ :div {:class "space-y-1.5"}
                  ($ :label {:class "text-[10px] font-bold text-base-content/50 uppercase tracking-widest px-0.5" :for "reports-filter-supplier"} "Supplier")
                  ($ :select {:id "reports-filter-supplier"
                              :class "ds-select ds-select-sm ds-select-bordered w-full bg-white text-xs rounded-lg focus:ring-2 focus:ring-primary/20"
                              :value (or selected-supplier-id "")
                              :on-change #(rf/dispatch [:user-expenses/reports-set-filter
                                                        :supplier-id
                                                        (let [v (.. % -target -value)]
                                                          (when (seq (str/trim v)) v))])}
                    ($ :option {:value ""} "All suppliers")
                    (mapv (fn [{:keys [id name]}]
                            ($ :option {:key id :value id} name))
                      suppliers*)))

                ($ :div {:class "space-y-1.5"}
                  ($ :label {:class "text-[10px] font-bold text-base-content/50 uppercase tracking-widest px-0.5" :for "reports-filter-category"} "Category")
                  ($ :select {:id "reports-filter-category"
                              :class "ds-select ds-select-sm ds-select-bordered w-full bg-white text-xs rounded-lg focus:ring-2 focus:ring-primary/20"
                              :value (or selected-category-id "")
                              :on-change #(rf/dispatch [:user-expenses/reports-set-filter
                                                        :category-id
                                                        (let [v (.. % -target -value)]
                                                          (when (seq (str/trim v)) v))])}
                    ($ :option {:value ""} "All categories")
                    (mapv (fn [{:keys [id name]}]
                            ($ :option {:key id :value id} name))
                      categories*)))

                ($ :div {:class "space-y-1.5"}
                  ($ :label {:class "text-[10px] font-bold text-base-content/50 uppercase tracking-widest px-0.5" :for "reports-filter-subcategory"} "Subcategory")
                  ($ :select {:id "reports-filter-subcategory"
                              :class "ds-select ds-select-sm ds-select-bordered w-full bg-white text-xs rounded-lg focus:ring-2 focus:ring-primary/20"
                              :value (or selected-subcategory-id "")
                              :on-change #(rf/dispatch [:user-expenses/reports-set-filter
                                                        :subcategory-id
                                                        (let [v (.. % -target -value)]
                                                          (when (seq (str/trim v)) v))])}
                    ($ :option {:value ""} "All subcategories")
                    (mapv (fn [{:keys [id name]}]
                            ($ :option {:key id :value id} name))
                      subcategories*)))

                ($ :div {:class "space-y-1.5"}
                  ($ :label {:class "text-[10px] font-bold text-base-content/50 uppercase tracking-widest px-0.5" :for "reports-filter-expense-category"} "Expense Type")
                  ($ :select {:id "reports-filter-expense-category"
                              :class "ds-select ds-select-sm ds-select-bordered w-full bg-white text-xs rounded-lg focus:ring-2 focus:ring-primary/20"
                              :value (or selected-expense-category-id "")
                              :on-change #(rf/dispatch [:user-expenses/reports-set-filter
                                                        :expense-category-id
                                                        (let [v (.. % -target -value)]
                                                          (when (seq (str/trim v)) v))])}
                    ($ :option {:value ""} "All expense types")
                    (mapv (fn [{:keys [id name]}]
                            ($ :option {:key id :value id} name))
                      expense-categories*)))

                ($ :div {:class "space-y-1.5"}
                  ($ :label {:class "text-[10px] font-bold text-base-content/50 uppercase tracking-widest px-0.5" :for "reports-filter-manufacturer"} "Manufacturer")
                  ($ :select {:id "reports-filter-manufacturer"
                              :class "ds-select ds-select-sm ds-select-bordered w-full bg-white text-xs rounded-lg focus:ring-2 focus:ring-primary/20"
                              :value (or selected-manufacturer-id "")
                              :on-change #(rf/dispatch [:user-expenses/reports-set-filter
                                                        :manufacturer-id
                                                        (let [v (.. % -target -value)]
                                                          (when (seq (str/trim v)) v))])}
                    ($ :option {:value ""} "All manufacturers")
                    (mapv (fn [{:keys [id name]}]
                            ($ :option {:key id :value id} name))
                      manufacturers*))))))))

      ($ :main {:class "max-w-7xl mx-auto px-4 sm:px-6 py-8 space-y-8"}
        ($ :div {:class "grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6"}
          ($ stat-card {:title "Total Spent"
                        :value (format-money total-sum primary-currency)
                        :subtitle "In selected period"
                        :icon "💰"
                        :loading? summary-loading?})
          ($ stat-card {:title "Transaction Volume"
                        :value (format-int total-expenses)
                        :subtitle "Recorded expenses"
                        :icon "📋"
                        :loading? summary-loading?})
          ($ stat-card {:title "Average Expense"
                        :value (if average-spend (format-money average-spend primary-currency) "—")
                        :subtitle "Per transaction"
                        :icon "📊"
                        :loading? summary-loading?})
          ($ stat-card {:title "Active Scope"
                        :value selected-supplier-name
                        :subtitle "Supplier filter context"
                        :icon "🏪"
                        :loading? false}))

        ($ :div {:class "grid grid-cols-1 xl:grid-cols-2 gap-8"}
          ;; 1) Supplier deep-dive
          ($ section-shell {:title "Supplier Deep Dive"
                            :subtitle "Detailed breakdown for the selected supplier scope"
                            :loading? supplier-deep-dive-loading?
                            :error supplier-deep-dive-error
                            :header-actions (when (str/blank? (str (or selected-supplier-id "")))
                                              ($ :span {:class "ds-badge ds-badge-warning ds-badge-sm"} "Select a supplier"))}
            (if (str/blank? (str (or selected-supplier-id "")))
              ($ :div {:class "flex flex-col items-center justify-center h-64 text-center p-8 bg-base-50 rounded-xl border border-dashed border-base-300"}
                ($ :div {:class "text-4xl mb-4"} "🏪")
                ($ :h4 {:class "font-bold text-lg mb-2"} "Select a supplier first")
                ($ :p {:class "text-base-content/60 max-w-sm"}
                  "Choose a specific supplier from the filters above to see detailed breakdown, trends, and alias patterns."))

              ($ :div {:class "space-y-6"}
                (if (seq deep-dive-summary)
                  ($ :div {:class "grid grid-cols-1 sm:grid-cols-2 gap-4"}
                    (mapv
                      (fn [row]
                        (let [currency (:currency row)
                              total-amount (:total_amount row)
                              expense-count (:expense_count row)]
                          ($ :div {:key (str "supplier-summary-" currency)
                                   :class "rounded-xl bg-base-50 p-4 border border-base-200"}
                            ($ :div {:class "flex justify-between items-start mb-2"}
                              ($ :span {:class "ds-badge ds-badge-sm ds-badge-outline font-mono"} currency)
                              ($ :span {:class "text-xs font-medium text-base-content/50 uppercase"} "Total"))
                            ($ :p {:class "text-2xl font-bold tracking-tight"} (format-amount-only total-amount))
                            ($ :p {:class "text-xs text-base-content/60 mt-1"}
                              (str (format-int expense-count) " transactions")))))
                      deep-dive-summary))
                  ($ :div {:class "ds-alert ds-alert-info"}
                    ($ :span "No summary data available for this supplier.")))

                ($ :div {:class "space-y-3"}
                  ($ :h4 {:class "text-sm font-bold uppercase tracking-wider text-base-content/70 pl-1"} "Monthly Trend")
                  (if (seq deep-dive-trend)
                    ($ :div {:class "overflow-x-auto rounded-lg border border-base-200/60 shadow-sm"}
                      ($ :table {:class "ds-table ds-table-sm ds-table-zebra w-full"}
                        ($ :thead {:class "bg-base-200/40 text-base-content/60 border-b border-base-200"}
                          ($ :tr
                            ($ :th
                              ($ :button {:type "button"
                                          :class "font-bold hover:text-primary transition flex items-center gap-1"
                                          :on-click #(set-trend-sort! (fn [current]
                                                                        (toggle-sort-config current :month :text)))}
                                (sortable-column-label "Month" trend-sort :month)))
                            ($ :th
                              ($ :button {:type "button"
                                          :class "font-bold hover:text-primary transition flex items-center gap-1"
                                          :on-click #(set-trend-sort! (fn [current]
                                                                        (toggle-sort-config current :currency :text)))}
                                (sortable-column-label "Currency" trend-sort :currency)))
                            ($ :th {:class "text-right"}
                              ($ :button {:type "button"
                                          :class "font-bold hover:text-primary transition inline-flex items-center justify-end gap-1 w-full"
                                          :on-click #(set-trend-sort! (fn [current]
                                                                        (toggle-sort-config current :total_amount :number)))}
                                (sortable-column-label "Total" trend-sort :total_amount)))
                            ($ :th {:class "text-right"}
                              ($ :button {:type "button"
                                          :class "font-bold hover:text-primary transition inline-flex items-center justify-end gap-1 w-full"
                                          :on-click #(set-trend-sort! (fn [current]
                                                                        (toggle-sort-config current :expense_count :number)))}
                                (sortable-column-label "Count" trend-sort :expense_count)))))
                        ($ :tbody
                          (mapv
                            (fn [row]
                              ($ :tr {:key (str "trend-" (:month row) "-" (:currency row)) :class "hover"}
                                ($ :td {:class "font-medium"} (month-label (:month row)))
                                ($ :td {:class "text-xs text-base-content/60"} (str (:currency row)))
                                ($ :td {:class "text-right font-mono font-medium"}
                                  (format-amount-only (:total_amount row)))
                                ($ :td {:class "text-right text-base-content/70"}
                                  (format-int (:expense_count row)))))
                            deep-dive-trend))))
                    ($ :p {:class "text-sm text-base-content/60 italic p-4 bg-base-50 rounded-lg text-center"}
                      "No trend data found.")))

                ($ :div {:class "space-y-3"}
                  ($ :h4 {:class "text-sm font-bold uppercase tracking-wider text-base-content/70 pl-1"} "Active Articles")
                  (if (seq deep-dive-top-aliases)
                    ($ :div {:class "overflow-x-auto rounded-lg border border-base-200/60 shadow-sm max-h-96"}
                      ($ :table {:class "ds-table ds-table-sm ds-table-pin-rows w-full"}
                        ($ :thead {:class "bg-base-200/40 text-base-content/60 border-b border-base-200"}
                          ($ :tr
                            ($ :th
                              ($ :button {:type "button"
                                          :class "font-bold hover:text-primary transition text-left"
                                          :on-click #(set-alias-sort! (fn [current]
                                                                        (toggle-sort-config current :article_canonical_name :text)))}
                                (sortable-column-label "Article" alias-sort :article_canonical_name)))
                            ($ :th
                              ($ :button {:type "button"
                                          :class "font-bold hover:text-primary transition text-left"
                                          :on-click #(set-alias-sort! (fn [current]
                                                                        (toggle-sort-config current :currency :text)))}
                                (sortable-column-label "Curr" alias-sort :currency)))
                            ($ :th {:class "text-right"}
                              ($ :button {:type "button"
                                          :class "font-bold hover:text-primary transition w-full text-right"
                                          :on-click #(set-alias-sort! (fn [current]
                                                                        (toggle-sort-config current :total_amount :number)))}
                                (sortable-column-label "Total" alias-sort :total_amount)))
                            ($ :th {:class "text-right"}
                              ($ :button {:type "button"
                                          :class "font-bold hover:text-primary transition w-full text-right"
                                          :on-click #(set-alias-sort! (fn [current]
                                                                        (toggle-sort-config current :line_count :number)))}
                                (sortable-column-label "Receipts" alias-sort :line_count)))))
                        ($ :tbody
                          (mapv
                            (fn [row]
                              ($ :tr {:key (str "alias-" (or (:alias_id row) (:alias_label row)) "-" (:currency row)) :class "hover group"}
                                ($ :td {:class "text-xs text-base-content group-hover:text-primary transition-colors"}
                                  (or (:article_canonical_name row) "—"))
                                ($ :td {:class "text-xs text-base-content/60"} (str (:currency row)))
                                ($ :td {:class "text-right font-mono text-sm"}
                                  (format-amount-only (:total_amount row)))
                                ($ :td {:class "text-right text-xs text-base-content/60"}
                                  (format-int (:line_count row)))))
                            deep-dive-top-aliases))))
                    ($ :p {:class "text-sm text-base-content/60 italic p-4 bg-base-50 rounded-lg text-center"}
                      "No alias details found."))))))

          ;; 2) Day-of-week pattern
          ($ section-shell {:title "Spending by Day"
                            :subtitle "Weekly distribution analysis"
                            :loading? day-of-week-loading?
                            :error day-of-week-error}
            (if (seq day-pattern)
              ($ :div {:class "space-y-4"}
                ($ :p {:class "text-xs text-base-content/60 bg-base-50 p-3 rounded-lg flex items-center gap-2"}
                  ($ :span "ℹ️")
                  "Click any row to filter the heatmap (Widget #6) by that day of the week.")

                ($ :div {:class "space-y-3"}
                  (mapv
                    (fn [row]
                      (let [iso-day (:iso_day_of_week row)
                            selected? (= selected-day-of-week iso-day)
                            total (or (->number (:total_amount row)) 0)
                            ratio (if (pos? day-pattern-max) (/ total day-pattern-max) 0)]
                        ($ :button {:id (str "btn-day-filter-" (:day_key row))
                                    :key (str "dow-" iso-day)
                                    :class (str "w-full text-left group transition-all duration-200")
                                    :on-click #(rf/dispatch [:user-expenses/reports-toggle-day-of-week iso-day])}
                          ($ :div {:class "flex items-end justify-between gap-2 mb-1"}
                            ($ :div {:class "flex items-baseline gap-2"}
                              ($ :span {:class (str "font-bold text-sm " (if selected? "text-primary" "text-base-content/80"))} (:day_label row))
                              ($ :span {:class "text-xs text-base-content/50"}
                                (str (format-int (:expense_count row)) " txs")))
                            ($ :span {:class "font-mono text-sm font-medium"}
                              (format-money total primary-currency)))

                          ($ :div {:class (str "h-3 rounded-full overflow-hidden "
                                            (if selected? "bg-primary/10 ring-2 ring-primary ring-offset-1" "bg-base-100"))}
                            ($ :div {:class (str "h-full rounded-full transition-all duration-500 "
                                              (if selected? "bg-primary" "bg-base-content/20 group-hover:bg-primary/60"))
                                     :style {:width (str (* 100 ratio) "%")}})))))
                    day-pattern)))
              ($ :div {:class "flex flex-col items-center justify-center h-48 text-center p-8 bg-base-50 rounded-xl border border-dashed border-base-300"}
                ($ :p {:class "text-base-content/60"} "No weekly pattern data available.")))))

        ($ :div {:class "grid grid-cols-1 xl:grid-cols-2 gap-8"}
          ;; 3) Top items table/report
          ($ section-shell {:title "Top Global Items"
                            :subtitle "Highest spending by article across all suppliers"
                            :loading? top-items-loading?
                            :error top-items-error}
            ($ :div {:class "space-y-4"}
              (if (seq selected-bucket-key)
                ($ :div {:class "bg-primary/5 border border-primary/20 rounded-lg p-3 flex items-center justify-between text-sm"}
                  ($ :div {:class "flex items-center gap-2"}
                    ($ :span "🔍")
                    ($ :span "Filtered by size bucket: "
                      ($ :span {:class "font-bold text-primary"} selected-bucket-key)))
                  ($ button {:btn-type :ghost :size :xs :class "text-primary"
                             :on-click #(rf/dispatch [:user-expenses/reports-toggle-amount-bucket nil])}
                    "Clear"))
                ($ :p {:class "text-xs text-base-content/50 px-1"}
                  "Tip: Use the 'Size Distribution' widget below to filter this list by expense amount."))

              (if (seq top-items-visible)
                ($ :div {:class "overflow-x-auto rounded-lg border border-base-200/60 shadow-sm"}
                  ($ :table {:id "table-top-items"
                             :class "ds-table ds-table-sm ds-table-zebra w-full"}
                    ($ :thead {:class "bg-base-200/40 text-base-content/60 border-b border-base-200"}
                      ($ :tr
                        ($ :th
                          ($ :button {:type "button"
                                      :class "font-bold hover:text-primary transition flex items-center gap-1"
                                      :on-click #(set-top-items-sort! (fn [current]
                                                                        (toggle-sort-config current :article_canonical_name :text)))}
                            (sortable-column-label "Article" top-items-sort :article_canonical_name)))
                        ($ :th
                          ($ :button {:type "button"
                                      :class "font-bold hover:text-primary transition flex items-center gap-1"
                                      :on-click #(set-top-items-sort! (fn [current]
                                                                        (toggle-sort-config current :currency :text)))}
                            (sortable-column-label "Curr" top-items-sort :currency)))
                        ($ :th {:class "text-right"}
                          ($ :button {:type "button"
                                      :class "font-bold hover:text-primary transition inline-flex items-center justify-end gap-1 w-full"
                                      :on-click #(set-top-items-sort! (fn [current]
                                                                        (toggle-sort-config current :total_amount :number)))}
                            (sortable-column-label "Total" top-items-sort :total_amount)))
                        ($ :th {:class "text-right hidden sm:table-cell"}
                          ($ :button {:type "button"
                                      :class "font-bold hover:text-primary transition inline-flex items-center justify-end gap-1 w-full"
                                      :on-click #(set-top-items-sort! (fn [current]
                                                                        (toggle-sort-config current :qty_total :number)))}
                            (sortable-column-label "Qty" top-items-sort :qty_total)))
                        ($ :th {:class "text-right"}
                          ($ :button {:type "button"
                                      :class "font-bold hover:text-primary transition inline-flex items-center justify-end gap-1 w-full"
                                      :on-click #(set-top-items-sort! (fn [current]
                                                                        (toggle-sort-config current :line_count :number)))}
                            (sortable-column-label "Receipts" top-items-sort :line_count)))))
                    ($ :tbody
                      (mapv
                        (fn [row]
                          ($ :tr {:key (str "top-item-" (or (:alias_id row) (:alias_label row)) "-" (:currency row)) :class "hover"}
                            ($ :td {:class "font-medium"}
                              (or (some-> (:article_canonical_name row) str str/trim not-empty)
                                "—"))
                            ($ :td {:class "text-xs text-base-content/60"} (str (:currency row)))
                            ($ :td {:class "text-right font-mono font-medium"}
                              (format-amount-only (:total_amount row)))
                            ($ :td {:class "text-right hidden sm:table-cell text-base-content/70"}
                              (format-int (:qty_total row)))
                            ($ :td {:class "text-right text-base-content/60"}
                              (format-int (:line_count row)))))
                        top-items-visible))))
                ($ :div {:class "flex flex-col items-center justify-center p-8 bg-base-50 rounded-xl text-center"}
                  ($ :p {:class "text-base-content/60"} "No items found matching the current filters.")))))

          ;; 4) Monthly comparison selector + deltas
          ($ section-shell {:title "Monthly Comparison"
                            :subtitle "Compare spending between two specific months"
                            :loading? monthly-comparison-loading?
                            :error monthly-comparison-error}
            ($ :div {:class "space-y-6"}
              ($ :div {:class "bg-base-50 p-4 rounded-xl border border-base-200 flex flex-wrap gap-4 items-end"}
                ($ :div {:class "flex-1 min-w-[200px]"}
                  ($ :label {:class "text-xs font-bold uppercase text-base-content/50 mb-1.5 block"
                             :for "reports-month-a-select"}
                    "Base Month (A)")
                  ($ :select {:id "reports-month-a-select"
                              :class "ds-select ds-select-sm ds-select-bordered w-full bg-white"
                              :value (or month-a "")
                              :on-change #(rf/dispatch [:user-expenses/reports-set-filter
                                                        :month-a
                                                        (.. % -target -value)])}
                    (mapv
                      (fn [month]
                        ($ :option {:key (str "month-a-" month) :value month}
                          (month-label month)))
                      month-options*)))

                ($ :div {:class "flex items-center justify-center pb-1 text-base-content/30"}
                  ($ :span {:class "text-xl"} "VS"))

                ($ :div {:class "flex-1 min-w-[200px]"}
                  ($ :label {:class "text-xs font-bold uppercase text-base-content/50 mb-1.5 block"
                             :for "reports-month-b-select"}
                    "Comparison Month (B)")
                  ($ :select {:id "reports-month-b-select"
                              :class "ds-select ds-select-sm ds-select-bordered w-full bg-white"
                              :value (or month-b "")
                              :on-change #(rf/dispatch [:user-expenses/reports-set-filter
                                                        :month-b
                                                        (.. % -target -value)])}
                    (mapv
                      (fn [month]
                        ($ :option {:key (str "month-b-" month) :value month}
                          (month-label month)))
                      month-options*)))))

            (if (seq monthly-by-currency)
              ($ :div {:class "overflow-x-auto rounded-lg border border-base-200/60 shadow-sm"}
                ($ :table {:class "ds-table ds-table-sm w-full"}
                  ($ :thead {:class "bg-base-200/40 text-base-content/60 border-b border-base-200"}
                    ($ :tr
                      ($ :th {:class "whitespace-nowrap font-bold"} "Currency")
                      ($ :th {:class "text-right font-bold whitespace-nowrap"} "A")
                      ($ :th {:class "text-right font-bold whitespace-nowrap"} "B")
                      ($ :th {:class "text-right font-bold whitespace-nowrap"} "Cash")
                      ($ :th {:class "text-right font-bold whitespace-nowrap"} "%")
                      ($ :th {:class "text-right font-bold whitespace-nowrap"} "Count")))
                  ($ :tbody
                    (mapv
                      (fn [row]
                        (let [delta (or (->number (:delta_amount row)) 0)
                              positive? (>= delta 0)]
                          ($ :tr {:key (str "monthly-currency-" (:currency row))}
                            ($ :td {:class "text-xs text-base-content/60"} (str (:currency row)))
                            ($ :td {:class "text-right font-mono text-base-content/70"}
                              (format-amount-only (:month_a_total row)))
                            ($ :td {:class "text-right font-mono text-base-content/70"}
                              (format-amount-only (:month_b_total row)))
                            ($ :td {:class (str "text-right font-mono font-bold " (if positive? "text-error" "text-success"))}
                              (str (if positive? "+" "") (format-amount-only delta)))
                            ($ :td {:class (str "text-right font-medium " (if positive? "text-error" "text-success"))}
                              (format-percent (:delta_percent row)))
                            ($ :td {:class "text-right text-xs"}
                              (format-int (:delta_count row))))))
                      monthly-by-currency))))
              ($ :p {:class "text-center p-4 bg-base-50 rounded-lg text-base-content/60 text-sm"}
                "Select two different months to compare."))

            (when (seq monthly-by-supplier)
              ($ :div {:class "mt-6 pt-6 border-t border-base-100"}
                ($ :h4 {:class "text-sm font-bold uppercase tracking-wider text-base-content/70 mb-3 pl-1"} "Largest Changes by Vendor")
                ($ :div {:class "grid grid-cols-1 sm:grid-cols-2 gap-2"}
                  (mapv
                    (fn [row]
                      (let [delta (or (->number (:delta_amount row)) 0)
                            positive? (>= delta 0)]
                        ($ :div {:key (str "supplier-delta-" (or (:supplier_id row) (:supplier_name row)) "-" (:currency row))
                                 :class "flex items-center justify-between gap-3 p-3 bg-base-50 rounded-lg border border-base-100 hover:bg-base-100 transition shadow-sm"}
                          ($ :span {:class "text-sm font-medium truncate"} (or (:supplier_name row) "Unknown supplier"))
                          ($ :span {:class (str "font-mono font-bold text-sm " (if positive? "text-error" "text-success"))}
                            (str (if positive? "+" "") (format-money delta (:currency row)))))))
                    (take 6 monthly-by-supplier)))))))

        ($ :div {:class "grid grid-cols-1 xl:grid-cols-2 gap-8"}
          ;; 5) Size distribution chart
          ($ section-shell {:title "Expense Sizes"
                            :subtitle "Distribution by transaction amount"
                            :loading? size-distribution-loading?
                            :error size-distribution-error}
            ($ :div {:class "space-y-4"}
              ($ :p {:class "text-xs text-base-content/60 bg-base-50 p-3 rounded-lg flex items-center gap-2"}
                ($ :span "ℹ️")
                "Click any bucket to filter the Global Items list (Widget #3) by amount.")

              (if (seq size-buckets)
                ($ :div {:class "space-y-3"}
                  (mapv
                    (fn [row]
                      (let [bucket-key (:bucket_key row)
                            selected? (= selected-bucket-key bucket-key)
                            total (or (->number (:total_amount row)) 0)
                            ratio (if (pos? size-buckets-max) (/ total size-buckets-max) 0)]
                        ($ :button {:id (str "btn-size-bucket-" bucket-key)
                                    :key (str "bucket-" bucket-key)
                                    :class "w-full text-left group transition-all duration-200"
                                    :on-click #(rf/dispatch [:user-expenses/reports-toggle-amount-bucket bucket-key])}
                          ($ :div {:class "flex items-end justify-between gap-2 mb-1"}
                            ($ :div {:class "flex items-baseline gap-2"}
                              ($ :span {:class (str "font-bold text-sm " (if selected? "text-primary" "text-base-content/80"))} (:bucket_label row))
                              ($ :span {:class "text-xs text-base-content/50"}
                                (str (format-int (:expense_count row)) " txs")))
                            ($ :span {:class "font-mono text-sm font-medium"}
                              (format-money total primary-currency)))

                          ($ :div {:class (str "h-4 rounded-md overflow-hidden "
                                            (if selected? "bg-primary/10 ring-2 ring-primary ring-offset-1" "bg-base-100"))}
                            ($ :div {:class (str "h-full rounded-md transition-all duration-500 "
                                              (if selected? "bg-primary" "bg-base-content/20 group-hover:bg-primary/60"))
                                     :style {:width (str (* 100 ratio) "%")}})))))
                    size-buckets))
                ($ :p {:class "text-center p-8 text-base-content/60"}
                  "No size distribution data available."))))

          ;; 6) Daily heatmap
          ($ section-shell {:title "Calendar Heatmap"
                            :subtitle "Daily spending intensity"
                            :loading? daily-heatmap-loading?
                            :error daily-heatmap-error}
            ($ :div {:class "space-y-4"}
              (if (seq heatmap-visible)
                ($ :div {:class "space-y-4"}
                  ($ :div {:class "flex gap-2 flex-wrap text-xs text-base-content/50 mb-2"}
                    ($ :div {:class "flex items-center gap-1"}
                      ($ :div {:class "w-3 h-3 bg-base-200 rounded-sm"}) "None")
                    ($ :div {:class "flex items-center gap-1"}
                      ($ :div {:class "w-3 h-3 bg-primary/20 rounded-sm"}) "Low")
                    ($ :div {:class "flex items-center gap-1"}
                      ($ :div {:class "w-3 h-3 bg-primary/40 rounded-sm"}) "Medium")
                    ($ :div {:class "flex items-center gap-1"}
                      ($ :div {:class "w-3 h-3 bg-primary/70 rounded-sm"}) "High")
                    ($ :div {:class "flex items-center gap-1"}
                      ($ :div {:class "w-3 h-3 bg-primary rounded-sm"}) "Max"))

                  ($ :div {:class "grid grid-cols-7 gap-1.5"}
                    ;; Do headers for days of week? Maybe simplistic "Mon Tue..." etc.
                    ;; For now just the cells
                    (mapv
                      (fn [row]
                        (let [total (or (->number (:total_amount row)) 0)
                              ratio (if (pos? heatmap-max) (/ total heatmap-max) 0)
                              selected? (= selected-day (:day row))]
                          ($ :button {:id (str "btn-heatmap-day-" (:day row))
                                      :key (str "heat-" (:day row))
                                      :class (str "aspect-square rounded md:rounded-lg text-[10px] md:text-xs font-bold transition-all duration-200 relative group "
                                               (heat-intensity-class ratio)
                                               " "
                                               (if selected? "ring-2 ring-offset-2 ring-primary z-10 scale-110 shadow-lg" "hover:scale-105"))
                                      :title (str (:day row) " · " (format-money total primary-currency)
                                               " · " (format-int (:expense_count row)) " expenses")
                                      :on-click #(rf/dispatch [:user-expenses/reports-toggle-selected-day (:day row)])}
                            ($ :span {:class "opacity-70 group-hover:opacity-100"} (subs (:day row) 8 10)))))
                      heatmap-visible))

                  (when selected-heatmap-day
                    ($ :div {:class "mt-4 rounded-xl border border-primary/20 bg-primary/5 p-4 flex items-center justify-between shadow-sm animate-in fade-in slide-in-from-top-2 duration-200"}
                      ($ :div
                        ($ :p {:class "text-xs font-bold text-primary uppercase tracking-wider mb-0.5"} "Selected Day")
                        ($ :p {:class "text-lg font-bold"} (str (:day selected-heatmap-day)))
                        ($ :div {:class "text-sm text-base-content/70 mt-1 flex gap-3"}
                          ($ :span (str "Spent: " ($ :span {:class "font-mono font-bold text-base-content"} (format-money (:total_amount selected-heatmap-day) primary-currency))))
                          ($ :span (str "Tx count: " ($ :span {:class "font-mono font-bold text-base-content"} (format-int (:expense_count selected-heatmap-day)))))))
                      ($ button {:id "btn-heatmap-clear-selected-day"
                                 :btn-type :ghost
                                 :size :sm
                                 :class "text-base-content/60 hover:text-base-content"
                                 :on-click #(rf/dispatch [:user-expenses/reports-toggle-selected-day (:day selected-heatmap-day)])}
                        "Clear"))))
                ($ :div {:class "flex items-center justify-center p-8 bg-base-50 rounded-xl border border-dashed border-base-300"}
                  ($ :p {:class "text-base-content/60"} "No activity data in this range."))))))

        ($ :div {:class "grid grid-cols-1 mb-12"}
          ;; 7) Category allocation
          ($ section-shell {:title "Category Allocation"
                            :subtitle "Spending breakdown by category"
                            :loading? category-allocation-loading?
                            :error category-allocation-error
                            :header-actions ($ :label {:class "cursor-pointer ds-label p-0 gap-2 hover:opacity-80 transition"}
                                              ($ :span {:class "text-xs font-medium text-base-content/70"} "Show Uncategorized")
                                              ($ :input {:id "toggle-category-uncategorized-visibility"
                                                         :type "checkbox"
                                                         :class "ds-toggle ds-toggle-primary ds-toggle-xs"
                                                         :checked show-uncategorized?
                                                         :on-change #(rf/dispatch [:user-expenses/reports-set-filter
                                                                                   :show-uncategorized?
                                                                                   (.. % -target -checked)])}))}
            (if (seq category-rows)
              ($ :div {:class "space-y-3"}
                (when (seq selected-category-key)
                  ($ :div {:class "bg-primary/5 border border-primary/20 rounded-lg p-3 flex items-center justify-between text-sm mb-2"}
                    ($ :span (str "Filtering report by: " selected-category-key))
                    ($ button {:btn-type :ghost :size :xs :class "text-primary"
                               :on-click #(rf/dispatch [:user-expenses/reports-toggle-category nil])}
                      "Clear selection")))

                (mapv
                  (fn [row]
                    (let [category-key (:category_key row)
                          selected? (= selected-category-key category-key)
                          total (or (->number (:total_amount row)) 0)
                          ratio (if (pos? category-max) (/ total category-max) 0)]
                      ($ :button {:id (str "btn-category-filter-" category-key)
                                  :key (str "category-" category-key)
                                  :class "w-full text-left group transition-all duration-200"
                                  :on-click #(rf/dispatch [:user-expenses/reports-toggle-category category-key])}
                        ($ :div {:class "flex items-end justify-between gap-2 mb-1"}
                          ($ :div {:class "flex items-baseline gap-2"}
                            ($ :div {:class "flex items-center gap-2"}
                              ($ :div {:class "w-2 h-2 rounded-full bg-primary/50"})
                              ($ :span {:class (str "font-bold text-sm " (if selected? "text-primary" "text-base-content/80"))}
                                (or (:category_name row) "Uncategorized")))
                            ($ :span {:class "text-xs text-base-content/50"}
                              (str (format-int (:line_count row)) " receipts · " (format-percent (:allocation_pct row)))))
                          ($ :span {:class "font-mono text-sm font-medium"}
                            (format-money total primary-currency)))

                        ($ :div {:class "h-2 bg-base-100 rounded-full overflow-hidden relative"}
                          ($ :div {:class (str "h-full rounded-full transition-all duration-500 absolute top-0 left-0 "
                                            (if selected? "bg-primary" "bg-base-content/20 group-hover:bg-primary/60"))
                                   :style {:width (str (* 100 ratio) "%")}})))))
                  category-rows))
              ($ :p {:class "text-center p-8 bg-base-50 rounded-xl text-base-content/60"}
                "No category data available."))))))))
