# PLAN — Simplify Admin List View Display Settings

Date: 2025-12-13

This plan proposes a simpler, more explicit, and easier-to-reason-about system for **admin list view display settings** (the `:show-*?` toggles, column visibility, filter icons, table width, and rows-per-page) described in `ADMIN-LIST-VIEW-DISPLAY-SETTINGS.md`.

> Scope note: this started as a plan, but we’re using it as a living document. It now also tracks implementation progress and follow-up phases.

## URLs (current)

- Admin settings page: `http://localhost:8085/admin/admin-settings`
  - Legacy routes redirect to it:
    - `http://localhost:8085/admin/settings`
    - `http://localhost:8085/admin/amin-settings`
- User settings page: `http://localhost:8085/admin/user-settings` (implemented; unified settings UI)

---

## Goals

1. **One place to compute effective settings**
   - A single resolver produces the effective settings used by list/table/actions.
   - A single resolver also exposes which settings are **locked** (so UI can hide controls consistently).

2. **Separate “default” from “locked”**
   - Today, `view-options` uses **key presence** to mean “locked”, which blocks representing “default but user-changeable”.
   - New model must express:
     - default value (used when user has no preference)
     - lock (forced on/off; user cannot override)

3. **Unify persistence and restore**
   - Column visibility for admin entities currently uses a separate localStorage key (`column-visibility-<entity>`), distinct from `ui-entity-prefs`.
   - Target: all per-entity preferences (display toggles, column visibility, per-column filtering overrides, table width) share one persistence mechanism.

4. **Reduce merge points**
   - Today merges occur in multiple places (admin content renderer, `list-view`, some action components subscribe directly).
   - Target: one merge/resolution boundary per list page rendering path.

5. **Keep existing admin capabilities**
  - Admin Settings page (`/admin/admin-settings`) still supports organization-wide configuration.
   - Entity feature flags (e.g. read-only, batch ops) still reliably constrain UI.

---

## Non-goals (for the first pass)

- Redesigning the entire Admin Settings UI/UX.
- Reworking backend authorization or role/permission semantics.
- Large-scale schema changes in server persistence (unless required for “default vs lock”).

---

## Current behavior recap (from `ADMIN-LIST-VIEW-DISPLAY-SETTINGS.md`)

### Display toggles
- Single read API: `app.template.frontend.subs.ui/::entity-display-settings`.
- Precedence (high → low):
  1) hardcoded view-options from app-db (`[:admin :settings :view-options]` then `[:admin :config :view-options]`)
  2) user prefs `[:ui :entity-prefs <entity> :display ...]`
  3) legacy prefs
  4) `[:ui :defaults]`
  5) `[:ui]`
  6) fallback `default-display-settings`

### Hardcoded/locked rules
- A key is treated as locked when it is **present** in view-options (`contains?`).
- The settings panel hides controls when a key is present in the hardcoded map.

### Column visibility
- Admin entities (vector-config mode): visibility stored as a **vector** under admin config and persisted in **separate** localStorage key `column-visibility-<entity>`.
- Template/legacy entities: visibility stored as a **boolean map** under `ui-entity-prefs`.

### Why it feels complex
- Multiple layers of defaults.
- Multiple merge points.
- Different storage mechanism for admin columns.
- Panel exposes only a subset of settings.

---

## Business logic: what these settings *mean* (and what must remain true)

This section is the “domain rules” layer we should preserve while simplifying the plumbing.

### Entity types and their constraints
Admin list entities (examples): `:users`, `:admins`, `:audit-logs`, `:login-events`.

Entities can declare **feature constraints** (today via `entities.edn :features` and overlay logic in `effective-display-settings`):

- **Read-only entity**
  - Must not allow data-changing controls.
  - Effects on display toggles:
    - force `:show-add-button?` = false
    - force `:show-edit?` = false
    - force `:show-delete?` = false
  - UI implication:
    - settings panel should not offer these toggles (or show them as locked).

- **Batch operations disabled**
  - Batch controls should not be usable.
  - Effects on display toggles:
    - force `:show-select?` = false (or at least disable batch buttons)
    - force `:show-batch-edit?` = false
    - force `:show-batch-delete?` = false
  - UI implication:
    - selection UI exists primarily to enable batch actions; if batch actions are impossible, selection is usually pointless.

- **Deletion constraints enabled**
  - Deletes may be conditionally allowed/blocked.
  - UI implication:
    - delete buttons might remain visible, but behavior can differ (confirmations, disabled states, error messages). This is separate from display settings.

### Meaning of each display toggle
These toggles should be treated as *pure UI behavior flags* unless overridden by a policy lock.

- `:show-edit?`
  - Controls whether row-level edit affordances are visible.
  - Must be forced off for read-only entities.

- `:show-delete?`
  - Controls whether row-level delete affordances are visible.
  - Must be forced off for read-only entities.

- `:show-add-button?`
  - Controls whether the list header shows the “Add” CTA.
  - Must be forced off for read-only entities.

- `:show-select?`
  - Controls whether selection checkboxes render.
  - In admin lists, selection is mainly valuable for batch actions.

- `:show-batch-edit?`, `:show-batch-delete?`
  - Controls whether batch action buttons appear.
  - Typically requires `:show-select?` to be true and at least 2 selected rows.

- `:show-highlights?`
  - Purely cosmetic (row hover/updated highlighting). Safe as user preference.

- `:show-pagination?`
  - Controls whether pagination UI renders.
  - Runtime caveat: even when true, the UI may still hide pagination when `total-pages <= 1`.

- `:show-timestamps?`
  - Controls whether created/updated timestamp columns render.
  - These columns also interact with column visibility configuration (timestamp headers check `visible-columns`).

- `:show-filtering?`
  - Global master switch for rendering filter affordances.
  - Separate from per-column filter toggles:
    - global must be true
    - field must be filterable by config
    - user can still disable filter icon per column via `[:ui :entity-prefs <entity> :filters :fields <col>]`.

### Column-level rules

- **Available columns**
  - The set of columns an entity *can* render.

- **Always-visible columns**
  - Cannot be hidden by the user.

- **Default visible columns**
  - What a user sees before any personalization.

- **Ordering matters**
  - Admin column visibility is stored as a vector today to preserve order.

- **Per-column filtering override**
  - Even if a column is technically filterable, a user can hide filter affordances for that column.

### Persistence rules

- User preferences must be:
  - per-user (localStorage is acceptable in this app)
  - per-entity
  - stable across reloads

---

## Proposed simplified model (target state)

### 1) Introduce an explicit “policy vs preferences” structure

For each entity, we resolve effective settings from:

1. **Policy (organization / system constraints)**
   - supplies defaults
   - optionally locks values

2. **User preferences**
   - applied only if the key is not locked

3. **Fallback defaults**
   - in-code defaults for any key missing from policy and user prefs

The key design change: **locks and defaults are different concepts**.

### 2) Single resolver output (the only merge point)

Resolver returns a map like:

- `:effective` — the values used by UI components
- `:locked` — map of locked keys → locked value (or a set of locked keys)
- `:defaults` — resolved defaults (useful for “reset to default” UX)
- optional `:sources` — for debugging (where each effective value came from)

### 3) Configuration semantics

We should support two adjacent concepts:

- **Organization default**: “We generally prefer highlights off, but users can enable it.”
- **Organization lock**: “Selection must be on for this list because workflows depend on it.”

This cannot be represented with the current “presence = locked” behavior.

### 4) Unified persistence for all per-entity preferences

Store everything under `ui-entity-prefs`:

- `[:ui :entity-prefs <entity> :display ...]` — display toggles
- `[:ui :entity-prefs <entity> :columns :visible-order]` — vector of visible columns (ordered)
- `[:ui :entity-prefs <entity> :columns :width]` — table width
- `[:ui :entity-prefs <entity> :filters :fields]` — per-column filter icon toggles

Then derive the boolean map for rendering as needed.

---

## Migration strategy (phased, low risk)

### Phase 1 — Centralize resolution (behavior-preserving)

**Objective:** keep current semantics, but remove duplicate merge points by routing everything through one resolver.

Work items:
- Create a single resolver/hook used by:
  - `list-view`
  - action renderers
  - settings panel
- Ensure the resolver still respects existing precedence.
- Make “hardcoded map” for the panel come from the resolver output (not a parallel subscription).

Acceptance criteria:
- No visible behavior changes across admin list pages.
- Settings panel hides the same locked toggles as before.

Risks:
- Components currently subscribe directly (e.g. enhanced actions). They must either:
  - receive resolved settings via props, or
  - share the same resolver/hook to guarantee consistency.

### Phase 2 — Add explicit defaults vs locks in admin policy

**Objective:** enable “default but user-changeable” at the organization/admin config level.

Proposed policy representation (conceptual, not final API):
- `:display-defaults` — map
- `:display-locks` — map or set

Backward compatibility:
- Existing `view-options` flat map can be interpreted as `:display-locks` initially.

Acceptance criteria:
- We can set a default without hiding the user toggle.
- We can lock a value and reliably prevent user override.

### Phase 3 — Unify admin column visibility persistence

**Objective:** remove `column-visibility-<entity>` as a separate persistence system.

Work items:
- On bootstrap (or first use per entity), migrate:
  - read `column-visibility-<entity>` if present
  - write into unified `ui-entity-prefs` path
  - optionally delete the old key after a successful write
- Update admin column toggle event(s) to write to unified prefs.
- Ensure always-visible columns remain always-visible.

Acceptance criteria:
- Admin column visibility persists and restores after reload.
- Only one persistence key is required for user prefs (`ui-entity-prefs`).

### Phase 4 — Align settings panel with the full model

**Objective:** resolve “settings exist but no toggle” mismatches.

Decide explicitly for each setting:
- user-toggleable in panel
- policy-only (admin decides, users can’t)
- removed from the model

Candidates currently missing in panel:
- `:show-timestamps?` (toggle exists)
- `:show-filtering?` (exists; missing toggle event)
- `:show-add-button?` (exists)
- `:show-batch-edit?` / `:show-batch-delete?` (exist)

Acceptance criteria:
- No “ghost settings” (either they’re user controllable, or clearly policy-only).

### Phase 5 — Remove legacy layers / simplify inputs

**Objective:** delete the old paths and redundant defaults once migration is stable.

Work items:
- Remove legacy prefs reads.
- Reduce overlapping defaults (`[:ui :defaults]` etc.) if they are only serving historical compatibility.
- Collapse redundant merges (admin renderer → list-view → subscription).

Acceptance criteria:
- The effective settings story is explainable in one paragraph.
- The code path for “where did this value come from?” is trivial.

---

## Design decisions (RESOLVED)

1. **Where do organization defaults live?**
   - ✅ **Option A**: keep in `view-options.edn` (expanded to support defaults + locks).

2. **Should entity feature constraints become locks?**
   - ✅ **Yes**: read-only and batch-disabled will produce locked values (not ad-hoc overlays).

3. **Do we keep "controls" preferences?**
   - ✅ **Remove**: no UI exists for these; will be removed from the model.

4. **Column configuration source-of-truth**
   - ✅ **Do NOT use** `resources/db/*/models.edn :admin` metadata.
   - Keep `table-columns.edn` as the source of truth for column config.

---

## Implementation Progress

### Phase 1 — Centralize resolution
- [x] Create unified resolver hook → `src/app/template/frontend/settings/resolver.cljs`
- [x] Create UI subscriptions using resolver → `::resolved-display-settings`, `::locked-display-settings`
- [x] Action renderers use `::entity-display-settings` → already uses resolver via subscription chain
- [x] Table uses `::hardcoded-view-options` → now derives from `::locked-display-settings`
- [x] Settings panel receives locks via `hardcoded-display-settings` prop
- [x] List-view merges subscribed settings from resolver with props
- [ ] (Future cleanup) Remove duplicate merge logic in `content.cljs` → can be simplified in Phase 5

**Status:** Phase 1 complete. All display settings now flow through the resolver subscription chain.

### Phase 2 — Add explicit defaults vs locks
- [x] Resolver supports new schema (`:display-defaults`, `:display-locks`)
- [x] Feature constraints converted to locks in resolver (`feature-constraints->locks`)
- [x] Backward compatible: old "presence = locked" schema still works
- [ ] (Optional) Migrate existing view-options.edn to new schema

**Status:** Phase 2 complete. Resolver supports both old and new schemas.

**New schema example:**
```clojure
:users
{:display-defaults {:show-batch-edit? true
                    :show-pagination? true}
 :display-locks {:show-delete? false}  ; user cannot toggle this
 :search-fields [:email :first-name :last-name]
 ...}
```

### Phase 3 — Unify column visibility persistence
- [x] Add migration logic for old localStorage keys → `persistence.cljs` migration utilities
- [x] Update admin column toggle to use unified prefs → `config.cljs` events now persist to `ui-entity-prefs`
- [x] Test restore on reload → migration triggers on admin config load

**Status:** Phase 3 complete. Admin column visibility now uses unified `ui-entity-prefs` persistence:
- Migration from `column-visibility-<entity>` localStorage keys to unified prefs
- `::toggle-column-visibility` writes to both admin config (runtime) and `ui-entity-prefs` (persistence)
- `::load-saved-column-config` reads from unified prefs first, falls back to legacy
- Legacy localStorage keys are deprecated but still read during migration

**Files changed:**
- `src/app/template/frontend/interceptors/persistence.cljs` — Added migration utilities
- `src/app/admin/frontend/events/config.cljs` — Updated toggle/reorder/reset events

### Phase 4 — Align settings panel with model
- [x] Remove `:controls` from settings panel → panel now uses locked settings from resolver
- [x] Add all missing toggles to settings panel → timestamps, filtering, add button, batch edit/delete

**Status:** Phase 4 complete. Settings panel now includes all display toggles:
- Edit, Delete, Highlights, Selection, Pagination (existing)
- Timestamps, Filtering, Add Button, Batch Edit, Batch Delete (newly added)

**Files changed:**
- `src/app/template/frontend/events/list/ui_state.cljs` — Added `toggle-filtering`, `toggle-add-button`, `toggle-batch-edit`, `toggle-batch-delete`
- `src/app/template/frontend/components/settings/list_view_settings.cljs` — Added UI toggles for all settings

### Notes from implementation (phases 1–5)

- **Locked values must preserve explicit `false`:** when resolving “locked” view-options, we had to treat *key presence* (`contains?`) as the lock signal so that a locked value of `false` is not accidentally dropped by `or`-style fallback logic.
- **Column ids can be keywords or strings:** admin config and UI runtime sometimes represented column ids differently (e.g. `"created_at"` vs `:created_at`). We normalized header/filter/sort checks to avoid “filter icon missing” / “sort mismatch” issues.
- **Timestamp headers required special handling:** the “timestamps” header sentinel needed the same normalization treatment so it consistently participates in visible-columns/filtering logic.
- **Vector-config mode is gated behind config load:** we ensured vector-config behaviors only activate once admin config is actually loaded, preventing regressions on non-admin/template pages.

### Phase 5 — Remove legacy layers
- [x] Legacy prefs still read for backward compatibility (not fully removed)
- [x] Legacy localStorage events marked as deprecated

## Recent updates (Dec 13)

- Added `src/app/shared/specs/view_options.cljc` with a Malli schema for both admin and domain `view-options.edn` files, a helper to detect nested `:display-locks`, and helper functions (`validate-view-options` / `validate-view-options-strict`) used by backend readers/writers.
- Updated `src/app/template/backend/routes/admin/settings.clj` to validate view-options during read + write for both admin and user files and to log/throw when schema or consistency issues occur, preventing invalid configs from being persisted.
- Corrected `src/app/admin/frontend/config/view-options.edn` so the `:login-events :filters :success` options are strings (`["true" "false"]`), matching the new Malli schema, then reran (and documented) the validation command to confirm both files pass.
- Verified stability by running `bb be-test` (126 backend tests, 0 failures) and `npm run test:cljs` (225 frontend tests, 0 failures) before finalizing the guards.
- [ ] (Future) Full removal once migration is stable

**Status:** Phase 5 partially complete. Legacy paths remain for backward compatibility during migration period.

### Phase 6 — Admin settings UX parity (view-options)

**Objective:** make “View options” behave like “Table columns” in the admin settings page: stage changes locally, require an explicit save, and avoid hard reload/state loss (without localStorage or any other persistent UI-state storage).

- [x] Stage view-options edits in a draft (no per-click save)
- [x] Add explicit “Save settings” + “Discard changes” actions
- [x] Keep `:admin :config :view-options` synced so list pages update immediately after a successful save
- [x] Remove settings page UI-state persistence (no `localStorage` for settings tabs/state)
- [x] Reduce dev reload triggers on save (avoid shadow compile-time inlining of mutable EDNs; ignore those EDN writes in backend watcher)
- [x] Tests green (FE + BE)

**Status:** Phase 6 complete.

**Key behavioral changes:**
- `/admin/admin-settings` → “View options” now edits a **draft**; nothing is persisted until the user clicks **Save settings**.
- Leaving edit mode discards staged view-options changes.
- Saving view-options uses a single PUT to `/admin/api/settings` (full settings map update).

**Files changed (high signal):**
- `src/app/admin/frontend/pages/settings.cljs` — view-options now uses draft + Save/Discard (no auto-save)
- `src/app/admin/frontend/events/settings.cljs` — new draft/save events + dirty tracking; removed settings UI localStorage
- `src/app/admin/frontend/config/preload.cljs` — don’t inline mutable admin settings EDNs (prevents shadow-triggered full reloads)
- `dev/system/watchers.clj` — ignore admin settings EDN writes so dev system doesn’t restart on save
- `src/app/admin/frontend/events/config.cljs` — refined `:admin/config-loaded?`/`:admin/config-loading?` bookkeeping (used for gating)

---

## Known Issues

### Resolved
- Vector-config mode is now gated by `:admin/config-loaded?` and the related tests are passing.

### Open / follow-up
- Manual UX check in a running dev session: confirm that saving view-options in `/admin/admin-settings` no longer causes a full page reload and that the active tab/state is preserved purely via in-memory app-db.

  (This is intentionally achieved by *preventing reload triggers*, not by persisting UI state.)

---

## Next phase — apply the same pattern to user pages

### Phase 7 — User settings page (`/admin/user-settings`)

**Objective (implemented):** implement an **admin** page that edits the **domain-owned, user-facing defaults** (Expenses domain) using the same “draft + explicit Save/Discard” UX as admin settings.

This page is intentionally **not** an editor for admin-only entities (Users/Admins/etc). It is an editor for the config that powers user-facing pages like `/expenses/*`.

**Config location (source-of-truth):**

- Domain-owned UI config lives under `src/app/domain/frontend/expenses/config/`
  - `entities.edn`
  - `view-options.edn` (domain schema: `:display-defaults` + `:display-locks`)
  - `table-columns.edn`
  - `form-fields.edn`

**Architecture (current):**

- User routes read config from app-db `[:domain :config ...]`.
  - Preloaded at boot from inline resources:
    - `src/app/domain/frontend/expenses/config/preload.cljs`
    - `src/app/template/frontend/events/bootstrap.cljs`
- Display settings are resolved via the unified resolver:
  - `src/app/template/frontend/settings/resolver.cljs`
  - `src/app/template/frontend/subs/ui.cljs` selects config source based on route:
    - admin routes → `[:admin :config]`
    - user routes → `[:domain :config]`
- Admin `/admin/user-settings` loads/saves the **domain** config via an admin backend API:
  - GET/PUT `/admin/api/settings/user-ui-config` (reads/writes those EDN files)

**Implementation status:** ✅ done

- [x] Add route + navigation entry for `/admin/user-settings` (sidebar link at bottom, after “Admin Settings”)
- [x] Implement settings page with “View options” + “Table columns”
- [x] Stage edits in a draft + dirty tracking + explicit Save/Discard
- [x] Load/save via admin API that persists to `src/app/domain/frontend/expenses/config/*`
- [x] Keep scope limited to domain entities (currently only `:expenses` exists in domain config)
- [x] Fix rendering bug (admin layout requires children via `:children`)
- [x] Frontend tests green

**Key files added/changed (high signal):**

- `src/app/admin/frontend/pages/unified_settings.cljs` — unified settings page used by both `/admin/admin-settings` and `/admin/user-settings`
- `src/app/admin/frontend/events/unified_settings.cljs` — unified state management (draft + load/save)
- `src/app/template/backend/routes/admin/settings.clj` — GET/PUT `/admin/api/settings/user-ui-config`
- `src/app/admin/frontend/components/layout.cljs` — sidebar link placement
- `src/app/template/frontend/subs/ui.cljs` — route-aware config source selection (admin vs domain)

**Resolved regression (2025-12-14):** `/admin/user-settings` toggles were unclickable because `:editing?` was not passed to `user-entity-settings-card` in `user-entity-editor`.

**Important behavior note (why “defaults not applied” can happen):**

- User-facing pages also have **per-user preferences** stored in `ui-entity-prefs` (localStorage).
- The resolver applies **user prefs** on top of domain defaults when the setting is not locked.
- Domain `view-options.edn` uses the explicit schema:
  - `:display-defaults` for defaults
  - `:display-locks` for forced/immutable values
  - So a user preference like `[:ui :entity-prefs :expenses :display :show-select? true]` can override a default `false` only when that key is not locked.

**Follow-up (next session candidates):**

1) Review the current domain-owned defaults/locks split:
   - confirm which keys should be defaults vs locked for user-facing pages.

2) Add a “Reset to org defaults” action that clears user prefs for an entity (so defaults are visible immediately).

3) If the goal is “org defaults apply immediately without recompiling”: consider loading domain config from the server at runtime (rather than `shadow.resource/inline`), using the same config files as the source-of-truth.

---

## Test plan (when we implement)

### Automated (CLJS)
- Unit tests for the resolver:
  - lock overrides user prefs
  - user prefs override defaults
  - missing values fall back correctly
  - feature constraints produce expected locks
- Persistence migration tests:
  - migrate old `column-visibility-<entity>` into unified prefs
  - ensure always-visible columns remain visible

### Manual smoke checks (admin UI)
- `/admin/users`:
  - toggle highlights/select/pagination/edit/delete
  - confirm persistence across refresh
- `/admin/audit-logs`:
  - confirm any entity constraints still apply
- Column visibility:
  - hide/show columns, refresh, verify restore
  - verify always-visible columns cannot be hidden
- Filtering:
  - global filtering on/off + per-column filter icon toggles

---

## Verification (completed)

- Frontend tests (latest): 228 tests, 1300 assertions, 0 failures. (2025-12-14)
- Backend tests: not re-run in this session (previously green).

---

## Deliverables

- A simplified “settings resolution” module/hook used across list/table/actions.
- A backward-compatible policy schema that supports **defaults vs locks**.
- Unified persistence for column visibility and all list preferences.
- Updated docs replacing the old “multiple merge points” narrative with the new model.
- Admin settings “View options” UX parity with “Table columns” (draft + Save/Discard; no hard reload on save).

---

## Completed follow-up — Refactor `/admin/admin-settings` + `/admin/user-settings` UI (parity + full coverage)

This is now implemented. See `PLAN-settings-ui-parity.md` for the detailed implementation plan and current status.

### Goal

Refactor both settings pages so they share **exactly the same UI and UIX component structure**, aligned with this plan and the current implemented behavior.

### Requirements

1) **UI parity**
   - `/admin/admin-settings` and `/admin/user-settings` must look and behave the same.
   - Prefer one shared UIX “settings shell” component used by both routes.

2) **Show “all possible settings” in edit mode (even if missing from EDN)**
   - In **edit mode**, the UI must render the full set of supported settings controls, regardless of whether the key currently exists in the underlying EDN file(s).
   - “All possible settings” should be derived from the *model we support today* (this doc + resolver/table implementation), not just from what happens to be configured.

3) **Single-scope editing + switchable scope**
  - In **edit mode**, show settings for **only one scope at a time**.
  - Provide a way to switch the scope being edited (Admin vs User) without leaving edit mode.
  - In **edit mode**, also show settings for **only one page (entity)** at a time (e.g. `:users` or `:expenses`).
  - Provide a way to switch the page/entity being edited without leaving edit mode.

4) **View (read-only) mode shows all pages (entities), but only implemented settings**
  - When **not editing**, show an overview for **both** scopes (Admin Settings + User Settings) that includes **all pages/entities** in each scope (e.g. `:admins`, `:users`, `:audit-logs`, `:login-events`, `:expenses`, etc.).
  - Only render settings that are actually implemented/persisted (i.e. present in the loaded config for that scope/entity).
  - Practical meaning: from either route (`/admin/admin-settings` *or* `/admin/user-settings`), view mode acts as a consolidated “settings overview” for the whole system.

5) **No functional changes in this phase**
   - This is a refactor and UI unification effort. Preserve existing save semantics (draft + Save/Discard), APIs, and resolver behavior.

6) **Prevent hard reload after Save (both pages)**
  - Saving settings must not trigger a full reload/restart (same UX guarantee already implemented for admin settings).
  - This must be ensured for both:
    - saving admin settings (`/admin/admin-settings`)
    - saving user/domain settings (`/admin/user-settings`)

### Definitions / scopes

- **Admin scope**: data loaded from `[:admin :settings]` / `[:admin :config]` and persisted via the existing admin settings API (currently `/admin/api/settings`).
- **User scope**: data loaded from `[:admin :user-settings]` (or equivalent) and persisted via `/admin/api/settings/user-ui-config` (domain-owned config under `src/app/domain/frontend/expenses/config/`).

### Proposed UX

- **Top-level mode toggle**: `View` / `Edit` (or a single “Edit settings” button that enters edit mode).

- **View mode** (default):
  - Render a consolidated overview (same content from either route) grouped by scope:
    - “Admin settings” (shows all admin entities/pages, but only keys present per entity)
    - “User settings” (shows all user/domain entities/pages, but only keys present per entity)
  - Each entity/page shows the currently implemented settings (e.g. display toggles, pagination config, filters config, etc.) as a readable summary.
  - Optional: include a “source file” hint per scope (admin `view-options.edn` vs domain `view-options.edn`) and/or per entity.

- **Edit mode**:
  - Render a **scope switcher** (Admin ↔ User).
  - Render a **page/entity switcher** within the selected scope (e.g. `:users` / `:admins` / `:login-events` or `:expenses`).
  - Render **only the selected page/entity form**.
  - The form shows **all supported settings controls**, even if missing from EDN.
    - For missing keys, show sensible defaults (derived from resolver defaults / current UI defaults).
    - Clearly indicate “Not configured yet” vs “Configured”, if helpful.
  - Maintain the existing draft UX:
    - staged edits
    - explicit **Save** and **Discard**
    - dirty indicator

### Implementation approach (plan)

1) **Introduce a shared “settings definitions” registry**
   - A single data structure describing:
     - setting key
     - label/help text
     - control type (toggle/select/number/list)
     - how to read the current value from a scope config
     - how to write updates into the scope draft
     - default value (when missing from EDN)
   - Source for “all possible settings” should be the *supported model*:
     - display toggle keys (see `display-toggle-keys`)
     - pagination defaults
     - any other settings currently supported by resolver + settings UI (per this doc)

2) **Build one UIX “SettingsShell” component**
   - Responsibilities:
     - mode state (view/edit)
     - scope switcher (in edit)
     - Save/Discard wiring (existing events)
     - layout + shared styling
   - Both routes should only provide:
     - page title
     - which scopes are available
     - how to load/save that scope (existing events)

3) **Implement “View mode overview” rendering**
  - Render an overview for all entities/pages in both scopes.
  - For each entity/page, render only settings that are present in the underlying config.
  - Use the same definitions registry but with a “present-only” filter.

4) **Implement “Edit mode full coverage” rendering**
   - Render all settings from the definitions registry.
   - Backfill missing keys with defaults for display.

5) **Edit-mode scope switching behavior**
   - Decide and document one of:
     - (Preferred) keep separate drafts per scope and preserve unsaved edits when switching, OR
     - prompt the user if switching would discard changes.
   - Acceptance criteria must include “no accidental silent loss of edits”.

6) **Extend “no hard reload on save” to user settings**
  - Apply the same approach already used for admin settings to the user settings save path.
  - Ensure saving domain EDNs does not trigger shadow compile / dev system restart.
  - Ensure the UI updates by syncing app-db config (post-save) rather than relying on recompilation.

### Acceptance criteria

- Visual/UI parity between `/admin/admin-settings` and `/admin/user-settings` (same layout, same components).
- View mode (from either route) shows two sections (Admin + User) and covers all entities/pages, listing only settings present in each scope/entity config.
- Edit mode:
  - shows one scope at a time
  - has a scope switcher
  - shows one entity/page at a time
  - has an entity/page switcher
  - renders the full set of supported settings controls even if keys are missing from EDN
  - Save/Discard works as today
- Saving does not trigger a hard reload/restart for either admin settings or user settings.
- No backend API changes required.
- Frontend tests updated/added as needed; existing tests remain green.

### Test plan (for the refactor)

- CLJS unit tests:
  - settings definitions registry contains expected keys
  - “present-only” filtering works for view mode
  - defaults are applied for missing keys in edit mode

- Manual browser checks:
  - `/admin/admin-settings` view mode shows admin + user sections and includes all entities/pages (implemented settings only)
  - `/admin/user-settings` view mode shows admin + user sections and includes all entities/pages (implemented settings only)
  - entering edit mode, switching scope and entity/page, Save/Discard behavior
  - no full reload on save (admin + user); UI state remains stable
