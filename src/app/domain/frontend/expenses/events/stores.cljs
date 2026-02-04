(ns app.domain.frontend.expenses.events.stores
  "Stores domain events - generated using the expenses event factory."
  (:require
    [app.domain.frontend.expenses.events.events-factory :as factory]
    [app.domain.frontend.expenses.events.entity-configs :as configs]))

(factory/register-entity-events! configs/stores-config)
