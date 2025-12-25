(ns app.domain.frontend.expenses.ui.select-options)

(defn supplier-label
  [supplier]
  (or (:display-name supplier)
    (:display_name supplier)
    (:id supplier)
    ""))

(defn article-label
  [article]
  (or (:canonical-name article)
    (:canonical_name article)
    (:name article)
    (:id article)
    ""))

(defn expense-label
  [expense]
  (str (or (:date expense) "No date")
    " - "
    (or (:supplier-name expense) (:supplier_name expense) "Unknown Supplier")
    " (" (or (:total-amount expense) (:total_amount expense) "0.00") ")"))
