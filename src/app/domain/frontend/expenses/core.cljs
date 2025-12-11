(ns app.domain.frontend.expenses.core
  "Bootstrap for expenses domain frontend"
  (:require
    [app.domain.frontend.expenses.events.expenses]
    [app.domain.frontend.expenses.events.payers]
    [app.domain.frontend.expenses.events.receipts]
    [app.domain.frontend.expenses.events.suppliers]
    [app.domain.frontend.expenses.subs.expenses]
    [app.domain.frontend.expenses.subs.payers]
    [app.domain.frontend.expenses.subs.receipts]
    [app.domain.frontend.expenses.subs.suppliers]))

(defn init!
  "Ensure expenses domain events/subs are loaded."
  [])
