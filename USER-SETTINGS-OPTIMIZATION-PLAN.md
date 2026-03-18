# User Settings Optimization Plan

## Summary

The `/admin/user-settings` page has a set of concrete, fixable gaps — most of which mirror
the issues addressed in `ADMIN-SETTINGS-OPTIMIZATION-PLAN.md` but at the **user-settings ↔
live-user-page** boundary rather than the admin-settings ↔ admin-page boundary.

The two biggest gaps are:

1. **Admin-imposed locks are invisible in the user-settings editor.** The page never loads
   admin config (`load-admin? false`), so `admin-view-options` is always `nil`, and the
   "immutable / inherited from admin" lock overlay never renders.

2. **Admin locks do not flow to live user-facing pages.** The resolver reads
   `[:domain :config :view-options]` for user routes. Admin locks live in
   `[:admin :settings :view-options]`. There is no runtime merge step, so a lock set in
   `/admin/admin-settings` has no effect on e.g. `/expenses/articles`.

Steps 1–2 below address these two root causes. Steps 3–5 cover the remaining
gaps (form fields, per-page, label overrides) that mirror the SKIPPED tests
from the admin plan.

---

## Context: What the Admin-Settings Plan Already Fixed

The following were resolved in `ADMIN-SETTINGS-OPTIMIZATION-PLAN.md` and are already
working for admin routes:

| Fix | File | Status |
|-----|------|--------|
| `::seed-per-page-from-config` seeds per-page from admin config on first load | `events/list/ui_state.cljs` | DONE |
| `resolve-column-label-override` — explicit `:label` wins over `:label-key` | `settings/resolver.cljs` | DONE |
| `:admin/sortable-columns` uses route-aware `resolve-config-source` | `subs/config.cljs` | DONE |

These fixes **partially** benefit user routes (label priority is shared logic, per-page
seeding shares the same event). The steps below verify and extend that coverage.

---

## Implementation Steps

### 1. Load admin config when visiting `/admin/user-settings`

**Goal:**
Make the user-settings editor aware of admin-imposed locks so it can show them as
"inherited / immutable" in the `user-entity-editor` card. Currently `load-admin? false`
means `admin-view-options` is always `nil` on this page.

**Root cause:**
`page.cljs` line 373-377:
```clj
(rf/dispatch [::unified-events/init {:initial-scope page-scope
                                     :fixed-scope   page-scope
                                     :load-admin?   (= page-scope :admin)   ; ← false on user-settings
                                     :load-user?    (= page-scope :user)}])
```
`user-entity-editor` (in `editors.cljs`) receives `admin-view-options` from
`(get admin-config selected-entity)`, but `admin-config` is nil, so
`admin-locks` is always `{}` and the inherited-lock section never renders.

**Fix:**
Change `load-admin?` to `true` when `page-scope = :user`. Admin config is cheap
(already cached after visiting `/admin/settings`) and is read-only on the user-settings
page — it is only used to display inherited lock state, not to drive edits.

**Files:**
- `src/app/admin/frontend/pages/unified_settings/page.cljs`

**Dependencies:**
- None

**Owner:** Coder

---

### 2. Propagate admin locks to live user-facing pages

**Goal:**
When admin sets a display lock in `/admin/admin-settings` (e.g., `show-delete? locked = true`),
that lock must also prevent the action on `/expenses/articles` and similar user-facing pages.
Currently it has no effect because the resolver reads only
`[:domain :config :view-options]` for user routes, which does not include admin-scope locks.

**Root cause:**
`resolver/resolve-display-settings` and `subs/ui.cljs::entity-display-settings` source
all settings from the domain config path. The admin config path
(`[:admin :settings :view-options]`) is never consulted for user routes.

There are two valid approaches — recommend approach (a):

**(a) Runtime merge in the subscription (frontend-only fix):**
`subs/ui.cljs::entity-display-settings` can read both paths and let the resolver
apply admin locks on top. This requires no backend change, is transparent to
live pages, and keeps the resolver as the single authority.

**(b) Backend merge at load time:**
`GET /admin/api/settings/user-ui-config` returns admin locks merged into the
user-ui-config payload. Simpler for the frontend but mixes scopes at the API
boundary and makes the admin-lock layer harder to distinguish in the editor.

**Files:**
- `src/app/template/frontend/subs/ui.cljs`
- `src/app/template/frontend/settings/resolver.cljs`
- (if approach b) `src/app/template/backend/routes/admin/settings_io.clj`
- (if approach b) `src/app/template/backend/routes/admin/settings.clj`

**Dependencies:**
- Step 1 (admin config must be loaded before it can be merged)

**Owner:** Coder

**Notes:**
- Approach (a) means the subscription needs two signal inputs: admin-config path +
  domain-config path. `resolve-display-settings` already knows how to merge locks
  over defaults; it just needs to be called with the admin lock map as an additional
  overlay layer.
- The user-settings editor already renders inherited locks via `immutable-locks`
  (after Step 1 fixes the nil `admin-view-options`). Step 2 makes those same locks
  effective on the live page, not just visible in the editor.

---

### 3. Fix form-fields editor to use user-route field universe

**Goal:**
The `form-fields-editor` field universe must match what the live user-facing create/edit
form actually renders. Currently it subscribes to `:form-entity-specs` which resolves
through the admin-route path (because `/admin/user-settings` is an admin route).
The field list shown in the editor therefore reflects admin form-field configuration,
not domain configuration.

**Root cause:**
`editors.cljs` `form-fields-editor`:
```clj
all-form-specs (use-subscribe [:form-entity-specs])
```
`specs/generic.cljs::form-entity-specs/by-name` uses `resolver/resolve-config-source`
which returns admin config when `(paths/admin-route? db)` is true. On
`/admin/user-settings` the route IS an admin route, so the resolver always picks
admin form-fields config as the field universe source.

**Fix:**
The user-scope form-fields editor should derive its field universe from the **domain
form-fields config** rather than the currently-resolved admin form spec. Two options:

**(a) Pass explicit domain-config field universe as a prop:**
`page.cljs` can derive the field universe from `(get-in user-draft [:form-fields entity-kw])`
and pass it as a prop to `form-fields-editor`. The component uses it as the fallback
universe instead of `:form-entity-specs`.

**(b) Add a separate `:user-form-entity-specs` subscription:**
A new subscription that always resolves from `[:domain :config :form-fields]` regardless
of current route. This mirrors how the live user page will see it.

Approach (a) is lower impact and avoids a new subscription.

**Files:**
- `src/app/admin/frontend/pages/unified_settings/editors.cljs`
- `src/app/admin/frontend/pages/unified_settings/page.cljs`
- (if approach b) `src/app/admin/frontend/specs/generic.cljs`

**Dependencies:**
- Step 1 (user-draft is already loaded; this step just changes which sub the editor uses)

**Owner:** Coder

**Notes:**
- This is the form-fields issue (Test 10) SKIPPED in the admin plan.
- The admin form-fields tab is unaffected — it should keep using `:form-entity-specs`
  resolved from admin config.

---

### 4. Verify and fix per-page seeding on user-facing list pages

**Goal:**
Ensure user-facing list pages (e.g. `/expenses/articles`) honor the resolved
`per-page` value from domain config on first load, not just in local UI state.
The admin-route fix (`::seed-per-page-from-config`) seeds from
`[:admin :settings :view-options]`. The user-route equivalent must seed from
`[:domain :config :view-options]`.

**Root cause (likely):**
`::seed-per-page-from-config` fires on list-component mount and reads from admin
config path. On user routes, the domain config path
(`[:domain :config :view-options :entity :display-defaults :per-page]`) may not
be consulted.

**Verify:**
1. Visit a user-facing list page while logged in as a non-admin user.
2. Check that the default rows per page matches the value in
   `[:domain :config :view-options :entity :display-defaults :per-page]`.
3. Change the per-page value in `/admin/user-settings`, save, reload the list page,
   confirm it changed.

**Files:**
- `src/app/template/frontend/events/list/ui_state.cljs`
- `src/app/template/frontend/components/list.cljs`
- `src/app/template/frontend/subs/ui.cljs`

**Dependencies:**
- Step 2 (admin-locked per-page should also be immutable on user pages after Step 2)

**Owner:** Coder

**Notes:**
- If `::seed-per-page-from-config` already reads from `::entity-display-settings`
  (which resolves from the correct scoped config), this step may be a no-op.
  Verify via REPL/browser before coding.

---

### 5. Verify column label overrides flow from user settings to live pages

**Goal:**
Column label overrides saved in `/admin/user-settings` → Table Columns tab must
appear on the live user-facing list page headers.

**Data flow to verify:**
1. User saves a label override for e.g. `category_name` → `"My Category"`.
2. `save-success` writes to `[:domain :config :table-columns :entity :column-metadata]`.
3. `entity_specs.cljs::entity-specs/by-name` picks up that path for user routes
   (via `resolve-config-source`).
4. `resolver/apply-column-label-override` now prioritises explicit `:label` over
   `:label-key` (fix already in place).
5. Live list column header shows `"My Category"`.

**Files:**
- `src/app/template/frontend/db/entity_specs.cljs`
- `src/app/template/frontend/settings/resolver.cljs` (already fixed)

**Dependencies:**
- Step 1 (user-settings must be loaded before label overrides exist in domain config)

**Owner:** Coder

**Notes:**
- This is likely already working after the `resolve-column-label-override` fix. Confirm
  via browser test before coding — the step may be pure verification.

---

### 6. Add focused tests for the resolved user-settings layer

**Goal:**
Cover the scenarios that were previously untestable or untested:
- Admin lock propagation to user routes (Step 2 outcome)
- Form-fields editor shows user-route field universe (Step 3 outcome)
- Per-page seeding on user-facing pages (Step 4 outcome)
- Column label overrides flow end-to-end (Step 5 outcome)

**Files:**
- `test/app/admin/frontend/events/user_settings_test.cljs` (new or extend)
- `test/app/template/frontend/settings_test.cljs` (new or extend)
- Focused REPL checks (see Validation Plan below)

**Dependencies:**
- All prior steps

**Owner:** Coder

**Notes:**
- Prefer testing the resolved/effective behavior (what does the live page see?) over
  raw draft mutations (what does the editor store?).

---

## Edge Cases

- **Admin locks + user locks on same setting:** Admin lock must always win (it is the
  `immutable-locks` layer). User-set lock should be ignored or rejected when admin also
  locks the same setting.
- **`nil` admin config:** If admin config fails to load (network error), user-settings editor
  must still be usable — inherited lock section should show a loading/error state, not crash.
- **Empty domain config:** Missing `display-defaults`, `display-locks`, `column-metadata`, or
  `form-fields` for an entity should inherit from template defaults without rendering blank
  or contradictory UI.
- **`per-page` boundary values:** Zero, negative, and non-numeric values must be rejected;
  UI should revert or show a validation error.
- **Label override cleared to blank:** Clearing a label in user settings should remove the
  `:label` key (not persist an empty string), and the live page should fall back to
  `:label-key` i18n translation.
- **Lock cleared by admin after user stored a conflicting default:** User config must
  re-read the effective state rather than showing a stale lock badge.

---

## Validation Plan

REPL / frontend checks first:

1. **Admin lock visibility in editor (Step 1):**
   - Set a lock in `/admin/admin-settings` for an entity (e.g., lock `show-delete? = false`).
   - Navigate to `/admin/user-settings`, select the same entity, View Options tab.
   - Verify the lock is shown as "Inherited from admin" / disabled toggle.

2. **Admin lock propagation to live page (Step 2):**
   - With the lock from above, open the live user-facing list page.
   - Verify the Delete button is absent or disabled.
   - Remove the admin lock, reload, verify Delete returns.

3. **Form-fields editor universe (Step 3):**
   - Open `/admin/user-settings` → Form Fields tab for `:articles`.
   - Verify the field checklist matches the fields shown in the live `/expenses/articles`
     create/edit form, not the admin-route form.

4. **Per-page seeding on user routes (Step 4):**
   - Set `per-page = 10` in `/admin/user-settings` for `:articles`, save.
   - Navigate to the live user-facing articles list.
   - Verify 10 rows shown without needing to manually change the selector.

5. **Column label override on user route (Step 5):**
   - Set a custom label for `category_name` in user-settings Table Columns, save.
   - Navigate to the live user-facing list.
   - Verify the column header shows the custom label.
   - Clear the label, verify fallback to i18n translation.

Focused tests to add:

- Effective admin-lock overlay in `::entity-display-settings` subscription.
- User-settings `form-fields-editor` renders domain field universe.
- `::seed-per-page-from-config` reads from domain config on user routes.
- Column label override end-to-end (user settings save → live page spec).

Real UI spot-check pages after all steps:

- `/admin/user-settings`
- `/admin/admin-settings` (regression — must not be broken)
- `/expenses/articles` (or whichever live entity is configured)
- One additional entity (e.g. suppliers or stores)

---

## Open Questions / Assumptions

- **Assumption:** Admin config is cheap enough to load on the user-settings page. If it is
  slow (large payload), a lighter endpoint returning only locks could be introduced.
- **Assumption:** The admin-lock merge should happen at the subscription level (approach a in
  Step 2), not at the API level. This keeps scopes clean and avoids mixing admin-owned data
  into user-owned API responses.
- **Open question:** Should the user-settings page allow admins to *see* admin locks but
  not *edit* them? Current design shows them as disabled toggles, which is the recommended UX.
- **Open question:** Should clearing an admin lock automatically reset the user default to
  nil/inherit, or leave the user default in place? Recommended: leave user default in place
  (user config is independent of admin config).

## Recommendation

Start with Step 1 (trivial one-line fix in `page.cljs`) and Step 2 (runtime merge in the
subscription). Together they fix the most visible gap — admin locks having no effect on user
pages — and they make the user-settings editor honest about what it represents. Steps 3–5
can follow in any order, as they are independent of each other after Step 1.
