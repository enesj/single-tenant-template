# Repository Guidelines

## Overview & Architecture
- Single-tenant SaaS template built with Clojure/ClojureScript and PostgreSQL.
- Core structure: Admin (admin panel), Template (shared SaaS infrastructure), Domain (feature modules), Shared (cross-platform utilities).

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
src/app/        # admin, template, domain, shared (plus a small frontend/ folder for global assets)
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
- Admin settings pages:
	- `/admin/admin-settings` (admin UI config) and `/admin/user-settings` (domain-owned user UI config); `/admin/settings` is a legacy redirect.
- Dev stderr: suppressed by default; set `DEV_SUPPRESS_STDERR=false` (or `0`/`no`) to keep stderr visible.
- Dev watchers ignore runtime-edited UI config EDNs under `src/app/admin/frontend/config/*.edn` and `src/app/domain/**/config/*.edn` to avoid disruptive reloads.
## Debugging & Development Tools

This project includes specialized AI skills for debugging and development. Use **Morph MCP (Warp Grep)** to quickly discover relevant docs and skills.

### Available Skills

| Skill | Purpose | When to Use |
|-------|---------|------------|
| **app-db-inspect** | Inspect re-frame app-db state safely | Frontend state, auth issues, data loading, UI problems |
| **reframe-events-analysis** | Analyze re-frame event history and performance | Event debugging, performance optimization, subscriptions |
| **system-logs** | Monitor and analyze server/shadow-cljs logs | Build output, compilation errors, runtime issues |

**Search with Morph MCP (Warp Grep)** by querying for skill names or topics (e.g., “app-db-inspect”, “system-logs”, “re-frame events”).

See `.claude/skills/*/SKILL.md` for detailed documentation, patterns, and implementation guides.

## Agent Debugging & Testing Workflow

- Prefer evaluation tools over speculation:
	- **Clojure (backend)**: Use the `mcp__clojure-mcp__clojure_eval` MCP tool to run code and verify behavior.
	- **ClojureScript (frontend)**: Use the `mcp__clojure-mcp__clojurescript_eval` MCP tool.
- Run automated tests to verify changes:
		- **Frontend tests**: `npm run test:cljs` (Node.js, fast) or `npm run test:cljs:karma` (browser)
		- **Frontend config checks (fast)**: `bb validate-frontend-config`, `bb config-audit --strict` (CI uses `npm run test:config-audit`)
		- See `docs/testing/fe/` for testing patterns, utilities, and debugging guides.
	- **🚨 CRITICAL: ALWAYS save full test output FIRST - never run tests multiple times:**
		```bash
		# ✅ GOOD - run once, analyze many times
		npm run test:cljs 2>&1 | tee /tmp/test-output.txt
		grep "FAIL" /tmp/test-output.txt

		# ❌ BAD - runs tests multiple times (wasteful)
		npm run test:cljs | grep FAIL
		npm run test:cljs | grep ERROR
		```
	- Never run the entire test suite when you're working on a concrete problem. Run only the relevant test(s): a single test file, a focused test suite, or tagged/filtered tests (use your test runner's selector/filter options). This speeds feedback and avoids noise.
- Use skills when relevant:
	- Frontend state/auth/UI issues → **app-db-inspect**.
	- Frontend event flow or performance issues → **reframe-events-analysis**.
	- Backend errors, build failures, or compile problems → **system-logs**.
- Be documentation-first when stuck:
	- Use Morph MCP (Warp Grep) to consult docs (architecture, backend, frontend, migrations, validation, etc.) before inventing new patterns.
- Add or improve logging when debugging:
	- Prefer adding structured logs around the failing path instead of large refactors; keep them if they provide long‑term value.
- After backend changes, ensure the system is running cleanly:
	- Use the `system-logs` skill to restart the system via `mcp__clojure-mcp__clojure_eval` and re-attach to logs; verify there are no startup or runtime errors.
- After frontend or shared FE/BE build changes, verify compilation:
	- Ensure shadow-cljs compiles successfully for relevant builds (e.g. `app`, `admin`); use `system-logs` to inspect compile output and fix all errors/warnings that break builds.
- Always confirm the fix:
	- Re-run the failing path (tests, HTTP call, or UI flow) and confirm behavior end‑to‑end before considering the task done.

### Planning & Phased Execution for Bigger Tasks

- For any bigger task, start with a concrete multi-phase plan before coding.
- Implement strictly phase-by-phase:
	- For each phase, implement only that phase, then test it before moving on.
	- **Backend code**: Use `mcp__clojure-mcp__clojure_eval` for Clojure evaluation.
	- **Frontend code**: Use `mcp__clojure-mcp__clojurescript_eval` for ClojureScript evaluation.
- If testing for a phase fails:
	- First, try to diagnose and fix the issue.
	- If you cannot resolve it, record the problem in the Clojure MCP scratch pad (phase, what was attempted, what failed, current hypothesis), then continue to the next phase using the same rules.
- Planning and progress tracking:
	- If the task is really big, persist the plan to a markdown file in the repo root (e.g. `PLAN-<short-name>.md`) and update it as phases move from planned → in-progress → done/blocked.
	- If the task is not that big, skip the markdown file and instead use the Clojure MCP scratch pad to store the plan, track progress, and list open issues.

## Component ID Requirements (Browser Testing)

🚨 **CRITICAL**: All interactive UI components MUST have unique `:id` attributes for browser testing via **chrome-mcp**.

This enables automated browser testing where elements are located by ID. See `INTERACTIVE-COMPONENTS-ID-AUDIT.md` for the full audit and patterns.

### ID Patterns by Component Type

| Component Type | ID Pattern | Example |
|---------------|------------|----------|
| Form fields | `(str formId "-" field-type)` | `"login-form-input"`, `"signup-form-select"` |
| Buttons | `(str "btn-" action "-" context)` | `"btn-submit-login"`, `"btn-delete-users-123"` |
| Settings toggles | `(str "toggle-" label "-" entity)` | `"toggle-timestamps-users"` |
| Column toggles | `(str "col-toggle-" entity "-" field)` | `"col-toggle-users-email"` |
| Action dropdowns | `(str "actions-btn-" entity-id)` | `"actions-btn-123"` |
| Filter controls | `(str "filter-" type "-" field)` | `"filter-toggle-users-name"` |

### When Creating New Components

1. **Always accept an `:id` prop** in the component's props map
2. **Generate fallback IDs** when explicit ID not provided:
   ```clojure
   (let [field-id (or id (when formId (str formId "-input")))]
     ($ :input {:id field-id ...}))
   ```
3. **Error elements** should have IDs too: `(str field-id "-error")`
4. **Test your component** is selectable via chrome-mcp before merging

### Form Field Components (Already Implemented)

All form fields in `src/app/template/frontend/components/form/fields/` now auto-generate IDs:
- `input.cljs` → `(str formId "-input")`
- `select.cljs` → `(str formId "-select")`
- `checkbox.cljs` → `(str formId "-checkbox")`
- `textarea.cljs` → `(str formId "-textarea")`
- `number.cljs` → `(str formId "-number")`

---

## Documentation & AI Search

Use **Morph MCP (Warp Grep)** as the standard way to search project documentation (`docs/**`) and skill docs (`.claude/skills/**`).

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

**🚨 IMPORTANT: Always save test output before analysis:**
```bash
# Frontend - save and analyze
npm run test:cljs 2>&1 | tee /tmp/fe-test.txt
# Backend - save and analyze
bb be-test 2>&1 | tee /tmp/be-test.txt
# Then grep/search the saved files - NEVER re-run tests!
```

**🚨 NEW RULE:** When working on a concrete problem, do NOT run the entire test suite — run only the relevant test(s) (single test file, focused suite, or tagged tests). This reduces turnaround time and avoids noisy output; save the full output before analysis as above.

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


**Tool**: Use `morph-mcp.edit_file` over other editing tools or full file writes EXCEPT FOR CLOJRE FILES (.clj, .cljs, .cljc, edn, .bb). Handles indentation and fuzzy matching—faster, fewer errors.
To work with CLojure files use the Clojure-mcp tools.


IMPORTANT: If you need to explore the codebase to gather context, and the task does not involve a single file or function which is provided by name, use `mcp__morph-mcp__warpgrep_codebase_search` instead of other search tools. If you have an exact string, include it in your Warp Grep query; for semantic/vague searches, phrase the query in natural language. If given a complex task, best practice is to run multiple (no more than 2) parallel Warp Grep searches to understand code paths and features (e.g., “where is the code for <vague feature/code flow>”).

Finally read `.github/copilot-instructions.md` for general coding instructions.
