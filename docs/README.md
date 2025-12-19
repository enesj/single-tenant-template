<!-- ai: {:tags [:overview :contributor :single-tenant] :kind :overview} -->

# Single-Tenant SaaS Template Docs

## Purpose

This repository is the **single-tenant template** extracted from the Hosting multi-tenant app. It retains shared/template/admin infrastructure and tooling but **does not include Hosting/Financial/Integration domains or tenant-aware RLS**. Hosting-specific docs remain as reference examples when you build your own domains.

## Quick Navigation

| Your Role | Essential Reading | Quick Start |
|-----------|-------------------|-------------|
| **New Developer** | `docs/backend/single-tenant-template.md` | `docs/operations/README.md#initial-setup` |
| **Backend** | `docs/backend/http-api.md`, `docs/backend/services.md` | `bb run-app`, `bb be-test` |
| **Frontend** | `docs/frontend/app-shell.md`, `docs/frontend/template-component-integration.md` | `npm run watch`, `bb fe-test` |
| **Admin UI** | `docs/frontend/admin-panel-single-tenant.md` | Open `http://localhost:8085/admin/users` |
| **Migrations/DB** | `docs/migrations/migration-overview.md` | REPL-based migrations, `bb backup-db` |

## Scope

- **This repo**: single-tenant defaults, admin/shared code, simplified admin auth, DB from `resources/db/models.edn`.
- **Hosting/Financial/Integration**: not present; any remaining docs with those names are reference-only and can be ignored for the template.

## Document Map

- **Architecture & System Design**
  - `docs/architecture/overview.md`
  - `docs/architecture/data-layer.md`
- **Backend**
  - `docs/backend/single-tenant-template.md` — canonical guide to this template
  - `docs/backend/http-api.md`, `docs/backend/services.md`, `docs/backend/security-middleware.md`
- **Frontend**
  - `docs/frontend/app-shell.md` — app shell, routing, builds
  - `docs/frontend/template-component-integration.md` — using template UI components
  - `docs/frontend/admin-panel-single-tenant.md` — single-tenant admin flow and extension points
- **Migrations & DB**
  - [Migrations](docs/migrations/migration-overview.md) - Hierarchical migration system (REPL-based)
  - `docs/reference/database-schema.md` — schema reference for template models
- **Operations**
  - `docs/operations/dev-environment.md` — commands, env, local workflow
  - [Deployment](docs/operations/hosting-overview.md) - Cloud hosting guide

## Getting Started (Template)

```bash
bb run-app          # start backend + shadow-cljs watch + nREPL
open http://localhost:8085/admin/users
bb be-test          # backend tests
bb fe-test          # frontend tests

# 🚨 ALWAYS save test output before analysis:
bb be-test 2>&1 | tee /tmp/be-test.txt
npm run test:cljs 2>&1 | tee /tmp/fe-test.txt
# Then analyze saved files - NEVER re-run tests!
```

## Metadata & RAG

- Single-tenant docs use tags like `:single-tenant`, `:template`, `:admin`.
- Hosting-only reference docs should be tagged `:hosting`, `:reference-only` for easy filtering.

---

For historical context and spin-out decisions, see `SINGLE_TENANT_PLAN.md`. Start with `docs/backend/single-tenant-template.md` to understand what’s included here and how to extend it.
