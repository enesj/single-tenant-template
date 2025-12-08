# Plan: Admin Expenses List-View Integration (2025-12-08)

## Goals
- Replace bespoke tables for Expenses, Receipts, Suppliers, and Payers admin pages with the shared `list-view` stack (template entity store + adapters + specs).
- Normalize API data into template list state, including pagination/filter metadata, so view options/export/settings work.
- Preserve existing guarded routing/auth and refresh semantics.

## Constraints / Notes
- Follow template component integration patterns used by Users/Audit/Login Events.
- Keep IDs as strings in template store; handle namespaced keys defensively.
- Respect `resources/public/admin/ui-config/view-options.edn` when wiring `display-settings`.
- Admin served at http://localhost:8085/admin; hot reload via `bb run-app` + `npm run watch:admin`.

## References
- Docs: `docs/frontend/template-component-integration.md`, `docs/frontend/admin-panel-single-tenant.md`.
- Patterns: `app.admin.frontend.adapters.{users,audit,login-events}`, `app.admin.frontend.events.{users,audit,login-events}`, `app.admin.frontend.pages.{users,audit,login-events}`.
- Template list state paths: `app.template.frontend.db.paths`, list events/subs under `app.template.frontend.events.list.*` / `subs`.

## Work Phases
1) Baseline review (patterns & current expenses pages)
   - Inspect current expenses/receipts/suppliers/payers events/subs/pages/routes.
   - Review admin list adapters/events/specs for users/audit/login-events.

2) Entity specs design
   - Define `entity-spec` for each: expenses, receipts, suppliers, payers (columns, renderers, actions if any).
   - Add to admin specs machinery (new adapter namespaces or shared spec ns).

3) Adapter + sync wiring
   - Create adapters to normalize API rows and register `entity-key` + spec + sync events.
   - Ensure pagination/filter metadata stored under template list paths; loading flags aligned.

4) Page integration
   - Replace manual tables with `$ list-view {...}` using entity name/spec, display-settings, optional actions.
   - Use admin layout/wrapper consistent with existing admin list pages.

5) Routing/controllers & loading
   - Ensure controllers dispatch list load with pagination/filter params derived from template state where needed.
   - Maintain auth guard; provide refresh hooks if required.

6) Testing
   - Add/adjust CLJS tests for adapters/normalization.
   - Run `npm run test:cljs` (or `bb fe-test-node`) and sanity manual check instructions.

## Current Status
- Created plan; phases not started.
