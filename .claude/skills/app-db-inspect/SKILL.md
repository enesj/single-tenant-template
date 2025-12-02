---
description: Inspect ClojureScript re-frame app-db state safely when debugging frontend state issues, authentication problems, data loading status, route inconsistencies, or UI state. Use when user mentions app-db, re-frame state, frontend debugging, or asks about current application state.
allowed-tools:
  - clojure-mcp:clojurescript_eval
---

# App-DB Inspect Skill

Safely inspects re-frame app-db state in ClojureScript applications without encountering `IDeref` protocol errors.

## When to Use This Skill

Trigger when user asks about:
- Current frontend state or re-frame app-db
- Authentication issues (login, session, user info, tenant context)
- Data loading status or entity stores
- Route/navigation state inconsistencies
- UI state, theme, or interface configuration
- Errors, warnings, or state inconsistencies
- Keywords: "app-db", "re-frame state", "frontend state", "application state"

## Core Inspection Pattern

Always wrap inspection code with proper error handling:

```clojure
(try
  (if (exists? re-frame.db/app-db)
    (let [app-db @re-frame.db/app-db]
      ;; Replace this map with focused inspection per request
      {:success true
       :data {:session (select-keys (get app-db :session {})
                                   [:authenticated? :session-valid? :user :tenant-id])
              :current-route (select-keys (get app-db :current-route {})
                                          [:template :name :parameters])
              :ui (get app-db :ui)
              :entities (keys (get app-db :entities {}))}})
    {:success false
     :error "app-db not found - ensure re-frame is initialized"})
  (catch js/Error e
    {:success false
     :error (str "Inspection failed: " (.-message e))}))
```

## Focus-Based Inspection

Determine what to inspect based on user keywords:

| Keywords | Focus | Code Section |
|----------|-------|--------------|
| Auth, login, session, authenticated | `:session` | Auth summary below |
| Entity, data, loading, fetch | `:entities` | Entities overview below |
| Route, navigation, page, url | `:current-route` | Route info below |
| Error, warning, bug, problem, issue | Error metadata | Error detection below |
| UI, theme, interface, display | `:ui` | UI state below |

## Common Code Snippets

### Authentication Summary

```clojure
(let [app-db @re-frame.db/app-db]
  {:auth-summary
   {:authenticated? (get-in app-db [:session :authenticated?])
    :session-valid? (get-in app-db [:session :session-valid?])
    :user-email (get-in app-db [:session :user :email])
    :tenant-id (get-in app-db [:session :tenant-id])
    :tenant-name (get-in app-db [:session :tenant-name])}})
```

### Route Info

```clojure
(let [app-db @re-frame.db/app-db]
  {:route-info
   (select-keys (get app-db :current-route {})
                [:template :name :parameters])})
```

### Entities Overview

```clojure
(let [app-db @re-frame.db/app-db]
  {:entities-overview
   (->> (get app-db :entities {})
        (map (fn [[entity-type data]]
               [entity-type
                {:total-items (get-in data [:metadata :total-items] 0)
                 :loading? (get-in data [:metadata :loading?] false)
                 :has-data? (boolean (seq (get data :data {})))
                 :error (get-in data [:metadata :error])}]))
        (into {}))})
```

### UI State

```clojure
(let [app-db @re-frame.db/app-db]
  {:ui-state
   {:theme (get-in app-db [:ui :theme])
    :sidebar-open? (get-in app-db [:ui :sidebar-open?])
    :pagination (get-in app-db [:ui :pagination])}})
```

### Error Detection

```clojure
(let [app-db @re-frame.db/app-db]
  {:errors-and-warnings
   {:validation-errors (get app-db :validation-errors)
    :api-errors (->> (get app-db :entities {})
                     (filter (fn [[_ data]] (get-in data [:metadata :error])))
                     (map (fn [[k v]] [k (get-in v [:metadata :error])]))
                     (into {}))
    :route-auth-mismatch? (and (not (get-in app-db [:session :authenticated?]))
                               (not= (get-in app-db [:current-route :template]) "/login"))}})
```

## Debugging Scenarios

### "Not Logged In" Issue

```clojure
(let [app-db @re-frame.db/app-db
      authenticated? (get-in app-db [:session :authenticated?])
      session-valid? (get-in app-db [:session :session-valid?])
      has-user? (boolean (get-in app-db [:session :user]))
      current-route (get-in app-db [:current-route :template])]
  {:debug-auth
   {:authenticated? authenticated?
    :session-valid? session-valid?
    :has-user? has-user?
    :current-route current-route
    :diagnosis (cond
                 (not authenticated?) "User is not authenticated"
                 (not session-valid?) "Session expired or invalid"
                 (not has-user?) "Missing user data - possible API failure"
                 :else "Authentication state appears valid")}})
```

### Data Not Loading

```clojure
(let [app-db @re-frame.db/app-db]
  {:debug-loading
   (->> (get app-db :entities {})
        (map (fn [[entity-type store]]
               [entity-type
                {:loading? (get-in store [:metadata :loading?])
                 :has-data? (boolean (seq (get store :data {})))
                 :error (get-in store [:metadata :error])
                 :item-count (count (get store :data {}))}]))
        (into {}))})
```

### Route/Auth Mismatch

```clojure
(let [app-db @re-frame.db/app-db
      authenticated? (get-in app-db [:session :authenticated?])
      current-route (get-in app-db [:current-route :template])
      protected-routes #{"admin" "/dashboard" "/properties"}
      is-protected? (some #(clojure.string/starts-with? current-route %)
                          protected-routes)]
  {:route-auth-check
   {:authenticated? authenticated?
    :current-route current-route
    :is-protected? is-protected?
    :should-redirect? (and is-protected? (not authenticated?))
    :diagnosis (if (and is-protected? (not authenticated?))
                 "User on protected route without authentication - should redirect to /login"
                 "Route/auth state is consistent")}})
```

## Best Practices

1. **Always check existence**: Use `(exists? re-frame.db/app-db)` before deref
2. **Use safe access**: Prefer `get-in` with defaults over direct nested access
3. **Handle empty data**: Assume keys may be missing; provide sensible defaults
4. **Structure output**: Return well-organized maps with clear keys
5. **Include metadata**: Add `:success` flag and `:error` on failure
6. **Detect inconsistencies**: Look for auth-route mismatches, loading without data, API errors
7. **Provide context**: Explain what state means in brief natural language after showing the map

## Example Output

```clojure
{:auth-summary
 {:authenticated? false,
  :session-valid? true,
  :user-email nil,
  :tenant-id nil,
  :tenant-name nil}}
```

Interpretation: Session is valid but user is not authenticated. This is normal for unauthenticated visitors or after logout.

## Troubleshooting

- **app-db not found** → Ensure re-frame is initialized in the application (check browser console)
- **Errors during eval** → Verify the ClojureScript build is running and browser is connected
- **Missing data paths** → Not all keys exist in all states; use `get-in` with defaults gracefully
- **stale data** → Re-frame may not have loaded entities yet; check `:loading?` flag in metadata
