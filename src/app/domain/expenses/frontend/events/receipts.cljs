(ns app.domain.expenses.frontend.events.receipts
  "Receipts domain events - generated using the expenses event factory."
  (:require
    [app.domain.expenses.frontend.events.events-factory :as factory]
    [app.domain.expenses.frontend.events.entity-configs :as configs]))

;; Register standard CRUD events for receipts using the factory
(factory/register-entity-events! configs/receipts-config)