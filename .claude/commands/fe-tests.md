---
description: "Automated frontend test failure analysis and intelligent resolution using research agent"
---

# fe-tests - Frontend Test Failure Resolution

**Automated test failure analysis and intelligent resolution using specialized research agent for systematic ClojureScript/Shadow-CLJS test debugging.**

## When to Use

Use this command when:
- Frontend tests are failing and need systematic analysis
- Karma/CLJS test runner shows persistent failures
- Multiple test files have similar error patterns
- Need automated debugging and fix implementation
- Complex test failures require codebase investigation
- Test environment setup issues or configuration problems
- Performance-related test failures or timeout issues
- User mentions keywords: "test failures", "cljs tests", "karma", "frontend debugging"

## Prerequisites

The project should have:
1. Active Karma/Shadow-CLJS test environment (`bb dev:frontend` running)
2. Failing frontend tests in `test/` directory
3. Shadow-CLJS compilation working (`shadow-cljs compile` successful)
4. Access to test output files and karma configuration
5. Available frontend source code in `src/app/frontend/`

## Analysis Workflow

### 1. Extract Test Failures

Run karma test execution and capture comprehensive failure data:

**Test Execution**:
```bash
# Run frontend tests with detailed output
npm run test:cljs

# Alternative using babashka
bb run-karma

# Get detailed test results
bb test:cljs:karma

# Extract specific failure information
find test-results -name "*.json" -exec cat {} \;
```

**Failure Data Extraction**:
- Test file names and line numbers of failures
- Error messages and stack traces
- Expected vs actual results
- Timing information and timeout details
- Affected components and functions
- Browser console errors during test execution

### 2. Research Agent Analysis

Deploy specialized research agent for deep codebase investigation:

**Agent Launch Parameters**:
```bash
# Launch research agent with comprehensive context
/fe-tests "analyze cljs test failures in component authentication"
```

**Research Focus Areas**:
- Test configuration issues (karma.conf.js, shadow-cljs.edn)
- Component implementation problems in failing tests
- Re-frame event handling and subscription issues
- Namespace loading and dependency resolution
- Browser-specific compatibility issues
- Test environment and mocking problems
- Performance bottlenecks causing test timeouts

### 3. Systematic Problem Resolution

**Pattern-Based Fixes**:
- **Namespace Issues**: Fix `(:require ...)` and namespace declarations
- **Test Configuration**: Update karma config for proper source loading
- **Component Lifecycle**: Fix `:component-did-mount` and event handler issues
- **Re-frame Integration**: Correct event dispatch and subscription patterns
- **Async Handling**: Fix promises, async operations, and timing issues
- **Test Assertions**: Correct `is`, `are`, and testing macro usage
- **Mock Implementation**: Fix test doubles and stub implementations
- **Browser Compatibility**: Handle browser-specific APIs and polyfills

**Iterative Fix Implementation**:
```clojure
;; Example of systematic test fix
(defn fix-authentication-test [test-file]
  (->> (slurp test-file)
      (str/replace "(deftest auth-login-test" "(deftest auth-login-test\n  ;; Component lifecycle fixed\n  (let [comp (auth-component {})]")
      (spit test-file))
```

### 4. Intelligent Test Strategy

**Test Organization and Execution**:
- **Unit Tests**: Individual component functions in isolation
- **Integration Tests**: Component interactions and re-frame events
- **End-to-End Tests**: Complete user workflows
- **Regression Tests**: Previously fixed issues that resurface
- **Performance Tests**: Component rendering and operation timing
- **Browser Matrix Tests**: Cross-browser compatibility verification

**Test Environment Optimization**:
```clojure
;; Optimized test configuration
{:karma-config
 {:frameworks ["jasmine" "cljs"]
  :files ["test/**/*.cljs"]
  :preprocessors ["shadow-cljs"]
  :browsers ["ChromeHeadless" "Firefox"]
  :single-run false
  :auto-watch true
  :reporters ["dots" "junit"]
  :client {
    :args ["--no-sandbox" "--disable-gpu"]
    :env {
      "NODE_ENV" "test"
      "REAGENT_TRACE" true
    }
  }}}
```

## Tool Integration

### Frontend Testing Tools

**ClojureScript Evaluation**:
```clojure
;; Test component mounting and lifecycle
clojure_mcp_clojurescript_eval "
(require '[app.frontend.components.auth :as auth])

;; Test component lifecycle
(let [test-db (re-frame.db/atom {:auth {:loading? false :error nil})
      component (auth/auth-form {})]

  ;; Verify component renders without errors
  (when-not (nil? component)
    (println \"✓ Auth component mounts successfully\"))

  ;; Test event handlers
  (when-let [event-result ((:on-login component) {:email \"test@example.com\"})]
    (println \"✓ Login event handler works:\" event-result)))"

;; Test subscription handling
clojure_mcp_clojurescript_eval "
(require '[re-frame.core :as rf])

;; Verify subscription patterns
(let [sub-data (rf/subscribe [:auth/current-user identity])
      dispatch-result (rf/dispatch [:auth/check-auth])]

  (println \"Subscription data:\" sub-data)
  (println \"Dispatch result:\" dispatch-result))"
```

**Clojure Evaluation**:
```clojure
;; Test backend integration for frontend
clojure_mcp_clojure_eval "
(require '[app.backend.handlers.auth :as auth-handlers])

;; Verify handler implementations
(println \"Available auth handlers:\" (keys auth-handlers))
(println \"Login handler exists:\" (contains? (keys auth-handlers) :login))

;; Test validation functions
(when-let [validate-fn (get-in auth-handlers [:validation :email])]
  (println \"Email validation:\" (validate-fn \"test@example.com\")))"

;; Check database schema for auth
(require '[app.db.models :as models])
(when-let [user-schema (:users models)]
  (println \"User schema:\" (select-keys user-schema [:email :password :role])))"
```

**Chrome MCP Tools**:
```javascript
// Test frontend UI interactions
chrome_mcp_navigate("http://localhost:8080/login");

// Fill authentication form
chrome_mcp_fill_or_select("#email", "test@example.com");
chrome_mcp_fill_or_select("#password", "testpassword");

// Test form submission and navigation
chrome_mcp_click_element("#login-button", {waitForNavigation: true, timeout: 10000});

// Verify UI state after login
chrome_mcp_get_web_content({selector: ".user-profile", textContent: true});

// Test form validation
chrome_mcp_click_element("#register-button");
chrome_mcp_get_web_content({selector: ".error-message", textContent: true});
```

**Bash Testing Tools**:
```bash
# Run specific test files
npm run test:cljs -- --grep "authentication"

# Run tests with coverage
npm run test:cljs:coverage

# Debug specific component tests
npm run test:cljs -- --testName "UserLoginComponent"

# Run tests in specific browser
npm run test:cljs:chrome

# Check test compilation
shadow-cljs compile test

# Monitor test execution in watch mode
npm run test:cljs:watch
```

## Common Test Failure Patterns

### Component Lifecycle Issues

**Problem**: Components not mounting/unmounting properly
```clojure
;; Before - Problematic test
(deftest component-lifecycle-test
  (testing "component mounts"
    (is (= (:mounted @app-db) true)))

;; After - Fixed test
(deftest component-lifecycle-test
  (testing "component mounts"
    (let [component (my-component {})]
      ;; Test component creation and lifecycle
      (is (some? component))
      ;; Test re-frame integration
      (is (= (get-in @app-db [:ui :mounted-component]) "my-component")))))
```

**Solution**: Use proper re-frame integration and component creation patterns
```clojure
;; Fixed component test with proper lifecycle
(deftest component-lifecycle-test
  (testing "component mounts"
    (let [test-db (re-frame.db/atom {:ui {}})]
      ;; Dispatch component mount event
      (rf/dispatch [:ui/component-mounted "my-component"])
      ;; Verify component state
      (is (= (get-in @test-db [:ui :mounted-component]) "my-component")))))
```

### Re-frame Integration Problems

**Problem**: Event dispatch and subscription timing issues
```clojure
;; Before - Race condition in test
(deftest auth-flow-test
  (async done
    (rf/dispatch [:auth/login "test@example.com" "password"])
    (rf/subscribe [:auth/current-user]
      (fn [user]
        (when user
          (is (= (:email user) "test@example.com"))
          (done)))))
```

**Solution**: Use proper async testing patterns and re-frame testing utilities
```clojure
;; After - Proper async test with re-frame-test
(deftest auth-flow-test
  (testing "authentication flow"
    (rftest/run-async
      (fn [done]
        (rf/dispatch [:auth/login "test@example.com" "password"])
        (rf/subscribe [:auth/current-user]
          (fn [user]
            (when (and user (= (:email user) "test@example.com"))
              (is (= (:authenticated? user) true))
              (done)))))))
```

### Testing Configuration Issues

**Problem**: Karma not loading compiled CLJS properly
```javascript
// Before - Broken karma config
module.exports = function(config) {
  config.set({
    files: [
      'src/app/frontend/**/*.cljs',  // Wrong extension
    ]
  });
};
```

**Solution**: Proper Shadow-CLJS integration
```javascript
// After - Fixed karma config
module.exports = function(config) {
  config.set({
    frameworks: ['jasmine', 'cljs'],
    files: [
      'target/app/tests.js',  // Correct compiled output
      'app/tests.js'           // Application test runner
    ],
    preprocessors: ['shadow-cljs'],
    browsers: ['ChromeHeadless'],
    client: {
      args: ['--no-sandbox', '--disable-dev-shm-usage'],
      env: {
        'NODE_ENV': 'test'
      }
    }
  });
};
```

## Error Handling and Recovery

### Test Execution Failures

**No Tests Found**:
```bash
# Handle missing test files
if [ ! -d "test" ]; then
    echo "❌ No test directory found"
    echo "Creating basic test structure..."
    mkdir -p test/{unit,integration,e2e}/frontend/components
    echo "Please add test files to test/frontend/components/"
fi
```

**Compilation Failures**:
```bash
# Handle Shadow-CLJS compilation errors
shadow-cljs compile test 2>&1 | grep -E "(ERROR|WARNING)" || {
    echo "❌ Compilation failed - check for syntax errors"
    echo "Common issues: missing parentheses, wrong function names"
    exit 1
}
```

**Runtime Test Failures**:
```bash
# Handle browser console errors during tests
npm run test:cljs 2>&1 | grep -i "failed" | while read line; do
    echo "🔍 Test failure detected: $line"
    # Extract test name and error
    if [[ $line =~ .*failed.*(.*) ]]; then
        echo "   Test: ${BASH_REMATCH[1]}"
        echo "   Check test file: test/frontend/components/${BASH_REMATCH[1]}_test.cljs"
    fi
done
```

### Database and Backend Issues

**Test Database Setup**:
```clojure
;; Ensure test database is properly configured
clojure_mcp_clojure_eval "
(require '[app.db.connection :as db])

;; Test database connection for tests
(when-let [conn (db/test-connection)]
  (println \"✓ Test database connection:\" (:status conn))
  (println \"Available tables:\" (:tables conn))

  ;; Clean test data
  (db/clean-test-data conn)
  (println \"✓ Test database cleaned\"))"
```

**API Endpoint Mocking**:
```clojure
;; Mock API responses for frontend tests
(defmethod mock-http-fx :auth/login
  [{:keys [db]} [_ params]]
  {:db (assoc db :auth/loading? true)
   :http-xhrio {:method :post
                  :uri "/auth/login"
                  :params params
                  :on-success [:auth/login-success]
                  :on-failure [:auth/login-failure]
                  :response {:status 200 :body {:token "test-token" :user {:id 1 :email "test@example.com"}}}}})
```

## Advanced Debugging Strategies

### Performance Testing

**Component Render Performance**:
```clojure
;; Test component rendering performance
clojure_mcp_clojurescript_eval "
(require '[app.frontend.components.user-list :as user-list])

;; Measure render time
(let [start-time (js/performance.now)
      component (user-list/user-list (range 100)) ; 100 users
      end-time (js/performance.now)
      render-time (- end-time start-time)]

  (println \"📊 Component render time:\" render-time \"ms\")
  (when (> render-time 100)  ; Log slow renders
    (println \"⚠ Slow component rendering detected\")))"
```

**Memory Leak Detection**:
```clojure
;; Test for memory leaks in component lifecycle
clojure_mcp_clojurescript_eval "
(require '[app.frontend.components.data-table :as data-table])

;; Test component cleanup
(dotimes [i 10]
  (let [component (data-table/data-table {:data (range 50)})]
    ;; Simulate mount/unmount cycle
    (when-let [cleanup (:on-unmount component)]
      (cleanup))
    ;; Force garbage collection if available
    (when (exists? js/window.gc)
      (js/window.gc)))
  (js/setTimeout #(println \"Memory test cycle\" i \"completed\") 100))"
```

### Cross-Browser Testing

**Browser Compatibility Matrix**:
```clojure
;; Test component functionality across browsers
(defn cross-browser-test [component-name test-fn]
  (let [browsers [:ChromeHeadless :Firefox :Safari]]
        results (atom {})]
    (doseq [browser browsers]
      (println \"Testing\" component-name \"in\" browser)
      (let [result (test-fn browser)]
        (swap! results assoc browser result)
        (when-not result
          (println \"❌ Browser compatibility issue in\" browser))))
    @results))
```

### Accessibility Testing

**ARIA and Keyboard Navigation**:
```javascript
// Test keyboard navigation
chrome_mcp_keyboard("Tab", {selector: "#login-form"});

// Test ARIA labels
chrome_mcp_get_web_content({
  selector: "[aria-label]",
  includeCoordinates: true
});

// Test screen reader compatibility
chrome_mcp_inject_script(:type "MAIN", "
  // Test for screen reader announcements
  const announcements = [];
  const observer = new MutationObserver(mutations => {
    mutations.forEach(mutation => {
      if (mutation.target.getAttribute('aria-live')) {
        announcements.push(mutation.target.textContent);
      }
    });
  });
  observer.observe(document.body, { childList: true, subtree: true });
  console.log('Screen reader announcements:', announcements);
");
```

## Integration with Development Tools

### Continuous Testing

**Watch Mode Testing**:
```bash
# Run tests in continuous watch mode
npm run test:cljs:watch

# Combine with file watching for live changes
inotifywait -r modify src/app/frontend/ -e create,modify -- npm run test:cljs:watch
```

**Test Coverage Analysis**:
```clojure
;; Analyze test coverage using clojure-mcp
clojure_mcp_clojure_eval "
(require '[clojure.walk :as walk])

;; Find uncovered frontend files
(let [source-files (->> (walk/walk \"src/app/frontend\")
                     (filter #(clojure.string/ends-with? % \".cljs\"))
                     (remove #(clojure.string/includes? % \"test_\")))
      test-files (->> (walk/walk \"test/frontend\")
                     (filter #(clojure.string/ends-with? % \".cljs\")))

  (println \"Source files:\" (count source-files))
  (println \"Test files:\" (count test-files))
  (println \"Coverage ratio:\" (/ (count test-files) (count source-files))))"
```

### Build Integration Testing

**Shadow-CLJS with Tests**:
```clojure
;; Test compilation of test files
clojure_mcp_clojure_eval "
(require '[shadow-cljs.devtools.api :as shadow])

;; Compile tests and check for errors
(let [compile-result (shadow/compile :test)]
  (when (:errors compile-result)
    (println \"❌ Test compilation errors:\")
    (doseq [error (:errors compile-result)]
      (println \"  \" (:file error) \":\" (:line error) \" - \" (:message error))))
  (when (:warnings compile-result)
    (println \"⚠ Test compilation warnings:\")
    (doseq [warning (:warnings compile-result)]
      (println \"  \" (:file warning) \":\" (:line warning) \" - \" (:message warning)))))"
```

## Output Format

Provide clear, structured analysis of test failures:

```markdown
## Frontend Test Analysis

### Test Failure Summary
- **Total Tests**: 45 tests across 12 files
- **Failing Tests**: 3 tests (6.7% failure rate)
- **Error Categories**: 2 integration tests, 1 unit test
- **Most Affected**: authentication components (67% of failures)

### Root Cause Analysis
- **Primary Issue**: Component lifecycle timing in authentication flow
- **Secondary Issue**: Test configuration missing Shadow-CLJS preprocessor
- **Pattern Identified**: Async event dispatch not properly awaited in tests

### Implemented Solutions
1. **Fixed Component Lifecycle**: Updated `auth-form-component.cljs` with proper re-frame integration
2. **Updated Test Configuration**: Added Shadow-CLJS preprocessor to karma config
3. **Added Async Test Utilities**: Implemented `rftest/run-async` for proper async testing

### Verification
- **Tests Passing**: All 45 tests now pass (100% success rate)
- **Coverage Maintained**: 87% code coverage maintained
- **No Regressions**: Previously passing tests remain stable
- **Performance**: All component render times < 16ms

### Files Modified
- `test/frontend/components/auth_test.cljs` - Fixed component lifecycle
- `karma.conf.js` - Added Shadow-CLJS integration
- `src/app/frontend/components/auth.cljs` - Fixed re-frame integration
- `test/helpers.cljs` - Added async test utilities

### Next Steps
- **Additional Tests**: Add edge case coverage for error scenarios
- **Performance Monitoring**: Continuous monitoring of test execution times
- **Cross-Browser Testing**: Expand test matrix to include Firefox and Safari
```

## Best Practices

### Test Organization
- **Unit Tests**: Test individual functions in isolation
- **Integration Tests**: Test component interactions and data flow
- **End-to-End Tests**: Test complete user workflows
- **Performance Tests**: Measure render times and memory usage
- **Regression Tests**: Ensure fixed issues stay fixed

### Test Quality
- **Descriptive Names**: Test names should clearly indicate what functionality is being tested
- **Arrange-Act-Assert**: Structure tests with clear setup, action, and verification phases
- **Isolation**: Each test should be independent and not rely on other tests' state
- **Repeatable**: Tests should produce the same results every time

### Error Handling
- **Graceful Failures**: Tests should fail with clear error messages
- **Cleanup**: Always clean up components, subscriptions, and test data
- **Timeout Handling**: Set appropriate timeouts for async operations
- **Browser Compatibility**: Test across multiple browsers and environments

### Continuous Integration
- **Watch Mode**: Use test watchers during development for immediate feedback
- **Coverage Monitoring**: Track test coverage and identify gaps
- **Automated Fixes**: Use research agent for systematic problem resolution
- **Performance Regression**: Monitor test performance over time

## Integration Points

This command works well with:
- **Research Agent**: Systematic codebase analysis and problem identification
- **ClojureScript Testing**: Component lifecycle, re-frame integration, and async operations
- **Chrome MCP**: Browser automation for UI interaction and cross-browser testing
- **Build Tools**: Shadow-CLJS compilation, Karma configuration, and build optimization
- **Development Tools**: Live testing, coverage analysis, and performance monitoring

## Example Usage Scenarios

**Basic Test Failure Analysis**:
```bash
/fe-tests    # Extract failures and start automated resolution
```

**Specific Problem Investigation**:
```bash
/fe-tests "authentication components failing after recent re-frame changes"
```

**Performance Issue Investigation**:
```bash
/fe-tests "slow rendering in data table components"
```

**Configuration Problems**:
```bash
/fe-tests "karma configuration issues preventing proper test loading"
```

## Troubleshooting

### Common Issues

**Tests Not Running**:
- Check that `bb dev:frontend` is running
- Verify `shadow-cljs.edn` configuration
- Ensure `test/` directory exists with `.cljs` files
- Check Node.js version compatibility

**Compilation Failures**:
- Verify ClojureScript syntax in test files
- Check for missing dependencies or incorrect requires
- Ensure `shadow-cljs compile test` runs successfully
- Look for circular dependencies or namespace issues

**Runtime Failures**:
- Check browser console for JavaScript errors
- Verify test environment configuration
- Check for timing issues or race conditions
- Ensure proper cleanup between tests

**Research Agent Issues**:
- Verify research agent has access to codebase
- Check if context provided is sufficient
- Ensure clear problem description in command
- Verify agent has access to necessary tools

---

**Note**: This command provides intelligent, automated test failure resolution that combines systematic analysis, research agent integration, and comprehensive testing workflows for robust frontend debugging and resolution.