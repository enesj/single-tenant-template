# Plan: Removal of Unused/Legacy Code in `src/app/domain`

**Date:** 2026-01-13
**Status:** In Progress — Phase 3 (legacy alias refactor) completed for payers/suppliers/expense-items

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

The following service files contain wrapper vars that alias service methods:

| File | Lines | Pattern |
|------|-------|---------|
| `expense_items.clj` | 25-44 | `(def list-expense-items (:list service))` |
| `payers.clj` | 23-51 | `(def list-payers (:list service))` |
| `suppliers.clj` | 22-26 | `(def get-supplier (:get service))` |
| `article_aliases.clj` | 25-26 | `(def ^:private list-article-aliases-base (:list service))` |
| `price_observations.clj` | 25-26 | `(def ^:private list-price-observations-base (:list service))` |

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
- **Action:** ✅ Done for the above. Remaining alias-like private vars in other services (`article_aliases.clj`, `price_observations.clj`) can be evaluated separately.

---

## Category 3: Legacy Arity Support (LOW-MEDIUM CONFIDENCE)

### Backend Registry - `src/app/domain/backend/registry.clj`

#### `fn-supports-arity?` function (lines 76-84)
```clojure
(defn- fn-supports-arity?
  "True if `f` has a declared invoke/doInvoke method for `arity`.

  This is used to support both legacy (2-arity) and newer (3-arity) domain route fns."
  [f arity]
  (some (fn [^java.lang.reflect.Method m]
          (and (#{"invoke" "doInvoke"} (.getName m))
            (= arity (count (.getParameterTypes m)))))
    (.getDeclaredMethods (class f))))
```

#### `call-user-api-route-fn` function (lines 86-100)
```clojure
(defn- call-user-api-route-fn
  "Invoke a domain `:user-api` route function with the right arity.

  Supported signatures:
  - (fn [db wrap-user-auth] ...)
  - (fn [db wrap-user-auth app-config] ...)
  - (fn [db wrap-user-auth & [app-config]] ...)

  Prefer passing `app-config` when supported."
  [route-fn db wrap-user-auth app-config]
  (cond
    (fn-supports-arity? route-fn 3) (route-fn db wrap-user-auth app-config)
    (fn-supports-arity? route-fn 2) (route-fn db wrap-user-auth)
    ;; Fallback: preserve previous behavior.
    :else (route-fn db wrap-user-auth app-config)))
```

- **Status:** Supports backward compatibility with old 2-arity route functions
- **Status (verified):** Compatibility is explicitly covered by `test/app/domain/backend/registry_test.clj`.
- **Risk:** MEDIUM - Removing this breaks tests and any future/legacy domains that still provide 2-arity route fns.
- **Action:** Keep until we intentionally drop legacy support. If/when removing:
  1) verify all manifests provide a 3-arity `:user-api` wrapper,
  2) update/remove the compatibility tests,
  3) update docs to require `(fn [db wrap-user-auth app-config] ...)` (where `app-config` may be nil).

---

## Category 4: Stub Implementations (LOW CONFIDENCE - Requires Feature Decision)

### Settings Handlers - `src/app/domain/backend/expenses/handlers/user_expenses/settings.clj`

Lines 22-46 contain stub implementations:
```clojure
;; ---------------------------------------------------------------------------
;; Settings handlers (stub implementation)
;; ---------------------------------------------------------------------------

(defn get-settings-handler
  "GET /api/v1/expenses/settings - fetch user settings.
   Currently returns default values until settings storage is implemented."
  [_db]
  (fn [request]
    ;; ... returns hardcoded defaults
    ))

(defn update-settings-handler
  "PUT /api/v1/expenses/settings - update user settings.
   Currently a no-op stub that returns the input."
  [_db]
  (fn [request]
    ;; ... no-op, just echoes input
    ))
```

**File header comment (lines 1-5):**
```
NOTE: Settings storage is currently stubbed - returns defaults.
TODO: Add user_expense_settings table or JSONB column to users table.
```

- **Status:** Documented stub with TODO for proper implementation
- **Risk:** LOW (feature works, just not persisted)
- **Action:** This is **intentional technical debt**, not unused code. Keep stub or implement the TODO.

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
| Legacy Function Aliases | 5 | ~40 | MEDIUM | ✅ Refactor completed for payers/suppliers/expense-items; others TBD |
| Legacy Arity Support | 1 | ~25 | MEDIUM | Keep (covered by tests); remove only as an intentional breaking change |
| Stub Implementations | 1 | ~50 | N/A | Keep or implement TODO |
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
1. Verify all domain routes use 3-arity
2. Simplify `call-user-api-route-fn` to direct call
3. Remove `fn-supports-arity?` if no longer needed

> Note: Phase 4 requires updating/removing the explicit compatibility tests.

### Phase 5: Decision Required (Stubs)
1. Decide: Keep stub as-is, implement settings storage, or document as intentional limitation

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
- Settings stub is documented with a TODO - this is intentional, not accidental

---

## Next Steps

1. Review this plan and confirm which categories to proceed with
2. Run verification searches to ensure no external callers will break
3. Execute changes phase-by-phase with testing after each phase
