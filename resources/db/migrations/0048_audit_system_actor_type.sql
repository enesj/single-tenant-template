-- Add "system" value to audit_actor_type enum for external API failure logging.
-- Make actor_id nullable since system entries may not have a triggering user.
ALTER TYPE audit_actor_type ADD VALUE IF NOT EXISTS 'system';

ALTER TABLE audit_logs ALTER COLUMN actor_id DROP NOT NULL;
