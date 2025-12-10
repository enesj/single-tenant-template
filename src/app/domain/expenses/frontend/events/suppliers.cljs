(ns app.domain.expenses.frontend.events.suppliers
  "Suppliers domain events - generated using the expenses event factory."
  (:require
    [app.domain.expenses.frontend.events.events-factory :as factory]
    [app.domain.expenses.frontend.events.entity-configs :as configs]))

;; Register standard CRUD events for suppliers using the factory
(factory/register-entity-events! configs/suppliers-config)