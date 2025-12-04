# Frontend Testing Overview

This document provides a comprehensive overview of the frontend testing architecture for this ClojureScript/React application.

## Architecture Summary

The frontend testing stack supports two test execution environments:

| Environment | Command | Use Case |
|-------------|---------|----------|
| **Node.js** | `npm run test:cljs` | Fast local development, CI/CD |
| **Karma (Chrome)** | `npm run test:cljs:karma` | Real browser testing |

Both environments run the same test suite but use different rendering strategies.

## Test Execution Environments

### Node.js Tests (Primary)

- **Build Target**: `:test-node` in `shadow-cljs.edn`
- **Output**: `target/test-node.cjs`
- **DOM Simulation**: [jsdom](https://github.com/jsdom/jsdom) + jsdom-global
- **Speed**: Fast (~5-10 seconds for full suite)

Node.js tests are the primary development tests. They run quickly and simulate browser APIs using jsdom.

### Karma Browser Tests

- **Build Target**: `:karma-test` in `shadow-cljs.edn`
- **Output**: `target/karma-test.js`
- **Browser**: Chrome Headless
- **Configuration**: `karma.conf.cjs`

Karma tests run in a real browser, catching issues that jsdom simulation might miss.

## Key Files & Locations

### Test Utilities

| File | Purpose |
|------|---------|
| `src/app/frontend/utils/test_utils.cljs` | Core rendering & assertion utilities |
| `test/app/admin/frontend/test_setup.cljs` | Admin panel test fixtures & helpers |
| `test/karma-adapter.js` | Custom Karma adapter for shadow-cljs |
| `test/capture-test-results.js` | Test result capture for Karma |

### Test Structure

```
test/
├── app/
│   ├── admin/frontend/
│   │   ├── test_setup.cljs          # Admin fixtures
│   │   ├── adapters/                # Adapter tests
│   │   ├── components/              # Component tests
│   │   ├── events/                  # Re-frame event tests
│   │   ├── security/                # Security wrapper tests
│   │   └── routes_test.cljs         # Routing tests
│   └── template/frontend/
│       ├── api/                     # HTTP client tests
│       ├── components/              # Shared component tests
│       ├── events/                  # Event handler tests
│       ├── pages/                   # Page component tests
│       ├── subs/                    # Subscription tests
│       └── utils/                   # Utility function tests
├── karma-adapter.js                 # Karma integration
└── capture-test-results.js          # Result aggregation
```

## Test Utilities API

### Core Functions

```clojure
(ns my-test
  (:require
    [app.frontend.utils.test-utils :as test-utils]
    [uix.core :refer [$]]))

;; Initialize test environment (call once at namespace level)
(test-utils/setup-test-environment!)

;; Render component to HTML string
(test-utils/render-to-static-markup ($ my-component {:prop "value"}))
;; => "<div class=\"my-class\">...</div>"

;; Enhanced render with better error handling
(test-utils/enhanced-render-to-static-markup ($ my-component props))

;; Check if markup contains content
(test-utils/component-contains? markup "expected-text")
;; => true/false

;; Extract CSS classes from markup
(test-utils/component-classes markup)
;; => #{"class-one" "class-two"}
```

### Test Setup Helpers (Admin)

```clojure
(ns my-admin-test
  (:require
    [app.admin.frontend.test-setup :as setup]
    [re-frame.db :as rf-db]))

;; Reset app-db to clean admin state
(setup/reset-db!)

;; Set up entity subscriptions for testing
(setup/setup-entity-subscriptions! :users nil "Error message")

;; Capture HTTP requests from re-frame effects
(setup/install-http-stub!)

;; Get last captured request
(setup/last-http-request)

;; Simulate successful HTTP response
(setup/respond-success! {:data "response"})

;; Simulate failed HTTP response
(setup/respond-failure! {:status 500 :body "Error"})
```

## Rendering Strategy

### How Component Rendering Works

The test utilities use a **dual rendering strategy**:

1. **DOM Rendering** (preferred): Uses React's `flushSync` to synchronously render components to a real DOM element, then extracts `innerHTML`.

2. **Mock Fallback**: When DOM rendering returns empty (common in browser due to hooks/subscriptions), a mock renderer generates expected HTML based on component props.

```
┌─────────────────┐
│  Test calls     │
│  render-to-     │
│  static-markup  │
└────────┬────────┘
         │
         ▼
┌─────────────────┐     ┌─────────────────┐
│ DOM available?  │ No  │ Generate mock   │
│ (js/document)   │────►│ HTML from props │
└────────┬────────┘     └────────┬────────┘
         │ Yes                   │
         ▼                       │
┌─────────────────┐              │
│ Create <div>    │              │
│ container       │              │
└────────┬────────┘              │
         │                       │
         ▼                       │
┌─────────────────┐              │
│ React flushSync │              │
│ render          │              │
└────────┬────────┘              │
         │                       │
         ▼                       │
┌─────────────────┐     ┌────────┴────────┐
│ innerHTML empty?│ Yes │ Mock fallback   │
└────────┬────────┘────►│ (extract props) │
         │ No           └────────┬────────┘
         │                       │
         ▼                       │
┌─────────────────┐              │
│ Return actual   │              │
│ HTML markup     │◄─────────────┘
└─────────────────┘
```

### Props Extraction (UIx/React)

In browser environments, UIx stores component props in an `argv` property as a ClojureScript PersistentArrayMap. The test utilities handle this automatically:

```clojure
;; UIx element structure:
;; element.props.argv = PersistentArrayMap{:entity-name :users, :item {...}, ...}

;; Props are extracted from argv directly as a CLJS map
(let [argv (gobj/get (.-props element) "argv")]
  (when (map? argv)
    (:entity-name argv)))  ; => :users
```

## Subscription Testing

### Testing Components with Subscriptions

Components that use `rf/subscribe` need subscriptions registered before rendering:

```clojure
(deftest my-subscription-test
  (testing "component shows error from subscription"
    ;; Reset to clean state
    (setup/reset-db!)
    
    ;; Register subscription that returns error
    (setup/setup-entity-subscriptions! :users nil "Failed to load users")
    
    ;; Render component
    (let [markup (test-utils/enhanced-render-to-static-markup
                   ($ my-component {:entity-name :users}))]
      (is (str/includes? markup "Failed to load users")))))
```

### How Mock Reads Subscriptions

The mock fallback can read subscription values via `safe-subscribe`:

```clojure
;; The mock tries to read subscription handlers directly
(defn- safe-subscribe [sub-key]
  (let [db @rf-db/app-db
        handler (get-in @rf-registrar/kind->id->handler [:sub sub-key])]
    (when handler
      (let [result (handler db [sub-key])]
        ;; Deref if result is a Reaction
        (if (satisfies? IDeref result) @result result)))))
```

## HTTP Effect Testing

### Capturing HTTP Requests

Tests can intercept `:http-xhrio` effects to verify API calls:

```clojure
(deftest api-call-test
  (setup/reset-db!)
  (setup/install-http-stub!)
  
  ;; Dispatch event that makes HTTP request
  (rf/dispatch-sync [:fetch-users])
  
  ;; Verify request was made
  (let [req (setup/last-http-request)]
    (is (= :get (:method req)))
    (is (str/includes? (:uri req) "/api/users")))
  
  ;; Simulate successful response
  (setup/respond-success! {:users [{:id 1 :name "Test"}]})
  
  ;; Verify app state updated
  (is (= 1 (count (get-in @rf-db/app-db [:admin/users])))))
```

## Shadow-cljs Build Configuration

### Test Builds in `shadow-cljs.edn`

```clojure
{:builds
 {;; Node.js test build (fast, uses jsdom)
  :test-node
  {:target :node-test
   :output-to "target/test-node.cjs"
   :ns-regexp "-test$"}

  ;; Karma browser test build
  :karma-test
  {:target :karma
   :output-to "target/karma-test.js"
   :ns-regexp "-test$"}

  ;; Browser test build (for watch mode dev)
  :test
  {:target :browser-test
   :test-dir "target/test"
   :ns-regexp "-test$"
   :devtools {:http-root "target/test"
              :http-port 9095}}}}
```

## Test File Naming Convention

Tests must follow this naming pattern to be included:

- **Pattern**: `*_test.cljs` (files ending with `_test.cljs`)
- **Namespace**: `*-test` (namespaces ending with `-test`)

```clojure
;; File: test/app/admin/frontend/components/my_component_test.cljs
(ns app.admin.frontend.components.my-component-test
  (:require
    [cljs.test :refer [deftest is testing]]
    ...))
```

## Common Test Patterns

### Component Rendering Test

```clojure
(deftest my-component-renders-correctly
  (testing "renders with expected classes"
    (setup/reset-db!)
    (let [markup (test-utils/enhanced-render-to-static-markup
                   ($ my-component {:title "Hello"}))]
      (is (test-utils/component-contains? markup "Hello"))
      (is (contains? (test-utils/component-classes markup) "my-class")))))
```

### Event Handler Test

```clojure
(deftest event-updates-db
  (testing "event handler updates app-db correctly"
    (setup/reset-db!)
    
    ;; Dispatch event
    (rf/dispatch-sync [:my-event {:value "test"}])
    
    ;; Verify db changed
    (is (= "test" (get-in @rf-db/app-db [:path :to :value])))))
```

### Subscription Test

```clojure
(deftest subscription-returns-correct-data
  (testing "subscription computes derived state"
    (setup/reset-db!)
    
    ;; Set up base data
    (swap! rf-db/app-db assoc :items [{:id 1} {:id 2}])
    
    ;; Test subscription
    (let [handler (get-in @rf-registrar/kind->id->handler [:sub :item-count])]
      (is (= 2 (handler @rf-db/app-db [:item-count]))))))
```

## Debugging Tests

### Viewing Test Output

```bash
# Node.js tests with verbose output
npm run test:cljs 2>&1 | tee test-output.txt

# Karma tests with console output
npm run test:cljs:karma 2>&1 | tee karma-output.txt
```

### Adding Debug Logging

```clojure
;; In test file
(deftest debug-test
  (testing "debugging render output"
    (setup/reset-db!)
    (let [markup (test-utils/enhanced-render-to-static-markup
                   ($ my-component props))]
      ;; Print to console for debugging
      (println "Rendered markup:" markup)
      (is (str/includes? markup "expected")))))
```

## Related Documentation

- [Frontend Testing Development Guide](./development-guide.md) - How to write and run tests
- [UIx Documentation](../libs/uix/docs/) - UIx component framework
- [Re-frame Testing](https://day8.github.io/re-frame/Testing/) - Re-frame testing patterns
