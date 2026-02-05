(ns app.domain.frontend.expenses.events.categories
  "Categories domain events - generated using the expenses event factory."
  (:require
    [app.domain.frontend.expenses.events.events-factory :as factory]
    [app.domain.frontend.expenses.events.entity-configs :as configs]))

(factory/register-entity-events! configs/categories-config)
