<!-- ai: {:tags [:backend :single-tenant :template] :kind :guide} -->

# Single-Tenant Template Guide (This Repo)

This document explains what the **single-tenant template** includes, how it differs from the Hosting multi-tenant app, and where to extend it.

## What’s Included

- **Backend entrypoint**: `app.backend.core`
  - Loads config via Aero (`config/base.edn`), builds HikariCP pool, wires DI with `app.template.di.config/create-service-container`.
  - Webserver setup in `app.backend.webserver`.
- **Frontend entrypoint**: `app.template.frontend.core`
  - Boots template routes and components, calls `app.admin.frontend.core/init-admin!`, mounts `current-page`.
  - Admin routes: `/admin/login`, `/admin`, `/admin/users`, `/admin/audit`, `/admin/login-events`.
- **Domain sample (new)**: Home Expenses Tracker backend domain under `src/app/domain/expenses` with services/routes mounted at `/admin/api/expenses` (suppliers, payers, receipts, expenses, articles/price-history, reports). Frontend pages are pending.
- **Admin UI**: `src/app/admin/frontend/*` with list/form patterns and templates.
- **Template/shared libs**: `src/app/template/*`, `src/app/shared/*` (components, validation, schemas, HTTP, CRUD helpers).
- **Database models**: `resources/db/models.edn` (single-tenant; includes audit/login event tables).
- **Tooling**: Babashka tasks (`bb run-app`, tests, lint), Shadow-CLJS builds, nREPL-ready dev loop.

## What’s NOT Included (Hosting Reference Only)

- Hosting/Financial/Integration domain code (`src/app/domain/*`), RLS-heavy tenant context middleware, multi-tenant role matrix.
- Domain-specific migrations under `resources/db/domain/*` (removed).
- These topics remain documented for reference in Hosting docs and are tagged `:hosting`/`:reference-only`.

## Running the Template

```bash
bb run-app          # start backend + shadow-cljs watch + nREPL
open http://localhost:8085/admin/users
bb be-test          # backend tests
bb fe-test          # frontend tests
```

Ports and DB names come from `config/base.edn` (dev defaults to port 8085 unless changed).

## Where to Extend

### Backend
- Add services/routes under `src/app/backend` or introduce your own domain namespaces (e.g., `src/app/domain/<your-domain>/backend`).
- Wire new routes in `app.backend.routes` (or add a new routes ns) and register services in the DI container (`app.template.di.config`).
- Reuse shared response/HTTP utilities in `src/app/shared`.

### Frontend
- Add pages under `src/app/admin/frontend/pages` (admin) or `src/app/template/frontend/pages` (public).
- Use list/form templates and components from `src/app/template/frontend/components` and `src/app/admin/frontend/components`.
- Routing: update `app.template.frontend.routes` and include your page/view in `current-page`.

### Database & Migrations
- Edit `resources/db/models.edn` to add tables/fields.
- Generate/apply migrations:
  - `clj -X:migrations-dev` (dev) / `clj -X:migrations-test` (test)
  - `bb backup-db`, `bb restore-db` for safety.
- RLS: this template runs single-tenant by default. If you add tenant-aware features, follow the Hosting docs for RLS patterns.

## Single-Tenant Auth Notes

- `app.template.frontend.components.auth-guard` treats `nil` admin auth as allowed in this template to keep the admin UI usable without full auth wiring.
- Replace or tighten this behavior when you add real auth.
- Backend OAuth callback (`app.template.backend.auth.service/process-oauth-callback`) simply upserts a user and returns session-ish data for the frontend. It does not create/switch tenants or run onboarding flows; those template helpers were removed for this single-tenant setup.

## Code Pointers (Template)

| Area | Namespaces/Files |
|------|------------------|
| Backend entry | `src/app/backend/core.clj`, `src/app/backend/webserver.clj` |
| DI container | `src/app/template/di/config.clj` |
| Admin pages | `src/app/admin/frontend/pages/*` |
| Frontend shell | `src/app/template/frontend/core.cljs`, `src/app/template/frontend/routes.cljs` |
| Template UI | `src/app/template/frontend/components/*` |
| Shared libs | `src/app/shared/*` |
| Migrations | `resources/db/models.edn`, `docs/migrations/migration-overview.md` |

## Hosting Docs as Reference

Legacy hosting/financial/integration docs are tagged `:reference-only` and kept only for historical context. They do not apply to this repo’s single-tenant, monitoring-focused setup.
