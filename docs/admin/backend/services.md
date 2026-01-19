<!-- ai: {:namespaces [app.admin.backend.services.* app.template.backend.services.monitoring.login-events app.template.backend.routes.admin-*] :tags [:backend :architecture :admin :single-tenant] :kind :guide} -->

# Admin Backend Services

This document covers admin-only services that power the admin console (users, audit, login events, dashboard, integrations).

Domain services live in their respective domain docs (see [Expenses Services](../../domain/expenses/backend-services.md)).

## Service Map (high level)
- **Admin users** (`app.admin.backend.services.admin.users`) – CRUD + role changes + email verification + password resets + impersonation support.
- **Admin audit** (`app.admin.backend.services.admin.audit`) – write/list audit events; merges admin/user context and normalizes principal identifiers.
- **Admin auth** (`app.admin.backend.services.admin.auth`) – session creation/validation for admin login flows.
- **Admin admins** (`app.admin.backend.services.admin.admins`) – manage admin accounts and metadata.
- **Admin dashboard** (`app.admin.backend.services.admin.dashboard`) – summary stats for the admin landing page.
- **Monitoring: integrations** (`app.admin.backend.services.admin.monitoring.integrations`) – integration status and performance data.
- **Monitoring: login events** (`app.template.backend.services.monitoring.login-events`) – record/list login attempts for admins/users, normalize principal info for UI.
- **Shared helpers** – response coercion, pagination, and logging in `app.template.backend.routes.admin.utils`.

## How Routes Bind to Services
- `app.template.backend.routes.admin.users` → basic user CRUD.
- `app.template.backend.routes.admin.user-operations` → role update, force verify email, reset password, impersonation, activity aggregation, advanced search.
- `app.template.backend.routes.admin.audit` → global audit listing/export.
- `app.template.backend.routes.admin.login-events` → login event listing.

## Monitoring/Audit Data Shape
- **Audit events**: include `principal-id`, `principal-type` (`admin|user`), `action`, `metadata`, `created-at`. Use `admin-utils/log-admin-action` when adding new admin actions.
- **Login events**: include `principal-id`, `principal-type`, `success`, `reason`, `ip`, `user-agent`, `created-at`, and resolved `principal-name/email` when available.
- **User activity aggregation** (`app.admin.backend.services.admin.users/get-user-activity`): combines audit + login events and derived stats for the per-user modal. If you add new audit actions, keep names consistent so aggregation stays meaningful.

## Adding/Extending Services
- Reuse shared DB helpers and HoneySQL builders already present in service namespaces.
- Keep admin auth + security middleware in place; never expose `/admin/api/**` without `wrap-admin-authentication`.
- When adding a service method, expose it through a focused route namespace rather than a monolithic handler.
- Normalize response keys for the admin UI (plain `:id`, `:email`, `:name`, `:created-at`, `:principal-type`).

## Testing Notes
- Run `bb be-test` for backend tests.
- Prefer exercising new service functions through their route handlers in tests to cover middleware + serialization.
- Use the dev rate-limit endpoints to validate middleware ordering when adding new routes.
