(ns app.template.frontend.pages.expense-new
  "User-facing expense creation form."
  (:require
    [app.template.frontend.components.button :refer [button]]
    [app.shared.type-conversion :as type-conv]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect use-state]]
    [uix.re-frame :refer [use-subscribe]]))

;; ========================================================================
;; Helpers
;; ========================================================================

(defn- pad-two [value]
  (let [s (str value)]
    (if (< (count s) 2) (str "0" s) s)))

(defn- current-datetime-local []
  (let [now (js/Date.)]
    (str (.getFullYear now)
      "-" (pad-two (inc (.getMonth now)))
      "-" (pad-two (.getDate now))
      "T" (pad-two (.getHours now))
      ":" (pad-two (.getMinutes now)))))

(defn- new-line-item []
  {:id (str (random-uuid))
   :raw_label ""
   :qty ""
   :unit_price ""
   :line_total ""})

(defn- safe-parse-number [value]
  (cond
    (number? value) value
    (string? value) (type-conv/parse-number value)
    :else nil))

(defn- format-decimal [n]
  (when (some? n) (.toFixed n 2)))

(defn- recalc-line-total-if-possible [item]
  (let [qty-num (safe-parse-number (:qty item))
        unit-num (safe-parse-number (:unit_price item))
        line-str (:line_total item)]
    (if (and (number? qty-num) (number? unit-num) (str/blank? (str line-str)))
      (assoc item :line_total (format-decimal (* qty-num unit-num)))
      item)))

(defn- update-line-item [items item-id key value]
  (mapv (fn [item]
          (if (= item-id (:id item))
            (-> item (assoc key value) recalc-line-total-if-possible)
            item))
    items))

(defn- remove-line-item [items item-id]
  (let [remaining (vec (remove #(= item-id (:id %)) items))]
    (if (seq remaining) remaining [(new-line-item)])))

(defn- prepare-line-items [items]
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

(defn- line-items-total [items]
  (->> items
    (map (fn [{:keys [line_total]}] (safe-parse-number line_total)))
    (remove nil?)
    (reduce + 0)))

(def currency-options
  [{:label "BAM" :value "BAM"}
   {:label "EUR" :value "EUR"}
   {:label "USD" :value "USD"}])

(def ^:private amount-tolerance 0.01)

;; ========================================================================
;; Line Item Component
;; ========================================================================

(defui line-item-row [{:keys [item on-change on-remove]}]
  (let [{:keys [id raw_label qty unit_price line_total]} item]
    ($ :div {:class "grid grid-cols-12 gap-2 items-center"}
      ($ :input {:type "text"
                 :class "ds-input ds-input-sm ds-input-bordered col-span-5"
                 :placeholder "Item description"
                 :value (or raw_label "")
                 :on-change #(on-change id :raw_label (.. % -target -value))})
      ($ :input {:type "number"
                 :class "ds-input ds-input-sm ds-input-bordered col-span-2"
                 :placeholder "Qty"
                 :value (or qty "")
                 :on-change #(on-change id :qty (.. % -target -value))})
      ($ :input {:type "number"
                 :step "0.01"
                 :class "ds-input ds-input-sm ds-input-bordered col-span-2"
                 :placeholder "Unit price"
                 :value (or unit_price "")
                 :on-change #(on-change id :unit_price (.. % -target -value))})
      ($ :input {:type "number"
                 :step "0.01"
                 :class "ds-input ds-input-sm ds-input-bordered col-span-2"
                 :placeholder "Total"
                 :value (or line_total "")
                 :on-change #(on-change id :line_total (.. % -target -value))})
      ($ :button {:type "button"
                  :class "ds-btn ds-btn-ghost ds-btn-sm ds-btn-square col-span-1"
                  :on-click #(on-remove id)}
        "×"))))

;; ========================================================================
;; Main Form
;; ========================================================================

(defui expense-new-page []
  (let [suppliers (or (use-subscribe [:user-expenses/suppliers]) [])
        payers (or (use-subscribe [:user-expenses/payers]) [])
        loading? (use-subscribe [:user-expenses/form-loading?])
        form-error (use-subscribe [:user-expenses/form-error])
        [supplier-id set-supplier-id!] (use-state nil)
        [payer-id set-payer-id!] (use-state nil)
        [purchased-at set-purchased-at!] (use-state (current-datetime-local))
        [total-amount set-total-amount!] (use-state "")
        [currency set-currency!] (use-state "BAM")
        [notes set-notes!] (use-state "")
        [line-items set-line-items!] (use-state [(new-line-item)])
        [validation-error set-validation-error!] (use-state nil)]
    
    ;; Load suppliers and payers on mount
    (use-effect
      (fn []
        (rf/dispatch [:user-expenses/fetch-suppliers {:limit 100}])
        (rf/dispatch [:user-expenses/fetch-payers {:limit 100}])
        js/undefined)
      [])
    
    ;; Set default selections
    (use-effect
      (fn []
        (when (and (seq suppliers) (not supplier-id))
          (set-supplier-id! (:id (first suppliers))))
        (when (and (seq payers) (not payer-id))
          (set-payer-id! (:id (first payers)))))
      [supplier-id payer-id suppliers payers])
    
    (let [prepared-items (prepare-line-items line-items)
          computed-total (line-items-total prepared-items)
          parsed-total (safe-parse-number total-amount)
          total-diff (when (and (number? parsed-total) (number? computed-total) (pos? computed-total))
                       (js/Math.abs (- parsed-total computed-total)))
          total-mismatch? (and total-diff (> total-diff amount-tolerance))
          
          handle-line-change (fn [item-id key]
                               (fn [_e value]
                                 (set-line-items! (update-line-item line-items item-id key value))))
          
          handle-submit (fn []
                          (let [effective-total (or parsed-total (when (pos? computed-total) computed-total))
                                has-items? (seq prepared-items)]
                            (cond
                              (or (str/blank? (str supplier-id))
                                (str/blank? (str payer-id))
                                (str/blank? purchased-at))
                              (set-validation-error! "Supplier, payer, and date are required.")
                              
                              (not has-items?)
                              (set-validation-error! "Add at least one line item with a label and total.")
                              
                              (or (nil? effective-total) (<= effective-total 0))
                              (set-validation-error! "Enter a total amount greater than 0.")
                              
                              (and (pos? computed-total) (> (or total-diff 0) amount-tolerance))
                              (set-validation-error!
                                (str "Total (" (format-decimal effective-total)
                                  ") must match line items (" (format-decimal computed-total) ")."))
                              
                              :else
                              (do
                                (set-validation-error! nil)
                                (rf/dispatch [:user-expenses/create-expense
                                              {:supplier_id supplier-id
                                               :payer_id payer-id
                                               :purchased_at purchased-at
                                               :currency currency
                                               :notes notes
                                               :total_amount effective-total
                                               :items (vec prepared-items)}])))))]
      
      ($ :div {:class "min-h-screen bg-base-100"}
        ;; Header
        ($ :header {:class "bg-white border-b border-base-200"}
          ($ :div {:class "max-w-4xl mx-auto px-4 py-4 sm:py-6"}
            ($ :div {:class "flex items-center justify-between"}
              ($ :div
                ($ :div {:class "text-sm ds-breadcrumbs"}
                  ($ :ul
                    ($ :li ($ :a {:href "/expenses"} "Expenses"))
                    ($ :li "New Expense")))
                ($ :h1 {:class "text-xl sm:text-2xl font-bold"} "Add Expense"))
              ($ :div {:class "flex gap-2"}
                ($ button {:btn-type :ghost
                           :on-click #(rf/dispatch [:navigate-to "/expenses"])}
                  "Cancel")
                ($ button {:btn-type :primary
                           :loading? loading?
                           :on-click handle-submit}
                  "Save Expense")))))
        
        ;; Errors
        (when (or validation-error form-error)
          ($ :div {:class "max-w-4xl mx-auto px-4 mt-4"}
            ($ :div {:class "ds-alert ds-alert-error"}
              ($ :span (or validation-error form-error)))))
        
        ;; Form
        ($ :main {:class "max-w-4xl mx-auto px-4 py-6"}
          ($ :div {:class "bg-white rounded-xl shadow-sm border border-base-200 p-6 space-y-6"}
            ;; Basic info section
            ($ :div {:class "grid grid-cols-1 md:grid-cols-2 gap-4"}
              ;; Supplier
              ($ :div
                ($ :label {:class "ds-label"}
                  ($ :span {:class "ds-label-text font-medium"} "Supplier *"))
                ($ :select {:class "ds-select ds-select-bordered w-full"
                            :value (or supplier-id "")
                            :on-change #(set-supplier-id! (.. % -target -value))}
                  ($ :option {:value ""} "Select supplier...")
                  (for [s suppliers]
                    ($ :option {:key (:id s) :value (:id s)}
                      (:display_name s)))))
              
              ;; Payer
              ($ :div
                ($ :label {:class "ds-label"}
                  ($ :span {:class "ds-label-text font-medium"} "Payer *"))
                ($ :select {:class "ds-select ds-select-bordered w-full"
                            :value (or payer-id "")
                            :on-change #(set-payer-id! (.. % -target -value))}
                  ($ :option {:value ""} "Select payer...")
                  (for [p payers]
                    ($ :option {:key (:id p) :value (:id p)}
                      (:label p)))))
              
              ;; Date
              ($ :div
                ($ :label {:class "ds-label"}
                  ($ :span {:class "ds-label-text font-medium"} "Purchase Date *"))
                ($ :input {:type "datetime-local"
                           :class "ds-input ds-input-bordered w-full"
                           :value purchased-at
                           :on-change #(set-purchased-at! (.. % -target -value))}))
              
              ;; Currency
              ($ :div
                ($ :label {:class "ds-label"}
                  ($ :span {:class "ds-label-text font-medium"} "Currency"))
                ($ :select {:class "ds-select ds-select-bordered w-full"
                            :value currency
                            :on-change #(set-currency! (.. % -target -value))}
                  (for [{:keys [label value]} currency-options]
                    ($ :option {:key value :value value} label)))))
            
            ;; Line items section
            ($ :div {:class "border-t pt-6"}
              ($ :div {:class "flex items-center justify-between mb-4"}
                ($ :h3 {:class "font-semibold"} "Line Items")
                ($ :button {:type "button"
                            :class "ds-btn ds-btn-ghost ds-btn-sm"
                            :on-click #(set-line-items! (conj line-items (new-line-item)))}
                  "+ Add Item"))
              
              ;; Column headers
              ($ :div {:class "grid grid-cols-12 gap-2 text-xs text-base-content/70 font-medium mb-2"}
                ($ :span {:class "col-span-5"} "Description")
                ($ :span {:class "col-span-2"} "Qty")
                ($ :span {:class "col-span-2"} "Unit Price")
                ($ :span {:class "col-span-2"} "Total")
                ($ :span {:class "col-span-1"}))
              
              ;; Items
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
              
              ;; Computed total
              (when (pos? computed-total)
                ($ :div {:class "flex justify-end mt-4 text-sm"}
                  ($ :span {:class "text-base-content/70 mr-2"} "Line items total:")
                  ($ :span {:class (str "font-mono font-medium "
                                     (when total-mismatch? "text-warning"))}
                    (format-decimal computed-total) " " currency))))
            
            ;; Total amount
            ($ :div {:class "border-t pt-6 grid grid-cols-1 md:grid-cols-2 gap-4"}
              ($ :div
                ($ :label {:class "ds-label"}
                  ($ :span {:class "ds-label-text font-medium"} "Total Amount *"))
                ($ :input {:type "number"
                           :step "0.01"
                           :class (str "ds-input ds-input-bordered w-full "
                                    (when total-mismatch? "ds-input-warning"))
                           :value total-amount
                           :placeholder (when (pos? computed-total) (str "Auto: " (format-decimal computed-total)))
                           :on-change #(set-total-amount! (.. % -target -value))}))
              
              ($ :div
                ($ :label {:class "ds-label"}
                  ($ :span {:class "ds-label-text font-medium"} "Notes"))
                ($ :textarea {:class "ds-textarea ds-textarea-bordered w-full"
                              :rows 2
                              :value notes
                              :placeholder "Optional notes..."
                              :on-change #(set-notes! (.. % -target -value))})))))))))
