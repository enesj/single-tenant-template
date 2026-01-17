## Repo Instructions for Copilot Chat

- Read `AGENTS.md` first for repo-wide policy and workflow; use this file for implementation guidance (coding patterns, migrations, common issues, security checks).
- Scripting policy: never create/run Python scripts in this repo; use Babashka (`.bb`) or Bash (`.sh`) as needed (details in `AGENTS.md`).

### Instruction Precedence

- If two instructions conflict, follow the more specific one for the code/path you are working on.
- If they are equally specific, prefer `AGENTS.md` for policy/workflow and this file for implementation details.

### Workflow (See `AGENTS.md`)

- Follow `AGENTS.md` for documentation-first search (Warp Grep), debugging/testing discipline, and phased execution planning.
- Frontend tests: prefer `bb fe-test-parallel` (Node.js, fast, parallel). NPM alias: `npm run test:cljs:parallel`. Browser tests: `npm run test:cljs:karma`.
- Save output first when running suites:
  - `bb fe-test-parallel 2>&1 | tee /tmp/frontend-test-$(date +%H%M%S).txt`
  - `bb be-test 2>&1 | tee /tmp/backend-test-$(date +%H%M%S).txt`

### Coding & Migrations

- Follow the conventions in `.claude/coding.instructions.md` for:
  - Naming, architecture, and reuse patterns.
  - Migrations workflow (edit canonical EDN, use REPL helpers, never touch generated migrations).
  - Common backend/frontend issues and security guidelines.

# Coding & Development Instructions

## Coding Style & Patterns
- Naming: DB tables/columns use `snake_case`; code uses kebab-case (`:user-settings`). Namespaces dotted (e.g., `app.template.frontend.events`).

- Architecture: protocols-first services; enforce security middleware; keep concerns isolated.
- Reuse-first: for functionality, use shared logic/utilities (`src/app/shared/**`, `src/app/template/shared/**`) before adding new code to FE or BE.
- Frontend composition: for UI, start with template components (`src/app/template/frontend/**`); reuse other components defined in current of folders containing current folder; and create new components only if nothing existing fits.
- UI: use DaisyUI component classes prefixed with `ds-` (e.g., `ds-btn`, `ds-card`) when creating new components or modifying shared components. Tailwind utilities remain unprefixed (`flex`, `text-sm`).

# Clojure REPL Evaluation

Use the **clojure-mcp** MCP server tools for evaluating code:

- **Backend (Clojure `.clj`)**: Use `mcp__clojure-mcp__clojure_eval` to run code and verify behavior.
- **Frontend (ClojureScript `.cljs`)**: Use `mcp__clojure-mcp__clojurescript_eval` for frontend evaluation.

The MCP tools provide persistent REPL sessions - namespaces and state are maintained between evaluations.
Always use `:reload` when requiring namespaces to pick up changes.

**Troubleshooting ClojureScript REPL**:
If you get a `FileNotFoundException` when requiring `.cljs` files, it means the REPL is in Clojure (JVM) mode. Switch to the ClojureScript runtime by evaluating:
```clojure
(shadow.cljs.devtools.api/nrepl-select :app)
```
(Replace `:app` with `:admin` if working on the admin panel).

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


## Migrations Workflow

⚠️ **IMPORTANT: Read the migrations documentation first.** Use Morph MCP (Warp Grep) to search `docs/migrations/**` (and related docs) before making any changes. Key docs: `docs/migrations/complete-guide.md`, `docs/migrations/migration-overview.md`.

**Quick workflow**: Edit canonical EDN under `resources/db/{template,shared}` → run REPL helpers via `src/app/migrations/simple_repl.clj` → never hand-edit `resources/db/migrations/*`.

**Databases**: Dev on `:55432`, test on `:55433`. Use `bb backup-db` / `bb restore-db` before migrations for safety.

## Common Issues & Fixes
- PostgreSQL JSON serialization: convert PG-specific objects (PGobject, arrays, timestamps) before returning API responses.
  - Pattern: apply a DB serialization helper (e.g., `convert-pg-objects`) to query results before `response/ok`.
- Namespaced keys: JOINs often return `:table/col`; normalize to simple keys where callers expect them (e.g., `:id` via `(or (:id x) (:admins/id x))`).
- Re-frame orchestration: ensure `app.template.frontend.events.core` is loaded so event namespaces register.
- Entity store sync: after updates, refresh both the feature store and UI read locations to avoid empty tables until refresh.
- HoneySQL clause keywords: verify correct keyword shapes (`:id` vs `:users/id`) to prevent silent query issues.

### Recurring frontend issue: list pages show only timestamp columns

- **Symptom:** list table renders only `created-at` / `updated-at` columns.
- **Root cause (most common):** entity spec lookup returned `nil` due to **entity key mismatch** (UI uses app/kebab-case like `:price-observations`, but specs/models/config may be keyed by db/snake_case like `:price_observations`).
- **Fix:** ensure the spec map keys are normalized to **app/kebab-case** (use `model-naming/db-keyword->app` in spec generation / lookups).

### Common follow-up: API has data but table rows are empty

- **Symptom:** Network response has non-empty `{:data [...]}`, columns render, but list is empty.
- **Root cause (common):** re-frame handlers using `common-interceptors` include `trim-v`; handler event vectors are **already trimmed**.
  - Use handler args like `[params]`, `[response]`, `[error]` (NOT `[_ params]`, `[_ response]`, `[_ error]`). Otherwise you bind `nil` and end up syncing `[]`.

## Frontend UI Conventions
- Effective `:entity-spec`: When a page's table uses customized or computed fields, pass the effective spec to `list-view` via `:entity-spec`. The table forwards it to the column settings so toggles operate on the exact rendered fields.
- Fallback behavior: If `:entity-spec` is omitted, settings fall back to the template spec; computed/admin-only fields might be missing from toggles.
- Example:
  ```clojure
  (comment
    ($ list-view
       {:entity-name :users
        :entity-spec users-entity-spec
        :title "Users"}))
  ```
- Recommendation: Admin pages should pass the spec produced by the admin spec generator to ensure toggles match admin-visible columns.

## 🚨 Component ID Requirements (Browser Testing)

All interactive UI components MUST have unique `:id` attributes for automated browser testing via **chrome-mcp**.

See `AGENTS.md` ("Component ID Requirements") and `INTERACTIVE-COMPONENTS-ID-AUDIT.md` for the canonical patterns and examples.

## Security & Configuration
- Secrets: never commit; keep in `config/.secrets.edn` and environment vars for CI/CD.
- Security checks (manual):
  - `curl -I https://localhost:8085/admin` (headers)
  - `curl -k http://localhost:8085/admin` (HTTPS redirect)
  - `curl -H "X-Forwarded-For: 192.168.1.100" http://localhost:8085/api/test` (rate limiting)
- Optional services: see `docker-compose.yml`; document port/env changes.
