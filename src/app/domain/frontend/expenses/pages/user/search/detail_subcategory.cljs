(ns app.domain.frontend.expenses.pages.user.search.detail-subcategory
  "Subcategory-specific detail body with stores filter and articles table."
  (:require
    [app.domain.frontend.expenses.pages.user.search.helpers :as h]
    [uix.core :refer [$ defui use-state]]))

(defui subcategory-detail-body [{:keys [related related-loading? t]}]
  (let [[store-filter set-store-filter!] (use-state nil)
        [[sort-col sort-dir] set-sort!] (use-state [:total_bam :desc])
        stores (:stores related)
        articles (:articles related)
        store-articles (:store-articles related)
        ;; Build per-store index
        store-art-index (reduce (fn [acc {:keys [store_id article_id]}]
                                  (update acc (str store_id) (fnil conj #{}) (str article_id)))
                          {} store-articles)
        store-art-aggs (reduce (fn [acc row]
                                 (assoc acc [(str (:store_id row)) (str (:article_id row))]
                                   {:total_qty (:total_qty row) :total_bam (:total_bam row)}))
                         {} store-articles)
        filtered-articles (if store-filter
                            (let [art-ids (get store-art-index store-filter #{})]
                              (->> articles
                                (filter #(contains? art-ids (str (:id %))))
                                (mapv #(merge % (get store-art-aggs [store-filter (str (:id %))])))))
                            articles)
        sorted-articles (let [key-fn (case sort-col
                                       :canonical_name :canonical_name
                                       :last_price :last_price
                                       :total_qty :total_qty
                                       :total_bam :total_bam
                                       :total_bam)
                              cmp    (fn [a b]
                                       (let [va (get a key-fn) vb (get b key-fn)]
                                         (cond
                                           (and (number? va) (number? vb)) (compare va vb)
                                           (and (string? va) (string? vb)) (.localeCompare va vb)
                                           (nil? va) -1 (nil? vb) 1
                                           :else (compare (str va) (str vb)))))
                              sorted (sort cmp filtered-articles)]
                          (if (= sort-dir :desc) (reverse sorted) sorted))
        toggle-sort (fn [col]
                      (if (= col sort-col)
                        (set-sort! [col (if (= sort-dir :asc) :desc :asc)])
                        (set-sort! [col (if (= col :canonical_name) :asc :desc)])))]
    ($ :div {:class "space-y-4"}
      (when related-loading?
        ($ :div {:class "flex items-center gap-2 text-xs text-base-content/40 pt-2"}
          ($ :span {:class "ds-loading ds-loading-spinner ds-loading-xs"})
          (t :search/related-loading)))

      ;; Stores (clickable filter)
      (when (and (not related-loading?) (seq stores))
        ($ :div {:class "pt-2"}
          ($ :p {:class "text-sm font-semibold uppercase tracking-wide text-base-content/50 mb-2"}
            (str (t :search/supplier-stores) " (" (count stores) ")"))
          ($ :div {:class "flex flex-wrap gap-1.5"}
            (for [store stores]
              (let [sid (str (:id store))
                    active? (= store-filter sid)]
                ($ :button {:key sid
                            :class (str "text-sm px-3 py-1 rounded-full border transition-colors "
                                     (if active?
                                       "bg-primary text-primary-content border-primary"
                                       "bg-base-100 border-base-300 hover:bg-base-200"))
                            :title (:address store)
                            :on-click #(set-store-filter! (if active? nil sid))}
                  (:display_name store)))))))

      ;; Articles table
      (when (and (not related-loading?) (seq articles))
        ($ :div {:class "pt-2"}
          ($ :p {:class "text-sm font-semibold uppercase tracking-wide text-base-content/50 mb-2"}
            (str (t :search/subcategory-articles) " (" (count sorted-articles) ")"))
          (if (seq sorted-articles)
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
                        (h/format-amount (:total_bam art) "BAM")))))))
            ($ :p {:class "text-sm text-base-content/40"} (t :search/subcategory-no-articles)))))

      (when (and (not related-loading?) (empty? articles))
        ($ :p {:class "text-sm text-base-content/40 pt-2"} (t :search/subcategory-no-articles))))))
