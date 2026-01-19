-- FORWARD

-- Drop old index on payers.type if it exists
DROP INDEX IF EXISTS idx_payers_type;

-- Drop legacy column if it exists (removes dependency on enum type)
ALTER TABLE IF EXISTS payers DROP COLUMN IF EXISTS type;

-- Drop legacy enum type if it still exists
DROP TYPE IF EXISTS payer_type;

-- BACKWARD
-- No-op (cannot safely recreate legacy enum/column without original data definitions)
