# Fallback Implementation Inventory — Admin and Domain

Generated: 2026-05-01

Updated: 2026-05-01 — highest-confidence removal candidates implemented; medium-confidence API/sort cleanup implemented where safe.

## Scope

This inventory covers fallback, compatibility, legacy, no-op, shim, and transitional implementations in:

- `src/app/admin/**`
- `src/app/domain/**`

A scan for the exact spelling `failback` found no matches. The inventory below uses `fallback` broadly, including compatibility facades and legacy migration paths.

## Method

Evidence used:

- Broad marker scan saved to `tmp/domain-admin-fallback-marker-scan.txt`.
  - Markers included: `fallback`, `legacy`, `compat`, `backward`, `deprecated`, `shim`, `workaround`, `temporary`, `transitional`, `cutover`, `no-op`, `tolerate`, and related wording.
- Targeted reads of high-signal files and reference searches for likely-removable shims.
- Exact `failback` spelling search:
  - `rg -n -i "failback" src/app/admin src/app/domain`
  - Result: no matches.

The scan produced 360 marker hits. Many are legitimate runtime resilience paths, so this report focuses on removability rather than counting every fallback expression.

## Removal candidates

### Highest confidence — implemented

These compatibility/no-op paths were removed after local call sites were updated.

| Candidate | Location | What changed |
| --- | --- | --- |
| No-op entity config loader event `::load-entity-configs` | `src/app/admin/frontend/events/config.cljs` | Removed the event and removed its bootstrap dispatch. Config bootstrap now dispatches only `::async-load-configs`. |
| No-op admin single-entity refresh event `:admin/refresh-entity` | `src/app/admin/frontend/events/entity_sync.cljs` | Removed the no-op event and removed dispatches from domain admin mutation flows. List reload/list-sync paths remain active. |
| No-op deletion-constraints hook `use-deletion-constraints` | `src/app/admin/frontend/handlers/generic.cljs`, `src/app/admin/frontend/components/generic_admin_entity_page.cljs` | Removed the hook, its call, and now-unused `id-utils`/`use-effect` plumbing. |
| `create-test-data!` multi-tenant setup placeholder | `src/app/admin/backend/setup.clj`, `src/app/template/backend/migrations/simple_repl.clj` | Removed the placeholder function and cleaned up the REPL helper comment example. |
| Legacy batch action dispatch bridge in `create-generic-selection-handler` | `src/app/admin/frontend/handlers/generic.cljs`, `src/app/admin/frontend/components/generic_admin_entity_page.cljs` | Removed the unused selection handler bridge and generic `:admin/show-batch-actions` / `:admin/hide-batch-actions` state. Active user/audit batch actions remain registered where they are actually used. |
| Backward-compatible admin column events `:admin/toggle-column-visibility` and `:admin/reorder-columns` | `src/app/admin/frontend/events/config.cljs`, template callers/tests | Updated callers to dispatch canonical namespaced events and removed the legacy proxy events. |

Post-removal exact search for these symbols/events produced no matches in `src/app` or `test/app`.

### Medium confidence — partially implemented

These candidates had mixed evidence. Code-only compatibility paths with clear canonical replacements were removed; boundary/data-shape fallbacks remain until their persisted data or public API risk is addressed.

#### Implemented in the medium-confidence pass

| Candidate | Location | What changed |
| --- | --- | --- |
| Legacy admin email column preference migration | `src/app/admin/frontend/adapters/admins.cljs`, adapter tests | Removed local app-db persisted preference migration from `:email-masked` to `:email`. The admin adapter now only initializes current list UI state. Runtime table-column config migration remains in `table_columns.cljs` / backend settings I/O because current config still references privacy/reference fields. |
| Legacy audit visible-order migration | `src/app/admin/frontend/adapters/audit.cljs`, adapter tests | Removed local app-db migration from the old audit visible order to enhanced audit columns. Audit adapter initialization now only seeds current server-list UI state. |
| Backward-compatible `:admin/entity-spec` subscription | `src/app/admin/frontend/specs/generic.cljs`, `src/app/template/frontend/shared/utils/entity.cljs`, admin spec tests | Updated template entity-spec proxy subscriptions and tests to use canonical `:admin/entity-specs-by-name`; removed the alias subscription. |
| Compatibility helper `primary-user-ui-config-paths` | `src/app/domain/backend/registry.clj`, `src/app/template/backend/routes/admin/settings_bootstrap.clj`, registry tests | Updated user runtime-config bootstrap to consume all maps from `get-ui-config-paths`; removed the first-domain compatibility helper and its tests. |
| Legacy API route aliases `/api/v1/expenses/summary`, `/by-month`, `/by-supplier` | `src/app/domain/backend/expenses/routes/user_api.clj`, frontend web/mobile callers, docs/tests | Moved consumers to canonical `/api/v1/expenses/reports/summary`, `/reports/by-month`, and `/reports/by-supplier`; removed the top-level legacy routes. |
| User-expenses sort parameter tolerance for `order-by` / `order-dir` | `src/app/domain/backend/expenses/handlers/user_expenses/helpers.clj`, receipts route tests, docs | `parse-sort-params` now accepts only canonical `sort=field:dir` at the HTTP boundary while still returning `:order-by` / `:order-dir` for existing internal service option maps. |

Post-removal exact search for these removed symbols/events produced no matches in `src/app` or `test/app` except the still-intentional table-column/settings I/O legacy normalizers documented below.

#### Deferred medium-confidence candidates

| Candidate | Location | Current purpose | Evidence / risk | Removal suggestion |
| --- | --- | --- | --- | --- |
| Legacy table-column config normalization for audit/admin/users | `src/app/admin/frontend/events/settings/table_columns.cljs`, `src/app/template/backend/routes/admin/settings_io.clj` | Normalizes old config shapes: `admin-email`, `admin-name`, `email-masked`, `user-ref`. | Current `config/frontend-config-allowlist.edn`, admin `table-columns.edn`, and backend settings I/O tests still include privacy/reference fields such as `user-ref` and `email-masked`; not all occurrences are legacy-only. | **Keep for now.** Dig deeper only as a dedicated privacy/config migration: first split current privacy fields from true legacy aliases, then audit `frontend_runtime_configs` before removing normalizers. |
| User/global role fallback in auth helpers | `src/app/domain/backend/expenses/handlers/user_expenses/helpers.clj` | Falls back to global user role for legacy/pre-tenant sessions. | Security-sensitive, used by many handlers via `ensure-role` / `tenant-elevated?`, and explicitly covered by role fallback tests. Template auth/middleware still contains compatibility role fallbacks too. | **Keep unless the product/security contract changes.** If removing, make it a tenant-auth hardening task: prove all sessions include membership roles, update middleware/contracts, then change tests to require membership role. |

### Lower confidence — likely architectural facades, not immediate removals

These are compatibility facades that still have real call sites. They are candidates for gradual refactoring, not quick deletion.

| Facade | Location | Current usage / note | Recommendation |
| --- | --- | --- | --- |
| Articles service facade | `src/app/domain/backend/expenses/services/articles.clj` | Used by config maps, handlers, routes, OCR extraction, article aliases. | Keep until callers are migrated to `services.articles.service` / focused modules. |
| Stores service facade | `src/app/domain/backend/expenses/services/stores.clj` | Used by route configs and OCR extraction modules. | Keep for now; remove only after route configs and OCR call sites target focused namespaces directly. |
| Search handlers facade | `src/app/domain/backend/expenses/handlers/search.clj` | Used by `routes/core.clj` and `routes/user_api.clj`. | Keep unless routes are rewritten to require focused search namespaces directly. |
| Receipt OCR core facade | `src/app/domain/backend/expenses/workers/receipt_ocr/core.clj` | Used by user receipts and admin receipt routes. | Keep until route/handler call sites use `runner` and `ui-queue` directly. |
| Receipt OCR markdown facade | `src/app/domain/backend/expenses/workers/receipt_ocr/markdown.clj` | Used by OCR extraction modules for header/text/item helpers. | Keep unless all extraction modules import focused markdown namespaces directly. |
| User power forms facade | `src/app/domain/frontend/expenses/components/user_power_forms.cljs` | Used broadly by user pages with `:refer` imports. | Keep until pages import focused `article-forms`, `category-forms`, and `reference-forms` directly. |
| Re-frame event aggregate facades | `src/app/domain/frontend/expenses/events/user_expenses/reports.cljs`, `crud.cljs`, `receipts.cljs` | Required by the top-level aggregator to load sub-event registrations. | Keep unless the top-level aggregator requires sub-namespaces directly. |
| Service config facade | `src/app/domain/backend/expenses/services/service_configs.clj` | Re-exports registry/normalization and preserves service-registration side effects. | Keep unless all consumers are migrated and service registration side effects are moved to an explicit startup path. |

## Fallbacks that should probably stay

These are not good removal candidates because they provide runtime resilience, data recovery, or user-facing robustness.

| Implementation | Location | Why keep |
| --- | --- | --- |
| Exchange-rate provider fallback and cached-rate failover | `src/app/domain/backend/expenses/services/exchange_rates.clj` | Direct CBBH HTML, Serper fallback, and cached-rate fallback protect expense conversion from provider outages and create admin-visible alerts. |
| Receipt image preprocessing fallback | `src/app/domain/backend/expenses/services/receipts/image_preprocess.clj` | Falls back to original bytes when ImageMagick is unavailable or preprocessing fails, avoiding unnecessary OCR pipeline failure for non-HEIC images. |
| Supplier OCR / legacy matching fallbacks | `src/app/domain/backend/expenses/services/suppliers.clj`, `services/suppliers/legacy_matching.clj` | Protects OCR/import paths from duplicate suppliers caused by legal suffixes, branch suffixes, and older normalization. Remove only after data cleanup proves no legacy normalized keys remain. |
| Store resolution fallback without location evidence | `src/app/domain/backend/expenses/services/stores/resolution.clj` | Uses the only existing store or creates a supplier-named store when merchant data lacks usable location evidence. Removing could reduce receipt auto-posting quality. |
| City extraction fallbacks | `src/app/domain/backend/expenses/services/cities_resolver.clj`, `cities_normalize.clj` | ZIP/name heuristics and Places fallback improve city resolution from messy receipt addresses. Removing would likely increase missing city IDs. |
| Dynamic UI config fallback specs | `src/app/domain/frontend/expenses/admin/adapters/specs.cljs`, user form components, admin generic specs | Prevents blank/broken forms while DB-backed config is still loading. Keep unless runtime config loading is guaranteed before all consumers mount. |
| Currency option fallback | `src/app/domain/frontend/expenses/ui/currencies.cljs` | Provides BAM/EUR/USD options when profile/global settings are not loaded yet. Good user-facing resilience. |
| SPA fallback routes | `src/app/domain/shared/routes/expenses_user.cljc` | These are router/browser-refresh fallbacks, not dead compatibility code. Keep. |

## Suggested removal order

1. Retire legacy preference migrations after a persisted-settings audit:
   - admin email column migration
   - audit visible-order migration
   - table-column config legacy normalizers.
2. Migrate facade call sites to focused namespaces where it improves clarity.
3. Leave operational/data-quality fallbacks in place unless product requirements explicitly prefer hard failure over best-effort behavior.

## Validation before removing candidates

For any removal PR, run at minimum:

- `bb unused-public-var --domain --admin`
- `clj-kondo --parallel --cache false --lint src/app/admin src/app/domain`
- `npx shadow-cljs compile admin`
- `npx shadow-cljs compile app`

For backend route/service facade removals, also require focused backend namespace loading or route tests. For UI preference migration removals, audit persisted settings first so old browser/DB prefs are not silently ignored.
