# PLAN — Refactor /admin/admin-settings + /admin/user-settings UI (parity + full coverage)

**Created**: 2025-12-13
**Updated**: 2025-12-14
**Status**: Complete

---

## Goal

Refactor both settings pages so they share **exactly the same UI and UIX component structure**, aligned with the plan in `PLAN-simplify-admin-list-display-settings.md` and the current implemented behavior.

---

## Requirements Summary

1. **UI parity**: `/admin/admin-settings` and `/admin/user-settings` must look and behave the same.
2. **Show "all possible settings" in edit mode**: Even if keys are missing from EDN.
3. **Single-scope editing + switchable scope**: Edit one scope at a time (Admin vs User), with a scope switcher.
4. **View mode shows all pages (entities)**: Both scopes (Admin + User), all entities, only implemented settings.
5. **No functional changes**: Preserve existing save semantics (draft + Save/Discard).
6. **Prevent hard reload after Save**: For both pages.

---

## Legacy baseline (pre-refactor)

> Note: This section describes the starting point before the unified settings refactor. Both routes now use `src/app/admin/frontend/pages/unified_settings.cljs`.

### Admin Settings (`/admin/admin-settings`)
- **File**: `src/app/admin/frontend/pages/settings.cljs`
- **Events**: `src/app/admin/frontend/events/settings.cljs`
- **Features**:
  - 3 tabs: View Options, Form Fields, Table Columns
  - Domain tabs within View Options: System, Domain, Other
  - Edit mode toggle (global)
  - Draft + Save/Discard for View Options
  - Displays entities grouped by domain
  - ~740 lines

### User Settings (`/admin/user-settings`)
- **File**: `src/app/admin/frontend/pages/user_settings.cljs`
- **Events**: `src/app/admin/frontend/events/user_settings.cljs`
- **Features**:
  - 2 tabs: View Options, Table Columns
  - Always in edit mode (no view mode)
  - Draft + Save/Discard
  - Only domain entities (:expenses)
  - ~325 lines

### Key Differences to Reconcile
| Feature | Admin Settings | User Settings |
|---------|---------------|---------------|
| View/Edit mode toggle | Yes | No (always edit) |
| Tabs | 3 (View Options, Form Fields, Table Columns) | 2 (View Options, Table Columns) |
| Domain organization | Yes (System, Domain, Other) | Yes (simpler) |
| Scope | Admin entities | Domain entities |
| Edit mode behavior | Toggle | Always on |
| Entity grouping | Complex domain-section component | Simpler inline grouping |

---

## Implementation Plan

### Phase 1 — Create shared settings definitions registry
**Objective**: A single data structure describing all supported settings.

**File**: `src/app/admin/frontend/settings/definitions.cljs` (new)

```clojure
;; Settings definition structure:
;; - :key - setting keyword (e.g., :show-edit?)
;; - :label - human-readable label
;; - :control-type - :toggle | :select | :number | :list
;; - :default - default value when missing from EDN
;; - :help-text - optional help text
```

**Tasks**:
- [ ] Define `display-setting-keys` (shared between both pages)
- [ ] Define `action-setting-keys` (shared between both pages)
- [ ] Define `domain-groups` (System vs Domain entities)
- [ ] Implement helper functions for setting labels, entity titles

### Phase 2 — Create shared SettingsShell component
**Objective**: One UIX component that both routes use.

**File**: `src/app/admin/frontend/components/settings_shell.cljs` (new)

**Props**:
- `:page-title` - "Admin Settings" or "User Settings"
- `:scope` - current editing scope (:admin | :user)
- `:on-scope-change` - callback for scope switching
- `:mode` - :view | :edit
- `:on-mode-change` - callback for mode toggle
- `:dirty?` - boolean indicating unsaved changes
- `:saving?` - boolean indicating save in progress
- `:loading?` - boolean indicating data loading
- `:on-save` - callback for save
- `:on-discard` - callback for discard
- `:children` - content to render

**Tasks**:
- [ ] Create shell component with header, mode toggle, save/discard buttons
- [ ] Add scope switcher (Admin ↔ User) for edit mode
- [ ] Add entity/page switcher for edit mode
- [ ] Wire up callbacks for save/discard

### Phase 3 — Implement view mode overview rendering
**Objective**: Both routes show consolidated overview in view mode.

**Tasks**:
- [ ] Create `settings-overview` component
- [ ] Render two sections: "Admin Settings" and "User Settings"
- [ ] For each section, show all entities/pages
- [ ] For each entity, show only settings present in config (not all possible)
- [ ] Add visual distinction between sections

### Phase 4 — Implement edit mode full coverage rendering
**Objective**: Edit mode shows all supported settings for selected scope/entity.

**Tasks**:
- [ ] Create `settings-editor` component
- [ ] Render all settings controls even if keys missing from EDN
- [ ] Apply defaults for missing keys
- [ ] Indicate "Not configured yet" vs "Configured" where helpful
- [ ] Maintain draft UX (staged edits, Save/Discard, dirty indicator)

### Phase 5 — Add scope switcher (Admin vs User) in edit mode
**Objective**: Allow switching between editing admin settings vs user settings.

**Tasks**:
- [ ] Add scope switcher UI in edit mode
- [ ] Preserve unsaved edits when switching (keep separate drafts per scope)
- [ ] Ensure no accidental silent loss of edits

### Phase 6 — Add entity/page switcher in edit mode
**Objective**: Edit one entity at a time with switcher.

**Tasks**:
- [ ] Add entity/page selector in edit mode
- [ ] Filter entities by current scope
- [ ] Preserve selected entity when switching scopes (if exists in target scope)

### Phase 7 — Ensure no hard reload on save for both pages
**Objective**: Saving settings must not trigger a full reload/restart.

**Tasks**:
- [ ] Verify admin settings save doesn't reload (already implemented)
- [ ] Verify user settings save doesn't reload
- [ ] Update app-db config post-save rather than relying on recompilation
- [ ] Test both paths

### Phase 8 — Refactor both pages to use shared components
**Objective**: Both `/admin/admin-settings` and `/admin/user-settings` use the shared shell.

**Tasks**:
- [x] Route both pages to unified settings UI (`unified-settings-page`)
- [x] Remove duplicated UI in favor of shared components
- [x] Ensure visual parity

### Phase 9 — Testing and verification
**Objective**: All tests pass, manual verification complete.

**Tasks**:
- [x] Run FE tests: `npm run test:cljs 2>&1 | tee /tmp/fe-test.txt`
- [x] Verify `/admin/admin-settings` view mode shows both scopes
- [x] Verify `/admin/user-settings` view mode shows both scopes
- [x] Verify edit mode scope switching
- [x] Verify edit mode entity switching
- [x] Verify Save/Discard behavior
- [x] Verify no full reload on save

---

## Acceptance Criteria

- [x] Visual/UI parity between `/admin/admin-settings` and `/admin/user-settings`
- [x] View mode (from either route) shows two sections (Admin + User) with all entities/pages
- [x] Edit mode shows one scope at a time with scope switcher
- [x] Edit mode shows one entity at a time with entity switcher
- [x] Edit mode renders full set of supported settings controls
- [x] Save/Discard works as today
- [x] Saving does not trigger hard reload/restart
- [x] No backend API changes required
- [x] Frontend tests remain green

---

## Key Files

### Current (in use)
- `src/app/admin/frontend/pages/unified_settings.cljs`
- `src/app/admin/frontend/events/unified_settings.cljs`
- `src/app/admin/frontend/components/settings_shell.cljs`
- `src/app/admin/frontend/components/settings_views.cljs`
- `src/app/admin/frontend/settings/definitions.cljs`
- `src/app/admin/frontend/routes.cljs`

### Legacy baseline (no longer routed to)
- `src/app/admin/frontend/pages/settings.cljs`
- `src/app/admin/frontend/pages/user_settings.cljs`
- `src/app/admin/frontend/events/settings.cljs`
- `src/app/admin/frontend/events/user_settings.cljs`

---

## Progress

| Phase | Status | Notes |
|-------|--------|-------|
| 1. Shared definitions | ✅ Complete | `src/app/admin/frontend/settings/definitions.cljs` |
| 2. SettingsShell component | ✅ Complete | `src/app/admin/frontend/components/settings_shell.cljs` |
| 3. View mode overview | ✅ Complete | Integrated in unified-settings page |
| 4. Edit mode full coverage | ✅ Complete | Shows all settings for selected entity |
| 5. Scope switcher | ✅ Complete | Admin ↔ User switching in edit mode |
| 6. Entity switcher | ✅ Complete | Entity dropdown in edit mode |
| 7. No reload on save | ✅ Complete | Using existing save mechanisms |
| 8. Refactor both pages | ✅ Complete | Both routes use `unified-settings-page` |
| 9. Testing | ✅ Complete | `npm run test:cljs` green (228 tests, 0 failures) |

## Implementation Summary

### Scope Clarification (Updated)
- **Admin Settings** (`/admin/admin-settings`): Controls display settings for admin pages showing entities like `:users`, `:admins`, `:audit-logs`, `:login-events`, `:expenses`, `:receipts`, `:suppliers`, `:payers`, `:articles`, etc.
- **User Settings** (`/admin/user-settings`): Controls display settings for user-facing pages (currently only `:expenses` entity)

### Resolved regressions

- **2025-12-14 — `/admin/user-settings` toggles unclickable**
  - **Symptom**: all “Default On/Off” toggles rendered but could not be clicked.
  - **Root cause**: `user-entity-editor` did not pass `:editing? true` to `views/user-entity-settings-card`, so rows rendered as non-clickable `<span>`.
  - **Fix**: pass `:editing? true` when rendering `user-entity-settings-card`.
  - **Verification**: confirmed in browser (toggles render as `<button>` and cycle states) + `npm run test:cljs` green.

### New Files Created
- `src/app/admin/frontend/settings/definitions.cljs` - Shared settings definitions registry with detailed tooltips
- `src/app/admin/frontend/components/settings_shell.cljs` - Unified shell component
- `src/app/admin/frontend/components/settings_views.cljs` - Shared view/edit components with tooltips
- `src/app/admin/frontend/events/unified_settings.cljs` - Unified state management
- `src/app/admin/frontend/pages/unified_settings.cljs` - Unified settings page

### Modified Files
- `src/app/admin/frontend/routes.cljs` - Both `/admin/admin-settings` and `/admin/user-settings` now use `unified-settings-page`

### Key Features Implemented
1. **View mode**: Shows overview of both Admin and User settings scopes
2. **Edit mode**: Single scope at a time with scope switcher (Admin ↔ User)
3. **Entity selection**: Dropdown to select which entity to edit
4. **Full settings coverage**: Edit mode shows all possible settings even if not configured
5. **Save/Discard**: Preserved existing draft + Save/Discard UX
6. **Visual parity**: Both routes use the same unified page component
7. **Tooltips**: All toggle settings now have detailed tooltip descriptions explaining their functionality
