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

---

# Admin `/admin/expenses` – apply the same edit-form fixes (implementation instructions)

This section is a **step-by-step implementation guide** to bring the admin edit modal on `http://localhost:8085/admin/expenses` to parity with the fixed user edit modal on `http://localhost:8085/expenses/list`.

## Goal / expected behavior (admin)

When editing an expense from `/admin/expenses`:

1. The edit modal **loads and displays the expense details + line items** (master-detail).
2. Changing `qty` or `unit_price` **recalculates** the line’s `line_total` immediately.
3. The main `total_amount` **auto-tracks** the sum of line items (manual override still possible via “Use total”).
4. “Update Expense” **enables** when the form is dirty + valid and **disables** when invalid or when totals mismatch.
5. After saving and reopening, line items are still present (items are updated in-place using `expense_items.id`).

## Why admin edit is currently broken

Admin expense list rows are not shaped like the form expects:

- The row data passed to the modal contains **kebab-case keys** (e.g. `:total-amount`, `:supplier-id`) and usually **does not include** `:items`.
- The form spec expects **snake/underscore keys** (e.g. `:total_amount`, `:supplier_id`, `:items`) and item keys like `:raw_label`, `:unit_price`, `:line_total`.
- The existing admin form (`src/app/domain/frontend/expenses/components/expense_form.cljs`) merges `initial-data` directly into defaults, so fork sees constantly-changing `:initial-values` and can reset `dirty` → submit stays disabled/flickery.

So, admin edit needs **(1) a detail fetch** + **(2) normalization** + **(3) memoization** + **(4) submit payload preparation**, just like the user form.

## Implementation steps

### 1) Fix the admin detail response-key mismatch in the expenses event factory

The admin backend detail endpoint returns a **singular key**:

- `GET /admin/api/expenses/entries/:id` → `{ success: true, expense: {...} }`

But the factory detail handler currently extracts with `(keyword (name entity-key))`, and the FE config uses the **plural** `:expenses`, so it looks for `:expenses` and stores `nil`.

**File:** `src/app/domain/frontend/expenses/events/events_factory.cljs`

1. Update `generate-detail-events` to support a config override:
   - Add optional `:detail-response-key` to the config destructuring.
   - In `detail-loaded`, extract entity with:

     ```clojure
     (let [response-key (or detail-response-key (keyword (name entity-key)))
           entity (get response response-key)]
       ...)
     ```

2. Add a dedicated failure event for detail loads so `:detail-loading?` is cleared:
   - Register `(keyword event-ns "detail-load-failed")`
   - In that handler:
     - set `(conj base-path :detail-loading?)` to `false`
     - set `(conj base-path :error)` to `(admin-http/extract-error-message error)` (or call `finish-load` and also clear `:detail-loading?`)
   - In `load-detail`’s `:on-failure`, call `detail-load-failed` (not the list `load-failed` event).

**File:** `src/app/domain/frontend/expenses/events/entity_configs.cljs`

3. Add the override for expenses:

   ```clojure
   (def expenses-config
     {:entity-key :expenses
      ...
      :detail-response-key :expense
      ...})
   ```

After this, `(::expenses-events/load-detail expense-id)` will populate:

- `[:admin :expenses :entries :by-id expense-id]`

and `[:expenses/entry expense-id]` will return the full entity.

### 2) Make the admin edit modal fetch and use expense detail (master-detail)

**File:** `src/app/domain/frontend/expenses/components/expense_form.cljs`

Update `expense-edit-form-modal` to mirror the user version (`user-expense-edit-form-modal`):

1. Coerce `expense-id` to a stable string (`expense-id*`).
2. `use-effect` on `[expense-id*]` to dispatch the detail fetch:

   ```clojure
   (rf/dispatch [::expenses-events/load-detail expense-id*])
   ```

3. Subscribe to:
   - `current-expense` via `[:expenses/entry expense-id*]`
   - `loading?` via `[:expenses/entry-detail-loading?]`
   - `error` via `[:expenses/entries-error]` (or add a dedicated `:expenses/entry-error` sub if you want separate list/detail errors)
4. Decide `effective-data`:
   - If the detail entity is loaded for this `expense-id*`, use it.
   - Else fall back to the list row `initial-data`.
5. Normalize `effective-data` into form initial values using `use-memo`:
   - Add a local `normalize-initial-data` function (see step 3) and call it inside `use-memo` with `[effective-data]` deps.
6. Render the form body with a stable key:
   - `{:key (str "admin-expense-edit-" expense-id*) ...}`

This ensures:

- admin edit loads items into `:items`
- fork form doesn’t reset itself while typing

### 3) Normalize initial data + prepare/validate submit payload (copy from user form)

**File:** `src/app/domain/frontend/expenses/components/expense_form.cljs`

Bring these helpers over from `src/app/domain/frontend/expenses/components/user_expense_form.cljs` (same semantics):

1. **Normalization**
   - Add `normalize-initial-data` that maps admin/list keys to form keys:
     - `:supplier_id` from `(:supplier_id x)`, `(:supplier-id x)`, `(:expenses/supplier_id x)`
     - same for `:payer_id`, `:purchased_at`, `:total_amount`, `:currency`, `:notes`
   - Convert `purchased_at` into `datetime-local` format (copy `pad-two` + `datetime-local` helper).
   - Normalize `:items`:
     - ensure each item has `:id` (string)
     - string-coerce fields for inputs
     - keep `:line_total_auto?` defaulting to `true`
     - if no items, set `[(new-line-item)]`

2. **Memoization to avoid fork dirty resets**
   - In `expense-form-body`, change:
     - `entity-spec` → `use-memo` over `[suppliers payers]`
     - `form-initial-values` → `use-memo` over `[initial-data]`

   Copy the pattern from `user-expense-form-body`:

   ```clojure
   (let [entity-spec (use-memo #(get-expense-form-spec suppliers payers) [suppliers payers])
         form-initial-values (use-memo
                               (fn []
                                 (merge {:currency "BAM"
                                         :purchased_at (current-datetime-local)
                                         :items [(new-line-item)]}
                                        initial-data))
                               [initial-data])]
     ...)
   ```

3. **Prepare and validate submit payload**
   - Add `prepare-line-items` (copy from user form) so:
     - placeholder/blank items are dropped
     - `qty`, `unit_price`, `line_total` are coerced to numbers where possible
     - existing `:id` is preserved so backend updates rows in-place
   - In `handle-submit`, compute:
     - `prepared-items`
     - `computed-total` via `line-items-total`
     - `parsed-total` via `safe-parse-number`
     - `total-mismatch?` using the same `amount-tolerance` rule
   - If invalid (missing required fields, no items, total mismatch), show a `ds-alert-error` and do **not** call `on-submit`.
   - Else call `on-submit` with the canonical payload:

     ```clojure
     {:supplier_id supplier-id
      :payer_id payer-id
      :purchased_at purchased-at
      :currency currency
      :notes notes
      :total_amount effective-total
      :items prepared-items}
     ```

4. Ensure `expense-edit-form-modal` submits via:
   - `::expenses-events/update-entry-modal` (already implemented and expects `:expense` in response)

### 4) Wire the admin modal to use the normalized data

After steps 1–3:

- `expense-edit-form-modal` should pass `:initial-data normalized-data` to `expense-form-body`
- `expense-form-body` should pass `:initial-values form-initial-values` to `($ form ...)`

At this point, the admin modal should behave identically to the user modal.

## Verification checklist (admin)

### A) chrome-mcp (interactive browser test)

1. Navigate to `http://localhost:8085/admin/expenses`.
2. Click edit on a known expense:
   - `#btn-edit-expenses-<expense-id>`
3. Confirm the form is populated:
   - `#expense-total_amount` is non-empty
   - line item inputs have values:
     - `#items-<expense_item_id>-qty`
     - `#items-<expense_item_id>-unit_price`
     - `#items-<expense_item_id>-line_total`
4. Change qty and verify derived totals:
   - edit `#items-<expense_item_id>-qty`
   - assert `#items-<expense_item_id>-line_total` updates immediately
   - assert `#expense-total_amount` auto-updates (unless manual override was used)
5. Verify submit enablement:
   - `#btn-update` becomes enabled after a valid change
   - make totals mismatch intentionally → `#btn-update` should disable and show an error
6. Click `#btn-update` and confirm the request includes item IDs:
   - PUT body `items[]` includes `id` for existing items
7. Reopen the edit modal and confirm items still render (not placeholders).

### B) ClojureScript eval (state sanity checks)

Use `mcp__clojure-mcp__clojurescript_eval`:

1. Confirm the detail entity is stored:
   - check `[:admin :expenses :entries :by-id <expense-id>]` exists and has `:items`
2. Confirm the normalized form values include `:items` with `:id` (string) for each item.

### C) Postgres check (optional)

Use `mcp__postgres__execute_sql` to confirm item rows are updated in-place:

- The `expense_items.id` for an existing item should remain the same after update (qty/line_total change).
