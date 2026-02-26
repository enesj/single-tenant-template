# Allium Review Report — Shared Code — 2026-02-26

## 1) Allium review verdict

**misaligned**

Primary reason: `specs/allium/shared/` is empty — no authoritative spec exists to validate shared-contract changes against. Additionally, three concrete behavior issues were identified that conflict with or outpace existing specs.

---

## 2) Evidence

### Changed files reviewed

- `src/app/shared/auth.cljc` — partial role string-constant removal
- `src/app/shared/data.cljc` — `get-server-port` hardened to throw on missing config
- `src/app/shared/model_customizations.cljc` — bulk deletion of admin/form customization extraction functions
- `src/app/shared/field_metadata.cljc` — new namespace (field type resolution, model introspection)
- `src/app/shared/pagination.cljc` — new `paginate-with-sort` + `paginate` public API aliases
- `src/app/shared/patterns.cljc` — converted to re-export facade (sub-namespaces: `patterns/auth`, `patterns/date_time`, etc.)
- `src/app/shared/specs/entities.cljc` — new Malli schemas for admin/user entities.edn
- `src/app/shared/specs/form_fields.cljc` — new Malli schemas for form-fields.edn
- `src/app/shared/specs/view_options.cljc` — new Malli schemas for view-options.edn
- `src/app/shared/validation/builder.cljc` — references still-required `validation/metadata` and `validation/unique`
- `src/app/shared/type_conversion.cljc` — unchanged behavior; `cast-for-database` etc. re-exported from `type-conversion-db`

### Spec files consulted

- `specs/allium/template/dry-principle.allium`
- `specs/allium/template/platform-boundaries.allium`
- `specs/allium/template/domain-architecture.allium`
- `specs/allium/domain/expenses/implementation.allium`
- `specs/allium/README.md`
- `specs/allium/shared/` — **empty** (no shared specs exist)

---

## 3) Precise mismatch list

### M1 — Spec-coverage gap: no `specs/allium/shared/` spec exists

The entire `specs/allium/shared/` directory is empty. The following stable shared contracts changed in this branch with no spec to verify against:

- Role/auth utility API (`auth.cljc`)
- Pagination math and public API (`pagination.cljc`)
- Field metadata and type resolution (`field_metadata.cljc`)
- Shared Malli config-file specs (`specs/entities`, `specs/form_fields`, `specs/view_options`)
- Model customization extraction (deleted from `model_customizations.cljc`)

All findings below are observable behavior issues; none can be formally flagged as spec violations because no spec exists.

---

### M2 — `auth.cljc`: orphaned role concepts after partial string-constant removal

- **Removed**: `role-owner`, `role-viewer`, `role-unassigned` string constants (DB-friendly)
- **Still present**: `core-roles #{:owner :admin :member :viewer :unassigned}` and `core-role-hierarchy [:unassigned :viewer :member :admin :owner]`
- **Effect**: The keyword forms `:owner`, `:viewer`, `:unassigned` remain as live role concepts, but their DB-serializable string equivalents (`"owner"`, `"viewer"`, `"unassigned"`) are gone. Any caller that previously used `role-owner` / `role-viewer` / `role-unassigned` as DB comparison strings will silently fail (var-not-found at compile time, not runtime). The removal is incomplete: either `core-roles` and `core-role-hierarchy` should be narrowed to match (`:admin`, `:member` only), or the string constants should be restored.

---

### M3 — `specs/view_options.cljc` and `specs/entities.cljc`: duplicated display-toggle schema

- `DisplayTogglesMap` in `specs/view_options.cljc` (lines 17–32) defines all `:show-*?` booleans and `:per-page`.
- `AdminDisplaySettings` in `specs/entities.cljc` (lines 56–76) defines the **identical** set of keys and constraints.
- These are two independent Malli schemas for the same logical contract.
- **Effect**: A toggle added to one won't automatically be validated in the other. This contradicts `dry-principle.allium` (`AdapterUtilitiesEliminatePerEntityGlueDuplication`).

Additionally, `:per-page` can now appear in **three** places for a given entity:
  1. At `EntityViewOptions` top level (`:per-page`)
  2. Inside `PaginationConfig` (`:default-page-size`)
  3. Inside `DisplayTogglesMap` / `AdminDisplaySettings` (`:per-page`)

No spec or code documents which takes precedence.

---

### M4 — `field_metadata.cljc`: empty section stubs

The new file has four section headers with no functions beneath them:
- `Field Type Utilities` (line 136)
- `Validation Utilities` (line 139)
- `Debug and Inspection Utilities` (line 142)

These are scaffolding skeletons committed to the codebase. They imply planned behavior that isn't yet implemented and create misleading structure for future readers.

---

### M5 — `pagination.cljc`: `paginate-with-sort` introduces client-side fallback risk

A new `paginate-with-sort` function applies in-memory sort + `paginate-collection`. The fallback audit (P1, completed in this branch) eliminated all `{:fetch-limit 1000}` dispatches and moved to server-side pagination. Adding a client-side sort+paginate utility to shared code creates a footgun: future callers may use it on unbound collections, reintroducing the problem that was just fixed. No spec governs client-side vs server-side pagination semantics in shared utilities.

---

## 4) Recommended fix direction

### Short term (before commit)

- **M2**: Narrow `core-roles` and `core-role-hierarchy` in `auth.cljc` to match the removed string constants (keep only `:admin` and `:member` if `:owner`/`:viewer`/`:unassigned` are retired), OR restore `role-owner`/`role-viewer`/`role-unassigned` string constants. Document which roles are active in the single-tenant context.
- **M3**: Extract a single shared `DisplayTogglesSchema` in `specs/view_options.cljc` and import/reuse it in `specs/entities.cljc` via namespace require. Remove the duplicate definition.
- **M4**: Remove the empty section stubs from `field_metadata.cljc` until the functions exist.

### Spec additions required (close spec-coverage gap)

Add `specs/allium/shared/auth-roles.candidate.allium` to model:
- active role set and hierarchy
- string-constant contract for DB serialization
- `role->keyword` / `role->string` normalization guarantee

Add `specs/allium/shared/pagination.candidate.allium` to model:
- `paginate` public API shape
- `page->offset` / `offset->page` contract
- explicit guidance: `paginate-with-sort` is client-side only, not a substitute for server-side pagination

Add `specs/allium/shared/config-file-specs.candidate.allium` to model:
- shared Malli contract for display-toggles (single source)
- `EntityViewOptions` three-way `:per-page` precedence rule

---

## 5) Residual risks

- `validation/builder.cljc` still requires `validation/metadata` and `validation/unique` (both had lines deleted). If those files are now empty or missing exports, the builder would fail at compile time. Not confirmed — validation/metadata.cljc and validation/unique.cljc may still exist with reduced content, but should be verified.
- `model_customizations.cljc` deleted `extract-all-admin-customizations` and related functions. If any domain or admin code still calls these (even indirectly via a service), the error would appear only at runtime (nil/NPE, not compile-time). A grep for callers was not performed in this pass.
- `data.cljc` `get-server-port` now throws; any test or local dev setup that previously relied on the `8080` default will now error at boot. Config files (`config/base.edn`) must supply `:webserver :port` explicitly — this is already the case per `config/base.edn` (port 8085), so risk is low but worth flagging.

---

## 6) Commit status

**not committed** — misalignment found:
- M2 (auth orphaned roles) requires resolution before merge.
- M3 (DisplayTogglesMap duplication) is a DRY violation.
- M4 (empty stubs) should be cleaned up.
- M1 (no shared specs) requires at least candidate spec additions before the shared contract is considered reviewable.
