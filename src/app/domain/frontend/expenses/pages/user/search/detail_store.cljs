(ns app.domain.frontend.expenses.pages.user.search.detail-store
  "Store-specific detail body with articles table."
  (:require
    [app.domain.frontend.expenses.pages.user.search.helpers :as h]
    [uix.core :refer [$ defui use-state]]))

(defui store-detail-body [{:keys [related related-loading? t]}]
  (let [[[sort-col sort-dir] set-sort!] (use-state [:total_bam :desc])
        detail (:detail related)
        articles (:articles related)
        ;; Sort
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
      ;; Store metadata rows
      ($ :div {:class "space-y-0"}
        (h/detail-row (t :search/detail-supplier) (:supplier_display_name detail))
        (h/detail-row (t :search/store-city) (:city_name detail))
        (h/detail-row (t :search/detail-address) (:address detail)))

      ;; Loading spinner
      (when related-loading?
        ($ :div {:class "flex items-center gap-2 text-xs text-base-content/40 pt-2"}
          ($ :span {:class "ds-loading ds-loading-spinner ds-loading-xs"})
          (t :search/related-loading)))

      ;; Articles table
      (when (and (not related-loading?) (seq articles))
        ($ :div {:class "pt-2"}
          ($ :p {:class "text-sm font-semibold uppercase tracking-wide text-base-content/50 mb-2"}
            (str (t :search/store-articles) " (" (count articles) ")"))
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
        ($ :p {:class "text-sm text-base-content/40 pt-2"} (t :search/store-no-articles))))))
