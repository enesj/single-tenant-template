<!-- ai: {:tags [:backend :single-tenant :template] :kind :guide} -->

# Single-Tenant Template Guide (This Repo)

This document explains what the **single-tenant template** includes, how it differs from the Hosting multi-tenant app, and where to extend it.

## What’s Included

- **Backend entrypoint**: `app.template.backend.core`
  - Loads config via Aero (`config/base.edn`), builds HikariCP pool, wires DI with `app.template.di.config/create-service-container`.
  - Webserver setup in `app.template.backend.webserver`.
- **Frontend entrypoints**:
  - Shared SPA bootstrap: `app.template.frontend.core` (used by both Shadow builds `:app` and `:admin`)
  - Admin module: `app.admin.frontend.core` (initialized lazily via `init-admin!` when current route/URL is under `/admin/*`)
- **Domain sample (new)**: Home Expenses Tracker domain lives under:
  - Backend: `src/app/domain/backend/expenses` (services + routes mounted at `/admin/api/expenses`)
  - Frontend: `src/app/domain/frontend/expenses` (user routes/pages, admin adapters/subs, and domain-owned config)
- **Admin UI**: `src/app/admin/frontend/*` with list/form patterns and templates.
- **Template/shared libs**: `src/app/template/*`, `src/app/shared/*` (components, validation, schemas, HTTP, CRUD helpers).
- **Database models**: source EDN under `resources/db/{template,shared,domain}/**` merged into `resources/db/models.edn` (single-tenant; includes audit/login event tables + domain tables).
- **Tooling**: Babashka tasks (`bb run-app`, tests, lint), Shadow-CLJS builds, nREPL-ready dev loop.

## What’s NOT Included (Hosting Reference Only)

- Hosting/Financial/Integration domains from the multi-tenant app, RLS-heavy tenant context middleware, multi-tenant role matrix.
- These topics remain documented for reference in Hosting docs and are tagged `:hosting`/`:reference-only`.

## Running the Template

```bash
bb run-app          # start backend + shadow-cljs watch + nREPL
open http://localhost:8085/admin/users
bb be-test          # backend tests
bb fe-test-node     # frontend tests
```

Ports and DB names come from `config/base.edn` (dev defaults to port 8085 unless changed).

## Where to Extend

### Backend
- Template backend/runtime concerns: add routes/middleware/webserver glue under `src/app/template/backend`.
- Admin-only backend services live under `src/app/admin/backend`.
- Domain backend code lives under `src/app/domain/backend/<your-domain>`.
- Prefer wiring new domain APIs via the backend domain registry (`app.domain.backend.registry`) so template/admin remain domain-agnostic.
- Register new backend services in the DI container (`app.template.di.config`) as needed.
- Reuse shared response/HTTP utilities in `src/app/shared`.

### Frontend
- Add admin-only pages under `src/app/admin/frontend/pages` (infrastructure/admin shell).
- Add concrete domain pages/events/subs under `src/app/domain/frontend/<your-domain>`.
- Use list/form templates and components from `src/app/template/frontend/components` and `src/app/admin/frontend/components`.
- Routing:
  - User routes are contributed via `app.domain.frontend.registry` (see `all-user-routes`).
  - Domain page components are aggregated via `app.domain.frontend.pages` to avoid circular deps.

### Database & Migrations
- Do **not** edit `resources/db/models.edn` directly (it is generated).
- Edit source files under `resources/db/{template,shared,domain}/**` (e.g. `resources/db/domain/models.edn`).
- Generate/apply migrations via the REPL helper (`app.template.backend.migrations.simple-repl`) or via the bb/clj tasks documented in `../../general/migrations/*`.
- Frontend-config alignment (migration-adjacent):
  - REPL opt-in: `(mig/migrate! :dev {:sync-frontend-config? true})`
  - One-shot command: `bb migrate-and-sync-frontend-config`
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
| Domain registries | `src/app/domain/backend/registry.clj`, `src/app/domain/frontend/registry.cljs`, `src/app/domain/frontend/pages.cljs` |
| Template UI | `src/app/template/frontend/components/*` |
| Shared libs | `src/app/shared/*` |
| Migrations | `resources/db/models.edn`, `../../general/migrations/migration-overview.md` |

## Hosting Docs as Reference

Legacy hosting/financial/integration docs are tagged `:reference-only` and kept only for historical context. They do not apply to this repo’s single-tenant, monitoring-focused setup.
