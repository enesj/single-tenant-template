<!-- ai: {:tags [:frontend :http] :kind :reference} -->

# Frontend HTTP Request Standards

## Overview

Single-tenant app with an admin console at `/admin`. All admin API calls go to `/admin/api/*` and must include the admin token header (`x-admin-token`). Use the shared helpers instead of inline `:http-xhrio` blocks to keep auth, formats, and timeouts consistent. The public `:app` build is optional; if used, it should also rely on the template helper for any public calls.

## Core Principles

1. Use helper functions (`admin-http` for admin, `http` for template/public).  
2. Never inline formats/timeouts/headers; helpers own them.  
3. Standard error extraction; surface user-friendly toasts.  
4. Paginate and filter server-side when available.  
5. Keep tokens out of app-db; inject via headers in helpers.

## Helper Architecture

### Admin (`app.admin.frontend.utils.http`)

```clojure
(:require [app.admin.frontend.utils.http :as admin-http])

(admin-http/admin-get {:uri "/admin/api/audit"
                       :params {:page 1 :per-page 20}
                       :on-success [:admin/audit-loaded]
                       :on-failure [:admin/audit-load-failed]})

(admin-http/admin-post {:uri "/admin/api/users"
                        :params user
                        :on-success [:admin/user-saved]
                        :on-failure [:admin/user-save-failed]})
```

- Injects `x-admin-token: <token>` automatically (reads via auth persistence when available; falls back to localStorage).  
- JSON request/response formats configured centrally.  
- Timeouts: 10s default; exports may use longer where supported.  
- Error messages normalized via `admin-http/extract-error-message`.

### Template/Public (`app.template.frontend.api.http`)

Used only if the public `:app` build calls backend endpoints.

```clojure
(:require [app.template.frontend.api.http :as http])

(http/get-request {:uri "/api/health"
                   :on-success [:health/ok]
                   :on-failure [:health/fail]})
```

Defaults: JSON formats, 8s timeout, no tenant headers (single-tenant).

#### Entity CRUD: explicit public vs admin

The template/public HTTP namespace also exposes generic entity CRUD helpers.

- Public/template routes should call the **public** helpers (or the legacy wrappers):

```clojure
(http/create-entity-public {:entity-name "receipts" :data payload :on-success [:ok] :on-failure [:err]})
(http/update-entity-public {:entity-name "receipts" :id 123 :data payload :on-success [:ok] :on-failure [:err]})
(http/delete-entity-public {:entity-name "receipts" :id 123 :on-success [:ok] :on-failure [:err]})

;; Backward-compatible wrappers (public-only):
(http/create-entity {...})
(http/update-entity {...})
(http/delete-entity {...})
```

- Admin routes must use **explicit** admin helpers (admin entity routes are mounted under `/admin/api/entities/*`):

```clojure
(http/create-entity-admin {:entity-name "receipts" :data payload :on-success [:ok] :on-failure [:err]})
```

In practice, admin UI flows should prefer the CRUD bridge + admin adapters; the bridge defaults decide admin vs public based on route/path (never on presence of a token).

## Migration Patterns

### Before (inline, avoid)

```clojure
{:http-xhrio {:method :get
              :uri "/admin/api/audit"
              :headers {"x-admin-token" token} ; manual
              :format :json
              :response-format :json}}
```

### After (helper)

```clojure
{:http-xhrio (admin-http/admin-get
               {:uri "/admin/api/audit"
                :params {:page 1 :per-page 20}
                :on-success [:admin/audit-loaded]
                :on-failure [:admin/audit-load-failed]})}
```

## Error Handling

Use helper extractors and keep reducers slim.

```clojure
(rf/reg-event-db
  :admin/audit-load-failed
  (fn [db [_ err]]
    (let [msg (admin-http/extract-error-message err)]
      (assoc-in db [:entities :audit :error] msg))))
```

## Standards Checklist

- ✅ Use `admin-http` or `http`; no inline `:http-xhrio`.  
- ✅ Do not hand-roll headers or formats.  
- ✅ Use server pagination params (`:page`, `:per-page`, filters) where endpoints accept them.  
- ✅ Handle `:on-failure` with user-friendly messaging.  
- ✅ Prefer `:on-success` to dispatch adapter syncs before storing rows.

## Common Issues

- Missing require: `(:require [app.admin.frontend.utils.http :as admin-http])`.  
- Forgetting params map for pagination → default page 1/per-page may differ.  
- Storing tokens in app-db → avoid; let helpers read storage.

## Future

- Add request-level tracing toggle for admin-only debugging.  
- Add standardized export helper with longer timeout.  
- Centralize rate-limit/backoff handling if needed.

*Last Updated: 2025-11-26*
