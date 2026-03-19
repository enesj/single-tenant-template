-- Seed migration: settings hierarchy foundation
-- 1) Singleton global_settings row
-- 2) Enabled currencies (BAM as base + 7 common currencies)
-- 3) Backfill expenses.original_amount and bam_amount from total_amount
-- 4) Tenant settings for each existing tenant

-- 1. Global settings singleton
INSERT INTO global_settings (id, default_currency, default_note, auto_publish_after_upload, ai_receipt_enhancement)
VALUES (gen_random_uuid(), 'BAM', NULL, false, false)
ON CONFLICT DO NOTHING;

-- 2. Enabled currencies
INSERT INTO enabled_currencies (id, code, name, is_base) VALUES
  (gen_random_uuid(), 'BAM', 'Convertible Mark', true),
  (gen_random_uuid(), 'EUR', 'Euro', false),
  (gen_random_uuid(), 'USD', 'US Dollar', false),
  (gen_random_uuid(), 'GBP', 'British Pound', false),
  (gen_random_uuid(), 'CHF', 'Swiss Franc', false),
  (gen_random_uuid(), 'RSD', 'Serbian Dinar', false),
  (gen_random_uuid(), 'TRY', 'Turkish Lira', false)
ON CONFLICT (code) DO NOTHING;

-- 3. Backfill existing expenses: all existing are BAM
UPDATE expenses
SET original_amount = total_amount,
    bam_amount = total_amount
WHERE original_amount IS NULL;

-- 4. Create tenant_settings for each existing tenant
INSERT INTO tenant_settings (id, tenant_id, email_notifications)
SELECT gen_random_uuid(), t.id, true
FROM tenants t
WHERE NOT EXISTS (
  SELECT 1 FROM tenant_settings ts WHERE ts.tenant_id = t.id
);
