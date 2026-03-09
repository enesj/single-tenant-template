(ns app.domain.frontend.expenses.pages.user.search
  "Cross-entity search page.

  Shows a search input and fans results across all entity types.
  Selecting a result opens a slide-in detail panel on the right."
  (:require
    [app.template.frontend.i18n :refer [use-t]]
    [app.template.frontend.utils.timestamp :as timestamp]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect use-ref use-state]]
    [uix.re-frame :refer [use-subscribe]]
    app.domain.frontend.expenses.subs.user-expenses))

;; ---------------------------------------------------------------------------
;; Formatting helpers
;; ---------------------------------------------------------------------------

(defn- format-amount [amount currency]
  (when amount
    (try
      (.toLocaleString (js/Number amount) "en-US"
        #js {:style "currency"
             :currency (or currency "USD")
             :minimumFractionDigits 2
             :maximumFractionDigits 2})
      (catch :default _
        (str (or currency "") " " amount)))))

(defn- format-date [s]
  (when s (timestamp/format-timestamp-string s)))

;; ---------------------------------------------------------------------------
;; Result card atoms
;; ---------------------------------------------------------------------------

(defui expense-card [{:keys [item t on-click selected?]}]
  ($ :button
    {:class (str "w-full text-left px-3 py-2.5 rounded-lg border transition-colors "
              (if selected?
                "bg-primary/10 border-primary/30"
                "bg-base-100 border-base-300 hover:bg-base-200"))
     :on-click on-click}
    ($ :div {:class "flex items-center justify-between gap-2"}
      ($ :div {:class "min-w-0 flex-1"}
        ($ :p {:class "text-sm font-medium truncate"}
          (or (:supplier_display_name item) "—"))
        ($ :p {:class "text-xs text-base-content/60 truncate mt-0.5"}
          (str (format-date (:purchased_at item))
            (when (:payer_label item) (str " · " (:payer_label item))))))
      ($ :div {:class "flex-shrink-0 text-right"}
        ($ :p {:class "text-sm font-semibold"}
          (format-amount (:total_amount item) (:currency item)))
        ($ :span {:class (str "text-xs px-1.5 py-0.5 rounded "
                           (if (:is_posted item)
                             "bg-success/10 text-success"
                             "bg-warning/10 text-warning"))}
          (if (:is_posted item) (t :search/posted) (t :search/pending)))))))

(defui receipt-card [{:keys [item t on-click selected?]}]
  ($ :button
    {:class (str "w-full text-left px-3 py-2.5 rounded-lg border transition-colors "
              (if selected?
                "bg-primary/10 border-primary/30"
                "bg-base-100 border-base-300 hover:bg-base-200"))
     :on-click on-click}
    ($ :div {:class "min-w-0"}
      ($ :p {:class "text-sm font-medium truncate"}
        (or (:original_filename item) "—"))
      ($ :p {:class "text-xs text-base-content/60 truncate mt-0.5"}
        (str (or (:supplier_guess item) "")
          (when (:store_guess item) (str " · " (:store_guess item))))))))

(defui simple-card [{:keys [label subtitle on-click selected?]}]
  ($ :button
    {:class (str "w-full text-left px-3 py-2.5 rounded-lg border transition-colors "
              (if selected?
                "bg-primary/10 border-primary/30"
                "bg-base-100 border-base-300 hover:bg-base-200"))
     :on-click on-click}
    ($ :p {:class "text-sm font-medium truncate"} (or label "—"))
    (when subtitle
      ($ :p {:class "text-xs text-base-content/60 truncate mt-0.5"} subtitle))))

;; ---------------------------------------------------------------------------
;; Result group
;; ---------------------------------------------------------------------------

(defui result-group [{:keys [title badge-class items render-item]}]
  (when (seq items)
    ($ :div {:class "mb-5"}
      ($ :div {:class "flex items-center gap-2 mb-2"}
        ($ :span {:class (str "text-xs font-semibold uppercase tracking-wide px-2 py-0.5 rounded-full " badge-class)}
          title)
        ($ :span {:class "text-xs text-base-content/40"} (count items)))
      ($ :div {:class "space-y-1.5"}
        (for [item items]
          (render-item item))))))

;; ---------------------------------------------------------------------------
;; Detail panel
;; ---------------------------------------------------------------------------

(defn- detail-row [label value]
  (when value
    ($ :div {:class "flex justify-between gap-4 py-2 border-b border-base-200 last:border-0"}
      ($ :span {:class "text-sm text-base-content/50 flex-shrink-0"} label)
      ($ :span {:class "text-sm font-medium text-right truncate"} (str value)))))

;; Sortable column header
(defn- sort-header [{:keys [label col sort-col sort-dir on-sort class]}]
  (let [active? (= col sort-col)
        arrow (if active? (if (= sort-dir :asc) " ▲" " ▼") "")]
    ($ :th {:class (str class " cursor-pointer select-none hover:text-base-content")
            :on-click #(on-sort col)}
      (str label arrow))))

;; Article-specific detail body with interactive store filtering
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
        (detail-row (t :search/article-manufacturer) (:manufacturer_display_name detail))
        ;; Category > Subcategory in one row
        (let [cat (:category_name detail)
              sub (:subcategory_name detail)
              combined (cond
                         (and cat sub) (str cat " > " sub)
                         sub sub
                         cat cat
                         :else nil)]
          (detail-row (t :search/article-subcategory) combined))
        ;; Price variants instead of single last price
        (when (seq price-variants)
          ($ :div {:class "flex justify-between gap-4 py-2 border-b border-base-200"}
            ($ :span {:class "text-sm text-base-content/50 flex-shrink-0"}
              (t :search/article-prices))
            ($ :div {:class "flex flex-wrap gap-1.5 justify-end"}
              (for [p price-variants]
                ($ :span {:key (str p)
                          :class "text-sm font-medium bg-base-200 px-2 py-0.5 rounded"}
                  (format-amount p "BAM"))))))
        (detail-row (t :search/article-total-turnover)
          (when (and stats (pos? (:total_turnover stats)))
            (format-amount (:total_turnover stats) "BAM")))
        (detail-row (t :search/article-total-items)
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
                    (sort-header {:label (t :search/article-date) :col :purchased_at
                                  :sort-col sort-col :sort-dir sort-dir :on-sort toggle-sort
                                  :class "text-left py-1.5 pr-2 font-medium"})
                    (sort-header {:label (t :search/article-price) :col :unit_price
                                  :sort-col sort-col :sort-dir sort-dir :on-sort toggle-sort
                                  :class "text-right py-1.5 px-1 font-medium"})
                    (sort-header {:label (t :search/article-qty) :col :qty
                                  :sort-col sort-col :sort-dir sort-dir :on-sort toggle-sort
                                  :class "text-right py-1.5 px-1 font-medium"})
                    (sort-header {:label (t :search/article-total) :col :line_total
                                  :sort-col sort-col :sort-dir sort-dir :on-sort toggle-sort
                                  :class "text-right py-1.5 pl-1 font-medium"})
                    (when-not store-filter
                      (sort-header {:label (t :search/article-store) :col :store_display_name
                                    :sort-col sort-col :sort-dir sort-dir :on-sort toggle-sort
                                    :class "text-left py-1.5 pl-2 font-medium"}))))
                ($ :tbody
                  (for [row sorted-history]
                    (let [store-tip (str (when (:supplier_display_name row)
                                           (str (:supplier_display_name row) "\n"))
                                      (or (:store_address row) ""))]
                      ($ :tr {:key (str (:expense_id row))
                              :class "border-b border-base-200/50 hover:bg-base-200/30"}
                        ($ :td {:class "py-1.5 pr-2"} (format-date (:purchased_at row)))
                        ($ :td {:class "py-1.5 px-1 text-right font-medium"}
                          (format-amount (:unit_price row) "BAM"))
                        ($ :td {:class "py-1.5 px-1 text-right"} (some-> (:qty row) str))
                        ($ :td {:class "py-1.5 pl-1 text-right font-semibold"}
                          (format-amount (:line_total row) "BAM"))
                        (when-not store-filter
                          ($ :td {:class "py-1.5 pl-2 truncate max-w-[150px]"
                                  :title store-tip}
                            (or (:store_display_name row) "—")))))))))
            ($ :p {:class "text-sm text-base-content/40"} (t :search/article-no-history)))))

      ;; No data at all
      (when (and (not related-loading?) (empty? price-history) (not (seq stores)))
        ($ :p {:class "text-sm text-base-content/40 pt-2"} (t :search/article-no-history))))))

;; Store-specific detail body with articles table
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
        (detail-row (t :search/detail-supplier) (:supplier_display_name detail))
        (detail-row (t :search/store-city) (:city_name detail))
        (detail-row (t :search/detail-address) (:address detail)))

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
                  (sort-header {:label (t :search/store-article-name) :col :canonical_name
                                :sort-col sort-col :sort-dir sort-dir :on-sort toggle-sort
                                :class "text-left py-1.5 pr-2 font-medium"})
                  (sort-header {:label (t :search/store-last-price) :col :last_price
                                :sort-col sort-col :sort-dir sort-dir :on-sort toggle-sort
                                :class "text-right py-1.5 px-1 font-medium"})
                  (sort-header {:label (t :search/store-total-qty) :col :total_qty
                                :sort-col sort-col :sort-dir sort-dir :on-sort toggle-sort
                                :class "text-right py-1.5 px-1 font-medium"})
                  (sort-header {:label (t :search/store-total-bam) :col :total_bam
                                :sort-col sort-col :sort-dir sort-dir :on-sort toggle-sort
                                :class "text-right py-1.5 pl-1 font-medium"})))
              ($ :tbody
                (for [art sorted-articles]
                  ($ :tr {:key (str (:id art))
                          :class "border-b border-base-200/50 hover:bg-base-200/30"}
                    ($ :td {:class "py-1.5 pr-2"} (or (:canonical_name art) "\u2014"))
                    ($ :td {:class "py-1.5 px-1 text-right font-medium"}
                      (format-amount (:last_price art) "BAM"))
                    ($ :td {:class "py-1.5 px-1 text-right"} (some-> (:total_qty art) str))
                    ($ :td {:class "py-1.5 pl-1 text-right font-semibold"}
                      (format-amount (:total_bam art) "BAM")))))))))

      ;; No articles
      (when (and (not related-loading?) (empty? articles))
        ($ :p {:class "text-sm text-base-content/40 pt-2"} (t :search/store-no-articles))))))

;; Supplier-specific detail body with stores list + articles table
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
                  (format-amount (:total_spendings store) "BAM")))))))

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
                  (sort-header {:label (t :search/store-article-name) :col :canonical_name
                                :sort-col sort-col :sort-dir sort-dir :on-sort toggle-sort
                                :class "text-left py-1.5 pr-2 font-medium"})
                  (sort-header {:label (t :search/store-last-price) :col :last_price
                                :sort-col sort-col :sort-dir sort-dir :on-sort toggle-sort
                                :class "text-right py-1.5 px-1 font-medium"})
                  (sort-header {:label (t :search/store-total-qty) :col :total_qty
                                :sort-col sort-col :sort-dir sort-dir :on-sort toggle-sort
                                :class "text-right py-1.5 px-1 font-medium"})
                  (sort-header {:label (t :search/store-total-bam) :col :total_bam
                                :sort-col sort-col :sort-dir sort-dir :on-sort toggle-sort
                                :class "text-right py-1.5 pl-1 font-medium"})))
              ($ :tbody
                (for [art sorted-articles]
                  ($ :tr {:key (str (:id art))
                          :class "border-b border-base-200/50 hover:bg-base-200/30"}
                    ($ :td {:class "py-1.5 pr-2"} (or (:canonical_name art) "\u2014"))
                    ($ :td {:class "py-1.5 px-1 text-right font-medium"}
                      (format-amount (:last_price art) "BAM"))
                    ($ :td {:class "py-1.5 px-1 text-right"} (some-> (:total_qty art) str))
                    ($ :td {:class "py-1.5 pl-1 text-right font-semibold"}
                      (format-amount (:total_bam art) "BAM")))))))))

      ;; No articles
      (when (and (not related-loading?) (empty? articles))
        ($ :p {:class "text-sm text-base-content/40 pt-2"} (t :search/supplier-no-articles))))))

;; Manufacturer-specific detail body with suppliers list + articles table
(defui manufacturer-detail-body [{:keys [related related-loading? t]}]
  (let [[supplier-filter set-supplier-filter!] (use-state nil)
        [[sort-col sort-dir] set-sort!] (use-state [:total_bam :desc])
        suppliers (:suppliers related)
        articles (:articles related)
        supplier-articles (:supplier-articles related)
        ;; Build per-supplier article lookup: {supplier-id #{article-id ...}}
        sup-art-index (reduce (fn [acc {:keys [supplier_id article_id]}]
                                (update acc (str supplier_id)
                                  (fnil conj #{}) (str article_id)))
                        {} supplier-articles)
        ;; Build per-supplier article aggregates: {[supplier-id article-id] {:total_qty :total_bam}}
        sup-art-aggs (reduce (fn [acc row]
                               (assoc acc [(str (:supplier_id row)) (str (:article_id row))]
                                 {:total_qty (:total_qty row)
                                  :total_bam (:total_bam row)}))
                       {} supplier-articles)
        ;; Filter articles by selected supplier
        filtered-articles (if supplier-filter
                            (let [art-ids (get sup-art-index supplier-filter #{})]
                              (->> articles
                                (filter #(contains? art-ids (str (:id %))))
                                (mapv (fn [art]
                                        (let [aggs (get sup-art-aggs [supplier-filter (str (:id art))])]
                                          (merge art aggs))))))
                            articles)
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
                              sorted (sort cmp filtered-articles)]
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

      ;; Suppliers list (clickable for filtering)
      (when (and (not related-loading?) (seq suppliers))
        ($ :div {:class "pt-2"}
          ($ :p {:class "text-sm font-semibold uppercase tracking-wide text-base-content/50 mb-2"}
            (str (t :search/manufacturer-suppliers) " (" (count suppliers) ")"))
          ($ :div {:class "space-y-1.5"}
            (for [sup suppliers]
              (let [sid (str (:id sup))
                    active? (= supplier-filter sid)]
                ($ :button {:key sid
                            :class (str "w-full text-left flex items-center px-3 py-2 rounded-lg border transition-colors "
                                     (if active?
                                       "bg-primary/10 border-primary/30"
                                       "bg-base-100 border-base-300 hover:bg-base-200"))
                            :on-click #(set-supplier-filter! (if active? nil sid))}
                  ($ :div {:class "min-w-0 flex-1"}
                    ($ :p {:class "text-sm font-medium truncate"} (or (:display_name sup) "\u2014"))
                    (when (:address sup)
                      ($ :p {:class "text-xs text-base-content/60 truncate mt-0.5"} (:address sup))))))))))

      ;; No suppliers
      (when (and (not related-loading?) (empty? suppliers))
        ($ :p {:class "text-sm text-base-content/40 pt-2"} (t :search/manufacturer-no-suppliers)))

      ;; Articles table
      (when (and (not related-loading?) (seq articles))
        ($ :div {:class "pt-2"}
          ($ :p {:class "text-sm font-semibold uppercase tracking-wide text-base-content/50 mb-2"}
            (str (t :search/supplier-articles) " (" (count sorted-articles) ")"))
          (if (seq sorted-articles)
            ($ :div {:class "max-h-[360px] overflow-y-auto overflow-x-auto"}
              ($ :table {:class "w-full text-sm"}
                ($ :thead {:class "sticky top-0 bg-base-100"}
                  ($ :tr {:class "border-b border-base-300 text-base-content/50"}
                    (sort-header {:label (t :search/store-article-name) :col :canonical_name
                                  :sort-col sort-col :sort-dir sort-dir :on-sort toggle-sort
                                  :class "text-left py-1.5 pr-2 font-medium"})
                    (sort-header {:label (t :search/store-last-price) :col :last_price
                                  :sort-col sort-col :sort-dir sort-dir :on-sort toggle-sort
                                  :class "text-right py-1.5 px-1 font-medium"})
                    (sort-header {:label (t :search/store-total-qty) :col :total_qty
                                  :sort-col sort-col :sort-dir sort-dir :on-sort toggle-sort
                                  :class "text-right py-1.5 px-1 font-medium"})
                    (sort-header {:label (t :search/store-total-bam) :col :total_bam
                                  :sort-col sort-col :sort-dir sort-dir :on-sort toggle-sort
                                  :class "text-right py-1.5 pl-1 font-medium"})))
                ($ :tbody
                  (for [art sorted-articles]
                    ($ :tr {:key (str (:id art))
                            :class "border-b border-base-200/50 hover:bg-base-200/30"}
                      ($ :td {:class "py-1.5 pr-2"} (or (:canonical_name art) "\u2014"))
                      ($ :td {:class "py-1.5 px-1 text-right font-medium"}
                        (format-amount (:last_price art) "BAM"))
                      ($ :td {:class "py-1.5 px-1 text-right"} (some-> (:total_qty art) str))
                      ($ :td {:class "py-1.5 pl-1 text-right font-semibold"}
                        (format-amount (:total_bam art) "BAM")))))))
            ($ :p {:class "text-sm text-base-content/40"} (t :search/supplier-no-articles)))))

      ;; No articles at all
      (when (and (not related-loading?) (empty? articles))
        ($ :p {:class "text-sm text-base-content/40 pt-2"} (t :search/supplier-no-articles))))))

;; Category-specific detail body with subcategories list + stores + articles table
(defui category-detail-body [{:keys [related related-loading? t]}]
  (let [[subcat-filter set-subcat-filter!] (use-state nil)
        [store-filter set-store-filter!] (use-state nil)
        [[sort-col sort-dir] set-sort!] (use-state [:total_bam :desc])
        subcategories (:subcategories related)
        stores (:stores related)
        articles (:articles related)
        subcat-articles (:subcat-articles related)
        store-articles (:store-articles related)
        ;; Build per-subcategory index
        sub-art-index (reduce (fn [acc {:keys [subcategory_id article_id]}]
                                (update acc (str subcategory_id) (fnil conj #{}) (str article_id)))
                        {} subcat-articles)
        sub-art-aggs (reduce (fn [acc row]
                               (assoc acc [(str (:subcategory_id row)) (str (:article_id row))]
                                 {:total_qty (:total_qty row) :total_bam (:total_bam row)}))
                       {} subcat-articles)
        ;; Build per-store index
        store-art-index (reduce (fn [acc {:keys [store_id article_id]}]
                                  (update acc (str store_id) (fnil conj #{}) (str article_id)))
                          {} store-articles)
        store-art-aggs (reduce (fn [acc row]
                                 (assoc acc [(str (:store_id row)) (str (:article_id row))]
                                   {:total_qty (:total_qty row) :total_bam (:total_bam row)}))
                         {} store-articles)
        ;; Apply filters
        filtered-articles (cond->> articles
                            subcat-filter
                            (filter #(contains? (get sub-art-index subcat-filter #{}) (str (:id %))))
                            store-filter
                            (filter #(contains? (get store-art-index store-filter #{}) (str (:id %)))))
        ;; Override aggregates when exactly one filter is active
        filtered-articles (if (and subcat-filter (not store-filter))
                            (mapv #(merge % (get sub-art-aggs [subcat-filter (str (:id %))])) filtered-articles)
                            (if (and store-filter (not subcat-filter))
                              (mapv #(merge % (get store-art-aggs [store-filter (str (:id %))])) filtered-articles)
                              (vec filtered-articles)))
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

      ;; Subcategories (clickable filter)
      (when (and (not related-loading?) (seq subcategories))
        ($ :div {:class "pt-2"}
          ($ :p {:class "text-sm font-semibold uppercase tracking-wide text-base-content/50 mb-2"}
            (str (t :search/category-subcategories) " (" (count subcategories) ")"))
          ($ :div {:class "space-y-1.5"}
            (for [sub subcategories]
              (let [sid (str (:id sub))
                    active? (= subcat-filter sid)]
                ($ :button {:key sid
                            :class (str "w-full text-left px-3 py-2 rounded-lg border transition-colors "
                                     (if active?
                                       "bg-primary/10 border-primary/30"
                                       "bg-base-100 border-base-300 hover:bg-base-200"))
                            :on-click #(set-subcat-filter! (if active? nil sid))}
                  ($ :p {:class "text-sm font-medium truncate"} (or (:name sub) "\u2014"))))))))

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
            (str (t :search/category-articles) " (" (count sorted-articles) ")"))
          (if (seq sorted-articles)
            ($ :div {:class "max-h-[360px] overflow-y-auto overflow-x-auto"}
              ($ :table {:class "w-full text-sm"}
                ($ :thead {:class "sticky top-0 bg-base-100"}
                  ($ :tr {:class "border-b border-base-300 text-base-content/50"}
                    (sort-header {:label (t :search/store-article-name) :col :canonical_name
                                  :sort-col sort-col :sort-dir sort-dir :on-sort toggle-sort
                                  :class "text-left py-1.5 pr-2 font-medium"})
                    (sort-header {:label (t :search/store-last-price) :col :last_price
                                  :sort-col sort-col :sort-dir sort-dir :on-sort toggle-sort
                                  :class "text-right py-1.5 px-1 font-medium"})
                    (sort-header {:label (t :search/store-total-qty) :col :total_qty
                                  :sort-col sort-col :sort-dir sort-dir :on-sort toggle-sort
                                  :class "text-right py-1.5 px-1 font-medium"})
                    (sort-header {:label (t :search/store-total-bam) :col :total_bam
                                  :sort-col sort-col :sort-dir sort-dir :on-sort toggle-sort
                                  :class "text-right py-1.5 pl-1 font-medium"})))
                ($ :tbody
                  (for [art sorted-articles]
                    ($ :tr {:key (str (:id art))
                            :class "border-b border-base-200/50 hover:bg-base-200/30"}
                      ($ :td {:class "py-1.5 pr-2"} (or (:canonical_name art) "\u2014"))
                      ($ :td {:class "py-1.5 px-1 text-right font-medium"}
                        (format-amount (:last_price art) "BAM"))
                      ($ :td {:class "py-1.5 px-1 text-right"} (some-> (:total_qty art) str))
                      ($ :td {:class "py-1.5 pl-1 text-right font-semibold"}
                        (format-amount (:total_bam art) "BAM")))))))
            ($ :p {:class "text-sm text-base-content/40"} (t :search/category-no-articles)))))

      (when (and (not related-loading?) (empty? articles))
        ($ :p {:class "text-sm text-base-content/40 pt-2"} (t :search/category-no-articles))))))

;; Subcategory-specific detail body — stores + articles
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
                    (sort-header {:label (t :search/store-article-name) :col :canonical_name
                                  :sort-col sort-col :sort-dir sort-dir :on-sort toggle-sort
                                  :class "text-left py-1.5 pr-2 font-medium"})
                    (sort-header {:label (t :search/store-last-price) :col :last_price
                                  :sort-col sort-col :sort-dir sort-dir :on-sort toggle-sort
                                  :class "text-right py-1.5 px-1 font-medium"})
                    (sort-header {:label (t :search/store-total-qty) :col :total_qty
                                  :sort-col sort-col :sort-dir sort-dir :on-sort toggle-sort
                                  :class "text-right py-1.5 px-1 font-medium"})
                    (sort-header {:label (t :search/store-total-bam) :col :total_bam
                                  :sort-col sort-col :sort-dir sort-dir :on-sort toggle-sort
                                  :class "text-right py-1.5 pl-1 font-medium"})))
                ($ :tbody
                  (for [art sorted-articles]
                    ($ :tr {:key (str (:id art))
                            :class "border-b border-base-200/50 hover:bg-base-200/30"}
                      ($ :td {:class "py-1.5 pr-2"} (or (:canonical_name art) "\u2014"))
                      ($ :td {:class "py-1.5 px-1 text-right font-medium"}
                        (format-amount (:last_price art) "BAM"))
                      ($ :td {:class "py-1.5 px-1 text-right"} (some-> (:total_qty art) str))
                      ($ :td {:class "py-1.5 pl-1 text-right font-semibold"}
                        (format-amount (:total_bam art) "BAM")))))))
            ($ :p {:class "text-sm text-base-content/40"} (t :search/subcategory-no-articles)))))

      (when (and (not related-loading?) (empty? articles))
        ($ :p {:class "text-sm text-base-content/40 pt-2"} (t :search/subcategory-no-articles))))))

(defui detail-panel [{:keys [selected results related related-loading? t on-close]}]
  (when selected
    (let [{:keys [type id]} selected
          items (get results (keyword type))
          item  (first (filter #(= (str (:id %)) (str id)) (or items [])))
          is-article? (= (keyword type) :articles)
          is-store? (= (keyword type) :stores)
          is-supplier? (= (keyword type) :suppliers)
          is-manufacturer? (= (keyword type) :manufacturers)
          is-category? (= (keyword type) :categories)
          is-subcategory? (= (keyword type) :subcategories)]
      ($ :div {:class "h-full flex flex-col bg-base-100 border-l border-base-300"}
        ;; Header
        ($ :div {:class "flex items-center justify-between px-5 py-3 border-b border-base-300 flex-shrink-0"}
          ($ :p {:class "text-base font-semibold"}
            (cond
              is-article?      (or (:canonical_name item) (t (keyword "search" (str "type-" (name type)))))
              is-store?        (or (:display_name item) (t (keyword "search" (str "type-" (name type)))))
              is-supplier?     (or (:display_name item) (t (keyword "search" (str "type-" (name type)))))
              is-manufacturer? (or (:display_name item) (t (keyword "search" (str "type-" (name type)))))
              is-category?     (or (:name item) (t (keyword "search" (str "type-" (name type)))))
              is-subcategory?  (str (or (:name item) (t (keyword "search" (str "type-" (name type)))))
                                 (when (:category_name item) (str " (" (:category_name item) ")")))
              :else            (t (keyword "search" (str "type-" (name type))))))
          ($ :button {:class "text-base-content/40 hover:text-base-content transition-colors"
                      :on-click on-close}
            ($ :svg {:class "w-4 h-4" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
              ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                        :d "M6 18L18 6M6 6l12 12"}))))
        ;; Body
        ($ :div {:class "flex-1 overflow-y-auto p-5"}
          (if item
            (cond
              ;; Rich article detail with price history + store filtering
              is-article?
              ($ article-detail-body
                {:related related
                 :related-loading? related-loading?
                 :t t})
              ;; Rich store detail with supplier/city + articles list
              is-store?
              ($ store-detail-body
                {:related related
                 :related-loading? related-loading?
                 :t t})
              ;; Rich supplier detail with stores + articles list
              is-supplier?
              ($ supplier-detail-body
                {:related related
                 :related-loading? related-loading?
                 :t t})
              ;; Rich manufacturer detail with suppliers + articles list
              is-manufacturer?
              ($ manufacturer-detail-body
                {:related related
                 :related-loading? related-loading?
                 :t t})
              ;; Rich category detail with subcategories + articles list
              is-category?
              ($ category-detail-body
                {:related related
                 :related-loading? related-loading?
                 :t t})
              ;; Subcategory detail with articles list
              is-subcategory?
              ($ subcategory-detail-body
                {:related related
                 :related-loading? related-loading?
                 :t t})
              ;; Standard entity detail
              :else
              ($ :div {:class "space-y-4"}
                ;; Entity key-value rows
                ($ :div {:class "space-y-0"}
                  (case (keyword type)
                    :payers
                    ($ :<>
                      (detail-row (t :search/detail-label) (:label item)))

                    :expense-cats
                    ($ :<>
                      (detail-row (t :search/detail-name) (:name item)))

                    :categories
                    ($ :<>
                      (detail-row (t :search/detail-name) (:name item))
                      (detail-row (t :search/detail-description) (:description item)))

                    :manufacturers
                    ($ :<>
                      (detail-row (t :search/detail-name) (:display_name item))
                      (detail-row (t :search/detail-key) (:normalized_key item)))

                    :cities
                    ($ :<>
                      (detail-row (t :search/detail-name) (:name item))
                      (detail-row (t :search/detail-zip) (:zip item))
                      (detail-row (t :search/detail-country) (:country item)))

                    ;; fallback
                    ($ :pre {:class "text-xs text-base-content/50 whitespace-pre-wrap"}
                      (pr-str item))))

                ;; Related records loading spinner
                (when related-loading?
                  ($ :div {:class "flex items-center gap-2 text-xs text-base-content/40 pt-2"}
                    ($ :span {:class "ds-loading ds-loading-spinner ds-loading-xs"})
                    (t :search/related-loading)))

                ;; Related expenses
                (when (and (not related-loading?) (seq (:expenses related)))
                  ($ :div {:class "pt-2"}
                    ($ :p {:class "text-xs font-semibold uppercase tracking-wide text-base-content/50 mb-1.5"}
                      (t :search/related-expenses))
                    ($ :div {:class "divide-y divide-base-200"}
                      (for [e (:expenses related)]
                        ($ :div {:key (str (:id e))
                                 :class "flex items-center justify-between py-1.5"}
                          ($ :div
                            ($ :p {:class "text-xs font-medium"}
                              (or (:supplier_display_name e) (:payer_label e) "\u2014"))
                            ($ :p {:class "text-xs text-base-content/50"}
                              (format-date (:purchased_at e))))
                          ($ :p {:class "text-xs font-semibold shrink-0 pl-2"}
                            (format-amount (:total_amount e) (:currency e))))))))))
            ($ :p {:class "text-sm text-base-content/40"} "\u2014")))))))

;; ---------------------------------------------------------------------------
;; Main page
;; ---------------------------------------------------------------------------

(defui search-page []
  (let [t               (use-t)
        query           (use-subscribe [:user-expenses/search-query])
        loading?        (use-subscribe [:user-expenses/search-loading?])
        results         (use-subscribe [:user-expenses/search-results])
        selected        (use-subscribe [:user-expenses/search-selected])
        related         (use-subscribe [:user-expenses/search-related])
        related-loading? (use-subscribe [:user-expenses/search-related-loading?])
        input-ref       (use-ref nil)
        has-results?    (some seq (vals (or results {})))
        panel-open?     (some? selected)]

    ;; Focus input on mount
    (use-effect
      (fn []
        (when-let [el @input-ref]
          (.focus el))
        js/undefined)
      [])

    ($ :div {:class "flex flex-col h-full overflow-hidden"}

      ;; Search bar — always full width
      ($ :div {:class "flex-shrink-0 p-4 border-b border-base-200"}
        ($ :h1 {:class "text-3xl font-bold mb-4"} (t :search/title))
        ($ :div {:class "relative"}
          ($ :div {:class "absolute inset-y-0 left-4 flex items-center pointer-events-none"}
            ($ :svg {:class "w-5 h-5 text-base-content/40" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
              ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                        :d "M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0"})))
          ($ :input
            {:ref input-ref
             :type "text"
             :class "w-full pl-11 pr-4 py-3 rounded-lg border border-base-300 bg-base-100 text-lg focus:outline-none focus:ring-2 focus:ring-primary/30 focus:border-primary"
             :placeholder (t :search/placeholder)
             :value (or query "")
             :on-change (fn [e]
                          (rf/dispatch [:user-expenses/set-search-query (.. e -target -value)]))})))

      ;; Two-column area: results + detail panel
      ($ :div {:class "flex flex-1 overflow-hidden"}

        ;; Left column — results
        ($ :div {:class (str "flex flex-col overflow-hidden transition-all duration-300 "
                          (if panel-open? "w-1/3 border-r border-base-300" "w-full"))}
          ($ :div {:class "flex-1 overflow-y-auto p-4"}
            (cond
              loading?
              ($ :div {:class "flex items-center justify-center py-12 text-base-content/40 text-sm"}
                (t :search/loading))

              (and query (< (count query) 2))
              ($ :div {:class "flex items-center justify-center py-12 text-base-content/40 text-sm"}
                (t :search/min-chars))

              (and query (>= (count query) 2) (not has-results?))
              ($ :div {:class "flex items-center justify-center py-12 text-base-content/40 text-sm"}
                (t :search/no-results query))

              has-results?
              ($ :<>
                ;; Suppliers
                ($ result-group
                  {:title (t :search/type-supplier)
                   :badge-class "bg-green-100 text-green-700"
                   :items (:suppliers results)
                   :render-item (fn [item]
                                  (let [id (str (:id item))
                                        sel? (= selected {:type :suppliers :id id})]
                                    ($ simple-card
                                      {:key id
                                       :label (:display_name item)
                                       :subtitle (:address item)
                                       :selected? sel?
                                       :on-click #(rf/dispatch [:user-expenses/select-search-result
                                                                {:type :suppliers :id id}])})))})

                ;; Stores
                ($ result-group
                  {:title (t :search/type-store)
                   :badge-class "bg-orange-100 text-orange-700"
                   :items (:stores results)
                   :render-item (fn [item]
                                  (let [id (str (:id item))
                                        sel? (= selected {:type :stores :id id})]
                                    ($ simple-card
                                      {:key id
                                       :label (:display_name item)
                                       :subtitle (:address item)
                                       :selected? sel?
                                       :on-click #(rf/dispatch [:user-expenses/select-search-result
                                                                {:type :stores :id id}])})))})

                ;; Articles
                ($ result-group
                  {:title (t :search/type-article)
                   :badge-class "bg-teal-100 text-teal-700"
                   :items (:articles results)
                   :render-item (fn [item]
                                  (let [id (str (:id item))
                                        sel? (= selected {:type :articles :id id})]
                                    ($ simple-card
                                      {:key id
                                       :label (:canonical_name item)
                                       :selected? sel?
                                       :on-click #(rf/dispatch [:user-expenses/select-search-result
                                                                {:type :articles :id id}])})))})

                ;; Payers
                ($ result-group
                  {:title (t :search/type-payer)
                   :badge-class "bg-pink-100 text-pink-700"
                   :items (:payers results)
                   :render-item (fn [item]
                                  (let [id (str (:id item))
                                        sel? (= selected {:type :payers :id id})]
                                    ($ simple-card
                                      {:key id
                                       :label (:label item)
                                       :selected? sel?
                                       :on-click #(rf/dispatch [:user-expenses/select-search-result
                                                                {:type :payers :id id}])})))})

                ;; Expense categories
                ($ result-group
                  {:title (t :search/type-expense-cat)
                   :badge-class "bg-yellow-100 text-yellow-700"
                   :items (:expense-cats results)
                   :render-item (fn [item]
                                  (let [id (str (:id item))
                                        sel? (= selected {:type :expense-cats :id id})]
                                    ($ simple-card
                                      {:key id
                                       :label (:name item)
                                       :selected? sel?
                                       :on-click #(rf/dispatch [:user-expenses/select-search-result
                                                                {:type :expense-cats :id id}])})))})

                ;; Categories
                ($ result-group
                  {:title (t :search/type-category)
                   :badge-class "bg-violet-100 text-violet-700"
                   :items (:categories results)
                   :render-item (fn [item]
                                  (let [id (str (:id item))
                                        sel? (= selected {:type :categories :id id})]
                                    ($ simple-card
                                      {:key id
                                       :label (:name item)
                                       :subtitle (:description item)
                                       :selected? sel?
                                       :on-click #(rf/dispatch [:user-expenses/select-search-result
                                                                {:type :categories :id id}])})))})

                ;; Subcategories
                ($ result-group
                  {:title (t :search/type-subcategory)
                   :badge-class "bg-fuchsia-100 text-fuchsia-700"
                   :items (:subcategories results)
                   :render-item (fn [item]
                                  (let [id (str (:id item))
                                        sel? (= selected {:type :subcategories :id id})]
                                    ($ simple-card
                                      {:key id
                                       :label (:name item)
                                       :subtitle (:category_name item)
                                       :selected? sel?
                                       :on-click #(rf/dispatch [:user-expenses/select-search-result
                                                                {:type :subcategories :id id}])})))})

                ;; Manufacturers
                ($ result-group
                  {:title (t :search/type-manufacturer)
                   :badge-class "bg-indigo-100 text-indigo-700"
                   :items (:manufacturers results)
                   :render-item (fn [item]
                                  (let [id (str (:id item))
                                        sel? (= selected {:type :manufacturers :id id})]
                                    ($ simple-card
                                      {:key id
                                       :label (:display_name item)
                                       :selected? sel?
                                       :on-click #(rf/dispatch [:user-expenses/select-search-result
                                                                {:type :manufacturers :id id}])})))})

                ;; Cities
                ($ result-group
                  {:title (t :search/type-city)
                   :badge-class "bg-sky-100 text-sky-700"
                   :items (:cities results)
                   :render-item (fn [item]
                                  (let [id (str (:id item))
                                        sel? (= selected {:type :cities :id id})]
                                    ($ simple-card
                                      {:key id
                                       :label (:name item)
                                       :subtitle (when (:zip item) (str (:zip item) (when (:country item) (str ", " (:country item)))))
                                       :selected? sel?
                                       :on-click #(rf/dispatch [:user-expenses/select-search-result
                                                                {:type :cities :id id}])})))}))

              :else
              ($ :div {:class "flex flex-col items-center justify-center py-16 text-base-content/30"}
                ($ :svg {:class "w-12 h-12 mb-3" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
                  ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "1.5"
                            :d "M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0"}))
                ($ :p {:class "text-sm"} (t :search/placeholder))))))

        ;; Right column — detail panel (slides in)
        (when panel-open?
          ($ :div {:class "w-2/3 overflow-hidden"}
            ($ detail-panel
              {:selected selected
               :results (or results {})
               :related related
               :related-loading? related-loading?
               :t t
               :on-close #(rf/dispatch [:user-expenses/clear-search-selection])})))))))
