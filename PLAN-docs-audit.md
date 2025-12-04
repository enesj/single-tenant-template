# Plan: Docs Coverage Audit (last 7 days — as of 2025-12-04)

Goal: For every commit from the past 7 days, verify documentation coverage in `docs/` and add/update docs (with metadata + IA links) where missing. Process commits oldest → newest.

## Workflow (phased)
1) **Inventory commits** – gather commit list + changed files (`git log --since="7 days ago"`, `git diff --name-only <range>`), confirm ordering.  
2) **Per-commit review** – inspect diff, identify impacted areas (backend/frontend/ops/migrations/tests), note expected doc surfaces.  
3) **Docs update** – edit/create docs with correct `<!-- ai: {...} -->` metadata; update `docs/index.md` + relevant section links; align with backend/frontend/operations/migrations guides.  
4) **Validate** – run targeted tests if code touched (`bb be-test`, `npm run test:cljs`, etc.) when risk warrants; record results.  
5) **Summarize** – produce final report: commits checked, docs touched/created, outstanding gaps/todos, tests run/results.

## Doc surfaces to align
- IA + entry: `docs/index.md`, `docs/ai-quick-access.md`
- Backend: `docs/backend/*.md` (services/http/security/di)
- Frontend: `docs/frontend/*.md` (app shell, admin panel, feature guides, template integration)
- Operations: `docs/operations/*` (dev env, commands/ports/workflows)
- Migrations/DB: `docs/migrations/migration-overview.md`, source models under `resources/db/{template,shared}/`

## Commit queue (oldest → newest; status = TODO until reviewed)
| Status | Commit | Date | Subject |
| --- | --- | --- | --- |
| DONE | 6a2b2f6 | 2025-12-01 | Initial commit: Complete multi-tenant SaaS template (baseline docs present) |
| DONE | df22fa2 | 2025-12-01 | chore: update MCP configuration and cleanup documentation (added ops README for config/base.edn) |
| DONE | 00e44d6 | 2025-12-02 | refactor: enhance Claude Code skills and MCP integration (no new doc gaps) |
| DONE | 704b02c | 2025-12-02 | refactor: simplify MCP configuration and UI toggle logic (list-view doc updated for hidden hardcoded toggles) |
| DONE | 54d9df7 | 2025-12-02 | enhance admin settings and frontend components (added /admin/settings + backend settings routes to docs) |
| DONE | ea4da9d | 2025-12-02 | refactor: implement reactive display settings architecture with cells module (doc reflects display-settings hook/cells) |
| DONE | f10a013 | 2025-12-02 | refactor: enhance CRUD operations with improved ID extraction and recent update tracking (no doc changes needed) |
| DONE | 349c568 | 2025-12-02 | update copilot instructions (no doc changes needed) |
| DONE | e889b44 | 2025-12-02 | refactor: consolidate CRUD architecture with bridge system and success handlers (covered in CRUD event flow doc) |
| DONE | e50955f | 2025-12-02 | fix: resolve CRUD architecture issues with adapter handlers and form state (no doc impact) |
| DONE | 75e7519 | 2025-12-03 | Update project documentation and architecture (already in repo) |
| DONE | 569d3ea | 2025-12-03 | feat: implement comprehensive admin management system with password functionality (docs shipped in commit) |
| DONE | ef2154b | 2025-12-03 | feat: optimize admin entity page with memoization and add documentation files (no extra doc action) |
| DONE | d13bcd9 | 2025-12-03 | fix: resolve display settings merge order and update audit pagination investigation (list-view doc updated for hidden hardcoded controls) |
| DONE | 10d5a1c | 2025-12-03 | feat: enable interactive features for admin entities (no doc impact) |
| DONE | bc914bb | 2025-12-03 | feat: enhance admin system with login events and audit improvements (HTTP API + admin-panel docs updated for delete/bulk-delete) |
| DONE | 3d4c9b5 | 2025-12-04 | chore: remove obsolete plan files (no docs) |
| DONE | d1c368a | 2025-12-04 | refactor: reorganize testing infrastructure and remove obsolete files (testing docs already fresh) |
| DONE | ab0ea95 | 2025-12-04 | feat: add comprehensive admin route tests and update testing plan (no doc impact) |
| DONE | 1285463 | 2025-12-04 | feat: add comprehensive database integration tests and enhance fixtures (no doc impact) |
| DONE | 4198d10 | 2025-12-04 | feat: complete backend testing infrastructure with comprehensive admin test coverage (testing docs already fresh) |
| DONE | fbaee62 | 2025-12-04 | chore: update configuration after testing infrastructure improvements (no doc change needed) |

## Tracking notes
- Work one commit at a time; update Status to IN-PROGRESS/DONE with doc file refs + test notes.
- Add follow-up todos under each commit if coverage deferred, and roll unresolved items into final summary.
- Keep generated files untouched (`resources/db/models.edn`); edit source models if schema changes surface.
