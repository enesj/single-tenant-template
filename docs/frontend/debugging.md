# Frontend Debugging & Tracing

This project includes a custom in-memory trace collector that captures re-frame activity without spamming the console. It is auto-loaded in dev mode and runs silently in the background.

## Custom Tracer

The custom tracer stores re-frame events, subscriptions, and rendering metrics in a circular buffer, allowing you to query them on demand via the REPL.

### How to Use It

**Step 1: Connect to the Browser REPL**

Open your terminal and connect to the running ClojureScript REPL (e.g., via CIDER, Calva, or command line):

```bash
# Example nREPL connection (port 8777)
clojure -M:nrepl/connect localhost:8777
```

**Step 2: Access the Tracer**

The namespace is preloaded, but you'll need to require it to use the alias:

```clojure
(require '[app.template.frontend.dev.repl-tracing :as rt])

;; See help
(rt/help)
```

**Step 3: Query Traces**

All functions return ClojureScript data structures, not just printed text.

```clojure
;; Get the 20 most recent traces (all operations)
(rt/recent)

;; Get the last 50 event traces only
(rt/events 50)

;; Get recent subscription traces
(rt/subscriptions)

;; Search for specific events by keyword or string
(rt/search :initialize)
(rt/search "user")

;; Find slow operations (>100ms)
(rt/slow 100)

;; Get buffer stats
(rt/stats)
;; => {:total-count 1234, :event-count 856, ...}

;; Get captured console logs
(rt/logs)
;; => [{:op-type :log, :level :info, :message "..."} ...]

;; Clear the buffer
(rt/clear)
```

### Advanced Usage

Since the functions return data, you can process it with standard Clojure functions:

```clojure
;; Find long-running events of a specific type
(->> (rt/events)
     (filter #(= :admin/load-users (get-in % [:event 0])))
     (filter #(> (:duration-ms %) 50))
     (count))

;; Analyze frequency of events
(->> (rt/events)
     (map #(first (:event %)))
     frequencies)
```

### What Gets Traced

- **Events**: All dispatched re-frame events (`:op-type :event`).
- **Subscriptions**: Creation/disposal of subscriptions (`:op-type :sub/create`).
- **Renders**: Component re-renders (`:op-type :render`).
- **Application Logs**: Timbre logs (`log/info`, `log/warn`, etc.) redirected from console (`:op-type :log`).
- **Internal Re-frame Warnings**: Re-frame internal warnings (e.g. "Handling event") captured as logs.
- **Timing**: Duration of all operations in milliseconds.
- **Context**: Event vectors, subscription queries, and other tags.

## Reducing Console Noise

To keep the console clean while debugging, we have configured the application to completely suppress standard console output in favor of the in-memory tracer.

1.  **Cleaner Interceptors**: The `re-frame/debug` interceptor has been removed from the default interceptor chain in development, stopping redundant event logging.
2.  **Active Redirection**: On startup (`app.template.frontend.dev.tracing/start!`), the application:
    *   Unregisters default re-frame console tracers.
    *   Configures **Timbre** to disable its console appender and send logs to the in-memory tracer instead.
    *   Overwrites (shims) Re-frame's internal loggers to redirect warnings to the in-memory tracer.
3.  **Custom Tracer**: The custom tracer (`:app/dev-tracer`) remains active and collects all these signals in the background.

This approach ensures that you have full visibility into the application's runtime behavior (including logs that typically spam the console) without being overwhelmed by a flood of messages. You can inspect them at your leisure using `(rt/logs)`.

### Comparison

| Console Logs (Legacy) | Custom Tracer (Current) |
| :--- | :--- |
| Spammy, easy to miss important errors | Silent execution, query on demand |
| Hard to filter/search | Full Clojure data manipulation |
| Clutters browser console | Clean console, focused debugging |
| Lost on page refresh | Persists during session (in-memory) |
| Mixed output formats | Structured data (`:op-type`, `:data`, `:level`) |
