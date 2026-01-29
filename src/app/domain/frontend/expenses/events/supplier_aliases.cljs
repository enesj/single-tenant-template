(ns app.domain.frontend.expenses.events.supplier-aliases
  "Supplier aliases domain events - generated using the expenses event factory."
  (:require
    [app.domain.frontend.expenses.events.events-factory :as factory]
    [app.domain.frontend.expenses.events.entity-configs :as configs]))

;; Register standard CRUD events for supplier aliases using the factory
(factory/register-entity-events! configs/supplier-aliases-config)
