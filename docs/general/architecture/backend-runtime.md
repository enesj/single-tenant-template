<!-- ai: {:tags [:architecture :single-tenant] :kind :guide} -->

# Backend Runtime (Single-Tenant)

How an admin request flows through the stack on port 8085.

## Request Pipeline
```
Client → http-kit/Ring → security middleware → admin auth → JSON parsing → route handler → services → PostgreSQL → response
```

## Server Startup
- Entry: `app.template.backend.core/main` loads config (`config/base.edn` via Aero) and starts `app.template.backend.webserver/create-webserver`.
- Admin UI + API served from the same http-kit instance.

## Middleware Order
Applied in `app.template.backend.middleware.security` and admin routes:
1) HTTPS enforcement (skipable in dev via `DISABLE_HTTPS_REDIRECT`).
2) [Optional] Rate limiting (dev helpers at `/admin/api/dev-*`).
3) Security headers.
4) Admin auth: `app.template.backend.middleware.admin/wrap-admin-authentication` inside `/admin/api/**`.
5) JSON/body parsing and error handling (`app.template.backend.routes.admin.utils`).

## Routing
- Composed in `app.template.backend.routes.admin-api` under `/admin/api`:
  - `/login` and `/logout` (admin session)
  - `/auth/*` (password flows)
  - `/dashboard`
  - `/users` (CRUD)
  - `/user-management/*` (roles, verify email, reset password, activity, search)
  - `/audit` (global audit logs)
  - `/login-events` (global login history)
  - Dev helpers: `/dev-get-rate-limits`, `/dev-clear-rate-limits`, `/test-put`

## Handler Pattern
- Validate/parse params → call service → normalize keys → `success-response`/`error-response` (in `app.template.backend.routes.admin.utils`).
- Logging: see `🌐 GLOBAL REQUEST DEBUG` and `🔐 ADMIN AUTH CHECK` logs for traceability.

## Services Touched per Flow
- **Users**: `app.admin.backend.services.admin.users`
- **Advanced user ops/activity**: `app.admin.backend.services.admin`
- **Audit logs**: `app.admin.backend.services.admin.audit`
- **Login events**: `app.template.backend.services.monitoring.login-events`

## Data Access
- Single schema (no RLS/tenant context). Use HoneySQL builders in services + `next.jdbc` adapters.
- Convert PG objects before responding to keep JSON clean.

## Dev Notes
- Run: `bb run-app`; Admin UI: `http://localhost:8085/admin`
- Quick sanity: `PUT /admin/api/test-put`
- Tests: `bb be-test`, `bb fe-test`
