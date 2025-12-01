<!-- ai: {:tags [:reference :database :single-tenant] :kind :reference} -->

# Database Schema Reference (Single-Tenant Template)

Canonical schema lives in `resources/db/models.edn` and is generated via the migrations helper in `src/app/migrations/simple_repl.clj` (`mig/make-all-migrations!`, `mig/migrate!`, etc.). There is no tenant/RLS layer in this template.

## Tables & Types

### `users`
- **Fields**: `id` (uuid, pk), `email` (varchar 255, unique, validated), `full_name`, `password_hash` (text, required), `role` (`user-role`, default `member`), `status` (`user-status`, default `active`), `last_login_at` (timestamptz), `avatar_url`, `auth_provider` (text, default `"password"`), `provider_user_id`, `created_at` (timestamptz, default `NOW()`), `updated_at` (timestamptz, default `NOW()`).
- **Enums**: `user-role` = `admin | member | viewer`; `user-status` = `active | inactive | suspended`.
- **Indexes**: `idx_users_email` (unique), `idx_users_status`, `idx_users_created_at`, `idx_users_auth_provider_provider_user_id_external` (unique where auth_provider <> password).

### `admins`
- **Fields**: `id` (uuid, pk), `email` (varchar 255, unique, validated), `full_name`, `password_hash` (text, required), `role` (`admin-role`, default `admin`), `status` (`admin-status`, default `active`), `last_login_at` (timestamptz), `created_at` (timestamptz, default `NOW()`), `updated_at` (timestamptz, default `NOW()`).
- **Enums**: `admin-role` = `admin | support | owner`; `admin-status` = `active | suspended`.
- **Indexes**: `idx_admins_email` (unique), `idx_admins_status`.

### `email_verification_tokens`
- **Fields**: `id` (uuid, pk), `user_id` (fk → `users.id`, cascade delete), `token` (varchar 255, unique, required), `expires_at` (timestamptz, required), `attempts` (int, default 0), `last_attempted_at` (timestamptz), `used_at` (timestamptz), `created_at` (timestamptz, default `NOW()`).
- **Indexes**: `idx_email_tokens_user`, `idx_email_tokens_token` (unique), `idx_email_tokens_expires`.

### `audit_logs`
- **Fields**: `id` (uuid, pk), `actor_type` (`audit-actor-type`), `actor_id` (uuid), `action` (text, required), `target_type` (text), `target_id` (uuid), `metadata` (jsonb), `ip` (text), `user_agent` (text), `created_at` (timestamptz, default `NOW()`).
- **Enums**: `audit-actor-type` = `user | admin`.
- **Indexes**: `idx_audit_logs_created_at` (created_at), `idx_audit_logs_actor` (actor_type, actor_id).
- **Usage**: Populated by admin actions (update/create/delete/login/logout, impersonation, verification, etc.) and exposed via `/admin/api/audit` and `/admin/api/user-management/activity/:id`.

### `login_events`
- **Fields**: `id` (uuid, pk), `principal_type` (`login-principal-type`), `principal_id` (uuid), `success` (boolean), `reason` (text), `ip` (text), `user_agent` (text), `created_at` (timestamptz, default `NOW()`).
- **Enums**: `login-principal-type` = `user | admin`.
- **Indexes**: `idx_login_events_created_at` (created_at), `idx_login_events_principal` (principal_type, principal_id).
- **Usage**: Records successful and failed logins for admins and users. Surface via `/admin/api/login-events` and per-user activity modal.

## Notes
- Schema is single-database, single-tenant; no `tenant_id` columns or RLS policies are present.
- Always edit `resources/db/models.edn` then regenerate migrations; do not hand-edit `resources/db/migrations/*`.
- For monitoring data (audit/login events), frontend expects epoch millis for `created_at`; backend services normalize PG timestamps accordingly.
