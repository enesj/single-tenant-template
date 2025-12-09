<!-- ai: {:tags [:meta :ai :single-tenant] :kind :reference} -->

# AI Quick Access to Project Docs (Single-Tenant)

Fast pointers for AI agents (and humans) to the current single-tenant docs. Use these before broad searches.

## Quick recipes
- By namespace: `rg -n "<!-- ai: .*app\.backend\.routes" docs`
- By task keyword: `rg -n "\brun-app\b|\bbe-test\b|\bfe-test\b|\blint\b|\bcljfmt\b" docs/operations`
- Migrations/workflow: `rg -n "migrate|models.edn|mig/" docs/migrations`

## 🚨 Critical Testing Workflow
ALWAYS save test output before analysis - never re-run tests:
```bash
bb be-test 2>&1 | tee /tmp/be-test.txt
npm run test:cljs 2>&1 | tee /tmp/fe-test.txt
# Then grep saved files repeatedly
```

## Canonical entry points
- `docs/index.md` – doc IA
- `docs/architecture/overview.md` – system overview
- `docs/backend/http-api.md` – admin API surface
- `docs/backend/services.md` – backend services map
- `docs/frontend/app-shell.md` – admin UI shell/routing
- `docs/migrations/migration-overview.md` – models/migrations flow
- `docs/operations/dev-environment.md` – dev flow/tasks
- `docs/reference/api-reference.md` – stable admin API reference

## Single-tenant quick links
- Template scope: `docs/backend/single-tenant-template.md`
- Admin shell: `docs/frontend/admin-panel-single-tenant.md`
- HTTP/API: `docs/backend/http-api.md`, `docs/frontend/http-standards.md`
- DB/migrations: `resources/db/models.edn`, `docs/migrations/*`
- Monitoring: audit/login events in `docs/backend/http-api.md` and `docs/reference/api-reference.md`
- Home Expenses domain: endpoints in `docs/backend/http-api.md` (`/admin/api/expenses/**`), implementation plan `app-specs/home-expenses-tracker-plan.md`

## Metadata for RAG
All docs should start with:
```markdown
<!-- ai: {:namespaces [app.backend.routes app.shared.http]
         :tags [:backend :http]
         :kind :reference} -->
```
- `:namespaces` – related code namespaces (useful for filtered searches)
- `:tags` – domain tags (e.g., `:backend`, `:frontend`, `:migrations`, `:shared`, `:operations`)
- `:kind` – `:guide`, `:reference`, `:runbook`, `:overview`

## Best practices
- Keep paths stable and metadata updated when scope changes.
- Prefer file-level references over deep anchors.
- Add tags for every relevant area (e.g., shared pagination: `:shared :frontend :backend`).
