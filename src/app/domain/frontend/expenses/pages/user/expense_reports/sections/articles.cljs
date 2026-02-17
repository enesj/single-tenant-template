(ns app.domain.frontend.expenses.pages.user.expense-reports.sections.articles
  (:require
    [app.domain.frontend.expenses.pages.user.expense-reports.components :refer [section-shell]]
    [app.domain.frontend.expenses.pages.user.expense-reports.utils :refer [format-amount-only
                                                                           format-int
                                                                           sortable-column-label
                                                                           toggle-sort-config]]
    [app.template.frontend.components.button :refer [button]]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui]]))

(defui articles-tab
  [{:keys [selected-bucket-key
           top-items-loading?
           top-items-error
           top-items-visible
           top-items-sort
           set-top-items-sort!
           expanded-top-item-alias-id
           top-item-breakdown
           top-item-breakdown-loading?
           top-item-breakdown-error]}]
  (let [expanded-id (some-> expanded-top-item-alias-id str str/trim not-empty)]
    ($ :div {:class "grid grid-cols-1"}
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
              "Tip: Use the 'Expense Sizes' report in the Expenses tab to filter this list by expense amount."))

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
                        (sortable-column-label "Receipts" top-items-sort :line_count)))
                    ($ :th {:class "hidden md:table-cell text-right"}
                      ($ :button {:type "button"
                                  :class "font-bold hover:text-primary transition inline-flex items-center justify-end gap-1 w-full"
                                  :on-click #(set-top-items-sort! (fn [current]
                                                                    (toggle-sort-config current :store_count :number)))}
                        (sortable-column-label "Stores" top-items-sort :store_count)))
                    ($ :th {:class "hidden md:table-cell text-right"}
                      ($ :button {:type "button"
                                  :class "font-bold hover:text-primary transition inline-flex items-center justify-end gap-1 w-full"
                                  :on-click #(set-top-items-sort! (fn [current]
                                                                    (toggle-sort-config current :supplier_count :number)))}
                        (sortable-column-label "Suppliers" top-items-sort :supplier_count)))))
                ($ :tbody
                  (->> top-items-visible
                    (mapcat
                      (fn [row]
                        (let [alias-id (some-> (:alias_id row) str str/trim not-empty)
                              expanded? (and (seq alias-id)
                                          (= alias-id expanded-id))
                              article-label (or (some-> (:article_canonical_name row) str str/trim not-empty)
                                              "—")]
                          (cond->
                            [($ :tr {:key (str "top-item-" (or alias-id (:alias_label row)) "-" (:currency row))
                                     :class (str "hover " (when expanded? "bg-primary/5"))}
                               ($ :td {:class "font-medium"}
                                 (if (seq alias-id)
                                   ($ :button {:type "button"
                                               :class "w-full flex items-center gap-2 text-left hover:text-primary transition"
                                               :on-click #(rf/dispatch [:user-expenses/reports-toggle-expanded-top-item alias-id])}
                                     ($ :span {:class "text-base-content/40"} (if expanded? "▾" "▸"))
                                     ($ :span article-label))
                                   ($ :span article-label)))
                               ($ :td {:class "text-xs text-base-content/60"} (str (:currency row)))
                               ($ :td {:class "text-right font-mono font-medium"}
                                 (format-amount-only (:total_amount row)))
                               ($ :td {:class "text-right hidden sm:table-cell text-base-content/70"}
                                 (format-int (:qty_total row)))
                               ($ :td {:class "text-right text-base-content/60"}
                                 (format-int (:line_count row)))
                               ($ :td {:class "hidden md:table-cell text-right text-base-content/70"}
                                 (format-int (:store_count row)))
                               ($ :td {:class "hidden md:table-cell text-right text-base-content/70"}
                                 (format-int (:supplier_count row))))]

                            expanded?
                            (conj
                              ($ :tr {:key (str "top-item-details-" alias-id "-" (:currency row))}
                                ($ :td {:col-span 7
                                        :class "bg-base-200/20 p-4"}
                                  (cond
                                    top-item-breakdown-loading?
                                    ($ :div {:class "text-sm text-base-content/60"}
                                      "Loading suppliers & stores…")

                                    (seq top-item-breakdown-error)
                                    ($ :div {:class "text-sm text-error"}
                                      top-item-breakdown-error)

                                    :else
                                    (let [suppliers (or (:suppliers top-item-breakdown) [])
                                          stores (or (:stores top-item-breakdown) [])]
                                      ($ :div {:class "grid grid-cols-1 md:grid-cols-2 gap-4"}
                                        ($ :div {:class "rounded-lg border border-base-200 bg-white/70 p-3"}
                                          ($ :div {:class "text-xs font-bold uppercase tracking-wider text-base-content/50 mb-2"}
                                            "Suppliers")
                                          (if (seq suppliers)
                                            ($ :table {:class "ds-table ds-table-xs w-full"}
                                              ($ :thead
                                                ($ :tr
                                                  ($ :th "Supplier")
                                                  ($ :th {:class "text-right"} "Total")
                                                  ($ :th {:class "text-right"} "Qty")
                                                  ($ :th {:class "text-right"} "Receipts")))
                                              ($ :tbody
                                                (mapv
                                                  (fn [s]
                                                    ($ :tr {:key (str "top-item-supplier-" (:supplier_id s) "-" (:currency s))}
                                                      ($ :td {:class "max-w-[14rem] truncate"}
                                                        (or (some-> (:supplier_name s) str str/trim not-empty) "—"))
                                                      ($ :td {:class "text-right font-mono"}
                                                        (format-amount-only (:total_amount s)))
                                                      ($ :td {:class "text-right"}
                                                        (format-int (:qty_total s)))
                                                      ($ :td {:class "text-right"}
                                                        (format-int (:line_count s)))))
                                                  suppliers)))
                                            ($ :div {:class "text-sm text-base-content/60"}
                                              "No supplier breakdown found.")))

                                        ($ :div {:class "rounded-lg border border-base-200 bg-white/70 p-3"}
                                          ($ :div {:class "text-xs font-bold uppercase tracking-wider text-base-content/50 mb-2"}
                                            "Stores")
                                          (if (seq stores)
                                            ($ :table {:class "ds-table ds-table-xs w-full"}
                                              ($ :thead
                                                ($ :tr
                                                  ($ :th "Store")
                                                  ($ :th {:class "text-right"} "Total")
                                                  ($ :th {:class "text-right"} "Qty")
                                                  ($ :th {:class "text-right"} "Receipts")))
                                              ($ :tbody
                                                (mapv
                                                  (fn [st]
                                                    ($ :tr {:key (str "top-item-store-" (:store_id st) "-" (:currency st))}
                                                      ($ :td {:class "max-w-[14rem] truncate"}
                                                        (or (some-> (:store_name st) str str/trim not-empty) "—"))
                                                      ($ :td {:class "text-right font-mono"}
                                                        (format-amount-only (:total_amount st)))
                                                      ($ :td {:class "text-right"}
                                                        (format-int (:qty_total st)))
                                                      ($ :td {:class "text-right"}
                                                        (format-int (:line_count st)))))
                                                  stores)))
                                            ($ :div {:class "text-sm text-base-content/60"}
                                              "No store breakdown found.")))))))))))))
                    vec))))
            ($ :div {:class "flex flex-col items-center justify-center p-8 bg-base-50 rounded-xl text-center"}
              ($ :p {:class "text-base-content/60"} "No items found matching the current filters."))))))))