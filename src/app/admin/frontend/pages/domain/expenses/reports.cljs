(ns app.admin.frontend.pages.domain.expenses.reports
  (:require
    [app.admin.frontend.components.layout :as layout]
    [app.admin.frontend.events.reports]
    [app.admin.frontend.subs.reports]
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.stats :refer [page-header]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-state]]
    [uix.re-frame :refer [use-subscribe]]))

;; ---------------------------------------------------------------------------
;; Formatting helpers
;; ---------------------------------------------------------------------------

(defn- fmt-amount [v]
  (let [n (if (number? v) v (js/parseFloat (str v)))]
    (if (js/isNaN n)
      "—"
      (.toLocaleString n "en" #js {:minimumFractionDigits 2 :maximumFractionDigits 2}))))

(defn- fmt-pct [v]
  (let [n (if (number? v) v (js/parseFloat (str v)))]
    (if (or (nil? v) (js/isNaN n))
      "—"
      (str (.toFixed n 1) "%"))))

(defn- fmt-int [v]
  (let [n (if (number? v) v (js/parseInt (str v) 10))]
    (if (js/isNaN n) "—" (.toLocaleString n "en"))))

(defn- parse-sort-val [v]
  (cond
    (nil? v) nil
    (number? v) v
    :else (let [n (js/parseFloat (str v))]
            (if (js/isNaN n) (str v) n))))

(defn- top-item-unit-label
  [row]
  (let [label (str (or (:article-canonical-name row) (:alias-label row) ""))]
    (cond
      (re-find #"(?i)(?:/|\b)(?:kg|kilogram(?:a)?)(?:\b|$)" label) "kg"
      (re-find #"(?i)(?:/|\b)(?:l|lt|ltr|lit|litar|litra)(?:\b|$)" label) "l"
      (re-find #"(?i)(?:/|\b)(?:ko|kom|komad|komada|pc)(?:\b|$)" label) "kom"
      :else nil)))

(defn- top-item-unit-price
  [row]
  (let [total (parse-sort-val (:total-amount row))
        qty (parse-sort-val (:qty-total row))]
    (when (and (number? total) (number? qty) (pos? qty))
      (/ total qty))))

(defn- enrich-top-item-row
  [row]
  (assoc row
    :unit-label (top-item-unit-label row)
    :derived-unit-price (top-item-unit-price row)))

(defn- sort-data [data sort-state]
  (if-not (:column sort-state)
    data
    (let [{:keys [column direction]} sort-state
          sorted (sort-by #(parse-sort-val (get % column)) data)]
      (if (= direction :desc)
        (vec (reverse sorted))
        (vec sorted)))))

(defn- supplier-row-id
  [row]
  (str (:supplier-id row) "::" (or (:currency row) "all")))

(defn- article-row-id
  [row]
  (str (:alias-id row) "::" (or (:currency row) "all")))

;; ---------------------------------------------------------------------------
;; Shared UI
;; ---------------------------------------------------------------------------

(defui loading-spinner []
  ($ :div {:class "flex justify-center items-center py-12"}
    ($ :div {:class "ds-loading ds-loading-spinner ds-loading-md text-primary"})))

(defui report-section [{:keys [title subtitle children report-key body-class]}]
  (let [loading? (use-subscribe [:admin/report-loading? report-key])
        error (use-subscribe [:admin/report-error report-key])]
    ($ :div {:class "bg-base-100 rounded-2xl border border-base-300 shadow-sm overflow-hidden"}
      ($ :div {:class "px-6 py-4 border-b border-base-200 bg-base-200/30"}
        ($ :h3 {:class "text-lg font-bold text-base-content"} title)
        (when subtitle
          ($ :p {:class "text-sm text-base-content/60 mt-0.5"} subtitle)))
      ($ :div {:class (str "p-6 " body-class)}
        (cond
          error ($ :div {:class "ds-alert ds-alert-error"} ($ :span error))
          loading? ($ loading-spinner)
          :else children)))))

(defui sortable-th [{:keys [label column report-key class]}]
  (let [sort-state (use-subscribe [:admin/report-sort report-key])
        active? (= (:column sort-state) column)
        direction (when active? (:direction sort-state))]
    ($ :th {:class (str "cursor-pointer select-none hover:bg-base-200/50 transition-colors " class)
            :on-click #(rf/dispatch [:admin/report-toggle-sort report-key column])}
      ($ :span {:class "inline-flex items-center gap-1"}
        label
        ($ :span {:class (str "text-xs " (if active? "opacity-100" "opacity-30"))}
          (case direction
            :asc "↑"
            :desc "↓"
            "↕"))))))

(defui detail-card [{:keys [title subtitle loading? error empty? empty-label children]}]
  ($ :div {:class "rounded-xl border border-base-300 bg-base-100 overflow-hidden"}
    ($ :div {:class "px-4 py-3 border-b border-base-200 bg-base-200/40"}
      ($ :div {:class "flex items-center justify-between gap-3"}
        ($ :h4 {:class "font-semibold text-sm text-base-content"} title)
        (when subtitle
          ($ :span {:class "text-[11px] uppercase tracking-wide text-base-content/45"}
            subtitle))))
    ($ :div {:class "p-4"}
      (cond
        loading? ($ loading-spinner)
        error ($ :div {:class "ds-alert ds-alert-error ds-alert-sm"}
                ($ :span error))
        empty? ($ :p {:class "text-sm text-base-content/50 text-center py-4"}
                 empty-label)
        :else children))))

(defui nested-table-shell [{:keys [children]}]
  ($ :div {:class "overflow-auto max-h-72 rounded-lg border border-base-300"}
    children))

(defui section-table-head [{:keys [headers]}]
  ($ :thead {:class "sticky top-0 z-10 bg-base-100 shadow-sm"}
    ($ :tr
      (map (fn [{:keys [key label class]}]
             ($ :th {:key key :class class} label))
        headers))))

(defui supplier-detail-panel [{:keys [detail-id currency]}]
  (let [stores (or (use-subscribe [:admin/report-detail-data :supplier-stores detail-id]) [])
        stores-loading? (use-subscribe [:admin/report-detail-loading? :supplier-stores detail-id])
        stores-error (use-subscribe [:admin/report-detail-error :supplier-stores detail-id])
        deep-dive (or (use-subscribe [:admin/report-detail-data :supplier-articles detail-id]) {})
        articles (vec (or (:top-aliases deep-dive) []))
        articles-loading? (use-subscribe [:admin/report-detail-loading? :supplier-articles detail-id])
        articles-error (use-subscribe [:admin/report-detail-error :supplier-articles detail-id])]
    ($ :div {:class "p-4 bg-base-200/20"}
      ($ :div {:class "grid grid-cols-1 xl:grid-cols-2 gap-4"}
        ($ detail-card
          {:title "Stores"
           :subtitle (or currency "all currencies")
           :loading? stores-loading?
           :error stores-error
           :empty? (empty? stores)
           :empty-label "No stores found for this supplier in the current filter scope."}
          ($ nested-table-shell
            ($ :table {:class "ds-table ds-table-sm w-full"}
              ($ section-table-head
                {:headers [{:key :store :label "Store"}
                           {:key :city :label "City"}
                           {:key :currency :label "Currency" :class "text-right"}
                           {:key :total :label "Total" :class "text-right"}
                           {:key :expenses :label "Expenses" :class "text-right"}
                           {:key :share :label "Share" :class "text-right"}]})
              ($ :tbody
                (map-indexed
                  (fn [idx row]
                    ($ :tr {:key (str detail-id "-store-" idx)}
                      ($ :td {:class "font-medium"} (or (:store-name row) "Unmapped store"))
                      ($ :td {:class "text-base-content/60"} (or (:city-name row) "—"))
                      ($ :td {:class "text-right text-xs font-mono"} (or (:currency row) "—"))
                      ($ :td {:class "text-right font-semibold tabular-nums"} (fmt-amount (:total-amount row)))
                      ($ :td {:class "text-right tabular-nums"} (fmt-int (:expense-count row)))
                      ($ :td {:class "text-right"}
                        ($ :span {:class "ds-badge ds-badge-sm ds-badge-primary ds-badge-outline"}
                          (fmt-pct (:share-pct row))))))
                  stores)))))
        ($ detail-card
          {:title "Articles"
           :subtitle (or currency "all currencies")
           :loading? articles-loading?
           :error articles-error
           :empty? (empty? articles)
           :empty-label "No article spending found for this supplier in the current filter scope."}
          ($ nested-table-shell
            ($ :table {:class "ds-table ds-table-sm w-full"}
              ($ section-table-head
                {:headers [{:key :article :label "Article"}
                           {:key :currency :label "Currency" :class "text-right"}
                           {:key :total :label "Total" :class "text-right"}
                           {:key :lines :label "Lines" :class "text-right"}]})
              ($ :tbody
                (map-indexed
                  (fn [idx row]
                    ($ :tr {:key (str detail-id "-article-" idx)}
                      ($ :td {:class "font-medium"}
                        (or (:article-canonical-name row) (:alias-label row) "Unmapped"))
                      ($ :td {:class "text-right text-xs font-mono"} (or (:currency row) "—"))
                      ($ :td {:class "text-right font-semibold tabular-nums"}
                        (fmt-amount (:total-amount row)))
                      ($ :td {:class "text-right tabular-nums"}
                        (fmt-int (:line-count row)))))
                  articles)))))))))

(defui article-detail-panel [{:keys [detail-id currency]}]
  (let [breakdown (or (use-subscribe [:admin/report-detail-data :article-breakdown detail-id]) {})
        loading? (use-subscribe [:admin/report-detail-loading? :article-breakdown detail-id])
        error (use-subscribe [:admin/report-detail-error :article-breakdown detail-id])
        suppliers (vec (or (:suppliers breakdown) []))
        stores (vec (or (:stores breakdown) []))]
    ($ :div {:class "p-4 bg-base-200/20"}
      ($ :div {:class "grid grid-cols-1 xl:grid-cols-2 gap-4"}
        ($ detail-card
          {:title "Suppliers"
           :subtitle (or currency "all currencies")
           :loading? loading?
           :error error
           :empty? (empty? suppliers)
           :empty-label "No supplier breakdown found for this article in the current filter scope."}
          ($ nested-table-shell
            ($ :table {:class "ds-table ds-table-sm w-full"}
              ($ section-table-head
                {:headers [{:key :supplier :label "Supplier"}
                           {:key :currency :label "Currency" :class "text-right"}
                           {:key :total :label "Total" :class "text-right"}
                           {:key :qty :label "Qty" :class "text-right"}
                           {:key :lines :label "Lines" :class "text-right"}]})
              ($ :tbody
                (map-indexed
                  (fn [idx row]
                    ($ :tr {:key (str detail-id "-supplier-" idx)}
                      ($ :td {:class "font-medium"} (or (:supplier-name row) "Unknown supplier"))
                      ($ :td {:class "text-right text-xs font-mono"} (or (:currency row) "—"))
                      ($ :td {:class "text-right font-semibold tabular-nums"} (fmt-amount (:total-amount row)))
                      ($ :td {:class "text-right tabular-nums"} (fmt-amount (:qty-total row)))
                      ($ :td {:class "text-right tabular-nums"} (fmt-int (:line-count row)))))
                  suppliers)))))
        ($ detail-card
          {:title "Stores"
           :subtitle (or currency "all currencies")
           :loading? loading?
           :error error
           :empty? (empty? stores)
           :empty-label "No store breakdown found for this article in the current filter scope."}
          ($ nested-table-shell
            ($ :table {:class "ds-table ds-table-sm w-full"}
              ($ section-table-head
                {:headers [{:key :store :label "Store"}
                           {:key :supplier :label "Supplier"}
                           {:key :currency :label "Currency" :class "text-right"}
                           {:key :total :label "Total" :class "text-right"}
                           {:key :qty :label "Qty" :class "text-right"}
                           {:key :lines :label "Lines" :class "text-right"}]})
              ($ :tbody
                (map-indexed
                  (fn [idx row]
                    ($ :tr {:key (str detail-id "-store-" idx)}
                      ($ :td {:class "font-medium"} (or (:store-name row) "Unmapped store"))
                      ($ :td {:class "text-base-content/60"} (or (:supplier-name row) "Unknown supplier"))
                      ($ :td {:class "text-right text-xs font-mono"} (or (:currency row) "—"))
                      ($ :td {:class "text-right font-semibold tabular-nums"} (fmt-amount (:total-amount row)))
                      ($ :td {:class "text-right tabular-nums"} (fmt-amount (:qty-total row)))
                      ($ :td {:class "text-right tabular-nums"} (fmt-int (:line-count row)))))
                  stores)))))))))

(defui supplier-explorer-table []
  (let [data (use-subscribe [:admin/report-data :top-suppliers])
        sort-state (use-subscribe [:admin/report-sort :top-suppliers])
        sorted (sort-data (or data []) sort-state)
        [expanded set-expanded] (use-state #{})]
    ($ report-section {:title "Suppliers"
                       :subtitle "All supplier rows in the current filter scope. Click a row to inspect stores and articles."
                       :report-key :top-suppliers
                       :body-class "p-0"}
      (if (seq sorted)
        ($ :div {:class "overflow-x-auto"}
          ($ :div {:class "max-h-[38rem] overflow-y-auto"}
            ($ :table {:class "ds-table ds-table-sm w-full"}
              ($ :thead {:class "sticky top-0 z-10 bg-base-100 shadow-sm"}
                ($ :tr
                  ($ :th {:class "w-12"} "")
                  ($ sortable-th {:label "Supplier" :column :supplier-name :report-key :top-suppliers})
                  ($ sortable-th {:label "Currency" :column :currency :report-key :top-suppliers :class "text-right"})
                  ($ sortable-th {:label "Total" :column :total-amount :report-key :top-suppliers :class "text-right"})
                  ($ sortable-th {:label "Expenses" :column :expense-count :report-key :top-suppliers :class "text-right"})
                  ($ sortable-th {:label "Share" :column :share-pct :report-key :top-suppliers :class "text-right"})))
              ($ :tbody
                (map-indexed
                  (fn [idx row]
                    (let [detail-id (supplier-row-id row)
                          expandable? (some? (:supplier-id row))
                          expanded? (contains? expanded detail-id)]
                      ($ :<> {:key detail-id}
                        ($ :tr {:class (str "transition-colors "
                                         (when expandable? "cursor-pointer hover:bg-base-200/40 ")
                                         (when expanded? "bg-primary/5"))
                                :on-click (fn []
                                            (when expandable?
                                              (let [opening? (not expanded?)]
                                                (set-expanded (fn [current]
                                                                (if (contains? current detail-id)
                                                                  (disj current detail-id)
                                                                  (conj (or current #{}) detail-id))))
                                                (when opening?
                                                  (rf/dispatch [:admin/fetch-supplier-stores-detail
                                                                detail-id
                                                                {:supplier-id (:supplier-id row)
                                                                 :currency (:currency row)}])
                                                  (rf/dispatch [:admin/fetch-supplier-articles-detail
                                                                detail-id
                                                                {:supplier-id (:supplier-id row)
                                                                 :currency (:currency row)}])))))}
                          ($ :td {:class "text-center text-base-content/50"}
                            (if expandable?
                              (if expanded? "▾" "▸")
                              "•"))
                          ($ :td {:class "font-medium"} (or (:supplier-name row) "Unknown"))
                          ($ :td {:class "text-right text-xs font-mono"} (or (:currency row) "—"))
                          ($ :td {:class "text-right font-semibold tabular-nums"}
                            (fmt-amount (:total-amount row)))
                          ($ :td {:class "text-right tabular-nums"}
                            (fmt-int (:expense-count row)))
                          ($ :td {:class "text-right"}
                            ($ :span {:class "ds-badge ds-badge-sm ds-badge-primary ds-badge-outline"}
                              (fmt-pct (:share-pct row)))))
                        (when expanded?
                          ($ :tr
                            ($ :td {:colSpan 6 :class "p-0"}
                              ($ supplier-detail-panel {:detail-id detail-id
                                                        :currency (:currency row)})))))))
                  sorted)))))
        ($ :p {:class "text-base-content/50 text-center py-6"} "No supplier data available")))))

(defui article-explorer-table []
  (let [data (->> (or (use-subscribe [:admin/report-data :top-items]) [])
               (mapv enrich-top-item-row))
        sort-state (use-subscribe [:admin/report-sort :top-items])
        sorted (sort-data data sort-state)
        [expanded set-expanded] (use-state #{})]
    ($ report-section {:title "Articles"
                       :subtitle "All article rows in the current filter scope. Click a row to inspect suppliers and stores."
                       :report-key :top-items
                       :body-class "p-0"}
      (if (seq sorted)
        ($ :div {:class "overflow-x-auto"}
          ($ :div {:class "max-h-[38rem] overflow-y-auto"}
            ($ :table {:class "ds-table ds-table-sm w-full"}
              ($ :thead {:class "sticky top-0 z-10 bg-base-100 shadow-sm"}
                ($ :tr
                  ($ :th {:class "w-12"} "")
                  ($ sortable-th {:label "Article" :column :article-canonical-name :report-key :top-items})
                  ($ sortable-th {:label "Currency" :column :currency :report-key :top-items :class "text-right"})
                  ($ sortable-th {:label "Price / unit" :column :derived-unit-price :report-key :top-items :class "text-right"})
                  ($ sortable-th {:label "Suppliers" :column :supplier-count :report-key :top-items :class "text-right"})
                  ($ sortable-th {:label "Stores" :column :store-count :report-key :top-items :class "text-right"})))
              ($ :tbody
                (map-indexed
                  (fn [idx row]
                    (let [detail-id (article-row-id row)
                          expandable? (some? (:alias-id row))
                          expanded? (contains? expanded detail-id)
                          unit-label (or (:unit-label row) "unit")]
                      ($ :<> {:key detail-id}
                        ($ :tr {:class (str "transition-colors "
                                         (when expandable? "cursor-pointer hover:bg-base-200/40 ")
                                         (when expanded? "bg-secondary/5"))
                                :on-click (fn []
                                            (when expandable?
                                              (let [opening? (not expanded?)]
                                                (set-expanded (fn [current]
                                                                (if (contains? current detail-id)
                                                                  (disj current detail-id)
                                                                  (conj (or current #{}) detail-id))))
                                                (when opening?
                                                  (rf/dispatch [:admin/fetch-article-breakdown-detail
                                                                detail-id
                                                                {:alias-id (:alias-id row)
                                                                 :currency (:currency row)}])))))}
                          ($ :td {:class "text-center text-base-content/50"}
                            (if expandable?
                              (if expanded? "▾" "▸")
                              "•"))
                          ($ :td {:class "font-medium"}
                            (or (:article-canonical-name row) (:alias-label row) "Unmapped"))
                          ($ :td {:class "text-right text-xs font-mono"} (or (:currency row) "—"))
                          ($ :td {:class "text-right font-semibold tabular-nums whitespace-nowrap"}
                            (if-let [price (:derived-unit-price row)]
                              (str (fmt-amount price) " / " unit-label)
                              "—"))
                          ($ :td {:class "text-right tabular-nums"} (fmt-int (:supplier-count row)))
                          ($ :td {:class "text-right tabular-nums"} (fmt-int (:store-count row))))
                        (when expanded?
                          ($ :tr
                            ($ :td {:colSpan 6 :class "p-0"}
                              ($ article-detail-panel {:detail-id detail-id
                                                       :currency (:currency row)})))))))
                  sorted)))))
        ($ :p {:class "text-base-content/50 text-center py-6"} "No article data available")))))

(defui category-allocation-table []
  (let [data (use-subscribe [:admin/report-data :category-allocation])
        sort-state (use-subscribe [:admin/report-sort :category-allocation])
        deduped (->> (or data [])
                  (reduce (fn [acc row]
                            (let [k [(:category-key row) (:currency row)]]
                              (if (contains? acc k) acc (assoc acc k row))))
                    {})
                  vals
                  vec)
        categories (if sort-state
                     (sort-data deduped sort-state)
                     (sort-by (fn [r] (- (or (js/parseFloat (str (:total-amount r))) 0))) deduped))]
    ($ report-section {:title "Category Allocation"
                       :subtitle "Spending breakdown by product category"
                       :report-key :category-allocation}
      (if (seq categories)
        ($ :div {:class "overflow-x-auto"}
          ($ :table {:class "ds-table ds-table-sm w-full"}
            ($ :thead
              ($ :tr
                ($ sortable-th {:label "Category" :column :category-name :report-key :category-allocation})
                ($ sortable-th {:label "Currency" :column :currency :report-key :category-allocation :class "text-right"})
                ($ sortable-th {:label "Total" :column :total-amount :report-key :category-allocation :class "text-right"})
                ($ sortable-th {:label "Items" :column :line-count :report-key :category-allocation :class "text-right"})
                ($ sortable-th {:label "Allocation" :column :allocation-pct :report-key :category-allocation :class "text-right"})
                ($ :th {:class "w-32"} "")))
            ($ :tbody
              (map-indexed
                (fn [idx row]
                  (let [pct (or (js/parseFloat (str (:allocation-pct row))) 0)]
                    ($ :tr {:key (str (:category-key row) "-" (:currency row) "-" idx)}
                      ($ :td {:class "font-medium"} (or (:category-name row) "Uncategorized"))
                      ($ :td {:class "text-right text-xs font-mono"} (or (:currency row) "—"))
                      ($ :td {:class "text-right font-semibold tabular-nums"}
                        (fmt-amount (:total-amount row)))
                      ($ :td {:class "text-right tabular-nums"} (fmt-int (:line-count row)))
                      ($ :td {:class "text-right"}
                        ($ :span {:class "ds-badge ds-badge-sm ds-badge-secondary ds-badge-outline"}
                          (fmt-pct pct)))
                      ($ :td
                        ($ :div {:class "w-full bg-base-200 rounded-full h-2"}
                          ($ :div {:class "bg-secondary rounded-full h-2 transition-all"
                                   :style {:width (str (min pct 100) "%")}}))))))
                categories))))
        ($ :p {:class "text-base-content/50 text-center py-4"} "No category data available")))))

(defui supplier-trends-section []
  (let [data (use-subscribe [:admin/report-data :supplier-monthly-trends])
        grouped (->> (or data [])
                  (group-by (fn [r] [(:supplier-id r) (:supplier-name r) (:currency r)]))
                  (sort-by (fn [[_ rows]]
                             (- (reduce + 0 (map #(or (js/parseFloat (str (:total-amount %))) 0) rows)))))
                  vec)]
    ($ report-section {:title "Supplier Monthly Trends"
                       :subtitle "Top supplier spending over time"
                       :report-key :supplier-monthly-trends}
      (if (seq grouped)
        ($ :div {:class "space-y-4"}
          (map-indexed
            (fn [idx [[supplier-id supplier-name currency] rows]]
              (let [sorted-rows (sort-by :month rows)]
                ($ :div {:key (str supplier-id "-" currency "-" idx)
                         :class "bg-base-200/30 rounded-xl p-4"}
                  ($ :div {:class "flex items-center justify-between mb-3"}
                    ($ :span {:class "font-semibold text-base-content"} (or supplier-name "Unknown"))
                    ($ :span {:class "ds-badge ds-badge-sm ds-badge-ghost font-mono"} currency))
                  ($ :div {:class "grid grid-cols-3 sm:grid-cols-4 md:grid-cols-6 gap-2"}
                    (map (fn [row]
                           ($ :div {:key (:month row)
                                    :class "text-center p-2 bg-base-100 rounded-lg"}
                             ($ :div {:class "text-[10px] text-base-content/50 font-mono"} (:month row))
                             ($ :div {:class "text-sm font-semibold tabular-nums"}
                               (fmt-amount (:total-amount row)))
                             ($ :div {:class "text-[10px] text-base-content/40"}
                               (str (fmt-int (:expense-count row)) " exp"))))
                      sorted-rows)))))
            grouped))
        ($ :p {:class "text-base-content/50 text-center py-4"} "No trend data available")))))

;; ---------------------------------------------------------------------------
;; Filters bar
;; ---------------------------------------------------------------------------

(defui filters-bar []
  (let [filters (use-subscribe [:admin/reports-filters])
        months-back (or (:months-back filters) 6)
        filter-options (use-subscribe [:admin/report-data :filter-options])
        suppliers (or (:suppliers filter-options) [])
        has-filters? (or (:supplier-id filters) (:expense-category-id filters) (:currency filters))]
    ($ :div {:class "flex flex-wrap items-end gap-4 p-4 bg-base-200/30 rounded-2xl border border-base-300"}
      ($ :div {:class "space-y-1"}
        ($ :label {:class "text-[10px] font-bold text-base-content/50 uppercase tracking-widest"
                   :for "admin-reports-months-back"}
          "Time Range")
        ($ :select {:id "admin-reports-months-back"
                    :class "ds-select ds-select-sm ds-select-bordered bg-base-100 font-medium"
                    :value months-back
                    :on-change (fn [e]
                                 (rf/dispatch [:admin/reports-set-filter :months-back
                                               (js/parseInt (.. e -target -value) 10)])
                                 (rf/dispatch [:admin/reports-refresh]))}
          ($ :option {:value 3} "Last 3 months")
          ($ :option {:value 6} "Last 6 months")
          ($ :option {:value 12} "Last 12 months")
          ($ :option {:value 24} "Last 24 months")))

      (when (seq suppliers)
        ($ :div {:class "space-y-1"}
          ($ :label {:class "text-[10px] font-bold text-base-content/50 uppercase tracking-widest"
                     :for "admin-reports-supplier"}
            "Supplier")
          ($ :select {:id "admin-reports-supplier"
                      :class "ds-select ds-select-sm ds-select-bordered bg-base-100 font-medium"
                      :value (or (:supplier-id filters) "")
                      :on-change (fn [e]
                                   (let [v (.. e -target -value)]
                                     (rf/dispatch [:admin/reports-set-filter :supplier-id
                                                   (when (seq v) v)])
                                     (rf/dispatch [:admin/reports-refresh])))}
            ($ :option {:value ""} "All Suppliers")
            (map (fn [s]
                   ($ :option {:key (str (:id s)) :value (str (:id s))} (:name s)))
              suppliers))))

      ($ :div {:class "flex items-end gap-2"}
        ($ button {:id "btn-admin-reports-refresh"
                   :btn-type :ghost
                   :size :sm
                   :class "font-medium"
                   :on-click #(rf/dispatch [:admin/reports-refresh])}
          "Refresh")
        (when has-filters?
          ($ button {:id "btn-admin-reports-clear"
                     :btn-type :ghost
                     :size :sm
                     :class "text-error/70 hover:text-error font-medium"
                     :on-click (fn []
                                 (rf/dispatch [:admin/reports-clear-filters])
                                 (rf/dispatch [:admin/reports-refresh]))}
            "Clear Filters"))))))

;; ---------------------------------------------------------------------------
;; Page
;; ---------------------------------------------------------------------------

(defui admin-reports-content []
  ($ :div {:class "py-6 min-h-screen bg-gradient-to-br from-base-100 via-base-200 to-base-300"}
    ($ page-header {:title "Expense Analytics"
                    :subtitle "Cross-tenant expense reports grouped by global entities"
                    :icon "M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"
                    :icon-color "from-secondary to-accent"
                    :bg-gradient "from-secondary/10 to-accent/10"})

    ($ :div {:class "mt-6 px-4 sm:px-6 lg:px-8 space-y-6"}
      ($ filters-bar)

      ($ :div {:class "grid grid-cols-1 xl:grid-cols-2 gap-6"}
        ($ supplier-explorer-table)
        ($ article-explorer-table))

      ($ :div {:class "grid grid-cols-1 xl:grid-cols-2 gap-6"}
        ($ category-allocation-table)
        ($ supplier-trends-section)))))

(defui admin-reports-page []
  ($ layout/admin-layout
    ($ admin-reports-content)))
