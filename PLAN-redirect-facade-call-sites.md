# Plan — Redirect facade call sites to concrete namespaces

Date: 2026-01-17

## Goal

Reduce (or eliminate) “re-export facade” usage by updating call sites to depend directly on the *concrete* namespaces that implement the behavior.

This avoids:
- stale function values from `(def f other/f)` re-exports (hot-reload / REPL)
- “barrel namespace” drift where a facade silently becomes a dumping ground
- accidental circular dependencies introduced by central aggregator namespaces

This plan is intentionally incremental and safe: migrate call sites first, then (optionally) convert remaining facades into explicitly-marked compatibility shims, then delete when no longer referenced.

## Scope

### Known facade-style namespaces (current inventory)

**Shared**
- `app.shared.date` (facade for date core/arithmetic/range)
- `app.shared.patterns` (facade for patterns.* modules)
- `app.shared.type-conversion` (partly facade: re-exports DB casting helpers)
- `app.shared.validation.field-types` (minor re-export wrapper)

**Template**
- `app.template.frontend.db.db` (frontend app-db entry point; re-exports flags/schemas/defaults/validation/interceptors)
- `app.template.frontend.utils.test-utils` (test wrapper)
- `app.template.backend.utils.adapters.database` (**removed**; callers use `app.shared.adapters.database` / `app.shared.adapters.normalization` directly)
- `app.template.di.config` (DI config + factory re-exports)

**Domain**
- `app.domain.frontend.expenses.components.form-fields` (expense form-fields facade)
- `app.domain.frontend.expenses.components.expense-form` (public entrypoint facade)
- `app.domain.frontend.expenses.components.user-expense-form` (public entrypoint facade)
- `app.domain.backend.expenses.integrations.mistral-ocr` (integration facade for config/http/batch)

**Admin**
- `app.template.frontend.events.auth.ids` (event id constants re-export)

## Decision rule: where redirecting is applicable

Redirect call sites when **all** of the following are true:

1. The facade does not add meaningful orchestration logic (it is mostly `:require` + `def` aliases).
2. Call sites are internal to this repo (not part of an intentional “public API surface”).
3. The concrete namespace is stable enough to depend on directly.
4. Redirecting does **not** create a circular dependency (check require graph).

Prefer keeping a facade (and converting function aliases into forwarding wrappers) when:
- it is explicitly a *stable public entrypoint* for a feature (e.g. a UI component namespace)
- you want a single namespace for “blessed imports” across the project

## Phased execution

### Phase 0 — Prepare an allowlist (optional)

Some facades may be intentional public APIs. Decide which ones are “keepers” vs “migrate away”.

Suggested defaults:
- **Keep as public entrypoints** (do not redirect callers yet):
  - `app.domain.frontend.expenses.components.expense-form`
  - `app.domain.frontend.expenses.components.user-expense-form`
  - `app.domain.frontend.expenses.components.form-fields`
  - `app.shared.date` (depending on team preference)
  - `app.shared.patterns` (depending on team preference)

- **Migrate away** (redirect call sites):
  - `app.template.di.config`
  - `app.template.backend.utils.adapters.database` (done/removed; shared adapters used directly)
  - `app.domain.backend.expenses.integrations.mistral-ocr` (if you want tests to stub submodules directly)
  - `app.shared.validation.field-types` (if it’s purely a wrapper)
  - `app.shared.type-conversion` (if it’s acting as a shim and you want direct deps)

### Phase 1 — Find call sites per facade

For each facade namespace `X`, identify:
- requires: `(:require [X :as ...])`
- refers: `(:require [X :refer [..]])`
- fully qualified symbols: `X/foo`

Record:
- file path
- alias used
- which symbols are actually referenced

### Phase 2 — Replace requires + usages

For each call site:

#### Case A: `:as` alias to facade

Before:
- `(:require [app.shared.patterns :as patterns])`
- `(patterns/valid-email? s)`

After:
- `(:require [app.shared.patterns.email :as email])`
- `(email/valid-email? s)`

Notes:
- Prefer alias names that match the module: `email`, `date-time`, `http`, `config`, etc.
- If multiple symbols came from different submodules of the facade, require each needed module explicitly.

#### Case B: `:refer` from facade

Before:
- `(:require [app.shared.patterns :refer [valid-email? valid-url?]])`
- `(valid-email? s)`

After (recommended: qualified usage):
- `(:require [app.shared.patterns.email :as email]
             [app.shared.patterns.url :as url])`
- `(email/valid-email? s)`
- `(url/valid-url? s)`

Alternative (less preferred):
- `(:require [app.shared.patterns.email :refer [valid-email?]] ...)`

Rationale: qualified calls make it obvious what module owns the behavior.

#### Case C: Facade imported only for one symbol

If a file imports the facade only for a single symbol, redirecting is usually trivial and improves clarity.

### Phase 3 — Keep compatibility temporarily (optional)

After call sites are migrated, choose one:

- **Option 1 (preferred)**: Keep the facade with explicit deprecation markers and a removal date.
  - Add `^:deprecated` or a clear docstring warning.
  - (If repo policy requires) mark alias vars with `^:legacy-alias`.

- **Option 2**: Delete the facade namespace file entirely if there are no remaining references.

### Phase 4 — Add enforcement

Update the legacy inventory/audit tooling to prevent new “re-export facades” from being reintroduced unless explicitly allowed.

Possible enforcement ideas:
- detect namespaces whose bodies are mostly `(def x other/x)` aliases
- detect “index” namespaces that export from >N child namespaces
- allowlist explicit public entrypoints (docstring marker like `"Stable public entrypoint"`)

### Phase 5 — Verification

Use focused verification instead of running everything:
- Run relevant FE/BE test subsets touching changed requires.
- Run config audits if you touched frontend config or entity specs.
- Sanity-check reload behavior (especially if the original motivation is stale function values).

## Worked example: redirecting from `app.shared.patterns`

Target outcome:
- Replace `(patterns/valid-email? ...)` with `(email/valid-email? ...)`

Steps:
1. Find files requiring `app.shared.patterns`.
2. For each file, check which symbols are used (email/url/slug/phone/date-time/auth).
3. Replace requires to concrete namespaces.
4. Update usages.
5. Ensure no circular deps introduced.

## Risk management / common pitfalls

- **Circular deps**: Facades sometimes exist to avoid circular requires. If redirecting introduces a cycle, prefer:
  - extracting shared code to a lower-level namespace
  - or keeping the facade but switching function exports to forwarding wrappers

- **CLJS vs CLJ/CLJC**: Keep platform splits in mind; a facade might hide reader conditionals.

- **Public API assumptions**: UI “entrypoint” namespaces may be used externally by other parts of the app. Treat deletions carefully.

## End state targets

- Internal code depends on concrete namespaces directly.
- Remaining facades are either:
  - explicitly intentional public entrypoints, OR
  - explicitly deprecated compatibility shims with a scheduled deletion.

