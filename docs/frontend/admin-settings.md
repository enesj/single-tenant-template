<!-- ai: {:tags [:frontend :admin :settings] :kind :guide} -->

# Admin Settings Configuration Guide

## Overview

The admin settings UI provides a centralized interface for managing UI configuration across the application:

- **Admin settings**: `/admin/admin-settings` (admin panel behavior + admin entity configuration)
- **User UI config**: `/admin/user-settings` (domain-owned defaults/locks for the user-facing app)

Legacy `/admin/settings` route has been removed; use `/admin/admin-settings`.

Both pages persist changes by writing EDN config files in-repo via backend endpoints under `/admin/api/settings/*`.

## Architecture

### Unified Settings Model

The settings system is unified under a single “scope” concept:

1. **Admin Scope**: Configuring how the admin panel behaves (admin list views, forms, table columns).
2. **User UI Config Scope**: Configuring defaults and constraints for the user-facing application.

The “User UI Config” scope is **domain-owned configuration** (currently the Expenses domain) and is **not** per-user localStorage preferences.

### Configuration Files

Admin settings are stored as EDN and loaded from:

- **System/admin-owned** config under `src/app/admin/frontend/config/`

The backend supports an optional domain-admin overlay merge from `src/app/domain/**/admin/config/`, but the Expenses domain does not currently provide admin UI config (admin pages removed).

Both scopes use the same EDN file types:

- **`entities.edn`**: Registry of known admin entities and their metadata.
- **`view-options.edn`**: Policy defaults/locks for display toggles and per-column visibility policy.
- **`form-fields.edn`**: Create/edit field lists and required fields per entity.
- **`table-columns.edn`**: Structural column configuration (available columns, filterable/sortable, always-visible).

Only `entities.edn` is inlined at build time (via preload namespaces) so routes and adapters can be available early; the other files are edited at runtime and loaded via the settings API.

User-facing (domain-owned) UI config is stored alongside the domain (currently Expenses):

- `src/app/domain/frontend/expenses/config/entities.edn`
- `src/app/domain/frontend/expenses/config/view-options.edn`
- `src/app/domain/frontend/expenses/config/form-fields.edn`
- `src/app/domain/frontend/expenses/config/table-columns.edn`

These domain files are edited via `/admin/user-settings` and persisted via the backend settings API.

### Frontend Components (key files)

```
src/app/admin/frontend/
├── pages/unified_settings.cljs        # Settings UI entrypoint (thin wrapper)
├── pages/unified_settings/            # Settings UI implementation (split namespaces)
├── components/settings_shell.cljs     # Layout wrapper + save/discard UX
├── components/settings_views.cljs     # Settings view entrypoint (thin wrapper)
├── components/settings_views/         # Reusable cards/editors (split namespaces)
├── events/settings.cljs               # Admin scope entrypoint (thin wrapper)
├── events/settings/                   # Admin scope handlers/subs/helpers (split namespaces)
├── events/unified_settings.cljs       # Unified orchestration for the settings UI
├── events/user_settings.cljs          # User UI config editor entrypoint (thin wrapper)
└── events/user_settings/              # User UI config handlers/subs/helpers (split namespaces)
```

### Backend Integration

```
src/app/template/backend/routes/admin/
└── settings.clj                       # API endpoints for all config types
```

## View Options Configuration

### Display Toggles (defaults + locks)

View options support a **new explicit schema** (preferred) and a legacy schema.

Preferred shape per entity:

```clojure
{:users
 {:display-defaults {:show-pagination? true
                     :show-timestamps? true}
  :display-locks    {:show-delete? false}

  ;; Column visibility policy (separate from table-columns.edn)
  :column-defaults  {:email true :role true}
  :column-locks     {:id true}}}
```

Legacy admin shape (deprecated): top-level `:show-*?` keys are treated as **locks when present**.

Display toggle keys are defined in `src/app/shared/specs/view_options.cljc` (`display-toggle-keys`).

| Setting | Type | Description |
|---------|------|-------------|
| `:show-edit?` | boolean | Show edit buttons in list rows |
| `:show-delete?` | boolean | Show delete buttons in list rows |
| `:show-highlights?` | boolean | Enable row highlighting on hover |
| `:show-select?` | boolean | Show multi-select checkboxes |
| `:show-timestamps?` | boolean | Show created/updated timestamp columns |
| `:show-pagination?` | boolean | Show pagination controls |
| `:show-filtering?` | boolean | Show filtering controls |
| `:show-add-button?` | boolean | Show "Add New" button in list header |
| `:show-batch-edit?` | boolean | Enable batch edit operations |
| `:show-batch-delete?` | boolean | Enable batch delete operations |

## Table Columns Configuration

Table columns are structural config in `table-columns.edn`. The settings UI exposes per-entity editing for:

- `:available-columns`
- `:default-visible-columns`
- `:filterable-columns`
- `:sortable-columns`
- `:always-visible` (**structural enforcement**)

`:always-visible` columns are **always shown** and cannot be hidden via user preferences or view-options policy. In the UI, the editor includes a per-property **“Toggle All”** row to bulk-select/deselect each list.

## UI Implementation Notes

### Unified Shell

The settings shell provides:

1. **Scope Switching**: Toggle between Admin and User UI config.
2. **State Management**: Dirty state, save, and discard.
3. **Mode Toggle**: View (overview) vs Edit (single-entity focus).

## API Integration

### Endpoints

All endpoints below are under the admin API namespace:

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/admin/api/settings` | Load admin `view-options.edn` |
| PUT | `/admin/api/settings` | Replace admin `view-options.edn` (validated) |
| PATCH | `/admin/api/settings/entity` | Update a single admin entity setting |
| DELETE | `/admin/api/settings/entity` | Remove a single admin entity setting |
| GET | `/admin/api/settings/form-fields` | Load admin `form-fields.edn` |
| PATCH | `/admin/api/settings/form-fields/entity` | Patch admin form-fields for one entity |
| GET | `/admin/api/settings/table-columns` | Load admin `table-columns.edn` |
| PATCH | `/admin/api/settings/table-columns/entity` | Patch admin table-columns for one entity |
| GET | `/admin/api/settings/user-ui-config` | Load domain-owned user UI config bundle |
| PUT | `/admin/api/settings/user-ui-config` | Update domain-owned user UI config bundle (any subset; validated) |

### Validation

Settings writes are validated against Malli specs under `src/app/shared/specs/*`. Invalid payloads fail with HTTP `400` and include validation details in the response body/logs.

## Schema Alignment (DB ↔ UI Config)

Frontend config EDNs are also validated against the consolidated DB schema in `resources/db/models.edn`.

Note: the validation/sync tooling also accepts `--schema <path>` where `<path>` can be either:
- a consolidated schema file (e.g. `resources/db/models.edn`), or
- the hierarchical schema directory (e.g. `resources/db`), which merges `template/domain/shared` sources in-memory.

- `bb validate-frontend-config` checks both **shape** (Malli) and **schema alignment** (entities/fields exist).
- `bb sync-frontend-config` computes a patch plan to align configs with the DB schema.
  - Default is **dry-run** + **fail-on-mismatch** (no files written).
  - Use `--apply` to write changes.
  - Use `--only <domain>` / `--skip <domain>` to scope domain configs.

### Normalization rules

- **Entities**: normalized by name with `_` and `-` treated as equivalent (e.g., `:audit_logs` ↔ `:audit-logs`).
- **Fields**: normalized by name with `_` and `-` treated as equivalent (e.g., `full_name` ↔ `full-name`).
- Validation is tolerant for matching, but `--apply` preserves each file's existing style.

### Computed / UI-only fields

Computed fields belong in UI config only (never in `models.edn`). A field is treated as computed/UI-only if it is:

- listed under the entity's `:computed-fields` in `table-columns.edn`, **or**
- allowlisted in a UI allowlist EDN passed to the validator/sync (`--allowlist <path>`).

Allowlist format (EDN):

```clojure
{:audit-logs ["entity-name" "admin-email"]
 :users ["full-name"]}
```

Form-fields validation treats fields as DB-backed unless they are explicitly allowlisted.

### Domain discovery

Domain configs are discovered automatically from:

- `src/app/domain/frontend/*/config/`

Any domain folder containing `config/` is included; the validator/sync only checks the standard files when present.

## Troubleshooting

**Settings not applying**
- Check browser console for errors and the `/admin/api/settings/*` network calls.
- If you edited EDN manually, run `bb validate-frontend-config` to confirm schema validity.
