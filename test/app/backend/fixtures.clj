(ns app.backend.fixtures
  "Test fixtures for Kaocha hooks - reuses dev system lifecycle.
   
   This namespace provides:
   - Kaocha before/after suite hooks for test system lifecycle
   - Access to test database and service container
   - Transaction-based test isolation fixtures"
  (:require
    [app.backend.core :as backend]
    [next.jdbc :as jdbc]
    [system.state :as state]
    [taoensso.timbre :as log]))

;; ============================================================================
;; Test System State (reuses system.state atoms)
;; ============================================================================

(defonce ^:private test-instance (atom nil))

(defn get-test-db
  "Get the database connection from the running test system"
  []
  (get @state/state :database))

(defn get-test-service-container
  "Get the service container from the running test system"
  []
  (get @state/state :service-container))

(defn get-test-config
  "Get the config from the running test system"
  []
  (get @state/state :config))

;; ============================================================================
;; Kaocha Hooks
;; ============================================================================

(defn start-test-system
  "Kaocha before-suite hook: Start full test system using dev infrastructure.
   
   Reuses `with-test-system` which provides:
   - :test profile config (port 8086, db 55433)
   - Full DI container with all services
   - Running webserver for integration tests"
  []
  (log/info "🧪 Starting test system (reusing dev infrastructure)...")
  (let [;; Wrap state publishing like dev/system/core.clj does
        publish-state (fn [system-state]
                        (reset! state/state system-state)
                        ;; Block until cancelled (like in dev)
                        (try
                          (loop [] (Thread/sleep 60000) (recur))
                          (catch InterruptedException _
                            (log/info "Test system interrupted"))))
        ;; Start the test system in a future
        instance (future
                   (try
                     (backend/with-test-system publish-state)
                     (catch Exception e
                       (log/error e "Test system startup failed")
                       (throw e))))]
    (reset! test-instance instance)
    ;; Wait for system to be ready
    (loop [attempts 0]
      (cond
        (>= attempts 50)
        (throw (ex-info "Test system failed to start within 5 seconds" 
                        {:attempts attempts}))

        (nil? @state/state)
        (do (Thread/sleep 100) (recur (inc attempts)))

        :else
        (log/info "✅ Test system ready" 
                  {:database (some? (get-test-db))
                   :service-container (some? (get-test-service-container))
                   :config (some? (get-test-config))})))))

(defn reset-test-system!
  "Kaocha after-suite hook: Shutdown test system cleanly"
  []
  (log/info "🧹 Stopping test system...")
  (when-let [instance @test-instance]
    (future-cancel instance)
    (try 
      @instance 
      (catch java.util.concurrent.CancellationException _
        ;; Expected when future is cancelled
        nil)))
  (reset! test-instance nil)
  (reset! state/state nil)
  (log/info "✅ Test system stopped"))

;; ============================================================================
;; Dynamic Vars for Test Access
;; ============================================================================

(def ^:dynamic *test-db* 
  "Dynamic var bound to test database connection within test fixtures"
  nil)

(def ^:dynamic *test-service-container*
  "Dynamic var bound to service container within test fixtures"
  nil)

;; ============================================================================
;; Test Fixtures for Individual Tests
;; ============================================================================

(defn with-test-db
  "Fixture that provides database access to a test.
   Binds *test-db* to the database connection."
  [f]
  (if-let [db (get-test-db)]
    (binding [*test-db* db]
      (f))
    (do
      (log/warn "Test DB not available, running test without DB")
      (f))))

(defn with-service-container
  "Fixture that provides service container access to a test.
   Binds *test-service-container* to the service container."
  [f]
  (if-let [sc (get-test-service-container)]
    (binding [*test-service-container* sc]
      (f))
    (do
      (log/warn "Service container not available")
      (f))))

(defn with-test-system-bindings
  "Fixture that provides both DB and service container."
  [f]
  (binding [*test-db* (get-test-db)
            *test-service-container* (get-test-service-container)]
    (f)))

(defn with-transaction-rollback
  "Fixture that wraps test in a transaction and rolls back after.
   Useful for DB tests that shouldn't persist changes.
   
   Usage in deftest:
   (use-fixtures :each fixtures/with-transaction-rollback)
   
   Then use fixtures/*test-db* in your test."
  [f]
  (if-let [db (get-test-db)]
    (jdbc/with-transaction [tx db {:rollback-only true}]
      (binding [*test-db* tx]
        (f)))
    (do
      (log/warn "Test DB not available for transaction rollback")
      (f))))

;; ============================================================================
;; Test State Inspection (for debugging)
;; ============================================================================

(defn test-system-status
  "Returns current test system status for debugging"
  []
  {:instance-running? (and @test-instance (not (future-done? @test-instance)))
   :state-available? (some? @state/state)
   :db-available? (some? (get-test-db))
   :service-container-available? (some? (get-test-service-container))
   :config-available? (some? (get-test-config))})
