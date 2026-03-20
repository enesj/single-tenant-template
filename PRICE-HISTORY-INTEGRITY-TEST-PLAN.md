# Price History Integrity — Chrome DevTools Test Plan

## Summary
Validate that article last-price suggestions only use OCR-originated, unmodified expense items. Manual expenses (`receipt_id IS NULL`) and OCR receipts whose approved item price differs from the OCR extraction (`price_modified = true`) must not affect quick-add last-price suggestions.

## Implementation steps (ordered)

1. Goal: establish a baseline article with known eligible OCR price history.
File list: [PRICE-HISTORY-INTEGRITY-PLAN.md](/Users/enes/Projects/single-tenant-template/PRICE-HISTORY-INTEGRITY-PLAN.md), [quick_add.clj](/Users/enes/Projects/single-tenant-template/src/app/domain/backend/expenses/handlers/search/quick_add.clj), [approval.clj](/Users/enes/Projects/single-tenant-template/src/app/domain/backend/expenses/services/receipts/approval.clj)
Dependencies: live app at `http://localhost:8085`, authenticated user session in tenant `Jakic Family`, Chrome DevTools MCP, optional Postgres MCP for fixture inspection.
Owner: `Coder`

2. Goal: confirm the quick-add article search shows the latest eligible OCR price before introducing new records.
File list: [expenses_list.cljs](/Users/enes/Projects/single-tenant-template/src/app/domain/frontend/expenses/pages/user/expenses_list.cljs), [expense_new.cljs](/Users/enes/Projects/single-tenant-template/src/app/domain/frontend/expenses/pages/user/expense_new.cljs)
Dependencies: baseline article with an existing OCR-origin posted expense; article used in this run was `Ayran 2% 1kg`.
Owner: `Coder`

3. Goal: create a manual expense for the same article at a different price and verify that the suggested last price does not change.
File list: [expense_new.cljs](/Users/enes/Projects/single-tenant-template/src/app/domain/frontend/expenses/pages/user/expense_new.cljs), [quick_add.clj](/Users/enes/Projects/single-tenant-template/src/app/domain/backend/expenses/handlers/search/quick_add.clj)
Dependencies: quick-add add-expense modal, a valid payer, and at least one context selection (supplier/store/category) so the form can save.
Owner: `Coder`

4. Goal: create an extracted OCR receipt for the same article, change the approved unit price in the receipt detail approval UI, post it, and verify `price_modified = true` on the resulting expense item.
File list: [receipts_list.cljs](/Users/enes/Projects/single-tenant-template/src/app/domain/frontend/expenses/pages/user/receipts_list.cljs), [receipt_detail_modal.cljs](/Users/enes/Projects/single-tenant-template/src/app/domain/frontend/expenses/components/receipt_detail_modal.cljs), [forms.cljs](/Users/enes/Projects/single-tenant-template/src/app/domain/frontend/expenses/components/user_expense_form/forms.cljs), [approval.clj](/Users/enes/Projects/single-tenant-template/src/app/domain/backend/expenses/services/receipts/approval.clj)
Dependencies: extracted receipt visible in `/receipts`, valid supplier and payer selections, Chrome DevTools form interaction, Postgres verification of the created row.
Owner: `Coder`

5. Goal: re-run the quick-add article search after the modified OCR receipt is posted and verify the suggestion still resolves to the original eligible OCR price.
File list: [expense_new.cljs](/Users/enes/Projects/single-tenant-template/src/app/domain/frontend/expenses/pages/user/expense_new.cljs), [quick_add.clj](/Users/enes/Projects/single-tenant-template/src/app/domain/backend/expenses/handlers/search/quick_add.clj)
Dependencies: successful completion of steps 3 and 4.
Owner: `Coder`

## Edge cases

- Happy path: latest eligible OCR expense item (`receipt_id IS NOT NULL`, `price_modified = false`) is shown as the quick-add last price.
- `nil`: manual expenses with `receipt_id = NULL` must be ignored even when they are newer than the latest eligible OCR expense.
- Empty input/collection: article search with no matching history should return no last price instead of leaking a manual or modified OCR price from unrelated history.
- Invalid/boundary input: receipt approval line-item price edits must use valid step-aligned numeric values; browser number-input validation can block posting if the edited total is not representable at the input step.
- Boundary ordering: when newer manual and newer modified-OCR expenses exist for the same article, the suggestion must still fall back to the newest older eligible OCR price.

## Validation plan

- Primary validation: Chrome DevTools MCP end-to-end verification in the user UI.
- Supporting validation: Postgres MCP inspection of `expenses`, `receipts`, and `expense_items` to confirm `receipt_id` / `price_modified` state matches the UI scenario.
- Executed run:
  - Baseline article: `Ayran 2% 1kg`
  - Baseline eligible OCR last price shown in quick-add: `1.95`
  - Manual expense created at `7.77` with note `price history manual exclusion check`
  - Quick-add still showed `1.95` after manual save
  - Extracted OCR receipt `0a9c2dd1-15ed-4e66-a017-3f99995d2026` posted with approved unit price `10.00`
  - DB verification confirmed resulting expense item had `price_modified = true`
  - Quick-add still showed `1.95` after modified OCR posting

## Open questions / assumptions

- Assumption: using Postgres MCP for repeatable fixture setup is acceptable as support tooling even though the primary verification is browser-driven.
- Assumption: the target tenant remains `Jakic Family` and already contains the baseline OCR history for `Ayran 2% 1kg`.
- Open question: co-occurring article enrichment uses the same last-price helper, but that path was not explicitly exercised in this run.
- Open question: auto-posted OCR receipts (`price_modified = false` without human edits) were not re-verified in this browser pass because the baseline historical OCR expense already demonstrated inclusion behavior.
