<!-- ai: {:tags [:architecture :single-tenant] :kind :guide} -->

# Backend Runtime (Single-Tenant)

How an admin request flows through the stack on port 8085.

## Request Pipeline
```
Client → Jetty/Ring → security middleware → admin auth → JSON parsing → route handler → services → PostgreSQL → response
```

## Server Startup
- Entry: `app.backend.core/-main` loads config (`config/base.edn`) and starts `app.backend.webserver`.
- Admin UI + API served from the same Jetty instance.

## Middleware Order
Applied in `app.backend.middleware.security` and admin routes:
1) HTTPS enforcement (skipable in dev via `DISABLE_HTTPS_REDIRECT`).
2) [Optional] Rate limiting (dev helpers at `/admin/api/dev-*`).
3) Security headers.
4) Admin auth: `app.backend.middleware.admin/wrap-admin-authentication` inside `/admin/api/**`.
5) JSON/body parsing and error handling (`app.backend.routes.admin.utils`).

## Routing
- Composed in `app.backend.routes.admin-api` under `/admin/api`:
  - `/auth/*` (login/logout)
  - `/dashboard`
  - `/users` (CRUD)
  - `/user-management/*` (roles, verify email, reset password, impersonation, activity, search)
  - `/audit` (global audit logs)
  - `/login-events` (global login history)
  - Dev helpers: `/dev-get-rate-limits`, `/dev-clear-rate-limits`, `/test-put`

## Handler Pattern
- Validate/parse params → call service → normalize keys → `success-response`/`error-response` (in `admin.utils`).
- Logging: see `🌐 GLOBAL REQUEST DEBUG` and `🔐 ADMIN AUTH CHECK` logs for traceability.

## Services Touched per Flow
- **Users**: `app.backend.services.admin.users`
- **Advanced user ops/activity**: `app.backend.services.admin`
- **Audit logs**: `app.backend.services.admin.audit`
- **Login events**: `app.backend.services.monitoring.login-events`

## Data Access
- Single schema (no RLS/tenant context). Use HoneySQL builders in services + `next.jdbc` adapters.
- Convert PG objects before responding to keep JSON clean.

## Dev Notes
- Run: `bb run-app`; Admin UI: `http://localhost:8085/admin`
- Quick sanity: `PUT /admin/api/test-put`
- Tests: `bb be-test`, `bb fe-test`
