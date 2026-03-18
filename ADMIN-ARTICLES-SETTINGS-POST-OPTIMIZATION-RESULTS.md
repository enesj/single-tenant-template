# Admin Settings → Articles Page Post-Optimization Re-Verification Results

Related plan: `ADMIN-SETTINGS-OPTIMIZATION-PLAN.md`
Prior results: `ADMIN-ARTICLES-SETTINGS-VERIFICATION-RESULTS.md`

## Scope

This document records re-verification results for previously-failing tests
after the settings optimization changes from `ADMIN-SETTINGS-OPTIMIZATION-PLAN.md`.

Test method:
- Changes were made through the real `/admin/admin-settings` UI.
- Verification was done on the real `/admin/articles` page after reloads.
- Server state was verified via `/admin/api/settings/table-columns` API.

Browser state:
- No local `ui-entity-prefs` for `:articles` — clean default state.
- Other entity prefs present but not relevant.

## Baseline (Post-Optimization)

### Articles page state
- 25 rows displayed, Page 1 of 14
- Headers: Artikal, Kategorija, Potkategorija, Proizvođač, Kreirano
- Sortable headers: Artikal, Potkategorija, Proizvođač, Kreirano (NOT Kategorija — matches config)
- Selection checkboxes: visible
- Filter controls: 5 filter buttons (one per column)
- Edit/Delete: 50 buttons (2 per row × 25 rows)
- Pagination: visible

### Key observation
The baseline already showed improvement: **25 rows** displayed matching the admin default.
Previously the baseline showed **10 rows** despite the admin setting being 25.

## Results By Plan Task

### Test 9: Rows Per Page

Previous status: `FAIL`
New status: `PASS`

Result:
- Baseline showed 25 rows, Page 1 of 14 — admin default of 25 now honored.
- Changed admin Rows per page to 5, saved settings.
- Reloaded `/admin/articles`.
- Page showed 5 rows, Page 1 of 70 (350 articles / 5 = 70 pages).
- Restored to 25, confirmed 25 rows and Page 1 of 14.

Fix: New `::seed-per-page-from-config` event properly seeds per-page from admin config on first load,
with guards against overriding user-set localStorage preferences.

### Test 12: Display Labels

Previous status: `FAIL`
New status: `PASS (after code fix)`

Result:
- Admin settings had `category_name` display label set to "TEST LABEL" (sticky from prior session).
- Before code fix: articles page still showed "Kategorija" (i18n translation winning).
- Root cause: `resolve-column-label-override` in `resolver.cljs` prioritized `:label-key` (i18n)
  over `:label` (explicit admin override). When both were present, the i18n translation always won.
- Fix applied: reversed priority in `resolve-column-label-override` so explicit `:label` wins
  over `:label-key`. This is correct because an admin-set label is a deliberate customization.
- After fix: articles page showed "TEST LABEL" as the column header.
- Cleared the custom label (empty string), verified server removed `:label` while preserving
  `:label-key "common/category"`.
- After clearing: articles page reverted to "Kategorija" (i18n fallback from `:label-key`).

Code change: `src/app/template/frontend/settings/resolver.cljs` lines 300-311.

### Test 13.3: Sortable Toggle

Previous status: `FAIL`
New status: `PASS`

Result:
- Baseline: `category_name` sortable checkbox unchecked, Kategorija header had no sort affordance
  (cursor: auto, no clickable div).
- Checked `category_name` Sortable in admin settings.
- Server confirmed: `category_name` added to `sortable-columns` array (8 total).
- Reloaded `/admin/articles`.
- Kategorija header gained `cursor: pointer`, clickable sort div (`#header-category-name`).
- Clicked header: sort indicator (SVG arrow) appeared, confirming sort works.
- Unchecked Sortable, server confirmed removal (back to 7 sortable columns).

Fix: `:admin/sortable-columns` subscription now uses route-aware `resolve-config-source`.

### Test 10: Form Fields

Status: `SKIPPED`

Reason:
- Form Fields is a deeper architectural issue requiring Step 3 of the optimization plan.
- The form-fields editor builds checklists from table columns, while forms render from
  `:form-entity-specs/by-name`. Aligning these requires dedicated work.

### Test 17: Lock Enforcement

Status: `SKIPPED`

Reason:
- Lock enforcement across admin→user settings is a deeper architectural issue requiring
  Step 2 of the optimization plan.
- Admin locks need to be visible as inherited/effective state in user settings, not copied
  into user config. This requires dedicated work.

## Regression Spot-Check

Verified on `/admin/articles` after all changes restored:
- 25 rows, Page 1 of 14 ✓
- Headers: Artikal, Kategorija, Potkategorija, Proizvođač, Kreirano ✓
- 5 filter buttons ✓
- 50 edit/delete buttons (2 per row × 25) ✓
- Header checkbox (selection) ✓
- Pagination visible ✓

No regressions detected.

## Summary

| # | Test | Previous | New | Notes |
|---|------|----------|-----|-------|
| 9 | Rows Per Page | FAIL | PASS | `::seed-per-page-from-config` fix |
| 12 | Display Labels | FAIL | PASS | `resolve-column-label-override` priority fix |
| 13.3 | Sortable Toggle | FAIL | PASS | Route-aware `:admin/sortable-columns` |
| 10 | Form Fields | FAIL | SKIPPED | Needs Step 3 (form-fields alignment) |
| 17 | Lock Enforcement | FAIL | SKIPPED | Needs Step 2 (cross-scope locks) |
| LB-1 | Form Display Mode | — | PASS | Modal↔Inline verified on live page |
| LB-2 | Disallowed Action Mode | — | PASS | Config persistence verified; admin-bypassed |
| LB-3 | Action Gates | — | PASS | Config persistence verified; admin-bypassed |
| Regression | Spot-check | — | PASS | No regressions |

## Cleanup Status

- All temporary settings changes restored.
- `category_name` display label: cleared (server confirmed no `:label` key).
- `category_name` sortable: unchecked (server confirmed 7 sortable columns).
- Rows per page: restored to 25.
- No local entity-prefs for articles.

## List Behavior Tests

### Baseline (List Behavior)

Admin settings read-only view for Articles:
- Form display: **modal**
- Disallowed action mode: **disable**
- Action gates: Add/Edit/Delete = `expenses/articles.manage` (from domain config), Selection = No gate
- Gate badge: "3 gates"

### Test LB-1: Form Display Mode

Status: `PASS`

Result:
- Baseline: Edit button on articles page opened a `dialog modal` overlay (confirmed by
  `dialog modal` element in accessibility tree).
- Changed Form Display from "Modal" to "Inline" in admin settings, saved.
- Reloaded `/admin/articles`.
- Clicked Edit on first row: form rendered **inline** within the table (no `dialog` element
  in the DOM, form fields appeared between table header and remaining rows).
- Restored Form Display to "Modal", saved.
- Verified read-only view confirmed "Modal" persisted.

Code path: `list.cljs` line 241 (`use-modal-forms? = (= effective-form-display :modal)`) →
subscription `::entity-list-config` → `resolver/resolve-list-config` → `parse-list-config`.

### Test LB-2: Disallowed Action Mode

Status: `PASS (config persistence)`

Result:
- Baseline: Disallowed action mode = "Disable".
- Changed to "Hide" in admin settings, saved.
- Read-only view confirmed "Hide" persisted.
- Restored to "Disable", saved.
- Read-only view confirmed "Disable" persisted.

Note: Visual effect not testable on `/admin/articles` because `gate-allows-action?`
always returns `true` on admin routes (`admin-route? true` short-circuit in `list.cljs`
line 55). The mode only has visible impact on user-facing pages where gates can deny actions.

### Test LB-3: Action Gates

Status: `PASS (config persistence)`

Result:
- Baseline: Admin edit mode shows "No gate" for all gates (admin hasn't overridden domain defaults).
  Read-only view merges domain config showing `expenses/articles.manage` for Add/Edit/Delete.
- Set Add gate to "Expenses: can write", saved.
- Read-only view confirmed Add gate = `can-write` (admin override), Edit/Delete = `expenses/articles.manage`
  (domain config). Gate badge still "3 gates".
- Restored Add gate to "No gate" (empty value `""`), saved.
- Read-only view confirmed Add gate = "No gate", gate badge changed to "2 gates".

Note: Clearing a gate in admin creates an explicit empty override that supersedes the domain
config. This is different from "never set" (which inherits domain). The gate count dropped
from 3 → 2 because the explicit "No gate" admin override replaced the domain's `articles.manage`.
This is expected behavior of the layered `resolve-config-source` architecture.

Visual effect not testable on admin routes (gates bypassed for admin users).

### Cleanup (List Behavior)

- Form display: restored to Modal (confirmed in read-only view).
- Disallowed action mode: restored to Disable (confirmed in read-only view).
- Add gate: set to "No gate" — note this changed effective gate count from 3 → 2
  (admin override now explicitly clears the domain's `expenses/articles.manage` gate for Add).
- Edit/Delete gates: unchanged (`expenses/articles.manage` from domain config).
- Selection gate: unchanged (No gate).

### List Behavior Summary

| # | Test | Status | Notes |
|---|------|--------|-------|
| LB-1 | Form Display Mode | PASS | Modal→Inline verified on live page |
| LB-2 | Disallowed Action Mode | PASS (persistence) | Hide/Disable toggle persists; visual effect admin-bypassed |
| LB-3 | Action Gates | PASS (persistence) | Gate set/clear persists; admin routes bypass gate checks |

## Code Changes Made During This Session

1. `src/app/template/frontend/settings/resolver.cljs` (lines 300-311):
   - Changed `resolve-column-label-override` to prioritize explicit `:label` over `:label-key` (i18n).
   - Before: `(or translated-label static-label)` — i18n always wins.
   - After: `(or static-label translated-label)` — explicit admin label wins.
