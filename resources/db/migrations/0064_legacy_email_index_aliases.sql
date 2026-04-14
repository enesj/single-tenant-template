CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email ON users (email_lookup_hash);
CREATE UNIQUE INDEX IF NOT EXISTS idx_admins_email ON admins (email_lookup_hash);
CREATE INDEX IF NOT EXISTS idx_admin_invitations_email ON admin_invitations (email_lookup_hash);
CREATE INDEX IF NOT EXISTS idx_tenant_invitations_email ON tenant_invitations (email_lookup_hash);
