# Database Management Scripts

This folder contains scripts for managing PostgreSQL databases in the single-tenant template.

## Scripts

### Backup & Restore

- **backup_db.clj** - Creates timestamped database backups
- **restore-db.clj** / **restore_db_legacy.clj** - Restores database from backup files
- **clean_restore_db.clj** - Cleans and restores database in one operation

### Database Maintenance

- **clean-db.clj** - Completely drops and recreates database (DANGER: destructive!)
- **clean_and_init_dev_db.clj** - Initializes a clean development database
- **delete_articles.clj** - Deletes all articles (dry-run by default)
- **delete_stores.clj** - Deletes all stores and unmaps store aliases (dry-run by default)
- **empty_stores_suppliers_receipts.clj** - Empties suppliers/stores/aliases/receipts (also deletes dependent expenses)

### Schema Analysis

- **compare_db_schemas.clj** - Compares database schemas between environments
- **compare_with_models.clj** - Compares database schema with application models

## Usage Examples

```bash
# Create a backup
bb backup-db --dev

# Restore from a backup file
bb restore-db --dev backups/backup_dev_2025-06-27_19-58-16.sql

# Clean restore (drop DB completely, then restore)
bb clean-restore-db --dev backups/backup_dev_2025-06-27_19-58-16.sql

# Clean database (destructive)
bb clean-db --dev

# Compare schemas (advanced)
# These helpers are still in this folder, but prefer the bb.edn tasks when available.
# If you run scripts directly, invoke them via clojure and pass the env/profile expected by the script.
```

## Safety Notes

⚠️ **WARNING**: Scripts marked as destructive will completely drop databases. Always backup before running these scripts in any environment other than local development.
