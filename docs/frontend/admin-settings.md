<!-- ai: {:tags [:frontend :admin :settings] :kind :guide} -->

# Admin Settings Configuration Guide

## Overview

The admin settings page (`/admin/settings`) provides a centralized interface for managing UI configuration across all admin entities. It allows administrators to customize how lists, forms, and tables behave without modifying code.

## Architecture

### Configuration Files

Settings are stored in three EDN files under `src/app/admin/frontend/config/`:

- **`view-options.edn`**: Controls display toggles and action buttons
- **`form-fields.edn`**: Defines form field configurations per entity
- **`table-columns.edn`**: Configures table column behavior and properties

### Frontend Components

```
src/app/admin/frontend/
├── pages/settings.cljs           # Main settings page component
├── events/settings.cljs          # Re-frame events for settings CRUD
├── subs/settings.cljs            # Subscriptions for settings data
└── components/tabs.cljs          # Tabbed interface component
```

### Backend Integration

```
src/app/backend/routes/admin/
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

### Example Configuration

```clojure
{:users
 {:show-edit? true
  :show-delete? false
  :show-highlights? true
  :show-select? true
  :show-timestamps? false
  :show-pagination? true
  :show-add-button? true
  :show-batch-edit? false
  :show-batch-delete? false}}
```

## Form Fields Configuration

### Field Lists

Define which fields appear in forms for each entity:

```clojure
{:articles
 {:create-fields [:canonical_name :description :category]
  :edit-fields [:canonical_name :description :category :active?]
  :required-fields [:canonical_name]
  :field-config
  {:canonical_name {:type :text :max-length 100}
   :description {:type :textarea :rows 3}
   :category {:type :select :options ["General" "Specific"]}}}}
```

### Field Configuration Options

| Property | Type | Description | Example |
|----------|------|-------------|---------|
| `:type` | keyword | Field input type | `:text`, `:textarea`, `:select`, `:number` |
| `:max-length` | integer | Maximum character count | `100` |
| `:rows` | integer | Textarea row count | `3` |
| `:options` | vector | Select dropdown options | `["Option1" "Option2"]` |
| `:placeholder` | string | Input placeholder text | `"Enter name..."` |
| `:validation` | map | Validation rules | `{:pattern ".*@.*" :message "Invalid email"}` |

## Table Columns Configuration

### Column Properties

Configure how table columns behave:

```clojure
{:articles
 {:available-columns [:id :canonical_name :description :category :created-at]
  :default-hidden-columns [:id :description]
  :always-visible [:canonical_name]
  :unfilterable-columns [:description]
  :unsortable-columns [:description]
  :column-config
  {:canonical_name {:width "200px"}
   :category {:width "120px"}
   :created-at {:width "150px" :format "yyyy-MM-dd"}}}}
```

### Column Configuration Options

| Property | Type | Description |
|----------|------|-------------|
| `:available-columns` | vector | All possible columns for the entity |
| `:default-hidden-columns` | vector | Columns hidden by default |
| `:always-visible` | vector | Columns that cannot be hidden by users |
| `:unfilterable-columns` | vector | Columns that don't support filtering |
| `:unsortable-columns` | vector | Columns that don't support sorting |
| `:column-config` | map | Per-column display settings |

## UI Implementation

### Tabbed Interface

The settings page uses a tabbed layout:

1. **View Options Tab**
   - Entity selector dropdown
   - Toggle switches for each setting
   - Visual badges indicating current state
   - Edit mode for batch changes

2. **Form Fields Tab**
   - Entity selector
   - Multi-select boxes for field lists
   - Field configuration panel
   - Live preview of changes

3. **Table Columns Tab**
   - Entity selector
   - Column visibility controls
   - Drag-and-drop ordering
   - Width configuration inputs

### Real-time Updates

Settings changes are applied immediately:

1. **Optimistic Updates**: UI updates instantly on user action
2. **API Synchronization**: Changes sent to backend for persistence
3. **Cache Refresh**: Config loader updates after successful save
4. **Error Rollback**: Changes reverted on API failure

### State Management

Re-frame events handle settings operations:

```clojure
;; Load settings
:admin/load-view-options
:admin/load-form-fields
:admin/load-table-columns

;; Update settings
:admin/update-view-option-setting
:admin/update-form-field-setting
:admin/update-table-column-setting

;; Manage UI state
:admin/set-active-config-tab
:admin/enter-settings-edit-mode
:admin/exit-settings-edit-mode
```

## API Integration

### Endpoints

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/admin/api/settings` | Load view options |
| GET | `/admin/api/settings/form-fields` | Load form field configs |
| GET | `/admin/api/settings/table-columns` | Load table column configs |
| PATCH | `/admin/api/settings/entity` | Update view option |
| PATCH | `/admin/api/settings/form-fields/entity` | Update form fields |
| PATCH | `/admin/api/settings/table-columns/entity` | Update table columns |

### Request/Response Format

**PATCH Request Example**:
```json
{
  "entity-name": "articles",
  "setting-key": "show-edit?",
  "setting-value": false
}
```

**Response Format**:
```json
{
  "success": true,
  "data": {
    "updated": true,
    "entity": "articles",
    "config": "view-options"
  }
}
```

## Best Practices

### Configuration Design

1. **Consistent Defaults**: Use sensible defaults that work for most entities
2. **Progressive Enhancement**: Start minimal, add complexity as needed
3. **User Experience**: Don't disable features without clear reason
4. **Performance**: Avoid expensive operations in default views

### UI Guidelines

1. **Visual Feedback**: Show loading states during save operations
2. **Error Handling**: Display clear error messages with retry options
3. **Validation**: Validate configurations before applying
4. **Documentation**: Provide tooltips explaining each setting

### Code Organization

1. **Separation of Concerns**: Keep UI, events, and API logic separate
2. **Reusable Components**: Build generic settings components
3. **Testing**: Unit test settings logic and integration
4. **Migration**: Handle configuration format migrations gracefully

## Troubleshooting

### Common Issues

**Settings Not Applying**
- Check browser console for JavaScript errors
- Verify API responses in network tab
- Ensure config loader is registered

**Missing Entity Options**
- Confirm entity exists in `entities.edn`
- Check if entity has valid table columns
- Verify backend models are correctly defined

**Performance Issues**
- Reduce number of configured columns
- Optimize expensive computed fields
- Consider pagination for large lists

### Debug Tools

1. **Browser DevTools**: Check network requests and console errors
2. **Re-frame Debug**: Use app-db inspector to view settings state
3. **Backend Logs**: Check server logs for API errors
4. **Config Validation**: Use built-in validation helpers

## Migration Guide

### Adding New Entities

1. Add entity to `src/app/admin/frontend/config/entities.edn`
2. Create default configuration in each settings file
3. Add backend models if needed
4. Test all three configuration types

### Updating Configuration Format

1. Write migration script for existing configurations
2. Add version marker to configuration files
3. Test migration on development data
4. Document breaking changes

This comprehensive settings system provides flexible, user-friendly control over admin UI behavior while maintaining clear separation between configuration and application logic.