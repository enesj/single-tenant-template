<!-- ai: {:tags [:backend :template :overview :single-tenant] :kind :overview} -->

# Template Backend Docs

This folder documents the **single-tenant template** backend: routing, shared infrastructure, and extension points used by admin and domain code.

## What to Read
- **[single-tenant-template.md](single-tenant-template.md)** – repo-level guide and extension points.
- **[http-api.md](http-api.md)** – shared HTTP API shape and generic entity CRUD pointers.
- **[generic-entity-crud.md](generic-entity-crud.md)** – allowlisted CRUD surface under `/api/v1/entities/*`.
- **[security-middleware.md](security-middleware.md)** – HTTPS/security headers/rate limit + auth hooks.
- **[template-infrastructure.md](template-infrastructure.md)** – how we reuse template/shared libs in a single-tenant setup.

## Development Quick Links
- Start stack: `bb run-app` (backend + shadow-cljs + nREPL). Admin UI at `http://localhost:8085/admin`.
- Migrations: `app.template.backend.migrations.simple-repl` (`mig/make-all-migrations!`, `mig/migrate!`).
  - Frontend config alignment: `(mig/migrate! :dev {:sync-frontend-config? true})` or `bb migrate-and-sync-frontend-config`.
- Tests: `bb be-test`, `bb fe-test`; format via `bb cljfmt-check`.

## Adding Features Safely
- Reuse template/shared helpers before introducing new plumbing.
- Prefer new tables in `resources/db/models.edn`; regenerate migrations instead of hand-editing migration files.
- Register new services in the DI container and mount routes in `admin-api-routes` or domain registries.
