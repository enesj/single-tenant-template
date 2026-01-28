(ns app.domain.backend.expenses.services.manufacturers
  "Manufacturer CRUD services using the factory pattern.

  This is a canonical brand catalog used by articles via :manufacturer_id."
  (:require
    [app.domain.backend.expenses.services.service-configs :as configs]
    [app.domain.backend.expenses.services.services-factory :as factory]))

;; ============================================================================
;; Service Registration
;; ============================================================================

(def config
  (configs/get-entity-config :manufacturer))

;; ============================================================================
;; Generated CRUD Operations
;; ============================================================================

(def service
  (factory/build-entity-service config))

;; ============================================================================
;; Normalization (re-exported for external use)
;; ============================================================================

(def normalize-manufacturer-key
  configs/normalize-manufacturer-key)
