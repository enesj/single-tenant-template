# Next Session Prompt (2025-12-09)
PROMPT: Expand admin settings UI to edit view-options add/batch controls plus form-fields & table-columns configs.

## Context Snapshot
- Single-tenant SaaS template; admin served at http://localhost:8085/admin (no tenant/RLS). Hot reload via `bb run-app`.
- Admin settings page `/admin/settings` lives in `src/app/admin/frontend/pages/settings.cljs`; loads/patches view-options via `src/app/admin/frontend/events/settings.cljs` using `/admin/api/settings` (GET/PATCH/DELETE per docs/backend/http-api.md).
- UI config loader `src/app/admin/frontend/config/loader.cljs` caches EDN from `resources/public/admin/ui-config/{view-options,form-fields,table-columns}.edn`; provides `register-preloaded-config!` and getters used by template list/form components.
- View options currently expose display toggles (`show-edit?`, `show-delete?`, `show-select?`, `show-filtering?`, `show-pagination?`, etc.) and action keys already defined in code (`show-add-button?`, `show-batch-edit?`, `show-batch-delete?`) but not surfaced in UI or EDN.
- Form definitions and table column configs are static EDN; table columns use inverted config (available, default-hidden, unfilterable, unsortable, column-config, computed-fields) transformed by loader.
- Skills available: app-db-inspect, reframe-events-analysis, system-logs (see .claude/skills). Use them instead of manual guessing.
- Missing doc noted: `docs/frontend/feature-guides/admin.md` not found (path in index is stale).

## Task Focus
Add editing controls on `/admin/settings` so admins can toggle add/batch actions in view-options.edn. Add new tab(s) to edit selected fields from form-fields.edn and table-columns.edn (choose sensible controls: e.g., required/create/edit field lists, default-hidden/always-visible columns, widths/formatters). Ensure changes persist via backend APIs and refresh config caches.

## Code Map
- src/app/admin/frontend/pages/settings.cljs: UI tabs/cards, current display/action badges, edit toggle.
- src/app/admin/frontend/events/settings.cljs: load/update/remove view-option settings via /admin/api/settings; optimistic updates and cache sync.
- src/app/admin/frontend/config/loader.cljs: fetches EDN configs, transforms table-columns, exposes getters and register-preloaded-config! used for hot cache updates.
- resources/public/admin/ui-config/view-options.edn: current list toggles per entity (no add/batch keys yet).
- resources/public/admin/ui-config/form-fields.edn: create/edit/required fields and field-config per entity.
- resources/public/admin/ui-config/table-columns.edn: available/default-hidden/unfilterable/unsortable/always-visible, column-config, computed-fields per entity.
- Backend settings routes (`app.backend.routes.admin.settings`, see docs/backend/http-api.md) currently only cover view-options; likely need new endpoints or extensions to handle form-fields/table-columns.
- Template components consuming configs: `src/app/template/frontend/components/list.cljs` (display/action toggles, table columns), `src/app/template/frontend/components/form.cljs` & related form helpers.

## Commands to Run
- Dev stack: `bb run-app` (serves admin + shadow watch on 8085).
- Frontend tests: `npm run test:cljs 2>&1 | tee /tmp/fe-test.txt` (save output once, then grep).
- Backend tests (if you touch API): `bb be-test 2>&1 | tee /tmp/be-test.txt`.

## Gotchas
- Port is 8085, not 3000. Admin auth middleware protects /admin/api; use admin token/cookie when manual testing.
- Config loader expects `/admin/ui-config/*.edn` paths; keep formats consistent (table-columns uses inverted keys → loader normalizes to default-visible/filterable/sortable).
- After PATCH/DELETE, also update loader cache (`register-preloaded-config!`) so UI reflects changes without full reload.
- Edit mode cycles true → false → nil; nil removes hardcoded setting (user-configurable). Preserve optimistic updates + rollback on failure.
- Adding form/table editors will require either expanding existing /admin/api/settings endpoints or creating new ones; ensure backend writes back to respective EDN files and stays single-tenant.
- UI should stay within admin design system (DaisyUI/UIX) and tabs already in settings page; add new tab(s) instead of overcrowding existing ones.

## Checklist for Next Agent
1) Re-read docs/index.md and docs/frontend/admin-panel-single-tenant.md; review skills in .claude/skills (app-db-inspect, reframe-events-analysis, system-logs).
2) Confirm backend support: inspect `app.backend.routes.admin.settings` (and related services) to see how view-options are persisted; design/implement parallel endpoints for form-fields/table-columns or extend payload shape.
3) Extend frontend state/events: add load/update/remove handlers for new config types, reuse optimistic pattern, sync cache via config-loader.
4) Update `/admin/settings` UI: add controls for add/batch toggles (view-options) and new tab(s) for form-fields & table-columns (pick editable fields like required/create/edit lists, default-hidden/always-visible, widths). Keep edit mode guidance/tooltips.
5) Wire data flow: on tab mount, fetch configs; on save/delete, call backend and refresh config-loader + app-db; show loading/saving/error states.
6) Validate end-to-end: run `bb run-app`, exercise new UI, confirm persisted EDN updates, then run `npm run test:cljs` (and `bb be-test` if backend touched), reviewing saved output files.
7) Document changes briefly in code comments if behavior non-obvious; keep EDN formatting stable.
8) Prepare a comprehensive implementation plan markdown in repo root before coding (per user request) and track progress there; after plan is created, start implementation without waiting for further approval.

## Notes
- If docs are missing (feature-guides/admin), mention in PR/plan and proceed using existing pages as reference.
- Prefer MCP tools (vector search, skills, clojurescript_eval/clj-nrepl-eval) over ad-hoc shell where possible.
