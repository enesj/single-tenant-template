---
name: Settings Testing
description: Runs real-UI verification of admin and user settings against live pages, records task-linked results, and restores temporary changes.
model: GPT-5.3-Codex (copilot)
tools: ['vscode', 'execute', 'read', 'edit', 'search', 'todo', 'chrome-devtools/*', 'clojure-mcp/*', 'postgres/*']
---

# Settings Testing Skill

Use this when asked to test:

- `/admin/admin-settings`
- `/admin/user-settings`
- view options
- form fields
- table columns
- list behavior
- settings locks or propagation

## Source of truth

Read and follow:

- `AGENTS.md`
- `.github/copilot-instructions.md`
- `docs/admin/frontend/settings-testing.md`

If they conflict, follow the stricter rule.

## Primary workflow

Treat the invoking prompt as the scope definition for this run.

1. Create or reuse a `...-VERIFICATION-PLAN.md`.
2. Create or reuse a `...-VERIFICATION-RESULTS.md`.
3. Use `chrome-devtools` on the real running app.
4. Keep the source settings page and the live consuming page open side by side.
5. Change one setting at a time.
6. Save if required.
7. Reload the consuming page and verify the visible effect.
8. Record the result against the exact plan task ID.
9. Restore the temporary change and confirm restoration.

## Input contract

The prompt should tell you what exactly to test, for example:

- entity
- source settings page
- consuming page
- settings section
- plan task IDs
- required setup or cleanup

If the prompt is incomplete:

- keep the scope narrow
- make the smallest safe assumption
- state that assumption in the results
- do not expand into a full settings sweep unless explicitly requested

## Non-negotiable rules

- Do not do script-only verification when a real UI interaction is possible.
- Do not mark a settings test complete without checking the consuming page.
- Use DOM/script checks only as secondary evidence after the UI action.
- Watch for local `ui-entity-prefs` overrides before calling a default-setting test failed.
- If a value is sticky or fails to propagate, record that explicitly instead of silently cleaning it up.

## What to test

Always separate these categories in the results:

- display toggles
- form fields
- table-column visibility
- table-column labels
- filterable/sortable behavior
- list behavior
- locks and lock propagation
- cleanup/restoration

## Lock-specific rule

A lock test is incomplete unless it checks both:

1. the live consuming page
2. the downstream settings UI where a user would try to override it

## Cleanup contract

Before finishing:

- restore temporary toggles
- restore temporary locks
- delete temporary test records
- document any remaining sticky values
- record final badge/summary state after restoration
