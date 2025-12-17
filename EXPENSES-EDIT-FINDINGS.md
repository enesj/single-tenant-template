# Expenses edit/list debugging findings

This document summarizes the issues investigated around `http://localhost:8085/expenses/list` (user expenses), what was found, what was changed, and what still needs follow-up.

## Symptoms observed

1. **Edit modal didn’t populate line items**
   - Opening “Edit Expense” showed empty/placeholder line-items instead of the saved items.

2. **After canceling edit, the list collapsed to a single record**
   - The table briefly showed only the edited/loaded expense until a refresh or re-fetch.

3. **“Update Expense” button flickered**
   - After making a change, the button enabled briefly and then disabled again.

4. **Textarea edits didn’t “stick”**
   - Programmatic or manual changes to `notes` appeared to revert immediately, consistent with a controlled field whose `on-change` wasn’t actually wired.

## Root causes

### A) Single-entity “sync” overwrote the entire `:expenses` entity store

- The expenses adapter sync event is registered via:
  - `src/app/admin/frontend/adapters/expenses.cljs` (`::sync-expenses`)
  - `src/app/template/frontend/shared/utils/entity.cljs` (`register-sync-event!`)

- `register-sync-event!` **replaces** the entity store for the given entity:
  - overwrites `(paths/entity-data :expenses)`
  - overwrites `(paths/entity-ids  :expenses)`

So when code dispatches `::sync-expenses` with **one** expense (e.g. from a detail fetch), the shared entity store becomes “just that one expense”, which explains the list collapsing.

Where this happens:
- `src/app/template/frontend/events/user_expenses.cljs` in `:user-expenses/fetch-expense-success`:
  - dispatches `::admin-expenses-adapter/sync-expenses` with `[expense]`

### B) Fork form dirty-state reset from unstable `:initial-values`

- The template `form` uses fork/fork.re-frame; fork treats `:initial-values` changes as reinitialization.
- In the expenses edit modal, `initial-data` was being normalized/merged into a **fresh map** on each render, changing identity and causing fork to reset `dirty`.

### C) Shared textarea component wasn’t applying `:on-change`

- `src/app/template/frontend/components/form/fields/textarea.cljs` previously merged props incorrectly (nested map), so the actual `:on-change` handler was not applied.
- Result: a controlled textarea would continually render the old `:value`, making edits appear to “revert”, and keeping the form from becoming dirty.

### D) Line-items data wasn’t present in the edit modal’s `initial-data`

- The list view’s row data often does not include full `:items`.
- The current edit modal (`user-expense-edit-form-modal`) uses the passed `initial-data` from the list row.
- Unless the item is enriched with detail data (or the list endpoint includes items), line items will remain empty/placeholder.

## Changes made (code pointers)

### 1) Stabilize form initial values in the user expense form

File: `src/app/domain/frontend/expenses/components/user_expense_form.cljs`
- Memoized `entity-spec` and `:initial-values` via `use-memo`.
- Memoized the normalized edit initial-data via `use-memo` in `user-expense-edit-form-modal`.

Goal: prevent fork from reinitializing the form (and clearing `dirty`) on re-renders.

### 2) Fix textarea `on-change` wiring

File: `src/app/template/frontend/components/form/fields/textarea.cljs`
- Fixed props merging so `:on-change` is actually set.

Goal: ensure text edits update fork state and remain visible.

### 3) Routing/UI polish around manual expense entry

Files touched during the session:
- `src/app/template/frontend/routes.cljs` (legacy `/expenses/new` now routes to the list page)
- `src/app/domain/frontend/expenses/pages/user/expenses_dashboard.cljs`
- `src/app/domain/frontend/expenses/pages/user/expense_upload.cljs`
- `src/app/domain/frontend/expenses/pages/user/expenses_list.cljs`

Goal: converge UX around “list page + modal add/edit”.

## What remains to fix (recommended next steps)

### 1) Replace “sync single expense” with an upsert/merge into the entity store

Current behavior (problematic):
- `::sync-expenses` replaces the entire store (good for “sync a full list”, bad for “enrich one entity”).

Recommended approach:
- Add a new event (or extend `register-sync-event!`) that **merges** normalized entities into existing `(paths/entity-data entity-key)` without replacing `entity-ids`.
  - Example behavior: `(update-in db (paths/entity-data :expenses) merge entities-by-id)` and leave ids unchanged.
  - Optionally: add newly seen ids to `(paths/entity-ids :expenses)` if missing.

Then change `:user-expenses/fetch-expense-success` to dispatch the merge/upsert event instead of `::sync-expenses`.

This would allow:
- keeping the list stable (no collapse)
- enriching a single row entity with detail fields (like `:items`) for the edit modal

### 2) Ensure the backend returns line items on the detail endpoint

Even after FE enrichment, the edit modal can only render what the API returns.
If `GET /api/v1/expenses/:id` returns `items: []`, line items will remain empty.

### 3) If “Update Expense” still flickers

After the textarea fix + memoization, the next suspect is any remaining reinit trigger:
- changing `:initial-values` identity (from upstream subscriptions)
- form wrapper re-mounts

A good debugging tactic is to log when `:initial-values` changes identity in the edit modal and correlate with `dirty` resets.

## Evidence / notes

- The overwrite behavior is confirmed by `register-sync-event!` implementation in `src/app/template/frontend/shared/utils/entity.cljs` (it uses `assoc-in` for both entity data + ids).
- The textarea bug was consistent with controlled inputs reverting after `input/change` events.

## Key files

- User expenses events: `src/app/template/frontend/events/user_expenses.cljs`
- Expenses adapter sync: `src/app/admin/frontend/adapters/expenses.cljs`
- Template sync helper: `src/app/template/frontend/shared/utils/entity.cljs`
- User expense form (modal): `src/app/domain/frontend/expenses/components/user_expense_form.cljs`
- Shared textarea field: `src/app/template/frontend/components/form/fields/textarea.cljs`
