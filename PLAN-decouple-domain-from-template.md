<!-- ai: {:tags [:architecture :domain :template :plan] :kind :plan} -->

# PLAN — Decouple Domain Code from Template/Shared

## Goal
Make `app.template` + `app.shared` domain-agnostic so new apps can be built by changing **only** `src/app/domain/**` (plus `resources/db/domain/**`), without editing template/admin/shared.

## Core rule (dependency direction)
- ✅ Domain **may depend on** template/shared/admin (reuse infra/components).
- ❌ Template/shared/admin **must not depend on** any concrete domain (e.g. `expenses`).

## Current coupling inventory (Expenses → Template)

### Backend coupling
- `src/app/template/backend/routes/admin_api.clj`
  - Directly requires/mounts `app.domain.backend.expenses.routes.core`.
- `src/app/template/backend/routes/api.clj`
  - Directly requires/mounts `app.domain.backend.expenses.routes.user-api`.
  - Hard-codes expenses domain UI-config file paths and loads them into `/api/v1/config` as `:domain-ui-config`.
- `src/app/template/backend/routes/admin/settings_io.clj`
  - Hard-codes expenses domain-owned UI config EDN paths for `/admin/user-settings`.

### Frontend coupling
- `src/app/template/frontend/routes/data.cljs`
  - Hard-coded user-facing `/expenses/*` routes.
- `src/app/template/frontend/core.cljs`
  - Directly requires and renders `app.domain.frontend.expenses.pages.user.*` pages.
- `src/app/template/frontend/events/routing.cljs`
  - Registers expense-specific page init events (`:page/init-expenses-*`) and dispatches `:user-expenses/*` events.
- `src/app/template/frontend/events/user_expenses/**`
  - Entire user-expenses feature lives under template.

### Other coupling
- `src/app/template/frontend/components/form/master_detail.cljs`
  - Special-cases `:expense/id` and `:expenses/id` when matching IDs.

### Shared folder
- `src/app/shared/specs/*` expenses mentions appear only in comments/examples (ok; should be genericized).

## Target end state

### `app.shared`
- Only reusable, domain-neutral helpers/specs.
- Comments/examples use `src/app/domain/frontend/<domain>/config/*.edn` rather than `expenses`.

### `app.template`
- Provides mechanisms (routing composition, settings/config loading, reusable UI patterns).
- No concrete domain requires, no domain file paths, no domain endpoints.
- Consumes “domain contributions” via explicit extension points.

### `app.domain`
Owns all domain functionality:
- Backend routes (admin + optional user API).
- Frontend routes/pages/events (admin + optional user-facing app).
- Domain-owned UI config EDNs.
- Domain registry/manifest that the template reads.

## Plan (phased)

### Phase 0 — Define a domain manifest contract (BE + FE)
Define an explicit contract for what a domain contributes.

Backend manifest responsibilities:
- Provide **admin API routes** (reitit route vector, mounted under `/admin/api/<domain>`).
- Provide **user/public API routes** (reitit route vector, mounted under `/api/v1/<domain>`).
  - Template injects auth middleware so the domain doesn’t depend on template middleware namespaces.
- Provide **domain-owned user UI config bundle paths** for:
  - `entities.edn`, `view-options.edn`, `form-fields.edn`, `table-columns.edn`.

Frontend manifest responsibilities:
- Provide **user-facing routes** (reitit frontend routes) with:
  - `:name`, `:path`, `:controllers`, and crucially `:view` as an actual UIx component fn.
- Optionally provide:
  - navigation items (so the shell can build menus without hard-coded domain links).

Deliverable:
- A short doc (or code comment) describing the manifest shape + expectations.

---

### Phase 1 — Add registries in `src/app/domain/**`
Add two small registries so swapping domains means editing only domain code.

- `app.domain.backend.registry` (Clojure)
  - Returns enabled domain modules/manifests.
- `app.domain.frontend.registry` (ClojureScript)
  - Returns enabled domain modules/manifests.

Initial state: registry returns only `:expenses`.

---

### Phase 2 — Backend: remove template → expenses imports
Refactor template backend composition to depend only on the registry.

1) `src/app/template/backend/routes/admin_api.clj`
- Replace direct expenses require with: collect domain admin routes from `app.domain.backend.registry`.

2) `src/app/template/backend/routes/api.clj`
- Replace direct user-expenses require with: collect domain user-api routes from registry.
- Replace `load-expenses-domain-ui-config` with: load domain-ui-config bundle using registry-provided paths.

3) `src/app/template/backend/routes/admin/settings_io.clj`
- Replace hard-coded domain EDN paths with: resolve active domain bundle paths via registry.

Optional extension:
- Keep current single active “User UI Config” scope.
- Later add `?domain=<id>` to support multiple domains without breaking existing UI.

---

### Phase 3 — Frontend: move user-facing expenses feature into domain

1) Routes
- Move `/expenses/*` routes out of `app.template.frontend.routes.data` into domain.
- Build template routes as:
  - base template routes + domain user routes (from `app.domain.frontend.registry`) + admin routes.

2) Views
- Domain routes attach `:view` as a UIx component fn.
- Update `app.template.frontend.core/current-page` to render `route-view` for non-admin routes too.
- Remove direct requires of `app.domain.frontend.expenses.pages.user.*` from template.

3) Events
- Move `src/app/template/frontend/events/user_expenses/**` into domain.
- Remove `:page/init-expenses-*` template routing events; domain route controllers dispatch domain events directly.

---

### Phase 4 — Remove remaining expenses-specific code from template/shared
- `master-detail-form`: replace `:expense/id` / `:expenses/id` special casing with a generic ID extractor.
- Template icons: move `expenses-icon` into domain (or rename as generic icon + domain alias).
- Shared specs comments: replace “expenses” examples with `<domain>` placeholder.

---

### Phase 5 — Prove separation with a dummy domain dry run
Create a tiny “dummy” domain and enable it by changing only the registries:
- 1 admin API route
- 1 user-facing route + page
- minimal user-ui-config bundle

If this works without touching template/admin/shared, the architecture is genuinely reusable.

## Acceptance checklist (definition of done)
- `src/app/template/**` has **no** `app.domain.*` requires and no hard-coded domain config paths.
- `src/app/shared/**` has no domain-specific code (examples are generic).
- Expenses still works when enabled via registry.
- Swapping to a new domain requires changing only `src/app/domain/**`.
- `/api/v1/config` still returns `:domain-ui-config` (via registry, not hard-coded expenses).
- `/admin/user-settings` still reads/writes the domain-owned config bundle (via registry, not hard-coded expenses).

## Risks / tricky parts
- Template `current-page` currently hard-codes domain user pages.
  - Mitigation: route-provided `:view` function becomes the standard rendering path.
- “User UI config” scope assumes a single active domain.
  - Mitigation: keep single active domain initially; add `?domain=` later if needed.
- Route conflicts (`/expenses/new` vs `/expenses/:id`).
  - Mitigation: keep literal-over-param conflict strategy and preserve route ordering.
- Regression risk: coupling sneaks back in.
  - Mitigation (later): add a grep-based CI check preventing `src/app/template/**` from referencing `src/app/domain/**` directly.
