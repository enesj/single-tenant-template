# Expenses List Page — Chrome DevTools Test Plan

**Target**: `http://localhost:8085/t/enes-jakic/expenses/list`
**Tool**: `chrome-devtools` MCP
**Prerequisite**: Logged in as admin/owner of "Enes Jakic" workspace

---

## 1. Page Load & Initial Render

### 1.1 Navigate and verify initial render
- `navigate_page` to `http://localhost:8085/t/enes-jakic/expenses/list`
- `take_screenshot` — confirm page loaded
- `take_snapshot` — verify:
  - Page heading / title present
  - Table rendered with expense rows
  - "Add" button visible (ID: `btn-add-expenses`)
  - Pagination controls visible (`btn-prev-page`, `btn-next-page`)

### 1.2 Verify table structure
- `take_snapshot` — check:
  - Column headers present (e.g., supplier, amount, date, payer)
  - Select-all checkbox (ID: `select-all-expenses`)
  - Row action buttons (edit, delete) per row
  - Table ID: `table-expenses`

### 1.3 Verify reference data loaded
- `list_network_requests` — confirm:
  - `GET /api/v1/expenses` (main data)
  - `GET /api/v1/expenses/suppliers` (FK dropdown)
  - `GET /api/v1/expenses/payers` (FK dropdown)
  - Reference data requests completed successfully (200)

---

## 2. Pagination

### 2.1 Verify pagination controls
- `take_snapshot` — find:
  - Current page indicator (e.g., "Page 1 of N")
  - Previous button (ID: `btn-prev-page`) — should be disabled on page 1
  - Next button (ID: `btn-next-page`)
  - Go-to-page input (ID: `input-goto-page`)
  - Go-to-page button (ID: `btn-goto-page`)

### 2.2 Navigate to next page
- `click` on `btn-next-page`
- `take_screenshot` — verify:
  - Page indicator updated to "Page 2 of N"
  - Table data refreshed (different rows)
  - Previous button now enabled
  - Network request fired with offset > 0

### 2.3 Navigate to previous page
- `click` on `btn-prev-page`
- `take_screenshot` — verify page returns to 1

### 2.4 Go-to-page
- `fill` input `input-goto-page` with a valid page number
- `click` `btn-goto-page`
- `take_screenshot` — verify page jumped to entered number

---

## 3. Sorting

### 3.1 Verify sortable column headers
- `take_snapshot` — find column headers with sort indicators
- Note which column is currently sorted (default: `created_at` DESC)

### 3.2 Sort by a column
- `click` on a sortable column header (e.g., supplier name)
- `take_screenshot` — verify:
  - Sort arrow appears (↑ or ↓)
  - Table data reloads in new order
- `click` same column again — verify sort direction reverses

### 3.3 Verify network request includes sort params
- `list_network_requests` — confirm `order-by` and `order-dir` params sent

---

## 4. Filtering

### 4.1 Verify filter controls
- `take_snapshot` — find filter icons on filterable columns
- Note which columns have filter icons

### 4.2 Apply a text filter
- `click` filter icon on a text column (e.g., supplier name)
- `take_screenshot` — verify filter input/form appears
- `fill` filter input with a search term
- Submit/apply the filter
- `take_screenshot` — verify:
  - Table shows filtered results
  - Active filter badge/indicator visible
  - Page resets to 1

### 4.3 Clear filter
- Clear the active filter
- `take_screenshot` — verify full results restored

---

## 5. Row Selection & Batch Operations

### 5.1 Select individual rows
- `click` checkbox on first row (ID: `select-expenses-{id}`)
- `take_screenshot` — verify:
  - Checkbox checked
  - Row visually highlighted
  - Batch action buttons may appear

### 5.2 Select all rows
- `click` select-all checkbox (ID: `select-all-expenses`)
- `take_screenshot` — verify:
  - All visible row checkboxes checked
  - Batch buttons enabled (batch-edit, batch-delete)

### 5.3 Batch edit
- With 2+ rows selected, `click` batch edit button (ID: `btn-batch-edit-expenses`)
- `take_screenshot` — verify:
  - Batch edit form appears
  - Only common editable fields shown
- Cancel/close batch edit form

### 5.4 Batch delete
- With 2+ rows selected, find batch delete button (ID: `btn-batch-delete-expenses`)
- Verify button is present and enabled
- (Do NOT click to avoid data loss — verify presence only)

### 5.5 Deselect all
- `click` select-all checkbox again to deselect
- `take_screenshot` — verify all checkboxes unchecked, batch buttons hidden

---

## 6. Row Actions

### 6.1 Edit action
- Find edit button on a row (ID: `btn-edit-expenses-{id}`)
- `click` the edit button
- `take_screenshot` — verify:
  - Edit modal opens with expense data
  - Form fields populated
  - Cancel and Save buttons visible

### 6.2 Cancel edit
- `click` cancel/close button on edit modal
- `take_screenshot` — verify modal closed, table visible

### 6.3 Delete action
- Find delete button on a row (ID: `btn-delete-expenses-{id}`)
- Verify button is present
- (Do NOT click to avoid data loss)

### 6.4 Row expansion (power-user)
- If logged in as power-user with expenses that have line items:
  - Find expand button (ID: `btn-expand-expenses-{id}`)
  - `click` expand button
  - `take_screenshot` — verify:
    - Expansion panel appears below row
    - Line items table visible (Article, Qty, Unit Price, Total)
    - Chevron rotated 90 degrees
  - `click` expand button again — verify panel collapses

---

## 7. Add Expense (Modal)

### 7.1 Open add form
- `click` add button (ID: `btn-add-expenses`)
- `take_screenshot` — verify:
  - Add modal opens
  - Form fields: supplier, amount, date, payer, notes, etc.
  - FK dropdowns populated (suppliers, payers)
  - Submit and Cancel buttons visible

### 7.2 Close add form without saving
- `click` close/cancel button
- `take_screenshot` — verify modal closed, no data changed

---

## 8. Column Visibility

### 8.1 Check visible columns
- `take_snapshot` — count visible columns in table header
- Compare against expected default visible columns from config

### 8.2 Verify hidden columns are not rendered
- Check that columns with visibility `false` are absent from DOM

---

## 9. Display Settings Integration

### 9.1 Verify display settings applied
- `evaluate_script` to check current display settings:
  ```js
  re_frame.core.subscribe(cljs.core.vector(
    cljs.core.keyword("app.template.frontend.subs.ui", "entity-display-settings"),
    cljs.core.keyword("expenses")
  )).deref()
  ```
- Verify settings match expected config (show-edit?, show-delete?, show-select?, per-page, etc.)

### 9.2 Verify locked settings
- `evaluate_script` to check locked settings:
  ```js
  re_frame.core.subscribe(cljs.core.vector(
    cljs.core.keyword("app.template.frontend.subs.ui", "locked-display-settings"),
    cljs.core.keyword("expenses")
  )).deref()
  ```

---

## 10. Loading & Error States

### 10.1 Loading state
- On page navigation, verify loading spinner or skeleton appears briefly
- `take_screenshot` during initial load (if possible)

### 10.2 Empty state
- (If testable) Navigate with filters that return 0 results
- Verify empty state message displayed

---

## Quick Reference: Key Element IDs

| Element | ID Pattern | Example |
|---------|-----------|---------|
| Table | `table-expenses` | — |
| Select all | `select-all-expenses` | — |
| Row checkbox | `select-expenses-{id}` | `select-expenses-42` |
| Add button | `btn-add-expenses` | — |
| Edit button | `btn-edit-expenses-{id}` | `btn-edit-expenses-42` |
| Delete button | `btn-delete-expenses-{id}` | `btn-delete-expenses-42` |
| Expand button | `btn-expand-expenses-{id}` | `btn-expand-expenses-42` |
| Batch edit | `btn-batch-edit-expenses` | — |
| Batch delete | `btn-batch-delete-expenses` | — |
| Prev page | `btn-prev-page` | — |
| Next page | `btn-next-page` | — |
| Go-to input | `input-goto-page` | — |
| Go-to button | `btn-goto-page` | — |
| Add modal | `modal-add-expenses` | — |
| Close add modal | `btn-close-add-modal-expenses` | — |

## Key API Endpoints

| Action | Method | Endpoint |
|--------|--------|----------|
| List expenses | GET | `/api/v1/expenses?limit=N&offset=N&order-by=X&order-dir=Y` |
| Create expense | POST | `/api/v1/expenses` |
| Update expense | PUT | `/api/v1/expenses/{id}` |
| Batch delete | DELETE | `/api/v1/expenses/batch` |
| Suppliers (FK) | GET | `/api/v1/expenses/suppliers` |
| Payers (FK) | GET | `/api/v1/expenses/payers` |
| Categories (FK) | GET | `/api/v1/expenses/expense-categories` |
