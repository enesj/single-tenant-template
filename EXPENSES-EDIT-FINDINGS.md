# Expenses edit/list debugging findings

This document summarizes the issues investigated around `http://localhost:8085/expenses/list` (user expenses), what was found, what was changed, and how it was verified.

## Symptoms observed

1. **Line totals didn’t update reliably**
   - After entering qty + unit price, the line total sometimes didn’t recalculate on subsequent edits.

2. **Expense total didn’t auto-track line items**
   - `total_amount` didn’t automatically follow the sum of line items (only updated after clicking “Use total”).

3. **“Update Expense” stayed disabled**
   - Even after valid changes (and total matching line-item sum), the submit button stayed disabled or flickered.

4. **Edit modal didn’t populate line items (after reopen)**
   - After saving an expense and reopening edit, line items could be empty/placeholder.

5. **Textarea edits didn’t “stick”**
   - `notes` behaved like a controlled input whose `:on-change` was not wired.

## Root causes

### A) Line-items state & total syncing logic was incomplete

- `line_total` wasn’t consistently recalculated when qty/unit price changed.
- The total input started in a “manual” mode that required explicitly pressing “Use total”.

### B) Fork dirty-state reset from unstable `:initial-values`

- The template `form` uses fork/fork.re-frame; fork treats `:initial-values` changes as reinitialization.
- In the expenses edit modal, `initial-data` was being normalized/merged into a **fresh map** on each render, changing identity and causing fork to reset `dirty`.

### C) Shared textarea component wasn’t applying `:on-change`

- `src/app/template/frontend/components/form/fields/textarea.cljs` previously merged props incorrectly, so the actual `:on-change` handler wasn’t applied.

### D) Single-entity “sync” overwrote the entire `:expenses` entity store

- `register-sync-event!` was correct for full-list sync, but dispatching it with a **single** expense replaced `(paths/entity-data :expenses)` + `(paths/entity-ids :expenses)`, collapsing the list.
- Fix: add an explicit **upsert** path for single-entity detail fetch.

### E) Backend update ignored line items + FE submit dropped item IDs

- Server update path originally updated only the expense fields, not `expense_items`.
- FE submit originally stripped `:id` from items, forcing delete+insert behavior and breaking stable item identity.

## Changes made (code pointers)

### Frontend (edit form UX & correctness)

- Line totals recalc on qty/unit price edits:
  - `src/app/domain/frontend/expenses/components/form_fields.cljs`
- Total amount auto-syncs to the line-items sum by default (“Use total” toggles back to auto mode):
  - `src/app/domain/frontend/expenses/components/form_fields.cljs`
- Preserve `expense_items.id` in submit payload:
  - `src/app/domain/frontend/expenses/components/user_expense_form.cljs` (`prepare-line-items`)
- Stable `fork` dirty tracking (no reinit on render):
  - `src/app/domain/frontend/expenses/components/user_expense_form.cljs` (memoized `entity-spec`, `initial-values`, and normalized edit data)
- Fix textarea `on-change` wiring:
  - `src/app/template/frontend/components/form/fields/textarea.cljs`
- Fetch expense detail for edit modal so items are present:
  - `src/app/template/frontend/events/user_expenses.cljs` (`:user-expenses/fetch-expense`)
- Upsert single expense into entity store (don’t clobber list):
  - `src/app/admin/frontend/adapters/expenses.cljs` + `src/app/template/frontend/shared/utils/entity.cljs`
- Fix “Update Expense” enable/disable logic (dirty/valid):
  - `src/app/template/frontend/subs/form.cljs`

### Frontend (browser-testing IDs)

Added stable IDs for interactive form elements (needed for chrome-mcp):

- Total amount input: `#expense-total_amount`
- Line-items inputs:
  - `#items-<expense_item_id>-raw_label`
  - `#items-<expense_item_id>-qty`
  - `#items-<expense_item_id>-unit_price`
  - `#items-<expense_item_id>-line_total`
- Line-items buttons:
  - `#btn-add-items-line-item`
  - `#btn-remove-items-line-item-<expense_item_id>`

File:
- `src/app/domain/frontend/expenses/components/form_fields.cljs`

### Backend (master-detail update)

- `update-expense!` now performs a proper master-detail update:
  - update the expense fields
  - delete removed items
  - update existing items (by `id`)
  - insert new items
  - return the expense **with items**

Files:
- `src/app/domain/backend/expenses/services/expenses.clj`
- `src/app/domain/backend/expenses/handlers/user_expenses.clj`

### Backend (error response shape)

- Ensure admin-auth middleware errors are real JSON (avoid FE parse errors on 401/403):
  - `src/app/template/backend/middleware/admin.clj`

### Tests

- Added focused integration test for updating expenses + items:
  - `test/app/domain/expenses/services/expenses_services_test.clj`

## Evidence / verification

### chrome-mcp (edit flow)

For expense `cbcb31ce-1ccd-45be-a7d7-bc59fb287a72`:

1. Open edit modal from list: `#btn-edit-expenses-cbcb31ce-1ccd-45be-a7d7-bc59fb287a72`
2. Change qty: `#items-60abcf64-7a9a-4342-b7d1-9cb745312b51-qty` → line total updates immediately.
3. Confirm total auto-updates: `#expense-total_amount`.
4. Confirm “Update Expense” enables after valid change: `#btn-update`.
5. Submit and confirm request body includes item `id` and response returns `items[]` populated.
6. Reopen edit modal and confirm items are still present.

### ClojureScript eval

- Verified `app-db` state for current expense + entity store and validated the submit payload includes `items[].id`.

### Postgres check

- Verified `expense_items` row is updated in-place (same `id`, updated qty/line_total) for the above expense.

## Fork form support (master-detail)

Fork supports master-detail via `fork/field-array` (insert/remove handlers and per-row change/blur handlers). Our vendored version (`vendor/fork/re_frame.cljs`) exposes `field-array`, so master-detail forms are supported in this codebase.
