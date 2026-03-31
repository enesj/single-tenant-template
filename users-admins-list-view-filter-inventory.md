# Admin and tenant list-view filter inventory

Date: 2026-03-30

## Best practices for repeating this audit

- Test **one filter at a time**. Do not stack multiple active filters on the same page. Earlier combined-filter passes produced false positives.
- Start each column test from a **clean page state**:
  - reload the page before testing the next column, or
  - fully clear the active filter and verify the baseline row set is back before continuing.
- Prefer testing with a **visible first-page sample value** taken from the current table contents, then record that sample in the notes when useful.
- Treat these outcomes as **different buckets** and document them separately:
  - `Works`
  - `Does not work`
  - `No filter affordance in current UI`
  - `Untestable from current empty/blank sampled state`
  - `Needs targeted probe`
- For **text filters**, verify both of these before calling the filter working:
  - the visible row set actually changes, and
  - the remaining visible values still match the tested sample.
- For **date, numeric, confidence, enum/select, and other non-text controls**, prefer a targeted manual probe instead of forcing them through the generic text-input path.
- For **date filters specifically**, reuse the shared date-picker structure directly when probing them. The same picker UI appears across these pages, so treating it as a known shared control is more reliable than handling each date column as a one-off mystery.
- Keep **admin-space** and **tenant-space** testing in separate live sessions/tabs. Tenant-space testing is much easier once you have a verified owner session already active.
- If tenant auth expires, restore it before continuing; otherwise tenant pages can silently redirect or produce misleading results.
- When a page’s first visible rows contain blank values for a column, mark it as **untestable from sampled state** instead of guessing from another filter flow.
- When rerunning pages after a methodology change, explicitly note that the new one-filter-at-a-time findings **supersede** earlier combined-filter observations.
- Update this document **in batches while testing**, not only at the end, so fresh verified results do not drift away from the report.

Scope:

- Canonical component: `src/app/template/frontend/components/list.cljs`
- Target scope: all admin-space and tenant-space pages that render the canonical list-view, either directly or through a wrapper component
- Method: live browser verification against the running app via Chrome DevTools MCP
- No code changes were made to fix issues; this file is inventory only

## Full scoped page inventory

### Admin-space pages

Direct canonical list-view pages:

| Source file | Route |
| --- | --- |
| `src/app/admin/frontend/pages/tenants.cljs` | `/admin/tenants` |
| `src/app/admin/frontend/pages/domain/expenses/articles.cljs` | `/admin/articles` |
| `src/app/admin/frontend/pages/domain/expenses/article_aliases.cljs` | `/admin/article-aliases` |
| `src/app/admin/frontend/pages/domain/expenses/suppliers.cljs` | `/admin/suppliers` |
| `src/app/admin/frontend/pages/domain/expenses/stores.cljs` | `/admin/stores` |
| `src/app/admin/frontend/pages/domain/expenses/supplier_aliases.cljs` | `/admin/supplier-aliases` |
| `src/app/admin/frontend/pages/domain/expenses/store_aliases.cljs` | `/admin/store-aliases` |
| `src/app/admin/frontend/pages/domain/expenses/categories.cljs` | `/admin/categories` |
| `src/app/admin/frontend/pages/domain/expenses/cities.cljs` | `/admin/cities` |
| `src/app/admin/frontend/pages/domain/expenses/countries.cljs` | `/admin/countries` |
| `src/app/admin/frontend/pages/domain/expenses/subcategories.cljs` | `/admin/subcategories` |
| `src/app/admin/frontend/pages/domain/expenses/manufacturers.cljs` | `/admin/manufacturers` |
| `src/app/admin/frontend/pages/domain/expenses/unmapped_aliases.cljs` | `/admin/unmapped-aliases` |

Wrapped canonical list-view pages:

| Source file | Route | Notes |
| --- | --- | --- |
| `src/app/admin/frontend/pages/users.cljs` | `/admin/users` | Uses generic admin entity page wrapper that renders the shared list-view |
| `src/app/admin/frontend/pages/admins.cljs` | `/admin/admins` | Uses generic admin entity page wrapper that renders the shared list-view |

### Tenant-space pages

Tenant-facing routes are presented in their actual user-facing form as `/t/<tenant-slug>/...`.

| Source file | Route |
| --- | --- |
| `src/app/template/frontend/pages/tenant_members.cljs` | `/t/<tenant-slug>/tenant/members` |
| `src/app/domain/frontend/expenses/pages/user/unmapped_items.cljs` | `/t/<tenant-slug>/unmapped-items` |
| `src/app/domain/frontend/expenses/pages/user/expenses_list.cljs` | `/t/<tenant-slug>/expenses/list` |
| `src/app/domain/frontend/expenses/pages/user/receipts_list.cljs` | `/t/<tenant-slug>/receipts` |
| `src/app/domain/frontend/expenses/pages/user/suppliers.cljs` | `/t/<tenant-slug>/suppliers` |
| `src/app/domain/frontend/expenses/pages/user/payers.cljs` | `/t/<tenant-slug>/payers` |
| `src/app/domain/frontend/expenses/pages/user/stores.cljs` | `/t/<tenant-slug>/stores` |
| `src/app/domain/frontend/expenses/pages/user/store_aliases.cljs` | `/t/<tenant-slug>/store-aliases` |
| `src/app/domain/frontend/expenses/pages/user/expense_items.cljs` | `/t/<tenant-slug>/expense-items` |
| `src/app/domain/frontend/expenses/pages/user/articles.cljs` | `/t/<tenant-slug>/articles` |
| `src/app/domain/frontend/expenses/pages/user/manufacturers.cljs` | `/t/<tenant-slug>/manufacturers` |
| `src/app/domain/frontend/expenses/pages/user/categories.cljs` | `/t/<tenant-slug>/categories` |
| `src/app/domain/frontend/expenses/pages/user/expense_categories.cljs` | `/t/<tenant-slug>/expense-categories` |
| `src/app/domain/frontend/expenses/pages/user/cities.cljs` | `/t/<tenant-slug>/cities` |
| `src/app/domain/frontend/expenses/pages/user/subcategories.cljs` | `/t/<tenant-slug>/subcategories` |
| `src/app/domain/frontend/expenses/pages/user/payer_types.cljs` | `/t/<tenant-slug>/payer-types` |
| `src/app/domain/frontend/expenses/pages/user/article_aliases.cljs` | `/t/<tenant-slug>/article-aliases` |
| `src/app/domain/frontend/expenses/pages/user/supplier_aliases.cljs` | `/t/<tenant-slug>/supplier-aliases` |

## Summary

- Total pages in scoped inventory: `33`
- Pages with current live findings documented so far: `33 / 33`
- Audited so far:
  - Note: where pages were re-tested after the methodology reset, the one-filter-at-a-time results below supersede any earlier combined-filter observations.
  - `/admin/users`: all currently available filter controls tested; none behaved correctly end-to-end.
  - `/admin/admins`: `Email`, `Full name`, `Role`, and `Status` work; `Last login at` does not.
  - `/admin/tenants`: no visible column filter controls in the current UI.
  - `/admin/articles`: one-by-one rerun confirms `Artikal`, `Proizvođač`, `Kategorija`, and `Potkategorija` work; `Jedinica` does not.
  - `/admin/article-aliases`: all visible text filters still work one-by-one; `Kreirano` still needs a targeted date-picker probe.
  - `/admin/suppliers`: `Dobavljač` works; `Normalizovani ključ` does not; `Kreirano` still needs a targeted date-picker probe.
  - `/admin/stores`: only `Prodavnica` works in the fresh solo pass; `Dobavljač`, `Normalizovani ključ`, `Adresa`, and `Grad` do not.
  - `/admin/supplier-aliases`: `Originalna oznaka` works; `Dobavljač` and `Normalizovano` do not.
  - `/admin/store-aliases`: `Originalna oznaka`, `Dobavljač`, and `Trgovina` work; `Pouzdanost` and `Kreirano` still need targeted probes.
  - `/admin/categories`: `Kategorija` works; `Opis` does not; `Kreirano` still needs a targeted date-picker probe.
  - `/admin/cities`: only `Mjesto` works in the fresh solo pass; `Normalizovani ključ`, `Poštanski broj`, and `Država` do not.
  - `/admin/countries`: `Država` and `Kod` both work one-by-one.
  - `/admin/subcategories`: `Potkategorija` and `Kategorija` work; `Opis` was untestable from the first visible row; `Kreirano` still needs a targeted date-picker probe.
  - `/admin/manufacturers`: `Proizvođač` works; `Normalizovani ključ` and `Kreirano` still need targeted/manual probes.
  - `/admin/unmapped-aliases`: `Dobavljač` and `Originalna oznaka` work; `Jedinica` and `Ponavljanja` do not.
  - `/t/jakic-enes-test/articles`: all visible text filters (`Artikal`, `Proizvođač`, `Kategorija`, `Potkategorija`) fail one-by-one.
  - `/t/jakic-enes-test/payers`: `Naziv`, `Vrsta platitelja`, and `Zadano` all work one-by-one.
  - `/t/jakic-enes-test/payer-types`: `Naziv` and `Zadano` both work one-by-one.
  - `/t/jakic-enes-test/suppliers`: visible text filters were untestable from the current empty first-page state; `Kreirano` still needs a targeted date-picker probe.
  - `/t/jakic-enes-test/stores`: visible text filters were untestable from the current empty first-page state; `Kreirano` still needs a targeted date-picker probe.
  - `/t/jakic-enes-test/store-aliases`: visible text filters (`Originalna oznaka`, `Dobavljač`, `Trgovina`) all fail one-by-one; `Pouzdanost` and `Kreirano` still need targeted probes.
  - `/t/jakic-enes-test/categories`: both visible text filters (`Kategorija`, `Opis`) fail one-by-one.
  - `/t/jakic-enes-test/subcategories`: `Potkategorija` and `Kategorija` fail one-by-one; `Opis` is untestable from the first visible rows; `Kreirano` still needs a targeted date-picker probe.
  - `/t/jakic-enes-test/cities`: all visible text filters (`Mjesto`, `Normalizovani ključ`, `Poštanski broj`, `Država`) fail one-by-one; `Kreirano` still needs a targeted probe.
  - `/t/jakic-enes-test/manufacturers`: visible text filters (`Proizvođač`, `Normalizovani ključ`) fail one-by-one; `Kreirano` still needs a targeted date-picker probe.
  - `/t/jakic-enes-test/article-aliases`: visible text filters (`Originalna oznaka`, `Dobavljač`, `Artikal`, `Normalizovano`) fail one-by-one; `Pouzdanost` and `Kreirano` still need targeted probes.
  - `/t/jakic-enes-test/supplier-aliases`: all visible text filters (`Dobavljač`, `Originalna oznaka`, `Normalizovano`) fail one-by-one.
  - `/t/jakic-enes-test/expense-categories`: `Kategorija troška` fails one-by-one; `Kreirano` still needs a targeted date-picker probe.
  - `/t/jakic-enes-test/tenant/members`: current UI exposes no visible column filter controls.
  - `/t/jakic-enes-test/expense-items`: `Artikal` and `Originalna oznaka` fail in the fresh solo pass; date/numeric/unit controls still need targeted probes.
  - `/t/jakic-enes-test/unmapped-items`: `Dobavljač` and `Originalna oznaka` fail in the fresh solo pass; `Jedinica` and `Ponavljanja` still need targeted follow-up.
  - `/t/jakic-enes-test/expenses/list`: `Dobavljač` fails in the fresh solo pass; `Napomene` has no filter affordance; the other visible controls still need targeted probes.
  - `/t/jakic-enes-test/receipts`: `Originalni naziv datoteke` fails in the fresh solo pass; `Ukupno` has no filter affordance; the other visible controls still need targeted probes.
- All scoped pages now have live findings recorded below. Some individual controls remain marked `Needs targeted probe` where the current UI exposed a non-text or otherwise nontrivial control that was not conclusively classifiable via the clean one-filter-at-a-time text path.

## `/admin/users`

### Users visible columns

| Column | Filter control visible? | Result |
| --- | ---: | --- |
| `Id` | No | No filter affordance in current UI |
| `Email` | No | No filter affordance in current UI |
| `Full name` | No | No filter affordance in current UI |
| `Status` | Yes | **Does not work** |
| `Auth provider` | No | No filter affordance in current UI |
| `Created at` (`Kreirano`) | Yes | **Does not work** |
| `Updated at` (`Ažurirano`) | Yes | **Does not work** |

### Users notes

- Current page showed `55 records`.
- The currently rendered list did **not** show `last-login-at`, even though `src/app/admin/frontend/config/table-columns.edn` lists it as filterable for `:users`.

### Users filter results

#### Users `Status`

- Tested by selecting `Inactive` from the status filter dropdown.
- Observed result:
  - active filter chip changed to `Status: Inactive`
  - live error rendered on the page
  - record count stayed at `55 records`
  - visible rows on page 1 stayed `ACTIVE`
- Observed failure:
  - request failed with `500 Internal Server Error`
  - request URI included `/admin/api/users?status[value]=inactive&status[label]=Inactive...`
- Verdict: **does not work**

#### Users `Created at` (`Kreirano`)

- Tested by selecting range `2026-03-28` → `2026-03-29`.
- Observed result:
  - summary updated to `Filtering 2026-03-28 to 2026-03-29.`
  - active filter chip showed `Created at: 2026-03-28 - 2026-03-29`
  - record count stayed at `55 records`
  - first-page rows still included dates outside the selected range (`Mar 27`, `Mar 21`, `Mar 20`, etc.)
- Observed failure:
  - request failed with `500 Internal Server Error`
  - request URI included `/admin/api/users?...created-at[from]=...&created-at[to]=...`
- Verdict: **does not work**

#### Users `Updated at` (`Ažurirano`)

- Tested by selecting range `2026-03-28` → `2026-03-29`.
- Observed result:
  - summary updated to `Filtering 2026-03-28 to 2026-03-29.`
  - active filter chip showed `Updated at: 2026-03-28 - 2026-03-29`
  - record count stayed at `55 records`
  - first-page rows still included dates outside the selected range (`Mar 27`, `Mar 21`, `Mar 20`, etc.)
- Observed failure:
  - no visible `500` during this specific updated-at test
  - filter state changed, but the table output did not narrow
- Verdict: **does not work**

## `/admin/admins`

### Admins visible columns

| Column | Filter control visible? | Result |
| --- | ---: | --- |
| `Email` | Yes | **Works** |
| `Full name` | Yes | **Works** |
| `Role` | Yes | **Works** |
| `Status` | Yes | **Works** |
| `Last login at` | Yes | **Does not work** |
| `Created at` (`Kreirano`) | No | No filter affordance in current UI |
| `Updated at` (`Ažurirano`) | No | No filter affordance in current UI |

### Admins notes

- Current page baseline showed `2 records`.
- `Created at` and `Updated at` are visible on the page but do **not** expose filter buttons in the current UI.

### Admins filter results

#### Admins `Email`

- Tested with text input `enes.jakic@gmail.com`.
- Observed result:
  - active filter chip showed `Email: enes.jakic@gmail.com`
  - record count changed from `2 records` to `1 record`
  - visible table narrowed to the matching admin row
- Verdict: **works**

#### Admins `Full name`

- Tested with text input `Enes Jakic`.
- Observed result:
  - active filter chip showed `Full name: Enes Jakic`
  - record count changed from `2 records` to `1 record`
  - visible table narrowed to the matching admin row
- Verdict: **works**

#### Admins `Role`

- Tested by selecting `Owner`.
- Observed result:
  - active filter chip showed `Role: Owner`
  - record count changed from `2 records` to `1 record`
  - visible table narrowed to the owner row
- Verdict: **works**

#### Admins `Status`

- Tested by selecting `Suspended`.
- Observed result:
  - active filter chip showed `Status: Suspended`
  - record count changed from `2 records` to `0 records`
  - table rows disappeared as expected
- Verdict: **works**

#### Admins `Last login at`

- Tested by selecting range `2026-03-12` → `2026-03-13`.
- Expected from visible data:
  - one admin row had visible last login on `Mar 12`
- Observed result:
  - active filter chip showed `Last login at: 2026-03-12 - 2026-03-13`
  - UI reported `Found 0 matching items`
  - table changed to `0 records`
- Verdict: **does not work**

## Current-state verdict

### `/admin/users` verdict

- Working filters: **0**
- Non-working visible filters: `Status`, `Created at`, `Updated at`
- Visible columns without filter affordance: `Id`, `Email`, `Full name`, `Auth provider`

### `/admin/admins` verdict

- Working filters: `Email`, `Full name`, `Role`, `Status`
- Non-working visible filters: `Last login at`
- Visible columns without filter affordance: `Created at`, `Updated at`

## `/admin/tenants`

### Tenants visible columns

- Current page showed `66 records`.
- The shared list-view is present, but the current UI exposes **no visible column filter controls**.

### Tenants verdict

- Working filters: none visible
- Non-working filters: none visible
- Visible columns without filter affordance: all currently rendered tenant columns

## `/admin/articles`

### Articles visible columns

| Column | Filter control visible? | Result |
| --- | ---: | --- |
| `Artikal` | Yes | **Works** |
| `Proizvođač` | Yes | **Works** |
| `Kategorija` | Yes | **Works** |
| `Potkategorija` | Yes | **Works** |
| `Jedinica` | Yes | **Does not work** |

### Articles notes

- Current page showed `297 records`.
- This section was re-tested **one filter at a time** with a clean reset between runs, after the earlier batch-style pass produced at least one false positive.
- `Proizvođač` works when tested with a non-blank sampled value (`Bayer`).
- `Kategorija` and `Potkategorija` use a **contains-text** match, so values like `Pića` also match `Pakovana hrana i pića`, and `Voće` also matches `Suho voće`.
- `Jedinica` does not narrow the table: testing with `kg` left the page at `297 records` and visible `kom` rows remained on page 1.

### Articles filter results

#### Articles `Artikal`

- Tested with text input `Ananas CJ`.
- Observed result:
  - table narrowed to the matching article row
  - visible result set collapsed to the expected article
- Verdict: **works**

#### Articles `Proizvođač`

- Tested with text input `Bayer`.
- Observed result:
  - record count narrowed to `1`
  - visible result set collapsed to `ASPIRIN PLUS C EFF 10`
- Verdict: **works**

#### Articles `Kategorija`

- Tested with text input `Pića`.
- Observed result:
  - record count narrowed from `297` to `116`
  - visible rows all matched categories containing `Pića`
- Verdict: **works**

#### Articles `Potkategorija`

- Tested with text input `Voće`.
- Observed result:
  - record count narrowed from `297` to `4`
  - visible rows all matched subcategories containing `Voće`
- Verdict: **works**

#### Articles `Jedinica`

- Tested with text input `kg`.
- Observed result:
  - record count stayed at `297`
  - visible page-1 rows still included many `kom` values
  - the filter popover accepted input, but the table did not narrow
- Verdict: **does not work**

## `/admin/article-aliases`

### Article aliases visible columns

| Column | Filter control visible? | Result |
| --- | ---: | --- |
| `Originalna oznaka` | Yes | **Works** |
| `Dobavljač` | Yes | **Works** |
| `Artikal` | Yes | **Works** |
| `Normalizovano` | Yes | **Works** |
| `Jedinica` | Yes | **Works** |
| `Kreirano` | Yes | **Needs targeted probe** |

### Article aliases notes

- Current page showed `536 records`.
- Fresh one-by-one rerun confirmed that every visible text filter on this page still narrows correctly.
- `Kreirano` still needs a targeted date-picker probe.

## `/admin/suppliers`

### Suppliers visible columns

| Column | Filter control visible? | Result |
| --- | ---: | --- |
| `Dobavljač` | Yes | **Works** |
| `Normalizovani ključ` | Yes | **Does not work** |
| `Prodavnice` | No | No filter affordance in current UI |
| `Kreirano` | Yes | **Needs targeted probe** |

### Suppliers notes

- Current page showed `60 records`.
- Fresh one-by-one rerun showed `Dobavljač` narrowing correctly with `AFRODITA`.
- `Normalizovani ključ` did **not** narrow the table when tested alone with `afrodita`.

## `/admin/stores`

### Stores visible columns

| Column | Filter control visible? | Result |
| --- | ---: | --- |
| `Prodavnica` | Yes | **Works** |
| `Dobavljač` | Yes | **Does not work** |
| `Normalizovani ključ` | Yes | **Does not work** |
| `Adresa` | Yes | **Does not work** |
| `Grad` | Yes | **Does not work** |
| `Kreirano` | Yes | **Needs targeted probe** |

### Stores notes

- Current page showed `75 records`.
- Fresh one-by-one rerun showed only `Prodavnica` narrowing correctly.
- `Dobavljač`, `Normalizovani ključ`, `Adresa`, and `Grad` each accepted a solo text value but left the visible row set unchanged.

## `/admin/supplier-aliases`

### Supplier aliases visible columns

| Column | Filter control visible? | Result |
| --- | ---: | --- |
| `Dobavljač` | Yes | **Does not work** |
| `Originalna oznaka` | Yes | **Works** |
| `Normalizovano` | Yes | **Does not work** |

### Supplier aliases notes

- Current page showed `60 records`.
- Fresh one-by-one rerun confirmed `Dobavljač` still does not narrow.
- `Originalna oznaka` works.
- `Normalizovano` also fails in the fresh solo pass: testing with `afrodita` left the visible row set unchanged.

## `/admin/store-aliases`

### Store aliases visible columns

| Column | Filter control visible? | Result |
| --- | ---: | --- |
| `Originalna oznaka` | Yes | **Works** |
| `Dobavljač` | Yes | **Works** |
| `Trgovina` | Yes | **Works** |
| `Pouzdanost` | Yes | **Needs targeted probe** |
| `Kreirano` | Yes | **Needs targeted probe** |

### Store aliases notes

- Current page showed `81 records`.
- Fresh one-by-one rerun confirmed the visible text filters `Originalna oznaka`, `Dobavljač`, and `Trgovina` still work.
- `Pouzdanost` and `Kreirano` still need dedicated non-text probes.

## `/admin/categories`

### Categories visible columns

| Column | Filter control visible? | Result |
| --- | ---: | --- |
| `Kategorija` | Yes | **Works** |
| `Opis` | Yes | **Does not work** |
| `Kreirano` | Yes | **Needs targeted probe** |

### Categories notes

- Current page showed `29 records`.
- Fresh one-by-one rerun showed `Kategorija` narrowing correctly with `Dom i vrt`.
- `Opis` did **not** narrow the visible row set when tested alone with `Uređenje doma, baštenski i vanjski artikli.`

## `/admin/cities`

### Cities visible columns

| Column | Filter control visible? | Result |
| --- | ---: | --- |
| `Mjesto` | Yes | **Works** |
| `Normalizovani ključ` | Yes | **Does not work** |
| `Poštanski broj` | Yes | **Does not work** |
| `Država` | Yes | **Does not work** |
| `Kreirano` | Yes | **Needs targeted probe** |

### Cities notes

- Current page showed `498 records` during the fresh rerun.
- `Mjesto` narrowed correctly with `Aleksandrovac`.
- `Normalizovani ključ`, `Poštanski broj`, and `Država` each failed in the solo pass.

## `/admin/countries`

### Countries visible columns

| Column | Filter control visible? | Result |
| --- | ---: | --- |
| `Država` | Yes | **Works** |
| `Kod` | Yes | **Works** |

### Countries notes

- Fresh one-by-one rerun confirmed both visible text filters narrow correctly.

## `/admin/subcategories`

### Subcategories visible columns

| Column | Filter control visible? | Result |
| --- | ---: | --- |
| `Potkategorija` | Yes | **Works** |
| `Kategorija` | Yes | **Works** |
| `Opis` | Yes | **Untestable from sampled first row** |
| `Kreirano` | Yes | **Needs targeted probe** |

### Subcategories notes

- Fresh one-by-one rerun confirmed `Potkategorija` and `Kategorija` narrow correctly.
- The first visible sampled row still had a blank `Opis`, so that filter remains untestable from the current top-of-page sample.

## `/admin/manufacturers`

### Manufacturers visible columns

| Column | Filter control visible? | Result |
| --- | ---: | --- |
| `Proizvođač` | Yes | **Works** |
| `Normalizovani ključ` | Yes | **Needs targeted/manual probe** |
| `Kreirano` | Yes | **Needs targeted probe** |

### Manufacturers notes

- Fresh one-by-one rerun confirmed `Proizvođač` narrows correctly with `Amisu`.
- `Normalizovani ključ` did not complete cleanly under the generic text script and still needs a dedicated follow-up probe.

## `/admin/unmapped-aliases`

### Unmapped aliases visible columns

| Column | Filter control visible? | Result |
| --- | ---: | --- |
| `Dobavljač` | Yes | **Works** |
| `Originalna oznaka` | Yes | **Works** |
| `Jedinica` | Yes | **Does not work** |
| `Ponavljanja` | Yes | **Does not work** |

### Unmapped aliases notes

- Fresh one-by-one rerun confirmed `Dobavljač` and `Originalna oznaka` narrow correctly.
- `Jedinica` and `Ponavljanja` both failed in the solo pass: their active filter state changed, but the visible rows did not narrow.

## `/t/jakic-enes-test/articles`

### Tenant articles visible columns

| Column | Filter control visible? | Result |
| --- | ---: | --- |
| `Artikal` | Yes | **Does not work** |
| `Proizvođač` | Yes | **Does not work** |
| `Kategorija` | Yes | **Does not work** |
| `Potkategorija` | Yes | **Does not work** |

### Tenant articles notes

- Fresh one-by-one rerun was performed as an authenticated owner in the `jakic-enes-test` workspace.
- All four visible text filters updated their active filter chips, but none of them changed the visible row set.

## `/t/jakic-enes-test/suppliers`

### Tenant suppliers visible columns

| Column | Filter control visible? | Result |
| --- | ---: | --- |
| `Dobavljač` | Yes | **Untestable from current empty first-page state** |
| `Normalizovani ključ` | Yes | **Untestable from current empty first-page state** |
| `Kreirano` | Yes | **Needs targeted probe** |

### Tenant suppliers notes

- The fresh one-by-one rerun reached this page successfully in the owner workspace, but the current first-page state did not expose a non-empty sample value for the visible text filters.

## `/t/jakic-enes-test/stores`

### Tenant stores visible columns

| Column | Filter control visible? | Result |
| --- | ---: | --- |
| `Prodavnica` | Yes | **Untestable from current empty first-page state** |
| `Dobavljač` | Yes | **Untestable from current empty first-page state** |
| `Normalizovani ključ` | Yes | **Untestable from current empty first-page state** |
| `Adresa` | Yes | **Untestable from current empty first-page state** |
| `Grad` | Yes | **Untestable from current empty first-page state** |
| `Kreirano` | Yes | **Needs targeted probe** |

### Tenant stores notes

- The fresh one-by-one rerun reached this page successfully in the owner workspace, but the current first-page state did not expose non-empty sample values for the visible text filters.

## `/t/jakic-enes-test/payers`

### Tenant payers visible columns

| Column | Filter control visible? | Result |
| --- | ---: | --- |
| `Naziv` | Yes | **Works** |
| `Vrsta platitelja` | Yes | **Works** |
| `Zadano` | Yes | **Works** |
| `Email` | No | No filter affordance in current UI |

### Tenant payers notes

- Fresh one-by-one rerun as an authenticated owner in the `jakic-enes-test` workspace.
- `Naziv` narrowed correctly with `Enes Jakić`.
- `Vrsta platitelja` narrowed to zero rows with `admin`, confirming the filter is applied.
- `Zadano` narrowed correctly with `true`.

## `/t/jakic-enes-test/payer-types`

### Tenant payer types visible columns

| Column | Filter control visible? | Result |
| --- | ---: | --- |
| `Naziv` | Yes | **Works** |
| `Zadano` | Yes | **Works** |

### Tenant payer types notes

- Fresh one-by-one rerun confirmed `Naziv` narrows correctly with `Kućanstvo`.
- `Zadano` narrows correctly with `true`.

## `/t/jakic-enes-test/store-aliases`

### Tenant store aliases visible columns

| Column | Filter control visible? | Result |
| --- | ---: | --- |
| `Originalna oznaka` | Yes | **Does not work** |
| `Dobavljač` | Yes | **Does not work** |
| `Trgovina` | Yes | **Does not work** |
| `Pouzdanost` | Yes | **Needs targeted probe** |
| `Kreirano` | Yes | **Needs targeted probe** |

### Tenant store aliases notes

- Fresh one-by-one rerun showed the text filter UI accepting values, but the table stayed at `81 records` and the visible rows did not narrow.
- Confirmed broken with example inputs `Ložionička`, `KONZUM`, and `Podružnica`.

## `/t/jakic-enes-test/categories`

### Tenant categories visible columns

| Column | Filter control visible? | Result |
| --- | ---: | --- |
| `Kategorija` | Yes | **Does not work** |
| `Opis` | Yes | **Does not work** |

### Tenant categories notes

- Fresh one-by-one rerun showed `Kategorija` accepting `Dom i vrt` but leaving the page at the original row count.
- `Opis` also failed: the first visible description value yielded `0` rows even though that exact row was present before filtering.

## `/t/jakic-enes-test/subcategories`

### Tenant subcategories visible columns

| Column | Filter control visible? | Result |
| --- | ---: | --- |
| `Potkategorija` | Yes | **Does not work** |
| `Kategorija` | Yes | **Does not work** |
| `Opis` | Yes | **Untestable from sampled first rows** |
| `Kreirano` | Yes | **Needs targeted probe** |

### Tenant subcategories notes

- Fresh one-by-one rerun showed `Potkategorija` failing with `Alati` and `Kategorija` failing with `Željeznarija i alati`.
- The first visible sampled rows had blank `Opis` values, so that column remains untestable from the current top-of-page sample.

## `/t/jakic-enes-test/cities`

### Tenant cities visible columns

| Column | Filter control visible? | Result |
| --- | ---: | --- |
| `Mjesto` | Yes | **Does not work** |
| `Normalizovani ključ` | Yes | **Does not work** |
| `Poštanski broj` | Yes | **Does not work** |
| `Država` | Yes | **Does not work** |
| `Kreirano` | Yes | **Needs targeted probe** |

### Tenant cities notes

- Fresh one-by-one rerun showed all visible text filters failing.
- Examples: `Aleksandrovac` returned `0` rows, `78255` left the page at the same row count, and `Bosnia and Herzegovina` also returned `0` rows even though those values were visible before filtering.

## `/t/jakic-enes-test/manufacturers`

### Tenant manufacturers visible columns

| Column | Filter control visible? | Result |
| --- | ---: | --- |
| `Proizvođač` | Yes | **Does not work** |
| `Normalizovani ključ` | Yes | **Does not work** |
| `Kreirano` | Yes | **Needs targeted probe** |

### Tenant manufacturers notes

- Fresh one-by-one rerun showed both visible text filters failing with exact visible values (`Amisu`, `amisu`).

## `/t/jakic-enes-test/article-aliases`

### Tenant article aliases visible columns

| Column | Filter control visible? | Result |
| --- | ---: | --- |
| `Originalna oznaka` | Yes | **Does not work** |
| `Dobavljač` | Yes | **Does not work** |
| `Artikal` | Yes | **Does not work** |
| `Normalizovano` | Yes | **Does not work** |
| `Pouzdanost` | Yes | **Needs targeted probe** |
| `Kreirano` | Yes | **Needs targeted probe** |

### Tenant article aliases notes

- Fresh one-by-one rerun showed the visible text filters all failing with exact first-row values.
- Example failures: `CIG DUNHIL ESSEN BR`, `KONZUM`, `Cig Dunhill Essence Bronze`, and `cig-dunhil-essen-br` each returned `0` rows.

## `/t/jakic-enes-test/supplier-aliases`

### Tenant supplier aliases visible columns

| Column | Filter control visible? | Result |
| --- | ---: | --- |
| `Dobavljač` | Yes | **Does not work** |
| `Originalna oznaka` | Yes | **Does not work** |
| `Normalizovano` | Yes | **Does not work** |

### Tenant supplier aliases notes

- Fresh one-by-one rerun showed all three visible text filters failing with exact first-row values (`PETROL BH OIL COMPANY`, `PETROL BH OIL COMPANY`, `petrol-bh-oil-company`).

## `/t/jakic-enes-test/expense-categories`

### Tenant expense categories visible columns

| Column | Filter control visible? | Result |
| --- | ---: | --- |
| `Kategorija troška` | Yes | **Does not work** |
| `Kreirano` | Yes | **Needs targeted probe** |

### Tenant expense categories notes

- Fresh one-by-one rerun showed `Kategorija troška` failing with exact visible value `Komunalije`, returning `0` rows.

## `/t/jakic-enes-test/tenant/members`

### Tenant members visible columns

- Current page showed the shared list-view, but the current UI exposes **no visible column filter controls**.

### Tenant members notes

- Visible columns at the time of testing were `Ime`, `E-mail`, `Uloga`, `Članstvo`, `Račun`, and `Datum pristupanja`.
- None of those columns exposed a filter button in the current UI.

## `/t/jakic-enes-test/expense-items`

### Tenant expense items visible columns

| Column | Filter control visible? | Result |
| --- | ---: | --- |
| `Kupljeno` | Yes | **Needs targeted probe** |
| `Artikal` | Yes | **Does not work** |
| `Originalna oznaka` | Yes | **Does not work** |
| `Kol.` | Yes | **Needs targeted probe** |
| `Jedin. cijena` | Yes | **Needs targeted probe** |
| `Ukupno` | Yes | **Needs targeted probe** |
| `Unit` | Yes | **Needs targeted probe** |
| `Kreirano` | Yes | **Needs targeted probe** |

### Tenant expense items notes

- Fresh solo pass confirmed `Artikal` fails: using a visible article value (`Vrećica sa ručkom`) returned `0` rows.
- `Originalna oznaka` also fails: using the visible first-row value `VREĆA VAKUM ZA ODJEĆU HENGER XL 70 x145cm` returned `0` rows.
- `Kupljeno`, numeric fields, `Unit`, and `Kreirano` still need dedicated follow-up probes.

## `/t/jakic-enes-test/unmapped-items`

### Tenant unmapped items visible columns

| Column | Filter control visible? | Result |
| --- | ---: | --- |
| `Dobavljač` | Yes | **Does not work** |
| `Originalna oznaka` | Yes | **Does not work** |
| `Jedinica` | Yes | **Needs targeted probe** |
| `Ponavljanja` | Yes | **Needs targeted probe** |

### Tenant unmapped items notes

- Fresh solo pass confirmed `Dobavljač` fails with visible value `BINGO`.
- `Originalna oznaka` also fails with visible value `COKOLADNA BANANICA 25G STARK`.
- `Jedinica` and `Ponavljanja` still need dedicated follow-up probes.

## `/t/jakic-enes-test/expenses/list`

### Tenant expenses list visible columns

| Column | Filter control visible? | Result |
| --- | ---: | --- |
| `Kupljeno` | Yes | **Needs targeted probe** |
| `Dobavljač` | Yes | **Does not work** |
| `Trgovina` | Yes | **Needs targeted probe** |
| `Kategorija troška` | Yes | **Needs targeted probe** |
| `Platitelj` | Yes | **Needs targeted probe** |
| `Valuta` | Yes | **Needs targeted probe** |
| `Ukupno` | Yes | **Needs targeted probe** |
| `Napomene` | No | No filter affordance in current UI |
| `Kreirano` | Yes | **Needs targeted probe** |
| `Ažurirano` | Yes | **Needs targeted probe** |

### Tenant expenses list notes

- Fresh solo pass confirmed `Dobavljač` fails: using visible value `TROPIC MALOPRODAJA` returned `0` rows.
- `Napomene` is visible but exposes no filter button in the current UI.
- The date, select-like, and other nontrivial controls still need targeted follow-up probes.

## `/t/jakic-enes-test/receipts`

### Tenant receipts visible columns

| Column | Filter control visible? | Result |
| --- | ---: | --- |
| `Originalni naziv datoteke` | Yes | **Does not work** |
| `Status` | Yes | **Needs targeted probe** |
| `Dobavljač` | Yes | **Needs targeted probe** |
| `Datum kupovine` | Yes | **Needs targeted probe** |
| `Ukupno` | No | No filter affordance in current UI |
| `Kreirao/la` | Yes | **Needs targeted probe** |
| `Kreirano` | Yes | **Needs targeted probe** |
| `Ažurirano` | Yes | **Needs targeted probe** |

### Tenant receipts notes

- Fresh solo pass confirmed `Originalni naziv datoteke` fails: using visible value `IMG_4071.jpeg` returned `0` rows.
- `Ukupno` is visible but exposes no filter button in the current UI.
- `Status`, `Dobavljač`, `Datum kupovine`, `Kreirao/la`, `Kreirano`, and `Ažurirano` still need targeted follow-up probes.
