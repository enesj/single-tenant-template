-- Fix enum typos: backlog_status and backlog_type.
-- Idempotent: skips renames when the typo values no longer exist
-- (e.g. fresh DBs where 0001_schema.edn already has correct values).

DO $$
BEGIN
  -- backlog_status
  IF EXISTS (SELECT 1 FROM pg_enum WHERE enumlabel = 'In progres'
             AND enumtypid = 'backlog_status'::regtype) THEN
    ALTER TYPE backlog_status RENAME VALUE 'In progres' TO 'In progress';
  END IF;
  IF EXISTS (SELECT 1 FROM pg_enum WHERE enumlabel = 'Need improvments'
             AND enumtypid = 'backlog_status'::regtype) THEN
    ALTER TYPE backlog_status RENAME VALUE 'Need improvments' TO 'Need improvements';
  END IF;

  -- backlog_type
  IF EXISTS (SELECT 1 FROM pg_enum WHERE enumlabel = 'Improvment'
             AND enumtypid = 'backlog_type'::regtype) THEN
    ALTER TYPE backlog_type RENAME VALUE 'Improvment' TO 'Improvement';
  END IF;
END $$;
