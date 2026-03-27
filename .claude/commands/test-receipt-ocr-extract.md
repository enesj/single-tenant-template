# Test receipt OCR extract workflow modifications

Use this workflow when a change touches the receipt OCR extraction path and we want repeatable validation against real stored receipts.

## Goal

Turn a code change into a reproducible validation run that combines:

1. focused backend tests
2. batched receipt reparses
3. a root markdown report we can compare across implementation iterations

## Inputs

Treat the active user prompt as the scope definition.

Extract and honor:

- target profile (`dev`, `test`, `prod`) — default `dev`
- target OCR workflow (`mistral` or `llamaparse`)
- batch size — default `20`
- whether to run the whole corpus or selected batches only
- whether to reset receipts before OCR rerun
- whether to preserve existing report content or rewrite it
- any specific receipts, stores, or regression examples to prioritize

## Hard rules

- Follow `AGENTS.md` and `.github/copilot-instructions.md`.
- Use `postgres-mcp` for agent-driven DB inspection and summaries.
- Save test output once under `tmp/`.
- Do not use ad-hoc one-off `tmp/` runners when the reusable scripts in `scripts/bb/expenses/` are sufficient.
- Prefer a direct Clojure MCP eval path when available for one-off inspection, but use the repo scripts for repeatable batch execution.
- When the OCR workflow is `llamaparse`, load `.env` before running the repo scripts so the API key is available in the shell environment.
- LlamaParse reads its API key from `LLAMA_CLOUD_API_KEY` (preferred) or `LLAMAPARSE_API_KEY` (fallback).

## Recommended workflow

### 1. Focused backend validation first

Run the smallest useful backend suite for receipt OCR extraction changes and save the output once.

Typical starting point from this repository:

- `app.domain.backend.expenses.integrations.llamaparse-test`
- `app.domain.backend.expenses.workers.receipt-ocr-extraction.reconciliation-test`
- `app.domain.backend.expenses.workers.receipt-ocr-extraction.items-test`
- `app.domain.expenses.services.expenses-test`

Example pattern:

- `mkdir -p tmp && clj -M:test -m kaocha.runner --focus ... :app.backend 2>&1 | tee tmp/receipt-ocr-extract-focused-tests.txt`

### 2. Create or refresh the batch plan artifact

Use the reusable batch planner script:

- `clj -M scripts/bb/expenses/receipt_ocr_extract_batches.clj dev --status posted --status extracted --batch-size 20 --output tmp/receipt-ocr-batches.edn | tee tmp/receipt-ocr-batches.txt`

Adjust:

- profile
- statuses
- batch size
- limit/offset

If the user requested only a subset, narrow the run here instead of reparsing everything.

### 3. Reparse one batch at a time

Use the reusable batch reparse script.

Example pattern:

- `export RECEIPT_OCR_UI_MAX_CONCURRENT=6`
- `set -a && source .env && set +a`
- `clj -M scripts/bb/expenses/receipt_ocr_reparse_batch.clj dev --batch-label batch-1 --batches-file tmp/receipt-ocr-batches.edn --batch-number 1 --max-concurrent 6 2>&1 | tee tmp/receipt-ocr-batch-1.txt`

Important notes:

- Keep concurrency bounded.
- Reuse the repo script rather than rebuilding the runner inline.
- For `llamaparse`, confirm `.env` exports `LLAMA_CLOUD_API_KEY` before concluding the provider is misconfigured.
- If the user only wants inspection and the client exposes a direct Clojure MCP eval tool, you may inspect state via eval — but the final batch execution should still use the script so the run is reproducible.

### 4. Update the root markdown report after every batch

Use or create:

- `receipt-ocr-batch-testing-results.md`

For every batch, record:

- exact batch number
- receipt IDs
- artifact path under `tmp/`
- duration
- result summary (`:ok`, failures, skips)
- posted/extracted counts after the run
- structured merge counts (`matched-count`, `repaired-count`, `filled-count`)
- refine activity if it happened
- concise findings / anomalies worth investigating

### 5. End-state summary

After all requested batches finish, summarize:

- total receipts processed
- final receipt status counts
- total structured merge coverage
- whether real data exercised the repair path or only the matching path
- the next likely follow-up if a regression still is not represented in real data

## Scripts to use

- Batch planner: `scripts/bb/expenses/receipt_ocr_extract_batches.clj`
- Batch reparse: `scripts/bb/expenses/receipt_ocr_reparse_batch.clj`

## Preferred artifact set

- `tmp/receipt-ocr-extract-focused-tests.txt`
- `tmp/receipt-ocr-batches.edn`
- `tmp/receipt-ocr-batches.txt`
- `tmp/receipt-ocr-batch-<N>.txt`
- `receipt-ocr-batch-testing-results.md`

## Completion gate

Do not stop at test execution alone.

A complete run means:

1. focused validation executed and saved
2. the requested batches reparsed
3. the root markdown report updated batch-by-batch
4. final DB summary checked
5. anomalies and next steps captured clearly
