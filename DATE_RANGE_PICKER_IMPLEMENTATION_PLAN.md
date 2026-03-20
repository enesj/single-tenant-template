# Date Range Picker Implementation Plan

## Goal

Replace the current two-input date-range filter UI with a single custom date picker that:

- selects both `from` and `to` in one picker
- applies immediately after each click
- highlights dates that have records after all other active non-date filters are applied
- keeps dates without records selectable
- blocks future dates using the viewer's local timezone

The behavior contract is captured in [specs/allium/drafts/list-view-date-range-picker.candidate.allium](/Users/enes/Projects/single-tenant-template/specs/allium/drafts/list-view-date-range-picker.candidate.allium).

## Current Touchpoints

- Filter form state is initialized in [src/app/template/frontend/components/filter.cljs](/Users/enes/Projects/single-tenant-template/src/app/template/frontend/components/filter.cljs).
- The current date-range filter UI is two separate `<input type="date">` fields in [src/app/template/frontend/components/filter/ui.cljs](/Users/enes/Projects/single-tenant-template/src/app/template/frontend/components/filter/ui.cljs).
- Date-range local state and dispatch are handled by [src/app/template/frontend/components/filter/hooks.cljs](/Users/enes/Projects/single-tenant-template/src/app/template/frontend/components/filter/hooks.cljs).
- Date-range auto-apply, matching-count logic, and initial-value sync live in [src/app/template/frontend/components/filter/logic.cljs](/Users/enes/Projects/single-tenant-template/src/app/template/frontend/components/filter/logic.cljs).
- Client-side filter matching is implemented in [src/app/template/frontend/components/filter/helpers.cljs](/Users/enes/Projects/single-tenant-template/src/app/template/frontend/components/filter/helpers.cljs).
- Filter state writes and server refresh dispatch happen in [src/app/template/frontend/events/list/filters.cljs](/Users/enes/Projects/single-tenant-template/src/app/template/frontend/events/list/filters.cljs).
- Generic server-mode list requests already expand date ranges into `<field>-from` / `<field>-to` params in [src/app/domain/frontend/expenses/events/events_factory.cljs](/Users/enes/Projects/single-tenant-template/src/app/domain/frontend/expenses/events/events_factory.cljs).
- Generic backend route factories already allowlist and apply date-range query params in [src/app/domain/backend/expenses/routes/routes_factory.clj](/Users/enes/Projects/single-tenant-template/src/app/domain/backend/expenses/routes/routes_factory.clj).
- The shared form date picker wraps `react-day-picker` in [src/app/template/frontend/components/form/fields/date_picker.cljs](/Users/enes/Projects/single-tenant-template/src/app/template/frontend/components/form/fields/date_picker.cljs), but it currently has generic range semantics, `showOutsideDays false`, and a hardcoded popover id (`rdp-popover`).

## Recommended Approach

Build a filter-specific picker component instead of trying to stretch the generic form field picker into this behavior.

Why:

- The filter picker needs partial-range semantics that are not standard DayPicker range mode:
  - first click applies `from = selected day`, `to = today`
  - only the anchor day is visually selected until the second click
  - clicking inside a completed range clears it
  - clicking outside a completed range starts a new partial range
- The shared form picker currently assumes generic form-field usage and hardcodes popover ids, which is risky for multiple filter instances.
- Filter-specific highlighted-day logic depends on current list filters and list-mode data sources, not simple field input props.

## Phase 1: Introduce a Filter-Specific Date Picker

Create a new component, likely at:

- [src/app/template/frontend/components/filter/date_range_picker.cljs](/Users/enes/Projects/single-tenant-template/src/app/template/frontend/components/filter/date_range_picker.cljs)

Responsibilities:

- render one `react-day-picker` calendar
- show outside days
- disable dates after local today
- accept highlighted days as modifiers
- expose click behavior for:
  - empty -> partial
  - partial same day -> clear
  - partial different day -> complete with auto-reorder
  - complete click inside range -> clear
  - complete click outside range -> new partial
- keep stable unique ids for trigger, popover, and clear affordances so chrome-devtools can target them

Prefer a dedicated popover/trigger id per field, for example:

- `filter-date-picker-trigger-<field>`
- `filter-date-picker-popover-<field>`

## Phase 2: Replace the Two-Input Filter UI

Update [src/app/template/frontend/components/filter/ui.cljs](/Users/enes/Projects/single-tenant-template/src/app/template/frontend/components/filter/ui.cljs):

- remove the two separate `date-input` controls from `date-range-filter`
- mount the new filter-specific picker instead
- keep the existing status area only if it still adds value once immediate click behavior is in place

Update [src/app/template/frontend/components/filter/rendering.cljs](/Users/enes/Projects/single-tenant-template/src/app/template/frontend/components/filter/rendering.cljs) only as needed to thread the new props through cleanly.

## Phase 3: Rework Date-Range Filter State

Refactor [src/app/template/frontend/components/filter/hooks.cljs](/Users/enes/Projects/single-tenant-template/src/app/template/frontend/components/filter/hooks.cljs):

- replace the current `local-from` / `local-to` only model with explicit picker state:
  - `anchor-day`
  - `selected-from`
  - `selected-to`
  - `selection-state` (`empty`, `partial`, `complete`)
- compute local-day boundaries on click:
  - `from` = local `00:00:00.000`
  - `to` = local `23:59:59.999`
- apply the filter immediately on each click through `::filter-events/apply-filter`
- clear via `::filter-events/clear-filter` when the active selection is clicked again
- preserve partial selection while navigating months

Important:

- the existing debounced `use-date-range-auto-apply` path in [src/app/template/frontend/components/filter/logic.cljs](/Users/enes/Projects/single-tenant-template/src/app/template/frontend/components/filter/logic.cljs) should be bypassed or removed for this picker so we do not double-dispatch or overwrite partial-selection state
- `sync-state-with-initial-value` should reconstruct `partial` vs `complete` state from stored filter values where possible, or at minimum hydrate a completed range reliably

## Phase 4: Add Local-Day Utilities

Introduce small shared helpers in one of these existing date utility areas:

- [src/app/shared/date.cljc](/Users/enes/Projects/single-tenant-template/src/app/shared/date.cljc)
- or a filter-local helper file beside the new picker

Needed helpers:

- local today calculation
- start-of-local-day
- end-of-local-day
- same-local-day?
- local-day key formatting for highlight maps
- reorder two clicked days into ascending range

Keep these utilities ASCII-only and focused on day-boundary semantics rather than UI code.

## Phase 5: Highlighted-Day Data Source

This is the main architectural choice.

### Client-mode lists

For client-paginated lists, compute highlighted days locally from the currently loaded rows by:

- reusing [src/app/template/frontend/components/filter/helpers.cljs](/Users/enes/Projects/single-tenant-template/src/app/template/frontend/components/filter/helpers.cljs)
- applying all active filters except:
  - the current date field
  - any existing date-range selection for that same field
- extracting matching row timestamps into unique local-day keys

This can likely live in a new helper near the filter stack or in [src/app/template/frontend/components/filter/logic.cljs](/Users/enes/Projects/single-tenant-template/src/app/template/frontend/components/filter/logic.cljs).

### Server-mode lists

Client data is not sufficient for paginated server-mode lists because the current page does not represent the full filtered dataset.

Recommended server-mode plan:

1. Extend list responses to optionally include highlighted-day metadata for allowlisted date fields.
2. Store that metadata in list UI state when server responses arrive.
3. Feed that stored highlight map into the new picker.

Likely touchpoints:

- request/build path: [src/app/domain/frontend/expenses/events/events_factory.cljs](/Users/enes/Projects/single-tenant-template/src/app/domain/frontend/expenses/events/events_factory.cljs)
- backend list handler shaping: [src/app/domain/backend/expenses/routes/routes_factory.clj](/Users/enes/Projects/single-tenant-template/src/app/domain/backend/expenses/routes/routes_factory.clj)
- entity-specific list services where the highlighted-day aggregation query belongs

Recommendation:

- ship the UI/state refactor first with client-mode support and a clean metadata hook
- then add server-provided highlight maps for the server-paginated entities that use date filters most heavily

## Phase 6: Keep Display and Matching Consistent

Update formatting and matching helpers so the rest of the filter UI stays truthful:

- [src/app/template/frontend/components/filter/utils.cljs](/Users/enes/Projects/single-tenant-template/src/app/template/frontend/components/filter/utils.cljs)
- [src/app/template/frontend/components/filter/helpers.cljs](/Users/enes/Projects/single-tenant-template/src/app/template/frontend/components/filter/helpers.cljs)

Specific checks:

- active filter chips should display the actual applied range, including the first-click temporary `to = today`
- matching-count logic should not confuse partial visual selection with the applied filter range
- inclusive comparisons must continue to treat boundary days as included

## Phase 7: Verification

Minimum verification should cover both behavior and wiring.

Frontend-focused checks:

- first click applies `from = clicked day`, `to = today`
- first click only visually marks the anchor day
- second click completes the range
- second click before the anchor auto-reorders the range
- clicking the anchor again during partial selection clears the filter
- clicking inside a completed range clears the filter
- clicking outside a completed range starts a new partial range
- future days are disabled
- overflow days are visible and obey the same highlight rules
- non-highlighted days remain selectable
- changing non-date filters updates highlighted days without clearing the selected range

Good candidate test files to extend or add:

- [test/app/admin/frontend/components/admin_page_wrapper_test.cljs](/Users/enes/Projects/single-tenant-template/test/app/admin/frontend/components/admin_page_wrapper_test.cljs)
- filter-component tests near the shared list/filter stack if they already exist

Manual verification:

- exercise one client-mode list and one server-mode list in the browser
- confirm network requests still send `<field>-from` / `<field>-to`
- confirm repeated interactions do not reopen the old debounce/reset bug

## Delivery Order

1. Add the Allium spec artifact.
2. Build the dedicated filter date picker component.
3. Refactor date-range filter state and remove legacy date auto-apply coupling.
4. Add local client-side highlighted-day computation.
5. Add server-mode highlighted-day metadata for targeted entities.
6. Verify with focused frontend tests and browser checks.
