# Repository Guidelines

## Overview & Architecture
- Single-tenant SaaS template built with Clojure/ClojureScript and PostgreSQL.
- Core structure: Admin (admin panel), Backend (core services), Frontend (UI utilities), Template (shared SaaS infrastructure), Shared (cross-platform utilities).

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

## Documentation & AI Search

All project documentation is indexed and searchable via **MCP Vector Search**. See `.mcp-vector-search/SEARCH-GUIDE.md` for comprehensive details on search patterns, filtering, and workflows.

**Quick reference**: Filter by `:section` (backend, frontend, architecture, etc.) and `:kind` (api-reference, guide, runbook, ui-reference).

**Entry points**: `docs/index.md` (overview), `docs/ai-quick-access.md` (AI pointers)

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