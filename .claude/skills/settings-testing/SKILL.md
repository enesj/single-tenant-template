---
description: "Run real-UI settings verification for admin/user settings, record task-linked findings, and restore temporary changes"
metadata:
  tags: ["settings", "testing", "admin", "user-settings", "chrome-devtools", "verification"]
---

# settings-testing

Use this when someone asks to test:

- admin settings
- user settings
- view options
- form fields
- table columns
- lock propagation

## Source of truth

Follow:

- `AGENTS.md`
- `.github/copilot-instructions.md`
- `docs/admin/frontend/settings-testing.md`

## Default flow

Treat the invoking prompt as the scope definition for this run.

1. Create or reuse a plan document.
2. Create or reuse a results document.
3. Open the real settings page in one browser tab and the real consuming page in another.
4. Keep the relevant settings subform visible while validating.
5. Change one setting only.
6. Save if required.
7. Reload the consuming page.
8. Record pass/fail/inconclusive with the exact plan task reference.
9. Restore the original state.

## Input contract

The prompt should explain what exactly to test, for example:

- entity
- source settings page
- consuming page
- settings section
- task IDs or named checks
- data setup/cleanup needs

If the prompt is incomplete:

- keep the scope narrow
- make the smallest safe assumption
- state that assumption in the results
- do not silently expand into a full sweep

## Rules

- Use the real UI first; scripts are secondary evidence only.
- Do not rely on config inspection alone.
- Check for local `ui-entity-prefs` overrides before judging admin defaults.
- If locks are involved, also inspect the downstream settings UI where overrides would normally be attempted.
- If a value sticks or fails to propagate, record it instead of hiding it during cleanup.

## Before finishing

- Restore temporary settings changes.
- Restore temporary locks.
- Delete temporary test records.
- Record any unresolved sticky values.
- Record final restoration status in the results document.
