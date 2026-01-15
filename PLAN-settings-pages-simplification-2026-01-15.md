# PLAN — Simplify `/admin/admin-settings` + `/admin/user-settings` (list-view policy + config)
Date: 2026-01-15
Last updated: 2026-01-15

This is an audit + improvement plan for the two settings pages that manage list-view policy (defaults/locks) and related config for:

- Admin scope: `/admin/admin-settings` → `src/app/admin/frontend/config/*`
- User scope (domain-owned): `/admin/user-settings` → `src/app/domain/**/config/*`

It focuses on simplifying the “hardcoded” list-view settings (i.e., policy defaults/locks), eliminating drift/inconsistencies between backend, frontend editor UI, and runtime resolver behavior, and making permission-driven UI behavior predictable.

Related references already in repo:

- `docs/frontend/admin-settings.md` (architecture overview)
- `docs/frontend/list-view-controls-configuration.md` (intended list-view behavior)
- `PLAN-settings-ui-parity.md` (prior UI parity work)

---

## Status (implemented in this patch)

- [x] **Merge “Columns” policy UI into the Table Columns tab** by introducing a dedicated `columns-policy-card` (policy lives in `view-options`, structural config lives in `table-columns`).
  - Files: `src/app/admin/frontend/pages/unified_settings/page.cljs`, `src/app/admin/frontend/components/settings_views/cards.cljs`
- [x] **Remove the Entities tab** from `/admin/*-settings` because it was effectively a no-op (entity title wasn’t used anywhere in the settings UI/runtime).
  - Files: `src/app/admin/frontend/pages/unified_settings/page.cljs`, `src/app/admin/frontend/pages/unified_settings/editors.cljs`
- [x] **Make Form Fields affect the edit form**:
  - list edit forms now use the edit-form spec (`[:form-entity-specs/by-name <entity> true]`) instead of the table/vector spec.
  - admin settings events now sync `form-fields` + `table-columns` into `[:admin :config ...]` so runtime subscriptions update immediately.
  - Files: `src/app/template/frontend/components/list.cljs`, `src/app/template/frontend/components/list/rows.cljs`, `src/app/admin/frontend/events/settings/form_fields.cljs`, `src/app/admin/frontend/events/settings/table_columns.cljs`
- [x] **Show role-gated list buttons as visible-but-disabled** (instead of disappearing) where desired:
  - new list-view option `:disallowed-action-mode :disable` plus `:allow-add?` / `:allow-edit?` / `:allow-delete?`.
  - updated user pages: Expenses list, Suppliers, Payers.
  - Files: `src/app/template/frontend/components/list.cljs`, `src/app/template/frontend/components/list/rows.cljs`, `src/app/template/frontend/components/list/cells.cljs`, `src/app/domain/frontend/expenses/pages/user/expenses_list.cljs`, `src/app/domain/frontend/expenses/pages/user/suppliers.cljs`, `src/app/domain/frontend/expenses/pages/user/payers.cljs`
- [x] **Make Articles + Price Observations Edit/Delete settings actually work**:
  - Previously these pages passed `:render-actions (fn [_] nil)`, which suppressed row actions entirely.
  - Added modal edit forms + user-scoped PUT/DELETE endpoints so `show-edit?` / `show-delete?` toggles can be validated end-to-end.
  - Files: `src/app/domain/frontend/expenses/pages/user/articles.cljs`, `src/app/domain/frontend/expenses/pages/user/price_observations.cljs`, `src/app/domain/frontend/expenses/components/user_power_forms.cljs`, `src/app/domain/frontend/expenses/events/user_expenses/power_tools.cljs`, `src/app/domain/backend/expenses/handlers/user_articles.clj`, `src/app/domain/backend/expenses/handlers/user_price_observations.clj`, `src/app/domain/backend/expenses/routes/user_api.clj`
- [x] **Make default/lock changes easier to validate by clearing local overrides**:
  - User-facing list toggles can be overridden by browser-stored prefs (`[:ui :entity-prefs <entity> :display]`), making Defaults look “ignored”.
  - Added `Clear local overrides` action in `/admin/user-settings` and a `::clear-display-prefs` event to remove only display overrides.
  - Files: `src/app/admin/frontend/pages/unified_settings/editors.cljs`, `src/app/admin/frontend/components/settings_views/cards.cljs`, `src/app/template/frontend/events/list/ui_state.cljs`, `src/app/template/frontend/subs/ui.cljs`
- [x] **Remove stale “Expenses Admin” entities from admin scope** (admin pages removed):
  - remove Expenses admin groups/entities from the frontend domain registry
  - stop merging domain-admin config into admin settings in the backend
  - clean stale admin-owned `view-options.edn` keys for those entities
  - Files: `src/app/domain/frontend/registry.cljs`, `src/app/domain/backend/registry.clj`, `src/app/admin/frontend/config/view-options.edn`

---

## Current implementation (how it works today)

### 1) Pages / UI

- Both routes use the unified UIX page:
  - `src/app/admin/frontend/pages/unified_settings/page.cljs`
  - Shell/layout: `src/app/admin/frontend/components/settings_shell.cljs`
  - Editors/cards/rows:
    - `src/app/admin/frontend/pages/unified_settings/editors.cljs`
    - `src/app/admin/frontend/components/settings_views/*`

Config types in the UI:

- View options (policy defaults/locks): `view-options.edn`
- Form fields (create/edit field lists): `form-fields.edn`
- Table columns (structural column config): `table-columns.edn`

### 2) Backend persistence

- Admin settings (admin-owned config):
  - GET `"/admin/api/settings"` → `src/app/template/backend/routes/admin/settings.clj` `get-view-options-handler`
    - Reads via `src/app/template/backend/routes/admin/settings_io.clj` `read-view-options`
    - Supports merging domain-admin config + admin-owned config (admin “wins”).
    - In this repo, the Expenses domain no longer provides admin UI config (admin pages removed).
  - PUT `"/admin/api/settings"` → `update-view-options-handler`
    - Writes via `write-view-options!` to `src/app/admin/frontend/config/view-options.edn`

- User settings (domain-owned user UI config):
  - GET/PUT `"/admin/api/settings/user-ui-config"` → `get-user-ui-config-handler` / `update-user-ui-config-handler`
    - Reads/writes domain-owned EDNs (currently via `domain-registry/primary-user-ui-config-paths`).

### 3) Runtime behavior in list-view

- Display-toggle policy (defaults/locks) is enforced at runtime by:
  - Resolver: `src/app/template/frontend/settings/resolver.cljs`
  - UI subs: `src/app/template/frontend/subs/ui.cljs` `::locked-display-settings`
- List settings panel hides toggle controls when a key is locked:
  - `src/app/template/frontend/components/settings/list_view_settings.cljs`

---

## Key simplification opportunities (recommended direction)

### A) Decide what *policy* controls vs what remains *per-user preference*

Right now the codebase mixes these concepts for some settings (notably `:per-page`).

Recommendation: restrict `view-options.edn` to things the resolver + UI actually enforce as policy:

- Display toggles (show/edit/delete/select/filtering/etc.)
- Column visibility policy (`:column-defaults` / `:column-locks`)

For anything else (notably rows-per-page), pick one model and remove the other:

Option A (simpler): treat rows-per-page as per-user list UI state only, seeded by entity defaults (e.g., from `entities.edn`), and remove `:per-page` from policy.

Option B (more powerful): treat rows-per-page as a policy-controlled display setting, fully lockable, and enforce it end-to-end.

### B) Make “display setting key detection” canonical and shared

There are currently multiple independent implementations of “is this a display setting key?” (backend + frontend), which can drift.

Recommendation:

- Introduce a single canonical display-keys source based on `src/app/shared/specs/view_options.cljc`.
- Use it in:
  - backend routes (PATCH/DELETE behavior)
  - admin settings events (draft updates)
  - user settings events (draft updates)
  - settings panel hide/disable logic

### C) Remove duplicated tri-state mutation logic (admin vs user settings)

Admin settings and user settings duplicate the same tri-state “inherit/default/lock” mutations for:

- `:display-defaults` / `:display-locks`
- `:column-defaults` / `:column-locks`

Recommendation: extract shared pure helpers (functions that take a config map and return an updated config map) and reuse in both scopes.

### D) Fix the admin “merge + write” layering problem (domain-admin configs)

Today the admin config read path merges domain-admin config + admin-owned config, but the write path persists whatever the UI sends back to the admin-owned file.

This can accidentally “import” domain config into admin-owned files and then silently shadow future domain changes.

Recommendation: preserve layering explicitly:

- Read: `effective = merge(domain-base, admin-overrides)`
- Write: update only admin overrides (not the merged effective map)

---

## Inconsistencies / issues found (concrete)

### Fixed

1) Entities tab was effectively a no-op
   - Cause: entity title editing wasn’t used; UI titles come from `app.admin.frontend.settings.definitions/entity-title`.
   - Fix: removed the Entities tab entirely.

2) “Columns” policy UI lived under View Options (mixed responsibilities)
   - Cause: column visibility policy (`view-options.edn`) and structural column config (`table-columns.edn`) were edited in the same place.
   - Fix: extracted a dedicated `columns-policy-card` and moved it into the Table Columns tab.

3) Form Fields edits didn’t affect inline edit forms
   - Causes:
     - list row edit forms used the table spec (`entity-spec`) instead of the form spec.
     - admin settings updates didn’t propagate into `[:admin :config]` (runtime spec generation reads from there).
   - Fix: list rows now use edit-form spec; admin settings events sync `:form-fields` + `:table-columns` into `[:admin :config]`.

4) Role-gated list buttons disappeared
   - Cause: user pages were overriding `:show-edit?` / `:show-delete?` to false based on role (hiding instead of disabling).
   - Fix: list-view now supports `:disallowed-action-mode :disable` with `:allow-*?` flags; user pages updated to use disabled instead of hidden.

### Still open

5) (Resolved 2026-01-15) Admin settings showed removed “Expenses Admin” entities
   - Fix: Expenses domain no longer registers admin entities/groups in `src/app/domain/frontend/registry.cljs`.
   - Fix: backend no longer merges expenses domain-admin config into admin settings (`src/app/domain/backend/registry.clj`).
   - Fix: removed stale expenses entries from admin-owned `src/app/admin/frontend/config/view-options.edn`.
   - Note: removed unused `src/app/domain/frontend/expenses/admin/config/*` (2026-01-15).

6) Backend `display-setting-key?` does not include `:per-page`
   - File: `src/app/template/backend/routes/admin/settings.clj`
   - Frontend + specs treat `:per-page` as a display setting; backend routing logic does not.

7) Admin settings writes can persist merged (domain + admin) config into admin-owned files
   - Files: `src/app/template/backend/routes/admin/settings_io.clj`, `src/app/template/backend/routes/admin/settings.clj`

8) Rows-per-page “policy lock” is not enforced in the list settings UI
   - File: `src/app/template/frontend/components/settings/list_view_settings.cljs`

9) Settings shell edit-mode instructions don’t match persistence semantics
   - In admin scope, `form-fields` and `table-columns` are saved immediately via PATCH, while view-options is draft+Save.

---

## Plan (phased)

### Phase 0 — Done (2026-01-15)

- [x] Remove Entities tab
- [x] Move Columns policy UI into Table Columns tab
- [x] Make form-fields affect edit forms (incl. admin runtime sync)
- [x] Add visible-but-disabled role gating (`:disallowed-action-mode :disable`)

### Phase 1 — Correctness + clarity (small, low risk)

- [x] Remove stale “Expenses Admin” entities from admin scope (admin pages removed)
  - [x] Remove expenses admin groups/entities from frontend domain registry (`src/app/domain/frontend/registry.cljs`).
  - [x] Stop merging expenses domain-admin config into admin settings (`src/app/domain/backend/registry.clj`).
  - [x] Clean stale admin-owned `view-options.edn` keys for those entities (`src/app/admin/frontend/config/view-options.edn`).
  - [x] Delete unused `src/app/domain/frontend/expenses/admin/config/*`.

- [x] Fix `/admin/user-settings` route comments/text to reflect domain-owned config (not “per-user preferences”).
  - Files: `src/app/admin/frontend/routes.cljs`, `src/app/admin/frontend/pages/unified_settings/page.cljs`
- [x] Update settings shell copy to match real save behavior (draft vs immediate-save tabs).
  - File: `src/app/admin/frontend/components/settings_shell.cljs`
- [ ] Decide whether PATCH/DELETE endpoints for single-setting changes are still used; if they are:
  - [ ] Align backend “display key” detection with the canonical display keys (incl. `:per-page` only if we keep it as policy).

Acceptance:

- UI and code comments match reality; no user-facing behavior change.

### Phase 2 — Choose and enforce the `:per-page` model (simplify)

- [ ] Choose one:
  - [ ] Option A (recommended): remove `:per-page` from view-options policy (spec + editor UI + backend key routing), keep it as per-user list preference seeded from entity defaults.
  - [ ] Option B: fully support policy defaults/locks for per-page, including enforcement in list-view and settings panel.
- [ ] Update docs to reflect the chosen model (`docs/frontend/list-view-controls-configuration.md`).

Acceptance:

- There is a single, well-defined source of truth for rows-per-page.

### Phase 3 — Canonical key classification + shared mutation helpers

- [ ] Introduce shared helpers for tri-state mutations (pure functions) and reuse in:
  - `src/app/admin/frontend/events/settings/view_options.cljs`
  - `src/app/admin/frontend/events/user_settings/view_options.cljs`
- [ ] Replace scattered key checks with canonical key sets from `src/app/shared/specs/view_options.cljc`.

Acceptance:

- Admin and user settings draft behavior stays in sync by construction.

### Phase 4 — Fix admin overlay persistence (domain-admin merge safety)

- [ ] Decide on the desired ownership model for domain-admin config:
  - [ ] Keep domain-admin configs as base, and persist only admin overrides, or
  - [ ] Deprecate domain-admin configs and move everything into admin-owned config.
- [ ] Implement the chosen model so saving admin settings cannot silently “import” domain config and shadow it.

Acceptance:

- Admin saves only affect what the admin editor is responsible for.
- Domain-admin config changes still take effect unless explicitly overridden.

### Phase 5 — Tests + verification hooks

- [ ] Add/adjust focused tests around:
  - [ ] Form-fields edit/create spec selection for list edit forms.
  - [ ] `:disallowed-action-mode` behavior (hide vs disable) for add/edit/delete.
  - [ ] Canonical display key classification (incl. per-page decision).
  - [ ] Overlay write safety for merged admin configs.

Acceptance:

- Tests prevent the same class of drift from reappearing.
