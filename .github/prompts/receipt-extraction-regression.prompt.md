---
description: "Run the repository receipt extraction regression workflow after modifying OCR/LlamaParse receipt extraction code"
agent: "agent"
---

Run `@.github/agents/receipt-extraction-regression.agent.md` for this task.

Use `@.github/skills/receipt-extraction-regression/SKILL.md` as the primary workflow and source of truth.

Treat the active user prompt as the scope definition. If the prompt is broad or omitted, default to:

- the full stored LlamaParse receipts corpus
- focused backend validation in `app.domain.backend.expenses.integrations.llamaparse-test`
- artifact paths under `tmp/`

Required outputs:

- focused test result summary
- replay audit summary
- changed receipt list with before/after merchant names
- explicit classification of improvements vs regressions
- paths to saved `tmp/` artifacts

Non-negotiables:

- no provider calls
- use Clojure MCP eval against the running backend nREPL
- parallel replay must use pure logic only
- do not use `with-redefs` inside futures
- if no pre-change baseline exists, label the audit as heuristic
