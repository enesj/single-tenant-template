# Next Session Prompt (2025-12-03)
PROMPT: "admin users table sort reverts after click" (sorting on /admin/users briefly applies then reverts)

## Context Snapshot
- Single-tenant Clojure/ClojureScript template; admin UI at http://localhost:8085 (admin build in shadow-cljs). 
- Admin routes via `app.admin.frontend.routes` guarded start; `/admin/users` uses generic list components + admin adapters.
- Lists/tables share template list system (`app.template.frontend.components.list/table`) with sort state in list UI state events/subs.
- Users data fetched from `/admin/api/users` (backend route `app.backend.routes.admin.users`, composed in `app.backend.routes.admin-api`).
- State sync: admin users events sync into template entity store via `app.admin.frontend.adapters.users/sync-users-to-template`.
- Dev workflow: stack auto-reloads; primary commands `bb run-app` (backend+shadow), `npm run watch:admin` for admin build if needed; tests via `bb fe-test`, `bb be-test`.

## Task Focus
Investigate why clicking column headers on `/admin/users` applies sort momentarily then reverts (table refresh resets sort). Goal: keep selected sort stable across refreshes and re-fetches.

## Code Map (start here)
- `src/app/admin/frontend/pages/users.cljs` — renders admin users page via generic list entity wrapper.
- `src/app/admin/frontend/events/users/core.cljs` — admin load/fetch users, sync to template store, delete; dispatches to adapters.
- `src/app/admin/frontend/adapters/users.cljs` — bridges admin HTTP to template list CRUD (normalize fields, sort/pagination params).
- `src/app/template/frontend/events/list/ui_state.cljs` — list UI state for sort/pagination/toggles; event `::set-sort-field` controls sort direction.
- `src/app/template/frontend/components/list.cljs` & `table.cljs` — table UI uses `::list-subs/sort-config` to send sort field/direction to events.
- `src/app/template/frontend/events/list/crud.cljs` — list fetch/refresh wiring that may reset params.
- Backend: `src/app/backend/routes/admin/users.clj` & `src/app/backend/routes/admin_api.clj` — server-side support for sort params.

## Commands to Run
- Start stack: `bb run-app` (serves admin at 8085; auto-restarts).
- Frontend watch (if separate): `npm run watch:admin` (shadow-cljs :admin build on port 8085 assets).
- Optional tests: `bb fe-test` (CLJS), `bb be-test` (Clojure).
- CLJS tracing if needed: attach to shadow nREPL (`clj-nrepl-eval --discover-ports` → `(shadow/repl :admin)`), then use tracing helpers.

## Gotchas / Notes
- Port is 8085 (not 3000). Auth simplified single-tenant; ensure admin token/cookie present.
- Sort state stored per entity in list UI state; route controllers may dispatch `:admin/load-users` on navigation which can override sort params if defaults used.
- Admin adapters expect sort params names (e.g., `:sort-by`, `:sort-order`); confirm column headers send consistent fields.
- List components merge user prefs with page-level config (see `list_view_settings` docs). Hardcoded settings can lock controls.
- Monitoring/logs: use `./scripts/sh/monitoring/read_output.sh` (system-logs skill) to see backend/shadow output if issues.

## Checklist for Next Session
1) Reproduce: run `bb run-app`, open `http://localhost:8085/admin/users`, click column headers; watch network tab to see request params when sort toggles then resets.
2) Inspect UI state: use app-db-inspect skill to read `[:entities :users :list]`, `[:ui :lists :users :sort]` before/after clicks; confirm if sort field/direction persists.
3) Trace events: use reframe-events-analysis skill (`repl-trace/recent` or search for `:app.template.frontend.events.list.ui-state/set-sort-field` and follow dispatch flow to any refresh/guard events).
4) Check list fetch pipeline: in `app.template.frontend.events.list.crud` and admin adapters ensure sort params are passed through and not overwritten on refresh or when `:admin/load-users` runs after sync.
5) Review `app.admin.frontend.pages.users`/controllers for automatic reloads after sort; ensure table header click dispatch includes current pagination/filter and avoids triggering a full default reload.
6) Verify backend honors sort params (users routes); confirm response order matches request and no post-processing resort in adapters.
7) Implement fix (persist sort state or prevent immediate reload with default params), test again; add regression note/test if feasible.
8) Keep logs clean; rerun flow after fix and ensure sort sticks across page reload or pagination.
