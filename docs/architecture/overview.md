<!-- ai: {:tags [:architecture :single-tenant] :kind :guide} -->

# Architecture Overview (Single-Tenant)

This repo is a **single-tenant admin app**. Multi-tenant/RLS material from the hosting product is no longer applicable; use this doc as the current high-level map.

## System Context
- **Backend**: Ring + Reitit on Clojure. Admin-only routes under `/admin/api` (port 8085), secured by admin auth + security middleware.
- **Frontend**: Shadow CLJS + Re-frame/UIx. Admin SPA (`app.admin.frontend.core`) plus an optional public/app shell (`app.template.frontend.core`), both served from the backend.
- **Database**: PostgreSQL, single schema from `resources/db/models.edn` (admins, users, audit_events, login_events, supporting tables). No RLS/tenant switching.
- **Domains**: Concrete features live under `src/app/domain/**` and are enabled via registries (template/admin remain domain-agnostic).
- **Shared libs**: Template/shared components and validation/HTTP helpers reused on both sides.

## High-Level Shape
```
Admin SPA (8085) → /admin/api/** (Ring/Reitit)
   └─ Security middleware → Admin auth → Route handler → Services → PostgreSQL
```

## Core Pieces
- **Web server**: `app.template.backend.webserver` started by `app.template.backend.core/-main` (config from `config/base.edn`).
- **Routing**:
   - Backend: `app.template.backend.routes.admin-api` composes core admin routes and mounts enabled domain APIs from `app.domain.backend.registry` under `/admin/api/<domain-id>`.
   - Frontend: routes are composed in `app.template.frontend.routes.data` as: template shell routes + domain user routes from `app.domain.frontend.registry/all-user-routes` + admin routes from `app.admin.frontend.routes`.
   - Domain page components are aggregated in `app.domain.frontend.pages` to avoid circular dependencies.
- **Middleware**: `app.template.backend.middleware.security` (HTTPS/headers/[optional rate limit]), `app.template.backend.middleware.admin/wrap-admin-authentication`, JSON parsing/helpers in `app.template.backend.routes.admin.utils`.
- **Services**: `app.admin.backend.services.admin.*` (users, audit, auth, admins, dashboard, monitoring integrations), `app.template.backend.services.monitoring.login-events` (login history).
- **Data**: Source EDN in `resources/db/{template,shared,domain}/**` merged into `resources/db/models.edn`; migrations generated/applied via `app.template.backend.migrations.simple-repl`.
  - Frontend config alignment (migration-adjacent): `(mig/migrate! :dev {:sync-frontend-config? true})` or `bb migrate-and-sync-frontend-config`.

## Development Basics
- Run stack: `bb run-app` → http://localhost:8085/admin
- Tests: `bb be-test`, `bb fe-test`
- Format/lint: `bb cljfmt-check`, `bb lint`

If you need legacy multi-tenant examples, they remain in docs tagged `:reference-only`, but they do not describe this app.
