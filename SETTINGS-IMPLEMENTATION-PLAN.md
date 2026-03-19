# Settings Hierarchy Implementation Plan

**Spec**: `specs/allium/domain/expenses/settings-hierarchy.candidate.allium`
**Status**: Spec complete, implementation not started
**Created**: 2026-03-19

## Overview

Restructure the monolithic `/expenses/settings` page into a three-tier settings hierarchy:

| Tier | Owner | Storage | Surface |
|------|-------|---------|---------|
| **Global** | Platform admin | `global_settings` table (singleton) | Admin panel section |
| **Tenant** | Tenant owner | `tenant_settings` table (per-tenant) | Owner profile section |
| **Per-user** | Each user | `user_expense_settings` table (slimmed) | User profile page `/profile` |

Also introduces: multi-currency support (manual expenses), daily CBBH exchange rates, and currency mismatch detection on receipts.

---

## Phase 1: Database & Backend Foundation

### 1.1 New DB tables + migrations
- [ ] Create `global_settings` table (singleton):
  - `id`, `default_currency` (text, default "BAM"), `default_note` (text, nullable),
    `auto_publish_after_upload` (boolean, default false), `ai_receipt_enhancement` (boolean, default false),
    `created_at`, `updated_at`
- [ ] Create `enabled_currencies` table:
  - `id`, `code` (text, unique), `name` (text), `is_base` (boolean, default false),
    `created_at`, `updated_at`
- [ ] Create `daily_exchange_rates` table:
  - `id`, `currency_code` (text), `rate_date` (date), `rate` (numeric),
    `fetched_at` (timestamptz), `is_fallback` (boolean, default false),
    `created_at`
  - Unique index: `(currency_code, rate_date)`
- [ ] Create `exchange_rate_fetch_alerts` table:
  - `id`, `rate_date` (date), `error_message` (text), `fallback_source_date` (date),
    `created_at` (timestamptz), `acknowledged` (boolean, default false)
- [ ] Create `tenant_settings` table:
  - `id`, `tenant_id` (FK tenants, unique), `email_notifications` (boolean, default true),
    `created_at`, `updated_at`
- [ ] Add currency fields to `expenses` table:
  - `original_amount` (numeric), `bam_amount` (numeric),
    `exchange_rate` (numeric, nullable), `rate_fetched_at` (timestamptz, nullable)
  - Backfill: `original_amount = total_amount`, `bam_amount = total_amount`, `exchange_rate = null`
    (all existing expenses are BAM)
- [ ] Seed migration: insert singleton `global_settings` row + seed `enabled_currencies`
  (BAM, EUR, USD, GBP, CHF, HRK, RSD, TRY)
- [ ] Seed migration: create `tenant_settings` row for each existing tenant
- [ ] Apply migrations to dev + test DBs

**Current files to modify**:
- `resources/db/domain/models.edn` — add new table definitions
- Generate migration via `simple_repl.clj`

### 1.2 Remove columns from user_expense_settings
- [ ] Remove from `user_expense_settings`: `default_currency`, `default_note`,
  `auto_post_after_upload_enabled`, `receipt_refine_enabled`, `notifications_enabled`
- [ ] Keep: `user_id`, `tenant_id`, `default_payer_id`, `default_expense_category_id`,
  `receipt_ocr_provider` (internal, not user-facing)
- [ ] Migration to drop columns (after data migration to new tables)

**Note**: Phase 1.2 should happen AFTER Phase 2 backend reads are migrated, to avoid breaking running code.

---

## Phase 2: Backend Services & API

### 2.1 Global settings service
- [ ] New file: `src/app/domain/backend/expenses/services/global_settings.clj`
  - `get-global-settings` (singleton read)
  - `update-global-settings!` (partial update)
  - `get-enabled-currencies` (list all)
  - `add-enabled-currency!`, `remove-enabled-currency!`
- [ ] Validation: default_currency must be in enabled list, BAM cannot be removed

### 2.2 Daily exchange rate service
- [ ] New file: `src/app/domain/backend/expenses/services/exchange_rates.clj`
  - `get-daily-rates` (for a date)
  - `fetch-and-cache-daily-rates!` (Serper API → CBBH kursna lista → parse → store)
  - `ensure-daily-rates!` (idempotent: fetch only if missing for today)
  - `get-rate-for-currency` (lookup from cache)
  - `get-fallback-rates` (most recent cached)
  - `create-fetch-alert!`, `acknowledge-alert!`
- [ ] Serper API integration: search "CBBH kursna lista {date}", parse rates

### 2.3 Tenant settings service
- [ ] New file: `src/app/domain/backend/expenses/services/tenant_settings.clj`
  - `get-tenant-settings`, `update-tenant-settings!`
  - `provision-tenant-settings!` (called from tenant provisioning)

### 2.4 Update user_expense_settings service
- [ ] Modify: `src/app/domain/backend/expenses/services/user_expense_settings.clj`
  - Remove: `default-currency`, `default-note`, `notifications-enabled`,
    `auto-post-after-upload-enabled`, `receipt-refine-enabled` from `default-settings`
  - Remove: `allowed-currencies` (now in `enabled_currencies` table)
  - Keep: `get-user-expense-settings`, `upsert-user-expense-settings!`,
    `update-sticky-default-payer!`, `effective-settings`
  - Update `effective-settings` to merge global settings where needed

### 2.5 Expense creation — currency conversion
- [ ] Modify expense creation handlers to:
  - Accept `currency` from frontend (manual expenses)
  - Call `ensure-daily-rates!` for non-BAM currencies
  - Calculate `bam_amount = original_amount * rate`
  - Store `exchange_rate` and `rate_fetched_at`
- [ ] Receipt-originated expenses: always set `currency = "BAM"`, `bam_amount = total_amount`

**Files to modify**:
- `src/app/domain/backend/expenses/handlers/user_expenses/crud.clj`
- `src/app/domain/backend/expenses/handlers/user_receipts.clj`
- `src/app/domain/backend/expenses/workers/receipt_ocr/extraction/review.clj`
  (read `ai_receipt_enhancement` from global_settings instead of user settings)

### 2.6 Receipt OCR — currency mismatch detection
- [ ] Modify receipt extraction success handler:
  - After OCR, if `currency_guess` is not "BAM" and not "KM":
    - Set `status = review_required`
    - Set `error_message` with clear currency mismatch warning
  - Block auto-post for currency mismatches (already blocked by `requires_human_review`)

**Files to modify**:
- `src/app/domain/backend/expenses/workers/receipt_ocr/extraction/review.clj`
- Related supplier/store resolution files

### 2.7 Admin API routes for global settings
- [ ] New admin routes (in admin API):
  - GET/PUT `/admin/api/global-settings`
  - GET/POST/DELETE `/admin/api/enabled-currencies`
  - GET `/admin/api/daily-exchange-rates` (today's rates)
  - GET/PUT `/admin/api/exchange-rate-alerts` (list + acknowledge)

### 2.8 User API routes — profile & tenant settings
- [ ] New user API routes:
  - GET `/api/v1/profile` (user info + defaults + owner section data)
  - PUT `/api/v1/profile/defaults` (update default_expense_category_id)
  - PUT `/api/v1/tenant/settings` (owner: email_notifications)
  - PUT `/api/v1/tenant/name` (owner: workspace name)
- [ ] Modify existing:
  - GET/PUT `/api/v1/expenses/settings` — deprecate, eventually remove
  - GET `/api/v1/expenses/export` — move to profile/owner context
  - DELETE `/api/v1/expenses/all` — move to profile/owner context

### 2.9 Tenant provisioning integration
- [ ] Modify tenant provisioning to call `provision-tenant-settings!`
- [ ] Ensure `global_settings` singleton exists at app startup

**Files to modify**:
- `src/app/domain/backend/expenses/services/tenant_settings.clj` (new)
- `src/app/template/backend/core.clj` or DI config (startup seed)
- Tenant provisioning service

---

## Phase 3: Frontend — User Profile Page

### 3.1 Profile page component
- [ ] New file: `src/app/domain/frontend/expenses/pages/user/profile.cljs`
  - Account info section (email, name — read-only)
  - Per-user defaults section:
    - Default payer (read-only display, sticky)
    - Default category (dropdown, editable)
  - Owner section (conditionally rendered):
    - Workspace name (editable text field)
    - Email notifications (toggle)
    - Export all expenses (button → CSV download)
    - Delete all expenses (button → typed "DELETE" confirmation)

### 3.2 Profile events & subscriptions
- [ ] New file: `src/app/domain/frontend/expenses/events/user_expenses/profile.cljs`
  - Fetch profile data, update defaults, update tenant settings
  - Update workspace name
  - Export/delete actions
- [ ] Register subscriptions for profile state

### 3.3 Profile route
- [ ] Add route: `/profile` → `:user-profile` view
- [ ] Add to router: `src/app/domain/shared/routes/expenses_user.cljc`
- [ ] Add page init event: `:page/init-user-profile`
- [ ] Add sidebar nav item (user avatar/name → profile)

### 3.4 i18n translations
- [ ] Add BS/EN translations for profile page labels

**Files to modify**:
- `src/app/domain/frontend/expenses/pages.cljs` (add profile page mapping)
- `src/app/domain/frontend/expenses/routes/user.cljs` (add route)
- `src/app/domain/shared/routes/expenses_user.cljc` (add path)
- `src/app/template/frontend/events/routing.cljs` (add init event)
- `src/app/template/frontend/i18n.cljs` (add translations)
- Layout/sidebar component (add profile nav link)

---

## Phase 4: Frontend — Admin Global Settings

### 4.1 Admin global settings panel
- [ ] New admin page/section for global settings:
  - Default currency (autocomplete from enabled list)
  - Default note (text area)
  - Auto-publish toggle
  - AI enhancement toggle
  - Enabled currencies management (add/remove, BAM locked)
  - Today's exchange rates (read-only table)
  - Rate fetch alerts (warning banner, acknowledge button)

### 4.2 Admin events & API integration
- [ ] Events for: fetch/update global settings, manage currencies, view rates, acknowledge alerts

**Files to modify/create**:
- Admin frontend components (new page or section)
- Admin API integration events

---

## Phase 5: Remove Old Settings Page

### 5.1 Remove /expenses/settings route & components
- [ ] Delete: `src/app/domain/frontend/expenses/pages/user/expense_settings.cljs`
- [ ] Remove: settings events from `events/user_expenses/settings.cljs`
- [ ] Remove: endpoint definition from `events/user_expenses/endpoints.cljs`
- [ ] Remove: route from `routes/user.cljs` and `routes/expenses_user.cljc`
- [ ] Remove: page init event `:page/init-expense-settings` from routing.cljs
- [ ] Remove: i18n keys for `expense-settings/*`
- [ ] Remove: sidebar nav link to settings page

### 5.2 Remove old backend routes
- [ ] Remove: GET/PUT `/api/v1/expenses/settings`
  (after frontend is fully migrated to new endpoints)
- [ ] Remove: settings handler file or repurpose

### 5.3 Drop deprecated columns
- [ ] Migration: drop `default_currency`, `default_note`, `notifications_enabled`,
  `auto_post_after_upload_enabled`, `receipt_refine_enabled` from `user_expense_settings`
- [ ] Clean up service code that referenced these columns

### 5.4 Clean up tests
- [ ] Update: `test/app/domain/backend/expenses/handlers/user_expenses/settings_test.clj`
  (rewrite for new endpoints or delete if covered by new tests)
- [ ] Add tests for: global settings API, tenant settings API, profile API,
  exchange rate service, currency mismatch detection

---

## Phase 6: Exchange Rate Integration

### 6.1 Serper API integration
- [ ] Implement CBBH rate fetching via Serper API
- [ ] Parse exchange rate data from search results
- [ ] Store daily rates in `daily_exchange_rates`
- [ ] Fallback logic: use latest cached rates + create alert

### 6.2 Manual expense currency flow
- [ ] Frontend: currency dropdown in Smart Input Phase 2
- [ ] Backend: validate currency is enabled, fetch rate, calculate BAM amount
- [ ] Expense detail view: show conversion breakdown for non-BAM expenses

### 6.3 Receipt currency mismatch
- [ ] Backend: detect non-BAM/KM currency in OCR extraction
- [ ] Set receipt to `review_required` with clear error message
- [ ] Frontend: show warning banner in receipt review UI

---

## Dependencies & Order

```
Phase 1 (DB) ──→ Phase 2 (Backend) ──→ Phase 3 (Profile FE) ──→ Phase 5 (Cleanup)
                      │                                              ↑
                      └──→ Phase 4 (Admin FE) ───────────────────────┘
                      │
                      └──→ Phase 6 (Exchange Rates) ─────────────────┘
```

- Phase 1 must complete first (DB foundation)
- Phase 2 can partially parallelize (services are independent)
- Phase 3 and 4 can run in parallel (different UI surfaces)
- Phase 5 only after Phase 3 is live (old page replaced)
- Phase 6 can start alongside Phase 3/4 (backend service is independent)

---

## Key Files Reference

### Currently exist (to modify)
| File | Role |
|------|------|
| `resources/db/domain/models.edn` | DB schema definitions |
| `src/app/domain/backend/expenses/services/user_expense_settings.clj` | Per-user settings service |
| `src/app/domain/backend/expenses/handlers/user_expenses/settings.clj` | Settings API handlers |
| `src/app/domain/backend/expenses/handlers/user_expenses/crud.clj` | Expense creation (add currency) |
| `src/app/domain/backend/expenses/handlers/user_receipts.clj` | Receipt upload (auto-publish source) |
| `src/app/domain/backend/expenses/workers/receipt_ocr/extraction/review.clj` | OCR review (AI enhancement source, currency mismatch) |
| `src/app/domain/backend/expenses/routes/user_api.clj` | User API route definitions |
| `src/app/domain/frontend/expenses/pages/user/expense_settings.cljs` | Old settings page (to delete) |
| `src/app/domain/frontend/expenses/events/user_expenses/settings.cljs` | Old settings events (to delete) |
| `src/app/template/frontend/i18n.cljs` | Translations |

### To create (new)
| File | Role |
|------|------|
| `src/app/domain/backend/expenses/services/global_settings.clj` | Global settings service |
| `src/app/domain/backend/expenses/services/exchange_rates.clj` | Daily CBBH rate service |
| `src/app/domain/backend/expenses/services/tenant_settings.clj` | Tenant settings service |
| `src/app/domain/frontend/expenses/pages/user/profile.cljs` | User profile page |
| `src/app/domain/frontend/expenses/events/user_expenses/profile.cljs` | Profile events |

---

## Specs (all updated)
- `specs/allium/domain/expenses/settings-hierarchy.candidate.allium` — **primary spec** (new)
- `specs/allium/drafts/expenses/expense-entry.candidate.allium` — updated (currency model)
- `specs/allium/drafts/expenses/receipt-ocr.candidate.allium` — updated (BAM-only, mismatch detection)
- `specs/allium/drafts/expenses/payer-and-defaults.candidate.allium` — updated (default_note removed)
