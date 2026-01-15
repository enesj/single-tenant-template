# PLAN — Simplify `/admin/admin-settings` + `/admin/user-settings` (list-view “hardcoded” policy)
Date: 2026-01-15

This is an audit + improvement plan for the two settings pages that manage **list-view policy** (defaults/locks) and related config for:

- Admin scope: `/admin/admin-settings` → `src/app/admin/frontend/config/*`
- User scope (domain-owned): `/admin/user-settings` → `src/app/domain/**/config/*`

It focuses on simplifying the “hardcoded” list-view settings (i.e., **policy locks**) and eliminating drift/inconsistencies between backend, frontend editor UI, and runtime resolver behavior.

Related references already in repo:

- `PLAN-settings-ui-parity.md` (UI parity refactor; marked complete)
- `docs/frontend/list-view-controls-configuration.md` (current intended behavior)
- `docs/frontend/admin-settings.md` (architecture overview)

---

## Current implementation (how it works today)

### 1) Pages / UI

- Both routes use the unified UIX page:
  - `src/app/admin/frontend/pages/unified_settings/page.cljs`
  - Shell/layout: `src/app/admin/frontend/components/settings_shell.cljs`
  - Editors/cards/rows: `src/app/admin/frontend/pages/unified_settings/editors.cljs`,
    `src/app/admin/frontend/components/settings_views/*`

### 2) Backend persistence

- Admin settings (admin-owned config):
  - GET `"/admin/api/settings"` → `src/app/template/backend/routes/admin/settings.clj` `get-view-options-handler`
    - Reads via `src/app/template/backend/routes/admin/settings_io.clj` `read-view-options`
    - **Merges** domain-admin config + admin-owned config (admin “wins”).
  - PUT `"/admin/api/settings"` → `update-view-options-handler`
    - Writes via `write-view-options!` to `src/app/admin/frontend/config/view-options.edn`

- User settings (domain-owned user UI config):
  - GET/PUT `"/admin/api/settings/user-ui-config"` → `get-user-ui-config-handler` / `update-user-ui-config-handler`
    - Reads/writes domain-owned EDNs (currently via `domain-registry/primary-user-ui-config-paths`).

### 3) Runtime behavior in list-view

- Display-toggle “hardcoded” policy (locks) is enforced at runtime by:
  - Resolver: `src/app/template/frontend/settings/resolver.cljs`
  - UI subs: `src/app/template/frontend/subs/ui.cljs` `::locked-display-settings`
- List settings panel hides toggle controls when a key is locked:
  - `src/app/template/frontend/components/settings/list_view_settings.cljs`

---

## Key simplification opportunities (recommended direction)

### A) Decide what *policy* controls vs what remains *per-user preference*

Right now the codebase mixes these concepts for some settings (notably `:per-page`).

Recommendation: **restrict `view-options.edn` to things the resolver + UI actually enforce as policy**:

- Display toggles (show/edit/delete/select/filtering/etc.)
- Column visibility policy (`:column-defaults` / `:column-locks`)

For anything else (notably **rows-per-page**), pick *one* model and remove the other:

Option A (simpler): treat rows-per-page as **per-user list UI state only**, seeded by entity defaults (e.g., from `entities.edn`), and **remove `:per-page` from `view-options.edn` + settings editors**.

Option B (more powerful): treat rows-per-page as a **policy-controlled display setting**, fully lockable, and integrate it into:

- where per-user prefs are stored
- how `list-view` derives effective per-page
- how the settings panel hides/disables per-page when locked

Pick one and remove the half-supported hybrid.

### B) Make “display setting key detection” canonical and shared

There are currently multiple independent implementations of “is this a display setting key?” (backend + frontend), which is already drifting.

Recommendation:

- Introduce a single canonical “display keys” source based on `src/app/shared/specs/view_options.cljc` (and an explicit decision about `:per-page`).
- Use it in:
  - backend routes (PATCH/DELETE behavior)
  - admin settings events (draft updates)
  - user settings events (draft updates)
  - settings panel hide/disable logic (if needed)

### C) Remove duplicated tri-state mutation logic (admin vs user settings)

The admin settings and user settings event namespaces duplicate the same tri-state “inherit/default/lock” mutations for:

- `:display-defaults` / `:display-locks`
- `:column-defaults` / `:column-locks`

Recommendation: extract shared pure helpers (functions that take a config map and return an updated config map) and reuse in both scopes.

### D) Fix the admin “merge + write” layering problem (domain-admin configs)

Today the admin config read path **merges** domain-admin config + admin-owned config, but the write path persists whatever the UI sends back to the admin-owned file.

This can accidentally “import” domain config into admin-owned files and then silently shadow future domain changes.

Recommendation: preserve layering explicitly:

- Read: `effective = merge(domain-base, admin-overrides)`
- Write: update **only** admin overrides (not the merged effective map)

This likely requires either:

- computing and persisting only the overrides (diff) on save, or
- splitting API endpoints/semantics so the UI edits the overlay explicitly.

---

## Inconsistencies / issues found (concrete)

1) **Backend `display-setting-key?` does not include `:per-page`**
   - File: `src/app/template/backend/routes/admin/settings.clj`
   - Frontend + spec treat `:per-page` as a display setting; backend PATCH/DELETE logic does not.
   - Outcome: PATCH/DELETE semantics can write/remove per-page in the wrong place (or not at all), depending on how it’s called.

2) **Admin settings writes can persist merged (domain + admin) config into admin-owned files**
   - Files: `src/app/template/backend/routes/admin/settings_io.clj`, `src/app/template/backend/routes/admin/settings.clj`
   - Outcome: domain-admin config can become shadowed by admin-owned config unintentionally.

3) **Rows-per-page “policy lock” is not enforced in the list settings UI**
   - File: `src/app/template/frontend/components/settings/list_view_settings.cljs`
   - Outcome: even if `:per-page` is locked, the user can still change per-page in the settings panel.

4) **Rows-per-page is not part of the same per-user prefs system as other display settings**
   - Files: `src/app/template/frontend/events/list/ui_state.cljs`, `src/app/template/frontend/settings/resolver.cljs`
   - Outcome: `:per-page` lives in list UI state paths, not `[:ui :entity-prefs <entity> :display]`, so resolver locks/defaults won’t reliably control it after initial seeding.

5) **Settings shell “edit mode instructions” don’t match persistence semantics**
   - File: `src/app/admin/frontend/components/settings_shell.cljs`
   - In admin scope, `form-fields` and `table-columns` are saved immediately via PATCH; instructions still reference “Save changes”.

6) **Route comment drift**
   - File: `src/app/admin/frontend/routes.cljs`
   - `/admin/user-settings` comment currently describes “per-user preferences”, but it edits domain-owned config.

---

## Plan (phased)

### Phase 1 — Correctness + clarity (small, low risk)

- [ ] Fix the `/admin/user-settings` route comment to reflect domain-owned config.
- [ ] Update edit-mode instructions to match real save behavior (draft vs immediate-save tabs).
- [ ] Decide whether PATCH/DELETE endpoints for single-setting changes are still used; if they are:
  - [ ] Align backend “display key” detection with the canonical display keys (incl. `:per-page` only if we keep it as policy).

Acceptance:

- No user-facing behavior change, but the UI and code comments match reality.

### Phase 2 — Choose and enforce the `:per-page` model (simplify)

- [ ] Choose one:
  - [ ] **Option A (recommended)**: remove `:per-page` from view-options policy (spec + settings UI + any backend key routing), keep it as a per-user list preference seeded from entity defaults.
  - [ ] Option B: fully support policy defaults/locks for per-page, including enforcement in list-view and settings panel.
- [ ] Update docs to reflect the chosen model (`docs/frontend/list-view-controls-configuration.md`).

Acceptance:

- There is a single, well-defined source of truth for rows-per-page.
- If policy locking is supported for per-page, it is actually enforced end-to-end; otherwise, it is not offered in policy UI/spec.

### Phase 3 — Canonical key classification + shared mutation helpers

- [ ] Introduce shared helpers for tri-state mutations (pure functions) and reuse in:
  - `src/app/admin/frontend/events/settings/view_options.cljs`
  - `src/app/admin/frontend/events/user_settings/view_options.cljs`
- [ ] Replace scattered regex key checks with canonical key sets from `src/app/shared/specs/view_options.cljc` (plus any deliberate extras).

Acceptance:

- No more duplicated “display key” detection logic across scopes.
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
  - [ ] canonical display key classification (incl. per-page decision)
  - [ ] per-page lock/default behavior (if supported)
  - [ ] overlay write safety for merged admin configs
- [ ] Run fast checks:
  - `bb validate-frontend-config`
  - `bb config-audit --strict`
  - plus the most relevant CLJS test subset.

Acceptance:

- Tests prevent the same class of drift from reappearing.

