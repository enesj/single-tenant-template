# Admin config EDN usage report

This report audits runtime usage of these admin config files:

- `src/app/admin/frontend/config/entities.edn`
- `src/app/admin/frontend/config/form-fields.edn`
- `src/app/admin/frontend/config/table-columns.edn`

It focuses on **what the current code actually consumes**, and calls out config that is **schema/documentation-only**, **settings-UI-only**, or **currently ignored**.

Related docs:
- `docs/frontend/admin-settings.md`

---

## How these EDNs are loaded

### `entities.edn` (inlined at build time)

- Inlined via `shadow.resource/inline` in `src/app/admin/frontend/config/preload.cljs`.
- Immediately merged with `src/app/admin/frontend/system/entity_registry.cljs` (the “callable functions” registry):
  - `:adapter-init-fn` is overwritten with `:init-fn`.
  - `:components {:actions :custom-actions :modals}` may be overwritten with registry values.
- Stored into `app.admin.frontend.system.entity-registry/registered-entities`.
- Read via subscriptions in `src/app/admin/frontend/subs/config.cljs`:
  - `:admin/all-entity-configs`
  - `:admin/entity-config`

### `form-fields.edn`, `table-columns.edn` (loaded at runtime)

- Fetched from backend settings routes via `src/app/admin/frontend/config/loader.cljs`.
- Cached in `app.admin.frontend.config.loader/config-cache`.
- Copied into Re-frame DB under `[:admin :config ...]` by `:admin/load-ui-configs` in `src/app/admin/frontend/events/config.cljs`.
- Edited via admin UI at `/admin/settings` (see `src/app/admin/frontend/pages/settings.cljs`) using `src/app/admin/frontend/events/settings.cljs`.

---

## `src/app/admin/frontend/config/entities.edn`

### Shape

Top-level map keyed by entity keyword (e.g. `:users`). Each value is an entity config map.

### Key usage

| Key | Status | Consumed by |
|---|---|---|
| entity key (top-level) | used | `:admin/entity-config` + page routing/selection |
| `:entity-key` | used | `src/app/admin/frontend/renderers/actions.cljs` (prop key decisions), `src/app/admin/frontend/renderers/content.cljs` (titles/logging) |
| `:page-title` | used | `src/app/admin/frontend/components/admin_page_wrapper.cljs` (page header), `src/app/admin/frontend/renderers/content.cljs` (list title) |
| `:page-description` | used | `src/app/admin/frontend/components/admin_page_wrapper.cljs` |
| `:adapter-init-fn` | used, but EDN value is not used | `admin_page_wrapper.cljs` calls it, but the value comes from `entity_registry.cljs` via `config/preload.cljs` merge |
| `:display-settings` | used (as defaults) | `src/app/admin/frontend/renderers/content.cljs` → passed as `:display-settings` into `src/app/template/frontend/components/list.cljs` |
| `:features` | partially used | `renderers/content.cljs`, `handlers/generic.cljs`, `renderers/actions.cljs`, `components/generic_admin_entity_page.cljs` |
| `:components` | used, with caveats | `renderers/actions.cljs`, `components/generic_admin_entity_page.cljs` (modals/custom-header), `renderers/content.cljs` (list overrides) |
| `:custom-content` | partially used | `renderers/content.cljs` currently reads `:active-filters-display` only |

### Important “gotchas” / currently-unused pieces

- `:display-settings {:per-page ...}` **does affect** list pagination defaults.
  - `src/app/template/frontend/components/list.cljs` reads `:per-page` from either the top-level prop (`:per-page`, as passed by admin pages) or `:display-settings :per-page`.
  - If list UI state does not already have a per-page value, `list-view` seeds it by dispatching `::ui-events/set-per-page` once.
  - User changes (via the rows-per-page dropdown) still win because UI state is treated as the source of truth after initialization.
- `:features` keys that are **currently not referenced** anywhere in admin UI logic:
  - `:exportable?`
  - `:activity-tracking?`
- `:custom-content` keys other than `:active-filters-display` are currently **not used** by the generic pipeline.
  - Example: `{:export-controls true}` is not read anywhere in the generic renderers.
- `:components` values in EDN are **data (symbols)** and are generally **not callable**.
  - For `:actions`, `:custom-actions`, `:modals`, the runtime values typically come from `src/app/admin/frontend/system/entity_registry.cljs` via the merge in `src/app/admin/frontend/config/preload.cljs`.
  - EDN-only component keys not provided by the registry (e.g. `:custom-header`, `:list` overrides) remain effective.

### Feature flags actually enforced

| Feature flag | Behavior |
|---|---|
| `:batch-operations?` | when false, `renderers/content.cljs` forces `:show-select?`, `:show-batch-edit?`, `:show-batch-delete?` off; `generic_admin_entity_page.cljs` hides selection counter |
| `:read-only?` | when true, `renderers/content.cljs` forces `:show-add-button?`, `:show-delete?`, `:show-edit?` off |
| `:security-wrapper?` | when true, `handlers/generic.cljs` runs `security/init-security-wrapper!` via `additional-effects` |
| `:deletion-constraints?` | plumbed into action rendering (`renderers/actions.cljs`) and page hook signature, but single-tenant hook is currently a no-op (`handlers/generic.cljs/use-deletion-constraints`) |

---

## `src/app/admin/frontend/config/form-fields.edn`

### Shape

Top-level map keyed by entity keyword. Each value:

- `:create-fields` (vector)
- `:edit-fields` (vector)
- `:required-fields` (vector)
- `:field-config` (map keyed by field keyword)

### Where it’s used

- Settings editor UI:
  - `src/app/admin/frontend/pages/settings.cljs` toggles `:create-fields`, `:edit-fields`, `:required-fields`.
- Actual form field specs (used by the template form component in admin pages):
  - `src/app/admin/frontend/specs/generic.cljs` overrides `:form-entity-specs/by-name`.
  - It reads `[:admin :config :form-fields <entity>]` and builds field specs.

### What is actually consumed

#### Top-level keys

| Key | Status | Notes |
|---|---|---|
| `:create-fields` | used | drives the field order/specs for `:form-entity-specs/by-name` in `admin/specs/generic.cljs` |
| `:edit-fields` | settings-UI-only (currently) | `admin/specs/generic.cljs` *can* use it (via `:admin/form-entity-specs-by-name`), but the main template subscription `:form-entity-specs/by-name` does not pass an `editing?` flag, so edit-fields are not selected in normal runtime form rendering |
| `:required-fields` | used | marks `:required true` in generated field specs |
| `:field-config` | used | provides per-field metadata for the generated specs |

#### `:field-config` inner keys

Currently used by `build-field-spec-from-config` in `src/app/admin/frontend/specs/generic.cljs`:

- `:type`
- `:label` (if present)
- `:options` (select)
- `:placeholder`
- `:min-length`
- `:max-length`
- `:validation`

Present in `form-fields.edn` but currently **not wired into field specs**:

- `:default`
- `:min`
- `:max`
- `:step`

Note: the template form component **does** pass through `:step` if it exists on the field spec (`src/app/template/frontend/components/form.cljs`), but the admin spec builder currently does not include it.

### Entity coverage note

If an entity is present in `form-fields.edn` but not present in the admin entity registry (`entities.edn` → `registered-entities`), its config will only matter if some code explicitly requests form specs for that entity. In the current single-tenant setup, `:tenants` looks like a legacy/future entry.

---

## `src/app/admin/frontend/config/table-columns.edn`

### Shape

Top-level map keyed by entity keyword. Each value includes:

- `:available-columns` (vector)
- Column policy keys (persisted in EDN):
  - `:default-visible-columns`
  - `:filterable-columns`
  - `:sortable-columns`
- `:always-visible`
- `:computed-fields`
- `:column-config`

### Notes

`table-columns.edn` is now stored and used in the **internal (non-inverted)** shape. There is no longer any load-time conversion step in `src/app/admin/frontend/config/loader.cljs`.

### Where it’s used

- Admin list/table rendering and settings panel:
  - Column visibility policy resolution: `src/app/template/frontend/subs/ui.cljs` (`::visible-columns`, `::locked-visible-columns`)
  - Column toggling/reordering/reset: `src/app/admin/frontend/events/config.cljs`
  - Header rendering uses width from entity spec: `src/app/template/frontend/components/list/table.cljs`
- Admin settings editor UI:
  - `src/app/admin/frontend/pages/settings.cljs` edits `:default-visible-columns`, `:filterable-columns`, and `:sortable-columns` and saves them back to EDN.

### Key usage

| Key | Status | Consumed by |
|---|---|---|
| `:available-columns` | used | ordering + visibility map derivation in `template/frontend/subs/ui.cljs` and admin toggle events |
| `:default-visible-columns` | used | default visibility for `template/frontend/subs/ui.cljs` (`::visible-columns`) |
| `:filterable-columns` | used | drives filterable fields (`template/frontend/subs/ui.cljs` `::filterable-fields`) |
| `:sortable-columns` | used | drives sortable columns (`template/frontend/subs/ui.cljs` / list header logic) |
| `:always-visible` | used | treated as “locks=true” in `template/frontend/subs/ui.cljs`; enforced in `admin/events/config.cljs` (cannot hide) |
| `:column-config` | partially used | `:width` is used (via entity spec → `list/table.cljs`); other keys are currently not consumed |
| `:computed-fields` | currently only used for metadata | `admin/specs/generic.cljs` marks a field as computed and includes dependencies, but list/table rendering does not currently use that metadata |

### Column IDs: strings vs keywords

Some entities use string column IDs in `:available-columns` (e.g. audit/login events). The visibility pipeline normalizes these via `normalize-col` in `src/app/template/frontend/subs/ui.cljs`, so mixed string/keyword IDs work (they converge to keywords like `:created-at`).

### `:column-config` inner keys

Currently used:

- `:width` (picked up by `src/app/template/frontend/components/list/table.cljs` via the entity spec)

Currently **not used by rendering** (present in EDN, but no consumers in the template list/table code):

- `:formatter` (only referenced while building specs in `admin/specs/generic.cljs`; the template list/table does not consult it)
- `:computed-field` (present in some configs, but unused)

---

## Quick “what’s unused?” checklist

- `entities.edn`
  - `:display-settings :per-page` (now used as the initial per-page default for list UI state)
  - `:custom-content :active-filters-display` (used by `admin/renderers/content.cljs` to opt into the active filters UI)
  - `:adapter-init-fn` and `:components` values as written in EDN (overwritten by registry to become callable)

- `form-fields.edn`
  - `:edit-fields` (used at runtime to build admin form specs; see `src/app/admin/frontend/specs/generic.cljs`)
  - field-config keys: `:default`, `:min`, `:max`, `:step` — NOW wired into admin form specs (see `build-field-spec-from-config`)

- `table-columns.edn`
  - `:column-config :formatter` (no consumers in list/table rendering)
  - `:computed-fields` (metadata only; not used to compute values or alter rendering today)

---

## Suggested workflow simplifications

### Implementation status (updated 2025-12-14)

- [x] (3) Stop using inverted table-columns keys (internal shape is persisted)
- [x] (5) Add a lightweight config audit script (`bb config-audit`)
- [x] (6) Document precedence rules in one place (see `ADMIN-LIST-VIEW-DISPLAY-SETTINGS.md`)
- [x] (1) Single source of truth per concern documented (see section below)
- [x] (2) Wire up form field constraints :min, :max, :step, :default (now consumed by admin form specs)
- [x] (4) Ensure settings UI only exposes runtime-impact settings (periodic review; config audit can flag drift)

These settings currently span multiple EDNs, multiple loaders (inline vs runtime fetch), and (in a few places) config keys that exist but don’t affect runtime behavior. Here are practical ways to simplify the workflow.

### 1) Pick a single “source of truth” per concern

- **Entity page composition** (title/description, which adapters/components, custom header/modals): keep in `entities.edn`, but keep it *data-only*.
  - Recommendation: continue using `system/entity_registry.cljs` as the *only* place that provides **callable fns/components**. Treat EDN as declarative metadata only.
- **Table columns policy** (available/default/locks/filterable/sortable): keep in `table-columns.edn`.
- **Form field policy** (create/edit/required + field metadata): keep in `form-fields.edn`.
- **Display toggles policy** (show-edit?, show-delete?, column-defaults/locks): keep in `view-options.edn`.

This reduces “where should I change this?” ambiguity.

### 2) Remove or wire up “looks-configurable but isn’t” keys

- `:per-page` is now configurable via `entities.edn :display-settings` (and/or by passing a `:per-page` prop into `list-view`).
  - If you want to reduce “sources of truth”, you can still consider moving the default to a single config file (e.g. `view-options.edn`) and having admin pages pass it through consistently.
- If you *don’t* want it configurable, delete `:per-page` from `entities.edn` to avoid misleading future editors.

Same rule of thumb for other currently-unused keys: either (a) delete them, or (b) add a single, explicit runtime consumer.

### 3) Stop using inverted keys in persisted table-columns config

This has now been implemented:

- `table-columns.edn` persists the **internal** shape directly:
  - `:default-visible-columns`, `:filterable-columns`, `:sortable-columns`
- The admin config editor (legacy page) edits those keys.
- The admin config loader no longer performs any load-time conversion.

Result: fewer moving parts and less ambiguity about the config shape at runtime.

### 4) Make the settings UI only expose settings that have runtime impact

As the code stands:

- `form-fields.edn :edit-fields` affects runtime behavior (admin edit forms) and should stay exposed.
- `table-columns.edn :column-config :formatter` is not currently used by the list/table renderer.
- `entities.edn :custom-content` is only partially consumed (currently only `:active-filters-display`).

Current behavior (as of 2025-12-14):

- The routed settings pages (`/admin/admin-settings` and `/admin/user-settings`) intentionally expose:
  - display toggles (`:show-*?`) defaults/locks
  - column visibility defaults/locks
  - (and only the column metadata needed to render labels and enforce “always visible”)
- The legacy admin config editor page (`src/app/admin/frontend/pages/settings.cljs`) exposes:
  - `form-fields.edn` field lists (create/edit/required)
  - `table-columns.edn` policy lists (default-visible/always-visible/filterable/sortable)
  - and does **not** provide inputs for `:column-config :formatter`, `:computed-fields`, or field-config constraints like `:min/:max/:step/:default`.

Simplify by either:

- hiding these inputs from `/admin/settings` until there’s a runtime consumer, or
- implementing the consumer and adding a small test so it can’t regress.

In practice, the “hide until there’s a consumer” path is what we’re doing today for `:formatter`, `:computed-fields`, and the unused `:field-config` constraint keys.

### 5) Add a lightweight “config audit” script and run it in CI

You already used a small EDN key extraction one-liner. Formalize that into a script that:

- reads each config EDN
- extracts keys (including nested)
- checks for keys that have *no* consumers (via a curated allowlist + grep patterns)

Then run it in CI (or as a `bb` task) so unused config doesn’t accumulate.

Implemented:

- Script: `scripts/bb/config_audit.clj`
- Task: `bb config-audit` (optional `--strict`, optional `--allowlist scripts/bb/config_audit_allowlist.edn`)
- CI hook: `npm run test:cljs:ci` runs `bb config-audit --strict` before the CLJS test compile/run

### 6) Document the precedence rules in one place

Column visibility already has a real precedence chain (policy locks → user prefs → defaults → config). Put a short “precedence” section in the relevant docs (or at the top of each EDN) so future changes don’t accidentally create a second competing source of truth.

Implemented:

- `ADMIN-LIST-VIEW-DISPLAY-SETTINGS.md` now includes an explicit per-page precedence + seeding rule.
