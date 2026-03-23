(ns app.mobile.frontend.pages.manual-entry
  "Mobile quick-add expense — two-phase smart workflow (search → review → submit)."
  (:require
    [ajax.core :as ajax]
    [app.mobile.frontend.components.header :refer [mobile-header]]
    [app.template.frontend.i18n :refer [use-t]]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui] :as uix]
    [uix.re-frame :refer [use-subscribe]]))

;; ========================================================================
;; Helpers
;; ========================================================================

(defn- current-datetime-local []
  (let [now (js/Date.)
        pad (fn [n] (let [s (str n)] (if (< (count s) 2) (str "0" s) s)))]
    (str (.getFullYear now)
      "-" (pad (inc (.getMonth now)))
      "-" (pad (.getDate now))
      "T" (pad (.getHours now))
      ":" (pad (.getMinutes now)))))

(defn- parse-number [s]
  (when-not (str/blank? (str s))
    (let [n (js/parseFloat (str s))]
      (when-not (js/isNaN n) n))))

(defn- format-amount [n]
  (when n
    (.toFixed (js/Number. n) 2)))

(defn- compute-items-total
  "Sum qty * unit-price for all items."
  [items]
  (reduce (fn [acc {:keys [qty unit-price]}]
            (let [q (or (parse-number qty) 1)
                  p (or (parse-number unit-price) 0)]
              (+ acc (* q p))))
    0 items))

(def ^:private chip-colors
  {:supplier "bg-blue-100 text-blue-800"
   :store    "bg-green-100 text-green-800"
   :category "bg-purple-100 text-purple-800"
   :article  "bg-amber-100 text-amber-800"})

(def ^:private currency-options
  ["BAM" "EUR" "USD" "GBP" "CHF" "HRK" "RSD"])

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
;; Events — context suggestions (Phase 2)
;; ========================================================================

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
               :stores    (vec (or (:stores response) []))
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
;; Events — entity search (for autocomplete-field, used by receipt_review)
;; ========================================================================

(rf/reg-event-fx
  :mobile/search-entities
  (fn [_ [_ entity-type query]]
    (when (and query (>= (count query) 2))
      {:http-xhrio {:method :get
                    :uri (str "/api/v1/expenses/" (name entity-type) "s")
                    :params {:search query :per_page 10}
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success [:mobile/search-results entity-type]
                    :on-failure [:mobile/search-results entity-type nil]}})))

(rf/reg-event-db
  :mobile/search-results
  (fn [db [_ entity-type results]]
    (assoc-in db [:mobile :search-results entity-type]
              (or (:data results) results))))

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
    (let [{:keys [items context payer-id purchased-at currency notes]} form-data
          line-items (->> items
                       (keep (fn [{:keys [label qty unit-price]}]
                               (let [q (or (parse-number qty) 1)
                                     p (or (parse-number unit-price) 0)
                                     total (* q p)]
                                 (when (and (not (str/blank? (str label))) (pos? total))
                                   {:raw_label (str label)
                                    :qty q
                                    :unit_price p
                                    :line_total total}))))
                       vec)
          total-amount (reduce + 0 (map :line_total line-items))
          body (cond-> {:total_amount total-amount
                        :currency (or currency "BAM")
                        :items line-items}
                 payer-id (assoc :payer_id (str payer-id))
                 purchased-at (assoc :purchased_at purchased-at)
                 (get-in context [:supplier :id]) (assoc :supplier_id (str (get-in context [:supplier :id])))
                 (get-in context [:store :id]) (assoc :store_id (str (get-in context [:store :id])))
                 (get-in context [:category :id]) (assoc :expense_category_id (str (get-in context [:category :id])))
                 (not (str/blank? notes)) (assoc :notes notes))]
      {:db (assoc-in db [:mobile :manual-entry :submitting?] true)
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
    {:db (assoc-in db [:mobile :manual-entry :submitting?] false)
     :fx [[:dispatch [:mobile/show-toast :mobile/toast-expense-created]]
          [:dispatch [:mobile/navigate "/m/expenses"]]]}))

(rf/reg-event-fx
  :mobile/create-expense-failure
  (fn [{:keys [db]} [_ error]]
    {:db (-> db
           (assoc-in [:mobile :manual-entry :submitting?] false)
           (assoc-in [:mobile :manual-entry :error]
             (get-in error [:response :error])))}))

;; ========================================================================
;; Autocomplete dropdown component (shared — used by receipt_review.cljs)
;; ========================================================================

(defui autocomplete-field [{:keys [label placeholder entity-type
                                   search-value on-search-change
                                   on-select selected-label]}]
  (let [results (use-subscribe [:mobile/search-results entity-type])
        [show-dropdown? set-show-dropdown!] (uix/use-state false)]
    ($ :div {:class "relative"}
      ($ :label {:class "ds-label text-sm"} label)
      (if selected-label
        ;; Show selected value with clear button
        ($ :div {:class "ds-input ds-input-bordered w-full flex items-center justify-between"}
          ($ :span {:class "truncate"} selected-label)
          ($ :button {:class "ds-btn ds-btn-ghost ds-btn-xs"
                      :on-click (fn []
                                  (on-select nil nil)
                                  (on-search-change ""))}
            "x"))
        ;; Search input
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
          ;; Dropdown results
          (when (and show-dropdown? (seq results))
            ($ :ul {:class "absolute z-30 w-full bg-base-100 border border-base-300 rounded-lg shadow-lg max-h-48 overflow-y-auto mt-1"}
              (for [item results]
                ($ :li {:key (or (:id item) (random-uuid))
                        :class "px-3 py-2 hover:bg-base-200 cursor-pointer text-sm"
                        :on-mouse-down (fn []
                                         (on-select (:id item)
                                           (or (:display_name item)
                                             (:display-name item)
                                             (:name item)
                                             (:canonical_name item)
                                             (:canonical-name item)))
                                         (set-show-dropdown! false))}
                  (or (:display_name item)
                    (:display-name item)
                    (:name item)
                    (:canonical_name item)
                    (:canonical-name item)))))))))))

;; ========================================================================
;; Quick-add sub-components
;; ========================================================================

(defn- result-display-name [result]
  (or (:name result) (:display_name result) (:canonical_name result) ""))

(defn- result-entity-type [result]
  (keyword (or (:type result) "article")))

(defui search-result-item [{:keys [result on-select t]}]
  (let [entity-type (result-entity-type result)
        label (result-display-name result)
        price-info (:price_info result)]
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
      (when (and price-info (:avg_price price-info))
        ($ :span {:class "text-xs text-base-content/50 ml-2 whitespace-nowrap"}
          (str (format-amount (:avg_price price-info)) " BAM"))))))

(defui context-chip [{:keys [entity-type label on-remove]}]
  ($ :span {:class (str "inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium "
                     (get chip-colors entity-type "bg-base-200 text-base-content"))}
    label
    (when on-remove
      ($ :button {:class "ml-0.5 hover:opacity-70"
                  :on-click on-remove}
        "\u00d7"))))

(defui item-row [{:keys [item index on-change on-remove t]}]
  ($ :div {:class "flex items-center gap-2 bg-base-100 rounded-lg p-2"}
    ;; Label (article name)
    ($ :div {:class "flex-1 min-w-0"}
      ($ :input {:type "text"
                 :class "ds-input ds-input-bordered ds-input-sm w-full"
                 :placeholder (t :smart-expense/entity-article)
                 :value (or (:label item) "")
                 :on-change #(on-change :label (.. % -target -value))}))
    ;; Qty
    ($ :input {:type "number"
               :class "ds-input ds-input-bordered ds-input-sm w-14 text-center"
               :placeholder (t :mobile/qty-placeholder)
               :value (or (:qty item) "")
               :on-change #(on-change :qty (.. % -target -value))})
    ;; Unit price
    ($ :input {:type "number"
               :step "0.01"
               :class "ds-input ds-input-bordered ds-input-sm w-20 text-right"
               :placeholder (t :mobile/price-placeholder)
               :value (or (:unit-price item) "")
               :on-change #(on-change :unit-price (.. % -target -value))})
    ;; Remove
    ($ :button {:class "ds-btn ds-btn-ghost ds-btn-xs text-error flex-shrink-0"
                :on-click on-remove}
      "\u00d7")))

(defui suggestion-chips [{:keys [suggestions context on-select t]}]
  (let [{:keys [suppliers stores categories]} suggestions
        show-suppliers (and (seq suppliers) (not (:supplier context)))
        show-stores (and (seq stores) (not (:store context)))
        show-categories (and (seq categories) (not (:category context)))]
    (when (or show-suppliers show-stores show-categories)
      ($ :div {:class "space-y-2"}
        ($ :p {:class "text-xs text-base-content/50 font-medium uppercase tracking-wide"}
          (t :mobile/suggested-context))
        ($ :div {:class "flex flex-wrap gap-1.5"}
          (when show-suppliers
            (for [s (take 3 suppliers)]
              ($ :button {:key (str "s-" (:id s))
                          :class (str "px-2.5 py-1 rounded-full text-xs font-medium cursor-pointer active:opacity-70 "
                                   (:supplier chip-colors))
                          :on-click #(on-select :supplier s)}
                (or (:name s) (:display_name s)))))
          (when show-stores
            (for [s (take 3 stores)]
              ($ :button {:key (str "st-" (:id s))
                          :class (str "px-2.5 py-1 rounded-full text-xs font-medium cursor-pointer active:opacity-70 "
                                   (:store chip-colors))
                          :on-click #(on-select :store s)}
                (or (:name s) (:display_name s)))))
          (when show-categories
            (for [c (take 3 categories)]
              ($ :button {:key (str "c-" (:id c))
                          :class (str "px-2.5 py-1 rounded-full text-xs font-medium cursor-pointer active:opacity-70 "
                                   (:category chip-colors))
                          :on-click #(on-select :category c)}
                (or (:name c) (:display_name c))))))))))

;; ========================================================================
;; Phase 1 — Search + items + context
;; ========================================================================

(defui phase-one [{:keys [form set-form! on-continue]}]
  (let [t (use-t)
        search-results (use-subscribe [:mobile/quick-search-results])
        loading? (use-subscribe [:mobile/quick-search-loading?])
        [search-query set-search-query!] (uix/use-state "")
        items (:items form)
        context (:context form)
        total (compute-items-total items)

        add-context! (fn [entity-type result]
                       (set-form! (fn [f]
                                    (assoc-in f [:context entity-type]
                                              {:id (or (:id result) (:entity_id result))
                                               :name (result-display-name result)}))))
        remove-context! (fn [entity-type]
                          (set-form! (fn [f] (update f :context dissoc entity-type))))
        add-item! (fn [result]
                    (let [price-info (:price_info result)
                          unit-price (or (:avg_price price-info) "")]
                      (set-form! (fn [f]
                                   (update f :items conj
                                     {:label (result-display-name result)
                                      :article-id (:id result)
                                      :qty "1"
                                      :unit-price (if (number? unit-price) (str unit-price) unit-price)})))))
        update-item! (fn [idx k v]
                       (set-form! (fn [f]
                                    (update f :items
                                      (fn [its] (assoc-in (vec its) [idx k] v))))))
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

    ($ :div {:class "p-4 pb-32 space-y-4"}
      ;; Search input
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

      ;; Search results dropdown
      (when (and (>= (count search-query) 2) (seq search-results))
        ($ :div {:class "bg-base-100 rounded-xl shadow-sm border border-base-300 overflow-hidden divide-y divide-base-200"}
          (for [result search-results]
            ($ search-result-item {:key (str (:type result) "-" (:id result))
                                   :result result
                                   :on-select handle-select!
                                   :t t}))))

      ;; No results
      (when (and (>= (count search-query) 2) (not loading?) (empty? search-results))
        ($ :p {:class "text-sm text-base-content/50 text-center py-3"}
          (t :mobile/no-results)))

      ;; Context chips
      (when (seq context)
        ($ :div {:class "flex flex-wrap gap-1.5"}
          (for [[etype {:keys [name]}] context]
            ($ context-chip {:key (cljs.core/name etype)
                             :entity-type etype
                             :label name
                             :on-remove #(remove-context! etype)}))))

      ;; Items list
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
                         :index idx
                         :t t
                         :on-change (fn [k v] (update-item! idx k v))
                         :on-remove #(remove-item! idx)}))
          ;; Running total
          (when (pos? total)
            ($ :div {:class "flex justify-end items-center gap-2 pt-1"}
              ($ :span {:class "text-xs text-base-content/50"} (t :mobile/total-label))
              ($ :span {:class "text-sm font-semibold"} (str (format-amount total) " " (or (:currency form) "BAM")))))))

      ;; Continue button (fixed)
      ($ :div {:class "fixed bottom-16 left-0 right-0 p-4 bg-base-100 border-t border-base-300"}
        ($ :button
          {:class "ds-btn ds-btn-primary w-full h-12 text-base"
           :disabled (and (empty? items) (empty? context))
           :on-click on-continue}
          (t :mobile/continue))))))

;; ========================================================================
;; Phase 2 — Review + payer + date + notes + submit
;; ========================================================================

(defui phase-two [{:keys [form set-form! on-back]}]
  (let [t (use-t)
        payers (use-subscribe [:mobile/payers])
        suggestions (use-subscribe [:mobile/context-suggestions])
        [submitting? set-submitting!] (uix/use-state false)
        [error set-error!] (uix/use-state nil)
        items (:items form)
        context (:context form)
        total (compute-items-total items)

        update-field! (fn [k v] (set-form! (fn [f] (assoc f k v))))
        add-context! (fn [entity-type result]
                       (set-form! (fn [f]
                                    (assoc-in f [:context entity-type]
                                              {:id (:id result)
                                               :name (or (:name result) (:display_name result))}))))
        remove-context! (fn [entity-type]
                          (set-form! (fn [f] (update f :context dissoc entity-type))))
        default-payer-id (or (:payer-id form)
                           (some #(when (or (:is_default %) (:is-default %)) (:id %)) payers)
                           (:id (first payers)))]

    ;; Set default payer when payers load
    (uix/use-effect
      (fn []
        (when (and default-payer-id (not (:payer-id form)))
          (update-field! :payer-id default-payer-id)))
      [update-field! default-payer-id form])

    ($ :div {:class "p-4 pb-32 space-y-4"}
      ;; Back button
      ($ :button {:class "ds-btn ds-btn-ghost ds-btn-sm gap-1"
                  :on-click on-back}
        (t :mobile/back-to-items))

      (when error
        ($ :div {:class "ds-alert ds-alert-error text-sm"} ($ :span (if (keyword? error) (t error) error))))

      ;; Items summary
      ($ :div {:class "bg-base-100 rounded-xl p-3 shadow-sm"}
        ($ :h3 {:class "text-sm font-semibold text-base-content/70 mb-2"}
          (t :mobile/items-title))
        ($ :div {:class "space-y-1"}
          (for [[idx itm] (map-indexed vector items)]
            (let [q (or (parse-number (:qty itm)) 1)
                  p (or (parse-number (:unit-price itm)) 0)]
              ($ :div {:key idx :class "flex items-center justify-between text-sm"}
                ($ :span {:class "truncate flex-1 mr-2"} (or (:label itm) "—"))
                ($ :span {:class "text-base-content/60 whitespace-nowrap"}
                  (str q " × " (format-amount p) " = " (format-amount (* q p))))))))
        ($ :div {:class "flex justify-end pt-2 border-t border-base-200 mt-2"}
          ($ :span {:class "font-semibold"} (str (format-amount total) " " (or (:currency form) "BAM")))))

      ;; Context chips + suggestions
      ($ :div {:class "space-y-2"}
        (when (seq context)
          ($ :div {:class "flex flex-wrap gap-1.5"}
            (for [[etype {:keys [name]}] context]
              ($ context-chip {:key (cljs.core/name etype)
                               :entity-type etype
                               :label name
                               :on-remove #(remove-context! etype)}))))
        ($ suggestion-chips {:suggestions suggestions
                             :context context
                             :on-select add-context!
                             :t t}))

      ;; Payer
      ($ :div
        ($ :label {:class "ds-label text-sm"} (t :smart-expense/payer-label))
        ($ :select {:class "ds-select ds-select-bordered w-full"
                    :value (str (or (:payer-id form) default-payer-id ""))
                    :on-change #(update-field! :payer-id (.. % -target -value))}
          ($ :option {:value ""} (t :smart-expense/payer-select-ph))
          (for [p payers]
            ($ :option {:key (:id p) :value (str (:id p))}
              (or (:display_name p) (:display-name p) (:name p))))))

      ;; Date + Currency
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

      ;; Notes
      ($ :div
        ($ :label {:class "ds-label text-sm"} (t :smart-expense/notes-label))
        ($ :textarea {:class "ds-textarea ds-textarea-bordered w-full"
                      :rows 2
                      :placeholder (t :smart-expense/notes-ph)
                      :value (or (:notes form) "")
                      :on-change #(update-field! :notes (.. % -target -value))}))

      ;; Submit button (fixed)
      ($ :div {:class "fixed bottom-16 left-0 right-0 p-4 bg-base-100 border-t border-base-300"}
        ($ :button
          {:class (str "ds-btn ds-btn-primary w-full h-12 text-base "
                    (when submitting? "ds-loading"))
           :disabled submitting?
           :on-click (fn []
                       (let [prepared-items (->> items
                                              (keep (fn [{:keys [label qty unit-price]}]
                                                      (let [q (or (parse-number qty) 1)
                                                            p (or (parse-number unit-price) 0)]
                                                        (when (and (not (str/blank? (str label))) (pos? (* q p)))
                                                          {:label label :qty qty :unit-price unit-price}))))
                                              vec)
                             pid (or (:payer-id form) default-payer-id)]
                         (cond
                           (empty? prepared-items)
                           (set-error! :smart-expense/err-no-items)

                           (empty? context)
                           (set-error! :smart-expense/err-no-context)

                           (str/blank? (str pid))
                           (set-error! :smart-expense/err-no-payer)

                           :else
                           (do (set-submitting! true)
                             (set-error! nil)
                             (rf/dispatch [:mobile/create-expense
                                           (assoc form :payer-id pid)])))))}
          (when-not submitting?
            (t :smart-expense/save)))))))

;; ========================================================================
;; Main page — orchestrates Phase 1 → Phase 2
;; ========================================================================

(defui manual-entry-page []
  (let [t (use-t)
        [phase set-phase!] (uix/use-state :phase-1)
        [form set-form!] (uix/use-state {:items []
                                         :context {}
                                         :purchased-at (current-datetime-local)
                                         :currency "BAM"
                                         :payer-id nil
                                         :notes ""})]
    ;; Fetch payers on mount
    (uix/use-effect
      (fn [] (rf/dispatch [:mobile/fetch-payers]))
      [])

    ;; When entering Phase 2, fetch context suggestions based on article IDs
    (uix/use-effect
      (fn []
        (when (= phase :phase-2)
          (let [article-ids (keep :article-id (:items form))]
            (when (seq article-ids)
              (rf/dispatch [:mobile/fetch-context-suggestions article-ids])))))
      [form phase])

    ($ :<>
      ($ mobile-header {:title (t :mobile/quick-add-title)
                        :show-back? true})
      (case phase
        :phase-1 ($ phase-one {:form form
                               :set-form! set-form!
                               :on-continue #(set-phase! :phase-2)})
        :phase-2 ($ phase-two {:form form
                               :set-form! set-form!
                               :on-back #(set-phase! :phase-1)})))))
