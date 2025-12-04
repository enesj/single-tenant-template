# Backend Testing Development Guide

This guide covers how to write, run, and debug backend tests.

## Running Tests

### Basic Commands

```bash
# Run all backend tests
bb be-test

# Run with Kaocha directly (more options)
clj -M:test -m kaocha.runner

# Verbose output
clj -M:test -m kaocha.runner --reporter documentation

# Run specific namespace
clj -M:test -m kaocha.runner --focus app.backend.routes.admin.auth-test

# Run tests matching pattern
clj -M:test -m kaocha.runner --focus-meta :integration
```

### REPL-based Testing

```clojure
;; In REPL
(require '[kaocha.repl :as k])

;; Run all tests
(k/run)

;; Run specific namespace
(k/run 'app.backend.routes.admin.auth-test)

;; Run with watch
(k/run-all {:watch? true})
```

## Writing Tests

### Basic Test Structure

```clojure
(ns app.backend.routes.admin.my-feature-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [app.backend.test-helpers :as h]
    [app.backend.routes.admin.my-feature :as my-feature]))

(deftest feature-handler-test
  (testing "returns 200 for valid request"
    (let [handler (h/mock-admin-handler my-feature/handler)
          response (handler (h/mock-admin-request :get "/my-feature"))]
      (is (= 200 (:status response)))))
  
  (testing "returns 400 for invalid input"
    (let [handler (h/mock-admin-handler my-feature/handler)
          response (handler (h/mock-admin-request :post "/my-feature" {:invalid "data"}))]
      (is (= 400 (:status response))))))
```

### Testing with Mocked Dependencies

Use `with-redefs` to mock service functions:

```clojure
(deftest dashboard-stats-test
  (testing "returns dashboard statistics"
    (with-redefs [dashboard-service/get-stats (constantly {:users 100 :admins 5})]
      (let [handler (create-handler)
            response (handler (h/mock-admin-request :get "/dashboard"))]
        (is (= 200 (:status response)))
        (is (= 100 (-> response h/parse-response-body :users)))))))
```

### Testing with Mock Database

```clojure
(deftest user-lookup-test
  (let [mock-db (h/mock-db {:users [{:id 1 :email "test@example.com"}]})
        handler (create-handler mock-db)]
    (testing "finds user by email"
      (let [response (handler (h/mock-admin-request :get "/users/1"))]
        (is (= 200 (:status response)))
        (is (= "test@example.com" (-> response h/parse-response-body :email)))))))
```

### Testing HTTP Methods

```clojure
;; GET request
(h/mock-admin-request :get "/users")

;; POST with body
(h/mock-admin-request :post "/users" {:email "new@example.com"})

;; PUT with body
(h/mock-admin-request :put "/users/1" {:name "Updated"})

;; PATCH with body
(h/mock-admin-request :patch "/users/1" {:status "active"})

;; DELETE
(h/mock-admin-request :delete "/users/1")
```

### Testing Response Bodies

```clojure
(deftest response-structure-test
  (let [response (handler request)]
    ;; Check status
    (is (= 200 (:status response)))
    
    ;; Parse and check body
    (let [body (h/parse-response-body response)]
      (is (contains? body :data))
      (is (vector? (:data body)))
      (is (= 10 (count (:data body)))))))
```

### Testing Error Responses

```clojure
(deftest error-handling-test
  (testing "returns 404 for not found"
    (with-redefs [service/find-by-id (constantly nil)]
      (let [response (handler (h/mock-admin-request :get "/items/999"))]
        (is (= 404 (:status response)))
        (is (= "Not found" (-> response h/parse-response-body :error))))))
  
  (testing "returns 500 for service errors"
    (with-redefs [service/process (fn [_] (throw (ex-info "DB error" {})))]
      (let [response (handler (h/mock-admin-request :post "/items" {}))]
        (is (= 500 (:status response)))))))
```

## Test Helpers Reference

### `test/app/backend/test_helpers.clj`

| Function | Purpose |
|----------|---------|
| `mock-db` | Create mock database returning specified results |
| `mock-admin-request` | Create authenticated admin request |
| `mock-admin-handler` | Wrap handler with admin context |
| `parse-response-body` | Parse JSON response body |
| `stub-service-container` | Create stub service container |

### Example Usage

```clojure
(require '[app.backend.test-helpers :as h])

;; Create a mock database
(def db (h/mock-db {:users [{:id 1}] :admins [{:id 1}]}))

;; Create admin request with body
(def req (h/mock-admin-request :post "/users" {:email "test@example.com"}))

;; Parse response
(def body (h/parse-response-body response))
```

## Testing Patterns

### Pattern 1: Handler Factory Testing

When handlers are created by factory functions:

```clojure
(deftest handler-creation-test
  (testing "creates handler with required dependencies"
    (let [handler-fn (my-routes/create-handler {:db mock-db})]
      (is (fn? handler-fn))
      (let [response (handler-fn (h/mock-admin-request :get "/endpoint"))]
        (is (= 200 (:status response)))))))
```

### Pattern 2: Route Structure Testing

Verify route definitions:

```clojure
(deftest routes-structure-test
  (let [routes (my-routes/routes {:db mock-db})]
    (is (vector? routes))
    (is (= "/my-feature" (first routes)))
    (is (map? (second routes)))
    (is (contains? (second routes) :get))))
```

### Pattern 3: Service Function Testing

Test service layer functions:

```clojure
(deftest service-function-test
  (testing "service function exists and is callable"
    (is (fn? my-service/process-data))
    (is (fn? my-service/validate-input))))
```

### Pattern 4: Pagination Testing

```clojure
(deftest pagination-test
  (with-redefs [service/list-all (constantly (repeat 100 {:id 1}))]
    (testing "respects page size"
      (let [response (handler (h/mock-admin-request :get "/items?limit=10"))]
        (is (= 10 (-> response h/parse-response-body :data count)))))
    
    (testing "supports offset"
      (let [response (handler (h/mock-admin-request :get "/items?offset=50"))]
        (is (= 200 (:status response)))))))
```

### Pattern 5: Filter Testing

```clojure
(deftest filter-test
  (testing "filters by status"
    (with-redefs [service/list-filtered 
                  (fn [_ filters] 
                    (is (= "active" (:status filters)))
                    [{:id 1 :status "active"}])]
      (let [response (handler (h/mock-admin-request :get "/items?status=active"))]
        (is (= 200 (:status response)))))))
```

## Debugging Tests

### Verbose Output

```bash
# See full test output
clj -M:test -m kaocha.runner --reporter documentation --no-randomize
```

### Print Debugging

```clojure
(deftest debug-test
  (let [response (handler request)]
    (println "Status:" (:status response))
    (println "Body:" (h/parse-response-body response))
    (is (= 200 (:status response)))))
```

### REPL Debugging

```clojure
;; Load test namespace
(require '[app.backend.routes.admin.my-feature-test :as t] :reload)

;; Run specific test
(t/my-test)

;; Inspect handler
(def h (my-routes/create-handler deps))
(h (mock-admin-request :get "/test"))
```

### Common Issues

| Issue | Solution |
|-------|----------|
| "Handler not found" | Check route registration, verify path |
| "Unexpected 500" | Add try/catch in handler, check `with-redefs` |
| "Wrong status code" | Verify mock returns expected data |
| "Body parsing failed" | Check Content-Type header, response format |

## Integration Testing

### With Real Database

```clojure
(ns app.backend.integration-test
  (:require
    [app.backend.fixtures :as fixtures]
    [clojure.test :refer [deftest is use-fixtures]]))

(use-fixtures :each fixtures/with-transaction-rollback)

(deftest database-integration-test
  (let [db fixtures/*test-db*]
    (testing "creates record in database"
      (let [result (service/create! db {:name "Test"})]
        (is (:id result))
        (is (= "Test" (:name result)))))))
```

### Fixture Setup

```clojure
;; In fixtures.clj
(def ^:dynamic *test-db* nil)

(defn with-transaction-rollback [f]
  (next.jdbc/with-transaction [tx (get-test-db) {:rollback-only true}]
    (binding [*test-db* tx]
      (f))))
```

## Best Practices

1. **Isolate tests**: Each test should be independent
2. **Mock external dependencies**: Use `with-redefs` for services
3. **Test error paths**: Don't just test happy paths
4. **Use descriptive names**: `user-creation-with-invalid-email-returns-400`
5. **Group related tests**: Use `testing` blocks
6. **Keep tests fast**: Mock slow operations
7. **Test behavior, not implementation**: Focus on inputs/outputs

## File Organization

```
test/app/backend/
├── fixtures.clj              # System lifecycle
├── test_helpers.clj          # Shared utilities
├── routes_smoke_test.clj     # Basic route tests
└── routes/
    ├── admin/
    │   ├── auth_test.clj     # Authentication
    │   ├── dashboard_test.clj
    │   ├── users_test.clj
    │   └── ...
    └── api_test.clj          # Public API
```
