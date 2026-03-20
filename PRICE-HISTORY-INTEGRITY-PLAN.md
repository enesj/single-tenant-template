# Price History Integrity — Implementation Plan

> **Spec:** `specs/allium/domain/expenses/price-history-integrity.candidate.allium`
>
> **Rule:** Manually entered prices and user-modified OCR prices must NOT affect article price history (last-price suggestions).

## Background

Price history is **query-derived** — there is no `price_history` table. The function `quick-add-article-last-prices` in `handlers/search/quick_add.clj` uses a `ROW_NUMBER()` window over `expense_items` joined through `article_aliases` to find the most recent `unit_price` per article. This same function feeds both search-result price enrichment and co-occurring article prices.

### Current classification (implicit)

| Signal | Value | Meaning |
|---|---|---|
| `expenses.receipt_id` | `NULL` | Manual entry |
| `expenses.receipt_id` | UUID | OCR-originated |

### Missing signal

No per-item tracking of whether the user modified the OCR-extracted price at review/approval time.

---

## Step 1: Database Migration

**Add `price_modified` column to `expense_items`.**

### 1a. Update domain model

**File:** `resources/db/domain/models.edn`

Add to `:expense_items :fields`:
```clojure
[:price_modified :boolean {:null false :default false}]
```

### 1b. Generate & apply migration

```bash
# Generate migration SQL
bb gen-migration

# Apply to dev + test
clj -X:migrations
clj -X:migrations-test
```

**Expected SQL:**
```sql
ALTER TABLE expense_items
  ADD COLUMN price_modified BOOLEAN NOT NULL DEFAULT false;
```

All existing rows get `false` — conservative default (existing OCR items treated as unmodified; manual items excluded by receipt_id check anyway).

---

## Step 2: Backend — Set `price_modified` on Receipt Approval

**File:** `src/app/domain/backend/expenses/services/receipts/approval.clj`

When `approve-and-post!` or `approve-and-post-for-user!` creates expense items from a receipt, compare each item's `unit_price` against the OCR-extracted price from the receipt's `raw_extract_json`.

### 2a. Extract OCR prices from receipt

Parse `raw_extract_json` → extraction → items to get the original OCR unit prices. The receipt already stores this data.

```clojure
(defn- ocr-item-prices
  "Extract original OCR unit prices from receipt extraction, indexed by position."
  [receipt]
  (some->> receipt
    :raw_extract_json
    ;; or however extraction items are accessed
    parse-extraction-items
    (mapv :unit_price)))
```

### 2b. Compare and flag at item creation

For each expense item being created:
- Look up the corresponding OCR item by index
- If `approved-price != ocr-price` → `price_modified = true`
- If item was added by user (no OCR counterpart) → `price_modified = true`
- If auto-posted (no user review) → `price_modified = false` (all items)

```clojure
(defn- compute-price-modified
  [approved-item-idx approved-unit-price ocr-prices]
  (let [ocr-price (get ocr-prices approved-item-idx)]
    (or (nil? ocr-price)                          ;; user-added item
        (not= (bigdec approved-unit-price)
              (bigdec ocr-price)))))               ;; price changed
```

### 2c. Thread `price_modified` into item INSERT

The expense item creation query needs to include `price_modified` in the INSERT. Find the function that builds the item insert maps and add the flag.

### Files to modify:
- `src/app/domain/backend/expenses/services/receipts/approval.clj` — approval flow
- `src/app/domain/backend/expenses/services/expenses.clj` — if item INSERT is centralized here
- Auto-post path in `approval.clj` — ensure `price_modified = false` explicitly

---

## Step 3: Backend — Filter Price History Query

**File:** `src/app/domain/backend/expenses/handlers/search/quick_add.clj`

### 3a. Modify `quick-add-article-last-prices`

Add two WHERE conditions to the existing SQL:

```clojure
;; In the :where clause of the subquery:
[:and
  [:= :e.tenant_id tenant-id]
  [:in :aa.article_id article-ids]
  [:is-not :e.receipt_id nil]          ;; NEW: exclude manual expenses
  [:= :ei.price_modified false]]       ;; NEW: exclude user-modified prices
```

This single change filters both:
1. **Search result price enrichment** (`quick-add-search-articles` calls this)
2. **Co-occurring article price enrichment** (`cooccurring-articles` calls this)

### 3b. No changes needed elsewhere

The `cooccurring-articles` co-occurrence query itself (which articles appear together) should still consider ALL expenses — only the price enrichment step is filtered. This is already the case since co-occurrence uses a separate query and only calls `quick-add-article-last-prices` for price data.

---

## Step 4: Manual Entry — No Changes Needed

Manual expenses already have `receipt_id = NULL`. The query filter `e.receipt_id IS NOT NULL` excludes all their items. The `price_modified` column defaults to `false` and is irrelevant for manual entries.

---

## Step 5: Verification

### 5a. REPL verification

```clojure
;; 1. Check a manual expense's items are excluded
;;    (receipt_id IS NULL on parent expense)
(quick-add-article-last-prices db [article-uuid] tenant-id nil)
;; Should NOT include prices from manual expenses

;; 2. Check OCR expense with unmodified prices IS included
;; 3. Check OCR expense with modified prices IS excluded
```

### 5b. Test cases

| Scenario | `receipt_id` | `price_modified` | In price history? |
|---|---|---|---|
| OCR, price unchanged | UUID | `false` | Yes |
| OCR, user changed price | UUID | `true` | No |
| OCR, user added new item | UUID | `true` | No |
| OCR, auto-posted | UUID | `false` | Yes |
| Manual entry | `NULL` | `false` (default) | No |

### 5c. UI verification

1. Create a manual expense with article X at price 99.99
2. Search for article X in a new expense
3. Verify: the suggested price should NOT be 99.99 (should be from most recent OCR expense, or no price)

---

## File Summary

| File | Change |
|---|---|
| `resources/db/domain/models.edn` | Add `price_modified` column to `expense_items` |
| `resources/db/migrations/NNNN_*.sql` | Generated migration |
| `src/app/domain/backend/expenses/services/receipts/approval.clj` | Compare OCR vs approved prices, set `price_modified` |
| `src/app/domain/backend/expenses/services/expenses.clj` | Thread `price_modified` through item INSERT (if centralized) |
| `src/app/domain/backend/expenses/handlers/search/quick_add.clj` | Add WHERE filters to `quick-add-article-last-prices` |

## Risks & Considerations

- **Positional matching**: Item comparison is by index (position). The review UI preserves ordering from OCR extraction. If reordering is ever added, matching logic must change to label-based or ID-based.
- **Decimal comparison**: Use `bigdec` for price comparison to avoid floating-point issues (e.g., `2.50` vs `2.5`).
- **Existing data**: All existing `expense_items` get `price_modified = false`. This means existing OCR items where the user DID modify the price will still be treated as unmodified. This is acceptable — we can't retroactively determine which prices were changed.
- **No backfill needed**: The `receipt_id IS NOT NULL` filter already handles the manual-entry exclusion for all historical data. Only OCR price-modification tracking is imprecise for historical items.
