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

### 15) Domain Adapter Aggregation (Expenses)
**Pattern:** Adapter aggregator namespaces load/initialize multiple domain adapters in one place.  
**Why DRY:** One entrypoint for adapter init instead of per-page wiring.  
**Where:**
- `src/app/domain/frontend/expenses/adapters.cljs`
- `src/app/domain/frontend/expenses/admin/adapters.cljs`

### 16) Schema as Single Source of Truth (DB + UI Config Alignment)
**Pattern:** Canonical schema in `resources/db/{template,shared,domain}` merged into `resources/db/models.edn`; UI config validation aligns with schema.  
**Why DRY:** Avoids duplicating schema definitions across code and config; drives UI config checks from DB metadata.  
**Where:**
- `resources/db/{template,shared,domain}/**` → `resources/db/models.edn`
- Frontend config validation: `src/app/shared/frontend_config/validation.clj`
- Docs: `docs/backend/single-tenant-template.md`, `docs/frontend/admin-settings.md`

### 17) Centralized Field/Model Metadata
**Pattern:** Shared model naming, field metadata/specs, and type casting utilities used across layers.  
**Why DRY:** One place for key normalization, field typing, and casting logic.  
**Where:**
- `src/app/shared/model_naming.cljc`
- `src/app/shared/field_metadata.cljc`, `src/app/shared/field_specs.cljc`, `src/app/shared/field_types.cljc`
- `src/app/shared/type_conversion_db.cljc`

## DRY Conventions to Enforce During Audits
- Prefer template components (`src/app/template/frontend/components/**`) before adding new UI.
- Use entity specs + config files (entities/form-fields/table-columns/view-options) to drive UI, not custom pages.
- Use CRUD bridges for overrides rather than reimplementing CRUD flows.
- Use shared HTTP/pagination/query helpers instead of ad hoc logic.
- Keep normalization and sync logic in adapters + shared utils.
- Use domain registries for routing/config wiring instead of manual requires scattered across modules.
- Keep schema definitions in `resources/db/{template,shared,domain}/**` and regenerate; do not copy schema in code.

## Notes / Gaps
- This inventory is based on code + docs visible in `docs/` and core shared/template paths.  
- If additional domain modules are added later, re-run this inventory to include new shared patterns.
- Docs reference `app.shared.adapters.database`, but the expected file path isn’t present; verify the current DB-normalization utility location before auditing duplication in that area.

## Non-DRY Code Snippets (Candidates for Consolidation)

- **Duplicate model customization extraction** — **Resolved (2026-01-13)**.
  - `app.template.backend.utils.model-customizations` now delegates extraction helpers to `app.shared.model-customizations` to avoid drift.
  - Schema-stripping helpers (`strip-*`) remain in the template backend namespace for migration/alignment tooling.

- **Multiple HTTP request builders (frontend)** — three overlapping request helpers with similar responsibilities.
  - **Progress (2026-01-13):** centralized error extraction (admin helper now delegates to `app.shared.http/extract-error-message`, and the shared extractor was expanded to cover common admin response shapes).
  - **Progress (2026-01-13):** admin helper token retrieval now prefers auth persistence (`app.admin.frontend.auth.persistence/get-persisted-token`) with fallback to raw localStorage for legacy sessions.
  - **Progress (2026-01-13):** request map construction (formats, params/body inclusion, and safe Content-Type behavior) centralized in `app.shared.http.core/build-xhrio-request`; both `admin-request` and `api-request` now delegate to it.
  - **Progress (2026-01-13):** template entity CRUD helpers are now split into explicit `*-public` vs `*-admin` callers; the legacy `create-entity/update-entity/delete-entity` wrappers are public-only.
  - **Progress (2026-01-13):** admin vs public routing decisions for generic CRUD now live in the CRUD bridge default request handler (route/path based), not inside the HTTP helper.
  - **Remaining:** consider deprecating/removing legacy `app.shared.http.core/build-request` once all call sites are migrated to `build-xhrio-request`.
  `src/app/template/frontend/api/http.cljs`:
  ```clojure
  (defn api-request [{:keys [method uri params body format response-format on-success on-failure timeout headers]}] ...)
  ```
  `src/app/admin/frontend/utils/http.cljs`:
  ```clojure
  (defn admin-request [{:keys [method uri params body format response-format headers timeout on-success on-failure token]}] ...)
  ```
  `src/app/shared/http/core.cljs`:
  ```clojure
  (defn build-request [db context method endpoint-path & [opts]] ...)
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

- **Query builder duplication (backend)** — both template admin and domain services implement their own query builders.
  `src/app/template/backend/utils/query_builders.clj`:
  ```clojure
  (defn add-pagination [query {:keys [limit offset]}] ...)
  (defn add-sorting [query {:keys [sort-by sort-order table-alias default-column]}] ...)
  ```
  `src/app/domain/backend/expenses/services/services_factory.clj`:
  ```clojure
  (defn build-query-with-filters [config {:keys [limit offset order-by order-dir]}] ...)
  (defn apply-search-filter [query search-fields search-term] ...)
  ```

- **Duplicate key conversion in form submission** — **Resolved (2026-01-13)**.
  - Centralized as `app.shared.model-naming/app-map-keys->db`.
  - Call sites now delegate to the shared helper:
    - `src/app/template/frontend/events/form.cljs`
    - `src/app/admin/frontend/events/users/template/form_interceptors.cljs`

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
