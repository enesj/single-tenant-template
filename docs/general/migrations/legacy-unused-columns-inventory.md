# Legacy / Unused DB Columns Inventory

Date: 2026-02-15

## Scope and method

This audit used:

1. PostgreSQL metadata and data-population checks (tables/columns, constraints, indexes, triggers, views, procedures, `COUNT(*)` vs `COUNT(column)`).
2. Code-usage search across `src/**`, `test/**`, and frontend config EDN (`snake_case` + `kebab-case` variants).
3. Canonical schema review in `resources/db/domain/models.edn` and `resources/db/template/models.edn`.

Evidence artifacts are stored in:

- `tmp/legacy-columns-audit/db-column-evidence-2026-02-15.md`
- `tmp/legacy-columns-audit/code-usage-evidence-2026-02-15.md`
- `tmp/legacy-columns-audit/check-migrations-dev-20260215-172902.txt`
- `tmp/legacy-columns-audit/check-migrations-test-20260215-172905.txt`

## Classification summary

- **drop-now**: 0
- **defer**: 10
- **keep**: 13

No high-confidence safe drop was found in this pass.

## Exact inventoried columns and classification

| Column | Non-null / total | Class | Evidence-backed rationale |
| --- | ---: | --- | --- |
| `articles.category` | 0 / 163 | **defer** | Explicitly marked legacy in model comments, but still read/write referenced in backend/frontend and present in admin table config; indexed (`idx_articles_category`). |
| `expense_items.raw_label_id` | 0 / 156 | **defer** | Transitional field (model comment says backward-compat); still appears in UI/config; has FK + index (`expense_items_raw_label_id_fkey`, `idx_expense_items_raw_label_id`). |
| `receipts.raw_parse_json` | 0 / 57 | **defer** | Currently empty, but actively handled in receipt status/extraction code paths and exposed in receipts config. |
| `expenses.deleted_at` | 0 / 46 | **defer** | Not populated, but still exposed in admin table columns for expenses (soft-delete semantics likely expected). |
| `expense_items.deleted_at` | 0 / 156 | **defer** | Not populated, but still exposed in admin table columns for expense-items. |
| `suppliers.archived_at` | 0 / 28 | **defer** | Still exposed in admin/domain table configs (`archived_at` columns). |
| `stores.archived_at` | 0 / 30 | **defer** | Still exposed in admin/domain table configs (`archived_at` columns). |
| `stores.place_id` | 0 / 30 | **defer** | Heavy active usage in store resolution/canonicalization logic and indexed (`idx_stores_place_id`). |
| `payers.last4` | 0 / 2 | **defer** | Used by forms/config/tests, despite null data in current dataset. |
| `users.provider_user_id` | 0 / 1 | **defer** | Used in OAuth/auth service paths; coupled to partial unique index (`idx_users_auth_provider_provider_user_id_external`). |
| `admin_sessions.ip_address` | 9 / 9 | **keep** | Populated and actively persisted from request context in admin auth/session flows. |
| `admin_sessions.user_agent` | 9 / 9 | **keep** | Populated and actively persisted from request context in admin auth/session flows. |
| `audit_logs.metadata` | 0 / 0 | **keep** | Audit table currently empty in this env, but writer path persists metadata (`log-audit!`) and admin UI config expects it. |
| `expenses.notes` | 46 / 46 | **keep** | Fully populated and used in user/admin expense flows and UI rendering. |
| `receipts.currency_guess` | 57 / 57 | **keep** | Actively populated and consumed in receipt approval/query workflows. |
| `receipts.expense_id` | 46 / 57 | **keep** | Populated and explicitly used for receipt↔expense linkage in backend services. |
| `receipts.parsed_markdown` | 57 / 57 | **keep** | Populated and consumed by receipt/store fingerprinting workflows. |
| `receipts.purchased_at_guess` | 54 / 57 | **keep** | Populated and used in approval/update paths. |
| `receipts.raw_extract_json` | 57 / 57 | **keep** | Core OCR extraction payload field; used in multiple read/update paths. |
| `receipts.store_guess` | 56 / 57 | **keep** | Populated and used in receipt processing/approval paths. |
| `receipts.supplier_guess` | 57 / 57 | **keep** | Populated and used in receipt processing/approval paths. |
| `receipts.total_amount_guess` | 57 / 57 | **keep** | Populated and used in receipt quality/approval workflows. |
| `users.avatar_url` | 1 / 1 | **keep** | Populated and used in auth/profile paths and admin presentation. |

## Migration outcome

Because there are **no high-confidence `drop-now` columns**, no schema changes were applied in this pass.

- Canonical model files edited: **none**
- Generated migration files: **none**
- Applied migrations: **none**

## Dev/Test validation

Database alignment checks were executed and passed for both environments:

- `bb check-migrations dev` → ✅ all aligned
- `bb check-migrations test` → ✅ all aligned

Full command outputs are saved in `tmp/legacy-columns-audit/`.

## Risks and recommended follow-ups

1. **Config coupling blocker**: several all-null fields are still present in table config EDN (admin/domain). Dropping them safely requires coordinated config updates.
2. **Transition-field blocker**: fields like `articles.category` and `expense_items.raw_label_id` are legacy but still referenced by active code paths.
3. **Recommended next pass**:
   - First remove/replace field usage in frontend/backend config + code.
   - Re-run this same evidence process.
   - Only then generate and apply drop migrations for columns that become truly unused and data-empty.

## Update — single-column pass `expenses.deleted_at` (2026-02-15)

This targeted follow-up pass removed exactly one deferred column: `expenses.deleted_at`.

- Status: **completed**
- Generated migration: `resources/db/migrations/0056_schema.edn`
- Migration action: `{:action :drop-column, :field-name :deleted-at, :model-name :expenses}`

### What changed in this pass (`expenses.deleted_at`)

- Removed `"deleted_at"` from admin expenses table columns config:
  - `src/app/admin/frontend/config/table-columns.edn` (`:expenses :available-columns`)
- Removed `[:deleted_at :timestamptz]` from canonical domain model source:
  - `resources/db/domain/models.edn` (`:expenses :fields`)
- Regenerated consolidated schema and migration artifacts via official workflow:
  - `resources/db/models.edn` (generated)
  - `resources/db/migrations/0056_schema.edn` (generated)

### Evidence for this pass (`expenses.deleted_at`)

- Migration generation log:
  - `tmp/legacy-columns-audit/make-all-migrations-expenses-deleted-at-20260215-181936.txt`
- Migration apply (dev + test) log:
  - `tmp/legacy-columns-audit/migrate-dev-test-expenses-deleted-at-20260215-181948.txt`
- Alignment checks:
  - `tmp/legacy-columns-audit/check-migrations-dev-expenses-deleted-at-20260215-182003.txt`
  - `tmp/legacy-columns-audit/check-migrations-test-expenses-deleted-at-20260215-182006.txt`
- Focused validation:
  - `tmp/legacy-columns-audit/focused-frontend-config-files-test-expenses-deleted-at-20260215-182017.txt`

### Notes (`expenses.deleted_at`)

- `expense_items.deleted_at` remains unchanged (explicitly out of scope for this pass).
- Re-audit after this pass found no active code/config dependency on `expenses.deleted_at`.

## Update — single-column pass `expense_items.deleted_at` (2026-02-15)

This targeted follow-up pass removed exactly one deferred column: `expense_items.deleted_at`.

- Status: **completed**
- Generated migration: `resources/db/migrations/0057_schema.edn`
- Migration action: `{:action :drop-column, :field-name :deleted-at, :model-name :expense_items}`

### What changed in this pass (`expense_items.deleted_at`)

- Removed `"deleted_at"` from admin expense-items table columns config:
  - `src/app/admin/frontend/config/table-columns.edn` (`:expense-items :available-columns`)
- Removed `[:deleted_at :timestamptz]` from canonical domain model source:
  - `resources/db/domain/models.edn` (`:expense_items :fields`)
- Regenerated consolidated schema and migration artifacts via official workflow:
  - `resources/db/models.edn` (generated)
  - `resources/db/migrations/0057_schema.edn` (generated)

### Evidence for this pass (`expense_items.deleted_at`)

- Re-audit (no active references):
  - `tmp/legacy-columns-audit/re-audit-expense-items-deleted-at-references-20260215-182910.txt`
- Migration generation log:
  - `tmp/legacy-columns-audit/make-all-migrations-expense-items-deleted-at-20260215-182808.txt`
- Migration apply (dev + test) log:
  - `tmp/legacy-columns-audit/migrate-dev-test-expense-items-deleted-at-20260215-182824.txt`
- Alignment checks:
  - `tmp/legacy-columns-audit/check-migrations-dev-expense-items-deleted-at-20260215-182834.txt`
  - `tmp/legacy-columns-audit/check-migrations-test-expense-items-deleted-at-20260215-182842.txt`
- Focused validation:
  - `tmp/legacy-columns-audit/focused-frontend-config-files-test-expense-items-deleted-at-20260215-182916.txt`
- Dedicated pass note:
  - `tmp/legacy-columns-audit/expense-items-deleted-at-removal-pass-2026-02-15.md`

### Notes (`expense_items.deleted_at`)

- Re-audit after this pass found no active code/config dependency on `expense_items.deleted_at`.
- `expense_items.deleted_at` is absent from `information_schema.columns` in dev after migration apply.

## Update — single-column pass `suppliers.archived_at` (2026-02-15)

This targeted follow-up pass removed exactly one deferred column: `suppliers.archived_at`.

- Status: **completed**
- Generated migration: `resources/db/migrations/0058_schema.edn`
- Migration action: `{:action :drop-column, :field-name :archived-at, :model-name :suppliers}`

### What changed in this pass (`suppliers.archived_at`)

- Removed `"archived_at"` from suppliers table columns config:
  - `src/app/admin/frontend/config/table-columns.edn` (`:suppliers :available-columns`)
  - `src/app/domain/frontend/expenses/config/table-columns.edn` (`:suppliers :available-columns`)
- Removed `:archived-at` from user supplier edit initial-values:
  - `src/app/domain/frontend/expenses/components/user_reference_forms.cljs`
- Removed `[:archived_at :timestamptz]` from canonical schema source:
  - `resources/db/domain/models.edn` (`:suppliers :fields`)
- Regenerated schema and migrations via official workflow:
  - `resources/db/models.edn` (generated)
  - `resources/db/migrations/0058_schema.edn` (generated)

### Evidence for this pass (`suppliers.archived_at`)

- Dedicated pass note:
  - `tmp/legacy-columns-audit/suppliers-archived-at-removal-pass-2026-02-15.md`
- Re-audit (dependency removal):
  - `tmp/legacy-columns-audit/re-audit-suppliers-archived-at-references-20260215-183933.txt`
- Migration generation log:
  - `tmp/legacy-columns-audit/make-all-migrations-suppliers-archived-at-20260215-183547.txt`
- Migration apply (dev + test) log:
  - `tmp/legacy-columns-audit/migrate-dev-test-suppliers-archived-at-20260215-183644.txt`
- Alignment checks:
  - `tmp/legacy-columns-audit/check-migrations-dev-suppliers-archived-at-20260215-183656.txt`
  - `tmp/legacy-columns-audit/check-migrations-test-suppliers-archived-at-20260215-183659.txt`
- Focused validation:
  - `tmp/legacy-columns-audit/focused-validate-frontend-config-expenses-suppliers-archived-at-20260215-183939.txt`

### Notes (`suppliers.archived_at`)

- PostgreSQL evidence confirmed pre-drop usage was `0 / 28` non-null rows.
- Post-migration PostgreSQL metadata confirms `suppliers.archived_at` is absent from `information_schema.columns` in dev.
- `stores.archived_at` remains present and is explicitly out of scope for this pass.

## Update — single-column pass `stores.archived_at` (2026-02-15)

This targeted follow-up pass removed exactly one deferred column: `stores.archived_at`.

- Status: **completed**
- Generated migration: `resources/db/migrations/0059_schema.edn`
- Migration action: `{:action :drop-column, :field-name :archived-at, :model-name :stores}`

### What changed in this pass (`stores.archived_at`)

- Removed `"archived_at"` from stores table columns config:
  - `src/app/admin/frontend/config/table-columns.edn` (`:stores :available-columns`, `:stores :filterable-columns`)
  - `src/app/domain/frontend/expenses/config/table-columns.edn` (`:stores :available-columns`, `:stores :filterable-columns`)
- Removed `[:archived_at :timestamptz]` from canonical schema source:
  - `resources/db/domain/models.edn` (`:stores :fields`)
- Regenerated schema and migrations via official workflow:
  - `resources/db/models.edn` (generated)
  - `resources/db/migrations/0059_schema.edn` (generated)

### Evidence for this pass (`stores.archived_at`)

- Dedicated pass note:
  - `tmp/legacy-columns-audit/stores-archived-at-removal-pass-2026-02-15.md`
- Re-audit (dependency removal):
  - `tmp/legacy-columns-audit/re-audit-stores-archived-at-references-20260215-184635.txt`
- Migration generation log:
  - `tmp/legacy-columns-audit/make-all-migrations-stores-archived-at-20260215-184547.txt`
- Migration apply (dev + test) log:
  - `tmp/legacy-columns-audit/migrate-dev-test-stores-archived-at-20260215-184557.txt`
- Alignment checks:
  - `tmp/legacy-columns-audit/check-migrations-dev-stores-archived-at-20260215-184608.txt`
  - `tmp/legacy-columns-audit/check-migrations-test-stores-archived-at-20260215-184614.txt`
- Focused validation:
  - `tmp/legacy-columns-audit/focused-validate-frontend-config-expenses-stores-archived-at-20260215-184623.txt`

### Notes (`stores.archived_at`)

- PostgreSQL evidence confirmed pre-drop usage was `0 / 30` non-null rows.
- Post-migration PostgreSQL metadata confirms `stores.archived_at` is absent from `information_schema.columns` in dev.
- Re-audit after this pass found no active code/config dependency on `stores.archived_at`.

## Update — single-column pass `receipts.raw_parse_json` (2026-02-15)

This targeted follow-up pass removed exactly one deferred column: `receipts.raw_parse_json`.

- Status: **completed**
- Generated migration: `resources/db/migrations/0060_schema.edn`
- Migration action: `{:action :drop-column, :field-name :raw-parse-json, :model-name :receipts}`

### What changed in this pass (`receipts.raw_parse_json`)

- Removed backend writes/clears for `raw_parse_json` in receipt OCR/status flows:
  - `src/app/domain/backend/expenses/services/receipts/status.clj`
  - `src/app/domain/backend/expenses/workers/receipt_ocr/core.clj`
- Removed `"raw-parse-json"` from receipts table columns config:
  - `src/app/admin/frontend/config/table-columns.edn` (`:receipts :available-columns`)
  - `src/app/domain/frontend/expenses/config/table-columns.edn` (`:receipts :available-columns`)
- Removed canonical schema field `[:raw_parse_json :jsonb]` from:
  - `resources/db/domain/models.edn` (`:receipts :fields`)
- Regenerated schema and migration artifacts via official workflow:
  - `resources/db/models.edn` (generated)
  - `resources/db/migrations/0060_schema.edn` (generated)

### Evidence for this pass (`receipts.raw_parse_json`)

- Migration generation log:
  - `tmp/legacy-columns-audit/make-all-migrations-receipts-raw-parse-json-20260215-185348.txt`
- Migration apply (dev + test) log:
  - `tmp/legacy-columns-audit/migrate-dev-test-receipts-raw-parse-json-20260215-185400.txt`
- Alignment checks:
  - `tmp/legacy-columns-audit/check-migrations-dev-receipts-raw-parse-json-20260215-185411.txt`
  - `tmp/legacy-columns-audit/check-migrations-test-receipts-raw-parse-json-20260215-185422.txt`
- Focused backend validation:
  - `tmp/legacy-columns-audit/focused-backend-receipts-status-raw-parse-json-counters-20260215-185607.txt`
- Frontend config validation:
  - `tmp/legacy-columns-audit/focused-validate-frontend-config-expenses-receipts-raw-parse-json-20260215-185733.txt`
- Re-audit references:
  - `tmp/legacy-columns-audit/re-audit-receipts-raw-parse-json-references-20260215-185726.txt`

### Notes (`receipts.raw_parse_json`)

- PostgreSQL evidence confirmed pre-drop usage was `0 / 57` non-null rows.
- Post-migration PostgreSQL metadata confirms `receipts.raw_parse_json` is absent from `information_schema.columns` in dev.
- Re-audit confirms no `raw_parse_json` references remain in this pass’s targeted backend/config/schema files.
- Follow-up spec drift fix completed: `specs/allium/drafts/expenses/receipt-ocr.candidate.allium` now uses `raw_extract_json` semantics (no `raw_parse_payload` persistence assumptions).

## Update — single-column pass `payers.last4` (2026-02-15)

This targeted follow-up pass removed exactly one deferred column: `payers.last4`.

- Status: **completed**
- Generated migration: `resources/db/migrations/0061_schema.edn`
- Migration action: `{:action :drop-column, :field-name :last4, :model-name :payers}`

### What changed in this pass (`payers.last4`)

- Removed `"last4"` from payers table columns config:
  - `src/app/admin/frontend/config/table-columns.edn` (`:payers :available-columns`)
  - `src/app/domain/frontend/expenses/config/table-columns.edn` (`:payers :available-columns`)
- Removed `:last4` from user payer edit initial-values mapping:
  - `src/app/domain/frontend/expenses/components/user_reference_forms.cljs`
- Removed canonical schema field `[:last4 [:varchar 4]]` from:
  - `resources/db/domain/models.edn` (`:payers :fields`)
- Regenerated schema and migration artifacts via official workflow:
  - `resources/db/models.edn` (generated)
  - `resources/db/migrations/0061_schema.edn` (generated)

### Evidence for this pass (`payers.last4`)

- DB evidence (pre-drop population + post-drop absence):
  - `tmp/legacy-columns-audit/payers-last4-db-evidence-20260215-190815.md`
- Re-audit references:
  - `tmp/legacy-columns-audit/re-audit-payers-last4-references-20260215-190809.txt`
- Migration generation log:
  - `tmp/legacy-columns-audit/make-all-migrations-payers-last4-20260215-190705.txt`
- Migration apply (dev + test) log:
  - `tmp/legacy-columns-audit/migrate-dev-test-payers-last4-20260215-190717.txt`
- Alignment checks:
  - `tmp/legacy-columns-audit/check-migrations-dev-payers-last4-20260215-190733.txt`
  - `tmp/legacy-columns-audit/check-migrations-test-payers-last4-20260215-190736.txt`
- Focused frontend validation:
  - `tmp/legacy-columns-audit/focused-fe-payer-form-specs-last4-removal-rerun-20260215-190852.txt`

### Notes (`payers.last4`)

- PostgreSQL evidence confirmed pre-drop usage was `0 / 2` non-null rows.
- Post-migration PostgreSQL metadata confirms `payers.last4` is absent from `information_schema.columns` in dev.
- Re-audit after this pass found no active `last4` references in `src/**`, `test/**`, `resources/db/domain/models.edn`, and `resources/db/models.edn`.

## Update — single-column pass `articles.category` (2026-02-15)

This targeted follow-up pass removed exactly one deferred column: `articles.category`.

- Status: **completed**
- Generated migrations:
  - `resources/db/migrations/0062_schema.edn`
  - `resources/db/migrations/0063_schema.edn`

### What changed in this pass (`articles.category`)

- Removed backend persistence mapping for legacy article category:
  - `src/app/domain/backend/expenses/services/articles.clj` (`create-article!`, `update-article!`)
- Removed exact `"category"` exposure from articles table-column config:
  - `src/app/admin/frontend/config/table-columns.edn` (`:articles :available-columns`)
  - `src/app/domain/frontend/expenses/config/table-columns.edn` (`:articles :available-columns`)
- Removed canonical schema field from source model:
  - `resources/db/domain/models.edn` (`:articles :fields`, `[:category [:varchar 100]]` removed)
- Regenerated consolidated schema and migrations via official workflow:
  - `resources/db/models.edn` (generated)
  - `resources/db/migrations/0062_schema.edn` (generated, drop index)
  - `resources/db/migrations/0063_schema.edn` (generated, drop column)

### Evidence for this pass (`articles.category`)

- Dedicated pass note:
  - `tmp/legacy-columns-audit/articles-category-removal-pass-2026-02-15.md`
- Re-audit references:
  - `tmp/legacy-columns-audit/re-audit-articles-category-references-20260215-192700.txt`
- Migration generation logs:
  - `tmp/legacy-columns-audit/make-all-migrations-articles-category-clean-regen-20260215-192540.txt`
  - `tmp/legacy-columns-audit/make-all-migrations-articles-category-column-drop-20260215-192612.txt`
- Migration apply (dev + test) logs:
  - `tmp/legacy-columns-audit/migrate-dev-test-articles-category-index-drop-20260215-192551.txt`
  - `tmp/legacy-columns-audit/migrate-dev-test-articles-category-column-drop-20260215-192624.txt`
- Alignment checks:
  - `tmp/legacy-columns-audit/check-migrations-dev-articles-category-20260215-192634.txt`
  - `tmp/legacy-columns-audit/check-migrations-test-articles-category-20260215-192642.txt`
- Focused backend validation:
  - `tmp/legacy-columns-audit/focused-be-kaocha-articles-service-category-removal-20260215-192853.txt`
- DB evidence (pre-drop presence + post-drop absence):
  - `tmp/legacy-columns-audit/db-column-evidence-2026-02-15.md`

### Notes (`articles.category`)

- Pre-drop data evidence confirmed usage was `0 / 163` non-null rows.
- Post-migration metadata confirms `articles.category` and `idx_articles_category` are absent.
- Re-audit confirms no active `articles.category` dependency remains in `src/**`, `test/**`, `resources/db/domain/models.edn`, and `resources/db/models.edn`.

## Update — single-column pass `expense_items.raw_label_id` (2026-02-15)

This targeted follow-up pass removed exactly one deferred column: `expense_items.raw_label_id`.

- Status: **completed**
- Generated migrations:
  - `resources/db/migrations/0064_schema.edn`
  - `resources/db/migrations/0065_schema.edn`

### What changed in this pass (`expense_items.raw_label_id`)

- Removed `"raw_label_id"` from admin expense-items table columns config:
  - `src/app/admin/frontend/config/table-columns.edn` (`:expense-items :available-columns`)
- Removed `:raw-label-id` from user expense-item edit initial-values selection:
  - `src/app/domain/frontend/expenses/pages/user/expense_items.cljs`
- Removed canonical schema field and index from source model:
  - `resources/db/domain/models.edn` (`:expense_items`)
- Regenerated consolidated schema and migrations via official workflow:
  - `resources/db/models.edn` (generated)
  - `resources/db/migrations/0064_schema.edn` (generated, drop index)
  - `resources/db/migrations/0065_schema.edn` (generated, drop column)

### Evidence for this pass (`expense_items.raw_label_id`)

- Re-audit references:
  - `tmp/legacy-columns-audit/re-audit-expense-items-raw-label-id-references-20260215-193631.txt`
  - `tmp/legacy-columns-audit/re-audit-expense-items-raw-label-id-references-post-drop-20260215-194739.txt`
- Migration generation logs:
  - `tmp/legacy-columns-audit/make-all-migrations-expense-items-raw-label-id-index-drop-20260215-194108.txt`
  - `tmp/legacy-columns-audit/make-all-migrations-expense-items-raw-label-id-column-drop-20260215-194142.txt`
- Migration apply (dev + test) logs:
  - `tmp/legacy-columns-audit/migrate-dev-test-expense-items-raw-label-id-index-drop-20260215-194124.txt`
  - `tmp/legacy-columns-audit/migrate-dev-test-expense-items-raw-label-id-column-drop-20260215-194151.txt`
- Alignment checks:
  - `tmp/legacy-columns-audit/check-migrations-dev-expense-items-raw-label-id-20260215-194159.txt`
  - `tmp/legacy-columns-audit/check-migrations-test-expense-items-raw-label-id-20260215-194206.txt`
- Focused backend validation:
  - `tmp/legacy-columns-audit/focused-be-expense-items-namespace-raw-label-id-20260215-194436.txt`

### Notes (`expense_items.raw_label_id`)

- Pre-drop data evidence confirmed usage was `0 / 156` non-null rows.
- Post-migration metadata confirms `expense_items.raw_label_id` and `idx_expense_items_raw_label_id` are absent in dev.
- Re-audit confirms no active `raw_label_id` / `raw-label-id` dependency remains in scoped source/config files for this pass.

## Update — single-column pass `stores.place_id` (2026-02-15)

This targeted follow-up pass removed exactly one deferred column: `stores.place_id`.

- Status: **completed**
- Generated migration:
  - `resources/db/migrations/0066_schema.edn`

### What changed in this pass (`stores.place_id`)

- Removed stores `place_id` exposure from table columns config:
  - `src/app/admin/frontend/config/table-columns.edn` (`:stores`)
  - `src/app/domain/frontend/expenses/config/table-columns.edn` (`:stores`)
- Removed domain frontend stores-form dependencies:
  - `src/app/domain/frontend/expenses/config/form-fields.edn`
  - `src/app/domain/frontend/expenses/components/user_power_forms.cljs`
  - `src/app/domain/frontend/expenses/events/user_expenses/stores.cljs`
  - `src/app/domain/frontend/expenses/admin/adapters/specs.cljs`
  - `src/app/domain/frontend/expenses/admin/adapters/normalize.cljs`
- Removed backend SQL/search/select dependencies on `stores.place_id` and switched Places canonicalization to place-derived `normalized_key`:
  - `src/app/domain/backend/expenses/services/stores.clj`
  - `src/app/domain/backend/expenses/services/service_configs.clj`
  - `src/app/domain/backend/expenses/services/articles.clj`
- Removed canonical schema field and index from source model:
  - `resources/db/domain/models.edn` (`:stores`)
- Regenerated schema and migration artifacts via official workflow:
  - `resources/db/models.edn` (generated)
  - `resources/db/migrations/0066_schema.edn` (generated, drop index + drop column)

### Evidence for this pass (`stores.place_id`)

- Dedicated pass note:
  - `tmp/legacy-columns-audit/stores-place-id-removal-pass-2026-02-15.md`
- DB pre/post evidence:
  - `tmp/legacy-columns-audit/stores-place-id-db-evidence-20260215.md`
- Re-audit references:
  - `tmp/legacy-columns-audit/re-audit-stores-place-id-references-pre-migration-20260215-195942.txt`
  - `tmp/legacy-columns-audit/re-audit-stores-place-id-references-post-migration-20260215-200251.txt`
- Migration generation log:
  - `tmp/legacy-columns-audit/make-all-migrations-stores-place-id-20260215-195957.txt`
- Migration apply (dev + test) log:
  - `tmp/legacy-columns-audit/migrate-dev-test-stores-place-id-20260215-200007.txt`
- Alignment checks:
  - `tmp/legacy-columns-audit/check-migrations-dev-stores-place-id-20260215-200028.txt`
  - `tmp/legacy-columns-audit/check-migrations-test-stores-place-id-20260215-200035.txt`
- Focused stores/receipt resolution validation:
  - `tmp/legacy-columns-audit/focused-be-stores-receipt-resolution-place-id-drop-20260215-200132.txt`

### Notes (`stores.place_id`)

- PostgreSQL evidence confirmed pre-drop usage was `0 / 30` non-null rows.
- Post-migration metadata confirms both `stores.place_id` and `idx_stores_place_id` are absent.
- Migration alignment checks passed for both dev and test.
- A remaining config drift (`:stores.place_id`) is still reported in `src/app/admin/frontend/config/form-fields.edn`, which was outside the user-provided allowed edit list for this single-column pass.
- Blocker evidence:
  - `tmp/legacy-columns-audit/blocker-admin-form-fields-stores-place-id-20260215-200458.txt`

## Update — single-column pass `users.provider_user_id` (2026-02-15)

This targeted follow-up pass removed exactly one deferred column: `users.provider_user_id`.

- Status: **completed**
- Generated migrations:
  - `resources/db/migrations/0067_schema.edn`
  - `resources/db/migrations/0068_schema.edn`

### What changed in this pass (`users.provider_user_id`)

- Removed `provider_user_id` writes from template auth service:
  - `src/app/template/backend/auth/service.clj`
- Removed `"provider-user-id"` from admin users table columns config:
  - `src/app/admin/frontend/config/table-columns.edn` (`:users :available-columns`)
- Removed remaining admin backend runtime dependencies:
  - `src/app/admin/backend/services/admin/users/management.clj`
    - `create-user!` no longer destructures/writes `:provider_user_id`
  - `src/app/admin/backend/services/admin/users/validation.clj`
    - `:provider_user_id` removed from update `allowed-keys`
- Removed canonical schema field and dependent external unique index from source model:
  - `resources/db/template/models.edn` (`:users`)
- Regenerated consolidated schema and migration artifacts via official workflow in two safe steps:
  - `resources/db/models.edn` (generated)
  - `resources/db/migrations/0067_schema.edn` (generated, drop index)
  - `resources/db/migrations/0068_schema.edn` (generated, drop column)
- Applied migrations to both dev and test:
  - `0067_schema` (drop `idx_users_auth_provider_provider_user_id_external`)
  - `0068_schema` (drop `users.provider_user_id`)

### Evidence for this pass (`users.provider_user_id`)

- DB evidence (data presence):
  - `tmp/legacy-columns-audit/users-provider-user-id-db-evidence-20260215.md`
- Migration generation log:
  - `tmp/legacy-columns-audit/make-all-migrations-users-provider-user-id-20260215-201120.txt`
- Re-audit references:
  - `tmp/legacy-columns-audit/re-audit-users-provider-user-id-references-20260215-201207.txt`
- Prior blocker evidence (resolved):
  - `tmp/legacy-columns-audit/blocker-users-provider-user-id-active-admin-dependency-20260215-201502.txt`
- Focused backend create/update/auth validation:
  - `tmp/legacy-columns-audit/focused-be-admin-users-provider-user-id-drop-20260215-203253.txt`
- Alignment checks (dev + test):
  - `tmp/legacy-columns-audit/check-migrations-dev-test-users-provider-user-id-20260215-203412.txt`

### Notes (`users.provider_user_id`)

- A direct single migration (`drop-column` + `drop-index` in one file) is unsafe because dropping the column removes the dependent index first.
- This pass used the established generated-only two-step pattern:
  1. generated index-drop migration (`0067`), then
  2. generated column-drop migration (`0068`).
- No manual migration file content edits were made.

## Update — final cleanup pass `stores.place_id` admin form-fields drift (2026-02-15)

Reviewer-noted follow-up cleanup removed the last stale admin frontend form-fields reference to the already-dropped `stores.place_id` column.

- Status: **completed**
- Schema/migration impact: **none** (config-only corrective action)

### What changed in this cleanup pass

- Removed stale `place_id` form config from admin stores form-fields:
  - `src/app/admin/frontend/config/form-fields.edn`
    - `:stores :edit-fields` now excludes `"place_id"`
    - `:stores :field-config` no longer defines `:place_id`

### Evidence for this cleanup pass

- Focused admin frontend config validation (strict EDN/spec check):
  - `tmp/legacy-columns-audit/focused-admin-frontend-config-form-fields-stores-place-id-rerun-20260215-204839.txt`
- Drift re-audit (admin form-fields file):
  - `tmp/legacy-columns-audit/re-audit-admin-form-fields-stores-place-id-20260215-204824.txt`
- Drift re-audit (all admin frontend config):
  - `tmp/legacy-columns-audit/re-audit-admin-config-place-id-20260215-204956.txt`

### Notes (`stores.place_id` final cleanup)

- This closes the previously documented blocker about admin form-fields config drift for `stores.place_id`.
- No remaining `place_id` / `place-id` references were found under `src/app/admin/frontend/config` in this pass.
