# Plan: Price Consistency Check in Extraction Pipeline

## Problem

OCR extraction sometimes produces items where `unit_price × qty ≠ line_total`. The extraction model (LlamaParse → LLM) misparses the Bosnian `N,000x` quantity format, interpreting it as a price instead of a quantity.

### Real examples

| Receipt | Raw text | Correct parse | Extraction output |
|---------|----------|---------------|-------------------|
| IMG_3968.jpeg | `CIG DUNHILL ESSENCE BRONZE 3,000x 6,60 19,80E` | qty=3, price=6.60, total=19.80 | qty=1, price=3.00, total=19.80 |
| IMG_3890.HEIC | `Cig. Dunhill es 2,000x 6,40 12,80E` | qty=2, price=6.40, total=12.80 | qty=1, price=2.00, total=12.80 |

In both cases, `line_total` is correct but `unit_price` and `qty` are wrong. The structured table from LlamaParse has the correct 4-column parse, but the extraction model ignores it.

### Why existing checks don't catch this

1. **`lines-total-mismatch?`** (shape.clj:71) — checks `sum(line_totals) ≈ receipt total`. This passes because `line_total` per item is correct; only `unit_price` and `qty` are wrong.
2. **Markdown reconciliation** (reconcile.clj) — only fires when `parsed_markdown` is available. In these receipts, `markdown` is `null` (LlamaParse didn't produce one).
3. **`normalize-expense-item`** (expenses.clj:144) — only derives `unit_price` when it's `nil`. When the extraction provides a wrong-but-non-nil `unit_price`, it passes through unchecked.

---

## Fix Strategy

### Level 1: Post-processing consistency check (safety net)

**Where**: `extraction/items.clj` → `clean-extraction-items`, as a new repair step after filtering non-items.

**Logic**: For each item where all three values are present:
```
expected = unit_price × qty
actual   = line_total
```
If `|expected - actual| > 0.01` AND `qty > 0`:
- Recalculate: `unit_price = line_total / qty` (trust `line_total` as ground truth)
- Tag the item with `:price_repaired true` for audit trail in `post-processing`

**Why `line_total` is the trust anchor**:
- `lines-total-mismatch?` validates `sum(line_totals) ≈ receipt TOTAL` — so `line_total` has receipt-level confirmation
- The OCR error pattern is always in `unit_price`/`qty`, never in `line_total`

### Level 2: Leverage structured table rows from LlamaParse response

**Where**: `extraction.clj` → `persist-extract-result!`, before reconciliation.

**Logic**: When `response.items.pages[*].items` contains structured table rows (type="table" with `rows` array), parse them as an alternative item source — similar to how `markdown-items` are already used. Use `shape/prefer-markdown-items?`-style logic to prefer structured table items when they explain the receipt total better.

**Why this helps**: LlamaParse's structured table parser correctly identifies the 4-column layout (label, qty, unit_price, total) even when the extraction model doesn't. This is a stronger signal than the raw text.

### Level 3: Fallback derivation in `normalize-expense-item`

**Where**: `expenses.clj` → `normalize-expense-item`

**Logic**: Extend the existing derivation logic — currently only fires when `unit_price` is `nil`. Add a secondary check: if `unit_price` is present AND `qty > 0` AND `|unit_price × qty - line_total| > 0.01`, recalculate `unit_price = line_total / qty`.

This is a last-resort safety net at the DB insertion boundary.

---

## Implementation Order

### Step 1 — Level 1: Consistency repair in `clean-extraction-items`

**File**: `src/app/domain/backend/expenses/workers/receipt_ocr/extraction/items.clj`

1. Add a `repair-item-prices` function:
   ```clojure
   (defn- repair-item-prices
     "When unit_price × qty ≠ line_total, recalculate unit_price from line_total / qty.
     Returns [repaired-item, repaired?]."
     [item]
     ...)
   ```
2. Apply it in `clean-extraction-items` after the filtering reduce, mapping over the surviving items.
3. Track repair count in the `post-processing` map (`:price-repairs N`).

**Test file**: `test/app/domain/backend/expenses/workers/receipt_ocr/extraction/items_test.clj`
- Test: consistent item (6.60 × 2 = 13.20) → no change
- Test: inconsistent item (3.00 × 1 = 3.00 ≠ 19.80) → unit_price recalculated to 19.80
- Test: qty is nil → skip repair (let existing derivation handle it)
- Test: qty is zero → skip repair
- Test: unit_price is nil → skip repair (existing derivation handles it)
- Test: within tolerance (rounding diff ≤ 0.01) → no change

### Step 2 — Level 3: Safety net in `normalize-expense-item`

**File**: `src/app/domain/backend/expenses/services/expenses.clj`

1. After the existing `derived-unit-price` logic, add a consistency check:
   ```clojure
   ;; existing: derive when nil
   (when (nil? (:unit_price item*)) ...)
   ;; new: repair when inconsistent
   (when (and (:unit_price item*) (:qty item*) (:line_total item*)
              (inconsistent? ...)) ...)
   ```

**Test**: Add to existing expense normalization tests.

### Step 3 — Level 2: Structured table row extraction (separate PR, more complex)

**File**: `src/app/domain/backend/expenses/workers/receipt_ocr/extraction.clj`

1. Add a function to extract items from `response.items.pages[*].items` where `type = "table"`.
2. Parse the `rows` arrays using the same `parse-money` logic as `items_pipe.clj`.
3. Use as another candidate source alongside `markdown-items`.

This is more involved (needs to handle varying table column layouts) and can be a follow-up.

---

## Scope for this PR

**Steps 1 + 2 only** (the consistency checks). Step 3 (structured table extraction) is a separate enhancement.

## Validation

1. REPL: test `repair-item-prices` with the real receipt data from the DB
2. Unit tests: items_test.clj for the repair function
3. Re-process the 2 affected receipts to verify corrected prices
4. Run `bb be-test` to confirm no regressions
