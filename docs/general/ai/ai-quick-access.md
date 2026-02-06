<!-- ai: {:tags [:meta :ai :single-tenant] :kind :reference} -->

# AI Quick Access to Project Docs (Single-Tenant)

Fast pointers for AI agents (and humans) to the current single-tenant docs. Use these before broad searches.

## 🚨 AI Agent Behavior

- Don't try to revert the changes made by other agents or user during your session unless they are making the problem to you working on your task. If so ask user what to do.

## Quick recipes

- By namespace: `rg -n "<!-- ai: .*app\.template\.backend\.routes" docs`
- By task keyword: `rg -n "\brun-app\b|\bbe-test\b|\bfe-test\b|\blint\b|\bcljfmt\b" docs/general/operations`
- Migrations/workflow: `rg -n "migrate|models.edn|mig/" docs/general/migrations`
- Frontend-config alignment: `bb validate-frontend-config` then `bb sync-frontend-config` (dry-run) / `--apply`
- Doc-vs-code alignment audits: use **`doc-alignment-audit`** (Morph discovery → `bb audit-bundle` → Lattice evidence extraction)
- Beginning of session (multi-file): use **`session-context-bundle`** to generate a prompt-scoped bundle first, then query it with Lattice.
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

## 📊 Re-frame trace buffer (CLJS REPL)

The `reframe-events-analysis` skill uses `app.template.frontend.dev.repl-tracing` (REPL query helpers) on top of the dev trace buffer.

- It’s included in the `:app` dev build via `shadow-cljs.edn` under `:builds :app :dev :modules :app :preloads` (so your CLJS REPL session—e.g. `clj-nrepl-eval` connected to the shadow nREPL—can `require` it).
- If `(require '[app.template.frontend.dev.repl-tracing :as repl-trace])` fails, verify that preload entry and reload the page.

## Canonical entry points

- `docs/general/index.md` – doc IA
- `docs/general/architecture/overview.md` – system overview
- `docs/template/backend/http-api.md` – shared HTTP surfaces
- `docs/admin/backend/http-api.md` – admin endpoints
- `docs/admin/backend/services.md` – backend services map
- `docs/admin/frontend/app-shell.md` – admin UI shell/routing
- `docs/shared/frontend/master-detail-form.md` – reusable wrapper for edit forms needing detail fetch
- `docs/general/migrations/migration-overview.md` – models/migrations flow
- `docs/general/operations/dev-environment.md` – dev flow/tasks
- `docs/general/reference/api-reference.md` – stable admin API reference

## Single-tenant quick links

- Template scope: `docs/template/backend/single-tenant-template.md`
- Admin shell: `docs/admin/frontend/admin-panel-single-tenant.md`
- HTTP/API: `docs/template/backend/http-api.md`, `docs/admin/backend/http-api.md`, `docs/shared/frontend/http-standards.md`
- DB/migrations: `resources/db/models.edn`, `docs/general/migrations/*`
- Frontend config alignment: `bb validate-frontend-config`, `bb sync-frontend-config [--apply]`
- Monitoring: audit/login events in `docs/admin/backend/http-api.md` and `docs/general/reference/api-reference.md`
- Home Expenses domain: guide `docs/domain/expenses/index.md`; endpoints in `docs/domain/expenses/http-api.md` (`/admin/api/expenses/**`, `/api/v1/expenses/**`); plans `app-specs/home-expenses-tracker-plan.md`, `PLAN-mistral-ocr-pos-receipts.md`; worker `bb receipt-ocr-worker` (set `MISTRAL_API_KEY`)
- **Component IDs (browser testing)**: `INTERACTIVE-COMPONENTS-ID-AUDIT.md`, `docs/shared/frontend/component-library.md#component-id-requirements`

## AI Skills & Debugging Tools

### Available Skills (2025-12-24 consolidated)

All skills are available in `.claude/skills/`, `.codex/skills/`, and `.github/skills/`:

| Skill | Purpose | Trigger Keywords |
| ------- | ------- | ------------------ |
| **app-db-inspect** | Inspect re-frame app-db state safely | `app-db`, `re-frame state`, `frontend state` |
| **debugging** | Short, evidence-based debugging playbook | `debug`, `debugging`, `error`, `failure` |
| **doc-alignment-audit** | Hybrid doc-vs-code alignment audits (Morph → bundle → Lattice) | `alignment audit`, `docs vs code`, `doc mismatch`, `routing docs` |
| **session-context-bundle** | Session bootstrap bundle (Morph → `bb audit-bundle` → Lattice) | `start session`, `bootstrap context`, `prepare bundle`, `multi-file task` |
| **reframe-events-analysis** | Analyze re-frame event history and performance | `events`, `event history`, `tracing`, `re-frame` |
| **system-logs** | Monitor backend + shadow-cljs logs from dev output | `logs`, `build output`, `compilation errors` |
| **clojure-eval** | REPL exploration + evaluation workflow (nREPL, CLJS build selection) | `repl`, `nrepl`, `clj-nrepl-eval`, `eval` |
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
