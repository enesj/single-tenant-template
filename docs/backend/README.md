<!-- ai: {:tags [:backend :overview :single-tenant] :kind :overview} -->

# Backend Docs (Single-Tenant)

This folder now documents the **single-tenant** backend that powers the admin console (users, monitoring, audit/login events). Multi-tenant and domain-partitioned content has been removed or parked as reference.

## What to Read
- **[http-api.md](http-api.md)** – current admin API surface (`/admin/api/**`, port 8085).
- **[services.md](services.md)** – service map (admin users, audit/logging, monitoring/login-events, template helpers).
- **[security-middleware.md](security-middleware.md)** – HTTPS/security headers/rate limit + admin auth hooks.
- **[template-infrastructure.md](template-infrastructure.md)** – how we reuse template/shared libs in a single-tenant setup.
- **[single-tenant-template.md](single-tenant-template.md)** – repo-level guide and extension points.

Files tagged as “reference-only” (financial/hosting/integration/admin-billing) describe the old multi-tenant app; keep them out of day-to-day development.

## Current Architecture (concise)
- **Single tenant**: no tenant context or RLS switching; DB models live in `resources/db/models.edn`.
- **Admin-only surface** on port **8085**:
  - `/admin/api/user-management/*` – users + per-user activity (audit + login history).
  - `/admin/api/audit` – global audit log stream/filter/export.
  - `/admin/api/login-events` – global login events (admins + users) with filters.
  - `/admin/api/auth/*` – admin auth (token exchange, logout) if enabled.
- **Monitoring/logging**: login events and audit logs persisted via dedicated services; queries normalize principal type/id/email/name.
- **Middleware**: HTTPS redirect (prod), security headers, optional rate limiting, admin auth check, JSON coercion.
- **Data model**: core tables for admins/users plus `audit_events`/`login_events` (see `models.edn` and migrations).

## Development Quick Links
- Start stack: `bb run-app` (backend + shadow-cljs + nREPL). Admin UI at `http://localhost:8085/admin`.
- Migrations: `app.migrations.simple-repl` (`mig/make-all-migrations!`, `mig/migrate!`).
- Tests: `bb be-test`, `bb fe-test`; format via `bb cljfmt-check`.

## Adding Features Safely
- Reuse template/shared helpers before introducing new plumbing.
- Keep admin security middleware in place; don’t bypass auth on `/admin/api/**`.
- When adding endpoints, document them in `http-api.md` and extend `services.md`.
- Prefer new tables in `resources/db/models.edn`; regenerate migrations instead of hand-editing migration files.
