## Repo Instructions for Copilot Chat

- Treat this file and `AGENTS.md` as your primary instructions for this repo.
- Also load `.claude/coding.instructions.md` for detailed coding, migrations, and security rules.

### Coding & Migrations

- Follow the conventions in `.claude/coding.instructions.md` for:
  - Naming, architecture, and reuse patterns.
  - Migrations workflow (edit canonical EDN, use REPL helpers, never touch generated migrations).
  - Common backend/frontend issues and security guidelines.

### Debugging & Testing

- Prefer evaluation tools over speculation:
  - Use Clojure/ClojureScript eval (e.g. `clj-nrepl-eval`) to run code and verify behavior instead of guessing.
- Use the project’s debugging skills when relevant:
  - Frontend state/auth/UI issues → **app-db-inspect**.
  - Frontend event flow or performance issues → **reframe-events-analysis**.
  - Backend errors, build failures, or compile problems → **system-logs**.
- Be documentation-first when stuck:
  - Use MCP Vector Search to consult project docs before inventing new patterns.
- Add or improve logging when debugging:
  - Prefer small, targeted logs around the failing path over large refactors; keep high-value logs.
- After backend changes:
  - Use the `system-logs` skill to restart the system via `clj-nrepl-eval` and re-attach to logs; ensure there are no startup/runtime errors.
- After frontend or shared FE/BE build changes:
  - Ensure shadow-cljs compiles for the relevant builds (e.g. `app`, `admin`) and fix any breaking errors/warnings.
- Always confirm fixes:
  - Re-run the failing tests, HTTP calls, or UI flows and confirm correct behavior end-to-end before considering the task done.

### Planning & Phased Execution

- For any bigger task, start with a concrete multi-phase plan before coding.
- Implement strictly phase-by-phase and test each phase with Clojure/ClojureScript eval tools (MCP eval, `clj-nrepl-eval`, etc.) before moving on.
- If a phase cannot be fully fixed after testing, record the problem in the Clojure MCP scratch pad (phase, what was attempted, what failed, current hypothesis) and then continue with the next phase.
- For really big tasks, create a markdown plan file in the repo root (e.g. `PLAN-<short-name>.md`) and use it to track phases and progress; otherwise, use the Clojure MCP scratch pad to store the plan, progress, and open issues.
