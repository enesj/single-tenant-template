# Next Session Prompt (2025-12-03)
PROMPT: "admin audit pagination toggle locked"

## Context Snapshot
- Single-tenant SaaS template (Clojure/ClojureScript, Postgres); admin UI served at http://localhost:8085 (shadow-cljs :admin build, auto-reloads via `bb run-app`).
- Admin pages use generic entity system; audit logs page `/admin/audit` renders `generic-admin-entity-page :audit-logs` with list components from template layer.
- List/table controls come from template settings panel (`list_view_settings.cljs`) with toggles wired to `app.template.frontend.events.list.ui-state` and display settings merged from hardcoded page config + user prefs.
- Pagination rendering in `app.template.frontend.components.list` only hides when merged `:show-pagination?` is false; otherwise shows pagination component when `total-pages > 1`.
- Audit entity config (`src/app/admin/frontend/config/entities.edn`) hardcodes `:show-pagination? true` in `:display-settings`, likely locking the toggle (per list-view controls guide: hardcoded true makes control non-interactive and feature always on).
- Audit adapter initializes UI state with pagination defaults (`initialize-audit-ui-state` sets per-page/current-page paths) and fetch config if missing.

## Task Focus
Investigate why clicking the Pagination toggle in the settings panel on `/admin/audit` does nothing and pagination stays visible; allow the user to hide pagination or make the control clearly locked if intentionally forced on.

## Code Map
- `src/app/admin/frontend/config/entities.edn` — audit-logs display settings hardcode `:show-pagination? true` (and `:per-page 20`).
- `src/app/admin/frontend/pages/audit.cljs` — mounts `generic-admin-entity-page :audit-logs`.
- `src/app/admin/frontend/adapters/audit.cljs` — `init-audit-adapter!` and `::initialize-audit-ui-state` seed list UI state (sort, pagination paths, per-page/current-page).
- `src/app/template/frontend/components/settings/list_view_settings.cljs` — settings panel; Pagination toggle dispatches `::ui-events/toggle-pagination` but respects hardcoded flags.
- `src/app/template/frontend/events/list/ui_state.cljs` — `::toggle-pagination` flips `[:ui :entity-configs <entity> :show-pagination?]` unless hardcoded prevents it.
- `src/app/template/frontend/components/list.cljs` — renders pagination only if merged display settings `:show-pagination?` true and `total-pages > 1`.
- (optional) `src/app/template/frontend/hooks/display_settings.cljs` & `subs/ui.cljs` — how hardcoded page props merge with user prefs.

## Commands to Run
- Start stack: `bb run-app` (backend + shadow-cljs; admin at 8085). If needed, separate watch: `npm run watch:admin`.
- Tests (if changed CLJS/UI logic): `bb fe-test`. Backend unaffected unless API touched.

## Gotchas
- Admin auth simplified but still required; ensure you’re logged in on port 8085.
- Hardcoded display settings (`:show-pagination? true`) make the toggle appear enabled but non-interactive per docs; decide whether to remove/relax or to surface the locked state/tooltip.
- Pagination component already hides when `:show-pagination?` is false AND `total-pages > 1`; verify total-pages derivation when data small.

## Checklist for Next Agent
1) Reproduce on `/admin/audit`: open settings panel, click Pagination toggle; observe no state change/UI feedback.
2) Inspect display settings state via app-db (app-db-inspect skill): `[:ui :entity-configs :audit-logs :show-pagination?]` and hardcoded merge paths; confirm value never flips.
3) Review `entities.edn` hardcoded `:show-pagination? true`; decide whether to drop/parameterize so toggle is user-controlled, or make toggle visibly locked with tooltip consistent with doc matrix.
4) If unlocking: remove hardcoded key or set nil, ensure initial default still true; verify `toggle-pagination` updates merged settings and pagination hides when false (total-pages>1 condition).
5) If keeping locked: adjust settings UI to show disabled state for hardcoded true on audit page (match list-view controls guide), or add copy explaining pagination is always on.
6) Retest UI after change: toggle interaction, table rerender, pagination visibility; run `bb fe-test` if logic changed.
