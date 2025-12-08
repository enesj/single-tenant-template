<!-- ai: {:namespaces [app.backend.services.* app.backend.routes.admin-*] :tags [:backend :architecture :single-tenant] :kind :guide} -->

# Backend Services (Single-Tenant)

This app runs a single-tenant admin backend. Multi-tenant domain services (hosting/financial/integration) were removed; only the services below are active.

## Service Map (high level)
- **Admin users** (`app.backend.services.admin.users`) – CRUD + role changes + email verification + password resets + impersonation support.
- **Admin audit** (`app.backend.services.admin.audit`) – write/list audit events; merges admin/user context and normalizes principal identifiers.
- **Monitoring: login events** (`app.backend.services.monitoring.login-events`) – record/list login attempts for admins/users, normalize principal info for UI.
- **Admin service façade** (`app.backend.services.admin`) – higher-level helpers for advanced operations (e.g., `get-user-activity`, role updates) used by `admin.user-operations` routes.
- **Shared helpers** – response coercion, pagination, and logging in `app.backend.routes.admin.utils`.

## Domain: Home Expenses Tracker (new)
- **Suppliers** (`app.domain.expenses.services.suppliers`) — CRUD, normalization/dedupe by `normalized_key`, search/count helpers.
- **Payers** (`app.domain.expenses.services.payers`) — CRUD, default-per-type management, suggestions from payment hints.
- **Receipts** (`app.domain.expenses.services.receipts`) — upload with file-hash dedupe, status transitions, approve → post expense, extraction storage.
- **Expenses** (`app.domain.expenses.services.expenses`) — create/update with line items, soft delete, listing filters; records price observations.
- **Articles/Aliases/Price history** (`app.domain.expenses.services.articles`, `price-history`) — canonical articles, alias mapping, price observation queries.
- **Reports** (`app.domain.expenses.services.reports`) — summary, payer/supplier breakdowns, weekly/monthly totals, top suppliers.
- **Routes** mounted under `/admin/api/expenses` via `app.domain.expenses.routes.core` (see `docs/backend/http-api.md` for endpoint map).

## How Routes Bind to Services
- `app.backend.routes.admin.users` → basic user CRUD.
- `app.backend.routes.admin.user-operations` → role update, force verify email, reset password, impersonation, activity aggregation, advanced search.
- `app.backend.routes.admin.audit` → global audit listing/export.
- `app.backend.routes.admin.login-events` → login event listing.

## Monitoring/Audit Data Shape
- **Audit events**: include `principal-id`, `principal-type` (`admin|user`), `action`, `metadata`, `created-at`. Use `admin-utils/log-admin-action` when adding new admin actions.
- **Login events**: include `principal-id`, `principal-type`, `success`, `reason`, `ip`, `user-agent`, `created-at`, and resolved `principal-name/email` when available.
- **User activity aggregation** (`admin-service/get-user-activity`): combines audit + login events and derived stats for the per-user modal. If you add new audit actions, keep names consistent so aggregation stays meaningful.

## Adding/Extending Services
- Reuse shared DB helpers and HoneySQL builders already present in service namespaces.
- Keep admin auth + security middleware in place; never expose `/admin/api/**` without `wrap-admin-authentication`.
- When adding a service method, expose it through a focused route namespace rather than a monolithic handler.
- Normalize response keys for the admin UI (plain `:id`, `:email`, `:name`, `:created-at`, `:principal-type`).

## Testing Notes
- Run `bb be-test` for backend tests.
- Prefer exercising new service functions through their route handlers in tests to cover middleware + serialization.
- Use the dev rate-limit endpoints to validate middleware ordering when adding new routes.
