# Transaction Isolation in Backend Tests

## Overview

The backend test suite now supports transaction isolation for database-level
tests. This provides perfect test isolation by wrapping each test in a database
transaction that automatically rolls back, ensuring tests don't affect each
other.

## Benefits

1. **Perfect Isolation**: Each test runs in its own transaction that gets rolled
   back
2. **No Side Effects**: Tests can't affect each other's data
3. **Faster Execution**: No need to clean up data after tests
4. **Simpler Tests**: Focus on testing logic, not cleanup
5. **Realistic Testing**: Tests run against real database with real constraints

## Usage Patterns

### Pattern 1: Transaction-Only (Fast)

Use when you want transaction isolation without resetting the database:

```clojure
(deftest test-with-transaction
  (with-transaction
    (fn []
      ;; Use (current-db) instead of @ds
      (jdbc/execute! (current-db) ["INSERT INTO ..."])
      ;; All changes will be rolled back
      )))
```

### Pattern 2: Clean Transaction (Clean Slate)

Use when you need a clean database state AND transaction isolation:

```clojure
(deftest test-with-clean-state
  (with-clean-transaction
    (fn []
      ;; Database is reset to seed data
      ;; AND running in a transaction
      (jdbc/execute! (current-db) ["INSERT INTO ..."])
      )))
```

### Pattern 3: Namespace-Level Fixtures

Apply transaction isolation to all tests in a namespace:

```clojure
(use-fixtures :once start-test-system)
(use-fixtures :each with-transaction)

;; Now all tests run in transactions automatically
(deftest my-test
  ;; No need to wrap in with-transaction
  (jdbc/execute! (current-db) [...]))
```

## Important Notes

1. **Always use `(current-db)`** instead of `@ds` in transaction tests
2. **HTTP tests don't use transactions** - they test the full application stack
3. **Transactions auto-rollback** - no cleanup needed
4. **Nested transactions** - The framework handles this correctly

## When to Use Each Pattern

### Use Transaction Isolation For:

- Direct database operation tests
- Foreign key constraint tests
- Data integrity tests
- Complex database scenarios
- Tests that modify data

### Don't Use Transaction Isolation For:

- HTTP/API endpoint tests (use existing fixtures)
- Tests that need to verify committed data
- Tests that span multiple database connections

## Migration Guide

To migrate existing database tests:

1. Change fixture from `reset-db-and-run` to `with-transaction` or
   `with-clean-transaction`
2. Replace all `@ds` with `(current-db)`
3. Remove any manual cleanup code
4. Remove any transaction management code

### Before:

```clojure
(use-fixtures :each reset-db-and-run)

(deftest my-test
  (let [result (jdbc/execute! @ds ["INSERT ..."])]
    ;; test assertions
    ;; manual cleanup
    (jdbc/execute! @ds ["DELETE ..."])))
```

### After:

```clojure
(use-fixtures :each with-transaction)

(deftest my-test
  (let [result (jdbc/execute! (current-db) ["INSERT ..."])]
    ;; test assertions
    ;; no cleanup needed!
    ))
```

## Examples

See `test/app/backend/transaction_isolation_example_test.clj` for comprehensive
examples of all patterns.
