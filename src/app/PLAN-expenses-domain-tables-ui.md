# Expenses Domain — Admin + User Tables UI Plan

Last updated: 2025-12-24

This plan upgrades the “stub” admin pages for Expenses-domain reference entities (suppliers, articles, payers, etc.) into a drill-down friendly admin experience, and defines an incremental path for user-facing reference data.

## 1) Current State (Verified)

### Admin UI (already working as “stub pages”)

The following pages exist and already render via `generic-admin-entity-page`, providing:
table + pagination + column toggles + default CRUD (via the admin pipeline and domain adapters/config).

- `src/app/domain/frontend/expenses/pages/admin/suppliers.cljs`
- `src/app/domain/frontend/expenses/pages/admin/articles.cljs`
- `src/app/domain/frontend/expenses/pages/admin/payers.cljs`
- `src/app/domain/frontend/expenses/pages/admin/receipts.cljs`
- `src/app/domain/frontend/expenses/pages/admin/article_aliases.cljs`
- `src/app/domain/frontend/expenses/pages/admin/price_observations.cljs`

Expenses is already “custom” (list + detail) and should remain the reference implementation:
- `src/app/domain/frontend/expenses/pages/admin/expense_list.cljs`
- `src/app/domain/frontend/expenses/pages/admin/expense_detail.cljs`

### Domain-owned admin config (already present)

These files already define most of what Phase 1 originally proposed to “add”:

- `src/app/domain/frontend/expenses/admin/config/entities.edn`
- `src/app/domain/frontend/expenses/admin/config/form-fields.edn`
- `src/app/domain/frontend/expenses/admin/config/table-columns.edn`
- `src/app/domain/frontend/expenses/admin/config/view-options.edn`

### Admin routes (list routes exist; detail routes missing)

- Admin list routes already exist in `src/app/domain/frontend/expenses/routes.cljs` for:
  `/expenses`, `/expenses/:id`, `/receipts`, `/suppliers`, `/articles`, `/payers`, `/article-aliases`, `/price-observations`.
- There are currently **no admin detail routes** for suppliers/articles/payers/receipts/etc.

### Backend API (verified)

Admin CRUD endpoints exist for all domain tables under `/admin/api/expenses/*` and are generated/configured via:
- `src/app/domain/backend/expenses/routes/route_configs.clj`
- `src/app/domain/backend/expenses/routes/routes_factory.clj`

Important: the backend uses **plural keys for list** (e.g. `:suppliers`) and **singular keys for detail** (e.g. `:supplier`).

User API is mounted under `/api/v1/expenses`:
- `src/app/domain/backend/expenses/routes/user_api.clj`

Current user reference endpoints:
- `GET /api/v1/expenses/suppliers` (list only)
- `GET /api/v1/expenses/payers` (list only)
- No user endpoints for articles/article-aliases/price-observations/receipts.

### Frontend event generation (verified limitation)

Admin list events are generated via the event factory configs:
- `src/app/domain/frontend/expenses/events/entity_configs.cljs`

Only `:expenses` currently declares a `:detail-response-key`. If we add detail pages for other entities, the frontend must be updated to expect the backend’s singular response keys (e.g. `:supplier`, `:article`, `:payer`, `:receipt`, etc.).

## 2) Constraints / Principles

- Configuration-first: prefer extending existing domain config EDNs and `generic-admin-entity-page` overrides over creating bespoke pages.
- Keep `admin/adapters/specs.cljs` as **fallback-only** specs; domain config EDNs remain the source of truth once loaded.
- All new interactive elements must have stable `:id` attributes (see `INTERACTIVE-COMPONENTS-ID-AUDIT.md`).
- Avoid “phantom features”: only plan related panels that are supported by existing backend filters, or explicitly list backend changes required.

## 3) Phase Plan

### Phase 0 — Audit + Plumbing (unblocks detail pages)

Goal: make it possible to add detail routes/pages without guessing response shapes.

Work items:
1. Confirm backend detail response keys for each entity:
   - suppliers → `:supplier`, articles → `:article`, payers → `:payer`, receipts → `:receipt`,
     article-aliases → `:article-alias`, price-observations → `:price-observation`.
2. Update `src/app/domain/frontend/expenses/events/entity_configs.cljs` to provide `:detail-response-key` for each entity that will get a detail page.
3. Ensure the UI has stable “entity id” extraction for rows (generic list already uses `id-utils/extract-entity-id`).

Definition of done:
- Frontend can load a single entity by id for each Phase 1 entity (either via factory-generated detail events or explicit domain events).

### Phase 1 — Admin “Core Reference” Detail Pages (suppliers, articles, payers)

Goal: drill-down admin workflow for reference entities most relevant to expense entry.

Deliverables:
1. Add admin detail routes in `src/app/domain/frontend/expenses/routes.cljs`:
   - `/suppliers/:id`
   - `/articles/:id`
   - `/payers/:id`
2. Create detail pages under `src/app/domain/frontend/expenses/pages/admin/`:
   - `supplier_detail.cljs`
   - `article_detail.cljs`
   - `payer_detail.cljs`
3. Add “View” action on list rows:
   - Prefer a reusable admin action component (wired via domain admin config in `entities.edn`), rather than duplicating list pages.
   - Buttons must follow the `btn-*` id convention (e.g. `btn-view-suppliers-<id>`).
4. Related panels on detail pages (only using existing backend filters):
   - Supplier detail:
     - Expenses filtered by `supplier_id` (supported by expenses routes)
     - Article aliases filtered by `supplier_id` (supported)
     - Price observations filtered by `supplier_id` (supported)
   - Article detail:
     - Article aliases filtered by `article_id` (supported)
     - Price observations filtered by `article_id` (supported)
   - Payer detail:
     - Expenses filtered by `payer_id` (supported)

Definition of done:
- From each list page, clicking “View” navigates to the correct detail route.
- Each detail page loads the entity + at least one related panel reliably.

### Phase 2 — Admin “Complex” Entities (receipts, article-aliases, price-observations)

Goal: handle workflow/status + relationship-heavy forms and displays.

Receipts:
- Add `/receipts/:id` route + `receipt_detail.cljs`.
- Implement status workflow actions using existing backend endpoints:
  - retry (`POST /admin/api/expenses/receipts/:id/retry`)
  - status update (`POST /admin/api/expenses/receipts/:id/status`)
  - fail (`POST /admin/api/expenses/receipts/:id/fail`)
  - approve/post (`POST /admin/api/expenses/receipts/:id/approve`)
- Add a `receipt_viewer.cljs` component if needed:
  - image preview (if available) + structured JSON viewer for extraction payloads
  - every action button must have an `:id`

Article aliases + price observations:
- Upgrade form inputs away from raw UUID entry:
  - Supplier selector (entity-backed)
  - Article selector (entity-backed)
- Add optional “context” panels on detail pages (aliases ↔ observations) driven by filters.

Definition of done:
- Receipts detail page supports at least retry + status change end-to-end.
- Article aliases and price observations can be edited without manual UUID typing.

### Phase 3 — Expense Items (Decision Point)

Current reality:
- Expense items are stored in `expense_items` but are managed as nested `:items` within expense create/update.
- There is no standalone admin CRUD route for `expense_items` today.

Option A (recommended first): keep embedded, improve UX where it exists
- Improve line-item UX in existing forms/components.
- Add better “items” section on `admin-expense-detail` without adding a new entity.

Option B: standalone expense items admin page (bigger scope)
- Backend: add an admin endpoint (`/admin/api/expenses/expense-items`) + service + route config.
- Frontend: add entity config + adapters + list page + (optional) detail page.

Definition of done:
- A chosen option is implemented and validated; avoid half-implementing both.

### Phase 4 — User-Facing Reference Data (Incremental)

Reality check:
- User API currently exposes *read-only* suppliers and payers lists.
- There is no user API for managing (create/update/delete) suppliers/payers/articles.

Phase 4.1 (no backend changes): user read-only pages
- Add `/suppliers` and `/payers` user pages backed by the existing user endpoints.
- Use these pages primarily for transparency and search (and to validate the endpoints).

Phase 4.2 (requires backend work): user-managed reference data
- Decide ownership model:
  - Admin-owned global reference data vs user-owned per-user data vs hybrid.
- If user-managed:
  - Add user API CRUD routes under `src/app/domain/backend/expenses/routes/user_api.clj`
  - Add RLS/authorization rules and audit logging as needed
  - Add user pages + “quick add” flows from the expense form

Definition of done:
- 4.1: users can view suppliers/payers lists without errors.
- 4.2: only after a backend decision, users can create/edit/delete reference data safely.

### Phase 5 — Enhancements (optional, after core drill-down works)

- Inline editing on list pages (small updates without modals)
- Bulk operations where safe (batch edit/delete)
- Advanced filtering (date range/status)
- CSV export (admin)
- Audit trail widgets on detail pages (admin)

## 4) File/Module Map (Where Changes Should Land)

Admin UI:
- Entity specs fallback: `src/app/domain/frontend/expenses/admin/adapters/specs.cljs`
- Adapter init: `src/app/domain/frontend/expenses/admin/adapters/ui_state.cljs`
- Admin pages: `src/app/domain/frontend/expenses/pages/admin/*`
- Admin routes: `src/app/domain/frontend/expenses/routes.cljs`
- Domain admin config: `src/app/domain/frontend/expenses/admin/config/*.edn`

Events/data:
- Event factory + configs: `src/app/domain/frontend/expenses/events/events_factory.cljs`, `src/app/domain/frontend/expenses/events/entity_configs.cljs`
- Entity-specific event namespaces: `src/app/domain/frontend/expenses/events/*.cljs`

User UI:
- User routes: `src/app/domain/frontend/expenses/routes/user.cljs`
- User pages: `src/app/domain/frontend/expenses/pages/user/*`

Backend:
- Admin expenses routes: `src/app/domain/backend/expenses/routes/*`
- User API: `src/app/domain/backend/expenses/routes/user_api.clj`

## 5) Validation Checklist (per phase)

- Run config validation/audit when touching config EDNs:
  - `bb validate-frontend-config`
  - `bb config-audit --strict`
- For frontend behavior changes, run the narrowest relevant CLJS tests (save output once via `tee`).
- Manually verify in the admin UI at `http://localhost:8085`:
  - list → view → detail navigation
  - status/actions on receipts (Phase 2)
  - selectors work without manual UUID typing

