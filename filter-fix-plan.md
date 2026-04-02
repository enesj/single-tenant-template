# Filter Fix Plan — Revised Before Implementation

Date: 2026-03-31
Source: `users-admins-list-view-filter-inventory.md` plus repo verification from the current codebase

---

## Summary

The original failures were real, but the branch is no longer at the original starting point. Live verification now shows that **parts of admin users and several tenant text filters are already fixed**, while the remaining work has shifted toward **server-mode initialization gaps** and **date/range filters that still need correct frontend serialization plus backend consumption on custom handlers**. The safest plan is therefore: treat already-landed text-filter fixes as done, finish the shared date/range path, then close the remaining custom-handler gaps with live browser checks after each slice.

One more live-browser finding matters for implementation: the shared list filter UI can emit **string-keyed filter maps** and select filters wrapped as **vectors of `{value,label}` items**. Any frontend request-param normalizer used in these flows has to flatten those real runtime shapes, not only the ideal keyword-keyed single-map case.

Update after the latest live sweep: the next real admin bugs were not generic text filtering at all. They were:
- `supplier-aliases` count/pagination drift, where the filtered row set changed but the total stayed unfiltered because the custom count wrapper dropped non-search filters.
- missing `:date-range-columns` wiring on several admin expenses routes (`article-aliases`, `categories`, `subcategories`, `cities`, `manufacturers`), which meant the browser sent `created-at-from/to` but the route never turned them into backend filters.

Those admin issues are now fixed and re-verified from the browser session. Several older inventory claims for admin text filters (`suppliers.normalized-key`, `stores.supplier`, `stores.normalized-key`, `supplier-aliases.supplier`) are now confirmed stale.

A final tenant owner-session sweep exposed the last two still-live bugs on this branch:
- `subcategories` created-at filtering used an unqualified `:created_at` column instead of the service alias `:sc.created_at`, which caused a 500.
- `article-aliases` created-at filtering was being sent correctly from the browser, but the tenant route was actually served by the supplier-detail handler, which was not parsing or forwarding `created-at-from/to` at all.

Those tenant issues are now fixed and re-verified via Chrome DevTools browser-context fetches. At this point the live sweep no longer reproduces any remaining filter bugs from the implementation plan, aside from inventory rows that are now stale and the still-untestable empty-state `/admin/unmapped-aliases` page.

---

## Implementation steps (ordered)

### 1. Finish bespoke admin list flows based on current branch state

- **Goal:** Close the remaining admin-specific gaps without redoing fixes that are already live.
- **Files:**
  - `src/app/admin/frontend/events/users/core.cljs`
  - `src/app/admin/frontend/events/admins.cljs`
  - `src/app/admin/frontend/adapters/admins.cljs`
- **What changes:**
  - Keep the already-landed flattened admin filter params.
  - Ensure `/admin/admins` runs in server-pagination mode with a real refresh event, so filters stop falling back to incorrect client-side filtering.
  - Serialize date filters to ISO strings before they go over the wire.
  - Normalize live shared-list filter payloads, including string-keyed maps and vector-wrapped select values, before building query params.
  - Keep pagination and sort behavior unchanged.
- **Dependencies:** Must match the backend param contract from step 2.
- **Owner:** `Coder`

### 2. Expand admin backend filter extraction and query support

- **Goal:** Bring the remaining admin users/admins backend behavior up to parity with the filter UI that already exists.
- **Files:**
  - `src/app/template/backend/routes/admin/utils.clj` *(only if a tiny shared instant parser is needed)*
  - `src/app/template/backend/routes/admin/users.clj`
  - `src/app/template/backend/routes/admin/admins.clj`
  - `src/app/admin/backend/services/admin/users.clj`
  - `src/app/admin/backend/services/admin/admins.clj`
- **What changes:**
  - `/admin/users`
    - Preserve the already-landed `status` enum cast and email/full-name support.
    - Verify `created-at`, `updated-at`, and `last-login-at` date ranges against live requests now that the frontend sends ISO timestamps.
    - Keep `email-verified` boolean handling intact.
  - `/admin/admins`
    - Preserve the existing `email`, `full-name`, `status`, and `role` support.
    - Verify `last-login-at` date range against the corrected frontend request path.
  - Prefer small route-level extraction + service clauses over a premature route-factory migration.
- **Dependencies:** Step 1 must send the same param names this step accepts.
- **Owner:** `Coder`

### 3. Align admin expenses declarative filter mappings with the existing UI

- **Goal:** Re-verify admin expenses pages after the branch’s recent tenant/admin work, then only patch the declarative mappings that are still genuinely broken.
- **Files:**
  - `src/app/domain/frontend/expenses/events/entity_configs.cljs`
  - `src/app/domain/backend/expenses/routes/route_configs.clj`
  - `src/app/domain/backend/expenses/services/service_configs/config_maps.clj` *(only where support is genuinely missing)*
- **What changes:**
  - Keep the declarative mappings that are already present.
  - Use Chrome DevTools to re-test the historically broken admin pages before editing these configs.
  - Patch only the still-failing mappings, especially around date/range affordances and any remaining `normalized-key` / joined-text mismatches.
- **Dependencies:** None beyond existing declarative route/service factory behavior.
- **Owner:** `Coder`

### 4. Tenant phase 1 — finish simple list handlers and shared date serialization

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
  - Preserve the already-landed text-filter flattening work.
  - Serialize date filters consistently to ISO strings across all duplicated tenant flatteners.
  - Support per-column text filters only where the backing service/handler can accept them without redesign.
  - Preserve tenant scoping rules already present in suppliers/stores.
  - Add missing date/range handling on custom “simple” handlers that still ignore the now-correct query params.
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
- Treat Chrome DevTools verification as required for each live bugfix slice so the plan does not drift from the running app.

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
- **Current branch state:** assume the inventory document is stale in both directions until the live UI is re-checked; some originally broken text filters already work, while some date filters only looked fixed until browser/network verification.
- **Shared helper extraction:** assume this is optional and should happen only if the bugfix diffs remain small and verified.
- **Admin route factory migration:** assume this is out of scope for the initial fix; the bespoke admin users/admins handlers should be corrected directly first.
