<!-- ai: {:tags [:frontend :architecture] :kind :guide} -->

# Frontend App Shell Architecture

## Overview

Single-tenant frontend built with Shadow CLJS, Re-frame, and UIx. The app shell powers the public build (`:app`) and the admin console (`:admin`) where admins manage users and monitor audit/login events. Tenant switching, billing, and multi-tenant property dashboards have been removed; state and routes are single-tenant and admin-centric.

## Build System Architecture

### Shadow CLJS Configuration (`shadow-cljs.edn`)

```clojure
{:nrepl {:port 8777 :init-ns shadow.user}
 :source-paths ["src" "resources/db"]
 :builds
 {:app   {:target :browser
          :output-dir "resources/public/js/main"
          :asset-path "/js/main"
          :modules {:app {:init-fn app.template.frontend.core/init
                         :preloads [app.frontend.preload.silence]}}}

  :admin {:target :browser
          :output-dir "resources/public/js/admin"
          :asset-path "/js/admin"
          :modules {:app {:init-fn app.admin.frontend.core/init
                         :preloads [app.frontend.preload.silence
                                    app.frontend.dev.tracing]}}}

  :test  {:target :browser-test
          :test-dir "target/test"
          :ns-regexp "-test$"}
  :test-node {:target :node-test
              :output-to "target/test-node.cjs"
              :ns-regexp "-test$"}
  :karma-test {:target :karma
               :output-to "target/karma-test.js"
               :ns-regexp "-test$"}}}
```

### Build Targets

| Target | Purpose | Entry Point | Output Directory |
|--------|---------|-------------|------------------|
| `:app` | Public/shell build (optional landing) | `app.template.frontend.core/init` | `resources/public/js/main` |
| `:admin` | Admin console (users, audit, login events) | `app.admin.frontend.core/init` | `resources/public/js/admin` |
| `:test`, `:test-node`, `:karma-test` | CLJS tests (browser/node/karma) | Test namespaces | `target/test*` |

### Development Workflow

```bash
# Public build (rarely touched in single-tenant)
npm run watch

# Admin console (primary surface, served at http://localhost:8085)
npm run watch:admin

# Tests
npm run test:cljs
bb fe-test-node       # node runner

# Production bundles
npm run build
npm run build:admin
```

## Application Entry Points

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
    ;; Template plumbing
    [app.template.frontend.events.core]
    [app.template.frontend.events.list.crud]
    [app.template.frontend.events.list.ui-state]
    [re-frame.core :as rf]))

(defn init-admin! []
  ;; Theme + config + entity specs
  (rf/dispatch-sync [:app.template.frontend.events.bootstrap/initialize-theme])
  (rf/dispatch [:app.template.frontend.events.config/fetch-config])
  (rf/dispatch [:admin/load-ui-configs]))
```

Admin UI configuration lives under `src/app/admin/frontend/config/*.edn` (table-columns, view-options, form-fields). These are inlined at build time via `preload.cljs` and refreshed at runtime through the authenticated settings API (`/admin/api/settings*`), so there is no longer a public `/admin/ui-config` asset path.

### Public Shell (`app.template.frontend.core`)

The public build remains for lightweight landing/demo needs but carries no tenant switching. Routing and pages can be trimmed if unused; keep `init`/`after-load` wiring aligned with `shadow-cljs.edn`.

## State Management (app-db)

```clojure
{:auth {:admin {:id "a88373f4-..."
                :email "admin@example.com"
                :name "System Administrator"
                :role "owner"
                :token "..."}}   ;; stored transiently (see security)

 :routing {:handler :admin-dashboard
           :route-params {}
           :query-params {}}

 :ui {:theme :light
      :loading? false
      :sidebar-open? true
      :notifications []}

 :entities {:users {:list [] :loading? false :error nil}
            :audit {:list [] :pagination {:page 1 :per-page 20}}
            :login-events {:list [] :pagination {:page 1 :per-page 20}}}}
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

Admin routes live in `app.admin.frontend.routes` (reitit):

```clojure
["/admin"
 ["/login" {:name :admin-login :view login/admin-login-page
            :controllers [{:start #(rf/dispatch [:admin/init-login])}]}]
 ["" {:name :admin-dashboard :view dashboard/admin-dashboard-page
      :controllers [(guarded-start [:admin/load-dashboard])]}]
 ["/users" {:name :admin-users :view users/admin-users-page
            :controllers [(guarded-start [[:admin/load-users]])]}]
 ["/audit" {:name :admin-audit :view audit/admin-audit-page
            :controllers [(guarded-start [[:admin/load-audit-logs]])]}]
 ["/login-events" {:name :admin-login-events
                   :view login-events/admin-login-events-page
                   :controllers [(guarded-start [[:admin/load-login-events]])]}]]
```

`guarded-start` dispatches only after admin auth is confirmed; the login page remains open.

## Component Architecture

- UIx is used for components; Re-frame subscriptions are pulled via `urf/use-subscribe`.
- Admin tables reuse template list components (`app.template.frontend.components.*`) for pagination, filters, and exports.
- The per-user activity modal is driven by users events + audit/login fetches; global pages (`/admin/audit`, `/admin/login-events`) use the generic entity page wrapper.

## HTTP Client

All admin API calls are under `/admin/api/*` and include the bearer token:

```clojure
(rf/reg-event-fx :admin/api-request
  (fn [{:keys [db]} [_ {:keys [uri method params on-success on-failure]}]]
    {:http-xhrio {:uri uri
                  :method (or method :get)
                  :params params
                  :headers {"Authorization" (str "Bearer " (get-in db [:auth :admin :token]))}
                  :on-success on-success
                  :on-failure on-failure}}))
```

## Performance

- Code splitting is per-route via Shadow modules; admin uses a single module and relies on hot reload.
- Keep lists fast by using paginated endpoints (`page`/`per-page`) and server-side filtering where available.

## Testing

- CLJS tests via `npm run test:cljs`, `bb fe-test-node`.
- Prefer REPL-driven checks for admin events/subs; `shadow-cljs watch :admin` enables hot reload.
- When adding list pages, cover the adapter transforms (e.g., login events normalization) with cljs tests under `test/app/admin/frontend`.

## Security

- Admin token is stored transiently (local/session storage) and injected into `Authorization` headers.
- All admin routes are guarded in `guarded-start`; unauthenticated access redirects to `/admin/login`.
- Avoid storing PII in app-db beyond what is needed for table rows; audit/login responses already normalize names/emails.

---

**Related Documentation**
- `docs/frontend/feature-guides/admin.md` — admin features (users, audit, login events)
- `docs/frontend/component-library.md` — reusable UI components
- `docs/frontend/http-standards.md` — API patterns and headers

*Last Updated: 2025-11-26*
