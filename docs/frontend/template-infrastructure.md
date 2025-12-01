<!-- ai: {:tags [:frontend] :kind :guide} -->

# Template Infrastructure (Single-Tenant)

Shared UI infrastructure (UIx + Re-frame) used by the admin console. No tenant context or cross-domain orchestration; focus is reusable lists, forms, modals, buttons, and HTTP helpers.

## DRY Principle in Action

This template layer eliminates code duplication across admin features through a configuration-driven architecture.

### Without DRY (What We Avoid)

Each entity would need separate implementations:
- `users-list.cljs` - custom user table with pagination, filters, sorting
- `audit-list.cljs` - custom audit table with pagination, filters, sorting  
- `login-events-list.cljs` - custom login events table with pagination, filters, sorting
- **Repeated logic**: pagination controls, filter UI, sort handlers, loading states, export functions, column toggles

### With DRY (Our Approach)

One reusable `list-view` component + configuration:
- `list-view` component (handles all common logic once)
- `users-entity-spec` (defines user columns and data types)
- `audit-entity-spec` (defines audit columns and data types)
- `login-events-entity-spec` (defines login event columns and data types)

**Result:** 3 pages share the same battle-tested list implementation. Bug fixes and new features apply everywhere automatically.

### How Adapters Prevent Duplication

Without adapters, each page would duplicate:
1. Backend key transformation (`:audit-logs/action` → `:action`)
2. Loading state management (`[:entities :audit :loading?]`)
3. Re-frame event registration for CRUD operations
4. Data synchronization into app-db

Adapters centralize this once per entity type:

```clojure
;; Register once, use everywhere
(adapters.core/register-entity-spec-sub! {:entity-key :audit})
(adapters.core/register-sync-event! 
  {:event-id ::sync-audit
   :entity-key :audit
   :normalize-fn audit->entity})
```

Now all template components automatically work with the `:audit` entity.

## What the Template Layer Provides

- **List stack**: `list-view`, `table`, `pagination`, filters, column settings.  
- **Forms**: dynamic form + field components (text/number/select/json/etc.).  
- **Layout/UX**: modals, buttons, icons, states (loading/empty), cards/stats.  
- **State glue**: list/form events/subs, entity spec subscriptions, UI settings.  
- **HTTP**: use admin helpers for `/admin/api/*` (see `http-standards.md`).

## State Shape (admin usage)

```clojure
{:entities {:users {:data [...] :metadata {:loading? false}}
            :audit {:data [...] :metadata {:loading? false}}
            :login-events {:data [...] :metadata {:loading? false}}
            :specs {...}}
 :ui {:theme :light
      :entity-configs {...}   ;; list display settings
      :list-ui-state {...}}   ;; pagination, filters
 :forms {...}                ;; form values/errors per entity
 :auth {:admin {...}}}
```

Keep loading flags at `[:entities :<entity> :metadata :loading?]` so template subs work.

## Using Template Lists

```clojure
($ list-view
  {:entity-name :audit
   :entity-spec audit-entity-spec       ;; include rendered fields
   :title "Audit Logs"
   :display-settings {:show-pagination? true
                      :show-filtering? true
                      :page-size 20}
   :render-actions #( $ audit-row-actions {:row %})})
```

Provide `:entity-spec` that matches the columns you render (e.g., `:principal_email`, `:action`, `:created_at`).

## Adapter Expectations

- Normalize namespaced keys to plain (e.g., `:audit-logs/action` → `:action`).  
- Register spec subs and sync events so template lists read from `:entities`.  
- Bridge CRUD/exports via admin events; do not inline HTTP in components.

## Forms

```clojure
($ dynamic-form
  {:form-spec user-form-spec
   :values values
   :errors errors
   :on-change #(rf/dispatch [:users/form-update %])
   :on-submit #(rf/dispatch [:users/save %])})
```

Supported field types: text, email, number, textarea, select, checkbox, array-input, json-editor.

## Components Catalog (used in admin)

- Lists: `list-view`, `table`, `pagination`, `filter-form`, list settings.  
- UI: `button`, `change-theme`, `modal`, `confirm-dialog`, `icons`, `states`.  
- Forms: `dynamic-form` + field components.  
- Stats/cards: cards and stat grid for dashboard.

## Best Practices

- Always use helpers (`admin-http`) for API calls; avoid inline `:http-xhrio`.  
- Pass `:entity-spec` that mirrors rendered columns to keep toggles/export correct.  
- Handle both namespaced/plain keys in custom row actions.  
- Prefer server pagination/filtering; avoid storing large raw datasets.  
- Reuse existing components before adding new ones; keep `ds-` classes for DaisyUI.

*Last Updated: 2025-11-26*
