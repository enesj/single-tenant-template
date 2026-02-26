(ns app.domain.frontend.expenses.components.expense-form.specs
  "Expense form field spec builder for admin-scoped expense forms."
  (:require
    [app.domain.frontend.expenses.components.form-fields.line-items :refer [line-items-input]]
    [app.domain.frontend.expenses.components.form-fields.selects :refer [supplier-select-with-inline-create]]
    [app.domain.frontend.expenses.components.form-fields.total-amount :refer [total-amount-input
                                                                              totals-display]]
    [app.domain.frontend.expenses.ui.select-options :as select-options]
    [clojure.string :as str]))

(def line-item-columns
  [{:id :raw_label
    :label "Label"
    :type :text
    :placeholder "e.g. Milk, Bread"
    :width "min-w-[180px]"}
   {:id :qty
    :label "Qty"
    :type :number
    :step "0.001"
    :min "0"
    :width "w-[100px]"}
   {:id :unit_price
    :label "Price"
    :type :number
    :step "0.01"
    :min "0"
    :width "w-[100px]"}
   {:id :line_total
    :label "Total"
    :type :number
    :step "0.01"
    :min "0"
    :width "w-[100px]"}])


