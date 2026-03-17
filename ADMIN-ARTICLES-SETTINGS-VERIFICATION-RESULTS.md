# Admin Settings -> Articles Page Verification Results

Related plan: `ADMIN-ARTICLES-SETTINGS-VERIFICATION-PLAN.md`

## Scope

This document records execution results against the plan tasks in
`ADMIN-ARTICLES-SETTINGS-VERIFICATION-PLAN.md`.

Test method:
- Changes were made through the real `/admin/admin-settings` UI.
- Verification was done on the real `/admin/articles` page after reloads.
- DOM/script checks were used only to speed up observation after UI actions.

Important note:
- This browser had existing local `ui-entity-prefs` overrides for `:articles`.
- Those overrides masked some admin-default changes until they were temporarily cleared.
- Original local overrides were restored again after the latest clean-state checks.

## Baseline

### P.1 Capture current Articles admin settings

Status: `DONE`

Observed baseline:
- Badge summary: `4 defaults`, `No locks`
- View Options:
  - `Edit`: `Default On`, lock `Inherit`
  - `Delete`: `Default Off`, lock `Inherit`
  - `Selection`: `Inherit`, lock `Inherit`
  - `Filtering`: `Default On`, lock `Inherit`
  - `Pagination`: `Inherit`, lock `Inherit`
  - `Highlights`: `Inherit`, lock `Inherit`
  - `Add Button`: `Default Off`, lock `Inherit`
  - `Batch Edit`: `Inherit`, lock `Inherit`
  - `Batch Delete`: `Inherit`, lock `Inherit`
  - `Rows per page`: default `25`, lock `-`
- List Behavior:
  - `Form display`: `Modal`
  - `Disallowed action mode`: `Disable`
  - Gates:
    - `Add`: `No gate`
    - `Edit`: `No gate`
    - `Delete`: `No gate`
    - `Selection`: `No gate`
- Form Fields baseline recorded for `Create` and `Edit`
- Table Columns baseline recorded for policy and structural settings

### P.2 Capture current Articles page state

Status: `DONE`

Observed baseline before clearing local overrides:
- Visible headers: `Artikal`, `Kategorija`, `Potkategorija`, `Proizvođač`, `Kreirano`
- Selection checkboxes visible
- Filter controls visible
- Pagination visible
- `25` rows shown
- Add, edit, delete, batch edit, and batch delete controls were visible because of local overrides

## Results By Plan Task

### 1. Show Edit Button

Status: `PASS WITH CAVEAT`

Result:
- With existing local overrides still active, changing the admin default did not change the live page.
- After clearing local overrides, setting `Edit` to `Default Off` hid row edit buttons on `/admin/articles`.
- Restoring `Edit` to `Default On` brought the row edit buttons back.

### 2. Show Delete Button

Status: `PASS WITH CAVEAT`

Result:
- In clean state, `Delete = Default Off` hid row delete buttons.
- Changing `Delete` to `Default On` made row delete buttons reappear.

### 3. Show Selection

Status: `PASS`

Result:
- In clean state, setting `Selection = Default Off` removed the header checkbox and row checkboxes from `/admin/articles`.
- Restored to baseline afterward.

### 4. Show Filtering

Status: `PASS`

Result:
- In clean state, setting `Filtering = Default Off` removed filter controls from `/admin/articles`.
- Restored to baseline afterward.

### 5. Show Pagination

Status: `PASS`

Result:
- In clean state, `/admin/articles` showed visible pagination controls and page text such as `Page 1 of 35`.
- Changed `Pagination` to `Default Off` in `/admin/admin-settings`.
- Saved and reloaded `/admin/articles`.
- Pagination controls disappeared and the page-count text was no longer shown.

Current note:
- With pagination off, the live table still rendered `10` rows in the current view, so this setting appears to hide pagination UI rather than expand the list into all rows.

### 6. Show Highlights

Status: `PARTIAL / UI CRUD PATH BLOCKED`

Result:
- Verified from code that highlights are applied by frontend UI state, not by raw timestamps alone:
  - recently created row class: `bg-blue-200/50`
  - recently updated row class: `bg-green-200/50`
- Turning `Highlights` on by itself did not change existing rows in the current dataset.
- A temporary test article was inserted for verification and surfaced on the real `/admin/articles` page.
- With no frontend `recently-created` or `recently-updated` state set, the visible test row had no highlight class.
- After seeding the live app state for `:ui :recently-created :articles` with the test row ID, the visible row rendered with `bg-blue-200/50`.
- After updating the same test article and seeding `:ui :recently-updated :articles`, the visible row rendered with `bg-green-200/50`.
- The temporary article was deleted afterward.

Important limitation:
- The actual add/edit UI controls on `/admin/articles` did not open a working create/edit form in this session, even when the Add control was visible/enabled.
- Because of that, this was not a full end-to-end create/edit-through-UI confirmation.
- What was confirmed is the live table highlight rendering behavior once the frontend success-tracking state is present.

### 7. Show Add Button

Status: `PASS`

Result:
- In clean state, baseline `/admin/articles` did not expose the add control.
- Changed `Add Button` from `Default Off` to `Default On` in `/admin/admin-settings`.
- Saved, reloaded `/admin/articles`, and confirmed `btn-add-articles` appeared.
- Restored to baseline afterward.

### 8. Batch Edit & Batch Delete

Plan refs:
- `8.1`
- `8.2`

Status: `PASS`

Result:
- In clean state, baseline `/admin/articles` did not show batch edit/delete controls.
- Changed both `Batch Edit` and `Batch Delete` to `Default On` in `/admin/admin-settings`.
- Saved and reloaded `/admin/articles`.
- Batch controls appeared in disabled state when no rows were selected.
- After selecting two rows, the controls became active as `Edit 2 selected items` and `Delete 2 selected items`.
- Restored to baseline afterward.

### 9. Rows Per Page

Status: `FAIL`

Result:
- Clean-state baseline on `/admin/articles` rendered `10` rows and showed `Page 1 of 35`, even though the admin setting displayed `Rows per page = 25`.
- Changed `Rows per page` to `5` in `/admin/admin-settings`.
- Saved and reloaded `/admin/articles`.
- The list still rendered `10` rows and still showed `Page 1 of 35`.

Current interpretation:
- The saved admin setting for rows-per-page is not driving the live Articles list as expected.
- There may be another runtime source overriding page size, or the setting is not being consumed.

### 11. Table Columns - Visibility

Plan refs:
- `11.1`
- `11.2`
- `11.3`

Status: `PASS`

Result:
- Unchecked `category_name` -> `Default Visible` in `Table Columns`.
- Reloaded `/admin/articles`.
- `Kategorija` column disappeared as expected.
- Re-checked it and confirmed the column returned.

### 12. Table Columns - Display Labels

Plan ref:
- `12.2`

Status: `FAIL`

Result:
- Changed `category_name` display label to `TEST LABEL` in `Table Columns`.
- Reloaded admin settings and confirmed the custom label persisted in the settings UI.
- Reloaded `/admin/articles`.
- Header still showed `Kategorija`, not `TEST LABEL`.
- Repeated the same check with `/admin/admin-settings -> Articles -> Table Columns` visible on one page and the live `/admin/articles` page visible on the other.
- The saved admin-side label still did not propagate to the live Articles header.

Current interpretation:
- The display-label edit appears to save in admin settings.
- The articles list header does not consume the saved custom label.

### 13. Table Columns - Filterable / Sortable

Plan ref:
- `13.2`

Status: `PASS`

Result:
- Unchecked `category_name` -> `Filterable` in `Table Columns`.
- Reloaded `/admin/articles`.
- Category filter icon disappeared while other filter icons remained.
- Re-checked it and confirmed the filter icon returned.

Plan ref:
- `13.3`

Status: `FAIL`

Result:
- With the `Articles -> Table Columns` form open, `category_name -> Sortable` was checked through direct inspection of the row state.
- On the live `/admin/articles` page, the corresponding header still had no sort affordance:
  - header cells had `cursor: auto`
  - no `aria-sort`
  - no clickable sort control was exposed in the header
- Attempting to toggle `category_name -> Sortable` on in the admin form did not stick.
- The checkbox could flip briefly during interaction, then reverted back off within about a second.
- Reloading `/admin/articles` after that produced no visible sort capability in the live table.

Current interpretation:
- For the tested column, the `Sortable` toggle state itself does not reliably persist.
- Even when the admin form is used directly, the live Articles table does not expose a working sortable UI for that column.

### 10. Form Fields Configuration

Status: `FAIL`

Result:
- In `Admin Settings -> Articles -> Form Fields`, the Create checklist showed every tested field unchecked.
- The real `Add Articles` form still rendered at least:
  - `Canonical name`
  - `Normalized key`
  - `Subcategory`
  - `Link`
  - `Manufacturer`
- In the Edit checklist, only `canonical_name`, `subcategory_id`, `manufacturer_id`, `link`, and `normalized_key` were checked.
- The real `Edit Articles` modal rendered:
  - `Manufacturer`
  - `Canonical name`
  - `Subcategory`
  - `Link`
  - `Normalized key`
- As a stronger toggle check, `Edit -> link` was unchecked in admin settings, then the edit modal was closed and reopened.
- The `Link` field still appeared in the live edit modal after reopen.

Current interpretation:
- The Articles Add/Edit forms are not honoring the Form Fields checklist as expected.
- There is also a mapping mismatch between checklist keys and live form labels/inputs.

### 14. List Behavior - Form Display Mode

Status: `PASS`

Result:
- Baseline `Form display` was `Modal`.
- After switching it to `Inline`, the real `/admin/articles` Add flow stopped opening a modal and instead rendered the add form inline in the page body.
- After restoring `Form display` to `Modal`, reloading `/admin/articles`, and clicking Add again, the `Add Articles` modal overlay returned.

### 15. List Behavior - Disallowed Action Mode

Status: `INCONCLUSIVE / NO ADMIN-PAGE EFFECT`

Result:
- Set `Disallowed action mode` to `Hide`.
- Combined this with a restrictive Add gate during the Action Gates test.
- On the live `/admin/articles` page, the Add button still remained visible and clickable.

Current interpretation:
- On the admin Articles route, the disallowed-action presentation mode did not produce an observable effect in the tested gated scenario.

### 16. List Behavior - Action Gates

Status: `NO ADMIN-PAGE EFFECT`

Result:
- Set `Add` gate to `Expenses: power user`.
- Reloaded `/admin/articles`.
- The Add button still remained visible and clickable.
- Clicking Add still opened the `Add Articles` modal.
- While restoring this gate test, the settings UI briefly showed `Failed to save view options` before the state was corrected.

Current interpretation:
- Expenses-domain action gates are not currently affecting the admin-native `/admin/articles` page.
- This aligns with the admin-route CRUD fix made during this session, but it also means the List Behavior gate controls are not meaningfully testable against the admin Articles page in the same way the plan originally assumed.

### 17. Lock Enforcement via User Settings

Status: `FAIL`

Result:
- Set `Filtering` to `Locked On` for Articles in Admin Settings.
- The Admin Settings summary for Articles updated to show `1 locks` and `Filtering = Locked On`.
- Navigated to `/admin/user-settings`.
- In the Articles section, `Filtering` still appeared as `Inherit | Inherit` in the user-settings summary instead of reflecting the admin lock state.
- For the column-lock path, set `category_name` to `Locked On` in `Admin Settings -> Articles -> Table Columns`.
- The Admin Settings table-column policy updated to `1 locks` and showed the row label as `TEST LABEL`.
- Opened `User Settings -> Articles -> Table Columns`.
- The user-settings table-column policy still showed `0 locks`, the row label had reverted to `Category name`, and the row-level controls remained editable (`disabled: false` on the rendered buttons/inputs).
- Returned to `/admin/articles` and confirmed the live table still showed the category column header as `Kategorija`.
- Restored the temporary admin column lock back to `Inherit` afterward.

Current interpretation:
- The admin-side filtering lock did not propagate into the User Settings presentation for Articles as expected.
- Because the user-settings Articles summary did not reflect the lock, the planned “non-editable toggle” confirmation could not be observed.
- The same propagation failure affects Table Columns lock state as well: admin column locks and admin column labels are not being reflected in the user-settings Table Columns editor.

### R. Add/Edit/Delete Disabled Regression

Status: `RESOLVED`

Observed regression:
- During verification, `/admin/articles` rendered `Add`, `Edit`, and `Delete` controls in a disabled state even when the basic display toggles were enabled.
- The row `Actions` menu still rendered, which helped show this was not just a blanket hidden-controls setting.

Root cause:
- The shared list component was still evaluating Expenses-domain `action-gates` on the admin route.
- `Articles` currently resolves with `:disallowed-action-mode :disable` and `:action-gates` for `:add`, `:edit`, and `:delete` set to `:expenses/articles.manage`.
- On `/admin/articles`, the page is admin-native and does not carry an Expenses membership role in `[:session :membership-role]`, so the gate check resolved false and disabled all three actions.
- This came from the shared list-view gate path in [list.cljs](/Users/enes/Projects/single-tenant-template/src/app/template/frontend/components/list.cljs#L59).

Fix applied:
- Admin routes now bypass Expenses-domain action-gate checks inside the shared list-view gate resolver.
- Added a focused regression test in [list_vector_mode_dom_test.cljs](/Users/enes/Projects/single-tenant-template/test/app/template/frontend/components/list_vector_mode_dom_test.cljs#L38).

Post-fix verification:
- Reloaded the real `/admin/articles` page.
- Confirmed `btn-add-articles`, row `btn-edit-*`, and row `btn-delete-*` were all `disabled: false`.
- Clicked `Add` and confirmed the `Add Articles` modal opened.
- Clicked a row `Edit` button and confirmed the `Edit Articles` modal opened.

## Open Findings

1. Plan task `12.2` appears broken.
   - Custom display label persists in admin settings but does not affect the `/admin/articles` table header.

2. Plan task `13.3` appears broken.
   - The `Sortable` toggle for `category_name` did not reliably hold its state in the admin form, and the live articles table still exposed no sort affordance.

3. Plan task `9` appears broken.
   - Changing `Rows per page` in admin settings did not change the live `/admin/articles` row count.

4. Plan task `10` appears broken.
   - The Articles Add/Edit forms did not honor the Form Fields checklist, including after a direct field-toggle retest.

5. Plan task `17` appears broken.
   - `Filtering = Locked On` did not propagate into the Articles section on `/admin/user-settings`, and a temporary `category_name = Locked On` column lock also failed to propagate into `User Settings -> Articles -> Table Columns`.

6. Plan tasks `15` / `16` do not currently produce an observable admin-page effect.
   - The tested Add gate plus `Hide` mode still left the Add action visible and clickable on `/admin/articles`.

7. The display-label cleanup itself appears sticky.
   - The `category_name` custom label remained saved in admin settings during follow-up cleanup attempts.

8. Admin-default verification can be misleading when local `ui-entity-prefs` overrides exist.
   - This is not a product failure by itself, but it is a testing hazard and should be noted for future runs.

## Not Yet Executed

Pending plan areas:
- None. The remaining work is analysis of the observed failures rather than unexecuted plan tasks.

## Cleanup Status

Current cleanup status:
- Temporary `View Options` changes from completed tests were restored.
- The temporary `Filtering = Locked On` lock was reverted again after the user-settings propagation check.
- The temporary `category_name = Locked On` column lock used for `17.2` was also reverted back to `Inherit`.
- The `category_name` display-label cleanup is still incomplete because the saved `TEST LABEL` value remained sticky in admin settings during follow-up checks.
- Final restoration check:
  - `Admin Settings -> Articles -> Table Columns` is back to `0 locks`.
  - `/admin/articles` still renders the category column as `Kategorija`.
- Browser-local `ui-entity-prefs` were restored after the latest clean-state verification.
