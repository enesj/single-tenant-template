<!-- ai: {:tags [:operations :dev :runbook :single-tenant] :kind :runbook} -->

# Live-Reload Development Environment (Single-Tenant)

Quick reference for the hot-reload stack (backend + Shadow CLJS + watchers) on port **8085**.

## Entry Points
- `bb run-app` *(recommended)*: wraps `scripts/sh/development/run-app.sh`, checks port **8085**, and runs `clojure -M:dev:migrations-dev` under `monitor_terminal.sh` (auto-restart + logging).
- `scripts/sh/development/run-app.sh`: same as above without Babashka.
- `clj -M:nrepl` (default 7888): attach to the dev runtime; `(user/start)` boots the system manually if you prefer REPL-driven startup.

## What Starts
- Backend (Ring/Reitit) + file watchers (backend sources, `resources/db/models.edn`).
- Shadow CLJS `:app` watch + browser REPL selection.
- nREPL on 7888.

## Live Reload Flow
1) `bb run-app` → script checks 8085 → launches dev profile under monitor.  
2) Backend watcher restarts on `.clj/.cljc/.edn` changes in `src/app`, `dev`, `config`, `vendor`.  
3) Models watcher restarts on `resources/db/models.edn` changes.  
4) Shadow `watch :app` pushes frontend updates automatically.  
5) nREPL stays available for editor/Portal connections.

## Typical Workflow
```bash
bb run-app
# watch logs confirm backend restart + Shadow watch + nREPL
```
- Connect to `localhost:7888` if you need inline eval.
- Edit backend/edn → backend auto-restarts.
- Edit CLJS → Shadow hot-reloads in browser.
- Manual restart: `(system.core/restart-system)` from the REPL.

## Troubleshooting
- Port in use: script prints owner; free 8085 or adjust `run-app.sh`.
- Watcher thrash: check `system.watchers` logs; extend debounce or fix generators.
- nREPL conflicts: change port in `dev/core.clj` if 7888 is occupied.
- Missing Shadow build: ensure `shadow-cljs.edn` has `:app`, rerun `bb run-app`.

## Extend
- Add watchers in `dev/system/watchers.clj` (reuse the debounce pattern).
- Update `run-app.sh`/BB task if you add background services (e.g., CSS watchers) so everything boots together.
