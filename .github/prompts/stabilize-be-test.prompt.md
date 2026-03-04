---
name: stabilize-be-test
description: "Run bb be-test, triage failures, and apply minimal fixes until green (Clojure-first, multi-tenant aware)."
argument-hint: "Optional: Kaocha focus namespace (e.g., app.template.backend.services.member-test) or path to a tmp/*.txt log."
agent: agent
model: ["GPT-5 (copilot)"]
---

You are working in the `single-tenant-template` repo on macOS.

## What to do

Make the backend test suite pass (or the requested focused subset) by:

- running the tests,
- saving the complete output once under `tmp/` with `tee`,
- triaging failures (prefer smallest reproducible set),
- finding the root cause with evidence (not guesses),
- making the smallest correct code/test changes,
- re-running focused tests until green,
- then re-running the full `bb be-test`.

## Inputs you may get

- A Kaocha focus namespace (via `bb be-test --grep <namespace-regex>` or `bb be-test --focus <ns>` depending on repo tasks).
- A failing log path under `tmp/`.
- A description of one or more failing tests.

If no focus is provided, start with full `bb be-test`.

## Hard rules (repo constraints)

- Follow `AGENTS.md` and `.github/copilot-instructions.md`.
- **No secrets**: do not read, quote, request, or edit `config/.secrets.edn`, `~/.secrets.edn`, `.env`, CI secrets, etc. If a secret/config is required, tell the user exactly what to add using placeholders (e.g. `"REDACTED"`).
- **DB access**: for agent-driven DB inspection/queries use the Postgres MCP tools (not `psql`, not shelling out).
- **Schema changes**: do **not** directly edit files under `resources/db/migrations/`. If a schema/migration change is required, create a new migration via the repo workflow (and apply to dev + test).
- **Clojure/EDN edits**: use structure-aware Clojure MCP edits for `.clj`/`.cljs`/`.cljc`/`.edn`.
- Keep changes small and focused; avoid unrelated refactors.

## Workflow (follow in order)

1. **Run tests once and save output**
   - Always `mkdir -p tmp`.
   - Save the complete output to `tmp/` with `tee`.
   - Do not re-run just to grep.

2. **Triage and isolate**
   - Identify failing namespaces/tests.
   - Prefer a focused rerun while iterating.

3. **Debug with evidence**
   - If a failure looks like DB scoping, check:
     - dev vs test DB/profile mismatch,
     - tenant filters / ownership constraints,
     - key casing mismatch at boundaries (kebab/snake/camel),
     - UUID defaults (tables may require explicit UUIDs),
     - middleware masking (401 vs 403 vs 500).
   - If a failure looks like config:
     - confirm which config profile is active,
     - ensure errors are not swallowed and logs redact sensitive values.

4. **Implement minimal fix**
   - Update backend behavior OR tests (whichever is wrong) in the smallest way.
   - Ensure HTTP status codes and error shapes match expectations.

5. **Validate**
   - Rerun the focused failure until green.
   - Then rerun full `bb be-test`.

## Output contract (what you must report back)

- What you ran (commands) and where the log was saved under `tmp/`.
- A concise root-cause explanation for each failure fixed.
- Files changed (with 1-line purpose each).
- Evidence of success: focused rerun + full suite summary line.
- Update the todo list: mark completed vs pending.

## When blocked

If you hit something that requires user input (e.g., missing external service credentials), stop and ask for the minimum required info, with exact file paths/keys to modify using redacted placeholders.
