## Repo Instructions for Copilot Chat

Read `AGENTS.md` first for workflow, tool/skill selection, testing discipline, and component IDs.
This file focuses on implementation guidance (coding patterns, migrations, common issues, security checks).

### Instruction Precedence

- If two instructions conflict, follow the more specific one for the code/path you are working on.
- If equally specific, prefer `AGENTS.md` for policy/workflow and this file for implementation details.

# Coding & Development Instructions

## Coding Style & Patterns

- Naming: DB tables/columns use `snake_case`; code uses kebab-case (`:user-settings`). Namespaces dotted (e.g., `app.template.frontend.events`).
- Architecture: protocols-first services; enforce security middleware; keep concerns isolated.
- Reuse-first: prefer shared logic/utilities (`src/app/shared/**`, `src/app/template/shared/**`) before adding new code to FE or BE.
- Frontend composition: for UI, start with template components (`src/app/template/frontend/**`); reuse components from current or parent folders first.
- UI: use DaisyUI component classes prefixed with `ds-` (e.g., `ds-btn`, `ds-card`) when creating or modifying shared components. Tailwind utilities remain unprefixed.

## Migrations Workflow

- Read docs first: `docs/general/migrations/**` (start with `complete-guide.md` and `migration-overview.md`).
- Edit canonical EDN under `resources/db/{template,shared}`.
- Run REPL helpers via `src/app/migrations/simple_repl.clj`.
- Never hand-edit `resources/db/migrations/*`.
- Databases: dev on `:55432`, test on `:55433`; use `bb backup-db` / `bb restore-db` before migrations.

## Common Issues & Fixes

- PostgreSQL JSON serialization: convert PG-specific objects (PGobject, arrays, timestamps) before returning API responses.
  - Pattern: apply a DB serialization helper (e.g., `convert-pg-objects`) to query results before `response/ok`.
- Namespaced keys: JOINs often return `:table/col`; normalize to simple keys where callers expect them (e.g., `:id` via `(or (:id x) (:admins/id x))`).
- Re-frame orchestration: ensure `app.template.frontend.events.core` is loaded so event namespaces register.
- Entity store sync: after updates, refresh both the feature store and UI read locations to avoid empty tables until refresh.
- HoneySQL clause keywords: verify correct keyword shapes (`:id` vs `:users/id`) to prevent silent query issues.

### Recurring frontend issue: list pages show only timestamp columns

- Root cause: entity spec lookup returned `nil` due to entity key mismatch (app/kebab-case vs db/snake_case).
- Fix: normalize spec map keys to app/kebab-case (use `model-naming/db-keyword->app` in spec generation/lookups).

### Common follow-up: API has data but table rows are empty

- Root cause: re-frame handlers using `common-interceptors` include `trim-v`; handler event vectors are already trimmed.
- Fix: use handler args like `[params]`, `[response]`, `[error]` (not `[_ params]`, etc.).

## Frontend UI Conventions

- Pass the effective `:entity-spec` to `list-view` when tables use computed/custom fields so column toggles align.
- Admin pages should pass the spec produced by the admin spec generator.

## Security & Configuration

- Secrets: never commit; keep in `config/.secrets.edn` and environment vars for CI/CD.
- Security checks (manual):
  - `curl -I https://localhost:8085/admin` (headers)
  - `curl -k http://localhost:8085/admin` (HTTPS redirect)
  - `curl -H "X-Forwarded-For: 192.168.1.100" http://localhost:8085/api/test` (rate limiting)
