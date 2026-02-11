<!-- ai: {:tags [:migrations :overview] :kind :overview} -->

# Database Migrations (Single-Tenant)

This folder documents the migration system for the **single-tenant template**.

## 📚 Documentation Structure

### Core Documentation
- **[Migration Overview](./migration-overview.md)** - System architecture and workflows
- **[Complete Guide](./complete-guide.md)** - Detailed workflows, formats, and operations

## 🚀 Quick Navigation

**Need to...**
- **Understand the system?** → [Migration Overview](./migration-overview.md)
- **See full workflows?** → [Complete Guide](./complete-guide.md)

## 🔧 System Overview

This project uses the **automigrate** library with a single-tenant default.

- **📦 Models-driven**: Database schema defined in `resources/db/models.edn` (consolidated from `resources/db/{template,shared,domain}`)
- **🏢 Single-tenant by default**
- **🔄 Auto-generation**: Migrations automatically generated from model changes and extended EDN (functions, triggers, policies, views)
- **🛡️ Type-safe**: Comprehensive field type handling with JSONB support
- **⚡ Environment-aware**: Separate dev, test, and production configurations

## 📁 Canonical Inputs (important)

- Only canonical EDN files are ingested by the migration generator:
  - `resources/db/models.edn` (generated)
  - **Model sources (edit these):**
    - `resources/db/template/models.edn`
    - `resources/db/shared/models.edn`
    - `resources/db/domain/models.edn` (optional)
    - `resources/db/domain/*/models.edn` (optional per-domain modules)
  - **Extended sources (functions/triggers/policies/views):**
    - Template + shared are read from the directory root:
      - `resources/db/{template,shared}/{functions,triggers,policies,views}.edn` (each file is optional)
    - Domain extended objects are read from domain subdirectories:
      - `resources/db/domain/*/{functions,triggers,policies,views}.edn`
- Any other EDN files (e.g., `missing_policies.edn`, `rls_enablement.edn`) are ignored by the vendor tool. Merge their contents into the canonical `policies.edn` files.

**Note on `(mig/sync-db-to-edn!)`:** this is a DB→EDN *capture* utility. It writes snapshots under `resources/db/{functions,triggers,policies,views}/...`, but those output folders are **not** canonical inputs for migration generation. If you use this for discovery, copy/merge the captured SQL into the hierarchical inputs above.

**Extended EDN shape (functions/triggers/policies/views)**:

- Files are single maps: `{:name {:up "FORWARD SQL" :down "BACKWARD SQL"}}`
- Example for the shared `updated_at` function and a `users` trigger:

  ```clojure
  ;; resources/db/shared/functions.edn
  {:update-updated-at-column
   {:up "CREATE OR REPLACE FUNCTION update_updated_at_column() ..."
    :down "DROP FUNCTION IF EXISTS update_updated_at_column();"}}

  ;; resources/db/template/triggers.edn
  {:users-updated-at-trigger
   {:up "CREATE TRIGGER users_updated_at
         BEFORE UPDATE ON users
         FOR EACH ROW
         EXECUTE FUNCTION update_updated_at_column();"
    :down "DROP TRIGGER IF EXISTS users_updated_at ON users;"}}
  ```

## 🧪 REPL Utilities (preferred path)

Run migrations through `src/app/template/backend/migrations/simple_repl.clj` instead of the old deps aliases:

```clojure
(require '[app.template.backend.migrations.simple-repl :as mig])

(mig/make-all-migrations!)            ;; merge models → schema → extended
(mig/migrate!)                       ;; apply pending migrations + verify alignment (:dev)
(mig/migrate! :test)                 ;; always keep test DB migrated too
(mig/status)                         ;; list applied/pending
(mig/regenerate-extended-migrations-clean!) ;; prune extended and regenerate
(mig/check-duplicate-migrations)     ;; find duplicate numbers
(mig/sync-db-to-edn!)                ;; optional DB→EDN capture
```

## 📞 Support

- **Issues**: Check [Troubleshooting Guide](./troubleshooting.md)
- **Commands**: See [Command Reference](./command-reference.md)
- **Architecture**: Review [Migration Overview](./migration-overview.md)

---

*Last updated: January 2026*
