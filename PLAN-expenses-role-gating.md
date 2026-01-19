# PLAN — Expenses domain pages: role/capability gating + remove admin-panel access

**Created**: 2026-01-13  
**Updated**: 2026-01-13  
**Status**: ✅ Phases 0–9 complete; 🚧 Phase 10 (validation) in progress; ✅ Phase 11 (port `pages/admin/*` → user UI) complete; ✅ Phase 12 (delete legacy `pages/admin/*` + deprecated admin routes) complete

---

## Progress Log

### Phase 0 — Inventory ✅ COMPLETE

**App routes** (18 routes in `routes/user.cljs`):
- `/waiting-room`, `/expenses`, `/dashboard`, `/unmapped-items`
- `/expenses/dashboard`, `/expenses/list`, `/expenses/upload`
- `/receipts`, `/receipts/:receipt-id`, `/expenses/new`
- `/expenses/reports`, `/expenses/settings`, `/suppliers`, `/payers`
- `/articles`, `/article-aliases`, `/price-observations`
- `/expenses/:expense-id`

**Admin routes** (16 routes in `routes.cljs`):
- `/admin/expenses`, `/admin/expenses/:id`, `/admin/expense-items`
- `/admin/receipts`, `/admin/receipts/:id`
- `/admin/suppliers`, `/admin/suppliers/:id`
- `/admin/payers`, `/admin/payers/:id`
- `/admin/articles`, `/admin/articles/:id`
- `/admin/unmapped-items`
- `/admin/article-aliases`, `/admin/article-aliases/:id`
- `/admin/price-observations`, `/admin/price-observations/:id`

**Admin panel integration**:
- `src/app/admin/frontend/routes.cljs` imports `expenses-routes/routes` and merges into admin router
- `src/app/admin/frontend/components/layout.cljs` defines `domain-items` sidebar section

**Existing auth patterns**:
- Subs: `:current-user`, `:user-role`, `:is-tenant-owner?`
- Guard: `app.template.frontend.components.auth-guard/role-based-guard`
- Example: `unmapped_items.cljs` uses guard with `required-roles ["admin" "owner"]`

**Decision: Admin-only pages to port or remove**:
- `/admin/articles`, `/admin/article-aliases`, `/admin/price-observations`, `/admin/expense-items`
- **Recommendation**: Port as power-user pages under app routes (admin/owner only)

### Phase 1 — Authz API ✅ COMPLETE

Created `src/app/domain/frontend/expenses/authz.cljs`:
- `normalize-role`, `role-level`, `power-user?`, `assigned?`, `can-write?`, `can?`
- Re-frame subs: `:expenses/user-role`, `:expenses/power-user?`, `:expenses/assigned?`, `:expenses/can-write?`, `:expenses/can?`
- Capability map with 12 capabilities (access, write, delete, purge, etc.)

### Phase 2 — Remove admin-panel access ✅ COMPLETE

- Removed `expenses-routes` import from `src/app/admin/frontend/routes.cljs`
- Removed domain-items sidebar section from `src/app/admin/frontend/components/layout.cljs`
- Removed unused domain icon imports
- `/admin/expenses` and related routes now 404 in admin panel

### Phase 3 — Route/page gating ✅ COMPLETE

- Created `src/app/domain/frontend/expenses/components/page_guard.cljs`:
  - `expenses-page-guard` - generic guard with capability check
  - `power-user-guard` - guards for admin/owner only
  - `write-access-guard` - guards for member+ (can write)
- Refactored `unmapped_items.cljs` to use `power-user-guard`
- Added `write-access-guard` to `expense_new.cljs`

### Phase 4 — Action-level gating ✅ COMPLETE

- Fixed `payers.cljs` to use authz module (was missing "owner" in can-modify check)
- Updated `suppliers.cljs` to use authz module
- Removed admin panel links from supplier detail (articles, aliases, price-observations)
- Replaced inline role checks with `authz/can-write?` and `authz/power-user?`
- Removed duplicate `normalize-role` functions

### Phase 5 — Backend verification ✅ COMPLETE (secured + aligned)

Backend enforces the same role rules as the frontend:
- Viewer is read-only; member+ can write; admin/owner can perform “danger zone” operations.
- Key enforcement lives under:
  - `src/app/domain/backend/expenses/handlers/user_expenses/helpers.clj` (role sets)
  - `src/app/domain/backend/expenses/handlers/user_expenses/crud.clj` (expenses CRUD role gating)
  - `src/app/domain/backend/expenses/handlers/receipt_upload.clj` + `src/app/domain/backend/expenses/handlers/user_receipts.clj` (receipt upload/OCR/approve role gating)

### Phase 6 — Cleanup ✅ COMPLETE

- Kept the legacy admin routes/pages around temporarily for reference while porting (removed in Phase 12).

### Phase 7 — Finish viewer read-only UI ✅ COMPLETE

- Viewer cannot create/update/delete expenses.
- Viewer can view expenses list/detail and receipts list/detail, but cannot upload/OCR.
- “Approve & Post” is `member+`.

### Phase 8 — Settings page: role-based danger zone ✅ COMPLETE

- Settings is accessible to `viewer+`.
- Destructive actions (delete-all) are gated to `admin/owner` and require a confirmation token.

### Phase 9 — User API parity + role enforcement ✅ COMPLETE

- Implemented `/api/v1/expenses/settings`, `/api/v1/expenses/export`, `/api/v1/expenses/all` endpoints.
- Verified privileged endpoints return 403 when role is insufficient.

### Phase 10 — Validation 🚧 IN PROGRESS

- Run focused compile/tests and do a role smoke-test (viewer vs member vs admin/owner).
- ✅ Frontend compile (app): `npx shadow-cljs compile app 2>&1 | tee /tmp/shadow-compile-app-after-admin-pages-delete.txt`
- ✅ Frontend compile (admin): `npx shadow-cljs compile admin 2>&1 | tee /tmp/shadow-compile-admin-after-admin-pages-delete.txt`

### Phase 11 — Port deprecated domain “admin pages” into the user UI ✅ COMPLETE

- Goal: move any needed functionality from `src/app/domain/frontend/expenses/pages/admin/*` into the app build under `src/app/domain/frontend/expenses/pages/user/*`, gated by tenant roles/capabilities.
- ✅ Ensure the user sidebar includes a working “Expenses” link (`/expenses/list`) and a power-user “Expense Items” link (`/expense-items`).
- ✅ Ported `pages/admin/expense_items.cljs` into the user app as `/expense-items` (admin/owner only):
  - User UI: editable list + edit/delete actions
  - User API: `GET /api/v1/expenses/expense-items`, `PUT/DELETE /api/v1/expenses/expense-items/:id` (admin/owner only)
- ✅ Fixed backend update/delete SQL aliasing for expense items (HoneySQL `:update` alias syntax).

### Phase 12 — Delete legacy `pages/admin/*` + deprecated admin routes ✅ COMPLETE

- Deleted `src/app/domain/frontend/expenses/pages/admin/*` (16 files).
- Deleted `src/app/domain/frontend/expenses/routes.cljs` (deprecated admin `/admin/...` domain routes).
- Remaining: Phase 10 validation (compile outputs + role smoke test).

---

## Summary of Changes

### Files Created
- `src/app/domain/frontend/expenses/authz.cljs` - Centralized authorization module
- `src/app/domain/frontend/expenses/components/page_guard.cljs` - Page-level guards
- `src/app/domain/backend/expenses/handlers/user_expenses/settings.clj` - Settings, export, delete-all handlers (Phase 9)
- `src/app/domain/frontend/expenses/pages/user/expense_items.cljs` - Power-user Expense Items page (admin/owner) with editing
- `src/app/domain/backend/expenses/handlers/user_expenses/expense_items.clj` - User API expense items list + update + delete (admin/owner)

### Files Modified
- `src/app/admin/frontend/routes.cljs` - Removed domain routes import
- `src/app/admin/frontend/components/layout.cljs` - Removed domain sidebar section
- `src/app/domain/frontend/expenses/pages/user/unmapped_items.cljs` - Uses power-user-guard
- `src/app/domain/frontend/expenses/pages/user/expense_new.cljs` - Uses write-access-guard
- `src/app/domain/frontend/expenses/pages/user/payers.cljs` - Uses authz module
- `src/app/domain/frontend/expenses/pages/user/suppliers.cljs` - Uses authz module
- `src/app/domain/frontend/expenses/pages/user/expenses_list.cljs` - Uses can-write? for add/edit/delete gating (Phase 7)
- `src/app/domain/frontend/expenses/pages/user/expense_detail.cljs` - Uses can-write? for action buttons (Phase 7)
- `src/app/domain/frontend/expenses/pages/user/expense_settings.cljs` - Power-user gate for delete-all, added IDs (Phase 8)
- `src/app/domain/backend/expenses/routes/user_api.clj` - Added /settings, /export, /all + expense-items update/delete endpoints (Phases 9/11)
- `src/app/domain/frontend/expenses/events/user_expenses/power_tools.cljs` - Added expense-items update/delete events (Phase 11)

### Files Removed (Phase 12)
- `src/app/domain/frontend/expenses/routes.cljs` - removed deprecated admin `/admin/...` domain routes
- `src/app/domain/frontend/expenses/pages/admin/*` - removed admin panel pages

---

## Goal

Move the Expenses domain to a clear, consistent access model:

1) **Admin panel (`/admin/*`) has zero access to domain pages** (no “browse domain via admin UI”).  
2) **Domain pages live in the app build** and are gated by **tenant user roles/capabilities**:
   - Some pages/routes are **admin/owner only**.
   - Other pages are accessible to normal users but have **extra functionality** for admin/owner.
3) **Backend enforces the same rules** (frontend is UX; backend is security).

---

## Terminology (to avoid “admin” confusion)

- **Admin panel admin**: a platform/system operator authenticated via `/admin/login` and `/admin/api/*`.
- **Tenant “admin/owner” role**: an end-user role in the app build (`/login`, `/api/v1/*`) stored in `[:session :user :role]`.

This plan is about *tenant role-based access inside the domain*, not about giving the admin panel access to domain UI.

---

## Current State (what exists today)

### Frontend routing & pages

- **Admin panel has no Expenses domain routes/pages**:
  - `src/app/admin/frontend/routes.cljs` no longer requires/merges `app.domain.frontend.expenses.routes` (file removed).
  - Admin sidebar no longer shows an “Expenses Domain” section (`src/app/admin/frontend/components/layout.cljs`).
- **Legacy domain admin pages/routes removed** (Phase 12):
  - Deleted `src/app/domain/frontend/expenses/pages/admin/*`
  - Deleted `src/app/domain/frontend/expenses/routes.cljs`
- **Domain “user pages”** exist under:
  - `src/app/domain/frontend/expenses/pages/user/*`
  - Wired via domain registry:
    - `src/app/domain/frontend/registry.cljs` → `src/app/domain/frontend/expenses/routes/user.cljs`
    - Included in template app routes:
      - `src/app/template/frontend/routes/data.cljs` → `(domain-registry/all-user-routes)`

### Existing auth/role building blocks (reuse these)

- `:current-user`, `:user-role`, `:is-tenant-owner?` subs:
  - `src/app/template/frontend/subs/core.cljs`
- Role-based UI guard component:
  - `src/app/template/frontend/components/auth_guard.cljs` (`role-based-guard`)
- Example of a properly role-gated page:
  - `src/app/domain/frontend/expenses/pages/user/unmapped_items.cljs` (requires `"admin"|"owner"`)

### Follow-ups / audits

- Ensure no user pages link to `/admin/*` paths (grep + spot-check).
- Audit and remove any remaining `/admin/api/expenses/*` dependencies from the app build bundle (user runtime should prefer `/api/v1/expenses/*`).
- (Optional) Make `unassigned` gating consistent between controllers and page guards.

---

## Target Architecture

### 1) Only one “Expenses UI surface”: the app build

- Keep Expenses pages under app routes (current: `src/app/domain/frontend/expenses/routes/user.cljs`).
- Remove **all** Expenses domain routes and navigation from the admin panel.

### 2) Domain access is expressed as capabilities

Use a single place to answer:

- “May this user **enter** this route?”
- “May this user **see** this UI control?”
- “May this user **execute** this action?”

Recommendation: implement a small **Expenses authz** module that normalizes role + provides `can?`.

Example (conceptual):

- Roles: `"unassigned"`, `"viewer"`, `"member"`, `"admin"`, `"owner"`
- Capabilities (examples):
  - `:expenses/access` (assigned users)
  - `:expenses/expense.write` (create/update)
  - `:expenses/reference.write` (edit suppliers/payers)
  - `:expenses/reference.purge` (purge permanently)
  - `:expenses/unmapped.access` (unmapped items)
  - `:expenses/articles.manage` (mapping, aliases, etc.)
  - `:expenses/danger.delete-all` (danger zone actions)

The domain can start role-based and later evolve to permission-based (`[:session :permissions]`) without rewriting pages.

### 3) Two layers of gating (both required)

1) **Route/page gating**: prevent navigation and show “Access Denied”/redirect.
2) **Action gating**: hide/disable admin-only buttons/actions on shared pages.

Backend must still enforce (403) for protected endpoints.

---

## Route & Page Access Matrix (final policy)

This section is the **decision doc** for which Expenses pages exist in the app build and how they’re gated by tenant role.

### Existing user routes (app build)

Defined in `src/app/domain/frontend/expenses/routes/user.cljs`:

| Route | Page | Who can enter? | Notes |
|------:|------|----------------|------|
| `/waiting-room` | waiting room | authenticated + role=`unassigned` | For users not yet assigned to household/org role. |
| `/expenses` | expenses dashboard | `viewer/member/admin/owner` | If `unassigned` → redirect `/waiting-room`. |
| `/dashboard` | dashboard alias | `viewer/member/admin/owner` | Same gating as `/expenses`. |
| `/expenses/list` | expenses list | `viewer/member/admin/owner` | Viewer: read-only actions. |
| `/expenses/:expense-id` | expense detail | `viewer/member/admin/owner` | Viewer: no edit. |
| `/expenses/new` | new expense | `member/admin/owner` | Viewer blocked/redirect. |
| `/expenses/upload` | receipt upload | `member/admin/owner` | **Viewer cannot upload/OCR**. |
| `/receipts` | receipts inbox | `viewer/member/admin/owner` | OCR is `member+`. **“Approve & Post” stays `member+`.** |
| `/receipts/:receipt-id` | receipt detail | `viewer/member/admin/owner` | Mirrors `/receipts`. |
| `/expenses/reports` | reports | `viewer/member/admin/owner` | Exports could be power-user only if needed. |
| `/expenses/settings` | settings | `viewer/member/admin/owner` | Default preferences allowed for all; **danger-zone deletes are admin/owner only**. |
| `/suppliers` | suppliers | `viewer/member/admin/owner` | Add/edit/archive gated; purge admin/owner only. |
| `/payers` | payers | `viewer/member/admin/owner` | Add/edit/delete gated; align owner behavior. |
| `/articles` | articles | `admin/owner` | Power-user article catalog (used by unmapped-items workflow). |
| `/article-aliases` | article aliases | `admin/owner` | Power-user alias catalog (currently read-only list). |
| `/price-observations` | price observations | `admin/owner` | Power-user price history/observations (currently read-only list). |
| `/unmapped-items` | unmapped items | `admin/owner` | Already implemented with `role-based-guard`. |

### Admin panel routes (removed)

- Expenses domain routes/pages are no longer exposed under `/admin/*` (Phase 2).
- Legacy domain admin routes file `src/app/domain/frontend/expenses/routes.cljs` has been deleted (Phase 12).

---

## Pages to expose in the app build (by role)

This maps the concrete pages in `src/app/domain/frontend/expenses/pages/user/` to **(a)** who can enter and **(b)** what functionality is visible/usable per role.

### Roles (tenant roles, not admin-panel admins)

- `unassigned`: redirected to `/waiting-room` for any Expenses route.
- `viewer`: read-only access (cannot upload/OCR/create/update/delete).
- `member`: can create/update expenses, upload receipts, run OCR, and manage reference data.
- `admin` / `owner`: “power users” (everything `member` can do + destructive/admin-only tools).

### `expenses_dashboard.cljs` (`/expenses`, `/dashboard`, `/expenses/dashboard`)

- Enter: `viewer/member/admin/owner`
- Viewer: view summary/recent expenses; can navigate to list/reports.
- Member+: additionally sees “Upload Receipt” + “Add Expense”.
- Admin/Owner: additionally sees “Unmapped Items”.

### `expenses_list.cljs` (`/expenses/list`)

- Enter: `viewer/member/admin/owner`
- Viewer: list + view details only (no add/edit/delete).
- Member+: add + edit + delete (soft-delete) expenses.

### `expense_detail.cljs` (`/expenses/:expense-id`)

- Enter: `viewer/member/admin/owner`
- Viewer: view details only.
- Member+: edit/update expense state (e.g. “Mark as Posted”) and delete (soft-delete).

### `expense_new.cljs` (`/expenses/new`)

- Enter: `member/admin/owner`
- Member+: full manual-entry form.
- Admin/Owner: same form (no extra UI required unless you add admin-only fields later).

### `expense_upload.cljs` (`/expenses/upload`)

- Enter: `member/admin/owner`
- Member+: upload receipts (this is also the entry point for OCR processing).
- Viewer: cannot access this page.

### `receipts_list.cljs` (`/receipts`)

- Enter: `viewer/member/admin/owner`
- Viewer: view receipts, open “View Details” modal.
- Member+: additionally run OCR (single + batch) and upload receipts (via “Add” → `/expenses/upload`).
- Admin/Owner: same as member (including “Approve & Post”).

### `receipt_detail.cljs` (`/receipts/:receipt-id`)

- Enter: `viewer/member/admin/owner`
- Note: this route renders the receipts list and opens the shared detail modal; gating rules are the same as `/receipts`.

### `expense_reports.cljs` (`/expenses/reports`)

- Enter: `viewer/member/admin/owner`
- Viewer+: view charts + export (CSV/PDF).

### `expense_settings.cljs` (`/expenses/settings`)

- Enter: `viewer/member/admin/owner`
- Viewer+: can change personal/default preferences (currency, payer, notifications) and export data.
- Admin/Owner: additionally can execute destructive “danger zone” actions (e.g. delete-all).

### `suppliers.cljs` (`/suppliers`)

- Enter: `viewer/member/admin/owner`
- Viewer: read-only supplier list/detail.
- Member+: add/edit/archive suppliers.
- Admin/Owner: additionally “purge permanently” (typically only when archived).

### `payers.cljs` (`/payers`)

- Enter: `viewer/member/admin/owner`
- Viewer: read-only payers list.
- Member+: add/edit/delete payers.

### `articles.cljs` (`/articles`)

- Enter: `admin/owner`
- Admin/Owner: power-user article catalog (create new articles; used for mapping/aliases workflows).

### `article_aliases.cljs` (`/article-aliases`)

- Enter: `admin/owner`
- Admin/Owner: power-user alias catalog (**currently read-only list**; aliases are created via `/unmapped-items`).

### `price_observations.cljs` (`/price-observations`)

- Enter: `admin/owner`
- Admin/Owner: power-user price observations (**currently read-only list**).

### `unmapped_items.cljs` (`/unmapped-items`)

- Enter: `admin/owner`
- Admin/Owner: bulk-map raw labels to articles; create new articles; create aliases; resolve conflicts.

---

## `pages/admin/*` disposition

`src/app/domain/frontend/expenses/pages/admin/*` has been deleted (Phase 12). Any Expenses domain UI now lives under `src/app/domain/frontend/expenses/pages/user/` and is gated by tenant role/capability.

---

## Next Steps (precise follow-ups)

These are the concrete remaining tasks to make the policy above fully consistent across UI + backend.

### Phase 7 — Finish viewer read-only UI ✅ COMPLETE

- [x] `src/app/domain/frontend/expenses/pages/user/expenses_list.cljs`: gate add/edit/delete for viewers
- [x] `src/app/domain/frontend/expenses/pages/user/expense_detail.cljs`: gate “Mark as Posted” + “Delete” for viewers

### Phase 8 — Settings page: role-based “danger zone” ✅ COMPLETE

- [x] Confirmed policy: “Preferences + Export” = `viewer+`, “Delete all expenses” = `admin/owner` only
- [x] `src/app/domain/frontend/expenses/pages/user/expense_settings.cljs`: hide delete-all unless power-user; include confirmation token; add IDs

### Phase 9 — Backend parity for endpoints called by the app ✅ PARTIAL (stubbed settings)

- [x] `src/app/domain/backend/expenses/routes/user_api.clj`: add `/settings`, `/export`, `/all` endpoints
- [x] `src/app/domain/backend/expenses/handlers/user_expenses/settings.clj`: implement handlers (settings/export/delete-all)
- [ ] Implement persistent settings storage (currently stubbed in `src/app/domain/backend/expenses/handlers/user_expenses/settings.clj`)
- [ ] (Optional) Implement PDF export (currently stubbed)

### Phase 10 — Validation (save outputs)

- [x] Frontend compile (app): `npx shadow-cljs compile app 2>&1 | tee /tmp/shadow-compile-app-after-admin-pages-delete.txt`
- [x] Frontend compile (admin): `npx shadow-cljs compile admin 2>&1 | tee /tmp/shadow-compile-admin-after-admin-pages-delete.txt`
- [ ] Manual role smoke-test (viewer vs member vs admin/owner):
  - Viewer: can view dashboard/list/detail/receipts; cannot access `/expenses/new` or `/expenses/upload`; cannot OCR.
  - Member: can access `/expenses/new` + `/expenses/upload`, OCR receipts, and “Approve & Post”.
  - Admin/Owner: can access `/expense-items`, edit an item (PUT succeeds), and delete (soft-delete).

### Phase 11 — Port deprecated domain “admin pages” into the user UI ✅ COMPLETE

Goal: pages in `src/app/domain/frontend/expenses/pages/admin/*` are no longer reachable from `/admin/*`. Any domain functionality we still want must be exposed via app routes + user pages, gated by tenant role.

- [x] Decide which pages from `pages/admin/*` are needed in the app build (power-user pages) vs can stay deprecated reference only.
- [x] Add app routes for the ported pages in `src/app/domain/frontend/expenses/routes/user.cljs` (keep route order above `/expenses/:expense-id`).
- [x] Create user pages in `src/app/domain/frontend/expenses/pages/user/` for the ported admin pages and wrap each with `expenses-page-guard` + capability.
- [x] Wire view keywords in `src/app/domain/frontend/expenses/pages.cljs`.
- [x] Add power-user navigation links in `src/app/template/frontend/components/layout.cljs` (includes “Expense Items” for admin/owner).
- [x] Add missing user API endpoints in `src/app/domain/backend/expenses/routes/user_api.clj` and enforce role gating (403) for privileged operations.

### Phase 12 — Delete legacy `pages/admin/*` + deprecated admin routes ✅ COMPLETE

- [x] Deleted `src/app/domain/frontend/expenses/pages/admin/*` (16 files)
- [x] Deleted `src/app/domain/frontend/expenses/routes.cljs`

## Backend Alignment Check (what already exists)

User-facing API routes already include role-gated operations:

- `src/app/domain/backend/expenses/routes/user_api.clj`
  - Supplier purge endpoints exist (intended admin/owner only).
  - Articles + unmapped items routes are explicitly “role-gated to admin/owner”.

This is good: the backend is already moving toward “tenant admin/owner power-user” rather than “admin panel domain UI”.

Action item: audit that every privileged UI operation hits a user API endpoint that enforces role gating (403), and that no user page depends on `/admin/api/expenses/*`.

---

## Implementation Plan (phased)

### Phase 0 — Inventory + decisions (fast, no behavior changes)

- [ ] List all current Expenses-related routes:
  - App routes (`src/app/domain/frontend/expenses/routes/user.cljs`)
  - Admin routes (removed; previously `src/app/domain/frontend/expenses/routes.cljs`)
- [ ] Decide the access matrix above (finalize per route + per action).
- [ ] Identify any “missing” power-user pages currently only available in `pages/admin/*` and decide:
  - Port into app build (behind role gating), or
  - Remove as unnecessary.

### Phase 1 — Define a single “Expenses authz” API (frontend)

- [ ] Create a domain-local helper namespace (example name):
  - `src/app/domain/frontend/expenses/authz.cljs`
- [ ] Implement:
  - `normalize-role` (string)
  - `power-user?` (admin/owner)
  - `can?` (capability-based; uses role for now)
- [ ] Add re-frame subs that are easy to consume:
  - `[:expenses/can? :expenses/unmapped.access]`
  - `[:expenses/power-user?]`
  - (or keep it purely functional if you prefer)

Goal: stop re-implementing role parsing/checks across pages.

### Phase 2 — Remove admin-panel access to domain pages

- [ ] Remove Expenses domain routes from the admin router:
  - `src/app/admin/frontend/routes.cljs` (stop requiring/merging `app.domain.frontend.expenses.routes`)
- [ ] Remove the “Expenses Domain” section from admin navigation:
  - `src/app/admin/frontend/components/layout.cljs`
- [ ] Ensure no other admin code depends on those routes (search `/admin/expenses` etc).

Acceptance: Visiting `/admin/expenses` should 404 (or route-miss) and the admin sidebar should not show domain items.

### Phase 3 — Route/page gating in the app build (consistent UX)

Pick one approach and use it consistently:

**Option A (recommended): page-level guard**
- Wrap restricted page components with `auth-guard/role-based-guard`.
- Example pattern already exists:
  - `src/app/domain/frontend/expenses/pages/user/unmapped_items.cljs`

**Option B: controller-level redirect**
- Add controller start logic to check capability and dispatch `:navigate-to` if denied.
- Pro: less flicker, route never “enters”.
- Con: depends on session being present at controller time; needs careful handling during auth bootstrap.

Tasks:
- [ ] Apply gating to any admin/owner-only pages (and any new power-user routes you add).
- [ ] Ensure `unassigned` users consistently land in `/waiting-room` when hitting expenses routes.

### Phase 4 — Action-level gating (extra functionality for admin/owner)

- [ ] Replace inline role checks with `authz/can?` (or subs) across user pages.
- [ ] For shared pages:
  - Show read-only UI for viewers.
  - Hide/disable destructive or power-user actions for non-admin/owner.
- [ ] Remove all links from user pages to admin panel routes:
  - Example: fix “View all” links in `src/app/domain/frontend/expenses/pages/user/suppliers.cljs` to point to app routes (or introduce new app pages if needed).

### Phase 5 — Backend verification + hardening

- [ ] Ensure every privileged user action calls a user API endpoint that enforces role checks.
- [ ] Add/verify 403 behavior for:
  - Supplier purge endpoints
  - Articles/unmapped endpoints
  - Any “danger zone” endpoints you introduce (e.g. delete-all)
- [ ] Confirm no `/admin/api/expenses/*` endpoints are required for app build flows.

### Phase 6 — Cleanup, tests, and documentation

- [x] Removed now-unused admin-page code:
  - Deleted `src/app/domain/frontend/expenses/pages/admin/*`
  - Deleted `src/app/domain/frontend/expenses/routes.cljs`
- [ ] Add focused tests:
  - FE: ensure restricted routes render access denied/redirect for non-admin.
  - BE: ensure privileged endpoints return 403 for non-admin.
- [ ] Update docs (optional but recommended):
  - Add a short “Expenses roles & capabilities” note under `docs/domain/` (for example `docs/domain/expenses/index.md`).

---

## Validation / Acceptance Criteria

### App build (tenant users)

- Unassigned user:
  - Hitting `/expenses` (or other expenses routes) consistently ends at `/waiting-room`.
- Viewer user:
  - Can view dashboard/list/detail, but cannot mutate (no add/edit/delete actions).
- Member user:
  - Can create/edit expenses and reference data within allowed scope.
- Admin/Owner user:
  - Can access power-user routes (e.g. `/unmapped-items`) and sees extra actions on shared pages.

### Admin panel

- Admin sidebar contains no domain entries.
- `/admin/expenses` and related domain routes are not reachable from the admin UI.

### Security

- All privileged operations return 403 server-side when performed by a user without the required role/capability.

---

## Open Questions (decide before implementation)

1) Should `"viewer"` be allowed to upload receipts / run OCR, or is that a “write” capability?
2) Do you want to keep `/unmapped-items` at the root, or move it under `/expenses/unmapped-items` (with redirect)?
3) Are there any domain admin pages (articles, aliases, price observations) that must exist as **tenant power-user pages**? If yes, which routes and which APIs should back them?
