(ns app.domain.frontend.expenses.pages.user.expense-reports.sections.expenses
  (:require
    [app.domain.frontend.expenses.pages.user.expense-reports.components :refer [section-shell]]
    [app.domain.frontend.expenses.pages.user.expense-reports.utils :refer [->number
                                                                           format-amount-only
                                                                           format-int
                                                                           format-money
                                                                           format-percent
                                                                           heat-intensity-class
                                                                           month-label]]
    [app.template.frontend.components.button :refer [button]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui]]))

(defui expenses-tab
  [{:keys [day-of-week-loading?
           day-of-week-error
           day-pattern
           selected-day-of-week
           day-pattern-max
           primary-currency
           monthly-comparison-loading?
           monthly-comparison-error
           month-a
           month-b
           month-options*
           monthly-by-currency
           monthly-by-supplier
           size-distribution-loading?
           size-distribution-error
           size-buckets
           selected-bucket-key
           size-buckets-max
           daily-heatmap-loading?
           daily-heatmap-error
           heatmap-visible
           heatmap-max
           selected-day
           selected-heatmap-day]}]
  ($ :div {:class "grid grid-cols-1 xl:grid-cols-2 gap-8 items-start"}
    ($ :div {:class "space-y-8"}
      ($ section-shell {:title "Spending by Day"
                        :subtitle "Weekly distribution analysis"
                        :loading? day-of-week-loading?
                        :error day-of-week-error}
        (if (seq day-pattern)
          ($ :div {:class "space-y-4"}
            ($ :p {:class "text-xs text-base-content/70 bg-gradient-to-r from-primary/10 to-secondary/10 border border-primary/20 p-3 rounded-lg flex items-center gap-2"}
              ($ :span "ℹ️")
              "Click any row to filter the Calendar Heatmap in this tab by that day of the week.")

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
            ($ :p {:class "text-base-content/60"} "No weekly pattern data available."))))

      ($ section-shell {:title "Expense Sizes"
                        :subtitle "Distribution by transaction amount"
                        :loading? size-distribution-loading?
                        :error size-distribution-error}
        ($ :div {:class "space-y-4"}
          ($ :p {:class "text-xs text-base-content/70 bg-gradient-to-r from-primary/10 to-secondary/10 border border-primary/20 p-3 rounded-lg flex items-center gap-2"}
            ($ :span "ℹ️")
            "Click any bucket to filter the Top Global Items report in the Articles tab by amount.")

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
              "No size distribution data available.")))))

    ($ :div {:class "space-y-8"}
      ($ section-shell {:title "Monthly Comparison"
                        :subtitle "Compare spending between two specific months"
                        :loading? monthly-comparison-loading?
                        :error monthly-comparison-error}
        ($ :div {:class "space-y-6"}
          ($ :div {:class "bg-gradient-to-r from-primary/10 to-secondary/10 p-4 rounded-xl border border-primary/20 flex flex-wrap gap-4 items-end"}
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
                (take 6 monthly-by-supplier))))))

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
                (map-indexed
                  (fn [idx row]
                    (let [total (or (->number (:total_amount row)) 0)
                          ratio (if (pos? heatmap-max) (/ total heatmap-max) 0)
                          selected? (= selected-day (:day row))
                          month-key (subs (:day row) 0 7)
                          prev-month-key (when (pos? idx)
                                           (some-> (nth heatmap-visible (dec idx))
                                             :day
                                             (subs 0 7)))
                          show-month? (or (zero? idx) (not= month-key prev-month-key))
                          month-short (get {"01" "Jan" "02" "Feb" "03" "Mar" "04" "Apr"
                                            "05" "May" "06" "Jun" "07" "Jul" "08" "Aug"
                                            "09" "Sep" "10" "Oct" "11" "Nov" "12" "Dec"}
                                        (subs month-key 5 7)
                                        month-key)]
                      ($ :button {:id (str "btn-heatmap-day-" (:day row))
                                  :key (str "heat-" (:day row))
                                  :class (str "aspect-square rounded md:rounded-lg text-[10px] md:text-xs font-bold transition-all duration-200 relative group "
                                           (heat-intensity-class ratio)
                                           " "
                                           (if selected? "ring-2 ring-offset-2 ring-primary z-10 scale-110 shadow-lg" "hover:scale-105"))
                                  :title (str (:day row) " · " (format-money total primary-currency)
                                           " · " (format-int (:expense_count row)) " expenses")
                                  :on-click #(rf/dispatch [:user-expenses/reports-toggle-selected-day (:day row)])}
                        (when show-month?
                          ($ :span {:class "absolute top-1 left-1 rounded bg-base-100/80 px-1 leading-tight text-[9px] md:text-[10px] font-semibold uppercase tracking-wide text-base-content/70 backdrop-blur-sm"}
                            month-short))
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
              ($ :p {:class "text-base-content/60"} "No activity data in this range."))))))))