(ns app.domain.frontend.expenses.events.article-aliases
  "Article aliases domain events - generated using the expenses event factory."
  (:require
    [app.domain.frontend.expenses.events.entity-configs :as configs]
    [app.domain.frontend.expenses.events.events-factory :as factory]
    [app.domain.frontend.expenses.events.related-records-factory :as rr-factory]))

;; Register standard CRUD events for article-aliases using the factory
(factory/register-entity-events! configs/article-aliases-config)

;; Register related records modal events
(rr-factory/register-related-records-events!
  {:entity-key :article-aliases
   :base-path [:admin :expenses :article-aliases]
   :api-endpoint "/admin/api/expenses/article-aliases"
   :valid-related-types #{"expenses" "receipts"}})