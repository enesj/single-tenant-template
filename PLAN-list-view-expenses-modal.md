# Plan: List View Custom Forms + Modal Support for Admin Expenses

**Date:** 2025-12-12  
**Status:** In Progress

## Overview

Implement reusable support in `list-view` for custom add/edit forms with optional modal display. Apply this to Admin Expenses to show Add and Edit as modal windows instead of inline forms.

## Current Architecture

### List View Components
- `src/app/template/frontend/components/list.cljs` - Main `list-view` component
  - Controls `show-add-form?` state (global `[:ui :show-add-form]`)
  - Controls `editing` state (global `[:ui :editing]`)
  - When `show-add-form?` is true, hides table and shows `add-item-section`
  - When `editing` matches an item ID, renders inline edit form via `render-row`

- `src/app/template/frontend/components/list/ui.cljs` - Contains:
  - `add-item-section` - Generic add form using template `form` component
  - `header-section` - Title + Add button that toggles `show-add-form?`

- `src/app/template/frontend/components/list/rows.cljs` - Contains:
  - `render-row` - Decides inline edit form vs normal row based on `editing` state

- `src/app/template/frontend/components/list/cells.cljs` - Contains:
  - `edit-button` - Dispatches `::config-events/set-editing` (inline edit trigger)
  - `action-buttons` - Renders edit/delete buttons with show flags

### Admin Entity System
- `src/app/admin/frontend/renderers/content.cljs` - Creates main content for config-driven pages
  - Calls `list-view` with `:render-actions` override
  - Passes `display-settings` from entity config

- `src/app/admin/frontend/config/entities.edn` - Entity configurations
  - `:expenses` has `:show-edit? false`, `:show-add-button? true`

### Expenses Domain
- `src/app/domain/frontend/expenses/pages/admin/expense_form.cljs` - Manual expense entry page
  - Contains `admin-expense-form-page` with line-items, supplier/payer dropdowns
  - Dispatches `::expenses-events/create-entry` on save
  - Navigates to detail page on success

- `src/app/domain/frontend/expenses/events/events_factory.cljs` - Event generators
  - `create-entry-success` navigates to `/admin/expenses/:id`
  - No update-entry event currently exists

### Modal Components
- `src/app/template/frontend/components/modal_wrapper.cljs` - Reusable modal wrapper
  - Props: `visible?`, `title`, `size`, `on-close`, `children`

## Phases

### Phase 1: Add Modal State Management to List View ✅ COMPLETE
**Goal:** Add component-local state for modal open/close without breaking existing behavior.

**Changes:**
1. Add local state to `list-view`:
   - `add-modal-open?` - boolean
   - `edit-modal-open?` - boolean  
   - `edit-modal-item-id` - ID of item being edited in modal

2. Add props to `list-view`:
   - `:render-add-form` - optional fn `(fn [props] ...)` to render custom add form
   - `:render-edit-form` - optional fn `(fn [item props] ...)` to render custom edit form
   - `:form-display` - `:inline` (default) or `:modal`
   - `:on-add-success` - optional callback after successful add (modal close + refresh)
   - `:on-edit-success` - optional callback after successful edit

**Files:**
- `src/app/template/frontend/components/list.cljs`

### Phase 2: Implement Modal Rendering in List View ✅ COMPLETE
**Goal:** Render custom forms in modal when `:form-display :modal` is set.

**Changes:**
1. Import `modal-wrapper` component
2. When `add-modal-open?` and `:render-add-form` provided:
   - Show modal with custom add form
   - Keep table visible (don't hide like inline mode)
3. When `edit-modal-open?` and `:render-edit-form` provided:
   - Show modal with custom edit form prefilled with item data
4. Update `header-section` to support custom add button click handler

**Files:**
- `src/app/template/frontend/components/list.cljs`
- `src/app/template/frontend/components/list/ui.cljs`

### Phase 3: Extract Reusable Expense Form UI ✅ COMPLETE
**Goal:** Create a modal-friendly version of the expense form.

**Changes:**
1. Extract form body from `admin-expense-form-page` into `expense-form-body` component
   - Remove breadcrumbs/back button (modal variant)
   - Accept callbacks: `:on-save`, `:on-cancel`
   - Accept optional initial data for edit mode
2. Create `expense-add-modal-form` - wrapper that uses `expense-form-body`
3. Create `expense-edit-modal-form` - wrapper that loads item and uses `expense-form-body`

**Files:**
- `src/app/domain/frontend/expenses/components/expense_form.cljs` (new)
- `src/app/domain/frontend/expenses/pages/admin/expense_form.cljs` (refactor to use shared component)

### Phase 4: Add Update Event for Expenses ✅ COMPLETE
**Goal:** Support editing existing expenses.

**Changes:**
1. Added modal-specific events: `::create-entry-modal`, `::update-entry-modal`
2. Events call callbacks on success to close modal
3. Implemented callback dispatch via `::call-modal-callback` event

**Files:**
- `src/app/domain/frontend/expenses/events/expenses.cljs`
- `src/app/template/frontend/events/config.cljs` (added noop event)

### Phase 5: Wire Expenses List to Use Modal Forms ✅ COMPLETE
**Goal:** Connect the new modal forms to the expenses list page.

**Changes:**
1. Updated `expense_list.cljs` to use list-view directly instead of entity renderer
2. Passed `:render-add-form` fn that renders `expense-add-form-modal`
3. Passed `:render-edit-form` fn that renders `expense-edit-form-modal`
4. Passed `:form-display :modal`
5. Edit button in rows now passes `on-edit-click` to trigger modal edit

**Files:**
- `src/app/domain/frontend/expenses/pages/admin/expense_list.cljs` (rewritten)
- `src/app/template/frontend/components/list/cells.cljs` (edit-button, action-buttons, reactive-action-cell support on-edit-click)
- `src/app/template/frontend/components/list/rows.cljs` (passes on-edit-click)

### Phase 6: End-to-End Testing & Verification ⬜ READY FOR TESTING
**Goal:** Verify all functionality works correctly.

**Verification Steps:**
1. `/admin/expenses` - Add button opens modal with expense form
2. Modal form has all fields: supplier, payer, date, amount, currency, notes, line items
3. Save creates expense and:
   - Closes modal
   - Refreshes list
   - Shows new item
   - Does NOT navigate to detail page
4. Edit button opens modal with prefilled data
5. Save updates expense and closes modal + refreshes list
6. Cancel closes modal without saving
7. Other entity list views still work (no regressions)

**Test Commands:**
```bash
npm run test:cljs 2>&1 | tee /tmp/fe-test.txt
bb be-test 2>&1 | tee /tmp/be-test.txt
```

## Progress Notes

### Phase 1 Notes (2025-12-12)
- Added modal state: `add-modal-open?`, `edit-modal-open?`, `edit-modal-item`
- Added props: `:render-add-form`, `:render-edit-form`, `:form-display`, `:on-add-success`, `:on-edit-success`
- Created modal wrapper callbacks that close modal and optionally call success handlers
- Updated header-section to receive custom add click handler

### Phase 2 Notes (2025-12-12)
- Integrated modal-wrapper component
- Modal renders when `add-modal-open?` or `edit-modal-open?` with custom renderers
- Table stays visible when modal is open (unlike inline mode)
- Edit button in action cells can now trigger modal edit via callback prop

### Phase 3 Notes (2025-12-12)
- Created `expense_form.cljs` with `expense-form-body` shared component
- Created `expense-add-form-modal` for modal add
- Created `expense-edit-form-modal` for modal edit (placeholder, needs update event)
- Refactored `expense_form.cljs` page to use shared component

### Phase 4 Notes (2025-12-12)
- Added modal-specific events: `::create-entry-modal`, `::update-entry-modal`
- Added success events with callback dispatch: `::create-entry-modal-success`, `::update-entry-modal-success`
- Added `::call-modal-callback` event to invoke function callbacks from re-frame
- Added `::noop` event in config.cljs for modal close handlers

### Phase 5 Notes (2025-12-12)
- Completely rewrote `expense_list.cljs` to use list-view with modal support
- Created custom `expenses-entity-spec` with proper field specs
- Created `expenses-display-settings` to show edit/delete buttons
- Created `render-add-form` and `render-edit-form` functions
- Added `on-edit-click` prop support to edit-button, action-buttons, reactive-action-cell, render-row
- Build compiles successfully with 0 warnings
- Fixed response key handling in modal events (`:expense` singular not `:expenses`)

## Technical Decisions

1. **Component-local state for modals** - Keeps modal state scoped to the list view instance, avoiding conflicts between entities.

2. **Optional props preserve defaults** - When `:render-add-form` / `:render-edit-form` not provided, existing inline behavior continues.

3. **Form display mode as prop** - `:form-display :modal` vs `:inline` allows gradual migration and flexibility per entity.

4. **Success callbacks** - `:on-add-success` / `:on-edit-success` allow parent to control post-save behavior (close modal, refresh, etc.)

5. **Separated expense form body** - Makes form reusable in both page and modal contexts.
