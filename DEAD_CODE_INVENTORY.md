# Dead Code Removal Report — Admin and Domain

Updated: 2026-04-30

## Scope

Dead code was removed from:

- `src/app/admin/**`
- `src/app/domain/**`
- matching test code that only exercised deleted dead code

## Removed code

### Domain frontend

- Deleted unused re-frame event namespace:
  - `src/app/domain/frontend/expenses/events/user_expenses/dashboard.cljs`
- Removed its aggregator require from:
  - `src/app/domain/frontend/expenses/events/user_expenses.cljs`
- Removed unused workspace dashboard subscription:
  - `:workspace-dashboard/has-data?`
- Removed unused user-expenses subscriptions:
  - `:user-expenses/summary`
  - `:user-expenses/summary-loading?`
  - `:user-expenses/summary-error`
  - `:user-expenses/recent`
  - `:user-expenses/recent-loading?`
  - `:user-expenses/recent-error`
  - `:user-expenses/by-month`
  - `:user-expenses/by-month-loading?`
  - `:user-expenses/filtered-receipts`
- Removed unused duplicate-detection aggregate subscription:
  - `::loading-by-strategy`
- Deleted obsolete expense-new namespace:
  - `src/app/domain/frontend/expenses/pages/user/expense_new.cljs`
- Deleted test namespace that only targeted the obsolete expense-new namespace:
  - `test/app/domain/frontend/expenses/pages/user/expense_new_test.cljs`
- Removed unused smart-input helper export:
  - `expense-category-default-id`
- Removed unused smart-input constant:
  - `currency-options`

### Admin frontend

- Removed unused subscriptions:
  - `:admin/login-events-loading?`
  - `:admin/audit-logs-loading?`
  - `:admin/receipt-form-loading?`
- Removed unused settings navigation event:
  - `:app.admin.frontend.events.settings/apply-navigation-save-success`
- Removed now-unused `app.admin.frontend.config.loader` require from settings navigation events.

### Domain backend

- Removed legacy user-expenses sort wrappers:
  - `parse-order-by`
  - `parse-order-dir`
- Removed unused tenant-settings provisioning helper:
  - `provision-tenant-settings!`
- Removed unused city facade wrappers:
  - `find-city-by-normalized-key`
  - `find-city-by-country-and-zip`
- Removed unused payer wrapper and backing private binding:
  - `get-payer`
  - `get-payer*`

### Lower-priority lint cleanups

- Collapsed redundant nested `let` in:
  - `src/app/admin/frontend/config/preload.cljs`
- Removed unused signal-function bindings in:
  - `src/app/admin/frontend/subs/reports.cljs`
- Removed unused local `explicit-query?` from:
  - `src/app/domain/backend/expenses/services/cities_places.clj`
- Removed redundant `do` from:
  - `src/app/domain/backend/expenses/services/places_api.clj`
- Simplified redundant nested `or` in:
  - `src/app/domain/frontend/expenses/pages/user/expense_reports/components.cljs`
- Replaced single-argument `str` with a literal string in:
  - `src/app/domain/frontend/expenses/pages/user/expense_reports/sections/expenses.cljs`

## Validation

All checks passed after removal:

- `bb unused-public-var --domain --admin -o tmp/domain-admin-unused-diagnostics-after-removal.txt`
  - Result: `0` unused diagnostics.
- `clj-kondo --parallel --cache false --lint src/app/admin src/app/domain`
  - Result: `0` errors, `0` warnings.
- `npx shadow-cljs compile admin`
  - Result: build completed with `0` warnings.
- `npx shadow-cljs compile app`
  - Result: build completed with `0` warnings.
- Test-classpath backend namespace load for edited backend namespaces.
  - Result: all edited backend namespaces loaded successfully.

## Remaining notes

- No admin/domain unused public var, unused public keyword, unused namespace, unused referred var, or unused private var diagnostics remain from the repository’s `bb unused-public-var --domain --admin` task.
- Generated validation output is saved under `tmp/` and is ignored by git.
