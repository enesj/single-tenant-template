<!-- ai: {:tags [:frontend :admin :overview] :kind :overview} -->

# Admin Frontend Docs

The admin frontend powers users, audit logs, login events, and settings. It is the primary UI surface for the single-tenant template.

## Docs in This Folder

- `admin.md` — admin feature guide (users, audit, login events)
- `admin-panel-single-tenant.md` — admin routing/bootstrapping basics
- `admin-settings.md` — settings UI configuration guide
- `app-shell.md` — admin entrypoint, state shape, events, routing
- `list-view-controls-configuration.md` — list controls and column config

## Related Template/Shared Docs

- [Template App Shell](../../template/frontend/app-shell.md)
- [Template Component Integration](../../template/frontend/template-component-integration.md)
- [Shared Component Library](../../shared/frontend/component-library.md)
- [Shared HTTP Standards](../../shared/frontend/http-standards.md)

## Quick Architecture

- **Entry**: `app.template.frontend.core/init` (Shadow build `:admin`); admin module initialization via `app.admin.frontend.core/init-admin!`.
- **Routes**: admin routes in `app.admin.frontend.routes` (served under `/admin/*`).
- **Primary Routes**: `/admin/login`, `/admin`, `/admin/users`, `/admin/audit`, `/admin/login-events`.
- **HTTP**: use `app.admin.frontend.utils.http` (auth header, JSON formats, timeouts).
- **Components**: reuse `app.template.frontend.components.*` (lists, forms, modals, cards).
- **State**: admin auth + entities (users, audit, login events) + UI controls (theme, list settings).

## Conventions

- DaisyUI classes with `ds-` prefix; Tailwind utilities for layout.
- Provide `:entity-spec` matching rendered columns (especially for audit/login lists).
- Keep tokens out of app-db; helpers inject headers.
- Server pagination/filtering preferred; avoid large client-side lists.
- **All interactive components MUST have `:id` attributes** for browser testing via chrome-devtools (see shared component library).

## Links

- Admin console served at `http://localhost:8085/admin`.
- Backend endpoints under `/admin/api/*`.

*Last Updated: 2025-11-26*
