(ns app.domain.frontend.expenses.events.expenses
  "Expenses domain events - generated using the expenses event factory."
  (:require
    [app.domain.frontend.expenses.events.events-factory :as factory]
    [app.domain.frontend.expenses.events.entity-configs :as configs]))

;; Register standard CRUD events for expenses using the factory
;; This includes list, detail, and form events (create, success, failure)
(factory/register-entity-events! configs/expenses-config)