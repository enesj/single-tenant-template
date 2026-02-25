---
mode: agent
description: "Remove verified dead code from the admin module — unmounted routes, unreachable services, and orphaned frontend namespaces."
---

# Admin Dead Code Cleanup

## Summary
- Objective: delete admin code that is provably unreachable or unloaded, with no behavior change.
- Scope: backend route/service cluster, one frontend spec namespace, one test file.
- Every item below was verified dead by cross-referencing all `require` chains from `admin_api.clj` and all CLJS namespace imports.

## Dead Code Inventory

### 1. Unmounted backend route — integrations cluster (HIGH confidence)

These three files form a transitive dead-code island. The route namespace was never required or mounted in `admin_api.clj`, so the entire chain is unreachable at runtime.

| File | Reason dead |
|------|-------------|
| `src/app/template/backend/routes/admin/integrations.clj` | Never required in `admin_api.clj`; no other callers in `src/` |
| `src/app/admin/backend/services/admin/monitoring/integrations.clj` | Only caller is the dead route above |
| `src/app/admin/backend/services/admin/monitoring/shared.clj` | Only caller is the dead monitoring/integrations service above |

Handlers defined but never reachable:
- `get-integration-overview-handler`
- `get-integration-performance-handler`
- `get-webhook-status-handler`
- `routes`

### 2. Dead test file for unmounted route (HIGH confidence)

| File | Reason dead |
|------|-------------|
| `test/app/backend/routes/admin/integrations_test.clj` | Tests a route that is never mounted; tests can never pass against a live system |

### 3. Orphaned frontend namespace (HIGH confidence)

| File | Reason dead |
|------|-------------|
| `src/app/admin/frontend/specs/conditional.cljs` | No namespace requires it anywhere in `src/`; contains conditional visibility engine, role-based field permissions, computed field multimethods, dynamic formatting multimethods, and one re-frame subscription — all unreachable |

## What is NOT dead (verified live)

These were investigated and confirmed active — do not touch:

- `src/app/template/backend/routes/admin/utils.clj` → `log-admin-action` and `log-admin-action-with-context` are called from at least 30 sites across admin route handlers.
- `src/app/template/backend/routes/admin/dashboard.clj` → `rate-limiting` require is used at lines 46 and 49.
- `src/app/admin/frontend/adapters/backlog.cljs` → required by `entity_registry.cljs` and the backlog route.
- `src/app/admin/frontend/specs/form_components.cljs` → required by `specs/generic.cljs`.

## Implementation Plan

1. Delete the backend integrations cluster.
   - Remove `src/app/template/backend/routes/admin/integrations.clj`
   - Remove `src/app/admin/backend/services/admin/monitoring/integrations.clj`
   - Remove `src/app/admin/backend/services/admin/monitoring/shared.clj`
   - Confirm `src/app/admin/backend/services/admin/monitoring/` directory is now empty; remove if so.

2. Delete the dead test file.
   - Remove `test/app/backend/routes/admin/integrations_test.clj`

3. Delete the orphaned frontend namespace.
   - Remove `src/app/admin/frontend/specs/conditional.cljs`

4. Compile check.
   - Backend: `bb be-test` (or `bb run-app` + REPL reload) — no compilation errors.
   - Frontend: `bb fe-test-parallel` or `npm run test:cljs` — no missing-namespace errors.

5. Verify no remaining references.
   - `grep -r "admin.routes.integrations\|monitoring.integrations\|monitoring.shared\|specs.conditional" src/ test/` → zero results.

## Test Cases and Scenarios
- Run the full backend test suite after deletion: `bb be-test 2>&1 | tee tmp/dead-code-cleanup-be.txt`
- Run the full frontend test suite after deletion: `bb fe-test-parallel 2>&1 | tee tmp/dead-code-cleanup-fe.txt`
- No new test failures are acceptable; this is a deletion-only change.

## Acceptance Criteria
- All five files deleted.
- Zero compilation errors in backend or frontend.
- Zero test regressions.
- `grep` for deleted namespace symbols returns no hits in `src/` or `test/`.

## Assumptions and Defaults
- No schema or migration changes.
- No behavior change — deleted code was never executed.
- Temporary output files go under `tmp/`.
