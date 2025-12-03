# CRUD Event Flow Documentation

This document describes the architecture and event flow for CRUD (Create, Read, Update, Delete) operations in the frontend. Understanding this flow is essential for debugging issues and extending the system.

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Key Modules](#key-modules)
3. [Event Flow Diagrams](#event-flow-diagrams)
4. [The Bridge System](#the-bridge-system)
5. [Admin Context Handling](#admin-context-handling)
6. [Success Handling & Highlighting](#success-handling--highlighting)
7. [Common Patterns](#common-patterns)
8. [Debugging Guide](#debugging-guide)

---

## Architecture Overview

The CRUD system is built with three main layers:

```
┌─────────────────────────────────────────────────────────────────────┐
│                        UI Components                                 │
│   (Form components, List views, Action buttons)                     │
└─────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    Form Interceptors (Optional)                      │
│   Routes form submissions based on context (admin vs template)       │
│   Location: admin/frontend/events/users/template/form_interceptors  │
└─────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                       Bridge CRUD System                             │
│   Customizable CRUD handlers with context-aware overrides           │
│   Location: shared/frontend/bridges/crud.cljs                       │
└─────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      Shared Success Module                           │
│   Consistent ID extraction and recently-updated/created tracking    │
│   Location: shared/frontend/crud/success.cljs                       │
└─────────────────────────────────────────────────────────────────────┘
```

### Design Principles

1. **Single Source of Truth**: All success handling logic lives in `crud/success.cljs`
2. **Context-Aware**: The bridge system allows different behavior based on context (admin vs template)
3. **Extensible**: New entity types can register custom handlers via the bridge system
4. **Consistent Highlighting**: Recently created/updated items are tracked centrally

---

## Key Modules

### 1. `app.shared.frontend.crud.success`

**Purpose**: Single source of truth for CRUD success handling

**Key Functions**:
- `extract-entity-id` - Extracts ID from response (handles both `:id` and namespaced keys like `:users/id`)
- `track-recently-created` - Adds entity ID to highlight set for new items
- `track-recently-updated` - Adds entity ID to highlight set for updated items
- `handle-create-success` - Standard create success handler
- `handle-update-success` - Standard update success handler

```clojure
;; Example usage
(crud-success/handle-update-success db :users response)
;; Returns db with:
;; - Form state cleared
;; - Entity ID tracked in [:ui :recently-updated :users]
```

### 2. `app.shared.frontend.bridges.crud`

**Purpose**: Context-aware CRUD operation customization

**Key Functions**:
- `register-crud-bridge!` - Register custom handlers for an entity type
- `run-bridge-operation` - Execute operation through registered bridges
- `default-crud-success` - Default success handler for create/delete
- `default-update-success` - Default success handler for updates

### 3. `app.template.frontend.events.form`

**Purpose**: Form submission handling for template entities

**Key Events**:
- `::submit-form` - Main entry point for form submissions
- `::create-success` / `::update-success` - Success handlers
- `::create-failure` / `::update-failure` - Failure handlers
- `::cancel-form` / `::clear-form` - Form state management

### 4. `app.admin.frontend.events.users.template.form-interceptors`

**Purpose**: Route admin form submissions through the bridge system

**Key Event**:
- `:app.template.frontend.events.form/submit-form` - Intercepts form submissions

---

## Event Flow Diagrams

### Create Operation (Admin Context)

```
User clicks "Create User" button
        │
        ▼
┌─────────────────────────────────────────────────────────────┐
│ UI Component dispatches                                      │
│ [:app.template.frontend.events.form/submit-form form-data]  │
└─────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────┐
│ Form Interceptor (form_interceptors.cljs)                   │
│ Checks: admin context? entity = :users?                     │
│ YES → dispatch to bridge system                              │
└─────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────┐
│ Bridge System                                                │
│ [:app.template.frontend.events.list.crud/create-entity      │
│  :users form-data]                                          │
└─────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────┐
│ Admin Users Adapter (adapters/users.cljs)                   │
│ - Uses admin HTTP endpoint: /admin/api/users                │
│ - Checks for admin token                                    │
└─────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────┐
│ HTTP Request to /admin/api/users                            │
│ on-success: [:...crud/create-success :users response]       │
└─────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────┐
│ Bridge Success Handler                                       │
│ 1. default-crud-success tracks recently-created             │
│ 2. Adapter on-success adds [:admin/load-users]              │
└─────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────┐
│ Effects Executed:                                            │
│ - db updated with recently-created tracking                  │
│ - [:admin/load-users] dispatched to refresh list             │
│ - Row appears highlighted in blue                            │
└─────────────────────────────────────────────────────────────┘
```

### Update Operation (Admin Context)

```
User edits and clicks "Update" button
        │
        ▼
┌─────────────────────────────────────────────────────────────┐
│ UI Component dispatches                                      │
│ [:app.template.frontend.events.form/submit-form             │
│  {:entity-name :users :editing true :values {...}}]         │
└─────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────┐
│ Form Interceptor                                             │
│ Checks: admin? editing? entity = :users?                     │
│ YES → dispatch to bridge update                              │
└─────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────┐
│ Bridge System                                                │
│ [:app.template.frontend.events.list.crud/update-entity      │
│  :users id form-data]                                       │
└─────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────┐
│ Admin Users Adapter                                          │
│ HTTP PUT to /admin/api/users/:id                            │
└─────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────┐
│ Bridge Success Handler                                       │
│ default-update-success:                                      │
│ - Tracks [:ui :recently-updated :users #{id}]               │
│ Adapter on-success:                                          │
│ - Adds [:admin/load-users] dispatch                         │
└─────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────┐
│ Result:                                                      │
│ - Row highlights green (recently-updated)                    │
│ - List refreshes with updated data                           │
└─────────────────────────────────────────────────────────────┘
```

### Template Context (Non-Admin)

When not in admin context, forms use the template's direct path:

```
Form Submit
    │
    ▼
Form Interceptor → NOT admin context
    │
    ▼
[:app.template.frontend.events.form/process-default-submission]
    │
    ▼
Template HTTP endpoints (/api/entities/...)
    │
    ▼
Template success handlers (form.cljs)
```

---

## The Bridge System

### How It Works

The bridge system allows context-specific customization of CRUD operations:

```clojure
;; Register a bridge for :users entity
(register-crud-bridge!
  {:entity-key :users
   :bridge-id :admin
   :context-pred (fn [_db] true)  ; Always apply in admin
   :operations
   {:update {:request    custom-request-fn
             :on-success custom-success-fn
             :on-failure custom-failure-fn}}})
```

### Handler Signatures

Different operations have different handler signatures:

```clojure
;; Delete handlers
:request    (fn [cofx entity-type id default-effect])
:on-success (fn [cofx entity-type id default-effect])
:on-failure (fn [cofx entity-type error default-effect])

;; Create handlers
:request    (fn [cofx entity-type form-data default-effect])
:on-success (fn [cofx entity-type response default-effect])
:on-failure (fn [cofx entity-type error default-effect])

;; Update handlers - NOTE: 5 args for on-success!
:request    (fn [cofx entity-type id form-data default-effect])
:on-success (fn [cofx entity-type id response default-effect])  ; <-- 5 args
:on-failure (fn [cofx entity-type error default-effect])
```

**Important**: The `default-effect` contains the result of the default handler, including any db updates (like `recently-updated` tracking). Custom handlers should merge or use this to preserve expected behavior.

### Execution Order

When multiple bridges are registered:
1. Bridges are sorted by `:priority` (higher first, default 100)
2. Each applicable bridge is checked against its `:context-pred`
3. First matching bridge's handler is applied to modify the effect
4. Final effect is returned

---

## Admin Context Handling

### Detection

Admin context is detected automatically by the template HTTP layer (`template/frontend/api/http.cljs`):

```clojure
(defn- admin-context?
  "Best-effort detection that we are inside the admin bundle."
  []
  (boolean (or (get-admin-token)                                    ; Token in app-db or localStorage
               (str/includes? pathname "/admin")                     ; /admin in URL path
               (str/includes? (str/lower-case hostname) "admin"))))  ; admin subdomain
```

This detection is used by all CRUD operations (`create-entity`, `update-entity`, `delete-entity`) to automatically route to the correct endpoint.

### Automatic Admin Routing

The template HTTP helpers now automatically handle admin context:

```
┌─────────────────────────────────────────────────────────────┐
│ Template HTTP Layer (http.cljs)                              │
│                                                              │
│ admin-context? = true                                        │
│ ├── create-entity → POST /admin/api/{entity}                │
│ ├── update-entity → PUT /admin/api/{entity}/{id}            │
│ ├── delete-entity → DELETE /admin/api/{entity}/{id}         │
│ └── + x-admin-token header automatically attached            │
│                                                              │
│ admin-context? = false                                       │
│ ├── create-entity → POST /api/v1/entities/{entity}          │
│ ├── update-entity → PUT /api/v1/entities/{entity}/{id}      │
│ └── delete-entity → DELETE /api/v1/entities/{entity}/{id}   │
└─────────────────────────────────────────────────────────────┘
```

This means:
- **Batch operations** (like bulk delete) work correctly in admin context without special handling
- **Bridge customizations** can focus on success/failure behavior rather than endpoint routing
- The `x-admin-token` header is always attached when a token is available

### Admin-Specific Behavior

1. **Automatic endpoint routing**: Template HTTP helpers detect admin context and use `/admin/api/*` endpoints
2. **Authentication**: `x-admin-token` header automatically attached when token is present
3. **Custom refresh**: Adapters dispatch `[:admin/load-users]` instead of generic fetch
4. **Same highlighting**: Uses shared `crud/success` module

---

## Success Handling & Highlighting

### How Highlighting Works

1. **On success**: Entity ID is added to a set in app-db:
   - Create: `[:ui :recently-created :users #{id}]`
   - Update: `[:ui :recently-updated :users #{id}]`

2. **Table rendering**: Checks if row ID is in these sets:
   ```clojure
   (let [recently-created (get-in db [:ui :recently-created :users])
         recently-updated (get-in db [:ui :recently-updated :users])]
     (cond
       (contains? recently-created id) :highlight-created  ; Blue
       (contains? recently-updated id) :highlight-updated  ; Green
       :else nil))
   ```

3. **Clearing**: IDs are cleared on navigation or explicit action

### ID Extraction

The system handles various response formats:

```clojure
;; All these work:
{:id 123}                    ; → 123
{:users/id 456}              ; → 456
{:transaction-types/id 789}  ; → 789
```

---

## Common Patterns

### Adding a New Entity with Admin Overrides

1. Create adapter file:
   ```clojure
   (ns app.admin.frontend.adapters.my-entity
     (:require [app.admin.frontend.adapters.core :as adapters.core]))
   
   (adapters.core/register-admin-crud-bridge!
     {:entity-key :my-entity
      :operations {...}})
   ```

2. Register in form interceptors (if form behavior differs):
   ```clojure
   ;; In form_interceptors.cljs
   (and (= entity-k :my-entity) in-admin?)
   {:dispatch [:app.template.frontend.events.list.crud/update-entity ...]}
   ```

### Customizing Success Behavior

Use the bridge `on-success` handler:

```clojure
:on-success (fn [_cofx _entity-type _id _response default-effect]
              ;; default-effect already contains:
              ;; - :db with recently-updated tracking
              ;; - :dispatch for entity refresh
              
              ;; Add custom dispatch:
              (update default-effect :dispatch-n conj [:my-custom-event]))
```

---

## Debugging Guide

### Common Issues

#### 1. Row Not Highlighting After Update

**Symptoms**: Update succeeds but row doesn't turn green

**Checklist**:
- Is the entity ID being extracted correctly? Check response format
- Is `default-effect` being used in custom `on-success` handler?
- Is the `on-success` handler signature correct (5 args for update)?

**Debug**:
```clojure
;; Check recently-updated in app-db
(get-in @re-frame.db/app-db [:ui :recently-updated :users])
```

#### 2. Form Button Stays Disabled

**Symptoms**: Update button remains disabled after cancel

**Cause**: `:submitting?` not being reset

**Check**: `cancel-form` and `clear-form` events should set `:submitting? false`

#### 3. Wrong HTTP Endpoint Used

**Symptoms**: 404 errors or wrong data returned

**Check**: 
- Is admin token present? (Check localStorage and app-db)
- Is the URL path or hostname containing "admin"?
- The `admin-context?` function in `template/frontend/api/http.cljs` determines routing

**Debug**:
```clojure
;; Check if admin context is detected
(let [token (or (:admin/token @re-frame.db/app-db)
                (.getItem js/localStorage "admin-token"))
      pathname (.-pathname js/window.location)]
  (js/console.log "Admin token:" token)
  (js/console.log "Pathname:" pathname))
```

### Debug Logging

Enable detailed logging:

```clojure
;; In bridges/crud.cljs
(log/debug "Update success for" entity-type "id:" id)

;; In form.cljs
(log/debug "📤 FORM UPDATE-SUCCESS - entity-type:" entity-type
           "extracted entity-id:" entity-id)
```

### Tracing Event Flow

To trace which code path is being used:

1. Check browser Network tab for HTTP endpoint
2. Add `js/console.log` in form interceptor
3. Check `bridge-registry-summary` for registered bridges:
   ```clojure
   (app.shared.frontend.bridges.crud/bridge-registry-summary)
   ```

---

## File Reference

| File | Purpose |
|------|---------|
| `shared/frontend/crud/success.cljs` | Shared success handling, ID extraction, highlighting |
| `shared/frontend/bridges/crud.cljs` | Bridge system for context-aware CRUD |
| `template/frontend/api/http.cljs` | **HTTP helpers with automatic admin context routing** |
| `template/frontend/events/form.cljs` | Template form submission events |
| `template/frontend/events/list/crud.cljs` | Template list CRUD events |
| `admin/frontend/events/users/template/form_interceptors.cljs` | Admin form routing |
| `admin/frontend/adapters/users.cljs` | Admin users adapter with bridge registration |
| `admin/frontend/adapters/audit.cljs` | Audit logs adapter (normalization, sync, UI init) |
| `admin/frontend/adapters/login_events.cljs` | Login events adapter (normalization, sync, UI init) |
| `admin/frontend/events/audit.cljs` | Audit HTTP events (load, filter, delete, export) |
| `admin/frontend/events/login_events.cljs` | Login events HTTP events (load) |
| `admin/frontend/utils/audit.cljs` | Audit UI formatting helpers |

---

## Adapter Architecture

### What Belongs in Adapters

Adapters are data transformation and integration layers. They should contain:

```
✅ Data normalization functions
✅ Entity spec subscription registration  
✅ Template sync event registration
✅ Bridge registration for CRUD customization
✅ UI state initialization
```

### What Does NOT Belong in Adapters

HTTP logic and event handlers should be in events namespaces:

```
❌ HTTP request events
❌ Success/failure handlers
❌ UI formatting utilities (put in utils/)
❌ Business logic beyond data transformation
```

### Example: Clean Adapter Structure

```clojure
(ns app.admin.frontend.adapters.my-entity
  "Adapter for my-entity to work with the template system.
   
   Responsibilities:
   - Data normalization
   - Template system sync
   - Bridge registration
   - UI state initialization
   
   HTTP events are in app.admin.frontend.events.my-entity"
  (:require
    [app.admin.frontend.adapters.core :as adapters.core]
    [app.template.frontend.db.paths :as paths]
    [re-frame.core :as rf]))

;; Data normalization
(defn my-entity->template-entity [entity]
  (-> entity
    (update :id #(when % (str %)))))

;; Template integration
(adapters.core/register-entity-spec-sub! {:entity-key :my-entity})
(adapters.core/register-sync-event!
  {:event-id ::sync-to-template
   :entity-key :my-entity
   :normalize-fn my-entity->template-entity})

;; Bridge registration (optional - for CRUD customization)
(adapters.core/register-admin-crud-bridge!
  {:entity-key :my-entity
   :operations {...}})

;; UI state initialization
(rf/reg-event-fx ::initialize-ui-state ...)
(defn init-adapter! [] (rf/dispatch [::initialize-ui-state]))
```

---

## Related Documentation

- [Template Infrastructure](./template-infrastructure.md) - Template system overview
- [Admin Panel](./admin-panel-single-tenant.md) - Admin-specific features
- [HTTP Standards](./http-standards.md) - HTTP client patterns
