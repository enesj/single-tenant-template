<!-- ai: {:tags [:architecture :domain :template :plan] :kind :plan} -->

# PLAN — Decouple Domain Code from Template/Admin/Shared

> **STATUS: COMPLETED (Registry/manifest plumbing)** (2025-12-18)
>
> Follow-up plan for remaining concrete coupling removal:
> - `PLAN-decouple-domain-from-template-followup.md`
>
> This plan’s registry/manifest work has been implemented. Key changes:
> - Created `app.domain.backend.registry` and `app.domain.frontend.registry` 
> - Backend routes now use registry for domain API routes, UI configs, SPA routes, and OAuth redirects
> - Frontend routes now use registry for user routes and entity initialization
> - Admin entity registry merges domain entities from registry
> - Domain groups/settings come from domain registry
> - Circular dependencies resolved by:
>   - Admin routes imported directly in admin/frontend/routes.cljs (not via registry)
>   - Domain pages imported directly in template/frontend/core.cljs (not via registry)
>   - User routes provided via registry (no circular deps)

## Goal
Make `app.template`, `app.admin`, and `app.shared` domain-agnostic so new apps can be built by changing only:

- `src/app/domain/**`
- `resources/db/domain/**`
- (and the runtime-editable UI config EDNs that live under `src/app/domain/**/config/*.edn`)

…without editing template/admin/shared code.

## Core rule (dependency direction)
- ✅ Concrete domains (e.g. `expenses`) may depend on template/shared/admin.
- ❌ Template/shared/admin must not depend on any *concrete* domain.
- ✅ Template/admin may depend on a stable **domain extension interface** (manifests/registries), as long as it is not domain-specific.

**Decision needed (pick one and reflect it in the checklist):**
1) **Registry import allowed (simpler):** template/admin require `app.domain.*.registry` and never require `...expenses...` directly.
2) **Zero domain imports (stricter):** create an app-level composition layer that requires both template/admin and domain registries, and passes manifests into template/admin via pure functions.

This plan is written assuming **(1)**. If you want **(2)**, Phase 1 adds a composition namespace and template/admin route namespaces become constructors (no registry requires).

## Current coupling inventory (Expenses → Template/Admin/Shared)

### Backend coupling
- `src/app/template/backend/routes/admin_api.clj`
  - Directly requires/mounts `app.domain.backend.expenses.routes.core`.
- `src/app/template/backend/routes/api.clj`
  - Directly requires/mounts `app.domain.backend.expenses.routes.user-api`.
  - Hard-codes expenses domain UI-config file paths and loads them into `/api/v1/config` as `:domain-ui-config`.
- `src/app/template/backend/routes/admin/settings_io.clj`
  - Hard-codes expenses domain-owned UI config EDN paths for `/admin/user-settings`.
- `src/app/template/backend/routes.clj`
  - Hard-codes SPA GET routes for `/expenses/*` so `index.html` is served.
- `src/app/template/backend/routes/oauth.clj`
  - Hard-codes post-auth redirect path to `\"/expenses\"`.

### Template frontend coupling (user app)
- `src/app/template/frontend/routes/data.cljs`
  - Hard-coded user-facing `/expenses/*` routes.
- `src/app/template/frontend/core.cljs`
  - Directly requires and renders `app.domain.frontend.expenses.pages.user.*` pages.
- `src/app/template/frontend/events/routing.cljs`
  - Registers expense-specific page init events (`:page/init-expenses-*`) and dispatches `:user-expenses/*` events.
- `src/app/template/frontend/events/user_expenses/**`
  - Entire user-expenses feature lives under template.
- `src/app/template/frontend/subs/user_expenses.cljs`
  - Expense-specific subscriptions live under template.

### Admin frontend coupling
- `src/app/admin/frontend/routes.cljs`
  - Directly requires `app.domain.frontend.expenses.routes` and splices domain routes into admin router.
- `src/app/admin/frontend/system/entity_registry.cljs`
  - Hard-codes expenses admin entities and requires expenses adapters.
- `src/app/admin/frontend/adapters/expenses/**`
  - Entire expenses admin adapter layer lives under admin.
- `src/app/admin/frontend/subs/expenses.cljs`
  - Expense-specific admin subscriptions live under admin.
- `src/app/admin/frontend/config/*.edn`
  - Admin UI config bundles contain expenses entity keys and settings (`:expenses`, `:suppliers`, etc.).
- Settings UI domain grouping is hard-coded to `:expenses`:
  - `src/app/admin/frontend/pages/settings/constants.cljs`
  - `src/app/admin/frontend/pages/settings/tabs.cljs`
  - `src/app/admin/frontend/settings/definitions.cljs`

### Other coupling
- `src/app/template/frontend/components/form/master_detail.cljs`
  - Special-cases `:expense/id` and `:expenses/id` when matching IDs.

### Shared folder
- `src/app/shared/specs/*` expenses mentions appear only in comments/examples (OK, but should be genericized).

## Target end state

### `app.shared`
- Only reusable, domain-neutral helpers/specs.
- Examples use `<domain>` placeholders (no `expenses` in docs).

### `app.template` (user app shell)
- Provides mechanisms (routing composition, config loading, reusable UI patterns).
- No concrete domain requires, no concrete domain file paths, no concrete domain endpoints.
- User-facing pages/events/subs live in domain.

### `app.admin` (admin shell)
- Provides generic admin panel + settings UI.
- Domain admin entities/routes/adapters/config are contributed via extension points.
- No concrete domain requires, no concrete domain config baked in.

### `app.domain`
Owns all domain functionality:
- Backend routes (admin + optional user API).
- Frontend routes/pages/events/subs (user + optional admin extensions).
- Domain-owned UI config EDNs (user settings + optional admin settings additions).
- Domain registry/manifest that the shells read.

## Plan (phased)

### Phase 0 — Decide extension boundary + manifest contract (BE + FE)
Pick decision (1) or (2) above and lock the “allowed imports” rule.

Define a single manifest shape (nested maps) so both CLJ and CLJS agree on domain id and contributions.

**Backend manifest (CLJ):**
- `:id` keyword (e.g. `:expenses`)
- `:routes`
  - `:admin-api` — fn `(fn [db service-container] reitit-routes)` mounted under `/admin/api/<id>`
  - `:user-api` — fn `(fn [db user-auth-mw] reitit-routes)` mounted under `/api/v1/<id>`
- `:ui-config`
  - `:user` — `{:root-dir ...}` or `{:paths {...}}` for `entities/view-options/form-fields/table-columns`
- `:redirects`
  - `:post-login-path` (prefer `\"/\"` or `\"/home\"` unless domain truly needs otherwise)

**Frontend manifest (CLJS):**
- `:routes`
  - `:user` — reitit frontend route vector with `:view` fns
  - `:admin` (optional) — reitit frontend route vector under `/admin/*` (if domain adds admin pages)
- `:init!`
  - fn that ensures domain event/sub namespaces are loaded/registered (re-frame side effects).
- `:admin-ui-config` (optional)
  - domain additions for admin settings groupings / entity registry metadata, or pointers to server-provided bundles

Deliverable: add a doc section (in this plan or a code comment) with the manifest keys + 1 concrete example.

---

### Phase 1 — Add registries in `src/app/domain/**`
Add two small registries so swapping domains means editing only domain code.

- `app.domain.backend.registry` (Clojure)
  - `enabled-domains` returns `[manifest ...]` (initially only `:expenses`).
- `app.domain.frontend.registry` (ClojureScript)
  - same, split by `:routes` / `:init!` / config contributions.

(If using decision (2), these registries are required only by the app-level composition layer, not by template/admin.)

---

### Phase 2 — Backend: remove concrete domain imports + hard-coded SPA paths
1) `src/app/template/backend/routes/admin_api.clj`
- Replace direct expenses require with: collect admin API routes from backend registry.

2) `src/app/template/backend/routes/api.clj`
- Replace direct user-expenses require with: collect domain user API routes from registry.
- Replace `load-expenses-domain-ui-config` with: load `:ui-config :user` from the manifest.

3) `src/app/template/backend/routes/admin/settings_io.clj`
- Replace hard-coded domain EDN paths with manifest-provided `:ui-config :user`.

4) `src/app/template/backend/routes.clj`
- Remove hard-coded `/expenses/*` SPA routes.
  - Prefer a generic SPA fallback GET route (catch-all) for non-API/non-admin paths, **or**
  - generate SPA paths from registry-provided route paths.

5) `src/app/template/backend/routes/oauth.clj`
- Replace `redirect-url \"/expenses\"` with `(:redirects :post-login-path)` from manifest, or default to `\"/\"`.

Optional extension:
- Keep current single active “User UI Config” scope.
- Later add `?domain=<id>` to support multiple domains without breaking existing UI.

---

### Phase 3 — Template frontend (user app): move expenses feature into domain
1) Routes
- Move `/expenses/*` routes out of `app.template.frontend.routes.data` into domain manifest.
- Build routes as: base template routes + domain user routes + admin routes.

2) Views
- Domain routes attach `:view` as a UIx component fn.
- Update `app.template.frontend.core/current-page` to render the route-provided view for user routes (not just admin).
- Remove direct requires of `app.domain.frontend.expenses.pages.user.*` from template.

3) Events & subs
- Move `src/app/template/frontend/events/user_expenses/**` and `src/app/template/frontend/subs/user_expenses.cljs` into domain.
- Ensure these namespaces are loaded via domain `:init!` (or by requiring them from the domain routes ns).
- Remove expense-specific `:page/init-expenses-*` template routing events; domain route controllers dispatch domain events directly.

---

### Phase 4 — Admin frontend: make admin shell domain-agnostic
1) Routes
- Replace direct `app.domain.frontend.expenses.routes` require in `src/app/admin/frontend/routes.cljs` with domain admin routes from registry/manifest.

2) Admin entity registry + adapters
- Move `src/app/admin/frontend/adapters/expenses/**` and `src/app/admin/frontend/subs/expenses.cljs` into domain (domain-owned admin extension).
- Refactor `src/app/admin/frontend/system/entity_registry.cljs` so system entities are hard-coded, and domain entities come from manifests.

3) Admin config EDNs + settings UI
- Split admin UI config bundles into:
  - base/system config under `src/app/admin/frontend/config/*.edn` (users/admins/audit/login-events)
  - domain config under `src/app/domain/**/admin/config/*.edn` (or similar)
- Update settings grouping (`constants.cljs` / `tabs.cljs` / `settings/definitions.cljs`) to render domain groups from manifest, not hard-coded `:expenses`.

---

### Phase 5 — Remove remaining expenses-specific code from template/shared
- `master-detail-form`: replace `:expense/id` / `:expenses/id` special casing with a generic ID extractor.
- Shared specs comments: replace “expenses” examples with `<domain>` placeholder.

---

### Phase 6 — Prove separation + prevent regressions
Create a tiny “dummy” domain and enable it by changing only domain registries:
- 1 admin API route
- 1 user-facing route + page
- minimal user-ui-config bundle
- (optional) 1 admin extension route + entity

Add a CI guard (grep-based) that fails if `src/app/template/**` or `src/app/admin/**` references `app.domain.*.<concrete-domain>` or hard-codes `\"/expenses\"` paths.

## Acceptance checklist (definition of done)
- `src/app/template/**` contains no concrete domain references (no `expenses` routes/pages/events/subs/config paths).
- `src/app/admin/**` contains no concrete domain references (no `expenses` routes/adapters/subs/config/groups).
- `src/app/shared/**` contains no domain-specific code (examples are generic).
- Expenses still works when enabled via registries.
- Swapping to a new domain requires changing only `src/app/domain/**` and `resources/db/domain/**` (plus regenerated artifacts, if any).
- `/api/v1/config` still returns `:domain-ui-config` via manifest/registry (not hard-coded expenses).
- `/admin/user-settings` still reads/writes the domain-owned config bundle via manifest/registry (not hard-coded expenses).

## Risks / tricky parts
- CLJS event/sub registration is load-order sensitive.
  - Mitigation: add `:init!` hook to the domain manifest and call it on app startup.
- Backend SPA route handling: catch-all must not shadow `/api` and `/admin/api`.
  - Mitigation: route ordering + explicit admin/api routes first.
- Config editing: splitting admin config into base vs domain requires careful write-back.
  - Mitigation: decide ownership per entity key (manifest lists domain entities).
- Route conflicts (`/expenses/new` vs `/expenses/:id`).
  - Mitigation: keep literal-over-param conflict strategy and preserve route ordering.
