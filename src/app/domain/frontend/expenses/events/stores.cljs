(ns app.domain.frontend.expenses.events.stores
  "Stores domain events - generated using the expenses event factory."
  (:require
    [app.domain.frontend.expenses.events.entity-configs :as configs]
    [app.domain.frontend.expenses.events.events-factory :as factory]
    [app.domain.frontend.expenses.events.related-records-factory :as rr-factory]))

(factory/register-entity-events! configs/stores-config)

;; Register related records modal events
(rr-factory/register-related-records-events!
  {:entity-key :stores
   :base-path [:admin :expenses :stores]
   :api-endpoint "/admin/api/expenses/stores"
   :valid-related-types #{"expenses" "receipts" "articles"}})
