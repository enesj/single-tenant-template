<!-- ai: {:tags [:architecture :single-tenant] :kind :guide} -->

# Routing Architecture (Single-Tenant)

Admin-only app. Backend routes are under `/admin/api`; frontend routes are the admin SPA pages served from the same server.

## Backend (Reitit)
- Composed in `app.backend.routes.admin-api` at `/admin/api`.
- Public: `/auth/login`, `/auth/logout`.
- Protected (admin auth middleware):
  - `/dashboard`
  - `/users` (CRUD)
  - `/user-management/*` (role updates, verify email, reset password, impersonation, activity, search)
  - `/audit` (global audit logs)
  - `/login-events` (global login history)
  - Dev: `/dev-get-rate-limits`, `/dev-clear-rate-limits`, `/test-put`
- Middleware layering: security headers/HTTPS → admin auth → JSON/error helpers (`admin.utils`) → handler.
- Conflict avoidance: keep literal routes (e.g., `/search`) separate from param routes (`/:id`) under clear prefixes as done in `user-management`.

## Frontend (Re-frame/Uix)
- Admin shell routes in `app.template.frontend.routes`; admin pages in `src/app/admin/frontend/pages`.
- Primary pages: `/admin/login`, `/admin`, `/admin/users`, `/admin/audit`, `/admin/login-events`.
- Pattern per page: on mount → dispatch load event → show table/cards using template components; responses come from the backend endpoints above.

## Serving
- Backend serves the SPA shell for `/admin/*`; API responses are JSON under `/admin/api/**`.

If you add new pages/endpoints, follow the same prefixing (`/admin/api/<area>`), update the router, and mirror the route in the admin SPA with a load event + table/view using shared components.
