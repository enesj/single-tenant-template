<!-- ai: {:tags [:overview :contributor :single-tenant] :kind :overview} -->

# Single-Tenant SaaS Template Docs

## Purpose

This repository is the **single-tenant template** extracted from the Hosting multi-tenant app. It retains shared/template/admin infrastructure and tooling but **does not include Hosting/Financial/Integration domains or tenant-aware RLS**. Hosting-specific docs remain as reference examples when you build your own domains.

## Quick Navigation

| Your Role | Essential Reading | Quick Start |
|-----------|-------------------|-------------|
| **New Developer** | `../template/backend/single-tenant-template.md` | `./operations/README.md#initial-setup` |
| **Backend** | `../template/backend/http-api.md`, `../admin/backend/services.md` | `bb run-app`, `bb be-test` |
| **Frontend** | `../template/frontend/app-shell.md`, `../template/frontend/template-component-integration.md` | `npm run watch`, `bb fe-test` |
| **Admin UI** | `../admin/frontend/admin-panel-single-tenant.md` | Open `http://localhost:8085/admin/users` |
| **Migrations/DB** | `./migrations/migration-overview.md` | REPL-based migrations, `bb backup-db` |

## Scope

- **This repo**: single-tenant defaults, admin/shared code, simplified admin auth, DB from `resources/db/models.edn`.
- **Hosting/Financial/Integration**: not present; any remaining docs with those names are reference-only and can be ignored for the template.

## Document Map

- **Architecture & System Design**
  - `./architecture/overview.md`
  - `./architecture/data-layer.md`
- **Backend**
  - `../template/backend/single-tenant-template.md` — canonical guide to this template
  - `../template/backend/http-api.md`, `../admin/backend/services.md`, `../template/backend/security-middleware.md`
- **Frontend**
  - `../template/frontend/app-shell.md` — app shell, routing, builds
  - `../template/frontend/template-component-integration.md` — using template UI components
  - `../admin/frontend/admin-panel-single-tenant.md` — single-tenant admin flow and extension points
- **Migrations & DB**
  - [Migrations](./migrations/migration-overview.md) - Hierarchical migration system (REPL-based)
  - `./reference/database-schema.md` — schema reference for template models
- **Operations**
  - `./operations/dev-environment.md` — commands, env, local workflow
  - [Deployment](./operations/hosting-overview.md) - Cloud hosting guide

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

For historical context and spin-out decisions, see `SINGLE_TENANT_PLAN.md`. Start with `../template/backend/single-tenant-template.md` to understand what’s included here and how to extend it.
