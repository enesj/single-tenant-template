# Plan: Removal of Unused/Legacy Code in `src/app/domain`

**Date:** 2026-01-13
**Status:** In Progress — Phase 3 (legacy alias refactor) completed for payers/suppliers/expense-items/article-aliases/price-observations; Phase 4 (arity support) completed; Phase 5 (settings persistence) completed

## Overview

This document outlines unused, legacy, and stub code found in the `src/app/domain` directory that can be considered for removal or refactoring. The analysis identified **5 categories** of potential cleanup opportunities.

---

## Category 1: Empty/Placeholder Functions (HIGH CONFIDENCE)

### 1.1 Frontend Registry - `src/app/domain/frontend/registry.cljs`

#### `all-admin-routes` function (lines 117-123)
```clojure
(defn all-admin-routes
  "Admin routes are NOT provided via the registry to avoid circular dependencies.
   Use this function as a placeholder - admin routes should be required directly
   by admin/frontend/routes.cljs from domain route namespaces."
  []
  ;; Return empty - admin routes are merged directly in admin/frontend/routes.cljs
  [])
```
- **Status:** ✅ Removed (function was only referenced by a unit test)
- **Risk:** LOW
- **Action:** Done — removed `all-admin-routes` and the corresponding test.

#### `all-pages` function (lines 137-142)
```clojure
(defn all-pages
  "Pages are NOT provided via the registry to avoid circular dependencies.
   Use app.domain.frontend.pages instead."
  []
  ;; Return empty - pages should be loaded from app.domain.frontend.pages
  {})
```
- **Status:** ✅ Removed (function was only referenced by a unit test)
- **Risk:** LOW
- **Action:** Done — removed `all-pages` and the corresponding test.

### 1.2 Backend Registry - `src/app/domain/backend/registry.clj`

#### `get-ui-config-paths` function (lines 118-125)
```clojure
(defn get-ui-config-paths
  "Get the UI config paths for all enabled domains.
   Returns a merged map of domain-id -> paths."
  []
  (into {}
    (map (fn [manifest]
           [(:id manifest) (get-in manifest [:ui-config :user :paths])]))
    enabled-domains))
```
- **Status:** Actively used by template backend routes (domain-owned UI config loading)
- **Risk:** HIGH if removed without a replacement API (template calls this directly)
- **Action:** **Keep**. Not a legacy stub.

---

## Category 2: Legacy Function Name Aliases (MEDIUM CONFIDENCE)

### Backend Service Files - "Legacy function names for backward compatibility"

The following service files previously contained wrapper vars that aliased service methods:

| File | Pattern (historical) | Status |
|------|----------------------|--------|
| `expense_items.clj` | `(def list-expense-items (:list service))` | ✅ Removed |
| `payers.clj` | `(def list-payers (:list service))` | ✅ Removed |
| `suppliers.clj` | `(def get-supplier (:get service))` | ✅ Removed |
| `article_aliases.clj` | `(def ^:private list-article-aliases-base (:list service))` | ✅ Removed |
| `price_observations.clj` | `(def ^:private list-price-observations-base (:list service))` | ✅ Removed |

**Example from `expense_items.clj`:**
```clojure
;; Legacy function names for backward compatibility with routes
(def list-expense-items (:list service))
(def get-expense-item (:get service))
(def create-expense-item! (:create! service))
(def update-expense-item! (:update! service))
```

- **Status (previously):** These names were used via `requiring-resolve` and by route factory/config indirection.
- **Status (now):** ✅ Refactor complete for core expenses entities:
  - `src/app/domain/backend/expenses/routes/routes_factory.clj` now resolves CRUD/search/count by:
    1) preferring an explicitly named var (wrappers/overrides), then
    2) falling back to the namespace `service` map op (e.g. `(:list service)`), enabling alias removal.
  - `src/app/domain/backend/expenses/handlers/user_expenses/reference_data.clj` now resolves `:list/:get/:create!/:update!/:delete!` via the `service` map (with legacy-var fallback).
  - Removed legacy alias `def`s from:
    - `services/payers.clj`: removed `list-payers`, `get-payer`, `delete-payer!`, `count-payers`, `search-payers` (kept wrapped `create-payer!` / `update-payer!`).
    - `services/suppliers.clj`: removed `get-supplier`, `create-supplier!`, `update-supplier!` (kept archived-aware ops like `list-suppliers`, `delete-supplier!`, purge fns).
    - `services/expense_items.clj`: removed `list-expense-items`, `get-expense-item`, `create-expense-item!`, `update-expense-item!`, `count-expense-items`, `search-expense-items` (kept custom `delete-expense-item!`).
  - Updated direct call sites/tests to use the `service` map where needed.
- **Risk:** MEDIUM (resolved via incremental refactor + tests)
- **Action:** ✅ Done for the above **including**:
  - `services/article_aliases.clj`: removed private `list-article-aliases-base` and legacy CRUD alias `def`s; kept `list-article-aliases` wrapper for filtered supplier detail views.
  - `services/price_observations.clj`: removed private `list-price-observations-base` and legacy CRUD alias `def`s; kept `list-price-observations` wrapper + `create-price-observation!` override.

---

## Category 3: Legacy Arity Support (LOW-MEDIUM CONFIDENCE)

### Backend Registry - `src/app/domain/backend/registry.clj`

#### Phase 4 change (✅ completed)

The registry previously supported both 2-arity and 3-arity domain `:user-api` route functions via
reflection-based arity detection.

That compatibility layer has now been **removed**.

**New requirement:** domain `:user-api` route fns must accept 3 args:
- `(fn [db wrap-user-auth app-config] ...)` or
- `(fn [db wrap-user-auth & [app-config]] ...)`

**Current implementation (simplified):**
```clojure
(defn all-user-api-routes
  "Collect user API routes from all enabled domains.
   Returns a vector of reitit route vectors.

   `app-config` is optional.

   Domain `:user-api` fns must accept 3 args:
   - (fn [db wrap-user-auth app-config] ...)
   - (fn [db wrap-user-auth & [app-config]] ...)"
  [db wrap-user-auth & [app-config]]
  (mapv (fn [manifest]
          (when-let [route-fn (get-in manifest [:routes :user-api])]
            (route-fn db wrap-user-auth app-config)))
    enabled-domains))
```

- **Status:** ✅ Removed legacy 2-arity support (Phase 4)
- **Risk:** MEDIUM (intentional breaking change for any future 2-arity domains)
- **Action:** ✅ Updated tests by removing the explicit 2-arity compatibility test in `test/app/domain/backend/registry_test.clj`.

---

## Category 4: Stub Implementations (✅ Implemented)

### Settings Handlers - `src/app/domain/backend/expenses/handlers/user_expenses/settings.clj`

The settings endpoints are now backed by a real persistence layer:

- **Schema**: Added `user_expense_settings` (keyed by `user_id`) to `resources/db/domain/models.edn`.
- **Migration**: Generated `resources/db/migrations/0018_user_expense_settings.edn`.
- **Backend implementation**:
  - New service: `src/app/domain/backend/expenses/services/user_expense_settings.clj` (get + upsert)
  - Updated handlers:
    - `GET /api/v1/expenses/settings` returns defaults if no row exists
    - `PUT /api/v1/expenses/settings` validates input + upserts
  - Role gating aligns with other user-expenses endpoints:
    - GET: `reference-data-read-roles`
    - PUT: `reference-data-write-roles`
- **Tests**: Added focused handler tests in `test/app/domain/backend/expenses/handlers/user_expenses/settings_test.clj`.

- **Status:** ✅ Implemented (no longer a stub)
- **Risk:** LOW-MEDIUM (new table + migration, but isolated and covered by tests)

---

## Category 5: Empty Init Function (LOW CONFIDENCE)

### Expenses Domain Core - `src/app/domain/frontend/expenses/core.cljs`

```clojure
(defn init!
  "Ensure expenses domain events/subs are loaded."
  [])
```

- **Status:** ✅ Removed (namespace remains as a side-effect require bundle)
- **Risk:** LOW
- **Action:** Done — removed the empty `init!`.

---

## Summary by Category

| Category | Files Affected | Lines | Removal Risk | Recommendation |
|----------|---------------|-------|--------------|----------------|
| Empty/Placeholder Functions | 2 | ~25 | LOW | ✅ Removed |
| Legacy Function Aliases | 5 | ~40 | MEDIUM | ✅ Refactor completed for payers/suppliers/expense-items/article-aliases/price-observations |
| Legacy Arity Support | 1 | ~25 | MEDIUM | ✅ Removed (registry now requires 3-arity `:user-api` route fns) |
| Stub Implementations | 1 | ~50 | LOW-MEDIUM | ✅ Implemented settings persistence |
| Empty Init | 1 | ~3 | LOW | ✅ Removed |

---

## Execution Plan (Phase-by-Phase)

### Phase 1: Verification (Read-Only)
1. ✅ Search for all callers of `all-admin-routes` and `all-pages` in frontend registry
2. ✅ Search for all callers of `get-ui-config-paths` in backend registry
3. ✅ Verify all domain route functions now use 3-arity signature (and note tested 2-arity compatibility)
4. ✅ Confirm stub implementations are intentional (not dead code)

### Phase 2: Safe Removals (Empty Functions)
1. ✅ Remove `all-admin-routes` from `frontend/registry.cljs`
2. ✅ Remove `all-pages` from `frontend/registry.cljs`
3. ✅ Remove or inline `init!` in `expenses/core.cljs` (and update any tests that referenced it)

### Phase 3: Update Callers (Legacy Aliases)
1. ✅ Update dynamic resolution to allow using the `service` map
  - `routes_factory.clj`: prefer per-entity var, fallback to `service` map op
  - `reference_data.clj`: resolve ops via `service` map (legacy-var fallback)
2. ✅ Remove legacy function name aliases from service files (payers/suppliers/expense-items)
3. ✅ Update affected call sites/tests
4. ✅ Run backend tests

> Note: With current dynamic resolution patterns, Phase 3 is a refactor project (not a quick cleanup).

### Phase 4: Simplify Arity Support
1. ✅ Verify all domain routes use 3-arity
2. ✅ Simplify user route invocation to a direct call (removed `call-user-api-route-fn`)
3. ✅ Remove `fn-supports-arity?`

> Note: Phase 4 included removing the explicit 2-arity compatibility test.

### Phase 5: Decision Required (Stubs)
1. ✅ Implemented settings storage via `user_expense_settings` + migration + tests

---

## Files NOT Considered for Removal

After analysis, the following were determined to be **actively used** and **should NOT be removed**:

- **`unmapped_items.cljs`** - Part of the active power-user feature for managing unmapped receipt line items
- **All page components in `pages/user/`** - All are referenced in `pages.cljs` and have active routes
- **Registry manifests** - Core to domain architecture
- **Event/subs namespaces** - All loaded via registry for side effects

---

## Notes

- The domain architecture uses registries to dynamically compose routes, entities, and groups
- Some "legacy" code supports graceful migration from older patterns
- Settings persistence is now implemented and no longer considered legacy/stub code

---

## Next Steps

1. Review this plan and confirm which categories to proceed with
2. Run verification searches to ensure no external callers will break
3. Execute changes phase-by-phase with testing after each phase
