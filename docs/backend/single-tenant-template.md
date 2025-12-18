<!-- ai: {:tags [:backend :single-tenant :template] :kind :guide} -->

# Single-Tenant Template Guide (This Repo)

This document explains what the **single-tenant template** includes, how it differs from the Hosting multi-tenant app, and where to extend it.

## What’s Included

- **Backend entrypoint**: `app.template.backend.core`
  - Loads config via Aero (`config/base.edn`), builds HikariCP pool, wires DI with `app.template.di.config/create-service-container`.
  - Webserver setup in `app.template.backend.webserver`.
- **Frontend entrypoint**: `app.template.frontend.core`
  - Boots template routes and components, calls `app.admin.frontend.core/init-admin!`, mounts `current-page`.
  - Admin routes: `/admin/login`, `/admin`, `/admin/users`, `/admin/audit`, `/admin/login-events`.
- **Domain sample (new)**: Home Expenses Tracker domain lives under:
  - Backend: `src/app/domain/backend/expenses` (services + routes mounted at `/admin/api/expenses`)
  - Frontend: `src/app/domain/frontend/expenses` (admin-facing pages/components wired into the admin SPA)
- **Admin UI**: `src/app/admin/frontend/*` with list/form patterns and templates.
- **Template/shared libs**: `src/app/template/*`, `src/app/shared/*` (components, validation, schemas, HTTP, CRUD helpers).
- **Database models**: source EDN under `resources/db/{template,shared,domain}/**` merged into `resources/db/models.edn` (single-tenant; includes audit/login event tables + domain tables).
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
- Template backend/runtime concerns: add routes/middleware/webserver glue under `src/app/template/backend`.
- Admin-only backend services live under `src/app/admin/backend`.
- Domain backend code lives under `src/app/domain/backend/<your-domain>`.
- Wire new routes in `app.template.backend.routes` / `app.template.backend.routes.admin-api` and register services in the DI container (`app.template.di.config`).
- Reuse shared response/HTTP utilities in `src/app/shared`.

### Frontend
- Add pages under `src/app/admin/frontend/pages` (admin) or `src/app/template/frontend/pages` (public).
- Use list/form templates and components from `src/app/template/frontend/components` and `src/app/admin/frontend/components`.
- Routing: update `app.template.frontend.routes` and include your page/view in `current-page`.

### Database & Migrations
- Do **not** edit `resources/db/models.edn` directly (it is generated).
- Edit source files under `resources/db/{template,shared,domain}/**` (e.g. `resources/db/domain/models.edn`).
- Generate/apply migrations via the REPL helper (`app.template.backend.migrations.simple-repl`) or via the bb/clj tasks documented in `docs/migrations/*`.
- Use `bb backup-db`, `bb restore-db` for safety.
- RLS: this template runs single-tenant by default. If you add tenant-aware features, follow the Hosting docs for RLS patterns.

## Single-Tenant Auth Notes

- `app.template.frontend.components.auth-guard` treats `nil` admin auth as allowed in this template to keep the admin UI usable without full auth wiring.
- Replace or tighten this behavior when you add real auth.
- Backend OAuth callback (`app.template.backend.auth.service/process-oauth-callback`) simply upserts a user and returns session-ish data for the frontend. It does not create/switch tenants or run onboarding flows; those template helpers were removed for this single-tenant setup.

## Code Pointers (Template)

| Area | Namespaces/Files |
|------|------------------|
| Backend entry | `src/app/template/backend/core.clj`, `src/app/template/backend/webserver.clj` |
| DI container | `src/app/template/di/config.clj` |
| Admin pages | `src/app/admin/frontend/pages/*` |
| Frontend shell | `src/app/template/frontend/core.cljs`, `src/app/template/frontend/routes.cljs` (entrypoint) + `src/app/template/frontend/routes/` (implementation) |
| Template UI | `src/app/template/frontend/components/*` |
| Shared libs | `src/app/shared/*` |
| Migrations | `resources/db/models.edn`, `docs/migrations/migration-overview.md` |

## Hosting Docs as Reference

Legacy hosting/financial/integration docs are tagged `:reference-only` and kept only for historical context. They do not apply to this repo’s single-tenant, monitoring-focused setup.
