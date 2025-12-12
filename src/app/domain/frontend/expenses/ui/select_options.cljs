(ns app.domain.frontend.expenses.ui.select-options)

(defn supplier-label
  [supplier]
  (or (:display-name supplier)
    (:display_name supplier)
    (:id supplier)
    ""))
