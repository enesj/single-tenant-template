<!-- ai: {:tags [:architecture :single-tenant] :kind :guide} -->

# Data Layer (Single-Tenant)

Single schema, no RLS. Core tables: `admins`, `users`, `audit_events`, `login_events`, plus supporting tables defined in `resources/db/models.edn`.

## Schema Source
- Canonical: `resources/db/models.edn` (only one file; no domain folders).
- Migrations: generate/apply via `app.migrations.simple-repl` (`mig/make-all-migrations!`, `mig/migrate!`). Do not hand-edit `resources/db/migrations/*`.

## Key Tables (current app)
- `admins`: platform admins (email, role, password_hash, created_at).
- `users`: end users (email, name, auth metadata).
- `audit_events`: admin/user actions; includes `principal_id`, `principal_type`, `action`, `metadata`, `created_at`.
- `login_events`: admin/user login attempts; includes `principal_id`, `principal_type`, `success`, `reason`, `ip`, `user_agent`, `created_at`.

## Access Patterns
- Services use HoneySQL + `next.jdbc`; no tenant context/RLS needed.
- Always convert PG objects (UUID/JSON/Timestamp) before responding (`admin.utils` helpers or service-level converters).
- Pagination/filters handled in route utils (`extract-pagination-params`) and service-specific builders.

## Migrations Workflow
```clojure
(require '[app.migrations.simple-repl :as mig])
(mig/make-all-migrations!)  ; regen from models.edn
(mig/migrate!)             ; apply
(mig/status)               ; inspect
```
Use `:test` profile when running against test DB: `(mig/migrate! :test)`.

## Performance/Indexes
- Keep indexes aligned with query patterns in services (principal_id/type for audit/login events, email for users/admins).
- Update `models.edn` with new indexes; regenerate migrations instead of manual SQL.

## Backups/Restore
- Use your standard pg_dump/pg_restore scripts (none are auto-run here). Run `bb backup-db` / `bb restore-db` if provided in scripts.

If you reintroduce multi-tenant tables or RLS, document the policies and context wiring here; the current app does not use them.
