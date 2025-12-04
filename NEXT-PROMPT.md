# NEXT SESSION PROMPT — 2025-12-04
PROMPT: "bootstrap backend tests (Kaocha)"

## Context Snapshot (single-tenant template)
- Clojure backend with Reitit + Ring; entry `app.backend.core` builds service container via `app.template.di.config` and webserver in `app.backend.webserver` (serves admin+app on port 8085 dev / 8086 test).
- Routes assembled in `src/app/backend/routes.clj`; admin/API routes come from `app.backend.routes.admin-api` and `app.backend.routes.api`; frontend uses shared `resources/public/index.html`.
- DB models live in `resources/db/models.edn`; config via Aero `config/base.edn` (profiles :dev/:test, Postgres on 55432/55433, secrets expected in `.secrets.edn`).
- Tooling: Kaocha test runner via alias `:test` (deps.edn) and task `bb be-test`; test profile sets `-Daero.profile=test`.
- Frontend/admin stack is Re-frame/UIx; admin UI served at http://localhost:8085 (keep port in mind for curls).
- No RLS/tenant context; single-tenant defaults with simplified admin auth.

## Task Focus
Bootstrap a maintainable backend testing setup: use Kaocha + Ring mock requests; grow coverage starting from the new smoke tests and plan for DB-aware tests using the service container while keeping dependencies isolated.

## Code Map (relevant files)
- `src/app/backend/routes.clj` – defines all routes and middleware; uses service container, security wrapper, generates static/admin/API routes.
- `src/app/backend/webserver.clj` – wraps handlers with `:service-container`, runs http-kit server.
- `deps.edn` (:test alias) – Kaocha, ring-mock, test.check; sets `-Daero.profile=test`.
- `tests.edn` – Kaocha suite `:app.backend` with before/after hooks placeholders.
- `test/app/backend/routes_smoke_test.clj` – new Kaocha/`clojure.test` smoke tests stubbing admin/API deps: validates `/`, `/api/v1/metrics`, and `/api/v1/auth/login` using `wrap-service-container` + Ring mock.
- `docs/backend/services.md` – notes on testing via routes and `bb be-test`.
- `docs/operations/dev-environment.md` – dev loop commands (`bb run-app`, nREPL 7888, port 8085).

## Commands to Run
- `bb be-test` (preferred Kaocha entry).
- Or `clj -M:test -m kaocha.runner` to iterate.

## Gotchas / Assumptions
- Service container required for routes; tests stub `:crud-routes` and auth handlers—expand with real DI when adding integration tests.
- Metrics route uses login-monitoring/admin-dashboard DB calls; current tests `with-redefs` them—replace with fixtures hitting test DB when ready.
- Config expects `.secrets.edn` for DB creds; keep using stubs/mocks or spin up Postgres on 55433 for real runs.
- `docs/operations/README.md` is referenced in doc IA but not present; rely on `docs/operations/dev-environment.md` instead.
- Ports: 8085 dev, 8086 test; HTTPS redirect can be disabled via `DISABLE_HTTPS_REDIRECT=true`.

## Checklist for Next Agent
1) Decide short-term coverage targets (e.g., admin auth/login, audit listings) and whether to keep stubbing or stand up the test DB.
2) If using real DB: run migrations (`clj -X:migrations-test`) and seed minimal admin rows; plumb fixtures into Kaocha hooks (`tests.edn` before/after suite).
3) Extend smoke tests or add new namespaces under `test/app/backend/*_test.clj`; reuse `wrap-service-container` to pass DI map; prefer exercising route handlers.
4) Remove/replace legacy `test/README-test-repl.md` + `test/test/repl.clj` references to `app.domain.*` when they no longer apply.
5) Run `bb be-test`; capture failures/output in PR; keep `-Daero.profile=test` context.
6) Update docs if middleware/order or test setup changes.
