<!-- ai: {:tags [:frontend] :kind :guide} -->

# List View Controls & Configuration Guide

## Overview

List-view settings (toggles + column visibility + filters) are resolved from a combination of:

- **Feature constraints** (entity-level business rules)
- **Policy** (defaults/locks from `view-options.edn`)
- **Per-user preferences** (stored in app-db under `[:ui :entity-prefs]`)

The single source of truth for display-toggle resolution is `src/app/template/frontend/settings/resolver.cljs`.

## Core Concepts

### Two layers: structural vs policy

1. **Structural config** (`table-columns.edn`)
   - Defines what columns exist and what they *can* do (available/filterable/sortable/always-visible).
   - Supports optional `:column-metadata` for per-column display labels (e.g. `{"supplier_display_name" {:label "Supplier"}}`).
   - `:always-visible` is **enforced** (cannot be overridden by policy or user prefs).

2. **Policy** (`view-options.edn`)
   - Defines *defaults* and *locks* for display toggles and column visibility.
   - Locks remove user control (either by hiding a toggle or disabling a column button).

### Display toggles precedence

`src/app/template/frontend/settings/resolver.cljs` applies this precedence (high → low):

1. Locks from **feature constraints** (`entities.edn` → `:features`)
2. Locks from **policy** (`view-options.edn`)
3. **Per-user preferences** (`[:ui :entity-prefs <entity> :display]`)
4. Policy defaults (`view-options.edn` → `:display-defaults`)
5. Entity defaults (`entities.edn` → `:display-settings`)
6. In-code fallback defaults

## Where configuration lives

### Admin (admin list pages)

Admin configuration EDNs are in `src/app/admin/frontend/config/` and are edited via `/admin/admin-settings`:

- `entities.edn`
- `view-options.edn`
- `form-fields.edn`
- `table-columns.edn`

### Domain-owned user UI config (user-facing pages)

Domain configuration EDNs are stored alongside the domain (currently Expenses) and are edited via `/admin/user-settings`:

- `src/app/domain/frontend/expenses/config/entities.edn`
- `src/app/domain/frontend/expenses/config/view-options.edn`
- `src/app/domain/frontend/expenses/config/form-fields.edn`
- `src/app/domain/frontend/expenses/config/table-columns.edn`

## Policy schema (`view-options.edn`)

The schema is defined/validated in `src/app/shared/specs/view_options.cljc`.

Preferred shape per entity:

```clojure
{:users
 {:display-defaults {:show-pagination? true
                     :show-timestamps? true}
  :display-locks    {:show-delete? false}

  ;; Column visibility policy (separate from structural table-columns.edn)
  :column-defaults  {:email true :role true}
  :column-locks     {:id true}}}
```

Legacy admin shape (deprecated): top-level `:show-*?` keys are treated as **locks when present**.

## Per-user preferences (`[:ui :entity-prefs]`)

Template list settings events store user preferences under:

- `[:ui :entity-prefs <entity> :display :show-*]` — display toggles
- `[:ui :entity-prefs <entity> :columns :visible]` — column visibility map (`:col -> boolean`)
- `[:ui :entity-prefs <entity> :columns :width]` — table width
- `[:ui :entity-prefs <entity> :filters :fields]` — per-field filtering enabled map

See `src/app/template/frontend/events/list/settings.cljs` for the canonical structure (legacy paths are still read during migration but new writes go to `:entity-prefs` only).

## UI behavior (what users see)

### Locked display toggles

In the list-view settings panel, display toggles that are **locked by policy or feature constraints** are **hidden** (not rendered as interactive controls). This prevents users from toggling a setting that can’t change.

### Column visibility controls

Column buttons remain visible, but they become non-interactive when:

- The column is structurally enforced via `table-columns.edn` `:always-visible`, or
- The column is locked via `view-options.edn` `:column-locks`

In those cases the button is disabled and explains why via a tooltip/title.

### Column display labels (admin + user settings)

In the **Table Columns** editor (`/admin/admin-settings` and `/admin/user-settings`), each column has a **Display Label** input.

- Saving a non-blank value writes `:column-metadata <column> {:label "..."}` in `table-columns.edn`.
- Clearing the input removes that `:column-metadata` label override.
- Label overrides are resolved during entity-spec generation (`:entity-specs/by-name`) so list headers render the override label.

## Implementation pointers

- Resolver (display toggles): `src/app/template/frontend/settings/resolver.cljs`
- List view settings panel UI: `src/app/template/frontend/components/settings/list_view_settings.cljs`
- Per-user preference events: `src/app/template/frontend/events/list/settings.cljs`
- Admin settings editors (tri-state UI, table-columns editor, toggle-all): `src/app/admin/frontend/pages/unified_settings.cljs` (entrypoint) + `src/app/admin/frontend/pages/unified_settings/` (implementation)
- Settings view components (tri-state rows, bulk rows): `src/app/admin/frontend/components/settings_views.cljs` (entrypoint) + `src/app/admin/frontend/components/settings_views/` (implementation)

## Verification (fast checks)

- Validate config EDNs: `bb validate-frontend-config`
- Audit config keys vs usage: `bb config-audit --strict`
