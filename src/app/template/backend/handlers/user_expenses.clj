(ns app.template.backend.handlers.user-expenses
  "DEPRECATED: user-facing expenses handlers live in the Expenses domain.

   Prefer requiring `app.domain.backend.expenses.handlers.user-expenses` directly.
   This namespace remains as a compatibility shim for older code."
  (:require
    [app.domain.backend.expenses.handlers.user-expenses :as impl]))

(def list-expenses-handler impl/list-expenses-handler)
(def get-expense-handler impl/get-expense-handler)
(def create-expense-handler impl/create-expense-handler)
(def update-expense-handler impl/update-expense-handler)
(def delete-expense-handler impl/delete-expense-handler)

(def expense-summary-handler impl/expense-summary-handler)
(def spending-by-month-handler impl/spending-by-month-handler)
(def spending-by-supplier-handler impl/spending-by-supplier-handler)

(def list-suppliers-handler impl/list-suppliers-handler)
(def list-payers-handler impl/list-payers-handler)
