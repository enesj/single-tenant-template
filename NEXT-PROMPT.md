# NEXT SESSION PROMPT — 2025-12-08
PROMPT: admin expenses pages list-view integration

## Context Snapshot
- Single-tenant SaaS template (Clojure/ClojureScript + PostgreSQL); admin served at http://localhost:8085/admin with auto-reload via `bb run-app`.
- Admin UI relies on the template list stack (`app.template.frontend.components.list/list-view`) with entity specs + adapters (users, audit, login events) stored in the template list state; view-options can hardcode controls.
- Home Expenses tracker domain is new under `/admin/api/expenses/**` (suppliers, payers, receipts, expenses entries, reports); FE routes merged into admin via `src/app/domain/expenses/frontend/routes.cljs`.
- Expenses domain pages (Expenses, Receipts, Suppliers, Payers) currently render bespoke tables from `[:admin :expenses ...]` state and do NOT use list-view (no template sync, no pagination/filter UI).
- Template component integration doc highlights DRY list-view reuse; admin panel single-tenant doc covers guarded routes/auth flow.
- Available MCP skills: app-db-inspect, reframe-events-analysis, system-logs (use for state/events/logs debugging).
- Ops: hot reload stack via `bb run-app`; Shadow admin build `npm run watch:admin`; tests `npm run test:cljs` or `bb fe-test-node`, backend `bb be-test` if touched.
- Missing doc noted: `docs/frontend/feature-guides/admin.md` referenced in docs/index but not present here.

## Task Focus
Make admin Expenses, Receipts, Suppliers, and Payers pages use the shared list-view component (template entity store, adapters, specs, pagination/filtering/actions) like Users/Audit/Login Events.

## Code Map
- `src/app/domain/expenses/frontend/pages/expense_list.cljs` — manual table for expenses; loads via `::expenses-events/load-list`.
- `src/app/domain/expenses/frontend/pages/receipts.cljs` — manual receipts inbox using `::receipts-events/load-list`.
- `src/app/domain/expenses/frontend/pages/suppliers.cljs` — manual suppliers table using `::suppliers-events/load`.
- `src/app/domain/expenses/frontend/pages/payers.cljs` — manual payers table using `::payers-events/load`.
- `src/app/domain/expenses/frontend/events/{expenses,receipts,suppliers,payers}.cljs` — fetch data into `[:admin :expenses ...]`; no template sync/metadata.
- `src/app/domain/expenses/frontend/subs/*.cljs` — expose items/loading/error from the domain-local state.
- `src/app/domain/expenses/frontend/routes.cljs` — adds /admin routes with guarded controllers dispatching the load events.
- `src/app/admin/frontend/components/layout.cljs` — nav links for Expenses/Receipts/Suppliers/Payers.
- Template list stack: `src/app/template/frontend/components/list.cljs` (list-view), `src/app/template/frontend/components/table.cljs`, list events/subs under `src/app/template/frontend/events.list.*` and `subs`, paths in `app.template.frontend.db.paths`.
- Admin list patterns to mirror: adapters (`src/app/admin/frontend/adapters/{users,audit,login_events}.cljs`), list-loading events (`src/app/admin/frontend/events/{users,audit,login_events}.cljs`), renderer wrapper `src/app/admin/frontend/renderers/content.cljs`, page wrapper `src/app/admin/frontend/components/admin_page_wrapper.cljs`.
- Entity spec machinery: `src/app/admin/frontend/specs/generic.cljs` (+ `app.shared.field-specs`), display settings/user prefs via `app.admin.frontend.subs.config`; hardcoded controls in `resources/public/admin/ui-config/view-options.edn`.

## Commands to Run
- `bb run-app` (full stack + watcher, port 8085).
- `npm run watch:admin` for Shadow admin build (or `npm run watch` if both builds needed).
- Tests: `npm run test:cljs` (or `npm run test:cljs:karma` browser), `bb fe-test-node`; backend sanity `bb be-test` if backend touched.
- Optional REPL: `clj-nrepl-eval --discover-ports` then attach for REPL-driven debugging.

## Gotchas
- Admin auth token required for `/admin/api/**`; login flow sets `x-admin-token`/localStorage.
- list-view expects entity specs + template entity store; normalize IDs to strings and keep pagination/filter metadata under template paths (`app.template.frontend.db.paths`).
- Single-tenant only (no RLS/tenant switching); avoid host-specific fields.
- View-options can hide controls; respect `resources/public/admin/ui-config/view-options.edn` when wiring display settings.
- Current expenses pages keep data in `[:admin :expenses ...]`; migration needs normalization + sync events (adapter layer) similar to login-events adapter.
- Port 8085 (not 3000); stack auto-restarts—no manual restart necessary.
- Missing doc: `docs/frontend/feature-guides/admin.md` referenced but absent—flag if needed.

## Checklist for Next Agent
1) Create a comprehensive implementation plan in the repo root (e.g., `PLAN-expenses-list.md`) and track progress there. After the plan is written, start implementation immediately—no need to ask for approval.
2) Review how users/audit/login-events use list-view: adapters register entity-spec + sync events, events load data into the template store, pages render via admin page wrapper/list-view.
3) Design entity specs (columns/actions) for expenses, receipts, suppliers, payers; place them in the admin spec machinery (`app.admin.frontend.specs` or new adapters) so list-view columns/export settings align with API fields.
4) Add adapters to normalize API data and sync into template list state (`app.template.frontend.db.paths`), including pagination/filter metadata; wire controllers to template list events.
5) Replace manual tables in expenses domain pages with `$ list-view {...}` using proper `entity-name`, `entity-spec`, `display-settings`, and optional `render-actions`.
6) Ensure routes/controllers keep auth guard and re-dispatch load events with pagination/filter params derived from template state.
7) Test in browser via `bb run-app` + `npm run watch:admin` (http://localhost:8085/admin/{expenses,receipts,suppliers,payers}); verify pagination/filtering/selection/export if enabled.
8) Add/adjust CLJS tests for adapters/normalization; run `npm run test:cljs` (and backend tests if BE changes happen).
