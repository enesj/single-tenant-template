(ns app.domain.frontend.expenses.components.expense-form
  "Reusable expense form components using the template form system."
  (:require
    [app.domain.frontend.expenses.components.form-fields :refer [current-datetime-local line-items-input new-line-item total-amount-input]]
    [app.domain.frontend.expenses.events.expenses :as expenses-events]
    [app.domain.frontend.expenses.events.payers :as payers-events]
    [app.domain.frontend.expenses.events.suppliers :as suppliers-events]
    [app.domain.frontend.expenses.ui.select-options :as select-options]
    [app.template.frontend.components.form :refer [form]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect]]
    [uix.re-frame :refer [use-subscribe]]))

;; =============================================================================
;; Constants & Data
;; =============================================================================

(def currency-options
  [{:label "BAM" :value "BAM"}
   {:label "EUR" :value "EUR"}
   {:label "USD" :value "USD"}])

(def line-item-columns
  [{:id :raw_label
    :label "Label"
    :type :text
    :placeholder "e.g. Milk, Bread"}
   {:id :qty
    :label "Qty"
    :type :number
    :step "0.01"
    :min "0"
    :width "w-24"}
   {:id :unit_price
    :label "Unit Price"
    :type :number
    :step "0.01"
    :min "0"
    :width "w-32"}
   {:id :line_total
    :label "Line Total"
    :type :number
    :step "0.01"
    :min "0"
    :width "w-32"}])

(defn get-expense-form-spec
  [suppliers payers]
  [{:id :supplier_id
    :type :select
    :label "Supplier"
    :required true
    :placeholder "Select supplier"
    :options (map (fn [s] {:value (:id s) :label (select-options/supplier-label s)}) suppliers)}
   {:id :payer_id
    :type :select
    :label "Payer"
    :required true
    :placeholder "Select payer"
    :options (map (fn [p] {:value (:id p) :label (str (:label p) (when (:type p) (str " (" (:type p) ")")))}) payers)}
   {:id :purchased_at
    :type :datetime-local ;; standard input type
    :label "Purchased at"
    :required true}
   {:id :total_amount
    :component total-amount-input ;; Custom component
    :label "Total amount"
    :required true}
   {:id :currency
    :type :select
    :label "Currency"
    :required true
    :options currency-options}
   {:id :notes
    :type :textarea
    :label "Notes"
    :required false
    :placeholder "Optional notes"}
   {:id :items
    :component line-items-input ;; Custom component
    :label "Line Items"
    :columns line-item-columns}])

;; =============================================================================
;; Form Body Wrapper
;; =============================================================================

(defui expense-form-body
  [{:keys [mode initial-data on-submit on-cancel _loading?]}]
  (let [suppliers (use-subscribe [:expenses/suppliers])
        payers (use-subscribe [:expenses/payers])
        ;; Generate spec with current options
        entity-spec (get-expense-form-spec suppliers payers)

        ;; Prepare initial values
        default-values {:currency "BAM"
                        :purchased_at (current-datetime-local)
                        :items [(new-line-item)]}
        form-initial-values (merge default-values initial-data)

        ;; Handle form submission translation
        handle-submit (fn [{:keys [values]}]
                        (on-submit values))]

    ;; Load dependencies
    (use-effect
      (fn []
        (rf/dispatch [::suppliers-events/load-list {:limit 100 :offset 0}])
        (rf/dispatch [::payers-events/load-list {:limit 100 :offset 0}])
        js/undefined)
      [])

    ($ form
      {:entity-name "expense" ;; Use a generic name or "expense-entry"
       :entity-spec entity-spec
       :editing (= mode :edit)
       :initial-values form-initial-values
       :on-cancel on-cancel
       :on-submit handle-submit
       :button-text (if (= mode :edit) "Update Expense" "Save Expense")})))

;; =============================================================================
;; Modal Wrappers
;; =============================================================================

(defui expense-add-form-modal
  [{:keys [on-success on-cancel]}]
  (let [loading? (use-subscribe [:expenses/form-loading?])]
    ($ expense-form-body
      {:mode :create
       :loading? loading?
       :on-cancel on-cancel
       :on-submit (fn [form-data]
                    (rf/dispatch [::expenses-events/create-entry-modal form-data on-success]))})))

(defui expense-edit-form-modal
  [{:keys [expense-id initial-data on-success on-cancel]}]
  (let [loading? (use-subscribe [:expenses/form-loading?])]
    ($ expense-form-body
      {:mode :edit
       :initial-data initial-data
       :loading? loading?
       :on-cancel on-cancel
       :on-submit (fn [form-data]
                    (rf/dispatch [::expenses-events/update-entry-modal expense-id form-data on-success]))})))
