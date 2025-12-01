# Re-frame Events Analysis Command

**Command:** `/reframe-events-analysis "[task]"`

**Description:** Intelligent analysis of re-frame event patterns, performance, and debugging using browser development tools. Accepts concrete task descriptions for targeted investigation and customized analysis.

## When to Use

Use this command when you need to:
- Detect performance bottlenecks in event handlers
- Debug event sequences leading to issues
- Analyze subscription patterns and lifecycle
- Track user interaction flows
- Identify slow or problematic events
- Investigate re-frame application behavior
- **Execute concrete analysis tasks** with specific investigation goals

## What Happens

When you run `/reframe-events-analysis`, the system will:

1. **Navigate to your application** in the browser
2. **Connect to the ClojureScript REPL** for trace access
3. **Interpret concrete task** to customize analysis approach
4. **Execute targeted tracing** based on your specific requirements
5. **Collect event history** relevant to your investigation
6. **Analyze patterns** in timing, frequency, and sequences
7. **Provide task-specific insights** with actionable recommendations

**With concrete task description:**
- The task parameter guides the tracing and analysis approach
- Event collection focuses on patterns relevant to your problem
- Analysis is customized to address your specific investigation goals
- Recommendations are tailored to your stated requirements
- Output highlights evidence relevant to your stated problem

## Expected Output

```
## Re-frame Events Analysis

### Summary
✅ Successfully connected to re-frame tracing system
📊 Analyzed 50 events from the last 2 minutes
🎯 Focus: Performance analysis (slow events > 50ms)

### Performance Insights
🚨 Event [:properties/load-all] took 245ms (concerning)
   - Likely cause: Expensive database query + processing
   - Recommendation: Add loading state and pagination

⚠️  Event [:transactions/process] took 89ms (moderate)
   - Pattern: Repeated calls every 30 seconds
   - Recommendation: Consider debouncing or batch processing

### Event Flow Analysis
📈 User Action Sequence:
   (1) [:ui/click-load-properties] at T=0ms
   (2) [:properties/load-all] at T=15ms → 245ms duration
   (3) [:entities/update :properties] at T=265ms
   (4) [:ui/stop-loading] at T=270ms

### Subscriptions Analysis
📊 Subscription Performance:
   - [:properties/all] computed in 12ms (good)
   - [:current-user] computed in 2ms (excellent)
   - [:transactions/recent] computed in 45ms (concerning)

### Issues Detected
🔄 Rapid-fire events: [:ui/validation-check] called 8x in 500ms
   - Potential infinite loop in form validation
   - Recommendation: Add debouncing with 300ms delay

## Recommendations

1. **Optimize [:properties/load-all]**
   ```clojure
   ;; Consider pagination instead of loading all properties
   (defn load-properties-paginated [page page-size]
     {:properties/list (db/get-properties page page-size)})
   ```

2. **Add debouncing to validation**
   ```clojure
   ;; In interceptor or event handler
   (dispatch-debounce [:ui/validation-check form-data] 300)
   ```

3. **Batch transaction processing**
   ```clojure
   ;; Process transactions in batches of 50
   (defn process-transactions-batched [transactions]
     (->> transactions
          (partition-all 50)
          (map process-batch)))
   ```

### Next Steps
- Add performance monitoring to critical events
- Implement pagination for large data sets
- Consider using re-frame-fx for async operations


## Analysis Options

You can specify the type of analysis:

- **Performance**: `/reframe-events-analysis "Find slow events causing UI lag"` - Focus on slow events (>100ms)
- **Debugging**: `/reframe-events-analysis "Debug the form validation error sequence"` - Look for error patterns and sequences
- **Subscriptions**: `/reframe-events-analysis "Analyze subscription performance bottlenecks"` - Analyze subscription lifecycle
- **Recent**: `/reframe-events-analysis "Show events leading to the login failure"` - Show latest activity patterns
- **Custom Investigation**: `/reframe-events-analysis " Investigate why property deletion doesn't update UI"` - Targeted problem analysis




## Common Patterns Analyzed

### Performance Bottlenecks
- Events >100ms duration (critical)
- Events >16ms duration (affects 60fps)
- Cascading slow events
- Repeated expensive operations

### Debugging Insights
- Event sequences leading to errors
- Missing or unexpected events
- Rapid-fire duplicate events
- Circular dependencies

### Subscription Patterns
- Expensive subscription computations
- Subscription lifecycle issues
- Orphaned or leaked subscriptions
- Subscription creation frequency

## Technical Details

## Execute Tracing Commands

Use **clojure-mcp clojurescript eval** tool to:
2. Open the REPL
3. Require the tracing namespace
4. Execute appropriate tracing functions

The command typically executes tracing functions like:
```clojure
;; Recent events analysis
(trace/recent 20)

;; Performance analysis
(trace/slow 100)  ; Events > 100ms

;; Search for specific patterns
(trace/search :login)

;; Subscription operations
(trace/by-operation :sub)
```

## Integration

This command works well with:
- **App-DB Inspection**: Cross-reference events with state changes
- **Chrome DevTools**: Performance profiling and network analysis
- **Source Code Analysis**: Review event handler implementations

---

**Usage:** Type `/reframe-events-analysis` with a concrete task description: `/reframe-events-analysis "Debug why the dashboard data isn't loading"` to analyze your re-frame application's event history and patterns with targeted investigation.

**Examples:**
- `/reframe-events-analysis` - General event analysis
- `/reframe-events-analysis "Find performance bottlenecks in property loading"`
- `/reframe-events-analysis "Debug why user profile updates aren't persisting"`
- `/reframe-events-analysis "Analyze the event sequence causing form submission errors"`
