# Admin List View Display Settings (Current Implementation)

Scope: admin list pages under `src/app/admin/frontend/pages/**` (and the shared list/table components they render). This doc is meant as a “starting point” inventory before simplifying the approach.

---

## 1) Where admin list pages come from

Most admin list pages are tiny wrappers that call the generic entity page:

- `src/app/admin/frontend/pages/users.cljs` → `($ generic-admin-entity-page :users)`
- `src/app/admin/frontend/pages/admins.cljs` → `($ generic-admin-entity-page :admins)`
- `src/app/admin/frontend/pages/audit.cljs` → `($ generic-admin-entity-page :audit-logs)`
- `src/app/admin/frontend/pages/login_events.cljs` → `($ generic-admin-entity-page :login-events)`

The main pipeline for list rendering is:

1. `src/app/admin/frontend/components/generic_admin_entity_page.cljs:28` (`generic-admin-entity-page`)
2. `src/app/admin/frontend/components/admin_page_wrapper.cljs:20` (`admin-page-wrapper`)
3. `src/app/admin/frontend/renderers/content.cljs:66` (`create-main-content-renderer`)
4. `src/app/template/frontend/components/list.cljs:33` (`list-view`)
5. `src/app/template/frontend/components/table.cljs:196` (`table`) → renders the in-table settings panel.

---

## 2) What “Table Settings Panel” means in this repo

The *in-table* settings row is rendered by the shared table component:

- `src/app/template/frontend/components/table.cljs:282` inserts a dedicated row between `<thead>` and `<tbody>`
- Clicking the settings icon toggles the panel (`settings-panel-visible?`)
- The panel UI is `src/app/template/frontend/components/settings/list_view_settings.cljs:129` (`list-view-settings-panel`)

This is separate from the routed settings pages:

- `/admin/admin-settings` (admin scope)
- `/admin/user-settings` (user scope)

(``/admin/settings`` is a legacy route that redirects to ``/admin/admin-settings``.)

- `src/app/admin/frontend/pages/unified_settings.cljs` (edits display toggles + column visibility defaults/locks)

There is also a legacy “Admin UI Configuration” page:

- `src/app/admin/frontend/pages/settings.cljs` (edits `view-options`, `table-columns`, `form-fields` via API; currently not routed)

---

## 3) Settings taxonomy (what exists today)

There are three distinct “settings” categories that affect admin list views:

### A) Display toggles (`:show-*?`)

These are booleans like:

- `:show-edit?`, `:show-delete?`, `:show-select?`, `:show-highlights?`, `:show-pagination?`
- `:show-timestamps?`, `:show-filtering?`
- `:show-add-button?`
- `:show-batch-edit?`, `:show-batch-delete?`

The single “authoritative” read API for these values is:

- `src/app/template/frontend/subs/ui.cljs:73` (`::entity-display-settings`)

### B) Column visibility + per-column filterability

These are per-column preferences/config:

- Column visibility (which columns render)
- Per-column filter toggle (whether a column shows filter affordances)

Admin pages mostly run in **vector-config mode**, so column visibility is **not** stored in the same place as other “user prefs” (details below).

### C) Table size + pagination UI state

- Table width (pixels): user preference
- Rows-per-page: list UI state

---

## 4) Sources of settings (and how they’re loaded)

### 4.1 Admin entity page config (`entities.edn`)

Per-entity admin page configuration is in:

- `src/app/admin/frontend/config/entities.edn`

It includes `:display-settings` (defaults) and `:features` (feature flags) for each entity.

This file is preloaded and registered at startup:

- `src/app/admin/frontend/config/preload.cljs:65` reads `entities.edn`
- `src/app/admin/frontend/subs/config.cljs:109` exposes it via `:admin/entity-config`

### 4.2 Admin “hardcoded” view options (`view-options.edn` + Admin Settings UI)

Hardcoded/locked display toggles live in:

- `src/app/admin/frontend/config/view-options.edn` (preloaded)
- plus any server-side overrides loaded/edited from the admin settings UI (`/admin/admin-settings`):
  - `src/app/admin/frontend/events/settings.cljs:18` (`::load-view-options` → stores in `[:admin :settings :view-options]`)

They’re loaded into app-db for general consumption by:

- `src/app/admin/frontend/events/config.cljs:151` (`:admin/load-ui-configs` sets `[:admin :config]` from the config cache)
- `src/app/admin/frontend/config/preload.cljs:53` (preloads `view-options.edn` into `config-cache`)
- `src/app/admin/frontend/config/loader.cljs:5` (`config-cache`)

### 4.3 Table column config (`table-columns.edn`)

Column definitions live in:

- `src/app/admin/frontend/config/table-columns.edn`

This file is stored and used in the **internal (non-inverted)** shape.

There is no longer any preload/load-time conversion step in the frontend loader (no `:default-hidden-columns`, `:unfilterable-columns`, or `:unsortable-columns`; and no `transform-inverted-config` in `src/app/admin/frontend/config/loader.cljs`).

Per entity, the primary policy keys are:

- `:default-visible-columns`
- `:filterable-columns`
- `:sortable-columns`

And also:

- `:available-columns`, `:always-visible`, `:column-config` (width/formatter), `:computed-fields`

### 4.4 User preferences persistence (`:ui :entity-prefs` in localStorage)

Template “user preferences” are stored under `[:ui :entity-prefs <entity> ...]` and persisted to localStorage:

- Storage key: `ui-entity-prefs` (`src/app/template/frontend/interceptors/persistence.cljs:12`)
- Load on app bootstrap: `src/app/template/frontend/events/bootstrap.cljs:78` dispatches `::persistence/load-stored-prefs`
- Auto-save interceptor: `src/app/template/frontend/interceptors/persistence.cljs:64` (`persist-entity-prefs`)

---

## 5) Display settings: how values are merged + how “hardcoded” works

### 5.1 Single subscription that merges everything: `::entity-display-settings`

- `src/app/template/frontend/subs/ui.cljs:73` defines `::entity-display-settings`
- Precedence is (highest → lowest): hardcoded view-options → user prefs → legacy prefs → UI defaults → global UI → fallback defaults
- Hardcoded is detected by key *presence* using `(contains? hardcoded setting-key)` (`src/app/template/frontend/subs/ui.cljs:94-106`)

Important detail: hardcoded settings are read from app-db only:

- `[:admin :settings :view-options <entity>]` (admin settings UI/API-driven)
- `[:admin :config :view-options <entity>]` (preloaded config / config-loader snapshot)

### 5.2 “Hardcoded view options” map (used to hide toggles in the panel)

The settings panel doesn’t infer hardcoded status from the merged settings. Instead it is passed a map of hardcoded keys:

- `src/app/template/frontend/subs/ui.cljs:149` `::hardcoded-view-options`

This returns the merged view-options map (settings overrides win over config).

### 5.3 A second merging layer exists in `list-view`

`list-view` merges incoming props with the subscription result:

- `src/app/template/frontend/components/list.cljs:65-67`:
  - `merged-display-settings = (merge display-settings subscribed-settings)`
  - subscription values win over props for any overlapping keys

Admin pages also do a separate merge before that:

- `src/app/admin/frontend/renderers/content.cljs:41` (`effective-display-settings`)
  - merges `shared-utils/default-list-display-settings` + `entities.edn :display-settings` + propagated settings
  - overlays `:features` flags (read-only/batch-operations)

This means *today* there are multiple default/merge layers (template defaults, entities.edn defaults, view-options hardcoded, and per-user prefs).

### 5.4 Rows-per-page (`:per-page`) precedence + seeding

Rows-per-page is intentionally treated as **list UI state**, not a hardcoded "display toggle":

Precedence (highest → lowest):

1. Existing list UI state per-page (user choice)
  - Stored under the entity list UI state and controlled by `::ui-events/set-per-page`.
2. Configured default per-page
  - Provided either via the `list-view` prop `:per-page`, or via `entities.edn` `:display-settings :per-page`.
3. Fallback
  - If nothing else is present, list UI defaults fall back to 10.

Seeding rule:

- `list-view` will **seed** `:per-page` into list UI state only when it is missing.
- If the user already has a per-page choice in UI state, config defaults do **not** overwrite it.

Code references:

- `src/app/template/frontend/components/list.cljs` computes an effective per-page and conditionally dispatches the seed.
- `src/app/template/frontend/events/list/ui_state.cljs` owns `::set-per-page` and default/fallback behavior.

---

## 6) Column visibility (admin pages)

### 6.1 Vector-config mode detection

Admin lists usually run in vector-config mode:

- `src/app/template/frontend/components/list.cljs:73-75` checks `:admin/config-loaded?` + `column-config/vector-config?`
- `src/app/template/frontend/utils/column_config.cljs:8-13` checks `admin-config/has-vector-config?` (reads `config-cache`)

### 6.2 Where “visible columns” are read from (admin mode)

In vector-config mode, the *raw* visible columns value is a **vector** stored under admin config:

- `src/app/admin/frontend/subs/config.cljs:20` `::visible-columns` reads:
  - `[:admin :config :table-columns <entity> :visible-columns]`
  - else falls back to `:default-visible-columns`

`list-view` then converts this vector to a boolean map:

- `src/app/template/frontend/utils/column_config.cljs:22-50` (`get-visible-columns`)
  - builds `{col-key → true/false}` for all `available-columns`
  - forces `:always-visible` columns true

### 6.3 Default visible columns (admin mode)

Defaults come directly from the `table-columns.edn` entity entry (internal shape):

- `src/app/admin/frontend/config/table-columns.edn` (per entity)
  - `:available-columns`
  - `:default-visible-columns`
  - `:always-visible` columns cannot be hidden

### 6.4 Column visibility toggles in the table settings panel

UI:

- `src/app/template/frontend/components/settings/list_view_settings.cljs:30` (`column-visibility-settings`)
  - renders one button per field in `entity-spec`
  - disables buttons for `always-visible` columns (`always-visible-set`)
  - click dispatches:
    - vector-config mode → `:admin/toggle-column-visibility`
    - legacy mode → `::settings-events/toggle-column-visibility`
    - see `toggle-column!` (`src/app/template/frontend/components/settings/list_view_settings.cljs:16-25`)

Event (admin/vector mode):

- `src/app/admin/frontend/events/config.cljs:53` (`::toggle-column-visibility`)
  - preserves order relative to `:available-columns`
  - prevents hiding `:always-visible`
  - writes `[:admin :config :table-columns <entity> :visible-columns]`
  - persists to localStorage key `column-visibility-<entity>` (`src/app/admin/frontend/events/config.cljs:124-129`)

Observation: `::load-saved-column-config` exists (`src/app/admin/frontend/events/config.cljs:31-46`) but is not dispatched anywhere under `src/app/**` (so saved column visibility may not be restored automatically on reload).

### 6.5 Where visibility is applied during rendering

Headers:

- `src/app/template/frontend/components/list/table.cljs:149` builds headers
  - base headers check `visible-columns` and only render visible fields (`is-column-visible?`)
  - timestamp headers also check `visible-columns` (`column-visible?` helper)

Rows:

- `src/app/template/frontend/components/list/rows.cljs` computes values only for visible columns

---

## 7) Per-column filtering toggle (the small filter icon in the column buttons)

This is not the same as global `:show-filtering?`.

### 7.1 Which columns are allowed to be filterable

From admin table config:

- `src/app/template/frontend/subs/ui.cljs:165` `::filterable-fields` reads:
  - `[:admin :config :table-columns <entity> :filterable-columns]`

### 7.2 User preference state for per-column filter toggles

Stored in:

- `[:ui :entity-prefs <entity> :filters :fields <column>]` (boolean, default true)

Read/write:

- `src/app/template/frontend/events/list/settings.cljs:23` `::filterable-fields` (returns the map)
- `src/app/template/frontend/events/list/settings.cljs:39` `::toggle-field-filtering` (toggles and persists)

### 7.3 How it affects header filter icons

Headers decide “field filterable?” as:

- user override (map from `::settings-events/filterable-fields`) if present
- else allow if config says filterable

See:

- `src/app/template/frontend/components/list/table.cljs:192-206`
- filter icon is only rendered when `show-filtering?` is true AND `is-field-filterable?` is true

---

## 8) The “main” display toggles (Edit/Delete/Selection/Highlights/Pagination)

### Where the toggles are rendered (settings panel)

`list-view-settings-panel` currently renders toggles for:

- Edit (`::ui-events/toggle-edit`)
- Delete (`::ui-events/toggle-delete`)
- Highlights (`::ui-events/toggle-highlights`)
- Selection (`::ui-events/toggle-select`)
- Pagination (`::ui-events/toggle-pagination`)

See:

- `src/app/template/frontend/components/settings/list_view_settings.cljs:186-224`

### Where the toggles write state

All of these toggle events write to new user-pref path and persist:

- `src/app/template/frontend/events/list/ui_state.cljs:120` … `:after` includes `persistence/persist-entity-prefs`
- Writes under `[:ui :entity-prefs <entity> :display :show-…?]` via `toggle-entity-flag` (`src/app/template/frontend/events/list/ui_state.cljs:99-118`)

### How hardcoded settings hide toggles (panel behavior)

The settings panel hides a toggle entirely when `hardcoded-display-settings` contains its key:

- `src/app/template/frontend/components/settings/list_view_settings.cljs:160-168`

That hardcoded map is passed by the table:

- `src/app/template/frontend/components/table.cljs:260-305`
  - subscribes `::ui-subs/hardcoded-view-options`
  - passes it as `:hardcoded-display-settings` to the panel

### Where each toggle is applied

- **Edit/Delete**:
  - On admin pages using enhanced action buttons, visibility is driven by subscription directly:
    - `src/app/admin/frontend/renderers/actions.cljs:24-37` subscribes `::ui-subs/entity-display-settings` and passes `:show-edit?`, `:show-delete?` to the action component.
  - Some legacy/other paths also pass these booleans through list/row props via `base-props` (merged from `merged-display-settings`):
    - `src/app/template/frontend/components/list.cljs:219` + `src/app/template/frontend/components/list/rows.cljs`

- **Selection**:
  - Selection header and cells are reactive to `:show-select?`:
    - `src/app/template/frontend/components/list/cells.cljs:20` (`reactive-selection-cell`)
    - `src/app/template/frontend/components/list/cells.cljs:34` (`reactive-select-all-header`)
  - Batch buttons require 2+ selected ids:
    - `src/app/template/frontend/components/list/table.cljs:53-144` (`action-header-buttons`)

- **Highlights**:
  - Applied in the table body as row classes when `show-highlights?`:
    - `src/app/template/frontend/components/table.cljs:322-328`

- **Pagination**:
  - Pagination component rendered only when `:show-pagination?` and `total-pages > 1`:
    - `src/app/template/frontend/components/list.cljs:396-405`

---

## 9) Settings that exist, but are NOT exposed in the in-table settings panel

These keys are part of `::entity-display-settings`, but the *table settings panel* doesn’t give the user a toggle for them:

- `:show-timestamps?`
  - exists + toggle event exists (`src/app/template/frontend/events/list/ui_state.cljs:140-146`)
  - affects headers/rows, but there is no button in `list-view-settings-panel`
- `:show-filtering?`
  - exists in `::entity-display-settings` (`src/app/template/frontend/subs/ui.cljs:123`)
  - no toggle event found (only per-column filter toggles exist)
- `:show-add-button?`
  - controls the “+” button in `src/app/template/frontend/components/list/ui.cljs:37-56`
  - no toggle in settings panel
- `:show-batch-edit?` / `:show-batch-delete?`
  - control batch header buttons in `src/app/template/frontend/components/list/table.cljs:53-133`
  - no toggle in settings panel

Also, `::entity-display-settings` returns `:controls {...}` (e.g. `:show-edit-control?`) (`src/app/template/frontend/subs/ui.cljs:128-135`), but there is no UI for editing control-visibility preferences either.

---

## 10) Hardcoded vs user-changeable (admin pages)

### Hardcoded / “locked” (not shown in table settings panel)

If a key is present in the entity’s view-options map (`view-options.edn` or admin settings UI), then:

1. `::entity-display-settings` treats it as absolute precedence (`contains?` check)
2. The table settings panel hides the toggle entirely (`contains? hardcoded-display-settings …`)

See:

- Hardcoded read: `src/app/template/frontend/subs/ui.cljs:78-82`
- Hardcoded map for panel: `src/app/template/frontend/subs/ui.cljs:149-160`
- Panel hide logic: `src/app/template/frontend/components/settings/list_view_settings.cljs:160-168`

### User-changeable via table settings panel

Currently changeable *from the in-table panel*:

- `:show-edit?`, `:show-delete?`, `:show-highlights?`, `:show-select?`, `:show-pagination?` (unless hardcoded)
- Table width (`[:ui :entity-prefs <entity> :columns :width]`)
- Column visibility (admin/vector-config mode uses `:admin/toggle-column-visibility`)
- Per-column filter toggle (`[:ui :entity-prefs <entity> :filters :fields]`)
- Rows per page (UI state via `::ui-events/set-per-page`)

Notably, `:show-add-button?`, `:show-batch-*?`, `:show-timestamps?`, and `:show-filtering?` are not user-togglable from this panel today.

---

## 11) “Why it feels complex” (simplification targets)

Things contributing to complexity today (all observable in code above):

1. Multiple default layers:
   - `shared-utils/default-list-display-settings` (`src/app/template/frontend/utils/shared.cljs:178`)
   - `ui-subs/default-display-settings` (`src/app/template/frontend/subs/ui.cljs:47`)
   - bootstrap `[:ui :defaults]` (`src/app/template/frontend/events/bootstrap.cljs:50-77`)
2. Multiple merge points:
   - admin content renderer merges defaults + entities.edn + propagated settings + feature overlays (`src/app/admin/frontend/renderers/content.cljs:41-64`)
   - list-view then merges props again with `::entity-display-settings` (`src/app/template/frontend/components/list.cljs:65-67`)
   - admin enhanced actions ignore the passed display-settings and subscribe directly (`src/app/admin/frontend/renderers/actions.cljs:24-37`)
3. Column visibility uses a different storage mechanism from other prefs on admin pages:
   - admin event + localStorage JSON key `column-visibility-<entity>` (`src/app/admin/frontend/events/config.cljs`)
   - vs general prefs in `ui-entity-prefs` EDN (`src/app/template/frontend/interceptors/persistence.cljs`)
4. The in-table settings panel exposes only a subset of the settings that exist in `::entity-display-settings`.

---

## 12) Code reference index (files involved)

Admin pages & config:

- `src/app/admin/frontend/pages/*.cljs` (entry points for admin list pages)
- `src/app/admin/frontend/config/entities.edn` (per-entity page defaults + features)
- `src/app/admin/frontend/config/view-options.edn` (hardcoded/lockable `:show-*?` keys, plus filters/search/pagination config)
- `src/app/admin/frontend/config/table-columns.edn` (available cols, default hidden, always-visible, widths/formatters)
- `src/app/admin/frontend/config/preload.cljs` (preloads the EDN configs into cache/registry)
- `src/app/admin/frontend/config/loader.cljs` (config-cache + async fetch from `/admin/api/settings*`)
- `src/app/admin/frontend/events/config.cljs` (`:admin/load-ui-configs`, admin column visibility toggle, localStorage persistence)
- `src/app/admin/frontend/subs/config.cljs` (`:admin/entity-config`, `::visible-columns`, etc)
- `src/app/admin/frontend/renderers/content.cljs` (merges entity config + display settings and renders `list-view`)
- `src/app/admin/frontend/renderers/actions.cljs` (row action visibility reacts to `::entity-display-settings`)
- `src/app/admin/frontend/pages/settings.cljs` + `src/app/admin/frontend/events/settings.cljs` (global admin settings UI + API persistence)

Template (shared list/table/settings implementation):

- `src/app/template/frontend/subs/ui.cljs` (`::entity-display-settings`, `::hardcoded-view-options`, `::filterable-fields`, `::visible-columns`)
- `src/app/template/frontend/hooks/display_settings.cljs` (hook wrapper over `::entity-display-settings`)
- `src/app/template/frontend/events/list/ui_state.cljs` (toggle events for Edit/Delete/Select/Highlights/Pagination/Timestamps)
- `src/app/template/frontend/events/list/settings.cljs` (table width + per-column filter toggles + legacy column visibility)
- `src/app/template/frontend/utils/column_config.cljs` (switches between vector-config vs legacy mode)
- `src/app/template/frontend/interceptors/persistence.cljs` (`ui-entity-prefs` localStorage persistence)
- `src/app/template/frontend/components/list.cljs` (reads settings + passes to table)
- `src/app/template/frontend/components/table.cljs` (injects settings row + opens panel)
- `src/app/template/frontend/components/settings/list_view_settings.cljs` (the in-table settings panel UI)
- `src/app/template/frontend/components/list/table.cljs` (header/action buttons; batch controls; filter icon logic; timestamps headers)
- `src/app/template/frontend/components/list/ui.cljs` (Add button rendering gated by `:show-add-button?`)
- `src/app/template/frontend/components/list/cells.cljs` (reactive selection/timestamps/action cells)
