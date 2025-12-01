<!-- ai: {:tags [:frontend :template] :kind :guide} -->

# Template Component Integration (Single-Tenant)

Template components power the admin console (users, audit logs, login events). This guide shows how to use the shared UIx components with the admin adapters/events.

## Layering

This architecture separates concerns to maximize code reuse:

- **Template Layer**: Reusable components (`app.template.frontend.components.*`, list/form utilities, HTTP helpers)
  - *Benefit:* Write UI logic once, use across all entities
  
- **Adapter Layer**: Normalization + template sync (`app.admin.frontend.adapters.users`, `audit`, `login_events`)
  - *Benefit:* Standardize backend data transformation and state management
  
- **Admin Layer**: Pages, events, subs (`app.admin.frontend.pages/*`, `app.admin.frontend.events.*`)
  - *Benefit:* Focus on business logic, not UI plumbing

> [!TIP]
> **DRY in Practice**: The `list-view` component is used by users, audit, and login events pages. When we added CSV export functionality to `list-view`, all three pages gained export instantly—no code changes needed in the admin pages.

### Example: One Component, Multiple Pages

```clojure
;; Users page
($ list-view {:entity-name :users
              :entity-spec users-entity-spec
              :title "Users"})

;; Audit page  
($ list-view {:entity-name :audit
              :entity-spec audit-entity-spec
              :title "Audit Logs"})

;; Login events page
($ list-view {:entity-name :login-events
              :entity-spec login-events-entity-spec
              :title "Login Events"})
```

Same component, different configurations = DRY principle in action.

## Core Components

### `list-view`

Location: `src/app/template/frontend/components/list.cljs`

Key props: `:entity-name`, `:entity-spec`, `:title`, `:display-settings`, `:render-actions`.

Example (users):
```clojure
($ list-view
  {:entity-name :users
   :entity-spec users-entity-spec   ;; include rendered columns
   :title "Users"
   :show-add-form? false
   :display-settings {:show-select? true
                      :show-edit? false
                      :show-delete? false
                      :show-filtering? true
                      :show-pagination? true
                      :page-size 20}
   :render-actions (fn [user]
                     ($ admin-user-actions {:user user}))})
```

### `button`

Location: `src/app/template/frontend/components/button.cljs`

```clojure
($ button {:btn-type :primary
           :loading? saving?
           :on-click #(rf/dispatch [:admin/save-user user-id])
           :children "Save"})
```

### `change-theme`

Theme selector (DaisyUI).

```clojure
($ change-theme)
```

## Adapter Pattern (Users/Audit/Login Events)

Adapters normalize backend data (namespaced keys → plain), register entity specs, and sync into template list storage.

```clojure
(adapters.core/register-entity-spec-sub!
  {:entity-key :login-events})

(adapters.core/register-sync-event!
  {:event-id ::sync-login-events
   :entity-key :login-events
   :normalize-fn login-event->entity})
```

- Audit adapter formats action labels and timestamps; uses server pagination.  
- Login events adapter normalizes principal name/email/id and success flag.  
- Users adapter bridges CRUD and per-user activity modal data.

## Page Integration

Example users page:
```clojure
(defui admin-users-page []
  (let [selected (use-subscribe [::list-subs/selected-ids :users])]
    (use-effect (fn [] (rf/dispatch [:admin/load-users])) [])
    ($ admin-layout
      {:children
       ($ list-view {:entity-name :users
                     :entity-spec users-entity-spec
                     :title "Users"
                     :render-actions #( $ admin-user-actions {:user %})})}))
```

Audit and login event pages use the same pattern with their specs/adapters.

## Display Settings

```clojure
{:show-select? true
 :show-edit? false
 :show-delete? false
 :show-filtering? true
 :show-highlights? true
 :show-timestamps? true
 :show-pagination? true
 :page-size 20}
```

Provide an `:entity-spec` that matches rendered columns (e.g., `:principal_email`, `:action_label`, `:created_at`) so toggles/export work.

## Data Key Compatibility

Handle namespaced or plain keys in custom components:
```clojure
(let [email (or (:users/email user) (:email user))
      id    (or (:users/id user) (:id user))] ...)
```

## Paths and Loading State

Template list loading path: `[:entities :<entity> :metadata :loading?]`. Keep this shape when setting loading flags in events.

## Best Practices

- Use `app.admin.frontend.utils.http` for requests; don’t inline `:http-xhrio`.
- Keep adapters small; reuse `normalize-entity` and sync helpers.
- Pass `:entity-spec` that mirrors what the table renders (computed fields included).
- Compose actions (`:render-actions`) alongside defaults if enabled.
- Prefer server pagination; avoid storing large result sets in app-db.

## Components Catalog (used in admin)

- `list-view`, `table`, `pagination`
- `button`, `change-theme`, `icons`
- `modal`, `confirm-dialog`, `states` (loading/empty)
- `form` + field components

*Last Updated: 2025-11-26*
