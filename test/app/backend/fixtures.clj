(ns app.backend.fixtures
  "Test fixtures for Kaocha hooks - reuses dev system lifecycle.

   This namespace provides:
   - Kaocha before/after suite hooks for test system lifecycle
   - Access to test database and service container
   - Transaction-based test isolation fixtures"
  (:require
    [app.template.backend.core :as backend]
    [app.template.backend.migrations.simple-repl :as mig]
    [app.template.backend.services.gmail-smtp :as gmail-smtp]
    [automigrate.core :as am]
    [next.jdbc :as jdbc]
    [system.state :as state]
    [taoensso.timbre :as log]))

;; ============================================================================
;; Test System State (reuses system.state atoms)
;; ============================================================================

(defonce ^:private test-instance (atom nil))

(defonce ^:private migrations-ensured? (atom false))

(defn- current-test-jdbc-url
  "Best-effort JDBC URL for the DB the test system is *actually* using.

  Why: tests use the running system's pooled datasource from `system.state`, but
  `automigrate.core/migrate` runs against a JDBC URL. If an env var like
  DATABASE_URL points elsewhere, using `mig/get-jdbc-url` can migrate the wrong
  database and leave the test DB stale (e.g. missing payers columns)."
  []
  (let [db (get @state/state :database)]
    (or (when (instance? com.zaxxer.hikari.HikariDataSource db)
          (.getJdbcUrl ^com.zaxxer.hikari.HikariDataSource db))
      (mig/get-jdbc-url :test))))

(defn- ensure-test-schema!
  "Ensure the test DB schema is migrated.

  This keeps backend tests runnable even when the local test DB is stale.
  Runs at most once per JVM."
  []
  (when (compare-and-set! migrations-ensured? false true)
    (try
      ;; Always attempt to migrate. When already up-to-date, automigrate should be a no-op.
      ;; This avoids schema drift when new migrations are introduced but the DB happens to
      ;; include older "sentinel" columns.
      (log/info "🛠️ Ensuring test DB migrations are applied...")
      (am/migrate {:jdbc-url (current-test-jdbc-url)})
      (log/info "✅ Test DB migrations complete")
      (catch Throwable t
        ;; Allow retries if migration fails.
        (reset! migrations-ensured? false)
        (throw t)))))

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

   Kaocha hooks receive [suite test-plan] and must return suite.

   Reuses `with-test-system` which provides:
   - :test profile config (port 8086, db 55433)
   - Full DI container with all services
   - Running webserver for integration tests"
  [suite _test-plan]
  (log/info "🧪 Starting test system (reusing dev infrastructure)...")

  ;; Ensure no test run ever attempts real SMTP; tests can inspect the outbox.
  (gmail-smtp/clear-outbox!)

  ;; Avoid double-starts within the same JVM (e.g. when multiple Kaocha suites
  ;; run back-to-back). If a previous suite already started the system and it's
  ;; still running, re-use it instead of trying to bind the port again.
  (if (and @test-instance
        (not (future-done? @test-instance))
        (some? (get-test-db)))
    (do
      (log/info "♻️ Test system already running; reusing existing instance")
      (ensure-test-schema!)
      suite)

    (do
      ;; Clear stale instance refs (if any) before starting a fresh system.
      (when (and @test-instance (future-done? @test-instance))
        (reset! test-instance nil))

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

            (nil? (get-test-db))
            (do (Thread/sleep 100) (recur (inc attempts)))

            :else
            (do
              (log/info "✅ Test system ready"
                {:database (some? (get-test-db))
                 :service-container (some? (get-test-service-container))
                 :config (some? (get-test-config))})
              (ensure-test-schema!)))))

      ;; Return suite unchanged
      suite)))

(defn reset-test-system!
  "Kaocha after-suite hook: Shutdown test system cleanly.

   Kaocha hooks receive [suite test-plan] and must return suite."
  [suite _test-plan]
  (log/info "🧹 Stopping test system...")
  (when-let [instance @test-instance]
    (future-cancel instance)
    (try
      @instance
      (catch java.util.concurrent.CancellationException _
        ;; Expected when future is cancelled
        nil)
      (catch java.util.concurrent.ExecutionException e
        ;; Can happen if a double-start attempt failed (e.g. port already in use).
        ;; We still want teardown to be best-effort and not fail the whole test run.
        (log/warn e "Test system future ended with an exception during shutdown"))
      (catch Exception e
        (log/warn e "Unexpected error while stopping test system"))))
  (reset! test-instance nil)
  (reset! state/state nil)
  (log/info "✅ Test system stopped")
  ;; Return suite unchanged
  suite)

;; ============================================================================
;; Dynamic Vars for Test Access
;; ============================================================================

(def ^:dynamic *test-db*
  "Dynamic var bound to test database connection within test fixtures"
  nil)

;; ============================================================================
;; Test Fixtures for Individual Tests
;; ============================================================================

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
