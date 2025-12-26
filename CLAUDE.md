# Repository Guidelines

## Overview & Architecture
- Single-tenant SaaS template built with Clojure/ClojureScript and PostgreSQL.
- Core structure: Admin (admin panel), Backend (core services), Frontend (UI utilities), Template (shared SaaS infrastructure), Shared (cross-platform utilities).

## 🚨 SCRIPTING POLICY: NO PYTHON - Babashka/Bash Only

**NEVER create or run Python scripts in this repository.** This is a Clojure-focused codebase.

**Use Babashka (.bb files) as the preferred scripting language** - it provides:
- Native Clojure syntax and ecosystem access
- Fast startup times with GraalVM
- Seamless integration with existing Clojure code and tools
- Rich library support for common tasks

**Use Bash scripts (.sh) only when**:
- Babashka cannot handle the specific requirement
- Interacting with system-level tools that require shell commands
- Simple wrapper scripts for existing tools

**Why not Python?**
- This repository uses Clojure/ClojureScript exclusively
- Python introduces unnecessary dependencies and complexity
- Babashka provides better integration with the existing Clojure ecosystem

## Project Structure (Quick Map)
```
src/app/        # admin, backend, frontend, migrations, shared, template
test/           # *_test.clj / *_test.cljs mirroring src
resources/      # public assets, db models/migrations
config/         # base + secrets (local only)
cli-tools/      # dev utilities, test scripts
scripts/        # build/dev/testing helpers (sh, bb)
vendor/         # vendored libs (automigrate, ring, etc.)
Key configs: deps.edn, shadow-cljs.edn, resources/db/models.edn
```


## Development & Commands
- App start: App is ALWAYS RUNNING during development; no need to restart manually because the system automatically restarts after FE/BE changes.
- Admin UI is served by default at `http://localhost:8085` (not 3000); use that port in local testing and curl checks.

## Debugging & Development Tools

- Don't try to revert the changes made by other agents or user during your session unless they are making the problem to you working on your task. If so ask user what to do.

This project includes specialized AI skills that activate automatically:

- **app-db-inspect** - Inspect re-frame app-db state safely (mention: app-db, re-frame state, frontend state)
- **reframe-events-analysis** - Analyze re-frame event history and performance (mention: events, event history, tracing)
- **system-logs** - Monitor and analyze server/shadow-cljs logs (mention: logs, build output, compilation errors)

Use these skills when debugging by describing your issue naturally in Chat.

## 🚨 Critical Testing Workflow

**ALWAYS save test output FIRST - never run tests multiple times:**

```bash
# ✅ GOOD - run once, analyze many times
bb be-test 2>&1 | tee /tmp/be-test.txt
npm run test:cljs 2>&1 | tee /tmp/fe-test.txt
# Then grep the saved files repeatedly

# ❌ BAD - wasteful re-runs
bb be-test | grep FAIL
bb be-test | grep ERROR
```

## Component ID Requirements (Browser Testing)

🚨 **CRITICAL**: All interactive UI components MUST have unique `:id` attributes for browser testing via **chrome-mcp**.

### When Creating New Components

1. **Always accept an `:id` prop** and generate fallback IDs:
   ```clojure
   (let [field-id (or id (when formId (str formId "-input")))]
     ($ :input {:id field-id ...}))
   ```
2. **Error elements** need IDs too: `(str field-id "-error")`
3. See `INTERACTIVE-COMPONENTS-ID-AUDIT.md` for patterns and examples

### Standard ID Patterns

- Form fields: `(str formId "-" field-type)` → `"login-form-input"`
- Buttons: `(str "btn-" action "-" context)` → `"btn-delete-users-123"`
- Toggles: `(str "toggle-" label "-" entity)` → `"toggle-edit-users"`
- Dropdowns: `(str "actions-btn-" entity-id)` → `"actions-btn-123"`

---

## Documentation & AI Search

Use **Morph MCP (Warp Grep)** as the standard way to search project documentation (`docs/**`) and skill docs (`.claude/skills/**`).

**Entry points**: `docs/index.md` (overview), `docs/ai-quick-access.md` (AI pointers)

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
