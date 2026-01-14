# Plan: db/paths.cljs Refactor + app-db Alignment

## Goals
- Identify unused/redundant path helpers in `src/app/template/frontend/db/paths.cljs`.
- Ensure path helpers align with the actual app-db shape defined in defaults and schemas.
- Add missing path helpers for paths that are currently accessed directly.
- Prepare a consistent, canonical set of paths for lists/forms/routes/UI state.

## Phase 1 — Inventory & Alignment Map (Done)
1. **Inventory current path helpers**
   - Reviewed `paths/*` fns and usage across repo.
   - Unused helpers identified and cleaned up during refactor.
2. **Inventory direct app-db paths (no helper)**
   - Routes: `[:current-route :data :name]`.
   - UI: `[:ui :current-page]`, `[:ui :entity-configs ...]`, `[:ui :entity-prefs ...]`.
   - Forms: `[:forms <entity> :submitting?]`, `[:forms <entity> :values]`.
   - Lists: `[:ui :lists <entity> :filters]`, `:selected-ids`.
   - Fetch tracking: `[:entity-fetches <entity> <id>]`.
3. **Compare with canonical state definitions**
   - `src/app/template/frontend/db/defaults.cljs` and `src/app/template/frontend/db/schemas.cljs`.
   - Mismatch confirmed: **Forms** use `:values` in code; schema used `:data`.
   - Lists: redundant pagination shape (top-level + nested) confirmed.

## Phase 2 — Decide Canonical Shapes (Done)
1. **Forms**
   - Canonical key: `:values` (matches Fork + current usage).
   - Schema updated to `:values`.
2. **Lists**
   - Canonical read path: **top-level** `:current-page` in list UI state.
   - Keep nested `:pagination` as compatibility (still synced).

## Phase 3 — Expand `db/paths.cljs` (Done)
Added helpers for missing, **actually used** paths:
- `current-route-name`
- `form-values`
- `list-filters`
- `entity-prefs-*` helpers used by settings/subs
- `entity-fetches`

Removed unused/redundant helpers during cleanup (see Phase 5).

## Phase 4 — Migrate Call Sites (Done)
1. Replaced raw vectors with helpers:
   - `[:ui :current-page]` → `paths/current-page`.
   - `[:current-route :data :name]` → `paths/current-route-name`.
   - `[:ui :entity-configs ...]` → `paths/entity-display-settings` + `conj`.
   - `[:forms <entity> :submitting?]` → `paths/form-submitting?`.
   - `[:forms <entity> :values]` → `paths/form-values`.
   - `[:ui :lists <entity> :filters]` → `paths/list-filters`.
   - `[:ui :lists <entity> :selected-ids]` → `paths/entity-selected-ids`.
   - `[:entity-fetches <entity> <id>]` → `paths/entity-fetches` + `conj`.
2. Pagination fallbacks retained for compatibility (no behavior change yet).

## Phase 5 — Cleanup Unused Helpers (Done)
- Removed unused helpers: `entity-last-updated`, `entity-success`, `form-field`, `form-server-errors-all`, and unused list helpers added during draft.

## Phase 6 — Validation & Safety Checks (Pending)
1. Run app-db validation (via REPL if needed) to confirm schema alignment.
2. Smoke-test key list + form flows in admin UI.
3. Run focused CLJS tests if any cover list/form subs or events.

## Notes / Known Mismatches to Resolve
- Form state now aligned on `:values` across schema/paths/code.
- `paths/list-current-page` now points at top-level; nested `:pagination` remains for compatibility.
