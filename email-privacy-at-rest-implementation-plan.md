# Email Privacy At Rest Implementation Plan

## Summary

Introduce encrypted-at-rest email storage with blind indexes while preserving the app's current equality-based behavior for login, OAuth matching, password reset, tenant invitations, and admin invitations. Routine admin surfaces become pseudonymous by default: admins can still follow structural links between users, tenants, receipts, expenses, and payment-provider accounts, but they do not see user emails during normal operations. Invitation mismatch errors may still show the exact invited email, and any identity reveal becomes exceptional and audit-logged.

## Implementation Steps

1. Define the email privacy boundary.

Goal: centralize normalization, hashing, encryption, decryption, masking, and log redaction so email handling is consistent across user, admin, and invitation flows.

Files:

- `/Users/enes/Projects/single-tenant-template/src/app/template/backend/security/email.clj`
- `/Users/enes/Projects/single-tenant-template/src/app/template/backend/auth/service.clj`
- `/Users/enes/Projects/single-tenant-template/src/app/template/backend/auth/password_reset.clj`
- `/Users/enes/Projects/single-tenant-template/src/app/template/backend/services/invitation.clj`
- `/Users/enes/Projects/single-tenant-template/src/app/admin/backend/services/admin/auth.clj`
- `/Users/enes/Projects/single-tenant-template/src/app/admin/backend/services/admin/admin_invitation.clj`

Dependencies: none

Owner: `Coder`

Notes:

- Normalize email once before persistence and lookup.
- Compute `email_lookup_hash` using `HMAC-SHA256(normalized-email, lookup-key)`.
- Encrypt normalized email using random-IV authenticated encryption.
- Provide helpers for `mask-email`, `email->lookup-hash`, `encrypt-email`, `decrypt-email`.
- Include `key_version` support from the start.

1. Add new email storage columns through migrations.

Goal: stage the rollout without breaking existing flows and keep schema changes reversible.

Files:

- `/Users/enes/Projects/single-tenant-template/resources/db/migrations/<new-migration>.edn`
- `/Users/enes/Projects/single-tenant-template/resources/db/models.edn`

Dependencies: step 1

Owner: `Coder`

Notes:

- Update `users`, `admins`, `tenant_invitations`, and `admin_invitations`.
- Add `email_ciphertext`, `email_lookup_hash`, and `email_key_version`.
- Keep legacy plaintext `email` during rollout for dual-read and backfill.
- Add unique indexes on `users.email_lookup_hash` and `admins.email_lookup_hash`.
- Add non-unique indexes on invitation lookup hashes.
- Apply migrations to both dev and test databases.

1. Backfill existing data and enable dual-write.

Goal: populate new columns for existing rows and ensure every new write keeps plaintext and encrypted forms in sync until cutover.

Files:

- `/Users/enes/Projects/single-tenant-template/resources/db/migrations/<backfill-migration>.edn`
- `/Users/enes/Projects/single-tenant-template/src/app/template/backend/auth/service.clj`
- `/Users/enes/Projects/single-tenant-template/src/app/admin/backend/services/admin/auth.clj`
- `/Users/enes/Projects/single-tenant-template/src/app/template/backend/services/invitation.clj`
- `/Users/enes/Projects/single-tenant-template/src/app/admin/backend/services/admin/admin_invitation.clj`

Dependencies: step 2

Owner: `Coder`

Notes:

- Every create and update path writing email should populate both old and new columns during transition.
- Normalize before hashing/encryption so uniqueness is enforced on canonical values.
- Resolve any case-collision rows before enabling unique blind-index constraints.

1. Replace exact-match lookups with blind-index lookups.

Goal: preserve business behavior while removing runtime dependency on plaintext email queries.

Files:

- `/Users/enes/Projects/single-tenant-template/src/app/template/backend/auth/service.clj`
- `/Users/enes/Projects/single-tenant-template/src/app/template/backend/auth/password_reset.clj`
- `/Users/enes/Projects/single-tenant-template/src/app/admin/backend/services/admin/auth.clj`
- `/Users/enes/Projects/single-tenant-template/src/app/admin/backend/services/admin/admins.clj`

Dependencies: steps 1-3

Owner: `Coder`

Notes:

- `register-user-with-password!`, `login-with-password`, and OAuth correlation should query by hash.
- Password reset should find principals by hash instead of `WHERE email = ?`.
- Admin duplicate checks and admin auth should also query by hash.
- Returned rows should be decrypted only when the caller needs actual email data.

1. Rework invitation flows to use exact email identity without plaintext lookup.

Goal: keep duplicate invite prevention and invitation-acceptance matching behavior intact.

Files:

- `/Users/enes/Projects/single-tenant-template/src/app/template/backend/services/invitation.clj`
- `/Users/enes/Projects/single-tenant-template/src/app/admin/backend/services/admin/admin_invitation.clj`
- `/Users/enes/Projects/single-tenant-template/src/app/template/backend/routes/tenant.clj`
- `/Users/enes/Projects/single-tenant-template/src/app/template/backend/routes/admin/admin_invitations.clj`

Dependencies: steps 1-4

Owner: `Coder`

Notes:

- Duplicate pending invite checks move to hash columns.
- Existing-member checks compare against the user's hash-backed identity.
- Invitation mismatch errors may still show the exact invited email after decrypting the invitation record.
- Invitation email sending decrypts only at the delivery edge.

1. Convert routine admin surfaces to pseudonymous projections.

Goal: let admins follow structural relationships without exposing raw email in normal admin workflows.

Files:

- `/Users/enes/Projects/single-tenant-template/src/app/admin/backend/services/admin/users.clj`
- `/Users/enes/Projects/single-tenant-template/src/app/admin/backend/services/admin/admins.clj`
- `/Users/enes/Projects/single-tenant-template/src/app/template/backend/routes/admin/tenants.clj`
- `/Users/enes/Projects/single-tenant-template/src/app/domain/backend/expenses/routes/core.clj`
- `/Users/enes/Projects/single-tenant-template/src/app/domain/backend/expenses/services/`
- `/Users/enes/Projects/single-tenant-template/src/app/admin/frontend/pages/users.cljs`
- `/Users/enes/Projects/single-tenant-template/src/app/admin/frontend/pages/admins.cljs`

Dependencies: step 4

Owner: `Coder`

Notes:

- Replace exposed email fields with stable pseudonymous refs such as `User-184` and `Tenant-22`.
- Remove routine email columns from `/admin/users`, admin tenant membership views, and any admin receipts/expenses review screens.
- Admin review APIs for receipts and expenses may still return `user_ref`, `tenant_ref`, status, amount, and other operational fields.
- Remove `ILIKE` substring email search and email sort from routine admin surfaces.
- Preserve nickname or pseudonymous-ref search where needed.

1. Add audited exceptional identity-reveal paths.

Goal: preserve operability for support and incident handling without reintroducing routine email visibility.

Files:

- `/Users/enes/Projects/single-tenant-template/src/app/admin/backend/services/admin/`
- `/Users/enes/Projects/single-tenant-template/src/app/template/backend/routes/admin/`
- `/Users/enes/Projects/single-tenant-template/src/app/admin/frontend/`

Dependencies: step 6

Owner: `Coder`

Notes:

- Identity reveal should require an explicit support reason and generate an audit event.
- Keep the implementation minimal and isolated from normal list/detail pages.
- Do not expose reveal capability in the default browsing flow.

1. Add generic payment-provider account mappings.

Goal: support future billing providers without requiring routine admin email access.

Files:

- `/Users/enes/Projects/single-tenant-template/resources/db/migrations/<payment-provider-link-migration>.edn`
- `/Users/enes/Projects/single-tenant-template/resources/db/models.edn`
- `/Users/enes/Projects/single-tenant-template/src/app/template/backend/services/`
- `/Users/enes/Projects/single-tenant-template/src/app/template/backend/routes/admin/`

Dependencies: steps 4-6

Owner: `Coder`

Notes:

- Store mappings like `tenant_id -> provider_customer_ref` or `user_id -> provider_customer_ref`.
- Keep provider naming generic so the model works with Stripe or another service.
- Admin billing views should use internal account refs plus provider customer refs, not user email.

1. Remove raw email from session blobs, audit logs, and operational logs where it is not required.

Goal: avoid undermining the at-rest protection by leaving raw emails in secondary storage surfaces.

Files:

- `/Users/enes/Projects/single-tenant-template/resources/db/models.edn`
- `/Users/enes/Projects/single-tenant-template/src/app/template/backend/routes/email_verification.clj`
- `/Users/enes/Projects/single-tenant-template/src/app/shared/auth.cljc`
- `/Users/enes/Projects/single-tenant-template/src/app/admin/backend/services/admin/admins.clj`
- `/Users/enes/Projects/single-tenant-template/src/app/admin/backend/services/admin/admin_invitation.clj`
- `/Users/enes/Projects/single-tenant-template/src/app/template/backend/services/invitation_email.clj`
- `/Users/enes/Projects/single-tenant-template/src/app/template/backend/services/gmail_api.clj`
- `/Users/enes/Projects/single-tenant-template/src/app/template/backend/services/gmail_smtp.clj`
- `/Users/enes/Projects/single-tenant-template/src/app/template/backend/services/postmark_email.clj`

Dependencies: steps 1-6

Owner: `Coder`

Notes:

- DB-backed session payloads should prefer user/admin ids and roles over raw email.
- Audit rows should store masked hints or structured identifiers instead of full email in normal operations.
- Delivery logs should avoid raw `to-email` unless explicitly needed for debugging and acceptable under policy.

1. Remove legacy plaintext email columns after a stable transition window.

Goal: complete the migration and ensure email is no longer stored in plaintext in the main tables.

Files:

- `/Users/enes/Projects/single-tenant-template/resources/db/migrations/0062_schema.edn`
- `/Users/enes/Projects/single-tenant-template/resources/db/models.edn`

Dependencies: steps 1-7

Owner: `Coder`

Notes:

- Completed for local/dev+test environments via `0062_schema.edn`.
- Runtime lookup paths now depend only on `email_lookup_hash`, while explicit delivery/reveal flows resolve from `email_ciphertext`.
- Startup backfill/double-read rollout code was removed as part of the cutover.

## Edge Cases

- Happy path: user registration, login, OAuth sign-in, password reset, tenant invitation acceptance, and admin invitation acceptance all succeed with hash-based identity lookup.
- `nil` input: missing email fails validation before hashing or encryption.
- Empty input: blank email is rejected and never persisted.
- Invalid input: malformed email is rejected using existing format validation.
- Boundary case: two existing rows that differ only by case collide after normalization and must be resolved before unique blind-index constraints are enforced.
- Boundary case: invitation mismatch should still show the exact invited email only after a valid invitation token identifies a specific invitation.
- Boundary case: routine admin surfaces must preserve user-to-tenant and user-to-receipt/expense linkage without exposing email.
- Boundary case: payment-provider reconciliation must work using internal refs even when admins cannot see user email in normal tools.
- Boundary case: exceptional identity reveal must be audited and unavailable from ordinary browsing flows.
- Boundary case: lost encryption keys make email undecryptable; key backup and rotation procedures must exist before production rollout.

## Validation Plan

- Use focused REPL checks for normalization, hashing, encryption, decryption, and masking helpers.
- Run focused backend tests covering:
  - `/Users/enes/Projects/single-tenant-template/test/app/template/backend/auth/service_test.clj`
  - `/Users/enes/Projects/single-tenant-template/test/app/backend/routes/auth_test.clj`
  - `/Users/enes/Projects/single-tenant-template/test/app/backend/routes/admin/auth_test.clj`
  - `/Users/enes/Projects/single-tenant-template/test/app/template/backend/services/invitation_test.clj`
  - `/Users/enes/Projects/single-tenant-template/test/app/admin/backend/services/admin/admin_invitation_test.clj`
  - `/Users/enes/Projects/single-tenant-template/test/app/backend/routes/admin/users_test.clj`
- Completed validation for the cutover with:
  - `app.backend.routes.auth-test` passing after the session payload was updated to preserve resolved user email.
  - `mig/migrate! :dev` and `mig/migrate! :test` both applying `0062_schema.edn` successfully with aligned DB schema.
  - a direct schema check confirming `email` no longer exists on `users`, `admins`, `tenant_invitations`, or `admin_invitations`.
- Add focused tests for pseudonymous admin list/detail payloads and any new exceptional reveal routes.
- Add focused tests for payment-provider account-link reconciliation if that layer is introduced in the same phase.
- Add regression checks that session rows and audit rows no longer persist raw email unless intentionally allowed.
- Save test output once under `/Users/enes/Projects/single-tenant-template/tmp/` when executing verification commands.

## Current Conclusions

- Resolved: normalized email comparison uses a lower-cased canonical form via `app.template.backend.security.email/normalize-email`.
- Resolved: partial substring email search has been removed from routine admin user/admin list contracts; current admin filters search `full_name` and stable refs/ids, not email substrings.
- Resolved: invitation mismatch errors are allowed to show the exact invited email after the invitation token identifies a concrete invitation.
- Resolved: billing integrations now use a generic payment-provider abstraction via `payment_provider_account_links` with `account_kind`, `account_id`, `provider`, and `provider_customer_ref`.
- Current behavior differs from the earlier assumption: routine admin surfaces do not expose full email, but they still expose masked email hints (`email-masked`) on current admin tables and membership views.
- Resolved: `full_name` remains the active product field; no nickname implementation was found in application code, only in the candidate Allium spec.
- Resolved as follow-up work: key rotation is not implemented yet beyond write-time stamping with `EMAIL_PRIVACY_KEY_VERSION`; decryption currently uses a single active key, so true multi-key rotation/re-encryption remains post-cutover work.
