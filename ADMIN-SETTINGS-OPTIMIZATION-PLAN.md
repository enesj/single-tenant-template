# Admin Settings Optimization Plan

## Summary

The optimization looks feasible. The cleanest path is to stop treating admin settings, user settings, and live pages as separate consumers of slightly different config shapes, and instead introduce one route-aware "resolved settings" layer for list display, list behavior, columns, and form specs. That should let us fix the current Articles failures and reduce the chance of the same bugs on other admin pages.

## Implementation Steps

### 1. Define a single resolved settings contract for list pages and settings editors

Goal:
- Extend the current display/list-config resolver idea into a fuller contract, for example effective `:display`, `:list-config`, `:table-columns`, `:form-fields`, plus `:source` and `:locked` metadata.

Files:
- `src/app/template/frontend/settings/resolver.cljs`
- `src/app/template/frontend/subs/ui.cljs`
- `src/app/template/frontend/db/entity_specs.cljs`
- `src/app/admin/frontend/specs/generic.cljs`

Dependencies:
- None

Owner:
- `Coder`

### 2. Make `/admin/user-settings` render effective user-facing settings with upstream admin policy overlaid

Goal:
- Make user settings show inherited/effective admin locks and defaults instead of only raw user-owned config.

Files:
- `src/app/admin/frontend/events/user_settings/load_save.cljs`
- `src/app/admin/frontend/pages/unified_settings/page.cljs`
- `src/app/admin/frontend/pages/unified_settings/editors.cljs`
- `src/app/admin/frontend/components/settings_views/cards.cljs`
- `src/app/template/backend/routes/admin/settings_io.clj`
- `src/app/template/backend/routes/admin/settings.clj`

Dependencies:
- Step 1

Owner:
- `Coder`

Notes:
- Admin locks/defaults should be visible in user settings as inherited/effective state, not copied into user config. This should resolve the lock-propagation failures.

### 3. Fix the form-fields editor so it edits the same field universe the live forms render

Goal:
- Make the settings editor use resolved form field specs, including fallback and inherited fields, so it matches live create/edit forms.

Files:
- `src/app/admin/frontend/pages/unified_settings/editors.cljs`
- `src/app/template/frontend/components/form.cljs`
- `src/app/admin/frontend/specs/generic.cljs`
- `src/app/admin/frontend/config/form-fields.edn`

Dependencies:
- Step 1

Owner:
- `Coder`

Notes:
- Right now the editor builds checklists from table columns, while forms render from `:form-entity-specs/by-name`. The editor should use the same resolved form layer.

### 4. Unify table-column metadata consumption so labels, sortable/filterable flags, and visibility all come from the same resolved column spec

Goal:
- Make the live table and settings editor rely on one resolved column model for labels, sortability, filterability, and visibility.

Files:
- `src/app/template/frontend/db/entity_specs.cljs`
- `src/app/template/frontend/components/list/table.cljs`
- `src/app/template/frontend/subs/ui.cljs`
- `src/app/admin/frontend/events/settings/table_columns.cljs`
- `src/app/admin/frontend/events/user_settings/table_columns.cljs`

Dependencies:
- Step 1

Owner:
- `Coder`

Notes:
- This should address the display-label failure and make sortable/filterable behavior consistent between editor state and live headers.

### 5. Fix `per-page` so server-paginated admin pages actually use resolved page size for requests

Goal:
- Ensure server-paginated admin pages honor the resolved `per-page` value in backend requests, not just in local UI state.

Files:
- `src/app/template/frontend/components/list.cljs`
- `src/app/template/frontend/events/list/ui_state.cljs`
- `src/app/domain/frontend/expenses/events/events_factory.cljs`
- `src/app/admin/frontend/pages/domain/expenses/articles.cljs`

Dependencies:
- Step 1

Owner:
- `Coder`

Notes:
- `per-page` already flows through local state, but the server request path needs to honor the resolved value predictably on first load and after admin-setting changes.

### 6. Remove or narrow duplicate spec-generation paths so live pages and editors cannot drift

Goal:
- Consolidate spec generation around one route-aware generator.

Files:
- `src/app/template/frontend/db/entity_specs.cljs`
- `src/app/admin/frontend/specs/generic.cljs`
- `src/app/domain/frontend/expenses/admin/adapters/specs.cljs`

Dependencies:
- Steps 1, 3, 4

Owner:
- `Coder`

Notes:
- There is duplicated form-spec generation and multiple spec paths. Consolidating around one route-aware generator should reduce hidden regressions.

### 7. Add focused tests around the resolved layer instead of only smoke-testing raw events

Goal:
- Cover effective label overrides, effective form fields, inherited admin locks in user settings, and server `per-page` usage.

Files:
- `test/app/admin/frontend/events/settings_test.cljs`
- `test/app/admin/frontend/events/user_settings_test.cljs`
- `test/app/template/frontend/settings_test.cljs`
- New focused tests near `src/app/template/frontend/db/entity_specs.cljs` behavior

Dependencies:
- All prior steps

Owner:
- `Coder`

Notes:
- Prefer testing the resolved behavior rather than only testing raw draft updates.

## Edge Cases

- Happy path: admin change immediately affects the live admin page and the effective state shown in settings.
- `nil`: missing user config should inherit from admin and domain defaults without rendering blank or contradictory editors.
- Empty collections: empty `create-fields`, empty `column-metadata`, or empty `column-locks` should mean inherit or fallback, not broken UI.
- Invalid or boundary input: blank display labels should remove overrides cleanly; `per-page` should reject zero, negative, and non-numeric values; unknown columns and fields should be ignored or pruned safely.
- Cross-scope mismatch: user settings must show admin locks as inherited or effective without persisting them into user runtime overrides.
- Local overrides: `ui-entity-prefs` should still override defaults where allowed, but not make the settings editor itself misleading.

## Validation Plan

- REPL or focused frontend checks first:
  - verify resolved form spec for `:articles` create and edit in admin route
  - verify resolved table header label for `category_name`
  - verify effective user-settings lock state when admin lock exists
  - verify server request limit changes when `per-page` changes
- Focused tests to add or update:
  - effective form-fields fallback vs explicit config
  - effective column label override propagation
  - admin-to-user lock propagation in editor state
  - server pagination using resolved `per-page`
- Real UI verification after code changes:
  - `/admin/admin-settings`
  - `/admin/user-settings`
  - `/admin/articles`
  - one or two sibling admin pages such as suppliers or manufacturers to confirm this is shared behavior

## Open Questions / Assumptions

- Assumption: the right long-term model is "raw scope config + resolved effective view", not copying admin policy into user config.
- Assumption: missing `create-fields` should behave as inherited or fallback, not as "intentionally empty".
- Open question: whether admin settings should expose raw config, effective config, or both in the editor UI. Recommended direction: effective view with clear inherited badges.
- Open question: whether all admin expense pages should stay server-paginated, or whether some should move to client mode for simpler settings behavior.
- Open question: whether to fully retire the older spec-generation path in `src/app/admin/frontend/specs/generic.cljs` now, or first route both paths through the same helper and delete the duplicate later.

## Recommendation

Start with Steps 1 and 2 first. They create the shared foundation, and the current issues look much more like raw-vs-resolved drift than a set of unrelated bugs.
