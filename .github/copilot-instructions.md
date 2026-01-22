# Copilot instructions (single-tenant template)

**Instruction Precedence**: Read `AGENTS.md` first for **policy & workflow** (hard rules like "no Python", testing discipline, security/secrets, component ID patterns). This file covers **implementation patterns** within those constraints. If instructions conflict, follow the more specific one for the code/path you're working on; otherwise prefer `AGENTS.md` for policy and this file for patterns.

## Big picture / entrypoints
- **Backend system entry**: `src/app/template/backend/core.clj` (loads `config/base.edn`, `resources/db/models.edn`, starts webserver + DI container).
- **DI container**: `src/app/template/di/config.clj` (register/get services like `:crud-service`, `:auth-service`).
- **Top-level routing composition**: `src/app/template/backend/routes.clj` (mounts `/api/v1`, `/admin/api`, and SPA fallbacks).
- **HTTP routing boundaries**:
  - Admin API: `src/app/template/backend/routes/admin_api.clj` mounted at `/admin/api` (template JSON middleware + `wrap-admin-authentication`).
  - User API: `src/app/template/backend/routes/api.clj` mounted at `/api/v1` (includes `/config`, `/metrics`, auth routes).
  - Domain route registry: `src/app/domain/backend/registry.clj` defines enabled domains and provides `all-admin-api-routes`, `all-user-api-routes`, and SPA routes.
  - Example domain: Expenses admin routes are mounted under `/admin/api/expenses` (`src/app/domain/backend/expenses/routes/core.clj`).

## Local dev workflows (use these)
- Start full stack (hot reload): `bb run-app` (admin UI at `http://localhost:8085/admin`).
- Backend tests: `bb be-test` (Kaocha, uses `:test` profile).
- Frontend tests: `bb fe-test-parallel` (fast) or `npm run test:cljs`.
- **Save test output once** (don’t re-run to grep): `bb be-test 2>&1 | tee /tmp/be-test.txt`.
- **No Python** in this repo: use Babashka tasks (`bb ...`) or shell scripts under `scripts/sh/**`.

## Config & ports
- Runtime config: `config/base.edn` (dev web **8085**, DB **55432**; test web **8086**, DB **55433**). Keep secrets in `config/.secrets.edn` or `~/.secrets.edn`.
- Domain/user UI config EDNs are edited at runtime via `/admin/user-settings` and loaded dynamically (see `src/app/template/backend/routes/api.clj`).
- Dev helpers: rate limit/session helpers exist under `/admin/api/*` (see `docs/template/backend/security-middleware.md`).

## Database & migrations
- Edit canonical schema inputs under `resources/db/{template,shared,domain}`; **never** hand-edit `resources/db/migrations/*`.
- Preferred REPL helpers live in `src/app/template/backend/migrations/simple_repl.clj` (see `docs/general/migrations/migration-overview.md`).

## Project-specific conventions (common footguns)
- Naming boundary: DB is `snake_case`, app/runtime is kebab-case; normalize with `app.shared.model-naming/db-keyword->app` + `ensure-app-keyword`.
- Generic CRUD: `/api/v1/entities/*` is **deny-by-default allowlisted**; domain entities usually need domain APIs + a CRUD bridge (see `docs/template/backend/generic-entity-crud.md`).
- Frontend entity specs: `src/app/template/frontend/db/entity_specs.cljs` normalizes entity keys; if list pages show wrong columns, suspect snake↔kebab mismatch.
- Re-frame interceptors include `re-frame/trim-v` via `app.template.frontend.db.interceptors/common-interceptors`; handlers should destructure like `[params]` (not `[_ params]`).
- Backend JSON responses: convert PG-specific objects before encoding (see `app.shared.type-conversion` usage in services).

## Security middleware toggles
- HTTPS redirect can be disabled with `DISABLE_HTTPS_REDIRECT=true`; rate limiting with `DISABLE_RATE_LIMITING=true` (see `src/app/template/backend/middleware/security.clj`).
