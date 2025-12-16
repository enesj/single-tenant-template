<!-- ai: {:tags [:frontend :admin :settings] :kind :guide} -->

# Admin Settings Configuration Guide

## Overview

The admin settings UI provides a centralized interface for managing UI configuration across the application:

- **Admin settings**: `/admin/admin-settings` (admin panel behavior + admin entity configuration)
- **User UI config**: `/admin/user-settings` (domain-owned defaults/locks for the user-facing app)
- **Legacy**: `/admin/settings` redirects to `/admin/admin-settings`

Both pages persist changes by writing EDN config files in-repo via backend endpoints under `/admin/api/settings/*`.

## Architecture

### Unified Settings Model

The settings system is unified under a single “scope” concept:

1. **Admin Scope**: Configuring how the admin panel behaves (admin list views, forms, table columns).
2. **User UI Config Scope**: Configuring defaults and constraints for the user-facing application.

The “User UI Config” scope is **domain-owned configuration** (currently the Expenses domain) and is **not** per-user localStorage preferences.

### Configuration Files

Admin settings are stored in EDN files under `src/app/admin/frontend/config/`:

- **`entities.edn`**: Registry of known admin entities and their metadata.
- **`view-options.edn`**: Policy defaults/locks for display toggles and per-column visibility policy.
- **`form-fields.edn`**: Create/edit field lists and required fields per entity.
- **`table-columns.edn`**: Structural column configuration (available columns, filterable/sortable, always-visible).

User-facing (domain-owned) UI config is stored alongside the domain (currently Expenses):

- `src/app/domain/frontend/expenses/config/entities.edn`
- `src/app/domain/frontend/expenses/config/view-options.edn`
- `src/app/domain/frontend/expenses/config/form-fields.edn`
- `src/app/domain/frontend/expenses/config/table-columns.edn`

These domain files are edited via `/admin/user-settings` and persisted via the backend settings API.

### Frontend Components (key files)

```
src/app/admin/frontend/
├── pages/unified_settings.cljs        # Admin settings + user settings pages (scope switching)
├── components/settings_shell.cljs     # Layout wrapper + save/discard UX
├── components/settings_views.cljs     # Reusable cards/editors (tri-state controls, bulk rows)
├── events/settings.cljs               # Admin scope load/save + patch helpers
├── events/unified_settings.cljs       # Unified orchestration for the settings UI
└── events/user_settings.cljs          # User UI config editor (domain-owned)
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

## Troubleshooting

**Settings not applying**
- Check browser console for errors and the `/admin/api/settings/*` network calls.
- If you edited EDN manually, run `bb validate-frontend-config` to confirm schema validity.
