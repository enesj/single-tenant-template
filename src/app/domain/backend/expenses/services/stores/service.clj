(ns app.domain.backend.expenses.services.stores.service
  (:require
    [app.domain.backend.expenses.services.service-configs :as configs]
    [app.domain.backend.expenses.services.services-factory :as factory]))

(def config (configs/get-entity-config :store))

(def entity-service (factory/build-entity-service config))
