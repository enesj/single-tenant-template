<!-- ai: {:tags [:overview :architecture :single-tenant] :kind :overview} -->

# Single-Tenant SaaS Template — Documentation

## Overview

This repository is the **single-tenant template** extracted from the Hosting multi-tenant app. It keeps the shared/template/admin infrastructure (Clojure/ClojureScript, PostgreSQL, Shadow-CLJS, Babashka tooling) but ships **without Hosting/Financial/Integration business domains or tenant-aware RLS**. Hosting-specific docs remain as reference when you want examples of full domains.

**New (2025-12-08):** Home Expenses Tracker domain is included and exposed under `/admin/api/expenses` (suppliers, payers, receipts, expenses, articles, article aliases, price observations, reports). See `../domain/expenses/http-api.md` and `../domain/expenses/index.md` for details.

**New (2025-12-10):** Admin Settings UI expansion with comprehensive configuration management for view options, form fields, and table columns. See `../admin/frontend/admin-settings.md` for the complete guide.

**New (2025-12-25):** Expense Items sub-entity added with full CRUD support for managing individual expense line items. See `../domain/expenses/index.md` for details.

**New (2025-12-26):** Mistral OCR integration for POS receipts with an async worker (`bb receipt-ocr-worker`) plus user receipt upload/inbox/approval endpoints. See `PLAN-mistral-ocr-pos-receipts.md` and `../domain/expenses/index.md`.

**New (2026-01-07):** Receipt review workflow improvements (separate “review” vs “approve”, better receipt preview, and preserving original extraction guesses). See `../domain/expenses/index.md` and `../domain/expenses/http-api.md`.

**New (2026-01-08):** POS integration upgrades: auto-matching expense items to articles via supplier aliases, unmapped items management + batch alias creation, and 3-decimal precision support for line-item quantities. See `../domain/expenses/index.md`.

**New (2026-01-11):** Supplier enhancements (archiving + detail view support) and improved delete error handling for FK violations. See `../domain/expenses/index.md` and `../domain/expenses/http-api.md`.

**New (2026-01-13):** Expenses role/capability gating (frontend + API) and persisted user expense settings (`/api/v1/expenses/settings`). See `../domain/expenses/index.md` and `../domain/expenses/http-api.md`.

## Quick Start

1. **Dev setup** → `./operations/README.md#initial-setup`
2. **Template architecture** → `./architecture/overview.md` (with “Template vs Hosting” notes)
3. **Template guide** → `../template/backend/single-tenant-template.md`
4. **Frontend/app shell** → `../template/frontend/app-shell.md`
5. **Migrations** → `./migrations/migration-overview.md`

Common tasks:
- Start stack: `bb run-app` (serves app + admin UI)
- Admin UI: `http://localhost:8085/admin/users` (admin auth simplified for template)
- Tests: `bb be-test`, `bb fe-test`
- Guard against concrete-domain coupling: `bb guard-no-concrete-domain` (CI also runs this via `npm run test:config-audit`)

## Documentation by Role

**Backend**
- `../template/backend/single-tenant-template.md` — what this template includes and how to extend it
- `../template/backend/http-api.md` — shared HTTP surfaces and shapes
- `../admin/backend/http-api.md` — admin endpoints under `/admin/api`
- `../admin/backend/services.md` — admin service map
- `../template/backend/security-middleware.md` — request pipeline (see notes on single-tenant vs hosting)

**Frontend**
- `../template/frontend/app-shell.md` — app shell, routing, Shadow-CLJS builds
- `../template/frontend/template-component-integration.md` — using template UI components
- `../admin/frontend/admin.md` — admin panel features (users, audit, settings)
- `../admin/frontend/admin-settings.md` — comprehensive admin settings configuration guide
- `../admin/frontend/admin-panel-single-tenant.md` — single-tenant admin flow and extension points
- `../admin/frontend/list-view-controls-configuration.md` — list view controls and configuration
- `../shared/frontend/component-library.md` — component library with **ID requirements for browser testing**
- `../shared/frontend/master-detail-form.md` — reusable wrapper for edit forms with detail fetch (master/detail)

**Operations**
- `./operations/README.md` — commands, env, deployment notes
- `./migrations/migration-overview.md` — models/migrations workflow

**Domains**
- `../domain/expenses/index.md` — complete expenses domain guide with new entities (articles, aliases, price observations)

**Reference / Hosting examples (not present in this repo)**
- Backend domains: `./reference/hosting/hosting-domain.md`, `./reference/hosting/financial-domain.md`, `./reference/hosting/integration-domain.md`
- Frontend reference: `./reference/hosting/frontend-integration-domain.md`

## Template vs Hosting (at a glance)

- **Template scope (this repo)**: single-tenant; template/admin/shared code only; simplified admin auth; DB generated into `resources/db/models.edn` from `resources/db/{template,shared,domain}/**`.
- **Hosting reference**: multi-tenant RLS, tenant context middleware, full property/financial/integration domains. Use these docs as examples when adding your own domains.

## Architecture Snapshot (template)

```
Browser → app.template.frontend.core → routes (template + domains) → services/DI → PostgreSQL (single-tenant)
```

- Frontend: Re-frame + UIx; template routes in `app.template.frontend.routes` and domain user routes contributed via `app.domain.frontend.registry`.
  - Admin module wiring lives in `app.admin.frontend.core` and is initialized lazily when navigating under `/admin`.
- Backend: `app.template.backend.core` with DI container `app.template.di.config`.
- DB: migrations are generated from source files in `resources/db/{template,shared,domain}/**` (merged into `resources/db/models.edn`).

## Development Navigation

| Area | Key Docs | Code Pointers |
|------|----------|---------------|
| Admin (template) | `../admin/frontend/admin.md`, `../admin/frontend/admin-settings.md`, `../admin/frontend/admin-panel-single-tenant.md` | `src/app/admin/frontend` |
| Template frontend | `../template/frontend/app-shell.md`, `../template/frontend/template-component-integration.md` | `src/app/template/frontend` |
| Backend core | `../template/backend/single-tenant-template.md`, `../template/backend/http-api.md` | `src/app/template/backend` |
| Domains | `../domain/expenses/index.md` | `src/app/domain/backend/expenses`, `src/app/domain/frontend/expenses` |
| Migrations/DB | `./migrations/migration-overview.md` | `resources/db/*` |
| Hosting reference | Domain docs listed above | *Hosting repo only* |

## Ops & Tooling (template defaults)

- Babashka tasks: see `./operations/README.md` (`bb run-app`, `bb be-test`, `bb fe-test`, `bb backup-db`, `bb restore-db`).
- Shadow-CLJS: `npm run watch` / `npm run build`.
- Ports: defaults to 8085 for app/admin (adjust per your config).

## Linking & Metadata

- Single-tenant docs use tags like `:single-tenant` and `:template`.
- Hosting-only docs are tagged `:hosting` and `:reference-only` so they can be filtered out.

---

Start with `../template/backend/single-tenant-template.md` to understand what is included here, then follow `./operations/README.md` to run the stack. For domain examples, consult the Hosting reference docs called out above.
