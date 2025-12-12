# NEXT Agent Prompt — 2025-12-12 — ✅ IMPLEMENTATION COMPLETE

**STATUS:** Implementation complete. Ready for manual testing.

**PROMPT:** List view: support custom add/edit forms + modal; use for Admin Expenses

## Implementation Summary

### Changes Made

#### Template List View Enhancement (`src/app/template/frontend/components/list.cljs`)
- Added modal state management: `add-modal-open?`, `edit-modal-open?`, `edit-modal-item`
- Added new props: `:render-add-form`, `:render-edit-form`, `:form-display`, `:on-add-success`, `:on-edit-success`
- Modal rendering with `modal-wrapper` when custom forms provided and `:form-display :modal`

#### UI Components Updates
- `list/ui.cljs` - header-section accepts `on-add-click` for custom add button behavior
- `list/cells.cljs` - edit-button, action-buttons, reactive-action-cell accept `on-edit-click` callback
- `list/rows.cljs` - render-row passes `on-edit-click` to action buttons

#### Expense Form Components (`src/app/domain/frontend/expenses/components/expense_form.cljs`)
- Created `expense-form-body` - reusable form UI for both page and modal
- Created `expense-add-form-modal` - modal wrapper for creating expenses
- Created `expense-edit-form-modal` - modal wrapper for editing expenses

#### Expense Events (`src/app/domain/frontend/expenses/events/expenses.cljs`)
- Added `::create-entry-modal` with callback support
- Added `::update-entry-modal` with callback support
- Added `::call-modal-callback` helper event

#### Expense List Page (`src/app/domain/frontend/expenses/pages/admin/expense_list.cljs`)
- Rewritten to use `list-view` directly with modal form props
- Custom `expenses-entity-spec` for table display
- `render-add-form` and `render-edit-form` functions for modal content

### Files Modified
1. `src/app/template/frontend/components/list.cljs`
2. `src/app/template/frontend/components/list/ui.cljs`
3. `src/app/template/frontend/components/list/cells.cljs`
4. `src/app/template/frontend/components/list/rows.cljs`
5. `src/app/template/frontend/events/config.cljs` (added noop event)
6. `src/app/domain/frontend/expenses/components/expense_form.cljs` (NEW)
7. `src/app/domain/frontend/expenses/events/expenses.cljs`
8. `src/app/domain/frontend/expenses/pages/admin/expense_list.cljs`

### Verification
- Both `app` and `admin` builds compile with 0 warnings
- Plan file: `PLAN-list-view-expenses-modal.md`

---

## Original Context

## Context snapshot (single-tenant template)
- Single-tenant SaaS template (Clojure/ClojureScript + PostgreSQL) with Admin UI served at `http://localhost:8085/admin`.
- Admin UI uses Re-frame + UIx; common list/table UX comes from template components under `src/app/template/frontend/components/*`.
- Many admin pages are configuration-driven via `generic-admin-entity-page` and `entities.edn`.
- The template `list-view` currently supports **generic add** (inline “add form”) and **generic edit** (inline row edit) via `app.template.frontend.components.form/form`.
- Expenses domain is mounted into admin router under `/admin/expenses`, `/admin/expenses/new`, `/admin/expenses/:id`.
- Expenses are non-trivial: they depend on other entities (supplier, payer) and contain line-items; generic list-view form is not sufficient.
- Requirement: allow `list-view` to accept **custom add/edit forms** and optionally show them in a **modal** (not only inline).

## Task focus
Implement reusable support in `list-view` for:
1) **Custom add form** and **custom edit form** renderers (opt-in; keep existing behavior as default).
2) Ability to render add/edit forms either **inline** (existing) or in a **modal**.
3) For **Admin Expenses** list (`/admin/expenses`): show “Add” and “Edit” as modal windows using the UI/fields of the form at `/admin/expenses/new` (and a similar edit form).

## Code map (starting points)
- `src/app/template/frontend/components/list.cljs` — `list-view` main component; currently drives add-form toggle and row-editing.
- `src/app/template/frontend/components/list/ui.cljs` — `header-section` and `add-item-section` (generic add form rendering).
- `src/app/template/frontend/components/list/rows.cljs` — `render-row` decides inline edit form vs normal row.
- `src/app/template/frontend/components/list/cells.cljs` — `edit-button` dispatches `::config-events/set-editing` (inline edit trigger).
- `src/app/template/frontend/events/config.cljs` — UI events `::set-show-add-form`, `::set-editing` (currently global `[:ui ...]`).
- `src/app/template/frontend/subs/ui.cljs` — subs `::show-add-form`, `::editing`, display-settings merging.
- `src/app/template/frontend/components/modal_wrapper.cljs` + `src/app/template/frontend/components/modal.cljs` — existing modal implementations.

Admin wiring:
- `src/app/admin/frontend/components/generic_admin_entity_page.cljs` — config-driven pages; delegates rendering to content renderer.
- `src/app/admin/frontend/renderers/content.cljs` — calls `list-view` with `:render-actions` and display settings.
- `src/app/admin/frontend/renderers/actions.cljs` — creates row-actions renderer from entity config.
- `src/app/admin/frontend/config/entities.edn` — includes `:expenses` config (currently `:show-edit? false`, `:show-add-button? true`).
- `src/app/admin/frontend/events/users/template/form_interceptors.cljs` — intercepts generic template form submissions (incl. `:expenses`), routes them through template list CRUD; this is NOT suitable for expense line-items.

Expenses domain:
- `src/app/domain/frontend/expenses/routes.cljs` — admin routes: `/admin/expenses`, `/admin/expenses/new`, `/admin/expenses/:id`.
- `src/app/domain/frontend/expenses/pages/admin/expense_list.cljs` — currently `($ generic-admin-entity-page :expenses)`.
- `src/app/domain/frontend/expenses/pages/admin/expense_form.cljs` — `admin-expense-form-page` (manual expense entry; line-items; loads suppliers/payers; dispatches `::expenses-events/create-entry`).
- `src/app/domain/frontend/expenses/pages/admin/expense_detail.cljs` — detail page (not an edit form today).
- `src/app/domain/frontend/expenses/events/entity_configs.cljs` + `src/app/domain/frontend/expenses/events/events_factory.cljs` — generates `load-list`, `load-detail`, `create-entry` (note: create-success currently navigates to `/admin/expenses/:id`).

## Commands to run (save outputs; do not re-run tests)
- Start dev stack (already auto-reloads): `bb run-app`
- Read logs (compile/runtime): `./scripts/sh/monitoring/read_output.sh -f`
- Frontend tests (save once): `npm run test:cljs 2>&1 | tee /tmp/fe-test.txt`
- Backend tests (save once): `bb be-test 2>&1 | tee /tmp/be-test.txt`

## Skills to use (MCP)
- `system-logs` — check backend/shadow build/runtime errors, restart system via nREPL if needed.
- `app-db-inspect` — inspect re-frame app-db if modal/list state behaves oddly.
- `reframe-events-analysis` — trace event sequences (especially around add/edit/open/close modal).

## Gotchas / constraints
- Admin UI runs on **8085**, not 3000.
- `list-view` add/edit UI state is currently global (`[:ui :show-add-form]`, `[:ui :editing]`), so modal state must not leak across entities/pages.
- `list-view` currently hides the table when `show-add-form?` is true (generic add mode). For modal add, keep table visible.
- `cells/edit-button` hardcodes inline editing via `::config-events/set-editing`. For modal edit, you likely need an override (callback) or a custom actions renderer.
- Expenses create currently uses `::expenses-events/create-entry` and then navigates to detail. For “create in modal from list”, adjust success behavior (e.g., optionally close modal + refresh list instead of navigating).
- There is no obvious “update-entry” event in expenses events factory today; confirm backend supports `PUT /admin/api/expenses/entries/:id` and decide what edit should update (header fields only vs items too).

## Checklist (what to do next session)
1) Docs refresh: skim `docs/index.md` + `docs/ai-quick-access.md`, then re-skim relevant frontend docs after each phase.
2) Add reusable API to `list-view`:
   - Accept optional `:render-add-form` and `:render-edit-form` (or similar) and a `:form-display` mode (`:inline` vs `:modal`).
   - Keep default behavior (generic inline add/edit) unchanged when custom props are absent.
3) Implement modal behavior:
   - Add internal (component-local) state for “add modal open?” and “edit modal open + which item?”.
   - Reuse existing modal component(s) (`modal-wrapper` or `modal`) for consistent UX.
   - Provide clean close behavior that clears errors and resets state.
4) Wire expenses page to use the new extension points:
   - Extract reusable form UI from `admin-expense-form-page` so it can render inside a modal (remove breadcrumbs/back button in modal variant).
   - Add an Edit modal variant (prefill from `load-detail`; confirm update API/event and implement as needed).
   - Update `entities.edn` / admin renderer wiring so `:expenses` uses custom add/edit forms and enables the appropriate actions.
5) Verify end-to-end in the browser:
   - `/admin/expenses`: Add opens modal with expense form UI; Save creates expense and refreshes list (no unwanted navigation).
   - `/admin/expenses`: Edit opens modal with prefilled form; Save updates and refreshes list.
   - Ensure other entities’ list views still work (no regressions).
6) Tests:
   - Run `npm run test:cljs 2>&1 | tee /tmp/fe-test.txt` once; only grep/analyze the saved file.

## Planning / execution instructions (important)
- Create a comprehensive implementation plan file in repo root (app root): `PLAN-list-view-expenses-modal.md`.
- Use that plan file to track progress across phases.
- Track each single phase’s implementation notes (what changed/what failed/hypothesis) in the Clojure MCP scratch pad.
- After the plan is created, start implementation immediately (do not pause to ask for approval).
