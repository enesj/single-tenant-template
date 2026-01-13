# PLAN — Expenses domain pages: role/capability gating + remove admin-panel access

**Created**: 2026-01-13  
**Updated**: 2026-01-13  
**Status**: Proposed (no code changes yet)

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

- **Admin panel imports domain routes**:
  - `src/app/admin/frontend/routes.cljs` requires `app.domain.frontend.expenses.routes` and merges its routes.
  - Admin sidebar shows an “Expenses Domain” section:
    - `src/app/admin/frontend/components/layout.cljs`
- **Domain “admin pages”** exist under:
  - `src/app/domain/frontend/expenses/pages/admin/*`
  - Wired via:
    - `src/app/domain/frontend/expenses/routes.cljs` (routes under `/admin/...` guarded by `:admin/check-auth-protected`)
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

### Gaps / footguns (examples)

- Some user pages still reference **admin panel paths**:
  - `src/app/domain/frontend/expenses/pages/user/suppliers.cljs` links to `/admin/expenses`, `/admin/article-aliases`, `/admin/price-observations` via “View all” links.
- Role checks are duplicated/inconsistent:
  - `suppliers.cljs` treats `"member"|"admin"|"owner"` as modifiable.
  - `payers.cljs` treats `"member"|"admin"` as modifiable (likely should include `"owner"` too).
- Some route init events gate “unassigned” but not consistently across all pages:
  - `src/app/template/frontend/events/routing.cljs` (some pages redirect to `/waiting-room`, others defer to page components).

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

## Route & Page Access Matrix (proposed)

Adjust this matrix to your product rules; it’s meant to make decisions explicit.

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
| `/expenses/upload` | receipt upload | `member/admin/owner` (or allow `viewer` if desired) | Decide whether upload is a “write” capability. |
| `/receipts` | receipts inbox | `viewer/member/admin/owner` | Gate per-action: approve/OCR might be power-user only if desired. |
| `/receipts/:receipt-id` | receipt detail | `viewer/member/admin/owner` | Mirrors `/receipts`. |
| `/expenses/reports` | reports | `viewer/member/admin/owner` | Exports could be power-user only if needed. |
| `/expenses/settings` | settings | `viewer/member/admin/owner` | Danger zone actions gated. |
| `/suppliers` | suppliers | `viewer/member/admin/owner` | Add/edit/archive gated; purge admin/owner only. |
| `/payers` | payers | `viewer/member/admin/owner` | Add/edit/delete gated; align owner behavior. |
| `/unmapped-items` | unmapped items | `admin/owner` | Already implemented with `role-based-guard`. |

### Admin panel routes (to be removed)

Defined in `src/app/domain/frontend/expenses/routes.cljs` and linked in `src/app/admin/frontend/components/layout.cljs`:

- `/admin/expenses`, `/admin/receipts`, `/admin/suppliers`, `/admin/payers`, `/admin/articles`, `/admin/expense-items`, `/admin/article-aliases`, `/admin/unmapped-items`, `/admin/price-observations`, etc.

Plan: remove these routes and links entirely.

---

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
  - Admin routes (`src/app/domain/frontend/expenses/routes.cljs`)
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

- [ ] Remove or deprecate now-unused code under:
  - `src/app/domain/frontend/expenses/pages/admin/*`
  - `src/app/domain/frontend/expenses/routes.cljs`
  - (only after verifying nothing imports them)
- [ ] Add focused tests:
  - FE: ensure restricted routes render access denied/redirect for non-admin.
  - BE: ensure privileged endpoints return 403 for non-admin.
- [ ] Update docs (optional but recommended):
  - Add a short “Expenses roles & capabilities” note under `docs/frontend/` or domain docs.

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

