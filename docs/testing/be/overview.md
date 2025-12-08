# Backend Testing Overview

This document covers the backend testing architecture, test infrastructure, and implementation patterns.

## Quick Start

```bash
# Run all backend tests
bb be-test

# Verbose output with documentation reporter
clj -M:test -m kaocha.runner --reporter documentation

# Run specific namespace
clj -M:test -m kaocha.runner --focus app.backend.routes.admin.auth-test
```

## Test Infrastructure

### Test Runner: Kaocha

The project uses [Kaocha](https://github.com/lambdaisland/kaocha) as the test runner, configured via `tests.edn`:

```clojure
#kaocha/v1
{:tests [{:id :unit
          :test-paths ["test"]
          :ns-patterns [".*-test$"]}]
 :plugins [:kaocha.plugin/hooks]
 :kaocha.hooks/before [:app.backend.fixtures/start-test-system]
 :kaocha.hooks/after [:app.backend.fixtures/reset-test-system!]}
```

### Test Profile

Tests run with `-Daero.profile=test`, which provides:

| Setting | Value | Purpose |
|---------|-------|---------|
| Web port | 8086 | Avoid conflicts with dev (8085) |
| DB port | 55433 | Separate test database |
| DB name | `single_tenant_pos_test` | Isolated test data |

### Test Database

```bash
# Start test database
docker-compose up -d postgres-test

# Run migrations on test DB
clj -X:migrations-test
```

## Architecture

### System Lifecycle

The test system reuses the dev system infrastructure with test-specific configuration:

```
┌─────────────────────────────────────────────────────────┐
│                    Kaocha Test Runner                    │
├─────────────────────────────────────────────────────────┤
│  before-suite:  fixtures/start-test-system              │
│  after-suite:   fixtures/reset-test-system!             │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌─────────────────┐    ┌─────────────────┐            │
│  │  Test System    │───▶│  system/state   │            │
│  │  (with-test-    │    │  atoms          │            │
│  │   system)       │    └─────────────────┘            │
│  └─────────────────┘                                   │
│           │                                            │
│           ▼                                            │
│  ┌─────────────────────────────────────────┐          │
│  │  Service Container                       │          │
│  │  • Database pool (HikariCP)             │          │
│  │  • CRUD routes                          │          │
│  │  • Auth routes                          │          │
│  │  • Config                               │          │
│  └─────────────────────────────────────────┘          │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### Key Files

| File | Purpose |
|------|---------|
| `test/app/backend/fixtures.clj` | System lifecycle hooks for Kaocha |
| `test/app/backend/test_helpers.clj` | Shared test utilities |
| `src/app/backend/core.clj` | Contains `with-test-system` |
| `src/system/state.clj` | Shared atoms for system state |

## Test Categories

### 1. Unit Tests (Isolated)

Test individual functions without database or system:

```clojure
(deftest password-hashing-test
  (let [password "secret123"
        hashed (auth/hash-password password)]
    (is (auth/verify-password password hashed))
    (is (not (auth/verify-password "wrong" hashed)))))
```

### 2. Handler Tests (Mocked)

Test route handlers with mocked dependencies:

```clojure
(deftest dashboard-handler-test
  (with-redefs [dashboard/get-dashboard-stats (constantly {:total-users 10})]
    (let [handler (h/mock-admin-handler dashboard-routes/create-dashboard-handler)
          response (handler (h/mock-admin-request :get "/dashboard"))]
      (is (= 200 (:status response))))))
```

### 3. Integration Tests

Test with real database using transaction rollback:

```clojure
(deftest create-user-integration-test
  (fixtures/with-transaction-rollback
    (let [result (users/create-user! fixtures/*test-db* {:email "test@example.com"})]
      (is (:id result)))))
```

## Test Utilities

### Mock Database

```clojure
(defn mock-db
  "Creates a mock database that returns specified results"
  [query-results]
  (reify java.sql.Connection
    ;; Mock implementation
    ))
```

### Mock Admin Request

```clojure
(defn mock-admin-request
  "Create a mock admin API request"
  [method path & [body]]
  (-> (mock/request method path)
      (mock/content-type "application/json")
      (mock/header "X-Admin-Token" "test-token")
      (cond-> body (mock/body (json/generate-string body)))))
```

### Parse Response Body

```clojure
(defn parse-response-body
  "Parse JSON response body"
  [response]
  (-> response :body slurp (json/parse-string true)))
```

## Coverage Areas

| Area | Test File | Tests |
|------|-----------|-------|
| Admin Authentication | `auth_test.clj` | Password hashing, sessions, tokens |
| Dashboard | `dashboard_test.clj` | Stats, structure, integration |
| User Management | `users_test.clj` | Normalization, filters, search |
| Audit Logs | `audit_test.clj` | Structure, actions, retrieval |
| Admin CRUD | `admins_test.clj` | List, create, update, delete |
| Login Events | `login_events_test.clj` | Monitoring, filters, pagination |
| Password Reset | `password_test.clj` | Forgot, verify, reset flows |
| Settings | `settings_test.clj` | View options CRUD |
| Bulk Operations | `user_bulk_test.clj` | Batch updates, export |
| User Operations | `user_operations_test.clj` | Role, verify, impersonate |
| Transactions | `transactions_test.clj` | Monitoring, trends, suspicious |
| Entities | `entities_test.clj` | CRUD with constraints |
| Integrations | `integrations_test.clj` | Status, performance, webhooks |
| CRUD Protocols | `service_test.clj` | Protocol verification |

## Current Status

- **121 tests** with **498 assertions**
- All tests passing ✅
- Coverage spans admin routes, user management, monitoring, and infrastructure

## Related Documentation

- [Development Guide](./development-guide.md) - Writing and debugging tests
- [Test Patterns](./test-patterns.md) - Common testing patterns
- [Fixtures Reference](./fixtures-reference.md) - Fixture utilities
