(ns app.domain.expenses.frontend.core
  "Bootstrap for expenses domain frontend"
  (:require
    [app.domain.expenses.frontend.events.expenses]
    [app.domain.expenses.frontend.events.payers]
    [app.domain.expenses.frontend.events.receipts]
    [app.domain.expenses.frontend.events.suppliers]
    [app.domain.expenses.frontend.subs.expenses]
    [app.domain.expenses.frontend.subs.payers]
    [app.domain.expenses.frontend.subs.receipts]
    [app.domain.expenses.frontend.subs.suppliers]))

(defn init!
  "Ensure expenses domain events/subs are loaded."
  [])
