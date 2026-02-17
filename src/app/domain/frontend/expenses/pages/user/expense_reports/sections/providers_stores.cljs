(ns app.domain.frontend.expenses.pages.user.expense-reports.sections.providers-stores
  (:require
    [app.domain.frontend.expenses.pages.user.expense-reports.components :refer [section-shell]]
    [app.domain.frontend.expenses.pages.user.expense-reports.utils :refer [format-amount-only
                                                                           format-int
                                                                           month-label
                                                                           sortable-column-label
                                                                           toggle-sort-config]]
    [clojure.string :as str]
    [uix.core :refer [$ defui]]))

(defui providers-stores-tab
  [{:keys [selected-supplier-id
           supplier-deep-dive-loading?
           supplier-deep-dive-error
           deep-dive-summary
           deep-dive-trend
           trend-sort
           set-trend-sort!
           deep-dive-top-aliases
           alias-sort
           set-alias-sort!]}]
  ($ :div {:class "grid grid-cols-1"}
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
                        ($ :tr {:key (str "alias-" (or (:alias_id row) (:alias_label row)) "-" (:currency row))
                                :class "hover group"}
                          ($ :td {:class "text-xs text-base-content group-hover:text-primary transition-colors"}
                            (or (:article_canonical_name row) "—"))
                          ($ :td {:class "text-xs text-base-content/60"} (str (:currency row)))
                          ($ :td {:class "text-right font-mono text-sm"}
                            (format-amount-only (:total_amount row)))
                          ($ :td {:class "text-right text-xs text-base-content/60"}
                            (format-int (:line_count row)))))
                      deep-dive-top-aliases))))
              ($ :p {:class "text-sm text-base-content/60 italic p-4 bg-base-50 rounded-lg text-center"}
                "No alias details found."))))))))