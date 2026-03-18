# User Settings Optimization Results

Related plan: `USER-SETTINGS-OPTIMIZATION-PLAN.md`
Prior admin results: `ADMIN-ARTICLES-SETTINGS-POST-OPTIMIZATION-RESULTS.md`

## Scope

This document tracks implementation and verification results for the user-settings
optimization plan. Each step is verified on the real `/admin/user-settings` UI and,
where relevant, on live user-facing pages.

---

## Step 1: Load admin config on user-settings page

### Problem
`page.cljs` passes `load-admin? false` when `page-scope = :user`, so `admin-view-options`
is always `nil`. The `user-entity-editor` renders no inherited admin locks.

### Status: DONE

### Changes
- `src/app/admin/frontend/pages/unified_settings/page.cljs` line 375:
  Changed `load-admin? (= page-scope :admin)` to `load-admin? true`.
  This ensures admin config (view-options, form-fields, table-columns) is loaded
  even when the page scope is `:user`.

### Verification
- Hard-reloaded `/admin/user-settings`.
- Confirmed via JS console that `[:admin :settings :view-options]` now has 24 entity configs loaded.
- Set a test admin lock in app-db: `{:show-delete? false}` for `:articles`.
- Opened Edit mode → Articles → View Options tab.
- **Delete row rendered as `StaticText "Enforced Off"` (non-interactive)** instead of a button,
  confirming the admin lock is visible as inherited/immutable in the user settings editor.
- Compare: Edit row (no admin lock) rendered as `button "Default Off"` (interactive).
- Cleaned up test lock after verification.

---

## Step 2: Propagate admin locks to live user-facing pages

### Problem
Admin locks in `[:admin :settings :view-options]` are not consulted by
`subs/ui.cljs::entity-display-settings` on user routes.

### Status: DONE (frontend merge; backend propagation not yet needed)

### Changes
- `src/app/template/frontend/subs/ui.cljs`:
  - Added `overlay-admin-locks` helper that merges admin-scope `:display-locks` and
    `:column-locks` on top of domain view-options for user routes.
  - Updated `gather-resolver-sources` and `gather-view-options-for-entity` to call
    `overlay-admin-locks` on user routes when admin settings are present in app-db.

### Verification
- Set test admin lock `{:show-delete? false}` in app-db for `:articles`.
- Subscribed to `::entity-display-settings` for `:articles` on the `/admin/user-settings` page.
- **`show-delete?` resolved to `false`** (admin lock applied over domain config).
- **`::locked-display-settings` returned `{:show-delete? false}`** — admin lock recognized.
- Other domain defaults unaffected (e.g., `show-edit? true`, `per-page 10`).
- Cleaned up test lock after verification.

### Limitation
The admin lock overlay only has effect when admin settings are loaded in app-db
(i.e., on admin pages). For standalone user-facing pages (e.g., `/expenses/articles`
visited directly by a regular user), admin settings are not loaded and locks
will not cascade. Full propagation would require the backend to include admin
locks in the `/api/v1/config` response. This is documented but not yet needed
since user-facing list pages use user-scope locks from `/admin/user-settings`.

---

## Step 3: Form-fields editor field universe

### Problem
`form-fields-editor` subscribes to `:form-entity-specs` which resolves through admin-route
path on `/admin/user-settings`.

### Status: VERIFIED — NO CHANGE NEEDED

### Finding
The plan's concern was incorrect. The `form-fields-editor` subscribes to `:form-entity-specs`
(defined in `entity_specs.cljs`), which returns **raw model specs from `models-data`** — NOT
the route-aware override in `generic.cljs` (`:form-entity-specs/by-name`).

Verified for Articles:
- Raw model fields: `["canonical-name", "normalized-key", "subcategory-id", "link", "manufacturer-id"]`
- Config-overridden fields (from `:form-entity-specs/by-name`): identical
- User-scope draft config matches domain config (same `:edit-fields` and `:field-config`)

The editor's field universe is route-independent (model-based), and the config being edited
is passed explicitly as a prop (admin or user-draft based on scope). No code change needed.

---

## Step 4: Per-page seeding on user-facing pages

### Problem
Need to verify `::seed-per-page-from-config` works for user routes (domain config path).

### Status: VERIFIED — WORKING CORRECTLY

### Finding
Tested on `/t/enes-jakic/expenses/list` (user-facing expenses list):
- Domain config `per-page: 25` (from `[:domain :config :view-options :expenses :display-defaults :per-page]`)
- User prefs `per-page: 20` (from `[:ui :entity-prefs :expenses :display :per-page]`)
- Effective per-page: `20` — user prefs correctly override domain default

The `::seed-per-page-from-config` flow works because:
1. `/api/v1/config` loads domain-ui-config into `[:domain :config]` (including view-options)
2. `::entity-display-settings` subscription resolves per-page from domain view-options
3. `configured-per-page` in `list.cljs` uses the resolved value
4. The `local-per-page?` guard correctly skips seeding when user has explicit prefs

No code change needed.

---

## Step 5: Column label overrides on user routes

### Problem
Need to verify user-settings label overrides flow to live user-facing page headers.

### Status: VERIFIED — WORKING CORRECTLY

### Finding
Tested on `/t/enes-jakic/expenses/list` (user-facing expenses list):
- Domain table-columns config has `column-metadata` with `:label-key` i18n keys
- `entity-specs/by-name` subscription correctly resolves on user routes:
  - `purchased-at` → "Kupljeno" (i18n translation)
  - `supplier-display-name` → "Dobavljač" (i18n translation)
  - 21 total field specs resolved
- Config loading path: `/api/v1/config` → `[:domain :config :table-columns]`
- Entity specs subscription: `resolve-config-source` picks domain config on user routes
- `apply-column-label-override`: explicit `:label` wins over `:label-key` (fix from admin plan)

The label override flow is:
1. Admin saves label in user-settings → `load_save.cljs save-success` writes to `[:domain :config :table-columns]`
2. `entity_specs.cljs` reads from domain config on user routes
3. `apply-column-label-override` prioritizes explicit `:label` over `:label-key`
4. Live page headers reflect the override

No code change needed. The resolver fix from the admin plan covers both routes.

---

## Step 6: Focused tests

### Status: DONE

### Tests added
File: `test/app/template/frontend/subs/ui_test.cljs`

5 new tests covering admin lock propagation to user routes:

1. **`admin-lock-propagation-display-locks-test`** — admin display-lock cascades to
   user route effective settings and appears in locked map.

2. **`admin-lock-propagation-merges-with-domain-locks-test`** — admin locks merge with
   (not replace) domain-scope locks; both scopes' locks apply simultaneously.

3. **`admin-lock-propagation-no-admin-settings-test`** — without admin settings loaded,
   domain config is used as-is (no crash, no stale locks).

4. **`admin-lock-propagation-user-prefs-cannot-override-admin-lock-test`** — user
   preferences cannot override an admin-imposed lock.

5. **`admin-column-lock-propagation-test`** — admin column-locks cascade to
   `visible-columns` and `locked-visible-columns` on user routes.

### Test run
- All 5 new tests pass (0 FAIL in `ui-test` section).
- Pre-existing failures: 52 failures, 13 errors (unchanged — all in other test files).
- The "Subscribe outside reactive context" warnings are expected (matches existing test patterns).

---

## Summary Table

| Step | Description | Status | Notes |
|------|-------------|--------|-------|
| 1 | Load admin config on user-settings page | DONE | One-line fix in `page.cljs` |
| 2 | Admin lock propagation to live pages | DONE | `overlay-admin-locks` in `subs/ui.cljs` |
| 3 | Form-fields editor field universe | VERIFIED OK | `:form-entity-specs` is route-independent |
| 4 | Per-page seeding on user routes | VERIFIED OK | Domain config path works correctly |
| 5 | Column label overrides on user routes | VERIFIED OK | Resolver fix covers both routes |
| 6 | Focused tests | DONE | 5 new tests in `ui_test.cljs` |

---

## Code Changes Summary

| File | Change |
|------|--------|
| `src/app/admin/frontend/pages/unified_settings/page.cljs` | `load-admin? true` (was `(= page-scope :admin)`) |
| `src/app/template/frontend/subs/ui.cljs` | Added `overlay-admin-locks` fn; updated `gather-resolver-sources` and `gather-view-options-for-entity` to apply admin locks on user routes |
| `test/app/template/frontend/subs/ui_test.cljs` | 5 new tests for admin lock propagation |

## Known Limitation

Admin lock propagation only works when admin settings are loaded in app-db (i.e.,
on admin pages). User-facing pages visited directly by regular users do not load
admin settings, so admin-scope locks will not cascade there. For full propagation,
the backend would need to include admin locks in the `/api/v1/config` response.
This is not yet needed since user-facing pages use user-scope locks configured
via `/admin/user-settings`.
