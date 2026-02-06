---
description: Inspect re-frame app-db state safely in ClojureScript when debugging frontend state (auth/session, routing, data loading, UI state).
---

# app-db-inspect

Use this when someone asks “what’s in app-db?” or you need quick, safe snapshots of frontend state.

## Use when
- Auth/session looks wrong (logged out, missing user/tenant)
- Route/navigation mismatch (wrong page, redirect loop)
- Data not loading (entity stores stuck loading, errors)
- UI state/config questions (theme, sidebar, settings)

## Fast check (copy/paste)

```clojure
(try
  (if (exists? re-frame.db/app-db)
    (let [db @re-frame.db/app-db]
      {:ok? true
       :auth (select-keys (get db :session {})
                          [:authenticated? :session-valid? :tenant-id :tenant-name])
       :user (select-keys (get-in db [:session :user] {})
                          [:id :email :name])
       :route (select-keys (get db :current-route {})
                           [:template :name :parameters])
       :entities (->> (keys (get db :entities {})) sort vec)})
    {:ok? false
     :error "re-frame.db/app-db not found (app not initialized / build not connected yet)"})
  (catch js/Error e
    {:ok? false :error (.-message e)}))
```

## Focused snippets

### Auth/session summary
```clojure
(let [db @re-frame.db/app-db]
  (select-keys (get db :session {})
               [:authenticated? :session-valid? :user :tenant-id :tenant-name]))
```

### Current route
```clojure
(let [db @re-frame.db/app-db]
  (select-keys (get db :current-route {})
               [:template :name :parameters]))
```

## Troubleshooting
- **FileNotFoundException**: If you get this when evaluating, the REPL is in Clojure (JVM) mode. Switch to ClojureScript by evaluating:
  ```clojure
  (shadow.cljs.devtools.api/nrepl-select :app)
  ```
  (Use `:admin` if working on the admin panel).

### Entity store health (loading/errors/counts)
```clojure
(let [db @re-frame.db/app-db]
  (->> (get db :entities {})
       (map (fn [[entity-type store]]
              [entity-type
               {:loading? (get-in store [:metadata :loading?] false)
                :total-items (get-in store [:metadata :total-items] 0)
                :error (get-in store [:metadata :error])
                :item-count (count (get store :data {}))}]))
       (into {})))
```

## Troubleshooting
- `app-db not found`: open the app in the browser and ensure the CLJS build is connected/initialized.
- Missing keys: not every screen populates the same paths; use `get-in` with defaults.
