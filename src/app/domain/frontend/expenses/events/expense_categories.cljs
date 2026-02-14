(ns app.domain.frontend.expenses.events.expense-categories
  "Expense categories domain events - generated using the expenses event factory."
  (:require
    [app.domain.frontend.expenses.events.entity-configs :as configs]
    [app.domain.frontend.expenses.events.events-factory :as factory]))

(factory/register-entity-events! configs/expense-categories-config)
