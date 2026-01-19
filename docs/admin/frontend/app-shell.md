<!-- ai: {:tags [:frontend :architecture :admin] :kind :guide} -->

# Admin App Shell

This document covers the **admin console** entrypoint, state shape, event system, and routing. For build targets and Shadow CLJS configuration, see [Template App Shell](../../template/frontend/app-shell.md).

## Application Entry Point

### Admin Console (`app.admin.frontend.core`)

```clojure
(ns app.admin.frontend.core
  (:require
    ;; Admin events/subs (users, audit, login events, dashboard)
    [app.admin.frontend.events.auth]
    [app.admin.frontend.events.dashboard]
    [app.admin.frontend.events.audit]
    [app.admin.frontend.events.login-events]
    [app.admin.frontend.events.users]
    [app.admin.frontend.subs.auth]
    [app.admin.frontend.subs.dashboard]
    [app.admin.frontend.subs.audit]
    [app.admin.frontend.subs.login-events]
    [app.admin.frontend.subs.users]
    ;; Domain registry (loads domain events/subs via side effects)
    [app.domain.frontend.registry :as domain-registry]
    ;; Template plumbing
    [app.template.frontend.events.core]
    [app.template.frontend.events.list.crud]
    [app.template.frontend.events.list.ui-state]
    [re-frame.core :as rf]))

(defn init-admin! []
  ;; Auth persistence + theme + config + UI configs
  (rf/dispatch [:admin/init-auth-persistence])
  (rf/dispatch-sync [:app.template.frontend.events.bootstrap/initialize-theme])
  (rf/dispatch [:app.template.frontend.events.config/fetch-config])
  (rf/dispatch [:admin/load-ui-configs])
  (domain-registry/init-all-domains!))
```

Admin UI configuration is stored as EDN under `src/app/admin/frontend/config/*.edn` (system scope) and `src/app/domain/**/admin/config/*.edn` (domain scope, currently Expenses). Only the **entity registry metadata** (`entities.edn`) is inlined at build time via preload namespaces; runtime-editable settings (`view-options.edn`, `form-fields.edn`, `table-columns.edn`) are loaded and saved via the authenticated settings API (`/admin/api/settings*`).

## State Management (app-db)

```clojure
{:auth {:admin {:id "a88373f4-..."
                :email "admin@example.com"
                :name "System Administrator"
                :role "owner"}}

 ;; reitit match map for the current location
 :current-route {:data {:name :admin-dashboard
                        :view ...}
                 :parameters {:path {} :query {}}}

 ;; reitit frontend controllers state (from apply-controllers)
 :controllers [...]

 :ui {:theme :light
      :loading? false
      :sidebar-open? true
      :notifications []}

 ;; Admin-specific state lives under :admin (users, audit, settings editor, ...)
 :admin {...}}
```

## Event System

```
Admin Events (app.admin.frontend.events.*)
├── auth.cljs          ← admin login/session bootstrap
├── dashboard.cljs     ← stats and cards on /admin/dashboard
├── users.cljs         ← list + per-user activity modal
├── audit.cljs         ← global audit log list/export
├── login_events.cljs  ← global login events list
└── config.cljs        ← UI config and entity registry glue
Template Events (app.template.frontend.events.*) wire list/form CRUD helpers.
```

Example patterns (admin HTTP calls hit `/admin/api/...`):

```clojure
;; Load audit logs
(rf/reg-event-fx :admin/load-audit-logs
  (fn [{:keys [db]} [_ pagination]]
    {:db (assoc-in db [:entities :audit :loading?] true)
     :http-xhrio {:method :get
                  :uri "/admin/api/audit"
                  :params pagination
                  :on-success [:admin/audit-loaded]
                  :on-failure [:admin/audit-load-failed]}}))

;; Load login events
(rf/reg-event-fx :admin/load-login-events
  (fn [{:keys [db]} [_ pagination]]
    {:db (assoc-in db [:entities :login-events :loading?] true)
     :http-xhrio {:method :get
                  :uri "/admin/api/login-events"
                  :params pagination
                  :on-success [:admin/login-events-loaded]
                  :on-failure [:admin/login-events-load-failed]}}))
```

## Subscriptions

```clojure
(rf/reg-sub :admin/current
  (fn [db _] (get-in db [:auth :admin])))

(rf/reg-sub :admin/authenticated?
  :<- [:admin/current]
  (fn [admin] (some? admin)))

(rf/reg-sub :users/list
  (fn [db _] (get-in db [:entities :users :list])))

(rf/reg-sub :audit/list
  (fn [db _] (get-in db [:entities :audit :list])))

(rf/reg-sub :login-events/list
  (fn [db _] (get-in db [:entities :login-events :list])))
```

## Routing

Admin routes live in `app.admin.frontend.routes` and are included in the unified router (`app.template.frontend.routes.data/app-routes`).

Key routes (see `src/app/admin/frontend/routes.cljs` for the full list):

```clojure
["/admin"
 ["/login" {:name :admin-login :view login/admin-login-page
            :controllers [{:start #(rf/dispatch [:admin/init-login])}]}]

 ["/forgot-password" {:name :admin-forgot-password :view forgot-password/admin-forgot-password-page
                      :controllers [{:start #(rf/dispatch [:admin/init-forgot-password])}]}]

 ["/reset-password" {:name :admin-reset-password :view reset-password/admin-reset-password-page
                     :controllers [{:start (fn [params]
                                             (when-let [token (get-in params [:query :token])]
                                               (rf/dispatch [:admin/verify-reset-token token])))}]}]

 ["" {:name :admin-dashboard :view dashboard/admin-dashboard-page
      :controllers [(guarded-start [:admin/load-dashboard])]}]

 ["/users" {:name :admin-users :view users/admin-users-page
            :controllers [(guarded-start [[:admin/load-users]])]}]

 ["/audit" {:name :admin-audit :view audit/admin-audit-page
            :controllers [(guarded-start [[:admin/load-audit-logs]])]}]

 ["/login-events" {:name :admin-login-events
                   :view login-events/admin-login-events-page
                   :controllers [(guarded-start [[:admin/load-login-events]])]}]

 ["/admins" {:name :admin-admins :view admins/admin-admins-page
             :controllers [(guarded-start [[:admin/load-admins]])]}]

 ["/admin-settings" {:name :admin-admin-settings :view unified-settings-page/admin-settings-page
                     :controllers [(guarded-start nil)]}]

 ["/user-settings" {:name :admin-user-settings :view unified-settings-page/user-settings-page
                    :controllers [(guarded-start nil)]}]]
```

`guarded-start` dispatches only after admin auth is confirmed; unauthenticated access redirects to `/admin/login`.

## Component Architecture

- UIx is used for components; Re-frame subscriptions are pulled via `urf/use-subscribe`.
- Admin tables reuse template list components (`app.template.frontend.components.*`) for pagination, filters, and exports.
- The per-user activity modal is driven by users events + audit/login fetches; global pages (`/admin/audit`, `/admin/login-events`) use the generic entity page wrapper.

## HTTP Client

All admin API calls are under `/admin/api/*`. Prefer the shared helper in `app.admin.frontend.utils.http`, which injects the admin token via the `x-admin-token` header.

```clojure
(:require [app.admin.frontend.utils.http :as admin-http])

(admin-http/admin-get {:uri "/admin/api/audit"
                       :params {:page 1 :per-page 20}
                       :on-success [:admin/audit-loaded]
                       :on-failure [:admin/audit-load-failed]})
```

See also: [Frontend HTTP Request Standards](../../shared/frontend/http-standards.md).

## Performance

- Shadow builds are configured as a single module per build (`:app` within each build). Keep the admin bundle lean by avoiding large, unused requires in `app.template.frontend.core` and loading domain/admin wiring lazily where practical.
- Keep lists fast by using paginated endpoints (`page`/`per-page`) and server-side filtering where available.

## Testing

- CLJS tests via `npm run test:cljs`, `bb fe-test-node`.
- Prefer REPL-driven checks for admin events/subs; the dev runtime watches `:app` (`shadow-cljs watch :app`) and the admin console is served by the same SPA bundle.
- When adding list pages, cover adapter transforms with cljs tests. For concrete domains, put domain-specific tests under `test/app/domain/frontend/**`.

## Security

- Admin token is stored transiently (local/session storage / persisted auth state) and injected into `x-admin-token` headers.
- All admin routes are guarded in `guarded-start`; unauthenticated access redirects to `/admin/login`.
- Avoid storing PII in app-db beyond what is needed for table rows; audit/login responses already normalize names/emails.

---

**Related Documentation**
- [Admin Features](./admin.md)
- [Admin Settings](./admin-settings.md)
- [Shared Component Library](../../shared/frontend/component-library.md)
