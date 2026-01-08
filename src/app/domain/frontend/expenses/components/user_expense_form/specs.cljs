(ns app.domain.frontend.expenses.components.user-expense-form.specs
  "Expense form field spec builder for user-scoped expense forms."
  (:require
    [app.domain.frontend.expenses.components.form-fields :as form-fields :refer [line-items-input]]
    [app.domain.frontend.expenses.components.user-expense-form.inline-supplier-select :refer [user-supplier-select-with-inline-create]]
    [app.domain.frontend.expenses.ui.select-options :as select-options]
    [clojure.string :as str]))

(def ^:private currency-options
  [{:label "BAM" :value "BAM"}
   {:label "EUR" :value "EUR"}
   {:label "USD" :value "USD"}])

(def ^:private line-item-columns
  [{:id :raw_label
    :label "Label"
    :type :text
    :placeholder "e.g. Milk, Bread"}
   {:id :qty
    :label "Qty"
    :type :number
    :step "0.001"
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
  "Return a field spec vector for the user expense form.

  Optional opts support the receipt-approval UX."
  ([suppliers payers]
   (get-expense-form-spec suppliers payers nil))
  ([suppliers payers {:keys [receipt-approval? supplier-guess receipt receipt-id]}]
   (let [receipt-id* (or receipt-id (:id receipt))
         receipt-id-str (some-> receipt-id* str)
         receipt-supplier-guess (some-> (or (:supplier-guess receipt) supplier-guess)
                                  str
                                  str/trim
                                  not-empty)
         receipt-total-guess (:total-amount-guess receipt)
         totals-match? (:total-guess-equals-lines-total-guess? receipt)]
     [{:id :supplier_id
       :type :select
       :component user-supplier-select-with-inline-create
       :label "Supplier"
       :required true
       :placeholder "Select supplier"
       :create-default-display-name (when receipt-approval?
                                      receipt-supplier-guess)
       :receipt-id receipt-id-str
       :receipt-supplier-guess receipt-supplier-guess
       :options (map (fn [s]
                       {:value (:id s)
                        :label (select-options/supplier-label s)})
                  suppliers)}
      {:id :payer_id
       :type :select
       :label "Payer"
       :required true
       :placeholder "Select payer"
       :options (map (fn [p]
                       {:value (:id p)
                        :label (str (:label p)
                                 (when (:type p)
                                   (str " (" (:type p) ")")))})
                  payers)}
      {:id :purchased_at
       :type :datetime-local
       :label "Purchased at"
       :required true}
      {:id :total_amount
       :component form-fields/total-amount-input
       :label "Total amount"
       :required true
       ;; Receipt-approval UX: total is auto-derived.
       :show-use-total? (not receipt-approval?)
       :receipt-total-guess receipt-total-guess
       :totals-match? totals-match?}
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
       :component line-items-input
       :label "Line Items"
       :columns line-item-columns
       :style (if receipt-approval? {:maxHeight "260px"} {:maxHeight "300px"})
       :overflow-y-class "overflow-y-auto"
       :scrollbar-gutter-stable? true}])))
