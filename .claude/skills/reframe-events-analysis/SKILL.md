---
description: "Trace and summarize re-frame event/subscription activity in dev builds"
metadata:
  tags: ["clojurescript", "re-frame", "debugging", "performance", "events"]
---

# reframe-events-analysis

Use this to answer “what events happened?”, “why did state change?”, and “what’s slow?” using the project’s dev tracing buffer.

## Use when
- UI feels slow (handlers/subscriptions/renders)
- Suspected event loops or repeated dispatches
- You need the recent event sequence around a bug

## Fast path
1) Use `clj-nrepl-eval` against the shadow-cljs nREPL to evaluate ClojureScript code.
2) Load helpers:
```clojure
(require '[app.template.frontend.dev.repl-tracing :as rt])
```
Note: the helper namespace must be included in the active shadow build. In this repo it’s wired into the `:app` dev build via `shadow-cljs.edn` → `:builds :app :dev :modules :app :preloads`.

## Common queries (copy/paste)
```clojure
(rt/recent 40)        ; latest traces
(rt/events 50)        ; event traces only
(rt/subscriptions 50) ; subscription traces only
(rt/slow 75 40)       ; traces >=75ms (last 40)
(rt/search :login 50) ; find traces by keyword
(rt/logs 50)          ; captured console logs
(rt/stats)            ; buffer stats
(rt/clear)            ; clear buffer before reproducing
```

## Troubleshooting
- **FileNotFoundException**: If you get this when evaluating, the REPL is in Clojure (JVM) mode. Switch to ClojureScript by evaluating:
  ```clojure
  (shadow.cljs.devtools.api/nrepl-select :app)
  ```
  (Use `:admin` if working on the admin panel).
- **No traces**: confirm dev build has `re-frame.trace.trace-enabled?` enabled (see `shadow-cljs.edn`) and reload the page.
- **Can’t `require` `app.template.frontend.dev.repl-tracing`**: verify it’s included in the build (see `shadow-cljs.edn` `:builds :app :dev :modules :app :preloads`) and reload.
- **Too noisy**: run `(rt/clear)`, reproduce once, then re-run queries.

## Where it lives
- `src/app/template/frontend/dev/tracing.cljs` (capture + ring buffer)
- `src/app/template/frontend/dev/repl_tracing.cljs` (REPL helpers)
- `shadow-cljs.edn` (preloads + `re-frame.trace.trace-enabled?`)
