# Admin Settings → Articles Page Verification Plan

**Goal**: Verify that settings configured on `/admin/admin-settings` for the **Articles** entity are actually applied on the `/admin/articles` list page.

**Tool**: Chrome DevTools MCP (observation-only, no code changes)

**Baseline**: Articles currently has "4 defaults, No locks" (from test results).

**Strategy**: For each setting category, record the current admin setting → navigate to articles page → verify it manifests in the UI → optionally change the setting → verify the change takes effect → restore original state.

---

## Pre-Test: Record Baseline State

### P.1 Capture current Articles admin settings

1. Navigate to `/admin/admin-settings`
2. Enter Edit Mode → select entity "articles"
3. **View Options tab**: Record all 9 display toggles (Default + Lock) and Rows per page (Default + Lock)
4. **List Behavior section**: Record Form display, Disallowed action mode, and all 4 Action gates
5. Switch to **Form Fields tab**: Screenshot/record which fields are checked for Create and Edit
6. Switch to **Table Columns tab**: Record column policy (Default visible + Lock visible per column), plus structural config (available columns, always-visible, display labels, filterable, sortable)
7. Stop editing

### P.2 Capture current Articles page state

1. Navigate to `/admin/articles`
2. Take screenshot
3. Record: which toolbar buttons are visible (Add, Batch Edit, Batch Delete), whether filtering controls appear, whether pagination appears, whether row highlights are present, whether Edit/Delete action buttons appear on rows, how many rows per page, which columns are shown, column header labels

---

## Test 1: Display Toggle — Show Edit Button

**Setting**: `show-edit?` (Default / Lock)
**Where visible**: Row action buttons (edit icon/button per row)

### 1.1 Verify current state

1. On `/admin/admin-settings` → Articles → View Options: Note `Edit` toggle state
2. On `/admin/articles`: Check if edit action button appears on article rows
3. Expected: If Default On → edit button visible; If Default Off → edit button hidden; If Inherit → system default applies

### 1.2 Toggle and verify

1. Change `Edit` Default to the **opposite** of current (e.g., Default On → Default Off or vice versa)
2. Save settings
3. Navigate to `/admin/articles`, reload
4. Verify edit button presence changed accordingly
5. **Restore** original setting and save

### 1.3 Lock and verify

1. Set `Edit` Lock to "Locked Off"
2. Save settings
3. Navigate to `/admin/articles` → verify edit button is hidden
4. Navigate to `/admin/user-settings` → verify user cannot override the Edit toggle (should be locked/grayed)
5. **Restore** Lock to "Inherit" and save

---

## Test 2: Display Toggle — Show Delete Button

**Setting**: `show-delete?`
**Where visible**: Row action buttons (delete icon/button per row)

### 2.1 Verify current state

1. Record `Delete` toggle (Default + Lock) for Articles
2. Check `/admin/articles` — do rows show a delete action?

### 2.2 Toggle and verify

1. Set `Delete` Default to "Default Off" → Save
2. On `/admin/articles`: verify delete action disappears from rows
3. Set back to "Default On" (or Inherit) → Save → verify it reappears
4. **Restore** original

---

## Test 3: Display Toggle — Show Selection (Checkboxes)

**Setting**: `show-select?`
**Where visible**: Checkbox column on each row + "Select all" checkbox in header

### 3.1 Verify current state

1. Record `Selection` toggle for Articles
2. On `/admin/articles`: Are row checkboxes visible? Is there a "Select all" checkbox?

### 3.2 Toggle and verify

1. Change `Selection` to opposite → Save
2. Verify checkboxes appear/disappear on articles page
3. **Restore** original

---

## Test 4: Display Toggle — Show Filtering

**Setting**: `show-filtering?`
**Where visible**: Filter controls above or beside the table (search bar, column filters, filter toggles)

### 4.1 Verify current state

1. Record `Filtering` toggle for Articles
2. On `/admin/articles`: Are filter controls visible?

### 4.2 Toggle and verify

1. Change `Filtering` to "Default Off" → Save
2. On `/admin/articles`: verify filter controls are hidden
3. Change to "Default On" → verify they reappear
4. **Restore** original

---

## Test 5: Display Toggle — Show Pagination

**Setting**: `show-pagination?`
**Where visible**: Pagination controls (page numbers, next/prev, per-page selector) below the table

### 5.1 Verify current state

1. Record `Pagination` toggle for Articles
2. On `/admin/articles`: Are pagination controls visible? (page selector, row count info)

### 5.2 Toggle and verify

1. Change `Pagination` to "Default Off" → Save
2. On `/admin/articles`: verify pagination controls disappear
3. **Restore** original

---

## Test 6: Display Toggle — Show Highlights

**Setting**: `show-highlights?`
**Where visible**: Visual highlights/badges on rows (e.g., new items, recently modified indicators)

### 6.1 Verify current state

1. Record `Highlights` toggle for Articles
2. On `/admin/articles`: Note any row highlighting/badges

### 6.2 Toggle and verify

1. Change `Highlights` to opposite → Save
2. Verify highlight indicators change on articles page
3. **Restore** original

---

## Test 7: Display Toggle — Show Add Button

**Setting**: `show-add-button?`
**Where visible**: "Add" / "New Article" button in the toolbar/header area

### 7.1 Verify current state

1. Record `Add Button` toggle for Articles
2. On `/admin/articles`: Is the Add/New button visible?

### 7.2 Toggle and verify

1. Set `Add Button` to "Default Off" → Save
2. On `/admin/articles`: verify Add button is hidden
3. Set to "Default On" → verify it reappears
4. **Restore** original

---

## Test 8: Display Toggles — Batch Edit & Batch Delete

**Setting**: `show-batch-edit?` and `show-batch-delete?`
**Where visible**: Batch action buttons that appear when rows are selected

### 8.1 Verify current state

1. Record `Batch Edit` and `Batch Delete` toggles for Articles
2. On `/admin/articles`: Select one or more rows (if checkboxes visible) → do Batch Edit / Batch Delete buttons appear?

### 8.2 Toggle and verify

1. Set `Batch Edit` to "Default Off" → Save
2. Select rows → verify Batch Edit button is absent
3. Set `Batch Delete` to "Default Off" → Save
4. Verify Batch Delete button is absent
5. **Restore** both to original

---

## Test 9: Rows Per Page Setting

**Setting**: `per-page` (Default + Lock)
**Where visible**: Number of rows displayed in the table, pagination per-page selector

### 9.1 Verify current state

1. Record `Rows per page` Default and Lock for Articles
2. On `/admin/articles`: How many rows are shown? What per-page value is selected?

### 9.2 Change default and verify

1. Set `Rows per page` Default to a distinctive value (e.g., 5)
2. Save settings
3. Navigate to `/admin/articles` (may need to **clear local overrides** or use incognito to see the default apply, since user pref may already be stored)
4. Verify 5 rows shown per page
5. **Restore** original

### 9.3 Lock per-page and verify

1. Set `Rows per page` Lock to a value (e.g., 25) → Save
2. Navigate to `/admin/articles` → verify 25 rows shown
3. Verify the per-page selector is **disabled/locked** (user cannot change it)
4. **Restore** Lock to "—" (Inherit)

---

## Test 10: Form Fields Configuration

**Setting**: Form fields checklist (which fields appear in Add/Edit forms)
**Where visible**: Article create form and article edit form

### 10.1 Record current form fields

1. On admin-settings → Articles → **Form Fields** tab
2. Record which fields are checked for **Create** column and **Edit** column
3. Note any differences between Create and Edit

### 10.2 Verify against Add form

1. On `/admin/articles`: Click "Add" button (or equivalent) to open the create form
2. Record which input fields appear
3. Cross-reference with the admin-settings Form Fields "Create" checklist — all checked fields should appear

### 10.3 Verify against Edit form

1. On `/admin/articles`: Click "Edit" on an existing article
2. Record which input fields appear in the edit form
3. Cross-reference with the admin-settings Form Fields "Edit" checklist

### 10.4 Toggle a field and verify

1. On admin-settings → Articles → Form Fields: Uncheck a currently-visible field (e.g., "Category") for Create
2. (Form Fields save immediately via PATCH — no Save button needed)
3. On `/admin/articles`: Open Add form → verify the unchecked field is now absent
4. **Restore**: Re-check the field

---

## Test 11: Table Columns — Visibility

**Setting**: Column visibility (Default visible / Lock visible per column)
**Where visible**: Which columns appear in the articles table header and data rows

### 11.1 Record current column state

1. On admin-settings → Articles → **Table Columns** tab
2. Record all columns, their default visibility, and lock status
3. Note which are marked "Always visible"

### 11.2 Verify on articles page

1. On `/admin/articles`: Record all visible column headers
2. Cross-reference with admin-settings default-visible columns

### 11.3 Change visibility and verify

1. On admin-settings → Articles → Table Columns policy card: Set a visible column's Default to "Hidden" → Save
2. On `/admin/articles`: Verify that column disappears from the table
3. **Restore** original setting

### 11.4 Lock a column visible

1. Set a column's Lock to "Visible" → Save
2. Navigate to `/admin/articles` → verify column appears
3. Verify user cannot hide it (column toggle in user settings should be locked)
4. **Restore** Lock to Inherit

---

## Test 12: Table Columns — Display Labels

**Setting**: Custom column label (overrides the default header text)
**Where visible**: Table column headers on the articles page

### 12.1 Record current labels

1. On admin-settings → Articles → Table Columns structural grid: Note any custom labels
2. On `/admin/articles`: Record actual column header text

### 12.2 Change a label and verify

1. On admin-settings → edit a column's display label to something distinctive (e.g., "TEST LABEL")
2. (Structural changes save immediately)
3. On `/admin/articles`: Verify the column header now shows "TEST LABEL"
4. **Restore** original label

---

## Test 13: Table Columns — Filterable / Sortable

**Setting**: `filterable?` and `sortable?` per column
**Where visible**: Filter icon / sort arrows on column headers

### 13.1 Verify current state

1. On admin-settings → Articles → Table Columns: Note which columns have filterable/sortable checked
2. On `/admin/articles`: Click column headers — do sort arrows appear? Are filter inputs available for marked columns?

### 13.2 Toggle filterable and verify

1. Uncheck `filterable` for a column that currently has it → (saves immediately)
2. On `/admin/articles`: Verify that column's filter control is gone
3. **Restore**

### 13.3 Toggle sortable and verify

1. Uncheck `sortable` for a column → (saves immediately)
2. On `/admin/articles`: Verify clicking that column header no longer sorts
3. **Restore**

---

## Test 14: List Behavior — Form Display Mode

**Setting**: Form display mode: `Inline` vs `Modal`
**Where visible**: How the Add/Edit form appears (replaces table inline or opens in a modal overlay)

### 14.1 Verify current mode

1. On admin-settings → Articles → List Behavior section: Record "Form display" value
2. On `/admin/articles`: Click Add or Edit — does the form appear inline (table disappears) or in a modal (overlay)?

### 14.2 Switch mode and verify

1. Change "Form display" from current to opposite (e.g., Modal → Inline)
2. Save settings
3. On `/admin/articles`: Click Add — verify form display mode changed
4. **Restore** original

---

## Test 15: List Behavior — Disallowed Action Mode

**Setting**: `hide` vs `disable` for actions the user doesn't have permission for
**Where visible**: How restricted actions appear (completely hidden vs grayed out)

### 15.1 Verify current mode

1. Record "Disallowed action mode" for Articles
2. This requires a scenario where an action is gated and the user doesn't have the permission

### 15.2 Change mode and verify (if testable)

1. Set an Action Gate (e.g., Add → some permission the current user lacks)
2. Set Disallowed action mode to "Hide" → Save → verify button is hidden on articles page
3. Change to "Disable" → Save → verify button is visible but disabled/grayed
4. **Restore** all to original

> **Note**: Testing this requires knowing the current user's permission level relative to a gate. May need to combine with Action Gates (Test 16).

---

## Test 16: List Behavior — Action Gates

**Setting**: Action gates for Add, Edit, Delete, Selection (permission gating)
**Where visible**: Whether the corresponding action is allowed/disallowed based on user role

### 16.1 Record current gates

1. On admin-settings → Articles → List Behavior: Record gate value for Add, Edit, Delete, Selection
2. Expected: "No gate" for all (from test results)

### 16.2 Set a gate and verify

1. Set "Add" gate to a restrictive permission (e.g., `expenses/power-user`)
2. Save settings
3. On `/admin/articles`: If current user doesn't have power-user role → Add button should be hidden (or disabled, depending on Disallowed action mode)
4. If current user does have the role → Add button should still be visible
5. **Restore** gate to "No gate"

> **Note**: Effect depends on the current user's roles. Record what happens regardless — this documents the gate enforcement behavior.

---

## Test 17: Lock Enforcement via User Settings

**Setting**: Any locked setting from Admin Settings
**Where visible**: User Settings page (`/admin/user-settings`) should show the setting as non-editable

### 17.1 Lock a display setting

1. On admin-settings → Articles: Set `Filtering` Lock to "Locked On" → Save
2. Navigate to `/admin/user-settings`
3. Find the Articles entity settings
4. Verify `Filtering` toggle is locked/grayed/non-clickable
5. Navigate to `/admin/articles` → verify filtering is ON regardless of user preference

### 17.2 Lock a column

1. On admin-settings → Articles → Table Columns policy: Lock a column to "Visible" → Save
2. On `/admin/user-settings`: Verify user cannot toggle that column off
3. On `/admin/articles`: Verify column is visible
4. **Restore** all locks to Inherit

---

## Post-Test: Verify Restoration

### R.1 Confirm Articles baseline restored

1. Navigate to `/admin/admin-settings`
2. Verify Articles card shows the same badge summary as baseline (e.g., "4 defaults, No locks")
3. Enter Edit Mode → select "articles" → verify all toggles match P.1 recorded values
4. Verify Form Fields and Table Columns match baseline

---

## Test Summary Table

| # | Test | Category | Status |
|---|------|----------|--------|
| P | Baseline Capture | Setup | |
| 1 | Show Edit Button | Display Toggle | |
| 2 | Show Delete Button | Display Toggle | |
| 3 | Show Selection | Display Toggle | |
| 4 | Show Filtering | Display Toggle | |
| 5 | Show Pagination | Display Toggle | |
| 6 | Show Highlights | Display Toggle | |
| 7 | Show Add Button | Display Toggle | |
| 8 | Batch Edit & Delete | Display Toggle | |
| 9 | Rows Per Page | Display Toggle | |
| 10 | Form Fields | Form Fields | |
| 11 | Column Visibility | Table Columns | |
| 12 | Column Labels | Table Columns | |
| 13 | Filterable / Sortable | Table Columns | |
| 14 | Form Display Mode | List Behavior | |
| 15 | Disallowed Action Mode | List Behavior | |
| 16 | Action Gates | List Behavior | |
| 17 | Lock Enforcement | Cross-cutting | |
| R | Baseline Restoration | Teardown | |
