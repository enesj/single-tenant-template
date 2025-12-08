# Test Fixtures Reference

This document covers the test fixtures and their usage for backend testing.

## Overview

Test fixtures provide system lifecycle management and test isolation utilities.

## Core Fixtures

### `test/app/backend/fixtures.clj`

```clojure
(ns app.backend.fixtures
  (:require
    [app.backend.core :as backend]
    [system.state :as state]
    [taoensso.timbre :as log]))
```

## System Lifecycle

### `start-test-system`

Kaocha `before-suite` hook that starts the test system:

```clojure
(defn start-test-system []
  "Starts test system with :test profile"
  ;; 1. Creates future running backend/with-test-system
  ;; 2. Publishes system state to system.state/state atom
  ;; 3. Waits for system to be ready (up to 50 attempts)
  ;; 4. Returns when system is ready
  )
```

**Usage**: Automatically called by Kaocha before test suite.

### `reset-test-system!`

Kaocha `after-suite` hook that stops the test system:

```clojure
(defn reset-test-system! []
  "Stops test system and cleans up state"
  ;; 1. Cancels the system future
  ;; 2. Resets state atoms to nil
  )
```

**Usage**: Automatically called by Kaocha after test suite.

## Database Fixtures

### `get-test-db`

Returns the test database connection:

```clojure
(defn get-test-db []
  (get @state/state :database))
```

**Usage**:
```clojure
(let [db (fixtures/get-test-db)]
  (jdbc/execute! db ["SELECT 1"]))
```

### `*test-db*`

Dynamic var bound to current test database/transaction:

```clojure
(def ^:dynamic *test-db* nil)
```

**Usage**:
```clojure
;; Inside with-test-db or with-transaction-rollback
(jdbc/execute! fixtures/*test-db* ["INSERT INTO users (email) VALUES (?)" "test@example.com"])
```

### `with-test-db`

Binds `*test-db*` to the test database for the duration of `f`:

```clojure
(defn with-test-db [f]
  (if-let [db (get-test-db)]
    (binding [*test-db* db] (f))
    (do (log/warn "Test DB not available") (f))))
```

**Usage as fixture**:
```clojure
(use-fixtures :each fixtures/with-test-db)

(deftest my-test
  (is (some? fixtures/*test-db*)))
```

### `with-transaction-rollback`

Runs test in a transaction that rolls back after completion:

```clojure
(defn with-transaction-rollback [f]
  (if-let [db (get-test-db)]
    (next.jdbc/with-transaction [tx db {:rollback-only true}]
      (binding [*test-db* tx] (f)))
    (f)))
```

**Usage as fixture**:
```clojure
(use-fixtures :each fixtures/with-transaction-rollback)

(deftest database-test
  ;; All changes here are automatically rolled back
  (let [result (users/create! fixtures/*test-db* {:email "test@example.com"})]
    (is (:id result))))
```

## Service Container

### `get-test-service-container`

Returns the test service container with all dependencies:

```clojure
(defn get-test-service-container []
  (get @state/state :service-container))
```

**Usage**:
```clojure
(let [container (fixtures/get-test-service-container)
      handler (routes/create-handler container)]
  (handler request))
```

## Usage Patterns

### Pattern 1: Unit Tests (No Fixtures)

For pure functions that don't need system:

```clojure
(ns app.backend.utils-test
  (:require [clojure.test :refer [deftest is]]))

(deftest pure-function-test
  (is (= 4 (my-utils/add 2 2))))
```

### Pattern 2: Handler Tests (Mocked Dependencies)

For testing handlers with mocked services:

```clojure
(ns app.backend.routes.my-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [app.backend.test-helpers :as h]))

(deftest handler-test
  (with-redefs [service/get-data (constantly {:value 42})]
    (let [handler (create-handler)
          response (handler (h/mock-admin-request :get "/data"))]
      (is (= 200 (:status response))))))
```

### Pattern 3: Integration Tests (Real Database)

For tests that need real database:

```clojure
(ns app.backend.integration-test
  (:require
    [clojure.test :refer [deftest is use-fixtures]]
    [app.backend.fixtures :as fixtures]))

(use-fixtures :each fixtures/with-transaction-rollback)

(deftest integration-test
  (let [db fixtures/*test-db*]
    (is (some? (jdbc/execute-one! db ["SELECT 1 as result"])))))
```

### Pattern 4: Full System Tests

For tests that need complete system:

```clojure
(ns app.backend.system-test
  (:require
    [clojure.test :refer [deftest is]]
    [app.backend.fixtures :as fixtures]
    [clj-http.client :as http]))

(deftest full-system-test
  ;; System is started by Kaocha hooks
  (let [response (http/get "http://localhost:8086/api/health")]
    (is (= 200 (:status response)))))
```

## Fixture Composition

### Combining Fixtures

```clojure
(use-fixtures :once
  fixtures/with-test-system)

(use-fixtures :each
  fixtures/with-transaction-rollback
  my-custom-fixture)
```

### Custom Fixtures

```clojure
(defn with-test-user [f]
  (let [user (users/create! fixtures/*test-db* {:email "test@example.com"})]
    (binding [*test-user* user]
      (try
        (f)
        (finally
          (users/delete! fixtures/*test-db* (:id user)))))))
```

## Configuration

### Test Profile Settings

The test profile (`:test`) configures:

| Setting | Value |
|---------|-------|
| `:port` | 8086 |
| `:database/port` | 55433 |
| `:database/name` | `single_tenant_pos_test` |

### Kaocha Configuration

In `tests.edn`:

```clojure
#kaocha/v1
{:kaocha.hooks/before [:app.backend.fixtures/start-test-system]
 :kaocha.hooks/after [:app.backend.fixtures/reset-test-system!]}
```

## Troubleshooting

### System Not Starting

```
Test system failed to start
```

**Check**:
1. Test database is running: `docker-compose up -d postgres-test`
2. Port 55433 is available
3. Migrations are applied: `clj -X:migrations-test`

### Database Not Available

```
Test DB not available
```

**Check**:
1. System started successfully
2. `@state/state` contains `:database` key
3. Use `fixtures/with-test-db` fixture

### Transaction Rollback Issues

If data persists between tests:

**Check**:
1. Using `with-transaction-rollback` fixture
2. All DB operations use `fixtures/*test-db*`
3. Not creating new connections inside test
