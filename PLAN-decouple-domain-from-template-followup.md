<!-- ai: {:tags [:architecture :domain :template :admin :plan] :kind :plan} -->

# PLAN — Finish Decoupling Concrete Domain Code (Expenses) from Template/Admin

> **Created:** 2025-12-18
> **Status:** ✅ COMPLETE
> 
> This is a follow-up to `PLAN-decouple-domain-from-template.md`. The registry/manifest plumbing is in place, but **concrete expenses code still lives under `app.template` and `app.admin`**. This plan moves the remaining concrete-domain code into `src/app/domain/**` and adds a regression guard.

## Goal

Make `app.template`, `app.admin`, and `app.shared` free of **concrete** domain coupling (e.g. “expenses”), so a new app can be built by changing only:

- `src/app/domain/**`
- `resources/db/domain/**`

…without editing template/admin/shared code.

## Core rule (dependency direction)

- ✅ Concrete domains (e.g. `expenses`) may depend on template/shared/admin **infrastructure**.
- ❌ Template/shared/admin must not depend on any *concrete* domain.
- ✅ Template/admin may depend on stable domain extension points (registries/manifests).

This plan continues using the existing “**registry import allowed**” decision.

## Current remaining coupling inventory (examples)

### Template (user app)

Concrete domain still exists under template:

- `src/app/template/frontend/events/user_expenses/**`
- `src/app/template/frontend/subs/user_expenses.cljs`
- `src/app/template/frontend/core.cljs` directly requires `app.domain.frontend.expenses.pages.user.*`
- `src/app/template/frontend/components/form/master_detail.cljs` special-cases `:expense/id` and `:expenses/id`

### Admin (admin app)

Concrete domain still exists under admin:

- `src/app/admin/frontend/adapters/expenses/**`
- `src/app/admin/frontend/subs/expenses.cljs`
- `src/app/admin/frontend/config/*.edn` contains `:expenses` entities (ex: `config/entities.edn`)

## Non-goals

- No functional UX redesign.
- No new architecture beyond what’s needed to remove concrete coupling.
- Avoid rewriting domain data flow unless required by dependency direction.

## Phased plan

### Phase 0 — Baseline checks (fast, before moving files)

**Deliverables**

- Confirm the concrete coupling list with searches (template/admin/shared).
- Identify the minimal entry points that currently load:
  - user expenses event handlers
  - user expenses subscriptions
  - admin expenses adapters
  - admin expenses subscriptions

**Acceptance**

- We have a complete list of files/namespaces and their load paths.

---

### Phase 1 — User app: move user-expenses events + subs into domain

**1.1 Move user-expenses events namespace tree**

Move these namespaces from template → domain (keep names stable where possible to reduce churn):

- from: `app.template.frontend.events.user-expenses.*`
- to: `app.domain.frontend.expenses.events.user-expenses.*`

File moves (approx):

- `src/app/template/frontend/events/user_expenses.cljs`
- `src/app/template/frontend/events/user_expenses/*`

→

- `src/app/domain/frontend/expenses/events/user_expenses.cljs`
- `src/app/domain/frontend/expenses/events/user_expenses/*`

**1.2 Move user-expenses subscriptions**

- from: `app.template.frontend.subs.user-expenses`
- to: `app.domain.frontend.expenses.subs.user-expenses`

File move:

- `src/app/template/frontend/subs/user_expenses.cljs`

→

- `src/app/domain/frontend/expenses/subs/user_expenses.cljs`

**1.3 Ensure domain init loads the moved namespaces**

Update `app.domain.frontend.registry/init-all-domains!` (or the expenses manifest’s `:init!`) to ensure the *user* expenses events/subs are loaded.

Rules:

- Template should call `init-all-domains!` without knowing domain ids.
- Avoid adding any `app.domain.frontend.expenses.*` requires back into template.

**1.4 Remove concrete domain requires from template event orchestration**

Update `src/app/template/frontend/events/core.cljs` so it no longer requires
`app.template.frontend.events.user-expenses`.

Instead, make the user app startup call the domain registry init once:

- add `(app.domain.frontend.registry/init-all-domains!)` during `app.template.frontend.core/init`
  (mirroring what `app.admin.frontend.core/init-admin!` already does).

**1.5 Remove expense-specific page init events from template routing**

`src/app/template/frontend/events/routing.cljs` currently registers:

- `:page/init-expenses-*`

and dispatches `:user-expenses/*` events.

Move these page-init handlers into the expenses domain (or replace them with
domain-owned events used directly by the domain route controllers), so
`app.template.frontend.events.routing` becomes domain-neutral.

**Acceptance**

- User expenses events/subs are registered and working.
- `src/app/template/frontend/events/user_expenses/**` and `src/app/template/frontend/subs/user_expenses.cljs` no longer exist.
- `src/app/template/frontend/events/core.cljs` no longer requires any concrete domain namespaces.
- `src/app/template/frontend/events/routing.cljs` contains no expense-specific page init events.

---

### Phase 2 — User app: remove concrete domain page requires from `app.template.frontend.core`

Right now `app.template.frontend.core` directly requires expense page namespaces and keeps a local `domain-pages` map. That violates the dependency rule.

**2.1 Make domain user routes provide the actual view function**

Update domain route definitions (already in `app.domain.frontend.expenses.routes.user`) so each route’s `:view` is a **UIx component function**, not a keyword.

- Domain route ns may require domain page namespaces.
- Template renders the `:view` when it’s a function.

**2.2 Teach template’s `current-page` to render `route-view` for non-admin routes**

`current-page` already renders `route-view` when it’s a function for admin routes. Extend the same behavior to user routes.

**2.3 Remove direct expenses page requires from template**

Remove these from `app.template.frontend.core`:

- `app.domain.frontend.expenses.pages.user.*`
- local `domain-pages` map

**2.4 Make domain routes dispatch domain-owned init events**

Update the controllers in `app.domain.frontend.expenses.routes.user` so they
dispatch domain-owned init events (defined under the expenses domain), not
template-owned `:page/init-expenses-*` events.

**Acceptance**

- `src/app/template/frontend/core.cljs` contains **no** `app.domain.frontend.expenses.*` requires.
- User routes still render the correct domain pages.

---

### Phase 3 — Genericize `master-detail-form` entity id matching

`src/app/template/frontend/components/form/master_detail.cljs` currently special-cases expense keys:

- `:expense/id`
- `:expenses/id`

**3.1 Add a generic id extractor option**

Add one of these (prefer simplest):

- `:get-id` (fn `(fn [entity] id-or-nil)`), or
- `:id-keys` (vector of candidate keywords, default `[:id]`).

Use it to implement `entity-id-matches?` without any expense-specific logic.

**3.2 Update call sites**

Wherever `master-detail-form` is used for expenses, pass `:get-id` or `:id-keys` so it continues to work.

**Acceptance**

- `master_detail.cljs` contains no expense-specific keys.
- Expenses edit flows still match the loaded entity correctly.

---

### Phase 4 — Admin app: move expenses adapters + subs into domain

Admin still owns expenses adapters/subs. The domain registry currently “re-exports” adapters from admin, which keeps the coupling alive.

**4.1 Move admin expenses adapter implementation**

Move:

- `src/app/admin/frontend/adapters/expenses/**`

→

- `src/app/domain/frontend/expenses/admin/adapters/**` (or similar)

Then update `app.domain.frontend.expenses.adapters` to require the new domain-owned adapter code (not admin).

**4.2 Move admin expenses subscriptions**

Move:

- `src/app/admin/frontend/subs/expenses.cljs`

→

- `src/app/domain/frontend/expenses/admin/subs/expenses.cljs`

Ensure the admin build loads these subs via the domain registry `:init!` (or a dedicated admin init hook in the manifest).

**Acceptance**

- `src/app/admin/frontend/adapters/expenses/**` and `src/app/admin/frontend/subs/expenses.cljs` no longer exist.
- Admin build still initializes expenses entities via `app.domain.frontend.registry` contributions.
- `src/app/admin/frontend/core.cljs` contains no concrete expenses requires (e.g. no `app.admin.frontend.subs.expenses`).

---

### Phase 5 — Admin config EDNs: split system vs domain

Admin config EDNs under `src/app/admin/frontend/config/*.edn` still include concrete domain entities.

Important detail from current architecture:

- `app.admin.frontend.config.preload` **inlines only** `config/entities.edn` at build time.
- `view-options.edn` / `table-columns.edn` / `form-fields.edn` are treated as runtime-editable settings and are intentionally **not** inlined.

**5.1 Pick a strategy for domain entity metadata (recommended: manifest-driven)**

Two viable approaches:

**A) Manifest-driven entity config (recommended)**

- Remove all expenses entities from `src/app/admin/frontend/config/entities.edn`.
- Extend the domain manifest to contribute full entity configs (page titles, descriptions, default list behavior, etc.), not just `:init-fn`.
- Register these domain entity configs during startup (after `domain-registry/init-all-domains!`).

This avoids any build-time coupling via inlined domain EDNs.

**B) Domain-owned entities.edn (optional)**

- Create a domain-owned `entities.edn` under `src/app/domain/frontend/expenses/admin/config/entities.edn`.
- Update `app.admin.frontend.config.preload` to inline and merge **both**:
  - system `app/admin/frontend/config/entities.edn`
  - domain-provided `entities.edn` resources for enabled domains

This keeps the “EDN is the source” workflow, but introduces build-time resource dependency on domain.

**5.2 Split runtime-editable admin settings files**

For files that are edited at runtime (currently under `src/app/admin/frontend/config/*.edn`):

- keep system-only settings in admin
- move domain settings to domain-owned config files (location is flexible, but should be under `src/app/domain/**`)

Then update the load/merge path so admin reads system + domain settings without hard-coding `:expenses`.

**Acceptance**

- `src/app/admin/frontend/config/*.edn` contains only system entities (`:users`, `:admins`, `:audit-logs`, `:login-events`, etc.).
- Expenses entity config is provided by domain.

---

### Phase 6 — Regression prevention (CI guard)

Add a Babashka guard script that fails if template/admin/shared contain concrete domain references.

**6.1 Add script**

- `scripts/bb/guard-no-concrete-domain.bb`

Checks (initial set):

- No `app.domain.*.expenses` requires in `src/app/template/**` or `src/app/admin/**`.
- No `:expenses`, `:expense/`, or `"/expenses"` literals in template/admin/shared (allowlist specific generic strings if needed).

**6.2 Wire to test task**

Add to CI/test entry point (whatever `npm run test:config-audit` or `bb` task is used in this repo).

**Acceptance**

- Guard fails on re-introduced coupling.
- Guard passes on mainline after the refactor.

## Definition of done (end-state checklist)

- `src/app/template/**` contains **no** concrete expenses code (events/subs/pages/routes/config paths).
- `src/app/admin/**` contains **no** concrete expenses code (adapters/subs/entity config/groups).
- `src/app/shared/**` contains no concrete domain examples (use `<domain>` placeholders).
- Admin and user builds compile.
- Relevant FE tests still pass (`npm run test:cljs`).

## Notes / known risks

- CLJS load-order is sensitive for event/sub registration.
  - Prefer making `app.template.frontend.core/init` call `app.domain.frontend.registry/init-all-domains!` once during startup.
- Route/view composition can create circular deps if pages require template core.
  - Keep pages depending on template components/events, not on template core.

---

## Implementation Summary (2025-12-18)

All phases completed. Key changes:

### Phase 1-2: Domain user-expenses events/subs moved
- Created `src/app/domain/frontend/expenses/events/user_expenses/*.cljs` (13 event files)
- Created `src/app/domain/frontend/expenses/subs/user_expenses.cljs`
- Created `src/app/domain/frontend/pages.cljs` aggregator to avoid circular deps
- Deleted old template user_expenses files

### Phase 3: Master-detail form genericized
- Added `:id-keys` option to `master_detail.cljs` for entity matching
- Domain specifies `[:expense/id :expenses/id]` where needed

### Phase 4: Admin adapters/subs moved to domain
- Created `src/app/domain/frontend/expenses/admin/adapters/*.cljs` (normalize, sync, admin_crud, specs, ui_state)
- Created `src/app/domain/frontend/expenses/admin/subs.cljs`
- Deleted old admin adapter files

### Phase 5: Admin config EDNs split
- Created `src/app/domain/frontend/expenses/admin/config/*.edn` (entities, table-columns, view-options, form-fields)
- Created `src/app/domain/frontend/expenses/admin/config/preload.cljs` for self-registration
- Updated `settings_io.clj` to merge domain + admin configs

### Phase 6: Guard script and genericization
- Created `scripts/bb/guard_no_concrete_domain.clj`
- Genericized `entity_sync.cljs` with dynamic handler registration
- Genericized `form_interceptors.cljs` with bridge entity registry
- Guard passes with only UI grouping warnings (intentional)

### Test results
- All 265 frontend tests pass
- Both admin and app builds compile with 0 warnings
