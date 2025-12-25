(ns app.domain.frontend.expenses.events.expense-items
  "Expense items domain events - generated using the expenses event factory."
  (:require
    [app.domain.frontend.expenses.events.entity-configs :as configs]
    [app.domain.frontend.expenses.events.events-factory :as factory]))

(factory/register-entity-events! configs/expense-items-config)
