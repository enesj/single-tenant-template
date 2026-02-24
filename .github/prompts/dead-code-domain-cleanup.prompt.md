---
mode: agent
description: "Remove dead code identified in the domain layer (expenses backend + frontend). Each item has been verified against the full src/ tree."
---

# Dead Code Cleanup — `src/app/domain/`

This prompt lists every verified dead symbol and namespace in the expenses domain.
Execute each item in order. Delete whole files where the entire namespace is dead.
Remove individual symbols from live files otherwise.

## Confidence legend
- **High** — confirmed zero callers/subscribers/requires across all of `src/`
- **Medium** — no production callers; exists for REPL/maintenance use (decide: keep in a dev namespace or delete)

---

## 1. Entire dead namespaces — delete the files

### `src/app/domain/backend/expenses/services/price_observations.clj`
**High confidence.** Never required by any file in `src/`. The `:price-observation` CRUD routes
are served by the factory pattern in `service_configs.clj` and `services_factory.clj`.
This file is a parallel implementation that was abandoned.
Delete the file. No require cleanup needed (nothing imports it).

Functions inside (all dead):
- `list-price-observations` (line 30)
- `count-price-observations` (line 63)
- `create-price-observation!` (line 92)

---

### `src/app/domain/frontend/expenses/config/preload.cljs`
**High confidence.** Never required by any file in `src/`. Admin UI reads config via the backend
API (`/admin/user-settings`), not via this compile-time preload.
Delete the file.

Functions inside (all dead):
- `entity-config` (line 26)
- `view-options` (line 31)
- `form-fields` (line 36)
- `table-columns` (line 41)

---

### `src/app/domain/frontend/expenses/core.cljs`
**High confidence.** Never required anywhere in `src/`. Predates `init.cljs` and was orphaned
when the domain registry was refactored. All side-effect registrations it would trigger happen
via `init.cljs` and admin page requires instead.
Delete the file.

---

### `src/app/domain/frontend/expenses/admin/subs.cljs`
**High confidence.** Never required by any file in `src/`. None of the 12 subscription keywords
are subscribed to in any component or event handler. Admin entity loading/error state is tracked
via the template-level entity machinery (`paths/entity-loading?`, `paths/entity-error`).
Delete the file.

Dead subscriptions (all 12):
- `:admin/expenses-loading?` (line 7)
- `:admin/expenses-error` (line 11)
- `:admin/receipts-loading?` (line 17)
- `:admin/receipts-error` (line 22)
- `:admin/suppliers-loading?` (line 27)
- `:admin/suppliers-error` (line 32)
- `:admin/payers-loading?` (line 38)
- `:admin/payers-error` (line 43)
- `:admin/articles-loading?` (line 49)
- `:admin/articles-error` (line 54)
- `:admin/article-aliases-loading?` (line 61)
- `:admin/article-aliases-error` (line 66)

---

### `src/app/domain/frontend/expenses/admin/adapters/ui_state.cljs`
**High confidence.** Never required by any file in `src/`. Despite its docstring claiming
"consumed by domain registry today", `registry.cljs` does not require this namespace.
All 10 public symbols are unreachable.
Delete the file.

Dead symbols:
- `entity-init-fns` (line 60)
- `init-all-adapters!` (line 70)
- `init-expenses-adapter!` (line 78)
- `init-receipts-adapter!` (line 79)
- `init-suppliers-adapter!` (line 80)
- `init-payers-adapter!` (line 81)
- `init-articles-adapter!` (line 82)
- `init-article-aliases-adapter!` (line 83)
- `init-supplier-aliases-adapter!` (line 84)
- `init-expense-items-adapter!` (line 86)

---

## 2. Individual dead symbols — remove from live files

### `src/app/domain/backend/expenses/services/suppliers.clj`
- **`active-expenses-counts-by-supplier`** (line 251) — **High.** No callers in `src/`.
  Returns supplier-id → active expense count. Remove the function.
- **`search-suppliers-autocomplete`** (line 603) — **High.** No callers in `src/`.
  Distinct from `search-suppliers` (which is used). Remove the function.

---

### `src/app/domain/backend/expenses/services/stores.clj`
- **`normalize-city-name`** (line 35) — **High.** Private, tagged `^:unused` by the author.
  Remove it.
- **`find-or-create-city!`** (line 50) — **High.** Docstring calls it "legacy". No callers.
  City creation goes through `services/cities.clj`. Remove it.
- **`bad-city?`** (line 178) — **High.** The `bad-city?` binding in
  `services/stores/matching.clj` is a local `let` — not a call to this function.
  Remove it.
- **`backfill-store-place-ids!`** (line 767) — **Medium.** No production callers.
  Described as admin/REPL maintenance. Either delete or move to a `(comment ...)` block.

---

### `src/app/domain/backend/expenses/services/manufacturers.clj`
- **`normalize-manufacturer-key`** (line 31) — **High.** Dead re-export:
  `(def normalize-manufacturer-key configs/normalize-manufacturer-key)`.
  Call sites use `configs/normalize-manufacturer-key` directly. Remove the re-export.

---

### `src/app/domain/backend/expenses/services/service_configs.clj`
- **`list-entity-configs`** (line 732) — **High.** Returns `(keys entity-configs)`.
  No callers in `src/`. Remove it.

---

### `src/app/domain/backend/expenses/handlers/user_expenses/reference_data.clj`
- **`delete-supplier-handler`** (line 225) — **High.** Defines a DELETE handler for
  `/suppliers/:id`. `user_api.clj` only mounts the batch handler; this singular handler is
  never mounted. Remove it or mount it in the route tree if the feature was deferred.
- **`delete-payer-handler`** (line 393) — **High.** Same situation for `/payers/:id`.
  Remove it or mount it.

---

### `src/app/domain/frontend/expenses/subs/user_expenses.cljs`
Nine Re-frame subscriptions that are registered but never subscribed to. The expense reports
page reads these values directly from the parent `:user-expenses/reports-filters` map.

Remove the following (lines 323–373, but keep lines 333 and 338 which ARE used):
- `:user-expenses/reports-filter-months-back` (line 323)
- `:user-expenses/reports-filter-supplier-id` (line 328)
- `:user-expenses/reports-filter-category-key` (line 348)
- `:user-expenses/reports-filter-day-of-week` (line 343)
- `:user-expenses/reports-filter-amount-bucket` (line 353)
- `:user-expenses/reports-filter-selected-day` (line 358)
- `:user-expenses/reports-filter-month-a` (line 363)
- `:user-expenses/reports-filter-month-b` (line 368)
- `:user-expenses/reports-filter-show-uncategorized?` (line 373)

**Keep** (confirmed used in `expense_reports.cljs` lines 86–87):
- `:user-expenses/reports-filter-expanded-supplier-id` (line 333)
- `:user-expenses/reports-filter-expanded-top-item-alias-id` (line 338)

---

## Execution order

1. Delete the 5 entire dead namespace files (section 1) — no require cleanup needed.
2. Remove `delete-supplier-handler` and `delete-payer-handler` from `reference_data.clj` — check `user_api.clj` to confirm neither is mounted before deleting.
3. Remove the 9 dead report-filter subscriptions from `user_expenses.cljs`.
4. Remove dead functions from `suppliers.clj`, `stores.clj`, `manufacturers.clj`, `service_configs.clj`.
5. Decide on `backfill-store-place-ids!` (medium confidence) separately.

After each file change, reload the namespace in the connected nREPL to confirm no compilation errors:
```clojure
(require 'the.changed.ns :reload)
```
