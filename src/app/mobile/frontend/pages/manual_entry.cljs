(ns app.mobile.frontend.pages.manual-entry
  "Mobile quick-add expense — two-phase smart workflow (search → review → submit)."
  (:require
    [ajax.core :as ajax]
    [app.domain.frontend.expenses.components.form-fields.helpers
     :refer [current-datetime-local format-decimal safe-parse-number]]
    [app.domain.frontend.expenses.components.manual-expense-form.smart-input.constants
     :refer [supplier-color-palette]]
    [app.domain.frontend.expenses.components.manual-expense-form.smart-input.helpers
     :refer [build-quick-pick-supplier-color-map supplier-chip-class store-chip-class]]
    [app.domain.frontend.expenses.shared.manual-entry.core :as manual-entry]
    [app.mobile.frontend.components.header :refer [mobile-header]]
    [app.template.frontend.i18n :refer [use-t]]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui] :as uix]
    [uix.re-frame :refer [use-subscribe]]))

;; ========================================================================
;; Helpers
;; ========================================================================

(def ^:private chip-colors
  {:supplier "bg-blue-100 text-blue-800"
   :store    "bg-green-100 text-green-800"
   :category "bg-purple-100 text-purple-800"
   :article  "bg-amber-100 text-amber-800"
   :payer    "bg-teal-100 text-teal-800"
   :date     "bg-sky-100 text-sky-800"
   :currency "bg-orange-100 text-orange-800"})

(def ^:private currency-options
  ["BAM" "EUR" "USD" "GBP" "CHF" "HRK" "RSD"])

(defn- result-display-name [result]
  (or (:label result)
    (:name result)
    (:display_name result)
    (:display-name result)
    (:canonical_name result)
    (:canonical-name result)
    ""))

(defn- result-entity-type [result]
  (keyword (or (:entity-type result) (:type result) "article")))

(defn- result-article-price [result]
  (or (get-in result [:price_info :avg_price])
    (:last-price result)
    (:last_price result)
    (:unit_price result)
    (:unit-price result)))

(defn- context-entry-label [entry]
  (or (:label entry) (:name entry) ""))

(defn- normalize-article-pick [article]
  {:id (or (:id article) (:article_id article) (:article-id article))
   :label (result-display-name article)
   :entity-type :article
   :last-price (result-article-price article)
   :entity article})

(defn- update-item-label-entry
  [item label]
  (-> item
    (assoc :label label)
    (dissoc :article-id)))

(defn- apply-item-article-selection
  [item article]
  (let [price (result-article-price article)]
    (cond-> (assoc item
              :label (result-display-name article)
              :article-id (:id article))
      price
      (assoc :unit-price (format-decimal price)))))

(def ^:private suggestion-chip-default-colors
  {:supplier "bg-blue-100 text-blue-800 border-[3px] border-solid border-blue-300"
   :store "bg-emerald-100 text-emerald-800 border-[3px] border-solid border-emerald-300"})

(defn- dedupe-by
  [key-fn items]
  (let [seen (volatile! #{})]
    (->> items
      (keep (fn [item]
              (let [k (key-fn item)]
                (when-not (contains? @seen k)
                  (vswap! seen conj k)
                  item))))
      vec)))

(defn- suggestion-store-supplier-id
  [store]
  (or (:supplier-id store)
    (:supplier_id store)
    (get-in store [:entity :supplier-id])
    (get-in store [:entity :supplier_id])))

(defn- suggestion-store-visible-label
  [store]
  (or (:address store)
    (:label store)
    (:display_name store)
    (:display-name store)
    ""))

(defn- suggestion-store-visible-dedupe-key
  [store]
  (let [supplier-id (some-> (suggestion-store-supplier-id store) str)
        label (some-> (suggestion-store-visible-label store) str str/trim str/lower-case)]
    (if (str/blank? label)
      (or (some-> (:id store) str)
        (str supplier-id "::missing-label"))
      (str supplier-id "::" label))))

(defn- visible-mobile-suggestions
  [suggestions context]
  (let [show-suppliers? (not (:supplier context))
        show-stores? (not (:store context))
        show-categories? (not (:category context))
        normalized-suppliers (mapv #(assoc % :label (result-display-name %))
                               (or (:suppliers suggestions) []))
        visible-stores (if show-stores?
                         (->> (:stores suggestions)
                           (mapv #(assoc % :label (suggestion-store-visible-label %)))
                           (dedupe-by suggestion-store-visible-dedupe-key)
                           (take 5)
                           vec)
                         [])
        inferred-suppliers (->> visible-stores
                             (keep (fn [store]
                                     (let [supplier-id (some-> (suggestion-store-supplier-id store) str)
                                           supplier-label (or (:supplier-display-name store)
                                                            (:supplier_display_name store)
                                                            (get-in store [:entity :supplier-display-name])
                                                            (get-in store [:entity :supplier_display_name]))]
                                       (when (and supplier-id
                                               (not (str/blank? (str supplier-label))))
                                         {:id supplier-id
                                          :label supplier-label
                                          :display_name supplier-label}))))
                             (dedupe-by #(some-> (:id %) str))
                             vec)
        visible-suppliers (if show-suppliers?
                            (->> (concat normalized-suppliers inferred-suppliers)
                              (dedupe-by #(some-> (:id %) str))
                              (take 3)
                              vec)
                            [])
        visible-categories (if show-categories?
                             (->> (:categories suggestions)
                               (take 3)
                               vec)
                             [])]
    {:suppliers visible-suppliers
     :stores visible-stores
     :categories visible-categories}))

(defn- build-suggestion-supplier-color-map
  [suppliers stores]
  (let [groups (cond-> []
                 (seq suppliers)
                 (conj {:entity-type :supplier
                        :items suppliers})

                 (seq stores)
                 (conj {:entity-type :store
                        :items stores}))]
    (build-quick-pick-supplier-color-map groups supplier-color-palette)))

(defn- suggestion-chip-class
  [supplier-color-map entity-type item]
  (case entity-type
    :supplier
    (or (supplier-chip-class supplier-color-map (:id item))
      (:supplier suggestion-chip-default-colors))

    :store
    (or (store-chip-class supplier-color-map (suggestion-store-supplier-id item))
      (:store suggestion-chip-default-colors))

    (get chip-colors entity-type "bg-base-200 text-base-content")))

(defn- entity-search-uri [entity-type]
  (case entity-type
    :supplier "/api/v1/expenses/suppliers"
    :store "/api/v1/expenses/stores"
    :article "/api/v1/expenses/articles"
    :category "/api/v1/expenses/expense-categories"
    :expense-category "/api/v1/expenses/expense-categories"
    :payer "/api/v1/expenses/payers"
    (str "/api/v1/expenses/" (name entity-type) "s")))

(defn- submit-error-key
  [items payer-id]
  (let [prepared (manual-entry/prepare-submit-items items)
        total (reduce + 0 (map :line_total prepared))]
    (cond
      (empty? prepared)
      :smart-expense/err-no-items

      (str/blank? (str payer-id))
      :smart-expense/err-no-payer

      (<= total 0)
      :smart-expense/err-no-total

      :else nil)))

(defn- phase-one-category-picks [expense-categories context search-query]
  (if (and (str/blank? search-query)
        (not (:category context)))
    (->> expense-categories
      (take 5)
      vec)
    []))

(defn- phase-one-history-picks [history items context search-query]
  (let [blank-search? (str/blank? search-query)
        items-empty? (empty? items)
        show-articles? (and blank-search? items-empty?)
        show-stores? (and show-articles?
                       (not (:store context))
                       (or (:category context)
                         (:supplier context)))]
    {:stores (if show-stores?
               (->> (:stores history)
                 (take 5)
                 vec)
               [])
     :articles (if show-articles?
                 (->> (:articles history)
                   (map normalize-article-pick)
                   (take 5)
                   vec)
                 [])}))

(defn- add-context-entry
  [context entity-type result]
  (let [label (result-display-name result)
        base-entry {:id (or (:id result) (:entity_id result) (:entity-id result))
                    :label label}
        supplier-id (or (:supplier_id result)
                      (:supplier-id result)
                      (get-in result [:entity :supplier_id])
                      (get-in result [:entity :supplier-id]))
        supplier-label (or (:supplier_display_name result)
                         (:supplier-display-name result)
                         (get-in result [:entity :supplier_display_name])
                         (get-in result [:entity :supplier-display-name]))]
    (case entity-type
      :supplier
      (let [next-context (assoc context :supplier base-entry)
            store-supplier-id (some-> (or (get-in next-context [:store :supplier-id])
                                        (get-in next-context [:store :supplier_id]))
                                str)]
        (if (and (:store next-context)
              store-supplier-id
              (not= store-supplier-id (some-> (:id base-entry) str)))
          (dissoc next-context :store)
          next-context))

      :store
      (cond-> (assoc context :store (assoc base-entry
                                      :supplier-id supplier-id
                                      :supplier-display-name supplier-label))
        supplier-id
        (assoc :supplier {:id supplier-id
                          :label (or supplier-label (get-in context [:supplier :label]))}))

      :category
      (assoc context :category base-entry)

      :expense-category
      (assoc context :category base-entry)

      (assoc context entity-type base-entry))))

;; ========================================================================
;; Events — unified quick-add search (debounced)
;; ========================================================================

(defonce ^:private search-timer (atom nil))

(rf/reg-event-fx
  :mobile/quick-search
  (fn [{:keys [db]} [_ query]]
    (when-let [timer @search-timer]
      (js/clearTimeout timer))
    (let [q (some-> query str str/trim)]
      (if (>= (count (or q "")) 2)
        (do (reset! search-timer
              (js/setTimeout
                #(rf/dispatch [:mobile/quick-search-fetch q])
                250))
          {:db (-> db
                 (assoc-in [:mobile :quick-search :query] q)
                 (assoc-in [:mobile :quick-search :loading?] true))})
        {:db (assoc-in db [:mobile :quick-search]
                       {:query q :loading? false :results []})}))))

(rf/reg-event-fx
  :mobile/quick-search-fetch
  (fn [_ [_ query]]
    {:http-xhrio {:method :get
                  :uri "/api/v1/expenses/quick-add-search"
                  :params {:type "all" :q query}
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [:mobile/quick-search-success]
                  :on-failure [:mobile/quick-search-failure]}}))

(rf/reg-event-db
  :mobile/quick-search-success
  (fn [db [_ response]]
    (-> db
      (assoc-in [:mobile :quick-search :loading?] false)
      (assoc-in [:mobile :quick-search :results] (vec (or (:results response) []))))))

(rf/reg-event-db
  :mobile/quick-search-failure
  (fn [db _]
    (-> db
      (assoc-in [:mobile :quick-search :loading?] false)
      (assoc-in [:mobile :quick-search :results] []))))

(rf/reg-sub
  :mobile/quick-search-results
  (fn [db _]
    (get-in db [:mobile :quick-search :results] [])))

(rf/reg-sub
  :mobile/quick-search-loading?
  (fn [db _]
    (get-in db [:mobile :quick-search :loading?] false)))

;; ========================================================================
;; Events — fetch payers
;; ========================================================================

(rf/reg-event-fx
  :mobile/fetch-payers
  (fn [_ _]
    {:http-xhrio {:method :get
                  :uri "/api/v1/expenses/payers"
                  :params {:limit 100 :offset 0}
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [:mobile/payers-loaded]
                  :on-failure [:mobile/payers-failed]}}))

(rf/reg-event-db
  :mobile/payers-loaded
  (fn [db [_ response]]
    (assoc-in db [:mobile :payers] (vec (or (:data response) [])))))

(rf/reg-event-db
  :mobile/payers-failed
  (fn [db _]
    (assoc-in db [:mobile :payers] [])))

(rf/reg-sub
  :mobile/payers
  (fn [db _]
    (get-in db [:mobile :payers] [])))

;; ========================================================================
;; Events — expense categories
;; ========================================================================

(rf/reg-event-fx
  :mobile/fetch-expense-categories
  (fn [_ _]
    {:http-xhrio {:method :get
                  :uri "/api/v1/expenses/expense-categories"
                  :params {:limit 500 :offset 0}
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [:mobile/expense-categories-loaded]
                  :on-failure [:mobile/expense-categories-failed]}}))

(rf/reg-event-db
  :mobile/expense-categories-loaded
  (fn [db [_ response]]
    (assoc-in db [:mobile :expense-categories] (vec (or (:data response) [])))))

(rf/reg-event-db
  :mobile/expense-categories-failed
  (fn [db _]
    (assoc-in db [:mobile :expense-categories] [])))

(rf/reg-sub
  :mobile/expense-categories
  (fn [db _]
    (get-in db [:mobile :expense-categories] [])))

(rf/reg-event-fx
  :mobile/fetch-quick-add-history
  (fn [_ [_ supplier-id]]
    {:http-xhrio {:method :get
                  :uri "/api/v1/expenses/quick-add-history"
                  :params (cond-> {:limit 10}
                            supplier-id (assoc :supplier_id (str supplier-id)))
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [:mobile/quick-add-history-loaded]
                  :on-failure [:mobile/quick-add-history-failed]}}))

(rf/reg-event-db
  :mobile/quick-add-history-loaded
  (fn [db [_ response]]
    (assoc-in db [:mobile :quick-add-history]
              {:loading? false
               :loaded? true
               :stores (vec (or (:stores response) []))
               :articles (vec (or (:articles response) []))})))

(rf/reg-event-db
  :mobile/quick-add-history-failed
  (fn [db _]
    (assoc-in db [:mobile :quick-add-history]
              {:loading? false
               :loaded? true
               :stores []
               :articles []})))

(rf/reg-sub
  :mobile/quick-add-history
  (fn [db _]
    (get-in db [:mobile :quick-add-history]
      {:loading? false
       :loaded? false
       :stores []
       :articles []})))

;; ========================================================================
;; Events — co-occurring articles + context suggestions
;; ========================================================================

(rf/reg-event-fx
  :mobile/fetch-cooccurring-articles
  (fn [{:keys [db]} [_ article-ids supplier-id]]
    (let [ids (vec (keep identity article-ids))]
      (if (seq ids)
        {:http-xhrio {:method :get
                      :uri "/api/v1/expenses/quick-add-cooccurring"
                      :params (cond-> {:article_ids (str/join "," (map str ids))}
                                supplier-id (assoc :supplier_id (str supplier-id)))
                      :response-format (ajax/json-response-format {:keywords? true})
                      :on-success [:mobile/cooccurring-articles-loaded]
                      :on-failure [:mobile/cooccurring-articles-failed]}}
        {:db (assoc-in db [:mobile :cooccurring-articles]
                       {:loading? false
                        :results []})}))))

(rf/reg-event-db
  :mobile/cooccurring-articles-loaded
  (fn [db [_ response]]
    (assoc-in db [:mobile :cooccurring-articles]
              {:loading? false
               :results (vec (or (:results response) []))})))

(rf/reg-event-db
  :mobile/cooccurring-articles-failed
  (fn [db _]
    (assoc-in db [:mobile :cooccurring-articles]
              {:loading? false
               :results []})))

(rf/reg-sub
  :mobile/cooccurring-articles
  (fn [db _]
    (get-in db [:mobile :cooccurring-articles]
      {:loading? false
       :results []})))

(rf/reg-event-fx
  :mobile/fetch-context-suggestions
  (fn [_ [_ article-ids]]
    (let [ids (vec (keep identity article-ids))]
      (when (seq ids)
        {:http-xhrio {:method :get
                      :uri "/api/v1/expenses/quick-add-context-suggestions"
                      :params {:article_ids (str/join "," (map str ids))}
                      :response-format (ajax/json-response-format {:keywords? true})
                      :on-success [:mobile/context-suggestions-loaded]
                      :on-failure [:mobile/context-suggestions-failed]}}))))

(rf/reg-event-db
  :mobile/context-suggestions-loaded
  (fn [db [_ response]]
    (assoc-in db [:mobile :context-suggestions]
              {:suppliers (vec (or (:suppliers response) []))
               :stores (vec (or (:stores response) []))
               :categories (vec (or (:categories response) []))})))

(rf/reg-event-db
  :mobile/context-suggestions-failed
  (fn [db _]
    (assoc-in db [:mobile :context-suggestions]
              {:suppliers [] :stores [] :categories []})))

(rf/reg-sub
  :mobile/context-suggestions
  (fn [db _]
    (get-in db [:mobile :context-suggestions] {:suppliers [] :stores [] :categories []})))

;; ========================================================================
;; Events — entity search (autocomplete)
;; ========================================================================

(rf/reg-event-fx
  :mobile/search-entities
  (fn [{:keys [db]} [_ entity-type query filters]]
    (let [search-query (some-> query str str/trim)
          result-path [:mobile :search-results entity-type]
          supplier-id (some-> (:supplier_id filters) str str/trim not-empty)]
      (if (and search-query (>= (count search-query) 2))
        {:http-xhrio {:method :get
                      :uri (if (= entity-type :article)
                             "/api/v1/expenses/quick-add-search"
                             (entity-search-uri entity-type))
                      :params (if (= entity-type :article)
                                (cond-> {:type "article"
                                         :q search-query
                                         :limit 10}
                                  supplier-id (assoc :supplier_id supplier-id))
                                {:search search-query
                                 :per_page 10})
                      :response-format (ajax/json-response-format {:keywords? true})
                      :on-success [:mobile/search-results entity-type]
                      :on-failure [:mobile/search-results entity-type nil]}}
        {:db (assoc-in db result-path [])}))))

(rf/reg-event-db
  :mobile/search-results
  (fn [db [_ entity-type results]]
    (assoc-in db [:mobile :search-results entity-type]
              (vec (or (:data results) (:results results) results [])))))

(rf/reg-sub
  :mobile/search-results
  (fn [db [_ entity-type]]
    (get-in db [:mobile :search-results entity-type] [])))

;; ========================================================================
;; Events — create expense (submit)
;; ========================================================================

(rf/reg-event-fx
  :mobile/create-expense
  (fn [{:keys [db]} [_ form-data]]
    (let [{:keys [items context currency purchased-at payer-id notes]} form-data
          body (manual-entry/prepare-submit-values
                 {:items items
                  :context context
                  :currency (or currency "BAM")
                  :purchased-at purchased-at
                  :payer-id payer-id
                  :notes notes})]
      {:db (-> db
             (assoc-in [:mobile :manual-entry :submitting?] true)
             (assoc-in [:mobile :manual-entry :error] nil))
       :http-xhrio {:method :post
                    :uri "/api/v1/expenses"
                    :params body
                    :format (ajax/json-request-format)
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success [:mobile/create-expense-success]
                    :on-failure [:mobile/create-expense-failure]}})))

(rf/reg-event-fx
  :mobile/create-expense-success
  (fn [{:keys [db]} _]
    {:db (-> db
           (assoc-in [:mobile :manual-entry :submitting?] false)
           (assoc-in [:mobile :manual-entry :error] nil))
     :fx [[:dispatch [:mobile/show-toast :mobile/toast-expense-created]]
          [:dispatch [:mobile/navigate "/m/expenses"]]]}))

(rf/reg-event-db
  :mobile/create-expense-failure
  (fn [db [_ error]]
    (-> db
      (assoc-in [:mobile :manual-entry :submitting?] false)
      (assoc-in [:mobile :manual-entry :error]
        (get-in error [:response :error])))))

(rf/reg-sub
  :mobile/manual-entry-submitting?
  (fn [db _]
    (get-in db [:mobile :manual-entry :submitting?] false)))

(rf/reg-sub
  :mobile/manual-entry-error
  (fn [db _]
    (get-in db [:mobile :manual-entry :error])))

;; ========================================================================
;; Autocomplete dropdown component
;; ========================================================================

(defui autocomplete-field [{:keys [label placeholder entity-type
                                   search-value on-search-change
                                   on-select selected-label]}]
  (let [results (use-subscribe [:mobile/search-results entity-type])
        [show-dropdown? set-show-dropdown!] (uix/use-state false)]
    ($ :div {:class "relative"}
      ($ :label {:class "ds-label text-sm"} label)
      (if selected-label
        ($ :div {:class "ds-input ds-input-bordered w-full flex items-center justify-between"}
          ($ :span {:class "truncate"} selected-label)
          ($ :button {:class "ds-btn ds-btn-ghost ds-btn-xs"
                      :on-click (fn []
                                  (on-select nil nil)
                                  (on-search-change ""))}
            "x"))
        ($ :<>
          ($ :input {:type "text"
                     :class "ds-input ds-input-bordered w-full"
                     :placeholder placeholder
                     :value (or search-value "")
                     :on-focus #(set-show-dropdown! true)
                     :on-blur #(js/setTimeout (fn [] (set-show-dropdown! false)) 200)
                     :on-change (fn [e]
                                  (let [v (.. e -target -value)]
                                    (on-search-change v)
                                    (rf/dispatch [:mobile/search-entities entity-type v])))})
          (when (and show-dropdown? (seq results))
            ($ :ul {:class "absolute z-30 w-full bg-base-100 border border-base-300 rounded-lg shadow-lg max-h-48 overflow-y-auto mt-1"}
              (for [item results]
                ($ :li {:key (or (:id item) (random-uuid))
                        :class "px-3 py-2 hover:bg-base-200 cursor-pointer text-sm"
                        :on-mouse-down (fn []
                                         (on-select (:id item) (result-display-name item) item)
                                         (set-show-dropdown! false))}
                  (result-display-name item))))))))))

;; ========================================================================
;; Quick-add sub-components
;; ========================================================================

(defui search-result-item [{:keys [result on-select t]}]
  (let [entity-type (result-entity-type result)
        label (result-display-name result)
        price (result-article-price result)]
    ($ :div {:class "flex items-center justify-between p-3 bg-base-100 rounded-lg active:bg-base-200 cursor-pointer"
             :on-click #(on-select result)}
      ($ :div {:class "flex items-center gap-2 flex-1 min-w-0"}
        ($ :span {:class (str "px-2 py-0.5 rounded-full text-[10px] font-medium "
                           (get chip-colors entity-type "bg-base-200 text-base-content"))}
          (t (case entity-type
               :supplier :smart-expense/entity-supplier
               :store    :smart-expense/entity-store
               :category :smart-expense/entity-category
               :article  :smart-expense/entity-article
               :smart-expense/entity-article)))
        ($ :span {:class "text-sm truncate"} label))
      (when price
        ($ :span {:class "text-xs text-base-content/50 ml-2 whitespace-nowrap"}
          (str (format-decimal price) " BAM"))))))

(defui context-chip [{:keys [entity-type label on-remove]}]
  ($ :span {:class (str "inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium border "
                     (get chip-colors entity-type "bg-base-200 text-base-content"))}
    label
    (when on-remove
      ($ :button {:class "ml-0.5 hover:opacity-70 cursor-pointer"
                  :type "button"
                  :on-click on-remove}
        "×"))))

(defui item-row [{:keys [item idx selected-supplier-id on-change on-select-article on-remove t]}]
  (let [results (use-subscribe [:mobile/search-results :article])
        [show-dropdown? set-show-dropdown!] (uix/use-state false)
        search-query (some-> (:label item) str str/trim)]
    ($ :div {:class "flex items-start gap-2 bg-base-100 rounded-lg p-2"}
      ($ :div {:class "relative flex-1 min-w-0"}
        ($ :input {:id (str "mobile-manual-item-article-" idx)
                   :type "text"
                   :class "ds-input ds-input-bordered ds-input-sm w-full"
                   :placeholder (t :smart-expense/entity-article)
                   :value (or (:label item) "")
                   :on-focus #(set-show-dropdown! true)
                   :on-blur #(js/setTimeout (fn [] (set-show-dropdown! false)) 200)
                   :on-change (fn [e]
                                (let [value (.. e -target -value)]
                                  (set-show-dropdown! true)
                                  (on-change :label value)
                                  (rf/dispatch [:mobile/search-entities
                                                :article
                                                value
                                                {:supplier_id selected-supplier-id}])))})
        (when (and show-dropdown? (>= (count (or search-query "")) 2) (seq results))
          ($ :ul {:class "absolute z-30 mt-1 max-h-48 w-full overflow-y-auto rounded-lg border border-base-300 bg-base-100 shadow-lg"}
            (for [article results]
              (let [price (result-article-price article)]
                ($ :li {:key (or (:id article) (random-uuid))}
                  ($ :button {:type "button"
                              :class "w-full px-3 py-2 text-left hover:bg-base-200 active:bg-base-200"
                              :on-mouse-down #(.preventDefault %)
                              :on-click (fn []
                                          (on-select-article article)
                                          (rf/dispatch [:mobile/search-entities :article "" nil])
                                          (set-show-dropdown! false))}
                    ($ :div {:class "flex items-center justify-between gap-2"}
                      ($ :span {:class "truncate text-sm"}
                        (result-display-name article))
                      (when price
                        ($ :span {:class "whitespace-nowrap text-xs text-base-content/50"}
                          (str (format-decimal price) " BAM")))))))))))
      ($ :input {:id (str "mobile-manual-item-qty-" idx)
                 :type "number"
                 :class "ds-input ds-input-bordered ds-input-sm w-14 text-center"
                 :placeholder (t :mobile/qty-placeholder)
                 :value (or (:qty item) "")
                 :on-change #(on-change :qty (.. % -target -value))})
      ($ :input {:id (str "mobile-manual-item-price-" idx)
                 :type "number"
                 :step "0.01"
                 :class "ds-input ds-input-bordered ds-input-sm w-20 text-right"
                 :placeholder (t :mobile/price-placeholder)
                 :value (or (:unit-price item) "")
                 :on-change #(on-change :unit-price (.. % -target -value))})
      ($ :button {:id (str "mobile-manual-item-remove-" idx)
                  :class "ds-btn ds-btn-ghost ds-btn-xs text-error flex-shrink-0"
                  :on-click on-remove}
        "×"))))

(defui suggestion-chips [{:keys [suggestions context on-select t]}]
  (let [{:keys [suppliers stores categories]} (visible-mobile-suggestions suggestions context)
        supplier-color-map (build-suggestion-supplier-color-map suppliers stores)]
    (when (or (seq suppliers) (seq stores) (seq categories))
      ($ :div {:class "space-y-2"}
        ($ :p {:class "text-xs text-base-content/50 font-medium uppercase tracking-wide"}
          (t :mobile/suggested-context))
        ($ :div {:class "flex flex-wrap gap-1.5"}
          (when (seq suppliers)
            (for [supplier suppliers]
              ($ :button {:key (str "s-" (:id supplier))
                          :class (str "px-2.5 py-1 rounded-full text-xs font-medium cursor-pointer active:opacity-70 "
                                   (suggestion-chip-class supplier-color-map :supplier supplier))
                          :on-click #(on-select :supplier supplier)}
                (result-display-name supplier))))
          (when (seq stores)
            (for [store stores]
              ($ :button {:key (str "st-" (:id store))
                          :class (str "px-2.5 py-1 rounded-full text-xs font-medium cursor-pointer active:opacity-70 "
                                   (suggestion-chip-class supplier-color-map :store store))
                          :on-click #(on-select :store store)}
                (result-display-name store))))
          (when (seq categories)
            (for [category categories]
              ($ :button {:key (str "c-" (:id category))
                          :class (str "px-2.5 py-1 rounded-full text-xs font-medium cursor-pointer active:opacity-70 "
                                   (:category chip-colors))
                          :on-click #(on-select :category category)}
                (result-display-name category)))))))))

(defui quick-pick-pill [{:keys [label price on-select]}]
  ($ :button {:type "button"
              :class "flex items-center justify-between gap-3 rounded-full border border-base-300 bg-base-100 px-3 py-2 text-sm active:bg-base-200"
              :on-click on-select}
    ($ :span {:class "truncate"} label)
    (when price
      ($ :span {:class "text-xs text-base-content/50 whitespace-nowrap"}
        (str (format-decimal price) " BAM")))))

;; ========================================================================
;; Phase 1 — Search + items
;; ========================================================================

(defui phase-one [{:keys [form set-form! on-add-store on-save on-cancel submitting?
                          submit-disabled? error on-disable-default-category-preselect]}]
  (let [t (use-t)
        history (use-subscribe [:mobile/quick-add-history])
        search-results (use-subscribe [:mobile/quick-search-results])
        loading? (use-subscribe [:mobile/quick-search-loading?])
        expense-categories (use-subscribe [:mobile/expense-categories])
        cooccurring-data (use-subscribe [:mobile/cooccurring-articles])
        payers (use-subscribe [:mobile/payers])
        [search-query set-search-query!] (uix/use-state "")
        items (:items form)
        context (:context form)
        payer-name (some (fn [p]
                           (when (= (str (:id p)) (str (:payer-id form)))
                             (or (:label p) (:display_name p) (:name p))))
                     payers)
        purchased-date (when-let [pa (:purchased-at form)]
                         (let [[y m d] (str/split (first (str/split (str pa) #"T")) #"-")]
                           (str d "." m "." y)))
        selected-supplier-id (get-in context [:supplier :id])
        total (manual-entry/compute-items-total items)
        cooccurring-picks (mapv normalize-article-pick (:results cooccurring-data))
        history-picks (phase-one-history-picks history items context search-query)
        article-history-picks (:articles history-picks)
        category-picks (phase-one-category-picks expense-categories context search-query)

        add-context! (fn [entity-type result]
                       (when (= entity-type :category)
                         (on-disable-default-category-preselect))
                       (set-form! (fn [f]
                                    (update f :context add-context-entry entity-type result))))
        remove-context! (fn [entity-type]
                          (when (= entity-type :category)
                            (on-disable-default-category-preselect))
                          (set-form! (fn [f] (update f :context dissoc entity-type))))
        add-item! (fn [result]
                    (let [price (result-article-price result)
                          label (result-display-name result)]
                      (set-form! (fn [f]
                                   (update f :items conj
                                     {:label label
                                      :article-id (:id result)
                                      :qty "1"
                                      :unit-price (if price (format-decimal price) "")})))))
        update-item! (fn [idx k v]
                       (set-form! (fn [f]
                                    (update f :items
                                      (fn [its]
                                        (let [items* (vec its)
                                              current-item (get items* idx {})]
                                          (assoc items*
                                            idx
                                            (if (= k :label)
                                              (update-item-label-entry current-item v)
                                              (assoc current-item k v)))))))))
        select-item-article! (fn [idx article]
                               (set-form! (fn [f]
                                            (update f :items
                                              (fn [its]
                                                (let [items* (vec its)
                                                      current-item (get items* idx {})]
                                                  (assoc items*
                                                    idx
                                                    (apply-item-article-selection current-item article))))))))
        remove-item! (fn [idx]
                       (set-form! (fn [f]
                                    (update f :items
                                      (fn [its]
                                        (let [remaining (vec (concat (subvec its 0 idx) (subvec its (inc idx))))]
                                          (if (empty? remaining) [] remaining)))))))
        handle-select! (fn [result]
                         (let [etype (result-entity-type result)]
                           (if (= etype :article)
                             (add-item! result)
                             (add-context! etype result))
                           (set-search-query! "")))]

    ($ :div {:class "p-4 pb-44 space-y-4"}
      ($ :div {:class "relative"}
        ($ :input {:type "text"
                   :class "ds-input ds-input-bordered w-full"
                   :placeholder (t :smart-expense/subtitle)
                   :value search-query
                   :auto-focus true
                   :on-change (fn [e]
                                (let [v (.. e -target -value)]
                                  (set-search-query! v)
                                  (rf/dispatch [:mobile/quick-search v])))})
        (when loading?
          ($ :span {:class "absolute right-3 top-1/2 -translate-y-1/2 ds-loading ds-loading-spinner ds-loading-xs"})))

      (when error
        ($ :div {:class "ds-alert ds-alert-error text-sm"}
          ($ :span (if (keyword? error) (t error) error))))

      (when (and (>= (count search-query) 2) (seq search-results))
        ($ :div {:class "bg-base-100 rounded-xl shadow-sm border border-base-300 overflow-hidden divide-y divide-base-200"}
          (for [result search-results]
            ($ search-result-item {:key (str (or (:type result) (:entity-type result)) "-" (:id result))
                                   :result result
                                   :on-select handle-select!
                                   :t t}))))

      (when (and (>= (count search-query) 2) (not loading?) (empty? search-results))
        ($ :p {:class "text-sm text-base-content/50 text-center py-3"}
          (t :mobile/no-results)))

      (when (and (str/blank? search-query) (seq cooccurring-picks))
        ($ :div {:class "space-y-2"}
          ($ :p {:class "text-xs text-base-content/50 font-medium uppercase tracking-wide"}
            (t :smart-expense/frequently-together))
          ($ :div {:class "flex flex-col gap-2"}
            (for [article cooccurring-picks]
              ($ quick-pick-pill {:key (str "article-pick-" (:id article))
                                  :label (:label article)
                                  :price (:last-price article)
                                  :on-select #(add-item! article)})))))

      (when (seq article-history-picks)
        ($ :div {:class "space-y-2"}
          ($ :p {:class "text-sm text-base-content/60"}
            (t :smart-expense/entity-article))
          ($ :div {:class "flex flex-col gap-2"}
            (for [article article-history-picks]
              ($ quick-pick-pill {:key (str "history-article-pick-" (:id article))
                                  :label (:label article)
                                  :price (:last-price article)
                                  :on-select #(add-item! article)})))))

      (when (seq category-picks)
        ($ :div {:class "space-y-2"}
          ($ :p {:class "text-sm text-base-content/60"}
            (t :smart-expense/entity-category))
          ($ :div {:class "flex flex-wrap gap-1.5"}
            (for [category category-picks]
              ($ :button {:key (str "phase-one-category-" (:id category))
                          :type "button"
                          :class (str "px-2.5 py-1 rounded-full text-xs font-medium cursor-pointer active:opacity-70 "
                                   (:category chip-colors))
                          :on-click #(add-context! :category category)}
                (str "📂 " (result-display-name category)))))))

      (when (or (seq context) payer-name purchased-date (:currency form))
        ($ :div {:class "rounded-lg border border-base-300 p-2"}
          ($ :p {:class "text-sm text-base-content/70 mb-1.5 uppercase tracking-wider font-semibold"}
            (t :smart-expense/defaults-label))
          ($ :div {:class "flex flex-wrap gap-1.5"}
            (for [[etype entry] context]
              ($ context-chip {:key (name etype)
                               :entity-type etype
                               :label (context-entry-label entry)
                               :on-remove #(remove-context! etype)}))
            (when payer-name
              ($ context-chip {:key "default-payer"
                               :entity-type :payer
                               :label payer-name
                               :on-remove #(set-form! (fn [f] (dissoc f :payer-id)))}))
            (when purchased-date
              ($ context-chip {:key "default-date"
                               :entity-type :date
                               :label purchased-date
                               :on-remove #(set-form! (fn [f] (dissoc f :purchased-at)))}))
            (when (:currency form)
              ($ context-chip {:key "default-currency"
                               :entity-type :currency
                               :label (:currency form)
                               :on-remove #(set-form! (fn [f] (dissoc f :currency)))})))))

      (when (seq items)
        ($ :div {:class "space-y-3"}
          ($ :div {:class "flex items-center justify-between"}
            ($ :h3 {:class "text-sm font-semibold text-base-content/70"}
              (t :mobile/items-title))
            ($ :button {:class "ds-btn ds-btn-ghost ds-btn-xs"
                        :on-click #(set-form! (fn [f] (update f :items conj {:label "" :qty "1" :unit-price ""})))}
              (t :mobile/add-article)))
          (for [[idx itm] (map-indexed vector items)]
            ($ item-row {:key idx
                         :item itm
                         :idx idx
                         :selected-supplier-id selected-supplier-id
                         :t t
                         :on-change (fn [k v] (update-item! idx k v))
                         :on-select-article #(select-item-article! idx %)
                         :on-remove #(remove-item! idx)}))
          (when (pos? total)
            ($ :div {:class "flex justify-end items-center gap-2 pt-1"}
              ($ :span {:class "text-xs text-base-content/50"} (t :mobile/total-label))
              ($ :span {:class "text-sm font-semibold"} (str (format-decimal total) " " (or (:currency form) "BAM")))))))

      ($ :div {:class "pt-3"}
        ($ :button {:id "mobile-manual-phase1-btn-add-store"
                    :type "button"
                    :class "ds-btn ds-btn-outline w-full h-11 text-base"
                    :disabled (or submitting? (empty? items))
                    :on-click (fn [_] (on-add-store))}
          (t :smart-expense/add-store)))

      ($ :div {:class "fixed bottom-16 left-0 right-0 p-4 bg-base-100 border-t border-base-300"}
        ($ :div {:class "grid grid-cols-2 gap-3"}
          ($ :button {:id "mobile-manual-phase1-btn-cancel"
                      :type "button"
                      :class "ds-btn ds-btn-ghost h-12 text-base"
                      :disabled submitting?
                      :on-click (fn [_] (on-cancel))}
            (t :smart-expense/cancel))
          ($ :button {:id "mobile-manual-phase1-btn-save"
                      :type "button"
                      :class (str "ds-btn ds-btn-primary h-12 text-base "
                               (when submitting? "ds-loading"))
                      :disabled (or submitting? submit-disabled?)
                      :on-click (fn [_] (on-save))}
            (when-not submitting?
              (t :smart-expense/save))))))))
;; ========================================================================
;; Phase 2 — Review + context + submit
;; ========================================================================

(defui phase-two [{:keys [form set-form! on-save on-cancel submitting?
                          submit-disabled? error on-disable-default-category-preselect]}]
  (let [t (use-t)
        payers (use-subscribe [:mobile/payers])
        suggestions (use-subscribe [:mobile/context-suggestions])
        [supplier-query set-supplier-query!] (uix/use-state "")
        [store-query set-store-query!] (uix/use-state "")
        [category-query set-category-query!] (uix/use-state "")
        items (:items form)
        context (:context form)
        payer-id-value (:payer-id form)
        total (manual-entry/compute-items-total items)

        update-field! (fn [k v] (set-form! (fn [f] (assoc f k v))))
        add-context! (fn [entity-type result]
                       (when (= entity-type :category)
                         (on-disable-default-category-preselect))
                       (set-form! (fn [f]
                                    (update f :context add-context-entry entity-type result))))
        remove-context! (fn [entity-type]
                          (when (= entity-type :category)
                            (on-disable-default-category-preselect))
                          (set-form! (fn [f] (update f :context dissoc entity-type))))
        default-payer-id (or payer-id-value
                           (manual-entry/payer-default-id payers))]

    ($ :div {:class "p-4 pb-36 space-y-4"}
      (when error
        ($ :div {:class "ds-alert ds-alert-error text-sm"}
          ($ :span (if (keyword? error) (t error) error))))

      ($ :div {:class "bg-base-100 rounded-xl p-3 shadow-sm"}
        ($ :h3 {:class "text-sm font-semibold text-base-content/70 mb-2"}
          (t :mobile/items-title))
        ($ :div {:class "space-y-1"}
          (for [[idx itm] (map-indexed vector items)
                :when (not (str/blank? (:label itm)))]
            (let [q (or (safe-parse-number (:qty itm)) 1)
                  p (or (safe-parse-number (:unit-price itm)) 0)]
              ($ :div {:key idx :class "flex items-center justify-between text-sm"}
                ($ :span {:class "truncate flex-1 mr-2"} (:label itm))
                ($ :span {:class "text-base-content/60 whitespace-nowrap"}
                  (str q " × " (format-decimal p) " = " (format-decimal (* q p))))))))
        ($ :div {:class "flex justify-end pt-2 border-t border-base-200 mt-2"}
          ($ :span {:class "font-semibold"} (str (format-decimal total) " " (or (:currency form) "BAM")))))

      ($ :div {:class "space-y-2"}
        (when (seq context)
          ($ :div {:class "flex flex-wrap gap-1.5"}
            (for [[etype entry] context]
              ($ context-chip {:key (name etype)
                               :entity-type etype
                               :label (context-entry-label entry)
                               :on-remove #(remove-context! etype)}))))
        ($ suggestion-chips {:suggestions suggestions
                             :context context
                             :on-select add-context!
                             :t t}))

      ($ :div {:class "space-y-3"}
        (when-not (:supplier context)
          ($ autocomplete-field {:label (t :smart-expense/entity-supplier)
                                 :placeholder (t :mobile/search-supplier)
                                 :entity-type :supplier
                                 :search-value supplier-query
                                 :on-search-change set-supplier-query!
                                 :selected-label nil
                                 :on-select (fn [id label raw]
                                              (when id
                                                (add-context! :supplier (assoc raw :id id :label label))))}))
        (when-not (:store context)
          ($ autocomplete-field {:label (t :smart-expense/entity-store)
                                 :placeholder (t :mobile/search-store)
                                 :entity-type :store
                                 :search-value store-query
                                 :on-search-change set-store-query!
                                 :selected-label nil
                                 :on-select (fn [id label raw]
                                              (when id
                                                (add-context! :store (assoc raw :id id :label label))))}))
        (when-not (:category context)
          ($ autocomplete-field {:label (t :smart-expense/entity-category)
                                 :placeholder (t :mobile/search-category)
                                 :entity-type :expense-category
                                 :search-value category-query
                                 :on-search-change set-category-query!
                                 :selected-label nil
                                 :on-select (fn [id label raw]
                                              (when id
                                                (add-context! :category (assoc raw :id id :label label))))})))

      ($ :div
        ($ :label {:class "ds-label text-sm"} (t :smart-expense/payer-label))
        ($ :select {:class "ds-select ds-select-bordered w-full"
                    :value (str (or (:payer-id form) default-payer-id ""))
                    :on-change #(update-field! :payer-id (.. % -target -value))}
          ($ :option {:value ""} (t :smart-expense/payer-select-ph))
          (for [p payers]
            ($ :option {:key (:id p) :value (str (:id p))}
              (or (:label p) (:display_name p) (:display-name p) (:name p))))))

      ($ :div {:class "grid grid-cols-3 gap-3"}
        ($ :div {:class "col-span-2"}
          ($ :label {:class "ds-label text-sm"} (t :smart-expense/date-label))
          ($ :input {:type "datetime-local"
                     :class "ds-input ds-input-bordered w-full"
                     :value (or (:purchased-at form) "")
                     :on-change #(update-field! :purchased-at (.. % -target -value))}))
        ($ :div
          ($ :label {:class "ds-label text-sm"} (t :smart-expense/currency-label))
          ($ :select {:class "ds-select ds-select-bordered w-full"
                      :value (or (:currency form) "BAM")
                      :on-change #(update-field! :currency (.. % -target -value))}
            (for [c currency-options]
              ($ :option {:key c :value c} c)))))

      ($ :div
        ($ :label {:class "ds-label text-sm"} (t :smart-expense/notes-label))
        ($ :textarea {:class "ds-textarea ds-textarea-bordered w-full"
                      :rows 2
                      :placeholder (t :smart-expense/notes-ph)
                      :value (or (:notes form) "")
                      :on-change #(update-field! :notes (.. % -target -value))}))

      ($ :div {:class "fixed bottom-16 left-0 right-0 p-4 bg-base-100 border-t border-base-300"}
        ($ :div {:class "grid grid-cols-2 gap-3"}
          ($ :button {:id "mobile-manual-phase2-btn-cancel"
                      :type "button"
                      :class "ds-btn ds-btn-ghost h-12 text-base"
                      :disabled submitting?
                      :on-click (fn [_] (on-cancel))}
            (t :smart-expense/cancel))
          ($ :button {:id "mobile-manual-phase2-btn-save"
                      :type "button"
                      :class (str "ds-btn ds-btn-primary h-12 text-base "
                               (when submitting? "ds-loading"))
                      :disabled (or submitting? submit-disabled?)
                      :on-click (fn [_] (on-save context))}
            (when-not submitting?
              (t :smart-expense/save))))))))
;; ========================================================================
;; Main page — orchestrates Phase 1 → Phase 2
;; ========================================================================

(defui manual-entry-page []
  (let [t (use-t)
        payers (use-subscribe [:mobile/payers])
        expense-categories (use-subscribe [:mobile/expense-categories])
        submitting? (use-subscribe [:mobile/manual-entry-submitting?])
        remote-error (use-subscribe [:mobile/manual-entry-error])
        [phase set-phase!] (uix/use-state :phase-1)
        [default-category-preselect-enabled? set-default-category-preselect-enabled!] (uix/use-state true)
        [local-error set-local-error!] (uix/use-state nil)
        [form set-form!] (uix/use-state {:items []
                                         :context {}
                                         :purchased-at (current-datetime-local)
                                         :currency "BAM"
                                         :payer-id nil
                                         :notes ""})
        item-article-ids (manual-entry/article-ids-from-items (:items form))
        selected-supplier-id (get-in form [:context :supplier :id])
        selected-category (get-in form [:context :category])
        effective-payer-id (or (:payer-id form)
                             (manual-entry/payer-default-id payers))
        submit-disabled? (boolean (submit-error-key (:items form) effective-payer-id))
        visible-error (or local-error remote-error)
        save-expense! (fn [context]
                        (if-let [error-key (submit-error-key (:items form) effective-payer-id)]
                          (set-local-error! error-key)
                          (do
                            (set-local-error! nil)
                            (rf/dispatch [:mobile/create-expense
                                          (assoc form
                                            :payer-id effective-payer-id
                                            :context context)]))))
        cancel-entry! (fn []
                        (.back js/window.history))]
    (uix/use-effect
      (fn []
        (rf/dispatch [:mobile/fetch-payers])
        (rf/dispatch [:mobile/fetch-expense-categories])
        js/undefined)
      [])

    (uix/use-effect
      (fn []
        (when (and local-error
                (or (seq (:items form))
                  effective-payer-id
                  (seq (:context form))))
          (set-local-error! nil))
        js/undefined)
      [form (:items form) (:context form) effective-payer-id local-error])

    (uix/use-effect
      (fn []
        (when (and effective-payer-id (not (:payer-id form)))
          (set-form! (fn [current-form]
                       (if (:payer-id current-form)
                         current-form
                         (assoc current-form :payer-id effective-payer-id)))))
        js/undefined)
      [form effective-payer-id (:payer-id form)])

    (uix/use-effect
      (fn []
        (rf/dispatch [:mobile/fetch-quick-add-history selected-supplier-id])
        js/undefined)
      [selected-supplier-id])

    (uix/use-effect
      (fn []
        (rf/dispatch [:mobile/fetch-cooccurring-articles item-article-ids selected-supplier-id])
        js/undefined)
      [item-article-ids selected-supplier-id])

    (uix/use-effect
      (fn []
        (when (and (= phase :phase-2) (seq item-article-ids))
          (rf/dispatch [:mobile/fetch-context-suggestions item-article-ids]))
        js/undefined)
      [phase item-article-ids])

    (uix/use-effect
      (fn []
        (when-let [default-category-chip (manual-entry/default-category-chip-to-preselect
                                           expense-categories
                                           nil
                                           default-category-preselect-enabled?
                                           selected-category)]
          (set-form! (fn [current-form]
                       (if (get-in current-form [:context :category])
                         current-form
                         (assoc-in current-form [:context :category] default-category-chip))))
          (set-default-category-preselect-enabled! false))
        js/undefined)
      [expense-categories default-category-preselect-enabled? selected-category])

    ($ :<>
      ($ mobile-header {:title (if (= phase :phase-2)
                                 (t :mobile/back-to-items)
                                 (t :mobile/quick-add-title))
                        :show-back? (= phase :phase-2)
                        :on-back (when (= phase :phase-2)
                                   #(set-phase! :phase-1))})
      (case phase
        :phase-1 ($ phase-one {:form form
                               :set-form! set-form!
                               :submitting? submitting?
                               :submit-disabled? submit-disabled?
                               :error visible-error
                               :on-disable-default-category-preselect #(set-default-category-preselect-enabled! false)
                               :on-add-store #(set-phase! :phase-2)
                               :on-save #(save-expense! (:context form))
                               :on-cancel cancel-entry!})
        :phase-2 ($ phase-two {:form form
                               :set-form! set-form!
                               :submitting? submitting?
                               :submit-disabled? submit-disabled?
                               :error visible-error
                               :on-disable-default-category-preselect #(set-default-category-preselect-enabled! false)
                               :on-save save-expense!
                               :on-cancel cancel-entry!})))))