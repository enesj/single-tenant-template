---
name: Dedup-Merge
description: Builds duplicate detection and merge tooling for Articles, Suppliers, Stores, and Manufacturers.
model: GPT-5.3-Codex (copilot)
tools: ['vscode', 'execute', 'read', 'agent', 'edit', 'search', 'web', 'vscode/memory', 'todo', 'clojure-mcp/*', 'postgres/*', 'chrome-mcp/*']
---

# Dedup-Merge Agent

Build an admin tool that finds potentially duplicated **Articles**, **Suppliers**, **Stores**, and **Manufacturers** and merges them into a single canonical record with user approval.

## Instruction precedence

1. `AGENTS.md` for workflow and hard rules.
2. `.github/copilot-instructions.md` for implementation guidance.
3. This file for feature-specific requirements.

## Mandatory repo rules

- **Clojure/EDN edits** (`.clj`, `.cljs`, `.cljc`, `.edn`) must use `clojure-mcp` structural editing tools.
- **REPL-first loop**: use `clj-nrepl-eval` for focused debugging/validation.
- **DB operations**: use `postgres-mcp` tools only; no direct `psql` usage.
- **Schema changes**: migrations only, never ad hoc DB/schema edits.
- **Temporary files**: use project-local `tmp/` only.
- Keep changes small, focused, and consistent with existing patterns.
- Respect `snake_case` (DB) vs `kebab-case` (app/runtime) boundaries.

---

## Motivation

OCR ingestion and manual entry create near-duplicate canonical records over time (e.g. `"Coca Cola"` vs `"Coca-Cola"` vs `"COCA COLA 330ml"`). These dilute reporting accuracy and clutter dropdowns. A merge tool lets an admin consolidate duplicates while safely reassigning all foreign-key references.

---

## Duplicate candidate detection — two strategies

### Strategy 1: N-word prefix grouping (deterministic)

Group records by the first **1, 2, and 3 words** of their `display_name` (or `canonical_name` for articles). Any group with >= 2 members is a merge candidate cluster.

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

Run for word counts 1, 2, 3 and union the results, deduplicating clusters that are subsets of larger ones.

### Strategy 2: Normalized-key similarity (fuzzy)

Use the existing `normalized_key` column (already stripped of diacritics, legal suffixes, special chars):

- **Levenshtein distance** — `levenshtein(a.normalized_key, b.normalized_key) <= 2` for short keys, `<= 3` for longer ones. Requires `fuzzystrmatch` extension.
- **Trigram similarity** — `similarity(a.normalized_key, b.normalized_key) > 0.4` using `pg_trgm` extension.

```sql
-- Example: find similar suppliers via trigram
SELECT a.id, a.display_name, b.id, b.display_name,
       similarity(a.normalized_key, b.normalized_key) AS sim
FROM suppliers a
JOIN suppliers b ON a.id < b.id
  AND similarity(a.normalized_key, b.normalized_key) > 0.4
ORDER BY sim DESC;
```

**Before implementing fuzzy strategies**, check whether `fuzzystrmatch` and `pg_trgm` extensions are available. If not, add a migration to create them (`CREATE EXTENSION IF NOT EXISTS fuzzystrmatch; CREATE EXTENSION IF NOT EXISTS pg_trgm;`).

### Candidate output format

Each strategy produces **clusters** — groups of 2+ records that are potential duplicates. The backend should return:

```clojure
{:entity-type :suppliers
 :strategy :prefix-2     ;; or :trigram, :levenshtein
 :clusters [{:ids ["uuid-1" "uuid-2" "uuid-3"]
             :records [{:id "uuid-1" :display-name "Bingo" :normalized-key "bingo" :record-count 15}
                       {:id "uuid-2" :display-name "BINGO d.o.o." :normalized-key "bingo" :record-count 3}
                       {:id "uuid-3" :display-name "Bingo Market" :normalized-key "bingo-market" :record-count 1}]
             :score 0.85}]}
```

`:record-count` = number of related records (expenses, aliases, etc.) to help the user pick the "primary" record.

---

## Merge operation — backend

### Merge semantics

Given a cluster, the user picks one record as **primary** (the survivor). All others are **secondary** (to be absorbed). The merge must:

1. **Reassign all FK references** from each secondary to primary.
2. **Merge aliases** — move alias records; handle unique constraint conflicts by skipping duplicates.
3. **Preserve the richest data** — if primary has `NULL` fields that a secondary has filled, copy them over (e.g. `address`, `link`, `manufacturer_id`).
4. **Delete secondaries** after reassignment.
5. **Wrap in a transaction** — all-or-nothing via `jdbc/with-transaction`.

### Per-entity reassignment map

#### Suppliers merge

| Table | Column | Action |
|-------|--------|--------|
| `expenses` | `supplier_id` | UPDATE to primary (**must happen before delete — ON DELETE RESTRICT**) |
| `stores` | `supplier_id` | UPDATE to primary (handle duplicate `normalized_key` per supplier — skip or append suffix) |
| `article_aliases` | `supplier_id` | UPDATE to primary (skip if `[supplier_id, raw_label_normalized]` unique conflict) |
| `supplier_aliases` | `supplier_id` | UPDATE to primary (skip if `raw_label_normalized` already exists for primary) |
| `price_observations` | `supplier_id` | UPDATE to primary |
| secondary supplier | — | DELETE after all reassignments |

#### Articles merge

| Table | Column | Action |
|-------|--------|--------|
| `expense_items` | `article_id` | UPDATE to primary |
| `article_aliases` | `article_id` | UPDATE to primary (skip unique conflicts on `[supplier_id, raw_label_normalized]`) |
| `price_observations` | `article_id` | UPDATE to primary |
| secondary article | — | DELETE after all reassignments |

#### Stores merge

| Table | Column | Action |
|-------|--------|--------|
| `expenses` | `store_id` | UPDATE to primary |
| `store_aliases` | `store_id` | UPDATE to primary (skip unique conflicts on `raw_label_normalized`) |
| secondary store | — | DELETE after all reassignments |

#### Manufacturers merge

| Table | Column | Action |
|-------|--------|--------|
| `articles` | `manufacturer_id` | UPDATE to primary |
| secondary manufacturer | — | DELETE after all reassignments |

### Handling unique constraint conflicts during alias merge

When reassigning aliases, use an upsert-style approach:

```sql
-- For supplier_aliases: move to primary, skip conflicts
UPDATE supplier_aliases
SET supplier_id = :primary-id
WHERE supplier_id = :secondary-id
  AND raw_label_normalized NOT IN (
    SELECT raw_label_normalized FROM supplier_aliases WHERE supplier_id = :primary-id
  );
-- Then delete remaining orphaned aliases pointing to secondary
DELETE FROM supplier_aliases WHERE supplier_id = :secondary-id;
```

Apply the same pattern for `article_aliases` (scoped by `[supplier_id, raw_label_normalized]`) and `store_aliases` (scoped by `raw_label_normalized`).

### API endpoints

```
GET  /admin/api/expenses/duplicates/:entity-type
     ?strategy=prefix|trigram|levenshtein
     &min-group-size=2
     &limit=50&offset=0
  Response: {:clusters [...] :total-clusters N}

POST /admin/api/expenses/duplicates/merge
     Body: {:entity-type "suppliers"
            :primary-id "uuid-primary"
            :secondary-ids ["uuid-2" "uuid-3"]}
  Response: {:success true
             :merged-count 2
             :reassigned {:expenses 12 :aliases 5 :stores 3 ...}}

GET  /admin/api/expenses/duplicates/preview-merge
     ?entity-type=suppliers&primary-id=...&secondary-ids=...,...
  Response: {:primary {...} :secondaries [...] :impact {:expenses 12 :aliases 5 ...}}
```

---

## Merge operation — frontend (admin UI)

### Page: `/admin/expenses/duplicates`

1. **Entity selector** — tabs or dropdown: Suppliers | Articles | Stores | Manufacturers.
2. **Strategy selector** — radio/toggle: "Word prefix" | "Fuzzy match (trigram)" | "Levenshtein".
3. **Cluster list** — each cluster rendered as a card:
   - All candidate records with `display_name`, `normalized_key`, and related record counts.
   - Radio button to pick the **primary** record (default: the one with highest record count).
   - Checkboxes on secondaries (all checked by default).
   - **"Merge" button** per cluster.
   - **"Skip / Dismiss"** to hide the cluster for this session.
4. **Bulk actions** — "Merge all visible clusters (using auto-selected primary)" with confirmation modal.
5. **Result feedback** — after merge, show counts of reassigned records and removed duplicates.

### Confirmation modal

Before executing merge, show:
- Primary record (highlighted).
- Records to be merged in (secondaries).
- Summary of what will be reassigned (fetched from `/preview-merge` endpoint, e.g. "12 expenses, 5 aliases, 3 stores will be moved to primary").
- **Confirm / Cancel** buttons.

### Interactive elements

All interactive elements must have stable, unique `:id` attributes for `chrome-mcp` verification:

- `#dedup-entity-tab-{entity}` — entity selector tabs.
- `#dedup-strategy-{name}` — strategy radio buttons.
- `#dedup-cluster-{idx}` — cluster card container.
- `#dedup-primary-radio-{record-id}` — primary selection radio.
- `#dedup-secondary-check-{record-id}` — secondary selection checkbox.
- `#dedup-merge-btn-{cluster-idx}` — per-cluster merge button.
- `#dedup-merge-all-btn` — bulk merge button.
- `#dedup-dismiss-btn-{cluster-idx}` — dismiss/skip button.
- `#dedup-confirm-modal` — confirmation modal container.
- `#dedup-confirm-btn` — confirm action button.
- `#dedup-cancel-btn` — cancel action button.

---

## Implementation phases

### Phase 1: PG extensions (if needed) — Owner: Migrations

- Check if `fuzzystrmatch` and `pg_trgm` are already installed.
- If not, add migration: `CREATE EXTENSION IF NOT EXISTS fuzzystrmatch; CREATE EXTENSION IF NOT EXISTS pg_trgm;`
- Apply to both dev and test DBs.

### Phase 2: Backend detection service — Owner: Coder

- Create `src/app/domain/backend/expenses/services/duplicates.clj`.
- Implement `find-duplicate-clusters` with multi-method or cond dispatch on strategy:
  - `:prefix-1`, `:prefix-2`, `:prefix-3` — N-word prefix grouping.
  - `:trigram` — `pg_trgm` similarity (threshold configurable, default 0.4).
  - `:levenshtein` — Levenshtein distance (threshold configurable by key length).
- Support all four entity types via a config map:
  ```clojure
  (def entity-configs
    {:suppliers   {:table "suppliers"   :name-col "display_name"   :key-col "normalized_key"}
     :articles    {:table "articles"    :name-col "canonical_name"  :key-col "normalized_key"}
     :stores      {:table "stores"      :name-col "display_name"   :key-col "normalized_key"}
     :manufacturers {:table "manufacturers" :name-col "display_name" :key-col "normalized_key"}})
  ```
- Enrich each candidate with `:record-count` (count of related FK references).
- Validate via REPL against live dev data.

Files:
- `src/app/domain/backend/expenses/services/duplicates.clj` (new)

### Phase 3: Backend merge service — Owner: Coder

- Create `src/app/domain/backend/expenses/services/merge.clj`.
- Implement `merge-records!` accepting `{:entity-type :primary-id :secondary-ids}`.
- Per-entity-type reassignment logic following the FK maps above.
- Handle unique constraint conflicts: skip conflicting aliases (don't fail).
- Implement `preview-merge` returning impact counts without mutating.
- Wrap mutations in `jdbc/with-transaction`.
- Return detailed result: `{:success true :merged-count N :reassigned {:expenses N ...} :skipped-aliases N}`.
- Validate via REPL: test with a known duplicate pair in dev.

Files:
- `src/app/domain/backend/expenses/services/merge.clj` (new)

### Phase 4: Backend routes — Owner: Coder

- Create `src/app/domain/backend/expenses/routes/duplicates.clj`.
- `GET /duplicates/:entity-type` — calls detection service.
- `GET /duplicates/preview-merge` — calls preview function.
- `POST /duplicates/merge` — calls merge service.
- Mount in `src/app/domain/backend/expenses/routes/core.clj` alongside existing entity routes.

Files:
- `src/app/domain/backend/expenses/routes/duplicates.clj` (new)
- `src/app/domain/backend/expenses/routes/core.clj` (add mount)

### Phase 5: Frontend — re-frame events & subs — Owner: Coder

- Create `src/app/domain/frontend/expenses/events/duplicates.cljs`.
- Events: `::load-clusters`, `::load-preview`, `::execute-merge`, `::dismiss-cluster`.
- Subs: `:expenses/duplicate-clusters`, `:expenses/merge-preview`, `:expenses/merge-result`.
- Follow existing event patterns (http-xhrio, success/failure handlers).

Files:
- `src/app/domain/frontend/expenses/events/duplicates.cljs` (new)

### Phase 6: Frontend — admin page UI — Owner: Designer + Coder

- Create `src/app/domain/frontend/expenses/pages/admin/duplicates.cljs`.
- Entity tabs, strategy selector, cluster cards, primary/secondary selection.
- Merge confirmation modal with preview data.
- Follow existing admin page patterns and styling.
- Add route entry and admin nav link.
- All interactive elements must use the `:id` patterns listed above.

Files:
- `src/app/domain/frontend/expenses/pages/admin/duplicates.cljs` (new)
- SPA route registration (in domain registry or equivalent)
- Admin nav entry

### Phase 7: Testing & validation — Owner: Coder

- Backend tests for detection (each strategy) and merge (each entity type).
- Test file: `test/app/domain/backend/expenses/services/duplicates_test.clj`.
- Test file: `test/app/domain/backend/expenses/services/merge_test.clj`.
- Edge cases: empty clusters, single-record entities, unique constraint conflicts during alias merge, merging supplier that has RESTRICT on expenses.
- REPL validation of full flow on dev data.
- Browser verification via `chrome-mcp` of the admin UI.

---

## Key technical constraints

- **Expenses block supplier delete**: `expenses.supplier_id` has `ON DELETE RESTRICT` — must reassign ALL expenses before deleting secondary suppliers.
- **Cascade danger**: Supplier delete cascades to `stores`, `article_aliases`, and `price_observations` — the merge MUST reassign these BEFORE delete.
- **Alias unique constraints**: `article_aliases` unique on `[supplier_id, raw_label_normalized]`; during merge, if both primary and secondary have an alias with the same normalized label for the same supplier, skip the secondary's alias.
- **Transaction isolation**: Use `jdbc/with-transaction` for atomicity.
- **Naming boundary**: DB columns `snake_case`, app layer `kebab-case`. Use `model-naming/db-keyword->app` at boundaries.
- **Existing normalization**: `normalized_key` is already computed on insert via `service_configs.clj` — leverage for grouping.
- **Stores are supplier-scoped**: When merging suppliers, stores from secondaries move to primary — watch for `normalized_key` uniqueness within the new supplier scope.

## Existing patterns to follow

- **Route factory**: `src/app/domain/backend/expenses/routes/routes_factory.clj` — follow its handler patterns for consistency.
- **Service configs**: `src/app/domain/backend/expenses/services/service_configs.clj` — normalization and entity config patterns.
- **Batch operations**: `src/app/domain/backend/expenses/services/supplier_aliases.clj` `batch-create-aliases!` — model for conflict handling and structured result returns.
- **Frontend events**: `src/app/domain/frontend/expenses/events/suppliers.cljs` — model for list loading, search, and CRUD events.
- **Admin detail views**: `src/app/domain/frontend/expenses/admin/components/detail_views/` — model for entity detail display.
