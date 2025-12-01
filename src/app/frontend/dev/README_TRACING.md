# re-frame Tracing System

A clean, Shadow-CLJS–friendly way to capture re-frame traces programmatically for debugging and analysis.

## 🚀 Quick Start

The tracing system auto-starts in development mode when your app loads. No setup required!

### Basic Usage (Browser REPL)

```clojure
;; Load the REPL helper functions
(require '[app.frontend.dev.repl-tracing :as repl-trace])

;; See what's happening in your app
(repl-trace/recent)          ; Last 20 traces
(repl-trace/events)          ; Last 20 events only
(repl-trace/stats)           ; Buffer statistics
(repl-trace/help)            ; Show all commands
```

## 📊 Available Features

### Trace Buffer Management
- **In-memory buffer** with configurable size (default: 5000 traces)
- **Automatic cleanup** - oldest traces are removed when buffer is full
- **Trace slimming** - removes large `:app-db-after` snapshots to save memory

### Trace Types Captured
- **Events** - re-frame dispatches and their execution
- **Subscriptions** - subscription creation and updates
- **Reactions** - reaction computations
- **Rendering** - component render cycles

### Filtering & Analysis
- **By operation type** - events, subscriptions, reactions, etc.
- **By performance** - find slow operations
- **By keyword** - search for specific events
- **By time range** - analyze specific time periods

## 🛠 REPL Commands

### Basic Inspection
```clojure
(repl-trace/recent)           ; Show recent traces (default: 20)
(repl-trace/recent 50)        ; Show 50 most recent traces
(repl-trace/events)           ; Show event traces only
(repl-trace/subscriptions)    ; Show subscription traces only
```

### Search & Filter
```clojure
(repl-trace/search :initialize)     ; Find :initialize events
(repl-trace/search "user")          ; Find traces containing "user"
(repl-trace/by-operation :render)   ; Show render traces only
(repl-trace/slow 100)              ; Show traces >100ms
```

### Management
```clojure
(repl-trace/stats)    ; Show buffer statistics
(repl-trace/clear)    ; Clear trace buffer
(repl-trace/help)     ; Show help
```

## 🔍 Advanced Usage

### Direct API Access
```clojure
(require '[app.frontend.dev.tracing :as tracing])

;; Get all traces
(tracing/get-history)

;; Get specific trace types
(tracing/get-events)
(tracing/get-subs)
(tracing/get-by-operation :render)

;; Buffer control
(tracing/clear-history!)
(tracing/get-stats)
```

### Custom Analysis
```clojure
;; Find slow events
(->> (tracing/get-events)
     (filter #(> (:duration %) 100))  ; >100ms
     (sort-by :duration >))

;; Event frequency analysis
(->> (tracing/get-events)
     (map #(first (get-in % [:tags :event])))
     frequencies
     (sort-by val >))
```

## 📋 Trace Structure

Each trace contains:
```clojure
{:id           "unique-trace-id"
 :operation    "operation-name"
 :op-type      :event | :sub/create | :render
 :start        1234567890         ; timestamp
 :end          1234567891         ; timestamp
 :duration     15                 ; milliseconds
 :tags         {:event [:initialize :app]  ; event vector
                :query-v [:user/id]}     ; subscription query
 ;; :app-db-after is automatically removed to save memory
}
```

## 🎯 Common Debugging Patterns

### Performance Issues
```clojure
;; Find slow operations
(repl-trace/slow 100)

;; Find events that take too long
(->> (tracing/get-events)
     (filter #(> (:duration %) 50))
     (sort-by :duration >))
```

### Event Flow Issues
```clojure
;; Trace a specific event through the system
(repl-trace/search :my-event)

;; See what happens after an event
(->> (tracing/get-history)
     (filter #(= (get-in % [:tags :event]) [:my-event]))
     first
     :start)
```

### Subscription Debugging
```clojure
;; Find subscription creation
(repl-trace/by-operation :sub/create)

;; Search for specific subscription queries
(repl-trace/search ":user/id")
```

## ⚙️ Configuration

The system is configured in `shadow-cljs.edn`:

```clojure
;; Enable tracing (already done)
:closure-defines {re-frame.trace.trace-enabled? true}

;; Auto-load tracer (already done)
:modules {:app {:preloads [app.frontend.dev.tracing]}}
```

### Buffer Size
```clojure
;; In app.frontend.dev.tracing, change:
(defonce max-items 5000)  ; Adjust buffer size as needed
```

## 🔧 Troubleshooting

### No traces appearing?
1. Ensure `re-frame.trace.trace-enabled?` is `true` in your build
2. Check that the tracer namespace is preloaded
3. Verify tracing is started: `(app.frontend.dev.tracing/start!)`

### Too much memory usage?
1. Reduce buffer size in the tracer namespace
2. Increase trace slimming to remove more data
3. Clear buffer regularly: `(app.frontend.dev.tracing/clear-history!)`

### Missing specific operations?
1. Check that your re-frame version supports tracing
2. Verify the operation types you're looking for exist
3. Use `(repl-trace/stats)` to see what's being captured

## 📚 Files Created

- `src/app/shared/dev/tracing.cljs` - Core tracing system
- `src/app/shared/dev/repl_tracing.cljs` - REPL helper functions
- `src/app/shared/dev/tracing_example.cljs` - Usage examples
- `src/app/shared/dev/README_TRACING.md` - This documentation
