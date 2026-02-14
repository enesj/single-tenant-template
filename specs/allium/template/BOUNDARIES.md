# Boundary claims → evidence

This document maps architecture claims to concrete code evidence.

## Claims and evidence

| Claim | Evidence (file + symbol) | Notes |
| --- | --- | --- |
| Top-level backend composition separates static/template routes, admin API routes, admin SPA routes, and domain SPA routes. | `src/app/template/backend/routes.clj` → `app-routes`, locals: `static-routes`, `admin-api-routes`, `admin-frontend-routes`, `frontend-routes`, `all-routes` | Route precedence is explicit via `concat` ordering. |
| Admin API boundary is mounted under `/admin/api`, with public + protected sections and domain-admin route contribution. | `src/app/template/backend/routes/admin_api.clj` → `admin-api-routes`, path root `"/admin/api"`, call to `domain-registry/all-admin-api-routes` | Confirms template/admin boundary plus domain extension point. |
| User API boundary is versioned under `/api/v1` and includes domain-user route contribution. | `src/app/template/backend/routes.clj` → mount `"/api"` + `"/v1"`; `src/app/template/backend/routes/api.clj` → `create-versioned-api-routes`, `domain-registry/all-user-api-routes` | Confirms template user boundary and domain extension point. |
| Domain backend manifest is the source of enabled domain backend capabilities and declared SPA paths. | `src/app/domain/backend/registry.clj` → `expenses-manifest`, `enabled-domains`, `all-admin-api-routes`, `all-user-api-routes`, `all-spa-routes` | Registry currently carries route and SPA path declarations. |
| Domain frontend manifest is the source of enabled domain frontend user routes. | `src/app/domain/frontend/registry.cljs` → `expenses-manifest`, `all-user-routes`; `src/app/domain/frontend/expenses/routes/user.cljs` → `routes` | Frontend route set is declared independently from backend SPA path list. |
| User API currently delegates users listing to an admin handler (explicit cross-boundary coupling). | `src/app/template/backend/routes/api.clj` → `create-versioned-api-routes` local `custom-get-handler` with `requiring-resolve` admin users handler; `src/app/template/backend/routes/admin/users.clj` → `list-users-handler` | **Decision recorded:** keep for now, but treat as temporary/explicit coupling. |
| Backend SPA fallback paths and frontend routes must remain a single source of truth (invariant). | Backend side: `src/app/template/backend/routes.clj` (`frontend-routes` + `domain-registry/all-spa-routes`), `src/app/domain/backend/registry.clj` (`:spa-routes`). Frontend side: `src/app/template/frontend/routes/data.cljs` (`app-routes`, `domain-registry/all-user-routes`), `src/app/domain/frontend/expenses/routes/user.cljs` (`routes`). | Evidence shows separate declarations today; invariant requires consolidation to one authoritative catalog (documented for Phase 2 implementation). |

## Recorded decisions (Phase 1)

- Keep User API users-list delegation to admin handler for now.
- Require a single source of truth for backend SPA fallback paths and frontend routes.
