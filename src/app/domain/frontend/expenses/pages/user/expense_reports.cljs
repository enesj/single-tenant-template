(ns app.domain.frontend.expenses.pages.user.expense-reports
  "User-facing expense reports and analytics page."
  (:require
    [app.domain.frontend.expenses.pages.user.expense-reports.components :refer [report-multi-select
                                                                                report-tabs
                                                                                stat-card]]
    [app.domain.frontend.expenses.pages.user.expense-reports.sections.articles :refer [articles-tab]]
    [app.domain.frontend.expenses.pages.user.expense-reports.sections.categories :refer [categories-tab]]
    [app.domain.frontend.expenses.pages.user.expense-reports.sections.expenses :refer [expenses-tab]]
    [app.domain.frontend.expenses.pages.user.expense-reports.sections.providers-stores :refer [providers-stores-tab]]
    [app.domain.frontend.expenses.pages.user.expense-reports.utils :as report-utils]
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.i18n :refer [use-t]]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-state]]
    [uix.re-frame :refer [use-subscribe]]))

(defn- normalize-selected-ids
  [value]
  (let [values (cond
                 (nil? value) []
                 (sequential? value) value
                 :else [value])]
    (->> values
      (keep (fn [v]
              (let [s (some-> v str str/trim)]
                (when (seq s) s))))
      distinct
      vec)))

(defn- normalize-report-options
  [rows]
  (->> (or rows [])
    (keep (fn [row]
            (let [id (some-> (:id row) str str/trim)
                  name* (some-> (:name row) str str/trim)
                  category-id (some-> (or (:category_id row) (:category-id row)) str str/trim)]
              (when (and (seq id) (seq name*))
                (cond-> {:id id :name name*}
                  (seq category-id) (assoc :category-id category-id))))))
    (sort-by (comp str/lower-case :name))
    vec))

(defn- merge-options-with-selected
  [available-options fallback-options selected-ids]
  (let [fallback-by-id (into {} (map (juxt :id identity) (or fallback-options [])))
        selected-options (->> (or selected-ids [])
                           (keep fallback-by-id))]
    (->> (concat (or available-options []) selected-options)
      (reduce (fn [acc {:keys [id] :as option}]
                (if (contains? acc id)
                  acc
                  (assoc acc id option)))
        {})
      vals
      (sort-by (comp str/lower-case :name))
      vec)))

(defui expense-reports-page []
  (let [t (use-t)
        summary (or (use-subscribe [:user-expenses/summary]) {})
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
        supplier-filter (:supplier-id reports-filters)
        category-filter (:category-id reports-filters)
        subcategory-filter (:subcategory-id reports-filters)
        expense-category-filter (:expense-category-id reports-filters)
        manufacturer-filter (:manufacturer-id reports-filters)
        selected-supplier-ids (normalize-selected-ids supplier-filter)
        selected-category-ids (normalize-selected-ids category-filter)
        selected-subcategory-ids (normalize-selected-ids subcategory-filter)
        selected-expense-category-ids (normalize-selected-ids expense-category-filter)
        selected-manufacturer-ids (normalize-selected-ids manufacturer-filter)
        selected-supplier-id (first selected-supplier-ids)
        expanded-supplier-id (use-subscribe [:user-expenses/reports-filter-expanded-supplier-id])
        expanded-top-item-alias-id (use-subscribe [:user-expenses/reports-filter-expanded-top-item-alias-id])
        selected-day-of-week (:day-of-week reports-filters)
        selected-category-key (:category-key reports-filters)
        selected-bucket-key (:amount-bucket reports-filters)
        selected-day (:selected-day reports-filters)
        month-a (:month-a reports-filters)
        month-b (:month-b reports-filters)
        show-uncategorized? (not= false (:show-uncategorized? reports-filters))
        has-global-filters? (or (seq selected-supplier-ids)
                              (seq selected-category-ids)
                              (seq selected-subcategory-ids)
                              (seq selected-expense-category-ids)
                              (seq selected-manufacturer-ids))

        [active-report-tab set-active-report-tab!] (use-state :expenses)
        [trend-sort set-trend-sort!] (use-state {:column :total_amount
                                                 :direction :desc
                                                 :type :number})
        [alias-sort set-alias-sort!] (use-state {:column :total_amount
                                                 :direction :desc
                                                 :type :number})
        [top-items-sort set-top-items-sort!] (use-state {:column :total_amount
                                                         :direction :desc
                                                         :type :number})
        monthly-currency-sort {:column :delta_amount
                               :direction :desc
                               :type :number}

        supplier-deep-dive (use-subscribe [:user-expenses/report-supplier-deep-dive])
        supplier-deep-dive-loading? (boolean (use-subscribe [:user-expenses/report-supplier-deep-dive-loading?]))
        supplier-deep-dive-error (use-subscribe [:user-expenses/report-supplier-deep-dive-error])

        day-of-week-data (or (use-subscribe [:user-expenses/report-day-of-week]) [])
        day-of-week-loading? (boolean (use-subscribe [:user-expenses/report-day-of-week-loading?]))
        day-of-week-error (use-subscribe [:user-expenses/report-day-of-week-error])

        top-items-data (or (use-subscribe [:user-expenses/report-top-items]) [])
        top-items-loading? (boolean (use-subscribe [:user-expenses/report-top-items-loading?]))
        top-items-error (use-subscribe [:user-expenses/report-top-items-error])
        top-item-breakdown (use-subscribe [:user-expenses/report-top-item-breakdown])
        top-item-breakdown-loading? (boolean (use-subscribe [:user-expenses/report-top-item-breakdown-loading?]))
        top-item-breakdown-error (use-subscribe [:user-expenses/report-top-item-breakdown-error])

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

        top-suppliers-data (or (use-subscribe [:user-expenses/report-top-suppliers]) [])
        top-suppliers-loading? (boolean (use-subscribe [:user-expenses/report-top-suppliers-loading?]))
        top-suppliers-error (use-subscribe [:user-expenses/report-top-suppliers-error])

        supplier-stores-data (or (use-subscribe [:user-expenses/report-supplier-stores]) [])
        supplier-stores-loading? (boolean (use-subscribe [:user-expenses/report-supplier-stores-loading?]))
        supplier-stores-error (use-subscribe [:user-expenses/report-supplier-stores-error])

        supplier-monthly-trends-data (or (use-subscribe [:user-expenses/report-supplier-monthly-trends]) [])
        supplier-monthly-trends-loading? (boolean (use-subscribe [:user-expenses/report-supplier-monthly-trends-loading?]))
        supplier-monthly-trends-error (use-subscribe [:user-expenses/report-supplier-monthly-trends-error])

        report-filter-options (use-subscribe [:user-expenses/report-filter-options])
        report-filter-options-loading? (boolean (use-subscribe [:user-expenses/report-filter-options-loading?]))
        filter-options-ready? (and (not report-filter-options-loading?) (some? report-filter-options))

        suppliers-fallback* (report-utils/supplier-options by-supplier)
        categories-fallback* (->> (if (seq categories-data) categories-data template-categories)
                               (keep (fn [row]
                                       (let [id (some-> (:id row) str)
                                             name* (some-> (:name row) str str/trim)]
                                         (when (and (seq id) (seq name*))
                                           {:id id :name name*}))))
                               (sort-by (comp str/lower-case :name))
                               vec)
        subcategories-fallback* (->> (if (seq subcategories-data) subcategories-data template-subcategories)
                                  (keep (fn [row]
                                          (let [id (some-> (:id row) str)
                                                category-id (some-> (or (:category_id row) (:category-id row)) str str/trim)
                                                name* (some-> (:name row) str str/trim)]
                                            (when (and (seq id) (seq name*))
                                              {:id id
                                               :name name*
                                               :category-id category-id}))))
                                  (sort-by (comp str/lower-case :name))
                                  vec)
        expense-categories-fallback* (->> (if (seq expense-categories-data) expense-categories-data template-expense-categories)
                                       (keep (fn [row]
                                               (let [id (some-> (:id row) str)
                                                     name* (some-> (:name row) str str/trim)]
                                                 (when (and (seq id) (seq name*))
                                                   {:id id :name name*}))))
                                       (sort-by (comp str/lower-case :name))
                                       vec)
        manufacturers-fallback* (->> (if (seq manufacturers-data) manufacturers-data template-manufacturers)
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

        available-suppliers* (normalize-report-options (:suppliers report-filter-options))
        available-categories* (normalize-report-options (:categories report-filter-options))
        available-subcategories* (normalize-report-options (:subcategories report-filter-options))
        available-expense-categories* (normalize-report-options (or (:expense-categories report-filter-options)
                                                                  (:expense_categories report-filter-options)))
        available-manufacturers* (normalize-report-options (:manufacturers report-filter-options))

        suppliers* (if filter-options-ready?
                     (merge-options-with-selected available-suppliers* suppliers-fallback* selected-supplier-ids)
                     suppliers-fallback*)
        categories* (if filter-options-ready?
                      (merge-options-with-selected available-categories* categories-fallback* selected-category-ids)
                      categories-fallback*)
        subcategories* (if filter-options-ready?
                         (merge-options-with-selected available-subcategories* subcategories-fallback* selected-subcategory-ids)
                         subcategories-fallback*)
        expense-categories* (if filter-options-ready?
                              (merge-options-with-selected available-expense-categories* expense-categories-fallback* selected-expense-category-ids)
                              expense-categories-fallback*)
        manufacturers* (if filter-options-ready?
                         (merge-options-with-selected available-manufacturers* manufacturers-fallback* selected-manufacturer-ids)
                         manufacturers-fallback*)
        supplier-name-by-id (into {} (map (juxt :id :name) suppliers*))
        selected-supplier-name (cond
                                 (> (count selected-supplier-ids) 1) (t :expense-reports/n-suppliers (count selected-supplier-ids))
                                 selected-supplier-id (or (get supplier-name-by-id (some-> selected-supplier-id str))
                                                        (:supplier-name supplier-deep-dive)
                                                        (t :expense-reports/selected-supplier))
                                 :else (t :expense-reports/all-suppliers-scope))

        month-options* (report-utils/month-options by-month month-a month-b)

        day-pattern (report-utils/aggregate-day-pattern day-of-week-data)
        day-pattern-max (apply max (cons 0 (map #(or (report-utils/->number (:total_amount %)) 0) day-pattern)))

        top-items-filtered (report-utils/filter-top-items-by-bucket top-items-data selected-bucket-key)
        top-items-visible (vec (take 20 (report-utils/sort-rows-by-config top-items-filtered top-items-sort)))

        monthly-by-currency-rows (or (:by_currency monthly-comparison) [])
        monthly-by-currency (report-utils/sort-rows-by-config monthly-by-currency-rows monthly-currency-sort)
        monthly-by-supplier (or (:by_supplier monthly-comparison) [])

        size-buckets (report-utils/aggregate-size-buckets size-distribution-data)
        size-buckets-max (apply max (cons 0 (map #(or (report-utils/->number (:total_amount %)) 0) size-buckets)))

        heatmap-rows-raw (report-utils/aggregate-heatmap daily-heatmap-data)
        heatmap-rows (if selected-day-of-week
                       (->> heatmap-rows-raw
                         (filter #(= selected-day-of-week (:iso_day_of_week %)))
                         vec)
                       heatmap-rows-raw)
        heatmap-visible (->> heatmap-rows (take-last 84) vec)
        heatmap-max (apply max (cons 0 (map #(or (report-utils/->number (:total_amount %)) 0) heatmap-visible)))
        selected-heatmap-day (first (filter #(= selected-day (:day %)) heatmap-visible))

        category-rows-raw (report-utils/aggregate-category-allocation category-allocation-data)
        category-rows (->> category-rows-raw
                        (filter (fn [row]
                                  (if show-uncategorized?
                                    true
                                    (not= "uncategorized" (:category_key row)))))
                        vec)
        category-subcategories-by-category-raw (report-utils/aggregate-subcategory-allocation category-allocation-data)
        category-subcategories-by-category (if show-uncategorized?
                                             category-subcategories-by-category-raw
                                             (dissoc category-subcategories-by-category-raw "uncategorized"))
        category-max (apply max (cons 0 (map #(or (report-utils/->number (:total_amount %)) 0) category-rows)))

        deep-dive-summary (or (:summary supplier-deep-dive) [])
        deep-dive-trend-rows (or (:trend supplier-deep-dive) [])
        deep-dive-trend (report-utils/sort-rows-by-config deep-dive-trend-rows trend-sort)
        deep-dive-top-aliases-rows (or (:top-aliases supplier-deep-dive) [])
        deep-dive-top-aliases (report-utils/sort-rows-by-config deep-dive-top-aliases-rows alias-sort)

        currency-totals (or (:currency-totals summary) {})
        primary-currency-key (or
                               (some #(when (contains? currency-totals %) %) [:BAM :USD :EUR "BAM" "USD" "EUR"])
                               (first (keys currency-totals)))
        primary-currency (if (keyword? primary-currency-key)
                           (name primary-currency-key)
                           (some-> primary-currency-key str))
        total-sum (reduce + 0 (map #(or (report-utils/->number %) 0) (vals currency-totals)))
        total-expenses (or (report-utils/->number (:total-expenses summary)) 0)
        average-spend (when (pos? total-expenses) (/ total-sum total-expenses))]

    ($ :div {:class "min-h-screen bg-gradient-to-b from-base-100 via-primary/5 to-secondary/10 pb-12"}
      ($ :header {:class "bg-white/80 backdrop-blur-xl border-b border-base-200 sticky top-0 z-30 shadow-sm transition-all"}
        ($ :div {:class "max-w-7xl mx-auto px-4 sm:px-6 py-4"}
          ($ :div {:class "flex flex-col xl:flex-row xl:items-start xl:justify-between gap-6"}
            ($ :div {:class "flex-shrink-0 space-y-1"}
              ($ :div {:class "flex items-center gap-2 mb-1"}
                ($ :span {:class "w-2 h-2 rounded-full bg-primary animate-pulse"})
                ($ :span {:class "text-xs font-bold text-primary uppercase tracking-widest"} (t :expense-reports/analytics-badge)))
              ($ :h1 {:class "text-2xl sm:text-3xl font-black text-base-content tracking-tight"} (t :expense-reports/title))
              ($ :p {:class "text-sm text-base-content/60 max-w-md font-medium"}
                (t :expense-reports/subtitle)))

            ($ :div {:class "w-full rounded-2xl border border-primary/20 bg-gradient-to-br from-white via-primary/5 to-secondary/10 p-4 sm:p-6 space-y-4 shadow-sm backdrop-blur-sm"}
              ($ :div {:class "flex items-center justify-between mb-3"}
                ($ :div {:class "flex items-center gap-2"}
                  ($ :span {:class "text-xs font-extrabold uppercase text-base-content/40 tracking-wider"} (t :expense-reports/global-filters))
                  (when has-global-filters?
                    ($ :span {:class "ds-badge bg-primary/10 text-primary border-primary/20 font-bold ds-badge-xs uppercase tracking-wide"} (t :expense-reports/active))))

                ($ :div {:class "flex items-center gap-2"}
                  ($ button {:id "btn-reports-refresh"
                             :btn-type :ghost
                             :size :sm
                             :class "text-base-content/60 hover:text-primary hover:bg-primary/5 font-medium transition-colors"
                             :on-click #(rf/dispatch [:user-expenses/reports-refresh])}
                    (t :expense-reports/refresh-data))

                  (when has-global-filters?
                    ($ button {:id "btn-reports-clear-local-filters"
                               :btn-type :ghost
                               :size :sm
                               :class "text-error/70 hover:text-error hover:bg-error/10 font-medium transition-colors"
                               :on-click #(rf/dispatch [:user-expenses/reports-clear-local-filters])}
                      (t :expense-reports/clear-filters)))))

              ($ :div {:class "grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-3"}
                ($ :div {:class "space-y-1.5 col-span-1 sm:col-span-2 lg:col-span-1 xl:col-span-1"}
                  ($ :label {:class "text-[10px] font-bold text-base-content/50 uppercase tracking-widest px-0.5" :for "reports-filter-months-back"} (t :expense-reports/filter-time-range))
                  ($ :select {:id "reports-filter-months-back"
                              :class "ds-select ds-select-sm ds-select-bordered w-full bg-white font-medium focus:border-primary focus:ring-2 focus:ring-primary/20 transition-all rounded-lg text-xs shadow-sm"
                              :value months-back
                              :on-change #(rf/dispatch [:user-expenses/reports-set-filter
                                                        :months-back
                                                        (js/parseInt (.. % -target -value) 10)])}
                    ($ :option {:value 3} (t :expense-reports/time-3mo))
                    ($ :option {:value 6} (t :expense-reports/time-6mo))
                    ($ :option {:value 12} (t :expense-reports/time-12mo))
                    ($ :option {:value 24} (t :expense-reports/time-24mo))))

                ($ :div {:class "space-y-1.5"}
                  ($ :label {:class "text-[10px] font-bold text-base-content/50 uppercase tracking-widest px-0.5"
                             :for "reports-filter-supplier-toggle"}
                    (t :expense-reports/filter-supplier))
                  ($ report-multi-select {:id "reports-filter-supplier"
                                          :field-label (t :expense-reports/filter-supplier)
                                          :all-label (t :expense-reports/all-suppliers)
                                          :options suppliers*
                                          :selected-ids selected-supplier-ids
                                          :on-change #(rf/dispatch [:user-expenses/reports-set-filter
                                                                    :supplier-id
                                                                    %])}))

                ($ :div {:class "space-y-1.5"}
                  ($ :label {:class "text-[10px] font-bold text-base-content/50 uppercase tracking-widest px-0.5"
                             :for "reports-filter-category-toggle"}
                    (t :expense-reports/filter-category))
                  ($ report-multi-select {:id "reports-filter-category"
                                          :field-label (t :expense-reports/filter-category)
                                          :all-label (t :expense-reports/all-categories)
                                          :options categories*
                                          :selected-ids selected-category-ids
                                          :on-change #(rf/dispatch [:user-expenses/reports-set-filter
                                                                    :category-id
                                                                    %])}))

                ($ :div {:class "space-y-1.5"}
                  ($ :label {:class "text-[10px] font-bold text-base-content/50 uppercase tracking-widest px-0.5"
                             :for "reports-filter-subcategory-toggle"}
                    (t :expense-reports/filter-subcategory))
                  ($ report-multi-select {:id "reports-filter-subcategory"
                                          :field-label (t :expense-reports/filter-subcategory)
                                          :all-label (t :expense-reports/all-subcategories)
                                          :options subcategories*
                                          :selected-ids selected-subcategory-ids
                                          :on-change #(rf/dispatch [:user-expenses/reports-set-filter
                                                                    :subcategory-id
                                                                    %])}))

                ($ :div {:class "space-y-1.5"}
                  ($ :label {:class "text-[10px] font-bold text-base-content/50 uppercase tracking-widest px-0.5"
                             :for "reports-filter-expense-category-toggle"}
                    (t :expense-reports/filter-expense-type))
                  ($ report-multi-select {:id "reports-filter-expense-category"
                                          :field-label (t :expense-reports/filter-expense-type)
                                          :all-label (t :expense-reports/all-expense-types)
                                          :options expense-categories*
                                          :selected-ids selected-expense-category-ids
                                          :on-change #(rf/dispatch [:user-expenses/reports-set-filter
                                                                    :expense-category-id
                                                                    %])}))

                ($ :div {:class "space-y-1.5"}
                  ($ :label {:class "text-[10px] font-bold text-base-content/50 uppercase tracking-widest px-0.5"
                             :for "reports-filter-manufacturer-toggle"}
                    (t :expense-reports/filter-manufacturer))
                  ($ report-multi-select {:id "reports-filter-manufacturer"
                                          :field-label (t :expense-reports/filter-manufacturer)
                                          :all-label (t :expense-reports/all-manufacturers)
                                          :options manufacturers*
                                          :selected-ids selected-manufacturer-ids
                                          :on-change #(rf/dispatch [:user-expenses/reports-set-filter
                                                                    :manufacturer-id
                                                                    %])})))))))

      ($ :main {:class "max-w-7xl mx-auto px-4 sm:px-6 py-8 space-y-8"}
        ($ :div {:class "grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6"}
          ($ stat-card {:title (t :expense-reports/stat-total-spent)
                        :value (report-utils/format-money total-sum primary-currency)
                        :subtitle (t :expense-reports/stat-total-desc)
                        :icon "💰"
                        :loading? summary-loading?})
          ($ stat-card {:title (t :expense-reports/stat-volume)
                        :value (report-utils/format-int total-expenses)
                        :subtitle (t :expense-reports/stat-volume-desc)
                        :icon "📋"
                        :loading? summary-loading?})
          ($ stat-card {:title (t :expense-reports/stat-average)
                        :value (if average-spend (report-utils/format-money average-spend primary-currency) "—")
                        :subtitle (t :expense-reports/stat-average-desc)
                        :icon "📊"
                        :loading? summary-loading?})
          ($ stat-card {:title (t :expense-reports/stat-scope)
                        :value selected-supplier-name
                        :subtitle (t :expense-reports/stat-scope-desc)
                        :icon "🏪"
                        :loading? false}))

        ($ report-tabs {:active-report-tab active-report-tab
                        :set-active-report-tab! set-active-report-tab!})

        (case active-report-tab
          :expenses ($ expenses-tab {:day-of-week-loading? day-of-week-loading?
                                     :day-of-week-error day-of-week-error
                                     :day-pattern day-pattern
                                     :selected-day-of-week selected-day-of-week
                                     :day-pattern-max day-pattern-max
                                     :primary-currency primary-currency
                                     :monthly-comparison-loading? monthly-comparison-loading?
                                     :monthly-comparison-error monthly-comparison-error
                                     :month-a month-a
                                     :month-b month-b
                                     :month-options* month-options*
                                     :monthly-by-currency monthly-by-currency
                                     :monthly-by-supplier monthly-by-supplier
                                     :size-distribution-loading? size-distribution-loading?
                                     :size-distribution-error size-distribution-error
                                     :size-buckets size-buckets
                                     :selected-bucket-key selected-bucket-key
                                     :size-buckets-max size-buckets-max
                                     :daily-heatmap-loading? daily-heatmap-loading?
                                     :daily-heatmap-error daily-heatmap-error
                                     :heatmap-visible heatmap-visible
                                     :heatmap-max heatmap-max
                                     :selected-day selected-day
                                     :selected-heatmap-day selected-heatmap-day})

          :articles ($ articles-tab {:selected-bucket-key selected-bucket-key
                                     :top-items-loading? top-items-loading?
                                     :top-items-error top-items-error
                                     :top-items-visible top-items-visible
                                     :top-items-sort top-items-sort
                                     :set-top-items-sort! set-top-items-sort!
                                     :expanded-top-item-alias-id expanded-top-item-alias-id
                                     :top-item-breakdown top-item-breakdown
                                     :top-item-breakdown-loading? top-item-breakdown-loading?
                                     :top-item-breakdown-error top-item-breakdown-error})

          :providers-stores ($ providers-stores-tab {:selected-supplier-id selected-supplier-id
                                                     :expanded-supplier-id expanded-supplier-id
                                                     :supplier-deep-dive-loading? supplier-deep-dive-loading?
                                                     :supplier-deep-dive-error supplier-deep-dive-error
                                                     :deep-dive-summary deep-dive-summary
                                                     :deep-dive-trend deep-dive-trend
                                                     :trend-sort trend-sort
                                                     :set-trend-sort! set-trend-sort!
                                                     :deep-dive-top-aliases deep-dive-top-aliases
                                                     :alias-sort alias-sort
                                                     :set-alias-sort! set-alias-sort!
                                                     :top-suppliers-data top-suppliers-data
                                                     :top-suppliers-loading? top-suppliers-loading?
                                                     :top-suppliers-error top-suppliers-error
                                                     :supplier-stores-data supplier-stores-data
                                                     :supplier-stores-loading? supplier-stores-loading?
                                                     :supplier-stores-error supplier-stores-error
                                                     :supplier-monthly-trends-data supplier-monthly-trends-data
                                                     :supplier-monthly-trends-loading? supplier-monthly-trends-loading?
                                                     :supplier-monthly-trends-error supplier-monthly-trends-error})

          :categories ($ categories-tab {:category-allocation-loading? category-allocation-loading?
                                         :category-allocation-error category-allocation-error
                                         :show-uncategorized? show-uncategorized?
                                         :category-rows category-rows
                                         :selected-category-key selected-category-key
                                         :subcategories-by-category category-subcategories-by-category
                                         :category-max category-max
                                         :primary-currency primary-currency})

          nil)))))
