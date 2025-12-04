<!-- ai: {:tags [:frontend :admin :single-tenant] :kind :guide} -->

# Admin Panel (Single-Tenant)

Single-tenant admin console served at `http://localhost:8085/admin`. No tenant switching or hosting domains; focus is user management plus monitoring (audit logs, login events).

## Entrypoints and Routing

- **Entrypoint**: `app.admin.frontend.core/init` → loads admin events/subs, applies theme, fetches UI config (`init-admin!`), mounts current page.
- **Routes** (`app.admin.frontend.routes`):
  - `/admin/login` (public) → login page
  - `/admin` or `/admin/dashboard` → dashboard (guarded)
  - `/admin/users` → user management + per-user activity modal (guarded)
  - `/admin/audit` → global audit logs (guarded)
  - `/admin/login-events` → global login events (guarded)
  - `/admin/settings` → hardcoded list-view settings (read/write `view-options.edn`)
- **Guard**: `guarded-start` dispatches controller events only after admin auth is confirmed. Unauthed users are redirected to `/admin/login`.

## Data Flow (Users + Activity)

1) Route controller dispatches load events (e.g., `:admin/load-users`).  
2) Admin adapters sync entities into the template list store.  
3) `generic-admin-entity-page` renders list/form using the synced entity-spec.  
4) Per-user activity modal triggers audit + login history fetch for that user; results populate modal tables and stats.  
5) Saves/updates dispatch refresh events to keep the list and activity modal in sync.

## Monitoring Pages

- **Audit Logs**: `/admin/audit` lists all audit events; uses template list components with server pagination and export.  
- **Login Events**: `/admin/login-events` lists admin/user logins; normalized rows include principal name/email, IP, user-agent, success/failure.  
- **Per-user modal**: mirrors the same data filtered by user.
- **Delete/Bulk delete**: both audit logs and login events support per-row delete and bulk delete actions (admin-only).

## Settings Page

- Page: `/admin/settings` (`app.admin.frontend.pages.settings`)  
- Data source: `resources/public/admin/ui-config/view-options.edn` read/write via backend settings routes.  
- Use it to lock list controls per-entity (show/hide edit/delete/select/timestamps/pagination/filtering); hardcoded controls are hidden in the UI while remaining effective.

## Extension Points

- **New admin pages**: add a page under `src/app/admin/frontend/pages`, wire a route in `app.admin.frontend.routes`, and add events/subs as needed.  
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
- Removed multi-tenant docs (billing/hosting/integrations); add new domain docs under `docs/frontend/feature-guides/` as you extend the app.
