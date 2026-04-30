(ns app.domain.frontend.expenses.pages.user.expense-reports.sections.expenses
  (:require
    [app.domain.frontend.expenses.pages.user.expense-reports.components :refer [section-shell]]
    [app.domain.frontend.expenses.pages.user.expense-reports.utils :refer [->number
                                                                           format-int
                                                                           format-money
                                                                           heat-intensity-class]]
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
              "Click any row to filter the Calendar Heatmap by that day of the week.")

            ($ :div {:class "space-y-3"}
              (mapv
                (fn [row]
                  (let [iso-day (:iso_day_of_week row)
                        selected? (= selected-day-of-week iso-day)
                        total (or (->number (:total_amount row)) 0)
                        ratio (if (pos? day-pattern-max) (/ total day-pattern-max) 0)]
                    ($ :button {:id (str "btn-day-filter-" (:day_key row))
                                :key (str "dow-" iso-day)
                                :class "w-full text-left group transition-all duration-200"
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

    ($ :div {:class "space-y-8"}
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
