-- FORWARD
DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = current_schema()
      AND table_name = 'expenses'
      AND column_name = 'is_posted'
  ) AND NOT EXISTS (
    SELECT 1
    FROM pg_indexes
    WHERE schemaname = current_schema()
      AND tablename = 'expenses'
      AND indexname = 'idx_expenses_is_posted'
  ) THEN
    EXECUTE 'CREATE INDEX idx_expenses_is_posted ON expenses USING btree (is_posted)';
  END IF;
END
$$;

-- BACKWARD
DROP INDEX IF EXISTS idx_expenses_is_posted;
