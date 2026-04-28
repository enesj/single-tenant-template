<!-- ai: {:tags [:operations :security :email-privacy] :kind :runbook} -->

# Email and Privacy Subject Key Management & Rotation Runbook

This runbook documents how to operate the current email privacy design safely, how operational privacy subject refs are keyed, and what to do if privacy keys ever need to change.

## Current Design

The email privacy implementation lives in `src/app/template/backend/security/email.clj`.

Today the system:

- normalizes email with `normalize-email`
- computes a blind lookup hash with `email->lookup-hash`
- encrypts email ciphertext with `encrypt-email`
- stamps each write with `email_key_version` via `current-key-version`
- decrypts ciphertext with the key matching the row `email_key_version` when that version is configured

Important operational constraint:

> Changing email privacy keys is a planned migration, not routine config maintenance.

`email_key_version` is stored on rows and is now used by decryption. This means encryption-key compatibility can be deployed before a rotation: old rows can continue to decrypt with their stored version while new writes use the active version.

Operational expense/receipt ownership now also uses `src/app/template/backend/security/privacy_subject.clj`. New operational rows store deterministic HMAC subject refs in columns such as `subject_ref` instead of direct `users.id` links. Those refs are derived with `PRIVACY_SUBJECT_KEY_B64`; the mapping is not stored in the database.

## Which Keys Exist

The current email privacy implementation uses these environment variables:

- `EMAIL_PRIVACY_ENCRYPTION_KEY_B64`
- `EMAIL_PRIVACY_ENCRYPTION_KEYRING_B64`
- `EMAIL_PRIVACY_ENCRYPTION_KEY_<VERSION>_B64`
- `EMAIL_PRIVACY_LOOKUP_KEY_B64`
- `EMAIL_PRIVACY_KEY_VERSION`
- `PRIVACY_SUBJECT_KEY_B64`

Their roles are different:

- `EMAIL_PRIVACY_ENCRYPTION_KEY_B64` encrypts and decrypts `email_ciphertext`
- `EMAIL_PRIVACY_ENCRYPTION_KEYRING_B64` optionally provides retired encryption read keys as comma-separated `version:base64-key` entries, for example `v1:...base64...,v2:...base64...`
- `EMAIL_PRIVACY_ENCRYPTION_KEY_<VERSION>_B64` optionally provides a single per-version encryption key, for example `EMAIL_PRIVACY_ENCRYPTION_KEY_V1_B64`
- `EMAIL_PRIVACY_LOOKUP_KEY_B64` computes `email_lookup_hash` for auth, password reset, invitation matching, and duplicate checks
- `EMAIL_PRIVACY_KEY_VERSION` labels newly written rows with a version such as `v1`
- `PRIVACY_SUBJECT_KEY_B64` computes pseudonymous operational `subject_ref` values for user-owned expenses, receipts, and user expense settings

All base64 key values should decode to at least 32 bytes.

Encryption key lookup order for a requested version is:

1. per-version env var, e.g. `EMAIL_PRIVACY_ENCRYPTION_KEY_V1_B64`
2. `EMAIL_PRIVACY_ENCRYPTION_KEYRING_B64`
3. `EMAIL_PRIVACY_ENCRYPTION_KEY_B64` when the requested version is the active `EMAIL_PRIVACY_KEY_VERSION`
4. bundled development default key only for `:dev`, `:local`, and `:test` profiles

Privacy subject refs currently have one active key. There is no keyring/version column for `subject_ref` yet, so changing `PRIVACY_SUBJECT_KEY_B64` requires a planned operational-data backfill/cutover before old subject refs can be matched with the new key.

## Safe Current Operating Mode

The current implementation is production-acceptable only in **stable-key mode**.

That means:

- keep encryption and lookup keys stable across deploys
- keep `PRIVACY_SUBJECT_KEY_B64` stable across deploys
- back up all privacy keys securely
- do not rotate any privacy key casually
- treat any key change as an application plus data migration

Encryption-key multi-key read support now exists. A rotation still requires a planned rollout and backfill; do not remove old keys until all rows have been re-encrypted and verified.

Privacy-subject key rotation support does not yet exist. Treat `PRIVACY_SUBJECT_KEY_B64` like a long-lived operational identity secret until a dedicated subject-ref key-version migration is designed.

## Key Change Categories

### Encryption key only

This is the simpler future migration.

Impact:

- affects `email_ciphertext`
- affects explicit email reveal/delivery/session hydration paths
- does **not** directly change equality lookup contracts

### Lookup key

This is the harder future migration.

Impact:

- affects `email_lookup_hash`
- affects login, password reset, admin auth, invitation checks, and duplicate detection
- cannot be safely rotated today by changing an environment variable and redeploying

### Privacy subject key

This is an operational relationship key.

Impact:

- affects `subject_ref` and `created_by_subject_ref` ownership matching
- affects user-scoped expense/receipt/settings/report visibility
- affects future backfill/cutover of legacy `user_id` rows
- cannot be safely rotated today by changing an environment variable and redeploying

## Operational Subject-Ref Backfill/Cutover

Legacy deployments that have not yet applied the subject-ref cutover and schema cleanup may still contain direct operational links such as `expenses.user_id`, `receipts.user_id`, and `user_expense_settings.user_id`.

Use the app-level backfill command, not DB-only SQL, because subject refs require the application secret:

```bash
bb privacy-subject-backfill dev --cutover --limit 100 --pretty
```

The command is dry-run by default and writes an EDN report under `tmp/`. Review the report before applying.

For a live cutover, first confirm that `PRIVACY_SUBJECT_KEY_B64` is stable for the target environment and that a recent database backup exists, then run with explicit apply flags:

```bash
railway run bb privacy-subject-backfill prod --cutover --apply --pretty
```

After the cutover apply finishes, run the read-only completion gate in the same target environment. It exits non-zero if any direct operational user links or missing subject refs remain:

```bash
railway run bb privacy-subject-backfill prod --check-complete --pretty
```

Notes:

- `--cutover` fills missing subject refs and nulls direct operational `users.id` links only when the matching subject ref exists or can be computed.
- `--check-complete` performs no writes; use it before applying the schema migration that drops direct-link columns.
- Without `--cutover`, `--apply` fills subject-ref columns but leaves direct legacy links in place.
- Use `--limit N` for batched operation.
- Production execution should happen through the deployment environment so the command receives the same platform-managed secrets as the app.
- App-level legacy read fallbacks have been removed.
- In the current schema, migration `0074_schema.edn` removes `expenses.user_id`, `expenses.created_by`, `receipts.user_id`, `receipts.created_by`, and `user_expense_settings.user_id` after cutover verification. Do not apply that migration to any environment until `--check-complete` reports `:complete? true` and `:remaining-link-count 0` there.

## Required Future Work Before Any Key Rotation

Before rotating keys in production, confirm compatibility support is deployed.

Minimum required changes:

1. Multi-key decryption support for ciphertext using stored `email_key_version` (implemented)
2. Write-path support for a new active version (implemented via `EMAIL_PRIVACY_KEY_VERSION`)
3. A controlled re-encryption backfill process for old rows
4. A verified procedure for retiring old keys after migration completion

For lookup-key rotation, additional work is required:

1. Lookup-key versioning support or dual-hash transition support
2. Controlled backfill/re-hash process for all protected rows
3. Read-side transition logic so auth/reset/invitation flows continue to work during migration

For privacy-subject key rotation, additional work is required:

1. Subject-ref key versioning or dual-subject transition support
2. Controlled backfill of operational rows using the application secret, not DB-only SQL
3. Read-side transition logic so subject-owned and legacy rows remain visible during migration
4. A verified cutover plan before nulling/removing legacy direct operational user links

## Migration Procedure: Encryption Key Rotation

Use this with the multi-key decryption support in `email.clj`.

### Phase 1: Ship compatibility code

Deploy an application release that can:

- encrypt new writes with the newest key/version
- decrypt old rows with the key matching their stored `email_key_version`
- continue reading both old and new ciphertext safely

Do **not** change production secrets before this release is live.

### Phase 2: Introduce new active write key

Update runtime secrets so new writes use:

- new `EMAIL_PRIVACY_ENCRYPTION_KEY_B64`
- new `EMAIL_PRIVACY_KEY_VERSION`

The app must still retain access to the old encryption key for reads.

### Phase 3: Re-encrypt old rows in batches

Run a controlled backfill job that:

- loads rows using the old version
- decrypts with the matching old key
- re-encrypts with the new key
- updates:
  - `email_ciphertext`
  - `email_key_version`

Protected tables currently in scope:

- `users`
- `admins`
- `tenant_invitations`
- `admin_invitations`

### Phase 4: Verify completion

Verify that:

- no rows remain on the old `email_key_version`
- auth and invitation flows still work
- explicit reveal/delivery paths still decrypt correctly

### Phase 5: Retire old key

Only after all rows are migrated and verified should old-key read support be removed.

## Migration Procedure: Lookup Key Rotation

This is more sensitive because auth and invitation behavior depend on `email_lookup_hash`.

### Phase 1: Ship lookup transition support

Before any secret change, deploy application support for one of these strategies:

- lookup-key versioning
- dual-hash storage during transition
- multi-version read logic for hash-based matching

Without that support, rotating `EMAIL_PRIVACY_LOOKUP_KEY_B64` can break:

- user login
- admin login
- password reset principal lookup
- invitation duplicate checks
- existing-member checks

### Phase 2: Backfill new lookup hashes

Run a controlled re-hash job that:

- decrypts or otherwise resolves the canonical email
- computes the new lookup hash with the new lookup key
- writes the new hash/version fields needed by the transition design

### Phase 3: Cut reads to the new hash path

After backfill and verification, switch lookup behavior to the new hash version.

### Phase 4: Retire old lookup support

Only after the entire dataset and all runtime reads are verified should old lookup-key support be removed.

## Rollback Strategy

If a key migration fails mid-flight:

- stop the backfill job
- keep compatibility code deployed
- keep old keys available
- revert write-side active version if necessary
- do **not** remove old-key read support until migration completion is confirmed

Compatibility code is what makes rollback possible.

## Verification Checklist

Before declaring a key migration complete, verify:

- application boots with the intended key configuration
- login still works for existing users
- admin auth still works
- password reset lookup still works
- tenant invitation accept flow still works
- admin invitation accept flow still works
- explicit email reveal paths still work
- no rows remain on retired key versions

Use the repo's standard migration workflow via `app.template.backend.migrations.simple-repl` for any schema-affecting transition support, and validate changes in both dev and test before production rollout.

## Where to Keep the Keys

### Production

Best practice: keep these values only in your platform secret manager or cloud secret store.

Good options:

- Railway service/environment variables
- 1Password Secrets Automation
- AWS Secrets Manager
- Google Secret Manager
- Azure Key Vault
- Doppler
- HashiCorp Vault

Recommended production posture:

- store `EMAIL_PRIVACY_ENCRYPTION_KEY_B64`, `EMAIL_PRIVACY_LOOKUP_KEY_B64`, and `PRIVACY_SUBJECT_KEY_B64` as platform-managed secrets
- restrict access to the smallest possible operator set
- enable audit logs for secret access/changes
- back up the values in a second secure recovery channel controlled by trusted operators
- document ownership and emergency recovery procedure

Do **not** store production values in:

- committed files
- `config/base.edn`
- `resources/`
- docs
- tickets/chat logs

### Local development

Use local environment variables or a gitignored local secret mechanism.

Bundled development defaults are available only when the active profile is `:dev`, `:local`, or `:test`. Any other profile, including `:staging`, must provide explicit email privacy keys and `PRIVACY_SUBJECT_KEY_B64`; the app will fail fast if they are absent.

Acceptable local options:

- shell environment variables
- a gitignored `.env`
- a local secret manager integration

Do not commit local key material.

## Operational Rule for the Team

Use this wording in runbooks and onboarding docs:

> Email privacy and privacy-subject keys are long-lived secrets. Changing them requires a staged application/data migration. Do not rotate them as routine config maintenance.

## Related Files

- `src/app/template/backend/security/email.clj`
- `src/app/template/backend/security/privacy_subject.clj`
- `src/app/template/backend/auth/service.clj`
- `src/app/template/backend/auth/password_reset.clj`
- `src/app/template/backend/services/invitation.clj`
- `src/app/admin/backend/services/admin/auth.clj`
- `src/app/admin/backend/services/admin/admin_invitation.clj`
- `src/app/template/backend/migrations/simple_repl.clj`
- `resources/db/migrations/0062_schema.edn`
