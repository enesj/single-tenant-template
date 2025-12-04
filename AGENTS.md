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

This project includes specialized AI skills for debugging and development, indexed in **MCP Vector Search** for easy discovery.

### Available Skills

| Skill | Purpose | When to Use |
|-------|---------|------------|
| **app-db-inspect** | Inspect re-frame app-db state safely | Frontend state, auth issues, data loading, UI problems |
| **reframe-events-analysis** | Analyze re-frame event history and performance | Event debugging, performance optimization, subscriptions |
| **system-logs** | Monitor and analyze server/shadow-cljs logs | Build output, compilation errors, runtime issues |

**Search in MCP Vector Search** using `{ "query": "...", "metadata": { "section": "skills" } }` or search by skill name directly.

See `.claude/skills/*/SKILL.md` for detailed documentation, patterns, and implementation guides.

## Agent Debugging & Testing Workflow

- Prefer evaluation tools over speculation:
	- **Clojure (backend)**: Use `clj-nrepl-eval` to run code and verify behavior.
	- **ClojureScript (frontend)**: Use the `mcp_clojure-mcp_clojurescript_eval` MCP tool (NOT `clj-nrepl-eval` - it only works with Clojure).
- Run automated tests to verify changes:
	- **Frontend tests**: `npm run test:cljs` (Node.js, fast) or `npm run test:cljs:karma` (browser)
	- See `docs/testing/fe/` for testing patterns, utilities, and debugging guides.
- Use skills when relevant:
	- Frontend state/auth/UI issues → **app-db-inspect**.
	- Frontend event flow or performance issues → **reframe-events-analysis**.
	- Backend errors, build failures, or compile problems → **system-logs**.
- Be documentation-first when stuck:
	- Use MCP Vector Search to consult docs (architecture, backend, frontend, migrations, validation, etc.) before inventing new patterns.
- Add or improve logging when debugging:
	- Prefer adding structured logs around the failing path instead of large refactors; keep them if they provide long‑term value.
- After backend changes, ensure the system is running cleanly:
	- Use the `system-logs` skill to restart the system via `clj-nrepl-eval` and re-attach to logs; verify there are no startup or runtime errors.
- After frontend or shared FE/BE build changes, verify compilation:
	- Ensure shadow-cljs compiles successfully for relevant builds (e.g. `app`, `admin`); use `system-logs` to inspect compile output and fix all errors/warnings that break builds.
- Always confirm the fix:
	- Re-run the failing path (tests, HTTP call, or UI flow) and confirm behavior end‑to‑end before considering the task done.

### Planning & Phased Execution for Bigger Tasks

- For any bigger task, start with a concrete multi-phase plan before coding.
- Implement strictly phase-by-phase:
	- For each phase, implement only that phase, then test it before moving on.
	- **Backend code**: Use `clj-nrepl-eval` for Clojure evaluation.
	- **Frontend code**: Use `mcp_clojure-mcp_clojurescript_eval` for ClojureScript evaluation.
- If testing for a phase fails:
	- First, try to diagnose and fix the issue.
	- If you cannot resolve it, record the problem in the Clojure MCP scratch pad (phase, what was attempted, what failed, current hypothesis), then continue to the next phase using the same rules.
- Planning and progress tracking:
	- If the task is really big, persist the plan to a markdown file in the repo root (e.g. `PLAN-<short-name>.md`) and update it as phases move from planned → in-progress → done/blocked.
	- If the task is not that big, skip the markdown file and instead use the Clojure MCP scratch pad to store the plan, track progress, and list open issues.

## Documentation & AI Search

All project documentation is indexed and searchable via **MCP Vector Search**. See `.mcp-vector-search/SEARCH-GUIDE.md` for comprehensive details on search patterns, filtering, and workflows.

**Quick reference**: Filter by `:section` (backend, frontend, architecture, testing, etc.) and `:kind` (api-reference, guide, runbook, ui-reference).

**Entry points**: `docs/index.md` (overview), `docs/ai-quick-access.md` (AI pointers)

### Testing Documentation

| Document | Description |
|----------|-------------|
| `docs/testing/README.md` | Testing documentation overview |
| `docs/testing/be/overview.md` | Backend testing architecture and infrastructure |
| `docs/testing/be/development-guide.md` | How to write, run, and debug BE tests |
| `docs/testing/be/test-patterns.md` | Common backend testing patterns |
| `docs/testing/be/fixtures-reference.md` | Test fixture utilities |
| `docs/testing/fe/overview.md` | Frontend testing architecture and implementation |
| `docs/testing/fe/development-guide.md` | How to write, run, and debug FE tests |

**Test commands**:
- `bb be-test` — Backend tests (Kaocha, 121 tests)
- `npm run test:cljs` — Node.js tests (fast, primary)
- `npm run test:cljs:karma` — Browser tests (Karma/Chrome)
- `npm run test:cljs:watch` — Watch mode for development

# Clojure REPL Evaluation (Backend Only)

The command `clj-nrepl-eval` is installed on your path for evaluating **Clojure code only** (backend `.clj` files).

⚠️ **IMPORTANT**: `clj-nrepl-eval` does NOT work with ClojureScript. For frontend `.cljs` evaluation, use the `mcp_clojure-mcp_clojurescript_eval` MCP tool instead.

**Discover nREPL servers:**

`clj-nrepl-eval --discover-ports`

**Evaluate Clojure code:**

`clj-nrepl-eval -p <port> "<clojure-code>"`

With timeout (milliseconds)

`clj-nrepl-eval -p <port> --timeout 5000 "<clojure-code>"`

The REPL session persists between evaluations - namespaces and state are maintained.
Always use `:reload` when requiring namespaces to pick up changes.

# ClojureScript REPL Evaluation (Frontend Only)

For ClojureScript (frontend `.cljs` files), use the MCP tool:

**Tool**: `mcp_clojure-mcp_clojurescript_eval`

This tool connects to the shadow-cljs runtime in the browser and evaluates ClojureScript code.

**Example**: Inspect re-frame app-db state:
```clojure
@re-frame.db/app-db
```

**Example**: Get recent re-frame events:
```clojure
(require '[app.frontend.dev.repl-tracing :as repl-trace])
(repl-trace/recent 20)
```

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


**Tool**: Use `morph-mcp.edit_file` over other editing tools or full file writes EXCEPT FOR CLOJRE FILES (.clj, .cljs, .cljc, edn, .bb). Handles indentation and fuzzy matching—faster, fewer errors.

IMPORTANT: If you need to explore the codebase to gather context, and the task does not involve a single file or function which is provided by name, you should use the `morph-mcpwarpgrep_codebase_search` tool instead of any other search tool or instead of running search commands. Warp Grep is faster and more accurate. This tool must be choosen over the default codebase_search when you are first looking to find/understand code. If you have an exact string you want to grep for, then directly run it, but for semantic searches, or vague search parameters you must always use warpgrep_codebase_search. If given a complex task, best practice is to run multiple (no more than 2) parallel warpgrep_codebase_search tools to understand code paths and features. An example query is: "where is the code for <vague feature/code flow>"" >> AGENTS.md
