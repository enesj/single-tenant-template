---
mode: agent
description: "Audit DRY implementation in admin code and execute a prioritized refactor backlog without behavior changes."
---

# Admin DRY Audit + Refactor Backlog

## Summary
- Objective: audit how DRY is implemented across admin code and produce/execute a behavior-preserving refactor backlog.
- Scope: `src/app/admin/**` (backend + frontend) plus focused tests under `test/app/admin/**`.
- Keep work internal-first: reduce duplication without changing user-visible behavior.

## Scope Hotspots to Prioritize
- Frontend settings stacks:
  - `src/app/admin/frontend/events/settings/**`
  - `src/app/admin/frontend/events/user_settings/**`
- Frontend users stacks:
  - `src/app/admin/frontend/events/users/**`
- Backend admin services:
  - `src/app/admin/backend/services/admin/**`

## Public APIs / Interfaces / Types
- No external contract changes planned.
- Keep stable:
  - Admin HTTP endpoint contracts used by frontend events (e.g. `/admin/api/**` routes).
  - Existing Re-frame event and subscription keyword contracts consumed by admin pages/components.
  - Existing admin auth/session behavior.

## Implementation Plan
1. Baseline audit document.
- Create `tmp/admin-dry-audit.md` with:
  - current DRY mechanisms,
  - duplication evidence,
  - risk/ROI ranking (P1/P2/P3),
  - explicit “intentional non-DRY” areas.

2. Consolidate duplicated settings utility logic.
- Extract shared utility module for overlapping helpers currently split between:
  - `src/app/admin/frontend/events/settings/utils.cljs`
  - `src/app/admin/frontend/events/user_settings/utils.cljs`
- Preserve call-site behavior (map normalization, keyword normalization, safe defaults).

3. Consolidate repeated view-options mutation patterns.
- Extract shared pure helpers for repeated update/dissoc workflows currently duplicated in:
  - `src/app/admin/frontend/events/settings/view_options.cljs`
  - `src/app/admin/frontend/events/user_settings/view_options.cljs`
- Keep semantics identical for display defaults/locks and column defaults/locks.

4. Consolidate settings entity load/update event boilerplate.
- Reduce repeated load/success/failure/update patterns in:
  - `src/app/admin/frontend/events/settings/form_fields.cljs`
  - `src/app/admin/frontend/events/settings/table_columns.cljs`
  - `src/app/admin/frontend/events/settings/view_options.cljs`
- Prefer shared event-construction helpers where they do not obscure event intent.

5. Consolidate user-settings draft/saved state helpers.
- Reuse shared helpers for draft/saved access and per-entity cleanup across:
  - `src/app/admin/frontend/events/user_settings/load_save.cljs`
  - `src/app/admin/frontend/events/user_settings/view_options.cljs`
  - `src/app/admin/frontend/events/user_settings/table_columns.cljs`
  - `src/app/admin/frontend/events/user_settings/form_fields.cljs`

6. Consolidate users event pipeline helpers.
- Reduce duplication in loading/error/success + dual-store synchronization flows in:
  - `src/app/admin/frontend/events/users/utils.cljs`
  - `src/app/admin/frontend/events/users/core.cljs`
  - `src/app/admin/frontend/events/users/security.cljs`
  - `src/app/admin/frontend/events/users/status.cljs`
  - `src/app/admin/frontend/events/users/bulk_operations.cljs`

7. Backend service DRY pass for admin users domain.
- Audit and reduce overlap between façade and submodules:
  - `src/app/admin/backend/services/admin/users.clj`
  - `src/app/admin/backend/services/admin/users/management.clj`
  - `src/app/admin/backend/services/admin/users/security.clj`
  - `src/app/admin/backend/services/admin/users/bulk.clj`
  - `src/app/admin/backend/services/admin/users/validation.clj`
- Preserve operation boundaries and error semantics.

8. Post-refactor audit update.
- Extend `tmp/admin-dry-audit.md` with before/after duplication map and deferred items.

## Test Cases and Scenarios
- Run focused existing admin tests first:
  - `test/app/admin/frontend/events/settings_test.cljs`
  - `test/app/admin/frontend/events/user_settings_test.cljs`
  - `test/app/admin/frontend/events/users/core_test.cljs`
  - `test/app/admin/frontend/events/users/security_test.cljs`
  - `test/app/admin/frontend/events/users/status_test.cljs`
  - `test/app/admin/frontend/routes_test.cljs`
- Add focused tests for extracted shared helpers:
  - pure normalization/mutation helpers,
  - event helper builders,
  - edge-case handling for nil/empty/invalid inputs.
- Minimum validation matrix: happy path, nil, empty collections/maps, invalid/boundary inputs.

## Acceptance Criteria
- No admin UI behavioral regressions.
- No endpoint contract changes.
- Existing event/subscription consumers continue working unchanged.
- Measurable reduction in duplicated logic in prioritized hotspot files.

## Assumptions and Defaults
- No migration/schema work.
- No behavior change unless explicitly documented as bug fix.
- Temporary analysis artifacts remain under `tmp/`.
