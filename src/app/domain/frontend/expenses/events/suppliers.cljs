(ns app.domain.frontend.expenses.events.suppliers
  "Suppliers domain events - generated using the expenses event factory."
  (:require
    [app.domain.frontend.expenses.events.events-factory :as factory]
    [app.domain.frontend.expenses.events.entity-configs :as configs]))

;; Register standard CRUD events for suppliers using the factory
(factory/register-entity-events! configs/suppliers-config)