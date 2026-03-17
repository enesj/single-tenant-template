# Admin Settings Page — Test Results

**URL**: `http://localhost:8085/admin/admin-settings`
**Date**: 2026-03-17
**Tool**: Chrome DevTools MCP
**Tester**: AI Agent (GitHub Copilot)

---

## Test 1: Page Load & View Mode

### 1.1 Navigate and verify initial render

**Result: PASS**

- H1 heading: "Admin Settings" — present
- Subtitle: "Manage defaults and locks for admin pages" — present
- "Edit Settings" button — present (uid=4_61)
- Section header: "⚙️ Admin Settings" (h2) — present
- Sidebar shows "Admin Settings" link with active styling

### 1.2 Verify entity cards in view mode

**Result: PASS**

16 entities found across 4 domain groups. Each entity has a card with badge summary and individual setting rows.

**Domain groups:**

| Group | Entities |
| ----- | -------- |
| 💰 Expenses Management | Article aliases, Articles, Categories, Manufacturers, Store aliases, Stores, Subcategories, Supplier aliases, Suppliers, Unmapped aliases |
| 📋 Project Management | Backlog |
| 🔒 Security & Audit | Audit logs, Login events |
| 👥 User Management | Admins, Tenants, Users |

**Entity badge summary:**

| Entity | Defaults | Locks |
| ------ | -------- | ----- |
| Article aliases | 3 defaults | No locks |
| Articles | 4 defaults | No locks |
| Categories | 6 defaults | No locks |
| Manufacturers | 2 defaults | No locks |
| Store aliases | 4 defaults | 1 locks |
| Stores | 3 defaults | 1 locks |
| Subcategories | 6 defaults | No locks |
| Supplier aliases | 3 defaults | No locks |
| Suppliers | 2 defaults | 1 locks |
| Unmapped aliases | 3 defaults | No locks |
| Backlog | No defaults | No locks |
| Audit logs | 1 defaults | 4 locks |
| Login events | 1 defaults | 3 locks |
| Admins | 1 defaults | 3 locks |
| Tenants | 2 defaults | No locks |
| Users | 2 defaults | 1 locks |

**Example card detail (Article aliases):**
Setting rows present: Edit, Delete, Selection, Filtering, Pagination, Highlights, Add Button, Batch Edit, Batch Delete, Rows per page — each showing `Default state | Lock state` format.

- Edit: Default On | Inherit
- Delete: Inherit | Inherit
- Selection: Inherit | Inherit
- Filtering: Default On | Inherit
- Pagination: Inherit | Inherit
- Highlights: Inherit | Inherit
- Add Button: Default Off | Inherit
- Batch Edit: Inherit | Inherit
- Batch Delete: Inherit | Inherit
- Rows per page: Default: 10 | Lock: —

### 1.3 Verify List Behavior cards

**Result: PASS**

Each of the 16 entities has a "List Behavior" sub-section (16 total, matching entity count).

**Example (Article aliases) List Behavior:**
- Badge: "0 gates"
- Description: "These options replace page-level list props such as modal mode, disallowed action mode, and runtime capability gates."
- Form display: modal
- Disallowed action mode: —
- Add: No gate
- Edit: No gate
- Delete: No gate

---

## Test 2: Enter Edit Mode

### 2.1 Click "Edit Settings"

**Result: PASS**

- Clicked "Edit Settings" button (uid=4_61)
- Button changed to "Stop Editing" (confirmed in DOM)
- Entity selector dropdown appeared with label "Entity:" and value "Admins" (auto-selected first entity)
- "Edit Mode Active" alert (h4) shown with message: "Click settings to modify. Save to persist changes."
- Three config tabs appeared:
  - 📋 View Options (active/bold)
  - 📄 Form Fields (dimmed)
  - 📊 Table Columns (dimmed)
- Admins entity card shown with "1 defaults", "3 locks" badges
- "Clear local overrides (2)" button present (indicates browser has local overrides)
- "All toggles" bulk row present
- Individual toggle rows for: Edit, Delete, Selection, Filtering, Pagination, Highlights, Add Button, Batch Edit, Batch Delete, Rows per page
- List Behavior section with: Form display, Disallowed action mode, Add/Edit/Delete/Selection gates

### 2.2 Verify entity selector

**Result: PASS**

Entity dropdown contains 17 options (1 placeholder + 16 entities):

| # | Value | Text | Selected |
| - | ----- | ---- | -------- |
| 0 | (empty) | Select entity... | No |
| 1 | admins | Admins | Yes |
| 2 | article-aliases | Article aliases | No |
| 3 | articles | Articles | No |
| 4 | audit-logs | Audit logs | No |
| 5 | backlog | Backlog | No |
| 6 | categories | Categories | No |
| 7 | login-events | Login events | No |
| 8 | manufacturers | Manufacturers | No |
| 9 | store-aliases | Store aliases | No |
| 10 | stores | Stores | No |
| 11 | subcategories | Subcategories | No |
| 12 | supplier-aliases | Supplier aliases | No |
| 13 | suppliers | Suppliers | No |
| 14 | tenants | Tenants | No |
| 15 | unmapped-aliases | Unmapped aliases | No |
| 16 | users | Users | No |

All 16 admin-scope entities match the view mode entity cards.

---

## Test 3: View Options Tab (Edit Mode)

### 3.1 Verify tristate toggle controls

**Result: PASS**

Entity "Admins" View Options tab shows 9 toggle rows plus "Rows per page" row. Each toggle has a Default button and a Lock button with tristate cycling.

Initial state (Admins):

| Toggle | Default | Lock |
| ------ | ------- | ---- |
| Edit | Default Off | Inherit |
| Delete | Inherit | Inherit |
| Selection | Inherit | Locked On |
| Filtering | Inherit | Locked On |
| Pagination | Inherit | Locked On |
| Highlights | Inherit | Inherit |
| Add Button | Inherit | Inherit |
| Batch Edit | Inherit | Inherit |
| Batch Delete | Inherit | Inherit |

Each toggle button shows a `→ Next State` hint text beside it. Tristate cycle confirmed: Inherit → Default On → Default Off → Inherit (for defaults), Inherit → Locked On → Locked Off → Inherit (for locks).

### 3.2 Toggle a display setting

**Result: PASS**

- Clicked "Edit" Default button (uid=5_39, was "Default Off")
- State changed to "Inherit" (next in tristate cycle) — confirmed via DOM
- "Discard changes" and "Save settings" buttons appeared at top of page (dirty state indicator)
- Badge changed from "1 defaults" to "No defaults"

### 3.3 Bulk toggle

**Result: PASS**

- "All toggles" row present with Default button and Lock button
- Initial state showed: Default = "Inherit", Lock = "Mixed" (since some locks were set)
- Clicked bulk Default button (uid=5_33) to cycle from "Inherit" → "Default On"
- All 9 individual toggles changed to "Default On" simultaneously
- All lock toggles changed to "Inherit"
- Badge updated to "9 defaults, No locks"

### 3.4 Per-page setting

**Result: PASS**

- Rows per page has two dropdowns: "Default:" and "Lock:"
- Default dropdown options: — (inherit), 5, 10, 20, 25, 50, 100
- Lock dropdown options: — (inherit), 5, 10, 20, 25, 50, 100
- Changed Default from "—" to "25" — value updated in DOM
- Change triggers dirty state (Save/Discard buttons visible)

### 3.5 List config settings

**Result: PASS**

List Behavior section (below display toggles) contains:

| Setting | Type | Options | Initial Value |
| ------- | ---- | ------- | ------------- |
| Form display | Dropdown | —, Inline, Modal | — |
| Disallowed action mode | Dropdown | —, Hide, Disable | — |
| Add gate | Dropdown | No gate + 22 permission gates | No gate |
| Edit gate | Dropdown | No gate + 22 permission gates | No gate |
| Delete gate | Dropdown | No gate + 22 permission gates | No gate |
| Selection gate | Dropdown | No gate + 22 permission gates | No gate |

Available permission gates include: Expenses: can write, power user, access, expense write, expense delete, expense items manage, reference write, unmapped access, articles manage, manufacturers manage, categories manage, expense categories manage, cities manage, subcategories manage, stores manage, store aliases manage, supplier aliases manage, danger execute, settings write, upload, reports export, receipts approve.

### 3.6 Discard changes

**Result: PASS**

- After bulk toggling all to "Default On" and changing per-page to 25:
- Clicked "Discard changes" button (uid=6_0)
- All toggles reverted to original saved state (Edit: Default Off, 3 locks on Selection/Filtering/Pagination)
- Badges reverted to "1 defaults, 3 locks"
- Per-page selector reverted to "—"
- Save/Discard buttons disappeared

### 3.7 Make changes and Save

**Result: PASS**

- Toggled Delete default from "Inherit" → "Default On"
- Clicked "Save settings" button (uid=8_1)
- Save completed (no error shown)
- Save/Discard buttons disappeared (clean state confirmed)
- Delete retained new "Default On" state after save
- Badges updated to "2 defaults, 3 locks"
- Reverted Delete back to "Inherit" and saved again to restore original state

---

## Test 4: Form Fields Tab

### 4.1 Switch to Form Fields tab

**Result: PASS**

- Clicked "📄 Form Fields" tab link (uid=5_23)
- Tab became focused/active
- "Form Fields Configuration" heading appeared (h3)
- Two sections rendered:
  - "Create Form Fields" — "Fields shown when creating a new record"
  - "Edit Form Fields" — "Fields shown when editing an existing record"

Articles entity form fields (11 checkboxes each section):

| Field | Create | Edit |
| ----- | ------ | ---- |
| canonical_name | unchecked | checked |
| category_name | unchecked | unchecked |
| subcategory_id | unchecked | checked |
| subcategory_name | unchecked | unchecked |
| manufacturer_display_name | unchecked | unchecked |
| manufacturer_id | unchecked | checked |
| link | unchecked | checked |
| normalized_key | unchecked | checked |
| created_at | unchecked | unchecked |
| updated_at | unchecked | unchecked |
| id | unchecked | unchecked |

### 4.2 Toggle a form field

**Result: PASS**

- Clicked "canonical_name" Create checkbox (uid=13_3)
- Checkbox toggled from unchecked to checked
- No Save/Discard buttons appeared (changes saved immediately via PATCH)
- Toggled back to unchecked to restore original state

### 4.3 Verify immediate save

**Result: PASS**

- After toggling checkbox, no Save/Discard buttons present (confirmed via DOM query)
- Switching entity (Articles → Admins) shows different form fields (Admins: id, email, full-name...)
- Entity isolation confirmed: each entity has its own set of form field checkboxes
- Switching back to Articles preserves the form fields state

---

## Test 5: Table Columns Tab

### 5.1 Switch to Table Columns tab

**Result: PASS**

- Clicked "📊 Table Columns" tab link (uid=5_25)
- Two main sections rendered:
  1. **Columns Policy Card** — "Columns" heading with badges: "0 defaults", "0 locks", "1 enforced"
     - Description references `view-options.edn` and `table-columns.edn`
     - Shows always-visible columns as enforced
     - Bulk "All columns" toggle with Default/Lock visibility buttons
  2. **Table Columns Configuration** — grid with column headers:
     - Column name, Display Label (text input), In table, Always Visible, Default Visible, Filterable, Sortable
     - Each row has a "⋮⋮" drag handle for reordering
     - "Toggle All" row with header checkboxes

Articles entity columns (11):

| Column | In table | Always Visible | Default Visible | Filterable | Sortable |
| ------ | -------- | -------------- | --------------- | ---------- | -------- |
| canonical_name | yes | yes (enforced) | yes | yes | yes |
| category_name | yes | no | yes | yes | yes |
| subcategory_id | yes | no | no | no | no |
| subcategory_name | yes | no | yes | yes | yes |
| manufacturer_display_name | yes | no | yes | yes | yes |
| manufacturer_id | yes | no | no | no | no |
| link | yes | no | no | yes | yes |
| normalized_key | yes | no | no | yes | yes |
| created_at | yes | no | yes | yes | yes |
| updated_at | yes | no | no | yes | yes |
| id | yes | no | no | no | no |

Columns Policy Card has per-column Default/Lock visibility toggles:

| Column | Default | Lock |
| ------ | ------- | ---- |
| Canonical name | Always visible (enforced) | — |
| Category name | Inherit | Inherit |
| Subcategory id | Inherit | Inherit |
| Subcategory name | Inherit | Inherit |
| Manufacturer display name | Inherit | Inherit |
| Manufacturer id | Inherit | Inherit |
| Link | Inherit | Inherit |
| Normalized key | Inherit | Inherit |
| Created at | Inherit | Inherit |
| Updated at | Inherit | Inherit |
| Id | Inherit | Inherit |

### 5.2 Verify column table structure

**Result: PASS**

Each column row has:
- Column key name (e.g., "canonical_name")
- Display Label text input (editable, with placeholder matching formatted name)
- 5 checkboxes: In table, Always Visible, Default Visible, Filterable, Sortable
- Drag handle button "⋮⋮" with description "Drag to reorder"

### 5.3 Toggle a column checkbox

**Result: PASS**

- Clicked subcategory_id "Sortable" checkbox (uid=16_126, was unchecked)
- Checkbox toggled to checked (confirmed: states changed from [T,F,F,F,F] to [T,F,F,F,T])
- Toggled back to restore original state

### 5.4 Edit column label

**Result: PASS**

- Clicked canonical_name Display Label input (uid=16_105)
- Input focused, editable, not disabled/readonly
- Typed "Test Label" — value accepted
- Input has placeholder "Canonical name" (formatted from column key)
- Cleared input to restore original empty state

### 5.5 Columns policy card toggles

**Result: PASS**

- Clicked Category name Default visibility button (uid=16_24, was "Inherit")
- Toggled to "Default On" — tristate cycle works same as View Options
- Save/Discard buttons appeared (dirty state)
- Badge updated from "0 defaults" to "1 defaults"
- Clicked Discard to restore original state

---

## Test 6: Entity Switching

### 6.1 Switch between entities

**Result: PASS**

Switched entity via dropdown (using React onChange) from Articles → Stores → Categories → Admins.

Each entity loads distinct settings:

| Entity | Defaults | Locks | Notable Differences |
| --- | --- | --- | --- |
| Articles | 0 defaults | 0 locks | All toggles Inherit |
| Stores | 3 defaults | 1 lock | Edit=Default On, Delete=Default Off, Filtering=Default On, Add Button=Locked Off |
| Categories | 6 defaults | 0 locks | Edit/Delete/Filtering/Pagination/Highlights/Add Button=Default On |
| Admins | 1 default | 3 locks | Edit=Default Off, Selection/Filtering/Pagination=Locked On |

- Entity dropdown values are kebab-case (e.g., `article-aliases`, `store-aliases`)
- View Options, Form Fields, and Table Columns all reload per-entity
- "Clear local overrides" button appears when browser has local overrides (Stores had 2, Categories had 1)
- List Behavior section (Form display, Disallowed action mode, Action gates) also varies per entity
  - Stores: Form display=Modal, Disallowed action mode=Disable
  - Categories: Form display=Modal, Disallowed action mode=Disable * (similar)

### 6.2 Dirty state isolation

**Result: PASS (with observation)**

Test flow:
1. On Stores, toggled "Selection" Default from Inherit → Default On
2. "Discard changes" + "Save settings" buttons appeared at top
3. Switched entity to Categories via dropdown — **entity switch succeeded despite pending changes**
4. Save/Discard buttons **persisted** across entity switch (dirty state is global, not per-entity)
5. Clicked "Discard changes" while on Categories — dirty state cleared for all entities
6. Switched back to Stores — Selection was back to Inherit (original value), no Save/Discard buttons

**Key behavior**: Dirty state is tracked globally. Entity switching does NOT block when changes are pending and does NOT auto-save or auto-discard. Discard clears all pending changes across all entities.

---

## Test 7: Mode Transitions

### 7.1 Edit → View transition

**Result: PASS (with important finding)**

Test flow:
1. Entered Edit Mode, selected Backlog entity (originally "No defaults, No locks", all Inherit)
2. Toggled Edit default from Inherit → Default On (dirty state, Save/Discard appeared)
3. Clicked "Stop Editing" **without saving**
4. Page returned to View Mode — all entity overview cards displayed
5. Checked Backlog card: showed "1 defaults, No locks" — **change was auto-saved**

**Key finding**: Clicking "Stop Editing" with unsaved changes **auto-saves** them. There is no confirmation dialog or warning. The "Discard changes" button is the only way to revert pending changes before exiting edit mode.

_Restored Backlog to original state (Inherit) after test._

### 7.2 View → Edit → View roundtrip

**Result: PASS**

- View Mode → Click "Edit Settings" → Edit Mode (entity dropdown + 3 tabs + alert appear)
- Edit Mode → Click "Stop Editing" → View Mode (overview cards reappear)
- No stale state observed between transitions
- Entity dropdown disappears in View Mode, reappears in Edit Mode
- Mode state is consistent: button text toggles between "Edit Settings" / "Stop Editing"

---

## Test 8: Error Handling

### 8.1 Network error simulation

**Result: SKIPPED**

Network error simulation requires intercepting HTTP requests (e.g., blocking `/admin/api/settings` endpoint). Chrome DevTools MCP does not provide a request interception/blocking tool. This test would need:
- Chrome DevTools Protocol `Fetch.enable` / `Fetch.failRequest` (not exposed via MCP)
- Or manually stopping the backend server during a save operation

Observed resilience indicators:
- Save/Discard buttons disable during save operations (prevents double-submit)
- Page reloads cleanly after navigation away and back
- No JavaScript console errors observed during normal Save/Discard/entity-switch flows

---

## Test 9: Page Reload Persistence

### 9.1 Verify settings persist across reload

**Result: PASS**

Test flow:
1. In Edit Mode, selected Tenants entity (originally "2 defaults, No locks")
2. Toggled Filtering Default from Inherit → Default On
3. Saved ("Save settings" button) — badges updated to "3 defaults, No locks"
4. Reloaded page via `navigate_page` to http://localhost:8085/admin/admin-settings
5. Page loaded in View Mode — Tenants card showed "3 defaults, No locks" ✓
6. Re-entered Edit Mode, selected Tenants — Filtering Default confirmed as "Default On" ✓

Settings are persisted to the backend database via `PUT /admin/api/settings` and survive full page reloads.

_Restored Tenants Filtering Default back to Inherit after test._

---

## Test 10: Sidebar & Navigation

### 10.1 Sidebar active state

**Result: PASS**

- "Admin Settings" sidebar link has `ds-active` CSS class when on the Admin Settings page
- "User Settings" sidebar link does NOT have `ds-active` class
- Navigating to User Settings: `ds-active` moves to "User Settings" link
- Both links are in the bottom section of the sidebar (below domain links)
- Active state is URL-driven and updates correctly on page navigation

### 10.2 Settings gear

**Result: PASS**

- Settings gear button (⚙️ icon, `title="Settings"`) is in the top-right header area
- Clicking opens a small popover with:
  - **Theme**: Light/Dark toggle
  - **Reload**: Button with refresh icon
- This is a global app settings popover, not specifically related to Admin Settings
- Closes on body click / clicking away

### 10.3 Additional navigation tests

**Toggle sidebar**: aria-label="Toggle sidebar" button (hamburger icon) collapses/expands the sidebar. When collapsed, content area expands to full width.

**Admin Settings ↔ User Settings**: Navigating from Admin Settings to User Settings via sidebar link works; page title changes to "User Settings", heading to "Manage domain-owned defaults and locks for user-facing pages", entity groups change to "User Settings" + "Other".

---

## Summary

| Test | Status | Notes |
| ---- | ------ | ----- |
| 1. Page Load & View Mode | PASS | All 16 entities, 4 domain groups, badges, settings rows render correctly |
| 2. Enter Edit Mode | PASS | Button, dropdown, tabs, alert all work as expected |
| 3. View Options Tab | PASS | Tristate toggles, bulk toggle, per-page, Save/Discard all functional |
| 4. Form Fields Tab | PASS | Checkbox grid, toggle, entity switching, immediate PATCH (no Save/Discard) |
| 5. Table Columns Tab | PASS | Column grid, checkboxes, label editing, policy card, drag handles all work |
| 6. Entity Switching | PASS | 16 entities load distinct configs; dirty state is global not per-entity |
| 7. Mode Transitions | PASS* | *Stop Editing auto-saves unsaved changes (no confirmation dialog) |
| 8. Error Handling | SKIPPED | Cannot simulate network errors via Chrome DevTools MCP |
| 9. Persistence | PASS | Settings saved via Save button persist across full page reload |
| 10. Sidebar & Navigation | PASS | Active state, gear icon, sidebar toggle, settings page navigation all work |

**Overall: 9 PASS, 1 SKIPPED**

### Key Findings

1. **Stop Editing auto-saves**: Clicking "Stop Editing" with pending View Options changes auto-saves them without warning. The only way to revert is to click "Discard changes" first.
2. **Dirty state is global**: Pending View Options changes persist across entity switches. Discard clears all entities at once.
3. **Save behavior varies by tab**:
   - View Options: Draft model with Save/Discard buttons
   - Form Fields: Immediate PATCH on toggle (no Save/Discard)
   - Table Columns structural changes: Immediate PATCH
   - Table Columns policy (Default/Lock visibility): Draft model with Save/Discard
4. **Local overrides**: Some entities show "Clear local overrides (N)" indicating browser-local state that can override admin defaults.
5. **Tristate cycle**: Defaults cycle Inherit → Default On → Default Off → Inherit; Locks cycle Inherit → Locked On → Locked Off → Inherit.
