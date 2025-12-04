# Backend Testing Implementation Plan

> **Status**: Phase 1-15 ✅ Complete | 121 tests, 498 assertions  
> **Last Updated**: 2025-12-04  
> **Prerequisite**: Test database running on port 55433

---

## Quick Start

```bash
# Run existing tests
bb be-test

# Or with Kaocha directly
clj -M:test -m kaocha.runner
```

---

## Context

| Aspect | Details |
|--------|---------|
| **Test Runner** | Kaocha via `bb be-test` |
| **Test Profile** | `-Daero.profile=test` |
| **Ports** | Web: 8086, DB: 55433 |
| **Config** | `config/base.edn` with `:test` profile |

### Key Files

| File | Purpose |
|------|---------|
| `tests.edn` | Kaocha configuration with hooks |
| `test/app/backend/routes_smoke_test.clj` | Existing smoke tests (3 tests) |
| `src/app/backend/core.clj` | Contains `with-test-system` |
| `src/system/state.clj` | Shared atoms for system state |

---

## Dev Environment Reuse

The dev environment has reloadable system infrastructure we reuse for testing:

### Existing Test System

Already defined at `src/app/backend/core.clj:169`:

```clojure
(def with-test-system
  "Test system using :test profile (port 8081, test database)"
  (my-system {:profile :test}))
```

Provides:
- Test profile config (`:test`)
- HikariCP pool to test DB (port 55433)
- Full service container with DI
- Webserver on test port (8086)
- Proper `with-open` resource cleanup

### State Atoms

`src/system/state.clj` provides shared atoms:

```clojure
(defonce instance (atom (future ::never-run)))  ; Running system future
(defonce state    (atom nil))                    ; Current system state map
```

---

## Implementation Phases

### Phase 1: Foundation (REQUIRED)

Create missing Kaocha hooks referenced in `tests.edn`:

#### 1.1 Create `test/app/backend/fixtures.clj`

```clojure
(ns app.backend.fixtures
  "Test fixtures for Kaocha hooks - reuses dev system lifecycle"
  (:require
    [app.backend.core :as backend]
    [system.state :as state]
    [taoensso.timbre :as log]))

(defonce ^:private test-instance (atom nil))

(defn get-test-db []
  (get @state/state :database))

(defn get-test-service-container []
  (get @state/state :service-container))

(defn start-test-system
  "Kaocha before-suite hook"
  []
  (log/info "🧪 Starting test system...")
  (let [publish-state (fn [system-state]
                        (reset! state/state system-state)
                        (try
                          (loop [] (Thread/sleep 60000) (recur))
                          (catch InterruptedException _
                            (log/info "Test system interrupted"))))
        instance (future
                   (try
                     (backend/with-test-system publish-state)
                     (catch Exception e
                       (log/error e "Test system startup failed"))))]
    (reset! test-instance instance)
    (loop [attempts 0]
      (cond
        (>= attempts 50)
        (throw (ex-info "Test system failed to start" {:attempts attempts}))
        (nil? @state/state)
        (do (Thread/sleep 100) (recur (inc attempts)))
        :else
        (log/info "✅ Test system ready")))))

(defn reset-test-system!
  "Kaocha after-suite hook"
  []
  (log/info "🧹 Stopping test system...")
  (when-let [instance @test-instance]
    (future-cancel instance)
    (try @instance (catch java.util.concurrent.CancellationException _)))
  (reset! test-instance nil)
  (reset! state/state nil)
  (log/info "✅ Test system stopped"))

(def ^:dynamic *test-db* nil)

(defn with-test-db [f]
  (if-let [db (get-test-db)]
    (binding [*test-db* db] (f))
    (do (log/warn "Test DB not available") (f))))

(defn with-transaction-rollback [f]
  (if-let [db (get-test-db)]
    (next.jdbc/with-transaction [tx db {:rollback-only true}]
      (binding [*test-db* tx] (f)))
    (f)))
```

#### 1.2 Create `test/app/backend/test_helpers.clj`

```clojure
(ns app.backend.test-helpers
  "Shared test utilities"
  (:require
    [app.backend.routes :as routes]
    [app.backend.webserver :as webserver]
    [app.backend.routes.admin-api :as admin-api]
    [app.backend.services.admin.dashboard :as admin-dashboard]
    [app.backend.services.monitoring.login-events :as login-monitoring]
    [cheshire.core :as json]
    [ring.mock.request :as mock]))

(defn stub-service-container
  ([] (stub-service-container {}))
  ([overrides]
   (merge
     {:models-data {}
      :crud-routes ["/crud" {:get {:handler (constantly {:status 200})}}]
      :auth-routes {:login-handler (fn [_] {:status 200
                                            :headers {"Content-Type" "application/json"}
                                            :body (json/generate-string {:ok true})})}
      :password-routes {}
      :config {:base-url "http://localhost:8086"}}
     overrides)))

(defn build-handler
  ([] (build-handler (stub-service-container)))
  ([service-container]
   (with-redefs [admin-api/admin-api-routes (fn [_ _] ["/admin/api" {:get {:handler (constantly {:status 200})}}])
                 admin-dashboard/get-dashboard-stats (fn [_] {:total-admins 0})
                 login-monitoring/count-recent-login-events (fn [_ _] 0)]
     (-> (routes/app-routes {} service-container)
         (webserver/wrap-service-container service-container)))))

(defn slurp-body [resp]
  (let [body (:body resp)]
    (cond (string? body) body
          (nil? body) ""
          :else (slurp body))))

(defn parse-json-body [resp]
  (json/parse-string (slurp-body resp) true))

(defn json-request [method path & [body]]
  (-> (mock/request method path)
      (mock/content-type "application/json")
      (cond-> body (mock/body (json/generate-string body)))))

(defn admin-request [method path & [body token]]
  (-> (json-request method path body)
      (mock/header "X-Admin-Token" (or token "test-admin-token"))))
```

---

### Phase 2: Admin Auth Tests

`test/app/backend/routes/admin/auth_test.clj` ✅:
- Password hashing/verification tests
- Session token generation tests
- SHA-256 legacy format tests
- Session store tests

### Phase 3: Admin API Tests

Tests created ✅:
- `test/app/backend/routes/admin/dashboard_test.clj` - Dashboard stats, structure, session integration
- `test/app/backend/routes/admin/users_test.clj` - User data normalization, filters, search criteria
- `test/app/backend/routes/admin/audit_test.clj` - Audit log structure, actions, retrieval

### Phase 4: User API Tests

`test/app/backend/routes/api_test.clj` ✅:
- `/api/v1/config` endpoint
- `/api/v1/metrics` endpoint
- Login/auth endpoint tests

### Phase 5: DB Integration ✅

Requires:
1. Test DB running: `docker-compose up -d postgres-test`
2. Migrations applied: `clj -X:migrations-test`
3. Use `fixtures/with-transaction-rollback` for isolation

### Phase 6: Admin Management Tests ✅

`test/app/backend/routes/admin/admins_test.clj`:
- List admins with pagination and filters
- Get admin details by ID
- Create new admins
- Update admin info
- Delete admins
- Role and status management

### Phase 7: Login Events Tests ✅

`test/app/backend/routes/admin/login_events_test.clj`:
- Handler creation validation
- List login events with filtering
- Filter by principal type (user/admin)
- Filter by success status
- Pagination support
- Service function existence tests

### Phase 8: Password Reset Tests ✅

`test/app/backend/routes/admin/password_test.clj`:
- Forgot password handler (request password reset)
- Verify token handler
- Reset password handler
- Change password handler (for logged-in users)
- Token validation tests
- Error handling for invalid tokens

### Phase 9: Template CRUD Service Tests ✅

`test/app/template/backend/crud/service_test.clj`:
- Protocol existence verification (CrudService, MetadataService, ValidationService, TypeCastingService)
- Mock implementation tests for each protocol
- Protocol function verification
- Protocol satisfaction tests

### Phase 10: Settings Tests ✅

`test/app/backend/routes/admin/settings_test.clj`:
- Settings handler creation
- Get settings (current view-options)
- Update settings (full replacement)
- Patch settings (partial update)
- Delete settings
- Mock file I/O for settings storage

### Phase 11: User Bulk Operations Tests ✅

`test/app/backend/routes/admin/user_bulk_test.clj`:
- Bulk status update (multiple users)
- Bulk role update
- Batch update (multiple operations)
- Export users to CSV
- Empty items error handling

### Phase 12: User Operations Tests ✅

`test/app/backend/routes/admin/user_operations_test.clj`:
- Role update for single user
- Force verify email
- Reset password (admin-initiated)
- Get user activity history
- User impersonation
- User search with criteria
- Routes structure verification

### Phase 13: Transaction Monitoring Tests ✅

`test/app/backend/routes/admin/transactions_test.clj`:
- List transactions with pagination
- Get transaction by ID
- Transaction trends statistics
- Suspicious transaction detection
- Filter by date range and amount

### Phase 14: Entity CRUD Tests ✅

`test/app/backend/routes/admin/entities_test.clj`:
- Delete entity with dry-run
- Delete entity with force
- Delete entity with constraint violations
- Create entity
- Validation error handling
- Entity not found handling

### Phase 15: Integration Monitoring Tests ✅

`test/app/backend/routes/admin/integrations_test.clj`:
- List integrations
- Get integration by ID
- Integration status overview
- Integration performance metrics
- Webhook status monitoring
- Routes structure verification

---

## File Structure After Implementation

```
test/
├── app/
│   ├── backend/
│   │   ├── fixtures.clj           [Phase 1] ✅
│   │   ├── test_helpers.clj       [Phase 1] ✅
│   │   ├── routes_smoke_test.clj  [EXISTING] ✅
│   │   └── routes/
│   │       ├── admin/
│   │       │   ├── auth_test.clj        [Phase 2] ✅
│   │       │   ├── dashboard_test.clj   [Phase 3] ✅
│   │       │   ├── users_test.clj       [Phase 3] ✅
│   │       │   ├── audit_test.clj       [Phase 3] ✅
│   │       │   ├── admins_test.clj      [Phase 6] ✅
│   │       │   ├── login_events_test.clj [Phase 7] ✅
│   │       │   ├── password_test.clj    [Phase 8] ✅
│   │       │   ├── settings_test.clj    [Phase 10] ✅
│   │       │   ├── user_bulk_test.clj   [Phase 11] ✅
│   │       │   ├── user_operations_test.clj [Phase 12] ✅
│   │       │   ├── transactions_test.clj [Phase 13] ✅
│   │       │   ├── entities_test.clj    [Phase 14] ✅
│   │       │   └── integrations_test.clj [Phase 15] ✅
│   │       └── api_test.clj           [Phase 4] ✅
│   └── template/
│       └── backend/
│           └── crud/
│               └── service_test.clj   [Phase 9] ✅
```

---

## Verification Commands

```bash
# Full test suite
bb be-test

# Verbose output
clj -M:test -m kaocha.runner --reporter documentation

# Single namespace
clj -M:test -m kaocha.runner --focus app.backend.routes-smoke-test

# REPL-based testing
(require '[kaocha.repl :as k])
(k/run 'app.backend.routes-smoke-test)
```

---

## Gotchas

1. **Fixtures must exist**: `tests.edn` references `app.backend.fixtures/start-test-system` - tests fail without it
2. **Test DB required for integration**: Port 55433, run migrations first
3. **with-redefs scope**: Stub handlers in `build-handler` are created inside `with-redefs` - don't leak the handler outside
4. **Ports**: Dev uses 8085, test uses 8086 - don't mix them up

---

## Related Files

- `deps.edn` - Test dependencies under `:test` alias
- `tests.edn` - Kaocha configuration
- `config/base.edn` - Profile-based config (`:dev` vs `:test`)
- `docs/backend/services.md` - Service architecture notes
