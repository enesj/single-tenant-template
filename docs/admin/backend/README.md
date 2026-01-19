<!-- ai: {:tags [:backend :admin :overview :single-tenant] :kind :overview} -->

# Admin Backend Docs

Admin backend services and endpoints that power the admin console.

## What to Read
- **[http-api.md](http-api.md)** – admin API surface (`/admin/api/**`, port 8085).
- **[services.md](services.md)** – admin services (users, audit/logging, dashboard, login events).
- **[security-middleware.md](../../template/backend/security-middleware.md)** – HTTPS/security headers/rate limit + admin auth hooks.

## Current Architecture (concise)
- **Admin-only surface** on port **8085**:
  - `/admin/api/user-management/*` – users + per-user activity (audit + login history).
  - `/admin/api/audit` – global audit log stream/filter/export.
  - `/admin/api/login-events` – global login events (admins + users) with filters.
  - `/admin/api/auth/*` – admin auth (token exchange, logout).
- **Monitoring/logging**: login events and audit logs persisted via dedicated services; queries normalize principal type/id/email/name.
- **Middleware**: HTTPS redirect (prod), security headers, optional rate limiting, admin auth check, JSON coercion.

## Development Quick Links
- Start stack: `bb run-app` (backend + shadow-cljs + nREPL). Admin UI at `http://localhost:8085/admin`.
- Tests: `bb be-test`, `bb fe-test`; format via `bb cljfmt-check`.

## Adding Features Safely
- Keep admin security middleware in place; don’t bypass auth on `/admin/api/**`.
- When adding endpoints, document them in `http-api.md` and extend `services.md`.
