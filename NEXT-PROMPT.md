NEXT SESSION PROMPT — Simplify list view display settings (2025-12-09)

Context snapshot
- Single-tenant template (no RLS); admin console at http://localhost:8085/admin uses shared template components (re-frame + UIx).
- Display settings merge chain lives in `app.template.frontend.subs.ui/::entity-display-settings` (hardcoded admin config → defaults → user prefs) and is exposed via `app.template.frontend.hooks.display-settings`.
- Admin settings page `/admin/settings` reads/writes `resources/public/admin/ui-config/view-options.edn` via backend settings routes; config cached by `app.admin.frontend.config.loader` and preloaded in `app.admin.frontend.config.preload`.
- List-view settings panel UI is in `app.template.frontend.components.settings.list-view-settings`; it toggles controls/filters and column visibility, mixing vector-based admin config with template boolean maps.
- Shared list display defaults/helpers are in `app.template.frontend.utils.shared` and list settings events in `app.template.frontend.events.list.settings`; column config helpers in `app.template.frontend.utils.column_config` handle vector vs map modes.
- Admin view options and table columns are preloaded from EDN and stored in the `config-cache` atom; subs like `:admin/view-options` read directly from that cache (bypassing app-db).
- Skills available: `app-db-inspect` (inspect app-db), `reframe-events-analysis` (trace events), `system-logs` (monitor backend/shadow logs) — prefer these over ad-hoc tooling.

Task focus
- Refactor the list-view display settings system to make configuration simpler and clearer while preserving per-entity controls (toggles, pagination, filters, widths, add/batch/inline buttons) and user-configurable vs hardcoded behaviors.

Code map (quick purpose)
- `resources/public/admin/ui-config/view-options.edn` — current per-entity hardcoded view options (pagination, filtering, export/bulk, visibility defaults).
- `src/app/admin/frontend/config/preload.cljs` — inlines EDN configs into the bundle and registers them in the config cache.
- `src/app/admin/frontend/config/loader.cljs` — config cache + getters for view options/table columns/form fields; exposes `get-all-view-options` used by subs and settings page.
- `src/app/admin/frontend/config/entities.edn` (and template variant) — entity registry/specs referenced by list pages.
- `src/app/admin/frontend/events/settings.cljs` — admin page events to load/update/remove view options via `/admin/api/settings` and sync cache.
- `src/app/admin/frontend/subs/config.cljs` — subs for view options (`:admin/view-options`, `:admin/all-view-options`) hitting the config cache.
- `src/app/template/frontend/subs/ui.cljs` — authoritative merge for display settings (hardcoded + defaults + user prefs) and control visibility flags.
- `src/app/template/frontend/hooks/display_settings.cljs` — hook wrapper exposing merged settings to components.
- `src/app/template/frontend/components/settings/list_view_settings.cljs` — UI for table settings panel (toggle buttons, filter icons, table width, rows per page, column visibility).
- `src/app/template/frontend/events/list/settings.cljs` — shared events/subs for filterable fields, visible columns, table width stored under `[:ui :entity-configs ...]`.
- `src/app/template/frontend/utils/column_config.cljs` — helpers to reconcile vector-based admin config with template boolean maps and dispatch the right toggle events.
- `src/app/template/frontend/utils/shared.cljs` — default list display settings and hooks returning entity spec + display settings.

Commands to run
- Stack is already started with auto-reload (serves admin on 8085).
- Admin build/watch: `npm run watch:admin` (or `npm run watch` for public build if needed).
- CLJS tests: `npm run test:cljs` or `bb fe-test-node` (primary fast suite).
- REPL tracing/debugging: `clj-nrepl-eval --discover-ports` then attach; use skills `app-db-inspect` and `reframe-events-analysis` via CLJS eval.

Gotchas & assumptions
- Hardcoded view options hide corresponding controls in the panel but still enforce visibility; merging precedence in `subs.ui` matters when simplifying.
- Vector-based config (table-columns + view-options) is cached outside app-db; user prefs live under `[:ui :entity-configs ...]` — refactor must unify/clarify these sources.
- Always-visible columns and inverted configs are normalized in `config.preload`/`column_config`; avoid breaking these transforms.
- Single-tenant defaults (no tenant switch, simplified auth); port 8085 is expected by scripts/tests.
- `docs/frontend/feature-guides/admin.md` referenced in index is missing in repo (note for doc fixes).

Checklist for next agent
1) Reconfirm current flow: how view-options EDN → config loader cache → subs/ui merge → hooks → list-view components; trace toggles (column/filter/pagination) through events and state paths.
2) Decide target simplification: single source for display settings, clearer precedence between hardcoded + user overrides, and minimal duplication between vector-mode and map-mode paths.
3) Decide if we should keep the config files in public/admin/ui-config or move all to src/app/admin/frontend/config.
4) Design refactor plan (write it to a new markdown plan in repo root; keep progress updated there) including data model, API surface, and UI/UX changes for the settings panel.
5) Update settings panel + hooks/subs/events to match new model; ensure admin settings page still persists hardcoded overrides (or migrate as needed).
6) Align entity preload/loader so EDN configs map cleanly to the new structure; keep backwards compatibility or write migration for existing EDN keys.
7) Verify list pages (users, audit, login-events, expenses domain lists) still respect toggles and defaults; cover pagination/highlights/filtering/select/edit/delete/add/batch behaviors.
8) Run CLJS tests and manual smoke on `/admin/settings`, `/admin/users`, `/admin/audit`, `/admin/login-events`; watch logs via `system-logs` skill if issues arise.

Notes
- Missing doc: `docs/frontend/feature-guides/admin.md` not present; add/update if needed.
- After reading this file, create a comprehensive implementation plan markdown in the repo root (e.g., `PLAN-list-view-refactor.md`) to track progress, then start the implementation immediately without asking for approval.
