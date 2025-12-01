<!-- ai: {:tags [:reference :glossary :single-tenant] :kind :reference} -->

# Glossary (Single-Tenant Admin App)

Key terms used across the single-tenant template, admin backend, and admin UI.

## Core actors
- **Admin** — Staff user who signs in to the admin panel, authenticated via `/admin/api/auth/login` and represented by `admins` table rows.
- **User** — End user managed by admins. Stored in `users` table; managed via `/admin/api/users` and `/admin/api/user-management/*`.
- **Admin Token** — Session token issued on admin login. Passed via `x-admin-token` header or session cookie for protected routes.
- **Impersonation** — Temporary user session created by an admin (`POST /admin/api/user-management/impersonate/:id`) for debugging/support.

## Monitoring
- **Audit Log** — Record of admin/user actions (`audit_logs` table). Includes actor/target, action, metadata, and request context. Listed via `/admin/api/audit` and per-user activity.
- **Login Event** — Record of successful/failed logins for admins and users (`login_events` table). Listed via `/admin/api/login-events` and per-user activity.
- **User Activity** — Aggregated view combining audit logs, login history, and summary counters for a single user (`GET /admin/api/user-management/activity/:id`).

## HTTP & data
- **Admin API** — All routes under `http://localhost:8085/admin/api` (development). JSON-only, admin-authenticated.
- **Pagination** — `page`/`per-page` for user listings; `limit`/`offset` for audit and login events.
- **Success Envelope** — Standard success responses use `{:success true :data ...}`; some handlers return top-level keys (e.g., `{:users [...]}`, `{:logs [...]}`).
- **Request Context** — IP and user-agent extracted for audit/login records; surfaced as `:ip-address` and `:user-agent` in responses.

## Frontend stack
- **Shadow CLJS** — Builds the ClojureScript admin UI.
- **Re-frame** — Event/subscription layer for admin UI state.
- **DaisyUI** — Component class set (prefixed `ds-`) used with Tailwind utilities in the admin UI.

## Database
- **UUID PK** — All primary keys are UUIDs; exposed as strings in JSON.
- **Enums** — `user-role`, `user-status`, `admin-role`, `admin-status`, `audit-actor-type`, `login-principal-type` (see `resources/db/models.edn`).
- **Canonical Schema** — Defined in `resources/db/models.edn`; migrations generated via `app.migrations.simple-repl`.

## Security
- **Admin Auth Middleware** — `wrap-admin-authentication` protects `/admin/api/**`; expects the admin token and sets `:admin` on the request map.
- **Rate Limiting (dev helper)** — Optional; inspect/reset via `/admin/api/dev-get-rate-limits` and `/admin/api/dev-clear-rate-limits` during local debugging.

---

**Related references**:  
- [HTTP API](api-reference.md)  
- [Database Schema](database-schema.md)  
- [Backend Security Middleware](../backend/security-middleware.md)
