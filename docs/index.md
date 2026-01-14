<!-- ai: {:tags [:overview :architecture :single-tenant] :kind :overview} -->

# Single-Tenant SaaS Template — Documentation

## Overview

This repository is the **single-tenant template** extracted from the Hosting multi-tenant app. It keeps the shared/template/admin infrastructure (Clojure/ClojureScript, PostgreSQL, Shadow-CLJS, Babashka tooling) but ships **without Hosting/Financial/Integration business domains or tenant-aware RLS**. Hosting-specific docs remain as reference when you want examples of full domains.

**New (2025-12-08):** Home Expenses Tracker domain is included and exposed under `/admin/api/expenses` (suppliers, payers, receipts, expenses, articles, article aliases, price observations, reports). See `docs/backend/http-api.md` and `docs/expenses/index.md` for details.

**New (2025-12-10):** Admin Settings UI expansion with comprehensive configuration management for view options, form fields, and table columns. See `docs/frontend/admin-settings.md` for the complete guide.

**New (2025-12-25):** Expense Items sub-entity added with full CRUD support for managing individual expense line items. See `docs/expenses/index.md` for details.

**New (2025-12-26):** Mistral OCR integration for POS receipts with an async worker (`bb receipt-ocr-worker`) plus user receipt upload/inbox/approval endpoints. See `PLAN-mistral-ocr-pos-receipts.md` and `docs/expenses/index.md`.

**New (2026-01-07):** Receipt review workflow improvements (separate “review” vs “approve”, better receipt preview, and preserving original extraction guesses). See `docs/expenses/index.md` and `docs/backend/http-api.md`.

**New (2026-01-08):** POS integration upgrades: auto-matching expense items to articles via supplier aliases, unmapped items management + batch alias creation, and 3-decimal precision support for line-item quantities. See `docs/expenses/index.md`.

**New (2026-01-11):** Supplier enhancements (archiving + detail view support) and improved delete error handling for FK violations. See `docs/expenses/index.md` and `docs/backend/http-api.md`.

**New (2026-01-13):** Expenses role/capability gating (frontend + API) and persisted user expense settings (`/api/v1/expenses/settings`). See `docs/expenses/index.md` and `docs/backend/http-api.md`.

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
- Guard against concrete-domain coupling: `bb guard-no-concrete-domain` (CI also runs this via `npm run test:config-audit`)

## Documentation by Role

**Backend**
- `docs/backend/single-tenant-template.md` — what this template includes and how to extend it
- `docs/backend/http-api.md` — HTTP surfaces and admin routes
- `docs/backend/services.md` — service protocols and composition
- `docs/backend/security-middleware.md` — request pipeline (see notes on single-tenant vs hosting)

**Frontend**
- `docs/frontend/app-shell.md` — app shell, routing, Shadow-CLJS builds
- `docs/frontend/template-component-integration.md` — using template UI components
- `docs/frontend/admin.md` — admin panel features (users, audit, settings)
- `docs/frontend/admin-settings.md` — comprehensive admin settings configuration guide
- `docs/frontend/admin-panel-single-tenant.md` — single-tenant admin flow and extension points
- `docs/frontend/list-view-controls-configuration.md` — list view controls and configuration
- `docs/frontend/component-library.md` — component library with **ID requirements for browser testing**
- `docs/frontend/master-detail-form.md` — reusable wrapper for edit forms with detail fetch (master/detail)

**Operations**
- `docs/operations/README.md` — commands, env, deployment notes
- `docs/migrations/migration-overview.md` — models/migrations workflow

**Domains**
- `docs/expenses/index.md` — complete expenses domain guide with new entities (articles, aliases, price observations)

**Reference / Hosting examples (not present in this repo)**
- Backend domains: `docs/backend/hosting-domain.md`, `docs/backend/financial-domain.md`, `docs/backend/integration-domain.md`
- Frontend feature guides: `docs/frontend/feature-guides/hosting.md`, `billing.md`, `integrations.md`

## Template vs Hosting (at a glance)

- **Template scope (this repo)**: single-tenant; template/admin/shared code only; simplified admin auth; DB generated into `resources/db/models.edn` from `resources/db/{template,shared,domain}/**`.
- **Hosting reference**: multi-tenant RLS, tenant context middleware, full property/financial/integration domains. Use these docs as examples when adding your own domains.

## Architecture Snapshot (template)

```
Browser → (app.template.frontend.core | app.admin.frontend.core) → routes (template + domains) → services/DI → PostgreSQL (single-tenant)
```

- Frontend: Re-frame + UIx; template routes in `app.template.frontend.routes` and domain user routes contributed via `app.domain.frontend.registry`.
- Backend: `app.template.backend.core` with DI container `app.template.di.config`.
- DB: migrations are generated from source files in `resources/db/{template,shared,domain}/**` (merged into `resources/db/models.edn`).

## Development Navigation

| Area | Key Docs | Code Pointers |
|------|----------|---------------|
| Admin (template) | `docs/frontend/admin.md`, `docs/frontend/admin-settings.md`, `docs/frontend/admin-panel-single-tenant.md` | `src/app/admin/frontend` |
| Template frontend | `docs/frontend/app-shell.md`, `docs/frontend/template-component-integration.md` | `src/app/template/frontend` |
| Backend core | `docs/backend/single-tenant-template.md`, `docs/backend/http-api.md` | `src/app/template/backend` |
| Domains | `docs/expenses/index.md` | `src/app/domain/backend/expenses`, `src/app/domain/frontend/expenses` |
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
