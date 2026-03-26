(ns app.domain.frontend.expenses.events.unmapped-aliases
  "Admin unmapped aliases events generated via the shared expenses event factory."
  (:require
    [app.domain.frontend.expenses.events.entity-configs :as configs]
    [app.domain.frontend.expenses.events.events-factory :as factory]))

(factory/register-entity-events! configs/unmapped-aliases-config)
