<!-- ai: {:tags [:architecture :single-tenant] :kind :guide} -->

# Routing Architecture (Single-Tenant)

Admin-only app. Backend routes are under `/admin/api`; frontend routes are the admin SPA pages served from the same server.

## Backend (Reitit)
- Composed in `app.template.backend.routes.admin-api` at `/admin/api`.
- Public: `/auth/login`, `/auth/logout`.
- Protected (admin auth middleware):
  - `/dashboard`
  - `/users` (CRUD)
  - `/user-management/*` (role updates, verify email, reset password, impersonation, activity, search)
  - `/audit` (global audit logs)
  - `/login-events` (global login history)
  - `/expenses/*` (and any other enabled domain APIs) mounted under `/admin/api/<domain-id>` via `app.domain.backend.registry`
  - Dev: `/dev-get-rate-limits`, `/dev-clear-rate-limits`, `/test-put`
- Middleware layering: security headers/HTTPS → admin auth → JSON/error helpers (`admin.utils`) → handler.
- Conflict avoidance: keep literal routes (e.g., `/search`) separate from param routes (`/:id`) under clear prefixes as done in `user-management`.

## Frontend (Re-frame/Uix)
- Admin routes live in `app.admin.frontend.routes` (served at `/admin/*`).
- The template/app shell routes live in `app.template.frontend.routes` and are composed in `app.template.frontend.routes.data` from:
  - template shell routes
  - domain user routes from `app.domain.frontend.registry/all-user-routes`
  - admin routes from `app.admin.frontend.routes`
- Primary pages: `/admin/login`, `/admin`, `/admin/users`, `/admin/audit`, `/admin/login-events`.
- Pattern per page: on mount → dispatch load event → show table/cards using template components; responses come from the backend endpoints above.

## Serving
- Backend serves the SPA shell for `/admin/*`; API responses are JSON under `/admin/api/**`.

If you add new pages/endpoints, follow the same prefixing (`/admin/api/<area>`), update the router, and mirror the route in the admin SPA with a load event + table/view using shared components.
