(ns test.repl
  "REPL utilities for debugging problematic backend tests"
  (:require
    [app.backend.transaction-isolation-unit-test :as transaction-test]
    [app.domain.backend.fixtures :as fixtures]
    [app.domain.backend.invitation-service-test :as invitation-test]
    [app.domain.backend.routes-test :as routes-test]
    [app.domain.backend.single-entity-test :as single-entity-test]
    [app.domain.backend.test-helpers :as test-helpers]
    [app.template.backend.crud-meta-test :as crud-meta-test]
    [next.jdbc :as jdbc]
    [org.httpkit.client :as http]
    [taoensso.timbre :as log]))

;; =============================================================================
;; Test System Management
;; =============================================================================

(defn start-test-system!
  "Start the test system for debugging"
  []
  (log/info "🚀 Starting test system...")
  (fixtures/start-persistent-test-system!)
  (log/info "✅ Test system started successfully"))

(defn stop-test-system!
  "Stop the test system"
  []
  (log/info "🛑 Stopping test system...")
  (fixtures/reset-test-system!)
  (log/info "✅ Test system stopped"))

(defn restart-test-system!
  "Restart the test system"
  []
  (stop-test-system!)
  (Thread/sleep 2000)
  (start-test-system!))

;; =============================================================================
;; Individual Problematic Tests
;; =============================================================================

(defn run-transaction-isolation-test
  "Run the failing transaction isolation test"
  []
  (log/info "🧪 Running transaction isolation test...")
  (try
    (fixtures/with-clean-transaction
      (fn []
        (transaction-test/test-current-db-function)))
    (log/info "✅ Transaction isolation test passed")
    (catch Exception e
      (log/error e "❌ Transaction isolation test failed")
      (throw e))))

(defn run-single-entity-transaction-templates-test
  "Run the timeout failing single entity test for transaction templates"
  []
  (log/info "🧪 Running single entity transaction templates test...")
  (try
    (fixtures/with-clean-transaction
      (fn []
        (single-entity-test/test-transaction-templates-only)))
    (log/info "✅ Single entity transaction templates test passed")
    (catch Exception e
      (log/error e "❌ Single entity transaction templates test failed")
      (throw e))))

(defn run-single-entity-by-name-test
  "Run the timeout failing single entity by name test"
  []
  (log/info "🧪 Running single entity by name test...")
  (try
    (fixtures/with-clean-transaction
      (fn []
        (single-entity-test/test-single-entity-by-name)))
    (log/info "✅ Single entity by name test passed")
    (catch Exception e
      (log/error e "❌ Single entity by name test failed")
      (throw e))))

(defn run-routes-test
  "Run the timeout failing routes test"
  []
  (log/info "🧪 Running routes test...")
  (try
    (fixtures/with-clean-transaction
      (fn []
        (routes-test/test-all-entities-routes)))
    (log/info "✅ Routes test passed")
    (catch Exception e
      (log/error e "❌ Routes test failed")
      (throw e))))

(defn run-bulk-operations-test
  "Run the timeout failing bulk operations test"
  []
  (log/info "🧪 Running bulk operations test...")
  (try
    (fixtures/with-clean-transaction
      (fn []
        (routes-test/test-bulk-operations)))
    (log/info "✅ Bulk operations test passed")
    (catch Exception e
      (log/error e "❌ Bulk operations test failed")
      (throw e))))

(defn run-crud-meta-test
  "Run the timeout failing CRUD meta test"
  []
  (log/info "🧪 Running CRUD meta test...")
  (try
    (fixtures/with-clean-transaction
      (fn []
        (crud-meta-test/test-all-entities-meta-driven)))
    (log/info "✅ CRUD meta test passed")
    (catch Exception e
      (log/error e "❌ CRUD meta test failed")
      (throw e))))

(defn run-invitation-service-test
  "Run the tenant-id failing invitation service test"
  []
  (log/info "🧪 Running invitation service test...")
  (try
    (fixtures/with-clean-transaction
      (fn []
        (invitation-test/test-real-user-lookup)))
    (log/info "✅ Invitation service test passed")
    (catch Exception e
      (log/error e "❌ Invitation service test failed")
      (throw e))))

;; =============================================================================
;; Test Groups
;; =============================================================================

(defn run-timeout-tests
  "Run all tests that are failing with timeouts"
  []
  (log/info "🧪 Running all timeout tests...")
  (doseq [test-fn [run-single-entity-transaction-templates-test
                   run-single-entity-by-name-test
                   run-routes-test
                   run-bulk-operations-test
                   run-crud-meta-test]]
    (try
      (test-fn)
      (catch Exception e
        (log/error e "❌ Test failed, continuing...")))))

(defn run-all-problematic-tests
  "Run all problematic tests identified in the analysis"
  []
  (log/info "🧪 Running all problematic tests...")
  (doseq [test-fn [run-transaction-isolation-test
                   run-single-entity-transaction-templates-test
                   run-single-entity-by-name-test
                   run-routes-test
                   run-bulk-operations-test
                   run-crud-meta-test
                   run-invitation-service-test]]
    (try
      (test-fn)
      (catch Exception e
        (log/error e "❌ Test failed, continuing...")))))

;; =============================================================================
;; Debugging Utilities
;; =============================================================================

(defn check-server-status
  "Check if the test server is responding"
  []
  (log/info "🔍 Checking server status...")
  (try
    (let [response @(http/get "http://localhost:8081/health"
                      {:timeout 5000})]
      (if (= (:status response) 200)
        (log/info "✅ Server is responding at localhost:8081")
        (log/warn "⚠️ Server responded with status:" (:status response))))
    (catch Exception e
      (log/error e "❌ Server is not responding at localhost:8081"))))

(defn check-database-connection
  "Check database connection"
  []
  (log/info "🔍 Checking database connection...")
  (try
    (let [db (fixtures/current-db)]
      (if db
        (do
          (log/info "✅ Database connection available:" (type db))
          ;; Try a simple query
          (let [result (jdbc/execute-one! db ["SELECT 1 as test"])]
            (if result
              (log/info "✅ Database query successful")
              (log/warn "⚠️ Database query returned nil"))))
        (log/error "❌ No database connection available")))
    (catch Exception e
      (log/error e "❌ Database connection check failed"))))

(defn check-test-system-state
  "Check the current state of the test system"
  []
  (log/info "🔍 Checking test system state...")
  (check-server-status)
  (check-database-connection)
  (log/info "🔍 Auth tokens available:" (boolean (test-helpers/get-auth :csrf-token)))
  (log/info "🔍 Session cookie available:" (boolean (test-helpers/get-auth :session-cookie))))

(defn debug-simple-request
  "Make a simple request to test basic connectivity"
  []
  (log/info "🔍 Testing simple HTTP request...")
  (try
    (let [response (test-helpers/make-request :get "http://localhost:8081/api/v1/entities/transaction_templates" (test-helpers/build-request-headers))]
      (log/info "✅ Simple request successful, status:" (:status response))
      response)
    (catch Exception e
      (log/error e "❌ Simple request failed")
      (throw e))))

;; =============================================================================
;; REPL Helper Functions
;; =============================================================================

(defn setup!
  "Quick setup for REPL debugging"
  []
  (log/info "🛠️ Setting up REPL debugging environment...")
  (start-test-system!)
  (Thread/sleep 3000) ; Give server time to start
  (check-test-system-state))

(defn quick-test
  "Run a quick test to verify everything is working"
  []
  (log/info "🧪 Running quick connectivity test...")
  (debug-simple-request))

(comment
  ;; REPL Usage Examples:

  ;; 1. Setup the test environment
  (setup!)

  ;; 2. Check system status
  (check-test-system-state)

  (check-database-connection)

  ;; 3. Run a quick connectivity test
  (quick-test)

  ;; 4. Run individual problematic tests
  (run-transaction-isolation-test)
  (run-single-entity-by-name-test)
  (run-single-entity-transaction-templates-test)
  (run-invitation-service-test)
  (run-routes-test)
  (repeatedly 3 #(debug-simple-request))

  ;; 5. Run groups of tests
  (run-timeout-tests)
  (run-all-problematic-tests)

  ;; 6. Debug connectivity issues
  (check-server-status)
  (debug-simple-request)

  ;; 7. Restart system if needed
  (restart-test-system!)

  ;; 8. Clean shutdown
  (stop-test-system!))
