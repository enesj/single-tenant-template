# Plan: Simplify Display Settings Config System

**Status**: Planning  
**Created**: 2024-12-02  
**Goal**: Simplify the display settings system while preserving exact functionality

---

## Current Problems

1. **Prop drilling through multiple layers**: `list.cljs` → `table.cljs` → `rows.cljs` → `row-content` - settings get lost or don't update reactively

2. **Mixed patterns for the same problem**: 
   - Edit/Delete: Fixed with `reactive-enhanced-actions` wrapper in `actions.cljs`
   - Selection: Fixed with `reactive-selection-cell` in `rows.cljs` and `reactive-select-all-header` in `table.cljs`
   - Timestamps, Highlights: Still use prop drilling

3. **Scattered subscription points**: Some components subscribe directly, others receive props, making it hard to reason about reactivity

4. **Multiple sources of truth**: Settings come from `entity-configs`, `defaults`, `hardcoded-view-options`, `display-settings` props - merged in complex ways

5. **Duplicate code**: Similar reactive wrapper patterns repeated in different files

---

## Implementation Plan

### Phase 1: Create a Unified Display Settings Hook
**File**: `src/app/template/frontend/hooks/display_settings.cljs` (new)

```clojure
(defn use-display-settings [entity-name]
  "Single hook that returns all display settings for an entity, fully merged and reactive"
  ;; Returns: {:show-select? bool, :show-edit? bool, :show-delete? bool, 
  ;;           :show-timestamps? bool, :show-highlights? bool, ...}
```

- Encapsulates ALL the merging logic (entity-configs + defaults + hardcoded)
- Single subscription point - always reactive
- Components call this hook directly instead of receiving props

### Phase 2: Create Reactive Cell Components Library
**File**: `src/app/template/frontend/components/list/cells.cljs` (new)

Consolidate all reactive cell wrappers:
- `reactive-selection-cell` (move from `rows.cljs`)
- `reactive-action-cell` (consolidate from `actions.cljs`)
- `reactive-timestamp-cell` (new - for timestamps visibility)

Each uses `use-display-settings` internally.

### Phase 3: Simplify Row Rendering
**File**: `src/app/template/frontend/components/list/rows.cljs`

- Remove `show-select?`, `show-edit?`, `show-delete?`, `show-timestamps?` from props
- `row-content` just renders cells, each cell handles its own visibility
- Reduces props passed to `render-row` significantly

### Phase 4: Simplify List Component
**File**: `src/app/template/frontend/components/list.cljs`

- Remove `merged-display-settings` computation
- Remove display-related props from `base-props`
- Let child components subscribe to what they need

### Phase 5: Consolidate Settings Sources
**File**: `src/app/template/frontend/subs/ui.cljs`

- Create one authoritative subscription: `::merged-entity-settings`
- Deprecate or redirect other display-settings subscriptions to this one
- Document the merge precedence clearly

---

## Benefits

| Current | Proposed |
|---------|----------|
| 5+ files with display settings logic | 2 files (hook + cells) |
| Props drilled through 4 layers | Direct subscription at leaf |
| Ad-hoc reactive wrappers | Consistent pattern |
| Hard to add new toggles | Add to hook + cell, done |
| Debugging requires tracing prop flow | Each component self-contained |

---

## Migration Strategy

- **Backward compatible**: Keep existing props as optional overrides
- **Incremental**: Migrate one setting at a time (Selection → Edit/Delete → Timestamps → Highlights)
- **Test each phase**: Verify toggle functionality after each phase

---

## Estimated Effort

- Phase 1: 2-3 hours (new hook)
- Phase 2: 2-3 hours (consolidate cells)
- Phase 3-4: 3-4 hours (refactor existing components)
- Phase 5: 1-2 hours (cleanup subscriptions)
- Testing: 2-3 hours

**Total: ~12-15 hours**

---

# Test Specification

## Test Environment
- **URL**: `http://localhost:8085/admin/users`
- **Tool**: Chrome MCP Server
- **Pre-condition**: Admin panel loaded with Users table visible

---

## Interactive Elements Map

| Element ID | Type | Purpose | Location |
|------------|------|---------|----------|
| `#toggle-selection` | div (clickable) | Toggle row selection checkboxes | Settings panel, main controls row |
| `#toggle-edit` | div (clickable) | Toggle edit buttons on rows | Settings panel, main controls row |
| `#toggle-delete` | div (clickable) | Toggle delete buttons on rows | Settings panel, main controls row |
| `#toggle-timestamps` | div (clickable) | Toggle timestamp columns | Settings panel, main controls row |
| `#toggle-highlights` | div (clickable) | Toggle row highlighting | Settings panel, main controls row |
| `#select-all-users` | input[checkbox] | Select all rows checkbox | Table header, first column |
| `#select-users-{uuid}` | input[checkbox] | Individual row checkbox | Table row, first column |
| `#btn-edit-users-{uuid}` | button | Edit row button | Table row, actions column |
| `#btn-delete-users-{uuid}` | button | Delete row button | Table row, actions column |

---

## Test Procedures

### TEST 1: Selection Toggle
**Goal**: Verify Selection button shows/hides checkboxes reactively

```
STEP 1: Get initial state
  Tool: mcp_clojure-mcp_clojurescript_eval
  Code: (get-in @re-frame.db/app-db [:ui :entity-configs :users :show-select?])
  Expected: nil or false (default off)

STEP 2: Verify checkboxes hidden
  Tool: mcp_chrome-mcp_chrome_get_web_content
  Selector: #select-all-users
  Expected: Element NOT found OR parent has class "hidden-cell"

STEP 3: Click Selection toggle
  Tool: mcp_chrome-mcp_chrome_click_element
  Selector: #toggle-selection
  Expected: Click successful

STEP 4: Verify state changed
  Tool: mcp_clojure-mcp_clojurescript_eval
  Code: (get-in @re-frame.db/app-db [:ui :entity-configs :users :show-select?])
  Expected: true

STEP 5: Verify "Select All" checkbox visible
  Tool: mcp_chrome-mcp_chrome_get_web_content
  Selector: #select-all-users
  Expected: <input type="checkbox" id="select-all-users" ...>

STEP 6: Verify row checkbox visible
  Tool: mcp_chrome-mcp_chrome_get_web_content
  Selector: tbody tr:first-child td:first-child input[type="checkbox"]
  Expected: <input type="checkbox" id="select-users-..." ...>

STEP 7: Toggle OFF - Click Selection again
  Tool: mcp_chrome-mcp_chrome_click_element
  Selector: #toggle-selection

STEP 8: Verify checkboxes hidden again
  Tool: mcp_chrome-mcp_chrome_get_web_content
  Selector: tbody tr:first-child td:first-child
  Expected: Contains "hidden-cell" class, NO checkbox
```

### TEST 2: Edit Toggle
**Goal**: Verify Edit button shows/hides edit buttons reactively

```
STEP 1: Get initial state
  Tool: mcp_clojure-mcp_clojurescript_eval
  Code: (get-in @re-frame.db/app-db [:ui :entity-configs :users :show-edit?])

STEP 2: Check toggle button style (indicates current state)
  Tool: mcp_chrome-mcp_chrome_get_web_content
  Selector: #toggle-edit span
  Expected: class contains "font-bold" (on) or "font-light" (off)

STEP 3: Click Edit toggle to enable
  Tool: mcp_chrome-mcp_chrome_click_element
  Selector: #toggle-edit

STEP 4: Verify edit button visible in row
  Tool: mcp_chrome-mcp_chrome_get_web_content
  Selector: button[id^="btn-edit-users-"]
  Expected: Button element found

STEP 5: Click Edit toggle to disable
  Tool: mcp_chrome-mcp_chrome_click_element
  Selector: #toggle-edit

STEP 6: Verify edit button hidden
  Tool: mcp_chrome-mcp_chrome_get_web_content
  Selector: button[id^="btn-edit-users-"]
  Expected: Element NOT found
```

### TEST 3: Delete Toggle
**Goal**: Verify Delete button shows/hides delete buttons reactively

```
Same pattern as TEST 2:
  Replace: #toggle-edit → #toggle-delete
  Replace: btn-edit-users → btn-delete-users
  Replace: show-edit? → show-delete?
```

### TEST 4: Timestamps Toggle
**Goal**: Verify Timestamps shows/hides created_at/updated_at columns

```
STEP 1: Check initial state
  Tool: mcp_clojure-mcp_clojurescript_eval
  Code: (get-in @re-frame.db/app-db [:ui :entity-configs :users :show-timestamps?])

STEP 2: Click Timestamps toggle
  Tool: mcp_chrome-mcp_chrome_click_element
  Selector: #toggle-timestamps

STEP 3: Verify "Created" column header visible
  Tool: mcp_chrome-mcp_chrome_get_web_content
  Selector: #header-created-at
  Expected: Element found with "Created" label

STEP 4: Click to disable
  Tool: mcp_chrome-mcp_chrome_click_element
  Selector: #toggle-timestamps

STEP 5: Verify "Created" column hidden
  Tool: mcp_chrome-mcp_chrome_get_web_content
  Selector: #header-created-at
  Expected: Element NOT found or hidden
```

---

## Success Criteria

| Test | Pass Condition |
|------|----------------|
| Selection Toggle | Checkboxes appear/disappear immediately on click |
| Edit Toggle | Edit buttons appear/disappear immediately on click |
| Delete Toggle | Delete buttons appear/disappear immediately on click |
| Timestamps Toggle | Timestamp columns appear/disappear immediately on click |
| Toggle Button Style | `font-bold` when ON, `font-light` when OFF |
| State Persistence | Value in `app-db` matches UI state |
| No Page Refresh | Changes visible without browser refresh |

---

## Debugging Commands

```bash
# Force recompile if changes not picked up
rm -rf .shadow-cljs/builds/admin && npx shadow-cljs compile admin

# Check for compile errors
npx shadow-cljs compile admin 2>&1 | grep -i error
```

```clojure
;; Check all display settings at once
(get-in @re-frame.db/app-db [:ui :entity-configs :users])

;; Manually dispatch toggle
(rf/dispatch-sync [:app.template.frontend.events.list.ui-state/toggle-select :users])
```

---

## Progress Tracking

- [x] Phase 1: Unified Display Settings Hook
- [x] Phase 2: Reactive Cell Components Library
- [x] Phase 3: Simplify Row Rendering
- [x] Phase 4: Simplify List Component
- [x] Phase 5: Consolidate Settings Sources
- [ ] All tests passing
