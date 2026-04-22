-- FORWARD
DROP INDEX IF EXISTS idx_expenses_is_posted;
ALTER TABLE expenses DROP COLUMN IF EXISTS is_posted;

-- BACKWARD
ALTER TABLE expenses ADD COLUMN IF NOT EXISTS is_posted boolean NOT NULL DEFAULT true;
CREATE INDEX IF NOT EXISTS idx_expenses_is_posted ON expenses USING btree (is_posted);
