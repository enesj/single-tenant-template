(ns app.backend.mock-data-auto
  "Auto-generated mock data using the comprehensive auto-generation system.
   This completely replaces manual mock data with model-driven generation."
  (:require
    [app.domain.backend.auto-test-data :as auto-data]
    [taoensso.timbre :as log]))

;; =============================================================================
;; Auto-Generated Mock Data
;; =============================================================================

(def entities-mock-data
  "Comprehensive auto-generated mock data for all entities.
   Generated dynamically from models.edn with proper dependency resolution."
  (delay
    (let [data (auto-data/get-auto-generated-data {:seed-count 4})]
      (auto-data/validate-generated-data data)
      data)))

;; =============================================================================
;; Utility Functions
;; =============================================================================

(defn refresh-mock-data!
  "Generate fresh mock data (useful for test isolation)"
  []
  (alter-var-root #'entities-mock-data
    (constantly (delay (auto-data/refresh-test-data!)))))

(defn get-entity-data
  "Get data for a specific entity"
  [entity-keyword]
  (get @entities-mock-data entity-keyword))

(defn get-seed-data
  "Get seed data for a specific entity"
  [entity-keyword]
  (get-in @entities-mock-data [entity-keyword :seed-data]))

(defn get-new-data
  "Get new-data for testing CREATE operations"
  [entity-keyword]
  (get-in @entities-mock-data [entity-keyword :new-data]))

(defn get-update-data
  "Get update-data for testing UPDATE operations"
  [entity-keyword]
  (get-in @entities-mock-data [entity-keyword :update-data]))

(defn get-invalid-data
  "Get invalid data for testing validation"
  [entity-keyword]
  (get-in @entities-mock-data [entity-keyword :invalid-create-data]))

;; =============================================================================
;; Backwards Compatibility
;; =============================================================================

; Maintain compatibility with existing test patterns
(defn generate-fresh-mock-data
  "Legacy function for backwards compatibility"
  []
  @entities-mock-data)

;; =============================================================================
;; Development and Debugging
;; =============================================================================

(comment
  ; Development helpers
  (require '[app.backend.mock-data-auto :as auto-mock] :reload)

  ; Inspect generated data
  (def data @auto-mock/entities-mock-data)
  (keys data)
  (get-in data [:transaction-types :seed-data])
  (get-in data [:transactions :new-data])
  (get-in data [:items :invalid-create-data])

  ; Refresh data
  (auto-mock/refresh-mock-data!)

  ; Get specific entity data
  (auto-mock/get-entity-data :transaction-types)
  (auto-mock/get-seed-data :transactions)
  (auto-mock/get-invalid-data :items))
