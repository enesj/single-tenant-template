# Coding & Development Instructions

## Coding Style & Patterns
- Naming: DB tables/columns use `snake_case`; code uses kebab-case (`:user-settings`). Namespaces dotted (e.g., `app.template.frontend.events`).

- Architecture: protocols-first services; enforce security middleware; keep concerns isolated.
- Reuse-first: for functionality, use shared logic/utilities (`src/app/shared/**`, `src/app/template/shared/**`) before adding new code to FE or BE.
- Frontend composition: for UI, start with template components (`src/app/template/frontend/**`); reuse other components defined in current of folders containing current folder; and create new components only if nothing existing fits.
- UI: use DaisyUI component classes prefixed with `ds-` (e.g., `ds-btn`, `ds-card`) when creating new components or modifying shared components. Tailwind utilities remain unprefixed (`flex`, `text-sm`).

## Migrations Workflow

⚠️ **IMPORTANT: Read the migrations documentation first.** Search for "migrations" in MCP Vector Search to find the complete guide before making any changes. Key docs: `docs/migrations/complete-guide.md`, `docs/migrations/migration-overview.md`.

**Quick workflow**: Edit canonical EDN under `resources/db/{template,shared}` → run REPL helpers via `src/app/migrations/simple_repl.clj` → never hand-edit `resources/db/migrations/*`.

**Databases**: Dev on `:55432`, test on `:55433`. Use `bb backup-db` / `bb restore-db` before migrations for safety.

## Common Issues & Fixes
- PostgreSQL JSON serialization: convert PG-specific objects (PGobject, arrays, timestamps) before returning API responses.
  - Pattern: apply a DB serialization helper (e.g., `convert-pg-objects`) to query results before `response/ok`.
- Namespaced keys: JOINs often return `:table/col`; normalize to simple keys where callers expect them (e.g., `:id` via `(or (:id x) (:admins/id x))`).
- Re-frame orchestration: ensure `app.template.frontend.events.core` is loaded so event namespaces register.
- Entity store sync: after updates, refresh both the feature store and UI read locations to avoid empty tables until refresh.
- HoneySQL clause keywords: verify correct keyword shapes (`:id` vs `:users/id`) to prevent silent query issues.

## Frontend UI Conventions
- Effective `:entity-spec`: When a page's table uses customized or computed fields, pass the effective spec to `list-view` via `:entity-spec`. The table forwards it to the column settings so toggles operate on the exact rendered fields.
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
