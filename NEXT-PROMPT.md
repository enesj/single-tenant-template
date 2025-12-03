# NEXT SESSION PROMPT d 2025-12-03
PROMPT: "audit & login-events delete-only mode"

## Context snapshot
- Single-tenant Clojure/ClojureScript SaaS template with PostgreSQL; admin console lives at `http://localhost:8085/admin` (no tenant switching, no RLS).
- Admin UI entry: `app.admin.frontend.core/init` (Shadow-CLJS `:admin` build); routes in `app.admin.frontend.routes` for `/admin/audit` and `/admin/login-events`.
- Admin tables (users, audit, login-events, admins) all use the shared template list system: `app.template.frontend.components.list/list-view` via `app.admin.frontend.components.generic-admin-entity-page` plus entity configs in `src/app/admin/frontend/config/entities.edn`.
- Backend admin API is composed in `app.backend.routes.admin-api/admin-api-routes` under `/admin/api`; audit routes in `app.backend.routes.admin.audit` (`/admin/api/audit`), login-events routes in `app.backend.routes.admin.login-events` (`/admin/api/login-events`).
- Generic template CRUD events live in `app.template.frontend.events.list.crud` and `app.template.frontend.events.list.selection`; they build admin DELETE URLs as `/admin/api/<entity-name>/<id>` based on the entity keyword name.
- Audit and login-events DB tables are `:audit_logs` and `:login_events` in `resources/db/models.edn` (generated; **do not edit directly**, use `resources/db/template/*.edn` + migrations instead).
- Available MCP skills: `app-db-inspect` (inspect re-frame app-db), `reframe-events-analysis` (event traces), `system-logs` (backend + shadow-cljs logs). Prefer these over ad-hoc shell/debugging when checking UI state or server logs.
- Note: `docs/frontend/feature-guides/admin.md` is referenced in docs but missing in this repo; rely on `docs/frontend/admin-panel-single-tenant.md` and `docs/frontend/template-component-integration.md` instead.

## Task focus
- Enforce **delete-only** behaviour for the global Audit Logs and Login Events pages:
  - Permanently disable **per-row edit** and **batch edit** UI on `/admin/audit` and `/admin/login-events`.
  - Keep **single delete** and **batch delete** enabled and functional for both pages.
  - Fix the current "Failed to delete item" failures on these pages by aligning frontend delete behaviour with the backend API and adding any missing delete endpoints.

## Code map

- Frontend routing & pages
  - `src/app/admin/frontend/routes.cljs` d reitit routes for `/admin/audit` (`:admin-audit`) and `/admin/login-events` (`:admin-login-events`), each using guarded controllers to dispatch `:admin/load-audit-logs` / `:admin/load-login-events`.
  - `src/app/admin/frontend/pages/audit.cljs` d wraps the page in `generic-admin-entity-page` for `:audit-logs`.
  - `src/app/admin/frontend/pages/login_events.cljs` d wraps the page in `generic-admin-entity-page` for `:login-events`.

- Entity configuration & registry
  - `src/app/admin/frontend/config/entities.edn`
    - `:audit-logs` config: `:display-settings` currently has `:show-edit? true`, `:show-delete? true`, `:show-batch-edit? true`, and `:features {:batch-operations? true ...}` d this is why edit + batch edit are visible.
    - `:login-events` config: `:display-settings` has `:show-edit? false`, `:show-delete? true`, no explicit `:show-batch-edit?` (defaults to true in the list header), with `:features {:batch-operations? true ...}`.
  - `src/app/admin/frontend/system/entity_registry.cljs` d entity registry used by `admin-page-wrapper`:
    - `:audit-logs` d binds to `audit-adapter/init-audit-adapter!`, uses `enhanced-action-buttons` plus `admin-audit-actions` and `audit-details-modal`.
    - `:login-events` d binds to `login-events-adapter/init-login-events-adapter!`, uses `enhanced-action-buttons` only (no custom per-row actions yet).

- Generic admin entity page & wrapper
  - `src/app/admin/frontend/components/generic_admin_entity_page.cljs` d resolves `entity-config` via `[:admin/entity-config <entity>]`, then passes `:features` flags into `admin-page-wrapper` (e.g. `:batch-operations?` controls selection counter and batch ops visibility).
  - `src/app/admin/frontend/components/admin_page_wrapper.cljs` d wraps the page in `layout/admin-layout`, shows success/error messages, selection counter (`selection-counter` component), and calls `render-main-content` (which ultimately renders `list-view`).

- List + actions (template layer)
  - `src/app/template/frontend/components/list.cljs` d `list-view` reads `display-settings` and user overrides, then:
    - Passes `:show-edit?` / `:show-delete?` and other flags into `render-row` via `base-props`.
    - Passes `:show-batch-edit?` into `make-table-headers` (controls both batch edit and batch delete header buttons).
  - `src/app/template/frontend/components/list/rows.cljs` d row rendering:
    - Renders action cell via `cells/action-buttons`, receiving `:show-edit?` and `:show-delete?` from `base-props`.
  - `src/app/admin/frontend/components/enhanced_action_buttons.cljs` d admin-specific row actions:
    - Props: `{:entity-name item show-edit? show-delete? custom-actions}`; defaults `show-edit? true` / `show-delete? true` if not provided.
    - For `:users` and `:admins`, delete dispatches custom admin events; for all other entities it dispatches `[:app.template.frontend.events.list.crud/delete-entity entity-name-kw item-id]`.
    - Edit button only controls the inline edit form (via `:app.template.frontend.events.config/set-editing`).
  - `src/app/template/frontend/components/list/table.cljs` d table headers and batch actions:
    - `action-header-buttons` renders header-level **Batch edit**, **Batch delete**, and **⋯** "more" actions.
    - All three header buttons are gated by `show-batch-edit?` (if nil, treated as true), and they require 2+ selected rows to be enabled.
    - Batch delete dispatches `[:app.template.frontend.events.list.selection/delete-selected entity-name selected-ids]`.

- Template CRUD + selection
  - `src/app/template/frontend/events/list/crud.cljs`
    - `::delete-entity` builds DELETE requests via `app.template.frontend.api.http/delete-entity` with `:entity-name (name entity-type)`.
    - In admin context, `delete-entity` computes the URL as `/admin/api/<entity-name>/<id>` and sets `x-admin-token` from app-db or localStorage.
    - On failure, `::delete-failure` stores a generic error: `"Failed to delete item"` at `paths/entity-error`.
  - `src/app/template/frontend/events/list/selection.cljs`
    - `::delete-selected` iterates `selected-ids` and dispatches generic `::crud/delete-entity` for each.

- Audit-specific frontend logic
  - `src/app/admin/frontend/events/audit.cljs`
    - `:admin/load-audit-logs` issues `GET /admin/api/audit` with filters/pagination and syncs results via `audit-adapter/sync-audit-logs-to-template`.
    - `:admin/delete-audit-log` issues `DELETE /admin/api/audit/:id` with `x-admin-token`, then dispatches `:app.admin.frontend.adapters.audit/audit-log-deleted` and reloads logs.
    - `:admin/bulk-delete-audit-logs` is wired to call `DELETE /admin/api/audit/bulk` with `{:ids [...]}` but there is **no backend route for `/admin/api/audit/bulk` yet**.
  - `src/app/admin/frontend/components/audit_actions.cljs` d per-row audit actions dropdown:
    - Includes a "Delete Entry" dangerous action that confirms and dispatches `[:admin/delete-audit-log id]` (this path **does** line up with the backend delete route).
  - `src/app/admin/frontend/components/audit_export_controls.cljs` d custom header controls for `/admin/audit` (export all, clear filters).

- Login-events-specific frontend logic
  - `src/app/admin/frontend/events/login_events.cljs` d only handles loading (`GET /admin/api/login-events`) and state; there are **no dedicated delete or bulk delete events** for login events.
  - `src/app/admin/frontend/adapters/login_events.cljs` d adapter registering `:login-events` entity, normalizes IDs to strings, and initializes template UI state; deletion currently relies entirely on the generic template delete path.

- Backend audit + login-events
  - `src/app/backend/routes/admin/audit.clj`
    - `get-audit-logs-handler` uses `audit-service/get-audit-logs` and responds with `{:logs [...]}`.
    - `delete-audit-log-handler` handles `DELETE /admin/api/audit/:id` (via path `["/:id" {:delete ...}]` under `"/audit"` in `admin-api-routes`), explicitly sets `SET LOCAL app.bypass_rls = true`, deletes from `audit_logs` by UUID, logs admin action, and returns success vs 404.
    - There is **no** bulk delete route here yet; only single-row deletes.
  - `src/app/backend/routes/admin/login_events.clj`
    - Only defines `get-login-events-handler` for `GET /admin/api/login-events`; currently **no delete or bulk delete routes**.
  - `src/app/backend/services/admin/audit.clj` and `src/app/backend/services/monitoring/login_events.clj` d service layer helpers for audit + login-events; likely only read paths for login-events today.

- DB schema
  - `resources/db/models.edn` d `:audit_logs` and `:login_events` tables (IDs are UUIDs; basic metadata like `:action`, `:reason`, `:ip`, `:user_agent`, `:created_at`). Remember this file is generated; if you need schema changes, edit `resources/db/template/models.edn` / `resources/db/shared/models.edn` and regenerate migrations.

## Commands to run

- Start full dev stack (backend + Shadow CLJS + nREPL, port 8085):
  - `bb run-app`
- Admin frontend watcher (if you run it separately):
  - `npm run watch:admin`
- Frontend tests:
  - `npm run test:cljs`
  - `bb fe-test-node`
- Backend tests:
  - `bb be-test`
- Helpful REPL/monitoring (use when you need deeper debugging):
  - `clj-nrepl-eval --discover-ports` then attach to dev system.
  - Use **system-logs** skill to tail `./scripts/sh/monitoring/read_output.sh` output instead of manual `tail`/`grep`.

## Gotchas

- **Admin vs template endpoints:** generic template DELETE calls `/admin/api/<entity-name>/<id>` in admin context. For `:audit-logs` this becomes `/admin/api/audit-logs/:id`, but the backend only exposes `DELETE /admin/api/audit/:id`, so generic delete currently 404s and surfaces as `"Failed to delete item"`.
- **Login events are read-only today:** there is no `DELETE /admin/api/login-events/:id` route, so any generic delete attempts will also fail. You will need to add delete (and optional bulk delete) routes if you want delete/batch delete to work here.
- **Batch delete vs `:show-batch-edit?`:** the table header component uses a single `:show-batch-edit?` flag to gate **both** the "Batch edit" and "Batch delete" buttons (plus the "⋯" menu). Turning this off hides **all** header batch actions, not just batch edit.
- **Audit has two delete paths:** the dropdown in `admin-audit-actions` uses the correct `:admin/delete-audit-log` event and path (`/admin/api/audit/:id`), but the generic row trash icon uses template `::delete-entity` and hits `/admin/api/audit-logs/:id`. Decide which delete UX should remain and avoid double-delete affordances.
- **Bulk audit delete is half-wired:** frontend has `:admin/bulk-delete-audit-logs` expecting `DELETE /admin/api/audit/bulk`, but the backend lacks this route. Either implement the backend route or rewire batch delete to use the admin-specific bulk event instead of the generic template batch path.
- **Single-tenant + no RLS:** there is no per-tenant RLS here; `delete-audit-log-handler` already uses `SET LOCAL app.bypass_rls = true`. Be careful that any new delete endpoints for login-events also respect your desired audit/compliance model.
- **Docs gaps:** `docs/frontend/feature-guides/admin.md` is missing and `docs/backend/http-api.md` only documents GET for audit/login-events; update docs once you finalize new delete endpoints.

## Checklist for the next agent

1. **Reproduce current behaviour**
   - Run `bb run-app` (and `npm run watch:admin` if needed), log in as an owner admin at `http://localhost:8085/admin`.
   - Visit `/admin/audit` and `/admin/login-events`.
   - Verify that per-row Edit icons, header Batch edit, and header Batch delete appear; clicking delete should currently surface `"Failed to delete item"` due to endpoint mismatch/missing routes.

2. **Clarify desired UX and configuration**
   - Goal: for both pages, there should be **no inline editing** (per-row edit button or batch edit). Delete and batch delete should remain available and functional.
   - Decide whether audit logs should keep the dropdown "Delete Entry" action (`admin-audit-actions`) or rely solely on the standard trash icon; prefer a single, consistent delete affordance.

3. **Frontend config changes (entities.edn)**
   - In `src/app/admin/frontend/config/entities.edn`:
     - For `:audit-logs`, set `:display-settings` to **disable edit** while leaving delete on, e.g.: `:show-edit? false`, `:show-delete? true`. Keep `:show-select? true` and pagination as-is.
     - For `:login-events`, confirm `:show-edit? false` and `:show-delete? true` (already present) and consider adding `:show-batch-edit? false` if you introduce a separate configuration path for batch delete.
   - If you choose to keep using the template list header for batch delete, you will likely need to decouple `:show-batch-edit?` into separate `:show-batch-edit?` and `:show-batch-delete?` flags in `list/table.cljs` (and thread the new flag from `display-settings`). Document this behavioural change.

4. **Align single-row delete behaviour for audit logs**
   - Option A (minimal change, reuse existing admin events):
     - Hide the generic row delete icon for `:audit-logs` by passing `:show-delete? false` into `enhanced-action-buttons` for that entity (or by special-casing `entity-name` in `enhanced-action-buttons`), and rely exclusively on `admin-audit-actions`' "Delete Entry" (which calls `:admin/delete-audit-log`).
   - Option B (route generic delete through admin event):
     - Extend `enhanced-action-buttons` to treat `:audit-logs` specially: when `entity-name-kw` is `:audit-logs`, dispatch `[:admin/delete-audit-log item-id]` instead of `::crud-events/delete-entity`.
   - Whichever option you choose, ensure only **one** delete path remains visible in the UI and that it uses the `DELETE /admin/api/audit/:id` endpoint.

5. **Implement delete + batch delete for login events**
   - Backend:
     - In `src/app/backend/routes/admin/login_events.clj`, add a `delete-login-event-handler` similar to `delete-audit-log-handler`, using `next.jdbc/with-transaction` and deleting from `login_events` by UUID.
     - Add route(s) under `"/login-events"`:
       - `DELETE /admin/api/login-events/:id` for single delete.
       - Optionally `DELETE /admin/api/login-events/bulk` with `{:ids [...]}` for batch delete.
     - Log admin actions for deletions via `utils/log-admin-action` with a clear action name like `"delete_login_event"`.
   - Frontend:
     - Add `:admin/delete-login-event` and (optionally) `:admin/bulk-delete-login-events` events in `src/app/admin/frontend/events/login_events.cljs` mirroring the audit patterns.
     - Update `enhanced-action-buttons` to treat `:login-events` specially: for these rows, dispatch `:admin/delete-login-event` instead of generic `::crud-events/delete-entity`.
     - If you implement a login-events bulk endpoint, wire header batch delete for `:login-events` to your new bulk event instead of the generic template batch path.

6. **Fix batch delete wiring for audit logs**
   - Decide whether audit batch delete should:
     - Use the template `::selection/delete-selected` + generic delete (requires aligning backend to `/admin/api/audit-logs/:id` or adding a route alias), **or**
     - Use the admin-specific `:admin/bulk-delete-audit-logs` event and a new `DELETE /admin/api/audit/bulk` endpoint.
   - Prefer the admin-specific bulk event for audit, since `:admin/bulk-delete-audit-logs` already exists, and implement the matching backend route in `admin/audit.clj`.
   - Update the header batch delete wiring so that when the current entity is `:audit-logs`, it calls the admin bulk delete event instead of generic `::selection/delete-selected`.

7. **Update docs + configuration metadata**
   - Update `docs/backend/http-api.md` to document the new `DELETE /admin/api/audit/:id`, `DELETE /admin/api/audit/bulk` (if added), and the new login-events delete endpoints.
   - Optionally add a short note to `docs/frontend/admin-panel-single-tenant.md` explaining that audit/login-events are delete-only (no editing) and that delete/batch delete are owner-level maintenance operations.
   - If you introduce new display-settings flags (e.g., `:show-batch-delete?`), document them in `docs/frontend/template-component-integration.md` under the `list-view` section.

8. **Verify end-to-end behaviour (use MCP skills where helpful)**
   - Run `bb run-app` and `npm run watch:admin`, log in as an owner admin.
   - On `/admin/audit` and `/admin/login-events`:
     - Confirm per-row **Edit** icons and header **Batch edit** are not visible.
     - Confirm per-row **Delete** (and any dropdown-based delete actions) succeed without showing `"Failed to delete item"`.
     - Confirm **Batch delete** deletes multiple selected rows and refreshes the list.
   - Use **system-logs** to watch backend logs while deleting to ensure no 404/500 responses.
   - If UI state looks off (e.g., selection counter, list metadata, or errors not clearing), use **app-db-inspect** to inspect `:entities :audit-logs` / `:entities :login-events` and any `:error` fields under `paths/entity-metadata`.

