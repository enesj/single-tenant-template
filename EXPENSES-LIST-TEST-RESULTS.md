# Expenses List Page — Chrome DevTools Test Results

**Target**: `http://localhost:8085/t/enes-jakic/expenses/list`
**Date**: 2026-03-18
**User**: Enes Jakic (owner role, `enes-jakic` workspace)
**Related plan**: `EXPENSES-LIST-TEST-PLAN.md`

---

## Summary Table

| Test | Description | Status | Notes |
|------|-------------|--------|-------|
| 1 | Page Load & Initial Render | PASS | 20 rows, 11 pages, all columns rendered |
| 2 | Pagination | PASS | Next, prev, go-to-page all work |
| 3 | Sorting | PASS | Click toggles asc/desc, data re-fetches |
| 4 | Filtering | PASS (FIXED) | Backend text filters implemented; 16 BINGO results across 2 pages |
| 5 | Row Selection & Batch Ops | PASS | Single, multi, select-all, batch edit/delete buttons |
| 6 | Row Actions | PASS | Expand items, edit modal, delete button present |
| 7 | Add Expense | N/A | Expenses created via receipt upload, not standalone add |
| 8 | Column Visibility | PASS | Visible columns match config |
| 9 | Display Settings | PASS | All settings resolved correctly |
| 10 | Loading/Error States | SKIPPED | Not directly testable without network manipulation |

---

## Test 1: Page Load & Initial Render

### Status: PASS

### Findings
- Page heading: "Moji troškovi" (h1) with subtitle "Pregledajte i upravljajte historijom troškova"
- Table renders 10 expense rows per page (first page shows KONZUM, MDC NEKRETNINE, Ist Media, DUHANPROMET, etc.)
- Pagination: "Page 1 of 11" with prev (disabled), next, go-to-page input + button
- Sidebar navigation: all expected links present (Troškovi, Stavke troškova, Učitaj, Izvještaji, etc.)
- Dashboard button visible above table
- Select-all checkbox, filter buttons on column headers, batch action buttons (disabled when 0 selected)

### Column Headers Rendered
- 📌 (pin/bookmark)
- Kupljeno (purchased-at)
- Dobavljač (supplier name)
- Trgovina (store name)
- Kategorija troška (expense category)
- Platitelj (payer)
- Valuta (currency)
- Ukupno (total amount)
- Napomene (notes)
- Kreirano (created-at)

---

## Test 2: Pagination

### Status: PASS

### Findings
- **Next page**: Clicked next → page indicator updated to "Page 2 of 11", different rows loaded, prev button enabled
- **Previous page**: Clicked prev → returned to "Page 1 of 11", prev button disabled again
- **Go-to-page**: Entered page 5 in spinbutton, clicked "Go to Page" → jumped to "Page 5 of 11" with correct data
- Network requests confirm server-side pagination with `offset` parameter

---

## Test 3: Sorting

### Status: PASS

### Findings
- Clicked "Kupljeno" (purchased-at) column header → sort arrow "↓" appeared, data refreshed
- Clicked again → arrow changed direction, data reversed
- Default sort: `purchased-at` descending (newest first)
- Network requests include `order-by` and `order-dir` params

---

## Test 4: Filtering

### Status: PASS (FIXED)

### What Works
- Filter button click opens filter popover with text input
- Text input debounces (250ms) and dispatches `::apply-filter` event
- Frontend correctly sends filter params to server (e.g., `?supplier-display-name=BINGO`)
- Active filter badge displays: "supplier-display-name: BINGO"
- Clear/Close buttons work to remove filter

### Bug Found & Fixed: Backend Text Column Filters

**Original symptom**: Typing "BINGO" in supplier filter showed "Found 1 matching item" (client-side only) with pagination still at "Page 1 of 11".

**Root cause**: `list-expenses-handler` only extracted `from`, `to`, `supplier-id`, `payer-id`, `is-posted?` — text filters were silently ignored.

**Fix applied** (following admin article routes pattern):

1. **Handler** (`crud.clj`): Added 6 text filter param extractions:
   `supplier-display-name`, `store-display-name`, `expense-category-name`, `payer-label`, `currency`, `notes`

2. **Service** (`user_expenses.clj`): Added `apply-text-filters` helper with ILIKE conditions.
   Updated `list-user-expenses` and `count-user-expenses` to accept and apply text filters.
   `count-user-expenses` conditionally adds LEFT JOINs only when text filters are present.

### Post-Fix Verification
- Typed "BINGO" in supplier filter → **"Found 16 matching items"**
- Pagination: **"Page 1 of 2"** (16 items / 20 per page)
- All visible rows show "BINGO" as supplier across multiple stores (PJ 91 SUPERMARKET, PJ 213 HIPERMARKET MERKUR, PJ 219 Supermarket Alta, PJ 57 HIPERMARKET Otoka)
- Server-side filtering confirmed working end-to-end

---

## Test 5: Row Selection & Batch Operations

### Status: PASS

### Findings
- **Single select**: Click row checkbox → checkbox checked, "1 selected" counter appears
- **Multi select**: Check second row → "2 selected", batch buttons enabled:
  - "Edit 2 selected items" button
  - "Delete 2 selected items" button
  - "More actions for 2 selected items" (⋯) dropdown
- **Select-all**: Header checkbox selects all visible rows, state shows "mixed" when partial
- **Deselect**: Uncheck header checkbox → all deselected, batch buttons disabled ("0 selected")
- Batch buttons correctly disabled when 0 items selected

---

## Test 6: Row Actions

### Status: PASS

### Findings

#### 6.1 Row Expansion
- Clicked "Expand items" button on a row (MDC NEKRETNINE, uid=24_53)
- Expansion panel appeared below the row showing line items table:
  - Columns: Stavka (Article), Količina (Qty), Cijena (Unit Price), Ukupno (Total)
  - Example item: "Parking 1h" — Qty: 1, Price: 2 BAM, Total: 2 BAM
- Clicked expand again → panel collapsed

#### 6.2 Edit Action
- Clicked edit button on KONZUM row
- Edit modal opened with:
  - All form fields populated (supplier dropdown, store, purchased-at, total, currency, payer, notes)
  - Line items section with article rows
  - Each line item editable (article, quantity, unit price, total)
  - Save and Cancel buttons visible
- Cancel button closed the modal, returned to list view
- Note: Modal fetches fresh data from server (not using row summary)

#### 6.3 Delete Action
- Delete button present on each row (verified, not clicked to avoid data loss)

#### 6.4 Actions Dropdown (⋯)
- "Actions" dropdown button present on each row

---

## Test 7: Add Expense

### Status: N/A

### Findings
- The expenses list page does NOT have a standalone "Add" button visible in the UI
- An element with `id="btn-add-"` exists in the DOM but has empty text (likely hidden)
- `show-add-button?` is `true` in display settings
- Expenses are primarily created through the receipt upload flow (`/expenses/upload`)
- The "Dashboard" button (Nadzorna ploča) is visible above the table for navigation

---

## Test 8: Column Visibility

### Status: PASS

### Findings (from `::visible-columns` subscription)

| Column | Visible | Notes |
|--------|---------|-------|
| purchased-at | true | "Kupljeno" |
| supplier-display-name | true | "Dobavljač" |
| store-id | false | Hidden by default |
| expense-category-id | false | Hidden by default |
| payer-label | true | "Platitelj" |
| currency | true | "Valuta" |
| notes | true | "Napomene" |
| created-at | true | "Kreirano" |
| is-posted | false | Hidden |
| updated-at | false | Hidden |
| user-id | false | Hidden |
| created-by | false | Hidden |
| created-by-name | false | Hidden |
| tenant-id | false | Hidden |
| supplier-id | false | Hidden (raw FK) |
| payer-id | false | Hidden (raw FK) |
| receipt-id | false | Hidden |
| id | false | Hidden |

Locked columns: `receipt-id: false`, `is-posted: false`, `created-at: true` — users cannot toggle these.

---

## Test 9: Display Settings Integration

### Status: PASS

### Effective Display Settings (`::entity-display-settings` for `:expenses`)

```clojure
{:show-delete?         true
 :show-edit?           true
 :show-batch-delete?   true
 :show-batch-edit?     true
 :show-select?         true
 :show-timestamps?     true
 :show-selected-rows?  true
 :show-unselected-rows? true
 :show-add-button?     true
 :show-highlights?     true
 :show-pagination?     true
 :show-filtering?      true
 :per-page             20}
```

### Locked Display Settings (`::locked-display-settings` for `:expenses`)

```clojure
{:show-batch-delete? true
 :show-batch-edit?   true}
```

These locked settings mean users cannot disable batch operations through user settings.

---

## Test 10: Loading & Error States

### Status: SKIPPED

Not directly testable without network throttling or error injection. The page loads quickly enough that loading states are not observable via screenshot.

---

## Bugs Found & Fixed

### BUG-1: Server-Side Text Column Filtering — FIXED

**Priority**: Medium → **RESOLVED**
**Files changed**:
- `src/app/domain/backend/expenses/handlers/user_expenses/crud.clj` — extract 6 text filter params
- `src/app/domain/backend/expenses/services/user_expenses.clj` — `apply-text-filters` + conditional JOINs in count query

**Supported text filters**: `supplier-display-name`, `store-display-name`, `expense-category-name`, `payer-label`, `currency`, `notes`

### BUG-2: Add Button Rendered But Not Visible (NOT FIXED)

**Priority**: Low
**Impact**: `show-add-button?` is `true` and DOM has `btn-add-` element, but it's not visible. Either the button should be visible or the setting should be `false` for expenses (since expenses are created via receipt upload).

---

## Environment Notes

- **Locale**: Bosnian (`:bs`) — all UI labels in Bosnian
- **Pagination mode**: Server-side (`:server`) with refresh event
- **Per-page**: 20 items (user preference; domain default is 25)
- **Total expenses**: ~200+ (11 pages × ~20 items)
- **Role**: Owner (sees all tenant expenses)
