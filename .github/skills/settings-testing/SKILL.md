---
name: settings-testing
description: "Run real-UI settings verification for admin/user settings, record task-linked findings, and restore temporary changes. Use when testing admin-settings, user-settings, locks, propagation, or action gates."
---

# settings-testing

Use this when someone asks to test:

- admin settings (`/admin/admin-settings`)
- user settings (`/admin/user-settings`)
- view options (display toggles, per-page, list behavior)
- form fields (create/edit field checkboxes)
- table columns (visibility, labels, filterable, sortable, locks)
- lock propagation (admin → user settings, locks → live pages)
- action gates (permission gating on user-facing pages)

## Source of truth

Follow:

- `AGENTS.md`
- `.github/copilot-instructions.md`
- `docs/admin/frontend/settings-testing.md` (primary workflow)
- `docs/admin/frontend/unified-settings.md` (architecture and config reference)

## Default flow

Treat the invoking prompt as the scope definition for this run.

1. Create or reuse a plan document.
2. Create or reuse a results document.
3. Open the real settings page in one browser tab and the real consuming page in another.
4. Keep the relevant settings subform visible while validating.
5. Change one setting only.
6. Save if required.
7. Reload the consuming page.
8. Record pass/fail/inconclusive with the exact plan task reference.
9. Restore the original state.

## Input contract

The prompt should explain what exactly to test, for example:

- entity
- scope (`admin` or `user`)
- source settings page
- consuming page
- settings section
- task IDs or named checks
- data setup/cleanup needs

If the prompt is incomplete:

- keep the scope narrow
- make the smallest safe assumption
- state that assumption in the results
- do not silently expand into a full sweep

## Scope-specific guidance

### Admin scope

- Source page: `/admin/admin-settings`
- Consuming pages: `/admin/articles`, `/admin/suppliers`, and other admin list pages
- Form-fields and table-columns structural edits save **immediately** via PATCH
- View Options and Table Columns policy use **draft model** with Save/Discard
- Action gates are **bypassed** on admin routes — gate effects are not observable here
- To verify lock propagation: also check `/admin/user-settings` for "Enforced" labels

### User scope

- Source page: `/admin/user-settings`
- Consuming pages: `/t/:tenant/expenses/list`, `/t/:tenant/receipts`, and other user-facing pages
- **All changes are draft-based** — nothing saves immediately, always requires "Save settings"
- Admin locks appear as **"Enforced"** (non-interactive) in the user-settings editor
- Action gates are **enforced** on user routes against the user's membership role
- To verify upstream locks: also check `/admin/admin-settings`

## Rules

- Use the real UI first; scripts are secondary evidence only.
- Do not rely on config inspection alone.
- Check for local `ui-entity-prefs` overrides before judging admin defaults.
- If locks are involved, also inspect the downstream settings UI where overrides would normally be attempted.
- If a value sticks or fails to propagate, record it instead of hiding it during cleanup.
- When testing user-scope settings, pair with the correct user-facing consuming page (not an admin page).
- When testing action gates, use a user route — admin routes bypass all gates.

## Before finishing

- Restore temporary settings changes.
- Restore temporary locks.
- Delete temporary test records.
- Record any unresolved sticky values.
- Record final restoration status in the results document.
