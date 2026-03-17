# Admin Settings Page — Chrome DevTools Test Plan

**Target**: `http://localhost:8085/admin/admin-settings`  
**Tool**: `chrome-devtools` MCP  
**Prerequisite**: Logged in as admin (owner role)

---

## 1. Page Load & View Mode (Overview)

### 1.1 Navigate and verify initial render
- `navigate_page` to `http://localhost:8085/admin/admin-settings`
- `take_screenshot` — confirm page loaded
- `take_snapshot` — verify:
  - Heading "Admin Settings" is present
  - Description "Manage defaults and locks for admin pages" visible
  - "Edit Settings" button visible
  - Section header "⚙️ Admin Settings" visible

### 1.2 Verify entity cards in view mode
- `take_snapshot` and check entity groups are rendered:
  - **Expenses Management** group (💰) with entities:
    - Article aliases, Articles, Manufacturers, Categories, Subcategories, Suppliers, Stores, Countries, Cities, Unmapped aliases, Supplier aliases, Article aliases, Store aliases, Expenses, Expense items, Payers, Receipts, Reports
  - **User Management** group (👥): Users, Admins, Tenants
  - **Security & Audit** group (🔒): Audit Logs, Login Events
  - **Project Management** group (📋): Backlog
- For each entity card, verify:
  - Entity name heading
  - Badge text (e.g., "3 defaults", "No locks")
  - Settings rows: Edit, Delete, Selection, Filtering, Pagination, Highlights, Add Button, Batch Edit, Batch Delete, Rows per page
  - Each row shows Default/Lock state (e.g., "Default On | Inherit")

### 1.3 Verify List Behavior cards
- Each entity should have a "List Behavior" sub-card showing:
  - "Form display" row (value: inline or modal)
  - "Disallowed action mode" row (value: hide/disable or —)
  - Action gates: Add, Edit, Delete, Selection (each "No gate" or a specific gate)
  - Badge showing gate count (e.g., "0 gates")

---

## 2. Enter Edit Mode

### 2.1 Click "Edit Settings"
- `click` on the "Edit Settings" button (uid from snapshot)
- `take_screenshot` — confirm edit mode activated
- Verify:
  - Button changes to "Stop Editing"
  - Entity selector dropdown appears
  - First entity is auto-selected
  - Three config tabs appear: "View Options", "Form Fields", "Table Columns"

### 2.2 Verify entity selector
- `take_snapshot` — find the entity dropdown/select
- `click` to open entity selector
- `take_screenshot` — verify list of entities matches available admin-scope entities
- Select a different entity (e.g., click "Articles")
- `take_screenshot` — verify editor panel updates to show Articles settings

---

## 3. View Options Tab (Edit Mode)

### 3.1 Verify tristate toggle controls
- With an entity selected (e.g., "Article aliases"):
  - `take_snapshot` — confirm tristate toggle rows for:
    - Edit, Delete, Selection, Filtering, Pagination, Highlights, Add Button, Batch Edit, Batch Delete
  - Each toggle should show three states: Inherit / Default On / Default Off (or Lock On / Lock Off)
  - Identify toggle element uids from snapshot

### 3.2 Toggle a display setting
- `click` on a toggle to change state (e.g., change "Edit" from "Default On" to "Lock On")
- `take_screenshot` — verify:
  - Toggle visually changed
  - Save/Discard buttons appear (dirty state indicator)

### 3.3 Bulk toggle
- Find the "All toggles" bulk row
- `click` on the bulk toggle (e.g., "All Off")
- `take_screenshot` — verify all settings changed to off state

### 3.4 Per-page setting
- Find "Rows per page" selector
- Change value (e.g., from 10 to 25)
- `take_screenshot` — verify selector updated and dirty state shown

### 3.5 List config settings
- Find "Form display" dropdown → change from modal to inline (or vice versa)
- Find "Disallowed action mode" dropdown → change value
- Find action gate dropdowns (Add, Edit, Delete, Selection) → set a gate value
- `take_screenshot` — verify changes reflected

### 3.6 Discard changes
- `click` "Discard" button
- `take_screenshot` — verify:
  - All settings reverted to saved state
  - Save/Discard buttons disappear

### 3.7 Make changes and Save
- Toggle a setting (e.g., lock "Delete" OFF for Articles)
- `click` "Save" button
- `take_screenshot` — verify:
  - Save completes (no error)
  - Save/Discard buttons disappear
  - Setting persisted (switch to view mode and confirm)

---

## 4. Form Fields Tab (Edit Mode)

### 4.1 Switch to Form Fields tab
- `click` on "Form Fields" tab (text: "📄 Form Fields" or uid for the tab)
- `take_screenshot` — verify:
  - Form fields editor appears
  - Sections: "Create Form Fields" and "Edit Form Fields"
  - Each shows a grid of checkbox toggles for entity columns

### 4.2 Toggle a form field
- `click` on a checkbox to enable/disable a field (e.g., toggle "name" in create form)
- `take_screenshot` — verify checkbox state changed
- Note: Admin form-fields save immediately via PATCH (no draft/discard)

### 4.3 Verify immediate save
- After toggling, the change should persist immediately
- Switch to another entity and back → verify toggle state preserved
- `take_screenshot` for verification

---

## 5. Table Columns Tab (Edit Mode)

### 5.1 Switch to Table Columns tab
- `click` on "Table Columns" tab (text: "📊 Table Columns")
- `take_screenshot` — verify:
  - Columns policy card appears (defaults/locks badges)
  - Table columns editor appears with a table of columns
  - Column rows with IDs: `table-columns-row-{entity}-{col}`

### 5.2 Verify column table structure
- `take_snapshot` — verify each column row has:
  - Column name + Display Label input (ID: `col-label-{entity}-{col}`)
  - Checkboxes: In table, Always Visible, Default Visible, Filterable, Sortable
  - Drag handle for reordering

### 5.3 Toggle a column checkbox
- Find a checkbox (e.g., "Filterable" for a specific column)
- `click` the checkbox
- `take_screenshot` — verify state changed
- Note: Admin table-columns save immediately via PATCH

### 5.4 Edit column label
- Find a label input (ID pattern: `col-label-{entity}-{col}`)
- `click` on the input
- `type_text` to enter a custom label (e.g., "Custom Name")
- `click` outside to blur/commit
- `take_screenshot` — verify label updated

### 5.5 Columns policy card toggles
- Find columns policy card with column visibility toggles
- Toggle a column's visibility default/lock
- `take_screenshot` — verify badge counts update

---

## 6. Entity Switching in Edit Mode

### 6.1 Switch between entities
- Select entity "Users" from entity dropdown
- `take_screenshot` — verify editor shows Users settings
- Select entity "Audit Logs"
- `take_screenshot` — verify editor shows Audit Logs settings
- Verify each entity shows its own saved config (not bleeding state from previous entity)

### 6.2 Dirty state isolation
- On entity A, toggle a setting (creates dirty state)
- Switch to entity B → note if dirty indicator stays (it should, dirty is scope-level)
- Switch back to entity A → verify toggle change persisted in draft
- Discard → all entities should revert

---

## 7. Mode Transitions

### 7.1 Edit → View transition
- In edit mode with unsaved changes, click "Stop Editing"
- `take_screenshot` — verify returns to view mode overview
- Note: unsaved draft should still be retained if re-entering edit mode

### 7.2 View → Edit → View roundtrip
- Start in view mode → click "Edit Settings" → make a change → click "Save"
- Click "Stop Editing" → verify view mode shows updated values
- `take_screenshot` for confirmation

---

## 8. Error Handling

### 8.1 Network error simulation
- (Optional): Use `evaluate_script` to temporarily intercept fetch calls
- Attempt to save → verify error alert appears
- Verify error message displayed in the error alert area
- Verify dirty state is preserved (changes not lost)

---

## 9. Page Reload Persistence

### 9.1 Verify settings persist across reload
- Make and save a change (e.g., lock "Filtering" ON for Articles)
- `navigate_page` to reload page (`http://localhost:8085/admin/admin-settings`)
- `take_screenshot` — verify the saved change is visible in view mode overview
- Enter edit mode → select Articles → verify toggle state reflects saved value

---

## 10. Sidebar & Navigation

### 10.1 Sidebar active state
- `take_snapshot` — verify "Admin Settings" link in sidebar has active styling
- `click` "User Settings" link → verify navigates to `/admin/user-settings`
- `click` "Admin Settings" link → verify navigates back

### 10.2 Settings gear (header)
- `click` on gear icon (ID: `admin-settings-gear`)
- `take_screenshot` — verify dropdown shows Theme toggle and Reload button
- `click` Reload button (ID: `btn-admin-reload-everything`) → verify page reloads config
- `click` gear icon again to close dropdown

---

## Quick Reference: Key Element IDs

| Element | ID Pattern | Example |
|---------|-----------|---------|
| Admin sidebar toggle | `admin-sidebar-toggle` | — |
| Settings gear | `admin-settings-gear` | — |
| Reload button | `btn-admin-reload-everything` | — |
| Sidebar logout | `admin-sidebar-logout` | — |
| Table column row | `table-columns-row-{entity}-{col}` | `table-columns-row-articles-name` |
| Column label input | `col-label-{entity}-{col}` | `col-label-articles-name` |
| Column drag handle | `col-order-drag-{entity}-{col}` | `col-order-drag-articles-name` |
| Clear local prefs | `btn-clear-local-display-prefs-{entity}` | `btn-clear-local-display-prefs-articles` |

## Key API Endpoints (for network verification)

| Action | Method | Endpoint |
|--------|--------|----------|
| Load view options | GET | `/admin/api/settings` |
| Save view options | PUT | `/admin/api/settings` |
| Patch entity setting | PATCH | `/admin/api/settings/entity` |
| Load form fields | GET | `/admin/api/settings/form-fields` |
| Patch form field | PATCH | `/admin/api/settings/form-fields/entity` |
| Load table columns | GET | `/admin/api/settings/table-columns` |
| Patch table column | PATCH | `/admin/api/settings/table-columns/entity` |
