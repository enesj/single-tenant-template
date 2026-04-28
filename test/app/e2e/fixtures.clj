(ns app.e2e.fixtures
  "E2E test fixtures — Playwright browser lifecycle + test system startup.

   Provides:
   1. Kaocha before/after hooks that start the full test system (port 8086, DB 55433)
   2. Playwright headless Chromium lifecycle (per-suite)
   3. Fresh browser context per test (isolated cookies/storage)
   4. DB cleanup helpers for tenant-related tables

   Reuses `app.backend.fixtures/start-test-system` for the backend system."
  (:require
    [app.backend.fixtures :as backend-fixtures]
    [app.template.backend.services.gmail-smtp :as gmail-smtp]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]
    [taoensso.timbre :as log])
  (:import
    [com.microsoft.playwright BrowserType$LaunchOptions Playwright]))

;; ============================================================================
;; Constants
;; ============================================================================

(def base-url "http://localhost:8086")

;; ============================================================================
;; Playwright State (suite-level)
;; ============================================================================

(defonce ^:private playwright-instance (atom nil))
(defonce ^:private browser-instance (atom nil))

;; Per-test dynamic vars
(def ^:dynamic *context*
  "Playwright BrowserContext for the current test (fresh cookies/storage)."
  nil)

(def ^:dynamic *page*
  "Playwright Page for the current test."
  nil)

;; ============================================================================
;; Playwright Lifecycle
;; ============================================================================

(defn- launch-browser!
  "Create Playwright instance and launch headless Chromium."
  []
  (log/info "Launching headless Chromium via Playwright...")
  (let [pw   (Playwright/create)
        opts (-> (BrowserType$LaunchOptions.)
               (.setHeadless true))
        browser (.launch (.chromium pw) opts)]
    (reset! playwright-instance pw)
    (reset! browser-instance browser)
    (log/info "Playwright browser launched successfully")
    browser))

(defn- close-browser!
  "Shut down Playwright browser and instance."
  []
  (log/info "Closing Playwright browser...")
  (when-let [browser @browser-instance]
    (try (.close browser) (catch Exception e (log/warn e "Error closing browser"))))
  (when-let [pw @playwright-instance]
    (try (.close pw) (catch Exception e (log/warn e "Error closing Playwright"))))
  (reset! browser-instance nil)
  (reset! playwright-instance nil)
  (log/info "Playwright browser closed"))

(defn get-browser
  "Get the current Playwright browser instance."
  []
  @browser-instance)

;; ============================================================================
;; DB Cleanup
;; ============================================================================

(def ^:private tenant-tables
  "Tables to truncate before the E2E suite (in dependency order)."
  ["audit_logs"
   "tenant_invitations"
   "user_expense_settings"
   "expense_items"
   "expenses"
   "receipts"
   "payers"
   "expense_categories"
   "tenant_memberships"
   "tenants"
   "user_sessions"
   "users"])

(defn truncate-e2e-tables!
  "Truncate all tenant-related tables to start E2E with a clean slate.
   Uses CASCADE to handle foreign keys."
  []
  (when-let [db (backend-fixtures/get-test-db)]
    (log/info "Truncating E2E tables for clean slate...")
    (doseq [table tenant-tables]
      (try
        (jdbc/execute! db [(str "TRUNCATE TABLE " table " CASCADE")])
        (catch Exception e
          (log/warn "Could not truncate" table ":" (.getMessage e)))))
    (log/info "E2E tables truncated")))

(defn query-db
  "Execute a SQL query against the test database. Returns unqualified maps."
  [sql & params]
  (when-let [db (backend-fixtures/get-test-db)]
    (jdbc/execute! db (into [sql] params)
      {:builder-fn rs/as-unqualified-maps})))

;; ============================================================================
;; Kaocha Suite Hooks
;; ============================================================================

(defn start-e2e-system
  "Kaocha before-suite hook: Start test system + Playwright browser.

   Kaocha hooks receive [suite test-plan] and must return suite."
  [suite test-plan]
  ;; Always keep E2E hermetic with respect to outgoing emails.
  (gmail-smtp/clear-outbox!)
  ;; 1. Start the backend test system (port 8086, test DB)
  (backend-fixtures/start-test-system suite test-plan)
  ;; 2. Clean the DB for a fresh E2E run
  (truncate-e2e-tables!)
  ;; 3. Launch Playwright
  (launch-browser!)
  ;; Return suite unchanged
  suite)

(defn stop-e2e-system
  "Kaocha after-suite hook: Shut down browser and test system."
  [suite test-plan]
  (close-browser!)
  (backend-fixtures/reset-test-system! suite test-plan)
  suite)

;; ============================================================================
;; Per-Test Fixtures
;; ============================================================================

(defn with-browser-context
  "Per-test fixture: creates a fresh BrowserContext (isolated cookies/storage)
   and a new Page, binding them to *context* and *page*.

   We also truncate tenant-scoped tables before each test so E2E tests can use
   stable fixture emails (e2e-user-a@...) without cross-test contamination.

   Usage:
     (use-fixtures :each fixtures/with-browser-context)"
  [f]
  ;; Keep each test hermetic (DB state) even when running the full :e2e suite.
  (gmail-smtp/clear-outbox!)
  (truncate-e2e-tables!)
  (let [browser (get-browser)]
    (if browser
      (let [context (.newContext browser)
            page    (.newPage context)]
        (try
          (binding [*context* context
                    *page*    page]
            (f))
          (finally
            (try (.close context) (catch Exception _ nil)))))
      (do
        (log/error "No Playwright browser available — skipping test")
        (f)))))

(defn with-shared-context
  "Per-test fixture that reuses a single browser context across tests in a namespace.
   Useful when tests in a namespace build on shared state (logged-in sessions, etc.).

   Stores the context in a provided atom. First test creates it; all tests share it.

   Usage:
     (def shared-ctx (atom nil))
     (use-fixtures :each (partial fixtures/with-shared-context shared-ctx))"
  [ctx-atom f]
  (let [browser (get-browser)]
    (if browser
      (let [context (or @ctx-atom
                      (let [c (.newContext browser)]
                        (reset! ctx-atom c)
                        c))
            page (.newPage context)]
        (try
          (binding [*context* context
                    *page*    page]
            (f))
          (finally
            (try (.close page) (catch Exception _ nil)))))
      (do
        (log/error "No Playwright browser available — skipping test")
        (f)))))

;; ============================================================================
;; Test State Inspection
;; ============================================================================

(defn e2e-status
  "Returns current E2E system status for debugging."
  []
  {:backend-ready?   (some? (backend-fixtures/get-test-db))
   :browser-running? (some? @browser-instance)
   :base-url         base-url})

(comment
  ;; REPL helpers
  (e2e-status)
  (truncate-e2e-tables!)
  :rcf)
