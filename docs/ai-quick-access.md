<!-- ai: {:tags [:meta :ai :single-tenant] :kind :reference} -->

# AI Quick Access to Project Docs (Single-Tenant)

Fast pointers for AI agents (and humans) to the current single-tenant docs. Use these before broad searches.

## Quick recipes
- By namespace: `rg -n "<!-- ai: .*app\.template\.backend\.routes" docs`
- By task keyword: `rg -n "\brun-app\b|\bbe-test\b|\bfe-test\b|\blint\b|\bcljfmt\b" docs/operations`
- Migrations/workflow: `rg -n "migrate|models.edn|mig/" docs/migrations`
- Frontend-config alignment: `bb validate-frontend-config` then `bb sync-frontend-config` (dry-run) / `--apply`
- **AI Skills lookup**: Skills are available in `.claude/skills/`, `.codex/skills/`, and `.github/skills/` for multi-editor support

## 🚨 Critical Testing Workflow
ALWAYS save test output before analysis - never re-run tests:
```bash
bb be-test 2>&1 | tee /tmp/be-test.txt
npm run test:cljs 2>&1 | tee /tmp/fe-test.txt
# Then grep saved files repeatedly
```

## 💡 REPL Troubleshooting
If you get a `FileNotFoundException` when requiring `.cljs` files in the REPL, switch to the ClojureScript runtime:
```clojure
(shadow.cljs.devtools.api/nrepl-select :app)
```
(Use `:admin` if working on the admin panel).

## Canonical entry points
- `docs/index.md` – doc IA
- `docs/architecture/overview.md` – system overview
- `docs/backend/http-api.md` – admin API surface
- `docs/backend/services.md` – backend services map
- `docs/frontend/app-shell.md` – admin UI shell/routing
- `docs/frontend/master-detail-form.md` – reusable wrapper for edit forms needing detail fetch
- `docs/migrations/migration-overview.md` – models/migrations flow
- `docs/operations/dev-environment.md` – dev flow/tasks
- `docs/reference/api-reference.md` – stable admin API reference

## Single-tenant quick links
- Template scope: `docs/backend/single-tenant-template.md`
- Admin shell: `docs/frontend/admin-panel-single-tenant.md`
- HTTP/API: `docs/backend/http-api.md`, `docs/frontend/http-standards.md`
- DB/migrations: `resources/db/models.edn`, `docs/migrations/*`
- Frontend config alignment: `bb validate-frontend-config`, `bb sync-frontend-config [--apply]`
- Monitoring: audit/login events in `docs/backend/http-api.md` and `docs/reference/api-reference.md`
- Home Expenses domain: endpoints in `docs/backend/http-api.md` (`/admin/api/expenses/**`), implementation plan `app-specs/home-expenses-tracker-plan.md`
- **Component IDs (browser testing)**: `INTERACTIVE-COMPONENTS-ID-AUDIT.md`, `docs/frontend/component-library.md#component-id-requirements`

## AI Skills & Debugging Tools

### Available Skills (2025-12-24 consolidated)

All skills are available in `.claude/skills/`, `.codex/skills/`, and `.github/skills/`:

| Skill | Purpose | Trigger Keywords |
|-------|---------|------------------|
| **app-db-inspect** | Inspect re-frame app-db state safely | `app-db`, `re-frame state`, `frontend state` |
| **debugging** | Short, evidence-based debugging playbook | `debug`, `debugging`, `error`, `failure` |
| **reframe-events-analysis** | Analyze re-frame event history and performance | `events`, `event history`, `tracing`, `re-frame` |
| **system-logs** | Monitor backend + shadow-cljs logs from dev output | `logs`, `build output`, `compilation errors` |
| **fe-tests** | Automated frontend test failure analysis | `test failure`, `frontend test`, `test error` |
| **new-prompt** | Research repo docs and draft next-session prompt | `new prompt`, `session summary` |

### Using Skills

Skills activate automatically when relevant keywords are mentioned in your request. For example:
- "Check the app-db for user state" → activates `app-db-inspect`
- "Analyze recent events for the admin page" → activates `reframe-events-analysis`
- "The frontend tests are failing" → activates `fe-tests`

## Metadata for RAG
All docs should start with:
```markdown
<!-- ai: {:namespaces [app.template.backend.routes app.shared.http]
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
