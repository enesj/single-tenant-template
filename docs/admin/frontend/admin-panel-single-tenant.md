<!-- ai: {:tags [:frontend :admin :single-tenant] :kind :guide} -->

# Admin Panel (Single-Tenant)

Single-tenant admin console served at `http://localhost:8085/admin`. No tenant switching or hosting domains; focus is user management plus monitoring (audit logs, login events).

## Entrypoints and Routing

- **Entrypoint**: `app.template.frontend.core/init` (Shadow build `:admin`) → starts the shared SPA router + mounts the UI.
  - Admin module init is triggered by admin routes via `app.admin.frontend.core/init-admin!` (loads admin events/subs, applies theme, fetches UI config).
- **Routes** (`app.admin.frontend.routes`):
  - `/admin/login` (public) → login page
  - `/admin` or `/admin/dashboard` → dashboard (guarded)
  - `/admin/users` → user management + per-user activity modal (guarded)
  - `/admin/backlog` → canonical backlog page (guarded; priority-sorted list + CRUD)
  - `/admin/audit` → global audit logs (guarded)
  - `/admin/login-events` → global login events (guarded)
  - `/admin/admin-settings` → admin UI configuration (view options, form fields, table columns)
  - `/admin/user-settings` → domain-owned user UI config (user-facing defaults/locks; currently Expenses)
  - Domain pages (Expenses):
    - `/admin/articles`
    - `/admin/article-aliases`
    - `/admin/suppliers`
    - `/admin/supplier-aliases`
    - `/admin/manufacturers` (new 2026-01-29)
    - `/admin/price-observations`
    - `/admin/unmapped-aliases`
- **Guard**: `guarded-start` dispatches controller events only after admin auth is confirmed. Unauthed users are redirected to `/admin/login`.

## Data Flow (Users + Activity)

1) Route controller dispatches load events (e.g., `:admin/load-users`).  
2) Admin adapters sync entities into the template list store.  
3) `generic-admin-entity-page` renders list/form using the synced entity-spec.  
4) Per-user activity modal triggers audit + login history fetch for that user; results populate modal tables and stats.  
5) Saves/updates dispatch refresh events to keep the list and activity modal in sync.

## Domain Pages (Expenses)

All Expenses domain admin pages use the shared `list-view` component, entity specs, and the admin API under `/admin/api/expenses/*`. Typical features include dynamic columns, filters, pagination, selection, and inline/export actions.

- Articles, Article Aliases, Suppliers, Supplier Aliases, Manufacturers (new 2026-01-29), Price Observations, Unmapped Aliases.
- `/admin/articles` includes a row action **Show related records** that opens a 3-step modal wizard (type → record → details) with Back navigation between steps; supported types are Expenses, Receipts, Providers, Stores, Manufacturers, and Subcategories.
- Default edit flows may use modal forms; see Component Library for `:form-display :modal` and auto-close on success.

## Monitoring Pages

- **Audit Logs**: `/admin/audit` lists all audit events; uses template list components with server pagination and export.  
- **Login Events**: `/admin/login-events` lists admin/user logins; normalized rows include principal name/email, IP, user-agent, success/failure.  
- **Per-user modal**: mirrors the same data filtered by user.

## Settings Page

- Pages:
  - `/admin/admin-settings` (`app.admin.frontend.pages.unified-settings/admin-settings-page`)
  - `/admin/user-settings` (`app.admin.frontend.pages.unified-settings/user-settings-page`)
- Data sources:
  - Admin scope:
    - system config: `src/app/admin/frontend/config/{view-options,form-fields,table-columns,entities}.edn`
    - optional domain-admin overlay: `src/app/domain/**/admin/config/*.edn`
  - User UI config: `src/app/domain/**/config/*.edn` (e.g., `src/app/domain/frontend/expenses/config/*`)
- Use it to set defaults/locks per-entity; locked display toggles are hidden in list-view controls while remaining effective.

## Session Isolation

Admin and user sessions are isolated:
- User logout only clears the user `:auth-session` and leaves any `:admin-token` intact (so admin work continues).
- Admin logout removes only `:admin-token` and preserves any user `:auth-session`.
- Impersonation creates a user `:auth-session` while retaining the admin token.

## Extension Points

- **New admin pages**: add a page under `src/app/admin/frontend/pages`, wire a route in `app.admin.frontend.routes`, and add events/subs as needed.  
- **New domains**: add a domain manifest under `src/app/domain/**` and enable it via `app.domain.frontend.registry` (frontend) / `app.domain.backend.registry` (backend) without adding concrete-domain requires in template/admin/shared.  
- **Configs/specs**: extend entity specs/columns alongside your adapters so list toggles/export match rendered fields.  
- **Auth**: keep auth logic in `app.admin.frontend.events.auth`; avoid storing tokens in app-db beyond what UI needs.  
- **HTTP**: use `app.admin.frontend.utils.http` helpers for all admin requests.

## Relevant Namespaces

| Concern | Namespaces/Files |
|---------|------------------|
| Bootstrap | `app.admin.frontend.core`, `app.template.frontend.events.core` |
| Routing | `app.admin.frontend.routes` |
| Auth | `app.admin.frontend.events.auth`, `app.admin.frontend.subs.auth` |
| Users | `app.admin.frontend.events.users.*`, `app.admin.frontend.adapters.users` |
| Audit | `app.admin.frontend.events.audit`, `app.admin.frontend.pages.audit` |
| Login Events | `app.admin.frontend.events.login-events`, `app.admin.frontend.pages.login-events` |
| Shared UI | `app.template.frontend.components.*` (lists, modals, cards) |

## Notes

- Public `:app` build is optional; it does not handle tenants or onboarding.  
- Removed multi-tenant docs (billing/hosting/integrations); add new domain docs under `docs/domain/` as you extend the app.
