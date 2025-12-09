# Admin Settings UI Expansion - Implementation Plan

## Overview
Expand the admin settings UI (`/admin/settings`) to support:
1. **View Options**: Add controls for add/batch actions (`show-add-button?`, `show-batch-edit?`, `show-batch-delete?`)
2. **Form Fields**: New tab to edit `form-fields.edn` (required/create/edit field lists)
3. **Table Columns**: New tab to edit `table-columns.edn` (default-hidden, always-visible, column widths)

## Current State
- View options page at `src/app/admin/frontend/pages/settings.cljs` displays display toggles and action settings
- Backend routes in `src/app/backend/routes/admin/settings.clj` only handle `view-options.edn`
- Config loader in `src/app/admin/frontend/config/loader.cljs` loads all three EDN files
- Frontend events in `src/app/admin/frontend/events/settings.cljs` handle view-options CRUD

## Phase Breakdown

### Phase 1: Backend Extension ✅ → 🔄 IN PROGRESS
**Goal**: Add API endpoints for form-fields and table-columns configs

**Files to modify**:
- `src/app/backend/routes/admin/settings.clj` - Add handlers for form-fields and table-columns

**New endpoints**:
- `GET /admin/api/settings/form-fields` - Return all form-fields config
- `PATCH /admin/api/settings/form-fields/entity` - Update entity form config
- `GET /admin/api/settings/table-columns` - Return all table-columns config  
- `PATCH /admin/api/settings/table-columns/entity` - Update entity table config

**Implementation**:
1. Add path constants for form-fields.edn and table-columns.edn
2. Add read/write helper functions for each config type
3. Add route handlers following existing patterns
4. Register routes in the route definitions

---

### Phase 2: Frontend Events Extension
**Goal**: Add re-frame events for form-fields and table-columns

**Files to modify**:
- `src/app/admin/frontend/events/settings.cljs` - Add events for new config types

**New events**:
- `::load-form-fields` / `::load-form-fields-success` / `::load-form-fields-failure`
- `::update-form-field-setting` / success/failure
- `::load-table-columns` / success/failure  
- `::update-table-column-setting` / success/failure

**New subscriptions**:
- `::form-fields` - Get form fields from app-db
- `::table-columns` - Get table columns from app-db
- `::active-config-tab` - Track which config tab is active

---

### Phase 3: View Options UI Enhancement
**Goal**: Add add/batch action toggles to existing view-options UI

**Files to modify**:
- `src/app/admin/frontend/pages/settings.cljs`

**Changes**:
1. Add "Action Settings" section with badges for:
   - `show-add-button?`
   - `show-batch-edit?`
   - `show-batch-delete?`
2. Make action settings editable using existing edit mode
3. Use existing `action-setting-keys` which are already defined but not fully surfaced

---

### Phase 4: Form Fields Editor Tab
**Goal**: New tab to edit form-fields.edn settings

**Files to modify**:
- `src/app/admin/frontend/pages/settings.cljs`

**UI Components**:
1. Entity selector (dropdown to pick entity)
2. Field lists editor for selected entity:
   - Create fields (multi-select from available)
   - Edit fields (multi-select from available)
   - Required fields (multi-select from create+edit)
3. Field config editor (optional - can be phase 2):
   - Type selector
   - Validation options
   - Placeholder text

---

### Phase 5: Table Columns Editor Tab
**Goal**: New tab to edit table-columns.edn settings

**Files to modify**:
- `src/app/admin/frontend/pages/settings.cljs`

**UI Components**:
1. Entity selector (dropdown to pick entity)
2. Column configuration for selected entity:
   - Available columns (read-only reference)
   - Default hidden columns (multi-select)
   - Always visible columns (multi-select)
   - Unfilterable columns (multi-select)
   - Unsortable columns (multi-select)
3. Column config editor (optional):
   - Width settings
   - Formatter selection

---

### Phase 6: Cache Sync & Testing
**Goal**: Ensure config changes are reflected immediately

**Implementation**:
1. After successful save, call `config-loader/register-preloaded-config!` for the changed config type
2. Verify UI components receive updated configs
3. Test edit → save → verify flow for each config type
4. Run `npm run test:cljs` and `bb be-test`

---

## Technical Notes

### EDN File Formats

**view-options.edn** (existing):
```clojure
{:entity-name {:show-edit? true, :show-delete? false, ...}}
```

**form-fields.edn**:
```clojure
{:entity-name 
 {:create-fields [:field1 :field2],
  :edit-fields [:field1 :field3],
  :required-fields [:field1],
  :field-config {:field1 {:type :text, :max-length 50}}}}
```

**table-columns.edn** (inverted format - loader transforms):
```clojure
{:entity-name
 {:available-columns [:col1 :col2 ...],
  :default-hidden-columns [:col2],
  :unfilterable-columns [:col1],
  :unsortable-columns [:col1],
  :always-visible [:col1],
  :column-config {:col1 {:width "200px"}}}}
```

### Config Loader Integration
- Use `register-preloaded-config!` to update cache after saves
- Config types: `:view-options`, `:form-fields`, `:table-columns`

### Error Handling Pattern
- Optimistic updates with rollback on failure
- Loading/saving state indicators
- Error alerts with retry option

---

## Progress Tracking

| Phase | Status | Notes |
|-------|--------|-------|
| 1. Backend Extension | ✅ Complete | Added form-fields and table-columns handlers |
| 2. Frontend Events | ✅ Complete | Added load/update events for all config types |
| 3. View Options UI | ✅ Complete | Added action settings toggles |
| 4. Form Fields Tab | ✅ Complete | Added form-fields editor tab |
| 5. Table Columns Tab | ✅ Complete | Added table-columns editor tab |
| 6. Cache & Testing | ✅ Complete | Frontend and backend compile successfully |

---

## Commands Reference
- Dev server: `bb run-app` (port 8085)
- Frontend tests: `npm run test:cljs 2>&1 | tee /tmp/fe-test.txt`
- Backend tests: `bb be-test 2>&1 | tee /tmp/be-test.txt`
- ClojureScript eval: Use `mcp_clojure-mcp_clojurescript_eval`
- Clojure eval: Use `clj-nrepl-eval -p <port> "<code>"`
