(ns app.domain.frontend.expenses.events.article-aliases
  "Article aliases domain events - generated using the expenses event factory."
  (:require
    [app.domain.frontend.expenses.events.events-factory :as factory]
    [app.domain.frontend.expenses.events.entity-configs :as configs]))

;; Register standard CRUD events for article-aliases using the factory
(factory/register-entity-events! configs/article-aliases-config)