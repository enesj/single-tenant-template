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

(defui detail-panel [{:keys [selected results related related-loading? t on-close]}]
  (when selected
    (let [{:keys [type id]} selected
          items (get results (keyword type))
          item  (first (filter #(= (str (:id %)) (str id)) (or items [])))
          is-article? (= (keyword type) :articles)]
      ($ :div {:class "h-full flex flex-col bg-base-100 border-l border-base-300"}
        ;; Header
        ($ :div {:class "flex items-center justify-between px-5 py-3 border-b border-base-300 flex-shrink-0"}
          ($ :p {:class "text-base font-semibold"}
            (if is-article?
              (or (:canonical_name item) (t (keyword "search" (str "type-" (name type)))))
              (t (keyword "search" (str "type-" (name type))))))
          ($ :button {:class "text-base-content/40 hover:text-base-content transition-colors"
                      :on-click on-close}
            ($ :svg {:class "w-4 h-4" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
              ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                        :d "M6 18L18 6M6 6l12 12"}))))
        ;; Body
        ($ :div {:class "flex-1 overflow-y-auto p-5"}
          (if item
            (if is-article?
              ;; Rich article detail with price history + store filtering
              ($ article-detail-body
                {:related related
                 :related-loading? related-loading?
                 :t t})
              ;; Standard entity detail
              ($ :div {:class "space-y-4"}
                ;; Entity key-value rows
                ($ :div {:class "space-y-0"}
                  (case (keyword type)
                    :expenses
                    ($ :<>
                      (detail-row (t :search/detail-supplier) (:supplier_display_name item))
                      (detail-row (t :search/detail-payer) (:payer_label item))
                      (detail-row (t :search/detail-date) (format-date (:purchased_at item)))
                      (detail-row (t :search/detail-total) (format-amount (:total_amount item) (:currency item)))
                      (detail-row (t :search/detail-currency) (:currency item))
                      (detail-row (t :search/detail-notes) (:notes item)))

                    :receipts
                    ($ :<>
                      (detail-row (t :search/detail-filename) (:original_filename item))
                      (detail-row (t :search/detail-supplier-guess) (:supplier_guess item))
                      (detail-row (t :search/detail-store-guess) (:store_guess item))
                      (detail-row (t :search/detail-status) (:status item))
                      (detail-row (t :search/detail-created) (format-date (:created_at item))))

                    :suppliers
                    ($ :<>
                      (detail-row (t :search/detail-name) (:display_name item))
                      (detail-row (t :search/detail-key) (:normalized_key item))
                      (detail-row (t :search/detail-address) (:address item)))

                    :stores
                    ($ :<>
                      (detail-row (t :search/detail-name) (:display_name item))
                      (detail-row (t :search/detail-key) (:normalized_key item))
                      (detail-row (t :search/detail-address) (:address item)))

                    :payers
                    ($ :<>
                      (detail-row (t :search/detail-label) (:label item)))

                    :expense-cats
                    ($ :<>
                      (detail-row (t :search/detail-name) (:name item)))

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
                              (or (:supplier_display_name e) (:payer_label e) "—"))
                            ($ :p {:class "text-xs text-base-content/50"}
                              (format-date (:purchased_at e))))
                          ($ :p {:class "text-xs font-semibold shrink-0 pl-2"}
                            (format-amount (:total_amount e) (:currency e))))))))))
            ($ :p {:class "text-sm text-base-content/40"} "—")))))))

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
                ;; Expenses
                ($ result-group
                  {:title (t :search/type-expense)
                   :badge-class "bg-blue-100 text-blue-700"
                   :items (:expenses results)
                   :render-item (fn [item]
                                  (let [id (str (:id item))
                                        sel? (= selected {:type :expenses :id id})]
                                    ($ expense-card
                                      {:key id
                                       :item item
                                       :t t
                                       :selected? sel?
                                       :on-click #(rf/dispatch [:user-expenses/select-search-result
                                                                {:type :expenses :id id}])})))})

                ;; Receipts
                ($ result-group
                  {:title (t :search/type-receipt)
                   :badge-class "bg-purple-100 text-purple-700"
                   :items (:receipts results)
                   :render-item (fn [item]
                                  (let [id (str (:id item))
                                        sel? (= selected {:type :receipts :id id})]
                                    ($ receipt-card
                                      {:key id
                                       :item item
                                       :t t
                                       :selected? sel?
                                       :on-click #(rf/dispatch [:user-expenses/select-search-result
                                                                {:type :receipts :id id}])})))})

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
                                                                {:type :expense-cats :id id}])})))}))

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
