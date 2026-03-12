(ns app.domain.frontend.expenses.pages.user.expense-new
  "Standard expense entry form.

   Renders as a modal body — accepts :on-cancel and :on-success callbacks.
   Used by the expenses list page for adding new expenses."
  (:require
    [app.domain.frontend.expenses.components.manual-expense-form.search :as quick-add-search]
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.i18n :refer [use-t]]
    [app.shared.type-conversion :as type-conv]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect use-state]]
    [uix.re-frame :refer [use-subscribe]]))

(def ^:private form-id "expense-new-form")
(def ^:private amount-tolerance 0.01)
(def ^:private quick-add-entity-types [:supplier :store :category :article])

(defn- pad-two [value]
  (let [s (str value)]
    (if (< (count s) 2) (str "0" s) s)))

(defn current-datetime-local []
  (let [now (js/Date.)]
    (str (.getFullYear now)
      "-" (pad-two (inc (.getMonth now)))
      "-" (pad-two (.getDate now))
      "T" (pad-two (.getHours now))
      ":" (pad-two (.getMinutes now)))))

(defn new-line-item []
  {:id (str (random-uuid))
   :raw_label ""
   :qty ""
   :unit_price ""
   :line_total ""})

(defn- blank-line-item? [{:keys [raw_label qty unit_price line_total]}]
  (and (str/blank? (str raw_label))
    (str/blank? (str qty))
    (str/blank? (str unit_price))
    (str/blank? (str line_total))))

(defn safe-parse-number [value]
  (cond
    (number? value) value
    (string? value) (type-conv/parse-number value)
    :else nil))

(defn- format-decimal [n]
  (when (some? n) (.toFixed n 2)))

(defn recalc-line-total-if-possible [item]
  (let [qty-num (safe-parse-number (:qty item))
        unit-num (safe-parse-number (:unit_price item))
        line-str (:line_total item)]
    (if (and (number? qty-num) (number? unit-num) (str/blank? (str line-str)))
      (assoc item :line_total (format-decimal (* qty-num unit-num)))
      item)))

(defn update-line-item [items item-id key value]
  (mapv (fn [item]
          (if (= item-id (:id item))
            (-> item (assoc key value) recalc-line-total-if-possible)
            item))
    items))

(defn remove-line-item [items item-id]
  (let [remaining (vec (remove #(= item-id (:id %)) items))]
    (if (seq remaining) remaining [(new-line-item)])))

(defn prepare-line-items [items]
  (keep (fn [{:keys [raw_label qty unit_price line_total]}]
          (let [parsed-total (safe-parse-number line_total)]
            (when (and (not (str/blank? raw_label)) parsed-total)
              (let [qty-num (safe-parse-number qty)
                    unit-num (safe-parse-number unit_price)
                    base {:raw_label raw_label :line_total parsed-total}]
                (cond-> base
                  qty-num (assoc :qty qty-num)
                  unit-num (assoc :unit_price unit-num))))))
    items))

(defn line-items-total [items]
  (->> items
    (map (fn [{:keys [line_total]}] (safe-parse-number line_total)))
    (remove nil?)
    (reduce + 0)))

(defn- normalize-selection [entity-type result]
  (case entity-type
    :supplier {:id (:id result)
               :label (or (:label result) (:display_name result) (:display-name result))}
    :store {:id (:id result)
            :label (or (:label result) (:display_name result) (:display-name result))
            :supplier_id (or (:supplier_id result) (:supplier-id result))
            :supplier_display_name (or (:supplier_display_name result) (:supplier-display-name result))}
    :category {:id (:id result)
               :label (or (:label result) (:name result))}
    :article {:id (:id result)
              :label (or (:label result) (:canonical_name result) (:canonical-name result))
              :last_price (or (:last_price result) (:last-price result))}
    result))

(defn apply-supplier-selection [context supplier]
  (let [selected (normalize-selection :supplier supplier)
        selected-store (:store context)
        compatible-store? (or (nil? selected-store)
                            (= (str (:supplier_id selected-store)) (str (:id selected))))]
    (cond-> (assoc context :supplier selected)
      (not compatible-store?) (assoc :store nil))))

(defn clear-supplier-selection [context]
  (-> context
    (assoc :supplier nil)
    (assoc :store nil)))

(defn apply-store-selection [context store]
  (let [selected (normalize-selection :store store)
        supplier-id (:supplier_id selected)]
    (-> context
      (assoc :store selected)
      (cond-> supplier-id
        (assoc :supplier {:id supplier-id
                          :label (or (:supplier_display_name selected)
                                   (:supplier_display_name store)
                                   (:supplier-display-name store)
                                   "")})))))

(defn add-article-line-item [items article]
  (let [selected (normalize-selection :article article)
        label (:label selected)
        unit-price (some-> (:last_price selected) safe-parse-number format-decimal)
        with-article (fn [item]
                       (cond-> item
                         true (assoc :raw_label label)
                         (str/blank? (str (:qty item))) (assoc :qty "1")
                         true (assoc :article_id (:id selected))
                         (and unit-price (str/blank? (str (:unit_price item))))
                         (assoc :unit_price unit-price)))
        blank-index (first (keep-indexed (fn [idx item]
                                           (when (blank-line-item? item) idx))
                             items))]
    (if (some? blank-index)
      (assoc (vec items) blank-index (with-article (nth items blank-index)))
      (conj (vec items) (with-article (new-line-item))))))

(def currency-options
  [{:label "BAM" :value "BAM"}
   {:label "EUR" :value "EUR"}
   {:label "USD" :value "USD"}])

(defui line-item-row [{:keys [item on-change on-remove]}]
  (let [t (use-t)
        {:keys [id raw_label qty unit_price line_total]} item]
    ($ :div {:class "grid grid-cols-12 gap-2 items-center"}
      ($ :input {:type "text"
                 :id (str form-id "-item-" id "-raw-label")
                 :class "ds-input ds-input-sm ds-input-bordered col-span-5"
                 :placeholder (t :expense-new/item-description-ph)
                 :value (or raw_label "")
                 :on-change #(on-change id :raw_label (.. % -target -value))})
      ($ :input {:type "number"
                 :id (str form-id "-item-" id "-qty")
                 :step "0.001"
                 :min "0"
                 :class "ds-input ds-input-sm ds-input-bordered col-span-2"
                 :placeholder (t :expense-new/item-qty-ph)
                 :value (or qty "")
                 :on-change #(on-change id :qty (.. % -target -value))})
      ($ :input {:type "number"
                 :id (str form-id "-item-" id "-unit-price")
                 :step "0.01"
                 :min "0"
                 :class "ds-input ds-input-sm ds-input-bordered col-span-2"
                 :placeholder (t :expense-new/item-unit-price-ph)
                 :value (or unit_price "")
                 :on-change #(on-change id :unit_price (.. % -target -value))})
      ($ :input {:type "number"
                 :id (str form-id "-item-" id "-line-total")
                 :step "0.01"
                 :min "0"
                 :class "ds-input ds-input-sm ds-input-bordered col-span-2"
                 :placeholder (t :expense-new/item-total-ph)
                 :value (or line_total "")
                 :on-change #(on-change id :line_total (.. % -target -value))})
      ($ :button {:type "button"
                  :id (str "btn-remove-expense-new-item-" id)
                  :class "ds-btn ds-btn-ghost ds-btn-sm ds-btn-square col-span-1"
                  :on-click #(on-remove id)}
        "×"))))

(defui quick-add-search-field
  [{:keys [entity-type label placeholder input-id selected on-select on-clear filters helper-text]}]
  (let [t (use-t)
        results (or (use-subscribe [:user-expenses/quick-add-search-results entity-type]) [])
        loading? (boolean (use-subscribe [:user-expenses/quick-add-search-loading? entity-type]))
        [query set-query!] (use-state "")
        query* (str/trim query)
        show-results? (>= (count query*) 2)]
    ($ :div {:class "space-y-2"}
      ($ :label {:class "ds-label"}
        ($ :span {:class "ds-label-text font-medium"} label))
      (when selected
        ($ :div {:class "flex items-center justify-between gap-3 rounded-lg border border-base-300 bg-base-100 px-3 py-2"}
          ($ :div {:class "min-w-0"}
            ($ :div {:class "font-medium truncate"} (:label selected))
            (when-let [supplier-name (:supplier_display_name selected)]
              ($ :div {:class "text-xs text-base-content/60 truncate"} supplier-name)))
          ($ :button {:type "button"
                      :id (str input-id "-clear")
                      :class "ds-btn ds-btn-ghost ds-btn-xs"
                      :on-click (fn [_]
                                  (set-query! "")
                                  (rf/dispatch [:user-expenses/clear-quick-add-search entity-type])
                                  (on-clear))}
            (t :expense-new/clear-selection))))
      ($ :input {:id input-id
                 :type "text"
                 :class "ds-input ds-input-bordered w-full"
                 :placeholder placeholder
                 :value query
                 :on-change (fn [e]
                              (let [value (.. e -target -value)]
                                (set-query! value)
                                (rf/dispatch [:user-expenses/quick-add-search entity-type value filters])))})
      (when helper-text
        ($ :p {:class "text-xs text-base-content/60"} helper-text))
      (when show-results?
        ($ :div {:class "rounded-lg border border-base-200 bg-white shadow-sm overflow-hidden"}
          (cond
            loading?
            ($ :div {:class "px-3 py-2 text-sm text-base-content/60"}
              (t :expense-new/search-loading))

            (seq results)
            ($ :div {:class "divide-y divide-base-200"}
              (for [result results]
                ($ :button {:key (str entity-type "-" (:id result))
                            :id (str input-id "-result-" (:id result))
                            :type "button"
                            :class "w-full px-3 py-2 text-left hover:bg-base-100"
                            :on-click (fn [_]
                                        (set-query! "")
                                        (rf/dispatch [:user-expenses/clear-quick-add-search entity-type])
                                        (on-select result))}
                  ($ :div {:class "flex items-center justify-between gap-3"}
                    ($ :div {:class "font-medium min-w-0 truncate"} (:label result))
                    (when-let [price (quick-add-search/result-last-price result)]
                      (let [global-price? (quick-add-search/global-last-price? result)
                            tooltip (quick-add-search/last-price-tooltip result)]
                        ($ :span {:class (str "text-sm font-mono whitespace-nowrap "
                                           (if global-price?
                                             "text-amber-600 font-semibold"
                                             "text-base-content/50")
                                           (when tooltip " cursor-help"))
                                  :title tooltip}
                          (format-decimal price)))))
                  (when-let [supplier-name (:supplier_display_name result)]
                    ($ :div {:class "text-xs text-base-content/60"} supplier-name)))))

            :else
            ($ :div {:class "px-3 py-2 text-sm text-base-content/60"}
              (t :expense-new/no-results))))))))

(defui standard-expense-form [{:keys [on-cancel on-success]}]
  (let [t (use-t)
        payers (or (use-subscribe [:user-expenses/payers]) [])
        loading? (boolean (use-subscribe [:user-expenses/form-loading?]))
        form-error (use-subscribe [:user-expenses/form-error])
        [context set-context!] (use-state {:supplier nil
                                           :store nil
                                           :expense-category nil})
        [payer-id set-payer-id!] (use-state nil)
        [purchased-at set-purchased-at!] (use-state (current-datetime-local))
        [total-amount set-total-amount!] (use-state "")
        [currency set-currency!] (use-state "BAM")
        [notes set-notes!] (use-state "")
        [line-items set-line-items!] (use-state [(new-line-item)])
        [validation-error set-validation-error!] (use-state nil)]

    ;; Fetch payers on mount (previously done by page init event)
    (use-effect
      (fn []
        (rf/dispatch [:user-expenses/fetch-payers {:limit 100}])
        js/undefined)
      [])

    (use-effect
      (fn []
        #(doseq [entity-type quick-add-entity-types]
           (rf/dispatch [:user-expenses/clear-quick-add-search entity-type])))
      [])

    (use-effect
      (fn []
        (when (and (seq payers) (not payer-id))
          (set-payer-id! (:id (first payers))))
        js/undefined)
      [payer-id payers])

    (let [prepared-items (vec (prepare-line-items line-items))
          computed-total (line-items-total prepared-items)
          parsed-total (safe-parse-number total-amount)
          total-diff (when (and (number? parsed-total) (number? computed-total) (pos? computed-total))
                       (js/Math.abs (- parsed-total computed-total)))
          total-mismatch? (and total-diff (> total-diff amount-tolerance))
          has-context? (or (:supplier context) (:store context) (:expense-category context))
          handle-submit (fn []
                          (let [effective-total (or parsed-total (when (pos? computed-total) computed-total))
                                supplier-id (some-> context :supplier :id)
                                store-id (some-> context :store :id)
                                expense-category-id (some-> context :expense-category :id)]
                            (cond
                              (or (str/blank? (str payer-id))
                                (str/blank? purchased-at))
                              (set-validation-error! (t :expense-new/err-required))

                              (not has-context?)
                              (set-validation-error! (t :expense-new/err-context-required))

                              (empty? prepared-items)
                              (set-validation-error! (t :expense-new/err-no-items))

                              (or (nil? effective-total) (<= effective-total 0))
                              (set-validation-error! (t :expense-new/err-no-total))

                              (and (pos? computed-total) (> (or total-diff 0) amount-tolerance))
                              (set-validation-error!
                                (str "Total (" (format-decimal effective-total)
                                  ") must match line items (" (format-decimal computed-total) ")."))

                              :else
                              (do
                                (set-validation-error! nil)
                                (rf/dispatch [:user-expenses/create-expense-modal
                                              (cond-> {:payer_id payer-id
                                                       :purchased_at purchased-at
                                                       :currency currency
                                                       :notes notes
                                                       :total_amount effective-total
                                                       :items prepared-items}
                                                supplier-id (assoc :supplier_id supplier-id)
                                                store-id (assoc :store_id store-id)
                                                expense-category-id (assoc :expense_category_id expense-category-id))
                                              on-success])))))]
      ($ :div {:class "space-y-6"}
        (when (or validation-error form-error)
          ($ :div {:id (str form-id "-error")
                   :class "ds-alert ds-alert-error"}
            ($ :span (or validation-error form-error))))

        ($ :section {:class "space-y-4"}
          ($ :div
            ($ :h2 {:class "text-lg font-semibold"} (t :expense-new/context-title))
            ($ :p {:class "text-sm text-base-content/70"} (t :expense-new/context-subtitle)))
          ($ :div {:class "grid grid-cols-1 md:grid-cols-2 gap-4"}
            ($ quick-add-search-field
              {:entity-type :supplier
               :label (t :expense-new/supplier-label)
               :placeholder (t :expense-new/supplier-search-ph)
               :input-id (str form-id "-supplier-search")
               :selected (:supplier context)
               :filters {}
               :on-clear #(set-context! clear-supplier-selection)
               :on-select #(set-context! (fn [current]
                                           (apply-supplier-selection current %)))})
            ($ quick-add-search-field
              {:entity-type :store
               :label (t :expense-new/store-label)
               :placeholder (t :expense-new/store-search-ph)
               :input-id (str form-id "-store-search")
               :selected (:store context)
               :filters {:supplier_id (some-> context :supplier :id)}
               :on-clear #(set-context! (fn [current] (assoc current :store nil)))
               :on-select #(set-context! (fn [current]
                                           (apply-store-selection current %)))})
            ($ quick-add-search-field
              {:entity-type :category
               :label (t :expense-new/category-label)
               :placeholder (t :expense-new/category-search-ph)
               :input-id (str form-id "-category-search")
               :selected (:expense-category context)
               :filters {}
               :on-clear #(set-context! (fn [current] (assoc current :expense-category nil)))
               :on-select #(set-context! (fn [current]
                                           (assoc current :expense-category
                                             (normalize-selection :category %))))})
            ($ quick-add-search-field
              {:entity-type :article
               :label (t :expense-new/article-label)
               :placeholder (t :expense-new/article-search-ph)
               :input-id (str form-id "-article-search")
               :selected nil
               :filters {:supplier_id (some-> context :supplier :id)}
               :helper-text (t :expense-new/article-help)
               :on-clear (fn [] nil)
               :on-select #(set-line-items! (fn [items]
                                              (add-article-line-item items %)))}))

          ($ :div {:class "grid grid-cols-1 md:grid-cols-2 gap-4 border-t pt-6"}
            ($ :div
              ($ :label {:class "ds-label"}
                ($ :span {:class "ds-label-text font-medium"} (t :expense-new/payer-label)))
              ($ :select {:id (str form-id "-payer-select")
                          :class "ds-select ds-select-bordered w-full"
                          :value (or payer-id "")
                          :on-change #(set-payer-id! (.. % -target -value))}
                ($ :option {:value ""} (t :expense-new/payer-select))
                (for [p payers]
                  ($ :option {:key (:id p) :value (:id p)}
                    (:label p)))))

            ($ :div
              ($ :label {:class "ds-label"}
                ($ :span {:class "ds-label-text font-medium"} (t :expense-new/date-label)))
              ($ :input {:id (str form-id "-purchased-at")
                         :type "datetime-local"
                         :class "ds-input ds-input-bordered w-full"
                         :value purchased-at
                         :on-change #(set-purchased-at! (.. % -target -value))}))

            ($ :div
              ($ :label {:class "ds-label"}
                ($ :span {:class "ds-label-text font-medium"} (t :expense-new/currency-label)))
              ($ :select {:id (str form-id "-currency-select")
                          :class "ds-select ds-select-bordered w-full"
                          :value currency
                          :on-change #(set-currency! (.. % -target -value))}
                (for [{:keys [label value]} currency-options]
                  ($ :option {:key value :value value} label))))

            ($ :div
              ($ :label {:class "ds-label"}
                ($ :span {:class "ds-label-text font-medium"} (t :expense-new/total-amount-label)))
              ($ :input {:id (str form-id "-total-amount")
                         :type "number"
                         :step "0.01"
                         :class (str "ds-input ds-input-bordered w-full "
                                  (when total-mismatch? "ds-input-warning"))
                         :value total-amount
                         :placeholder (when (pos? computed-total)
                                        (str "Auto: " (format-decimal computed-total)))
                         :on-change #(set-total-amount! (.. % -target -value))})))

          ($ :div {:class "border-t pt-6"}
            ($ :div {:class "flex items-center justify-between mb-4"}
              ($ :h3 {:class "font-semibold"} (t :expense-new/line-items))
              ($ :button {:type "button"
                          :id "btn-add-expense-new-item"
                          :class "ds-btn ds-btn-ghost ds-btn-sm"
                          :on-click #(set-line-items! (conj line-items (new-line-item)))}
                (t :expense-new/add-item)))

            ($ :div {:class "grid grid-cols-12 gap-2 text-xs text-base-content/70 font-medium mb-2"}
              ($ :span {:class "col-span-5"} (t :expense-new/col-description))
              ($ :span {:class "col-span-2"} (t :expense-new/col-qty))
              ($ :span {:class "col-span-2"} (t :expense-new/col-unit-price))
              ($ :span {:class "col-span-2"} (t :expense-new/col-total))
              ($ :span {:class "col-span-1"}))

            ($ :div {:class "space-y-2"}
              (for [item line-items]
                ($ line-item-row {:key (:id item)
                                  :item item
                                  :on-change (fn [item-id key value]
                                               (set-line-items!
                                                 (update-line-item line-items item-id key value)))
                                  :on-remove (fn [item-id]
                                               (set-line-items!
                                                 (remove-line-item line-items item-id)))})))

            (when (pos? computed-total)
              ($ :div {:class "flex justify-end mt-4 text-sm"}
                ($ :span {:class "text-base-content/70 mr-2"} (t :expense-new/line-items-total))
                ($ :span {:class (str "font-mono font-medium "
                                   (when total-mismatch? "text-warning"))}
                  (format-decimal computed-total) " " currency))))

          ($ :div {:class "border-t pt-6"}
            ($ :label {:class "ds-label"}
              ($ :span {:class "ds-label-text font-medium"} (t :expense-new/notes-label)))
            ($ :textarea {:id (str form-id "-notes")
                          :class "ds-textarea ds-textarea-bordered w-full"
                          :rows 2
                          :value notes
                          :placeholder (t :expense-new/notes-placeholder)
                          :on-change #(set-notes! (.. % -target -value))}))

          ($ :div {:class "flex justify-end gap-2 pt-4 border-t border-base-200"}
            ($ button {:id "btn-cancel-expense-new"
                       :btn-type :ghost
                       :on-click on-cancel}
              (t :expense-new/cancel))
            ($ button {:id "btn-save-expense-new"
                       :btn-type :primary
                       :loading? loading?
                       :on-click handle-submit}
              (t :expense-new/save))))))))
