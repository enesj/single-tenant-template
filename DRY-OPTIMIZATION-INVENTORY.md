# Domain Code — DRY & Optimization Inventory

> Generated: 2026-03-16
> Scope: `src/app/domain/` (backend + frontend), with cross-references to `src/app/template/`

---

## Quick Reference — Priority Matrix

| # | Issue | Status | LOC saved | Priority |
|---|-------|--------|-----------|----------|
| 1 | Handler helper functions duplicated in 7+ files | **DONE** | ~350 | P1 |
| 2 | Duplicated query patterns in `related_records.clj` | **DONE** | ~200 | P1 |
| 3 | Batch-delete try/catch duplication | **DONE** | ~120 | P1 |
| 4 | Generic CRUD handler factory missing | Deferred | ~1 000 | P2 |
| 5 | Frontend event factory underused | **Already done** | ~2 869 | P2 |
| 6 | Validation inconsistency across create handlers | **Not needed** | — | P2 |
| 7 | Duplicate body-parsing functions | **DONE** | ~60 | P2 |
| 8 | Admin vs user expense form specs duplicated | Deferred | ~67 | P3 |
| 9 | Error response shape inconsistency | **Not needed** | — | P3 |
| 10 | Role-set constants scattered across handler files | **DONE** | ~40 | P3 |
| 11 | Pagination defaults inconsistent | **By design** | — | P3 |
| 12 | Dead code / empty files | **DONE** | ~30 | P3 |

**Implemented: 17 files changed, ~880 lines removed, ~330 added = ~550 net lines saved**
**Previously existing: §5 event factory already saves ~2,869 lines across 23 entities**

---

## Implemented (§1, §2, §3, §7, §10, §12)

### What was done

**Centralized helpers in `user_expenses/helpers.clj`** (already the shared helpers namespace):
- `to-app` — DB-to-app key normalization (was `(def ^:private to-app shared-db/to-app)` in 8 files)
- `admin-owner-roles` / `ensure-admin-or-owner` — role gate (was private `defn-` in 7 files)
- `body-has-any-key?` / `body-get-first` — multi-key body field extraction (was in 3 files)
- `parse-path-id` — UUID extraction from path params (was inline `(h/try-parse-uuid (or (get-in ...)))` in 8 files)
- `parse-batch-ids` — batch ID parsing with multi-key support (was ~6 lines in 8 files)
- `batch-delete-entities` — full batch-delete loop with PSQLException handling (was ~20 lines in 8 files)
- `PSQLException` import added to helpers (removed from all handler files)

**Refactored 8 handler files** to use shared helpers:
- `user_manufacturers.clj`, `user_categories.clj`, `user_subcategories.clj`
- `user_expense_categories.clj`, `user_cities.clj`, `user_store_aliases.clj`
- `user_stores.clj`, `user_articles.clj`

**DRY'd `related_records.clj`** query patterns:
- Extracted `tenant-scope`, `fetch-article-aggs`, `fetch-last-prices`, `merge-last-prices`, `articles-with-last-prices`
- 7 of 8 `related-for-*` functions refactored to use shared helpers
- Each function defines its own `base-joins` vector — helpers are parameterized over JOIN chains

**Dead code removed:**
- `report-config` from `service_configs/config_maps.clj` + registry entry (no DB table, no callers)
- `expense_form/forms.cljs` — empty namespace, unreferenced
- `expense_form/modals.cljs` — empty namespace, unreferenced
- `subs/payers.cljs`, `subs/receipts.cljs`, `subs/expenses.cljs` — empty namespaces, unreferenced

### Files modified
```
src/app/domain/backend/expenses/handlers/user_expenses/helpers.clj   (+74)
src/app/domain/backend/expenses/handlers/user_manufacturers.clj      (-80)
src/app/domain/backend/expenses/handlers/user_categories.clj         (-79)
src/app/domain/backend/expenses/handlers/user_subcategories.clj      (-75)
src/app/domain/backend/expenses/handlers/user_expense_categories.clj (-79)
src/app/domain/backend/expenses/handlers/user_cities.clj             (-89)
src/app/domain/backend/expenses/handlers/user_store_aliases.clj      (-95)
src/app/domain/backend/expenses/handlers/user_stores.clj             (-99)
src/app/domain/backend/expenses/handlers/user_articles.clj           (-97)
src/app/domain/backend/expenses/handlers/search/related_records.clj  (-200+)
src/app/domain/backend/expenses/services/service_configs/config_maps.clj  (removed report-config)
src/app/domain/backend/expenses/services/service_configs/registry.clj     (removed :report entry)
```

### Files deleted
```
src/app/domain/frontend/expenses/components/expense_form/forms.cljs
src/app/domain/frontend/expenses/components/expense_form/modals.cljs
src/app/domain/frontend/expenses/subs/payers.cljs
src/app/domain/frontend/expenses/subs/receipts.cljs
src/app/domain/frontend/expenses/subs/expenses.cljs
```

---

## Resolved — No Action Needed

### §5 — Frontend Event Factory Adoption

**Investigation result:** The event factory is already adopted across **all 23 CRUD entity namespaces**, saving ~2,869 lines of boilerplate. Each entity is 8–16 lines (config + registration call) vs ~115 lines hand-rolled. The only hand-rolled events are intentionally specialized workflows (unmapped-items 321 LOC, duplicates 249 LOC) that don't fit the factory pattern. This is a success story, not a TODO.

### §6 — Validation on Create Handlers

**Investigation result:** The service factory (`services_factory.clj`) already validates all `:required-fields` defined in each entity's config via `build-create-function`. Custom services (articles) also validate inline. Handler-level validation would be redundant.

### §9 — Error Response Shape

**Investigation result:** Three shapes exist but the frontend's `extract-error-message` in `shared/http.cljc` handles all of them via a 6-path fallback chain (`:error`, `[:response :error]`, `[:response :message]`, `[:body :error]`, `[:body :message]`, `:status-text`). Standardizing would be cosmetic churn (~50 LOC saved) with no functional benefit.

### §11 — Pagination Defaults

**Investigation result:** Receipts use 50 (heavy rows with OCR data, line items, approval status), all others use 200. This is an intentional design decision based on row payload weight, not an inconsistency.

---

## Deferred — Future Opportunities

### §4 — Generic CRUD Handler Factory (P2, High effort)

Each entity handler has enough variation (tenant scoping, extra query filters, entity-specific field extraction, alias endpoints) that a factory would need many escape hatches. The shared helpers already reduce boilerplate significantly. Revisit if more uniform entities are added.

### §8 — Admin vs User Expense Form Consolidation (P3, Low priority)

**Investigation result:** The `expense-form` namespace is effectively dead code (forms.cljs/modals.cljs deleted). Only `specs.cljs` and `normalization.cljs` remain, referenced by test files. ~90 LOC of normalization helpers (`pad-two`, `datetime-local`, `prepare-line-items`, `validate-expense-values`, `prepare-expense-submit-values`) and 27 LOC of `line-item-columns` are shared with `user-expense-form`. Net savings after extraction to `shared.cljs`: ~67 lines. Low ROI given the test refactoring effort required.
