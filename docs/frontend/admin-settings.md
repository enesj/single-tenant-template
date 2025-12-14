<!-- ai: {:tags [:frontend :admin :settings] :kind :guide} -->

# Admin Settings Configuration Guide

## Overview

The admin settings page (`/admin/settings`) provides a centralized interface for managing UI configuration across the application. It supports both **admin-facing** settings (view options, form fields) and **user-facing** defaults (locks, initial states).

## Architecture

### Unified Settings Model

The settings system is unified under a single "scope" concept:

1. **Admin Scope**: Configuring how the admin panel itself behaves.
2. **User Scope**: Configuring defaults and constraints for the user-facing application.

### Configuration Files

Settings are stored in EDN files under `src/app/admin/frontend/config/`:

- **`view-options.edn`**: Controls display toggles and action buttons.
- **`form-fields.edn`**: Defines form field configurations per entity.
- **`table-columns.edn`**: Configures table column behavior and properties.
- **`entities.edn`**: Registry of known entities and their metadata.

### Frontend Components

```
src/app/admin/frontend/
├── pages/unified_settings.cljs     # Main page with scope switching
├── components/settings_shell.cljs  # Layout wrapper for settings UI
├── components/settings_views.cljs  # Reusable settings card components
├── events/unified_settings.cljs    # Unified state management
└── definitions.cljs                # Entity groupings and domain logic
```

### Backend Integration

```
src/app/template/backend/routes/admin/
└── settings.cljs                 # API endpoints for all config types
```

## View Options Configuration

### Display Settings

Control how entity lists appear and behave:

| Setting | Type | Description | Default |
|---------|------|-------------|---------|
| `:show-edit?` | boolean | Show edit buttons in list rows | `true` |
| `:show-delete?` | boolean | Show delete buttons in list rows | `true` |
| `:show-highlights?` | boolean | Enable row highlighting on hover | `true` |
| `:show-select?` | boolean | Show multi-select checkboxes | `false` |
| `:show-timestamps?` | boolean | Show created/updated timestamp columns | `true` |
| `:show-pagination?` | boolean | Show pagination controls | `true` |

### Action Settings

Configure batch operations and add buttons:

| Setting | Type | Description | Default |
|---------|------|-------------|---------|
| `:show-add-button?` | boolean | Show "Add New" button in list header | `true` |
| `:show-batch-edit?` | boolean | Enable batch edit operations | `false` |
| `:show-batch-delete?` | boolean | Enable batch delete operations | `false` |

## UI Implementation

### Unified Shell

The settings page uses a unified shell (`settings_shell.cljs`) that provides:

1. **Scope Switching**: Toggle between Admin and User settings modes.
2. **State Management**: Handles dirty states, saving, and discarding changes.
3. **Mode Toggle**: Switches between "View" (overview) and "Edit" (detailed configuration).

### Edit Modes

- **View Mode**: Shows a high-level overview of all configured entities for the current scope.
- **Edit Mode**: Focuses on a single entity, allowing detailed modification of all available settings.

### Real-time Updates

Settings changes are applied immediately:

1. **Optimistic Updates**: UI updates instantly on user action.
2. **Draft State**: Changes are held in a draft state until explicitly saved.
3. **API Synchronization**: centralized save event flushes the draft to the backend.

## State Management

Re-frame events handle unified settings operations (`events/unified_settings.cljs`):

```clojure
;; Scope & Mode
:admin.unified-settings/set-scope    ; :admin | :user
:admin.unified-settings/set-mode     ; :view | :edit
:admin.unified-settings/set-selected-entity

;; Persistence
:admin.unified-settings/save-current-scope
:admin.unified-settings/discard-current-scope
```

## API Integration

### Endpoints

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/admin/api/settings` | Load admin view options |
| PATCH | `/admin/api/settings/entity` | Update admin view option |
| GET | `/admin/api/settings/user` | Load user settings |
| PUT | `/admin/api/settings/user` | Save user settings |

## Best Practices

### Configuration Design

1. **Consistent Defaults**: Use sensible defaults that work for most entities.
2. **Progressive Enhancement**: Start minimal, add complexity as needed.
3. **User Experience**: Don't disable features without clear reason.

### UI Guidelines

1. **Visual Feedback**: Show loading states during save operations.
2. **Validation**: Validate configurations before applying.
3. **Drafts**: Use the saving/dirty state to prevent accidental data loss.

### Code Organization

1. **Separation of Concerns**: Keep UI, events, and API logic separate.
2. **Unified logic**: Use `unified_settings` namespace to bridge admin/user config logic.
3. **Testing**: Unit test settings logic and integration.

## Troubleshooting

### Common Issues

**Settings Not Applying**
- Check browser console for JavaScript errors.
- Verify API responses in network tab.
- Ensure config loader is registered.

**Missing Entity Options**
- Confirm entity exists in `entities.edn`.
- Check if entity has valid table columns.

**Performance Issues**
- Reduce number of configured columns in large tables.
- Optimize expensive computed fields.