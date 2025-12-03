<!-- ai: {:tags [:frontend :overview] :kind :overview} -->

# Frontend Documentation (Single-Tenant)

Single-tenant frontend focused on the admin console (users, audit logs, login events, dashboard). No tenant switching, hosting, or billing domains.

## Docs in This Folder

- `admin.md` — admin feature guide (users, audit, login events)
- `admin-panel-single-tenant.md` — admin routing/bootstrapping basics
- `app-shell.md` — build targets, entrypoints, state shape, routing
- `component-library.md` — reusable components (UIx + DaisyUI)
- `crud-event-flow.md` — **CRUD event flow architecture and debugging guide**
- `home-page-authentication-flow.md` — admin auth flow/guards
- `http-standards.md` — HTTP helpers and request patterns
- `integration-domain.md` — marked unused in single-tenant
- `list-view-controls-configuration.md` — list controls and column config
- `template-component-integration.md` — using template components
- `template-infrastructure.md` — shared infrastructure notes

## Quick Architecture

- **Builds**: `:admin` (primary), `:app` (optional landing), tests via `:test`/`:test-node`.
- **Entry**: `app.admin.frontend.core/init`; routes in `app.admin.frontend.routes`.
- **Primary Routes**: `/admin/login`, `/admin`, `/admin/users`, `/admin/audit`, `/admin/login-events`.
- **HTTP**: use `app.admin.frontend.utils.http` (auth header, JSON formats, timeouts).
- **Components**: reuse `app.template.frontend.components.*` (lists, forms, modals, cards).
- **State**: admin auth + entities (users, audit, login events) + UI controls (theme, list settings).

## Architecture Philosophy

This frontend follows the **DRY (Don't Repeat Yourself) principle** through a three-layer architecture:

```
Template Layer → Adapter Layer → Admin Pages
```

- **Template Layer**: Reusable components (`list-view`, `dynamic-form`, buttons, modals) used across all features
- **Adapter Layer**: Normalizes backend data and syncs into standardized state shape
- **Admin Pages**: Configuration-driven pages using template components + entity specs

**Why This Matters:**

Instead of creating separate list implementations for users, audit logs, and login events, we configure a single `list-view` component with different entity specs. This means:

- ✅ Bug fixes apply to all lists at once
- ✅ New features (filters, exports) work everywhere automatically  
- ✅ Consistent UX across the admin console
- ✅ Faster development of new entity pages

See [template-infrastructure.md](file:///Users/enes/Projects/single-tenant-template/docs/frontend/template-infrastructure.md) for detailed architecture explanation.

## Conventions

- DaisyUI classes with `ds-` prefix; Tailwind utilities for layout.
- Provide `:entity-spec` matching rendered columns (especially for audit/login lists).
- Keep tokens out of app-db; helpers inject headers.
- Server pagination/filtering preferred; avoid large client-side lists.

## Links

- Admin console served at `http://localhost:8085/admin`.
- Backend endpoints under `/admin/api/*`.

*Last Updated: 2025-11-26*
