(ns app.template.frontend.helpers-test
  "Test utilities and helpers for frontend ClojureScript tests"
  (:require
    [app.template.frontend.auto-test-data :as auto-test-data]
    [clojure.test :refer [is testing]]))

;; Test data generators
(defn generate-test-entity
  "Generate a test entity using auto-generated data"
  ([entity-type] (generate-test-entity entity-type {}))
  ([entity-type overrides]
   (let [generated (auto-test-data/generate-entity entity-type)]
     (merge generated overrides))))

(defn generate-test-entities
  "Generate a collection of test entities using auto-generated data"
  ([entity-type] (generate-test-entities entity-type 3 {}))
  ([entity-type count] (generate-test-entities entity-type count {}))
  ([entity-type count overrides]
   (auto-test-data/generate-entities entity-type count overrides)))

;; Additional test data helpers
(defn generate-invalid-test-entity
  "Generate an entity with invalid data for testing validation"
  [entity-type]
  (auto-test-data/generate-invalid-entity entity-type))

(defn generate-comprehensive-test-data
  "Generate comprehensive test data for all entities"
  ([] (auto-test-data/get-auto-generated-data))
  ([options] (auto-test-data/get-auto-generated-data options)))

(defn reset-test-data!
  "Reset test data generation counters for test isolation"
  []
  (auto-test-data/reset-id-counter!))

;; Entity-specific helpers for common test scenarios

(defn generate-test-transactions
  "Generate test transactions with proper structure"
  ([count] (generate-test-entities :transactions count))
  ([count overrides] (generate-test-entities :transactions count overrides)))

(defn generate-test-transaction-types
  "Generate test transaction types with proper structure"
  ([count] (generate-test-entities :transaction_types count))
  ([count overrides] (generate-test-entities :transaction_types count overrides)))

;; Generic entity test helpers
(defn get-all-entity-types
  "Get all entity types from models"
  []
  (keys auto-test-data/models))

(defn generate-test-entity-for-any-type
  "Generate test entity for any available entity type"
  ([]
   (let [entity-types (get-all-entity-types)
         random-type (nth (vec entity-types)
                       (mod (auto-test-data/generate-unique-id)
                         (count entity-types)))]
     (generate-test-entity-for-any-type random-type)))
  ([entity-type] (generate-test-entity-for-any-type entity-type {}))
  ([entity-type overrides]
   (auto-test-data/generate-entity entity-type overrides)))

(defn generate-test-data-for-all-entities
  "Generate test data for all entities defined in models"
  ([count-per-entity]
   (reduce (fn [acc entity-type]
             (assoc acc entity-type
               (auto-test-data/generate-entities entity-type count-per-entity)))
     {}
     (get-all-entity-types))))

(defn run-test-for-each-entity
  "Run a test function for each entity type"
  [test-fn]
  (doseq [entity-type (get-all-entity-types)]
    (testing (str "Testing entity type: " (name entity-type))
      (test-fn entity-type))))

;; Removed duplicate - using the new implementation above

;; Normalized state helpers
;; Valid test database state
(def valid-test-db-state
  "Valid complete database state for testing that passes schema validation"
  (let [;; Import models-data to get correct entity names
        models-data (into {} (for [[k v] auto-test-data/models] [k v]))
        ;; Create entities structure using current entity names
        entities (into {}
                   (for [[k _] models-data]
                     [k {:data {} :ids [] :metadata {:loading? false :error nil :last-updated nil}}]))
        ;; Create UI lists for all entities
        ui-lists (into {}
                   (for [[k _] models-data]
                     [k {:current-page 1 :per-page 10 :sort {:field :id :direction :asc}
                         :selected-ids #{} :filter-modal {:open? false} :active-filters {}
                         :batch-edit {:popup {:open? false} :inline {:open? false}}}]))
        first-entity (-> models-data keys first)]
    {:current-route nil
     :controllers []
     :entities (assoc entities
                 :ui {:entity-name first-entity
                      :current-page nil
                      :theme "light"}
                 :specs {})
     :forms {}
     :ui {:current-page nil
          :lists ui-lists
          :recently-updated {}
          :recently-created {}
          :show-timestamps? false
          :show-edit? true
          :show-delete? true
          :show-highlights? true
          :show-select? false
          :defaults {}
          :entity-configs {}}
     :csrf-token nil}))

(defn empty-normalized-state
  "Create empty normalized state structure"
  []
  {:data {} :ids []})

;; Test assertions
(defn assert-normalized-structure
  "Assert that a value has the correct normalized structure"
  [normalized-data]
  (is (map? normalized-data) "Should be a map")
  (is (contains? normalized-data :data) "Should contain :data key")
  (is (contains? normalized-data :ids) "Should contain :ids key")
  (is (map? (:data normalized-data)) "Data should be a map")
  (is (vector? (:ids normalized-data)) "IDs should be a vector"))

(defn assert-entity-in-normalized
  "Assert that an entity exists in normalized data"
  [normalized-data entity-id]
  (is (contains? (:data normalized-data) entity-id)
    (str "Entity with ID " entity-id " should exist in data"))
  (is (some #{entity-id} (:ids normalized-data))
    (str "Entity ID " entity-id " should exist in ids vector")))

(defn assert-entity-not-in-normalized
  "Assert that an entity does not exist in normalized data"
  [normalized-data entity-id]
  (is (not (contains? (:data normalized-data) entity-id))
    (str "Entity with ID " entity-id " should not exist in data"))
  (is (not (some #{entity-id} (:ids normalized-data)))
    (str "Entity ID " entity-id " should not exist in ids vector")))
;; Comparison helpers
(defn entities-equal?
  "Check if two entity collections are equal (ignoring order)"
  [entities1 entities2]
  (= (set (map :id entities1))
    (set (map :id entities2))))

(defn log-test-start
  "Log the start of a test with emoji"
  [test-name]
  test-name)
;; REPL Test Runner
(defn run-all-frontend-tests
  "Run all frontend tests from the REPL. Use this with clojurescript_eval tool.
   Prerequisites: Make sure test namespaces are already loaded."
  []
  (println "🚀 Running Frontend Tests via REPL")
  (println "==================================")
  (println "📋 Test namespaces are ready for individual execution:")
  (println "  📊 app.frontend.state.normalize-test")
  (println "  🛤️ app.frontend.db.db-test")
  (println "  📝 app.frontend.components.form.validation-test")
  (println "  🔍 app.frontend.components.filter.helpers-test")
  (println "  ⚡ app.frontend.events.core-test")
  (println "\n🏁 Use individual test runner functions to execute tests!")
  (println "=================================="))

;; Individual test runners for browser testing
(defn run-normalize-tests []
  (println "📊 Running normalize tests...")
  (println "ℹ️ Execute: (cljs.test/run-tests 'app.frontend.state.normalize-test)"))

(defn run-db-tests []
  (println "🛤️ Running DB path tests...")
  (println "ℹ️ Execute: (cljs.test/run-tests 'app.frontend.db.db-test)"))

(defn run-form-validation-tests []
  (println "📝 Running form validation tests...")
  (println "ℹ️ Execute: (cljs.test/run-tests 'app.frontend.components.form.validation-test)"))

(defn run-filter-helpers-tests []
  (println "🔍 Running filter helpers tests...")
  (println "ℹ️ Execute: (cljs.test/run-tests 'app.frontend.components.filter.helpers-test)"))

(defn run-events-core-tests []
  (println "⚡ Running events core tests...")
  (println "ℹ️ Execute: (cljs.test/run-tests 'app.frontend.events.core-test)"))

;; Export for browser testing
(set! js/window.runAllFrontendTests run-all-frontend-tests)
(set! js/window.runNormalizeTests run-normalize-tests)
(set! js/window.runDbTests run-db-tests)
(set! js/window.runFormValidationTests run-form-validation-tests)
(set! js/window.runFilterHelpersTests run-filter-helpers-tests)
(set! js/window.runEventsCoreTests run-events-core-tests)
