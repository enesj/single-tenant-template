# Plan: Receipt Details → “Approve & Post” tab UI/UX

**Date Created**: 2026-01-07  
**Status**: Planning Phase (no code changes yet)

## Goal
Improve the Receipt Details modal’s **Approve & Post** tab to be faster to use and more informative, while keeping the existing receipt extraction + approval logic intact.

Requested changes:
1. Remove **“Use total”** button (total is auto-derived).
2. Show **Total guess** next to **Line items total**; when equal, render totals in **green**.
3. Show **Supplier guess** in the same row as **Supplier** dropdown + **New supplier** button.
4. Split current **Save Expense** action into:
   - **Save receipt** → persist form edits to the receipt record.
   - **Save expense** → create the expense from the receipt (post/approve flow).
5. Show the **receipt image** on the left side of this tab.

---

## Phase 0 — Discovery (confirm current code paths)

### UI entry point
- Receipt details modal tabs live in:
  - `src/app/domain/frontend/expenses/admin/components/detail_views.cljs`
  - Approve tab currently renders `expense-form/expense-add-form-modal` when receipt status ∈ `{extracted, review_required}`.

### Form implementation
- Expense form + submit orchestration:
  - `src/app/domain/frontend/expenses/components/expense_form.cljs`
- Shared form fields (supplier select, total input, line items input):
  - `src/app/domain/frontend/expenses/components/form_fields.cljs`

### Data needed for totals/supplier guess
- Receipt detail responses already include:
  - `:supplier_guess`
  - `:total_amount_guess`
  - `:lines-total-amount-guess`
  - `:total-guess-equals-lines-total-guess?`
  - (plus `:download_url` for preview)

**Outcome of Phase 0:** confirm all required fields are present in the receipt detail payload used by the modal; no DB schema change needed for UI-only display.

---

## Phase 1 — Layout: Image left + form right

### Change (Approve tab)
- Update `src/app/domain/frontend/expenses/admin/components/detail_views.cljs`:
  - Replace the current single-column card body with a responsive 2-column layout:
    - **Left column**: receipt preview image (reuse the preview UI from `receipt-viewer`).
    - **Right column**: approval form (`expense-add-form-modal` / new wrapper if needed).

### Implementation detail
- Extract a small reusable component (or option) for “preview-only” from:
  - `src/app/domain/frontend/expenses/components/receipt_viewer.cljs`
  - So Approve tab can show only the image and Open/Download actions, without markdown/JSON panels.

### IDs (browser testing)
- Ensure preview image/link/button IDs remain stable:
  - `receipt-viewer` already provides: `receipt-preview-img-<rid>`, `btn-download-receipt-<rid>`, etc.

---

## Phase 2 — Totals row + remove “Use total”

### 2.1 Remove “Use total” button (Approve flow only)
- Current “Use total” button is rendered by `total-amount-input`:
  - `src/app/domain/frontend/expenses/components/form_fields.cljs` (`total-amount-input`)

Plan:
- Keep current behavior for the **general expense form** (manual override may still be useful).
- Hide/remove “Use total” **only for receipt approval** by passing a field-spec flag:
  - Extend `get-expense-form-spec` in `src/app/domain/frontend/expenses/components/expense_form.cljs` to set something like:
    - `:show-use-total? false` (only when `receipt-approval?` true)
  - Update `total-amount-input` to read `field-spec` and conditionally omit the button.

### 2.2 Show “Line items total” + “Total guess” on one row
- Use:
  - computed line-items sum from current form values (already available in `total-amount-input`)
  - `receipt.total_amount_guess` for “Total guess”
  - `receipt.total-guess-equals-lines-total-guess?` to decide when to render in green

Plan:
- Pass receipt “guess context” into the total field spec:
  - `:receipt-total-guess`, `:receipt-lines-total-guess`, `:totals-match?`
- Render a single small row under the total input:
  - `Line items total: X` | `Total guess: Y`
  - If match: apply `text-success` (DaisyUI) to both values.

---

## Phase 3 — Supplier guess in supplier row

### Goal
Display the OCR supplier guess next to the Supplier select and the existing inline-create (“New supplier”) control.

Plan options:
1. **Preferred**: extend `supplier-select-with-inline-create` to accept a `:hint-right` (string or element) rendered in the same flex row as the select + New button.
2. **Fallback**: wrap the supplier field in the Approve tab layout and render the guess text beside it (less reusable).

Data:
- `receipt.supplier_guess` (string)
- Optionally indicate whether it already matches an existing supplier (`:supplier-guess-has-supplier?` / `:supplier-guess-supplier`).

IDs:
- Add a stable ID for the rendered guess text (for UI automation), e.g.:
  - `receipt-supplier-guess-<rid>`

---

## Phase 4 — Split actions: “Save receipt” vs “Save expense”

### Current behavior
- The form submit in receipt mode dispatches:
  - `:app.domain.frontend.expenses.events.receipts/approve-receipt`
  - Backend: `POST /admin/api/expenses/receipts/:id/approve` → creates expense + sets receipt status to `posted`.

### New behavior
**A) Save receipt**
- Persist the current form values to the receipt record without creating an expense.
- Purpose: allow review/edit now, post later.

**B) Save expense**
- Create the expense from the receipt (existing approve/post path).
- Must be enabled even if the user doesn’t change anything (no “must edit to enable” friction).

### Backend changes (needed for “Save receipt”)
- Add a dedicated endpoint:
  - `POST /admin/api/expenses/receipts/:id/review` (or `PATCH`)
- Handler/service should:
  - validate payload shape (same as approve payload)
  - persist “reviewed” values into receipt fields:
    - Update `raw_extract_json.extraction.items` to the reviewed items (so future UI uses the reviewed list)
    - Update `supplier_guess`, `total_amount_guess`, `currency_guess`, `purchased_at_guess` (so the receipt detail reflects reviewed values)
  - optionally update status:
    - if receipt is `review_required` and totals now match, flip to `extracted` (or introduce a “reviewed” status if desired later)

No DB migration required if we store reviewed values into existing columns/JSON.

### Frontend changes (Approve tab + events)
- Add a new re-frame event mirroring `::approve-receipt`:
  - `::save-receipt-review` → calls `/review`, refreshes receipt detail on success.
- Update `expense-add-form-modal` / `expense-form-body` to support two action buttons:
  - **Save receipt**:
    - enabled when form is valid and has changes (dirty), OR always enabled (product decision)
  - **Save expense**:
    - enabled when form is valid, regardless of dirty state
    - calls existing `::approve-receipt`

IDs (browser testing):
- `btn-save-receipt-<rid>`
- `btn-save-expense-<rid>`

---

## Phase 5 — Verification

### UI checks (manual)
- Approve tab shows image on left, form on right.
- Supplier row shows supplier guess in same row.
- Totals row shows both totals; when equal, the totals are green.
- No “Use total” button visible in Approve flow.
- “Save expense” enabled when form is valid even if no edits were made.
- “Save receipt” persists edits and they survive closing/reopening the modal.

### ClojureScript eval
- Verify the new events update app-db receipt detail and form state correctly.

### Postgres verification
- After **Save receipt**:
  - receipt row reflects reviewed values (especially `raw_extract_json.extraction.items` and guess columns).
- After **Save expense**:
  - expense created, receipt status becomes `posted`, receipt has `expense_id`.

---

## Notes / non-goals (for this plan)
- This plan does not change OCR/extraction logic.
- Status badge colors (e.g. `posted` vs `extracted`) are a separate UI pass unless explicitly requested.
