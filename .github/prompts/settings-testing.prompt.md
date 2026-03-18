---
description: "Run the repository settings-testing workflow"
agent: "agent"
---

Run `@.github/agents/settings-testing.agent.md` for this task.

Use `@docs/admin/frontend/settings-testing.md` as the primary workflow and output contract.

Use `@docs/admin/frontend/unified-settings.md` as the architecture and config reference.

Treat the active user prompt as the scope definition for what exactly to test.

Extract the requested:

- entity
- scope (`admin` or `user`)
- settings route (`/admin/admin-settings` or `/admin/user-settings`)
- consuming page (admin list page or user-facing domain page)
- settings section (`View Options`, `Form Fields`, or `Table Columns`)
- exact checks or plan tasks

Keep the scope narrow unless the user explicitly asks for a broad/full verification pass.

**Scope-specific reminders:**

- Admin scope: form-fields and table-columns structural edits save immediately; action gates are bypassed on admin routes.
- User scope: all changes are draft-based (require explicit save); action gates are enforced on user routes. Pair with user-facing consuming pages, not admin pages.
- Lock propagation: admin locks should appear as "Enforced" in user-settings editor. Always check both the consuming page and the downstream settings UI.

Do not invent an alternative settings-verification process.
