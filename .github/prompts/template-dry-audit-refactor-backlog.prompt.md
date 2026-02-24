---
mode: agent
description: "Audit DRY implementation in template code and execute a prioritized refactor backlog without behavior changes."
---

# Template DRY Audit + Refactor Backlog

## Summary
- Objective: audit how DRY is implemented across template code and produce/execute a behavior-preserving refactor backlog.
- Scope: `src/app/template/**` (backend + frontend + shared template bridges/utilities) plus focused tests under `test/app/template/**`.
- Keep all work internal: reduce duplication while preserving public behavior and contracts.

## Scope Hotspots to Prioritize
- Template frontend event/subscription stacks:
  - `src/app/template/frontend/events/**`
  - `src/app/template/frontend/subs/**`
- Template frontend list/filter/render pipeline:
  - `src/app/template/frontend/components/list/**`
  - `src/app/template/frontend/components/filter/**`
  - `src/app/template/frontend/subs/ui.cljs`
- Template backend route/helper stacks:
  - `src/app/template/backend/routes/**`
  - `src/app/template/backend/routes/admin/**`
- Template shared CRUD bridges and entity normalization:
  - `src/app/template/frontend/shared/bridges/crud.cljs`
  - `src/app/template/frontend/state/normalize.cljs`
  - `src/app/template/frontend/shared/utils/entity.cljs`

## Public APIs / Interfaces / Types
- No external contract changes planned.
- Keep stable:
  - `/api/v1/**` and `/admin/api/**` endpoint contracts exposed by template routes.
  - Re-frame event/subscription keywords consumed by template/admin/domain pages.
  - Template CRUD bridge event contracts (`register-template-crud-events!` consumers).
  - Entity schema expectations used by template list/form components.

## Implementation Plan
1. Baseline audit document.
- Create `tmp/template-dry-audit.md` with:
  - current DRY mechanisms,
  - duplication evidence,
  - risk/ROI ranking (P1/P2/P3),
  - intentional non-DRY exceptions.

2. Consolidate normalization utility overlap.
- Audit and reduce duplicated normalization patterns across:
  - `src/app/template/frontend/db/schemas.cljs`
  - `src/app/template/frontend/db/entity_specs.cljs`
  - `src/app/template/frontend/subs/ui.cljs`
  - `src/app/template/frontend/components/list/fields.cljs`
  - `src/app/template/frontend/utils/column_config.cljs`
- Extract shared pure helpers where safe; preserve exact key normalization semantics.

3. Consolidate list/filter matching logic.
- Reduce duplicated filter/matching usage across:
  - `src/app/template/frontend/components/list.cljs`
  - `src/app/template/frontend/subs/list.cljs`
  - `src/app/template/frontend/subs/entity.cljs`
  - `src/app/template/frontend/components/filter/helpers.cljs`
- Keep current filtering behavior and UI output unchanged.

4. Consolidate template list rendering config derivation.
- Reduce overlap between:
  - `src/app/template/frontend/components/list/table.cljs`
  - `src/app/template/frontend/components/list/rows.cljs`
- Extract shared derivation helpers for visible columns / policy defaults / lock handling.

5. Consolidate route/admin utility duplication.
- Audit helper overlap in template backend route layers:
  - `src/app/template/backend/routes/crud.clj`
  - `src/app/template/backend/routes/admin/utils.clj`
  - `src/app/template/backend/routes/admin/entities.clj`
  - `src/app/template/backend/routes/api.clj`
- Centralize reusable parsing/response/helper logic without changing route behavior.

6. Consolidate entity normalization and bridge glue.
- Reduce overlap between:
  - `src/app/template/frontend/state/normalize.cljs`
  - `src/app/template/frontend/shared/utils/entity.cljs`
  - `src/app/template/frontend/utils/state.cljs`
- Preserve normalized entity shape and existing consumer assumptions.

7. Reduce frontend event boilerplate in template list/form events.
- Audit and extract shared event helper patterns across:
  - `src/app/template/frontend/events/list/**`
  - `src/app/template/frontend/events/form.cljs`
  - `src/app/template/frontend/events/auth/**`
- Keep event keyword contracts and dispatch behavior unchanged.

8. Post-refactor audit update.
- Extend `tmp/template-dry-audit.md` with before/after duplication map and deferred items.

## Test Cases and Scenarios
- Run focused existing template tests first:
  - `test/app/template/backend/routes/admin/settings_io_test.clj`
  - `test/app/template/backend/routes/admin/utils_test.clj`
  - `test/app/template/frontend/events/form_events_test.cljs`
  - `test/app/template/frontend/events/list/filters_test.cljs`
  - `test/app/template/frontend/events/list/ui_state_test.cljs`
  - `test/app/template/frontend/subs/entity_test.cljs`
  - `test/app/template/frontend/subs/list_test.cljs`
  - `test/app/template/frontend/subs/ui_test.cljs`
  - `test/app/template/frontend/components/filter/helpers_test.cljs`
  - `test/app/template/frontend/utils/column_config_test.cljs`
- Add focused tests for newly extracted helpers:
  - normalization helper behavior,
  - list/filter derivation helpers,
  - route/helper utility behavior.
- Minimum validation matrix: happy path, nil, empty collections/maps, invalid/boundary inputs.

## Acceptance Criteria
- No API contract regressions.
- No UI behavior regressions in template list/form/filter/subscription flows.
- Existing event/subscription consumers continue working unchanged.
- Measurable reduction in duplicated logic in prioritized template files.

## Assumptions and Defaults
- No migration/schema work.
- No behavior change unless explicitly documented as bug fix.
- Temporary analysis artifacts remain under `tmp/`.
