<!-- ai: {:namespaces [app.template.backend.migrations.simple-repl] :tags [:migrations :database] :kind :guide} -->

# Complete Migration Guide (Single-Tenant)

## Overview

This guide covers the migration system for the **single-tenant template**.

## Migration System Architecture

### Hierarchical Model Structure

The migration system uses a **hierarchical approach** to organize database objects by domain. In the single-tenant template, the hierarchy keeps shared/template pieces separated; policies are optional.

```
resources/db/
├── shared/           # Cross-domain utilities and types
│   ├── models.edn    # Shared models and enums
│   ├── functions.edn # Utility functions
│   ├── views.edn     # Common views
│   ├── triggers.edn  # Shared triggers
│   └── policies.edn  # Optional policies
├── template/         # Template infrastructure
│   ├── models.edn
│   ├── views.edn
│   ├── triggers.edn
│   └── policies.edn
├── domain/           # Business domain schemas
│   ├── models.edn    # Flat domain models (supported)
│   └── <domain>/     # Optional modular domains (also supported)
│       ├── models.edn
│       ├── functions.edn
│       ├── triggers.edn
│       ├── views.edn
│       └── policies.edn
└── migrations/       # Sequential migration files
    ├── 0001_schema.edn
    ├── 0002_enable_hstore_extension.sql
    └── 0071_* (and other NNNN_* files)
```

`★ Insight ─────────────────────────────────────`
**Hierarchical migrations enable domain-driven database organization**: By separating database objects into shared, template, and domain-specific layers, the system keeps reusable infrastructure isolated from feature/domain logic, making the database structure more maintainable and understandable.
`─────────────────────────────────────────────────`

### Model Consolidation Process

The system automatically consolidates hierarchical EDN files into a single `models.edn` file:

```clojure
;; From: src/app/template/backend/migrations/hierarchical_models.clj
(defn read-hierarchical-edn
  "Read EDN files from hierarchical structure and merge them.
   Supports both a direct domain file (domain/models.edn) and domain subdirectories (domain/*/models.edn)."
  [base-path file-name]
  (let [template-data      (read-consolidated-edn (str base-path "/template/" file-name))
        shared-data        (read-consolidated-edn (str base-path "/shared/" file-name))
        domain-direct-data (read-consolidated-edn (str base-path "/domain/" file-name))
        ;; discover domain subdirectories under (str base-path "/domain")
        domain-dirs        ...
        domain-subdir-data (when domain-dirs
                             (reduce (fn [acc domain-name]
                                       (merge acc
                                         (read-consolidated-edn
                                           (str base-path "/domain/" domain-name "/" file-name))))
                               {}
                               domain-dirs))
        domain-data        (merge domain-direct-data domain-subdir-data)]
    (merge template-data domain-data shared-data)))
```

Note: extended objects (functions/triggers/policies/views) are merged using the vendor hierarchical reader, which reads:

- `resources/db/template/<file>.edn`
- `resources/db/shared/<file>.edn`
- `resources/db/domain/*/<file>.edn`

## Migration Workflow

### 1. Development Setup

```bash
# Start database services
docker-compose up -d

# Launch a REPL (e.g., clj -M:nrepl) and run:
# (require '[app.template.backend.migrations.simple-repl :as mig])
# (mig/make-all-migrations!)  ;; merge models -> schema -> extended
# (mig/migrate!)              ;; apply pending migrations + verify alignment (:dev)
# (mig/migrate! :test)        ;; always keep test DB migrated too
# (mig/status)                ;; inspect status
```

### 2. Creating New Migrations

#### Adding Models

```clojure
;; resources/db/domain/example/models.edn
{:properties
 {:fields
  [[:id :uuid {:primary-key true}]
   [:name [:varchar 255] {:null false}]
   [:description :text]
   [:price-cents :bigint {:null false}]
   [:status [:enum :property-status] {:null false :default "active"}]
   [:created-at :timestamptz]
   [:updated-at :timestamptz]]

  ;; Enum and other custom types live under :types
  :types
  [[:property-status :enum {:choices ["active" "inactive" "pending" "maintenance"]}]]

  :indexes
  [[:idx-properties-status :btree {:fields [:status]}]
   [:idx-properties-name-unique :btree {:fields [:name] :unique true}]]}}
```

#### Creating Custom Types

```clojure
;; Custom types are typically defined alongside the model that uses them:
;; resources/db/domain/hosting/models.edn
{:properties
 {:fields [[:id :uuid {:primary-key true}]
           [:status [:enum :property-status] {:null false}]]
  :types  [[:property-status :enum {:choices ["active" "inactive" "pending" "maintenance"]}]]}}
```

#### Adding Functions

```clojure
;; resources/db/domain/hosting/functions.edn
{:calculate-property-occupancy
 {:up "CREATE OR REPLACE FUNCTION calculate_property_occupancy(property_uuid UUID)
       RETURNS DECIMAL(5,2) AS $$
       BEGIN
         RETURN (
           SELECT CASE
             WHEN COUNT(*) = 0 THEN 0.0
             ELSE ROUND((COUNT(*) * 100.0 / (
               SELECT capacity FROM properties WHERE id = property_uuid
             )), 2)
           END
           FROM bookings b
           WHERE b.property_id = property_uuid
             AND b.status = 'confirmed'
             AND b.end_date >= CURRENT_DATE
         );
       END;
       $$ LANGUAGE plpgsql;"
  :down "DROP FUNCTION IF EXISTS calculate_property_occupancy(UUID);"}}
```

### 3. Migration Commands (REPL-first)

```clojure
(require '[app.template.backend.migrations.simple-repl :as mig])

;; Merge models -> schema -> extended, then apply (dev + test)
(mig/make-all-migrations!)
(mig/migrate!)
(mig/migrate! :test)
(mig/status)

;; Check test DB status
(mig/status :test)

;; Explain SQL for a migration number
(mig/explain 42)

;; Check that the DB is aligned with:
;; - files in resources/db/migrations
;; - hierarchical models/extended EDNs under resources/db/{template,domain,shared}
;; Prints a report and returns it.
(mig/check-migrations-alignment! :dev)

;; Same check, but throws if there are any differences.
(mig/assert-migrations-aligned! :dev)
```

### 4. Frontend-config alignment (migration-adjacent)

After schema changes (or UI config edits), validate and optionally sync frontend config EDNs with the consolidated schema:

```bash
# Validate config shape + schema alignment
bb validate-frontend-config

# Preview sync changes (dry-run, fails on mismatches)
bb sync-frontend-config

# Apply sync changes (explicit)
bb sync-frontend-config --apply
```

You can also do this directly from the REPL after migrations:

```clojure
;; Apply sync + validate via migrate! (opt-in)
(mig/migrate! :dev {:sync-frontend-config? true})

;; Forward args to the bb tasks (e.g., limit to a domain)
(mig/migrate! :dev {:sync-frontend-config? true
                    :frontend-config-args ["--only" "expenses"]})
```

Or run a one-shot command outside the REPL:

```bash
bb migrate-and-sync-frontend-config
```

Note: these commands **do not** modify the database. They keep UI config aligned with `resources/db/models.edn`.

#### Optional BB helpers

```bash
# Clean database
bb clean-db --dev

# Start app (will pick up already-applied migrations)
bb run-app

# Check migrations + schema alignment (prints report, exit 0/1/2)
bb check-migrations dev
```

## Migration File Format

### Sequential Migrations

Sequential migrations in `resources/db/migrations/` handle versioned changes:

```clojure
;; resources/db/migrations/0071_schema.edn
[
 ;; Add new column to existing table
 {:action :add-column
  :model-name :accounts
  :field-name :billing-email
  :options {:type [:varchar 255] :null true}}

 ;; Create new index
 {:action :create-index
  :model-name :properties
  :index-name :idx-properties-status
  :options {:fields [:status] :type :btree}}

 ;; Modify existing column
 {:action :alter-column
  :model-name :users
  :field-name :email
  :options {:type [:varchar 255] :null false :unique true}}
]
```

### Model Definitions

Model definitions support comprehensive table specifications:

```clojure
{:users
 {:fields [[:id :uuid {:primary-key true}]
           [:email [:varchar 255] {:null false :unique true}]
           [:created-at :timestamptz]
           [:updated-at :timestamptz]]
  :indexes [[:idx-users-created-at :btree {:fields [:created-at]}]]}}
```

### Enum Changes (Roles/Statuses)

PostgreSQL enums are effectively **append-only** in the Automigrate workflow:

- Removing values from `:choices` (e.g. `["active" "inactive" "pending" "suspended"]` → `["active" "inactive" "suspended"]`) is **not supported** by the schema diff engine.
- When Automigrate detects that the new model has *fewer* enum choices than the current DB type, it raises a migration error instead of generating unsafe SQL (dropping enum values while data may still reference them).

Recommended patterns:

- **Tighten behaviour with CHECK constraints, not enum removal**  
  - Keep the enum’s full `:choices` set.  
  - Restrict actual usage at the column level:
    - Example (users):
      - Enum remains `[:user-status {:choices [\"active\" \"inactive\" \"pending\" \"suspended\"]}]`
      - Column enforces a smaller set:
        - `[:status [:enum :user-status] {:null false :default \"active\" :check [:raw \"status in ('active','inactive','suspended')\"]}]`
    - Example (admins):
      - `[:status [:enum :admin-status] {:null false :default \"active\" :check [:raw \"status in ('active','suspended')\"]}]`
    - Example (owner only for admins, not users):
      - Enum stays `[:user-role {:choices [\"owner\" \"admin\" \"member\" \"viewer\"]}]`
      - Column for users prevents `owner`:
        - `[:role [:enum :user-role] {:null false :default \"member\" :check [:raw \"role in ('admin','member','viewer')\"]}]`

- **If you truly need to remove an enum value**  
  - Write an explicit, manual data+type migration (not via `:choices` diff alone):
    1. Clean/transform data so no rows use the to-be-removed value.
    2. Verify with queries (e.g., `SELECT * FROM users WHERE status = 'pending';`).
    3. Add a hand-written SQL migration (`ALTER TYPE ... DROP VALUE ...`) and apply it consciously.

This keeps Automigrate migrations safe and predictable, while still letting you tighten role/status semantics over time.
```

#### Template-specific metadata (validation/UI)

In the single-tenant template, models may include extra per-field metadata such as `:validation`, `:form`, `:admin`, `:security`, or `:ui` settings:

```clojure
[:email
 [:varchar 255]
 {:null false
  :unique true
  :check [:raw "email ~* '^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$'"]
  :validation {:type :email
               :constraints {:pattern "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
                             :max-length 255}
               :messages {:invalid "Please enter a valid email address"
                          :required "Email is required"}
               :ui {:input-type "email"
                    :placeholder "Enter email"}}}]
```

These keys are **for the application layer only** (validation and UI) and are not understood by Automigrate. Before generating schema migrations, `app.template.backend.migrations.function-defaults/load-and-preprocess-models` strips these metadata keys and normalises `:default` values into the format Automigrate expects. This keeps one canonical EDN model file that serves both:

- the migration system (via the preprocessed `db/models_processed.edn`), and
- the frontend/backend validation layer (via the original `models.edn`).

## Policies

Policies are optional in the single-tenant template. Use `.pol` files when you need explicit SQL policy blocks.
```

## Database Functions and Triggers

### Utility Functions

```clojure
;; resources/db/shared/functions.edn
{:normalize-name
 {:up "CREATE OR REPLACE FUNCTION normalize_name(name text)
       RETURNS text AS $$
       BEGIN
         RETURN trim(lower(name));
       END;
       $$ LANGUAGE plpgsql IMMUTABLE;"
  :down "DROP FUNCTION IF EXISTS normalize_name(text);"}}
```

### Automated Triggers

In the template we model timestamp automation via the **extended EDN pipeline**:

- Shared functions live in `resources/db/shared/functions.edn`
- Triggers live in:
  - `resources/db/{template,shared}/triggers.edn`
  - `resources/db/domain/*/triggers.edn`
- Each entry is a map of `:name {:up \"FORWARD SQL\" :down \"BACKWARD SQL\"}`

```clojure
;; resources/db/shared/functions.edn
{:update-updated-at-column
 {:up "CREATE OR REPLACE FUNCTION update_updated_at_column()
       RETURNS TRIGGER AS $$
       BEGIN
         NEW.updated_at = CURRENT_TIMESTAMP;
         RETURN NEW;
       END;
       $$ LANGUAGE plpgsql;"
  :down "DROP FUNCTION IF EXISTS update_updated_at_column();"}}

;; resources/db/template/triggers.edn
{:users-updated-at-trigger
 {:up "CREATE TRIGGER users_updated_at
       BEFORE UPDATE ON users
       FOR EACH ROW
       EXECUTE FUNCTION update_updated_at_column();"
  :down "DROP TRIGGER IF EXISTS users_updated_at ON users;"}}
```

After editing these EDN files, regenerate and apply extended migrations from a REPL:

```clojure
(require '[app.template.backend.migrations.simple-repl :as mig])
(mig/make-all-migrations!)  ;; or (mig/regenerate-extended-migrations-clean!)
(mig/migrate!)              ;; apply to :dev
(mig/migrate! :test)        ;; keep test DB migrated too
```

## Views and Complex Queries

### Materialized Views

```clojure
;; resources/db/domain/example/views.edn
{:property-summary-view
 {:up "CREATE MATERIALIZED VIEW property_summary AS
       SELECT
         p.id,
         p.name,
         COUNT(b.id) as booking_count,
         COALESCE(SUM(b.total_amount), 0) as total_revenue,
         p.created_at
       FROM properties p
       LEFT JOIN bookings b ON b.property_id = p.id
       GROUP BY p.id, p.name, p.created_at;"
  :down "DROP MATERIALIZED VIEW IF EXISTS property_summary;"}}
```

If you need indexes on a materialized view, add them in a separate manual SQL migration (`.sql`) or include additional `CREATE INDEX ...;` statements in the `:up` string and the corresponding `DROP INDEX ...;` in `:down`.

### Refresh Strategies

```bash
# Refresh materialized views
psql -d bookkeeping -c "REFRESH MATERIALIZED VIEW property_summary;"

# Create automated refresh function
psql -d bookkeeping -c "
CREATE OR REPLACE FUNCTION refresh_property_summary()
RETURNS void AS $$
BEGIN
  REFRESH MATERIALIZED VIEW CONCURRENTLY property_summary;
END;
$$ LANGUAGE plpgsql;"
```

## Rollback Procedures

### Backup Before Migration

```bash
# Create database backup
bb backup-db --dev

# Or manual backup
pg_dump -h localhost -U user -d bookkeeping > backup_before_migration.sql
```

### Migration Rollback

```clojure
;; Roll back all migrations (to 0) or to a specific number
(mig/migrate-to! 0)
(mig/migrate-to! 70)

;; Full database restore
;; (outside automigrate; use your backup file)
;; bb clean-restore-db --dev backup_before_migration.sql
```

### Manual Rollback SQL

```sql
-- Example: Rollback column addition
ALTER TABLE accounts DROP COLUMN IF EXISTS billing_email;

-- Example: Rollback index creation
DROP INDEX IF EXISTS idx_properties_status;

-- Example: Rollback constraint modification
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_email_key;
```

## Troubleshooting

### Common Migration Issues

#### 1. Permission Errors

```bash
# Check database user permissions
psql -h localhost -U user -d bookkeeping -c "\du"

# Grant necessary permissions
psql -h localhost -U postgres -d bookkeeping -c "
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO user;"
```

#### 2. Constraint Violations

```bash
# Check constraint details
psql -d bookkeeping -c "
SELECT conname, conrelid::regclass, conkey
FROM pg_constraint
WHERE conrelid = 'properties'::regclass;"

# Temporarily disable constraints during migration
psql -d bookkeeping -c "
ALTER TABLE properties DISABLE TRIGGER ALL;
-- Run migration
ALTER TABLE properties ENABLE TRIGGER ALL;"
```

#### 3. Lock Issues

```bash
# Check for active locks
psql -d bookkeeping -c "
SELECT pid, state, query
FROM pg_stat_activity
WHERE state = 'active'
  AND query LIKE '%migration%';"

# Kill blocking sessions
psql -d bookkeeping -c "
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE state = 'active'
  AND pid <> pg_backend_pid();"
```

### Debugging Migrations

#### Verbose Logging

```clojure
;; Inspect pending/applied from REPL
(mig/status)
```

#### Manual SQL Execution

```bash
# Execute a manual SQL migration file
psql -d bookkeeping -f resources/db/migrations/0002_enable_hstore_extension.sql
```

```clojure
;; Or inspect SQL from the REPL
(mig/explain 1 :direction :forward)
```

## Performance Optimization

### Migration Performance

#### Large Tables

```clojure
;; Prefer a manual SQL migration for advanced index options like CONCURRENTLY.
;; Automigrate supports index type selection (e.g. :gin) but not every PostgreSQL index option.
{:action :create-index
 :model-name :properties
 :index-name :idx-properties-search
 :options {:fields [:search_vector] :type :gin}}
```

#### Batch Operations

```sql
-- Process data in batches for large migrations
DO $$
DECLARE
  batch_size INTEGER := 1000;
  offset_val INTEGER := 0;
  total_processed INTEGER := 0;
BEGIN
  LOOP
    UPDATE properties
    SET search_vector = to_tsvector('english', name || ' ' || description)
    WHERE id IN (
      SELECT id FROM properties
      ORDER BY created_at
      LIMIT batch_size OFFSET offset_val
    );

    total_processed := total_processed + batch_size;
    offset_val := offset_val + batch_size;

    EXIT WHEN NOT FOUND;
  END LOOP;

  RAISE NOTICE 'Processed % records', total_processed;
END $$;
```

### Query Optimization

```sql
-- Analyze table statistics after migration
ANALYZE properties;

-- Update planner statistics
VACUUM ANALYZE properties;

-- Check query plans
EXPLAIN ANALYZE SELECT * FROM properties WHERE status = $1;
```

## Production Deployment

### Staging Validation

```bash
# Create staging database from production backup
pg_dump -h prod-host -U prod-user production_db | psql -h staging-host -U staging-user staging_db
```

```clojure
;; Run from a REPL configured for staging
(require '[app.template.backend.migrations.simple-repl :as mig])
(mig/migrate! :staging)
(mig/status :staging)
```

### Production Migration Process

```bash
# 1. Create backup
bb backup-db --prod

# 2. Put application in maintenance mode
./scripts/deployment/enable-maintenance.sh
```

```clojure
;; 3. Run migrations
(mig/migrate! :prod)
(mig/status :prod)
```

```bash
# 4. Verify migration success
./scripts/validation/post-migration-check.sh

# 5. Disable maintenance mode
./scripts/deployment/disable-maintenance.sh

# 6. Monitor application health
./scripts/monitoring/health-check.sh
```

### Zero-Downtime Migrations

```sql
-- Example: Add nullable column first
ALTER TABLE properties ADD COLUMN new_column VARCHAR(255);

-- Backfill data in batches
UPDATE properties SET new_column = default_value WHERE id IN (...);

-- Add NOT NULL constraint after backfill
ALTER TABLE properties ALTER COLUMN new_column SET NOT NULL;
```

## Best Practices

### Migration Design

1. **Backward Compatible**: Design migrations to work with both old and new application versions
2. **Idempotent**: Migrations should be safe to run multiple times
3. **Testable**: Validate migrations on realistic data volumes
4. **Rollbackable**: Always have a rollback strategy

### Data Safety

1. **Backup First**: Never run migrations without a recent backup
2. **Transaction Boundaries**: Group related changes in transactions
3. **Validation**: Verify data integrity after migrations
4. **Monitoring**: Watch for performance issues post-migration

### Development Workflow

1. **Feature Branches**: Create feature-specific migrations
2. **Code Review**: Review migration SQL for correctness
3. **Staging Testing**: Test on staging before production
4. **Documentation**: Document breaking changes and data transformations

---

**Related Documentation**:
- [Database Architecture](../architecture/data-layer.md) - Database design principles
- [Operations Guide](../operations/README.md) - Database backup and recovery

*Last Updated: 2025-11-12*
