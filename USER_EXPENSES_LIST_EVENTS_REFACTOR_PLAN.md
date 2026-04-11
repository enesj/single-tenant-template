# User Expenses List Events Refactor Plan

## Summary

Refactor duplicated user-expenses list-event plumbing in two passes instead of jumping straight to a broad factory. First, use the existing pagination list tests as the contract, fill only the real coverage gaps, and extract a local `user_expenses/list_support.cljs` helper that standardizes pagination/filter/sort request params through `paths/*`. Only after that checkpoint should we decide whether the remaining fetch/success/failure duplication is uniform enough for a second helper. This first pass explicitly keeps receipts, reports, recent, admin factory reuse, and modal CRUD flows out of scope.

## Status update (12 Apr 2026)

Completed so far:

- Step 1: expanded `pagination_lists_test.cljs` to lock the remaining first-pass contracts (`payers`, `payer-types`, `articles`, `article-aliases`, `supplier-aliases`, `store-aliases`, and the extra local writes for `payers` / `expense-categories`).
- Step 2: added `src/app/domain/frontend/expenses/events/user_expenses/list_support.cljs` and migrated the planned request-param builders to shared `paths/*`-based list-state reads.
- Step 3: extracted the small shared loading/error helpers and rolled them through the first-pass cohort.
- Step 4 checkpoint result: **yes, a narrow second helper was justified for the clean cohort**.

Implemented second-helper clean cohort:

- `expenses.cljs`
- `categories.cljs`
- `cities.cljs`
- `manufacturers.cljs`
- `subcategories.cljs`
- `stores.cljs` (`stores` + `store-aliases` list fetch/success flows)
- `power_tools.cljs` (`articles`, `expense-items`, `article-aliases`, `supplier-aliases` list fetch/success flows)
- `unmapped_aliases.cljs` (added after a follow-up audit confirmed it also matches the narrow helper contract)

Still intentionally custom for now:

- `lookups.cljs` (extra local state like `:user-payer-id` and the fixed-params/server-mode branch)
- `expense_categories.cljs` (extra local `:items` cache and custom local loading state)
- `recent.cljs`
- `receipts/list.cljs`

Validation so far:

- Focused frontend validation passed after the second-helper rollout: `47` tests, `127` assertions, `0` failures, `0` errors.
- Follow-up compile + focused validation passed after migrating `unmapped_aliases.cljs`: shadow `:app` compiled with `0` warnings, and the focused frontend suite passed at `48` tests, `129` assertions, `0` failures, `0` errors.

## Implementation steps (ordered)

### 1. Audit and extend the existing regression coverage for the actual first-pass scope

- **Goal:** Start from the test surface that already exists and add only the missing assertions needed to protect the first-pass refactor.
- **File list:**
  - `/Users/enes/Projects/single-tenant-template/test/app/domain/frontend/expenses/events/user_expenses/pagination_lists_test.cljs`
  - `/Users/enes/Projects/single-tenant-template/test/app/domain/frontend/expenses/events/user_expenses/test_support.cljs` (only if helpers need to expand)
  - `/Users/enes/Projects/single-tenant-template/test/app/domain/frontend/expenses/events/user_expenses_test.cljs` (only if the aggregator needs to pull in new focused test namespaces)
- **Dependencies:** none
- **Owner:** `Coder`
- **Notes:**
  - Do not create a new parallel regression namespace unless the current one becomes unmanageably large; `pagination_lists_test.cljs` already covers much of the contract we need.
  - Current coverage already pins behavior for `suppliers`, `receipts`, `unmapped-aliases`, `categories`, `subcategories`, `cities`, `manufacturers`, `expense-categories`, `expenses`, `stores`, and `recent`.
  - Fill the remaining high-value gaps before refactoring:
    - `payers` / `payer-types` server-mode vs fixed-params behavior in `lookups.cljs`
    - `articles`, `article-aliases`, and `supplier-aliases` list flows in `power_tools.cljs`
    - `store-aliases` list flow inside `stores.cljs`
    - any extra local writes that must survive, such as `[:user-expenses :payers :user-payer-id]` and `[:user-expenses :expense-categories :items]`
  - Keep the assertions centered on request params, loading/error transitions, `:total` persistence, optional `:date-highlights`, and any bespoke local cache writes.

### 2. Extract a local request-param helper and standardize list-state reads

- **Goal:** Remove the repeated `current-*page-params` functions and stop mixing raw `[:ui :lists ...]` lookups with `paths/list-*` helpers.
- **File list:**
  - `/Users/enes/Projects/single-tenant-template/src/app/domain/frontend/expenses/events/user_expenses/list_support.cljs` (new)
  - `/Users/enes/Projects/single-tenant-template/src/app/domain/frontend/expenses/events/user_expenses/lookups.cljs`
  - `/Users/enes/Projects/single-tenant-template/src/app/domain/frontend/expenses/events/user_expenses/power_tools.cljs`
  - `/Users/enes/Projects/single-tenant-template/src/app/domain/frontend/expenses/events/user_expenses/unmapped_aliases.cljs`
  - `/Users/enes/Projects/single-tenant-template/src/app/domain/frontend/expenses/events/user_expenses/subcategories.cljs`
  - `/Users/enes/Projects/single-tenant-template/src/app/domain/frontend/expenses/events/user_expenses/manufacturers.cljs`
  - `/Users/enes/Projects/single-tenant-template/src/app/domain/frontend/expenses/events/user_expenses/cities.cljs`
  - `/Users/enes/Projects/single-tenant-template/src/app/domain/frontend/expenses/events/user_expenses/categories.cljs`
  - `/Users/enes/Projects/single-tenant-template/src/app/domain/frontend/expenses/events/user_expenses/stores.cljs`
  - `/Users/enes/Projects/single-tenant-template/src/app/domain/frontend/expenses/events/user_expenses/expenses.cljs`
  - `/Users/enes/Projects/single-tenant-template/src/app/domain/frontend/expenses/events/user_expenses/expense_categories.cljs`
- **Dependencies:** Step 1
- **Owner:** `Coder`
- **Notes:**
  - The helper should be local to `user_expenses`, not shared with admin yet.
  - Standardize on `paths/resolved-list-per-page`, `paths/resolved-list-current-page`, `paths/list-sort-config`, and `paths/list-filters` instead of raw `[:ui :lists entity-key ...]` reads.
  - Reuse `/Users/enes/Projects/single-tenant-template/src/app/template/frontend/events/list/filter_serialization.cljs` for flattening/serialization work.
  - Keep the helper small and explicit. A likely shape is: `build-list-request-params` with inputs like `entity-key`, `default-limit`, and optional `base-params` or filter post-processing.
  - Preserve current event signatures and call sites. Do not normalize away existing event-vector differences such as `[:user-expenses/refresh-unmapped-aliases-list _ opts]`.
  - Receipts stay out of this helper because `status`, `show-purged?`, and processing-check fetches are a different request contract.

### 3. Extract the smallest shared loading/error helper

- **Goal:** Remove repeated `begin-entity-load` / `finish-entity-load` logic without forcing heterogeneous success handlers into a factory too early.
- **File list:**
  - `/Users/enes/Projects/single-tenant-template/src/app/domain/frontend/expenses/events/user_expenses/list_support.cljs`
  - same caller files from Step 2, but only the loading/error transitions
- **Dependencies:** Step 2
- **Owner:** `Coder`
- **Notes:**
  - Keep this helper DB-only at first: “start load”, “finish load”, and error extraction.
  - This step is low-risk because it does not need to standardize response shapes or sync dispatches.
  - `lookups.cljs`, `expense_categories.cljs`, and `stores.cljs` can still use this helper even if their success handlers remain custom.
  - Modal CRUD paths in `power_tools.cljs`, `stores.cljs`, and `reference_crud.cljs` are not part of this extraction.

### 4. Add a checkpoint before introducing any fetch/success/failure factory

- **Goal:** Decide with evidence whether a second helper is still worth it after Steps 2 and 3, instead of assuming a factory is automatically the best end state.
- **File list:**
  - `/Users/enes/Projects/single-tenant-template/src/app/domain/frontend/expenses/events/user_expenses/list_support.cljs`
  - strict-template-store list callers from Step 2, but only if they still match cleanly after the checkpoint
- **Dependencies:** Step 3
- **Owner:** `Coder`
- **Notes:**
  - Only proceed if the remaining cohort truly shares all of the following:
    - rows come from `:data`
    - `:total` falls back to `(count rows)`
    - `:date-highlights` is optional and stored the same way
    - the success path only needs one sync dispatch plus standard list metadata writes
  - The likely “clean cohort” is `expenses`, `categories`, `subcategories`, `cities`, `manufacturers`, `stores`, `articles`, `expense-items`, `article-aliases`, `supplier-aliases`, and possibly `store-aliases`.
  - Keep `lookups.cljs`, `expense_categories.cljs`, `unmapped_aliases.cljs`, `recent.cljs`, and `receipts/list.cljs` out unless the helper stays narrow enough to avoid callback sprawl.
  - If this checkpoint says “no”, stop after the smaller helpers; that is still a successful refactor.

### 5. Keep admin factory reuse and non-list flows explicitly out of the first PR

- **Goal:** Preserve scope and avoid creating a half-shared abstraction that couples user and admin event stacks prematurely.
- **File list:**
  - `/Users/enes/Projects/single-tenant-template/src/app/domain/frontend/expenses/events/events_factory.cljs` (reference only; no edits expected)
  - `/Users/enes/Projects/single-tenant-template/src/app/domain/frontend/expenses/events/user_expenses/list_support.cljs`
  - `/Users/enes/Projects/single-tenant-template/src/app/domain/frontend/expenses/events/user_expenses/receipts/list.cljs`
  - `/Users/enes/Projects/single-tenant-template/src/app/domain/frontend/expenses/events/user_expenses/reports/state.cljs`
  - `/Users/enes/Projects/single-tenant-template/src/app/domain/frontend/expenses/events/user_expenses/recent.cljs`
  - `/Users/enes/Projects/single-tenant-template/src/app/domain/frontend/expenses/events/user_expenses/reference_crud.cljs`
- **Dependencies:** none
- **Owner:** `Coder`
- **Notes:**
  - Do not reuse `events_factory.cljs` in this refactor. It currently assumes admin HTTP helpers, admin registration/dispatch conventions, and a different load-state footprint (`base-path` plus entity-path writes).
  - Keep `receipts/list.cljs` bespoke for `status` normalization, `show-purged?`, processing-check polling, and receipt-specific payload handling.
  - Keep `reports/state.cljs` separate because it manages report filter state, not list request construction.
  - Keep `recent.cljs` separate because it has its own pagination math and rows override behavior.
  - In mixed files like `power_tools.cljs` and `stores.cljs`, touch list fetch code only; do not roll modal CRUD handlers into this pass.

## Edge cases

- **Happy path:** list modules still derive `limit`/`offset` from template list state, forward flattened filters and sort params, persist totals, and store `:date-highlights` where the current code already does so.
- **`nil` inputs:**
  - missing list UI state should still fall back through `paths/resolved-list-*`
  - missing filter maps should serialize to no extra query params
  - missing sort config should not emit invalid `order-by` / `order-dir`
  - missing `:total` should fall back to row count where current code already does so
  - missing `:date-highlights` should not break success handlers
- **Empty collections:** empty responses should clear loading state, preserve zero totals, and avoid leaving stale rows or pagination metadata behind.
- **Invalid / boundary input:**
  - zero/negative pages must not produce negative offsets
  - unknown sort directions must not emit invalid direction strings
  - whitespace-heavy text filters should still normalize deterministically
  - mixed scalar/select/range filters should still flatten the same way they do today
  - entity-specific default limits (`10`, `25`, `50`, `100`, `200`) must remain intact
  - existing event vector shapes (`[params]`, `[_ params]`, refresh events with optional opts) must stay compatible
- **Special-case boundary:**
  - `lookups.cljs` has non-uniform behavior for `payers` / `payer-types` and extra state like `[:user-expenses :payers :user-payer-id]`
  - `expense_categories.cljs` stores an extra local `:items` slice in addition to the shared entity sync
  - `stores.cljs` contains both `:stores` and `:store-aliases` list flows in the same file
  - `receipts/list.cljs` has bespoke status/show-purged/polling semantics and should remain custom

## Validation plan

- Use focused frontend tests and/or CLJS REPL evaluation; at least one is required, and both are preferred for this refactor.
- REPL-first loop:
  - select the app build: `(shadow.cljs.devtools.api/nrepl-select :app)`
  - reload targeted tests: `(require 'app.domain.frontend.expenses.events.user-expenses.pagination-lists-test :reload)` and `(require 'app.template.frontend.events.list.filter-serialization-test :reload)`
  - run focused tests: `(cljs.test/run-tests 'app.domain.frontend.expenses.events.user-expenses.pagination-lists-test 'app.template.frontend.events.list.filter-serialization-test)`
- Minimum saved test output under `/Users/enes/Projects/single-tenant-template/tmp/`:
  - `bb fe-test-parallel --grep "app.domain.frontend.expenses.events.user-expenses.pagination-lists-test|app.template.frontend.events.list.filter-serialization-test" 2>&1 | tee tmp/fe-user-expenses-list-refactor-$(date +%H%M%S).log`
- If Step 4 proceeds into `power_tools.cljs`, `stores.cljs`, or other mixed namespaces, also run the user-expenses aggregator namespace:
  - `bb fe-test-parallel --grep "app.domain.frontend.expenses.events.user-expenses-test" 2>&1 | tee tmp/fe-user-expenses-aggregate-$(date +%H%M%S).log`
- If the refactor touches many runtime event namespaces, add one compile check from the REPL:
  - `(shadow.cljs.devtools.api/compile :app)`
- During implementation, all `.cljs` edits should use structural editing tools.

## Open questions / assumptions

- **Assumption:** `pagination_lists_test.cljs` is the main contract for this refactor, so the first move is to extend it rather than replace it.
- **Assumption:** the first helper should normalize on `paths/*` list-state accessors rather than preserving mixed direct `[:ui :lists ...]` lookups.
- **Open question:** Should `store-aliases` join the first-pass request helper immediately, or should `stores.cljs` be split conceptually into separate list families first?
- **Open question:** After Steps 2 and 3, is there enough truly uniform duplication left to justify a fetch/success/failure helper at all?
- **Open question:** Should `expense_categories.cljs` ever join a later success helper given its extra local `:items` write?
- **Open question:** Should `lookups.cljs` ever share more than request-building and loading/error helpers, given `:user-payer-id` and the server-mode/fixed-params branches?
- **Out-of-scope assumption:** admin `events_factory.cljs`, `receipts/list.cljs`, `reports/state.cljs`, `recent.cljs`, and modal CRUD handlers stay outside this refactor.
