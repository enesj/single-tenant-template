# Payer Types FK Migration Plan (Updated)

Goal: Replace `payers.type` enum with a FK to a new `payer_types` table, expose an admin/owner‑only CRUD for payer types, and seed a default row “Family” (is_default=true). Remove all fallbacks/legacy handling of the old enum immediately.

Owner: Expenses domain
Date: 2026‑01‑19

## Current State (Evidence)
- Models already reflect FK + reference table:
  - `resources/db/domain/models.edn:25` `:payers` includes `[:payer_type_id :uuid {:foreign-key :payer_types/id :on-delete :set-null}]` and index `:idx_payers_payer_type`.
  - `resources/db/domain/models.edn:35-46` defines `:payer_types` with `:label`, `:is_default`, timestamps, unique(label) and partial unique on `is_default`.
- Generated schema migration exists (drops enum/column, adds FK + indexes):
  - `resources/db/migrations/0019_schema.edn` includes `:create-table :payer_types`, `:drop-column :payers :type`, `:drop-type :payer-type`, and `:add-column :payer_type_id`.
- Backend services/routes present for payer types:
  - Service: `src/app/domain/backend/expenses/services/payer_types.clj` (enforces single default).
  - Routes: `src/app/domain/backend/expenses/routes/payer_types.clj` + registry in `routes/core.clj` under `/admin/api/expenses` (admin-auth gated).
- Frontend wiring present:
  - Domain configs include `:payers` with `payer_type_id` columns and `:payer-types` entity (config EDNs).
  - Admin-only page guarded by power-user gate: `src/app/domain/frontend/expenses/pages/user/payer_types.cljs`.
- Legacy fallback to old `:type` label still exists and must be removed:
  - `src/app/domain/backend/expenses/handlers/user_expenses/reference_data.clj` maps `:type` → `payer_type_id` in create/update handlers.

## Scope & Outcomes
- Database: Keep 0019 schema migration; add a seed migration to insert “Family” as default once `payer_types` exists.
- Backend: Remove `:type` fallback paths in user handlers; require `:payer_type_id` (or use default payer type ID when not supplied).
- Frontend: No change required for this task (configs already migrated); keep admin/owner gating.
- Cleanup: No legacy enum, no `:type` mapping left anywhere.

## Plan

### Phase 1 — Migrations (generate/apply + seed)
- Confirm/generated schema migrations are up to date:
  - `(mig/make-all-migrations!)` (ensures consolidated `resources/db/models.edn` + regenerates schema/extended as needed).
- Apply pending migrations and sync FE config in one shot:
  - `bb migrate-and-sync-frontend-config --profile dev`
  - Repeat for `:test` if needed: `(mig/migrate! :test)`
- Seed default row via a manual SQL migration (allowed per migration docs for data ops):
  - Add `resources/db/migrations/0020_seed_payer_types.sql`:
    - `UPDATE payer_types SET is_default = false WHERE is_default = true;`
    - `INSERT INTO payer_types (id, label, is_default, created_at, updated_at)
       VALUES (gen_random_uuid(), 'Family', true, NOW(), NOW())
       ON CONFLICT (label) DO NOTHING;`
- Re-run migrations: `bb migrate-and-sync-frontend-config --profile dev`.

### Phase 2 — Backend Cleanup (remove legacy)
- In `src/app/domain/backend/expenses/handlers/user_expenses/reference_data.clj`:
  - Remove all parsing of body `:type` and the resolver `find-payer-type-id-by-label` from create/update payers.
  - Require `:payer_type_id` explicitly; if not present, resolve the default payer type via `app.domain.backend.expenses.services.payer-types/get-default-payer-type` and inject its `:id`.
- Ensure `service_configs.clj` keeps `:required-fields [:payer_type_id :label]` for `:payers`.

### Phase 3 — Verification
- Schema status: `(mig/status)`; check that 0019 + 0020 are applied and `Family` exists as default.
- FE config alignment: already performed by `bb migrate-and-sync-frontend-config`.
- Quick API checks (admin-auth required):
  - `GET /admin/api/expenses/payer-types` returns list (contains Family, is_default=true).
  - Create payer requires/accepts `payer_type_id`; no `:type` accepted anymore.

### Rollback/Alignment Notes
- If a previous manual 0019/0020 were applied with different contents, use
  `(mig/check-migrations-alignment! :dev)` to detect drift. If drift is detected, reset the dev DB or reconcile numbers using the helpers in `simple_repl.clj` (duplicate-number check/regeneration) before reapplying.

### Definition of Done
- DB contains `payer_types` with a single default row “Family”.
- `payers` uses `payer_type_id` FK; old enum/column removed.
- Admin page for “Payer Types” works and is power-user gated.
- No code paths accept or transform legacy `:type` strings; handlers require `:payer_type_id` (or auto-default when omitted).
