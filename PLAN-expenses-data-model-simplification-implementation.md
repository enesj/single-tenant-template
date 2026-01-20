# Expenses Data Model Simplification — Implementation Workflow Tracker

Goal: Implement the simplified expenses data model (aliases as the single label source of truth) with a safe, phased rollout and a concrete checklist to track progress.

Owner: Expenses domain  
Date: 2026-01-20  
Status: In progress

## Recent progress (2026-01-20)
- Backend expense services (`create-expense!`/`update-expense!`) now preserve explicitly provided `article_id`, auto-link supplier-scoped aliases for new items, skip invalid labels instead of throwing, and record price observations when an article is present, aligning with the desired Phase 1A semantics.
- The backend suite (`bb be-test`) now reports 236 tests / 1024 assertions with 0 failures, showing the namespace regains compile/runtime stability after the restructure.

## References (Canonical)
- Decision/spec: `EXPENSES-DATA-MODEL-SIMPLIFICATION.md`
- Concrete snippets/guide: `EXPENSES-DATA-MODEL-IMPLEMENTATION.md`

## Target State (Recap)
- Database
  - Drop `raw_labels`
  - `expense_items` uses `alias_id` (FK → `article_aliases`), NOT NULL
  - Drop `expense_items.raw_label_id` and `expense_items.article_id`
  - `article_aliases` stores `raw_label` (latest-seen representative), `raw_label_normalized`, `supplier_id` (NOT NULL), `article_id` (nullable = unmapped)
  - Drop `article_aliases.confidence`
- API contract
  - Expense create/update accepts item `raw_label` strings (no frontend `alias_id` required)
  - Backend resolves/creates aliases and persists `expense_items.alias_id`
  - Mapping “unmapped queue” operates on aliases (update `article_aliases.article_id`), not per-item overrides
- Migration safety
  - “Unknown Supplier” is identified by `suppliers.normalized_key = 'unknown-supplier'`
  - Historical `expense_items.article_id` conflicts are recorded for manual review (`expense_alias_article_conflicts`)

---

## Rollout Strategy (How we stage risk)

1) **DB Phase 1 (Additive)**: add columns + relax constraints needed for backfill; keep legacy columns/tables.
2) **Code Rollout A (Compatible)**: code can run with Phase 1 schema and prepares the system for alias-based behavior (without breaking existing flows).
3) **DB Phase 2 (Backfill)**: populate aliases + link all `expense_items.alias_id`; persist conflicts for manual review.
4) **Code Rollout B (Cutover)**: switch reads/writes + UI to alias-based mapping; remove legacy endpoints/pages.
5) **DB Phase 3 (Finalize/Destructive)**: enforce NOT NULL constraints and drop legacy columns/tables/fields.

> Gate rule: do not run a DB phase unless the required code rollout is deployed and verified for that phase.

---

## Phase 0 — Preflight (Prep + Evidence)

- [ ] Confirm the repo is green on current main branch (focused tests only).
- [ ] Confirm `suppliers.normalized_key` is unique (required for Unknown Supplier upsert/lookup).
- [ ] Snapshot baseline counts (dev/staging/prod as appropriate):
  - `SELECT COUNT(*) FROM expense_items;`
  - `SELECT COUNT(*) FROM raw_labels;`
  - `SELECT COUNT(*) FROM article_aliases;`
  - `SELECT COUNT(*) FROM expense_items WHERE deleted_at IS NOT NULL;`
- [ ] Ensure you have a rollback path:
  - Preferred: database backup before DB Phase 2 and DB Phase 3.

---

## Phase 1 — DB Additive Schema (Safe)

Goal: Add the new columns required for backfill and ensure the schema can represent unmapped aliases.

- [ ] Update `resources/db/domain/models.edn` (Phase 1 state):
  - `article_aliases`: add `:raw_label` (temporarily nullable)
  - `article_aliases`: drop `:null false` from `:article_id` (must allow `NULL = unmapped`)
  - `expense_items`: add `:alias_id` (temporarily nullable, FK → `article_aliases`)
  - Keep `raw_labels`, `expense_items.raw_label_id`, `expense_items.article_id`, and `article_aliases.confidence` for now
- [ ] Generate migrations:
  - `(require '[app.template.backend.migrations.simple-repl :as mig])`
  - `(mig/make-all-migrations!)`
- [ ] Apply migrations:
  - `(mig/migrate!)`
- [ ] Verify:
  - `article_aliases` accepts `article_id = NULL`
  - `expense_items` has `alias_id` column

Phase 1 gate:
- [ ] App boots and core expenses flows still work (no behavior changes required yet).

---

## Phase 1A — Code Rollout A (Compatible, prepares cutover)

Goal: Introduce alias-resolution primitives and make the backend capable of persisting/reading `alias_id` without requiring the full cutover yet.

Backend (clj)
- [ ] `src/app/domain/backend/expenses/services/article_aliases.clj`
  - Add/confirm `find-or-create-alias!` (Unknown Supplier fallback by normalized_key)
  - Ensure it updates `raw_label` on conflict (latest-seen representative)
- [ ] `src/app/domain/backend/expenses/services/service_configs.clj`
  - Update expense-item service config to resolve `alias_id` from `raw_label` server-side (do not require frontend `alias_id`)
  - Update joins/selects/search to join `article_aliases` for `raw_label` (stop joining `raw_labels`)
  - Update article-alias config to *not* require `article_id` (unmapped allowed)
- [ ] `src/app/domain/backend/expenses/routes/route_configs.clj`
  - Align comments and required-fields with alias-based semantics (raw_label input-only; alias stored)

Verification
- [ ] Targeted backend tests pass (at minimum the expenses namespaces touched).

Phase 1A gate:
- [ ] No endpoints require client-sent `alias_id` for expense item create/update paths.

---

## Phase 2 — DB Backfill (Manual SQL Migration)

Goal: Migrate existing data: create missing aliases, link every expense item to an alias, and persist historical `article_id` conflicts for manual review.

- [ ] Create an empty SQL migration:
  - `(require '[app.template.backend.migrations.simple-repl :as mig])`
  - `(mig/make-migration! :type :empty-sql :name backfill_raw_labels_data)`
- [ ] Populate the SQL using the canonical snippet in `EXPENSES-DATA-MODEL-IMPLEMENTATION.md` (Phase 2 section):
  - Upsert Unknown Supplier by `normalized_key`
  - Create `expense_alias_article_conflicts`
  - Upsert aliases from `expense_items` + `raw_labels` + `expenses.supplier_id`
  - Backfill `expense_items.alias_id`
- [ ] Apply:
  - `(mig/migrate!)`

Verification
- [ ] `SELECT COUNT(*) FROM expense_items WHERE alias_id IS NULL;` → must be `0`
- [ ] Review conflicts:
  - `SELECT * FROM expense_alias_article_conflicts ORDER BY item_count DESC;`

Conflict handling (manual)
- [ ] For each conflict group, decide on the correct alias mapping:
  - If one article is correct: set `article_aliases.article_id` accordingly
  - If truly ambiguous: leave unmapped (`NULL`) and handle in the new unmapped-alias queue

Phase 2 gate:
- [ ] `expense_items.alias_id` is complete (no NULLs) before Phase 3.

---

## Phase 2A — Code Rollout B (Cutover: alias-only behavior + UI alignment)

Goal: Switch product behavior to the simplified model (aliases drive article linkage), remove per-item mapping semantics, and update UI accordingly.

Backend (clj)
- [ ] `src/app/domain/backend/expenses/services/expenses.clj`
  - Write `expense_items.alias_id` (not `raw_label_id` / `article_id`)
  - Read/join labels + article names via `article_aliases` (+ `articles`)
  - Price observations use alias’ `article_id` when present
- [ ] Replace legacy per-item “unmapped items” endpoints:
  - Remove/refactor `src/app/domain/backend/expenses/routes/articles.clj` custom endpoints that map *items* to articles
  - Introduce/route alias-based endpoints (map alias → article; list unmapped aliases)
- [ ] Remove raw-labels endpoints:
  - `src/app/domain/backend/expenses/handlers/user_raw_labels.clj`
  - `src/app/domain/backend/expenses/routes/raw_labels.clj`
  - Remove references from `src/app/domain/backend/expenses/routes/user_api.clj`

Frontend (cljs)
- [ ] Remove raw-labels UI page and wiring:
  - `src/app/domain/frontend/expenses/pages/user/raw_labels.cljs`
  - Remove `/raw-labels` routes/nav/events
- [ ] Refactor unmapped UX to aliases:
  - `src/app/domain/frontend/expenses/events/unmapped_items.cljs` (+ `subs/*` + `components/*`)
  - `src/app/domain/frontend/expenses/events/user_expenses/endpoints.cljs` (remove legacy endpoints; use alias-based endpoints)
- [ ] Verify expense detail still renders label text:
  - `src/app/domain/frontend/expenses/pages/user/expense_detail.cljs`

Config (edn)
- [ ] `src/app/domain/frontend/expenses/config/table-columns.edn`
  - `expense-items`: show `alias_id` and alias-driven `raw_label` fields
  - `article-aliases`: include `raw_label`
- [ ] `src/app/domain/frontend/expenses/config/form-fields.edn`
  - Remove `confidence` from article-aliases fields
- [ ] `src/app/domain/frontend/expenses/authz.cljs`
  - Remove raw-labels capability if present

Verification
- [ ] Backend tests: `bb be-test` (or targeted namespaces)
- [ ] Frontend tests: `bb fe-test-parallel` (or targeted)
- [ ] Manual smoke:
  - Receipt upload → expense created with items showing `raw_label`
  - Unmapped queue shows aliases, mapping updates `article_aliases.article_id`

Phase 2A gate:
- [ ] No user-facing code depends on `raw_labels` or `expense_items.article_id`.

---

## Phase 3 — DB Finalize Schema (Destructive)

Goal: Enforce constraints and delete legacy schema pieces.

- [ ] Update `resources/db/domain/models.edn` (final state):
  - `article_aliases`: `supplier_id NOT NULL`, `raw_label NOT NULL`, keep `article_id NULLABLE`
  - `expense_items`: `alias_id NOT NULL`
  - Drop: `raw_labels` table
  - Drop: `expense_items.raw_label_id`, `expense_items.article_id`
  - Drop: `article_aliases.confidence`
- [ ] Generate migrations: `(mig/make-all-migrations!)`
- [ ] Apply migrations: `(mig/migrate!)`

Verification
- [ ] `SELECT COUNT(*) FROM expense_items WHERE alias_id IS NULL;` → must be `0`
- [ ] Confirm dropped columns/tables do not exist and app boots cleanly.

---

## Post-Implementation Cleanup (Optional but recommended)

- [ ] Decide whether to keep or drop `expense_alias_article_conflicts` after review is complete.
- [ ] Remove any remaining dead code/mentions of raw-labels in FE configs and backend services.
- [ ] Update docs if implementation deviated from the plan (keep decision + implementation guide canonical).

## Definition of Done
- DB matches the simplified schema (no `raw_labels`, no `expense_items.article_id/raw_label_id`, no `article_aliases.confidence`).
- Receipt/expense ingestion creates/uses aliases and persists `alias_id`.
- Unmapped mapping UX operates on aliases (not expense items), and mapping updates `article_aliases.article_id`.
- Focused BE/FE tests pass; manual smoke checks pass.

