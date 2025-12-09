# List View Display Settings Refactoring Plan

## Overview

This plan outlines the refactoring of the list-view display settings system to make configuration simpler and clearer while preserving all existing functionality.

## Current State Analysis

### Data Sources (5 different sources!)
1. **`view-options.edn`** - Hardcoded per-entity display settings (show-edit?, show-delete?, pagination, filtering, etc.)
2. **`table-columns.edn`** - Column visibility, ordering, filtering, sorting configuration
3. **Config loader cache** (`config-cache` atom) - Runtime cache outside app-db
4. **Re-frame app-db paths**:
   - `[:admin :config :view-options]` - Loaded on init
   - `[:admin :settings :view-options]` - Updated via Settings page API
   - `[:ui :entity-configs <entity> ...]` - User preferences (toggles)
   - `[:ui :defaults]` - Default values
5. **Entity registry** (`entities.edn`) - Entity metadata, actions, adapters

### Current Flow
```
view-options.edn → preload.cljs → config-cache atom → subs/ui.cljs merge
                                                    ↓
                                            app-db [:admin :config :view-options]
                                                    ↓
Settings page → API → app-db [:admin :settings :view-options] → config-cache sync
                                                    ↓
User toggles → app-db [:ui :entity-configs <entity>]
                                                    ↓
                                    subs/ui.cljs ::entity-display-settings
                                                    ↓
                                    hooks/display_settings.cljs
                                                    ↓
                                    list-view + settings panel
```

### Problems Identified
1. **5 different data sources** with complex merge precedence
2. **Dual storage**: config-cache atom AND app-db both store view-options
3. **Inconsistent data formats**: vectors (table-columns) vs boolean maps (user prefs)
4. **Complex precedence**: hardcoded settings vs user preferences spread across multiple subscriptions
5. **Config files in `resources/public/`** - served statically, requires API for updates

---

## Target Architecture

### Simplified Data Model

**Single source of truth for each concern:**

| Concern | Source | Format | Storage |
|---------|--------|--------|---------|
| Column layout | `table-columns.edn` | Vectors | config-cache → app-db |
| Display settings (hardcoded) | `view-options.edn` | Maps | config-cache → app-db |
| User preferences | app-db only | Maps | `[:ui :entity-prefs <entity>]` |
| Entity metadata | `entities.edn` | Maps | entity-registry |

### Merge Strategy (3 levels, clear precedence)
```
1. Hardcoded (view-options.edn) - Locks settings, hides controls
2. User preferences (app-db) - Runtime toggles
3. Defaults (code constants) - Fallback values
```

### Key Decisions

1. **Keep config files in `resources/public/admin/ui-config/`**
   - They work well with shadow-cljs inline resources
   - Settings page can update via API (already implemented)
   - Simpler than moving to src/ and requiring build changes

2. **Eliminate config-cache duplication**
   - Preload directly into app-db on init
   - Config-cache becomes read-only bootstrap cache
   - All runtime reads from app-db subscriptions

3. **Unify user preferences path**
   - All user toggles under `[:ui :entity-prefs <entity> :display]`
   - Visible columns under `[:ui :entity-prefs <entity> :columns]`
   - Filter settings under `[:ui :entity-prefs <entity> :filters]`

4. **Simplify subscription layer**
   - Single `::display-settings` sub with clear merge logic
   - Remove redundant subs that query config-cache directly

---

## Implementation Phases

### Phase 1: Consolidate User Preferences Path ✅ → ☐
**Goal**: Unify scattered user preference paths into single structure

**Changes**:
- [ ] Create new path structure: `[:ui :entity-prefs <entity>]`
- [ ] Migrate `[:ui :entity-configs <entity> :show-*]` → `[:ui :entity-prefs <entity> :display :show-*]`
- [ ] Migrate `[:ui :entity-configs <entity> :visible-columns]` → `[:ui :entity-prefs <entity> :columns :visible]`
- [ ] Migrate `[:ui :entity-configs <entity> :filterable-fields]` → `[:ui :entity-prefs <entity> :filters :fields]`
- [ ] Update all toggle events to use new paths

**Files**:
- `src/app/template/frontend/events/list/ui_state.cljs` - toggle events
- `src/app/template/frontend/events/list/settings.cljs` - column/filter events
- `src/app/template/frontend/subs/ui.cljs` - subscriptions

### Phase 2: Simplify Display Settings Subscription ☐
**Goal**: Single authoritative subscription with clear merge logic

**Changes**:
- [ ] Refactor `::entity-display-settings` to use new paths
- [ ] Remove `get-display-setting` helper (inline logic)
- [ ] Remove `get-entity-setting` helper (inline logic)
- [ ] Add explicit default values map in subscription
- [ ] Update docstring with clear precedence rules

**Files**:
- `src/app/template/frontend/subs/ui.cljs`

### Phase 3: Remove Config-Cache Runtime Reads ☐
**Goal**: Config-cache becomes bootstrap-only, app-db is runtime source

**Changes**:
- [ ] Update `::hardcoded-view-options` sub to read from app-db only
- [ ] Update `::filterable-fields` sub to read from app-db
- [ ] Update `config/loader.cljs` - mark cache as bootstrap-only
- [ ] Ensure preload writes to both cache AND app-db

**Files**:
- `src/app/template/frontend/subs/ui.cljs`
- `src/app/admin/frontend/config/loader.cljs`
- `src/app/admin/frontend/config/preload.cljs`

### Phase 4: Simplify Settings Panel ☐
**Goal**: Cleaner UI code, remove mode switching logic

**Changes**:
- [ ] Remove `vector-mode?` checks (always use unified approach)
- [ ] Simplify column visibility toggle logic
- [ ] Update filter toggle logic for new paths
- [ ] Remove redundant subscriptions in component

**Files**:
- `src/app/template/frontend/components/settings/list_view_settings.cljs`
- `src/app/template/frontend/utils/column_config.cljs`

### Phase 5: Update List View Integration ☐
**Goal**: List view uses simplified settings flow

**Changes**:
- [ ] Remove redundant subscriptions in list.cljs
- [ ] Use `use-display-settings` hook consistently
- [ ] Pass effective settings to child components
- [ ] Verify all entity pages work correctly

**Files**:
- `src/app/template/frontend/components/list.cljs`
- Admin pages (users, audit, login-events, etc.)

### Phase 6: Testing & Cleanup ☐
**Goal**: Verify everything works, remove dead code

**Changes**:
- [ ] Run CLJS tests: `npm run test:cljs`
- [ ] Manual smoke test: `/admin/settings`, `/admin/users`, `/admin/audit`
- [ ] Remove unused helpers/functions
- [ ] Update documentation

---

## Implementation Progress

| Phase | Status | Notes |
|-------|--------|-------|
| 1. Consolidate Paths | ✅ Complete | Events read from both paths, write to new path. Tests updated. |
| 2. Simplify Subs | ✅ Complete | Added default-display-settings, refactored ::entity-display-settings |
| 3. Remove Cache Reads | ✅ Complete | subs/ui.cljs no longer imports config-loader. Tests updated. |
| 4. Settings Panel | ✅ Complete | Removed vector-mode? branching, simplified toggle events |
| 5. List View | ✅ Complete | Removed redundant alias variable |
| 6. Testing | ✅ Complete | Removed unused helper functions, all tests pass |

---

## Detailed Changes by Phase

### Phase 1: Consolidate User Preferences Path

**Files Modified:**
- `src/app/template/frontend/events/list/settings.cljs`
  - Updated `::visible-columns` subscription to read from new path first: `[:ui :entity-prefs entity :columns :visible]`
  - Updated `::toggle-column-visibility` event to write to new path
  - Updated `::filterable-fields` subscription to read from `[:ui :entity-prefs entity :filters :fields]`
  - Updated `::toggle-field-filtering` event to write to new path
  - Added backward compatibility: reads from legacy `[:ui :entity-configs entity]` if new path is empty

- `src/app/template/frontend/events/list/ui_state.cljs`
  - Updated `toggle-entity-flag` helper to write to `[:ui :entity-prefs entity :display setting]`
  - Maintains read from both new and legacy paths for compatibility

- `test/app/template/frontend/settings_test.cljs`
  - Updated assertions to check new path structure `[:ui :entity-prefs :items :display :show-edit?]`

**Path Migration Summary:**
```clojure
;; OLD (Legacy - Deprecated)
[:ui :entity-configs <entity> :show-edit?]
[:ui :entity-configs <entity> :visible-columns]
[:ui :entity-configs <entity> :filterable-fields]

;; NEW (Current)
[:ui :entity-prefs <entity> :display :show-edit?]
[:ui :entity-prefs <entity> :columns :visible]
[:ui :entity-prefs <entity> :filters :fields]
```

### Phase 2: Simplify Display Settings Subscription

**Files Modified:**
- `src/app/template/frontend/subs/ui.cljs`
  - Added `default-display-settings` constant map with all setting defaults
  - Added `default-control-settings` constant map for control visibility
  - Refactored `::entity-display-settings` subscription with inline helpers:
    - `get-setting`: Checks hardcoded → new path → legacy path → defaults
    - `get-control`: Checks new path → legacy path → defaults (no hardcoded for controls)
  - Removed complex external helper dependencies
  - Clear precedence documented in subscription code

**Default Values Location:**
```clojure
;; File: src/app/template/frontend/subs/ui.cljs

(def default-display-settings
  {:show-timestamps?    true
   :show-edit?          true
   :show-delete?        true
   :show-highlights?    true
   :show-select?        true
   :show-filtering?     true
   :show-pagination?    true
   :show-add-button?    true
   :show-batch-edit?    false
   :show-batch-delete?  false})

(def default-control-settings
  {:show-timestamps-control?  true
   :show-edit-control?        true
   :show-delete-control?      true
   :show-highlights-control?  true
   :show-select-control?      true
   :show-filtering-control?   true
   :show-invert-selection?    true
   :show-delete-selected?     true})
```

### Phase 3: Remove Config-Cache Runtime Reads

**Files Modified:**
- `src/app/template/frontend/subs/ui.cljs`
  - Removed `config-loader` import (was: `[app.admin.frontend.config.loader :as admin-config]`)
  - Updated `::hardcoded-view-options` to read from app-db paths only:
    - `[:admin :settings :view-options entity]` (API-updated settings)
    - `[:admin :config :view-options entity]` (app-init settings)
  - Updated `::filterable-fields` to read from `[:admin :config :table-columns entity :filterable-columns]`
  - Config-cache atom remains for bootstrap only, not queried at runtime

- `test/app/template/frontend/subs/ui_test.cljs`
  - Removed `config-loader` mock in `filterable-fields-vector-config-test`
  - Updated test to populate app-db with config data instead

**Config Settings Storage:**
```clojure
;; Bootstrap (compile-time): resources/public/admin/ui-config/*.edn
;; → preload.cljs loads into config-cache atom
;; → init event copies to app-db

;; Runtime (query locations):
[:admin :config :view-options entity]       ;; Initial config
[:admin :settings :view-options entity]     ;; API-updated config
[:admin :config :table-columns entity]      ;; Column config
```

### Phase 4: Simplify Settings Panel

**Files Modified:**
- `src/app/template/frontend/components/settings/list_view_settings.cljs`
  - Removed `_vector-mode?` parameter from `toggle-column!` function (was unused)
  - Updated call site: `(toggle-column! entity-kw field-id)` instead of `(toggle-column! true entity-kw field-id)`

- `src/app/template/frontend/utils/column_config.cljs`
  - Simplified `toggle-column-event` function signature: removed `vector-mode?` parameter
  - Now always returns `[:admin/toggle-column-visibility entity-kw field-id]`
  - Simplified `toggle-filter-event` function signature: removed `_vector-mode?` parameter
  - Removed branching logic (was: `if vector-mode?` conditional)

- `test/app/template/frontend/utils/column_config_test.cljs`
  - Updated `toggle-events-test` to match new API signatures
  - Removed tests for mode-switching behavior (no longer exists)

### Phase 5: List View Integration

**Files Modified:**
- `src/app/template/frontend/components/list.cljs`
  - Removed redundant `effective-hardcoded-settings` variable (was just aliasing `hardcoded-view-options`)
  - Direct usage: `:page-display-settings hardcoded-view-options` in table props

### Phase 6: Testing & Cleanup

**Files Modified:**
- `src/app/template/frontend/subs/ui.cljs`
  - Removed unused `get-entity-setting` helper function (73 lines)
  - Removed unused `get-display-setting` helper function (33 lines)
  - Removed duplicate `default-control-settings` definition
  - Total cleanup: ~110 lines of dead code removed

**Test Results:**
- ✅ All 212 tests pass with 1250 assertions
- ✅ Admin build compiles successfully (7 pre-existing warnings in unrelated files)
- ✅ No breaking changes to public APIs

---

## Default Values Reference

```clojure
(def default-display-settings
  {:show-edit?          true
   :show-delete?        true
   :show-select?        true
   :show-filtering?     true
   :show-pagination?    true
   :show-timestamps?    true
   :show-highlights?    true
   :show-add-button?    true
   :show-batch-edit?    false
   :show-batch-delete?  false})
```

---

## Manual Testing Guide

### Prerequisites
1. Ensure the admin app is running: Admin UI is at `http://localhost:8085`
2. Ensure you have test data populated (users, audit logs, etc.)
3. Have browser DevTools open (Console + Network tabs)

### Test Scenario 1: Settings Panel Toggles

**Goal:** Verify user preferences are saved and loaded correctly

**Steps:**
1. Navigate to `http://localhost:8085/admin/users`
2. Open the Settings Panel (gear icon in the top-right)
3. **Test Display Toggles:**
   - Toggle "Edit" off → verify Edit buttons disappear from table rows
   - Toggle "Edit" back on → verify Edit buttons reappear
   - Toggle "Delete" off → verify Delete buttons disappear
   - Toggle "Timestamps" off → verify created_at/updated_at columns disappear
   - Toggle "Highlights" off → verify row highlighting on hover is disabled
   - Toggle "Pagination" off → verify pagination controls disappear (if > 1 page)
4. **Test Column Visibility:**
   - Click "Name" toggle → verify name column disappears from table
   - Click "Name" toggle again → verify name column reappears
   - Try toggling multiple columns on/off
5. **Test Filter Controls:**
   - Click the filter icon next to "Email" column toggle
   - Verify filter state changes (icon color change)
   - Try filtering with different settings enabled/disabled
6. **Test Table Width:**
   - Change table width value (e.g., from 1200 to 1500)
   - Press Enter or blur input
   - Verify table width changes visually
7. **Test Rows Per Page:**
   - Change rows per page dropdown (e.g., 10 → 25)
   - Verify table updates with new page size

**Verify Persistence:**
1. After making changes, refresh the page (`Cmd+R` / `Ctrl+R`)
2. Verify all your setting changes persisted (toggles, column visibility, table width)

**Check app-db State:**
```javascript
// Open browser console and run:
window.re_frame.db.app_db.deref()

// Look for your changes at:
// [:ui :entity-prefs :users :display]
// [:ui :entity-prefs :users :columns :visible]
// Example expected structure:
// {:ui {:entity-prefs {:users {:display {:show-edit? false
//                                         :show-timestamps? false}
//                              :columns {:visible [:id :email :role]}}}}}
```

### Test Scenario 2: Hardcoded Settings (view-options.edn)

**Goal:** Verify hardcoded settings override user preferences and hide controls

**Steps:**
1. Edit `resources/public/admin/ui-config/view-options.edn`
2. Add hardcoded setting for users entity:
```clojure
:users {:show-edit? false     ;; Lock edit off
        :show-delete? false}  ;; Lock delete off
```
3. Restart the admin app or reload config
4. Navigate to `http://localhost:8085/admin/users`
5. Open Settings Panel
6. **Verify:**
   - Edit and Delete buttons do NOT appear in table (locked off)
   - Edit and Delete toggles do NOT appear in settings panel (controls hidden)
   - User cannot override hardcoded settings
7. **Test Timestamps (not hardcoded):**
   - Timestamps toggle should still appear
   - Toggle it off → verify timestamps columns disappear
   - Verify this user preference persists on refresh

**Check app-db State:**
```javascript
// Hardcoded settings should be at:
window.re_frame.db.app_db.deref()
// [:admin :config :view-options :users]
// Should contain: {:show-edit? false, :show-delete? false}
```

### Test Scenario 3: Multiple Entities

**Goal:** Verify settings are entity-specific and don't conflict

**Steps:**
1. Navigate to `http://localhost:8085/admin/users`
2. Toggle "Edit" off for users
3. Navigate to `http://localhost:8085/admin/audit`
4. **Verify:**
   - Audit page should have "Edit" ON by default (separate entity)
   - Settings are not shared between entities
5. Toggle "Timestamps" off for audit
6. Navigate back to `http://localhost:8085/admin/users`
7. **Verify:**
   - Users still has "Edit" OFF (persisted)
   - Users should have "Timestamps" ON (separate setting per entity)

**Check app-db State:**
```javascript
// Both entities should have separate state:
window.re_frame.db.app_db.deref()
// [:ui :entity-prefs :users :display :show-edit?] → false
// [:ui :entity-prefs :audit :display :show-edit?] → true (or missing, uses default)
// [:ui :entity-prefs :audit :display :show-timestamps?] → false
```

### Test Scenario 4: Column Configuration

**Goal:** Verify column visibility and filtering work correctly

**Steps:**
1. Navigate to `http://localhost:8085/admin/users`
2. Open Settings Panel
3. **Test Column Visibility:**
   - Note which columns are visible initially
   - Toggle "Email" off → verify email column disappears
   - Toggle "Role" off → verify role column disappears
   - Verify "ID" column cannot be hidden (always-visible)
4. **Test Column Filtering:**
   - Click filter icon next to "Email" column
   - Type a filter value in the email column header
   - Verify filtering works
   - Disable filter for "Email" (click filter icon again)
   - Verify filter input disappears from email column header
5. **Test Always-Visible Columns:**
   - Try to toggle "ID" column off
   - Verify it stays visible (should be disabled/grayed out)

### Test Scenario 5: Settings Page API Updates

**Goal:** Verify settings page can update view-options via API

**Steps:**
1. Navigate to `http://localhost:8085/admin/settings`
2. Find the "View Options" section
3. Make changes to entity settings (e.g., lock show-edit for users)
4. Save settings
5. **Verify:**
   - Success message appears
   - Settings persist after page refresh
   - Navigate to affected entity page (e.g., /admin/users)
   - Verify hardcoded settings apply (controls hidden, locked behavior)

**Check Network Tab:**
- Look for PUT/POST request to `/api/v1/admin/settings/view-options`
- Verify request payload contains updated settings
- Check response status (should be 200 OK)

### Test Scenario 6: Backward Compatibility

**Goal:** Verify legacy paths still work during migration period

**Steps:**
1. Open browser console
2. Manually set legacy path:
```javascript
// Set legacy setting
let db = window.re_frame.db.app_db.deref()
db = assoc_in(db, ['ui', 'entity-configs', 'users', 'show-select'], false)
window.re_frame.db.app_db.reset(db)
```
3. Verify legacy setting is read and applied to UI
4. Toggle the setting in Settings Panel
5. Verify it writes to NEW path but still works

### Common Issues to Watch For

**Issue 1: Settings not persisting after refresh**
- Check: `[:ui :entity-prefs]` path in app-db
- Verify: Events are dispatching correctly (check re-frame-10x or console)
- Debug: Use `app-db-inspect` skill to examine state

**Issue 2: Hardcoded settings not hiding controls**
- Check: `[:admin :config :view-options]` contains expected config
- Verify: `::hardcoded-view-options` subscription returns correct map
- Verify: Settings panel component receives `:page-display-settings` prop

**Issue 3: Column toggles not working**
- Check: `[:admin :config :table-columns entity :available-columns]` is populated
- Verify: Admin events are registered (check re-frame event registry)
- Debug: Check browser console for errors

**Issue 4: Filters not showing/working**
- Check: `[:admin :config :table-columns entity :filterable-columns]` is populated
- Verify: User filter settings at `[:ui :entity-prefs entity :filters :fields]`
- Verify: Filter icon click handlers are wired correctly

### Debugging Tools

**2. App-db Inspection:**
```javascript
// Full app-db
window.re_frame.db.app_db.deref()

// Specific paths
_.get(window.re_frame.db.app_db.deref(), ['ui', 'entity-prefs'])
_.get(window.re_frame.db.app_db.deref(), ['admin', 'config', 'view-options'])
```

**3. Subscription Values:**
```javascript
// Check subscription result
@(window.re_frame.core.subscribe(['app.template.frontend.subs.ui/entity-display-settings', 'users']))
```

**4. MCP Skills:**
- `app-db-inspect`: Examine re-frame app-db state
- `reframe-events-analysis`: Analyze event history and performance
- `system-logs`: Check for backend/compile errors

---

## Risk Mitigation

1. **Backward compatibility**: Keep old event names as aliases during migration
2. **Incremental rollout**: Each phase is independently testable
3. **Data preservation**: User preferences should persist through migration
4. **Rollback plan**: Git branches allow easy revert if issues found

---

## Open Questions

1. Should we persist user preferences to localStorage for session persistence?
2. Should table-columns config move to same EDN as view-options (single file)?
3. Do we need server-side preference storage for multi-device sync?

---

## Known Issues & Bugs

⚠️ **CRITICAL ISSUES FOUND DURING MANUAL TESTING (9 Dec 2025)**

See `BUGFIXES-list-view-settings.md` for detailed analysis and fixes:

1. **Settings not persisting after page refresh** - CRITICAL
   - User preferences lost on Ctrl+R/Cmd+R
   - No localStorage persistence implemented
   - FIX: Add persistence interceptor + bootstrap loader

2. **Users `:show-select?` toggle OFF by default** - MEDIUM
   - Expected: ON (per default-display-settings)
   - Actual: OFF for users entity
   - FIX: Verify view-options.edn or page-level props

3. **Timestamps filter toggle doesn't work** - MEDIUM
   - Filter icon visible but click has no effect
   - State mismatch between display and filter state
   - FIX: Update filter icon state check logic

4. **Always-visible columns not properly configured** - LOW
   - Expected: ID always visible (disabled toggle)
   - Actual: Only email is always-visible per config
   - FIX: Add `:id` to `:always-visible` in table-columns.edn

**Status:** Refactoring complete, but requires bug fixes before production use.

---

## Notes

- Admin UI at `http://localhost:8085`
- Use `system-logs` skill for backend issues
- Use `app-db-inspect` skill for frontend state debugging
- Use `reframe-events-analysis` skill for event flow issues
