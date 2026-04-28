<!-- ai: {:tags [:architecture :single-tenant] :kind :guide} -->

# Routing Architecture (Single-Tenant)

Single-tenant app with two surfaces:

- **Admin**: SPA under `/admin/*`, API under `/admin/api/*`
- **User**: pages under domain routes (e.g. `/expenses/*`), API under `/api/v1/*` (including the generic `/api/v1/entities/*` surface)

The same server serves both.

## Backend (Reitit)

- **Admin API**: composed in `app.template.backend.routes.admin-api` at `/admin/api`.
  - Public: `/login`, `/logout`.
  - Protected (admin auth middleware):
    - `/dashboard`
    - `/users` (CRUD)
    - `/user-management/*` (role updates, verify email, reset password, activity, search)
    - `/audit` (global audit logs)
    - `/login-events` (global login history)
    - `/expenses/*` (and any other enabled domain APIs) mounted under `/admin/api/<domain-id>` via `app.domain.backend.registry`
    - Dev: `/dev-get-rate-limits`, `/dev-clear-rate-limits`, `/test-put`

- **User API**: composed in `app.template.backend.routes.api` at `/api/v1`.
  - Domain user APIs are mounted under `/api/v1/<domain>` (e.g. `/api/v1/expenses/*`).
  - Generic entity CRUD is mounted under `/api/v1/entities/*` and is deny-by-default (allowlisted). See [Generic Entity CRUD API](../backend/generic-entity-crud.md).

- Middleware layering: security headers/HTTPS → auth → JSON/error helpers → handler.
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
- Backend serves SPA shells for `/admin/*` and user pages (domain routes like `/expenses/*`). API responses are JSON under `/admin/api/**` and `/api/v1/**`.

If you add new pages/endpoints, follow the same prefixing (`/admin/api/<area>`), update the router, and mirror the route in the admin SPA with a load event + table/view using shared components.
