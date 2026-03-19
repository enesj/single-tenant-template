# Settings Hierarchy Implementation Plan

**Spec**: `specs/allium/domain/expenses/settings-hierarchy.candidate.allium`
**Status**: Phase 1–6 complete
**Created**: 2026-03-19
**Last updated**: 2026-03-19

## Overview

Restructure the monolithic `/expenses/settings` page into a three-tier settings hierarchy:

| Tier | Owner | Storage | Surface |
|------|-------|---------|---------|
| **Global** | Platform admin | `global_settings` table (singleton) | Admin panel section |
| **Tenant** | Tenant owner | `tenant_settings` table (per-tenant) | Owner profile section |
| **Per-user** | Each user | `user_expense_settings` table (slimmed) | User profile page `/profile` |

Also introduces: multi-currency support (manual expenses), daily CBBH exchange rates, and currency mismatch detection on receipts.

---

## Phase 1: Database & Backend Foundation  ✓ COMPLETE

### 1.1 New DB tables + migrations  ✓
- [x] Create `global_settings` table (singleton):
  - `id`, `default_currency` (varchar 3, default "BAM"), `default_note` (text, nullable),
    `auto_publish_after_upload` (boolean, default false), `ai_receipt_enhancement` (boolean, default false),
    `created_at`, `updated_at`
- [x] Create `enabled_currencies` table:
  - `id`, `code` (varchar 3, unique), `name` (varchar 100), `is_base` (boolean, default false),
    `created_at`, `updated_at`
- [x] Create `daily_exchange_rates` table:
  - `id`, `currency_code` (varchar 3), `rate_date` (date), `rate` (decimal 18,8),
    `fetched_at` (timestamptz), `is_fallback` (boolean, default false),
    `created_at`
  - Unique index: `(currency_code, rate_date)`
- [x] Create `exchange_rate_fetch_alerts` table:
  - `id`, `rate_date` (date), `error_message` (text), `fallback_source_date` (date),
    `created_at` (timestamptz), `acknowledged` (boolean, default false)
- [x] Create `tenant_settings` table:
  - `id`, `tenant_id` (FK tenants, unique), `email_notifications` (boolean, default true),
    `created_at`, `updated_at`
- [x] Add currency fields to `expenses` table:
  - `original_amount` (decimal 12,2), `bam_amount` (decimal 12,2),
    `exchange_rate` (decimal 18,8, nullable), `rate_fetched_at` (timestamptz, nullable)
  - Backfill: `original_amount = total_amount`, `bam_amount = total_amount`, `exchange_rate = null`
    (all existing expenses are BAM)
- [x] Expand `currency` enum: `["BAM" "EUR" "USD" "GBP" "CHF" "HRK" "RSD" "TRY"]`
- [x] Seed migration: insert singleton `global_settings` row + seed `enabled_currencies`
  (BAM, EUR, USD, GBP, CHF, HRK, RSD, TRY)
- [x] Seed migration: create `tenant_settings` row for each existing tenant
- [x] Apply migrations to dev + test DBs (0045_schema.edn + 0046_seed_settings_hierarchy.sql)

### 1.2 Remove columns from user_expense_settings  ✓ COMPLETE (finished in Phase 5.3)
- [x] Remove from `user_expense_settings`: `default_currency`, `default_note`,
  `auto_post_after_upload_enabled`, `receipt_refine_enabled`, `notifications_enabled`
- [x] Keep: `user_id`, `tenant_id`, `default_payer_id`, `default_expense_category_id`,
  `receipt_ocr_provider` (internal, not user-facing)
- [x] Migration to drop columns (0047_schema.edn, applied to dev + test DBs)

---

## Phase 2: Backend Services & API  ✓ COMPLETE (minor items deferred)

### 2.1 Global settings service  ✓
- [x] New file: `src/app/domain/backend/expenses/services/global_settings.clj`
  - `get-global-settings` (singleton read)
  - `update-global-settings!` (partial update)
  - `get-enabled-currencies` (list all)
  - `add-enabled-currency!`, `remove-enabled-currency!`
- [x] Validation: default_currency must be in enabled list, BAM cannot be removed

### 2.2 Daily exchange rate service  ✓
- [x] New file: `src/app/domain/backend/expenses/services/exchange_rates.clj`
  - `get-daily-rates` (for a date)
  - `fetch-and-cache-daily-rates!` (CBBH HTML primary → Serper fallback → DB fallback)
  - `ensure-daily-rates!` (idempotent: fetch only if missing for today)
  - `get-rate-for-currency` (lookup from cache)
  - `get-fallback-rates` (most recent cached)
  - `create-fetch-alert!`, `acknowledge-alert!`
- [x] CBBH HTML direct fetch (primary) + Serper API search (fallback)

### 2.3 Tenant settings service  ✓
- [x] New file: `src/app/domain/backend/expenses/services/tenant_settings.clj`
  - `get-tenant-settings`, `update-tenant-settings!`
  - `provision-tenant-settings!` (called from tenant provisioning)

### 2.4 Update user_expense_settings service  ✓
- [x] Modify: `src/app/domain/backend/expenses/services/user_expense_settings.clj`
  - Added: `per-user-defaults`, `effective-settings-with-global`, `update-user-default-category!`
  - Later slimmed the service back down after Phase 5.3 dropped deprecated columns
  - `effective-settings-with-global` merges global + per-user settings

### 2.5 Expense creation — currency conversion  ✓ (receipt side deferred)
- [x] Modify expense creation handlers to:
  - Accept `currency` from frontend (manual expenses)
  - Call `ensure-daily-rates!` for non-BAM currencies
  - Calculate `bam_amount = original_amount * rate` (HALF_UP rounding to 2dp)
  - Store `exchange_rate` and `rate_fetched_at`
- [x] BAM expenses: `original_amount = bam_amount = total_amount`, no rate
- [x] Low-level `create-expense!` allowlist updated for new columns
- [ ] Receipt-originated expenses: always set `currency = "BAM"`, `bam_amount = total_amount`
  *(deferred — receipt auto-post already forces BAM via `build-review-data`, not urgent)*

### 2.6 Receipt OCR — currency mismatch detection  ✓
- [x] Modify `auto-approve-extracted-receipt!`:
  - After OCR, if `currency_guess` is not "BAM" and not "KM":
    - Set `status = review_required`
    - Set `error_message` with clear currency mismatch warning
  - Currency mismatch check is first in the cond chain (blocks auto-post)
- [x] Fixed pre-existing bug: `resolve-payer-id` now passes `tenant_id` to `get-user-expense-settings`

### 2.7 Admin API routes for global settings  ✓
- [x] New admin routes (under `/admin/api/expenses/`):
  - GET/PUT `/global-settings`
  - GET/POST `/enabled-currencies`, DELETE `/enabled-currencies/:code`
  - GET `/daily-exchange-rates`, POST `/daily-exchange-rates/fetch`
  - GET `/exchange-rate-alerts`, PUT `/exchange-rate-alerts/:id/acknowledge`
- [x] Handler: `src/app/domain/backend/expenses/handlers/admin_global_settings.clj`
- [x] Routes: `src/app/domain/backend/expenses/routes/global_settings.clj`
- [x] Admin route splicing: `core.clj` uses `(into [...] (global-settings/routes db app-config))`

### 2.8 User API routes — profile & tenant settings  ✓ COMPLETE
- [x] New user API routes:
  - GET `/api/v1/profile` (user info + effective settings + currencies + tenant settings for owner)
  - PUT `/api/v1/profile/defaults` (update default_expense_category_id)
  - PUT `/api/v1/tenant/settings` (owner/admin: email_notifications)
  - PUT `/api/v1/tenant/name` (owner: workspace name)
- [x] Handler: `src/app/domain/backend/expenses/handlers/user_expenses/profile.clj`
- [x] Profile routes mounted at template API level via `requiring-resolve` (avoids compile-time coupling)
- [x] Tenant routes mounted in `src/app/template/backend/routes/tenant.clj` via `requiring-resolve`
- [x] Tenant name: `update-tenant!` added to `services/tenant.clj`
- [x] Deprecate old routes:
  - GET/PUT `/api/v1/expenses/settings` removed
  - GET `/api/v1/expenses/export` moved to GET `/api/v1/profile/export`
  - DELETE `/api/v1/expenses/all` moved to DELETE `/api/v1/profile/all`

### 2.9 Tenant provisioning integration  ✓ (startup seed deferred)
- [x] Modify `provision-tenant!` to create `tenant_settings` row (step 4c, ON CONFLICT DO NOTHING)
- [ ] Ensure `global_settings` singleton exists at app startup
  *(low priority — seed migration already creates it; only needed for fresh DBs without running migrations)*

---

## Phase 3: Frontend — User Profile Page  ✓ COMPLETE

### 3.1 Profile page component
- [x] New file: `src/app/domain/frontend/expenses/pages/user/profile.cljs`
  - Account info section (email, name — read-only)
  - Per-user defaults section:
    - Default payer (read-only display, sticky from last-used payer)
    - Default category (dropdown, editable → PUT `/api/v1/profile/defaults`)
  - Owner/admin section (conditionally rendered via `:user-role` sub):
    - Workspace name (editable text field → PUT `/api/v1/tenant/name`)
    - Email notifications (toggle → PUT `/api/v1/tenant/settings`)
    - Export all expenses (button → existing CSV download dispatch)
    - Delete all expenses (button → typed "DELETE" confirmation dialog)

### 3.2 Profile events & subscriptions
- [x] New file: `src/app/domain/frontend/expenses/events/user_expenses/profile.cljs`
  - `:profile/fetch` → GET `/api/v1/profile` (returns `{:data {:user ... :settings ... :enabled-currencies ... :tenant-settings ...}}`)
  - `:profile/update-defaults` → PUT `/api/v1/profile/defaults`
  - `:profile/update-tenant-settings` → PUT `/api/v1/tenant/settings`
  - `:profile/update-tenant-name` → PUT `/api/v1/tenant/name`
  - `:profile/export` → reuse existing `[:user-expenses/export {:format :csv :all true}]`
  - `:profile/delete-all` → reuse existing `[:user-expenses/delete-all "DELETE_ALL_EXPENSES"]`
- [x] Subscriptions: `:profile/data`, `:profile/loading?`, `:profile/saving?`

### 3.3 Profile route wiring (7 files to touch)
- [x] `src/app/domain/shared/routes/expenses_user.cljc` — add descriptor `{:id :user-profile :path "/profile" :spa-fallback? true}`
- [x] `src/app/domain/frontend/expenses/routes/user.cljs` — add `:user-profile` route options with `controllers/user-guarded-start :page/init-user-profile`
- [x] `src/app/domain/frontend/expenses/pages.cljs` — add `:user-profile profile-page` to pages map + require
- [x] `src/app/template/frontend/events/routing.cljs` — add `:page/init-user-profile` event (set page + dispatch `:profile/fetch`)
- [x] `src/app/template/frontend/components/settings/global_settings.cljs` — change "Expenses Settings" link to "Profile" → `/profile`
- [x] `src/app/template/frontend/i18n.cljs` — add `profile/*` BS/EN translations

### 3.4 i18n translations
- [x] Add BS/EN translations for profile page labels under `profile/*` namespace

**Implementation notes (lessons learned)**:
- Profile routes live at `/api/v1/profile` (template level, not under `/expenses`) — use `requiring-resolve` pattern
- Settings panel gear icon (top-right header) currently links to `/expenses/settings` — update to `/profile`
- Sidebar does NOT have a settings nav item; the link is in the header settings panel dropdown
- Use `xhrio` helper from `events/user_expenses/xhrio.cljs` for HTTP requests
- Endpoints go in `endpoints.cljs` using `(api/versioned-endpoint "/profile")` etc.
- Follow UIx patterns from `expense_settings.cljs`: `defui`, `use-state`, `use-effect`, `use-subscribe`, `use-t`

---

## Phase 4: Frontend — Admin Global Settings  ✓ COMPLETE

### 4.1 Admin global settings panel
- [x] New admin page/section for global settings:
  - Default currency (autocomplete from enabled list)
  - Default note (text area)
  - Auto-publish toggle
  - AI enhancement toggle
  - Enabled currencies management (add/remove, BAM locked)
  - Today's exchange rates (read-only table)
  - Rate fetch alerts (warning banner, acknowledge button)

### 4.2 Admin events & API integration
- [x] Events for: fetch/update global settings, manage currencies, view rates, acknowledge alerts

**Files to modify/create**:
- Admin frontend components (new page or section in admin SPA) — complete
- Admin API integration events — complete

---

## Phase 5: Remove Old Settings Page  ✓ COMPLETE

**Prerequisite**: Phase 3 must be live (profile page replaces settings page).

### 5.1 Remove /expenses/settings frontend
- [x] Delete: `src/app/domain/frontend/expenses/pages/user/expense_settings.cljs`
- [x] Remove: settings events from `events/user_expenses/settings.cljs`
- [x] Remove: endpoint `settings-endpoint` from `events/user_expenses/endpoints.cljs`
- [x] Remove: `:expense-settings` route from `routes/user.cljs` and `routes/expenses_user.cljc`
- [x] Remove: page init event `:page/init-expense-settings` from `routing.cljs`
- [x] Remove: all `expense-settings/*` i18n keys from `i18n.cljs` (both `:bs` and `:en`)
- [x] Remove: `expense-settings-page` from `pages.cljs` require + pages map

### 5.2 Remove old backend routes
- [x] Remove: GET/PUT `/api/v1/expenses/settings` handler + route registration
- [x] Remove or repurpose: `handlers/user_expenses/settings.clj`
- [x] Remove: `/expenses/export` and `/expenses/all` routes from `user_api.clj`
  (export/delete-all move to profile page, hitting same backend services)

### 5.3 Drop deprecated columns (was Phase 1.2)
- [x] Migration: drop `default_currency`, `default_note`, `notifications_enabled`,
  `auto_post_after_upload_enabled`, `receipt_refine_enabled` from `user_expense_settings`
- [x] Clean up service code that referenced these columns
- [x] Remove `^:deprecated allowed-currencies` from `user_expense_settings.clj`

### 5.4 Clean up tests
- [x] Update: `test/app/domain/backend/expenses/handlers/user_expenses/settings_test.clj`
  (deleted after removing the deprecated endpoint; replacement coverage added for the new handlers)
- [x] Add tests for: global settings API, tenant settings API, profile API,
  exchange rate service, currency mismatch detection

---

## Phase 6: Exchange Rate Integration  ✓ COMPLETE

### 6.1 Serper API integration  ✓ (done in Phase 2.2)
- [x] CBBH HTML direct fetch (primary source)
- [x] Serper API search (fallback source)
- [x] Store daily rates in `daily_exchange_rates`
- [x] Fallback logic: use latest cached rates + create `exchange_rate_fetch_alerts`

### 6.2 Manual expense currency conversion — backend  ✓ (done in Phase 2.5)
- [x] Backend: validate currency, fetch rate via `ensure-daily-rates!`, calculate BAM amount
- [x] `create-user-expense!` handles multi-arity for optional `app-config`
- [x] Frontend: currency dropdown in Smart Input and user expense forms now follows enabled currencies from profile/global settings
- [x] Expense detail view: show conversion breakdown for non-BAM expenses

### 6.3 Receipt currency mismatch  ✓ COMPLETE
- [x] Backend: detect non-BAM/KM currency in OCR extraction (done in Phase 2.6)
- [x] Set receipt to `review_required` with clear error message
- [x] Frontend: shared receipt review modal surfaces the warning banner / error message for review-required receipts

---

## Dependencies & Order

```
Phase 1 (DB) ──→ Phase 2 (Backend) ──→ Phase 3 (Profile FE) ──→ Phase 5 (Cleanup)
       ✓                ✓                    ✓                        │
                      │                                               ↑
                      └──→ Phase 4 (Admin FE) ────────────────────────┘
                      │
                      └──→ Phase 6 (FE only — backend done in Phase 2)
```

- Phase 1: COMPLETE
- Phase 2: COMPLETE
- Phase 3: COMPLETE
- Phase 4: COMPLETE
- Phase 5: COMPLETE
- Phase 6: COMPLETE

---

## Implementation Lessons Learned

### Route architecture
- **Profile routes live at template level**, not under `/expenses`. They use `requiring-resolve`
  to lazy-load domain handlers, avoiding compile-time template→domain coupling.
- **Tenant settings/name routes** extend existing `tenant.clj` routes in the template layer.
- **Admin global settings routes** use `(into [...] (routes ...))` splicing because
  `global-settings/routes` returns a vector of route vectors, not a single nested route.
  Reitit would misinterpret the nested structure without splicing.

### Domain→template boundary
- `requiring-resolve` is the pattern for template code calling domain services/handlers.
  This avoids `:require` at compile time and keeps the template domain-agnostic.
- The `registry.clj` passes `(:config service-container)` to admin routes so domain
  services can access app-config (Serper API key, etc.) without DI coupling.

### Currency conversion
- Conversion logic lives in `create-user-expense!` (user-facing service), not the
  low-level `create-expense!`. Only manual user-created expenses can have non-BAM currencies.
- `app-config` threading: registry → routes/core.clj → handlers → services.

### Frontend patterns
- Settings panel (gear icon, top-right) is the only path to settings — no sidebar nav item.
  File: `src/app/template/frontend/components/settings/global_settings.cljs`
- Route wiring requires touching 7 files (descriptor, route options, page mapping, init event,
  settings panel link, i18n, and the page component itself).
- UIx patterns: `defui`, `$` for JSX, `use-state`/`use-effect`/`use-subscribe`, `use-t` for i18n.
- Re-frame `common-interceptors` includes `trim-v` — handlers destructure `[params]` not `[_ params]`.
- HTTP requests use `xhrio` helper (wraps `http/api-request`).

### Bug fixes applied during implementation
- `resolve-payer-id` in `review.clj` was missing `tenant_id` argument (broke after multi-tenancy migration).
  Fixed to destructure `tenant_id` and guard with `(when (and tenant_id user_id) ...)`.

---

## Key Files Reference

### Created in Phase 1–5 (implemented)
| File | Role | Status |
|------|------|--------|
| `resources/db/domain/models.edn` | DB schema (5 new tables + expense currency columns) | Modified |
| `src/app/domain/backend/expenses/services/global_settings.clj` | Global settings service | **New** |
| `src/app/domain/backend/expenses/services/exchange_rates.clj` | Daily CBBH rate service | **New** |
| `src/app/domain/backend/expenses/services/tenant_settings.clj` | Tenant settings service | **New** |
| `src/app/domain/backend/expenses/handlers/admin_global_settings.clj` | Admin API handlers | **New** |
| `src/app/domain/backend/expenses/routes/global_settings.clj` | Admin route definitions | **New** |
| `src/app/domain/backend/expenses/handlers/user_expenses/profile.clj` | Profile API handlers | **New** |
| `src/app/domain/backend/expenses/routes/core.clj` | Admin route assembly (spliced global-settings) | Modified |
| `src/app/domain/backend/registry.clj` | Passes app-config to admin routes | Modified |
| `src/app/domain/backend/expenses/services/user_expense_settings.clj` | Slimmed per-user settings service | Modified |
| `src/app/domain/backend/expenses/services/user_expenses.clj` | Currency conversion in create | Modified |
| `src/app/domain/backend/expenses/services/expenses.clj` | Allowlist new columns | Modified |
| `src/app/domain/backend/expenses/handlers/user_expenses/crud.clj` | Pass app-config | Modified |
| `src/app/domain/backend/expenses/routes/user_api.clj` | Wire app-config to create handler; old export/delete routes removed | Modified |
| `src/app/domain/backend/expenses/workers/receipt_ocr/extraction/review.clj` | Currency mismatch + payer-id fix | Modified |
| `src/app/domain/backend/expenses/workers/receipt_ocr/refine.clj` | AI refine toggle now follows global settings | Modified |
| `src/app/domain/backend/expenses/workers/receipt_ocr/runner.clj` | Global settings threaded into OCR worker opts | Modified |
| `src/app/template/backend/routes/api.clj` | Profile routes via requiring-resolve | Modified |
| `src/app/template/backend/routes/tenant.clj` | Tenant settings/name routes | Modified |
| `src/app/template/backend/services/tenant.clj` | `update-tenant!`, tenant_settings provisioning, slim user settings inserts | Modified |
| `src/app/domain/frontend/expenses/pages/user/profile.cljs` | User profile page | **New** |
| `src/app/domain/frontend/expenses/events/user_expenses/profile.cljs` | Profile events & subs | **New** |
| `src/app/admin/frontend/pages/domain/expenses/global_settings.cljs` | Admin global settings UI | **New** |
| `src/app/domain/frontend/expenses/events/admin_global_settings.cljs` | Admin global settings events | **New** |

### Removed in Phase 5
| File | Role |
|------|------|
| `src/app/domain/frontend/expenses/pages/user/expense_settings.cljs` | Old settings page |
| `src/app/domain/frontend/expenses/events/user_expenses/settings.cljs` | Old settings events |

---

## Specs (all updated)
- `specs/allium/domain/expenses/settings-hierarchy.candidate.allium` — **primary spec** (new)
- `specs/allium/drafts/expenses/expense-entry.candidate.allium` — updated (currency model)
- `specs/allium/drafts/expenses/receipt-ocr.candidate.allium` — updated (BAM-only, mismatch detection)
- `specs/allium/drafts/expenses/payer-and-defaults.candidate.allium` — updated (default_note removed)

---

## Remaining Follow-up

There is no required frontend implementation work left for this plan.

What remains is follow-up work only:
- browser-based verification of the new profile/admin/settings/currency flows
- optional UI polish for the receipt review warning presentation
- optional broader currency UX polish outside the expense detail page
- unrelated frontend config alignment cleanup reported by the migration helper
