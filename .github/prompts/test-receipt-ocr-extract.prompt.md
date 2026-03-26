---
description: "Test receipt OCR extract workflow modifications with focused tests, batch reparses, and markdown reporting"
agent: "agent"
---

Run `@.claude/commands/test-receipt-ocr-extract.md` exactly as the active instructions for this task.

Treat the active user prompt as the scope definition for this run, including:

- profile (`dev`, `test`, or `prod`; default `dev`)
- OCR workflow/provider (`RECEIPT_OCR_WORKFLOW`)
- batch size (default `20`)
- whether to run the full receipt corpus or only selected batches
- whether receipts should be reset before rerun
- which focused backend tests should be run before reparsing
- whether to create or update `receipt-ocr-batch-testing-results.md`

Use the reusable scripts under `scripts/bb/expenses/` for batch planning and batch reparse execution.

If the current client exposes a direct Clojure MCP eval tool, prefer it for one-off inspection and targeted validation. For repeatable batch execution and artifacts, still use the repo scripts so the workflow is reproducible.