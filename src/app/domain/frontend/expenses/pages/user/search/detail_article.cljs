(ns app.domain.frontend.expenses.pages.user.search.detail-article
  "Article-specific detail body with interactive store filtering
  and price history table."
  (:require
    [app.domain.frontend.expenses.pages.user.search.helpers :as h]
    [uix.core :refer [$ defui use-state]]))

(defui article-detail-body [{:keys [related related-loading? t]}]
  (let [[store-filter set-store-filter!] (use-state nil)
        [[sort-col sort-dir] set-sort!] (use-state [:purchased_at :desc])
        detail (:detail related)
        stats (:stats related)
        price-history (:price_history related)
        stores (:stores related)
        ;; Price variants (distinct unit prices)
        price-variants (->> price-history
                         (keep :unit_price)
                         distinct
                         sort
                         vec)
        ;; Filter by store
        filtered-history (if store-filter
                           (filter #(= (str (:store_id %)) store-filter) price-history)
                           price-history)
        ;; Sort
        sorted-history (let [key-fn (case sort-col
                                      :purchased_at :purchased_at
                                      :unit_price :unit_price
                                      :qty :qty
                                      :line_total :line_total
                                      :store_display_name :store_display_name
                                      :purchased_at)
                             sorted (sort-by (fn [row] (or (get row key-fn) "")) filtered-history)]
                         (if (= sort-dir :desc) (reverse sorted) sorted))
        toggle-sort (fn [col]
                      (if (= col sort-col)
                        (set-sort! [col (if (= sort-dir :asc) :desc :asc)])
                        (set-sort! [col (if (= col :purchased_at) :desc :asc)])))]
    ($ :div {:class "space-y-4"}
      ;; Metadata rows
      ($ :div {:class "space-y-0"}
        (h/detail-row (t :search/article-manufacturer) (:manufacturer_display_name detail))
        ;; Category > Subcategory in one row
        (let [cat (:category_name detail)
              sub (:subcategory_name detail)
              combined (cond
                         (and cat sub) (str cat " > " sub)
                         sub sub
                         cat cat
                         :else nil)]
          (h/detail-row (t :search/article-subcategory) combined))
        ;; Price variants instead of single last price
        (when (seq price-variants)
          ($ :div {:class "flex justify-between gap-4 py-2 border-b border-base-200"}
            ($ :span {:class "text-sm text-base-content/50 flex-shrink-0"}
              (t :search/article-prices))
            ($ :div {:class "flex flex-wrap gap-1.5 justify-end"}
              (for [p price-variants]
                ($ :span {:key (str p)
                          :class "text-sm font-medium bg-base-200 px-2 py-0.5 rounded"}
                  (h/format-amount p "BAM"))))))
        (h/detail-row (t :search/article-total-turnover)
          (when (and stats (pos? (:total_turnover stats)))
            (h/format-amount (:total_turnover stats) "BAM")))
        (h/detail-row (t :search/article-total-items)
          (when (and stats (pos? (:total_items stats)))
            (str (:total_items stats)))))

      ;; Loading spinner
      (when related-loading?
        ($ :div {:class "flex items-center gap-2 text-xs text-base-content/40 pt-2"}
          ($ :span {:class "ds-loading ds-loading-spinner ds-loading-xs"})
          (t :search/related-loading)))

      ;; Stores filter chips
      (when (and (not related-loading?) (seq stores))
        ($ :div {:class "pt-2"}
          ($ :p {:class "text-sm font-semibold uppercase tracking-wide text-base-content/50 mb-2"}
            (t :search/article-stores))
          ($ :div {:class "flex flex-wrap gap-1.5"}
            ;; "All" chip
            ($ :button {:class (str "text-sm px-3 py-1 rounded-full border transition-colors "
                                 (if (nil? store-filter)
                                   "bg-primary text-primary-content border-primary"
                                   "bg-base-100 border-base-300 hover:bg-base-200"))
                        :on-click #(set-store-filter! nil)}
              (t :search/article-all-stores))
            ;; Store chips with tooltip (supplier + address)
            (let [store-supplier (reduce (fn [acc row]
                                           (let [sid (str (:store_id row))]
                                             (if (and sid (not (get acc sid)))
                                               (assoc acc sid (:supplier_display_name row))
                                               acc)))
                                   {} price-history)]
              (for [store stores]
                (let [sid (str (:id store))
                      active? (= store-filter sid)
                      supplier (get store-supplier sid)
                      tooltip (str (when supplier (str supplier "\n"))
                                (:address store))]
                  ($ :button {:key sid
                              :class (str "text-sm px-3 py-1 rounded-full border transition-colors "
                                       (if active?
                                         "bg-primary text-primary-content border-primary"
                                         "bg-base-100 border-base-300 hover:bg-base-200"))
                              :title tooltip
                              :on-click #(set-store-filter! (if active? nil sid))}
                    (:display_name store))))))))

      ;; Price history table
      (when (and (not related-loading?) (seq price-history))
        ($ :div {:class "pt-2"}
          ($ :p {:class "text-sm font-semibold uppercase tracking-wide text-base-content/50 mb-2"}
            (str (t :search/article-price-history)
              (when store-filter
                (str " (" (count filtered-history) ")"))))
          (if (seq sorted-history)
            ($ :div {:class "overflow-x-auto"}
              ($ :table {:class "w-full text-sm"}
                ($ :thead
                  ($ :tr {:class "border-b border-base-300 text-base-content/50"}
                    (h/sort-header {:label (t :search/article-date) :col :purchased_at
                                    :sort-col sort-col :sort-dir sort-dir :on-sort toggle-sort
                                    :class "text-left py-1.5 pr-2 font-medium"})
                    (h/sort-header {:label (t :search/article-price) :col :unit_price
                                    :sort-col sort-col :sort-dir sort-dir :on-sort toggle-sort
                                    :class "text-right py-1.5 px-1 font-medium"})
                    (h/sort-header {:label (t :search/article-qty) :col :qty
                                    :sort-col sort-col :sort-dir sort-dir :on-sort toggle-sort
                                    :class "text-right py-1.5 px-1 font-medium"})
                    (h/sort-header {:label (t :search/article-total) :col :line_total
                                    :sort-col sort-col :sort-dir sort-dir :on-sort toggle-sort
                                    :class "text-right py-1.5 pl-1 font-medium"})
                    (when-not store-filter
                      (h/sort-header {:label (t :search/article-store) :col :store_display_name
                                      :sort-col sort-col :sort-dir sort-dir :on-sort toggle-sort
                                      :class "text-left py-1.5 pl-2 font-medium"}))))
                ($ :tbody
                  (for [row sorted-history]
                    (let [store-tip (str (when (:supplier_display_name row)
                                           (str (:supplier_display_name row) "\n"))
                                      (or (:store_address row) ""))]
                      ($ :tr {:key (str (:expense_id row))
                              :class "border-b border-base-200/50 hover:bg-base-200/30"}
                        ($ :td {:class "py-1.5 pr-2"} (h/format-date (:purchased_at row)))
                        ($ :td {:class "py-1.5 px-1 text-right font-medium"}
                          (h/format-amount (:unit_price row) "BAM"))
                        ($ :td {:class "py-1.5 px-1 text-right"} (some-> (:qty row) str))
                        ($ :td {:class "py-1.5 pl-1 text-right font-semibold"}
                          (h/format-amount (:line_total row) "BAM"))
                        (when-not store-filter
                          ($ :td {:class "py-1.5 pl-2 truncate max-w-[150px]"
                                  :title store-tip}
                            (or (:store_display_name row) "\u2014")))))))))
            ($ :p {:class "text-sm text-base-content/40"} (t :search/article-no-history)))))

      ;; No data at all
      (when (and (not related-loading?) (empty? price-history) (not (seq stores)))
        ($ :p {:class "text-sm text-base-content/40 pt-2"} (t :search/article-no-history))))))
