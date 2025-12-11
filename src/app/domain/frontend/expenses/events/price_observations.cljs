(ns app.domain.frontend.expenses.events.price-observations
  "Price observations domain events - generated using the expenses event factory."
  (:require
    [app.domain.frontend.expenses.events.events-factory :as factory]
    [app.domain.frontend.expenses.events.entity-configs :as configs]))

;; Register standard CRUD events for price-observations using the factory
(factory/register-entity-events! configs/price-observations-config)