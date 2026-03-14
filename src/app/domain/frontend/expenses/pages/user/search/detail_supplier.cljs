(ns app.domain.frontend.expenses.pages.user.search.detail-supplier
  "Supplier-specific detail body with stores list and articles table."
  (:require
    [app.domain.frontend.expenses.pages.user.search.helpers :as h]
    [uix.core :refer [$ defui use-state]]))

(defui supplier-detail-body [{:keys [related related-loading? t]}]
  (let [[[sort-col sort-dir] set-sort!] (use-state [:total_bam :desc])
        stores (:stores related)
        articles (:articles related)
        ;; Sort articles
        sorted-articles (let [key-fn (case sort-col
                                       :canonical_name :canonical_name
                                       :last_price :last_price
                                       :total_qty :total_qty
                                       :total_bam :total_bam
                                       :total_bam)
                              cmp    (fn [a b]
                                       (let [va (get a key-fn)
                                             vb (get b key-fn)]
                                         (cond
                                           (and (number? va) (number? vb)) (compare va vb)
                                           (and (string? va) (string? vb)) (.localeCompare va vb)
                                           (nil? va) -1
                                           (nil? vb) 1
                                           :else (compare (str va) (str vb)))))
                              sorted (sort cmp articles)]
                          (if (= sort-dir :desc) (reverse sorted) sorted))
        toggle-sort (fn [col]
                      (if (= col sort-col)
                        (set-sort! [col (if (= sort-dir :asc) :desc :asc)])
                        (set-sort! [col (if (= col :canonical_name) :asc :desc)])))]
    ($ :div {:class "space-y-4"}
      ;; Loading spinner
      (when related-loading?
        ($ :div {:class "flex items-center gap-2 text-xs text-base-content/40 pt-2"}
          ($ :span {:class "ds-loading ds-loading-spinner ds-loading-xs"})
          (t :search/related-loading)))

      ;; Stores list
      (when (and (not related-loading?) (seq stores))
        ($ :div {:class "pt-2"}
          ($ :p {:class "text-sm font-semibold uppercase tracking-wide text-base-content/50 mb-2"}
            (str (t :search/supplier-stores) " (" (count stores) ")"))
          ($ :div {:class "space-y-1.5"}
            (for [store stores]
              ($ :div {:key (str (:id store))
                       :class "flex items-center justify-between px-3 py-2 rounded-lg border border-base-300 bg-base-100"}
                ($ :div {:class "min-w-0 flex-1"}
                  ($ :p {:class "text-sm font-medium truncate"} (or (:display_name store) "\u2014"))
                  (when (:address store)
                    ($ :p {:class "text-xs text-base-content/60 truncate mt-0.5"} (:address store))))
                ($ :p {:class "text-sm font-semibold flex-shrink-0 pl-3"}
                  (h/format-amount (:total_spendings store) "BAM")))))))

      ;; No stores
      (when (and (not related-loading?) (empty? stores))
        ($ :p {:class "text-sm text-base-content/40 pt-2"} (t :search/supplier-no-stores)))

      ;; Articles table
      (when (and (not related-loading?) (seq articles))
        ($ :div {:class "pt-2"}
          ($ :p {:class "text-sm font-semibold uppercase tracking-wide text-base-content/50 mb-2"}
            (str (t :search/supplier-articles) " (" (count articles) ")"))
          ($ :div {:class "max-h-[360px] overflow-y-auto overflow-x-auto"}
            ($ :table {:class "w-full text-sm"}
              ($ :thead {:class "sticky top-0 bg-base-100"}
                ($ :tr {:class "border-b border-base-300 text-base-content/50"}
                  (h/sort-header {:label (t :search/store-article-name) :col :canonical_name
                                  :sort-col sort-col :sort-dir sort-dir :on-sort toggle-sort
                                  :class "text-left py-1.5 pr-2 font-medium"})
                  (h/sort-header {:label (t :search/store-last-price) :col :last_price
                                  :sort-col sort-col :sort-dir sort-dir :on-sort toggle-sort
                                  :class "text-right py-1.5 px-1 font-medium"})
                  (h/sort-header {:label (t :search/store-total-qty) :col :total_qty
                                  :sort-col sort-col :sort-dir sort-dir :on-sort toggle-sort
                                  :class "text-right py-1.5 px-1 font-medium"})
                  (h/sort-header {:label (t :search/store-total-bam) :col :total_bam
                                  :sort-col sort-col :sort-dir sort-dir :on-sort toggle-sort
                                  :class "text-right py-1.5 pl-1 font-medium"})))
              ($ :tbody
                (for [art sorted-articles]
                  ($ :tr {:key (str (:id art))
                          :class "border-b border-base-200/50 hover:bg-base-200/30"}
                    ($ :td {:class "py-1.5 pr-2"} (or (:canonical_name art) "\u2014"))
                    ($ :td {:class "py-1.5 px-1 text-right font-medium"}
                      (h/format-amount (:last_price art) "BAM"))
                    ($ :td {:class "py-1.5 px-1 text-right"} (some-> (:total_qty art) str))
                    ($ :td {:class "py-1.5 pl-1 text-right font-semibold"}
                      (h/format-amount (:total_bam art) "BAM")))))))))

      ;; No articles
      (when (and (not related-loading?) (empty? articles))
        ($ :p {:class "text-sm text-base-content/40 pt-2"} (t :search/supplier-no-articles))))))
