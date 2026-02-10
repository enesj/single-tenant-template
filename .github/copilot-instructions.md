# Copilot instructions (single-tenant template)

**Instruction Precedence**: Read `AGENTS.md` first for **policy & workflow** (hard rules like "no Python", testing discipline, security/secrets, component ID patterns). This file covers **implementation patterns** within those constraints.

## MCP tools (use these first)

- **Clojure/EDN edits**: For `.clj`/`.cljs`/`.cljc`/`.edn`, use `clojure-mcp` structural editing tools (prefer `mcp__clojure-mcp__clojure_edit`). If a plain-text edit produces reader/compilation errors (unbalanced parens, invalid EDN, etc.), stop and redo/fix using `clojure-mcp` instead of continuing with ad-hoc text diffs.
- **REPL evaluation is the main debugger/test runner**: Use `clj-nrepl-eval` (shell) for exploration, debugging, and running focused tests (examples below).
- **Database operations**: Use `postgres-mcp` tools for queries and schema inspection (e.g. `mcp__postgres__execute_sql`, `mcp__postgres__list_tables`).
  - For agent-driven DB reads/writes, use `postgres-mcp` only. Do not run direct `psql` commands (including `bb -e` + `clojure.java.shell`) for DB inspection/querying.
  - If a query returns `(empty)`, verify before assuming failure: table may be empty, or MCP may be attached to the wrong/disconnected DB session.
  - Run: `mcp__postgres__list_tables`, then `SELECT current_database(), current_user;`, then `SELECT COUNT(*) FROM <table>;`.
  - In VS Code, reconnect/restart the PostgreSQL MCP session and re-run the same query when results look inconsistent.
- **Browser interactions**: Use `chrome-mcp` tools for interactive UI testing; ensure stable `:id` attributes (see `AGENTS.md` for ID patterns).

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
- User starts the app using `bb run-app`. App is automatically reloaded on file changes so no need to ever start it again.
- Backend tests: `bb be-test` (Kaocha, uses `:test` profile).
- Frontend tests: `bb fe-test-parallel` (fast) or `npm run test:cljs`.
- **Temporary files**: Use project-local `tmp/` (not system `/tmp`) for all transient artifacts; remove them when no longer needed.
- **Save test output once** (don’t re-run to grep; see `AGENTS.md` “Testing discipline”): `mkdir -p tmp && bb be-test 2>&1 | tee tmp/be-test.txt`.

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

## Clojure development patterns
- REPL-first workflow
  - Evaluate code in the connected REPL; do not spawn new REPLs.
  - Prefer `clj-nrepl-eval` for REPL interaction:
    - Discover: `clj-nrepl-eval --discover-ports`
    - Eval: `clj-nrepl-eval -p <PORT> "(require 'my.ns :reload)"`
    - For CLJS via shadow nREPL, select build first: `(shadow.cljs.devtools.api/nrepl-select :app)` or `:admin`.
  - Use `clojure-mcp` structural edits for Clojure/EDN changes (don’t free-type large diffs). If you hit reader/compilation errors after a plain-text edit, immediately switch/redo using `clojure-mcp` to keep forms balanced.
  - After edits, reload explicitly: `(require 'my.ns :reload)`.
  - For CLJS, select the build first: `(shadow.cljs.devtools.api/nrepl-select :app)` or `:admin`.
  - Prefer returning values over printing.
  - REPL validation checklist (required for behavior changes / non-trivial changes; encouraged always)
    - Verify via REPL validation and/or focused tests; **at least one is required** (REPL is preferred for iteration).
    - Confirm you’re connected to the right runtime:
      - Clojure: correct nREPL port (prefer `clj-nrepl-eval --discover-ports`).
      - ClojureScript: select the build first: `(shadow.cljs.devtools.api/nrepl-select :app)` or `:admin`.
    - Reload the namespaces you changed (`(require 'my.ns :reload)`) and re-evaluate the smallest thing that proves the change.
    - Minimum edge cases to validate (as applicable): happy path, `nil`, empty collections, invalid/boundary inputs.
    - If you use tests, run the smallest focused set and **save the output once** with `tee` (don’t re-run just to grep), e.g. `mkdir -p tmp && bb be-test 2>&1 | tee tmp/be-test.txt`.
    - Remove any temporary instrumentation (`println`, extra logging, debug `def`s) before finishing; keep RCF `(comment ...)` examples when they’re useful.
- Docstrings and function templates
  - Put docstrings immediately after the function name and before the arg vector.
  ```clj
  (defn my-fn
    "Does X with Y."
    [x y]
    ...)
  ```
  - Docstring quoting rules (important)
    - Use straight double quotes for docstrings and escape any interior double quotes with `\"`.
    - Do not use smart quotes (“ ”) or unescaped `"` inside docstrings; they will break the reader.
    - Prefer backticks for code identifiers inside docstrings when helpful (e.g., `identity`), but still escape literal double quotes.
    - Example (correct):
    ```clj
    (defn normalize-arglist
      "Normalize a Postgres function \"argument\" list (e.g. identity args).\n\nThis is intentionally conservative. It helps match the common case where the EDN function definition lists only argument types."
      [s]
      ...)
    ```
- Indentation and alignment
  - Align multi-line elements (vectors/maps/lists) vertically; rely on correct indentation for bracket balancing.
  ```clj
  (if (and cond-a
           cond-b)
    x
    y)
  (when ok?
    (do-something))
  ```
- Inline def for debugging
  - Inline `def` may be used inside fns to keep intermediate state inspectable during REPL work when helpful. Prefer `tap>` for lighter inspection when a global isn’t needed.
  ```clj
  (defn process [xs]
    (def xs xs)
    (let [g (group-by :k xs)] g))
  (tap> {:debug/value xs})
  ```
- Rich Comment Forms (RCF)
  - Use `(comment ...)` blocks to document validated usage and edge cases; keep examples copy‑eval ready.
  ```clj
  (comment
    (process [{:k 1} {:k 2}])
    :rcf)
  ```
- Testing from the REPL
  - Run tests via REPL for focus and speed.
  ```clj
  ;; Clojure — run a whole namespace
  (require 'app.backend.routes.api-test :reload)
  (clojure.test/run-tests 'app.backend.routes.api-test)

  ;; Clojure — run a single var
  (clojure.test/test-vars [#'app.backend.routes.api-test/metrics-endpoint-test])

  ;; ClojureScript — select build, reload ns, run
  (shadow.cljs.devtools.api/nrepl-select :app)  ;; or :admin
  (require 'app.domain.frontend.registry-test :reload)
  (cljs.test/run-tests 'app.domain.frontend.registry-test)
  ```
  - Follow existing file conventions for `deftest` naming (many use `*-test` suffix; some use `test-*` prefix). Keep names descriptive and stay consistent within the namespace; group related checks with `testing`; add `is` messages when useful.
  - Optional: Kaocha REPL commands are fine, but default `clojure.test`/`cljs.test` patterns above are preferred.
- Exploration helpers
  - Dynamic deps via `clojure.repl.deps/add-libs` are for exploration only; never commit them.
  - Avoid stdin reads in Babashka/nREPL contexts; pass data as args.
- Reload safety
  - Keep top-level code idempotent and side-effect-light so repeated `:reload` remains safe.

## Security middleware toggles
- HTTPS redirect can be disabled with `DISABLE_HTTPS_REDIRECT=true`; rate limiting with `DISABLE_RATE_LIMITING=true` (see `src/app/template/backend/middleware/security.clj`).
