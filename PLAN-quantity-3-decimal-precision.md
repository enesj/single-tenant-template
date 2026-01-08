# Plan: Support 3-Decimal Precision for Quantity in Line Items

**Date Created**: 2026-01-08
**Status**: Implemented (2026-01-08)

## Goal

Update the quantity field in the "Approve & Post" tab's Line Items form to support **3 decimal places** (step `0.001`) instead of the current 2 decimal places (step `0.01`). This is necessary because quantity can represent **weight** (e.g., 1.250 kg), which requires more precision.

---

## Phase 0 — Discovery (Current State Analysis)

### Current Configuration - Already Correct! ✓

**File**: `src/app/domain/frontend/expenses/admin/config/form-fields.edn`

The admin config already specifies 3-decimal precision for quantity:

```edn
:expense-items
{...
 :field-config
 {...,
  :qty {:type :number, :step 0.001},   ;; ← Already 3 decimals!
  ...}}

:price-observations
{...
 :field-config
 {...,
  :qty {:type :number, :step 0.001},   ;; ← Already 3 decimals!
  ...}}
```

**Note**: The admin config is used by the **generic admin CRUD system** for expense-items and price-observations tables.

---

### Hardcoded Form - Needs Update ⚠️

**File**: `src/app/domain/frontend/expenses/components/expense_form.cljs`

**Current code** (lines 35-57):
```clojure
(def line-item-columns
  [{:id :raw_label
    :label "Label"
    :type :text
    :placeholder "e.g. Milk, Bread"}
   {:id :qty
    :label "Qty"
    :type :number
    :step "0.01"    ;; ← Only 2 decimals (needs change to "0.001")
    :min "0"
    :width "w-24"}
   {:id :unit_price
    :label "Unit Price"
    :type :number
    :step "0.01"
    :min "0"
    :width "w-32"}
   {:id :line_total
    :label "Line Total"
    :type :number
    :step "0.01"
    :min "0"
    :width "w-32"}])
```

**Problem**: This hardcoded definition is used by:
- `expense-add-form-modal` (receipt approval flow)
- `expense-edit-form-modal`
- `expense-form-body` (general expense form)

These forms use the **template form system**, not the generic admin CRUD system, so they don't read from the admin config.

---

### Formatting Function - Needs Review ⚠️

**File**: `src/app/domain/frontend/expenses/components/form_fields.cljs`

**Current code** (lines 53-57):
```clojure
(defn format-decimal
  "Format a number to a 2 decimal place string for inputs."
  [n]
  (when (some? n)
    (.toFixed n 2)))
```

**Question**: Should this be updated to support variable decimal places?

**Analysis**:
- Used for `unit_price`, `line_total`, and `total_amount` - these should remain 2 decimals (currency)
- `qty` values are NOT formatted by this function in the current flow
- The line item input displays values directly from the form state without formatting

**Conclusion**: `format-decimal` does NOT need to change. It's correctly used only for monetary values.

---

### User Expense Pages - Separate Issue

**File**: `src/app/domain/frontend/expenses/pages/user/expense_new.cljs`

**Current code** (lines 99-103):
```clojure
($ :input {:type "number"
           :class "ds-input ds-input-sm ds-input-bordered col-span-2"
           :placeholder "Qty"
           :value (or qty "")
           :on-change #(on-change id :qty (.. % -target -value))})
```

**Problem**: No `:step` attribute specified at all. Defaults to browser default (usually 1).

**Note**: This is in the **user-facing expenses** app, not the admin panel. May be a separate consideration.

---

## Phase 1 — Update Hardcoded Line Item Columns

### File: `src/app/domain/frontend/expenses/components/expense_form.cljs`

**Location**: Lines 35-57, specifically line 43

**Change**:
```clojure
;; Before:
{:id :qty
 :label "Qty"
 :type :number
 :step "0.01"    ;; 2 decimals
 :min "0"
 :width "w-24"}

;; After:
{:id :qty
 :label "Qty"
 :type :number
 :step "0.001"   ;; 3 decimals for weight support
 :min "0"
 :width "w-24"}
```

**Impact**:
- ✓ Receipt approval flow (Approve & Post tab)
- ✓ Admin expense creation modal
- ✓ Admin expense edit modal
- ✓ All forms using `expense-form-body` component

---

## Phase 2 — Verify No Other Changes Needed

### Check: format-decimal function
**Verdict**: No change needed - only used for currency values (unit_price, line_total, total_amount)

### Check: Validation logic
**Location**: `expense_form.cljs` lines 371-403

The `validate-expense-values` and `validate-receipt-review-values` functions check:
- Supplier, payer, date presence
- At least one line item
- Total amount > 0
- Total matches line items

**Verdict**: No quantity-specific validation to update.

### Check: Data submission
**Location**: `expense_form.cljs` lines 441-454

`prepare-expense-submit-values` uses `prepare-line-items` which parses qty via `safe-parse-number`.

**Verdict**: `safe-parse-number` handles any decimal precision - no change needed.

### Check: Database schema
**Verdict**: Numeric/decimal columns in PostgreSQL typically support up to 6+ decimal places. The `qty` column should already support 3 decimals. No migration needed.

---

## Phase 3 — Verification

### UI Checks (Manual)

1. **Navigate to receipts detail page** (`/admin/receipts/:id`)
2. **Click "Approve & Post" tab**
3. **Click in Quantity field** of a line item
4. **Verify**:
   - Browser shows step arrows incrementing by 0.001
   - Can type values like `1.234` or `0.500`
   - Values are preserved (not truncated)

5. **Test calculation**:
   - Enter qty: `0.500`
   - Enter unit price: `10.00`
   - Verify line total: `5.00` (correctly calculated)

### Browser Testing

```javascript
// In browser console, verify step attribute
document.querySelector('input[id*="qty"]').step
// Expected: "0.001"
```

### ClojureScript REPL Verification

```clojure
(require '[app.domain.frontend.expenses.components.expense-form :as form])

;; Verify the column definition
(:step (nth (:line-item-columns form/form-fields) 1))
;; Expected: "0.001"
```

### Database Verification (Post-Deployment)

```sql
-- Create an expense with 3-decimal qty
INSERT INTO expense_items (expense_id, raw_label, qty, unit_price, line_total)
VALUES ('<expense-uuid>', 'Test Item', 1.234, 10.00, 12.34);

-- Verify qty is stored with full precision
SELECT qty, round(qty::numeric, 3) as qty_rounded
FROM expense_items
WHERE raw_label = 'Test Item';
-- Expected: qty = 1.234, qty_rounded = 1.234
```

---

## Summary of Changes

| File | Change |
|------|--------|
| `src/app/domain/frontend/expenses/components/expense_form.cljs` | Set the qty input `:step` to `"0.001"` (3 decimal places) |
| `test/app/domain/frontend/expenses/components/expense_form_test.cljs` | Added a regression test ensuring qty step stays at `"0.001"` |

**Total**: 2 files touched

---

## Related: User-Facing Expenses App

The user-facing expenses app (`/expenses`) has a separate implementation in:
- `src/app/domain/frontend/expenses/pages/user/expense_new.cljs`

This file also defines line item inputs but **without a step attribute**, which means it defaults to step=1.

**Decision**: Is this in scope?

- If yes: Add `:step "0.001"` to the qty input around line 99-103
- If no: Leave as is (user can still type decimals, browser just won't show proper increment buttons)

---

## Notes

### Why This Change Is Safe

1. **Backward Compatible**: Existing 2-decimal values (e.g., `2.50`) still work perfectly
2. **More Precision**: Adding a decimal place only increases precision, never reduces it
3. **No Data Migration**: Database already supports higher precision
4. **No Breaking Changes**: The API and validation logic don't depend on specific decimal places

### Input Type: Number vs Text

Using `<input type="number" step="0.001">` provides:
- ✓ Mobile numeric keypad
- ✓ Browser validation
- ✓ Increment/decrement buttons
- ✓ Proper sorting in tables

Alternative would be `type="text"` with custom validation, but that loses UX benefits.

### Currency vs Weight Precision

- **Currency (2 decimals)**: BAM, EUR, USD use 2 decimal places
- **Weight (3 decimals)**: Common in retail (e.g., 1.234 kg, 0.500 kg)
- **Unit price**: Still 2 decimals (price per unit is currency)

---

## Non-Goals

- This plan does NOT change currency precision (unit_price, line_total, total_amount remain 2 decimals)
- This plan does NOT modify the database schema
- This plan does NOT change validation logic
- This plan does NOT address the user-facing expenses app (unless specified)
