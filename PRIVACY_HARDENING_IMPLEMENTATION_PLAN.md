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
| Reveal-email hardening | Not started | Narrow role/ownership, add stronger reason taxonomy/rate limits/alerts/notifications as appropriate. |
| Receipt raw content minimization | Not started | Define safe admin detail projection for raw OCR, markdown, filenames, and storage keys. |
| DB relationship privacy strategy | Not started | Larger architecture decision: pseudonymous subject IDs, separate privacy boundary, or DB-level controls. |
| Plaintext `full_name` strategy | Not started | Decide whether names are identity-management-only, encrypted, pseudonymized, or removed from operational views. |
| Key rotation/keyring support | Not started | Add multi-key decrypt and active-key encrypt support. |
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
- **Progress:** Not started.

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
- **Progress:** Not started.

### 6. Decide and implement DB relationship privacy strategy

- **Goal:** Reduce the ability to connect spending records to real people from DB-only access.
- **Approach options:**
  - Keep direct FKs but accept DB-admin trust boundary.
  - Introduce pseudonymous subject IDs separated from identity table.
  - Split identity and operational databases/roles.
  - Add row-level/role-level DB controls.
- **Files likely impacted:** schema EDN, migrations, services, tests, docs.
- **Validation:** migration checks dev/test plus service-level tests.
- **Progress:** Not started.

### 7. Decide plaintext `full_name` policy

- **Goal:** Avoid names acting as plaintext identifiers in operational data.
- **Approach options:** encrypt, pseudonymize, remove from operational responses, or classify as identity-management-only data.
- **Files likely impacted:** auth services, admin users/admins, tenant routes, audit views, frontend configs.
- **Validation:** route tests and frontend table tests.
- **Progress:** Not started.

### 8. Add real email encryption key rotation/keyring

- **Goal:** Support decrypting old emails with old keys and encrypting new emails with the active key version.
- **Approach:**
  - Introduce keyring configuration.
  - Use row `email_key_version` for decrypt.
  - Keep active key for encrypt.
  - Add rotation/re-encryption utility later if needed.
- **Files likely impacted:**
  - `src/app/template/backend/security/email.clj`
  - auth/admin/tenant tests.
- **Validation:** tests for current key, old key, missing key, invalid ciphertext.
- **Progress:** Not started.

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
