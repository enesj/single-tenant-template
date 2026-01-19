-- Seed default payer type (Family) only if no default exists
-- Safe for re-runs; does not modify existing defaults

-- Insert "Family" as the default payer type if:
--   - No existing row has is_default = true
--   - No row labeled 'Family' (case-insensitive) exists
INSERT INTO payer_types (id, label, is_default, created_at, updated_at)
SELECT '9e8f7e66-5cdd-4b3e-9f17-4d97c9d84f01'::uuid, 'Family', true, NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM payer_types WHERE is_default = true
) AND NOT EXISTS (
  SELECT 1 FROM payer_types WHERE lower(label) = 'family'
);

