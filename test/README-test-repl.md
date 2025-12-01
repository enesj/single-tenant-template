# Test REPL Debugging Guide

This guide shows how to use the `test.repl` namespace to debug the 7 backend test errors and 1 failure identified in the test suite.

## Quick Start

1. **Start a REPL session:**
   ```bash
   clj -A:dev:test
   ```

2. **Load the test.repl namespace:**
   ```clojure
   (require '[test.repl :as tr])
   ```

3. **Setup the test environment:**
   ```clojure
   (tr/setup!)
   ```

## Debugging Individual Issues

### 1. Transaction Isolation Test (1 FAILURE)
```clojure
;; This test expects :mock-db-connection but gets real HikariDataSource
(tr/run-transaction-isolation-test)
```

### 2. Timeout Tests (6 ERRORS)
```clojure
;; Test individual timeout issues:
(tr/run-single-entity-transaction-templates-test)
(tr/run-single-entity-by-name-test)
(tr/run-routes-test)
(tr/run-bulk-operations-test)
(tr/run-crud-meta-test)

;; Or run all timeout tests at once:
(tr/run-timeout-tests)
```

### 3. Tenant ID Test (1 ERROR)
```clojure
;; This test fails because tenant-id is nil
(tr/run-invitation-service-test)
```

## Debugging System Issues

### Check System Health
```clojure
;; Check if server is responding
(tr/check-server-status)

;; Check database connection
(tr/check-database-connection)

;; Check overall system state
(tr/check-test-system-state)

;; Test basic HTTP connectivity
(tr/quick-test)
```

### System Management
```clojure
;; Restart the test system if needed
(tr/restart-test-system!)

;; Stop the system when done
(tr/stop-test-system!)
```

## Batch Testing

### Run All Problematic Tests
```clojure
;; Run all 7 problematic tests identified in analysis
(tr/run-all-problematic-tests)
```

## Troubleshooting Common Issues

### Server Timeout Issues
If tests are failing with 30-second timeouts:

1. **Check if server started properly:**
   ```clojure
   (tr/check-server-status)
   ```

2. **Check server logs for startup issues**

3. **Try restarting the test system:**
   ```clojure
   (tr/restart-test-system!)
   ```

4. **Test basic connectivity:**
   ```clojure
   (tr/debug-simple-request)
   ```

### Database Connection Issues
If database tests are failing:

1. **Check database connection:**
   ```clojure
   (tr/check-database-connection)
   ```

2. **Verify test database is running on localhost:5433**

3. **Check if database fixtures are loading properly**

### Authentication Issues
If API requests are failing due to auth:

1. **Check auth tokens:**
   ```clojure
   (tr/check-test-system-state)
   ```

2. **The system should automatically set up CSRF tokens and session cookies**

## Error Patterns Found

### 1. HTTP Timeouts (6 tests)
- **Pattern:** `idle timeout: 30000ms` from `org.httpkit.client.TimeoutException`
- **Root Cause:** Test server at localhost:8081 not responding
- **Tests Affected:** single-entity-test, routes-test, crud-meta-test

### 2. Mock vs Real Database (1 test)
- **Pattern:** Expected `:mock-db-connection` but got `HikariDataSource`
- **Root Cause:** `current-db` function returning real connection instead of mock
- **Test Affected:** transaction-isolation-unit-test

### 3. Missing Tenant Context (1 test)
- **Pattern:** `tenant-id cannot be nil for get-real-user`
- **Root Cause:** Test not setting up required tenant context
- **Test Affected:** task-2-5-invitation-service-test

## Example Debugging Session

```clojure
;; 1. Start REPL and load namespace
(require '[test.repl :as tr])

;; 2. Setup test environment
(tr/setup!)

;; 3. Check system health
(tr/check-test-system-state)

;; 4. If server issues, try basic connectivity
(tr/debug-simple-request)

;; 5. Run a specific problematic test
(tr/run-single-entity-transaction-templates-test)

;; 6. If that works, try all timeout tests
(tr/run-timeout-tests)

;; 7. Clean up when done
(tr/stop-test-system!)
```

## Files Involved

- **Test Files:** All problematic tests are wrapped with proper fixtures
- **Fixtures:** Uses `app.backend.fixtures/with-clean-transaction` for isolation
- **Test Helpers:** Uses `app.backend.test-helpers` for HTTP requests
- **System Management:** Uses `app.backend.fixtures` for test system lifecycle
