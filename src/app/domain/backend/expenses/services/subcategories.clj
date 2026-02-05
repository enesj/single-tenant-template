(ns app.domain.backend.expenses.services.subcategories
  "Subcategory CRUD services using the factory pattern.

  Subcategories belong to a category via `:category_id` and are referenced by
  articles via `:subcategory_id`."
  (:require
    [app.domain.backend.expenses.services.service-configs :as configs]
    [app.domain.backend.expenses.services.services-factory :as factory]))

;; ============================================================================
;; Service Registration
;; ============================================================================

(def config
  (configs/get-entity-config :subcategory))

;; ============================================================================
;; Generated CRUD Operations
;; ============================================================================

(def service
  (factory/build-entity-service config))


