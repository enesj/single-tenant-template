---
description: "General-purpose debugging tool for testing, refactoring, and creating new features using browser and ClojureScript tools"
---

# debugg - Multi-Purpose Debugging Tool

**Comprehensive debugging assistant for testing functionality, refactoring code, and developing new features using Chrome MCP tools and ClojureScript evaluation.**

## Usage

```bash
/debugg "task description" [--option1] [--option2] [...]
```

## Description

The `/debugg` command is a versatile debugging tool that accepts concrete task descriptions and intelligently determines the appropriate debugging approach based on task content. It integrates browser automation, ClojureScript evaluation, database testing, and event tracing to provide comprehensive debugging capabilities.

**Progress Monitoring and Exit Strategy**: When command determines that it is not making progress despite repeated efforts, it will print a comprehensive summary of all actions performed so far and exit gracefully. This prevents infinite loops and provides transparency about debugging attempts made.

The command analyzes task description to determine best approach:

- **"test"**, **"verify"**, **"check"** → Feature testing and verification
- **"debug"**, **"fix"**, **"issue"**, **"problem"** → Problem diagnosis and debugging
- **"refactor"**, **"improve"**, **"optimize"** → Code refactoring and optimization
- **"create"**, **"add"**, **"implement"**, **"feature"** → New feature development
- **"performance"**, **"slow"**, **"optimize"** → Performance analysis and optimization
- **"trace"**, **"events"**, **"re-frame"** → Event tracing and analysis
- **"database"**, **"backend"**, **"api"** → Backend and database testing
- **"app-db"**, **"state"**, **"store"** → State inspection and analysis

## Essential Information

### System Access
- **Admin Login**: admin@example.com / admin123
- **Home Page**: localhost:8080
- **Database Config**:
  - Dev: localhost:5432/bookingkeeping (user/password)
  - Test: localhost:5433/bookingkeeping-test (user/password)

### Tool Integration
- **Chrome MCP**: All browser interactions (navigation, form filling, console access)
- **ClojureScript EVAL**: Frontend code testing and debugging
- **Clojure EVAL**: Backend testing and database queries
- **Events History**: Use reframe-events skill for re-frame event analysis
- **App-DB Inspection**: Use app-db-inspect skill for state analysis

## Debugging Workflows

### 1. Feature Testing

**Task Examples**: "test login functionality", "verify user registration", "check form validation"

**Workflow**:
1. Navigate to application (localhost:8080)
2. Perform login with admin credentials if needed
3. Execute feature interactions (form fills, button clicks)
4. Verify results using app-db inspection and UI state
5. Run relevant ClojureScript tests
6. Analyze any errors or unexpected behavior

**Implementation**:
```bash
# Navigate to application and perform login
chrome_mcp_navigate "http://localhost:8080/admin/login"

# Fill credentials and submit form
chrome_mcp_fill_or_select "#email" "admin@example.com"
chrome_mcp_click_element "#login-button" {:waitForNavigation true}

# Wait for page to load
sleep 3

# Verify authentication state using app-db inspection
clojure_mcp_clojurescript_eval "
(require '[re-frame.core :as rf])
(println \"Authentication state:\" @rf/app-db)
(println \"Current user:\" (:admin/current-user @rf/app-db))
(println \"Authenticated?:\" (:admin/authenticated? @rf/app-db))
"

# Run frontend tests
clojure_mcp_clojurescript_eval "
(require '[app.frontend.components.shared-utils :as utils])
(utils/test-component \".login-form\" {:dry-run false})
"
```

### 2. Problem Debugging

**Task Examples**: "debug why profile page won't load", "fix subscription errors", "solve auth token issues"

**Workflow**:
1. Navigate to problem area
2. Reproduce issue with browser interactions
3. Capture browser console output
4. Analyze re-frame event history using reframe-events skill
5. Inspect app-db state for inconsistencies
6. Test specific components with ClojureScript evaluation
7. Identify root cause and suggest solutions

**Implementation**:
```bash
# Navigate to problematic page
chrome_mcp_navigate "http://localhost:8080/admin/users"

# Capture console output for error analysis
chrome_mcp_console {:includeExceptions true}

# Try to reproduce the issue
chrome_mcp_click_element "#user-profile-link"

# Analyze re-frame events leading to the problem
skill "reframe-events-analysis" "analyze recent events for user profile loading errors"

# Inspect relevant app-db state
skill "app-db-inspect" "analyze authentication state and user data"

# Test specific components
clojure_mcp_clojurescript_eval "
(require '[app.frontend.components.user-profile :as profile])
(profile/test-render \"fake-user-id\")
"
```

### 3. Code Refactoring

**Task Examples**: "refactor subscription handling", "improve component structure", "optimize data flow"

**Workflow**:
1. Analyze current implementation using browser tools
2. Examine component code with ClojureScript evaluation
3. Identify refactoring opportunities
4. Test refactored implementation
5. Performance comparison if applicable
6. Verify maintained functionality

**Implementation**:
```bash
# Navigate to component to analyze
chrome_mcp_navigate "http://localhost:8080/admin/dashboard"

# Examine component structure
clojure_mcp_clojurescript_eval "
(require '[app.frontend.components.some-component :as component])
(println \"Component structure:\")
(clojure.pprint/pprint (meta component))
"

# Analyze subscription patterns
skill "reframe-events-analysis" "analyze subscription performance and lifecycle"

# Test refactored component implementation
clojure_mcp_clojurescript_eval "
(require '[app.frontend.components.refactored-component :as new-component])
(new-component/test \"test-data\")
"
```

### 4. Feature Development

**Task Examples**: "create tenant management", "add user roles", "implement new dashboard"

**Workflow**:
1. Analyze existing patterns in similar features
2. Examine database schema and app-db structure
3. Test implementation approaches with ClojureScript evaluation
4. Create and test new components
5. Integrate with existing authentication and routing
6. Verify end-to-end functionality

**Implementation**:
```bash
# Analyze existing database schema
clojure_mcp_clojurescript_eval "
(require '[app.shared.schemas.domain :as schemas])
(println \"Available schemas:\" (keys schemas))
(println \"User schema:\" (:users schemas))
"

# Test new data structure
clojure_mcp_clojurescript_eval "
(require '[app.template.frontend.db.tenants :as tenants])
(tenants/create \"test-tenant\" {:name \"Test Tenant\" :plan \"basic\"})
"
```

### 5. Performance Analysis

**Task Examples**: "analyze dashboard performance", "optimize slow loading", "improve response times"

**Workflow**:
1. Measure current performance metrics
2. Use event tracing to identify bottlenecks
3. Analyze database query performance
4. Profile ClojureScript execution
5. Test optimization approaches
6. Verify performance improvements

**Implementation**:
```bash
# Navigate to performance-critical page
chrome_mcp_navigate "http://localhost:8080/admin/dashboard"

# Measure page load time
chrome_mcp_inject_script :type "MAIN" "
performance.mark('page-load-start');
setTimeout(() => {
  performance.mark('page-load-end');
  const loadTime = performance.measure('page-load', 'page-load-start', 'page-load-end');
  console.log('Page load time:', loadTime + 'ms');
}, 100);
"

# Trace performance-critical events
skill "reframe-events-analysis" "analyze performance bottlenecks in dashboard loading"

# Profile database queries
clojure_mcp_clojurescript_eval "
(require '[app.backend.services.performance :as perf])
(perf/profile-query \"expensive-dashboard-query\")
"
```

### 6. Backend/Database Testing

**Task Examples**: "test database connection", "verify API endpoints", "check query performance"

**Workflow**:
1. Test database connectivity and authentication
2. Execute API endpoint tests
3. Verify data consistency between frontend and backend
4. Check query performance and optimization
5. Test error handling and edge cases

**Implementation**:
```bash
# Test database connectivity
clojure_mcp_clojure_eval "
(require '[app.db.connection :as db])
(db/test-connection)
"

# Verify API endpoints
clojure_mcp_clojure_eval "
(require '[app.backend.handlers.api-test :as api-test])
(api-test/verify-endpoints)
"

# Check query performance
clojure_mcp_clojure_eval "
(require '[app.backend.services.query-analyzer :as query-analyzer])
(query-analyzer/profile-slow-queries)
"
```

### 7. Event History Analysis

**Task Examples**: "trace events leading to error", "check user interaction flow", "analyze subscription cascades"

**Workflow**:
1. Use reframe-events-analysis skill for comprehensive tracing
2. Focus on specific event patterns or time ranges
3. Correlate events with app-db state changes
4. Identify performance issues or infinite loops
5. Analyze subscription lifecycle problems

**Implementation**:
```bash
# Analyze recent events for specific patterns
skill "reframe-events-analysis" "find events causing authentication failures"

# Check subscription performance
skill "reframe-events-analysis" "analyze subscription performance and lifecycle issues"

# Trace user interaction sequences
skill "reframe-events-analysis" "analyze user interaction flows and timing"
```

### 8. State Inspection

**Task Examples**: "inspect app-db state for entity data consistency", "verify authentication tokens", "check form validation state"

**Workflow**:
1. Use app-db-inspect skill for comprehensive state analysis
2. Focus on specific data paths or validation states
3. Cross-reference with UI state through browser tools
4. Verify data consistency and relationships
5. Check for unexpected state mutations

**Implementation**:
```bash
# Comprehensive app-db inspection
skill "app-db-inspect" "analyze authentication state and user data"

# Targeted data path analysis
clojure_mcp_clojurescript_eval "
(require '[re-frame.core :as rf])
(def user-data (get-in @rf/app-db [:admin/current-user :users/entities]))
(println \"User data:\" user-data)
"

# Verify form validation state
clojure_mcp_clojurescript_eval "
(require '[re-frame.core :as rf])
(def form-errors (:admin/form-errors @rf/app-db))
(when form-errors
  (println \"Form validation errors:\" form-errors))
"
```

## Tool-Specific Capabilities

### Chrome MCP Tools
```javascript
// Navigation and page interaction
chrome_mcp_navigate("http://localhost:8080/admin/login")
chrome_mcp_click_element("#submit-button", {waitForNavigation: true})
chrome_mcp_fill_or_select("#email", "test@example.com")

// Console and debugging
chrome_mcp_console({includeExceptions: true, maxMessages: 50})
chrome_mcp_get_web_content({selector: ".error-message"})

// Form interaction and data extraction
chrome_mcp_get_interactive_elements({includeCoordinates: true})
chrome_mcp_fill_or_select("#user-input", "test value")

// Script injection for custom testing
chrome_mcp_inject_script(:type "MAIN", "console.log('Custom test script loaded');")
```

### ClojureScript Evaluation
```clojure
;; Frontend testing
(re-frame.core/subscribe [:admin/users])
(re-frame.core/dispatch [:admin/load-users])

;; Component testing
(require '[app.frontend.components.test-component :as comp])
(comp/test-data-validation {:test-data "sample"})

;; Database testing
(require '[app.backend.services.database :as db])
(db/test-connection)
(db/query-with-logging "SELECT * FROM users LIMIT 5")
```

### Clojure Evaluation
```clojure
;; Backend testing and database queries
(require '[app.backend.handlers.admin :as admin])
(admin/get-user-stats)
(admin/verify-tenant-isolation "test-tenant")

;; Database schema inspection
(require '[app.db.models :as models])
(clojure.pprint/pprint (:users models))
```

## Common Debugging Patterns

### Authentication Testing
```bash
# Test login flow completely
chrome_mcp_navigate "http://localhost:8080/admin/login"
chrome_mcp_fill_or_select "#email" "admin@example.com"
chrome_mcp_fill_or_select "#password" "admin123"
chrome_mcp_click_element "#login-button" {waitForNavigation: true}

# Verify session establishment
clojure_mcp_clojurescript_eval "
(require '[re-frame.core :as rf])
(sleep 2000) ; Wait for redirect
(println \"Auth state after login:\" @rf/app-db)
"
```

### Performance Debugging
```bash
# Measure page load performance
chrome_mcp_inject_script :type "MAIN" "
const startTime = performance.now();
window.addEventListener('load', () => {
  const loadTime = performance.now() - startTime;
  console.log('Page load time:', loadTime + 'ms');
});
"

# Trace slow re-frame events
skill "reframe-events-analysis" "analyze performance bottlenecks"

# Profile database queries
clojure_mcp_clojure_eval "
(require '[app.backend.services.performance :as perf])
(perf/profile-slow-queries {:min-threshold-ms 100})
"
```

### Error Pattern Analysis
```bash
# Capture and analyze console errors
chrome_mcp_console {includeExceptions: true, filter: "ERROR"}

# Test error recovery scenarios
chrome_mcp_navigate "http://localhost:8080/admin/login"
chrome_mcp_fill_or_select "#email" "invalid-email"
chrome_mcp_click_element "#login-button"

# Analyze error handling
skill "reframe-events-analysis" "find error handling patterns in authentication flow"
```

## Error Handling and Recovery

### Browser Automation Failures
```bash
# Retry mechanism with exponential backoff
chrome_mcp_navigate "http://localhost:8080/admin/login"

for attempt in {1..3}; do
  if chrome_mcp_fill_or_select "#email" "test@example.com"; then
    chrome_mcp_fill_or_select "#password" "password123"
    if chrome_mcp_click_element "#login-button" {waitForNavigation: true, timeout: 10000}; then
      echo "Login successful on attempt $attempt"
      break
    else
      echo "Login failed on attempt $attempt, retrying..."
  fi
  sleep 2
done
```

### ClojureScript Evaluation Errors
```clojure
;; Safe evaluation with error handling
(try
  (require '[app.frontend.some-component :as comp])
  (comp/test-function "test-data")
  (catch js/Error e
    (println "ClojureScript error:" (.-message e))
    (println "Falling back to manual testing...")))
```

### Missing Dependencies or Tools
```bash
# Check if Chrome MCP is available
if ! chrome_mcp_navigate "http://localhost:8080" 2>/dev/null; then
  echo "❌ Chrome MCP not available. Using fallback browser testing..."
  # Fallback testing approach
  open "http://localhost:8080"
fi

# Check if ClojureScript evaluation is working
clojure_mcp_clojurescript_eval "(println 'ClojureScript evaluation working')"

# Check if Clojure evaluation is available
clojure_mcp_clojure_eval "(println 'Clojure evaluation working')"
```

## Progress Monitoring and Exit Strategy

### Progress Detection
- **Task Completion Tracking**: Each sub-task is marked as completed upon successful execution
- **Effort Counter**: Tracks number of attempts made for each debugging approach
- **Stagnation Detection**: Identifies when debugging approaches yield no new information or progress

### Exit Conditions
The command will exit and provide a summary when:
- **No Progress After Multiple Attempts**: When the same debugging approach has been tried 3+ times without progress
- **Exhausted Available Tools**: When all relevant debugging tools have been utilized without resolution
- **Repetitive Error Patterns**: When the same errors occur repeatedly despite different approaches
- **Missing Dependencies**: When required system components are unavailable and cannot be resolved

### Summary Output
```
=== DEBUGGING SESSION SUMMARY ===
Original Task: [task description]
Duration: [time elapsed]
Actions Performed:
✓ [action 1] - [result]
✓ [action 2] - [result]
✗ [failed action] - [error/reason]

Findings:
- [key observation 1]
- [key observation 2]
- [pattern identified]

Next Steps Suggested:
- [recommendation 1]
- [alternative approach]
- [further investigation needed]

Session Status: COMPLETE - No further progress possible with available tools
===================================
```

## Best Practices for Debugging

1. **Specific Task Descriptions**: Provide clear, concrete descriptions of what needs to be debugged
2. **Context Information**: Include relevant details about the issue or feature being debugged
3. **Step-by-Step Approach**: Allow the tool to work through problems systematically
4. **Verification Requirements**: Specify what constitutes successful completion
5. **Performance Considerations**: Include performance metrics when relevant
6. **Pattern Recognition**: Identify when debugging is cycling through the same issues despite different approaches

## Integration with Other Skills

### Complementary Debugging
- **Events History**: Use `reframe-events-analysis` skill for detailed re-frame event tracing
- **State Inspection**: Use `app-db-inspect` skill for comprehensive app-db analysis
- **System Logs**: Use `system-logs` skill for monitoring compilation and runtime issues

### Enhanced Analysis Workflow
```bash
# Start with feature testing
/debugg "test user registration flow"

# If issues found, switch to event tracing
skill "reframe-events-analysis" "analyze authentication and registration events"

# For state-related issues, use app-db inspection
skill "app-db-inspect" "analyze user state and authentication data"

# For database issues, use backend testing
clojure_mcp_clojure_eval "(require '[app.db.test :as db-test]) (db-test/run-all)"
```

## Examples

### Testing User Registration
```bash
/debugg "test complete user registration flow from form submission to email verification"

# Expected workflow:
# 1. Navigate to registration form
# 2. Fill form fields with valid test data
# 3. Submit form and capture response
# 4. Verify email is received (if applicable)
# 5. Check user appears in database
# 6. Test authentication with new credentials
```

### Debugging Data Loading Issues
```bash
/debugg "debug why tenant list is empty in admin panel despite database records"

# Expected workflow:
# 1. Navigate to admin panel
# 2. Check browser console for JavaScript errors
# 3. Analyze re-frame events for data loading
# 4. Inspect app-db state for entity data
# 5. Verify API responses and database connectivity
# 6. Test subscription handling and updates
```

### Optimizing Dashboard Performance
```bash
/debugg "analyze why financial reports page takes 10+ seconds to load and optimize it"

# Expected workflow:
# 1. Measure current performance metrics
# 2. Trace performance-critical events
# 3. Identify database query bottlenecks
# 4. Profile ClojureScript execution
# 5. Test optimization approaches
# 6. Verify performance improvements
```

### Creating New Features
```bash
/debugg "create a new tenant invitation system with email sending and acceptance flow"

# Expected workflow:
# 1. Analyze existing patterns in similar features
# 2. Examine database schema and app-db structure
# 3. Test implementation approaches with ClojureScript evaluation
# 4. Create and test new components
# 5. Integrate with existing authentication and routing
# 6. Verify end-to-end functionality
```

---

**Note**: This debugging tool provides comprehensive, intelligent debugging assistance that adapts to your specific development needs and workflows.