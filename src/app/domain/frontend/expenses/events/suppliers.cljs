(ns app.domain.frontend.expenses.events.suppliers
  "Suppliers domain events generated using the expenses event factory."
  (:require
    [app.domain.frontend.expenses.events.entity-configs :as configs]
    [app.domain.frontend.expenses.events.events-factory :as factory]
    [app.domain.frontend.expenses.events.related-records-factory :as rr-factory]))

;; Register standard CRUD events for suppliers using the factory
(factory/register-entity-events! configs/suppliers-config)

;; Register related records modal events
(rr-factory/register-related-records-events!
  {:entity-key :suppliers
   :base-path [:admin :expenses :suppliers]
   :api-endpoint "/admin/api/expenses/suppliers"
   :valid-related-types #{"expenses" "receipts" "articles" "stores"}})


