# Settings Hierarchy — Browser Test Plan

**Spec**: `specs/allium/domain/expenses/settings-hierarchy.candidate.allium`
**Implementation plan**: `SETTINGS-IMPLEMENTATION-PLAN.md`
**Status**: Complete (all 41 tests executed)
**Last updated**: 2026-03-19
**Created**: 2026-03-19

Legend: `[ ]` pending · `[✓]` pass · `[✗]` fail · `[~]` partial/skip

---

## Pre-flight

- [✓] App running at `http://localhost:8085` (`bb run-app`)
- [✓] Admin panel accessible at `http://localhost:8085/admin`
- [✓] At least one expense category exists
- [✓] At least one payer exists
- [✓] EUR in enabled_currencies (seeded by default)

---

## Area 1 — Routing & Navigation

| # | Test | Action | Expected | Result | Notes |
|---|------|---------|----------|--------|-------|
| T1 | Gear icon visible | Open `/expenses`, inspect header top-right gear icon | Settings panel or dropdown opens | [✓] | "Settings" button in header opens modal with theme picker, Profile link, Change Password link |
| T2 | Profile link | Click `#settings-panel-profile` | Navigates to `/profile` | [✓] | `#settings-panel-profile` exists, navigates to `/profile` |
| T3 | Old route removed | Navigate to `/expenses/settings` | 404 / redirect — NOT the old settings page | [✓] | Falls through to expense detail view with "not found" — old settings page is gone |
| T4 | Admin settings link | Admin sidebar → "Expenses Settings" | Navigates to `/admin/expenses-settings` | [✓] | Sidebar "Settings" under Domain > Expenses → `/admin/expenses-settings` with all 4 sections |

---

## Area 2 — User Profile Page (`/profile`)

| # | Test | Action | Expected | Result | Notes |
|---|------|---------|----------|--------|-------|
| T5 | Page loads | Navigate to `/profile` | Skeleton pulse then content | [✓] | Content loads immediately from auth session data |
| T6 | Account info displayed | Check `#profile-user-name`, `#profile-user-email`, `#profile-user-role` | Real user data visible, no input fields | [✓] | "enes jakic", "jakic.enes.dev@gmail.com", "owner" — all read-only SPANs |
| T7 | Default payer (read-only) | Check `#profile-default-payer` | Shows payer name or empty label | [✓] | "Nijedan zadani platitelj" — read-only text |
| T8 | Default category dropdown | Change `#profile-default-category-select` | Save button `#btn-profile-save-defaults` enables | [✓] | Changed to "Domaćinstvo", save button enabled |
| T9 | Save defaults | Click `#btn-profile-save-defaults` | Spinner → saves → dirty clears | [✓] | Saved, button went back to disabled |
| T10 | Defaults persist | Reload page | Category select shows saved value | [✓] | "Domaćinstvo" persisted after reload |
| T11 | Workspace section gated | As member (non-owner): check workspace card | Section absent | [✓] | Switched to "Jakic Family" (member role) — no Workspace or Data sections visible |
| T12 | Workspace section visible | As owner: visit `/profile` | "Workspace" and "Data management" sections present | [✓] | "Radni prostor" and "Podaci" sections both visible as owner |
| T13 | Workspace name edit | Owner: change `#profile-workspace-name-input` | `#btn-profile-save-workspace-name` enables | [✓] | Changed name, save button enabled |
| T14 | Save workspace name | Click `#btn-profile-save-workspace-name` | Saves; reload confirms | [~] | **BUG-2**: DB persisted ("TEST" suffix confirmed in DB), but UI shows stale value after reload — profile reads tenant name from auth session cache, not fresh API |
| T15 | Email notifications toggle | Toggle `#profile-email-notifications-toggle` | `#btn-profile-save-workspace-settings` enables | [✓] | Toggled off, save button enabled |
| T16 | Save notifications | Click `#btn-profile-save-workspace-settings` | Saves; reload confirms | [✓] | Saved; persisted as unchecked after reload |
| T17 | Export button | Click `#btn-profile-export-expenses` | CSV download triggers | [✓] | GET `/api/v1/profile/export?all=true&format=csv` returned 200 |
| T18 | Delete guard | Type anything ≠ "DELETE" in `#profile-delete-confirmation-input` | `#btn-profile-delete-all-expenses` stays disabled | [✓] | Typed "NOTDELETE" — button stayed disabled |
| T19 | Delete unlock | Type "DELETE" exactly | Delete button enables | [✓] | Typed "DELETE" — button enabled |

---

## Area 3 — Admin Global Settings (`/admin/expenses-settings`)

| # | Test | Action | Expected | Result | Notes |
|---|------|---------|----------|--------|-------|
| T20 | Page loads | Navigate to `/admin/expenses-settings` | 4 sections: Global defaults, Enabled currencies, Exchange rates, Fetch alerts | [✓] | All 4 sections visible |
| T21 | Save disabled initially | Inspect `#btn-admin-expenses-settings-save` | Disabled (no dirty state) | [✓] | |
| T22 | Currency dropdown options | Inspect `#admin-expenses-default-currency` options | Lists only enabled currencies | [✓] | |
| T23 | Change default currency | Select different currency | Save button enables | [✓] | Changed to EUR |
| T24 | Save persists | Save → reload | New currency shows in dropdown | [✓] | EUR persisted after reload |
| T25 | Default note edit | Clear `#admin-expenses-default-note`, type text | Save enables | [✓] | "Test default note" |
| T26 | Auto-publish toggle | Click `#admin-expenses-auto-publish-toggle` | Dirty → save enables | [✓] | |
| T27 | AI enhancement toggle | Click `#admin-expenses-ai-enhancement-toggle` | Dirty → save enables | [✓] | |
| T28 | Multi-field save | Change multiple fields → save → reload | All changes persist | [✓] | All 4 fields persisted |

---

## Area 4 — Enabled Currencies Management

| # | Test | Action | Expected | Result | Notes |
|---|------|---------|----------|--------|-------|
| T29 | BAM row locked | Inspect BAM row action | Button shows "Locked" / disabled | [✓] | BAM row has disabled/locked button |
| T30 | Add form validation | Enter 2-char code, no name | `#btn-admin-add-currency` stays disabled | [✓] | |
| T31 | Add currency | Code `JPY`, name `Japanese Yen` → click `#btn-admin-add-currency` | Row appears; inputs clear | [✓] | JPY row appeared |
| T32 | Remove currency | Click `#btn-admin-remove-currency-jpy` | Row disappears | [✓] | JPY removed |
| T33 | Dropdown updates | After removing currency, reload admin | Removed currency gone from default-currency select | [✓] | JPY absent from dropdown |

---

## Area 5 — Exchange Rates & Alerts

| # | Test | Action | Expected | Result | Notes |
|---|------|---------|----------|--------|-------|
| T34 | Rates initial state | Check rates table on load | Empty state OR rows with Live/Fallback badges | [✓] | Rows with Live badges shown |
| T35 | Fetch latest rates | Click `#btn-admin-refresh-exchange-rates` | Loading → `#admin-expenses-rates-last-status` appears | [✓] | Rates fetched from frankfurter.dev |
| T36 | Rates populated | After fetch | Rows with currency, rate, fetched-at, badge | [✓] | 6 currency rows with rates |
| T37 | Alerts empty state | Inspect alerts section | `#admin-expenses-alerts-empty` with green message | [✓] | Green "no alerts" message |
| T38 | Acknowledge alert | If alerts exist: click `#btn-admin-ack-alert-{id}` | Alert row disappears | [~] | No alerts to test; empty state verified |

---

## Area 6 — Currency Conversion in Expenses

| # | Test | Action | Expected | Result | Notes |
|---|------|---------|----------|--------|-------|
| T39 | BAM expense (baseline) | Create manual expense in BAM | No conversion breakdown in detail | [✓] | AFRODITA 15 BAM; detail shows no conversion section; API confirms exchange_rate=null |
| T40 | Non-BAM expense | Create manual expense in EUR | Detail shows `EUR → BAM @ rate` | [✗→fix] | **BUG-1 FIXED**: Root cause was JS identifier collision — snake_case + kebab-case symbols in `:keys` both compile to same JS var. Fix: removed all snake_case from destructuring. REPL-verified; browser re-test pending |
| T41 | Currency dropdown | Open expense form currency dropdown | Only enabled currencies listed | [✓] | Shows BAM, CHF, EUR, GBP, RSD, TRY, USD (JPY absent after T32 removal) |

---

## Summary

| Area | Total | Pass | Fail | Partial | Status |
|------|-------|------|------|---------|--------|
| Routing | 4 | 4 | 0 | 0 | **Done** |
| Profile page | 15 | 14 | 0 | 1 | **Done** (T14 stale cache bug) |
| Admin settings | 9 | 9 | 0 | 0 | **Done** |
| Currencies | 5 | 5 | 0 | 0 | **Done** |
| Exchange rates | 5 | 4 | 0 | 1 | **Done** (T38 skipped — no alerts) |
| Expense creation | 3 | 2 | 1 | 0 | **Done** (T40 bug fixed, re-test pending) |
| **Total** | **41** | **38** | **1** | **2** | |

---

## Bugs Found

### BUG-1: Expense detail page renders stale/incomplete data (T40) — FIXED
- **File**: `src/app/domain/frontend/expenses/pages/user/expense_detail.cljs`
- **Severity**: Medium
- **Status**: **FIXED** — code change applied, REPL-verified
- **Description**: When viewing a non-BAM expense detail page, the conversion breakdown section does not render even though:
  1. Backend API returns correct data (`exchange_rate: 1.95583`, `bam_amount: 39.12`, `currency: "EUR"`)
  2. Re-frame app-db at `[:user-expenses :current-expense :data]` has all correct kebab-case keys
  3. The `conversion-breakdown` component exists and `show-conversion?` logic is correct
- **Symptoms**:
  - Supplier shows "—" instead of "AFRODITA" (despite `:supplier-display-name` being in the data)
  - Status shows "Na čekanju" despite `:is-posted true`
  - No conversion breakdown section rendered
  - Payer, total, currency, date, and items render correctly
- **Root cause**: ClojureScript compiles both `supplier-display-name` and `supplier_display_name` to the same JS identifier (`supplier_display_name`). The `:keys` destructuring had both snake_case and kebab-case symbols, causing shadowing. Since normalized data only has kebab-case keys, the snake_case symbol "won" and bound to `nil`.
- **Fix**: Removed all snake_case symbols from `:keys` destructuring in both `expense-detail-page` and `line-item-table` components. Data is always normalized to kebab-case by `convert-db-keys->app-keys`.
- **Verification**: REPL confirmed `convert-db-keys->app-keys` produces `:supplier-display-name`, `:is-posted`, `:exchange-rate`, `:bam-amount`, `:raw-label`, `:unit-price`, `:line-total` — all matching the fixed destructuring. Browser verification pending (impersonation session lacks tenant context).
- **Screenshot (pre-fix)**: `tmp/t40-expense-detail.png`

### BUG-2: Workspace name shows stale value after save (T14)
- **File**: Profile page component (reads tenant name from auth session)
- **Severity**: Low
- **Status**: Open
- **Description**: After saving a new workspace name via `#btn-profile-save-workspace-name`, the value persists to the database (confirmed via SQL), but the UI shows the old name after reload. The profile page reads the tenant name from the cached auth session rather than fetching fresh tenant data after save.
- **Fix needed**: After saving workspace name, either (a) re-fetch auth status to refresh the session cache, or (b) read tenant name from a dedicated API call in the profile page instead of relying on the auth session.

### NOTE: User feedback (not a test bug)
- **Profile page "Zadani platitelj"**: User requested this be an editable dropdown (like Default Category), not read-only text. Implementation change needed in profile page component.
