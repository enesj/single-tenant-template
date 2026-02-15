<!-- ai: {:tags [:architecture :single-tenant] :kind :guide} -->

# Data Layer (Single-Tenant)

Single schema, no RLS. Core tables: `admins`, `users`, `audit_logs`, `login_events`, plus supporting tables defined in `resources/db/models.edn`.

All user/application tables in `resources/db/models.edn` include both `created_at` and `updated_at`, except `automigrate_migrations`.

## Schema Source
- Canonical inputs: source EDN in `resources/db/{template,shared,domain}/**`.
- Generated output: `resources/db/models.edn` (auto-merged from template/shared/domain sources).
- Migrations: generate/apply via `app.template.backend.migrations.simple-repl` (`mig/make-all-migrations!`, `mig/migrate!`). Do not hand-edit `resources/db/migrations/*`. After any migration change, apply to both dev and test DBs.

## Key Tables (current app)
- `admins`: platform admins (email, role, password_hash, created_at, updated_at).
- `users`: end users (email, name, auth metadata, created_at, updated_at).
- `audit_logs`: admin/user actions; includes `actor_id`, `actor_type`, `action`, `metadata`, `created_at`, `updated_at`.
- `login_events`: admin/user login attempts; includes `principal_id`, `principal_type`, `success`, `reason`, `ip`, `user_agent`, `created_at`, `updated_at`.

## Access Patterns
- Services use HoneySQL + `next.jdbc`; no tenant context/RLS needed.
- Always convert PG objects (UUID/JSON/Timestamp) before responding (`admin.utils` helpers or service-level converters).
- Pagination/filters handled in route utils (`extract-pagination-params`) and service-specific builders.
- `updated_at` is maintained in PostgreSQL via `update_updated_at_column()` and per-table `<table>_updated_at` triggers defined in `resources/db/template/triggers.edn`; app update code should not set `updated_at` manually.

## Migrations Workflow
```clojure
(require '[app.template.backend.migrations.simple-repl :as mig])
(mig/make-all-migrations!)  ; regen from canonical sources
(mig/migrate!)             ; apply to :dev
(mig/migrate! :test)       ; apply to :test
(mig/status)               ; inspect :dev status
(mig/status :test)         ; inspect :test status
```

Frontend config alignment (migration-adjacent):

```clojure
;; Apply sync + validate via migrate! (opt-in)
(mig/migrate! :dev {:sync-frontend-config? true})
```

```bash
# One-shot command outside the REPL
bb migrate-and-sync-frontend-config
```

## Performance/Indexes
- Keep indexes aligned with query patterns in services (actor_id/type for `audit_logs`, principal_id/type for `login_events`, email for users/admins).
- Update `models.edn` with new indexes; regenerate migrations instead of manual SQL.

## Backups/Restore
- Use your standard pg_dump/pg_restore scripts (none are auto-run here). Run `bb backup-db` / `bb restore-db` if provided in scripts.

If you reintroduce multi-tenant tables or RLS, document the policies and context wiring here; the current app does not use them.
