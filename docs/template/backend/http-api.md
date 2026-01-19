<!-- ai: {:namespaces [app.template.backend.routes.admin-api app.template.backend.routes.api] :tags [:backend :http :template :single-tenant] :kind :reference} -->

# Template HTTP API (Overview)

This document captures **shared HTTP API shape** for the single-tenant template. It is the baseline for both admin and domain APIs.

- **Admin routes** live under `/admin/api` on **http://localhost:8085**. See [Admin HTTP API](../../admin/backend/http-api.md).
- **User routes** live under `/api/v1` (e.g. `/api/v1/expenses/**`). See [Domain HTTP APIs](../../domain/expenses/http-api.md).
- **Generic entity CRUD** lives under `/api/v1/entities/*` for allowlisted template entities. Domain entities that need ownership/business rules should use domain APIs and route template CRUD actions via a frontend bridge. See [Generic Entity CRUD API](./generic-entity-crud.md).

## Base Shape
- **Admin auth**: `app.template.backend.middleware.admin/wrap-admin-authentication` expects an admin token (dev mode may relax). Pass `x-admin-token: <token>` or the token cookie set by the admin login flow.
- **User auth**: mounted by the template with a `wrap-user-authentication` middleware (session/cookie-based in the template).
- **Content**: JSON request/response.
  - Admin endpoints typically return `{:success true :data ...}` / `{:success false :error {:message ...}}`.
  - User endpoints typically return `{:data ...}` / `{:error ...}`.
- **Middleware**: JSON parsing + security headers + auth. Rate-limiting hooks are available but may be disabled in dev.

## Notes for Contributors
- Add new admin endpoints under `src/app/template/backend/routes/admin/*` and compose them in `app.template.backend.routes.admin-api/admin-api-routes`.
- Add new user endpoints under `src/app/template/backend/routes/api/*` or the appropriate domain route namespaces.
- Keep responses normalized (`success-response`/`error-response` helpers in `admin-utils`).
