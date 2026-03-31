# Filter Fix Plan — Admin & Tenant List Views

Date: 2026-03-31
Source: `users-admins-list-view-filter-inventory.md` (2026-03-30 audit)

---

## Root Cause Analysis

The audit uncovered **four distinct categories of filter failure**, each with a different root cause. All stem from mismatches between what the frontend sends and what the backend accepts.

### Category A — Tenant-space filters universally broken (systemic)

**Affected:** Nearly all tenant-space pages (~15 pages, ~50+ filter columns)

**Root cause:** Two independent frontend event systems exist for fetching list data:

1. **Admin event factory** (`events/events_factory.cljs`): Uses `server-filter-keys` / `server-search-keys` config to map frontend filter field IDs → backend query param names. The `::load-list` event (line 150-194) reads filters from the UI state and transforms them using this mapping.

2. **Tenant/user event handlers** (`events/user_expenses/power_tools.cljs`): Uses `normalize-filter-params` (line 55-84) which forwards **all** filters from UI state as raw query params — but the **backend user handlers** (`handlers/user_articles.clj`, `user_categories.clj`, etc.) only extract `:search` (and sometimes `:unit`) and **ignore all other filter params**.

The admin event factory translates, e.g., `:canonical-name` → `:search` via `server-filter-keys`. The tenant event system sends the raw field ID (e.g., `:canonical-name`) as a query param. The backend user handler only looks for `:search`, so the filter is ignored.

**Files involved:**
- `src/app/domain/frontend/expenses/events/user_expenses/power_tools.cljs` — `normalize-filter-params`, `current-list-page-params`
- `src/app/domain/backend/expenses/handlers/user_*.clj` (all user handlers) — manual filter extraction

### Category B — Admin route filter-params whitelist too narrow

**Affected:** Admin-space pages where *some* columns work and others don't

| Page | Working | Failing |
|------|---------|---------|
| `/admin/articles` | artikal, proizvođač, kategorija, potkategorija | jedinica |
| `/admin/suppliers` | dobavljač | normalizovani ključ |
| `/admin/stores` | prodavnica | dobavljač, norm. ključ, adresa, grad |
| `/admin/supplier-aliases` | originalna oznaka | dobavljač, normalizovano |
| `/admin/categories` | kategorija | opis |
| `/admin/cities` | mjesto | norm. ključ, poštanski broj, država |
| `/admin/unmapped-aliases` | dobavljač, orig. oznaka | jedinica, ponavljanja |

**Root cause:** The admin route config (in `route_configs.clj`) only declares a subset of columns in `:filter-params`. For example:

- `article-config` has `:filter-params [:search :category-name :subcategory-name :manufacturer-display-name]` — missing `:unit`
- `supplier-config` has `:filter-params [:search]` — only `:search` maps to `display_name`; `:normalized-key` is not listed
- `store-config` has `:filter-params [:search]` — missing `:supplier-display-name`, `:normalized-key`, `:address`, `:city-name`
- `category-config` has `:filter-params [:search]` — `:search` maps to `:name` only; `:description` is not extracted
- `city-config` has `:filter-params [:search]` — missing `:normalized-key`, `:zip`, `:country`

Meanwhile, the **service factory** configs (`service_configs/config_maps.clj`) already define `text-filter-columns` that support per-column ILIKE filtering. The backend *can* filter by these columns — the route config just doesn't extract the params.

**Additionally:** The admin frontend `entity_configs.cljs` uses `server-filter-keys` / `server-search-keys` to map frontend field IDs to backend params. Where the frontend sends a field ID that *has* no `server-filter-keys` mapping, it's silently dropped (line 188-190 in `events_factory.cljs`).

**Files involved:**
- `src/app/domain/backend/expenses/routes/route_configs.clj` — `:filter-params` declarations
- `src/app/domain/frontend/expenses/events/entity_configs.cljs` — `server-filter-keys`
- `src/app/domain/backend/expenses/services/service_configs/config_maps.clj` — `text-filter-columns` (backend is ready)

### Category C — `/admin/users` filters return 500

**Affected:** `/admin/users` — Status, Created at, Updated at filters

**Root cause (Status 500):** The `build-user-list-filter-clauses` function (`admin/users.clj:30-37`) uses `[:= :u/status status]` without casting the value to the PostgreSQL enum type. The `users.status` column is a `user-status` enum. Without `(tc/cast-for-database :user-status status)`, PostgreSQL rejects the comparison with a type error.

Compare with the admins service (`admin/admins.clj:113`) which correctly uses `(tc/cast-for-database :admin-status status)`.

**Root cause (Created at / Updated at):** The users handler (`routes/admin/users.clj:19-21`) only extracts `:search`, `:status`, and `:email-verified`. Date range params like `:created-at-from` / `:created-at-to` are never extracted from the request. The handler also uses `:params` (merged Ring params) instead of `:query-params`, and has no date-range extraction logic.

**Additionally:** The frontend `table-columns.edn` marks these columns as filterable, and the filter UI sends the date-range params, but the backend simply ignores them.

**Files involved:**
- `src/app/admin/backend/services/admin/users.clj:30-37` — missing enum cast
- `src/app/template/backend/routes/admin/users.clj:16-21` — manual filter extraction, no date-range support

### Category D — Admin `last-login-at` date filter returns 0 results

**Affected:** `/admin/admins` — Last login at filter

**Root cause:** The admins handler (`routes/admin/admins.clj:31-33`) only extracts `:search`, `:status`, and `:role`. There is no date-range extraction for `last-login-at`. The frontend sends `last-login-at-from` / `last-login-at-to` params, but the backend ignores them completely, returning an unfiltered list. The count query also doesn't apply the filter, so it returns 0 (likely a different code path issue with how the count is computed when unknown params are present).

**Files involved:**
- `src/app/template/backend/routes/admin/admins.clj:29-33` — missing date-range extraction

---

## Fix Strategy

### Group 1: Backend route config alignment (Category B)

**Goal:** Align `:filter-params` in `route_configs.clj` with the `text-filter-columns` already supported by the service factory.

**Approach:** For each entity config, expand `:filter-params` to include all filterable columns that the service's `text-filter-columns` / `numeric-filter-columns` already support. This is a low-risk change because the service layer already handles these filters — we're just letting the route layer extract them.

**Specific changes in `src/app/domain/backend/expenses/routes/route_configs.clj`:**

| Entity config | Current `:filter-params` | Add |
|--------------|-------------------------|-----|
| `article-config` | `[:search :category-name :subcategory-name :manufacturer-display-name]` | `:unit` |
| `supplier-config` | `[:search]` | (none needed — `normalized_key` is auto-generated, not a useful filter target. But the table shows it, so consider adding `:normalized-key` or removing the column filter UI) |
| `store-config` | `[:search]` | `:supplier-display-name`, `:normalized-key`, `:address`, `:city-name` |
| `category-config` | `[:search]` | `:description` (needs `text-filter-columns` added to service config too) |
| `city-config` | `[:search]` | `:normalized-key`, `:zip`, `:country` |
| `manufacturer-config` | `[:search]` | `:normalized-key` |
| `subcategory-config` | `[:search :category-name]` | `:description` (needs `text-filter-columns` in service config) |
| `supplier-alias-config` | `{:supplier-id :uuid, :unmapped-only :boolean, :search :string}` | `:supplier-display-name`, `:raw-label-normalized` |

**Service config changes in `service_configs/config_maps.clj`:**

Some entities need `text-filter-columns` added to their service config:

| Service config | Add to `text-filter-columns` |
|---------------|------------------------------|
| `supplier-config` | `{:normalized-key :normalized_key}` |
| `category-config` | `{:name :name, :description :description}` |
| `city-config` | `{:name :name, :normalized-key :normalized_key, :zip :zip, :country :country}` |
| `manufacturer-config` | `{:normalized-key :normalized_key}` |
| `store-config` | `{:supplier-display-name :s.display_name, :normalized-key :st.normalized_key, :address :st.address, :city-name :c.name}` |
| `subcategory-config` | Add `:description :sc.description` to existing `text-filter-columns` |
| `supplier-alias-config` | `{:supplier-display-name :s.display_name, :raw-label-normalized :sa.raw_label_normalized}` (check table alias) |

**Frontend config changes in `entity_configs.cljs`:**

Expand `server-filter-keys` for entities that currently only use `server-search-keys`:

| Entity config | Change |
|--------------|--------|
| `suppliers-config` | Add `server-filter-keys` with `:normalized-key` → `:normalized-key` mapping |
| `stores-config` | Replace `server-search-keys` with `server-filter-keys` including all new columns |
| `categories-config` | Replace `server-search-keys` with `server-filter-keys` with `:name` → `:search`, `:description` → `:description` |
| `cities-config` | Replace `server-search-keys` with `server-filter-keys` including all columns |
| `manufacturers-config` | Replace `server-search-keys` with `server-filter-keys` including `:normalized-key` |
| `subcategories-config` | Add `:description` → `:description` to existing `server-filter-keys` |
| `unmapped-aliases-config` | Add `:unit` → `:unit`, `:occurrence-count` range mapping |

### Group 2: Fix `/admin/users` 500 errors (Category C)

**Step 1 — Enum cast fix:** In `src/app/admin/backend/services/admin/users.clj`, line 36, change:
```clojure
status (conj [:= :u/status status])
```
to:
```clojure
status (conj [:= :u/status (tc/cast-for-database :user-status status)])
```

**Step 2 — Date-range support:** Add date-range filter extraction to the users handler (`routes/admin/users.clj`). Two approaches:

- **Quick fix:** Add manual date-range extraction for `:created-at` and `:updated-at` in the handler, then pass to the service.
- **Better fix:** Refactor the users handler to use the routes factory pattern (like domain entities), which handles date-range extraction generically. This is more work but eliminates the maintenance burden.

**Step 3 — Missing filter columns:** The users page has no filter UI for `Email`, `Full name`, or `Auth provider`. Either:
- Add these to `filterable-columns` in `table-columns.edn` and add corresponding backend support
- Or accept the current state (these columns may be intentionally excluded)

### Group 3: Fix `/admin/admins` last-login-at filter (Category D)

Add date-range extraction for `last-login-at` to the admins handler. Same two approaches as Group 2:

- **Quick fix:** Extract `last-login-at-from` / `last-login-at-to` from params, parse to Instant, add WHERE clause `[:>= :a/last_login_at from]` / `[:<= :a/last_login_at to]`.
- **Better fix:** Refactor to use routes factory.

### Group 4: Fix tenant-space filters (Category A) — largest impact

This is the biggest group. There are two viable strategies:

#### Strategy A: Align user handlers with admin route factory pattern

Refactor each user handler to use the routes factory or replicate its filter extraction logic. This means each handler would extract per-column filter params using a config map, similar to how `routes_factory.clj` does it.

**Pros:** Consistent with admin pattern. Full filter support. Minimal frontend changes.
**Cons:** Large refactor (~15 handler files). Risk of introducing regressions.

#### Strategy B: Make tenant event system use filter-key-map (like admin events)

Add `server-filter-keys` configuration to the tenant-space event handlers in `power_tools.cljs`, similar to how the admin `events_factory.cljs` maps field IDs to backend params. Then update user handlers to extract the mapped param names.

**Pros:** Smaller frontend change. Can be done incrementally.
**Cons:** Still requires backend handler changes. Two event systems with similar-but-different config.

#### Strategy C (recommended): Unify admin and tenant list fetch via shared factory

Create a **shared `load-list` event factory** that both admin and tenant pages use. The factory accepts a config map (including `filter-key-map`, `api-endpoint`, `base-path`) and generates the `::load-list` event with proper filter mapping. Tenant pages would switch from `power_tools.cljs` refresh events to factory-generated events.

On the backend, refactor user handlers to **delegate to the same service factory** that admin routes use, adding tenant-scoping. Since the services already support tenant-scoped queries, this is mostly a routing/handler change.

**Pros:** Single source of truth. Eliminates divergence. Future filter additions automatically work in both spaces.
**Cons:** Largest up-front effort. Requires careful migration.

#### Recommended implementation order for Group 4:

1. **Phase 1 — Backend:** For each user handler, add per-column filter param extraction matching the admin route config's `:filter-params`. This can be done entity-by-entity. Start with the highest-traffic pages (articles, categories, cities).

2. **Phase 2 — Frontend:** Update `power_tools.cljs` `normalize-filter-params` to optionally apply a `filter-key-map` per entity type, so field IDs get translated to the backend param names the handlers now expect.

3. **Phase 3 — Consolidation:** Gradually migrate tenant pages to use the admin event factory (or a shared factory) and eliminate the separate `power_tools.cljs` refresh pattern.

---

## Affected Files Summary

### Backend (route configs & route factory)
- `src/app/domain/backend/expenses/routes/route_configs.clj` — expand `:filter-params`
- `src/app/domain/backend/expenses/services/service_configs/config_maps.clj` — add `text-filter-columns` where missing

### Backend (user handlers — tenant-space)
- `src/app/domain/backend/expenses/handlers/user_articles.clj`
- `src/app/domain/backend/expenses/handlers/user_categories.clj`
- `src/app/domain/backend/expenses/handlers/user_cities.clj`
- `src/app/domain/backend/expenses/handlers/user_subcategories.clj`
- `src/app/domain/backend/expenses/handlers/user_manufacturers.clj`
- `src/app/domain/backend/expenses/handlers/user_stores.clj`
- `src/app/domain/backend/expenses/handlers/user_store_aliases.clj`
- `src/app/domain/backend/expenses/handlers/user_expense_categories.clj`
- `src/app/domain/backend/expenses/handlers/user_receipts.clj`
- `src/app/domain/backend/expenses/handlers/user_expenses/expense_items.clj` (sub-namespace)
- `src/app/domain/backend/expenses/handlers/user_expenses/crud.clj`
- `src/app/domain/backend/expenses/handlers/user_expenses/supplier_aliases.clj`
- `src/app/domain/backend/expenses/handlers/user_expenses/article_aliases.clj`

### Backend (admin template handlers)
- `src/app/admin/backend/services/admin/users.clj` — enum cast fix + date-range support
- `src/app/template/backend/routes/admin/users.clj` — date-range extraction
- `src/app/template/backend/routes/admin/admins.clj` — date-range extraction for last-login-at

### Frontend (admin event configs)
- `src/app/domain/frontend/expenses/events/entity_configs.cljs` — expand `server-filter-keys`

### Frontend (tenant event handlers)
- `src/app/domain/frontend/expenses/events/user_expenses/power_tools.cljs` — add filter-key-map support

---

## Execution Priority

| Priority | Group | Impact | Effort | Description |
|----------|-------|--------|--------|-------------|
| P0 | Group 2 (step 1) | Fix 500 errors | Small | Enum cast in users service |
| P1 | Group 1 | ~20 admin filter columns | Medium | Route config + service config alignment |
| P2 | Group 2 (step 2) + Group 3 | ~5 date filter columns | Medium | Date-range support in users/admins handlers |
| P3 | Group 4 (phase 1-2) | ~50 tenant filter columns | Large | Backend user handler + frontend filter-map |

---

## Verification Plan

After each group, re-run the one-filter-at-a-time audit from the inventory document on the affected pages. Specifically:

- **Group 1:** Re-test all "Does not work" admin-space text filters
- **Group 2:** Re-test `/admin/users` Status, Created at, Updated at
- **Group 3:** Re-test `/admin/admins` Last login at
- **Group 4:** Re-test all tenant-space pages that currently show "Does not work"

For "Needs targeted probe" items (date pickers, confidence sliders, etc.), those should be tested as part of Group 2/3 and the date-range portions of Group 1/4.
