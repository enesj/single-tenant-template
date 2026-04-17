# Multi-Column Sorting — Implementation Plan

> **Status**: In progress
> **Scope**: One-shot end-to-end migration from single-column sorting to canonical multi-column sorting
> **Decision**: No compatibility layer; frontend and backend transition together in one branch
> **Tracking**: This file is the implementation tracker and should be updated as work progresses

---

## Summary

Implement canonical multi-column sorting across the shared list system, including UIX interactions, re-frame state, client-side sorting, request serialization, backend parsing, SQL ordering, and focused tests. The final system should use one sort model only — `:sorts` — and one wire contract for both admin and user list endpoints.

---

## Key decisions

- [x] Full cutover only; do **not** keep `:sort` as a compatibility layer
- [x] Canonical frontend state will be `[:ui :lists <entity> :sorts]`
- [x] Sorting order will be user-manageable in the UI, not inferred magically
- [x] Backend and frontend must transition together
- [x] Finalized the query param contract for multi-sort as `sort=field:dir,field2:dir`
- [x] Cap active sort columns at `3`

---

## Progress snapshot

| # | Workstream | Depends On | Status |
| --- | --- | --- | --- |
| 0 | Finalize sort contract and UX rules | — | `[x]` |
| 1 | Frontend canonical `:sorts` state and events | 0 | `[x]` |
| 2 | Shared UIX header and sort-order controls | 1 | `[x]` |
| 3 | Client-side sorting parity for local/override lists | 1 | `[x]` |
| 4 | Frontend request serialization | 0, 1 | `[x]` |
| 5 | Backend parsing and route helpers | 4 | `[x]` |
| 6 | Query builder and service multi-order support | 5 | `[x]` |
| 7 | Focused tests | 1, 2, 3, 4, 5, 6 | `[~]` |
| 8 | Runtime verification on admin + user receipts | 7 | `[ ]` |

```text
0 ──→ 1 ──→ 2
│     └──→ 3
└──→ 4 ──→ 5 ──→ 6 ──→ 7 ──→ 8
```

---

## Workstream 0 — Finalize sort contract and UX rules

**Goal**: Lock the canonical state shape, click semantics, and wire format before implementation.

### Workstream 0 tasks

- [ ] Confirm canonical state shape:
  - `[:ui :lists <entity> :sorts]`
  - Example: `[{:field :status :direction :asc} {:field :created-at :direction :desc}]`
- [ ] Confirm header click semantics:
  - plain click = set/toggle primary sort
  - Shift+click = append/toggle additional sort
- [ ] Confirm explicit sort-order controls in UI:
  - active sort chips
  - remove action
  - move left / move right
  - clear all
- [ ] Confirm final request contract sent to the backend
- [ ] Confirm stable tie-breaker policy for paginated lists

### Workstream 0 files likely touched

- `src/app/template/frontend/components/list/table.cljs`
- `src/app/template/frontend/components/list.cljs`
- `src/app/template/frontend/db/paths.cljs`
- `src/app/domain/backend/expenses/handlers/user_expenses/helpers.clj`
- `src/app/template/backend/routes/admin/utils.clj`

---

## Workstream 1 — Frontend canonical `:sorts` state and events

**Goal**: Replace single-column sort state with canonical ordered multi-sort state in re-frame.

### Workstream 1 tasks

- [ ] Add/replace path helpers for `:sorts`
- [ ] Replace single-sort event logic with multi-sort stack event logic
- [ ] Reset pagination to page 1 whenever sort stack changes
- [ ] Add explicit events for:
  - set/toggle primary sort
  - append/toggle sort
  - remove sort
  - move sort left/right
  - clear sorts
- [ ] Update subscriptions to expose ordered active sorts cleanly

### Workstream 1 files likely touched

- `src/app/template/frontend/db/paths.cljs`
- `src/app/template/frontend/events/list/ui_state.cljs`
- `src/app/template/frontend/subs/list.cljs`

---

## Workstream 2 — Shared UIX header and sort-order controls

**Goal**: Make multi-sort visible and manageable in the shared list UI.

### Workstream 2 tasks

- [ ] Update shared table header to show active sort direction
- [ ] Add active sort priority badges (e.g. `1`, `2`, `3`)
- [ ] Pass modifier-aware click handling from the header
- [ ] Add a sort chip strip above the table
- [ ] Add chip actions:
  - remove
  - move left/right
  - clear all
- [ ] Preserve stable IDs for any new interactive elements

### Workstream 2 files likely touched

- `src/app/template/frontend/components/list/table.cljs`
- `src/app/template/frontend/components/list.cljs`
- `src/app/template/frontend/components/list/ui.cljs`

---

## Workstream 3 — Client-side sorting parity

**Goal**: Ensure local sorting in client-rendered lists matches the new multi-sort semantics.

### Workstream 3 tasks

- [ ] Replace single-field local sorting in rows-override mode
- [ ] Apply ordered comparator chaining for `:sorts`
- [ ] Preserve consistent nil ordering
- [ ] Keep client-side behavior aligned with eventual backend ordering rules

### Workstream 3 files likely touched

- `src/app/template/frontend/components/list/overrides.cljs`
- `src/app/template/frontend/subs/entity.cljs`
- `src/app/template/frontend/subs/list.cljs`
- `src/app/template/frontend/components/list.cljs`

---

## Workstream 4 — Frontend request serialization

**Goal**: Replace single-sort request params with canonical ordered multi-sort serialization.

### Workstream 4 tasks

- [ ] Remove singular `:order-by` / `:order-dir` serialization from shared helpers
- [ ] Add a canonical multi-sort request serializer
- [ ] Update user-expenses list request helpers
- [ ] Update generic admin/domain list request helpers

### Workstream 4 files likely touched

- `src/app/template/frontend/db/paths.cljs`
- `src/app/domain/frontend/expenses/events/user_expenses/list_support.cljs`
- `src/app/domain/frontend/expenses/events/events_factory.cljs`

---

## Workstream 5 — Backend parsing and route helpers

**Goal**: Parse the new multi-sort request contract on the backend and normalize it safely.

### Workstream 5 tasks

- [ ] Replace singular sort param parsing in user-expenses helpers
- [ ] Replace singular sort extraction in shared admin route helpers
- [ ] Normalize fields to app keywords before allowlist checks
- [ ] Ignore or reject malformed sort entries safely

### Workstream 5 files likely touched

- `src/app/domain/backend/expenses/handlers/user_expenses/helpers.clj`
- `src/app/template/backend/routes/admin/utils.clj`

---

## Workstream 6 — Query builder and service multi-order support

**Goal**: Support ordered multi-column `ORDER BY` generation across shared and bespoke list queries.

### Workstream 6 tasks

- [ ] Extend query builder helpers to apply multiple order clauses
- [ ] Update generic list service factory to accept ordered multi-sort input
- [ ] Update bespoke list services still using singular ordering
- [ ] Append stable tie-breakers for deterministic pagination
- [ ] Preserve allowlist-first behavior for sortable fields

### Workstream 6 files likely touched

- `src/app/shared/query_builders.clj`
- `src/app/domain/backend/expenses/services/services_factory.clj`
- `src/app/domain/backend/expenses/services/articles/crud.clj`
- `src/app/domain/backend/expenses/services/receipts/queries.clj`

---

## Workstream 7 — Focused tests

**Goal**: Lock down the transition with focused tests instead of relying on manual verification.

### Workstream 7 tasks

- [ ] Update frontend event tests for multi-sort stack behavior
- [ ] Add DOM tests for header badges and sort chip controls
- [ ] Update local sorting tests for ordered comparator chaining
- [ ] Update request serialization tests for the new sort contract
- [ ] Add backend parsing tests for multi-sort input
- [ ] Add/query-builder tests for generated HoneySQL `:order-by` clauses

### Workstream 7 files likely touched

- `test/app/template/frontend/events/list/ui_state_test.cljs`
- `test/app/template/frontend/components/list_vector_mode_dom_test.cljs`
- `test/app/template/frontend/list_operations_test.cljs`
- `test/app/domain/frontend/expenses/events/user_expenses/pagination_lists_test.cljs`
- backend/shared test namespaces adjacent to the touched Clojure files

---

## Workstream 8 — Runtime verification

**Goal**: Verify real behavior on both admin and user flows once focused tests pass.

### Workstream 8 tasks

- [ ] Verify admin receipts multi-sort behavior
- [ ] Verify user receipts multi-sort behavior
- [ ] Verify row order before and after refresh matches server output
- [ ] Verify reorder/remove/clear controls update UI and server requests consistently
- [ ] Verify no regressions in single-column usage after the cutover

### Workstream 8 suggested targets

- Admin receipts list
- User receipts list

---

## Edge cases

- [ ] No active sorts → fallback to entity default ordering
- [ ] Empty result set → no errors, stable request/response handling
- [ ] Duplicate sort field in input → dedupe deterministically
- [ ] Unknown field → safe fallback / allowlist rejection
- [ ] Invalid direction → normalize or ignore safely
- [ ] Namespaced / snake_case field inputs → normalize before allowlisting
- [ ] Computed sort fields (like receipt status) still map to correct SQL expressions
- [ ] Mixed asc/desc ordering preserves declared priority exactly
- [ ] Nil values sort consistently across client and server paths

---

## Validation checklist

- [ ] REPL-check frontend sort state transitions
- [ ] REPL-check backend sort parsing and query building
- [ ] Run focused frontend tests
- [ ] Run focused backend/shared tests
- [ ] Save shell test output once under `tmp/` if shell commands are used
- [ ] Manually verify admin receipts list
- [ ] Manually verify user receipts list

---

## Constraints

- Clojure/EDN edits must use `clojure-mcp` structural editing tools
- No DB schema changes are expected for this task
- DB access/query inspection must use the repo-approved database tooling only
- Temporary artifacts go under `tmp/`
- Keep changes focused; do not mix unrelated refactors into this migration
- Preserve stable `:id` attributes for new interactive UI controls

---

## Open questions / assumptions

- [ ] Final wire format still needs one explicit decision
- [ ] Need to confirm whether max active sort count should be capped
- [ ] Need to confirm whether any bespoke list pages outside the shared path require extra migration work
- [x] Assumption: entity-specific default ordering remains after all user sorts are cleared
- [x] Assumption: no compatibility layer will be kept
- [x] Assumption: backend and frontend ship together

---

## Progress log

- **2026-04-17** — Initial implementation plan created before coding. Agreed on a full one-shot transition with no `:sort` compatibility layer, and on tracking progress in this file during implementation.
- **2026-04-18** — Core multi-sort cutover is now in place across the shared frontend list state/UI, canonical request serialization (`sort=field:dir,...`), backend sort parsing, shared query helpers, and the main backend list services/handlers.
- **2026-04-18** — Fixed a receipts regression where the user-facing `Datum kupovine` / `:purchased-at-guess` column was exposed in the UI but missing from the backend receipts sort allowlist. Added focused route/service regression coverage for canonical receipts sort parsing and purchase-date ordering. Focused backend validation passes and is saved in `tmp/be-receipts-sort-validation.txt`.
