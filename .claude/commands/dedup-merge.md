---
description: "Find and merge duplicate Articles, Suppliers, Stores, and Manufacturers with user approval"
---

# dedup-merge - Duplicate Detection & Merge Tool

Build an admin tool that finds potentially duplicated **Articles**, **Suppliers**, **Stores**, and **Manufacturers** and merges them into a single canonical record with user approval.

## Motivation

OCR ingestion and manual entry create near-duplicate canonical records over time (e.g. `"Coca Cola"` vs `"Coca-Cola"` vs `"COCA COLA 330ml"`). These dilute reporting accuracy and clutter dropdowns. A merge tool lets an admin consolidate duplicates while safely reassigning all FK references.

## Implementation phases

Execute phases in order. Each phase must be validated before proceeding to the next.

---

### Phase 1: PG extensions (if needed)

Check whether `fuzzystrmatch` and `pg_trgm` extensions are installed:

```sql
SELECT * FROM pg_extension WHERE extname IN ('fuzzystrmatch', 'pg_trgm');
```

If missing, add a migration using the `/migrations` skill:
- Edit `resources/db/shared/functions.edn` (or a new `resources/db/shared/extensions.edn` if the pattern exists) to add:
  ```sql
  CREATE EXTENSION IF NOT EXISTS fuzzystrmatch;
  CREATE EXTENSION IF NOT EXISTS pg_trgm;
  ```
- Apply to both dev and test DBs via `(mig/migrate!)` and `(mig/migrate! :test)`.

---

### Phase 2: Backend detection service

**Create** `src/app/domain/backend/expenses/services/duplicates.clj`

Implement `find-duplicate-clusters` with multi-method or cond dispatch on strategy.

#### Entity config map

```clojure
(def entity-configs
  {:suppliers      {:table "suppliers"      :name-col "display_name"  :key-col "normalized_key"}
   :articles       {:table "articles"       :name-col "canonical_name" :key-col "normalized_key"}
   :stores         {:table "stores"         :name-col "display_name"  :key-col "normalized_key"}
   :manufacturers  {:table "manufacturers"  :name-col "display_name"  :key-col "normalized_key"}})
```

#### Strategy 1: N-word prefix grouping (deterministic)

Group records by the first 1, 2, and 3 words of their name column. Any group with >= 2 members is a merge candidate cluster.

```sql
-- Example: group suppliers by first-2-words
SELECT
  array_to_string((string_to_array(lower(display_name), ' '))[1:2], ' ') AS prefix,
  array_agg(id) AS ids,
  array_agg(display_name) AS names,
  count(*) AS cnt
FROM suppliers
GROUP BY prefix
HAVING count(*) >= 2
ORDER BY cnt DESC;
```

Run for word counts 1, 2, 3 and union/deduplicate clusters that are subsets of larger ones.

#### Strategy 2: Normalized-key similarity (fuzzy)

Use the existing `normalized_key` column:

- **Trigram similarity**: `similarity(a.normalized_key, b.normalized_key) > 0.4` via `pg_trgm`.
- **Levenshtein distance**: `levenshtein(a.normalized_key, b.normalized_key) <= 2` for short keys, `<= 3` for longer ones via `fuzzystrmatch`.

```sql
-- Trigram example
SELECT a.id, a.display_name, b.id, b.display_name,
       similarity(a.normalized_key, b.normalized_key) AS sim
FROM suppliers a
JOIN suppliers b ON a.id < b.id
  AND similarity(a.normalized_key, b.normalized_key) > 0.4
ORDER BY sim DESC;
```

#### Output format

Each strategy returns clusters:

```clojure
{:entity-type :suppliers
 :strategy :prefix-2
 :clusters [{:ids ["uuid-1" "uuid-2"]
             :records [{:id "uuid-1" :display-name "Bingo" :normalized-key "bingo" :record-count 15}
                       {:id "uuid-2" :display-name "BINGO d.o.o." :normalized-key "bingo" :record-count 3}]
             :score 0.85}]
 :total-clusters 12}
```

`:record-count` = number of related FK references (expenses, aliases, etc.) to help pick the primary.

**Validate**: Run detection against live dev data via REPL for each entity type and strategy.

---

### Phase 3: Backend merge service

**Create** `src/app/domain/backend/expenses/services/merge.clj`

#### Merge semantics

Given a cluster, the user picks one record as **primary** (survivor). All others are **secondary** (absorbed). The merge must:

1. Reassign all FK references from each secondary to primary.
2. Merge aliases — move alias records; handle unique constraint conflicts by skipping duplicates.
3. Preserve the richest data — if primary has NULL fields that a secondary has filled, copy them over (e.g. `address`, `link`, `manufacturer_id`).
4. Delete secondaries after reassignment.
5. Wrap everything in `jdbc/with-transaction`.

#### Per-entity FK reassignment map

**Suppliers merge:**

| Table | Column | Action |
|-------|--------|--------|
| `expenses` | `supplier_id` | UPDATE to primary (**MUST happen before delete — ON DELETE RESTRICT**) |
| `stores` | `supplier_id` | UPDATE to primary (handle duplicate `normalized_key` per supplier) |
| `article_aliases` | `supplier_id` | UPDATE to primary (skip if `[supplier_id, raw_label_normalized]` unique conflict) |
| `supplier_aliases` | `supplier_id` | UPDATE to primary (skip if `raw_label_normalized` already exists) |
| `price_observations` | `supplier_id` | UPDATE to primary |
| secondary supplier | — | DELETE after all reassignments |

**Articles merge:**

| Table | Column | Action |
|-------|--------|--------|
| `expense_items` | `article_id` | UPDATE to primary |
| `article_aliases` | `article_id` | UPDATE to primary (skip unique conflicts on `[supplier_id, raw_label_normalized]`) |
| `price_observations` | `article_id` | UPDATE to primary |
| secondary article | — | DELETE |

**Stores merge:**

| Table | Column | Action |
|-------|--------|--------|
| `expenses` | `store_id` | UPDATE to primary |
| `store_aliases` | `store_id` | UPDATE to primary (skip unique conflicts on `raw_label_normalized`) |
| secondary store | — | DELETE |

**Manufacturers merge:**

| Table | Column | Action |
|-------|--------|--------|
| `articles` | `manufacturer_id` | UPDATE to primary |
| secondary manufacturer | — | DELETE |

#### Handling unique constraint conflicts

Use an exclude-then-delete pattern:

```sql
-- Move non-conflicting aliases to primary
UPDATE supplier_aliases
SET supplier_id = :primary-id
WHERE supplier_id = :secondary-id
  AND raw_label_normalized NOT IN (
    SELECT raw_label_normalized FROM supplier_aliases WHERE supplier_id = :primary-id
  );
-- Delete remaining (conflicting) aliases that still point to secondary
DELETE FROM supplier_aliases WHERE supplier_id = :secondary-id;
```

Same pattern for `article_aliases` (scoped by `[supplier_id, raw_label_normalized]`) and `store_aliases` (scoped by `raw_label_normalized`).

#### Functions to implement

- `merge-records! [db {:keys [entity-type primary-id secondary-ids]}]` — executes merge in transaction, returns `{:success true :merged-count N :reassigned {:expenses N :aliases N ...} :skipped-aliases N}`.
- `preview-merge [db {:keys [entity-type primary-id secondary-ids]}]` — returns impact counts without mutating.
- `enrich-record-counts [db entity-type record-ids]` — counts related records per candidate.

**Validate**: Test merge on a known duplicate pair in dev via REPL. Verify FK references moved correctly and secondaries deleted.

**Follow existing patterns**: Model after `batch-create-aliases!` in `src/app/domain/backend/expenses/services/supplier_aliases.clj` for conflict handling and structured result returns.

---

### Phase 4: Backend routes

**Create** `src/app/domain/backend/expenses/routes/duplicates.clj`

Endpoints:

```
GET  /admin/api/expenses/duplicates/:entity-type
     ?strategy=prefix-1|prefix-2|prefix-3|trigram|levenshtein
     &min-group-size=2
     &limit=50&offset=0
  -> {:clusters [...] :total-clusters N}

GET  /admin/api/expenses/duplicates/preview-merge
     ?entity-type=suppliers&primary-id=...&secondary-ids=...,...
  -> {:primary {...} :secondaries [...] :impact {:expenses 12 :aliases 5 ...}}

POST /admin/api/expenses/duplicates/merge
     Body: {:entity-type "suppliers" :primary-id "uuid" :secondary-ids ["uuid-2" "uuid-3"]}
  -> {:success true :merged-count 2 :reassigned {...}}
```

**Mount** in `src/app/domain/backend/expenses/routes/core.clj` alongside existing entity routes:

```clojure
(duplicates/routes db)  ;; add to the route vector
```

Follow the reitit handler patterns from the existing route factory in `routes_factory.clj`.

---

### Phase 5: Frontend re-frame events & subs

**Create** `src/app/domain/frontend/expenses/events/duplicates.cljs`

Events:
- `::load-clusters [entity-type strategy params]` — GET detection endpoint.
- `::load-preview [entity-type primary-id secondary-ids]` — GET preview endpoint.
- `::execute-merge [entity-type primary-id secondary-ids]` — POST merge endpoint.
- `::dismiss-cluster [cluster-idx]` — local state: hide cluster for this session.
- Success/failure handlers for each.

Subscriptions:
- `:expenses/duplicate-clusters` — current cluster list.
- `:expenses/merge-preview` — preview data for confirmation modal.
- `:expenses/merge-result` — last merge result for feedback.
- `:expenses/duplicate-loading?` — loading state.

Follow existing event patterns in `src/app/domain/frontend/expenses/events/suppliers.cljs` (http-xhrio, success/failure dispatch).

Re-frame handlers use `trim-v` interceptor — destructure event args as `[params]` not `[_ params]`.

---

### Phase 6: Frontend admin page UI

**Create** `src/app/domain/frontend/expenses/pages/admin/duplicates.cljs`

#### Layout

1. **Entity selector** — tabs: Suppliers | Articles | Stores | Manufacturers.
2. **Strategy selector** — radio/toggle: "Word prefix" | "Trigram" | "Levenshtein".
3. **Cluster list** — each cluster as a card:
   - All candidate records showing `display_name`, `normalized_key`, and related record counts.
   - Radio to pick **primary** record (default: highest record count).
   - Checkboxes on secondaries (all checked by default).
   - **"Merge"** button per cluster.
   - **"Skip / Dismiss"** to hide for this session.
4. **Bulk actions** — "Merge all visible clusters (using auto-selected primary)" with confirmation.
5. **Result feedback** — after merge, show counts of reassigned records and removed duplicates.

#### Confirmation modal

Before executing merge, fetch `/preview-merge` and show:
- Primary record (highlighted).
- Records to be merged (secondaries).
- Impact summary (e.g. "12 expenses, 5 aliases, 3 stores will be moved to primary").
- Confirm / Cancel buttons.

#### Required element IDs

All interactive elements must have stable, unique `:id` attributes:

- `#dedup-entity-tab-{entity}` — entity selector tabs
- `#dedup-strategy-{name}` — strategy radio buttons
- `#dedup-cluster-{idx}` — cluster card container
- `#dedup-primary-radio-{record-id}` — primary selection radio
- `#dedup-secondary-check-{record-id}` — secondary checkbox
- `#dedup-merge-btn-{cluster-idx}` — per-cluster merge button
- `#dedup-merge-all-btn` — bulk merge button
- `#dedup-dismiss-btn-{cluster-idx}` — dismiss button
- `#dedup-confirm-modal` — confirmation modal
- `#dedup-confirm-btn` — confirm button
- `#dedup-cancel-btn` — cancel button

#### Routing

Add route entry for `/admin/expenses/duplicates` in the SPA route registration (domain registry or equivalent). Add admin nav link.

Follow existing admin page patterns in `src/app/domain/frontend/expenses/pages/admin/`.

---

### Phase 7: Testing & validation

**Create** test files:
- `test/app/domain/backend/expenses/services/duplicates_test.clj`
- `test/app/domain/backend/expenses/services/merge_test.clj`

Test cases:
- Detection: each strategy x each entity type returns valid clusters.
- Merge: each entity type reassigns all FK references correctly.
- Edge cases: empty clusters, single-record entities, unique constraint conflicts during alias merge.
- Supplier merge with RESTRICT: verify expenses are reassigned before delete succeeds.
- Stores merge across suppliers: verify scoped uniqueness handled.

REPL validation of full flow on dev data. Browser verification via `chrome-mcp` of admin UI.

---

## Critical technical constraints

| Constraint | Detail |
|-----------|--------|
| **Expenses block supplier delete** | `expenses.supplier_id` has `ON DELETE RESTRICT` — must reassign ALL expenses before deleting secondary suppliers |
| **Cascade danger** | Supplier delete cascades to `stores`, `article_aliases`, `price_observations` — reassign BEFORE delete |
| **Alias unique constraints** | `article_aliases` unique on `[supplier_id, raw_label_normalized]`; skip secondary's alias if conflict |
| **Transaction isolation** | Use `jdbc/with-transaction` for atomicity |
| **Naming boundary** | DB columns `snake_case`, app layer `kebab-case`; use `model-naming/db-keyword->app` |
| **Stores are supplier-scoped** | When merging suppliers, stores from secondaries move to primary — watch `normalized_key` uniqueness within new supplier scope |
| **Existing normalization** | `normalized_key` computed on insert via `service_configs.clj` — leverage for grouping |

## Key files to reference

| Purpose | File |
|---------|------|
| DB schema | `resources/db/domain/models.edn` |
| Route factory | `src/app/domain/backend/expenses/routes/routes_factory.clj` |
| Service configs | `src/app/domain/backend/expenses/services/service_configs.clj` |
| Batch conflict pattern | `src/app/domain/backend/expenses/services/supplier_aliases.clj` (`batch-create-aliases!`) |
| Route mounting | `src/app/domain/backend/expenses/routes/core.clj` |
| Frontend events pattern | `src/app/domain/frontend/expenses/events/suppliers.cljs` |
| Admin detail views | `src/app/domain/frontend/expenses/admin/components/detail_views/` |
| Domain registry | `src/app/domain/backend/registry.clj` |

## Files to create

- `src/app/domain/backend/expenses/services/duplicates.clj`
- `src/app/domain/backend/expenses/services/merge.clj`
- `src/app/domain/backend/expenses/routes/duplicates.clj`
- `src/app/domain/frontend/expenses/events/duplicates.cljs`
- `src/app/domain/frontend/expenses/pages/admin/duplicates.cljs`
- `test/app/domain/backend/expenses/services/duplicates_test.clj`
- `test/app/domain/backend/expenses/services/merge_test.clj`

## Files to modify

- `src/app/domain/backend/expenses/routes/core.clj` — mount duplicates routes
- SPA route registration — add `/admin/expenses/duplicates`
- Admin nav — add link to duplicates page
