# Privacy Hardening Implementation Plan

Created: 2026-04-28

This file tracks the implementation plan and live progress for the remaining privacy/security gaps identified in the admin/user data protection audit.

## Goal

Reduce the impact of database read access and routine global-admin access by minimizing identity exposure, removing bearer secrets from plaintext storage, tightening break-glass flows, and documenting the intended identity-management exceptions.

## Progress summary

| Area | Status | Notes |
| --- | --- | --- |
| Admin session token hashing | Completed | New sessions store only a deterministic SHA-256 hash in `admin_sessions.token`; raw bearer tokens remain only at the browser/API boundary. |
| Reset/verification/invitation token hashing | Completed | Password reset, email verification, tenant invitation, and admin invitation tokens now persist only deterministic SHA-256 hashes; raw tokens are returned only for URL/email delivery. |
| User CSV export hardening | Completed | User CSV export now emits pseudonymous user refs and non-identity account metadata only; raw emails, encrypted email fields, full names, tenant joins, and stale role/tenant columns are omitted. |
| Reveal-email hardening | Completed | Reveal endpoints are now owner-only break-glass flows requiring structured reason codes plus detailed justification, with structured audit metadata. |
| Receipt raw content minimization | Completed | Routine admin receipt projections now omit raw OCR JSON, parsed markdown, storage keys, original filenames, and file hashes while preserving derived review metadata and download URL boundaries. |
| DB relationship privacy strategy | Decision recorded | A true DB-dump relationship privacy fix requires a dedicated schema/migration project; continue using app-layer pseudonymous projections until that project is explicitly scoped. |
| Plaintext `full_name` strategy | Completed | Names remain identity-management data, while routine operational audit/login-monitoring views now use pseudonymous refs instead of `full_name`. |
| Key rotation/keyring support | Completed | Email ciphertext decrypts by stored `email_key_version` using keyring/per-version config; new writes continue using the active key version. |
| Dev/staging key policy | Not started | Prevent shared defaults outside local development and document rotation/reset path. |
| Stale impersonation docs cleanup | Not started | Remove old runtime impersonation references from docs. |

## Ordered implementation plan

### 1. Hash admin session tokens at rest

- **Goal:** If the database is read, stored admin session values should not be directly usable as bearer tokens.
- **Approach:**
  - Keep generated session tokens unchanged at the API/cookie boundary.
  - Store `sha-256` of the token in `admin_sessions.token`.
  - Hash incoming bearer token before lookup, activity update, and invalidation.
  - Optionally tolerate already-hashed input defensively in helper functions without exposing raw values.
- **Files:**
  - `src/app/admin/backend/services/admin/auth.clj`
  - existing focused tests under `test/app/**/admin*auth*` or add a narrow backend test if no coverage exists.
- **Validation:**
  - Focused backend test for login/session lookup/logout behavior.
  - Direct assertion that DB/session insert stores a hash, not the raw token.
- **Progress:** Completed on 2026-04-28.
- **Implementation notes:**
  - Added `hash-session-token` in `src/app/admin/backend/services/admin/auth.clj`.
  - `create-admin-session!` returns the raw token to the caller but persists only the hash.
  - Session lookup, last-activity update, and invalidation hash the incoming bearer token before DB comparison.
  - No schema migration was required because the existing `admin_sessions.token` column now stores the hash value.

### 2. Hash reset, verification, and invitation tokens

- **Goal:** Password reset, email verification, tenant invitation, and admin invitation tokens should not be usable directly from DB dumps.
- **Approach:**
  - Add token hashing helper shared by auth/invitation flows.
  - Prefer storing hashes in existing token columns if compatible, or add `token_hash` columns through canonical EDN + generated migrations if existing flows require transition fields.
  - Query/update by token hash.
  - Keep raw token only in generated URLs/emails.
- **Files likely impacted:**
  - `src/app/template/backend/auth/password_reset.clj`
  - `src/app/template/backend/auth/email_verification.clj`
  - `src/app/template/backend/services/invitation.clj`
  - `src/app/admin/backend/services/admin/admin_invitation.clj`
  - canonical schema EDN only if a migration is needed.
- **Validation:** focused backend tests for create/find/update token-hash boundaries; DB-backed invitation suite attempted but blocked by local test web port conflict.
- **Progress:** Completed on 2026-04-28.
- **Implementation notes:**
  - Added shared `app.template.backend.security.tokens/hash-token` helper.
  - Password reset tokens are stored, looked up, and marked used by hash.
  - Email verification tokens are stored, looked up, and marked used by hash.
  - Tenant/admin invitations store and look up token hashes; create/resend responses still return raw tokens for email links.
  - Invitation list/detail helpers remove stored token values so hashed tokens are not exposed as routine API payload fields.
  - Resending an invitation now mints a fresh raw token because the previous stored value is intentionally non-recoverable.

### 3. Harden user CSV export

- **Goal:** Prevent broad decrypted email export by routine admins.
- **Approach options:**
  - Disable export by default, or
  - require owner-only/break-glass privilege, reason, audit event, and explicit identity-management context, or
  - export pseudonymous refs/masked emails instead of raw emails.
- **Files likely impacted:**
  - `src/app/admin/backend/services/admin/users/bulk.clj`
  - `src/app/template/backend/routes/admin/user_bulk.clj`
  - frontend export controls if present.
- **Validation:** export route tests confirm emails are absent or route is forbidden for routine roles.
- **Progress:** Completed on 2026-04-28.
- **Implementation notes:**
  - Replaced the stale export query with a minimal users-only query over `id`, `status`, `email_verified`, `auth_provider`, `created_at`, and `last_login_at`.
  - Removed email decryption/resolution from CSV export entirely.
  - CSV content now uses `User Ref` instead of raw `ID`/`Email` and omits full names and tenant relationship fields.
  - Added a focused service regression test that fails if export output includes raw email, ciphertext, lookup hash, full name, or tenant data.

### 4. Harden reveal-email flows

- **Goal:** Make identity reveal a true break-glass path rather than a routine support role action.
- **Approach options:**
  - Restrict to owner/superadmin, require structured reason, add rate limits, alerts, and audit metadata.
  - Consider removing reveal endpoints where identity-management pages already intentionally show emails.
- **Files likely impacted:**
  - `src/app/admin/backend/services/admin/identity_reveal.clj`
  - `src/app/template/backend/routes/admin/users.clj`
  - `src/app/template/backend/routes/admin/admins.clj`
- **Validation:** focused route/service tests for allowed/denied roles and audit behavior.
- **Progress:** Completed on 2026-04-28.
- **Implementation notes:**
  - Changed user/admin reveal-email routes from support-level access to owner-level access.
  - Added structured break-glass reason codes: `:account-security`, `:legal-request`, `:data-subject-request`, `:identity-management`, and `:production-incident`.
  - Increased free-text justification minimum length to 20 characters and require both reason code and details before DB lookup.
  - Audit events now include `reason_code`, `reason_label`, detailed reason, entity ref, masked email, and reveal marker while continuing not to include raw email in audit changes.
  - Added focused service tests for validation/audit metadata and route tests proving support is denied while owner is allowed.

### 5. Minimize admin receipt raw content

- **Goal:** Global admins can review/edit receipt-generated expenses without unnecessary raw OCR, markdown, filenames, or storage keys.
- **Approach:**
  - Define safe receipt detail/list projections.
  - Return raw content only behind explicit review/debug endpoint if truly needed.
  - Scrub `raw_extract_json`, `parsed_markdown`, `storage_key`, and potentially `original_filename` by default.
- **Files likely impacted:**
  - `src/app/domain/backend/expenses/privacy.clj`
  - `src/app/domain/backend/expenses/routes/receipts.clj`
  - `src/app/domain/backend/expenses/services/receipts/queries.clj`
- **Validation:** backend route tests confirm raw fields are absent from routine admin responses.
- **Progress:** Completed on 2026-04-28.
- **Implementation notes:**
  - Added `scrub-receipt-raw-content` to the expenses privacy projection layer.
  - `admin-receipt-view` and `admin-receipts-view` now remove `raw_extract_json`, `parsed_markdown`, `storage_key`, `original_filename`, and `file_hash` variants recursively.
  - Receipt detail still computes derived fields such as content type and `download-url` before projection; raw storage metadata stays out of the JSON response.
  - The explicit download endpoint remains the file access boundary.
  - Added focused projection and route tests proving raw receipt content/storage fields are absent from routine admin payloads.

### 6. Decide and implement DB relationship privacy strategy

- **Goal:** Reduce the ability to connect spending records to real people from DB-only access.
- **Approach options:**
  - Keep direct FKs but accept DB-admin trust boundary.
  - Introduce pseudonymous subject IDs separated from identity table.
  - Split identity and operational databases/roles.
  - Add row-level/role-level DB controls.
- **Decision:** Do not attempt an opportunistic partial schema rewrite in this hardening pass. The current app-layer work reduces routine admin exposure, but a DB-only attacker can still follow direct operational foreign keys. Meaningfully changing that requires a dedicated migration project that introduces a separate privacy-subject boundary and updates all operational ownership joins atomically.
- **Recommended implementation project:**
  - Introduce pseudonymous subject IDs that operational tables reference instead of identity rows.
  - Keep identity-to-subject mapping in a narrower identity boundary with stricter DB role access.
  - Migrate receipts, expenses, expense items, payer ownership, and tenant membership touchpoints in one planned migration sequence.
  - Add DB role/view restrictions for read-only operational access where practical.
- **Files likely impacted:** canonical schema EDN, generated migrations, auth/tenant/expenses services, route tests, migration docs.
- **Validation:** migration checks against dev and test databases, service-level ownership tests, and DB role/view access checks.
- **Progress:** Decision recorded on 2026-04-28; implementation deferred to a dedicated schema/migration project.

### 7. Decide plaintext `full_name` policy

- **Goal:** Avoid names acting as plaintext identifiers in operational data.
- **Decision:** Classify `full_name` as identity-management data. It may remain visible on explicit identity-management pages such as `/admin/users`, `/admin/admins`, and tenant/member administration, but routine operational monitoring/audit payloads should use pseudonymous refs.
- **Files impacted:**
  - `src/app/admin/backend/services/admin/audit.clj`
  - `src/app/template/backend/services/monitoring/login_events.clj`
  - `test/app/backend/routes/admin/audit_test.clj`
  - `test/app/backend/routes/admin/login_events_test.clj`
- **Validation:** focused admin audit/login-events tests confirm names are ignored even when present in raw rows and API-failure metadata stores a user ref instead of a plaintext triggering-user name.
- **Progress:** Completed on 2026-04-28.
- **Implementation notes:**
  - Audit user/admin entity labels now resolve to `User-...`/`Admin-...` refs rather than querying `full_name`.
  - Audit list queries no longer join `admins` to select `full_name`; `:admin-name` is kept as a compatibility display field containing the pseudonymous admin ref.
  - API-failure audit metadata no longer stores `:triggering-user-name`; new entries store `:triggering-user-ref`, old routine metadata is scrubbed when read, and duplicate raw `:metadata` is omitted from routine audit responses.
  - Login-events list queries no longer join `admins`/`users` for names; `:principal-name` is retained as a compatibility alias for `:principal-ref`.

### 8. Add real email encryption key rotation/keyring

- **Goal:** Support decrypting old emails with old keys and encrypting new emails with the active key version.
- **Approach:**
  - Introduce keyring configuration.
  - Use row `email_key_version` for decrypt.
  - Keep active key for encrypt.
  - Add rotation/re-encryption utility later if needed.
- **Files impacted:**
  - `src/app/template/backend/security/email.clj`
  - `test/app/template/backend/security/email_test.clj`
  - `docs/general/operations/email-privacy-key-management.md`
- **Validation:** focused tests cover active-key writes, old-key decrypt by stored version, keyring config, per-version env config, missing retired key in prod, raw-email precedence, and malformed ciphertext rejection.
- **Progress:** Completed on 2026-04-28.
- **Implementation notes:**
  - `encrypt-email` now uses the requested active key version rather than ignoring the optional version argument.
  - `decrypt-email` accepts an optional key version and resolves the configured encryption key for that version.
  - `resolve-email` now reads `email_key_version` aliases from rows and decrypts ciphertext with the stored version.
  - Supported read-key configuration order is per-version env var, `EMAIL_PRIVACY_ENCRYPTION_KEYRING_B64`, active `EMAIL_PRIVACY_ENCRYPTION_KEY_B64`, then the local dev default outside prod-like profiles.
  - No schema migration was required because `email_key_version` already exists on protected rows.

### 9. Tighten dev/staging key policy

- **Goal:** Prevent default development keys from protecting any shared/staging/prod data.
- **Approach:**
  - Keep local dev convenience only when environment is explicitly local.
  - Fail fast in staging/prod-like envs if explicit keys are absent.
  - Document reset/rotation for local data.
- **Files likely impacted:** config/security docs and `email.clj` config checks.
- **Validation:** focused config tests.
- **Progress:** Not started.

### 10. Remove stale impersonation documentation

- **Goal:** Docs should match runtime behavior after impersonation removal.
- **Files likely impacted:**
  - `docs/admin/backend/http-api.md`
  - `docs/general/reference/api-reference.md`
  - `docs/general/reference/glossary.md`
- **Validation:** grep for stale `impersonat` references.
- **Progress:** Not started.

## Edge cases to preserve

- Existing login/logout/session flows keep returning and accepting raw bearer tokens at the HTTP/session boundary.
- Missing, nil, blank, malformed, expired, and revoked tokens remain rejected.
- A DB dump must not contain a directly reusable admin session bearer token after new session creation.
- Existing local dev workflows should continue without requiring new secrets for the first admin-session hashing slice.
- Identity-management pages `/admin/users` and `/admin/tenants` may intentionally display emails; operational receipt/expense pages should not expose user identity linkage.

## Validation log

| Date | Check | Result | Output |
| --- | --- | --- | --- |
| 2026-04-28 | Plan created | Completed | n/a |
| 2026-04-28 | Focused admin auth tests | Passed: 8 tests, 35 assertions, 0 failures, 0 errors | `tmp/admin-auth-token-hashing-tests.txt` |
| 2026-04-28 | Focused token hashing unit suite | Passed: 16 tests, 65 assertions, 0 failures, 0 errors | `tmp/privacy-token-hashing-unit-suite.txt` |
| 2026-04-28 | DB-backed invitation focused suite | Blocked by local port conflict: `java.net.BindException: Address already in use` while starting test system | `tmp/privacy-token-invitation-tests.txt` |
| 2026-04-28 | Focused user CSV export tests | Passed: 7 tests, 36 assertions, 0 failures, 0 errors | `tmp/user-csv-export-tests.txt` |
| 2026-04-28 | Focused reveal-email hardening tests | Passed: 22 tests, 125 assertions, 0 failures, 0 errors | `tmp/reveal-email-hardening-tests.txt` |
| 2026-04-28 | Focused receipt raw-content privacy tests | Passed: 14 tests, 109 assertions, 0 failures, 0 errors | `tmp/receipt-raw-content-privacy-tests.txt` |
| 2026-04-28 | Focused `full_name` operational privacy tests | Passed: 15 tests, 92 assertions, 0 failures, 0 errors | `tmp/full-name-operational-privacy-tests.txt` |
| 2026-04-28 | Focused email keyring tests | Passed: 6 tests, 9 assertions, 0 failures, 0 errors | `tmp/email-keyring-tests.txt` |
