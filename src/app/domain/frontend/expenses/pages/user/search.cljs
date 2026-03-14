(ns app.domain.frontend.expenses.pages.user.search
  "Cross-entity search page.

  Shows a search input and fans results across all entity types.
  Selecting a result opens a slide-in detail panel on the right.

  Facade — delegates to sub-namespaces under `search/`."
  (:require
    [app.domain.frontend.expenses.pages.user.search.cards :as cards]
    [app.domain.frontend.expenses.pages.user.search.detail-article :as detail-article]
    [app.domain.frontend.expenses.pages.user.search.detail-category :as detail-category]
    [app.domain.frontend.expenses.pages.user.search.detail-manufacturer :as detail-manufacturer]
    [app.domain.frontend.expenses.pages.user.search.detail-store :as detail-store]
    [app.domain.frontend.expenses.pages.user.search.detail-subcategory :as detail-subcategory]
    [app.domain.frontend.expenses.pages.user.search.detail-supplier :as detail-supplier]
    [app.domain.frontend.expenses.pages.user.search.helpers :as h]
    [app.template.frontend.i18n :refer [use-t]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect use-ref]]
    [uix.re-frame :refer [use-subscribe]]
    app.domain.frontend.expenses.subs.user-expenses))

;; ---------------------------------------------------------------------------
;; Detail panel — dispatcher that selects which detail body to render
;; ---------------------------------------------------------------------------

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
              ($ detail-article/article-detail-body
                {:related related
                 :related-loading? related-loading?
                 :t t})
              ;; Rich store detail with supplier/city + articles list
              is-store?
              ($ detail-store/store-detail-body
                {:related related
                 :related-loading? related-loading?
                 :t t})
              ;; Rich supplier detail with stores + articles list
              is-supplier?
              ($ detail-supplier/supplier-detail-body
                {:related related
                 :related-loading? related-loading?
                 :t t})
              ;; Rich manufacturer detail with suppliers + articles list
              is-manufacturer?
              ($ detail-manufacturer/manufacturer-detail-body
                {:related related
                 :related-loading? related-loading?
                 :t t})
              ;; Rich category detail with subcategories + articles list
              is-category?
              ($ detail-category/category-detail-body
                {:related related
                 :related-loading? related-loading?
                 :t t})
              ;; Subcategory detail with articles list
              is-subcategory?
              ($ detail-subcategory/subcategory-detail-body
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
                      (h/detail-row (t :search/detail-label) (:label item)))

                    :expense-cats
                    ($ :<>
                      (h/detail-row (t :search/detail-name) (:name item)))

                    :categories
                    ($ :<>
                      (h/detail-row (t :search/detail-name) (:name item))
                      (h/detail-row (t :search/detail-description) (:description item)))

                    :manufacturers
                    ($ :<>
                      (h/detail-row (t :search/detail-name) (:display_name item))
                      (h/detail-row (t :search/detail-key) (:normalized_key item)))

                    :cities
                    ($ :<>
                      (h/detail-row (t :search/detail-name) (:name item))
                      (h/detail-row (t :search/detail-zip) (:zip item))
                      (h/detail-row (t :search/detail-country) (:country item)))

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
                              (h/format-date (:purchased_at e))))
                          ($ :p {:class "text-xs font-semibold shrink-0 pl-2"}
                            (h/format-amount (:total_amount e) (:currency e))))))))))
            ($ :p {:class "text-sm text-base-content/40"} "\u2014")))))))

;; ---------------------------------------------------------------------------
;; Main page
;; ---------------------------------------------------------------------------

(defui search-page-content
  [{:keys [query loading? results selected related related-loading?
           set-query! select-result! clear-selection!]}]
  (let [t            (use-t)
        input-ref    (use-ref nil)
        has-results? (some seq (vals (or results {})))
        panel-open?  (some? selected)]
    (use-effect
      (fn []
        (when-let [el @input-ref]
          (.focus el))
        js/undefined)
      [])

    ($ :div {:class "flex flex-col h-full overflow-hidden"}
      ($ :div {:class "flex-shrink-0 p-4 border-b border-base-200"}
        ($ :h1 {:class "text-3xl font-bold mb-4"} (t :search/title))
        ($ :div {:class "relative"}
          ($ :div {:class "absolute inset-y-0 left-4 flex items-center pointer-events-none"}
            ($ :svg {:class "w-5 h-5 text-base-content/40"
                     :fill "none"
                     :stroke "currentColor"
                     :viewBox "0 0 24 24"}
              ($ :path {:stroke-linecap "round"
                        :stroke-linejoin "round"
                        :stroke-width "2"
                        :d "M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0"})))
          ($ :input
            {:ref input-ref
             :type "text"
             :class "w-full pl-11 pr-4 py-3 rounded-lg border border-base-300 bg-base-100 text-lg focus:outline-none focus:ring-2 focus:ring-primary/30 focus:border-primary"
             :placeholder (t :search/placeholder)
             :value (or query "")
             :on-change (fn [e]
                          (set-query! (.. e -target -value)))})))

      ($ :div {:class "flex flex-1 overflow-hidden"}
        ($ :div {:class (str "flex flex-col overflow-hidden transition-all duration-300 "
                          (if panel-open?
                            "w-1/3 border-r border-base-300"
                            "w-full"))}
          ($ :div {:class "flex-1 overflow-y-auto p-4"}
            (cond
              loading?
              ($ :div {:class "flex items-center justify-center py-12 text-base-content/40 text-base"}
                (t :search/loading))

              (and query (< (count query) 2))
              ($ :div {:class "flex items-center justify-center py-12 text-base-content/40 text-base"}
                (t :search/min-chars))

              (and query (>= (count query) 2) (not has-results?))
              ($ :div {:class "flex items-center justify-center py-12 text-base-content/40 text-base"}
                (t :search/no-results query))

              has-results?
              ($ :<>
                ($ cards/result-group
                  {:title (t :search/type-supplier)
                   :badge-class "bg-green-100 text-green-700"
                   :items (:suppliers results)
                   :render-item (fn [item]
                                  (let [id (str (:id item))
                                        sel? (= selected {:type :suppliers :id id})]
                                    ($ cards/simple-card
                                      {:key id
                                       :label (:display_name item)
                                       :subtitle (:address item)
                                       :selected? sel?
                                       :on-click #(select-result! {:type :suppliers :id id})})))})

                ($ cards/result-group
                  {:title (t :search/type-store)
                   :badge-class "bg-orange-100 text-orange-700"
                   :items (:stores results)
                   :render-item (fn [item]
                                  (let [id (str (:id item))
                                        sel? (= selected {:type :stores :id id})]
                                    ($ cards/simple-card
                                      {:key id
                                       :label (:display_name item)
                                       :subtitle (:address item)
                                       :selected? sel?
                                       :on-click #(select-result! {:type :stores :id id})})))})

                ($ cards/result-group
                  {:title (t :search/type-article)
                   :badge-class "bg-teal-100 text-teal-700"
                   :items (:articles results)
                   :render-item (fn [item]
                                  (let [id (str (:id item))
                                        sel? (= selected {:type :articles :id id})]
                                    ($ cards/simple-card
                                      {:key id
                                       :label (:canonical_name item)
                                       :selected? sel?
                                       :on-click #(select-result! {:type :articles :id id})})))})

                ($ cards/result-group
                  {:title (t :search/type-payer)
                   :badge-class "bg-pink-100 text-pink-700"
                   :items (:payers results)
                   :render-item (fn [item]
                                  (let [id (str (:id item))
                                        sel? (= selected {:type :payers :id id})]
                                    ($ cards/simple-card
                                      {:key id
                                       :label (:label item)
                                       :selected? sel?
                                       :on-click #(select-result! {:type :payers :id id})})))})

                ($ cards/result-group
                  {:title (t :search/type-expense-cat)
                   :badge-class "bg-yellow-100 text-yellow-700"
                   :items (:expense-cats results)
                   :render-item (fn [item]
                                  (let [id (str (:id item))
                                        sel? (= selected {:type :expense-cats :id id})]
                                    ($ cards/simple-card
                                      {:key id
                                       :label (:name item)
                                       :selected? sel?
                                       :on-click #(select-result! {:type :expense-cats :id id})})))})

                ($ cards/result-group
                  {:title (t :search/type-category)
                   :badge-class "bg-violet-100 text-violet-700"
                   :items (:categories results)
                   :render-item (fn [item]
                                  (let [id (str (:id item))
                                        sel? (= selected {:type :categories :id id})]
                                    ($ cards/simple-card
                                      {:key id
                                       :label (:name item)
                                       :subtitle (:description item)
                                       :selected? sel?
                                       :on-click #(select-result! {:type :categories :id id})})))})

                ($ cards/result-group
                  {:title (t :search/type-subcategory)
                   :badge-class "bg-fuchsia-100 text-fuchsia-700"
                   :items (:subcategories results)
                   :render-item (fn [item]
                                  (let [id (str (:id item))
                                        sel? (= selected {:type :subcategories :id id})]
                                    ($ cards/simple-card
                                      {:key id
                                       :label (:name item)
                                       :subtitle (:category_name item)
                                       :selected? sel?
                                       :on-click #(select-result! {:type :subcategories :id id})})))})

                ($ cards/result-group
                  {:title (t :search/type-manufacturer)
                   :badge-class "bg-indigo-100 text-indigo-700"
                   :items (:manufacturers results)
                   :render-item (fn [item]
                                  (let [id (str (:id item))
                                        sel? (= selected {:type :manufacturers :id id})]
                                    ($ cards/simple-card
                                      {:key id
                                       :label (:display_name item)
                                       :selected? sel?
                                       :on-click #(select-result! {:type :manufacturers :id id})})))})

                ($ cards/result-group
                  {:title (t :search/type-city)
                   :badge-class "bg-sky-100 text-sky-700"
                   :items (:cities results)
                   :render-item (fn [item]
                                  (let [id (str (:id item))
                                        sel? (= selected {:type :cities :id id})]
                                    ($ cards/simple-card
                                      {:key id
                                       :label (:name item)
                                       :subtitle (when (:zip item)
                                                   (str (:zip item)
                                                     (when (:country item)
                                                       (str ", " (:country item)))))
                                       :selected? sel?
                                       :on-click #(select-result! {:type :cities :id id})})))}))

              :else
              ($ :div {:class "flex flex-col items-center justify-center py-16 text-base-content/30"}
                ($ :svg {:class "w-12 h-12 mb-3"
                         :fill "none"
                         :stroke "currentColor"
                         :viewBox "0 0 24 24"}
                  ($ :path {:stroke-linecap "round"
                            :stroke-linejoin "round"
                            :stroke-width "1.5"
                            :d "M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0"}))
                ($ :p {:class "text-base"} (t :search/placeholder))))))

        (when panel-open?
          ($ :div {:class "w-2/3 overflow-hidden"}
            ($ detail-panel
              {:selected selected
               :results (or results {})
               :related related
               :related-loading? related-loading?
               :t t
               :on-close clear-selection!})))))))

(defui search-page []
  (let [query (use-subscribe [:user-expenses/search-query])
        loading? (use-subscribe [:user-expenses/search-loading?])
        results (use-subscribe [:user-expenses/search-results])
        selected (use-subscribe [:user-expenses/search-selected])
        related (use-subscribe [:user-expenses/search-related])
        related-loading? (use-subscribe [:user-expenses/search-related-loading?])]
    ($ search-page-content
      {:query query
       :loading? loading?
       :results results
       :selected selected
       :related related
       :related-loading? related-loading?
       :set-query! #(rf/dispatch [:user-expenses/set-search-query %])
       :select-result! #(rf/dispatch [:user-expenses/select-search-result %])
       :clear-selection! #(rf/dispatch [:user-expenses/clear-search-selection])})))
