# Expenses Domain — Admin + User Tables UI Plan

Last updated: 2025-12-26

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

### Admin routes (list + detail routes exist)

- Admin routes exist in `src/app/domain/frontend/expenses/routes.cljs` for list + detail flows:
  `/expenses`, `/expenses/:id`, `/expense-items`,
  `/receipts`, `/receipts/:id`,
  `/suppliers`, `/suppliers/:id`,
  `/articles`, `/articles/:id`,
  `/payers`, `/payers/:id`,
  `/article-aliases`, `/article-aliases/:id`,
  `/price-observations`, `/price-observations/:id`.

### Backend API (verified)

Admin CRUD endpoints exist for all domain tables under `/admin/api/expenses/*` and are generated/configured via:
- `src/app/domain/backend/expenses/routes/route_configs.clj`
- `src/app/domain/backend/expenses/routes/routes_factory.clj`

Important: the backend uses **plural keys for list** (e.g. `:suppliers`) and **singular keys for detail** (e.g. `:supplier`).

User API is mounted under `/api/v1/expenses`:
- `src/app/domain/backend/expenses/routes/user_api.clj`

Current user endpoints:
- Suppliers: `GET/POST /api/v1/expenses/suppliers`, `PUT/DELETE /api/v1/expenses/suppliers/:id` (writes role-gated)
- Payers: `GET/POST /api/v1/expenses/payers`, `PUT/DELETE /api/v1/expenses/payers/:id` (writes role-gated)
- Receipt upload: `POST /api/v1/expenses/upload` (multipart `file`)
- Receipts inbox: `GET /api/v1/expenses/receipts`, `GET /api/v1/expenses/receipts/:id`, `POST /api/v1/expenses/receipts/:id/approve`

### Frontend event generation (verified limitation)

Admin list events are generated via the event factory configs:
- `src/app/domain/frontend/expenses/events/entity_configs.cljs`

Admin entity configs declare `:detail-response-key` in `src/app/domain/frontend/expenses/events/entity_configs.cljs` to match backend singular response keys (e.g. `:supplier`, `:article`, `:payer`, `:receipt`, etc.).

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

### Phase 3 — Expense Items (Implemented)

Current reality:
- Expense items remain embedded on expense detail/edit flows (`:items`), but also have a standalone admin CRUD surface:
  - Admin API: `/admin/api/expenses/expense-items`
  - Admin page: `/admin/expense-items` (modal create/edit)

### Phase 4 — User-Facing Reference Data (Incremental)

**Use Case Context**: Family expense tracking app with few users. Reference data (suppliers, payers, articles) represents shared household entities—"Amazon" or "Whole Foods" should be defined once and reused by all family members.

**Ownership Model Decision**: ✅ **Global reference data** (shared catalog)
- All users see the same suppliers/payers/articles
- Any member or admin can add to the shared catalog
- Viewers have read-only access
- No per-user isolation needed; reference tables have no `user_id` column

#### Role Permissions for Reference Data

| Role | Dashboard | View Reference Data | Add/Edit Reference Data | Manage Users |
|------|-----------|:-------------------:|:-----------------------:|:------------:|
| `unassigned` | ⚠️ "Wait for role assignment" screen | ❌ | ❌ | ❌ |
| `viewer` | ✅ Normal | ✅ Read-only (all data) | ❌ | ❌ |
| `member` | ✅ Normal | ✅ Read-only (all data) | ✅ Can modify shared catalog | ❌ |
| `admin` | ✅ Normal | ✅ Read-only (all data) | ✅ Can modify shared catalog | ✅ Add/remove users, change roles |

**Key distinction**: Admin vs Member difference is **user management**, not reference data access. Both can equally contribute to the shared supplier/payer/article catalog.

---

#### Current State (Reality Check)

The user API exposes shared reference data + receipts flows:
- `GET/POST /api/v1/expenses/suppliers` + `PUT/DELETE /api/v1/expenses/suppliers/:id` (writes role-gated to `member|admin`)
- `GET/POST /api/v1/expenses/payers` + `PUT/DELETE /api/v1/expenses/payers/:id` (writes role-gated to `member|admin`)
- `POST /api/v1/expenses/upload` (receipt upload, multipart `file`)
- `GET /api/v1/expenses/receipts` + `GET /api/v1/expenses/receipts/:id` + `POST /api/v1/expenses/receipts/:id/approve`

Write endpoints enforce role checks; viewers have read-only access.

Note: the backend must serve the SPA (index.html) for `/suppliers` and `/payers` deep-links
(see `src/app/domain/backend/registry.clj` → `:spa-routes`).

---

#### Phase 4.1: User Reference Pages (Suppliers + Payers)

**Status**: ✅ Implemented (2025-12-25)

**Goal**: Expose existing read-only user endpoints through user-facing UI pages.

**Deliverables**:
1. Create user pages under `src/app/domain/frontend/expenses/pages/user/`:
   - `suppliers.cljs` — Display all suppliers in a read-only table
   - `payers.cljs` — Display all payers in a read-only table
2. Add user routes in `src/app/domain/frontend/expenses/routes/user.cljs`:
   - `/suppliers` — Suppliers list page
   - `/payers` — Payers list page
3. Reuse or adapt admin table components:
   - Table rendering, pagination, column toggles
   - Hide "Add/Edit/Delete" actions for `viewer` role
   - Enable add/edit/delete for `member`/`admin` roles (shared catalog)
4. Role-based access:
   - `unassigned` → Redirect to pending assignment screen
   - `viewer`/`member`/`admin` → Can view pages

**Definition of done**:
- Users can visit `/suppliers` and `/payers` and see the full shared catalog
- Viewers see read-only tables (no add/edit buttons)
- Members/admins see tables with placeholder "Add" buttons (disabled or showing "coming soon")

---

#### Phase 4.2: User Write Permissions

**Goal**: Allow `member` and `admin` roles to create, edit, and delete reference data in the shared catalog.

**Status**: ✅ Implemented (2025-12-25)

**Backend changes** (`src/app/domain/backend/expenses/routes/user_api.clj`):

1. **CRUD endpoints for reference entities**:
   ```clojure
   ;; Suppliers
   POST   /api/v1/expenses/suppliers           ; Create
   PUT    /api/v1/expenses/suppliers/:id       ; Update
   DELETE /api/v1/expenses/suppliers/:id       ; Delete

   ;; Payers
   POST   /api/v1/expenses/payers
   PUT    /api/v1/expenses/payers/:id
   DELETE /api/v1/expenses/payers/:id
   ```

2. **Add authorization middleware**:
   ```clojure
   (defn wrap-user-authz [{:keys [require-role]}]
     (fn [handler]
       (fn [request]
         (let [user (:user request)]
           (if (contains? require-role (:role user))
             (handler request)
             {:status 403 :body "Forbidden"})))))

   ;; Apply to write endpoints
   (POST "/api/v1/expenses/suppliers" []
     (wrap-user-authz {:require-role #{:member :admin}})
     create-supplier-handler)
   ```

3. **No RLS policies needed** — data is shared globally; only role-based write permissions

**Frontend changes**:

1. **Add/edit forms** (`src/app/domain/frontend/expenses/pages/user/`):
   - Create modal or form component for adding suppliers/payers
   - Reuse form field definitions from admin config where possible
   - Add validation (normalized_key, display_name)

2. **Wire up "Add" buttons**:
   - Members/admins can click "Add Supplier" → opens form
   - Viewers: no add/edit actions visible

3. **Optional: Quick-add from expense form**:
   - When entering an expense, type "Amaz" → auto-suggest existing suppliers
   - Option to "Create new supplier" if no match (members/admins only)

**Definition of done**:
- Members and admins can create/edit/delete suppliers and payers
- Changes are immediately visible to all users (shared catalog)
- Viewers can view but not modify reference data
- Unassigned users cannot access reference data pages

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
