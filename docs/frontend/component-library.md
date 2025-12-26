<!-- ai: {:tags [:frontend :ui :reference] :kind :reference} -->

# Component Library Documentation

## Overview

Single-tenant admin UI built with UIx, Tailwind, and DaisyUI (`ds-` prefixed classes). **These components implement the DRY principle** by providing reusable, configuration-driven UI for users, audit logs, and login events.

> [!NOTE]
> **Code Reuse Strategy**: Before creating a new component, check if existing components can be configured to meet your needs. The `list-view`, `dynamic-form`, and modal components are designed to handle most admin use cases through props and entity specs.

The same `list-view` component powers:
- User management (`/admin/users`)
- Audit logs (`/admin/audit`)  
- Login events (`/admin/login-events`)

Each page provides different `:entity-spec` and `:render-actions` configurations—no duplicate list code.

## Design System

### CSS Framework

Tailwind + DaisyUI; keep `ds-` classes for DaisyUI and plain Tailwind utilities elsewhere.

```clojure
;; tailwind.config.js
module.exports = {
  content: ["./src/**/*.cljs"],
  theme: {
    extend: {
      colors: {
        primary: "#3b82f6",
        secondary: "#6b7280"
      }
    }
  },
  plugins: [require("daisyui")]
}
```

### Component Naming Convention

```clojure
(defui component-name
  "Short docstring"
  [{:keys [required-props] :or {}}]
  ;; body
  )
```

## Core Components

### Button (`app.template.frontend.components.button`)

Used for admin actions (load activity, export, deactivate user).

```clojure
($ button {:btn-type :primary
           :on-click #(rf/dispatch [:admin/export-audit])
           :children "Export Activity"})

($ button {:btn-type :ghost
           :loading? loading?
           :children ($ icon {:name :refresh})})
```

Types: `:primary`, `:secondary`, `:success`, `:warning`, `:ghost`. Use `:loading?` and `:disabled` when hitting admin APIs.

### Form (`app.template.frontend.components.form`)

Dynamic forms for creating/editing users.

```clojure
(def user-form-spec
  [{:id :name  :type :text  :label "Name"  :required true}
   {:id :email :type :email :label "Email" :required true}
   {:id :role  :type :select :label "Role"
    :options [{:value "owner" :label "Owner"}
              {:value "admin" :label "Admin"}]}])

($ dynamic-form {:form-spec user-form-spec
                 :values form-values
                 :errors errors
                 :on-change #(rf/dispatch [:users/form-update %])
                 :on-submit #(rf/dispatch [:users/save %])})
```

Field types: `:text`, `:email`, `:number`, `:textarea`, `:select`, `:checkbox`, `:array-input`, `:json-editor`.

#### Master/Detail Form Wrapper (`app.template.frontend.components.form.master-detail`)

For edit flows that need a **detail fetch** (e.g. header + line items), use the reusable wrapper:

- `master-detail-form` (source: `src/app/template/frontend/components/form/master_detail.cljs`)

It standardizes:

- Detail load orchestration for `:edit` mode
- Fallback to list-row data while loading
- Stable Fork initialization (memoized spec + initial values)
- Hooks for domain-specific normalization/validation/payload preparation

Guide: `docs/frontend/master-detail-form.md`

### List (`app.template.frontend.components.list`)

Backed by template list adapters; used for audit logs and login events.

```clojure
($ list-view {:entity-name :audit
              :entity-spec audit-entity-spec   ;; include rendered fields
              :title "Audit Logs"
              :filterable-columns [:action :principal_email :created_at]
              :per-page 20})
```

Features: dynamic columns, filtering, pagination, sorting, selection, batch actions. Use `:entity-spec` that matches rendered fields (e.g., `:principal_email`, `:action`).

### Row Actions (Admin Lists)

Rule: only **Edit** and **Delete** are rendered as buttons. All other actions (e.g., **View Details**) live inside the actions dropdown.

Dropdown actions should open modals for their output (details, previews, etc.) so the list page remains in place. Avoid route navigation from dropdown actions.

```clojure
(defn render-row-actions [entity-segment item]
  (let [item-id (id-utils/extract-entity-id item)]
    ($ dropdown/action-dropdown
      {:entity-id item-id
       :actions [{:group-title "View"
                  :items [{:id "view-details"
                           :icon ($ view-details-icon)
                           :label "View Details"
                           :on-click (fn [e]
                                       (.stopPropagation e)
                                       (rf/dispatch [:navigate-to (str "/admin/" entity-segment "/" item-id)]))}]}]
       :position :portal})))
```

This keeps row layouts consistent and preserves `actions-btn-<id>` hooks for browser tests.

### Filter (`app.template.frontend.components.filter`)

```clojure
(def login-filter-spec
  [{:field :principal_type :type :select :label "Type"
    :options [{:value "admin" :label "Admin"}
              {:value "user" :label "User"}]}
   {:field :success :type :select :label "Result"
    :options [{:value true :label "Success"}
              {:value false :label "Failed"}]}
   {:field :created_at :type :date :label "When"
    :operators [:after :before :between]}])

($ filter-form {:filter-spec login-filter-spec
                :active-filters filters
                :on-filter-change #(rf/dispatch [:login-events/filters %])
                :on-filter-clear #(rf/dispatch [:login-events/clear-filters])})
```

### Pagination (`app.template.frontend.components.pagination`)

```clojure
($ pagination {:current-page page
               :total-pages total-pages
               :on-page-change #(rf/dispatch [:audit/change-page %])})
```

## Layout Components

### Modal (`app.template.frontend.components.modal`)

Used for the per-user “Activity & Analytics” modal.

```clojure
($ modal {:open? open?
          :size :xl
          :title "User Activity & Analytics"
          :on-close #(rf/dispatch [:users/close-activity])
          :children ($ user-activity-body {:user user})})
```

### Card (`app.template.frontend.components.cards`)

Dashboard stat cards.

```clojure
($ card {:title "Recent Logins"
         :subtitle "Last 24h"
         :actions [($ button {:btn-type :ghost
                              :on-click #(rf/dispatch [:admin/refresh-dashboard])
                              :children "Refresh"})]
         :children ($ stats-content)})
```

## Interactive Components

### Confirm Dialog (`app.template.frontend.components.confirm_dialog`)

```clojure
($ confirm-dialog {:open? show?
                   :title "Deactivate user"
                   :message "This will block access until re-enabled."
                   :confirm-text "Deactivate"
                   :on-confirm #(rf/dispatch [:users/deactivate id])
                   :on-cancel #(rf/dispatch [:users/close-confirm])})
```

### Notifications (`app.template.frontend.components.notifications`)

```clojure
(rf/dispatch [:notifications/show
              {:type :success
               :title "Activity exported"
               :message "CSV downloaded"}])
```

## Data Display

### Table (`app.template.frontend.components.table`)

Useful for compact login events tables.

```clojure
($ table {:headers [{:key :created_at :label "Login Time" :sortable? true}
                    {:key :principal_type :label "Type"}
                    {:key :principal_email :label "Email"}
                    {:key :ip_address :label "IP"}
                    {:key :user_agent :label "User Agent"}]
          :rows login-rows
          :on-sort #(rf/dispatch [:login-events/sort %])})
```

### Stats (`app.template.frontend.components.stats`)

```clojure
($ stat-grid {:stats [{:title "Recent Logins" :value 12}
                      {:title "Failed Logins" :value 1}
                      {:title "Admins" :value 2}]})
```

## Form Field Components

```clojure
($ input {:id :name
          :value (:name form)
          :label "Full Name"
          :required true
          :on-change #(rf/dispatch [:users/form-update %])})

($ select-input {:id :role
                 :value (:role form)
                 :label "Role"
                 :options [{:value "owner" :label "Owner"}
                           {:value "admin" :label "Admin"}]
                 :on-change #(rf/dispatch [:users/form-update %])})
```

## Utility Components

### Loading / Empty States (`app.template.frontend.components.states`)

```clojure
($ loading-spinner {:size :lg :text "Loading audit logs..."})

($ empty-state {:title "No audit log entries"
                :message "Activity will appear after admins perform actions."})
```

### Icons (`app.template.frontend.components.icons`)

```clojure
($ icon {:name :shield-check :size :md :class "text-primary"})
```

## Styling and Theming

- Prefer `ds-` prefixed DaisyUI classes for shared look: `ds-btn`, `ds-card`, `ds-table`.
- Use Tailwind utilities for layout (`flex`, `gap-4`, `grid-cols-3`).
- Keep theme tokens in Tailwind config; avoid inline hex values when a token exists.

## Accessibility

Ensure `aria-label`, keyboard handlers, and focus management on modals/dialogs. Use `tab-index` only when necessary.

## Component ID Requirements (Browser Testing)

🚨 **CRITICAL**: All interactive UI components MUST have unique `:id` attributes for automated browser testing via **chrome-mcp**.

### Why This Matters

Browser testing tools like chrome-mcp locate elements by ID. Without proper IDs:
- ❌ Automated tests cannot reliably find interactive elements
- ❌ Accessibility tools may not properly associate labels with inputs
- ❌ Debugging becomes harder without unique element identifiers

### ID Patterns by Component Type

| Component Type | Pattern | Example |
|---------------|---------|----------|
| Form fields | `(str formId "-" field-type)` | `"login-form-input"`, `"user-form-select"` |
| Buttons | `(str "btn-" action "-" context)` | `"btn-submit-login"`, `"btn-delete-users-123"` |
| Settings toggles | `(str "toggle-" label "-" entity)` | `"toggle-timestamps-users"` |
| Column toggles | `(str "col-toggle-" entity "-" field)` | `"col-toggle-users-email"` |
| Action dropdowns | `(str "actions-btn-" entity-id)` | `"actions-btn-123"` |
| Filter controls | `(str "filter-" type "-" field)` | `"filter-toggle-users-name"` |

### Implementation Pattern

When creating components, always:

1. **Accept an `:id` prop** in the component's props map
2. **Generate fallback IDs** when explicit ID not provided:

```clojure
(defui my-input [{:keys [id formId label value on-change]}]
  (let [field-id (or id (when formId (str formId "-input")))
        error-id (when field-id (str field-id "-error"))]
    ($ :div
      ($ :label {:for field-id} label)
      ($ :input {:id field-id
                 :value value
                 :on-change on-change})
      ($ :div {:id error-id :class "text-error"}
        error-message))))
```

### Already Implemented

Form field components in `src/app/template/frontend/components/form/fields/` auto-generate IDs:
- `input.cljs` → `(str formId "-input")`
- `select.cljs` → `(str formId "-select")`
- `checkbox.cljs` → `(str formId "-checkbox")`
- `textarea.cljs` → `(str formId "-textarea")`
- `number.cljs` → `(str formId "-number")`

See `INTERACTIVE-COMPONENTS-ID-AUDIT.md` in repo root for complete patterns and audit status.

## Performance

- Memoize heavy rows (e.g., login events with many columns) when passing to lists/tables.
- Use paginated endpoints and avoid storing large raw responses in app-db.

## Testing Components

Favor small cljs tests for adapters/formatters that feed components (e.g., login event normalization). For UI behavior, rely on REPL/hot reload verification.

## Best Practices

1. Single responsibility per component.
2. Keep props consistent (`:on-submit`, `:on-change`, `:loading?`).
3. Guard admin-only actions in events, not just UI.
4. Pass `:entity-spec` that matches rendered columns so toggles/export work.
5. Avoid embedding secrets/tokens in app-db; use headers in effects.
6. **Always include `:id` attributes** on interactive elements for browser testing (see Component ID Requirements above).

---

**Related Documentation**
- `docs/frontend/app-shell.md` — app shell and routes
- `docs/frontend/feature-guides/admin.md` — admin flows (users, audit, login events)
- `docs/frontend/http-standards.md` — API integration
- `docs/shared/validation.md` — validation helpers

*Last Updated: 2025-12-25*
