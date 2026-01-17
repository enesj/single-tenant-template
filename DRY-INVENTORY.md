# DRY Inventory (Single-Tenant Template)

This is a focused inventory of how DRY (Don't Repeat Yourself) is implemented in this codebase. It is intended as a reference list for later audits of duplication or drift.

## Sources Reviewed
- `docs/frontend/template-infrastructure.md` (explicit DRY section; list-view + entity-specs)
- `docs/frontend/component-library.md` (component reuse strategy)
- `docs/frontend/admin-panel-single-tenant.md`
- `docs/frontend/admin-settings.md`
- `docs/frontend/crud-event-flow.md`
- `docs/frontend/README.md` (Template → Adapter → Admin layering)
- `docs/frontend/template-component-integration.md` (shared components + adapters)
- `docs/backend/template-infrastructure.md` (backend reuse and shared utilities)
- `docs/backend/single-tenant-template.md`
- `docs/shared/template-domain-integration.md`
- `docs/shared/README.md`
- `docs/shared/architecture.md`
- `docs/shared/auth-utilities.md`
- `docs/shared/date-utilities.md`
- `docs/shared/http-utilities.md`
- `docs/shared/pagination-utilities.md`
- `docs/` exists; **no `@docs` folder found**

## Module Coverage Map (All Modules)
- **`src/app/shared`**: Cross-platform helpers (auth/date/http/pagination/type conversion/validation/patterns/strings), model naming, frontend-config validation.
- **`src/app/template`**: Shared backend DI + routes + utilities; shared frontend components and CRUD/event glue.
- **`src/app/admin`**: Admin adapters/events/pages that consume template/shared infrastructure.
- **`src/app/domain`**: Domain registry + expenses domain (backend services/routes; frontend adapters/events/config).
- **`resources/db`**: Canonical schema sources + generated models/migrations; shared single source of truth.
- **`docs`**: Guidance on reuse patterns and layering.
- **`scripts` / `cli-tools`**: Automation for consistent workflows (migrations/config validation/testing).
- **`vendor`**: Vendored libraries (no DRY patterns; treated as external code).
- **`test` / `tests.edn` / `test-results`**: Tests mirror `src` structure; no additional DRY abstractions inventoried yet.

## Inventory of DRY Patterns

### 1) Template List View: One List Component, Many Entities
**Pattern:** Configuration-driven list UI that centralizes pagination, filtering, sorting, batch edit, table rendering, and selection in a single component.  
**Why DRY:** Avoids building separate list pages per entity.  
**Where:**
- `src/app/template/frontend/components/list.cljs` (`list-view`)  
- Supporting submodules in `src/app/template/frontend/components/list/` and shared table/pagination components.
**Notes:** Driven by `:entity-spec`, `:display-settings`, `:render-actions`, etc., letting admin pages reuse the same list logic.

### 2) Template Forms: Shared Form Renderer + Field Components
**Pattern:** Central form rendering and validation with shared field components.  
**Why DRY:** Avoids bespoke form implementations for each entity.  
**Where:**
- `src/app/template/frontend/components/form.cljs` (form renderer, field orchestration)
- `src/app/template/frontend/components/form/fields/*` (inputs, select, textarea, checkbox, number, etc.)
- Example ID generation and error handling: `src/app/template/frontend/components/form/fields/input.cljs`
**Notes:** Field components generate fallback IDs from `formId` and share validation/error display patterns.

### 3) Adapter Layer: Normalize + Sync Once, Reuse Everywhere
**Pattern:** Admin adapters call shared normalize/sync/register helpers instead of re-implementing entity logic.  
**Why DRY:** One normalization and event-registration pipeline for all entities.  
**Where:**
- `src/app/admin/frontend/adapters/core.cljs` (adapter registry + helpers)
- `src/app/template/frontend/shared/utils/entity.cljs` (normalize-entity, register-sync/upsert/subs)

### 4) CRUD Bridge System: Central CRUD Defaults + Context Overrides
**Pattern:** A shared CRUD event system with default request/success/failure handlers, with optional overrides per context.  
**Why DRY:** Keeps CRUD flow consistent, avoids duplicating HTTP + error handling across entities.  
**Where:**
- `src/app/template/frontend/shared/bridges/crud.cljs` (default handlers + bridge registry)
- `src/app/admin/frontend/adapters/core.cljs` (admin bridge registration helper)

### 5) Config-Driven UI (Entities, Columns, Forms, View Options)
**Pattern:** Declarative EDN configuration + shared Malli validation.  
**Why DRY:** Centralizes UI behavior in configuration rather than per-page code.  
**Where:**
- Specs: `src/app/shared/specs/entities.cljc`, plus shared specs for form fields / table columns / view options
- Validation: `src/app/shared/frontend_config/validation.clj`
- Admin settings I/O + merge of admin + domain configs: `src/app/template/backend/routes/admin/settings_io.clj`

### 6) Shared HTTP Helpers (Frontend + Cross-Platform)
**Pattern:** Single set of request builders and error extraction helpers.  
**Why DRY:** Prevents custom HTTP wiring per feature.  
**Where:**
- `src/app/template/frontend/api/http.cljs` (request builders + entity CRUD)
- `src/app/shared/http.cljc` (cross-platform constants + error response helpers)

### 7) Shared Pagination + Backend Query Builders
**Pattern:** Central pagination and query composition utilities.  
**Why DRY:** Avoids ad hoc pagination/sorting/filtering in each service.  
**Where:**
- `src/app/shared/pagination.cljc` (cross-platform pagination utilities)
- `src/app/shared/query_builders.clj` (shared backend HoneySQL query-building helpers)
- `src/app/template/backend/utils/query_builders.clj` (shared query composition for admin services)
- `src/app/domain/backend/expenses/services/services_factory.clj` (generic domain query builder functions)

### 8) Backend Service DI Container
**Pattern:** Generic dependency injection + lifecycle manager for backend services.  
**Why DRY:** Standardizes service wiring, lifecycle, and error handling.  
**Where:**
- `src/app/template/di/container.clj`
- `src/app/template/di/config.clj`
- `src/app/template/di/registry.clj`

### 9) Model Customizations Extraction
**Pattern:** Central extraction of admin/form customizations and computed fields from `models.edn`.  
**Why DRY:** Single source of truth for UI-related metadata derived from schemas.  
**Where:**
- `src/app/template/backend/utils/model_customizations.clj`

### 10) Shared Admin Route Utilities
**Pattern:** Central response helpers, error handling, UUID parsing, and request context extraction.  
**Why DRY:** Avoids inconsistent error/response logic across admin routes.  
**Where:**
- `src/app/template/backend/routes/admin/utils.clj`

### 11) Centralized Admin Settings API
**Pattern:** Uniform read/write/validate APIs for admin settings.  
**Why DRY:** Ensures consistent handling of view-options, table-columns, and form-fields.  
**Where:**
- `src/app/template/backend/routes/admin/settings.clj`
- `src/app/template/backend/routes/admin/settings_io.clj`

### 12) Shared UI Components Catalog
**Pattern:** UI component library reused across admin screens.  
**Why DRY:** Avoids custom UI per screen; promotes consistent look/behavior.  
**Where:**
- `src/app/template/frontend/components/*` (e.g., `table.cljs`, `pagination.cljs`, `modal.cljs`, `filter/*`, `button.cljs`)
- Documented in `docs/frontend/component-library.md`

### 13) Shared Utilities Layer (`app.shared/*`)
**Pattern:** Small, focused, cross-platform utility namespaces for auth, dates, HTTP, pagination, strings, patterns, validation, and type conversion.  
**Why DRY:** Prevents duplicating core helpers across frontend/backend; keeps behavior consistent.  
**Where:**
- `src/app/shared/auth.cljc` (roles/permissions)
- `src/app/shared/date*.cljc` (date/time helpers)
- `src/app/shared/http.cljc` (HTTP constants/helpers)
- `src/app/shared/pagination.cljc` (pagination math + shapes)
- `src/app/shared/string.cljc`, `src/app/shared/patterns.cljc`
- `src/app/shared/type_conversion*.cljc` (cast/normalize/prepare data)
- `src/app/shared/validation/**` and `src/app/shared/schemas/**`
- Documented in `docs/shared/*`

### 14) Domain Registry (Backend + Frontend)
**Pattern:** Single registry per side (backend/frontend) to compose domain routes, UI config, and init hooks.  
**Why DRY:** Avoids hardcoding domain wiring in multiple places; enables consistent discovery.  
**Where:**
- `src/app/domain/backend/registry.clj`
- `src/app/domain/frontend/registry.cljs`
**Notes:**
- Backend registry now also exposes **domain-owned admin UI config paths** (used by admin settings I/O) so template code doesn’t hardcode per-domain file paths.
- Backend registry also centralizes **primary-domain user UI config path selection** (`primary-user-ui-config-paths`) to avoid duplicated single-domain/back-compat logic across routes and settings I/O.
- Verified via focused Kaocha run: `/tmp/be-test-dry22-domain-registry-focus.txt`.
- Frontend registry keeps event/sub loading domain-local via init aggregators (e.g. `src/app/domain/frontend/expenses/init.cljs`) to avoid the registry growing a huge require list.

### 15) Domain Adapter Aggregation (Expenses)
**Pattern:** Adapter aggregator namespaces load/initialize multiple domain adapters in one place.  
**Why DRY:** One entrypoint for adapter init instead of per-page wiring.  
**Where:**
- `src/app/domain/frontend/expenses/adapters.cljs`
- `src/app/domain/frontend/expenses/admin/adapters.cljs`

**Notes:**
- Expenses adapters now expose a generic init API (`init-entity-adapter!`) plus a lookup map (`entity-init-fns`) and a bulk initializer (`init-all-adapters!`).
- Per-entity init vars (e.g. `init-expenses-adapter!`) remain for backwards compatibility, but are backed by the shared map to avoid drift.

### 16) Schema as Single Source of Truth (DB + UI Config Alignment)
**Pattern:** Canonical schema in `resources/db/{template,shared,domain}` merged into `resources/db/models.edn`; UI config validation aligns with schema.  
**Why DRY:** Avoids duplicating schema definitions across code and config; drives UI config checks from DB metadata.  
**Where:**
- `resources/db/{template,shared,domain}/**` → `resources/db/models.edn`
- Frontend config validation: `src/app/shared/frontend_config/validation.clj`
- Docs: `docs/backend/single-tenant-template.md`, `docs/frontend/admin-settings.md`

**Notes:**
- `app.shared.frontend-config.schema/models-index` accepts either a consolidated `models.edn` file path (default) **or** a hierarchical schema directory like `resources/db` (template/domain/shared, including `resources/db/domain/*/models.edn`).
- This lets `bb validate-frontend-config --schema resources/db` validate against the canonical **source** model inputs without requiring regeneration of `resources/db/models.edn` first.

### 17) Centralized Field/Model Metadata
**Pattern:** Shared model naming, field metadata/specs, and type casting utilities used across layers.  
**Why DRY:** One place for key normalization, field typing, and casting logic.  
**Where:**
- `src/app/shared/model_naming.cljc` (notably `db-keyword->app`, `ensure-app-keyword`, `app-map-keys->db`)
- `src/app/shared/field_metadata.cljc`, `src/app/shared/field_specs.cljc`, `src/app/shared/field_types.cljc`
- `src/app/shared/type_conversion_db.cljc`

**Progress (2026-01-14):**
- Centralized field label derivation as `app.shared.labels/field-name->label` and delegated both validation metadata + field-spec generation to it.
- Added `app.shared.model-naming/ensure-app-keyword` so callers can normalize entity/field identifiers in one step (best-effort keyword coercion + snake_case → kebab-case).
- Normalized entity identifier inputs (snake_case → kebab-case) at key lookup boundaries:
  - entity specs (`:entity-specs/by-name`, `:form-entity-specs/by-name`)
  - template UI subscriptions (display settings + visible columns)
  - list settings events and admin column visibility persistence
  - persisted `ui-entity-prefs` localStorage keys are migrated on load to avoid “shadow prefs” under snake_case keys (entity keys + nested field/column identifiers).
- Added CLJS regression tests to lock behavior.

## DRY Conventions to Enforce During Audits
- Prefer template components (`src/app/template/frontend/components/**`) before adding new UI.
- Use entity specs + config files (entities/form-fields/table-columns/view-options) to drive UI, not custom pages.
- Use CRUD bridges for overrides rather than reimplementing CRUD flows.
- Use shared HTTP/pagination/query helpers instead of ad hoc logic.
- Keep normalization and sync logic in adapters + shared utils.
- Normalize entity/field identifiers at boundaries using `app.shared.model-naming/ensure-app-keyword` (instead of scattering `keyword` / `_→-` fixes across call sites).
- Use domain registries for routing/config wiring instead of manual requires scattered across modules.
- Keep schema definitions in `resources/db/{template,shared,domain}/**` and regenerate; do not copy schema in code.

## Notes / Gaps
- This inventory is based on code + docs visible in `docs/` and core shared/template paths.  
- If additional domain modules are added later, re-run this inventory to include new shared patterns.
- ✅ `app.shared.adapters.database` exists (JVM-only) and centralizes PostgreSQL JDBC object conversion; template backend delegates to it (2026-01-13).

## Non-DRY Code Snippets (Candidates for Consolidation)

- **Entity key normalization drift (frontend prefs + subs)** — **Resolved (2026-01-14)**.
  - Fixed snake_case vs kebab-case mismatches by normalizing entity identifiers at subscription/event boundaries and migrating persisted prefs keys.
  - Key files:
    - `src/app/template/frontend/subs/ui.cljs`
    - `src/app/template/frontend/events/list/settings.cljs`
    - `src/app/template/frontend/events/list/ui_state.cljs`
    - `src/app/template/frontend/interceptors/persistence.cljs`
    - `src/app/admin/frontend/events/config.cljs`
    - `src/app/template/frontend/db/entity_specs.cljs`
  - Tests:
    - `test/app/template/frontend/db/entity_specs_test.cljs`
    - `test/app/template/frontend/subs/ui_test.cljs`
  - Verification: `npm run test:cljs` (output: `/tmp/fe-test-dry18-entity-key-normalization-prefs.txt`, `/tmp/fe-test-dry17-entity-spec-normalization-2.txt`).
  - Verification (refactor): `npm run test:cljs` (output: `/tmp/fe-test-dry19-ensure-app-keyword.txt`).

- **Duplicate model customization extraction** — **Resolved (2026-01-13)**.
  - `app.template.backend.utils.model-customizations` now delegates extraction helpers to `app.shared.model-customizations` to avoid drift.
  - Schema-stripping helpers (`strip-*`) remain in the template backend namespace for migration/alignment tooling.

- **Multiple HTTP request builders (frontend)** — three overlapping request helpers with similar responsibilities.
  - **Progress (2026-01-13):** centralized error extraction (admin helper now delegates to `app.shared.http/extract-error-message`, and the shared extractor was expanded to cover common admin response shapes).
  - **Progress (2026-01-13):** admin helper token retrieval now prefers auth persistence (`app.admin.frontend.auth.persistence/get-persisted-token`) with fallback to raw localStorage for legacy sessions.
  - **Progress (2026-01-13):** request map construction (formats, params/body inclusion, and safe Content-Type behavior) centralized in `app.shared.http.core/build-xhrio-request`; both `admin-request` and `api-request` now delegate to it.
  - **Progress (2026-01-13):** template entity CRUD helpers are now split into explicit `*-public` vs `*-admin` callers; the legacy `create-entity/update-entity/delete-entity` wrappers are public-only.
  - **Progress (2026-01-13):** admin vs public routing decisions for generic CRUD now live in the CRUD bridge default request handler (route/path based), not inside the HTTP helper.
  - **Progress (2026-01-13):** removed legacy `app.shared.http.core/build-request` after migrating all call sites to `build-xhrio-request`.
  `src/app/template/frontend/api/http.cljs`:
  ```clojure
  (defn api-request [{:keys [method uri params body format response-format on-success on-failure timeout headers]}] ...)
  ```
  `src/app/admin/frontend/utils/http.cljs`:
  ```clojure
  (defn admin-request [{:keys [method uri params body format response-format headers timeout on-success on-failure token]}] ...)
  ```

- **Duplicate JSON response helpers (backend)** — response helpers defined in two places.
  - **Progress (2026-01-13):** added CLJ-only helpers in `app.shared.http` for routes that must return JSON *string* bodies: `encode-json-body`, `json-string-response`, `error-string-response`.
  - **Progress (2026-01-13):** `app.template.backend.routes.admin.utils/json-response` + `error-response` now delegate to `app.shared.http` (preserving the admin routes' string-body requirement).
  - **Progress (2026-01-13):** expenses user handler helpers now delegate to `app.shared.http/json-string-response` to avoid drifting JSON response behavior.
  `src/app/template/backend/routes/admin/utils.clj`:
  ```clojure
  (defn json-response [data & {:keys [status] :or {status 200}}] ...)
  (defn error-response [message & {:keys [status details] :or {status 500}}] ...)
  ```
  `src/app/shared/http.cljc`:
  ```clojure
  (defn json-response ([data] (json-response status-ok data)) ...)
  (defn error-response ([message] (error-response status-internal-server-error message)) ...)
  ```

- **Duplicate user-facing expenses handler utilities (backend)** — **Resolved (2026-01-13)**.
  - Consolidated request user extraction (`:session` and `:identity`), role gating, safe JSON body parsing defaults, and consistent JSON *string* responses in:
    - `src/app/domain/backend/expenses/handlers/user_expenses/helpers.clj`
  - Updated user handlers to delegate instead of re-defining local helpers:
    - `src/app/domain/backend/expenses/handlers/user_receipts.clj`
    - `src/app/domain/backend/expenses/handlers/user_articles.clj`
  - Added regression tests to lock the early auth/role behavior:
    - `test/app/domain/expenses/handlers/user_handlers_test.clj`
  - Verified via focused Kaocha run:
    - `/tmp/be-test-dry10-user-handlers.txt` (3 tests, 0 failures)
    - `/tmp/be-test-dry10-user-handlers-2.txt` (3 tests, 0 failures)

- **Duplicate frontend config EDN I/O + validation logging (backend)** — **Resolved (2026-01-14)**.
  - Centralized safe vs strict EDN reads, validation warning logging, and pretty EDN writes in:
    - `src/app/shared/frontend_config/io.clj` (`app.shared.frontend-config.io`)
  - Updated both admin settings I/O and user `/api/v1/config` domain UI config loading to delegate:
    - `src/app/template/backend/routes/admin/settings_io.clj`
    - `src/app/template/backend/routes/api.clj`
  - Added focused unit coverage:
    - `test/app/shared/frontend_config/io_test.clj`
  - Verified via focused Kaocha run:
    - `/tmp/be-test-dry11-frontend-config-io.txt` (2 tests, 0 failures)

- **Duplicate status badge mapping (frontend)** — **Resolved (2026-01-14)**.
  - Centralized status → DaisyUI badge variant mapping as:
    - `app.template.frontend.components.advanced-fields/status->badge-class`
  - Updated admin-side callers to delegate instead of hardcoding status cases:
    - `src/app/admin/frontend/components/ui.cljs` (admin status badges now reuse template mapping + renderer)
    - `src/app/admin/frontend/specs/conditional.cljs` (dynamic `:status-badge` formatting uses shared mapping)
  - Added focused unit coverage:
    - `test/app/template/frontend/components/advanced_fields_test.cljs`
  - Verified via ClojureScript test run:
    - `/tmp/fe-test-dry12-status-badge-3.txt` (270 tests, 0 failures)

- **Admin-only shared UI utils used by domain (frontend)** — **Resolved (2026-01-14)**.
  - Extracted shared display/formatting helpers from admin into the template layer so domain code doesn’t depend on admin namespaces:
    - `src/app/template/frontend/utils/display.cljs`
  - Extracted reusable detail-view UI components from admin into the template layer:
    - `src/app/template/frontend/components/detail.cljs`
  - Added a template-level shared-utils aggregator for these helpers/components:
    - `src/app/template/frontend/components/shared_utils.cljs`
  - Updated call sites to depend on template instead of admin:
    - `src/app/admin/frontend/components/ui.cljs`
    - `src/app/domain/frontend/expenses/**`
  - Verified via ClojureScript test run:
    - `/tmp/fe-test-dry12-template-shared-utils-2.txt` (270 tests, 0 failures)

- **Duplicate formatting helpers (admin vs template)** — **Resolved (2026-01-14)**.
  - Removed the remaining duplication by delegating the admin formatting helpers to the template implementation:
    - `src/app/admin/frontend/components/format.cljs` now delegates `react-element?`, `format-value`, `format-date`, `format-relative-time`, `user-initials`, `tenant-label` to `app.template.frontend.utils.display`.
  - Verified via ClojureScript test run:
    - `/tmp/fe-test-dry13-admin-format-delegates-1.txt` (270 tests, 0 failures)

- **Query builder duplication (backend)** — both template admin and domain services implement their own query builders.
- **Query builder duplication (backend)** — **Resolved (2026-01-13)**.
  - Introduced a small shared backend helper namespace: `src/app/shared/query_builders.clj`.
    - Pagination normalization + application: `normalize-limit`, `normalize-offset`, `apply-pagination`
    - Sorting helpers: `normalize-order-direction`, `apply-order-by`
    - Search/where helpers: `build-ilike-or`, `merge-where-and`, `apply-search-where`
  - Updated call sites to delegate (public signatures preserved):
    - `src/app/template/backend/utils/query_builders.clj`
    - `src/app/domain/backend/expenses/services/services_factory.clj`
  - Added focused unit tests: `test/app/shared/query_builders_test.clj`.
  - Verified via backend test suite: `bb be-test` (208 tests, 0 failures).

- **Duplicate key conversion in form submission** — **Resolved (2026-01-13)**.
  - Centralized as `app.shared.model-naming/app-map-keys->db`.
  - Call sites now delegate to the shared helper:
    - `src/app/template/frontend/events/form.cljs`
    - `src/app/admin/frontend/events/users/template/form_interceptors.cljs`

- **Duplicate DB result → app normalization helpers (backend)** — **Resolved (2026-01-13)**.
  - Centralized as `app.shared.adapters.database/to-app` (PGobject/PgArray conversion + snake_case → kebab-case keys).
  - Removed the template adapter re-export shim (`app.template.backend.utils.adapters.database/*`); call sites use shared adapters directly.
  - Removed duplicated local `to-app` implementations across expenses backend routes/handlers:
    - `src/app/domain/backend/expenses/routes/routes_factory.clj`
    - `src/app/domain/backend/expenses/routes/receipts.clj`
    - `src/app/domain/backend/expenses/routes/reports.clj`
    - `src/app/domain/backend/expenses/handlers/user_articles.clj`
    - `src/app/domain/backend/expenses/handlers/user_receipts.clj`
    - `src/app/domain/backend/expenses/handlers/receipt_upload.clj`
  - Verified via focused backend tests:
    - `clj -M:test -m kaocha.runner --focus app.domain.backend.expenses.routes.receipts-test` (output: `/tmp/be-test-dry8-receipts.txt`)
    - `clj -M:test -m kaocha.runner --focus app.shared.adapters.database-test` (output: `/tmp/be-test-dry8-adapters.txt`)

- **Duplicate UUID parsing helpers (backend)** — **Resolved (2026-01-13)**.
  - Centralized best-effort UUID parsing as `app.shared.type-conversion/try-parse-uuid` (blank/invalid → nil).
  - Updated call sites to delegate instead of defining local `try-parse-uuid`/`try-uuid` helpers:
    - `src/app/domain/backend/expenses/handlers/user-expenses/helpers.clj`
    - `src/app/domain/backend/expenses/handlers/user_articles.clj`
    - `src/app/domain/backend/expenses/handlers/user_receipts.clj`
    - `src/app/domain/backend/expenses/handlers/receipt_upload.clj`
    - `src/app/domain/backend/expenses/services/receipts/parsing.clj`
    - `src/app/domain/backend/expenses/services/{price_observations,article_aliases}.clj`
    - `src/app/domain/backend/expenses/services/user_expenses.clj` (batch update)
    - `src/app/domain/backend/expenses/workers/receipt_ocr/core.clj` (batch OCR)
    - `src/app/template/backend/services/monitoring/login_events.clj`
    - `src/app/template/backend/routes/password_reset.clj` (normalize session user-id)
    - `src/app/template/backend/routes/admin/utils.clj`
  - Added focused unit coverage: `test/app/shared/type_conversion_test.cljc`.

## Prioritization (Order for DRY Fixes)

1) **Duplicate model customization extraction** — ✅ **Done (2026-01-13)**
  - Template backend delegates to shared; strip helpers kept for migrations.

2) **Duplicate key conversion in form submission** — ✅ **Done (2026-01-13)**
  - Added `app.shared.model-naming/app-map-keys->db`; updated both call sites.

3) **Multiple HTTP request builders (frontend)**  
  - Medium impact, medium risk: three overlapping helpers; needs careful API alignment.  
   - Files: `src/app/template/frontend/api/http.cljs`, `src/app/admin/frontend/utils/http.cljs`, `src/app/shared/http/core.cljs`
  - Status: ✅ **Done (2026-01-13)** — error extraction + token retrieval aligned; request map construction centralized; template CRUD split into explicit public vs admin callers.

4) **Duplicate JSON response helpers (backend)**  
   - Medium impact, medium risk: two response layers; standardize on one to avoid drift.  
   - Files: `src/app/template/backend/routes/admin/utils.clj`, `src/app/shared/http.cljc`
  - Status: ✅ **Done (2026-01-13)** — shared response shape lives in `app.shared.http`; CLJ-only JSON string encoding helpers added for non-muuntaja routes.

5) **Query builder duplication (backend)**  
   - Higher complexity: domain vs template query needs alignment; refactor last.  
   - Files: `src/app/template/backend/utils/query_builders.clj`, `src/app/domain/backend/expenses/services/services_factory.clj`
  - Status: ✅ **Done (2026-01-13)** — common pagination/sorting/search query-building behavior centralized in `app.shared.query-builders` while preserving existing APIs.

6) **Remove legacy `build-request` (frontend)**
  - Low impact, low risk: delete unused legacy request builder to prevent drift and confusion.
  - Files: `src/app/shared/http/core.cljs`
  - Status: ✅ **Done (2026-01-13)** — removed `build-request`; callers use `build-xhrio-request` via higher-level helpers.
  - Verification: ✅ `npm run test:cljs` (2026-01-13) — 268 tests, 0 failures (output: `/tmp/fe-test-dry6.txt`).

7) **Shared DB adapter utilities (backend)**
  - Low impact, medium value: align docs with code and centralize PG JDBC object conversion + key normalization in `app.shared.adapters.*`, with template backend delegating for backward compatibility.
  - Files: `src/app/shared/adapters/database.clj`, `src/app/shared/adapters/normalization.cljc`, `src/app/template/backend/utils/adapters/{database,normalization}.clj`
  - Status: ✅ **Done (2026-01-13)** — created `app.shared.adapters.database` + `app.shared.adapters.normalization`; template adapters now delegate.
  - Verification: ✅ `clj -M:test -m kaocha.runner --focus app.shared.adapters.database-test` (2026-01-13) — 2 tests, 0 failures (output: `/tmp/be-test-dry7.txt`).

8) **Deduplicate expenses backend `to-app` helpers**
  - Low impact, medium value: replace repeated local `(-> data convert-pg-objects convert-db-keys->app-keys)` helpers with a single shared helper.
  - Files: `src/app/shared/adapters/database.clj` + expenses backend routes/handlers listed above.
  - Status: ✅ **Done (2026-01-13)** — standardized on `db-adapter/to-app` (delegating to shared implementation).
  - Verification: ✅ focused tests (outputs: `/tmp/be-test-dry8-receipts.txt`, `/tmp/be-test-dry8-adapters.txt`).

9) **Deduplicate UUID parsing helpers (backend)**
  - Low impact, medium value: remove local `try-parse-uuid`/`try-uuid` helpers scattered across routes/handlers/services.
  - Files: `src/app/shared/type_conversion.cljc` + call sites listed above.
  - Status: ✅ **Done (2026-01-13)** — standardized on `app.shared.type-conversion/try-parse-uuid`.
  - Verification:
    - ✅ `clj -M:test -m kaocha.runner --focus app.shared.type-conversion-test` (output: `/tmp/be-test-dry9-type-conversion.txt`).
    - ✅ `clj -M:test -m kaocha.runner --focus app.domain.expenses.services.receipts-test` (output: `/tmp/be-test-dry9-receipts-services.txt`).
    - ✅ `clj -M:test -m kaocha.runner --focus app.backend.routes.admin.password-test` (output: `/tmp/be-test-dry9-password-routes.txt`).

10) **Deduplicate user-facing expenses handler utilities (backend)**
  - Consolidate request user extraction (`:session` + `:identity`), role gating, safe JSON body parsing defaults, and consistent JSON string responses.
  - Status: ✅ **Done (2026-01-13)** — standardized on `app.domain.backend.expenses.handlers.user-expenses.helpers`.
  - Verification: ✅ focused tests (outputs: `/tmp/be-test-dry10-user-handlers.txt`, `/tmp/be-test-dry10-user-handlers-2.txt`).

11) **Centralize frontend config EDN I/O (backend)**
  - Deduplicate EDN read/validate/log logic used by admin settings I/O and `/api/v1/config` domain UI config loading.
  - Status: ✅ **Done (2026-01-14)** — introduced `app.shared.frontend-config.io` and migrated both call sites.
  - Verification: ✅ `clj -M:test -m kaocha.runner --focus app.shared.frontend-config.io-test` (output: `/tmp/be-test-dry11-frontend-config-io.txt`).

12) **Centralize status badge mapping (frontend)**
  - Deduplicate status → DaisyUI badge-class mapping and reuse template rendering in admin UI.
  - Status: ✅ **Done (2026-01-14)** — introduced `app.template.frontend.components.advanced-fields/status->badge-class` and migrated admin call sites.
  - Verification: ✅ `npm run test:cljs` (output: `/tmp/fe-test-dry12-status-badge-3.txt`).

13) **Move admin shared UI helpers into template (frontend)**
  - Remove domain → admin coupling by relocating shared display/detail utilities to the template layer.
  - Status: ✅ **Done (2026-01-14)** — created `app.template.frontend.utils.display`, `app.template.frontend.components.detail`, and a template `shared-utils` aggregator; migrated admin + domain call sites.
  - Verification: ✅ `npm run test:cljs` (output: `/tmp/fe-test-dry12-template-shared-utils-2.txt`).
