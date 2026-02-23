# Copilot instructions (single-tenant template)

**Precedence**: `AGENTS.md` = policy/workflow & hard rules. This file = implementation patterns inside those constraints.

## MCP tools (default)

- **Clojure/EDN** (`.clj`/`.cljs`/`.cljc`/`.edn`): use `clojure-mcp` structural edits (prefer `mcp__clojure-mcp__clojure_edit`). If a plain-text edit causes reader/compilation errors, stop and redo with `clojure-mcp`.
- **REPL**: use `clj-nrepl-eval` for exploration, debugging, and focused tests.
- **DB**: use `postgres-mcp` for queries + schema (e.g. `mcp__postgres__execute_sql`, `mcp__postgres__list_tables`).
- **Browser**: use `chrome-mcp`; ensure stable `:id` attributes (see `AGENTS.md` for ID patterns).

## Clojure CLI fallbacks (clojure-mcp-light)

Use these only when MCP tools aren’t available (or you want native diff UI):

- Auto paren repair: `clj-paren-repair-claude-hook` (configured via `.claude/settings.json`).
- Manual paren repair: `clj-paren-repair path/to/file.clj` (don’t “hunt parens”).
- REPL eval without MCP: `clj-nrepl-eval --discover-ports` then `clj-nrepl-eval -p <PORT> "(require 'my.ns :reload)"`. `deps.edn` has `:nrepl` (port **7888**).

## Entrypoints

- System: `src/app/template/backend/core.clj` (config, models, webserver, DI).
- DI container: `src/app/template/di/config.clj`.
- Routing composition: `src/app/template/backend/routes.clj`.
- Route boundaries:
  - Admin API: `src/app/template/backend/routes/admin_api.clj` mounted at `/admin/api`.
  - User API: `src/app/template/backend/routes/api.clj` mounted at `/api/v1`.
  - Domain registry: `src/app/domain/backend/registry.clj`.
  - Example domain: Expenses admin routes under `/admin/api/expenses` (`src/app/domain/backend/expenses/routes/core.clj`).

## Local dev

- Run once: `bb run-app` (auto-reloads).
- Tests: `bb be-test` (backend), `bb fe-test-parallel` (frontend) or `npm run test:cljs`.
- Save test output once (don’t re-run to grep): `mkdir -p tmp && bb be-test 2>&1 | tee tmp/be-test.txt`.

## Config & ports

- Runtime: `config/base.edn` (dev web **8085**, DB **55432**; test web **8086**, DB **55433**). Secrets: `config/.secrets.edn` or `~/.secrets.edn`.
- User UI config: edited via `/admin/user-settings`, loaded dynamically (see `src/app/template/backend/routes/api.clj`).
- Dev helpers: rate limit/session helpers under `/admin/api/*` (see `docs/template/backend/security-middleware.md`).

## DB & migrations

- Edit canonical schema inputs under `resources/db/{template,shared,domain}`; never hand-edit `resources/db/migrations/*`.
- REPL helpers: `src/app/template/backend/migrations/simple_repl.clj` (see `docs/general/migrations/migration-overview.md`).

## Conventions / footguns

- Naming boundary: DB is `snake_case`, app/runtime is kebab-case; normalize with `app.shared.model-naming/db-keyword->app` + `ensure-app-keyword`.
- Generic CRUD: `/api/v1/entities/*` is deny-by-default allowlisted; domain entities usually need domain APIs + a CRUD bridge (see `docs/template/backend/generic-entity-crud.md`).
- Frontend entity specs: `src/app/template/frontend/db/entity_specs.cljs` normalizes keys; wrong columns often = snake↔kebab mismatch.
- Re-frame: `re-frame/trim-v` is in `app.template.frontend.db.interceptors/common-interceptors`; handlers should destructure like `[params]` (not `[_ params]`).
- JSON: convert PG-specific objects before encoding (see `app.shared.type-conversion` usage in services).

## Clojure patterns

- REPL-first:
  - Use the connected REPL; don’t spawn new REPLs.
  - Only edit when the REPL is connected; if evaluation errors indicate the REPL is unavailable, reconnect before continuing.
  - Use `clojure-mcp` structural edits for Clojure/EDN; if you hit reader/compilation errors after a plain-text edit, immediately redo with `clojure-mcp`.
  - Reload explicitly: `(require 'my.ns :reload)`.
  - CLJS: select build first: `(shadow.cljs.devtools.api/nrepl-select :app)` or `:admin`.
  - Prefer returning values over printing.
- Validation (required for behavior changes / non-trivial changes):
  - Prove the change via REPL and/or focused tests (at least one).
  - Confirm runtime: correct nREPL port; for CLJS select build first.
  - Minimum edge cases (as applicable): happy path, `nil`, empty collections, invalid/boundary inputs.
  - If using tests, run the smallest focused set and save output once with `tee`.
  - Remove temporary instrumentation (`println`, extra logging, debug `def`s) before finishing; keep useful `(comment ...)` examples.
- Docstrings:
  - Docstring after fn name, before args; use straight `"` and escape interior quotes (`\"`) (avoid smart quotes).
  ```clj
  (defn my-fn
    "Does X with Y."
    [x y]
    ...)
  ```
- Indentation: align multi-line forms for readability + delimiter safety.
  ```clj
  (if (and cond-a
           cond-b)
    x
    y)
  ```
- Inline `def`: allowed for REPL inspection; prefer `tap>` when a global isn’t needed.
  ```clj
  (defn process [xs]
    (def xs xs)
    (group-by :k xs))
  ```
- RCF: keep `(comment ...)` blocks copy‑eval ready.
  ```clj
  (comment
    (process [{:k 1} {:k 2}])
    :rcf)
  ```
- Tests from REPL:
  ```clj
  ;; CLJ
  (require 'app.backend.routes.api-test :reload)
  (clojure.test/run-tests 'app.backend.routes.api-test)

  ;; CLJS
  (shadow.cljs.devtools.api/nrepl-select :app)
  (require 'app.domain.frontend.registry-test :reload)
  (cljs.test/run-tests 'app.domain.frontend.registry-test)
  ```
  - Follow existing `deftest` naming conventions in the namespace; use `testing`; add `is` messages when useful.
  - Optional: Kaocha REPL commands are fine, but the `clojure.test`/`cljs.test` patterns above are preferred.
- Exploration: `clojure.repl.deps/add-libs` is exploration-only; never commit. Avoid stdin reads in BB/nREPL; pass args.
- Reload safety: keep top-level code idempotent and side-effect-light so `:reload` is safe.

## Security middleware toggles

- `DISABLE_HTTPS_REDIRECT=true` disables HTTPS redirect; `DISABLE_RATE_LIMITING=true` disables rate limiting (see `src/app/template/backend/middleware/security.clj`).
