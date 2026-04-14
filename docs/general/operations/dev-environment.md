<!-- ai: {:tags [:operations :dev :runbook :single-tenant] :kind :runbook} -->

# Live-Reload Development Environment (Single-Tenant)

Quick reference for the hot-reload stack (backend + Shadow CLJS + watchers) on port **8085**.

## Entry Points

- `bb run-app` *(recommended)*: wraps `scripts/sh/development/run-app.sh`, checks port **8085**, and runs `clojure -M:dev` under `monitor_terminal.sh` (auto-restart + logging).
- `scripts/sh/development/run-app.sh`: same as above without Babashka.
- `clj -M:nrepl` (default 7888): attach to the dev runtime; `(user/start)` boots the system manually if you prefer REPL-driven startup.

## What Starts

- Backend (Ring/Reitit) + file watchers (backend sources, `resources/db/models.edn`).
- Shadow CLJS `:app` watch + browser REPL selection.
- nREPL on 7888.
- Structured dev logging via Timbre (watcher/system lifecycle events).

## Live Reload Flow

1) `bb run-app` → script checks 8085 → launches dev profile under monitor.  
2) Backend watcher restarts on `.clj/.cljc/.edn` changes in `src/app`, `dev`, `config`, `vendor`, **except** UI config bootstrap-default EDNs:
   - `src/app/admin/frontend/config/*.edn`
   - `src/app/domain/frontend/**/config/*.edn`
   These serve as bootstrap defaults (seeded into the DB at startup). Runtime config is managed via the DB through `/admin/admin-settings` and `/admin/user-settings`.  
3) Models watcher restarts on `resources/db/models.edn` changes.  
4) Shadow `watch :app` pushes frontend updates automatically.  
5) nREPL stays available for editor/Portal connections.

## Typical Workflow

```bash
bb run-app
```

- Connect to `localhost:7888` if you need inline eval.
- Edit backend/edn → backend auto-restarts.
- Edit CLJS → Shadow hot-reloads in browser.
- After schema changes or UI config edits:
  - `bb validate-frontend-config`
  - `bb sync-frontend-config` (dry-run) / `bb sync-frontend-config --apply`
  - or `bb migrate-and-sync-frontend-config` (migrate + apply + validate)
- Manual restart: `(system.core/restart-system)` from the REPL.

## Optional Workers

- Receipt OCR processing no longer ships with a dedicated shell worker script in this template. Use the admin or user OCR endpoints instead:
  - Admin: `POST /admin/api/expenses/receipts/:id/ocr`
  - User:  `POST /api/v1/expenses/receipts/:id/ocr`
  - Select provider via `RECEIPT_OCR_WORKFLOW=mistral|llamaparse` (default `mistral`).
  - Set `MISTRAL_API_KEY` for `mistral` or `LLAMA_CLOUD_API_KEY` for `llamaparse`.

## Troubleshooting

- **FileNotFoundException in REPL**: If you get this when requiring `.cljs` files, the REPL is in Clojure (JVM) mode. Switch to ClojureScript by evaluating:

  ```clojure
  (shadow.cljs.devtools.api/nrepl-select :app)
  ```

  (Use `:admin` if working on the admin panel).
- **Re-frame trace queries in CLJS REPL**: `reframe-events-analysis` uses `app.template.frontend.dev.repl-tracing` (helpers on top of the dev trace buffer). It’s included in the `:app` dev build via `shadow-cljs.edn` under `:builds :app :dev :modules :app :preloads`; after changing preloads, reload `http://localhost:8085`.
- Port in use: script prints owner; free 8085 or adjust `run-app.sh`.
- Duplicate `clojure -M:dev`: if a `shadow-cljs` instance for this project or the dev nREPL on `7888` is already running, startup logs a reuse warning and skips starting duplicate frontend/nREPL services. Stop the existing process first if you need a clean single-process dev session.
- Watcher thrash: check `system.watchers` logs; extend debounce or fix generators.
- nREPL conflicts: change port in `dev/core.clj` if 7888 is occupied.
- Missing Shadow build: ensure `shadow-cljs.edn` has `:app`, rerun `bb run-app`.
- Noisy stderr: dev startup suppresses stderr by default; set `DEV_SUPPRESS_STDERR=false` (or `0`/`no`) to keep stderr visible.

## Extend

- Add watchers in `dev/system/watchers.clj` (reuse the debounce pattern).
- Update `run-app.sh`/BB task if you add background services (e.g., CSS watchers) so everything boots together.
