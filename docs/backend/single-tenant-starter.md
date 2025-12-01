# Single-Tenant Starter Template (Hosting New Project Generator)

> **Note:** This document describes the generator’s starter project. For the template you are currently in, see `docs/backend/single-tenant-template.md`. If you’re not running the generator, treat this as reference.

The **single-tenant starter** is a pre-baked project template that ships with:

- A reloadable **backend** (Clojure + http-kit + Reitit) wired via `app.backend.core`.
- A reloadable **frontend** (ClojureScript + shadow-cljs + re-frame + Uix) with a simple landing page.
- Shared/template UI components (buttons, cards, etc.) and shared libraries (validation, schemas, HTTP utilities).
- AI/dev tooling folders: `.claude`, `.clojure-mcp`, `.codex`, `.clj-kondo`.

Unlike the main Hosting app, the starter ships **without any business domain** (no financial/hosting/integration domain code or DB schema). It is designed to be a clean base where you add your own domain tables, services, and pages.

## Generating a New Project

From the generator project:

```bash
cd project-templates/new-project-generator
clj -M:run --name my-starter --modules single-tenant-base --port 8088
```

This will create a new folder (alongside the Hosting repo), for example:

```text
/Users/enes/Projects/my-starter
```

Key options:

- `--name` – target project folder name.
- `--modules single-tenant-base` – use the pre-baked single-tenant starter.
- `--port` – dev web server port; updates `config/base.edn` and dev scripts.

The generated project includes `GENERATED_TEMPLATE.md` and a `TEMPLATE_PROMPTS/` folder summarizing the starter and next steps.

## Project Layout (Generated Starter)

At a high level the generated project looks like:

- `bb.edn`, `deps.edn`, `build.clj`, `shadow-cljs.edn`, `tailwind.config.js`, `postcss.config.mjs`, `package.json`, `.pre-commit-config.yaml`, `karma.conf.cjs`
- `config/` – minimal `base.edn` with `:database` and `:webserver` settings.
- `dev/` – REPL and watcher entrypoints (`dev/core.clj`, `dev/system/core.clj`).
- `resources/public/` – `index.html`, CSS, and JS output directory (`/js/main`).
- `resources/db/models.edn` – includes a simple `:users` table so the starter has a working DB-backed list view.
- `scripts/` – shell helpers (backend/frontend/dev scripts, console monitoring).
- `src/app/backend/`
  - `core.clj` – loads config, creates DB pool, starts HTTP server.
  - `routes.clj` – minimal routes: `GET /` (SPA shell), `GET /api/health` (JSON), `GET /api/users` (users JSON).
  - `backend/services/users.clj` – read-only service for listing users from the `users` table.
  - `webserver.clj` – http-kit server wiring.
- `src/app/migrations/`
  - `simple_repl.clj` – small REPL helper for generating and running automigrate schema migrations from `resources/db/models.edn`.
- `src/app/frontend/`
  - `core.cljs` – SPA entrypoint: landing page + a simple users table rendered from `/api/users`.
- `src/app/shared/` – shared utilities (validation, pagination, HTTP, schemas).
- `src/app/template/` – UI component library + shared SaaS helpers, adapted to avoid domain-specific dependencies for the starter.
- `src/system/state.clj` – shared dev-system state.
- `vendor/` – vendored libraries (automigrate, ring, reitit, etc.).
- `.claude`, `.clojure-mcp`, `.codex`, `.clj-kondo` – AI/dev tooling.

## Running the Starter in Dev

From inside the generated project:

```bash
bb run-app         # Start backend + shadow-cljs + watchers
```

Then open:

- `http://localhost:<port>/` – main SPA landing page (hero + features + users table).
- `http://localhost:<port>/api/health` – JSON health endpoint (`{"status":"ok"}`).
- `http://localhost:<port>/api/users` – JSON payload with users from the `users` table (`{"users":[...]}`).

Notes:

- On first use, run `npm install` in the generated project so `shadow-cljs` can build the frontend bundle.
- The dev alias (`:dev` in `deps.edn`) starts the same stack as in the main Hosting app (nREPL, shadow-cljs, watchers), but against the **starter** code.

## Adding Your Own Domain

The starter intentionally ships with **no business domain**. To add one:

1. **Define tables** in `resources/db/models.edn`:
   - The starter ships with a simple `:users` table used by the home page list view.
   - Extend this file with additional entities (e.g. `:accounts`, `:projects`).
   - Reuse patterns from `resources/db/models.edn` in the Hosting repo as a reference.
2. **Generate migrations**:
   - Use the `:migrations-dev` alias in `deps.edn` with the starter’s `models.edn`.
   - Alternatively, from a REPL, use `app.migrations.simple-repl`:
     - `(require '[app.migrations.simple-repl :as mig])`
     - `(mig/make-schema-migration!)`
     - `(mig/migrate!)`
3. **Add backend domain code**:
   - Create `src/app/domain/<your-domain>/backend/*` for services, routes, and DB helpers.
   - Integrate with the existing `app.backend.routes` or add new route namespaces.
4. **Add frontend domain code**:
   - Create pages under `src/app/<your-namespace>/frontend/pages/*`.
   - Reuse `app.template.frontend.components.*` for tables, forms, filters, and layout.
5. **Wire routes & UI**:
   - Extend `app.frontend.core` to add navigation to your new pages.
   - Optionally introduce a client-side router using `reitit.frontend` if needed.

Because the starter already includes shared/template modules, you can follow the same patterns as the main Hosting app without bringing over multi-tenant domains or RLS policies.

## Relationship to the Main Hosting App

- The starter reuses the **tooling and infrastructure** patterns from the Hosting repo:
  - Dev system + file watchers.
  - Shared UI components.
  - JSON encoding, DB pool helpers, vendored automigrate.
- It deliberately **does not** include:
  - Hosting/financial/integration domain code (`src/app/domain/*`).
  - Domain-specific DB models (`resources/db/domain/*`).
  - Multi-tenant RLS policies from `resources/db/template` and `resources/db/shared`.

Use this template when you want a clean, single-tenant app with Hosting’s tooling and UI stack, without bringing along the full multi-tenant property hosting domain.
