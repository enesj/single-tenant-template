<!-- ai: {:tags [:overview :architecture :single-tenant] :kind :overview} -->

# Single-Tenant SaaS Template — Documentation

## Overview

This repository is the **single-tenant template** extracted from the Hosting multi-tenant app. It keeps the shared/template/admin infrastructure (Clojure/ClojureScript, PostgreSQL, Shadow-CLJS, Babashka tooling) but ships **without Hosting/Financial/Integration business domains or tenant-aware RLS**. Hosting-specific docs remain as reference when you want examples of full domains.

## Quick Start

1. **Dev setup** → `docs/operations/README.md#initial-setup`
2. **Template architecture** → `docs/architecture/overview.md` (with “Template vs Hosting” notes)
3. **Template guide** → `docs/backend/single-tenant-template.md`
4. **Frontend/app shell** → `docs/frontend/app-shell.md`
5. **Migrations** → `docs/migrations/migration-overview.md`

Common tasks:
- Start stack: `bb run-app` (serves app + admin UI)
- Admin UI: `http://localhost:8085/admin/users` (admin auth simplified for template)
- Tests: `bb be-test`, `bb fe-test`

## Documentation by Role

**Backend**
- `docs/backend/single-tenant-template.md` — what this template includes and how to extend it
- `docs/backend/http-api.md` — HTTP surfaces and admin routes
- `docs/backend/services.md` — service protocols and composition
- `docs/backend/security-middleware.md` — request pipeline (see notes on single-tenant vs hosting)

**Frontend**
- `docs/frontend/app-shell.md` — app shell, routing, Shadow-CLJS builds
- `docs/frontend/template-component-integration.md` — using template UI components
- `docs/frontend/feature-guides/admin.md` — admin panel patterns (users/audit)
- `docs/frontend/admin-panel-single-tenant.md` — (new) single-tenant admin flow and extension points

**Operations**
- `docs/operations/README.md` — commands, env, deployment notes
- `docs/migrations/migration-overview.md` — models/migrations workflow

**Reference / Hosting examples (not present in this repo)**
- Backend domains: `docs/backend/hosting-domain.md`, `docs/backend/financial-domain.md`, `docs/backend/integration-domain.md`
- Frontend feature guides: `docs/frontend/feature-guides/hosting.md`, `billing.md`, `integrations.md`

## Template vs Hosting (at a glance)

- **Template scope (this repo)**: single-tenant; template/admin/shared code only; simplified admin auth; DB defined in `resources/db/models.edn`.
- **Hosting reference**: multi-tenant RLS, tenant context middleware, full property/financial/integration domains. Use these docs as examples when adding your own domains.

## Architecture Snapshot (template)

```
Browser → app.template.frontend.core → admin/template routes → services/DI → PostgreSQL (single-tenant)
```

- Frontend: Re-frame + UIX, routes in `app.template.frontend.routes`, admin bootstrap in `app.admin.frontend.core`.
- Backend: `app.backend.core` with DI container `app.template.di.config`.
- DB: migrations sourced from `resources/db/models.edn`; use `clj -X:migrations-dev` / `bb` tasks.

## Development Navigation

| Area | Key Docs | Code Pointers |
|------|----------|---------------|
| Admin (template) | `docs/frontend/feature-guides/admin.md`, `docs/frontend/admin-panel-single-tenant.md` | `src/app/admin/frontend` |
| Template frontend | `docs/frontend/app-shell.md`, `docs/frontend/template-component-integration.md` | `src/app/template/frontend` |
| Backend core | `docs/backend/single-tenant-template.md`, `docs/backend/http-api.md` | `src/app/backend` |
| Migrations/DB | `docs/migrations/migration-overview.md` | `resources/db/*` |
| Hosting reference | Domain docs listed above | *Hosting repo only* |

## Ops & Tooling (template defaults)

- Babashka tasks: see `docs/operations/README.md` (`bb run-app`, `bb be-test`, `bb fe-test`, `bb backup-db`, `bb restore-db`).
- Shadow-CLJS: `npm run watch` / `npm run build`.
- Ports: defaults to 8085 for app/admin (adjust per your config).

## Linking & Metadata

- Single-tenant docs use tags like `:single-tenant` and `:template`.
- Hosting-only docs are tagged `:hosting` and `:reference-only` so they can be filtered out.

---

Start with `docs/backend/single-tenant-template.md` to understand what is included here, then follow `docs/operations/README.md` to run the stack. For domain examples, consult the Hosting reference docs called out above.
