# Allium Review Report — 2026-02-26

## 1) Allium review verdict

**misaligned**

## 2) Evidence

### Changed files reviewed (current-session scope from recent commits)

- `src/app/domain/backend/expenses/services/receipts/queries.clj`
- `src/app/domain/backend/expenses/handlers/user_receipts.clj`
- `src/app/domain/backend/expenses/routes/receipts.clj`
- `src/app/domain/backend/expenses/routes/core.clj`
- `src/app/domain/backend/expenses/routes/user_api.clj`
- `src/app/domain/frontend/expenses/events/user_expenses/receipts/list.cljs`
- `src/app/domain/frontend/expenses/pages/user/receipts_list.cljs`
- `src/app/domain/frontend/expenses/pages/user/expense_upload.cljs`
- `src/app/template/frontend/components/list/fields.cljs`
- `src/app/template/frontend/components/advanced_fields.cljs`

### Spec files consulted

- `specs/allium/domain/expenses/implementation.allium`
- `specs/allium/drafts/expenses/receipt-ocr.candidate.allium`
- `specs/allium/drafts/list-view-filtering.candidate.allium`
- `specs/allium/drafts/list-view-sort.candidate.allium`
- `specs/allium/README.md`

## 3) Precise mismatch list

1. **`effective_status` semantics diverge from receipt OCR candidate spec**
   - Spec:
     - `specs/allium/drafts/expenses/receipt-ocr.candidate.allium` (~line 409)
     - `receipt.effective_status = if receipt.status = extracted and not receipt.totals_match: review_required else: receipt.status`
   - Code now adds `refine_pending` exception:
     - `src/app/domain/backend/expenses/services/receipts/queries.clj` (~line 109):
       - `and not coalesce((raw_extract_json->>'refine_pending')::boolean, false)`
     - Same exception mirrored in:
       - `src/app/domain/backend/expenses/handlers/user_receipts.clj`
       - `src/app/domain/backend/expenses/routes/receipts.clj`
   - Effect: while `refine_pending=true`, extracted+totals-mismatch is not surfaced as `review_required`.

2. **Frontend introduces transient status `"refining"` not modeled in receipt status contract**
   - Spec enum currently:
     - `specs/allium/drafts/expenses/receipt-ocr.candidate.allium` (~line 44):
       - `uploaded | parsing | parsed | extracting | extracted | review_required | approved | posted | failed`
   - Code now injects synthetic UI status:
     - `src/app/domain/frontend/expenses/events/user_expenses/receipts/list.cljs` (~line 110):
       - `(assoc receipt :status "refining")`
   - UI rendering updated to expect `refining`:
     - `src/app/domain/frontend/expenses/pages/user/receipts_list.cljs`
     - `src/app/domain/frontend/expenses/pages/user/expense_upload.cljs`
     - `src/app/template/frontend/components/list/fields.cljs`
     - `src/app/template/frontend/components/advanced_fields.cljs`

3. **Admin receipt operations surface appears reduced vs candidate `AdminReceiptOperations`**
   - Candidate includes:
     - `RetryReceiptProcessing`
     - `MarkReceiptFailed`
     - `RunReceiptOCRSweep`
     - (see `specs/allium/drafts/expenses/receipt-ocr.candidate.allium`, ~lines 413+)
   - Current admin routes removed equivalents:
     - `src/app/domain/backend/expenses/routes/receipts.clj`
       - removed `/pending`
       - removed `/:id/retry`
       - removed `/:id/fail`
       - removed `/:id/ocr`

## 4) Recommended fix direction

- update `specs/allium/drafts/expenses/receipt-ocr.candidate.allium` to model:
  - `effective_status` guarded by `not refine_pending`
  - `refining` as presentation-derived state (or explicit derived field)
  - reduced admin operation surface (or mark as deferred/optional)
- If spec is intended unchanged: revert code to strict candidate semantics.

## 5) Residual risks

- `get_errors` reported one diagnostic:
  - `src/app/domain/backend/expenses/routes/receipts.clj` (~line 342): unused binding `app-config`
- Branch context mismatch at review time:
  - attachment mentioned `wk2`, repo state was on `allium`
- No additional behavior tests were run in this review pass.

## 6) Commit status

**not committed** — misalignment found, commit blocked by reviewer gate policy.
