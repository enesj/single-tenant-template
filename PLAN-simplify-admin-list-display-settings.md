# PLAN — Simplify Admin List View Display Settings

Date: 2025-12-13

This plan proposes a simpler, more explicit, and easier-to-reason-about system for **admin list view display settings** (the `:show-*?` toggles, column visibility, filter icons, table width, and rows-per-page) described in `ADMIN-LIST-VIEW-DISPLAY-SETTINGS.md`.

> Scope note: this started as a plan, but we’re using it as a living document. It now also tracks implementation progress and follow-up phases.

## URLs (current)

- Admin settings page: `http://localhost:8085/admin/admin-settings`
  - Legacy routes redirect to it:
    - `http://localhost:8085/admin/settings`
    - `http://localhost:8085/admin/amin-settings`
- Next: user settings page (to implement): `http://localhost:8085/admin/user-settings`

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

**Objective:** implement a user-facing settings page that reuses the same “draft + explicit Save/Discard” UX as admin settings, but persists user preferences (per-user) rather than org-wide policy.

**Config location:** user settings config (Expenses domain) lives under `src/app/domain/frontend/expenses/config`.

Work items:
- [ ] Add route + navigation entry for `/admin/user-settings`
- [ ] Implement a settings page with “Table columns” + “View options” tabs
- [ ] Use the same staged-edit model (draft in app-db + dirty tracking)
- [ ] Persist on explicit save only (no per-click saves)
- [ ] Keep user prefs storage in `ui-entity-prefs` (localStorage is OK for per-user prefs)
- [ ] Ensure no hard reload on save; page state retention is via in-memory state
- [ ] Add tests for draft/save/discard behavior and for persistence wiring

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

- Frontend tests: 219 tests, 1267 assertions, 0 failures.
- Backend tests: 126 tests, 514 assertions, 0 failures.

---

## Deliverables

- A simplified “settings resolution” module/hook used across list/table/actions.
- A backward-compatible policy schema that supports **defaults vs locks**.
- Unified persistence for column visibility and all list preferences.
- Updated docs replacing the old “multiple merge points” narrative with the new model.
- Admin settings “View options” UX parity with “Table columns” (draft + Save/Discard; no hard reload on save).
