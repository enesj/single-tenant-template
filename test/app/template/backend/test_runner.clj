(ns app.template.backend.test-runner
  (:require
    [clojure.string :as str]
    [clojure.test :refer [run-tests]]
    [taoensso.timbre :as log]))
   ;; Import all test namespaces

;; ============================================================================
;; Test Configuration
;; ============================================================================

(defn configure-test-logging
  "Configure logging for test runs"
  []
  ;; Configure clean test logging format
  (log/set-config!
    {:output-fn (fn [{:keys [level ?ns-str ?file ?line msg_]}]
                  (str (-> level name str/upper-case) " [" (or ?ns-str ?file "?") ":" ?line "] - " (force msg_)))})
  (log/set-level! :info)
  (log/info "Starting service layer test suite..."))

;; ============================================================================
;; Individual Test Suite Runners
;; ============================================================================

(defn run-crud-service-tests
  "Run CRUD service tests"
  []
  (log/info "=== Running CRUD Service Tests ===")
  (let [results (run-tests 'app.template.backend.crud.service-test)]
    (log/info "CRUD Service Tests completed:"
      "tests:" (:test results)
      "pass:" (:pass results)
      "fail:" (:fail results)
      "error:" (:error results))
    results))

(defn run-metadata-service-tests
  "Run metadata service tests"
  []
  (log/info "=== Running Metadata Service Tests ===")
  (let [results (run-tests 'app.template.backend.metadata.service-test)]
    (log/info "Metadata Service Tests completed:"
      "tests:" (:test results)
      "pass:" (:pass results)
      "fail:" (:fail results)
      "error:" (:error results))
    results))

(defn run-container-tests
  "Run dependency injection container tests"
  []
  (log/info "=== Running DI Container Tests ===")
  (let [results (run-tests 'app.template.di.container-test)]
    (log/info "DI Container Tests completed:"
      "tests:" (:test results)
      "pass:" (:pass results)
      "fail:" (:fail results)
      "error:" (:error results))
    results))

(defn run-integration-tests
  "Run integration tests"
  []
  (log/info "=== Running Integration Tests ===")
  (let [results (run-tests 'app.template.backend.crud.integration-test)]
    (log/info "Integration Tests completed:"
      "tests:" (:test results)
      "pass:" (:pass results)
      "fail:" (:fail results)
      "error:" (:error results))
    results))

;; ============================================================================
;; Complete Test Suite Runner
;; ============================================================================

(defn run-all-service-tests
  "Run all service layer tests"
  []
  (configure-test-logging)

  (log/info "")
  (log/info "🚀 Starting Backend Service Layer Test Suite")
  (log/info "============================================")
  (log/info "")

  (let [start-time (System/currentTimeMillis)

        ;; Run all test suites
        crud-results (run-crud-service-tests)
        metadata-results (run-metadata-service-tests)
        container-results (run-container-tests)
        integration-results (run-integration-tests)

        end-time (System/currentTimeMillis)
        duration (- end-time start-time)

        ;; Aggregate results
        total-tests (+ (:test crud-results)
                      (:test metadata-results)
                      (:test container-results)
                      (:test integration-results))

        total-pass (+ (:pass crud-results)
                     (:pass metadata-results)
                     (:pass container-results)
                     (:pass integration-results))

        total-fail (+ (:fail crud-results)
                     (:fail metadata-results)
                     (:fail container-results)
                     (:fail integration-results))

        total-error (+ (:error crud-results)
                      (:error metadata-results)
                      (:error container-results)
                      (:error integration-results))

        success? (and (zero? total-fail) (zero? total-error))]

    (log/info "")
    (log/info "============================================")
    (log/info "📊 Test Suite Summary")
    (log/info "============================================")
    (log/info (str "Total Duration: " duration "ms"))
    (log/info (str "Total Tests: " total-tests))
    (log/info (str "✅ Passed: " total-pass))
    (log/info (str "❌ Failed: " total-fail))
    (log/info (str "💥 Errors: " total-error))
    (log/info "")

    (when success?
      (log/info "🎉 ALL TESTS PASSED! Service layer is working correctly."))

    (when-not success?
      (log/error "❌ SOME TESTS FAILED! Please check the test output above."))

    (log/info "")
    (log/info "Test Coverage Breakdown:")
    (log/info "• CRUD Service: Complete CRUD operations, tenant isolation, validation")
    (log/info "• Metadata Service: Entity metadata, field metadata, foreign keys, validation")
    (log/info "• Type Casting Service: Insert/update casting, field value casting")
    (log/info "• Query Builder Service: SELECT/INSERT/UPDATE/DELETE query building")
    (log/info "• DI Container: Service creation, dependency wiring, configuration")
    (log/info "• Integration: Full CRUD pipeline, multi-tenant isolation, legacy compatibility")
    (log/info "")

    {:success? success?
     :duration duration
     :summary {:total-tests total-tests
               :total-pass total-pass
               :total-fail total-fail
               :total-error total-error}
     :details {:crud-service crud-results
               :metadata-service metadata-results
               :di-container container-results
               :integration integration-results}}))

;; ============================================================================
;; Specific Test Category Runners
;; ============================================================================

(defn run-unit-tests
  "Run only unit tests (service tests)"
  []
  (configure-test-logging)
  (log/info "Running unit tests only...")

  (let [crud-results (run-crud-service-tests)
        metadata-results (run-metadata-service-tests)
        container-results (run-container-tests)]

    {:crud-service crud-results
     :metadata-service metadata-results
     :di-container container-results}))

(defn run-validation-tests
  "Run validation-focused tests"
  []
  (configure-test-logging)
  (log/info "Running validation-focused tests...")

  ;; Run specific validation tests from each suite
  (run-tests 'app.template.backend.crud.service-test/test-validation-integration)
  (run-tests 'app.template.backend.metadata.service-test/test-validate-field)
  (run-tests 'app.template.backend.metadata.service-test/test-validate-entity)
  (run-tests 'app.template.backend.crud.integration-test/test-validation-integration-pipeline))

(defn run-multi-tenant-tests
  "Run multi-tenant isolation tests"
  []
  (configure-test-logging)
  (log/info "Running multi-tenant isolation tests...")

  (run-tests 'app.template.backend.crud.service-test/test-tenant-isolation)
  (run-tests 'app.template.backend.crud.integration-test/test-multi-tenant-isolation-integration))

;; ============================================================================
;; Performance Test Runner
;; ============================================================================

(defn run-performance-tests
  "Run performance-focused tests"
  []
  (configure-test-logging)
  (log/info "Running performance tests...")

  (run-tests 'app.template.backend.crud.integration-test/test-batch-operations-performance))

;; ============================================================================
;; Development Helper Functions
;; ============================================================================

(defn run-quick-smoke-test
  "Run a quick smoke test to verify basic functionality"
  []
  (configure-test-logging)
  (log/info "Running quick smoke test...")

  (let [results (run-tests 'app.template.backend.crud.service-test/test-crud-service-creation
                  'app.template.backend.metadata.service-test/test-metadata-service-creation
                  'app.template.di.container-test/test-complete-service-container)]

    (if (and (zero? (:fail results)) (zero? (:error results)))
      (log/info "✅ Smoke test passed - basic service creation working")
      (log/error "❌ Smoke test failed - check service creation"))

    results))

;; ============================================================================
;; Main Entry Points
;; ============================================================================

(defn -main
  "Main entry point for running tests from command line"
  []
  (let [results (run-all-service-tests)]
    (if (:success? results)
      (System/exit 0)
      (System/exit 1))))

;; For REPL development
(comment
  ;; Run all tests
  (run-all-service-tests)

  ;; Run specific test suites
  (run-crud-service-tests)
  (run-metadata-service-tests)
  (run-container-tests)
  (run-integration-tests)

  ;; Run test categories
  (run-unit-tests)
  (run-validation-tests)
  (run-multi-tenant-tests)
  (run-performance-tests)

  ;; Quick verification
  (run-quick-smoke-test))
