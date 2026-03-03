---
name: stabilize-e2e
description: "Run bb e2e-test, triage failures, and apply minimal fixes until green (Clojure-first, multi-tenant aware)."
argument-hint: "Optional: Kaocha --focus namespace (e.g., app.e2e.multi-tenancy.data-scoping-test) or a short note like 'platform-admin-test failing'."
agent: agent
---

You are working in the `single-tenant-template` repo on macOS.

## What to do

Make the E2E suite pass (or the requested focused subset) by:

- running the tests,
- reading the failure output once (saved under `tmp/`),
- finding the root cause (often boundary mismatches: auth/authz, tenant scoping, JSON key casing, UUID/defaults, DB/profile mismatch),
- making the smallest correct code/test changes,
- re-running focused tests until green,
- then re-running the full E2E suite.

## Inputs you may get

- A Kaocha focus namespace (via `bb e2e-test --focus <ns>`), or a general instruction to run `bb e2e-test`.
- A failing log path under `tmp/`.
- A description of one or more failing tests.

If no focus is provided, start with the full E2E suite.

## Hard rules (repo constraints)

- Follow `AGENTS.md` and `.github/copilot-instructions.md`.
- **No secrets**: do not read, quote, request, or edit `config/.secrets.edn`, `~/.secrets.edn`, `.env`, CI secrets, etc. If a secret/config is required, tell the user exactly what to add using placeholders (e.g. `"REDACTED"`).
- **DB access**: for agent-driven DB inspection/queries use the Postgres MCP tools (not `psql`, not shelling out).
- **Schema changes**: do not hand-edit live DB schema; use the repo’s migrations workflow (and apply to dev + test) when schema changes are truly needed.
- **Clojure/EDN edits**: use structure-aware Clojure MCP edits for `.clj`/`.cljs`/`.cljc`/`.edn`.
- Keep changes small and focused; avoid unrelated refactors.

## Workflow (follow in order)

1. **Run tests once and save output**
   - Always `mkdir -p tmp`.
   - Save the complete output to `tmp/` with `tee`.
   - Do not re-run just to grep.

2. **Triage and isolate**
   - Identify failing namespaces/tests.
   - Prefer a focused rerun (`bb e2e-test --focus <namespace>`) while iterating.

3. **Debug with evidence**
   - If a test inserts via `fixtures/query-db` and reads via API, verify both are using the same test system + test DB.
   - If a failure looks like “row missing”, check for:
     - tenant filters accidentally applied,
     - JSON key casing mismatch,
     - missing UUID `id` (DB tables often have `id uuid NOT NULL` with **no default**),
     - 401/403 middleware short-circuiting.

4. **Common session-learned gotchas to check**
   - **JSON casing**: `route-utils/success-response` converts keys all the way to **camelCase**. Tests should look for `:displayName` (not only `:display-name` / `:display_name`).
   - **UUID PK defaults**: if inserts fail with `NOT NULL` on `id`, supply an `id` in direct SQL inserts and/or generate one in CRUD create paths (only when metadata indicates UUID primary key).
   - **Auth/authz**: for role-based access checks, ensure write methods return 403 for viewers rather than reaching the DB.

5. **Implement minimal fix**
   - Update backend behavior OR tests (whichever is wrong) in the smallest way.
   - Ensure HTTP status codes match expectations (401 vs 403 vs 404).

6. **Validate**
   - Rerun the focused failing namespace until it’s green.
   - Then rerun full `bb e2e-test`.

## Output contract (what you must report back)

- What you ran (test commands) and where the log was saved under `tmp/`.
- A concise root-cause explanation for each failure fixed.
- Files changed (with 1-line purpose each).
- Evidence of success: focused rerun + full E2E summary line.
- Update the todo list: mark completed vs pending.

## When blocked

If you hit something that requires user input (e.g., missing external service credentials), stop and ask for the minimum required info, with exact file paths/keys to modify using redacted placeholders.