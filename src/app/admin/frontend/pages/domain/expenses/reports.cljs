(ns app.admin.frontend.pages.domain.expenses.reports
  (:require
    [app.admin.frontend.components.layout :as layout]
    [app.admin.frontend.events.reports]
    [app.admin.frontend.subs.reports]
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.stats :refer [page-header]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]))

;; ---------------------------------------------------------------------------
;; Formatting helpers
;; ---------------------------------------------------------------------------

(defn- fmt-amount [v]
  (let [n (if (number? v) v (js/parseFloat (str v)))]
    (if (js/isNaN n)
      "\u2014"
      (.toLocaleString n "en" #js {:minimumFractionDigits 2 :maximumFractionDigits 2}))))

(defn- fmt-pct [v]
  (let [n (if (number? v) v (js/parseFloat (str v)))]
    (if (or (nil? v) (js/isNaN n))
      "\u2014"
      (str (.toFixed n 1) "%"))))

(defn- fmt-int [v]
  (let [n (if (number? v) v (js/parseInt (str v) 10))]
    (if (js/isNaN n) "\u2014" (.toLocaleString n "en"))))

;; ---------------------------------------------------------------------------
;; Shared UI atoms
;; ---------------------------------------------------------------------------

(defui loading-spinner []
  ($ :div {:class "flex justify-center items-center py-12"}
    ($ :div {:class "ds-loading ds-loading-spinner ds-loading-md text-primary"})))

(defui report-section [{:keys [title subtitle children report-key]}]
  (let [loading? (use-subscribe [:admin/report-loading? report-key])
        error (use-subscribe [:admin/report-error report-key])]
    ($ :div {:class "bg-base-100 rounded-2xl border border-base-300 shadow-sm overflow-hidden"}
      ($ :div {:class "px-6 py-4 border-b border-base-200 bg-base-200/30"}
        ($ :h3 {:class "text-lg font-bold text-base-content"} title)
        (when subtitle
          ($ :p {:class "text-sm text-base-content/60 mt-0.5"} subtitle)))
      ($ :div {:class "p-6"}
        (cond
          error ($ :div {:class "ds-alert ds-alert-error"} ($ :span error))
          loading? ($ loading-spinner)
          :else children)))))

;; ---------------------------------------------------------------------------
;; Report tables
;; ---------------------------------------------------------------------------

(defui top-suppliers-table []
  (let [data (use-subscribe [:admin/report-data :top-suppliers])]
    ($ report-section {:title "Top Suppliers"
                       :subtitle "Ranked by total spending across all users"
                       :report-key :top-suppliers}
      (if (seq data)
        ($ :div {:class "overflow-x-auto"}
          ($ :table {:class "ds-table ds-table-sm w-full"}
            ($ :thead
              ($ :tr
                ($ :th {:class "w-10"} "#")
                ($ :th "Supplier")
                ($ :th {:class "text-right"} "Currency")
                ($ :th {:class "text-right"} "Total")
                ($ :th {:class "text-right"} "Expenses")
                ($ :th {:class "text-right"} "Share")))
            ($ :tbody
              (map-indexed
                (fn [i row]
                  ($ :tr {:key (str (:supplier-id row) "-" (:currency row) "-" i)}
                    ($ :td {:class "text-base-content/50 font-mono"} (str (inc i)))
                    ($ :td {:class "font-medium"} (or (:supplier-name row) "Unknown"))
                    ($ :td {:class "text-right text-xs font-mono"} (or (:currency row) "\u2014"))
                    ($ :td {:class "text-right font-semibold tabular-nums"}
                      (fmt-amount (:total-amount row)))
                    ($ :td {:class "text-right tabular-nums"}
                      (fmt-int (:expense-count row)))
                    ($ :td {:class "text-right"}
                      ($ :span {:class "ds-badge ds-badge-sm ds-badge-primary ds-badge-outline"}
                        (fmt-pct (:share-pct row))))))
                data))))
        ($ :p {:class "text-base-content/50 text-center py-4"} "No supplier data available")))))

(defui top-items-table []
  (let [data (use-subscribe [:admin/report-data :top-items])]
    ($ report-section {:title "Top Items"
                       :subtitle "Most purchased articles by total spending"
                       :report-key :top-items}
      (if (seq data)
        ($ :div {:class "overflow-x-auto"}
          ($ :table {:class "ds-table ds-table-sm w-full"}
            ($ :thead
              ($ :tr
                ($ :th {:class "w-10"} "#")
                ($ :th "Article")
                ($ :th {:class "text-right"} "Currency")
                ($ :th {:class "text-right"} "Total")
                ($ :th {:class "text-right"} "Qty")
                ($ :th {:class "text-right"} "Suppliers")
                ($ :th {:class "text-right"} "Stores")))
            ($ :tbody
              (map-indexed
                (fn [i row]
                  ($ :tr {:key (str (:alias-id row) "-" (:currency row) "-" i)}
                    ($ :td {:class "text-base-content/50 font-mono"} (str (inc i)))
                    ($ :td {:class "font-medium"}
                      (or (:article-canonical-name row) (:alias-label row) "Unmapped"))
                    ($ :td {:class "text-right text-xs font-mono"} (or (:currency row) "\u2014"))
                    ($ :td {:class "text-right font-semibold tabular-nums"}
                      (fmt-amount (:total-amount row)))
                    ($ :td {:class "text-right tabular-nums"} (fmt-int (:qty-total row)))
                    ($ :td {:class "text-right tabular-nums"} (fmt-int (:supplier-count row)))
                    ($ :td {:class "text-right tabular-nums"} (fmt-int (:store-count row)))))
                data))))
        ($ :p {:class "text-base-content/50 text-center py-4"} "No item data available")))))

(defui category-allocation-table []
  (let [data (use-subscribe [:admin/report-data :category-allocation])
        categories (->> (or data [])
                     (reduce (fn [acc row]
                               (let [k [(:category-key row) (:currency row)]]
                                 (if (contains? acc k) acc (assoc acc k row))))
                       {})
                     vals
                     (sort-by (fn [r] (- (or (js/parseFloat (str (:total-amount r))) 0))))
                     vec)]
    ($ report-section {:title "Category Allocation"
                       :subtitle "Spending breakdown by product category"
                       :report-key :category-allocation}
      (if (seq categories)
        ($ :div {:class "overflow-x-auto"}
          ($ :table {:class "ds-table ds-table-sm w-full"}
            ($ :thead
              ($ :tr
                ($ :th "Category")
                ($ :th {:class "text-right"} "Currency")
                ($ :th {:class "text-right"} "Total")
                ($ :th {:class "text-right"} "Items")
                ($ :th {:class "text-right"} "Allocation")
                ($ :th {:class "w-32"} "")))
            ($ :tbody
              (map-indexed
                (fn [i row]
                  (let [pct (or (js/parseFloat (str (:allocation-pct row))) 0)]
                    ($ :tr {:key (str (:category-key row) "-" (:currency row) "-" i)}
                      ($ :td {:class "font-medium"} (or (:category-name row) "Uncategorized"))
                      ($ :td {:class "text-right text-xs font-mono"} (or (:currency row) "\u2014"))
                      ($ :td {:class "text-right font-semibold tabular-nums"}
                        (fmt-amount (:total-amount row)))
                      ($ :td {:class "text-right tabular-nums"} (fmt-int (:line-count row)))
                      ($ :td {:class "text-right"}
                        ($ :span {:class "ds-badge ds-badge-sm ds-badge-secondary ds-badge-outline"}
                          (fmt-pct pct)))
                      ($ :td
                        ($ :div {:class "w-full bg-base-200 rounded-full h-2"}
                          ($ :div {:class "bg-secondary rounded-full h-2 transition-all"
                                   :style {:width (str (min pct 100) "%")}}))))))
                categories))))
        ($ :p {:class "text-base-content/50 text-center py-4"} "No category data available")))))

(defui supplier-trends-section []
  (let [data (use-subscribe [:admin/report-data :supplier-monthly-trends])
        grouped (->> (or data [])
                  (group-by (fn [r] [(:supplier-id r) (:supplier-name r) (:currency r)]))
                  (sort-by (fn [[_ rows]]
                             (- (reduce + 0 (map #(or (js/parseFloat (str (:total-amount %))) 0) rows)))))
                  vec)]
    ($ report-section {:title "Supplier Monthly Trends"
                       :subtitle "Top supplier spending over time"
                       :report-key :supplier-monthly-trends}
      (if (seq grouped)
        ($ :div {:class "space-y-4"}
          (map-indexed
            (fn [i [[_sid sname currency] rows]]
              (let [sorted-rows (sort-by :month rows)]
                ($ :div {:key (str _sid "-" currency "-" i)
                         :class "bg-base-200/30 rounded-xl p-4"}
                  ($ :div {:class "flex items-center justify-between mb-3"}
                    ($ :span {:class "font-semibold text-base-content"} (or sname "Unknown"))
                    ($ :span {:class "ds-badge ds-badge-sm ds-badge-ghost font-mono"} currency))
                  ($ :div {:class "grid grid-cols-3 sm:grid-cols-4 md:grid-cols-6 gap-2"}
                    (map (fn [row]
                           ($ :div {:key (:month row)
                                    :class "text-center p-2 bg-base-100 rounded-lg"}
                             ($ :div {:class "text-[10px] text-base-content/50 font-mono"} (:month row))
                             ($ :div {:class "text-sm font-semibold tabular-nums"}
                               (fmt-amount (:total-amount row)))
                             ($ :div {:class "text-[10px] text-base-content/40"}
                               (str (fmt-int (:expense-count row)) " exp"))))
                      sorted-rows)))))
            grouped))
        ($ :p {:class "text-base-content/50 text-center py-4"} "No trend data available")))))

;; ---------------------------------------------------------------------------
;; Filters bar
;; ---------------------------------------------------------------------------

(defui filters-bar []
  (let [filters (use-subscribe [:admin/reports-filters])
        months-back (or (:months-back filters) 6)
        filter-options (use-subscribe [:admin/report-data :filter-options])
        suppliers (or (:suppliers filter-options) [])
        has-filters? (or (:supplier-id filters) (:expense-category-id filters) (:currency filters))]
    ($ :div {:class "flex flex-wrap items-end gap-4 p-4 bg-base-200/30 rounded-2xl border border-base-300"}
      ($ :div {:class "space-y-1"}
        ($ :label {:class "text-[10px] font-bold text-base-content/50 uppercase tracking-widest"
                   :for "admin-reports-months-back"}
          "Time Range")
        ($ :select {:id "admin-reports-months-back"
                    :class "ds-select ds-select-sm ds-select-bordered bg-base-100 font-medium"
                    :value months-back
                    :on-change (fn [e]
                                 (rf/dispatch [:admin/reports-set-filter :months-back
                                               (js/parseInt (.. e -target -value) 10)])
                                 (rf/dispatch [:admin/reports-refresh]))}
          ($ :option {:value 3} "Last 3 months")
          ($ :option {:value 6} "Last 6 months")
          ($ :option {:value 12} "Last 12 months")
          ($ :option {:value 24} "Last 24 months")))

      (when (seq suppliers)
        ($ :div {:class "space-y-1"}
          ($ :label {:class "text-[10px] font-bold text-base-content/50 uppercase tracking-widest"
                     :for "admin-reports-supplier"}
            "Supplier")
          ($ :select {:id "admin-reports-supplier"
                      :class "ds-select ds-select-sm ds-select-bordered bg-base-100 font-medium"
                      :value (or (:supplier-id filters) "")
                      :on-change (fn [e]
                                   (let [v (.. e -target -value)]
                                     (rf/dispatch [:admin/reports-set-filter :supplier-id
                                                   (when (seq v) v)])
                                     (rf/dispatch [:admin/reports-refresh])))}
            ($ :option {:value ""} "All Suppliers")
            (map (fn [s]
                   ($ :option {:key (str (:id s)) :value (str (:id s))} (:name s)))
              suppliers))))

      ($ :div {:class "flex items-end gap-2"}
        ($ button {:id "btn-admin-reports-refresh"
                   :btn-type :ghost
                   :size :sm
                   :class "font-medium"
                   :on-click #(rf/dispatch [:admin/reports-refresh])}
          "Refresh")
        (when has-filters?
          ($ button {:id "btn-admin-reports-clear"
                     :btn-type :ghost
                     :size :sm
                     :class "text-error/70 hover:text-error font-medium"
                     :on-click (fn []
                                 (rf/dispatch [:admin/reports-clear-filters])
                                 (rf/dispatch [:admin/reports-refresh]))}
            "Clear Filters"))))))

;; ---------------------------------------------------------------------------
;; Page
;; ---------------------------------------------------------------------------

(defui admin-reports-content []
  ($ :div {:class "py-6 min-h-screen bg-gradient-to-br from-base-100 via-base-200 to-base-300"}
    ($ page-header {:title "Expense Analytics"
                    :subtitle "Cross-tenant expense reports grouped by global entities"
                    :icon "M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"
                    :icon-color "from-secondary to-accent"
                    :bg-gradient "from-secondary/10 to-accent/10"})

    ($ :div {:class "mt-6 px-4 sm:px-6 lg:px-8 space-y-6"}
      ($ filters-bar)

      ($ :div {:class "grid grid-cols-1 xl:grid-cols-2 gap-6"}
        ($ top-suppliers-table)
        ($ top-items-table))

      ($ :div {:class "grid grid-cols-1 xl:grid-cols-2 gap-6"}
        ($ category-allocation-table)
        ($ supplier-trends-section)))))

(defui admin-reports-page []
  ($ layout/admin-layout
    ($ admin-reports-content)))
