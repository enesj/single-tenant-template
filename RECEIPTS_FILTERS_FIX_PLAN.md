# Admin Receipts — Filters & Sorting Fix Plan

## Context

`/admin/receipts` had broken filtering and (partially) sorting. Root causes spanned
three layers: the frontend events factory was dropping select-shaped values, the
admin receipts list handler was ignoring text/date filter params, and the select
dropdown panel's `z-10` lost the stacking war against the sticky table header at
`z-index: 300`.

## Already applied (session so far)

| # | File | Change |
|---|---|---|
| 1 | `src/app/domain/frontend/expenses/events/entity_configs.cljs` | Added `:server-filter-keys` to `receipts-config` so the events factory forwards `original-filename`, `supplier-guess`, `created-by-name`, `status`. |
| 2 | `src/app/domain/backend/expenses/routes/receipts.clj` | `list-receipts-handler` now threads text filters, date-range filters, and `show-purged?` into `list-receipts-page` opts. |
| 3 | `src/app/domain/frontend/expenses/events/events_factory.cljs` | Added two new branches in `load-list`'s reduce-kv — single select (`{:value :label}` wrapper) and multi-select (vector of same). Added `clojure.string` require. |
| 4 | `src/app/template/frontend/components/filter/ui.cljs` | Raised dropdown options panel from Tailwind `z-10` to `:style {:z-index z/dropdown-inline}` (= 1000). Added `z-scale` require. |

Status: browser-verified by the user. Dropdown renders above the sticky header.

## Remaining work



### Scope B — Refactors to align receipts with the standard patterns used by other admin entities, to reduce the likelihood of similar bugs in the future and make it easier for future devs to understand and modify the code.

- [ ] **B1. Migrate admin receipts list to `routes-factory/register-entity-routes!`.**
  Every other admin entity (`articles`, `stores`, `suppliers`, etc.) is declared
  in `src/app/domain/backend/expenses/routes/route_configs.clj` via
  `:filter-params` + `:date-range-columns` and consumed by
  `routes-factory/build-list-handler`. Receipts is hand-rolled because of the
  sibling routes (`/download`, `/approve`, `/review`) and the enrichment
  pipeline in `enrich-receipt-for-detail`.
  Approach:
  - Define a `receipts-config` entry with `:filter-params`, `:date-range-columns`,
    `:text-filter-keys`, `:order-by-whitelist`.
  - Keep `get-receipt-handler`, `download-receipt-handler`, `approve-and-post-handler`,
    `save-review-handler`, and `delete-receipt-handler` as bespoke handlers
    (factory doesn't support enrichment/sibling routes).
  - Replace only `list-receipts-handler` with the factory-generated one; compose
    the bespoke handlers alongside in `routes`.
  - Risk: the enrichment (`enrich-receipt-for-detail`) runs in `get-receipt-handler`,
    not `list-receipts-handler`, so factory adoption on LIST should be safe.
  - Deliverable: a separate PR titled
    `refactor(admin/receipts): adopt routes-factory for list handler`.

- [ ] **B2. Extract a shared helper for select-shaped filter values.**
  The two new branches in `events_factory.cljs` (single/multi select) overlap
  conceptually with logic in `template.frontend.events.list.filters/::apply-filter`
  which wraps select values as `{:value :label}`. Extract
  `select-filter-value->backend-param` into
  `src/app/template/frontend/events/list/filter_serialization.cljs` (new file)
  and call it from both the factory and any ad-hoc consumers.
  Defer unless a third consumer appears — YAGNI.

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

After each scope-A item is done, gate the PR on:

| Check | How | Pass criteria |
|---|---|---|
| Backend handler forwards filters | A1 test | opts map contains expected keys |
| Frontend factory serialises selects | A2 test | query-string contains `status=...` |
| Browser UX | A3 manual | every filter/sort action round-trips without regression |
| No new test regressions | A4 `tee` output | new failure count ≤ baseline in MEMORY.md |
| Clean compile | `shadow.cljs.devtools.api/compile :admin` and `:app` | 0 warnings |

## Out of scope / non-goals

- Rewriting the receipts list handler to factory form (see B1).
- Consolidating select-filter serialization helpers (see B2).
- Adding UI toggles for `show-purged?` (see C1).
- i18n for status labels (see C2).
- Any changes to the `get-receipt-handler` enrichment pipeline or the
  `effective-status` computation — those are correct today.
