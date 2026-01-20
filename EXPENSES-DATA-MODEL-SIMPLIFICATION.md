# Expenses Domain: Data Model Simplification Decision

**Date**: 2026-01-20
**Status**: Approved
**Context**: Household expense tracking application

## Executive Summary

This document outlines the decision to simplify the expenses domain data model by:
1. Eliminating the `raw_labels` table
2. Merging raw label storage into `article_aliases`
3. Removing the `article_id` foreign key from `expense_items`
4. Resolving articles dynamically through `article_aliases`

**Rationale**: The application is a household expense tracker, not an accounting system. Simplicity and ease of use take precedence over audit-grade data lineage and per-item historical correctness.

---

## Background: Current Data Model

### Current State

```
raw_labels
  - id (PK)
  - raw_label (TEXT)
  - normalized_key (VARCHAR)
  - created_at, updated_at

expense_items
  - id (PK)
  - expense_id (FK)
  - raw_label_id (FK → raw_labels)
  - article_id (FK → articles, nullable)
  - qty, unit_price, line_total
  - deleted_at, created_at

article_aliases
  - id (PK)
  - supplier_id (FK → suppliers)
  - raw_label_normalized (VARCHAR)
  - article_id (FK → articles)
  - confidence
  - created_at
  - UNIQUE(supplier_id, raw_label_normalized)
```

### Problems with Current Model

1. **Three-table label flow**: `raw_labels` → `expense_items` → `article_aliases` → `articles`
2. **Redundant article reference**: `expense_items.article_id` duplicates what `article_aliases` provides
3. **Unnecessary complexity**: Household users don't need per-item article overrides
4. **Maintenance overhead**: More tables = more code, more potential bugs

---

## Decision: Simplified Data Model

### New Schema Structure

```
expense_items
  - id (PK)
  - expense_id (FK → expenses)
  - alias_id (FK → article_aliases, NOT NULL)
  - qty (NUMERIC)
  - unit_price (NUMERIC)
  - line_total (NUMERIC)
  - deleted_at (TIMESTAMPTZ)
  - created_at (TIMESTAMPTZ)

article_aliases (unified)
  - id (PK)
  - supplier_id (FK → suppliers, NOT NULL)  ← Uses "Unknown Supplier" record if unknown
  - raw_label (TEXT, NOT NULL)              ← NEW: stores a representative (latest-seen) raw label
  - raw_label_normalized (VARCHAR, NOT NULL)
  - article_id (FK → articles, nullable)    ← NULL = unmapped
  - created_at (TIMESTAMPTZ)
  - UNIQUE(supplier_id, raw_label_normalized)

articles (unchanged)
  - id (PK)
  - canonical_name (VARCHAR)
  - normalized_key (VARCHAR)
  - category (VARCHAR)
  - created_at, updated_at
```

### What Changed

| Change | Description |
|--------|-------------|
| **Remove `raw_labels` table** | Raw label text stored directly in `article_aliases` |
| **Add `raw_label` TEXT to `article_aliases`** | Stores representative (latest-seen) OCR/POS text for display/debug |
| **Remove `expense_items.article_id`** | Article resolved via `alias_id.article_id` |
| **Replace `expense_items.raw_label_id` with `alias_id`** | Clearer semantics |
| **Keep `article_aliases.article_id` nullable** | NULL indicates unmapped items |
| **Drop `article_aliases.confidence`** | Not needed; mapping is deterministic by `(supplier, normalized_label)` |

---

## Workflow: Receipt Upload to Expense

### Step 1: Upload and Extract

- Total amount from receipt header
- Line items: raw_label, quantity, unit_price, line_total
- Supplier name (guessed from receipt)

Receipt status: `uploaded` → `extracted` (or `review_required` if totals mismatch)

### Step 2: Validation

Calculate sum of line totals and compare to header total:

```
IF abs(total_amount_guess - sum(line_totals)) <= 0.01
  → status = 'extracted' (ready to save)
ELSE
  → status = 'review_required' (user intervention needed)
```

### Step 3: Auto-Create Aliases

For each extracted line item:

1. Normalize the raw_label (trim, lowercase, remove punctuation, collapse whitespace)
2. Look up existing alias: `WHERE supplier_id = ? AND raw_label_normalized = ?`
3. **If found**: Use existing alias_id
4. **If not found**: CREATE new alias with:
   - `supplier_id` = detected supplier (or "Unknown Supplier" ID if null)
   - `raw_label` = extracted raw text (stored as latest-seen representative)
   - `raw_label_normalized` = normalized version
   - `article_id` = NULL (unmapped)

Store `expense_items.alias_id` for each item

### Step 4: Admin Maps Unmapped Aliases

Admin/owner users review unmapped aliases:

```
SELECT * FROM article_aliases WHERE article_id IS NULL;
```

For each unmapped alias:
- Map to existing article, OR
- Create new article and map to it

Update: `UPDATE article_aliases SET article_id = ? WHERE id = ?`

**Result**: All past and future expenses using this alias now link to the article

---

## API Contract (Decision)

- Expense creation/update APIs accept `raw_label` strings for line items (not `alias_id`).
- Backend resolves/creates the `(supplier_id, raw_label_normalized)` alias and persists `expense_items.alias_id`.
- Admin mapping APIs operate on `article_aliases` by `id` (map alias → article).

---

## Supplier Handling (No Change Required)

The existing `suppliers` table already handles supplier name normalization:

```
suppliers
  - id (PK)
  - display_name (VARCHAR)
  - normalized_key (VARCHAR)
  - address (TEXT)
  - archived_at (TIMESTAMPTZ)
  - created_at, updated_at
```

**No `supplier_aliases` table needed** because:
- `suppliers.normalized_key` provides deduplication
- OCR supplier guess is normalized and matched against existing suppliers
- New suppliers created automatically if not found

---

## Query Patterns

### Get expense items with article names

```
SELECT
  ei.*,
  aa.raw_label,
  a.canonical_name AS article_name
FROM expense_items ei
JOIN article_aliases aa ON ei.alias_id = aa.id
LEFT JOIN articles a ON aa.article_id = a.id;
```

### Find unmapped items for admin review

```
SELECT
  aa.id,
  aa.raw_label,
  s.display_name AS supplier,
  COUNT(ei.id) AS occurrence_count
FROM article_aliases aa
LEFT JOIN suppliers s ON aa.supplier_id = s.id
LEFT JOIN expense_items ei ON ei.alias_id = aa.id
WHERE aa.article_id IS NULL
GROUP BY aa.id, s.display_name
ORDER BY occurrence_count DESC;
```

### Auto-link article during expense creation

```
-- Given: supplier_id (or unknown_supplier_id), raw_label
-- Look up or create alias
INSERT INTO article_aliases (supplier_id, raw_label, raw_label_normalized, article_id)
VALUES (?, ?, normalize(?), NULL)
ON CONFLICT (supplier_id, raw_label_normalized) DO UPDATE SET raw_label = EXCLUDED.raw_label
RETURNING id, article_id;
```

---

## Migration Strategy

### Database Migration Steps

1. **Add `raw_label` column to `article_aliases`**
   - Type: TEXT (temporarily nullable; final schema makes it NOT NULL)
   - Backfill from existing `raw_labels` table

2. **Update `expense_items` table**
   - Add `alias_id` (temporarily nullable; FK → `article_aliases`)
   - Backfill `alias_id` during Phase 2 (from existing `raw_label_id` + supplier)
   - Remove legacy columns: `raw_label_id` and `article_id`
   - Ensure `alias_id` is NOT NULL (final schema)

3. **Migrate data from `raw_labels` and backfill alias mappings**
   - **Prerequisite**: Ensure a single supplier exists with `normalized_key = 'unknown-supplier'` (migration can upsert this using a fixed UUID, but always uses `normalized_key` for lookup)
   - Iterate through existing `expense_items` (joining `expenses` to get `supplier_id`)
   - For each item:
     - Determine effective `supplier_id` (use "Unknown Supplier" ID if expense has no supplier)
     - Normalize the raw label from the linked `raw_labels` record
     - **Upsert** into `article_aliases` using `(supplier_id, raw_label_normalized)`
     - Update `expense_items.alias_id` to the resulting `article_aliases.id`
   - If existing `expense_items.article_id` contains historical mappings:
     - If a `(supplier_id, raw_label_normalized)` group maps to exactly one non-null `article_id`, backfill `article_aliases.article_id`
     - If a group maps to multiple distinct non-null `article_id` values, **record a conflict** and leave `article_aliases.article_id = NULL` for manual review

4. **Update unique constraint on `article_aliases`**
   - `supplier_id` is now NOT NULL
   - Constraint `UNIQUE(supplier_id, raw_label_normalized)` remains valid

5. **Drop legacy fields**
   - Drop `article_aliases.confidence`
   - Drop `raw_labels` table (no longer needed after migration)

### Data Integrity Notes

- All existing `expense_items` must have a valid `alias_id` after migration
- Unmapped items will have `article_id = NULL` in `article_aliases`
- Expense item label display comes from the joined alias (`article_aliases.raw_label`, stored as latest-seen representative text)

---

## Benefits of Simplified Model

| Benefit | Explanation |
|---------|-------------|
| **Fewer tables** | One less table to maintain (`raw_labels` eliminated) |
| **Simpler code** | Single source of truth for label semantics |
| **Clearer semantics** | `alias_id` explicitly points to the mapping record |
| **Zero-friction onboarding** | Users can upload receipts immediately without pre-configuring articles |
| **Learn-as-you-go** | System builds article intelligence from real receipts |
| **Easier maintenance** | Less ORM code, fewer queries, simpler mental model |

---

## Trade-offs and Limitations

### Accepted Trade-offs

| Limitation | Impact | Mitigation |
|------------|--------|------------|
| No per-item article overrides | All items with same (supplier, label) map to same article | Acceptable for household use; edge cases rare |
| Article lookups require JOIN | Two-hop query (items → aliases → articles) | Negligible performance impact with proper indexing |
| Alias updates affect all expenses | Changing alias.article_id updates historical expenses | Usually desired behavior; keeps data consistent |

### Out of Scope

The following features are explicitly NOT supported:
- Per-item article overrides (same label = different article for specific expenses)
- Historical snapshot of article assignments at time of expense creation
- Multiple article mappings for the same label at the same supplier

**Rationale**: These are enterprise requirements not needed for household expense tracking.

---

## User Roles and Permissions

### Household Members (member, viewer)
- Upload receipts
- View expenses with raw labels
- See article names if mapped

### Admin/Owner Users
- Access unmapped aliases queue
- Map aliases to articles (bulk operations)
- Create new articles
- Manage supplier records

---

## Related Decisions

### Supplier Aliases Considered and Rejected

**Proposal**: Create `supplier_aliases` table for supplier name matching.

**Decision**: Not needed. The existing `suppliers` table with `normalized_key` handles supplier deduplication. OCR supplier guesses are normalized and matched against existing suppliers.

### Progressive Complexity Considered

**Proposal**: Basic mode (totals only) vs Advanced mode (line items).

**Status**: Deferred for future consideration. Current implementation always extracts line items, but UI can emphasize totals for casual users.

---

## Index Requirements

Ensure these indexes exist for performance:

```
article_aliases
  - UNIQUE(supplier_id, raw_label_normalized)  ← Primary lookup
  - INDEX(article_id)                           ← Find all aliases for an article
  - INDEX(raw_label_normalized)                 ← Search by label

expense_items
  - INDEX(alias_id)                             ← Join to article_aliases
  - INDEX(expense_id)                           ← Items per expense
```

---

## Success Criteria

The simplified data model is successful when:

1. Users can upload receipts and see extracted data immediately
2. Admin users can efficiently map unmapped aliases in bulk
3. Query performance for expense display remains acceptable
4. Code maintenance is reduced (fewer files, simpler queries)
5. New feature development is faster (less model complexity)

---

## Appendix: Terminology

| Term | Definition |
|------|------------|
| **Raw label** | Original text extracted from receipt by OCR (e.g., "MILK 1L") |
| **Normalized label** | Canonical form used for matching (e.g., "milk 1l") |
| **Alias** | Mapping record connecting (supplier, raw_label) to article |
| **Unmapped** | Alias with `article_id = NULL` (not yet linked to article) |
| **Auto-linking** | Automatic article assignment during expense creation |
| **Review required** | Receipt status when header total ≠ sum of line totals |

---

## Document History

| Date | Change | Author |
|------|--------|--------|
| 2026-01-20 | Initial decision document | System |

---

## Codebase Changes Inventory

### Backend (clj)

**Removals**
- [ ] **Delete Service**: `app.domain.backend.expenses.services.raw-labels`
- [ ] **Delete Tests**: `test/app/domain/backend/expenses/services/raw_labels_test.clj`
- [ ] **Remove Routes**: Remove `raw-labels` routes from `app.domain.backend.expenses.routes.core` and `user_api.clj`
- [ ] **Registry**: Remove `raw-labels-service` from `app.domain.backend.registry`

**Updates**
- [ ] **Service**: `app.domain.backend.expenses.services.article-aliases`
    - Update create/update logic to accept and store `raw_label`
    - Remove any `confidence` field/logic
    - Implement "Unknown Supplier" fallback logic for creation
    - Ensure `supplier_id` is never nil (enforce schema change logic in code)
- [ ] **Service**: `app.domain.backend.expenses.services.expense-items`
    - Accept line items with `raw_label` in API-facing flows; resolve/create alias server-side and persist `alias_id`
    - Remove `article_id` handling from item creation/updates
    - Update list/fetch queries to JOIN `article_aliases` to get `raw_label`
- [ ] **Service**: `app.domain.backend.expenses.services.ocr-processing` (or equivalent ingestion logic)
    - Refactor "Step 3" flow: Instead of creating `raw_label` -> `expense_item`, now create/find `article_alias` -> `expense_item`
- [ ] **Specs**: Update domain models in `models.edn` to match new schema (remove invalid foreign keys)

### Frontend (cljs)

**Removals**
- [ ] **Events/Subs**: Remove legacy `raw-labels` subscriptions and events if present
- [ ] **Clean up**: Remove `raw-labels` from `entities.edn` and `table-columns.edn` configuration

**Updates**
- [ ] **Unmapped Items Page**:
    - Refactor from querying "expense items without articles" to "article aliases without articles"
    - Update the UI table to list unique aliases rather than individual expense occurrences
    - Update "Map to Article" action to update the `article_alias`
- [ ] **Expense Detail View**:
    - Update line item display to read label text from the nested/joined alias data
- [ ] **Expense Form**:
    - Keep sending `raw_label` strings for items; backend resolves/creates aliases and persists `alias_id` (no frontend `alias_id` required).

### Configurations & SQL
- [ ] **Migration**: Create standard SQL migration file implementing the steps defined in "Migration Strategy"
- [ ] **Conflict Report**: Persist ambiguous historical `article_id` mappings in `expense_alias_article_conflicts` for manual review
- [ ] **Seeds/Tests**: Update test data generators to respect new schema constraints (no NULL supplier_ids in aliases, always link items to aliases)

---

## Step-by-Step Migration Instructions

> [!IMPORTANT]  
> Do not attempt to apply all model changes in a single step using `automigrate`. It will likely attempt to drop the `raw_labels` table before we have migrated the data. Follow this 3-phase approach.

### Phase 1: Additive Schema Changes
**Goal**: Create the new columns needed for migration without breaking existing code.

1.  **Edit** `resources/db/domain/models.edn` (or appropriate source file):
    *   Update `article_aliases`: Add `[:raw_label :text]` (temporarily nullable).
    *   Update `article_aliases`: Drop `:null false` from `:article_id` (must allow `NULL` for unmapped aliases before Phase 2).
    *   Update `expense_items`: Add `[:alias_id :uuid {:foreign-key :article_aliases/id}]` (temporarily nullable).
    *   **Do not** remove `raw_labels` table or old columns yet.
2.  **Generate Migration**:
    *   Run REPL: `(require '[app.template.backend.migrations.simple-repl :as mig])`
    *   Run `(mig/make-all-migrations!)`
    *   Verify generated `.edn` file creates the new columns.
3.  **Apply**: `(mig/migrate!)`

### Phase 2: Data Backfill (Manual SQL)
**Goal**: Move data from `raw_labels` to `article_aliases` and link `expense_items`.

1.  **Create SQL Migration**:
    *   Run `(mig/make-migration! :type :empty-sql :name backfill_raw_labels_data)`
2.  **Edit generated SQL file** (`resources/db/migrations/NNNN_backfill...sql`):
    *   **FORWARD Section**:
        ```sql
        -- 0. Ensure Unknown Supplier exists (safe upsert on normalized_key)
        -- NOTE: suppliers.id has no DB default; use a fixed UUID for first insert, but always look up by normalized_key.
        INSERT INTO suppliers (id, display_name, normalized_key, created_at, updated_at)
        VALUES ('00000000-0000-0000-0000-000000000000', 'Unknown Supplier', 'unknown-supplier', NOW(), NOW())
        ON CONFLICT (normalized_key) DO UPDATE
          SET display_name = EXCLUDED.display_name,
              updated_at = EXCLUDED.updated_at;

        -- 1. Create a table to persist mapping conflicts for manual review
        CREATE TABLE IF NOT EXISTS expense_alias_article_conflicts (
          supplier_id uuid NOT NULL,
          raw_label_normalized text NOT NULL,
          article_ids uuid[] NOT NULL,
          item_count bigint NOT NULL,
          created_at timestamptz NOT NULL DEFAULT NOW(),
          UNIQUE (supplier_id, raw_label_normalized)
        );

        -- 2. Upsert Article Aliases from Expense Items
        -- We join expense_items -> raw_labels AND expense_items -> expenses (for supplier)
        -- We also attempt to backfill article_id where unambiguous, and record conflicts.
        WITH unknown_supplier AS (
          SELECT id
          FROM suppliers
          WHERE normalized_key = 'unknown-supplier'
        ),
        item_rows AS (
          SELECT
            COALESCE(e.supplier_id, (SELECT id FROM unknown_supplier)) AS supplier_id,
            rl.raw_label AS raw_label,
            rl.normalized_key AS raw_label_normalized,
            ei.article_id AS existing_article_id
          FROM expense_items ei
          JOIN raw_labels rl ON ei.raw_label_id = rl.id
          JOIN expenses e ON ei.expense_id = e.id
          -- NOTE: include soft-deleted items so alias_id backfill covers all rows (Phase 3 makes alias_id NOT NULL)
        ),
        grouped AS (
          SELECT
            supplier_id,
            raw_label_normalized,
            MAX(raw_label) AS raw_label_sample,
            ARRAY_AGG(DISTINCT existing_article_id) FILTER (WHERE existing_article_id IS NOT NULL) AS article_ids,
            COUNT(*) AS item_count
          FROM item_rows
          GROUP BY supplier_id, raw_label_normalized
        ),
        conflicts AS (
          INSERT INTO expense_alias_article_conflicts (supplier_id, raw_label_normalized, article_ids, item_count)
          SELECT supplier_id, raw_label_normalized, article_ids, item_count
          FROM grouped
          WHERE COALESCE(array_length(article_ids, 1), 0) > 1
          ON CONFLICT DO NOTHING
          RETURNING 1
        )
        INSERT INTO article_aliases (supplier_id, raw_label, raw_label_normalized, article_id, created_at)
        SELECT
          supplier_id,
          raw_label_sample,
          raw_label_normalized,
          CASE WHEN array_length(article_ids, 1) = 1 THEN article_ids[1] ELSE NULL END AS article_id,
          NOW()
        FROM grouped
        ON CONFLICT (supplier_id, raw_label_normalized)
        DO UPDATE SET raw_label = EXCLUDED.raw_label; -- Keep latest-seen representative label

        -- 3. Link Expense Items to Aliases
        WITH unknown_supplier AS (
          SELECT id
          FROM suppliers
          WHERE normalized_key = 'unknown-supplier'
        )
        UPDATE expense_items ei
        SET alias_id = aa.id
        FROM raw_labels rl, expenses e, article_aliases aa, unknown_supplier us
        WHERE ei.raw_label_id = rl.id
          AND ei.expense_id = e.id
          AND aa.raw_label_normalized = rl.normalized_key
          AND aa.supplier_id = COALESCE(e.supplier_id, us.id);
          
        -- Review conflicts (manual follow-up):
        -- SELECT * FROM expense_alias_article_conflicts ORDER BY item_count DESC;

        -- 4. Catch-all for any items that might have missed the join (optional safety check)
        -- ...
        ```
3.  **Apply**: `(mig/migrate!)`
4.  **Verify**:
    *   Confirm all items were linked: `SELECT COUNT(*) FROM expense_items WHERE alias_id IS NULL;` (should be 0)
    *   Review any conflicts: `SELECT * FROM expense_alias_article_conflicts ORDER BY item_count DESC;`

### Phase 3: Finalize Schema (Destructive)
**Goal**: Enforce constraints and remove legacy tables.

1.  **Edit** `resources/db/domain/models.edn`:
    *   **Update** `article_aliases`: Set `[:raw_label ... {:null false}]` and `[:supplier_id ... {:null false}]`.
    *   **Update** `expense_items`: Set `[:alias_id ... {:null false}]`.
    *   **Remove** `expense_items`: `raw_label_id` and `article_id` columns.
    *   **Remove** `raw_labels` table definition entirely.
    *   **Remove** `article_aliases`: `confidence` column.
2.  **Generate Migration**:
    *   Run `(mig/make-all-migrations!)`
    *   Verify generated `.edn` file attempts to drop columns/tables and add NOT NULL constraints.
3.  **Apply**: `(mig/migrate!)`
