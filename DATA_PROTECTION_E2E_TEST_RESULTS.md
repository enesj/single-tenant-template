# Data Protection E2E Test Results

Created: 2026-04-28  
Plan: `DATA_PROTECTION_E2E_TEST_PLAN.md`  
Execution method: Chrome DevTools MCP + PostgreSQL MCP + focused repo validation commands

## Run summary

Status: Continued pass complete — remaining gaps resolved with focused evidence and documented DB-context caveat

| Area | Status | Notes |
| --- | --- | --- |
| P0.1 Application and browser readiness | PASS | Chrome DevTools MCP reached authenticated admin pages at `http://localhost:8085`; network inspection worked. |
| P0.2 Schema and migration readiness | PASS with DB-context note | Direct operational user-link columns are absent; subject-ref columns remain. Official app-config privacy check reports complete; PostgreSQL MCP is connected to port `5432`, not the configured dev port `55432`. |
| S1 Admin session token hashing | PASS with historical/DB-context warning | Focused token tests prove new admin session storage hashes tokens. PostgreSQL MCP historical row counts remain non-authoritative until connected to dev port `55432`. |
| S2 Reset/verification/invitation token hashing | PASS with historical/DB-context warning | Focused reset/verification/invitation token tests prove new token storage/lookups hash tokens. Browser mail flows were not replayed in this continuation. |
| S3 User CSV export privacy | PASS | Chrome DevTools MCP verified `/admin/api/users/actions/export` returns pseudonymous CSV without raw email/full-name/encrypted-email fields. |
| S4 Reveal-email break-glass controls | PASS | Service/route tests and Chrome DevTools MCP verified owner-gated structured reason enforcement and audit metadata without raw email in audit changes. |
| S5 Admin receipt/expense raw-content minimization smoke | PASS | Admin receipt and expense pages/API payloads omit checked identity, subject-ref, and raw receipt fields. |
| S6 Operational ownership via subject refs | PASS | Official backfill completion check and focused subject-ref tests passed; no direct operational `user_id` columns remain in checked tables. |
| S7 Full-name / routine identity-surface privacy | PASS after remediation | Audit/login-event UI and API payloads now expose pseudonymous refs and omit UUID-shaped identity fields (`actor-id`/`target-id`/`principal-id`). |
| S8 Email encryption keyring behavior | PASS | Focused keyring tests cover active key version writes, retired-key reads, per-version env keys, and malformed ciphertext rejection. |
| S9 Dev/staging key policy | PASS | Focused profile tests verify dev/test defaults work while staging/prod-like missing keys fail fast without editing secrets. |
| S10 Impersonation removal smoke | PASS with historical audit note | No impersonation control observed in tested admin pages; POST to stale impersonation route returned 405. Historical `user.impersonated` audit rows remain visible as old audit data only. |
| S11 Audit logs frontend config alignment | PASS | `bb validate-frontend-config` passed; audit page rendered without console errors. The prior identity UUID leak was remediated under S7. |
| Console/network error scan | PASS | Tested receipts, expenses, audit logs, and login events had no console errors; inspected fetch/XHR calls returned 200. |

## Assumptions and scope for this first implementation pass

- This is the first execution pass for the comprehensive plan, not the final full S1-S11 sign-off.
- The app is expected to already be running at `http://localhost:8085`.
- Chrome DevTools MCP is the required browser driver for live UI checks.
- PostgreSQL MCP is used for DB/schema checks; no direct `psql` is used.
- No secrets files are read or modified.
- If no authenticated admin browser session is available and no safe credentials are discoverable from existing browser state, browser checks will stop at unauthenticated reachability and the blocked admin-only scenarios will be recorded rather than guessed.

## Evidence log

### P0.1 Application and browser readiness

Result: PASS

Chrome DevTools MCP state:

- Existing browser session was authenticated as an admin/owner.
- `/admin/receipts`, `/admin/expenses`, `/admin/audit`, and `/admin/login-events` loaded successfully.
- Fetch/XHR network requests were visible and inspectable.
- Console error scans for the tested pages returned no errors/warnings/issues.

Notes:

- Dev-only informational logs were present on the already-open receipts page, but no fatal console errors were observed during the tested page loads.
- The layout sidebar intentionally displayed the logged-in admin email. That value is an identity-management/session surface, not an operational table payload; it accounts for the one visible email count in DOM scans.

### P0.2 Schema and migration readiness

Result: PASS with DB-context note

PostgreSQL MCP schema query results:

| Table | Direct `user_id` / `created_by` columns | Subject-ref columns present |
| --- | --- | --- |
| `expenses` | Absent | `subject_ref`, `created_by_subject_ref` |
| `receipts` | Absent | `subject_ref`, `created_by_subject_ref` |
| `user_expense_settings` | Absent | `subject_ref` |

Subject-ref data counts:

| Table | Total rows | Rows with `subject_ref` | Rows with `created_by_subject_ref` |
| --- | ---: | ---: | ---: |
| `expenses` | 237 | 236 | 226 |
| `receipts` | 222 | 222 | 222 |
| `user_expense_settings` | 5 | 5 | n/a |

Additional schema observation:

- Public tables with `subject_ref` no longer also have `user_id`.
- Other identity/system tables still intentionally use `user_id`, including `tenant_memberships`, `onboarding_progress`, and `email_verification_tokens`.

PostgreSQL MCP warning:

- PostgreSQL MCP reported it is connected to database `multi_tenant_pos` as `app_user` on server port `5432`.
- The repository dev config in `config/base.edn` expects dev DB port `55432` and test DB port `55433`.
- On the PostgreSQL MCP connection, one linked `expenses` row had nil `subject_ref` while its receipt had a subject ref.

Official app-config check:

- Command output saved to `tmp/privacy-e2e-subject-check.txt`.
- `bb privacy-subject-backfill --check-complete --pretty` completed successfully.
- Summary: `:complete? true`, `:remaining-link-count 0`, and 0 missing subject references across expenses, receipts, and settings.

Result interpretation:

- Treat the official app-config check as the authoritative local-app result for now.
- Treat the PostgreSQL MCP one-row warning as a DB-connection-context discrepancy unless the MCP connection is reconfigured to the configured dev DB port.

### S1 Admin session token hashing

Result: PASS with historical/DB-context warning

PostgreSQL MCP token-shape checks:

| Table | Total token rows | SHA-256-shaped rows | Non-SHA-256-shaped rows | Newest row |
| --- | ---: | ---: | ---: | --- |
| `admin_sessions` | 91 | 2 | 89 | 2026-04-28 18:03:35 +02:00 |

Latest `admin_sessions` rows:

- The newest two sessions are 64-character lowercase-hex SHA-256-shaped values.
- Older sessions remain UUID-shaped historical rows.

Result interpretation:

- New admin session writes are covered by focused token hashing tests.
- Historical raw/UUID-shaped admin session rows were observed only through the PostgreSQL MCP connection that reports server port `5432`; this is not the configured app dev DB port (`55432`). Treat those counts as non-authoritative for the running local app unless the MCP connection is reconfigured.
- No raw bearer/admin token values were copied into this report.

Validation:

- Focused token hashing tests passed and output was saved to `tmp/privacy-e2e-token-hashing-focused-tests.txt`.
- Result: 16 tests, 65 assertions, 0 failures, 0 errors.

### S2 Reset, verification, and invitation token hashing

Result: PASS with historical/DB-context warning

The browser flows for password reset, email verification, tenant invitation, and admin invitation were not replayed in this continuation. Existing focused token tests cover the new-write and lookup code paths. A first-pass DB precheck found historical non-SHA-256-shaped token rows on the PostgreSQL MCP connection:

| Table | Total token rows | SHA-256-shaped rows | Non-SHA-256-shaped rows | Newest row |
| --- | ---: | ---: | ---: | --- |
| `admin_invitations` | 1 | 0 | 1 | 2026-03-12 20:02:48 +01:00 |
| `email_verification_tokens` | 3 | 0 | 3 | 2026-03-28 13:50:06 +01:00 |
| `tenant_invitations` | 6 | 0 | 6 | 2026-03-24 09:16:55 +01:00 |

Result interpretation:

- Focused tests prove newly generated password reset, email verification, tenant invitation, and admin invitation tokens are stored/looked up by hash, not by raw token value.
- The historical row counts above came from the PostgreSQL MCP connection on port `5432`, not the configured dev DB on port `55432`; do not use them as final app-local evidence until MCP is reconnected to the configured dev DB.
- If the target policy is no raw token material anywhere in every historical/local database, perform a deliberate cleanup/migration on the correct DB connection after operator approval.

Validation:

- Focused token hashing tests passed and output was saved to `tmp/privacy-e2e-token-hashing-focused-tests.txt`.
- Result: 16 tests, 65 assertions, 0 failures, 0 errors.

### S3 User CSV export privacy

Result: PASS

Chrome DevTools MCP observations:

- Page: `/admin/users`
- Status: page loaded successfully and returned user data from `/admin/api/users?limit=25&offset=0`.
- Console errors/warnings/issues: none.
- The page intentionally displayed emails and full names as an identity-management exception.
- No visible CSV export control was found in the accessibility snapshot.
- Endpoint smoke: authenticated POST to `/admin/api/users/actions/export` returned 200.
- Response content type: `text/csv; charset=utf-8`.
- Download filename header matched `users-export-*.csv`.
- CSV header: `User Ref,Status,Email Verified,Auth Provider,Created At,Last Login`.
- Rows exported in the live smoke: 4.
- Forbidden CSV scan: no raw email pattern, no `Email` header, no `Full Name` header, no encrypted email persistence field names, and `User Ref` header present.

Code search note:

- The correct backend route is mounted at `/admin/api/users/actions/export`, not `/admin/api/user-bulk/export`.
- Service code intentionally exports pseudonymous user refs plus safe account metadata only.

Result interpretation:

- S3 is satisfied through the live authenticated endpoint even though a visible UI control was not discoverable in the current accessibility snapshot.
- Follow-up UX work may still add or expose a clearer export control if product requirements need a visible button.

### S4 Reveal-email break-glass controls

Result: PASS

Focused code coverage:

- `app.admin.backend.services.admin.identity-reveal-test` verifies missing reason code rejection, short reason rejection, structured reason normalization, audit metadata, masked email audit entry, and absence of raw email in audit changes.
- `app.backend.routes.admin.users-test` and `app.backend.routes.admin.admins-test` verify `/reveal-email` routes are owner-only.

Chrome DevTools MCP live smoke:

- Page context: `/admin/users` with existing owner session.
- Missing reason-code reveal request returned 400 with field `reason-code` and did not return email data.
- Short reason reveal request returned 400 with field `reason` and did not return email data.
- Valid owner reveal request returned 200 with reason code `legal-request`; the script recorded only booleans (`hasEmail`, `hasMaskedEmail`) and did not copy the revealed email value into this report.
- Latest reveal audit payload had action `reveal_user_email`, omitted `actor-id`/`target-id`, included masked email metadata, included reason code, and did not include raw email in `changes`.

Validation:

- Focused identity/key tests passed and output was saved to `tmp/privacy-e2e-identity-key-focused-tests.txt`.
- Result: 29 tests, 140 assertions, 0 failures, 0 errors.

### S5 Admin receipt raw-content minimization smoke

Result: PASS

Chrome DevTools MCP pages checked:

- `/admin/receipts`
- `/admin/expenses`

Receipt page DOM observations:

- Table showed operational columns such as status, purchase date, supplier, guessed currency/amount, created, and updated.
- The `Originalni naziv datoteke` / original filename column was not visible.
- No `subject_ref` text was visible in the operational table.

Receipt API payload check:

- Endpoint checked: `/admin/api/expenses/receipts?limit=50&offset=0`
- Status: 200
- Top-level keys: `success`, `receipts`, `total`, `purged-total`
- First row keys: `created-at`, `currency-guess`, `expense-id`, `file-purged-at`, `id`, `lines-total-amount-guess`, `payer-id`, `purchased-at-guess`, `refine-pending`, `status`, `supplier-guess`, `total-amount-guess`, `updated-at`
- Forbidden field scan: no hits for direct user links, subject refs, email privacy fields, full names, raw OCR JSON, parsed markdown, storage keys, file hashes, or original filenames.
- Email-like value count in payload: 0

Expense API payload check:

- Endpoint checked: `/admin/api/expenses/entries?limit=25&offset=0`
- Status: 200
- Top-level keys: `success`, `expenses`, `total`
- First row keys: `bam-amount`, `created-at`, `currency`, `exchange-rate`, `expense-category-id`, `expense-category-name`, `id`, `notes`, `original-amount`, `payer-id`, `payer-label`, `payer-type`, `purchased-at`, `rate-fetched-at`, `receipt-id`, `store-id`, `supplier-display-name`, `supplier-id`, `supplier-normalized-key`, `tenant-id`, `total-amount`, `updated-at`
- Forbidden field scan: no hits for direct user links, subject refs, email privacy fields, full names, or raw receipt fields.
- Email-like value count in payload: 0

Notes:

- `payer-id` and `tenant-id` remain present in admin expense/receipt payloads. They are operational IDs, not direct user identifiers; keep them in the threat-model review for indirect linkage risk.

### S6 Operational ownership via subject refs only

Result: PASS

Storage/schema evidence:

- See P0.2: `expenses`, `receipts`, and `user_expense_settings` no longer have direct operational `user_id` / `created_by` columns in the checked schema.
- Official app-config completion check saved to `tmp/privacy-e2e-subject-check.txt` reported `:complete? true`, `:remaining-link-count 0`, and no missing subject refs across the checked operational tables.

Focused test evidence:

- Focused subject-ref/privacy tests passed and output was saved to `tmp/privacy-e2e-subject-ref-focused-tests.txt`.
- Result: 18 tests, 80 assertions, 0 failures, 0 errors.

Result interpretation:

- The storage and service-level privacy boundary for operational ownership is covered by the official backfill gate plus focused subject-ref tests.
- A full two-browser cross-user UI E2E remains a useful future regression addition, but it is no longer a blocking gap for this continuation.

### S7 Full-name / routine identity-surface privacy

Result: PASS after remediation

Code changes made during continuation:

- `src/app/admin/backend/services/admin/audit.clj`
  - Routine audit-list responses now remove raw `:actor-id` and `:target-id` after deriving pseudonymous/admin display refs.
- `src/app/template/backend/services/monitoring/login_events.clj`
  - Routine login-event list responses now remove raw `:principal-id` after deriving `:principal-ref` and `:principal-name`.
- `test/app/backend/routes/admin/audit_test.clj`
  - Added assertions that routine audit results omit `:actor-id` and `:target-id`.
- `test/app/backend/routes/admin/login_events_test.clj`
  - Added assertion that routine login-event results omit `:principal-id`.

Audit logs page:

- Page: `/admin/audit`
- UI rendered pseudonymous values such as `Admin-28B3FFCF` and `User-9A121414` in the Actor/Subject columns.
- API endpoint checked: `/admin/api/audit?limit=50&offset=0&sort=created-at%3Adesc`
- Status: 200
- First row keys after remediation: `action`, `actor-type`, `admin-name`, `admin-ref`, `audit-log-id`, `changes`, `created-at`, `entity-name`, `id`, `ip`, `ip-address`, `target-type`, `updated-at`, `user-agent`.
- Forbidden field-name scan: no hits for `actor-id` or `target-id`.
- Email-like value count in payload: 0
- Shape checks after remediation:
  - `actor-id`: 0 UUID-shaped hits.
  - `target-id`: 0 UUID-shaped hits.
  - `admin-ref`: pseudonymous-ref-shaped in 50/50.
  - `admin-name`: pseudonymous-ref-shaped in 50/50.

Login events page:

- Page: `/admin/login-events`
- API endpoint checked: `/admin/api/login-events?limit=50&offset=0&sort=created-at%3Adesc`
- Status: 200
- First row keys after remediation: `created-at`, `id`, `ip-address`, `principal-name`, `principal-ref`, `principal-type`, `reason`, `success`, `user-agent`.
- Forbidden field-name scan: no hits for `principal-id`.
- Email-like value count in payload: 0
- Shape checks after remediation:
  - `principal-id`: 0 UUID-shaped hits.
  - `principal-ref`: pseudonymous-ref-shaped in 50/50.
  - `principal-name`: pseudonymous-ref-shaped in 50/50.

Result interpretation:

- The visible UI remains privacy-preserving for actor/subject/principal display.
- The routine audit/login-event API payloads no longer expose UUID-shaped identity IDs through the previously leaking compatibility/raw ID fields.

Validation:

- Focused backend tests passed and output was saved to `tmp/privacy-e2e-s7-focused-tests.txt`.
- Result: 15 tests, 95 assertions, 0 failures, 0 errors.
- Chrome DevTools MCP live API re-check passed on `/admin/audit` and `/admin/login-events`.
- Console errors/warnings/issues after the live re-check: none.

### S8 Email encryption keyring behavior

Result: PASS

Focused test evidence:

- `app.template.backend.security.email-test` covered active key-version writes, retired ciphertext reads through keyring, per-version env keys for retired reads, invalid ciphertext rejection, and raw-email precedence for explicit identity-management values.
- Focused identity/key tests passed and output was saved to `tmp/privacy-e2e-identity-key-focused-tests.txt`.
- Result: 29 tests, 140 assertions, 0 failures, 0 errors.

Result interpretation:

- Backend keyring behavior is verified without reading or editing secrets.
- Identity-management pages remain allowed to display resolved emails; routine operational pages remain covered by S5/S7 forbidden-field checks.

### S9 Dev/staging key policy

Result: PASS

Focused test evidence:

- `app.template.backend.security.email-test` verified dev/test bundled defaults work for local usability.
- The same focused test verified staging/prod-like profiles fail fast with `:email-privacy/missing-key` when explicit encryption/lookup keys are absent.
- Focused identity/key tests passed and output was saved to `tmp/privacy-e2e-identity-key-focused-tests.txt`.
- Result: 29 tests, 140 assertions, 0 failures, 0 errors.

Result interpretation:

- This check was intentionally test-driven rather than secret-file-driven; no secrets were read or modified.

### S10 Impersonation removal smoke

Result: PASS with historical audit note

Chrome DevTools MCP observations:

- No impersonation/session-takeover control was observed on the tested admin pages.
- Authenticated `POST` probe to `/admin/api/user-management/impersonate/00000000-0000-0000-0000-000000000000` returned 405.
- Authenticated `GET` to the same path returned the SPA HTML fallback, not an API impersonation response.

Historical data note:

- The audit log still contains old `user.impersonated` events. That is acceptable as historical audit data if the runtime route/control is removed, but it should be mentioned in support docs so testers do not confuse old audit rows for active functionality.

### S11 Audit logs frontend config alignment

Result: PASS

Static/config validation:

- Command output saved to `tmp/privacy-e2e-validate-frontend-config.txt`.
- Result: pass.
- Final output included: `✅ All frontend config EDNs are valid.`
- Informational warnings about missing DB fields were present for several entities, but did not fail validation.

Chrome DevTools MCP page smoke:

- Page: `/admin/audit`
- Table rendered headers including `Action`, `Actor`, `Subject`, `Details`, and `Created`.
- No console errors/warnings/issues were reported for the page load.
- Audit data endpoint returned 200.

Resolution note:

- API identity UUID leakage found during the earlier page smoke was remediated and is now recorded as S7 pass-after-remediation.

### Validation command output

- `bb validate-frontend-config` was run once and saved to `tmp/privacy-e2e-validate-frontend-config.txt`.
- Summary from execution: frontend configuration validation passed; all frontend config EDNs are valid.
- Focused S7 backend tests were run once and saved to `tmp/privacy-e2e-s7-focused-tests.txt`.
- Focused token hashing tests were run once and saved to `tmp/privacy-e2e-token-hashing-focused-tests.txt`.
- Focused identity/key tests were run once and saved to `tmp/privacy-e2e-identity-key-focused-tests.txt`.
- Focused subject-ref/privacy tests were run once and saved to `tmp/privacy-e2e-subject-ref-focused-tests.txt`.
- Final focused regression sweep was run once and saved to `tmp/privacy-e2e-final-focused-tests.txt`.
- Final focused regression result: 62 tests, 315 assertions, 0 failures, 0 errors.
- Official privacy subject completion check was run once and saved to `tmp/privacy-e2e-subject-check.txt`.

## Open items

- Reconcile PostgreSQL MCP DB context (`5432`) with the configured dev DB port (`55432`) before using MCP historical row counts as final app-local evidence.
- Optional hardening: after MCP is connected to the correct DB, explicitly expire/delete/migrate any confirmed historical raw token rows if the target policy is zero legacy raw token material in local databases.
- Optional regression coverage: add full two-browser cross-user UI E2E for S6 and full browser mail/outbox replay for S2. Focused code-path tests currently cover the privacy-critical storage behavior.
- Optional UX follow-up: expose or document the admin users CSV export control if product requirements expect a visible button in `/admin/users`; the authenticated export endpoint itself passed privacy checks.
