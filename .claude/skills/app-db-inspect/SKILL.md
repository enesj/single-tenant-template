---
description: Inspect ClojureScript re-frame app-db state safely when debugging frontend state issues, authentication problems, data loading status, route inconsistencies, or UI state. Use when user mentions app-db, re-frame state, frontend debugging, or asks about current application state.
allowed-tools:
  - clojure-mcp:clojurescript_eval
---

# App-DB Inspect Skill

Safely inspects re-frame app-db state in ClojureScript applications without encountering `IDeref` protocol errors.

## When to Use This Skill

Use this skill when:
- User asks about current frontend state or re-frame app-db
- Debugging authentication issues (session, user info, tenant context)
- Checking data loading status or entity stores
- Verifying route and navigation state
- Investigating UI state, theme, or interface configuration
- Looking for errors, warnings, or state inconsistencies
- User mentions "app-db", "re-frame state", "frontend state", or "application state"

## Core Inspection Pattern

Always use this safe access pattern to avoid dereferencing issues:

```clojure
(if (exists? re-frame.db/app-db)
  (let [app-db @re-frame.db/app-db]
    ;; Inspect app-db safely here
    {:success true
     :data (select-keys app-db [:session :current-route :entities])})
  {:success false
   :error "app-db not found - ensure re-frame is initialized"})
```

## Key Sections to Inspect

### 1. Authentication State
```clojure
{:auth-summary
 {:authenticated? (get-in app-db [:session :authenticated?])
  :session-valid? (get-in app-db [:session :session-valid?])
  :user-email (get-in app-db [:session :user :email])
  :tenant-id (get-in app-db [:session :tenant-id])
  :tenant-name (get-in app-db [:session :tenant-name])}}
```

### 2. Current Route
```clojure
{:route-info
 {:template (get-in app-db [:current-route :template])
  :name (get-in app-db [:current-route :name])
  :params (get-in app-db [:current-route :parameters])}}
```

### 3. Entity Stores (Data Loading)
```clojure
{:entities-overview
 (->> (get app-db :entities {})
      (map (fn [[entity-type data]]
             [entity-type
              {:total-items (get-in data [:metadata :total-items] 0)
               :loading? (get-in data [:metadata :loading?] false)
               :has-data? (boolean (seq (get data :data {})))
               :error (get-in data [:metadata :error])}]))
      (into {}))}
```

### 4. UI State
```clojure
{:ui-state
 {:theme (get-in app-db [:ui :theme])
  :sidebar-open? (get-in app-db [:ui :sidebar-open?])
  :pagination (get-in app-db [:ui :pagination])}}
```

### 5. Error Detection
```clojure
{:errors-and-warnings
 {:validation-errors (get-in app-db [:validation-errors])
  :api-errors (->> (get app-db :entities {})
                   (filter (fn [[_ data]] (get-in data [:metadata :error])))
                   (map (fn [[k v]] [k (get-in v [:metadata :error])]))
                   (into {}))
  :route-auth-mismatch? (and (not (get-in app-db [:session :authenticated?]))
                            (not= (get-in app-db [:current-route :template]) "/login"))}}
```

## Analysis Strategies

### Focus-Based Inspection

Analyze user's request to determine focus:

- **Authentication keywords**: "auth", "login", "session", "authenticated"
  → Provide detailed authentication state analysis

- **Data loading keywords**: "entity", "data", "loading", "fetch"
  → Focus on entity stores and loading status

- **Route keywords**: "route", "navigation", "page", "url"
  → Analyze current route and navigation state

- **Error keywords**: "error", "warning", "problem", "issue", "bug"
  → Look for error states and inconsistencies

- **UI keywords**: "UI", "theme", "interface", "display"
  → Check UI state and configuration

### Comprehensive Overview

If no specific focus is requested, provide a structured overview:

```clojure
(if (exists? re-frame.db/app-db)
  (let [app-db @re-frame.db/app-db]
    {:success true
     :timestamp (js/Date.)
     :overview {
       :authentication {:authenticated? (get-in app-db [:session :authenticated?])
                       :user (select-keys (get-in app-db [:session :user] {})
                                         [:email :role])}
       :current-route {:template (get-in app-db [:current-route :template])
                      :name (get-in app-db [:current-route :name])}
       :entities-summary (->> (get app-db :entities {})
                             (map (fn [[k v]]
                                    [k {:count (count (get v :data {}))
                                        :loading? (get-in v [:metadata :loading?])}]))
                             (into {}))
       :ui-state {:theme (get-in app-db [:ui :theme])}
       :has-errors? (or (seq (get app-db :validation-errors))
                       (some (fn [[_ v]] (get-in v [:metadata :error]))
                             (get app-db :entities {})))}})
  {:success false
   :error "app-db not found"})
```

## Error Handling

Always wrap inspection code with proper error handling:

```clojure
(try
  (if (exists? re-frame.db/app-db)
    (let [app-db @re-frame.db/app-db]
      ;; Your inspection code
      {:success true :data app-db})
    {:success false :error "app-db not found"})
  (catch js/Error e
    {:success false
     :error (str "Inspection failed: " (.-message e))}))
```

## Common Debugging Scenarios

### Scenario 1: User Reports "Not Logged In"
Check authentication state consistency:
```clojure
{:debug-auth
 {:session-says-authenticated? (get-in app-db [:session :authenticated?])
  :session-valid? (get-in app-db [:session :session-valid?])
  :has-user-data? (boolean (get-in app-db [:session :user]))
  :current-route (get-in app-db [:current-route :template])
  :diagnosis (cond
               (not (get-in app-db [:session :authenticated?]))
               "User is not authenticated"

               (not (get-in app-db [:session :session-valid?]))
               "Session expired or invalid"

               (not (get-in app-db [:session :user]))
               "Missing user data - possible API failure"

               :else "Authentication state appears valid")}}
```

### Scenario 2: Data Not Loading
Check entity store state:
```clojure
{:debug-loading
 (let [entities (get app-db :entities {})]
   (->> entities
        (map (fn [[entity-type store]]
               [entity-type
                {:loading? (get-in store [:metadata :loading?])
                 :has-data? (boolean (seq (get store :data {})))
                 :error (get-in store [:metadata :error])
                 :item-count (count (get store :data {}))}]))
        (into {})))}
```

### Scenario 3: Route/Auth Mismatch
Verify route protection:
```clojure
{:route-auth-check
 (let [authenticated? (get-in app-db [:session :authenticated?])
       current-route (get-in app-db [:current-route :template])
       protected-routes #{"admin" "/dashboard" "/properties"}
       is-protected? (some #(clojure.string/starts-with? current-route %)
                          protected-routes)]
   {:authenticated? authenticated?
    :current-route current-route
    :is-protected-route? is-protected?
    :should-redirect? (and is-protected? (not authenticated?))
    :diagnosis (if (and is-protected? (not authenticated?))
                 "User on protected route without authentication - should redirect to /login"
                 "Route/auth state is consistent")})}
```

## Best Practices

1. **Always check existence**: Use `(exists? re-frame.db/app-db)` before accessing
2. **Use safe access**: Prefer `get-in` with default values over direct access
3. **Handle empty data**: Provide sensible defaults for missing paths
4. **Structure output**: Return well-organized maps with clear keys
5. **Include metadata**: Add timestamps and success flags
6. **Detect issues**: Look for common problems like auth mismatches
7. **Provide context**: Explain what state means, not just raw data

## Integration with Development Tools

This skill works well with:
- **Browser DevTools**: Verify UI state matches app-db state
- **HTTP debugging**: Check if API responses update app-db correctly

## Example Outputs

### Focused Authentication Check
```clojure
{:focus "authentication"
 :timestamp #inst "2024-12-15T10:30:00.000Z"
 :auth-state {
   :authenticated? true
   :session-valid? true
   :user {:email "user@example.com" :role "owner"}
   :tenant {:id 123 :name "Demo Tenant"}
   :diagnosis "✓ User is properly authenticated and session is valid"
 }}
```

### Comprehensive Overview
```clojure
{:overview true
 :timestamp #inst "2024-12-15T10:30:00.000Z"
 :summary {
   :authentication "✓ Authenticated as user@example.com"
   :route "Dashboard (/dashboard)"
   :data-stores "3 entity types loaded (properties: 5, transactions: 12, users: 3)"
   :ui-state "Light theme, sidebar closed"
   :issues []
 }}
```

## Troubleshooting

### Skill Not Activating
- Ensure user mentions keywords like "app-db", "re-frame state", "frontend state"
- Check that ClojureScript REPL is connected and running
- Verify re-frame is initialized in application

### Errors During Inspection
- Check that ClojureScript build is running (`npm run postcss:watch`)
- Ensure application is loaded in browser
- Verify re-frame namespace is available: `(exists? re-frame.db/app-db)`

### Missing Data
- Some keys may not exist in all application states
- Use `get-in` with defaults to handle missing paths gracefully
- Check if feature/domain is loaded in current context