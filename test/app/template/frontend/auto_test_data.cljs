(ns app.template.frontend.auto-test-data
  "Public API for test data generation.
   Aggregates models, utilities, and generators into a simplified interface."
  (:require
    [app.template.frontend.auto-test-generators :as generators]
    [app.template.frontend.auto-test-models :as models]
    [app.template.frontend.auto-test-utils :as utils]))

;; =============================================================================
;; Re-exports for Backward Compatibility
;; =============================================================================

(def models models/models)

;; =============================================================================
;; Entity Data Generation
;; =============================================================================

(defn generate-entity-data "Generate complete entity data based on models definition"
  [entity-keyword constraint-type]
  (let [entity-def (get models/models entity-keyword)
        fields (:fields entity-def)
        entity-types (:types entity-def)
        valid? (not= constraint-type :invalid)]

    (->> fields
      (keep #(generators/generate-field-value % entity-types generators/*data-context* valid?))
      (into {}))))

(defn generate-entity
  ([entity-type] (generate-entity entity-type {}))
  ([entity-type overrides]
   (let [generated (generate-entity-data entity-type :valid)
         ;; Generate UUID string for ID if not provided
         entity-id (or (:id overrides)
                     (let [id (generators/generate-unique-id)
                           padded (str "000000000000" id)]
                       (str "550e8400-e29b-41d4-a716-" (.substring padded (- (count padded) 12)))))
         base-entity (assoc generated :id entity-id)]
     ;; Merge after inserting the id so callers can override any field
     (merge base-entity overrides))))

(defn generate-entities
  ([entity-type count] (generate-entities entity-type count {}))
  ([entity-type count overrides]
   (repeatedly count #(generate-entity entity-type overrides))))

(defn generate-invalid-entity
  "Generate an entity with invalid data"
  [entity-type]
  (generate-entity-data entity-type :invalid))

;; =============================================================================
;; Complete Test Data Generation
;; =============================================================================

(defn generate-test-data-fixed
  "Fixed version of comprehensive test data generation"
  ([] (generate-test-data-fixed {}))
  ([options]
   (let [entities (keys models/models)
         dependency-graph (utils/build-dependency-graph models/models)
         sorted-entities (utils/topological-sort dependency-graph entities)
         seed-count (get options :seed-count 4)
         data-context (atom {})
         result (atom {})]

     (binding [generators/*data-context* data-context]
       ;; Process each entity
       (doseq [entity-keyword sorted-entities]
         (let [seed-data (vec (generate-entities entity-keyword seed-count))]
           ;; Store seed data for foreign key resolution
           (swap! data-context assoc entity-keyword seed-data)
           ;; Generate all data types for this entity
           (swap! result assoc entity-keyword
             {:seed-data seed-data
              :new-data (generate-entity entity-keyword)
              :update-data (generate-entity entity-keyword)
              :invalid-create-data (generate-invalid-entity entity-keyword)})))
       @result))))

;; =============================================================================
;; Public API
;; =============================================================================

^{:clojure-lsp/ignore [:clojure-lsp/unused-public-var]}
(defn get-auto-generated-data
  "Main entry point for getting auto-generated test data"
  ([]
   (get-auto-generated-data {}))
  ([options]
   (generate-test-data-fixed options)))

^{:clojure-lsp/ignore [:clojure-lsp/unused-public-var]}
(defn reset-id-counter!
  "Reset the ID counter for test isolation"
  []
  (reset! generators/*id-counter* 1000))
