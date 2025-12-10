<!-- ai: {:tags [:frontend] :kind :guide} -->

# List View Controls Configuration Guide

## Overview

The list view controls system configures table display settings, column visibility, and user interactions. It merges **page-level hardcoded settings** with **user preferences**, but hardcoded settings are now **hidden from the UI** (controls do not render when locked).

## Architecture

### **Control Configuration Levels**

```
Page Level (Hardcoded) ──┐
                         ├── Merged Settings ──> Visual Display
User Preferences ────────┘
```

1. **Page Level**: Hardcoded constraints set by developers
2. **User Preferences**: Runtime settings stored in Re-frame DB
3. **Merged Settings**: Combination used for actual display behavior (controls are hidden when hardcoded)

### Reactive pipeline
- `app.template.frontend.hooks.display-settings` merges hardcoded + user settings.
- `app.template.frontend.components.list.cells` renders cells using the merged settings (single source of truth).
- Hardcoded values can come from page props or `src/app/admin/frontend/config/view-options.edn`.

## Field-Level Configuration (models.edn)

### **Admin Metadata Structure**

Fields are configured in `resources/db/template/models.edn` (and `resources/db/shared/models.edn`) with `:admin` metadata:

```clojure
{:id :email
 :type :string
 :label "Email Address"
 :admin {:visible-in-table? true
         :filterable? true
         :sortable? true
         :display-order 1}}
```

### **Admin Configuration Options**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `:visible-in-table?` | boolean | `true` | Whether column appears in table |
| `:filterable?` | boolean | `true` | Whether field can be filtered |
| `:sortable?` | boolean | `true` | Whether column can be sorted |
| `:display-order` | integer | `nil` | Column order (lower = left) |

### **Example Field Configurations**

```clojure
;; Always visible, user can filter
{:id :email
 :admin {:visible-in-table? true
         :filterable? true
         :display-order 1}}

;; Hidden by default, no filtering
{:id :internal-id
 :admin {:visible-in-table? false
         :filterable? false}}

;; Visible but no user control
{:id :created-at
 :admin {:visible-in-table? true
         :filterable? false
         :display-order 99}}
```

## Page-Level Control Configuration

### **Hardcoded Display Settings**

Pages can enforce specific settings via props (controls are omitted entirely when locked):

```clojure
;; In page component (e.g., admin/users.cljs)
($ list-component
  {:entity-name :users
   :display-settings {:show-timestamps? false    ; Force OFF
                      :show-pagination? true}    ; Force ON
   ;; ... other props
   })
```

### **Control Behavior Matrix**

| Hardcoded Value | Visual Appearance | User Interaction | Feature State |
|----------------|-------------------|------------------|---------------|
| `false` | Control **not shown** | ❌ Hidden | ❌ Disabled |
| `true` | Control **not shown** | ❌ Hidden | ✅ Forced on |
| Not set | Control rendered | ✅ User can toggle | ↕️ User controlled |

### **Tooltip Messages**

Hardcoded controls are hidden, so tooltips only apply to user-controlled toggles.

## Available Controls

### **Main Display Controls**

| Control | Purpose | Default | Hardcoded Key |
|---------|---------|---------|---------------|
| **Edit** | Show edit buttons | `true` | `:show-edit?` |
| **Delete** | Show delete buttons | `true` | `:show-delete?` |
| **Highlights** | Row highlighting | `true` | `:show-highlights?` |
| **Selection** | Multi-select checkboxes | `false` | `:show-select?` |
| **Timestamps** | Created/updated columns | `true` | `:show-timestamps?` |
| **Pagination** | Page navigation | `true` | `:show-pagination?` |

### **Utility Controls**

| Control | Purpose | Behavior |
|---------|---------|----------|
| **Table Width** | Set table pixel width | Always interactive |
| **Rows Per Page** | Items per page | Always interactive |
| **Column Visibility** | Toggle specific columns | Based on field `:admin` config |

### **Filter Icons**

Timestamps and other controls with filtering show filter icons when:
- Control is active (`is-active? = true`)
- Field has filtering enabled (`:filterable? true`)
- Control is not hardcoded (hardcoded controls are hidden)

## Implementation Files

### **Core Components**

```
src/app/template/frontend/components/
├── settings/
│   └── list_view_settings.cljs     # Main settings panel
├── list/cells.cljs                 # Cell rendering (merged settings)
├── list.cljs                       # List wrapper component
├── table.cljs                      # Table component
└── pagination.cljs                 # Pagination component
```

### **State Management**

```
src/app/template/frontend/
├── events/list/ui-state.cljs       # Control toggle events
├── hooks/display_settings.cljs     # Merge hardcoded + user settings
└── subs/ui.cljs                    # Display settings subscriptions
```

### **Configuration Files**

```
resources/db/
├── template/models.edn             # Template/admin fields (users, audit, login events)
├── shared/models.edn               # Shared fields
└── domain/*                        # Only if you add new domains
```

## Configuration Examples

### **Admin Users Table**

```clojure
;; In resources/db/template/models.edn (single-tenant)
{:table :users
 :fields [{:id :email
           :admin {:visible-in-table? true
                   :display-order 1}}
          {:id :full-name
           :admin {:visible-in-table? true
                   :display-order 2}}
          {:id :role
           :admin {:visible-in-table? true
                   :display-order 3}}
          {:id :status
           :admin {:visible-in-table? true
                   :display-order 4}}
          {:id :auth-provider
           :admin {:visible-in-table? false}}]} ; Hidden by default
```

### **Page Configuration**

```clojure
;; In src/app/admin/frontend/pages/users.cljs
(defui users-page []
  ($ list-view
    {:entity-name :users
     :entity-spec users-entity-spec   ;; include rendered/computed fields
     :display-settings {:show-timestamps? false    ; Hide timestamps always
                        :show-select? true}        ; Enable selection always
     :page-title "User Management"
     :enable-search? true}))
```

## Best Practices

### **Field Configuration**

1. **Use display-order**: Set explicit order for important columns
2. **Hide sensitive data**: Set `:visible-in-table? false` for internal IDs
3. **Limit filtering**: Disable filtering for complex data types

## Admin Settings page
- Hardcoded view settings live in `src/app/admin/frontend/config/view-options.edn` and can be edited via the admin UI at `/admin/settings` (see `app.admin.frontend.pages.settings`).
4. **Consider performance**: Hide expensive computed fields by default

### **Page Configuration**

1. **Minimal hardcoding**: Only hardcode when business logic requires it
2. **User experience**: Don't hardcode `false` unless feature is genuinely unavailable
3. **Documentation**: Comment why settings are hardcoded
4. **Consistency**: Use same patterns across similar pages

### **Control Visibility**

```clojure
;; Good: Hardcode only when needed
{:show-edit? false}        ; When editing is not allowed
{:show-timestamps? true}   ; When timestamps are critical

;; Avoid: Over-constraining users
{:show-highlights? false   ; Let users decide
 :show-select? false       ; Unless selection breaks functionality
 :show-pagination? false}  ; Unless single page is required
```

## Testing Scenarios

### **Manual Testing Checklist**

1. **No hardcoded settings**:
   - ✅ All controls interactive
   - ✅ User preferences persist
   - ✅ Normal visual styling

2. **Hardcoded `true`**:
   - ✅ Control appears enabled
   - ❌ Control not clickable
   - ✅ Feature actually works
   - ✅ Tooltip explains constraint

3. **Hardcoded `false`**:
   - ❌ Control appears disabled (grayed)
   - ❌ Control not clickable
   - ❌ Feature actually disabled
   - ✅ Tooltip explains constraint

4. **Column visibility**:
   - ✅ Respects `:visible-in-table?` settings
   - ✅ Maintains `:display-order`
   - ✅ User can toggle configurable columns

### **Browser Testing**

```bash
# Admin pages served at http://localhost:8085/admin
# Test users list toggles
open http://localhost:8085/admin/users
```

## Common Issues & Solutions

### **Column Order Problems**
- **Issue**: Columns appear in wrong order
- **Solution**: Set explicit `:display-order` values in models.edn

### **Controls Not Responding**
- **Issue**: User cannot interact with controls
- **Solution**: Check for hardcoded settings in page props

### **Styling Inconsistencies**
- **Issue**: Hardcoded controls look wrong
- **Solution**: Verify visual state matches actual feature state

### **Filter Icons Missing**
- **Issue**: Filter icons don't appear
- **Solution**: Ensure `:filterable? true` in field admin config

This configuration system provides flexible, user-friendly control over list view behavior while maintaining clear boundaries between user preferences and application constraints.
