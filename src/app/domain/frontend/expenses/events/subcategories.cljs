(ns app.domain.frontend.expenses.events.subcategories
  "Subcategories domain events - generated using the expenses event factory."
  (:require
    [app.domain.frontend.expenses.events.events-factory :as factory]
    [app.domain.frontend.expenses.events.entity-configs :as configs]))

(factory/register-entity-events! configs/subcategories-config)
