(ns app.domain.frontend.expenses.pages.user.expense-reports.sections.categories
  (:require
    [app.domain.frontend.expenses.pages.user.expense-reports.components :refer [section-shell]]
    [app.domain.frontend.expenses.pages.user.expense-reports.utils :refer [format-amount-only
                                                                           format-int
                                                                           format-money
                                                                           format-percent
                                                                           ->number]]
    [app.template.frontend.components.button :refer [button]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui]]))

(defui categories-tab
  [{:keys [category-allocation-loading?
           category-allocation-error
           show-uncategorized?
           category-rows
           selected-category-key
           subcategories-by-category
           category-max
           primary-currency]}]
  (let [selected-category-name (or
                                 (some (fn [row]
                                         (when (= selected-category-key (:category_key row))
                                           (or (:category_name row) "Uncategorized")))
                                   category-rows)
                                 selected-category-key)
        selected-subcategories (or (get subcategories-by-category selected-category-key) [])]
    ($ :div {:class "grid grid-cols-1 mb-12"}
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
              ($ :div {:class "bg-primary/5 border border-primary/20 rounded-lg p-3 flex items-center justify-between text-sm mb-2 gap-3"}
                ($ :div {:class "space-y-1"}
                  ($ :span {:class "block"}
                    (str "Expanded category: " selected-category-name))
                  ($ :span {:class "block text-xs text-base-content/60"}
                    (str (format-int (count selected-subcategories)) " subcategories")))
                ($ button {:btn-type :ghost :size :xs :class "text-primary"
                           :on-click #(rf/dispatch [:user-expenses/reports-toggle-category nil])}
                  "Clear selection")))

            (mapv
              (fn [row]
                (let [category-key (:category_key row)
                      selected? (= selected-category-key category-key)
                      category-subcategories (or (get subcategories-by-category category-key) [])
                      total (or (->number (:total_amount row)) 0)
                      ratio (if (pos? category-max) (/ total category-max) 0)]
                  ($ :div {:key (str "category-" category-key)
                           :class "space-y-2"}
                    ($ :button {:id (str "btn-category-filter-" category-key)
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
                                 :style {:width (str (* 100 ratio) "%")}})))

                    (when selected?
                      ($ :div {:class "ml-4 rounded-lg border border-primary/15 bg-primary/5 p-3"}
                        ($ :div {:class "text-xs font-semibold uppercase tracking-wide text-primary/80 mb-2"}
                          "Subcategories")
                        (if (seq category-subcategories)
                          ($ :div {:class "flex flex-wrap gap-2"}
                            (mapv (fn [{:keys [id name total_amount]}]
                                    ($ :span {:id (str "subcategory-badge-spend-" id)
                                              :key (str "subcategory-badge-spend-" id)
                                              :class "inline-flex items-center gap-1.5 rounded-md bg-white border border-primary/20 px-2.5 py-1 text-xs"}
                                      ($ :span {:class "text-base-content"} name)
                                      ($ :span {:class "text-base-content/50 font-mono"}
                                        (if (seq (str primary-currency))
                                          (str primary-currency " ")
                                          ""))
                                      ($ :span {:class "font-mono font-semibold text-base-content"}
                                        (format-amount-only total_amount))))
                              category-subcategories))
                          ($ :p {:class "text-xs text-base-content/60"}
                            "No subcategories found for this category.")))))))
              category-rows))
          ($ :p {:class "text-center p-8 bg-base-50 rounded-xl text-base-content/60"}
            "No category data available."))))))
