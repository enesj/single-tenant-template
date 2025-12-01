<!-- ai: {:tags [:backend :template :single-tenant] :kind :guide} -->

# Template Infrastructure (Backend)

The single-tenant app still reuses the template/shared stack, but without RLS/tenant switching. This doc replaces the prior multi-tenant primer.

## What We Reuse
- **DI container**: `app.template.di.config/create-service-container` builds the service map for backend startup.
- **CRUD/HTTP helpers**: shared response, coercion, JSON parsing, pagination, and error helpers in `app.backend.routes.admin.utils` and `app.shared.*`.
- **Validation/Schemas**: schema/validation utilities under `src/app/shared` and `src/app/template/shared`.
- **Frontend bridge**: template frontend components are used by the admin UI; this doc focuses on backend but note that data shapes match what the template UI expects (plain ids, names, emails, timestamps).

## What Changed for Single-Tenant
- No tenant context function or RLS policies; models live only in `resources/db/models.edn`.
- Service container wiring is focused on admin + monitoring services (users, audit, login-events) instead of domain packs.
- Routes are composed in `app.backend.routes.admin-api` rather than generic CRUD routers.
- Auth template helpers that created/switched tenants were removed; OAuth callback now only upserts a user record for this single-tenant flow.

## Extending Safely
- Add schema to `resources/db/models.edn`, then regenerate migrations via `app.migrations.simple-repl`.
- Register new services in the DI container (or pass them explicitly) and mount routes in `admin-api-routes`.
- Keep response shapes consistent with template UI (flat keys, keywordized JSON).
- Reuse shared helpers before adding new plumbing.

If you need the old multi-tenant infra details, see the archived docs in this folder tagged `:reference-only`; they are not used by this app.
