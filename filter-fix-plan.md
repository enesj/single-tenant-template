# Filter Fix Plan — Revised Before Implementation

Date: 2026-03-31
Source: `users-admins-list-view-filter-inventory.md` plus repo verification from the current codebase

---

## Summary

The filter failures are real, but the implementation should **not** start with a broad refactor. The verified repo shape shows three distinct buckets: **bespoke admin users/admins flows that need explicit frontend and backend request shaping**, **admin expenses pages that are mostly declarative and only need config alignment**, and **tenant-space pages that should be handled in two phases, starting with simple list handlers only**. The safest plan is therefore: fix admin users/admins first, align admin expenses configs second, then tackle tenant simple handlers, while deferring alias/detail/custom tenant endpoints until the first pass is stable.

---

## Implementation steps (ordered)

### 1. Fix admin `/admin/users` and `/admin/admins` frontend request shaping

- **Goal:** Make the bespoke admin loaders flatten shared list UI filters into backend query params instead of forwarding nested maps directly.
- **Files:**
  - `src/app/admin/frontend/events/users/core.cljs`
  - `src/app/admin/frontend/events/admins.cljs`
- **What changes:**
  - Flatten date ranges to `<field>-from` / `<field>-to`.
  - Preserve scalar filters like `status`, `role`, `email`, `full-name`, and `email-verified`.
  - Keep pagination and sort behavior unchanged.
  - Do **not** extract a shared helper yet unless the diff stays very small after the bespoke fixes are done.
- **Dependencies:** Must match the backend param contract from step 2.
- **Owner:** `Coder`

### 2. Expand admin backend filter extraction and query support

- **Goal:** Bring the admin users/admins backend up to parity with the filter UI that already exists.
- **Files:**
  - `src/app/template/backend/routes/admin/utils.clj` *(only if a tiny shared instant parser is needed)*
  - `src/app/template/backend/routes/admin/users.clj`
  - `src/app/template/backend/routes/admin/admins.clj`
  - `src/app/admin/backend/services/admin/users.clj`
  - `src/app/admin/backend/services/admin/admins.clj`
- **What changes:**
  - `/admin/users`
    - Cast `status` to the PostgreSQL `user-status` enum.
    - Support `created-at`, `updated-at`, and `last-login-at` date ranges.
    - Keep `email-verified` boolean handling intact.
  - `/admin/admins`
    - Support per-column filters for `email` and `full-name`.
    - Support `last-login-at` date range.
    - Keep existing `status` and `role` filters.
  - Prefer small route-level extraction + service clauses over a premature route-factory migration.
- **Dependencies:** Step 1 must send the same param names this step accepts.
- **Owner:** `Coder`

### 3. Align admin expenses declarative filter mappings with the existing UI

- **Goal:** Fix admin expenses pages by aligning declarative frontend/backend configs where the service layer already supports the filters.
- **Files:**
  - `src/app/domain/frontend/expenses/events/entity_configs.cljs`
  - `src/app/domain/backend/expenses/routes/route_configs.clj`
  - `src/app/domain/backend/expenses/services/service_configs/config_maps.clj` *(only where support is genuinely missing)*
- **What changes:**
  - Add or correct mappings for already exposed admin filters, especially:
    - `articles`: `unit`
    - `stores`: `supplier-display-name`, `normalized-key`, `address`, `city-name`
    - `categories`: `description`
    - `cities`: `normalized-key`, `zip`, `country`
    - `manufacturers`: `normalized-key`
    - `subcategories`: `description`
    - `supplier-aliases`: `supplier-display-name`, `raw-label-normalized`
    - `unmapped-aliases`: keep this scope tight to `supplier`, `raw-label`, `raw-label-normalized`, `unit`, and occurrence-count range only
  - Explicitly resolve the `suppliers.normalized-key` gap as a **real bugfix decision**, not as an incidental refactor.
- **Dependencies:** None beyond existing declarative route/service factory behavior.
- **Owner:** `Coder`

### 4. Tenant phase 1 — simple list handlers only

- **Goal:** Restore tenant-space filtering incrementally by starting with the simpler, lower-risk list handlers and matching frontend request shaping to the backend handlers they actually call.
- **Files:**
  - Frontend:
    - `src/app/domain/frontend/expenses/events/user_expenses/lookups.cljs`
    - `src/app/domain/frontend/expenses/events/user_expenses/categories.cljs`
    - `src/app/domain/frontend/expenses/events/user_expenses/cities.cljs`
    - `src/app/domain/frontend/expenses/events/user_expenses/manufacturers.cljs`
    - `src/app/domain/frontend/expenses/events/user_expenses/subcategories.cljs`
    - `src/app/domain/frontend/expenses/events/user_expenses/stores.cljs`
  - Backend:
    - `src/app/domain/backend/expenses/handlers/user_expenses/reference_data.clj`
    - `src/app/domain/backend/expenses/handlers/user_categories.clj`
    - `src/app/domain/backend/expenses/handlers/user_cities.clj`
    - `src/app/domain/backend/expenses/handlers/user_manufacturers.clj`
    - `src/app/domain/backend/expenses/handlers/user_subcategories.clj`
    - `src/app/domain/backend/expenses/handlers/user_stores.clj`
- **What changes:**
  - Flatten tenant filters consistently.
  - Support per-column text filters only where the backing service/handler can accept them without redesign.
  - Preserve tenant scoping rules already present in suppliers/stores.
  - Avoid touching alias/detail/custom endpoints in this first tenant pass.
- **Dependencies:** Should reuse the same flattening rules proven in step 1, but without introducing a large shared abstraction up front.
- **Owner:** `Coder`

### 5. Tenant phase 2 — alias/detail/custom endpoints only if still failing after phase 1

- **Goal:** Handle the more custom tenant endpoints only after simple list behavior is verified.
- **Files:**
  - `src/app/domain/frontend/expenses/events/user_expenses/power_tools.cljs`
  - `src/app/domain/frontend/expenses/events/user_expenses/unmapped_aliases.cljs`
  - `src/app/domain/backend/expenses/handlers/user-articles.clj`
  - `src/app/domain/backend/expenses/handlers/user-store-aliases.clj`
  - `src/app/domain/backend/expenses/handlers/user_expenses/supplier_aliases.clj`
  - `src/app/domain/backend/expenses/handlers/user_expenses/supplier_detail.clj`
- **What changes:**
  - Only implement filters that the current services already support or can accept with a tiny handler change.
  - Keep `unmapped-aliases` intentionally narrow: no broad genericization beyond existing `supplier-id`, `unit`, text filters, and occurrence-count range.
  - Treat these endpoints as a second pass, not part of the initial “fix everything” sweep.
- **Dependencies:** Step 4 should already validate the tenant-side flattening pattern.
- **Owner:** `Coder`

### 6. Optional consolidation after behavior is stable

- **Goal:** If the first passes succeed cleanly, consider extracting a small shared frontend request-param helper to remove duplicated flattening logic.
- **Files:**
  - Possible shared frontend helper namespace(s)
  - The event files touched in steps 1 and 4
- **What changes:**
  - Consolidate only after tests prove the behavior.
  - Keep this optional and separate from the bugfix itself.
- **Dependencies:** All prior steps complete and validated.
- **Owner:** `Designer`

---

## Edge cases

- **Happy path:**
  - Text filter matches rows on admin users/admins.
  - Date range filters return expected narrowed results.
  - Tenant simple lists filter on the columns already exposed in the UI.
- **`nil` / cleared filters:**
  - Cleared filters should be omitted from request params entirely.
  - `false` booleans like `email-verified=false` or `unmapped-only=false` must not be dropped accidentally.
- **Empty input / empty collections:**
  - Empty strings should not become active filters.
  - Empty result sets should still return valid pagination metadata.
- **Invalid / boundary input:**
  - Invalid UUID filter values should not cause 500s.
  - One-sided date ranges (`from` only or `to` only) must work.
  - Invalid date strings must be handled consistently (see assumptions below).
  - Unsupported tenant filters should be ignored rather than silently mis-mapped to unrelated params.
- **Sorting/pagination interplay:**
  - Filter updates must preserve the existing sort contract.
  - Count and list queries must apply the same filters so pagination totals stay correct.

---

## Validation plan

- **Minimum validation requirement:** Use focused tests and/or REPL checks after each step; no broad suite runs.

### Focused tests

- Backend admin route coverage:
  - `test/app/backend/routes/admin/users_test.clj`
  - `test/app/backend/routes/admin/admins_test.clj`
- Add focused assertions for:
  - date-range params being extracted and forwarded correctly
  - `email` / `full-name` admin filters
  - `email-verified=false` preservation
  - enum-cast path for user status

### REPL / focused behavior checks

- If route tests are insufficient for tenant handlers, validate the smallest meaningful handler/service path via REPL.
- For any command-based validation, save output once under `tmp/` and do not re-run just to grep.

### Suggested execution order for verification

1. Validate admin users/admins route tests first.
2. Validate one admin expenses entity from each mapping pattern:
   - one plain text entity
   - one date-range entity
   - one alias/range entity
3. Validate tenant phase 1 entities before touching any phase 2 custom endpoint.

### Non-goals during validation

- No schema work.
- No migrations.
- No broad shared event-factory refactor in the same pass.

---

## Open questions / assumptions

- **Supplier normalized-key on admin suppliers:** this should be treated as a real bugfix choice. The current recommendation is to implement the backend/frontend mapping rather than quietly leaving a broken filter in the UI.
- **Invalid date params:** recommendation is to follow the repo’s lenient parsing style and ignore invalid values rather than returning a 400, unless product requirements say otherwise.
- **Tenant alias/detail pages:** assume these can be deferred to phase 2 unless the user explicitly wants them included in the first pass.
- **Shared helper extraction:** assume this is optional and should happen only if the bugfix diffs remain small and verified.
- **Admin route factory migration:** assume this is out of scope for the initial fix; the bespoke admin users/admins handlers should be corrected directly first.
