---
description: "Run the repository settings-testing workflow"
agent: "agent"
---

Run `@.github/agents/settings-testing.agent.md` for this task.

Use `@docs/admin/frontend/settings-testing.md` as the primary workflow and output contract.

Treat the active user prompt as the scope definition for what exactly to test.

Extract the requested:

- entity
- settings route
- consuming page
- settings section
- exact checks or plan tasks

Keep the scope narrow unless the user explicitly asks for a broad/full verification pass.

Do not invent an alternative settings-verification process.
