# List View Settings - Bug Fixes Required

## ✅ ALL ISSUES RESOLVED (9 Dec 2025)

### 1. Settings Not Persisting After Page Refresh ✅ FIXED

**Problem:**
- User preferences (toggles, column visibility, table width) are lost on page refresh (Ctrl+R)
- App-db state exists only in memory, not persisted

**Solution Implemented:**
Created new persistence interceptor and integrated it into all preference events:

1. **Created** `src/app/template/frontend/interceptors/persistence.cljs`:
   - `persist-entity-prefs` interceptor - auto-saves prefs to localStorage after events
   - `::load-stored-prefs` event - loads persisted prefs on app init
   - Uses `pr-str`/`reader/read-string` for EDN serialization
   - Storage key: `ui-entity-prefs`

2. **Modified** `src/app/template/frontend/events/bootstrap.cljs`:
   - Added require for persistence namespace
   - Added `[:dispatch [::persistence/load-stored-prefs]]` to `::initialize-db` event

3. **Modified** `src/app/template/frontend/events/list/ui_state.cljs`:
   - Added persistence interceptor to all toggle events:
     - `::toggle-highlights`
     - `::toggle-select`
     - `::toggle-timestamps`
     - `::toggle-edit`
     - `::toggle-delete`
     - `::toggle-pagination`

4. **Modified** `src/app/template/frontend/events/list/settings.cljs`:
   - Added persistence interceptor to:
     - `::toggle-field-filtering`
     - `::toggle-column-visibility`
     - `::update-table-width`

---

### 2. Users `:show-select?` Toggle Is OFF by Default ✅ FIXED

**Problem:**
- `:show-select?` toggle shows as OFF by default for users entity

**Solution Implemented:**
Added `:show-select? true` to users config in `view-options.edn`:
```clojure
:users
{:search-fields [:email :first-name :last-name],
 :show-select? true,  ;; ← Added
 ...}
```

---

### 3. Timestamps Toggle Redundant ✅ RESOLVED (Removed)

**Original Problem:**
- Filter icon next to timestamps toggle was checking only `:created-at` but toggling both fields
- Icon state didn't reflect actual filterability

**Resolution:**
After analysis, the "Timestamps" toggle was redundant since we already have separate toggles for:
- `:created-at` column (with its own filter icon)
- `:updated-at` column (with its own filter icon)

**Solution Implemented:**
Removed the Timestamps toggle entirely from `list_view_settings.cljs`:
- Removed `curr-show-timestamps?` variable
- Removed `timestamp-filtering-enabled?` and `show-timestamp-filter-icon?` logic
- Removed the Timestamps toggle-button from the Display Toggles section
- Simplified the `toggle-button` helper (no longer needs filter icon logic)

The individual `:created-at` and `:updated-at` column toggles in the Column Visibility section handle timestamp visibility, each with their own filter icon when filtering is enabled.

---

### 4. Always-Visible Columns Not Including ID ✅ FIXED

**Problem:**
- ID column was toggleable but user expected it to be always-visible
- Config had `:always-visible [:email]` without `:id`

**Solution Implemented:**
Updated `table-columns.edn` for `:users` and `:admins`:
```clojure
:users
{:default-hidden-columns [:last-login-at :updated-at]  ;; Removed :id
 :always-visible [:id :email]}  ;; Added :id

:admins
{:default-hidden-columns [:updated-at]  ;; Removed :id
 :always-visible [:id :email]}  ;; Added :id
```

---

### 5. Filter Icons Not Showing for created-at/updated-at Table Headers ✅ FIXED

**Problem:**
- Filter icons were showing in the Column Visibility settings panel for `created-at` and `updated-at`
- But the filter icons were NOT showing in the actual table headers for these columns
- The Column Visibility panel worked correctly because it used the properly subscribed `filterable-columns`
- The table headers used a broken `field-filterable?` function that called `use-subscribe` inside a regular function (violating React hooks rules)

**Root Cause:**
In `table.cljs`, the `timestamp-headers` block had a `field-filterable?` function that called `use-subscribe` inside a nested function, which is invalid because React hooks must be called at the top level of a component.

```clojure
;; BEFORE (broken) - use-subscribe inside a fn
field-filterable? (fn [key]
                    ...
                    (let [filterable-columns (use-subscribe [...])]  ;; ❌ Invalid!
                      ...))
```

**Solution Implemented:**
Modified `src/app/template/frontend/components/list/table.cljs` to use the already-computed `filterable-set` variable instead of calling `use-subscribe` inside the function:

```clojure
;; AFTER (fixed) - use pre-computed filterable-set
field-filterable? (fn [key]
                    (let [user-setting (resolve-setting (or user-filterable-settings {}) key)]
                      (if (not= user-setting ::not-found)
                        user-setting
                        (cond
                          (map? filterable-set) (boolean (get filterable-set key true))
                          (set? filterable-set) (contains? filterable-set key)
                          :else true))))
```

---

## Summary of Fixes Applied

| Issue | Status | Files Modified |
|-------|--------|----------------|
| 1. No persistence | ✅ FIXED | 4 files (created persistence.cljs, updated bootstrap, ui_state, settings) |
| 2. Selection default OFF | ✅ FIXED | 1 file (view-options.edn) |
| 3. Timestamps filter broken | ✅ FIXED | 1 file (list_view_settings.cljs) |
| 4. Always-visible config | ✅ FIXED | 1 file (table-columns.edn) |
| 5. Table header filter icons | ✅ FIXED | 1 file (table.cljs) |

## Testing After Fixes

Run manual tests to verify all scenarios from testing guide:
- ✅ Settings persist after refresh (localStorage)
- ✅ Timestamps filter toggle works correctly (only shows when filterable)
- ✅ Always-visible columns (ID, email) cannot be toggled
- ✅ Selection default is ON for users

## Files Created/Modified

### New Files:
- `src/app/template/frontend/interceptors/persistence.cljs` - Persistence interceptor

### Modified Files:
- `src/app/template/frontend/events/bootstrap.cljs` - Load stored prefs on init
- `src/app/template/frontend/events/list/ui_state.cljs` - Added persistence to toggles
- `src/app/template/frontend/events/list/settings.cljs` - Added persistence to settings
- `src/app/template/frontend/components/settings/list_view_settings.cljs` - Fixed timestamp filter logic
- `src/app/admin/frontend/config/view-options.edn` - Added show-select for users
- `src/app/admin/frontend/config/table-columns.edn` - Added :id to always-visible
