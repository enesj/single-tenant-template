# Repository Guidelines

## Overview & Architecture
- Multi-tenant property hosting SaaS built with Clojure/ClojureScript and PostgreSQL (RLS for tenant isolation).
- Core domains: Financial, Hosting, Integration, Admin, Template (shared SaaS infra). Keep domain boundaries clean and avoid cross-contamination.

## Project Structure (Quick Map)
```
src/app/        # admin, backend, frontend, migrations, shared, template
test/           # *_test.clj / *_test.cljs mirroring src
resources/      # public assets, db models/migrations
config/         # base + secrets (local only)
cli-tools/      # dev utilities, test scripts
scripts/        # build/dev/testing helpers (sh, bb)
vendor/         # vendored libs (automigrate, ring, etc.)
```

## Project Structure Quick Map (Detailed)
```
src/app/
├── admin/
│   └── frontend/                # Admin panel UI
│       ├── adapters/
│       ├── components/
│       ├── config/
│       ├── events/ (users/)
│       ├── handlers/
│       ├── pages/
│       ├── renderers/
│       ├── security/
│       ├── services/
│       ├── shared/ (examples/, hooks/)
│       ├── specs/
│       ├── subs/
│       ├── system/
│       └── utils/
├── backend/                     # Core backend services
│   ├── middleware/
│   ├── routes/ (admin/)
│   ├── security/
│   └── services/ (admin/)
├── frontend/                    # Core frontend UI and utils
│   ├── dev/
│   ├── preload/
│   ├── ui/
│   └── utils/
├── migrations/                  # Database migration helpers
├── shared/                      # Cross-platform utilities
│   ├── adapters/
│   ├── crud/
│   ├── events/
│   ├── frontend/ (bridges/, components/, utils/)
│   ├── http/
│   ├── response/
│   ├── schemas/ (domain/, template/)
│   └── validation/
└── template/                    # Multi-tenant SaaS infrastructure
    ├── backend/ (auth/, crud/, db/, email/, invitation/, metadata/, middleware/, routes/, subscription/, tenant/, user/, validation/)
    ├── di/
    ├── frontend/ (api/, components/, db/, events/, pages/, state/, subs/, utils/)
    └── shared/ (schemas/, utils/, validation/)

Key configs: deps.edn, shadow-cljs.edn, resources/db/models.edn
```

## Development & Commands
- App start: App is ALWAYS RUNNING during development; no need to restart manually because the system automatically restarts after FE/BE changes.
- Admin UI is served by default at `http://localhost:8085` (not 3000); use that port in local testing and curl checks.
## Debugging
- See `.claude/skills` for debugging skills/docs specific to this app.
- For debugging/testing always use clojure-mcp eval tools; for browser interactions, use chrome-mcp tools.

## Coding Style & Patterns
- Naming: DB tables/columns use `snake_case`; code uses kebab-case (`:user-settings`). Namespaces dotted (e.g., `app.domain.frontend.events`).

- Architecture: protocols-first services; propagate tenant context for every operation; enforce security middleware; keep domains isolated.
- Reuse-first: for functionality, use shared logic/utilities (`src/app/shared/**`, `src/app/template/shared/**`) before adding new code to FE or BE.
- Frontend composition: for UI, start with template components (`src/app/template/frontend/**`); reuse other components defined in current of folders containing current folder; , and create new components only if nothing existing fits.
- UI: use DaisyUI component classes prefixed with `ds-` (e.g., `ds-btn`, `ds-card`) when creating new components or modifying shared components. Tailwind utilities remain unprefixed (`flex`, `text-sm`).

## Migrations Workflow
- Edit canonical EDN under `resources/db/{template,shared,domain}`. Avoid manual edits to `resources/db/migrations/*`.
- REPL-first flow via `src/app/migrations/simple_repl.clj`: `(require '[app.migrations.simple-repl :as mig])`, then `(mig/make-all-migrations!)`, `(mig/migrate!)`, `(mig/status)`; pass a profile like `(mig/migrate! :test)` when needed. See `docs/migrations/complete-guide.md` for full details.
- DB: dev on `:55432`, test on `:55433`. Use `bb backup-db` / `bb restore-db` for safety.

### Policies
- Canonical files: `resources/db/{template,shared,domain}/*/policies.edn` only. The generator ignores extra files like `missing_policies.edn` or `rls_enablement.edn`.
- RLS enablement: add a dedicated entry (e.g., `enable_rls_on_tenant_tables`) with raw SQL `:up`/`:down` blocks inside a canonical `policies.edn`. The extended generator emits a `.pol` migration to run `ALTER TABLE ... ENABLE/DISABLE ROW LEVEL SECURITY`.
- Naming: use `tenant_isolation_<table>` for tenant filters and `admin_bypass_<table>` for admin access. Avoid alternate synonyms when equivalent policies exist.
- Don’t hand-edit `resources/db/migrations/*`. If you need to re-generate policies, update the EDN and run the generator.

### REPL Helpers (use the clojure-mcp eval tool to run)
```clojure
(require '[app.migrations.simple-repl :as mig])
(mig/make-all-migrations!)     ;; merge models → schema → extended
(mig/migrate!) (mig/status)    ;; apply and inspect
(mig/regenerate-extended-migrations-clean!)    ;; prune to base and re-gen extended
(mig/check-duplicate-migrations)               ;; report duplicate numbers
```

## Common Issues & Fixes
- PostgreSQL JSON serialization: convert PG-specific objects (PGobject, arrays, timestamps) before returning API responses.
  - Pattern: apply a DB serialization helper (e.g., `convert-pg-objects`) to query results before `response/ok`.
- Namespaced keys: JOINs often return `:table/col`; normalize to simple keys where callers expect them (e.g., `:id` via `(or (:id x) (:admins/id x))`).
- Re-frame orchestration: ensure `app.domain.frontend.events.core` is loaded so event namespaces register.
- Entity store sync: after updates, refresh both the feature store and UI read locations to avoid empty tables until refresh.
- HoneySQL clause keywords: verify correct keyword shapes (`:id` vs `:users/id`) to prevent silent query issues.

## Frontend UI Conventions
- Effective `:entity-spec`: When a page’s table uses customized or computed fields, pass the effective spec to `list-view` via `:entity-spec`. The table forwards it to the column settings so toggles operate on the exact rendered fields (e.g., includes `:tenant_name` instead of raw `:tenant_id`).
- Fallback behavior: If `:entity-spec` is omitted, settings fall back to the template spec; computed/admin-only fields might be missing from toggles.
- Example:
  ```clojure
  ($ list-view
     {:entity-name :users
      :entity-spec users-entity-spec
      :title "Users"})
  ```
- Recommendation: Admin pages should pass the spec produced by the admin spec generator to ensure toggles match admin-visible columns.

## Security & Configuration
- Secrets: never commit; keep in `config/.secrets.edn` and environment vars for CI/CD.
- Security checks (manual):
  - `curl -I https://localhost:8085/admin` (headers)
  - `curl -k http://localhost:8085/admin` (HTTPS redirect)
  - `curl -H "X-Forwarded-For: 192.168.1.100" http://localhost:8085/api/test` (rate limiting)
- Optional services: see `docker-compose.yml`; document port/env changes.

## Testing Guidelines
- Frameworks: Kaocha (Clojure), Shadow CLJS + cljs-test-runner (ClojureScript).
- Location/naming: under `test/` mirroring `src/`; suffix with `_test`.
- Commands: `bb be-test`, `bb fe-test`, watch with `npm run test:cljs:watch` while iterating.

## Commit & Pull Request Guidelines
- Commits: small, atomic; imperative subject with scope (e.g., `frontend: fix date filter`).
- Before PR: `bb cljfmt-check`, `bb cljfmt-fix` (if needed), `bb lint`, `bb be-test`, `bb fe-test`.
- PRs: clear description, linked issues, UI screenshots (if relevant), and notes for any DB/migration impact.

## Documentation Map
- Primary entry point for doc discovery: `docs/ai-quick-access.md` (metadata expectations, lookup recipes, and RAG CLI usage).
- Architecture: `docs/architecture/*` (routing: `docs/architecture/routing.md`)
- Backend/Frontend: `docs/backend/*`, `docs/frontend/*` (template UI: `docs/frontend/template-component-integration.md`)
- Auth/Security: `docs/authentication/*`, `docs/backend/security-middleware.md`, `docs/authentication/architecture-overview.md`
- Migrations: `docs/migrations/*` (overview: `docs/migrations/migration-overview.md`, quick start: `docs/migrations/quick-start.md`)
- Debugging: `docs/debugging/console-monitoring-setup.md`, `docs/debugging/debug-with-eval.md`, `docs/debugging/debug-with-eval-optimized.md`
- Shared utilities & libs: `docs/shared/*`, `docs/libs/*`

## Escalate to Codex MCP for complex tasks

GLM-4.6 is the primary model. Use the Codex MCP tool `mcp__codex__codex` as an escalation path whenever the task feels complex (deep refactor/architecture, tricky reasoning, ambiguous requirements, or you’re stuck/looping after 2 attempts).

### Guardrail: don’t let Codex read docs/
Do not ask Codex to read anything under `docs/`. If docs matter, GLM-4.6 reads them and includes a short summary + a few short quotes (with file/section names) in the prompt to Codex.

### Before calling Codex, prepare a “context packet”
Include: goal + constraints, relevant file paths/snippets, exact errors/log excerpts (trimmed), commands/repro steps (if any), environment details, and what you already tried/decided.

### Call settings
Prefer `approval-policy: on-request` and `sandbox: workspace-write` (or `read-only` if sufficient). Ask Codex for the minimal plan + concrete next steps (and verification if applicable).

# Clojure REPL Evaluation

The command `clj-nrepl-eval` is installed on your path for evaluating Clojure code via nREPL.

**Discover nREPL servers:**

`clj-nrepl-eval --discover-ports`

**Evaluate code:**

`clj-nrepl-eval -p <port> "<clojure-code>"`

With timeout (milliseconds)

`clj-nrepl-eval -p <port> --timeout 5000 "<clojure-code>"`

The REPL session persists between evaluations - namespaces and state are maintained.
Always use `:reload` when requiring namespaces to pick up changes.

# Clojure Parenthesis Repair

The command `clj-paren-repair` is installed on your path.

Examples:
`clj-paren-repair <files>`
`clj-paren-repair path/to/file1.clj path/to/file2.clj path/to/file3.clj`

**IMPORTANT:** Do NOT try to manually repair parenthesis errors.
If you encounter unbalanced delimiters, run `clj-paren-repair` on the file
instead of attempting to fix them yourself. If the tool doesn't work,
report to the user that they need to fix the delimiter error manually.

The tool automatically formats files with cljfmt when it processes them.
