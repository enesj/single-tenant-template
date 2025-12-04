# Frontend Testing Development Guide

Practical guide for developers and AI agents on writing, running, and debugging frontend tests.

## Quick Start

### Running Tests

```bash
# Run all tests (Node.js - fast, primary)
npm run test:cljs

# Run browser tests (Karma/Chrome - real browser)
npm run test:cljs:karma

# Watch mode for development
npm run test:cljs:watch
```

### Test Results Summary

After running tests, you'll see output like:
```
Testing app.admin.frontend.components.enhanced-action-buttons-test
Testing app.template.frontend.subs.list-test

Ran 212 tests containing 1251 assertions.
0 failures, 0 errors.
```

## Writing Tests

### Basic Test Structure

```clojure
(ns app.my-feature.frontend.my-test
  "Tests for my-feature component/event/subscription"
  (:require
    [app.admin.frontend.test-setup :as setup]
    [app.frontend.utils.test-utils :as test-utils]
    [cljs.test :refer [deftest is testing]]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]
    [uix.core :refer [$]]))

;; Initialize test environment once per namespace
(test-utils/setup-test-environment!)

(deftest my-feature-test
  (testing "description of what's being tested"
    ;; Arrange: Reset state
    (setup/reset-db!)
    
    ;; Act: Perform the action
    (let [result (do-something)]
      
      ;; Assert: Verify the outcome
      (is (= expected result)))))
```

### Component Tests

#### Testing Component Rendering

```clojure
(ns app.admin.frontend.components.button-test
  (:require
    [app.admin.frontend.components.button :as button]
    [app.admin.frontend.test-setup :as setup]
    [app.frontend.utils.test-utils :as test-utils]
    [cljs.test :refer [deftest is testing]]
    [clojure.string :as str]
    [uix.core :refer [$]]))

(test-utils/setup-test-environment!)

(deftest button-renders-with-label
  (testing "renders button with provided label"
    (setup/reset-db!)
    (let [markup (test-utils/enhanced-render-to-static-markup
                   ($ button/primary-button {:label "Click Me"}))]
      (is (str/includes? markup "Click Me"))
      (is (str/includes? markup "ds-btn")))))

(deftest button-has-correct-css-classes
  (testing "applies expected CSS classes"
    (setup/reset-db!)
    (let [markup (test-utils/enhanced-render-to-static-markup
                   ($ button/primary-button {:variant :danger}))
          classes (test-utils/component-classes markup)]
      (is (contains? classes "ds-btn-error")))))
```

#### Testing Components with Props

```clojure
(defn render-action-buttons [props]
  (test-utils/enhanced-render-to-static-markup
    ($ action-buttons/enhanced-action-buttons props)))

(deftest action-buttons-renders-edit-when-enabled
  (testing "renders edit button when show-edit? is true"
    (setup/reset-db!)
    (let [markup (render-action-buttons
                   {:entity-name :users
                    :item {:id 123 :name "Test User"}
                    :show-edit? true
                    :show-delete? false})]
      (is (str/includes? markup "btn-edit-users-123")))))

(deftest action-buttons-hides-delete-when-disabled
  (testing "does not render delete button when show-delete? is false"
    (setup/reset-db!)
    (let [markup (render-action-buttons
                   {:entity-name :users
                    :item {:id 123}
                    :show-edit? false
                    :show-delete? false})]
      (is (not (str/includes? markup "btn-delete"))))))
```

### Event Tests

#### Testing Re-frame Event Handlers

```clojure
(ns app.admin.frontend.events.users-test
  (:require
    [app.admin.frontend.events.users :as users-events]
    [app.admin.frontend.test-setup :as setup]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(deftest set-users-event-updates-db
  (testing "::set-users stores users in app-db"
    (setup/reset-db!)
    
    (rf/dispatch-sync [::users-events/set-users 
                       [{:id 1 :name "Alice"}
                        {:id 2 :name "Bob"}]])
    
    (is (= 2 (count (get-in @rf-db/app-db [:admin/users]))))
    (is (= "Alice" (get-in @rf-db/app-db [:admin/users 0 :name])))))
```

#### Testing HTTP Effects

```clojure
(deftest fetch-users-makes-api-call
  (testing "::fetch-users dispatches HTTP request"
    (setup/reset-db!)
    (setup/install-http-stub!)
    
    ;; Dispatch the fetch event
    (rf/dispatch-sync [::users-events/fetch-users])
    
    ;; Verify HTTP request was made
    (let [req (setup/last-http-request)]
      (is (some? req) "HTTP request should be captured")
      (is (= :get (:method req)))
      (is (str/includes? (:uri req) "/api/admin/users")))))

(deftest fetch-users-handles-success
  (testing "successful response updates app-db"
    (setup/reset-db!)
    (setup/install-http-stub!)
    
    (rf/dispatch-sync [::users-events/fetch-users])
    
    ;; Simulate API success response
    (setup/respond-success! {:users [{:id 1 :name "Test"}]})
    
    ;; Verify state updated
    (is (= 1 (count (get-in @rf-db/app-db [:admin/users]))))))

(deftest fetch-users-handles-failure
  (testing "failed response sets error state"
    (setup/reset-db!)
    (setup/install-http-stub!)
    
    (rf/dispatch-sync [::users-events/fetch-users])
    
    ;; Simulate API failure
    (setup/respond-failure! {:status 500 :body "Server error"})
    
    ;; Verify error state
    (is (some? (get-in @rf-db/app-db [:admin/users-error])))))
```

### Subscription Tests

```clojure
(ns app.template.frontend.subs.list-test
  (:require
    [app.admin.frontend.test-setup :as setup]
    [app.template.frontend.subs.list :as list-subs]
    [cljs.test :refer [deftest is testing]]
    [re-frame.db :as rf-db]
    [re-frame.registrar :as rf-registrar]))

(defn get-subscription [sub-key]
  (let [handler (get-in @rf-registrar/kind->id->handler [:sub sub-key])]
    (when handler
      (handler @rf-db/app-db [sub-key]))))

(deftest list-items-subscription-returns-items
  (testing "::list-items returns items from db"
    (setup/reset-db!)
    
    ;; Set up test data
    (swap! rf-db/app-db assoc-in [:entities :users :items]
           [{:id 1} {:id 2} {:id 3}])
    
    ;; Test subscription
    (let [result (get-subscription ::list-subs/list-items)]
      (is (= 3 (count result))))))
```

## Testing Patterns

### Pattern: Prime State Before Test

For components that depend on specific app-db state:

```clojure
(defn prime-constraint!
  "Set up deletion constraint state for testing"
  [entity-type {:keys [id can-delete? loading? error]}]
  (let [entity-id (or id 123)]
    (swap! rf-db/app-db
      (fn [db]
        (-> db
          (assoc-in [:deletion-constraints entity-type :results entity-id]
                    {:can-delete? can-delete?})
          (cond-> loading?
            (assoc-in [:deletion-constraints entity-type :loading entity-id] true)))))))

(deftest delete-button-disabled-when-cannot-delete
  (testing "shows disabled state when can-delete? is false"
    (setup/reset-db!)
    (prime-constraint! :users {:id 456 :can-delete? false})
    
    (let [markup (render-buttons {:entity-name :users 
                                  :item {:id 456}
                                  :show-delete? true})]
      (is (str/includes? markup "cursor-not-allowed")))))
```

### Pattern: Testing Conditional Rendering

```clojure
(deftest component-shows-loading-state
  (testing "shows spinner when loading"
    (setup/reset-db!)
    (swap! rf-db/app-db assoc :loading? true)
    
    (let [markup (test-utils/enhanced-render-to-static-markup
                   ($ my-component {}))]
      (is (str/includes? markup "ds-loading-spinner"))))
  
  (testing "shows content when not loading"
    (setup/reset-db!)
    (swap! rf-db/app-db assoc :loading? false)
    
    (let [markup (test-utils/enhanced-render-to-static-markup
                   ($ my-component {}))]
      (is (not (str/includes? markup "ds-loading-spinner")))
      (is (str/includes? markup "content")))))
```

### Pattern: Testing with Subscriptions

```clojure
(deftest component-displays-subscription-value
  (testing "shows error message from subscription"
    (setup/reset-db!)
    
    ;; Register test subscription
    (setup/setup-entity-subscriptions! :users nil "Failed to load users")
    
    (let [markup (test-utils/enhanced-render-to-static-markup
                   ($ admin-page-wrapper {:entity-name :users}))]
      (is (str/includes? markup "Failed to load users")))))
```

## Debugging Tests

### Finding Failing Tests

```bash
# Run tests and save output
npm run test:cljs 2>&1 | tee /tmp/test-output.txt

# Search for failures
grep -A 5 "FAIL in" /tmp/test-output.txt
```

### Debugging Specific Test

1. **Add println statements**:
```clojure
(deftest debug-my-test
  (testing "debugging"
    (setup/reset-db!)
    (println "DB state:" @rf-db/app-db)
    
    (let [markup (test-utils/enhanced-render-to-static-markup
                   ($ my-component props))]
      (println "Rendered markup:" markup)
      (is (str/includes? markup "expected")))))
```

2. **Run single test** (in REPL or by temporarily commenting others):
```clojure
;; In test file, comment out other tests temporarily
#_(deftest other-test ...)
```

### Common Issues & Solutions

#### Issue: Test passes in Node.js but fails in Karma

**Cause**: Browser environment handles props differently (UIx argv structure)

**Solution**: The test utilities handle this automatically via `extract-props-data`. If you see issues with prop extraction, ensure you're using `enhanced-render-to-static-markup`.

#### Issue: "Cannot read property of undefined"

**Cause**: App-db not initialized or missing expected keys

**Solution**: Always call `(setup/reset-db!)` at the start of each test:
```clojure
(deftest my-test
  (testing "something"
    (setup/reset-db!)  ; <-- Always include this
    ...))
```

#### Issue: HTTP request not captured

**Cause**: HTTP stub not installed

**Solution**: Call `install-http-stub!` before dispatching events that make HTTP calls:
```clojure
(deftest api-test
  (testing "api call"
    (setup/reset-db!)
    (setup/install-http-stub!)  ; <-- Required for HTTP capture
    (rf/dispatch-sync [:fetch-data])
    ...))
```

#### Issue: Subscription returns nil

**Cause**: Subscription handler not registered or wrong subscription key

**Solution**: Use `setup/setup-entity-subscriptions!` to register test subscriptions:
```clojure
(setup/setup-entity-subscriptions! :entity-name loading-value error-value)
```

## AI Agent Guidelines

### When Investigating Test Failures

1. **Save test output first** - Don't re-run tests repeatedly:
   ```bash
   npm run test:cljs 2>&1 | tee /tmp/test-output.txt
   ```

2. **Isolate the failing test** - Run or analyze one test at a time

3. **Check the test setup** - Ensure `reset-db!` and any stubs are called

4. **Examine the actual vs expected** - Look at the assertion error message

5. **Add targeted logging** - Use `println` to see actual values

### When Writing New Tests

1. **Follow existing patterns** - Look at similar tests in the same directory

2. **Use the test utilities** - Don't reinvent rendering logic

3. **Test one thing per test** - Keep tests focused and descriptive

4. **Use meaningful test names** - Names should describe what's being tested:
   ```clojure
   ;; Good
   (deftest button-disables-when-loading)
   
   ;; Bad  
   (deftest test-1)
   ```

### When Fixing Broken Tests

1. **Understand what changed** - Check recent commits or changes

2. **Verify the component behavior** - Is the test wrong or the component?

3. **Update test expectations** - If component behavior legitimately changed

4. **Fix the component** - If the test expectation is correct

## Test Commands Reference

| Command | Description | When to Use |
|---------|-------------|-------------|
| `npm run test:cljs` | Node.js tests (fast) | Primary development testing |
| `npm run test:cljs:karma` | Karma browser tests | Real browser verification |
| `npm run test:cljs:watch` | Watch mode | Continuous testing during dev |
| `npm run test:cljs:karma:compile` | Compile only | Debugging build issues |
| `npm run test:cljs:karma:run` | Run only | Run pre-compiled tests |

## Integration with Development Workflow

### Before Committing

```bash
# Run full test suite
npm run test:cljs

# Optionally run browser tests
npm run test:cljs:karma
```

### During Development

```bash
# Start watch mode in separate terminal
npm run test:cljs:watch

# Or run tests manually after changes
npm run test:cljs
```

### CI/CD Pipeline

The CI pipeline runs:
```bash
npm run test:cljs:ci
```

This ensures all tests pass before merging.

## Related Documentation

- [Frontend Testing Overview](./overview.md) - Architecture and implementation details
- [Re-frame Documentation](https://day8.github.io/re-frame/) - Re-frame patterns
- [cljs.test Reference](https://clojurescript.org/tools/testing) - ClojureScript testing
