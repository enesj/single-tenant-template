---
name: reframe-events-analysis
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
1) Use the `clojure-mcp` `clojurescript_eval` tool (built into Codex) to run CLJS directly against the live shadow build. Remember that app is always running and automatically refreshed, no need to try to start app.
2) If you need to switch builds, issue inside the eval: `(require '[shadow.cljs.devtools.api :as shadow]) (shadow/repl :app)` (or `:admin`).
3) Load helpers via eval:
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
- No traces: confirm dev build has `re-frame.trace.trace-enabled?` enabled (see `shadow-cljs.edn`) and reload the page.
- Can’t `require` `app.template.frontend.dev.repl-tracing`: verify it’s included in the build (see `shadow-cljs.edn` `:builds :app :dev :modules :app :preloads`) and reload.
- REPL connection issues: ensure the app is running, then use `--discover-ports` and connect to the shadow nREPL port.
- Too noisy: run `(rt/clear)`, reproduce once, then re-run queries.

## Where it lives
- `src/app/template/frontend/dev/tracing.cljs` (capture + ring buffer)
- `src/app/template/frontend/dev/repl_tracing.cljs` (REPL helpers)
- `shadow-cljs.edn` (preloads + `re-frame.trace.trace-enabled?`)
