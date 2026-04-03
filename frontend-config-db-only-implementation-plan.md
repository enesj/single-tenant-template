# Frontend Config DB-Only Implementation Plan

## Goal

Simplify mutable frontend configuration so `view-options`, `form-fields`, and `table-columns` are runtime-managed without relying on source-controlled EDN defaults at request time.

Keep `entities.edn` in place for now, especially admin `entities.edn`, because it still carries preload-time registry metadata and route wiring concerns that are not just mutable settings.

Behavior target: [specs/allium/template/frontend-config-runtime-resolution.candidate.allium](/Users/enes/Projects/single-tenant-template/specs/allium/template/frontend-config-runtime-resolution.candidate.allium)

## Current State

- Mutable config is read through `settings_io.clj`, which currently merges file-backed defaults with runtime overrides.
- `/api/v1/config` still has file-backed fallback behavior for user-facing config.
- Admin `entities.edn` is preloaded at build time and should not be folded into the same migration without a separate design.
- Validation/sync tooling currently assumes mutable EDNs still exist.

## Target State

- `view-options`, `form-fields`, and `table-columns` become DB-backed runtime config channels for `admin` and `user` scopes.
- Defaults for those channels are seeded into the runtime store for new environments.
- Effective reads come from the runtime store, not from reading mutable EDN files on request.
- `entities.edn` remains source-controlled until a separate decision is made.

## Phase 1: Lock Behavior

- Treat the new Allium spec as the behavior contract for mutable config channels.
- Add focused tests around:
  - defaults returned when no runtime snapshot exists
  - latest save wins
  - saved snapshots act as replacement snapshots, including nested-key removal behavior
  - scope isolation between `admin` and `user`

## Phase 2: Introduce Runtime Bootstrap

- Add a bootstrap path that ensures runtime rows exist for every mutable config channel:
  - `admin/view-options`
  - `admin/form-fields`
  - `admin/table-columns`
  - `user/view-options`
  - `user/form-fields`
  - `user/table-columns`
- Seed those rows from the current source-controlled defaults exactly once for fresh environments.
- Decide whether bootstrap happens in:
  - a migration/backfill step, or
  - an explicit startup/bootstrap task

Recommendation: prefer a migration/backfill plus an idempotent verifier.

## Phase 3: Switch Read Path

- Update `src/app/template/backend/routes/admin/settings_io.clj` so mutable config reads no longer depend on request-time EDN loading.
- Update `src/app/template/backend/routes/api.clj` so `/api/v1/config` reads mutable user config from the runtime store directly.
- Preserve a temporary compatibility fallback during rollout only if needed for safety.

## Phase 4: Switch Default Ownership

- After bootstrap is proven in dev and test, stop treating mutable EDNs as operational defaults.
- Replace them with one of:
  - migration-seeded canonical defaults, or
  - code-level seed maps used only for initial bootstrap

Recommendation: code-level seed maps or migration payloads are simpler than keeping dual truth in mutable EDN files.

## Phase 5: Simplify Tooling

- Remove mutable-config handling from:
  - `bb sync-frontend-config`
  - `bb validate-frontend-config`
- Keep tooling support for:
  - `entities.edn`
  - any remaining source-controlled config bundles
- Update docs that still describe mutable settings as file-backed.

## Phase 6: Cleanup

- Remove stale comments and fallback code that assume mutable EDNs are edited at runtime.
- Revisit whether user `entities.edn` should:
  - remain source-controlled, or
  - move into the runtime-managed model as a separate change

Do not fold admin `entities.edn` into this cleanup unless preload/registry behavior is redesigned first.

## Risks

- Fresh-environment bootstrap can fail if runtime rows are missing and reads assume they already exist.
- Mixed-mode rollout can produce confusing precedence if both file defaults and runtime defaults remain active too long.
- Multi-domain user config currently has special fallback behavior; removing file reads needs a deliberate multi-domain story.
- Treating mutable config as DB-only increases the importance of export/backup visibility for operational debugging.

## Validation Checklist

- Dev database bootstraps all six mutable config channels.
- Test database bootstraps all six mutable config channels.
- `/admin/admin-settings` still loads and saves successfully.
- `/admin/user-settings` still loads and saves successfully.
- `/api/v1/config` returns expected user-facing config without file-backed fallback.
- Fresh environment with no manual admin action still renders list pages correctly from seeded defaults.

## Suggested File Sequence

1. `src/app/template/backend/routes/admin/settings_io.clj`
2. `src/app/template/backend/routes/api.clj`
3. bootstrap/migration entrypoint for runtime config seeding
4. focused tests for settings I/O and `/api/v1/config`
5. docs and tooling cleanup
