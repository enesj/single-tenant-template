---
name: migrations
description: "Complete migration workflow guide - models, schema, functions, triggers, policies, views"
tags: ["migrations", "database", "schema", "automigrate", "models", "postgresql"]
---

# migrations

## Goal
Safely evolve the database schema through automigrate using canonical EDN sources.

## Core principle
**Never edit `resources/db/models.edn`** - it's auto-generated from hierarchical sources.

## Canonical sources (edit these)

### Models
- `resources/db/template/models.edn` - Application-specific entities
- `resources/db/shared/models.edn` - Shared models across apps
- `resources/db/domain/models.edn` - Flat domain models (optional)
- `resources/db/domain/*/models.edn` - Per-domain modules (optional)

### Extended objects (optional per file)
- `resources/db/{template,shared}/{functions,triggers,policies,views}.edn`
- `resources/db/domain/*/{functions,triggers,policies,views}.edn`

## REPL workflow (primary)

### Quick start
```clojure
(require '[app.template.backend.migrations.simple-repl :as mig])

;; Merge hierarchical models → schema → extended migrations
(mig/make-all-migrations!)

;; Apply pending migrations + verify alignment (default :dev)
(mig/migrate!)

;; Check status
(mig/status)
```

### Target other profiles
```clojure
(mig/migrate! :test)
(mig/status :test)

;; With frontend config sync (migration-adjacent)
(mig/migrate! :dev {:sync-frontend-config? true})

;; Forward args to bb sync (e.g., limit to domain)
(mig/migrate! :dev {:sync-frontend-config? true
                    :frontend-config-args ["--only" "expenses"]})
```

### Other commands
```clojure
(mig/migrate-to! 0)              ;; Rollback all
(mig/migrate-to! 70)             ;; Rollback to migration 70
(mig/explain 42)                 ;; Explain SQL for migration 42
(mig/check-duplicate-migrations) ;; Find duplicate numbers
(mig/regenerate-extended-migrations-clean!) ;; Prune + regenerate extended

;; Alignment checks
(mig/check-migrations-alignment! :dev)  ;; Returns report map
(mig/assert-migrations-aligned! :dev)   ;; Throws if misaligned
```

## Extended EDN format

### Functions/triggers/policies/views
Each file is a single map: `{:name {:up "FORWARD SQL" :down "BACKWARD SQL"}}`

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

## Model definition format

```clojure
;; resources/db/domain/example/models.edn
{:properties
 {:fields
  [[:id :uuid {:primary-key true}]
   [:name [:varchar 255] {:null false}]
   [:description :text]
   [:status [:enum :property-status] {:null false :default "active"}]
   [:created-at :timestamptz]
   [:updated-at :timestamptz]]

  :types
  [[:property-status :enum {:choices ["active" "inactive" "pending" "maintenance"]}]]

  :indexes
  [[:idx-properties-status :btree {:fields [:status]}]
   [:idx-properties-name-unique :btree {:fields [:name] :unique true}]]}}
```

## BB helpers (bash)

```bash
# Backup before major changes
bb backup-db --dev
bb restore-db --dev /path/to/backup.sql

# Clean database
bb clean-db --dev

# Check migrations + schema alignment (exit 0/1/2)
bb check-migrations dev

# Frontend config sync (migration-adjacent)
bb validate-frontend-config
bb sync-frontend-config          # Dry-run
bb sync-frontend-config --apply  # Apply

# One-shot: migrate + sync apply + validate
bb migrate-and-sync-frontend-config
```

## Enum changes (critical!)

PostgreSQL enums are **append-only** in Automigrate:

- ❌ Removing values from `:choices` is NOT supported
- ✅ Use CHECK constraints to tighten behavior instead

```clojure
;; Keep full enum choices
[:user-status {:choices ["active" "inactive" "pending" "suspended"]}]

;; Tighten at column level with CHECK
[:status [:enum :user-status]
 {:null false :default "active"
  :check [:raw "status in ('active','inactive','suspended')"]}]
```

If you truly need to remove a value:
1. Write manual data migration to clean references
2. Add manual SQL migration with `ALTER TYPE ... DROP VALUE ...`

## Model metadata keys

Template-specific keys (for validation/UI only, stripped before schema generation):
- `:validation` - Validation rules
- `:form` - Form configuration
- `:admin` - Admin panel settings
- `:security` - Security rules
- `:ui` - UI hints

These are NOT understood by Automigrate and are removed during preprocessing.

## Migration file types

- `.edn` - Schema migrations (auto-generated from model diffs)
- `.sql` - Manual SQL operations
- `.fn` - Database functions
- `.trg` - Triggers
- `.pol` - Policy SQL blocks (optional)
- `.view` - Database views

## Common issues

### "My model changes disappeared!"
You edited `resources/db/models.edn` directly.
- ❌ Never edit `models.edn` - it's auto-generated
- ✅ Edit source files in `resources/db/{template,shared,domain}/models.edn`
- Run `(mig/make-all-migrations!)` to merge sources → `models.edn`

### File structure guide
```
resources/db/
├── models.edn              ❌ Generated - DON'T EDIT
├── template/               ✅ Edit app models here
│   ├── models.edn
│   ├── triggers.edn
│   └── views.edn
├── shared/                 ✅ Edit shared models here
│   ├── models.edn
│   ├── functions.edn
│   └── triggers.edn
├── domain/                 ✅ Edit domain models here
│   └── models.edn
└── migrations/             ❌ Auto-generated - DON'T EDIT
```

## Before schema changes
1. **Backup**: `bb backup-db --dev`
2. **Check status**: `(mig/status)`
3. **Verify alignment**: `(mig/check-migrations-alignment! :dev)`

## After migrations
1. **Verify**: `(mig/status)`
2. **Check alignment**: `(mig/assert-migrations-aligned! :dev)`
3. **Test application**: Ensure app functions correctly
4. **Frontend config**: `bb validate-frontend-config` (or `bb sync-frontend-config --apply`)

## Production deployment
```bash
# 1. Backup
bb backup-db --prod

# 2. Run migrations (from REPL in production config)
(mig/migrate! :prod)
(mig/status :prod)

# 3. Verify with staging first
(mig/migrate! :staging)
(mig/status :staging)
```

## Debugging
```clojure
;; See all pending/applied migrations
(mig/status)

;; Explain what a migration does
(mig/explain 42)
(mig/explain 42 :direction :backward)

;; Check for duplicate migration numbers
(mig/check-duplicate-migrations)

;; Summary of all migrations
(mig/migration-summary)
```
