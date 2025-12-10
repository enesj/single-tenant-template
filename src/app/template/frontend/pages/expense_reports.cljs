(ns app.template.frontend.pages.expense-reports
  "User-facing expense reports and analytics page."
  (:require
    [app.template.frontend.components.button :refer [button]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect use-state]]
    [uix.re-frame :refer [use-subscribe]]))

;; ========================================================================
;; Formatting helpers
;; ========================================================================

(defn format-money
  [amount currency]
  (cond
    (nil? amount) "—"
    :else (try
            (.toLocaleString (js/Number amount) "en-US"
              #js {:style "currency"
                   :currency (or currency "USD")
                   :minimumFractionDigits 2
                   :maximumFractionDigits 2})
            (catch :default _
              (str (or currency "$") " " (.toFixed (js/Number amount) 2))))))

(defn month-name [month-key]
  (let [[year month] (clojure.string/split (name month-key) #"-")]
    (str (get ["" "Jan" "Feb" "Mar" "Apr" "May" "Jun" 
               "Jul" "Aug" "Sep" "Oct" "Nov" "Dec"]
           (js/parseInt month 10))
      " " year)))

;; ========================================================================
;; Chart Components
;; ========================================================================

(defui month-bar [{:keys [month total max-total currency]}]
  (let [height-pct (if (pos? max-total)
                     (* 100 (/ total max-total))
                     0)]
    ($ :div {:class "flex flex-col items-center gap-2"}
      ($ :div {:class "relative h-40 w-12 bg-base-200 rounded-t-lg overflow-hidden"}
        ($ :div {:class "absolute bottom-0 w-full bg-primary transition-all duration-300 rounded-t"
                 :style {:height (str height-pct "%")}}))
      ($ :span {:class "text-xs text-base-content/70"} (month-name month))
      ($ :span {:class "text-xs font-medium"} (format-money total currency)))))

(defui spending-chart [{:keys [data currency loading?]}]
  (let [max-total (apply max (map :total data))]
    ($ :div {:class "bg-white rounded-xl shadow-sm border border-base-200 p-6"}
      ($ :h3 {:class "font-semibold mb-4"} "Monthly Spending")
      (if loading?
        ($ :div {:class "h-48 flex items-center justify-center"}
          ($ :span {:class "ds-loading ds-loading-spinner"}))
        (if (seq data)
          ($ :div {:class "flex items-end justify-around gap-2 h-48"}
            (for [{:keys [month total]} (reverse (take 6 data))]
              ($ month-bar {:key month
                            :month month
                            :total total
                            :max-total max-total
                            :currency currency})))
          ($ :div {:class "h-48 flex items-center justify-center text-base-content/50"}
            "No spending data yet"))))))

(defui top-suppliers [{:keys [data currency loading?]}]
  ($ :div {:class "bg-white rounded-xl shadow-sm border border-base-200 p-6"}
    ($ :h3 {:class "font-semibold mb-4"} "Top Suppliers")
    (if loading?
      ($ :div {:class "space-y-3"}
        (for [i (range 5)]
          ($ :div {:key i :class "h-8 bg-base-200 rounded animate-pulse"})))
      (if (seq data)
        ($ :div {:class "space-y-3"}
          (for [{:keys [supplier_id supplier_name total count]} data]
            ($ :div {:key (or supplier_id supplier_name)
                     :class "flex items-center justify-between"}
              ($ :div
                ($ :p {:class "font-medium"} (or supplier_name "Unknown"))
                ($ :p {:class "text-xs text-base-content/60"} (str count " expenses")))
              ($ :span {:class "font-mono font-medium"} (format-money total currency)))))
        ($ :div {:class "text-base-content/50 text-center py-4"}
          "No supplier data yet")))))

;; ========================================================================
;; Summary Cards
;; ========================================================================

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

;; ========================================================================
;; Main Page
;; ========================================================================

(defui expense-reports-page []
  (let [summary (or (use-subscribe [:user-expenses/summary]) {})
        summary-loading? (boolean (use-subscribe [:user-expenses/summary-loading?]))
        by-month (or (use-subscribe [:user-expenses/by-month]) [])
        by-month-loading? (boolean (use-subscribe [:user-expenses/by-month-loading?]))
        by-supplier (or (use-subscribe [:user-expenses/by-supplier]) [])
        by-supplier-loading? (boolean (use-subscribe [:user-expenses/by-supplier-loading?]))
        [date-range set-date-range!] (use-state {:months-back 6})
        
        currency-totals (or (:currency-totals summary) {})
        primary-currency (or (some #(when (contains? currency-totals %) %) [:BAM :USD :EUR])
                           (-> currency-totals keys first))
        primary-currency-str (when primary-currency (name primary-currency))
        total-sum (reduce + 0 (vals currency-totals))
        total-expenses (or (:total-expenses summary) 0)
        avg-spend (when (pos? total-expenses) (/ total-sum total-expenses))]
    
    ;; Fetch data on mount and when range changes
    (use-effect
      (fn []
        (rf/dispatch [:user-expenses/fetch-summary])
        (rf/dispatch [:user-expenses/fetch-by-month date-range])
        (rf/dispatch [:user-expenses/fetch-by-supplier {:limit 10}])
        js/undefined)
      [date-range])
    
    ($ :div {:class "min-h-screen bg-base-100"}
      ;; Header
      ($ :header {:class "bg-white border-b border-base-200"}
        ($ :div {:class "max-w-6xl mx-auto px-4 py-4 sm:py-6"}
          ($ :div {:class "flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4"}
            ($ :div
              ($ :div {:class "text-sm ds-breadcrumbs"}
                ($ :ul
                  ($ :li ($ :a {:href "/expenses"} "Expenses"))
                  ($ :li "Reports")))
              ($ :h1 {:class "text-xl sm:text-2xl font-bold"} "Expense Reports"))
            ($ :div {:class "flex gap-2"}
              ($ :select {:class "ds-select ds-select-sm ds-select-bordered"
                          :value (:months-back date-range)
                          :on-change #(set-date-range! {:months-back (js/parseInt (.. % -target -value) 10)})}
                ($ :option {:value 3} "Last 3 months")
                ($ :option {:value 6} "Last 6 months")
                ($ :option {:value 12} "Last 12 months"))
              ($ button {:btn-type :ghost
                         :on-click #(rf/dispatch [:navigate-to "/expenses"])}
                "Dashboard")))))
      
      ;; Main content
      ($ :main {:class "max-w-6xl mx-auto px-4 py-6"}
        ;; Summary stats
        ($ :div {:class "grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8"}
          ($ stat-card {:title "Total Spent"
                        :value (format-money total-sum primary-currency-str)
                        :subtitle "all time"
                        :icon "💰"
                        :loading? summary-loading?})
          ($ stat-card {:title "Expenses"
                        :value total-expenses
                        :subtitle "total count"
                        :icon "📋"
                        :loading? summary-loading?})
          ($ stat-card {:title "Average"
                        :value (if avg-spend (format-money avg-spend primary-currency-str) "—")
                        :subtitle "per expense"
                        :icon "📊"
                        :loading? summary-loading?})
          ($ stat-card {:title "This Month"
                        :value (format-money (get (first by-month) :total) (or (get (first by-month) :currency) primary-currency-str))
                        :subtitle "spending"
                        :icon "📅"
                        :loading? by-month-loading?}))
        
        ;; Charts
        ($ :div {:class "grid grid-cols-1 lg:grid-cols-2 gap-6"}
          ($ spending-chart {:data by-month
                             :currency primary-currency-str
                             :loading? by-month-loading?})
          ($ top-suppliers {:data by-supplier
                            :currency primary-currency-str
                            :loading? by-supplier-loading?}))
        
        ;; Export actions
        ($ :div {:class "mt-8 bg-white rounded-xl shadow-sm border border-base-200 p-6"}
          ($ :h3 {:class "font-semibold mb-4"} "Export Data")
          ($ :p {:class "text-sm text-base-content/70 mb-4"}
            "Download your expense data for external analysis or record keeping.")
          ($ :div {:class "flex flex-wrap gap-2"}
            ($ button {:btn-type :outline
                       :size :sm
                       :on-click #(rf/dispatch [:user-expenses/export {:format :csv}])}
              "📄 Export CSV")
            ($ button {:btn-type :outline
                       :size :sm
                       :on-click #(rf/dispatch [:user-expenses/export {:format :pdf}])}
              "📑 Export PDF")))))))
