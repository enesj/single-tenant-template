# Admin Receipts — Filters & Sorting Fix Plan

## Context

`/admin/receipts` had broken filtering and (partially) sorting. Root causes spanned
three layers: the frontend events factory was dropping select-shaped values, the
admin receipts list handler was ignoring text/date filter params, and the select
dropdown panel's `z-10` lost the stacking war against the sticky table header at
`z-index: 300`.

## Already applied (session so far)

| # | File | Change |
| --- | --- | --- |
| 1 | `src/app/domain/frontend/expenses/events/entity_configs.cljs` | Added `:server-filter-keys` to `receipts-config` so the events factory forwards `original-filename`, `supplier-guess`, `created-by-name`, `status`. |
| 2 | `src/app/domain/backend/expenses/routes/receipts.clj` | `list-receipts-handler` now threads text filters, date-range filters, and `show-purged?` into `list-receipts-page` opts. |
| 3 | `src/app/domain/frontend/expenses/events/events_factory.cljs` | Added two new branches in `load-list`'s reduce-kv — single select (`{:value :label}` wrapper) and multi-select (vector of same). Added `clojure.string` require. |
| 4 | `src/app/template/frontend/components/filter/ui.cljs` | Raised dropdown options panel from Tailwind `z-10` to `:style {:z-index z/dropdown-inline}` (= 1000). Added `z-scale` require. |

Status: browser-verified by the user. Dropdown renders above the sticky header.

## Remaining work

### Scope A — Lock the working behavior with regression coverage (implement this first)

- [x] **A1. Backend route regression tests.**
  Extend `test/app/domain/backend/expenses/routes/receipts_test.clj` to pin the
  exact request contract that is working now.
  Coverage:
  - text filters: `original-filename`, `supplier-guess`, `created-by-name`
  - date-range filters: `purchased-at-guess-*`, `created-at-*`, `updated-at-*`
  - status parsing: single status and CSV multi-status
  - `show-purged?`
  - explicit sort params and the current default `:desc` fallback

- [x] **A2. Frontend generated-event regression tests.**
  Add a focused test namespace for
  `src/app/domain/frontend/expenses/events/receipts.cljs` /
  `events_factory.cljs` that captures the outgoing request map.
  Coverage:
  - single select `{:value :label}` serializes to a scalar backend param
  - multi-select serializes to a CSV backend param
  - text filters, date-range filters, and sort params survive the same request
  - pagination still uses the current list state

- [x] **A3. Focused validation.**
  Run only the new/affected backend + frontend test namespaces and save output
  once under `tmp/`.

### Scope B — Safer refactor prerequisites (do not implement before Scope A lands)

- [ ] **B1. Adopt the route factory only after it can preserve the current receipts contract.**
  The first pass review found that a direct swap to `routes-factory/build-list-handler`
  would currently drift behavior.
  Preconditions:
  - receipts date filters must keep working — either by teaching receipt queries
    to consume `:extra-filters` or by using a receipts-specific
    `:custom-query-params` path that preserves `:<field>-from/to` opts
  - status multi-select must still parse CSV into the current vector/string form
  - the response contract must preserve `:purged-total`
  - default sort direction must remain `:desc`
  - sorting allowlisting must stay in the supported layer (currently
    `sortable-receipt-columns` in `receipts/queries.clj`)
  - use `build-list-handler` directly or a list-only helper; do not force
    receipts into the full CRUD route registration shape prematurely
  Deliverable: a separate PR titled
  `refactor(admin/receipts): adopt routes-factory for list handler` once those
  prerequisites are met.

- [x] **B2. Extract shared filter serialization for the receipts/admin path.**
  Implemented in `src/app/template/frontend/events/list/filter_serialization.cljs`.
  Adopted by:
  - `src/app/domain/frontend/expenses/events/events_factory.cljs`
  - `src/app/domain/frontend/expenses/events/user_expenses/receipts/list.cljs`
  - `src/app/domain/frontend/expenses/events/user_expenses/expenses.cljs`
  - `src/app/domain/frontend/expenses/events/user_expenses/stores.cljs`
  - `src/app/domain/frontend/expenses/events/user_expenses/categories.cljs`
  - `src/app/domain/frontend/expenses/events/user_expenses/cities.cljs`
  - `src/app/domain/frontend/expenses/events/user_expenses/lookups.cljs`
  - `src/app/domain/frontend/expenses/events/user_expenses/manufacturers.cljs`
  - `src/app/domain/frontend/expenses/events/user_expenses/subcategories.cljs`
  - `src/app/domain/frontend/expenses/events/user_expenses/unmapped_aliases.cljs`
  - `src/app/domain/frontend/expenses/events/user_expenses/power_tools.cljs`
  - `src/app/domain/frontend/expenses/events/user_expenses/expense_categories.cljs`
  Status:
  - the same-pattern list-request serializer duplication in `user_expenses/*`
    is now removed
  - `src/app/domain/frontend/expenses/events/user_expenses/reports/state.cljs`
    remains separate because it normalizes report state, not list query params

### Scope C — Product gaps surfaced by this work

- [ ] **C1. `show-purged` UI toggle on the receipts list page.**
  The backend now honours `show-purged?`, but there is no UI toggle to flip it
  on the admin receipts page. Decide whether purged receipts should be behind a
  toggle button, a filter chip, or a separate tab. Out of scope for this fix.

- [ ] **C2. Status filter option label i18n.**
  `receipt-status-select-options` in
  `src/app/domain/frontend/expenses/admin/adapters/specs.cljs` hard-codes English
  labels. If expenses admin is within the i18n scope (see `MEMORY.md` — Tongue
  library rollout), these should use the `use-t` hook and translation keys.
  Confirm with product before widening the scope.

## Validation matrix

Gate the current work and any follow-up refactor on:

| Check | How | Pass criteria |
| --- | --- | --- |
| Backend handler forwarding | focused `receipts_test.clj` | opts map contains text/date/show-purged/status/sort keys |
| Frontend request serialization | focused receipts event test | request params contain select/date/text/sort values |
| No new targeted regressions | saved `tee` output under `tmp/` | focused backend/frontend namespaces pass |
| Browser UX (required for future refactor work) | manual | every filter/sort action round-trips without regression |
| Clean compile (only if runtime/frontend code changes) | `shadow.cljs.devtools.api/compile :admin` and `:app` | 0 warnings |

## Out of scope / non-goals

- Directly swapping receipts list routes to the generic route factory before the
  B1 prerequisites are met.
- Broad filter-serialization cleanup beyond the regression-coverage slice.
- Adding UI toggles for `show-purged?` (see C1).
- i18n for status labels (see C2).
- Any changes to the `get-receipt-handler` enrichment pipeline or the
  `effective-status` computation — those are correct today.
