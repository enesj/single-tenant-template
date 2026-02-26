(ns app.domain.frontend.expenses.components.expense-form.specs
  "Expense form field spec builder for admin-scoped expense forms.")

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


