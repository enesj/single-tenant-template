---
description: "Apply a settings change through the real admin/user settings UI"
agent: "agent"
---

Run `@.github/agents/change-settings.agent.md` for this task.

Use `@docs/admin/frontend/unified-settings.md` as the architecture and config reference.

This skill makes **permanent changes** through the real browser UI. No code modifications, no config file edits, no app-db manipulation.

Extract from the user prompt:

- **Page**: `/admin/admin-settings` or `/admin/user-settings`
- **Entity**: which entity to configure
- **What to change**: setting name and target value
- **Tab** (if not obvious): View Options, Form Fields, or Table Columns

Workflow:

1. Navigate to the settings page.
2. Enter edit mode.
3. Select the entity.
4. Switch to the correct tab.
5. Read current state, apply the change.
6. Save if required (admin scope: form-fields and table-columns structural save immediately; view-options and policy need "Save settings". User scope: everything needs "Save settings").
7. Confirm the change persisted.
8. Report before/after state.

Do not verify on consuming pages. Do not restore changes. Do not modify code.
