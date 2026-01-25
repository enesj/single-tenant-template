(ns app.domain.backend.expenses.services.manufacturers
  "Manufacturers are canonical product manufacturers/brands used to normalize
  `articles.manufacturer` into a stable reference via `articles.manufacturer_id`."
  (:require
    [app.domain.backend.expenses.services.service-configs :as configs]
    [app.domain.backend.expenses.services.services-factory :as factory]))

;; ============================================================================
;; Service Registration
;; ============================================================================

(def config (configs/get-entity-config :manufacturer))

;; ============================================================================
;; Generated CRUD Operations
;; ============================================================================

(def service (factory/build-entity-service config))
