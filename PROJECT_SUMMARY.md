# Single-Tenant SaaS Template - Project Summary
Last updated: 2026-01-14

## Overview
This repo is a single-tenant SaaS template extracted from a multi-tenant hosting platform. It provides:

- Backend: Ring + http-kit with PostgreSQL (next.jdbc + HoneySQL)
- Frontend: shadow-cljs SPA using re-frame + Uix
- Admin panel with admin-only API and UI
- Authentication with local password auth and optional OAuth flows (see config/base.edn)
- Database schema and migrations in resources/db
- Babashka tooling for dev, DB, and config workflows
- Tests: Kaocha (backend), cljs-test-runner (Node), Karma (browser)

## High-Level Architecture
```
┌──────────────────────────────────────────────────────────────────┐
│                           Web Browser                            │
│            Admin UI (shadow-cljs + re-frame + Uix)               │
└──────────────────────────────────────────────────────────────────┘
                    │ HTTP/JSON
                    ▼
┌──────────────────────────────────────────────────────────────────┐
│                 Ring + http-kit Web Server                       │
│  Routes + Middleware (auth, rate limiting, security)             │
│  Services (admin, users, audit, domain modules)                  │
│  Data access (HoneySQL + next.jdbc)                              │
└──────────────────────────────────────────────────────────────────┘
                    │ JDBC
                    ▼
┌──────────────────────────────────────────────────────────────────┐
│                     PostgreSQL Database                          │
│  models.edn + migrations + policies + triggers + views           │
└──────────────────────────────────────────────────────────────────┘
```

## Project Layout
```
src/app/
  admin/
    backend/
    frontend/
  template/
    backend/
    frontend/
    di/
    shared/
  domain/
    ... feature modules
  shared/          # cross-platform utilities
  frontend/        # global frontend assets
resources/db/
  models.edn
  migrations/
  domain/
  shared/
  template/
  functions/
  policies/
  triggers/
  views/
config/
  base.edn
  .secrets.edn (local)
scripts/bb/        # babashka tasks
scripts/           # shell helpers

# Root configs
Deps and tooling:
  deps.edn
  shadow-cljs.edn
  bb.edn
  karma.conf.cjs
```

## Admin Module Notes
- Backend setup: `src/app/admin/backend/setup.clj`
- Service entry: `src/app/admin/backend/services/admin.clj`
- Service modules: `admins.clj`, `auth.clj`, `audit.clj`, `dashboard.clj`, `monitoring/{integrations,shared}.clj`, `users.clj` and `users/{management,security,validation,bulk,deletion}.clj`
- Frontend entry: `src/app/admin/frontend/core.cljs` + `src/app/admin/frontend/routes.cljs`
- Frontend areas: adapters, auth, components, config, events, handlers, pages, renderers, security, services, settings, shared, specs, subs, system, utils

## Domain Module Notes
- Registries: `src/app/domain/backend/registry.clj`, `src/app/domain/frontend/registry.cljs`
- Domain pages entry: `src/app/domain/frontend/pages.cljs`
- Current domain: `expenses`
  - Backend: `src/app/domain/backend/expenses/{handlers,integrations,routes,services,workers}`
  - Frontend: `src/app/domain/frontend/expenses/{adapters,admin,authz,components,config,events,pages,routes,subs,ui}` plus `core.cljs` and `init.cljs`

## Runtime and Build
- Project type: deps.edn + Babashka
- Clojure: 1.12.0
- Java: 21.0.8
- Source paths: scripts/bb, src, test

## Key Dependencies (snapshot)
### Backend
- ring 1.15.3 (ring-defaults 0.7.0, ring-json 0.5.1, ring-anti-forgery 1.4.0, ring-session-timeout 0.3.0)
- http-kit 2.8.1
- reitit 0.9.2 (+ reitit-ring 0.9.2)
- muuntaja 0.6.11
- next.jdbc 1.3.1070
- honeysql 2.7.1364
- hikari-cp 3.3.0
- postgresql 42.7.7
- buddy-core 1.12.0-430 + buddy-hashers 2.0.167
- timbre 6.8.0
- cheshire 6.1.0
- clj-http 3.13.1
- postal 2.0.5
- java-time 1.4.3
- ring-oauth2 0.3.0
- automigrate 0.3.3
- aero 1.1.6

### Frontend
- shadow-cljs 3.3.4
- re-frame 1.4.3
- uix.core/uix.dom 1.4.8
- cljs-ajax 0.8.4
- day8.re-frame/http-fx 0.2.4
- tailwindcss 4.0.0
- daisyui 5.0.4

### Tooling and testing
- clj-kondo 2025.10.23
- cljfmt 0.9.2
- kaocha 1.91.1392 (alias :test)
- cljs-test-runner 3.8.1 (alias :cljs-test)
- playwright 1.52.0
- etaoin 1.1.43
- babashka 1.12.213 (alias :dev)

## Common Commands
```bash
# Dev
bb run-app

# Tests
bb be-test
npm run test:cljs
npm run test:cljs:karma

# Frontend config validation
bb validate-frontend-config
bb config-audit --strict
bb sync-frontend-config --apply
bb migrate-and-sync-frontend-config

# Database
bb backup-db --dev
bb restore-db --dev path/to/backup.sql

# Code quality
bb lint
bb cljfmt-check
bb cljfmt-fix
```

## Migrations (REPL)
```clojure
(require '[app.template.backend.migrations.simple-repl :as mig] :reload)
(mig/migrate!)
```

## Development Notes
- Dev server auto-reloads; no manual restart needed for FE or BE changes.
- Admin UI dev URL: http://localhost:8085
- Admin settings pages: /admin/admin-settings, /admin/user-settings
- DEV_SUPPRESS_STDERR=false (or 0/no) keeps stderr visible in dev.
- Dev watchers ignore runtime-edited config EDNs under:
  - src/app/admin/frontend/config/*.edn
  - src/app/domain/frontend/**/config/*.edn

## Ports and Profiles
- App port (dev): 8085 (config/base.edn)
- Postgres dev/test: 55432 / 55433 (config/base.edn)
- shadow-cljs devtools HTTP: 9650; nREPL: 8777 (shadow-cljs.edn)
