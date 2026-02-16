(ns app.domain.frontend.expenses.pages.user.expense-reports
  "User-facing expense reports and analytics page."
  (:require
    [app.template.frontend.components.button :refer [button]]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui]]
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
    (>= ratio 0.8) "bg-primary text-primary-content"
    (>= ratio 0.55) "bg-primary/70 text-base-100"
    (>= ratio 0.3) "bg-primary/40 text-base-content"
    (pos? ratio) "bg-primary/20 text-base-content"
    :else "bg-base-200 text-base-content/60"))

(defui stat-card [{:keys [title value subtitle icon loading?]}]
  ($ :div {:class "bg-white rounded-xl shadow-sm border border-base-200 p-5"}
    ($ :div {:class "flex items-start justify-between"}
      ($ :div
        ($ :p {:class "text-sm font-medium text-base-content/60"} title)
        (if loading?
          ($ :div {:class "h-7 w-24 bg-base-200 rounded animate-pulse mt-1"})
          ($ :p {:class "text-2xl font-bold mt-1"} value))
        (when subtitle
          ($ :p {:class "text-xs text-base-content/50 mt-1"} subtitle)))
      (when icon
        ($ :div {:class "p-2 bg-base-200 rounded-lg"}
          ($ :span {:class "text-xl"} icon))))))

(defui section-shell [{:keys [title loading? error children]}]
  ($ :section {:class "bg-white rounded-xl shadow-sm border border-base-200 p-5"}
    ($ :div {:class "flex items-center justify-between gap-2 mb-4"}
      ($ :h3 {:class "font-semibold"} title)
      (when loading?
        ($ :span {:class "ds-loading ds-loading-spinner ds-loading-sm"})))
    (if (seq error)
      ($ :div {:class "ds-alert ds-alert-error text-sm"}
        ($ :span (str error)))
      ($ :div {:class "space-y-3"}
        children))))

(defui expense-reports-page []
  (let [summary (or (use-subscribe [:user-expenses/summary]) {})
        summary-loading? (boolean (use-subscribe [:user-expenses/summary-loading?]))
        by-month (or (use-subscribe [:user-expenses/by-month]) [])
        by-supplier (or (use-subscribe [:user-expenses/by-supplier]) [])

        reports-filters (or (use-subscribe [:user-expenses/reports-filters]) {})
        months-back (or (:months-back reports-filters) 6)
        selected-supplier-id (:supplier-id reports-filters)
        selected-day-of-week (:day-of-week reports-filters)
        selected-category-key (:category-key reports-filters)
        selected-bucket-key (:amount-bucket reports-filters)
        selected-day (:selected-day reports-filters)
        month-a (:month-a reports-filters)
        month-b (:month-b reports-filters)
        show-uncategorized? (not= false (:show-uncategorized? reports-filters))

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
        supplier-name-by-id (into {} (map (juxt :id :name) suppliers*))
        selected-supplier-name (or (get supplier-name-by-id (some-> selected-supplier-id str))
                                 (:supplier-name supplier-deep-dive)
                                 "All suppliers")

        month-options* (month-options by-month month-a month-b)

        day-pattern (aggregate-day-pattern day-of-week-data)
        day-pattern-max (apply max (cons 0 (map #(or (->number (:total_amount %)) 0) day-pattern)))

        top-items-visible (-> top-items-data
                            (filter-top-items-by-bucket selected-bucket-key)
                            (subvec 0 (min 20 (count (filter-top-items-by-bucket top-items-data selected-bucket-key)))))

        monthly-by-currency (or (:by_currency monthly-comparison) [])
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
        deep-dive-trend (or (:trend supplier-deep-dive) [])
        deep-dive-top-aliases (or (:top-aliases supplier-deep-dive) [])

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

    ($ :div {:class "min-h-screen bg-base-100"}
      ($ :header {:class "bg-white border-b border-base-200"}
        ($ :div {:class "max-w-7xl mx-auto px-4 py-4 sm:py-6"}
          ($ :div {:class "flex flex-col xl:flex-row xl:items-end xl:justify-between gap-4"}
            ($ :div
              ($ :div {:class "text-sm ds-breadcrumbs"}
                ($ :ul
                  ($ :li ($ :a {:href "/expenses"} "Expenses"))
                  ($ :li "Reports")))
              ($ :h1 {:class "text-xl sm:text-2xl font-bold"} "Expense Reports")
              ($ :p {:class "text-sm text-base-content/70 mt-1"}
                "Explore spending behavior with interactive report widgets."))

            ($ :div {:class "flex flex-wrap items-center gap-2"}
              ($ :label {:class "text-xs text-base-content/60" :for "reports-filter-months-back"} "Range")
              ($ :select {:id "reports-filter-months-back"
                          :class "ds-select ds-select-sm ds-select-bordered"
                          :value months-back
                          :on-change #(rf/dispatch [:user-expenses/reports-set-filter
                                                    :months-back
                                                    (js/parseInt (.. % -target -value) 10)])}
                ($ :option {:value 3} "Last 3 months")
                ($ :option {:value 6} "Last 6 months")
                ($ :option {:value 12} "Last 12 months")
                ($ :option {:value 24} "Last 24 months"))

              ($ :label {:class "text-xs text-base-content/60" :for "reports-filter-supplier"} "Supplier")
              ($ :select {:id "reports-filter-supplier"
                          :class "ds-select ds-select-sm ds-select-bordered min-w-[14rem]"
                          :value (or selected-supplier-id "")
                          :on-change #(rf/dispatch [:user-expenses/reports-set-filter
                                                    :supplier-id
                                                    (let [v (.. % -target -value)]
                                                      (when (seq (str/trim v)) v))])}
                ($ :option {:value ""} "All suppliers")
                (mapv (fn [{:keys [id name]}]
                        ($ :option {:key id :value id} name))
                  suppliers*))

              ($ button {:id "btn-reports-refresh"
                         :btn-type :outline
                         :size :sm
                         :on-click #(rf/dispatch [:user-expenses/reports-refresh])}
                "Refresh")

              ($ button {:id "btn-reports-clear-local-filters"
                         :btn-type :ghost
                         :size :sm
                         :on-click #(rf/dispatch [:user-expenses/reports-clear-local-filters])}
                "Clear local filters")

              ($ button {:id "btn-reports-back-dashboard"
                         :btn-type :ghost
                         :size :sm
                         :on-click #(rf/dispatch [:navigate-to "/expenses"])}
                "Dashboard")))))

      ($ :main {:class "max-w-7xl mx-auto px-4 py-6 space-y-6"}
        ($ :div {:class "grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4"}
          ($ stat-card {:title "Total Spent"
                        :value (format-money total-sum primary-currency)
                        :subtitle "summary endpoint"
                        :icon "💰"
                        :loading? summary-loading?})
          ($ stat-card {:title "Expenses"
                        :value (format-int total-expenses)
                        :subtitle "posted entries"
                        :icon "📋"
                        :loading? summary-loading?})
          ($ stat-card {:title "Average Expense"
                        :value (if average-spend (format-money average-spend primary-currency) "—")
                        :subtitle "per expense"
                        :icon "📊"
                        :loading? summary-loading?})
          ($ stat-card {:title "Active Supplier Filter"
                        :value selected-supplier-name
                        :subtitle "cross-widget"
                        :icon "🏪"
                        :loading? false}))

        ($ :div {:class "grid grid-cols-1 xl:grid-cols-2 gap-6"}
          ;; 1) Supplier deep-dive
          ($ section-shell {:title "1) Supplier deep-dive"
                            :loading? supplier-deep-dive-loading?
                            :error supplier-deep-dive-error}
            (if (str/blank? (str (or selected-supplier-id "")))
              ($ :p {:class "text-sm text-base-content/70"}
                "Choose a supplier to load deep-dive details, trends, and top aliases.")
              ($ :div {:class "space-y-3"}
                ($ :p {:class "text-sm text-base-content/70"}
                  (str "Showing deep-dive for: " selected-supplier-name))

                (if (seq deep-dive-summary)
                  ($ :div {:class "grid grid-cols-1 sm:grid-cols-2 gap-2"}
                    (mapv
                      (fn [row]
                        (let [currency (:currency row)
                              total-amount (:total_amount row)
                              expense-count (:expense_count row)]
                          ($ :div {:key (str "supplier-summary-" currency)
                                   :class "rounded-lg border border-base-200 p-3"}
                            ($ :p {:class "text-xs text-base-content/60"} (str "Currency: " currency))
                            ($ :p {:class "font-semibold"} (format-money total-amount currency))
                            ($ :p {:class "text-xs text-base-content/60"}
                              (str (format-int expense-count) " expenses")))))
                      deep-dive-summary))
                  ($ :p {:class "text-sm text-base-content/60"}
                    "No supplier summary data."))

                ($ :div {:class "rounded-lg border border-base-200 p-3"}
                  ($ :p {:class "text-sm font-medium mb-2"} "Trend by month")
                  (if (seq deep-dive-trend)
                    ($ :div {:class "overflow-x-auto"}
                      ($ :table {:class "table table-sm"}
                        ($ :thead
                          ($ :tr
                            ($ :th "Month")
                            ($ :th "Currency")
                            ($ :th {:class "text-right"} "Total")
                            ($ :th {:class "text-right"} "Count")))
                        ($ :tbody
                          (mapv
                            (fn [row]
                              ($ :tr {:key (str "trend-" (:month row) "-" (:currency row))}
                                ($ :td (month-label (:month row)))
                                ($ :td (str (:currency row)))
                                ($ :td {:class "text-right font-mono"}
                                  (format-money (:total_amount row) (:currency row)))
                                ($ :td {:class "text-right"}
                                  (format-int (:expense_count row)))))
                            deep-dive-trend))))
                    ($ :p {:class "text-sm text-base-content/60"}
                      "No trend data for the selected filters.")))

                ($ :div {:class "rounded-lg border border-base-200 p-3"}
                  ($ :p {:class "text-sm font-medium mb-2"} "Top item aliases")
                  (if (seq deep-dive-top-aliases)
                    ($ :div {:class "overflow-x-auto"}
                      ($ :table {:class "table table-sm"}
                        ($ :thead
                          ($ :tr
                            ($ :th "Alias")
                            ($ :th "Currency")
                            ($ :th {:class "text-right"} "Total")
                            ($ :th {:class "text-right"} "Lines")))
                        ($ :tbody
                          (mapv
                            (fn [row]
                              ($ :tr {:key (str "alias-" (or (:alias_id row) (:alias_label row)) "-" (:currency row))}
                                ($ :td (or (:alias_label row) "Unmapped item"))
                                ($ :td (str (:currency row)))
                                ($ :td {:class "text-right font-mono"}
                                  (format-money (:total_amount row) (:currency row)))
                                ($ :td {:class "text-right"}
                                  (format-int (:line_count row)))))
                            deep-dive-top-aliases))))
                    ($ :p {:class "text-sm text-base-content/60"}
                      "No alias-level details available."))))))

          ;; 2) Day-of-week pattern
          ($ section-shell {:title "2) Day-of-week pattern"
                            :loading? day-of-week-loading?
                            :error day-of-week-error}
            (if (seq day-pattern)
              ($ :div {:class "space-y-2"}
                ($ :p {:class "text-sm text-base-content/70"}
                  "Click a day to toggle day-of-week filtering (applies to this widget and heatmap).")
                (mapv
                  (fn [row]
                    (let [iso-day (:iso_day_of_week row)
                          selected? (= selected-day-of-week iso-day)
                          total (or (->number (:total_amount row)) 0)
                          ratio (if (pos? day-pattern-max) (/ total day-pattern-max) 0)]
                      ($ :button {:id (str "btn-day-filter-" (:day_key row))
                                  :key (str "dow-" iso-day)
                                  :class (str "w-full text-left rounded-lg border p-2 transition "
                                           (if selected?
                                             "border-primary bg-primary/10"
                                             "border-base-200 hover:border-primary/40"))
                                  :on-click #(rf/dispatch [:user-expenses/reports-toggle-day-of-week iso-day])}
                        ($ :div {:class "flex items-center justify-between gap-2"}
                          ($ :div
                            ($ :p {:class "font-medium"} (:day_label row))
                            ($ :p {:class "text-xs text-base-content/60"}
                              (str (format-int (:expense_count row)) " expenses")))
                          ($ :p {:class "font-mono text-sm"}
                            (format-money total primary-currency)))
                        ($ :div {:class "mt-2 h-2 bg-base-200 rounded overflow-hidden"}
                          ($ :div {:class "h-full bg-primary"
                                   :style {:width (str (* 100 ratio) "%")}})))))
                  day-pattern))
              ($ :p {:class "text-sm text-base-content/60"}
                "No day-of-week data in the selected range."))))

        ($ :div {:class "grid grid-cols-1 xl:grid-cols-2 gap-6"}
          ;; 3) Top items table/report
          ($ section-shell {:title "3) Top items"
                            :loading? top-items-loading?
                            :error top-items-error}
            (if (seq selected-bucket-key)
              ($ :p {:class "text-xs text-base-content/70"}
                (str "Filtered by amount bucket: " selected-bucket-key))
              ($ :p {:class "text-xs text-base-content/70"}
                "Click a size bucket in widget #5 to cross-filter this table."))

            (if (seq top-items-visible)
              ($ :div {:class "overflow-x-auto"}
                ($ :table {:id "table-top-items"
                           :class "table table-sm"}
                  ($ :thead
                    ($ :tr
                      ($ :th "Item")
                      ($ :th "Currency")
                      ($ :th {:class "text-right"} "Total")
                      ($ :th {:class "text-right"} "Qty")
                      ($ :th {:class "text-right"} "Lines")))
                  ($ :tbody
                    (mapv
                      (fn [row]
                        ($ :tr {:key (str "top-item-" (or (:alias_id row) (:alias_label row)) "-" (:currency row))}
                          ($ :td (or (:alias_label row) "Unmapped item"))
                          ($ :td (str (:currency row)))
                          ($ :td {:class "text-right font-mono"}
                            (format-money (:total_amount row) (:currency row)))
                          ($ :td {:class "text-right"}
                            (format-int (:qty_total row)))
                          ($ :td {:class "text-right"}
                            (format-int (:line_count row)))))
                      top-items-visible))))
              ($ :p {:class "text-sm text-base-content/60"}
                "No top-item rows match the active filters.")))

          ;; 4) Monthly comparison selector + deltas
          ($ section-shell {:title "4) Monthly comparison"
                            :loading? monthly-comparison-loading?
                            :error monthly-comparison-error}
            ($ :div {:class "flex flex-wrap gap-2 items-end"}
              ($ :div
                ($ :label {:class "text-xs text-base-content/60"
                           :for "reports-month-a-select"}
                  "Month A")
                ($ :select {:id "reports-month-a-select"
                            :class "ds-select ds-select-sm ds-select-bordered"
                            :value (or month-a "")
                            :on-change #(rf/dispatch [:user-expenses/reports-set-filter
                                                      :month-a
                                                      (.. % -target -value)])}
                  (mapv
                    (fn [month]
                      ($ :option {:key (str "month-a-" month) :value month}
                        (month-label month)))
                    month-options*)))

              ($ :div
                ($ :label {:class "text-xs text-base-content/60"
                           :for "reports-month-b-select"}
                  "Month B")
                ($ :select {:id "reports-month-b-select"
                            :class "ds-select ds-select-sm ds-select-bordered"
                            :value (or month-b "")
                            :on-change #(rf/dispatch [:user-expenses/reports-set-filter
                                                      :month-b
                                                      (.. % -target -value)])}
                  (mapv
                    (fn [month]
                      ($ :option {:key (str "month-b-" month) :value month}
                        (month-label month)))
                    month-options*))))

            (if (seq monthly-by-currency)
              ($ :div {:class "overflow-x-auto"}
                ($ :table {:class "table table-sm"}
                  ($ :thead
                    ($ :tr
                      ($ :th "Currency")
                      ($ :th {:class "text-right"} (str "A (" (month-label month-a) ")"))
                      ($ :th {:class "text-right"} (str "B (" (month-label month-b) ")"))
                      ($ :th {:class "text-right"} "Δ Amount")
                      ($ :th {:class "text-right"} "Δ %")
                      ($ :th {:class "text-right"} "Δ Count")))
                  ($ :tbody
                    (mapv
                      (fn [row]
                        (let [delta (or (->number (:delta_amount row)) 0)
                              positive? (>= delta 0)]
                          ($ :tr {:key (str "monthly-currency-" (:currency row))}
                            ($ :td (str (:currency row)))
                            ($ :td {:class "text-right font-mono"}
                              (format-money (:month_a_total row) (:currency row)))
                            ($ :td {:class "text-right font-mono"}
                              (format-money (:month_b_total row) (:currency row)))
                            ($ :td {:class (str "text-right font-mono " (if positive? "text-success" "text-error"))}
                              (format-money delta (:currency row)))
                            ($ :td {:class "text-right"}
                              (format-percent (:delta_percent row)))
                            ($ :td {:class "text-right"}
                              (format-int (:delta_count row))))))
                      monthly-by-currency))))
              ($ :p {:class "text-sm text-base-content/60"}
                "No monthly comparison rows for the selected months."))

            (when (seq monthly-by-supplier)
              ($ :div {:class "rounded-lg border border-base-200 p-3"}
                ($ :p {:class "text-sm font-medium mb-2"} "Largest supplier deltas")
                ($ :div {:class "space-y-2"}
                  (mapv
                    (fn [row]
                      (let [delta (or (->number (:delta_amount row)) 0)
                            positive? (>= delta 0)]
                        ($ :div {:key (str "supplier-delta-" (or (:supplier_id row) (:supplier_name row)) "-" (:currency row))
                                 :class "flex items-center justify-between gap-3 text-sm"}
                          ($ :span (or (:supplier_name row) "Unknown supplier"))
                          ($ :span {:class (str "font-mono " (if positive? "text-success" "text-error"))}
                            (format-money delta (:currency row))))))
                    (take 6 monthly-by-supplier)))))))

        ($ :div {:class "grid grid-cols-1 xl:grid-cols-2 gap-6"}
          ;; 5) Size distribution chart with clickable buckets
          ($ section-shell {:title "5) Expense size distribution"
                            :loading? size-distribution-loading?
                            :error size-distribution-error}
            ($ :p {:class "text-sm text-base-content/70"}
              "Click a bucket to filter the top-items report.")
            (if (seq size-buckets)
              ($ :div {:class "space-y-2"}
                (mapv
                  (fn [row]
                    (let [bucket-key (:bucket_key row)
                          selected? (= selected-bucket-key bucket-key)
                          total (or (->number (:total_amount row)) 0)
                          ratio (if (pos? size-buckets-max) (/ total size-buckets-max) 0)]
                      ($ :button {:id (str "btn-size-bucket-" bucket-key)
                                  :key (str "bucket-" bucket-key)
                                  :class (str "w-full text-left rounded-lg border p-2 transition "
                                           (if selected?
                                             "border-primary bg-primary/10"
                                             "border-base-200 hover:border-primary/40"))
                                  :on-click #(rf/dispatch [:user-expenses/reports-toggle-amount-bucket bucket-key])}
                        ($ :div {:class "flex items-center justify-between gap-2"}
                          ($ :div
                            ($ :p {:class "font-medium"} (:bucket_label row))
                            ($ :p {:class "text-xs text-base-content/60"}
                              (str (format-int (:expense_count row)) " expenses")))
                          ($ :p {:class "font-mono text-sm"}
                            (format-money total primary-currency)))
                        ($ :div {:class "mt-2 h-2 bg-base-200 rounded overflow-hidden"}
                          ($ :div {:class "h-full bg-primary"
                                   :style {:width (str (* 100 ratio) "%")}})))))
                  size-buckets))
              ($ :p {:class "text-sm text-base-content/60"}
                "No size-distribution buckets found.")))

          ;; 6) Daily heatmap/calendar aggregation with day selection
          ($ section-shell {:title "6) Daily heatmap"
                            :loading? daily-heatmap-loading?
                            :error daily-heatmap-error}
            ($ :p {:class "text-sm text-base-content/70"}
              "Click a day cell to select/unselect it. Day-of-week filter applies here too.")
            (if (seq heatmap-visible)
              ($ :div {:class "space-y-3"}
                ($ :div {:class "grid grid-cols-7 gap-1"}
                  (mapv
                    (fn [row]
                      (let [total (or (->number (:total_amount row)) 0)
                            ratio (if (pos? heatmap-max) (/ total heatmap-max) 0)
                            selected? (= selected-day (:day row))]
                        ($ :button {:id (str "btn-heatmap-day-" (:day row))
                                    :key (str "heat-" (:day row))
                                    :class (str "h-10 rounded text-[11px] font-medium border transition "
                                             (heat-intensity-class ratio)
                                             " "
                                             (if selected? "border-primary ring-1 ring-primary" "border-base-300"))
                                    :title (str (:day row) " · " (format-money total primary-currency)
                                             " · " (format-int (:expense_count row)) " expenses")
                                    :on-click #(rf/dispatch [:user-expenses/reports-toggle-selected-day (:day row)])}
                          ($ :span (subs (:day row) 8 10)))))
                    heatmap-visible))

                (when selected-heatmap-day
                  ($ :div {:class "rounded-lg border border-primary/40 bg-primary/5 p-3"}
                    ($ :p {:class "text-sm font-medium"}
                      (str "Selected day: " (:day selected-heatmap-day)))
                    ($ :p {:class "text-sm"}
                      (str "Total: "
                        (format-money (:total_amount selected-heatmap-day) primary-currency)
                        " · Expenses: "
                        (format-int (:expense_count selected-heatmap-day))))
                    ($ button {:id "btn-heatmap-clear-selected-day"
                               :btn-type :ghost
                               :size :xs
                               :on-click #(rf/dispatch [:user-expenses/reports-toggle-selected-day (:day selected-heatmap-day)])}
                      "Clear selection"))))
              ($ :p {:class "text-sm text-base-content/60"}
                "No heatmap data in selected range/filter."))))

        ($ :div {:class "grid grid-cols-1"}
          ;; 7) Category allocation with uncategorized visibility
          ($ section-shell {:title "7) Category allocation"
                            :loading? category-allocation-loading?
                            :error category-allocation-error}
            ($ :div {:class "flex flex-wrap items-center gap-3"}
              ($ :label {:class "inline-flex items-center gap-2 text-sm"
                         :for "toggle-category-uncategorized-visibility"}
                ($ :input {:id "toggle-category-uncategorized-visibility"
                           :type "checkbox"
                           :class "ds-checkbox ds-checkbox-sm"
                           :checked show-uncategorized?
                           :on-change #(rf/dispatch [:user-expenses/reports-set-filter
                                                     :show-uncategorized?
                                                     (.. % -target -checked)])})
                ($ :span "Show uncategorized"))
              (when (seq selected-category-key)
                ($ :span {:class "text-xs text-base-content/70"}
                  (str "Selected category filter: " selected-category-key))))

            (if (seq category-rows)
              ($ :div {:class "space-y-2"}
                (mapv
                  (fn [row]
                    (let [category-key (:category_key row)
                          selected? (= selected-category-key category-key)
                          total (or (->number (:total_amount row)) 0)
                          ratio (if (pos? category-max) (/ total category-max) 0)]
                      ($ :button {:id (str "btn-category-filter-" category-key)
                                  :key (str "category-" category-key)
                                  :class (str "w-full text-left rounded-lg border p-2 transition "
                                           (if selected?
                                             "border-primary bg-primary/10"
                                             "border-base-200 hover:border-primary/40"))
                                  :on-click #(rf/dispatch [:user-expenses/reports-toggle-category category-key])}
                        ($ :div {:class "flex items-center justify-between gap-2"}
                          ($ :div
                            ($ :p {:class "font-medium"} (or (:category_name row) "Uncategorized"))
                            ($ :p {:class "text-xs text-base-content/60"}
                              (str (format-int (:line_count row)) " lines · "
                                (format-percent (:allocation_pct row)))))
                          ($ :p {:class "font-mono text-sm"}
                            (format-money total primary-currency)))
                        ($ :div {:class "mt-2 h-2 bg-base-200 rounded overflow-hidden"}
                          ($ :div {:class "h-full bg-primary"
                                   :style {:width (str (* 100 ratio) "%")}})))))
                  category-rows))
              ($ :p {:class "text-sm text-base-content/60"}
                "No category allocation rows found."))))))))
