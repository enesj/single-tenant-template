(ns app.mobile.frontend.pages.manual-entry
  "Mobile manual expense entry — full field parity with desktop form."
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
  (when-not (str/blank? s)
    (let [n (js/parseFloat s)]
      (when-not (js/isNaN n) n))))

(def ^:private default-form
  {:supplier-search ""
   :supplier-id nil
   :store-search ""
   :store-id nil
   :payer-search ""
   :payer-id nil
   :category-search ""
   :category-id nil
   :purchased-at nil
   :total-amount ""
   :currency "BAM"
   :notes ""
   :items [{:label "" :qty "1" :unit-price "" :total ""}]})

;; ========================================================================
;; Search / autocomplete events
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
;; Create expense event
;; ========================================================================

(rf/reg-event-fx
  :mobile/create-expense
  (fn [{:keys [db]} [_ form-data]]
    (let [{:keys [supplier-id store-id payer-id category-id
                  purchased-at total-amount currency notes items]} form-data
          line-items (->> items
                       (remove (fn [{:keys [label]}] (str/blank? label)))
                       (map (fn [{:keys [label qty unit-price total]}]
                              (cond-> {:raw_label label}
                                (parse-number total) (assoc :line_total (parse-number total))
                                (parse-number qty) (assoc :qty (parse-number qty))
                                (parse-number unit-price) (assoc :unit_price (parse-number unit-price)))))
                       vec)
          body (cond-> {}
                 supplier-id (assoc :supplier_id (str supplier-id))
                 store-id (assoc :store_id (str store-id))
                 payer-id (assoc :payer_id (str payer-id))
                 category-id (assoc :expense_category_id (str category-id))
                 purchased-at (assoc :purchased_at purchased-at)
                 (parse-number total-amount) (assoc :total_amount (parse-number total-amount))
                 currency (assoc :currency currency)
                 (not (str/blank? notes)) (assoc :notes notes)
                 (seq line-items) (assoc :items line-items))]
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
     :fx [[:dispatch [:mobile/show-toast "Expense created"]]
          [:dispatch [:mobile/navigate "/m/expenses"]]]}))

(rf/reg-event-fx
  :mobile/create-expense-failure
  (fn [{:keys [db]} [_ error]]
    {:db (-> db
           (assoc-in [:mobile :manual-entry :submitting?] false)
           (assoc-in [:mobile :manual-entry :error]
             (or (get-in error [:response :error]) "Failed to create expense")))}))

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
;; Line items sub-form
;; ========================================================================

(defui line-item-row [{:keys [item index on-change on-remove]}]
  ($ :div {:class "bg-base-100 rounded-lg p-3 space-y-2"}
    ($ :div {:class "flex items-center justify-between"}
      ($ :span {:class "text-xs text-base-content/60 font-medium"} (str "Item " (inc index)))
      ($ :button {:class "ds-btn ds-btn-ghost ds-btn-xs text-error"
                  :on-click on-remove}
        "Remove"))
    ($ :input {:type "text"
               :class "ds-input ds-input-bordered ds-input-sm w-full"
               :placeholder "Item name"
               :value (or (:label item) "")
               :on-change #(on-change :label (.. % -target -value))})
    ($ :div {:class "grid grid-cols-3 gap-2"}
      ($ :input {:type "number"
                 :class "ds-input ds-input-bordered ds-input-sm"
                 :placeholder "Qty"
                 :value (or (:qty item) "")
                 :on-change #(on-change :qty (.. % -target -value))})
      ($ :input {:type "number"
                 :class "ds-input ds-input-bordered ds-input-sm"
                 :placeholder "Price"
                 :step "0.01"
                 :value (or (:unit-price item) "")
                 :on-change #(on-change :unit-price (.. % -target -value))})
      ($ :input {:type "number"
                 :class "ds-input ds-input-bordered ds-input-sm"
                 :placeholder "Total"
                 :step "0.01"
                 :value (or (:total item) "")
                 :on-change #(on-change :total (.. % -target -value))}))))

;; ========================================================================
;; Main form page
;; ========================================================================

(defui manual-entry-page []
  (let [t (use-t)
        [form set-form!] (uix/use-state (assoc default-form :purchased-at (current-datetime-local)))
        [submitting? set-submitting!] (uix/use-state false)
        [error set-error!] (uix/use-state nil)

        update-field! (fn [k v] (set-form! (fn [f] (assoc f k v))))
        update-item! (fn [idx k v]
                       (set-form! (fn [f]
                                    (update f :items
                                      (fn [items]
                                        (assoc-in (vec items) [idx k] v))))))
        remove-item! (fn [idx]
                       (set-form! (fn [f]
                                    (update f :items
                                      (fn [items]
                                        (let [remaining (vec (concat (subvec items 0 idx) (subvec items (inc idx))))]
                                          (if (seq remaining) remaining [{:label "" :qty "1" :unit-price "" :total ""}])))))))
        add-item! (fn []
                    (set-form! (fn [f] (update f :items conj {:label "" :qty "1" :unit-price "" :total ""}))))]

    ($ :<>
      ($ mobile-header {:title (t :mobile/add-manually "Add Expense") :show-back? true})

      ($ :div {:class "p-4 pb-24 space-y-4"}
        (when error
          ($ :div {:class "ds-alert ds-alert-error text-sm"} ($ :span error)))

        ;; Supplier
        ($ autocomplete-field
          {:label (t :common/supplier "Supplier")
           :placeholder (t :mobile/search-supplier "Search supplier...")
           :entity-type :supplier
           :search-value (:supplier-search form)
           :on-search-change #(update-field! :supplier-search %)
           :on-select (fn [id label]
                        (update-field! :supplier-id id)
                        (update-field! :supplier-search (or label "")))
           :selected-label (when (:supplier-id form) (:supplier-search form))})

        ;; Store
        ($ autocomplete-field
          {:label (t :common/store "Store")
           :placeholder (t :mobile/search-store "Search store...")
           :entity-type :store
           :search-value (:store-search form)
           :on-search-change #(update-field! :store-search %)
           :on-select (fn [id label]
                        (update-field! :store-id id)
                        (update-field! :store-search (or label "")))
           :selected-label (when (:store-id form) (:store-search form))})

        ;; Date
        ($ :div
          ($ :label {:class "ds-label text-sm"} (t :common/date "Date"))
          ($ :input {:type "datetime-local"
                     :class "ds-input ds-input-bordered w-full"
                     :value (or (:purchased-at form) "")
                     :on-change #(update-field! :purchased-at (.. % -target -value))}))

        ;; Amount + Currency (side by side)
        ($ :div {:class "grid grid-cols-3 gap-3"}
          ($ :div {:class "col-span-2"}
            ($ :label {:class "ds-label text-sm"} (t :common/amount "Amount"))
            ($ :input {:type "number"
                       :step "0.01"
                       :class "ds-input ds-input-bordered w-full"
                       :placeholder "0.00"
                       :value (or (:total-amount form) "")
                       :on-change #(update-field! :total-amount (.. % -target -value))}))
          ($ :div
            ($ :label {:class "ds-label text-sm"} (t :common/currency "Currency"))
            ($ :select {:class "ds-select ds-select-bordered w-full"
                        :value (or (:currency form) "BAM")
                        :on-change #(update-field! :currency (.. % -target -value))}
              ($ :option {:value "BAM"} "BAM")
              ($ :option {:value "EUR"} "EUR")
              ($ :option {:value "USD"} "USD")
              ($ :option {:value "GBP"} "GBP")
              ($ :option {:value "CHF"} "CHF")
              ($ :option {:value "HRK"} "HRK")
              ($ :option {:value "RSD"} "RSD"))))

        ;; Payer
        ($ autocomplete-field
          {:label (t :common/payer "Payer")
           :placeholder (t :mobile/search-payer "Search payer...")
           :entity-type :payer
           :search-value (:payer-search form)
           :on-search-change #(update-field! :payer-search %)
           :on-select (fn [id label]
                        (update-field! :payer-id id)
                        (update-field! :payer-search (or label "")))
           :selected-label (when (:payer-id form) (:payer-search form))})

        ;; Category
        ($ autocomplete-field
          {:label (t :common/category "Category")
           :placeholder (t :mobile/search-category "Search category...")
           :entity-type :expense-categorie
           :search-value (:category-search form)
           :on-search-change #(update-field! :category-search %)
           :on-select (fn [id label]
                        (update-field! :category-id id)
                        (update-field! :category-search (or label "")))
           :selected-label (when (:category-id form) (:category-search form))})

        ;; Notes
        ($ :div
          ($ :label {:class "ds-label text-sm"} (t :common/notes "Notes"))
          ($ :textarea {:class "ds-textarea ds-textarea-bordered w-full"
                        :rows 2
                        :placeholder (t :mobile/notes-placeholder "Optional notes...")
                        :value (or (:notes form) "")
                        :on-change #(update-field! :notes (.. % -target -value))}))

        ;; Line Items
        ($ :div {:class "space-y-3"}
          ($ :div {:class "flex items-center justify-between"}
            ($ :h3 {:class "text-sm font-semibold text-base-content/70 uppercase tracking-wide"}
              (t :common/line-items "Line Items"))
            ($ :button {:class "ds-btn ds-btn-ghost ds-btn-xs"
                        :on-click add-item!}
              "+ Add Item"))
          (for [[idx item] (map-indexed vector (:items form))]
            ($ line-item-row
              {:key idx
               :item item
               :index idx
               :on-change (fn [k v] (update-item! idx k v))
               :on-remove #(remove-item! idx)})))

        ;; Submit button (fixed at bottom)
        ($ :div {:class "fixed bottom-16 left-0 right-0 p-4 bg-base-100 border-t border-base-300"}
          ($ :button
            {:class (str "ds-btn ds-btn-primary w-full h-12 text-base "
                      (when submitting? "ds-loading"))
             :disabled submitting?
             :on-click (fn []
                         (set-submitting! true)
                         (set-error! nil)
                         (rf/dispatch [:mobile/create-expense form]))}
            (when-not submitting?
              (t :common/save "Save Expense"))))))))
