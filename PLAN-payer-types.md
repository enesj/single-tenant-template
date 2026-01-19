# Plan: Payer Types refactor (enum → FK) and admin gating

Context prepared via session-context-bundle:
- Generated: `bb audit-bundle` (focused on models/migrations + expenses domain)
- Evidence: resources/db/models.edn already contains `:payer_types` and `:payers` → `:payer_type_id` FK

Objectives
- Ensure Payers.type is a FK to `payer_types` (models already aligned)
- Restrict Payer Types admin APIs/visibility to admin or owner (not support)
- Seed default row: label "Family" (is_default = true when no default exists)
- Run migrations and remove legacy enum usage if any (none found)

Implementation Steps
1) Session bootstrap (done)
   - Run `bb audit-bundle --query "payer types migrations expenses admin role" --glob "src/**,resources/**,docs/**,config/**"` → capture latest `target/audit-bundles/...` path.

2) Schema verification (done)
   - Confirmed `resources/db/models.edn`:
     - `:payer_types` {`label` text, `is_default` boolean, unique default via partial index}
     - `:payers` has `:payer_type_id` FK to `payer_types/id`.

3) Backend role gating (required)
   - Add `:owner` into `wrap-admin-role` hierarchy.
   - Apply route-level middleware to `/admin/api/expenses/payer-types` requiring `:admin` (admin and owner pass, support blocked).

4) Data seed migration (required)
   - Use automigrate to create an empty SQL migration (no manual file creation):
     - `(require '[automigrate.core :as am])`
     - `(am/make {:type :empty-sql :name "seed_payer_types_family" :resources-dir "resources"})`
   - Fill forward/backward SQL:
     - Forward: insert `Family` if missing; make it default only if no default exists.
     - Backward: delete `Family` row.

5) Generate + run migrations (Clojure MCP eval)
   - `(require '[app.template.backend.migrations.simple-repl :as mig])`
   - `(mig/make-all-migrations!)` (idempotent; ensures schema/extended are aligned)
   - `(mig/migrate! :dev)`
   - `(mig/status :dev)`

6) Cleanup
   - Search and remove any residual enum references for payer type (none found in repo scan).
   - Keep only FK-based paths; no fallbacks.

7) Validate
   - Backend: hit `/admin/api/expenses/payer-types` as admin (200) and as support (403).
   - UI: Admin list renders via expenses admin events; hidden from support users by API 403 (follow-up UI gate optional).

Notes / Risk
- Role names in DB: `admins.role` enum values are ["admin" "support" "owner"].
- Existing middleware referenced `:super_admin`; add `:owner` mapping and keep `:super_admin` for future.
- Unique default enforced by partial unique index `uniq_payer_types_default`.

Commands & Snippets (for reference)
- One-shot: `bb migrate-and-sync-frontend-config --profile dev`
- REPL route: see Step 5 forms.
