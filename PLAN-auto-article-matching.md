# Automatic Article Matching on Expense Creation

## Goal
To automatically link **Expense Items** to canonical **Articles** at the moment of expense creation/upload.
Currently, when an expense is created, items are saved with `article_id = nil` unless explicitly provided. This change will make the system "smart" by checking if we already know what "API GALA APPLES" means for "Walmart".

## Product Principles (for this feature)
- **Predictable beats clever**: for non-technical users, wrong auto-matches are worse than no match.
- **No surprises**: never override a user-provided `article_id`.
- **Low-friction learning loop**: if we can't match, the item should cleanly land in the "Unmapped Items" queue so users can map once and benefit forever.

## Decisions (answers to open questions)
1. **Matching strategy (v1)**: **exact alias match only** (supplier-scoped + normalized label equality). No fuzzy matching or “best guess” in v1.
    - Rationale: avoids incorrect matches and keeps behavior explainable.
2. **Which label to match on**: use the stored `expense_items.raw_label` (as received from upload/OCR) and the existing normalization (`normalize-alias-label`).
    - Rationale: this is the label the user sees in the unmapped queue and can map/edit.
3. **Blank/too-short labels**: if `raw_label` is blank/whitespace or extremely short (e.g. <2 characters after trim), **skip auto-link** and keep `article_id = nil`.
    - Rationale: prevents accidental matches and keeps the unmapped queue sane.
4. **Where to apply auto-linking**:
    - **v1**: only on `create-expense!`.
    - **follow-up**: optionally apply the same logic to `update-expense!` *only for newly inserted items* (never for existing items that already have an article).
5. **Confidence**: in v1, **ignore** alias confidence (any alias row links). If we later introduce fuzzy matching, confidence becomes meaningful and must be explained in UI.

## Implementation Status

**Started**: 2026-01-07

- [x] Implement auto-linking inside `expenses/create-expense!`
- [x] Add focused integration tests (`expenses-services-test`)
- [x] Run focused Kaocha test namespace and confirm green

### Progress Log
- 2026-01-07: Plan updated with decisions + acceptance criteria (no code changes yet).
- 2026-01-07: Implemented auto-linking in `create-expense!` (supplier-scoped alias lookup, skip blank/short labels, per-request cache).
- 2026-01-07: Added integration tests + verified via focused Kaocha run (`16 tests, 59 assertions, 0 failures`).
- 2026-01-07: Follow-up: implemented auto-linking for `update-expense!` **only for newly inserted items** + verified via focused Kaocha run (`17 tests, 63 assertions, 0 failures`).

## Proposed Changes

### Expenses Service
`src/app/domain/backend/expenses/services/expenses.clj`

1.  **Add Dependency**: Require `app.domain.backend.expenses.services.articles`.
2.  **Enhance `create-expense!`**:
    *   Use the existing required `supplier_id` from the expense payload.
    *   Iterate over the input `items`.
    *   For any item missing an `article_id`, perform a lookup using `articles/find-article-by-alias`.
    *   If a match is found, inject the `article_id`.
    *   If `raw_label` is blank/too short, skip matching.

```clojure
;; Pseudo-code logic to be added
(defn- attempt-link-article [tx supplier-id item]
  (if (:article_id item)
    item
        (if-let [article (articles/find-article-by-alias tx supplier-id (:raw_label item))]
             ;; NOTE: find-article-by-alias returns the joined article row; use (:id article).
             (assoc item :article_id (:id article))
       ;; MATCH NOT FOUND CASE:
       ;; Leave article_id as nil.
       ;; This allows the item to appear in the "Unmapped Items" queue
       ;; for manual review later.
       item)))
```

### Logic Flow
1. **Match Found**: Item is saved with `article_id` populated. Cost history is updated.
2. **Match Not Found**: Item is saved with `article_id = nil`.
   - These items will appear in the `list-unmapped-items` query.
   - User can manually map them later via the UI, which will create the alias for next time.

### Acceptance Criteria (definition of done)
- When `article_id` is provided in the request item, it is preserved (never overridden).
- When `article_id` is missing and an alias exists for `(supplier_id, normalize(raw_label))`, the item is created with the alias’ `article_id`.
- Matching is **supplier-scoped** (aliases from other suppliers must not match).
- When no alias exists (or label is blank/too short), `article_id` remains `nil`.
- The create flow stays atomic (expense + items are created in one transaction).

### Performance Notes (keep UX snappy)
- Avoid one DB query per item when possible.
    - v1 can be a simple per-item lookup (fine for small item counts).
    - Prefer a cheap in-request cache: dedupe repeated labels within the same expense.
    - Follow-up: batch lookup aliases for all distinct normalized labels for the supplier.

### Where this helps immediately
- Expenses created from the **receipt approval** workflow should benefit automatically as well, because they ultimately create expense items with `raw_label` values.

### Workflow Context (Addressing OCR Errors)
The user asked: *"User should be able to create the new article and modify the alias since it can have the OCr errors."*

**We Agreement**: Yes. The workflow for unmapped items (which this auto-linker skips) is:
1.  **Review**: User sees an unmapped item (e.g., `raw_label` = "GALA APPLES @# (OCR ERR)").
2.  **Modify Alias (Optional)**: User can choose to define the alias as "GALA APPLES @# (OCR ERR)" (to catch this exact error again) or clean it up if they prefer.
3.  **Link**: User connects it to the "Gala Apples" Article.
4.  **Future**: If the same `raw_label` appears again, this auto-linker will catch it.

*Note: This task only implements the **Auto-Linker**. The UI/Backend for the "Review & Map" workflow is a separate concern, but this auto-linker relies on the data it produces.*

## Verification Plan

### Manual Verification
1.  **Setup**:
    *   Ensure an Article exists (e.g., "Gala Apples").
    *   Ensure a Supplier exists (e.g., "Walmart").
    *   Create an Alias: "APPLES G" @ "Walmart" -> "Gala Apples".
2.  **Test**:
    *   Call `create-expense!` with Supplier "Walmart" and Item "APPLES G".
3.  **Assert**:
    *   Fetch the new expense item.
    *   Verify `article_id` is **not nil** and matches "Gala Apples".

### Automated Tests (recommended)
Add a small, focused backend test suite (no external calls):
- Links when alias exists.
- Does not link when alias does not exist.
- Does not link across suppliers.
- Preserves explicitly provided `article_id`.
- Skips linking for blank/too-short labels.
