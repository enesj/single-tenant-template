# Database Management Scripts

This folder contains scripts for managing PostgreSQL databases in the hosting application.

## Scripts

### Backup & Restore
- **backup_db.clj** - Creates timestamped database backups
- **restore-db.clj** / **restore_db.clj** - Restores database from backup files
- **clean_restore_db.clj** - Cleans and restores database in one operation

### Database Maintenance
- **clean-db.clj** - Completely drops and recreates database (DANGER: destructive!)
- **clean-and-init-dev-db.clj** - Initializes clean development database
- **init-dev-db.clj** - Sets up development database with initial data

### Schema Analysis
- **compare_db_schemas.clj** - Compares database schemas between environments
- **compare_with_models.clj** - Compares database schema with application models

### Tenant Management
- **list-tenants.clj** - Lists all tenants in the multi-tenant system

## Usage Examples

```bash
# Create backup
bb database/backup_db.clj dev

# Clean and restore development database
bb database/clean_restore_db.clj dev backup_dev_2023-01-01_12-00-00.sql

# Compare schemas
bb database/compare_db_schemas.clj dev prod

# List all tenants
bb database/list-tenants.clj dev
```

## Safety Notes

⚠️ **WARNING**: Scripts marked as destructive will completely drop databases. Always backup before running these scripts in any environment other than local development.
