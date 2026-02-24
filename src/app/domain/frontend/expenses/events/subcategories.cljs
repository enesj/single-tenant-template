(ns app.domain.frontend.expenses.events.subcategories
  "Subcategories domain events - generated using the expenses event factory."
  (:require
    [app.domain.frontend.expenses.events.entity-configs :as configs]
    [app.domain.frontend.expenses.events.events-factory :as factory]
    [app.domain.frontend.expenses.events.related-records-factory :as rr-factory]))

(factory/register-entity-events! configs/subcategories-config)

;; Register related records modal events
(rr-factory/register-related-records-events!
  {:entity-key :subcategories
   :base-path [:admin :expenses :subcategories]
   :api-endpoint "/admin/api/expenses/subcategories"
   :valid-related-types #{"articles"}})
