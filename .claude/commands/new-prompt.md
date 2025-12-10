# Research repo docs and draft a high-quality next-session prompt

description: "Research repo docs and draft a high-quality next-session prompt into a root markdown file."
argument-hint: "PROMPT=\"short text about the upcoming work\""

You are creating a reusable research workflow to prep a strong working prompt for the *next* Agent session. Follow these steps exactly; adapt only the PROMPT argument. Always read the relevant docs at the start **and again after each implementation phase** to refresh context. Default to MCP server tools (skills, vector search, eval) whenever available instead of local/native tools. At the start, inspect available skills under `.claude/skills` (via MCP Vector Search or listing) and use them when applicable.

## 1) Inputs
- `PROMPT` (required): phrase for the upcoming work (e.g., "admin users pagination", "add domain X backend", "harden login events export").

## 2) Scope the research (docs first, skills aware)
- Skim the doc IA: `docs/index.md` and `docs/ai-quick-access.md`.
- List/recall skills from `.claude/skills/*/SKILL.md`; prefer invoking these MCP skills when they fit the task (e.g., state inspection, event tracing, system logs).
- Targeted reads (pick what fits TOPIC):
  - Backend: `docs/backend/single-tenant-template.md`, `docs/backend/http-api.md`, `docs/backend/services.md`, `docs/backend/security-middleware.md`.
  - Frontend: `docs/frontend/app-shell.md`, `docs/frontend/admin-panel-single-tenant.md`, `docs/frontend/feature-guides/admin.md`, `docs/frontend/template-component-integration.md`.
  - Ops/dev: `docs/operations/dev-environment.md`.
  - DB/migrations: `docs/migrations/migration-overview.md`, `resources/db/models.edn`.
- Use MCP Vector Search when helpful: `{ "query": "<TOPIC or namespace>", "metadata": { "section": "skills|backend|frontend|operations|migrations|architecture" } }`.
- Use `rg` for code pointers (e.g., `rg -n "<keyword>" src docs resources`).

## 3) Extract essentials (short notes; re-check docs mid-implementation)
- Architecture & entry points relevant to TOPIC (backend namespaces, frontend modules, routes/builds, DI wiring).
- Key commands to run (bb tasks, npm/shadow, migrations).
- HTTP surfaces / events / subs / DB tables touched by TOPIC.
- Security/auth/tenant assumptions (single-tenant defaults!).
- Testing guidance (bb be-test/fe-test, npm test:cljs, etc.).

## 4) Draft the next-session prompt (the deliverable)
- Audience: a future Agent session working in this repo.
- Include:
  - **Context snapshot**: 5–8 bullets summarizing architecture + current scope (single-tenant template).
  - **Task focus**: restate PROMPT and desired outcome.
  - **Code map**: relevant namespaces/files with 1-line purpose.
  - **Commands to run**: dev, tests, builds relevant to TOPIC.
  - **Gotchas**: auth, ports (8085), single-tenant assumptions, migrations workflow, RLS absent.
  - **Checklist**: concrete steps the next agent should follow.
- Keep it concise and action-oriented; no fluff.

## 5) Write the prompt file (repo root)
- Always write to `NEXT-PROMPT.md` in the repo root.
- Overwrite safely: `cat > "NEXT-PROMPT.md" <<'EOF' ... EOF`.
- Include date and PROMPT near the top.

## 6) Finish
- Verify the file exists and summarize what was captured (paths + commands noted).
- If something was missing (docs not found), note that in the file and in the chat reply.
- Tell the agent to prepare the comprehensive plan for implementation using the clojure-mcp scratch-pad tool. It should use this tool to track the progress.
- After plan is created it should start the implementation without asking for approval.

Output expected: a single markdown file in the repo root containing the crafted next-session prompt tailored to TOPIC.
