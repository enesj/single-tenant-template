---
description: "Intelligent re-frame event history analysis using browser tracing tools"
tags: ["events history","clojurescript", "re-frame", "debugging", "performance", "browser"]
---

# reframe-events-analysis

Practical tracing of re-frame events using the built-in dev tracer and REPL helpers.

## Fast Path
- Stack is already running (auto-reloads). Find the nREPL port (usually 8777) with `clj-nrepl-eval --discover-ports` if unsure.
- Attach CLJS REPL via nREPL:
  - `clj-nrepl-eval -p 8777 "(require '[shadow.cljs.devtools.api :as shadow]) (shadow/repl :app)"`
  - Add `--with-sudo` if macOS blocks `ps` (seen in tests).
- Run trace helpers in the same session:
  - `(require '[app.frontend.dev.repl-tracing :as repl-trace])`
  - `repl-trace/recent`, `repl-trace/events`, `repl-trace/subscriptions`, `repl-trace/slow 100`, `repl-trace/search :initialize`, `repl-trace/stats`, `repl-trace/clear`
- Exit REPL: `:cljs/quit` (watcher keeps running).

## What This Captures
- Auto-starting trace callback (preloaded) that records re-frame traces into an in-memory ring buffer (default 5000 entries) and slims large `:app-db` data.
- Traces include `:op-type` (event, sub/create, render, etc.), `:tags` (event vector, subscription query), `:duration`, and timestamps.

## When to Use
- User asks “what events fired?” / “why did state change?” / “why is UI slow?”
- Need to spot slow handlers (>16ms noticeable, >50–100ms concerning).
- Debug duplicate/looping dispatches or subscription churn.

## Common Queries (copy/paste)
```clojure
(repl-trace/recent 40)                 ; latest activity
(repl-trace/slow 75 40)               ; slow traces (>=75ms), last 40
(repl-trace/by-operation :render 30)  ; renders
(repl-trace/search "user")           ; traces whose event vector includes "user"
(->> (repl-trace/events)              ; top frequent events
     (map :event)
     frequencies
     (sort-by val >)
     (take 10))
```

## Troubleshooting
- No traces? ensure dev build uses `:closure-defines {re-frame.trace.trace-enabled? true}` (already set) and preloads include `app.frontend.dev.tracing` (already in `shadow-cljs.edn`).
- Buffer too big? adjust `max-items` in `src/app/frontend/dev/tracing.cljs` or run `repl-trace/clear`.
- CLJS REPL errors/“connection refused”? make sure `bb run-app` is running; then attach with `clj-nrepl-eval -p 8777 "(require '[shadow.cljs.devtools.api :as shadow]) (shadow/repl :app)"` (add `--with-sudo` if ps is blocked). If shadow reports “already running on 9630”, stop it with `(require '[shadow.cljs.devtools.server :as server]) (server/stop!)` and retry.

## File Map
- `src/app/frontend/dev/tracing.cljs` — trace capture and buffer
- `src/app/frontend/dev/repl_tracing.cljs` — REPL helpers (`repl-trace/*`)
- `src/app/frontend/dev/README_TRACING.md` — fuller guide

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
