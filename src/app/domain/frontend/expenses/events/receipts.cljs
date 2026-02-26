(ns app.domain.frontend.expenses.events.receipts
  "Receipts domain events - generated using the expenses event factory."
  (:require
    [app.domain.frontend.expenses.events.entity-configs :as configs]
    [app.domain.frontend.expenses.events.events-factory :as factory]))

(def ^:private base-path [:admin :expenses :receipts])

;; Register standard CRUD events for receipts using the factory
(factory/register-entity-events! configs/receipts-config)




