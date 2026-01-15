# Important Namespaces (Top 50)

Each entry includes a concise agent instruction for working in that namespace.

| # | Namespace | Path | Agent instruction |
|---:|---|---|---|
| 1 | `app.template.backend.core` | `src/app/template/backend/core.clj` | Backend entry point; adjust system startup wiring and lifecycle only. |
| 2 | `app.template.backend.webserver` | `src/app/template/backend/webserver.clj` | Ring server setup; change server options and handler wiring here. |
| 3 | `app.template.backend.routes` | `src/app/template/backend/routes.clj` | Top-level route composition; mount new route trees here. |
| 4 | `app.template.backend.routes.admin-api` | `src/app/template/backend/routes/admin_api.clj` | Compose `/admin/api` routes and middleware; add admin endpoints via focused route namespaces. |
| 5 | `app.template.backend.routes.admin.utils` | `src/app/template/backend/routes/admin/utils.clj` | Shared admin route helpers (JSON/middleware); keep utilities reusable. |
| 6 | `app.template.backend.middleware.security` | `src/app/template/backend/middleware/security.clj` | Security headers/HTTPS policies; keep changes config-driven. |
| 7 | `app.template.backend.middleware.admin` | `src/app/template/backend/middleware/admin.clj` | Admin auth middleware; update authentication checks here. |
| 8 | `app.template.backend.auth.service` | `src/app/template/backend/auth/service.clj` | Auth service implementation; maintain token/session workflows. |
| 9 | `app.template.backend.auth.protocols` | `src/app/template/backend/auth/protocols.clj` | Auth service contracts; update only when API changes. |
| 10 | `app.template.backend.security.entity-access` | `src/app/template/backend/security/entity_access.clj` | Entity-level access rules; reuse for permission gating. |
| 11 | `app.template.backend.db.adapter` | `src/app/template/backend/db/adapter.clj` | DB adapter helpers; normalize results and PG types here. |
| 12 | `app.template.backend.db.protocols` | `src/app/template/backend/db/protocols.clj` | DB protocol definitions; keep core DB API stable. |
| 13 | `app.template.backend.routes.auth` | `src/app/template/backend/routes/auth.clj` | Auth HTTP routes; keep request/response shape consistent. |
| 14 | `app.template.backend.routes.crud` | `src/app/template/backend/routes/crud.clj` | Generic CRUD route helpers; extend for shared entity APIs. |
| 15 | `app.admin.backend.services.admin` | `src/app/admin/backend/services/admin.clj` | Admin service facade; orchestrate admin service calls. |
| 16 | `app.admin.backend.services.admin.admins` | `src/app/admin/backend/services/admin/admins.clj` | Admin account management; update owner/admin flows here. |
| 17 | `app.admin.backend.services.admin.users` | `src/app/admin/backend/services/admin/users.clj` | User management service entry; centralize user CRUD. |
| 18 | `app.admin.backend.services.admin.users.bulk` | `src/app/admin/backend/services/admin/users/bulk.clj` | Bulk user ops; keep batch validation and side effects centralized. |
| 19 | `app.admin.backend.services.admin.users.deletion` | `src/app/admin/backend/services/admin/users/deletion.clj` | User deletion/cleanup; ensure audit/logging is preserved. |
| 20 | `app.admin.backend.services.admin.users.management` | `src/app/admin/backend/services/admin/users/management.clj` | User management helpers (roles/status); reuse in admin flows. |
| 21 | `app.admin.backend.services.admin.users.security` | `src/app/admin/backend/services/admin/users/security.clj` | Password/reset/lockout helpers; enforce security rules. |
| 22 | `app.admin.backend.services.admin.users.validation` | `src/app/admin/backend/services/admin/users/validation.clj` | User validation rules; keep constraints in one place. |
| 23 | `app.admin.backend.services.admin.audit` | `src/app/admin/backend/services/admin/audit.clj` | Audit log service; append/query audit events here. |
| 24 | `app.admin.backend.services.admin.auth` | `src/app/admin/backend/services/admin/auth.clj` | Admin auth orchestration; bridge auth service to routes. |
| 25 | `app.shared.adapters.database` | `src/app/shared/adapters/database.clj` | Convert PG objects/keys; call before JSON responses. |
| 26 | `app.shared.auth` | `src/app/shared/auth.cljc` | Role/permission helpers; use for consistent auth checks. |
| 27 | `app.shared.http` | `src/app/shared/http.cljc` | HTTP response helpers/status codes; keep shapes stable. |
| 28 | `app.shared.pagination` | `src/app/shared/pagination.cljc` | Pagination math and shapes; reuse for list endpoints. |
| 29 | `app.shared.patterns` | `src/app/shared/patterns.cljc` | Shared regex/patterns; extend for validation needs. |
| 30 | `app.shared.string` | `src/app/shared/string.cljc` | String casing/slug/email helpers; avoid ad hoc logic. |
| 31 | `app.shared.date` | `src/app/shared/date.cljc` | Date/time helpers; keep parsing/formatting consistent. |
| 32 | `app.shared.validation.core` | `src/app/shared/validation/core.cljc` | Validation core; compose constraints here. |
| 33 | `app.template.frontend.core` | `src/app/template/frontend/core.cljs` | Template frontend bootstrap; wire init, routes, root view. |
| 34 | `app.template.frontend.routes` | `src/app/template/frontend/routes.cljs` | Client routes; update when adding pages. |
| 35 | `app.template.frontend.routes.data` | `src/app/template/frontend/routes/data.cljs` | Route data composition (template + domain + admin); keep registry integration here. |
| 36 | `app.template.frontend.events.core` | `src/app/template/frontend/events/core.cljs` | Event registration entrypoint; ensure event namespaces are required. |
| 37 | `app.template.frontend.events.routing` | `src/app/template/frontend/events/routing.cljs` | Routing events; update for navigation side effects. |
| 38 | `app.template.frontend.subs.core` | `src/app/template/frontend/subs/core.cljs` | Subscription registration entrypoint; ensure subs are loaded. |
| 39 | `app.template.frontend.db.db` | `src/app/template/frontend/db/db.cljs` | App-db schema/initial state; update for new state keys. |
| 40 | `app.template.frontend.db.entity-specs` | `src/app/template/frontend/db/entity_specs.cljs` | Entity specs; keep field configs in sync with backend. |
| 41 | `app.admin.frontend.core` | `src/app/admin/frontend/core.cljs` | Admin SPA bootstrap; integrate admin routes and layout. |
| 42 | `app.admin.frontend.routes` | `src/app/admin/frontend/routes.cljs` | Admin routes; update when adding admin pages. |
| 43 | `app.admin.frontend.events.auth` | `src/app/admin/frontend/events/auth.cljs` | Auth events (login/logout); keep token flow consistent. |
| 44 | `app.admin.frontend.events.users` | `src/app/admin/frontend/events/users.cljs` | User management events; sync with admin services. |
| 45 | `app.admin.frontend.pages.users` | `src/app/admin/frontend/pages/users.cljs` | Users page UI; wire list/detail actions. |
| 46 | `app.admin.frontend.subs.users` | `src/app/admin/frontend/subs/users.cljs` | User-related subscriptions; keep selectors narrow. |
| 47 | `app.domain.backend.registry` | `src/app/domain/backend/registry.clj` | Domain backend registry; register domain services/routes. |
| 48 | `app.domain.frontend.registry` | `src/app/domain/frontend/registry.cljs` | Domain frontend registry; register domain routes/components. |
| 49 | `app.domain.backend.expenses.routes.core` | `src/app/domain/backend/expenses/routes/core.clj` | Expenses backend route assembly; add new expense endpoints here. |
| 50 | `app.domain.frontend.expenses.core` | `src/app/domain/frontend/expenses/core.cljs` | Expenses frontend bootstrap; wire page and feature init. |
