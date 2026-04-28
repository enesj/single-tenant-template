# Data Protection E2E Test Plan

Created: 2026-04-28  
Source implementation tracker: `PRIVACY_HARDENING_IMPLEMENTATION_PLAN.md`

## Summary

This plan verifies, end-to-end, the data protection changes implemented today: token hashing, email/key privacy, identity reveal controls, receipt/expense operational privacy, subject-ref ownership, removal of impersonation assumptions, and frontend/admin table configuration privacy. The primary live verification method is **Chrome DevTools MCP** against the running application. Repeatable regression coverage should then be added to the existing Kaocha E2E harness where practical.

## Testing principles

- Use Chrome DevTools MCP for browser-driven E2E checks:
  - navigate real pages,
  - inspect accessibility snapshots,
  - inspect network requests/responses,
  - inspect console errors,
  - verify visible table columns and dialogs.
- Use PostgreSQL MCP for DB verification only; do not use `psql` or ad-hoc shell SQL.
- Do not read or edit secrets files. If a test requires a key, use existing local/dev/test configuration or ask the operator to set placeholders outside the agent workflow.
- Use project-local `tmp/` for captured outputs and screenshots references.
- Keep test data synthetic and obviously non-production, e.g. `privacy-e2e-user-a@example.com`.
- Validate both positive capability and negative leakage: the most important assertions are fields/tokens/emails that must **not** appear.
- Preserve intended exceptions: identity-management pages may show identity data; routine operational receipt/expense/audit views should not.

## Environments and tools

### Primary live-browser E2E target

- App URL: `http://localhost:8085`
- Browser driver: Chrome DevTools MCP
- DB inspection: PostgreSQL MCP connected to the local dev database
- Log inspection when needed: repo log helpers or system log skill; save outputs under `tmp/`

### Repeatable regression target

- Existing E2E suite: Kaocha `:e2e`
- Existing task: `bb e2e-test`
- Test web port: `8086`
- Existing E2E fixture namespaces:
  - `test/app/e2e/fixtures.clj`
  - `test/app/e2e/helpers.clj`
  - `test/app/e2e/multi_tenancy/platform_admin_test.clj`
  - `test/app/e2e/multi_tenancy/auth_context_test.clj`
  - `test/app/e2e/multi_tenancy/data_scoping_test.clj`
  - `test/app/e2e/multi_tenancy/invitation_test.clj`

## Pre-flight checks

### P0.1 Application and browser readiness

Goal: Confirm the app is reachable and Chrome DevTools MCP can drive it.

Steps:

1. Open `http://localhost:8085` with Chrome DevTools MCP.
2. Confirm the page loads without top-level console errors.
3. Capture a page snapshot and confirm expected login/admin navigation elements are discoverable.
4. Confirm network requests are visible in DevTools.

Expected:

- App loads.
- No fatal console errors.
- Chrome DevTools MCP can navigate, inspect snapshots, and list network requests.

Artifacts:

- Browser snapshot notes in `tmp/privacy-e2e-devtools-preflight.md` if needed.

### P0.2 Schema and migration readiness

Goal: Confirm local DB matches the privacy schema cleanup.

DB checks via PostgreSQL MCP:

- `expenses` has no `user_id` or `created_by` columns.
- `receipts` has no `user_id` or `created_by` columns.
- `user_expense_settings` has no `user_id` column.
- `expenses`, `receipts`, and `user_expense_settings` retain `subject_ref` columns.
- `privacy-subject` completion gate reports complete for dev/test if rerun.

Expected:

- No direct operational user-link columns remain.
- Completion gate remains `:complete? true` and `:remaining-link-count 0`.

## Test data setup

Use synthetic users and admins:

- User A: `privacy-e2e-user-a@example.com`
- User B: `privacy-e2e-user-b@example.com`
- Invited user: `privacy-e2e-invitee@example.com`
- Platform admin: existing seeded local admin or a synthetic admin created through supported local test helpers.

Create data through real UI/API flows where possible:

1. Register User A.
2. Register User B in an isolated browser context.
3. Ensure each user has a tenant, default payers, expense categories, and user expense settings.
4. Upload or seed at least one receipt for User A.
5. Create/approve at least one receipt-generated expense.
6. Create at least one tenant invitation and one admin invitation/reset/verification token flow where practical.

DB setup may use existing test helpers in automated E2E; live manual checks should prefer app flows plus PostgreSQL MCP verification.

## Chrome DevTools MCP execution workflow

For each browser-facing scenario:

1. Open a fresh tab or isolated browser context.
2. Navigate to the target URL.
3. Use accessibility snapshots to find elements by stable text/id.
4. Use form fill/click actions to perform the user/admin behavior.
5. Use network request inspection to capture the exact JSON response.
6. Assert that forbidden fields/values are absent from both DOM and network response.
7. Check console messages after the interaction.
8. Use PostgreSQL MCP for storage-layer assertions.
9. Save notes/output references under `tmp/`.

## E2E scenarios

### S1. Admin session token hashing at rest

Covered implementation area: `Admin session token hashing`.

Goal: A raw admin bearer token works at the HTTP boundary but is not stored raw in `admin_sessions.token`.

Chrome DevTools steps:

1. Navigate to `/admin/login`.
2. Log in as the synthetic platform admin.
3. Inspect the login network response and capture the returned raw token or session token boundary value.
4. Use that token for an authenticated admin request, e.g. `/admin/api/tenants`, through the browser session or request context.
5. Verify the request succeeds.

DB checks via PostgreSQL MCP:

1. Query the latest `admin_sessions` row for the admin.
2. Confirm `token` is a deterministic SHA-256-shaped hash.
3. Confirm stored `token` is not equal to the raw token observed at the HTTP boundary.

Expected:

- Admin login works.
- Authenticated admin APIs work.
- DB stores only hash-shaped token values.
- Raw token never appears in DB.

Negative cases:

- Reuse a malformed token and confirm admin API rejects it.
- Log out and confirm the previous token no longer works.

Automation target:

- Extend `test/app/e2e/multi_tenancy/platform_admin_test.clj` or add `test/app/e2e/privacy/admin_session_test.clj`.

### S2. Reset, verification, and invitation token hashing

Covered implementation area: `Reset/verification/invitation token hashing`.

Goal: User-facing token flows work, while stored token columns contain hashes only.

Chrome DevTools steps:

1. Register a new user and trigger any available email-verification flow.
2. Trigger password reset for that user.
3. Invite a tenant member from User A's tenant.
4. If admin invitations are UI-accessible, create/resend an admin invitation.
5. Use captured local outbox/test URLs or API-delivered raw tokens to complete each flow.

DB checks via PostgreSQL MCP:

- Password reset rows store token hashes, not raw URL tokens.
- Email verification rows store token hashes, not raw URL tokens.
- Tenant invitation rows store token hashes, not raw invitation tokens.
- Admin invitation rows store token hashes, not raw invitation tokens.

Expected:

- Tokens work exactly once where applicable.
- Expired/used/revoked/malformed tokens fail.
- Resend mints a new raw token and does not recover the old raw token.
- API list/detail responses do not expose stored token hashes as routine payload fields.

Negative cases:

- Duplicate tenant invitation remains blocked.
- Revoked invitation token cannot be accepted.
- Used password reset token cannot be reused.

Automation target:

- Extend `test/app/e2e/multi_tenancy/invitation_test.clj`.
- Add a focused E2E namespace for password reset / verification if browser routes are stable.

### S3. User CSV export privacy

Covered implementation area: `User CSV export hardening`.

Goal: Routine admin user CSV export contains pseudonymous refs and account metadata only.

Chrome DevTools steps:

1. Log in as platform admin.
2. Navigate to `/admin/users`.
3. Trigger the CSV export control.
4. Inspect the download/network response.

Expected CSV content:

- Contains pseudonymous user references.
- Contains safe account metadata such as status, auth provider, verification state, created/login timestamps if configured.

Forbidden CSV content:

- Raw email addresses.
- Email ciphertext.
- Email lookup hashes.
- Full names.
- Tenant relationship data.
- Password hashes.

DB/API checks:

- Compare exported rows against known users and confirm identity fields are omitted, not merely blank in the UI.

Negative cases:

- Support/lower-privilege admin should not gain broader identity export if role controls are present.

Automation target:

- Add E2E coverage around admin export if download capture is stable; otherwise keep backend route tests as authoritative and use Chrome DevTools as live smoke coverage.

### S4. Reveal-email break-glass controls

Covered implementation area: `Reveal-email hardening`.

Goal: Email reveal is owner-only and requires structured break-glass reason plus detailed justification.

Chrome DevTools steps:

1. Log in as a non-owner/support admin if available.
2. Navigate to identity-management pages such as `/admin/users` and `/admin/admins`.
3. Attempt reveal-email action.
4. Log in as owner/superadmin.
5. Attempt reveal-email with no reason.
6. Attempt reveal-email with only a short justification.
7. Attempt reveal-email with a valid reason code and sufficiently detailed justification.
8. Inspect network responses and UI dialog behavior.

Expected:

- Non-owner/support role is denied.
- Missing reason code is rejected.
- Too-short free-text justification is rejected.
- Valid owner break-glass request reveals the email only through the intended dialog/response.
- Audit event is created with reason code, reason label, detailed reason, entity ref, masked email, and reveal marker.
- Audit event does not store raw email in `changes`/metadata.

Negative cases:

- Blank, nil, malformed, or unsupported reason codes fail.
- Repeated reveal attempts should be visible in audit history and subject to any configured rate/alert behavior.

Automation target:

- Add browser checks for validation states on reveal modal once stable selectors are confirmed.

### S5. Admin receipt raw content minimization

Covered implementation area: `Receipt raw content minimization`.

Goal: Platform admins can read/edit receipt-generated expenses without routine exposure to raw OCR JSON, parsed markdown, original filenames, storage keys, file hashes, or subject refs.

Chrome DevTools steps:

1. As User A, create/upload a receipt and allow or simulate extraction to produce parsed data.
2. As platform admin, navigate to `/admin/receipts`.
3. Confirm the table does not show `Originalni naziv datoteke` / original filename.
4. Open receipt detail/review UI if available.
5. Inspect network responses for `/admin/api/expenses/receipts` list/detail calls.
6. Navigate to `/admin/expenses` or admin expense entries and inspect row payloads.

Forbidden in routine admin receipt payloads:

- `raw_extract_json`
- `rawExtractJson`
- `parsed_markdown`
- `parsedMarkdown`
- `storage_key`
- `storageKey`
- `original_filename`
- `originalFilename`
- `file_hash`
- `fileHash`
- `subject_ref`
- `created_by_subject_ref`
- direct user identity fields

Expected allowed behavior:

- Admin can still see derived review fields such as status, supplier guess, totals, currency, purchased-at guess, errors, retry count, and linked expense metadata.
- Explicit download endpoint remains the file access boundary.

Negative cases:

- Admin list filters should not include removed original filename or created-by identity filters.
- Browser table column chooser should not offer removed raw/identity columns for routine admin receipts.

Automation target:

- Extend `test/app/e2e/multi_tenancy/platform_admin_test.clj` with network payload assertions.
- Add frontend config validation as a prerequisite check: `bb validate-frontend-config`.

### S6. Operational ownership via subject refs only

Covered implementation area: `DB relationship privacy strategy`, `Fallback removal`, `Schema cleanup`.

Goal: User expense/receipt/settings behavior remains correct after direct operational `users.id` columns are removed.

Chrome DevTools steps:

1. Register User A.
2. Create or approve an expense from a receipt.
3. Set/update default payer or receipt OCR provider through user settings/profile pages where available.
4. Confirm User A can list, edit, delete, export, and report on their own expenses.
5. Register User B in another context.
6. Confirm User B cannot see User A's expenses/receipts/settings.
7. As platform admin, confirm admin can view operational expenses/receipts without identity linkage.

DB checks via PostgreSQL MCP:

- New `expenses` rows have `subject_ref` and optional `created_by_subject_ref`.
- New `receipts` rows have `subject_ref` when user-owned or nil when intentionally unassigned.
- New `user_expense_settings` rows are keyed by `subject_ref`.
- No new write attempts reference dropped direct columns.
- Subject refs are stable for the same user and different across users.
- There is no same-DB table mapping `users.id` to `subject_ref`.

Expected:

- User-scoped reads match by subject ref only.
- User A sees own operational data.
- User B cannot infer or access User A's data.
- Admin payloads scrub subject refs.
- Unassigned receipts are claimable only when `subject_ref` is nil.

Negative cases:

- Direct cross-tenant or cross-user resource access returns 404/denied, not leaked data.
- Legacy `user_id` request parameters do not broaden access.
- Empty/nil subject refs do not accidentally expose user-owned rows.

Automation target:

- Extend `test/app/e2e/multi_tenancy/data_scoping_test.clj`.
- Add a dedicated `app.e2e.privacy.subject-ref-test` namespace for DB-level storage assertions after UI/API actions.

### S7. Full-name operational privacy

Covered implementation area: `Plaintext full_name strategy`.

Goal: Names remain allowed on identity-management pages but are absent from routine operational audit/login-monitoring payloads.

Chrome DevTools steps:

1. Create users/admins with recognizable full names.
2. Trigger login success and login failure events.
3. Trigger audit events for admin/user/receipt/expense actions.
4. Navigate to admin audit/log-monitoring pages.
5. Inspect network responses for audit and login events.

Expected identity-management exception:

- `/admin/users`, `/admin/admins`, tenant/member management may display names/emails as explicit identity-management surfaces.

Forbidden routine operational payload content:

- `full_name`
- raw user/admin names in audit operational summaries where pseudonymous refs should appear
- triggering user name in API failure metadata

Expected routine operational content:

- `User-...` / `Admin-...` pseudonymous refs.
- `principal-ref` / compatibility aliases that contain pseudonymous refs.
- `admin-name` compatibility field contains pseudonymous admin ref, not plaintext full name.

Negative cases:

- Old audit metadata with names should be scrubbed on read if present.

Automation target:

- Extend `platform_admin_test.clj` or add `app.e2e.privacy.audit_identity_test`.

### S8. Email encryption keyring behavior

Covered implementation area: `Key rotation/keyring support`.

Goal: Current UI flows resolve emails correctly, while stored protected email fields remain encrypted/hash-based and version-aware.

Chrome DevTools steps:

1. Register a new user.
2. Invite a tenant member.
3. Create or inspect admin/user identity pages where emails are intentionally displayed.
4. Confirm login by email works.
5. Confirm identity-management pages can display intended emails.

DB checks via PostgreSQL MCP:

- Protected user/invitation/admin invitation rows store `email_ciphertext`, `email_lookup_hash`, and `email_key_version` where applicable.
- Raw email is absent from protected columns/tables except intentional legacy/configured identity columns if any remain by design.
- Stored key version is present and matches active config for new writes.

Expected:

- Login/invitation lookup by email works through lookup hash.
- Decryption for display works on identity-management pages.
- Operational pages do not expose raw emails.

Negative cases:

- Malformed ciphertext should not break routine list pages.
- Missing retired key in prod-like profile should fail fast; do not test by editing secrets directly.

Automation target:

- Keep keyring edge cases mostly in focused backend tests; add E2E smoke for register/login/invite display behavior.

### S9. Dev/staging key policy

Covered implementation area: `Dev/staging key policy`.

Goal: Local dev/test remains usable, while staging/prod-like profiles require explicit privacy keys.

Chrome DevTools scope:

- Live browser smoke only for local dev/test behavior: registration, login, admin identity pages, invitation flow.

Non-browser validation:

- Focused backend profile/config tests remain primary for fail-fast prod-like behavior.
- Do not modify secrets files during E2E.

Expected:

- Dev/test app flows continue to work without manual key setup.
- Prod-like missing key behavior is covered by focused tests rather than live secret manipulation.

### S10. Impersonation removal and admin operational access

Covered implementation area: `Stale impersonation docs cleanup` and operational replacement behavior.

Goal: Platform admins can work with receipt/expense operational data without impersonating users, and no runtime impersonation route remains available.

Chrome DevTools steps:

1. Log in as platform admin.
2. Navigate admin pages for tenants, receipts, and expenses.
3. Confirm admin can access operational receipts/expenses directly.
4. Inspect UI for absence of impersonation/session-takeover controls.
5. Attempt known stale route if still routable, e.g. `/admin/api/user-management/impersonate/:id`, using an authenticated admin request.

Expected:

- Admin receipts/expenses access works without impersonation.
- No impersonation UI control appears.
- Stale impersonation API route is 404/405/denied.
- User session is not minted from admin console.

Automation target:

- Extend `test/app/e2e/multi_tenancy/platform_admin_test.clj`.

### S11. Audit logs frontend config alignment

Covered implementation area: admin `audit-logs` computed table fields.

Goal: Audit logs page renders adapter-derived columns without frontend config unknown-field drift.

Chrome DevTools steps:

1. Log in as platform admin.
2. Navigate to the audit logs page.
3. Confirm visible columns include intended display fields such as actor, subject, and details.
4. Confirm no table rendering errors in console.
5. Inspect network payload and verify adapter-derived fields are populated or safely absent.

Static validation:

- Run `bb validate-frontend-config` and save output under `tmp/`.

Expected:

- No `audit-logs` unknown-field validation errors.
- Audit logs page renders without console errors.

## Cross-cutting forbidden-field checklist

Use this checklist against DOM text, network response JSON, CSV downloads, and DB rows depending on the scenario.

### Forbidden in routine operational admin receipt/expense payloads

- raw user email
- encrypted email fields
- email lookup hashes
- plaintext full name
- `user_id`
- `created_by`
- `subject_ref`
- `created_by_subject_ref`
- `raw_extract_json`
- `parsed_markdown`
- `storage_key`
- `file_hash`
- `original_filename`

### Forbidden in token storage

- raw admin session token
- raw password reset token
- raw email verification token
- raw tenant invitation token
- raw admin invitation token

### Allowed identity-management exceptions

- `/admin/users`
- `/admin/admins`
- tenant/member management pages
- owner-only break-glass reveal-email dialog after valid reason/justification

## Suggested automated E2E additions

### New namespace: `app.e2e.privacy.admin-operational-privacy-test`

Purpose:

- Admin can list receipts/expenses without impersonation.
- Admin payloads omit identity/raw receipt fields.
- Stale impersonation route is unavailable.

Candidate file:

- `test/app/e2e/privacy/admin_operational_privacy_test.clj`

Depends on:

- `test/app/e2e/fixtures.clj`
- `test/app/e2e/helpers.clj`
- admin helper patterns from `platform_admin_test.clj`

### New namespace: `app.e2e.privacy.subject-ref-storage-test`

Purpose:

- Register user, create receipt/expense/settings rows through app flows.
- Verify DB uses subject refs and no direct operational user-link columns exist.
- Verify cross-user/cross-tenant invisibility.

Candidate file:

- `test/app/e2e/privacy/subject_ref_storage_test.clj`

### New namespace: `app.e2e.privacy.identity-surface-test`

Purpose:

- Verify identity pages may show emails/names.
- Verify audit/login-monitoring/operational pages show pseudonymous refs instead.
- Verify reveal-email break-glass validation states in the browser.

Candidate file:

- `test/app/e2e/privacy/identity_surface_test.clj`

### Extension: `invitation_test.clj`

Purpose:

- Add DB assertions that tenant invitation token storage is hash-only.
- Assert routine invitation list/detail payloads do not expose token hashes.

### Extension: `platform_admin_test.clj`

Purpose:

- Assert admin token hash-at-rest after login.
- Assert admin receipt/expense network payloads are privacy-scrubbed.

## Manual Chrome DevTools MCP run order

Run in this order to maximize signal and avoid cascading setup confusion:

1. P0 pre-flight checks.
2. S1 admin session hashing.
3. S8 local email privacy smoke: register/login/invite.
4. S2 invitation/reset/verification token hashing.
5. S6 subject-ref storage and cross-user isolation.
6. S5 admin receipt raw-content minimization.
7. S10 admin operational access without impersonation.
8. S3 CSV export privacy.
9. S4 reveal-email break-glass.
10. S7 full-name operational privacy.
11. S11 audit logs config/page rendering.

## Evidence to capture

For each scenario, save concise evidence under `tmp/`:

- `tmp/privacy-e2e-devtools-notes.md` — manual DevTools observations and pass/fail notes.
- `tmp/privacy-e2e-network-redactions.md` — request URLs inspected and forbidden fields checked.
- `tmp/privacy-e2e-db-checks.md` — PostgreSQL MCP query summaries, not secrets.
- `tmp/privacy-e2e-console-checks.md` — console error summaries.
- `tmp/privacy-e2e-validation-output.txt` — output from validation/test commands when run.

Do not paste raw bearer tokens, reset tokens, invitation tokens, or secrets into evidence files. Record only whether raw values matched or did not match stored hashes.

## Edge cases

- Nil/blank token values are rejected.
- Malformed token values are rejected.
- Expired/revoked/used tokens are rejected.
- Empty receipt list renders without raw-column placeholders.
- Receipt with `subject_ref` nil is treated as unassigned only when intentionally unassigned.
- User with no expenses sees empty user lists/reports without leaking other users' rows.
- Admin with insufficient role cannot reveal email.
- Owner reveal request with missing/short reason fails.
- CSV export with zero users or minimal users still omits identity-sensitive columns.
- Audit metadata containing older plaintext name fields is scrubbed on read.
- Staging/prod-like missing privacy keys fail in focused config tests, not through unsafe secret edits.

## Exit criteria

The data protection E2E pass is complete when:

1. Chrome DevTools MCP live checks pass for S1 through S11 or each skipped item has a documented reason.
2. PostgreSQL MCP confirms no direct operational user-link columns remain and new operational rows use subject refs.
3. No forbidden field appears in inspected DOM, network JSON, CSV output, or routine admin operational payloads.
4. Token storage checks prove raw tokens are not stored.
5. Identity-management exceptions are verified and documented.
6. `bb validate-frontend-config` passes with no unknown-field errors.
7. Repeatable automated E2E tests are added or follow-up tickets are created for any manual-only DevTools checks.
8. All outputs are saved once under `tmp/` and referenced in the final validation summary.

## Open assumptions

- The local app is already running on `http://localhost:8085` as expected by the repo.
- A platform admin can be seeded or already exists for local admin E2E.
- Some token flows may rely on the local outbox/test email capture rather than real email delivery.
- Receipt OCR provider calls should not be required for E2E; use existing test fixtures, stored OCR responses, or app-supported review states where needed.
- Production/staging key policy should remain focused-test verified unless an operator explicitly provides a safe staging test environment.
