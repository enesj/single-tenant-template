<!-- ai: {:tags [:architecture :single-tenant] :kind :guide} -->

# Users and Roles (Single-Tenant)

This app has no tenants. Two principal actor types exist: **admins** (platform operators) and **users** (end users). Admin actions and logins are monitored via audit/login events.

## Entities
- **admins**: Stored in `admins` table. Fields: email, password_hash, role (e.g., `owner`, `admin`), timestamps.
- **users**: Stored in `users` table. Fields: email, name, auth metadata, created/updated timestamps.
- **audit_events**: Records admin/user actions. Fields: principal_id, principal_type (`admin|user`), action, metadata, created_at.
- **login_events**: Records login attempts. Fields: principal_id, principal_type, success, reason, ip, user_agent, created_at.

## Roles
- Admin roles are simple string roles (e.g., `owner`, `admin`). Authorization happens in middleware/handlers, not via a tenant matrix.
- Users are currently flat (no tenant-scoped roles); pages/permissions are enforced at the route/service level.

## Authentication
- Admin auth: `app.backend.middleware.admin/wrap-admin-authentication` protects `/admin/api/**`. Tokens returned by `/admin/api/auth/login`.
- User auth (if enabled) is separate; user logins are still captured in `login_events` for monitoring.

## Monitoring Hooks
- Use `admin-utils/log-admin-action` to emit audit entries when adding new admin operations.
- Login events are recorded via `app.backend.services.monitoring.login-events`; ensure new auth flows call it.

If you later add multi-tenant constructs, reintroduce tenant context and document the role model accordingly; the current app is single-tenant.
