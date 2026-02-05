(ns app.domain.backend.expenses.services.categories
  "Category CRUD services using the factory pattern."
  (:require
    [app.domain.backend.expenses.services.service-configs :as configs]
    [app.domain.backend.expenses.services.services-factory :as factory]))

;; ============================================================================
;; Service Registration
;; ============================================================================

(def config
  (configs/get-entity-config :category))

;; ============================================================================
;; Generated CRUD Operations
;; ============================================================================

(def service
  (factory/build-entity-service config))


