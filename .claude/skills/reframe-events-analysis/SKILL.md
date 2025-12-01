---
description: "Intelligent re-frame event history analysis using browser tracing tools"
tags: ["events history","clojurescript", "re-frame", "debugging", "performance", "browser"]
---

# reframe-events-analysis

**Intelligent analysis of re-frame event patterns, performance, and debugging using browser development tools and custom event interceptors.**

## Skill Purpose

This skill helps analyze re-frame application event history to:
- Debug event sequences leading to issues
- Analyze subscription patterns and lifecycle
- Track user interaction flows
- Identify slow or problematic events
- Monitor re-frame app-db state changes
- Trace event-handler performance bottlenecks

## When to Use

Use this skill when:
- User asks about re-frame event history or patterns
- Debugging re-frame application issues
- Performance optimization of event handlers
- Analyzing event flow and sequences
- Investigating subscription behavior
- User mentions keywords: "events", "event history", "re-frame tracing", "debug events"
- Need to understand why app-db state changed
- Looking for performance bottlenecks in UI interactions

## Analysis Workflow

### 1. Understand User Intent

Analyze the user's request for keywords:
- **Performance**: "slow", "performance", "speed", "bottleneck", "laggy" → Focus on duration analysis
- **Debugging**: "debug", "error", "failure", "issue", "broken" → Look for problematic patterns
- **Subscriptions**: "subscription", "sub", "query", "reaction" → Analyze subscription lifecycle
- **Flow**: "flow", "sequence", "pattern", "chain", "what happened" → Track event sequences
- **Recent**: "recent", "latest", "last", "just" → Focus on most recent activity
- **State**: "state", "app-db", "why did", "how did" → Focus on state changes

### 2. Check Available Tracing Systems

This project uses re-frame with custom event interceptors. Look for:
- Custom tracing namespace (likely `app.admin.frontend.events` or similar)
- Built-in re-frame tracing (`re-frame.trace`)
- Browser DevTools Performance tab
- Manual console.log instrumentation

**Common tracing approaches:**
1. **Custom Event Interceptor**: Add to project for comprehensive tracing
2. **re-frame.trace**: Built-in tracing functionality
3. **Browser DevTools**: Performance timeline and console
4. **Manual Instrumentation**: Strategic console.log in event handlers

### 3. Execute Tracing Commands

Use **clojure-mcp clojurescript-eval** tool with comprehensive error handling:

```clojure
;; Try to require built-in re-frame tracing first
(try
  (require '[re-frame.trace :as trace])
  (println "✓ Using re-frame.trace")
  (catch js/Error e
    (println "⚠ re-frame.trace not available:" (.-message e))
    false))

;; If no built-in tracing, use manual event inspection
(try
  ;; Get current re-frame events system info
  (require '[re-frame.core :as rf])

  ;; Get app-db current state for context
  (def current-app-db @re-frame.db/app-db)
  (println "✓ Current app-db keys:" (keys current-app-db))

  ;; Check for existing event handlers
  (def event-handlers rf.handlers/app-id)
  (println "✓ Event handlers available:" (some? event-handlers))

  (catch js/Error e
    (println "❌ Error accessing re-frame:" (.-message e))))
```

**Manual Event Tracing Examples:**
```clojure
;; Create a simple event tracer if none exists
(defn trace-event-dispatch [event-vect]
  (let [start (js/performance.now)]
    (println "🔄 EVENT:" event-vect "at" start)
    ;; Measure execution time
    (fn [db]
      (let [result (rf/dispatch event-vect)
            duration (- (js/performance.now) start)]
        (when (> duration 16) ; Log slow events
          (println "⚠ SLOW EVENT:" event-vect "took" duration "ms"))
        result))))

;; Trace specific event patterns
(defn trace-recent-events [event-pattern]
  (println "🔍 Looking for events matching:" event-pattern)
  ;; This would be implemented with custom interceptors
  )
```

### 4. Analyze Results

Based on traced event data structure (typically):
```clojure
{:event [:event/name arg1 arg2]     ; Event vector
 :op-type :event                      ; Operation type (:event, :sub, :fx, etc.)
 :start 153421.5                     ; Start timestamp (ms from performance.now())
 :duration-ms 0.5                    ; Duration in milliseconds
 :app-db-before {...}                 ; App-db state before event
 :app-db-after {...}                  ; App-db state after event
 :trace-id "uuid"}                    ; Unique trace identifier
```

Provide analysis for:

**Performance Issues:**
- Events with duration > 100ms are concerning
- Events > 16ms might cause frame drops (60fps target)
- Identify patterns of repeatedly slow events
- Check for cascading slow events

**Debugging Insights:**
- Find events preceding errors or failures
- Identify unusual event sequences
- Spot missing or unexpected events
- Check for rapid-fire duplicate events (potential infinite loops)

**Subscription Patterns:**
- Check subscription creation frequency
- Identify expensive subscription computations
- Track subscription lifecycle issues
- Find orphaned or leaked subscriptions

**Event Flow:**
- Map causal relationships between events
- Identify event chains and dependencies
- Track user interaction sequences
- Spot circular dependencies or loops

### 5. Provide Actionable Recommendations

Give specific, actionable advice:
- **Performance**: Suggest optimization strategies (memoization, debouncing, batch processing)
- **Debugging**: Point to likely problem areas and next debugging steps
- **Architecture**: Recommend event handler refactoring if needed
- **Best Practices**: Suggest re-frame patterns to avoid identified issues

## Common Analysis Patterns

### Performance Bottleneck Detection
```clojure
;; Find events taking > 100ms (using custom interceptor)
(defn find-slow-events [threshold-ms]
  (filter #(> (:duration-ms %) threshold-ms) traced-events))

;; Analyze duration distribution
(defn analyze-event-durations [events]
  (->> events
       (map :duration-ms)
       (group-by #(cond (> % 100) :critical
                        (> % 50) :warning
                        (> % 16) :notice
                        :else :ok))))

;; Get slowest events
(defn get-slowest-events [n]
  (take-last n (sort-by :duration-ms traced-events)))
```

### Event Flow Analysis
```clojure
;; Track events in chronological order
(defn chronological-events [events]
  (->> events
       (sort-by :start)
       (map #(select-keys % [:event :duration-ms :trace-id]))))

;; Find event chains (events within 100ms of each other)
(defn find-event-chains [events window-ms]
  (->> events
       (sort-by :start)
       (partition-by #(quot (:start %) window-ms))
       (filter #(> (count %) 2)) ; At least 2 events in window
       (map #(sort-by :start %))))

;; Detect rapid-fire events (potential infinite loops)
(defn detect-rapid-events [events min-interval-ms]
  (->> events
       (sort-by :start)
       (partition 2 1)
       (filter (fn [[a b]]
                  (< (- (:start b) (:start a)) min-interval-ms)))))
```

### Subscription Debugging
```clojure
;; All subscription operations (if trace data includes)
(defn get-subscription-events [events]
  (filter #(= (:op-type %) :sub) events))

;; Find expensive subscriptions
(defn find-expensive-subs [events threshold-ms]
  (->> (get-subscription-events events)
       (filter #(> (:duration-ms %) threshold-ms))
       (sort-by :duration-ms >)))

;; Analyze subscription frequency
(defn subscription-frequency [events]
  (->> (get-subscription-events events)
       (map :event)
       frequencies
       (sort-by val >)))
```

### App-db State Analysis
```clojure
;; Analyze state changes for specific events
(defn analyze-state-change [event-name events]
  (->> events
       (filter #(= (first (:event %)) event-name))
       (map #(hash-map :trace-id (:trace-id %)
                     :before (:app-db-before %)
                     :after (:app-db-after %)
                     :changes (keys (clojure.data/diff
                                      (:app-db-before %)
                                      (:app-db-after %)))))))

;; Find events that modified specific keys
(defn find-key-modifiers [key-path events]
  (filter #(contains? (get-in (:app-db-after %) key-path)
                       (get-in (:app-db-before %) key-path))
          events))
```

### Custom Analysis Functions

If built-in functions aren't sufficient, create custom analysis on the fly:

```clojure
;; Find events leading to a specific failure condition
(defn events-before-failure [failure-keyword events]
  (->> events
       (sort-by :start)
       (take-while #(not (some #{failure-keyword} (:event %))))
       (take-last 10)))

;; Analyze event frequency over time
(defn event-frequency-over-time [events]
  (->> events
       (group-by #(quot (:start %) 1000)) ; Group by second
       (map (fn [[second-group events-in-second]]
               [second-group (count events-in-second)]))
       (sort-by first)))

;; Find event cascades (bursts of activity)
(defn find-event-cascades [events threshold-ms min-events]
  (->> events
       (sort-by :start)
       (partition-by #(quot (:start %) threshold-ms))
       (filter #(> (count %) min-events))))

;; Detect potential memory leaks in subscriptions
(defn detect-subscription-leaks [events time-window-ms]
  (let [sub-events (get-subscription-events events)
        creation-events (filter #(= (second (:event %)) :create) sub-events)
        cleanup-events (filter #(= (second (:event %)) :cleanup) sub-events)]
    (when (> (count creation-events) (count cleanup-events))
      {:potential-leak true
       :created-but-not-cleaned (- (count creation-events)
                                   (count cleanup-events))
       :orphaned-subs (remove #(contains? (set cleanup-events) %)
                              creation-events)})))
```

## Fallback Strategies

If custom tracing isn't available:

### 1. Use Project-Specific REPL Tracing (Preferred)
This project has a dedicated wrapper for re-frame tracing that handles build configuration differences.

```clojure
;; Require the project-specific tracing namespace
(require '[app.frontend.dev.repl-tracing :as repl-trace])

;; Available commands:
(repl-trace/recent)           ; Return recent traces (default: 20)
(repl-trace/events)           ; Return event traces only
(repl-trace/subscriptions)    ; Return subscription traces only
(repl-trace/search :login)    ; Search traces by keyword
(repl-trace/stats)            ; Return buffer statistics
(repl-trace/help)             ; Show all available commands
```

### 2. Browser DevTools Performance
```clojure
;; Use Chrome DevTools Performance API
(defn mark-event-start [event-name]
  (.mark js/performance (str "event-start-" event-name)))

(defn mark-event-end [event-name]
  (.mark js/performance (str "event-end-" event-name)))

(defn measure-event [event-name]
  (.measure js/performance
           (str event-name "-duration")
           (str "event-start-" event-name)
           (str "event-end-" event-name)))
```

### 3. Manual Instrumentation
```clojure
;; Add to event handlers directly
(rf/reg-event-fx
  :some/event
  (fn [cofx _]
    (let [start (js/performance.now)]
      (println "🔄 START: :some/event" start)
      ;; ... event logic ...
      (let [duration (- (js/performance.now) start)]
        (when (> duration 50)
          (println "⚠ SLOW: :some/event took" duration "ms"))
        {:dispatch [:some/next-event]})))))
```

## Output Format

Provide analysis in clear, structured sections:

```markdown
## Event History Analysis

### Summary
[High-level overview of findings: number of events analyzed, time range, key insights]

### Performance Insights
- Event X took Yms (concerning because [reason])
- Pattern of slow events: [describe pattern]
- Recommendation: [specific optimization with code example]

### Event Flow
- User action triggered: [event sequence with timestamps]
- Notable patterns: [describe interesting sequences]
- Timing relationships: [describe causal chains]

### State Changes
- Events that modified [key]: [list events with state diffs]
- Unexpected state mutations: [describe suspicious changes]
- Subscription impacts: [describe which subs were triggered]

### Potential Issues
- [Specific issue with evidence from traces]
- [Another issue with supporting data]

### Recommendations
1. [Actionable recommendation with code example if applicable]
2. [Another recommendation with rationale]

### Next Steps
- [Suggested debugging steps to dig deeper]
- [Further analysis to perform]
- [Code to add for better tracing]
```

## Integration Points

This skill works well with:
- **app-db inspection skills**: Cross-reference events with state changes
- **Chrome DevTools**: Performance profiling and network analysis
- **Source code analysis**: Review event handler implementations for optimization
- **Login events analysis**: Track authentication flows and security events
- **Admin panel debugging**: Analyze admin interface event flows

## Best Practices

1. **Start Broad**: Begin with recent events overview, then narrow focus based on findings
2. **Be Specific**: When possible, ask for time thresholds, event names, or operation types
3. **Iterative Analysis**: Use initial findings to guide deeper investigation
4. **Cross-Reference**: Compare events with app-db state and UI behavior
5. **Document Patterns**: Record recurring issues for future reference and team knowledge
6. **Production Safety**: Use tracing levels to avoid performance impact in production

## Project-Specific Patterns

**Admin Panel Event Analysis:**
```clojure
;; Common admin events to monitor
(def admin-events-to-watch
  [:admin/login :admin/logout :admin/check-auth
   :admin/load-dashboard :admin/load-users
   :admin/users/delete :admin/users/create])

;; Trace authentication flows
(defn trace-auth-flow [events]
  (->> events
       (filter #(contains? admin-events-to-watch (first (:event %))))
       (sort-by :start)
       (map #(select-keys % [:event :duration-ms :trace-id :app-db-after]))))
```

**Frontend Performance Monitoring:**
```clojure
;; Monitor UI responsiveness
(defn check-ui-responsiveness [events]
  (let [ui-events (filter #(= (:op-type %) :render) events)]
    (when (> (count ui-events) 0)
      {:avg-render-time (->> ui-events
                            (map :duration-ms)
                            (apply +)
                            (/ (count ui-events)))
       :slow-renders (filter #(> (:duration-ms %) 16) ui-events)
       :frame-drop-risk (> (->> ui-events
                             (map :duration-ms)
                             (apply +))
                          (* (count ui-events) 16))})))
```

## Example Interactions

**User**: "Check for slow events in the admin panel"
**Action**: Execute performance analysis, find events > 50ms, focus on admin-specific events like `:admin/load-dashboard`

**User**: "Why did the login fail?"
**Action**: Search for login events `(trace/search :login)`, trace sequence, identify failure point in auth flow

**User**: "Analyze why the users table is slow to load"
**Action**: Get subscription operations, find expensive ones, suggest pagination or caching strategies

**User**: "Show recent event patterns"
**Action**: Get recent events, identify sequences and timing patterns, look for unusual cascades

**User**: "Are there any event loops or infinite cascades?"
**Action**: Check for rapid-fire events, analyze timing gaps, identify potential infinite loops

**User**: "What events modified the users data?"
**Action**: Find events with app-db changes affecting users key, analyze state transitions

## Tips for Effective Analysis

1. **Size Limits**: Most tracing functions have max limits (typically 100). Don't ask for more than needed
2. **Time Windows**: Consider the time window of analysis - recent activity vs. historical patterns
3. **Context Matters**: Always consider what the user was doing when issues occurred
4. **Multiple Angles**: Combine different analysis approaches (timing + frequency + state changes)
5. **Production Impact**: Use trace levels carefully to avoid performance degradation
6. **Error Recovery**: Always wrap tracing calls in try/catch to avoid breaking the app

---

**Note**: This skill adapts to the project's specific tracing implementation. Always:
1. Check the actual tracing namespace and available functions first
2. Adjust commands to match the project's API
3. Verify the data structure returned matches expectations
4. Fall back to alternative approaches if primary method unavailable
5. Use proper error handling to prevent tracing code from breaking the application