---
description: "Trace and summarize re-frame event/subscription activity in dev builds"
tags: ["clojurescript", "re-frame", "debugging", "performance", "events"]
---

# reframe-events-analysis

Use this to answer “what events happened?”, “why did state change?”, and “what’s slow?” using the project’s dev tracing buffer.

## Use when
- UI feels slow (handlers/subscriptions/renders)
- Suspected event loops or repeated dispatches
- You need the recent event sequence around a bug

## Fast path
1) Attach a CLJS REPL (shadow). Find ports if needed:
   - `clj-nrepl-eval --discover-ports`
2) Switch into CLJS for the running build (usually the shadow port, often `8777`):
   - `clj-nrepl-eval -p 8777 "(require '[shadow.cljs.devtools.api :as shadow]) (shadow/repl :app)"` (or `:admin`)
3) Load helpers:
```clojure
(require '[app.template.frontend.dev.repl-tracing :as repl-trace])
```

## Common queries (copy/paste)
```clojure
(repl-trace/recent 40)        ; latest traces
(repl-trace/events 50)        ; event traces only
(repl-trace/subscriptions 50) ; subscription traces only
(repl-trace/slow 75 40)       ; traces >=75ms (last 40)
(repl-trace/search :login 50) ; find traces by keyword
(repl-trace/stats)            ; buffer stats
(repl-trace/clear)            ; clear buffer before reproducing
```

## Troubleshooting
- No traces: confirm dev build has `re-frame.trace.trace-enabled?` enabled (see `shadow-cljs.edn`) and reload the page.
- REPL connection issues: ensure the app is running, then use `--discover-ports` and connect to the shadow nREPL port.
- Too noisy: run `(repl-trace/clear)`, reproduce once, then re-run queries.

## Where it lives
- `src/app/template/frontend/dev/tracing.cljs` (capture + ring buffer)
- `src/app/template/frontend/dev/repl_tracing.cljs` (REPL helpers)
- `shadow-cljs.edn` (preloads + `re-frame.trace.trace-enabled?`)
