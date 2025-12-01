<!-- ai: {:tags [:shared :security :single-tenant] :kind :guide} -->

# Authentication Utilities

Shared role/permission helpers for the single-tenant admin app.

- **Location**: `src/app/shared/auth.cljc`
- **Roles**: `:owner`, `:admin`, `:member`, `:viewer`
- **Purpose**: Single source of truth for role hierarchy and permission checks used by backend middleware and admin UI components.

## Key helpers
- `valid-role?` – ensure role is in the supported set.
- `role-includes?` – hierarchy checks (owner > admin > member > viewer).
- `get-permissions-for-role` / `calculate-user-permissions` – derive permission sets from a role.
- `has-permission?`, `has-any-permission?`, `has-all-permissions?` – predicate helpers for UI gates and backend guards.
- `get-auth-status` – normalize session/auth state for shared consumption.

## Usage patterns
- **Backend**: Apply `has-permission?` in handlers/middleware to guard admin actions (e.g., role updates, impersonation).
- **Frontend**: Gate buttons/menus using the same permission helpers to keep UI in sync with backend rules.

## Notes
- No tenant context or RLS is involved—permissions are global to the single-tenant admin app.
- Keep new permissions/roles centralized here so both backend and frontend stay aligned.
