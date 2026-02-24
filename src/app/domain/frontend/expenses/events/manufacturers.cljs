(ns app.domain.frontend.expenses.events.manufacturers
  "Manufacturers domain events - generated using the expenses event factory."
  (:require
    [app.domain.frontend.expenses.events.entity-configs :as configs]
    [app.domain.frontend.expenses.events.events-factory :as factory]
    [app.domain.frontend.expenses.events.related-records-factory :as rr-factory]))

;; Register standard CRUD events for manufacturers using the factory
(factory/register-entity-events! configs/manufacturers-config)

;; Register related records modal events
(rr-factory/register-related-records-events!
  {:entity-key :manufacturers
   :base-path [:admin :expenses :manufacturers]
   :api-endpoint "/admin/api/expenses/manufacturers"
   :valid-related-types #{"articles"}})
