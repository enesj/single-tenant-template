## Repo Instructions for Copilot Chat

- Treat this file and `AGENTS.md` as your primary instructions for this repo.
- Never revert changes authored by the user or another agent during this session unless those changes explicitly conflict with the Copilot task you are executing.

### Documentation-First Approach

- **At the beginning of any new task**, use Morph MCP (Warp Grep) to find and read relevant documentation before writing code.
- Search for concepts, patterns, or domain terms related to the task (e.g., "authentication", "migrations", "validation", "entity store").
- This ensures you follow established patterns and avoid reinventing solutions that already exist in the codebase.

### Coding & Migrations

- Follow the conventions in `.claude/coding.instructions.md` for:
  - Naming, architecture, and reuse patterns.
  - Migrations workflow (edit canonical EDN, use REPL helpers, never touch generated migrations).
  - Common backend/frontend issues and security guidelines.

### Debugging & Testing

- Prefer evaluation tools over speculation:
  - **Clojure (backend `.clj`)**: Use the `mcp__clojure-mcp__clojure_eval` MCP tool to run code and verify behavior.
  - **ClojureScript (frontend `.cljs`)**: Use the `mcp__clojure-mcp__clojurescript_eval` MCP tool.
- Use the project’s debugging skills when relevant:
  - Frontend state/auth/UI issues → **app-db-inspect**.
  - Frontend event flow or performance issues → **reframe-events-analysis**.
  - Backend errors, build failures, or compile problems → **system-logs**.
- Be documentation-first when stuck:
  - Use Morph MCP (Warp Grep) to consult project docs before inventing new patterns.
- Add or improve logging when debugging:
  - Prefer small, targeted logs around the failing path over large refactors; keep high-value logs.
- After backend changes:
  - Use the `system-logs` skill to restart the system and re-attach to logs; ensure there are no startup/runtime errors.
- After frontend or shared FE/BE build changes:
  - Run shadow-cljs compile for the relevant builds (e.g. `app`, `admin`) and fix any breaking errors/warnings.
- When you need to verify behavior for a concrete problem, do NOT run the full suite — run just the relevant test(s) (single file, focused suite, or tagged tests) so feedback stays fast and output stays manageable.
- Always confirm fixes

### Planning & Phased Execution

- For any bigger task, start with a concrete multi-phase plan before coding.
- Implement strictly phase-by-phase and test each phase before moving on:
  - **Backend**: Use `mcp__clojure-mcp__clojure_eval` for Clojure.
  - **Frontend**: Use `mcp__clojure-mcp__clojurescript_eval` for ClojureScript.
- If a phase cannot be fully fixed after testing, record the problem in the Clojure MCP scratch pad (phase, what was attempted, what failed, current hypothesis) and then continue with the next phase.
- For really big tasks, create a markdown plan file in the repo root (e.g. `PLAN-<short-name>.md`) and use it to track phases and progress; otherwise, use the Clojure MCP scratch pad to store the plan, progress, and open issues.

# Coding & Development Instructions

## Coding Style & Patterns
- Naming: DB tables/columns use `snake_case`; code uses kebab-case (`:user-settings`). Namespaces dotted (e.g., `app.template.frontend.events`).

- Architecture: protocols-first services; enforce security middleware; keep concerns isolated.
- Reuse-first: for functionality, use shared logic/utilities (`src/app/shared/**`, `src/app/template/shared/**`) before adding new code to FE or BE.
- Frontend composition: for UI, start with template components (`src/app/template/frontend/**`); reuse other components defined in current of folders containing current folder; and create new components only if nothing existing fits.
- UI: use DaisyUI component classes prefixed with `ds-` (e.g., `ds-btn`, `ds-card`) when creating new components or modifying shared components. Tailwind utilities remain unprefixed (`flex`, `text-sm`).

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

**All interactive UI components MUST have unique `:id` attributes** for automated browser testing via **chrome-mcp**.

### When Creating New Components

1. **Always accept an `:id` prop** in component props
2. **Generate fallback IDs** when explicit ID not provided:
   ```clojure
   (let [field-id (or id (when formId (str formId "-input")))]
     ($ :input {:id field-id ...}))
   ```
3. **Error elements** should also have IDs: `(str field-id "-error")`

### Standard ID Patterns

| Component Type | Pattern | Example |
|---------------|---------|----------|
| Form fields | `(str formId "-" type)` | `"login-form-input"` |
| Buttons | `(str "btn-" action "-" ctx)` | `"btn-delete-users-123"` |
| Toggles | `(str "toggle-" label "-" entity)` | `"toggle-edit-users"` |
| Column toggles | `(str "col-toggle-" entity "-" field)` | `"col-toggle-users-email"` |
| Action dropdowns | `(str "actions-btn-" id)` | `"actions-btn-123"` |

### Reference

- **Audit report**: `INTERACTIVE-COMPONENTS-ID-AUDIT.md` (patterns, examples, implementation status)
- **Form fields**: All fields in `components/form/fields/` auto-generate IDs from `formId`

## Security & Configuration
- Secrets: never commit; keep in `config/.secrets.edn` and environment vars for CI/CD.
- Security checks (manual):
  - `curl -I https://localhost:8085/admin` (headers)
  - `curl -k http://localhost:8085/admin` (HTTPS redirect)
  - `curl -H "X-Forwarded-For: 192.168.1.100" http://localhost:8085/api/test` (rate limiting)
- Optional services: see `docker-compose.yml`; document port/env changes.
