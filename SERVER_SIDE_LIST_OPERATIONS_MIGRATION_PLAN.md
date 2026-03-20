# Server-Side List Operations Migration Plan

## Goal

Reduce code complexity by consolidating list pagination, filtering, and sorting onto a single server-side path instead of maintaining mixed client/server list behavior.

Primary outcome:

- one mental model for list loading
- one filter/sort/pagination pipeline
- fewer frontend branches, fewer sync bugs, fewer test permutations

This is a follow-up initiative. After this migration is implemented, we return to the primary date-picker task and finish any remaining server-truth pieces there.

## Recommendation

Move to server-side pagination, filtering, and sorting as the default for list views across the app.

Keep client-side-only behavior only if a list is:

- tiny
- effectively static
- not worth the backend/API surface area

Even then, prefer making that an explicit exception rather than the shared default architecture.

## Why This Helps

Current complexity comes from supporting both modes in:

- frontend filter state and refresh logic
- list event generation
- matching-count calculations
- filter UI behavior
- tests
- backend contracts that are only partially used in some screens

A single server-side model would simplify:

- [src/app/template/frontend/components/filter.cljs](/Users/enes/Projects/single-tenant-template/src/app/template/frontend/components/filter.cljs)
- [src/app/template/frontend/components/filter/helpers.cljs](/Users/enes/Projects/single-tenant-template/src/app/template/frontend/components/filter/helpers.cljs)
- [src/app/template/frontend/components/list/overrides.cljs](/Users/enes/Projects/single-tenant-template/src/app/template/frontend/components/list/overrides.cljs)
- [src/app/template/frontend/events/list/filters.cljs](/Users/enes/Projects/single-tenant-template/src/app/template/frontend/events/list/filters.cljs)
- [src/app/template/frontend/events/list/ui_state.cljs](/Users/enes/Projects/single-tenant-template/src/app/template/frontend/events/list/ui_state.cljs)
- [src/app/domain/frontend/expenses/events/events_factory.cljs](/Users/enes/Projects/single-tenant-template/src/app/domain/frontend/expenses/events/events_factory.cljs)

## Scope

Includes:

- pagination mode consolidation
- server-side filter forwarding
- server-side sorting
- server-provided totals and list metadata
- frontend cleanup of client-mode branches

Excludes for the first pass:

- changing unrelated CRUD behavior
- redesigning list UI
- changing domain semantics of existing filters

## Migration Strategy

## Phase 1: Inventory and Categorize Lists

Create a list of all current list screens and classify them:

- transactional and already server-ready
- reference-data with server support already possible
- true tiny/static datasets that may remain exceptional

Likely places to inspect:

- [src/app/domain/frontend/expenses/events/entity_configs.cljs](/Users/enes/Projects/single-tenant-template/src/app/domain/frontend/expenses/events/entity_configs.cljs)
- [src/app/domain/frontend/expenses/events/user_expenses](/Users/enes/Projects/single-tenant-template/src/app/domain/frontend/expenses/events/user_expenses)
- [src/app/admin/frontend/events](/Users/enes/Projects/single-tenant-template/src/app/admin/frontend/events)

Deliverable:

- a simple table of entities and their target migration status

## Phase 2: Make Backend Contracts Complete

Before removing client-mode logic, every migrated list should support:

- `limit`
- `offset`
- `order-by`
- `order-dir`
- forwarded text filters
- forwarded number-range filters where relevant
- forwarded date-range filters where relevant
- `total` count in the response

Existing foundations are already present in:

- [src/app/domain/backend/expenses/routes/routes_factory.clj](/Users/enes/Projects/single-tenant-template/src/app/domain/backend/expenses/routes/routes_factory.clj)
- [src/app/domain/backend/expenses/routes/route_configs.clj](/Users/enes/Projects/single-tenant-template/src/app/domain/backend/expenses/routes/route_configs.clj)
- [src/app/domain/backend/expenses/services/services_factory.clj](/Users/enes/Projects/single-tenant-template/src/app/domain/backend/expenses/services/services_factory.clj)
- [src/app/domain/backend/expenses/services/articles/crud.clj](/Users/enes/Projects/single-tenant-template/src/app/domain/backend/expenses/services/articles/crud.clj)

Work here is mostly about filling gaps consistently, not inventing a new backend pattern.

## Phase 3: Standardize Frontend Request Building

Unify all list request generation around the server path in:

- [src/app/domain/frontend/expenses/events/events_factory.cljs](/Users/enes/Projects/single-tenant-template/src/app/domain/frontend/expenses/events/events_factory.cljs)
- [src/app/template/frontend/events/list/filters.cljs](/Users/enes/Projects/single-tenant-template/src/app/template/frontend/events/list/filters.cljs)
- [src/app/template/frontend/events/list/ui_state.cljs](/Users/enes/Projects/single-tenant-template/src/app/template/frontend/events/list/ui_state.cljs)

Targets:

- always forward active filters
- always forward sort state
- always treat page changes as server refresh triggers
- remove conditional behavior based on `:pagination-mode`

## Phase 4: Remove Client-Side Filtering and Sorting Paths

Once the backend contract is complete, remove or sharply reduce frontend data transforms that are only there for client mode.

Key candidates:

- [src/app/template/frontend/components/list/overrides.cljs](/Users/enes/Projects/single-tenant-template/src/app/template/frontend/components/list/overrides.cljs)
- matching-count logic in [src/app/template/frontend/components/filter/logic.cljs](/Users/enes/Projects/single-tenant-template/src/app/template/frontend/components/filter/logic.cljs)
- local filter result assumptions in shared list subscriptions

Important note:

- some local-only UI affordances may remain, but they should not be the source of truth for result sets

## Phase 5: Collapse Pagination Mode State

After migrated lists no longer rely on client mode, simplify the shared state model:

- remove `client | server` branching where possible
- stop persisting pagination mode for normal lists
- keep only one refresh strategy

Primary files:

- [src/app/template/frontend/db/paths.cljs](/Users/enes/Projects/single-tenant-template/src/app/template/frontend/db/paths.cljs)
- [src/app/template/frontend/events/list/ui_state.cljs](/Users/enes/Projects/single-tenant-template/src/app/template/frontend/events/list/ui_state.cljs)
- [src/app/template/frontend/subs/list.cljs](/Users/enes/Projects/single-tenant-template/src/app/template/frontend/subs/list.cljs)

## Phase 6: Update Shared Filter UX Assumptions

This is where we reconnect to the primary task.

Once lists are server-driven, update the date picker and related filter UI to rely on server-truth metadata when needed:

- highlighted days for paginated datasets
- truthful match counts
- filter summaries based on actual server-applied state

That work should build directly on:

- [src/app/template/frontend/components/filter/date_range_picker.cljs](/Users/enes/Projects/single-tenant-template/src/app/template/frontend/components/filter/date_range_picker.cljs)
- [DATE_RANGE_PICKER_IMPLEMENTATION_PLAN.md](/Users/enes/Projects/single-tenant-template/DATE_RANGE_PICKER_IMPLEMENTATION_PLAN.md)

## Phase 7: Testing and Verification

Minimum verification:

- focused frontend tests for list refresh behavior
- focused backend tests for query param forwarding and count correctness
- browser verification on at least:
  - one transactional server-paginated list
  - one former reference-data client list after migration

Good areas to extend:

- [test/app/template/frontend/filter_integration_test.cljs](/Users/enes/Projects/single-tenant-template/test/app/template/frontend/filter_integration_test.cljs)
- existing domain/backend list handler tests

## Proposed Rollout Order

1. Inventory all lists and mark migration targets.
2. Standardize backend support for missing filters/sort/counts.
3. Make frontend request generation always server-oriented.
4. Migrate one or two reference-data lists as proof of simplification.
5. Remove shared client-side filtering/sorting branches.
6. Revisit and finish the date-picker server-truth enhancements.

## Risks

- Some tiny lists may feel slower if every interaction becomes remote.
- Incomplete backend allowlists could temporarily regress filter behavior.
- UI elements currently relying on locally available full datasets may need replacement metadata from the server.

## Expected Payoff

If done cleanly, this should reduce:

- duplicate logic
- mixed-mode bugs
- frontend-only special cases
- test matrix size

And it should make future work, including the date picker, more straightforward because the app will have one canonical list behavior instead of two.
