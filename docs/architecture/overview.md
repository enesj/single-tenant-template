<!-- ai: {:tags [:architecture :single-tenant] :kind :guide} -->

# Architecture Overview (Single-Tenant)

This repo is a **single-tenant admin app**. Multi-tenant/RLS material from the hosting product is no longer applicable; use this doc as the current high-level map.

## System Context
- **Backend**: Ring + Reitit on Clojure. Admin-only routes under `/admin/api` (port 8085), secured by admin auth + security middleware.
- **Frontend**: Shadow CLJS + Re-frame/Uix admin SPA (`app.admin.frontend.core`), served from the backend.
- **Database**: PostgreSQL, single schema from `resources/db/models.edn` (admins, users, audit_events, login_events, supporting tables). No RLS/tenant switching.
- **Shared libs**: Template/shared components and validation/HTTP helpers reused on both sides.

## High-Level Shape
```
Admin SPA (8085) → /admin/api/** (Ring/Reitit)
   └─ Security middleware → Admin auth → Route handler → Services → PostgreSQL
```

## Core Pieces
- **Web server**: `app.template.backend.webserver` started by `app.template.backend.core/-main` (config from `config/base.edn`).
- **Routing**: `app.template.backend.routes.admin-api` composes auth, dashboard, users/user-management, audit, login-events, and domain mounts (e.g. expenses). Admin frontend routes (Re-frame) live in `app.template.frontend.routes` with admin pages in `src/app/admin/frontend/pages`.
- **Middleware**: `app.template.backend.middleware.security` (HTTPS/headers/[optional rate limit]), `app.template.backend.middleware.admin/wrap-admin-authentication`, JSON parsing/helpers in `app.template.backend.routes.admin.utils`.
- **Services**: `app.admin.backend.services.admin.*` (users, audit, facade, monitoring helpers), `app.template.backend.services.monitoring.login-events` (login history).
- **Data**: Source EDN in `resources/db/{template,shared,domain}/**` merged into `resources/db/models.edn`; migrations generated/applied via `app.template.backend.migrations.simple-repl`.

## Development Basics
- Run stack: `bb run-app` → http://localhost:8085/admin
- Tests: `bb be-test`, `bb fe-test`
- Format/lint: `bb cljfmt-check`, `bb lint`

If you need legacy multi-tenant examples, they remain in docs tagged `:reference-only`, but they do not describe this app.
