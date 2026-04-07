<!-- ai: {:tags [:operations :security :email-privacy] :kind :runbook} -->

# Email Privacy Key Management & Rotation Runbook

This runbook documents how to operate the current email privacy design safely and what to do if email privacy keys ever need to change.

## Current Design

The current implementation lives in `src/app/template/backend/security/email.clj`.

Today the system:

- normalizes email with `normalize-email`
- computes a blind lookup hash with `email->lookup-hash`
- encrypts email ciphertext with `encrypt-email`
- stamps each write with `email_key_version` via `current-key-version`
- decrypts ciphertext with a single active encryption key in `decrypt-email`

Important operational constraint:

> Changing email privacy keys is a planned migration, not routine config maintenance.

At the time of writing, `email_key_version` is stored on rows, but decryption still assumes one active encryption key. That means key changes are not currently safe as a simple secret update.

## Which Keys Exist

The current email privacy implementation uses these environment variables:

- `EMAIL_PRIVACY_ENCRYPTION_KEY_B64`
- `EMAIL_PRIVACY_LOOKUP_KEY_B64`
- `EMAIL_PRIVACY_KEY_VERSION`

Their roles are different:

- `EMAIL_PRIVACY_ENCRYPTION_KEY_B64` encrypts and decrypts `email_ciphertext`
- `EMAIL_PRIVACY_LOOKUP_KEY_B64` computes `email_lookup_hash` for auth, password reset, invitation matching, and duplicate checks
- `EMAIL_PRIVACY_KEY_VERSION` labels newly written rows with a version such as `v1`

## Safe Current Operating Mode

The current implementation is production-acceptable only in **stable-key mode**.

That means:

- keep encryption and lookup keys stable across deploys
- back up both keys securely
- do not rotate either key casually
- treat any key change as an application plus data migration

If you need true key rotation in the future, implement multi-key support first.

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

## Required Future Work Before Any Key Rotation

Before rotating keys in production, the app must first gain compatibility support.

Minimum required changes:

1. Multi-key decryption support for ciphertext using stored `email_key_version`
2. Write-path support for a new active version
3. A controlled re-encryption backfill process for old rows
4. A verified procedure for retiring old keys after migration completion

For lookup-key rotation, additional work is required:

1. Lookup-key versioning support or dual-hash transition support
2. Controlled backfill/re-hash process for all protected rows
3. Read-side transition logic so auth/reset/invitation flows continue to work during migration

## Migration Procedure: Encryption Key Rotation

Use this only after multi-key decryption support has been implemented.

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

- store `EMAIL_PRIVACY_ENCRYPTION_KEY_B64` and `EMAIL_PRIVACY_LOOKUP_KEY_B64` as platform-managed secrets
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

Acceptable local options:

- shell environment variables
- a gitignored `.env`
- a local secret manager integration

Do not commit local key material.

## Operational Rule for the Team

Use this wording in runbooks and onboarding docs:

> Email privacy keys are long-lived secrets. Changing them requires a staged application/data migration. Do not rotate them as routine config maintenance.

## Related Files

- `src/app/template/backend/security/email.clj`
- `src/app/template/backend/auth/service.clj`
- `src/app/template/backend/auth/password_reset.clj`
- `src/app/template/backend/services/invitation.clj`
- `src/app/admin/backend/services/admin/auth.clj`
- `src/app/admin/backend/services/admin/admin_invitation.clj`
- `src/app/template/backend/migrations/simple_repl.clj`
- `resources/db/migrations/0062_schema.edn`
