# Create `/admin/search` with tenant-search parity (global scope)

## Summary
Add a new admin page at `/admin/search` that provides **the same user experience and functionality as the tenant search page** at `/search`, but runs against **global data** instead of tenant-scoped data.

The codebase already has:
- a full **tenant search UI + events + related-detail flow**
- an **admin backend search endpoint** at `/admin/api/expenses/search`

What is missing is the admin frontend route/page wiring and the admin equivalent of the user `search/related` flow. The implementation should reuse as much of the tenant search UI as possible while preserving the admin shell and admin auth model.

## Implementation Changes

### 1) Add admin route for `/admin/search`
Update the admin frontend router so the admin app has a dedicated route:
- Path: `/admin/search`
- Route name: `:admin-search`
- View: new admin search page/view
- Controller: admin guarded route

Likely file:
- `src/app/admin/frontend/routes.cljs`

Also ensure the route is included in any admin navigation/menu that should expose it.

### 2) Create an admin search page with the same UI/behavior as tenant search
Build an admin page that matches the tenant search page behavior exactly:
- same search box UX
- same grouped cross-entity result sections
- same card/result presentation
- same slide-in detail panel
- same related-records loading behavior
- same minimum query length / debounce behavior
- same empty/loading/result states

Prefer reusing the existing tenant search page implementation rather than duplicating a 1:1 copy.

Candidate approaches:
- **Preferred:** extract the current tenant search page into a reusable shared component that accepts configuration
  - API endpoints
  - init event
  - subscription keys
  - dispatch event names
  - optional page wrapper/layout
- **Fallback:** create an admin-specific page by copying the tenant page and swapping event/subscription namespaces

Relevant existing files:
- `src/app/domain/frontend/expenses/pages/user/search.cljs`
- `src/app/domain/frontend/expenses/pages.cljs`
- `src/app/template/frontend/components/layout.cljs`

### 3) Add admin search frontend state/events
Create admin-side events/subscriptions with the same behavior as user search, but targeting admin endpoints.

Needed behavior:
- initialize admin search state on page entry
- debounce search input
- fetch results from admin endpoint
- select a result
- fetch related records for selected result
- clear selection
- manage `loading?`, `related-loading?`, `results`, `selected`, `related`

Likely new files:
- `src/app/admin/frontend/events/search.cljs`
- `src/app/admin/frontend/subs/search.cljs`

Or colocate under an admin expenses namespace if that matches current conventions.

Admin API targets should be:
- `/admin/api/expenses/search`
- `/admin/api/expenses/search/related`

### 4) Add missing admin related-records backend endpoint
The backend currently has:
- `admin-search-handler`
- `user-related-handler`

It does **not** currently have an admin equivalent for related detail loading.

Add:
- `admin-related-handler`

Behavior:
- same entity-type support as `user-related-handler`
- same response shape
- same related detail richness
- **no tenant filter**

Likely file:
- `src/app/domain/backend/expenses/handlers/search.clj`

Implementation approach:
- reuse the existing `related-for-*` helper fns
- call them with `tenant-id = nil`
- keep request validation and error handling parallel to the user handler

### 5) Expose admin related endpoint in admin expenses routes
Add route:
- `GET /admin/api/expenses/search/related`

Likely file:
- `src/app/domain/backend/expenses/routes/core.clj`

This should sit beside the existing admin search route.

### 6) Add admin page initialization event
If needed, add a page init event for admin search to keep routing lifecycle explicit and consistent.

Possible options:
- a dedicated `:page/init-admin-search`
- or admin route controller dispatching directly into admin search init

Keep it aligned with existing admin route/controller patterns in:
- `src/app/admin/frontend/routes.cljs`
- `src/app/template/frontend/events/routing.cljs` (only if shared routing machinery is needed)

### 7) Preserve admin shell/layout
The admin page should render inside the admin shell, not the tenant/user shell.

Requirements:
- keep admin top-level layout/navigation
- respect admin auth guard behavior
- avoid tenant route helpers like `th "/search"`
- use admin route navigation conventions

### 8) Keep search global, not tenant-scoped
This is the core behavioral difference.

Admin search must:
- return global results across all tenants/data where applicable
- use the existing admin backend search handler semantics
- use admin related detail queries without tenant filtering

Be careful that any reused user code does **not** implicitly assume:
- tenant session membership
- user-scoped endpoints
- user route helpers
- `:user-expenses/*` event/subscription keys

### 9) Add admin navigation entry if desired/required
If admin search should be user-discoverable from the admin UI, add a nav item or action entry for it.

Likely file(s):
- admin layout/nav components under `src/app/admin/frontend/**`

Do not accidentally add it to the tenant navigation in:
- `src/app/template/frontend/components/layout.cljs`

## Suggested File Touch List

### Backend
- `src/app/domain/backend/expenses/handlers/search.clj`
  - add `admin-related-handler`
- `src/app/domain/backend/expenses/routes/core.clj`
  - add `/search/related`

### Admin frontend
- `src/app/admin/frontend/routes.cljs`
  - add `/admin/search`
- `src/app/admin/frontend/events/search.cljs`
  - admin search event flow
- `src/app/admin/frontend/subs/search.cljs`
  - admin search subscriptions
- `src/app/admin/frontend/pages/search.cljs`
  - admin search page wrapper

### Reuse/refactor candidates
- `src/app/domain/frontend/expenses/pages/user/search.cljs`
  - extract reusable search UI if pursuing shared implementation
- possibly a new shared page/component, e.g.
  - `src/app/domain/frontend/expenses/pages/shared/search.cljs`
  - or `src/app/shared/frontend/...` if that better fits repo structure

## Validation Plan

### Backend validation
- confirm `/admin/api/expenses/search?q=<term>` returns grouped global results
- confirm `/admin/api/expenses/search/related?type=<type>&id=<uuid>` returns related detail payload
- verify no tenant filter is applied in admin related requests

### Frontend validation
- visit `/admin/search`
- verify admin auth guard works
- verify typing 2+ chars triggers debounced search
- verify results render in the same grouped format as tenant search
- verify selecting each supported entity type opens detail panel
- verify related panels load and render correctly for entities that support related data
- verify loading/empty/no-result states match tenant search behavior

### Minimum entity checks
Test at least:
- suppliers
- stores
- articles
- payers
- expense categories
- manufacturers
- categories
- subcategories
- cities

### Regression checks
- tenant `/search` continues to work unchanged
- existing admin entity pages remain unaffected
- no tenant-only route/layout leaks into admin UI

## Notes / Constraints
- Reuse is strongly preferred over wholesale copy/paste.
- Keep response shapes identical between user and admin search flows where possible.
- If the tenant page currently mixes UI with user-specific event names too tightly, first extract a configurable shared search experience, then wire user/admin variants on top.
- Avoid changing the user-visible behavior of tenant search except where needed for reuse/refactor safety.
