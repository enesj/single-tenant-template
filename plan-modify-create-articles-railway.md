# Add `/create-articles-remote-db` (Railway DB) Claude command

## Summary
Create a new Claude command `.claude/commands/create-articles-remote-db.md` modeled on `.claude/commands/create-articles.md`, but intended for **Railway Postgres** via `railway run` (uses `DATABASE_URL`). Keep the remote workflow **create + map only** (no receipt re-OCR, no alias deletions). To make this work, extend the existing Babashka scripts to support a `prod` profile and to **parse `DATABASE_URL`** into `psql` connection settings.

## Implementation Changes
### 1) New Claude command file
- Add `.claude/commands/create-articles-remote-db.md` with:
  - Frontmatter `description` + `metadata.tags` similar to `create-articles.md`.
  - A big “REMOTE/PROD DB” warning and prerequisites:
    - Railway CLI installed + `railway login` + `railway link`
    - `bb` + `psql` installed locally (scripts shell out to `psql`)
  - A “How to run against Railway” section:
    - All DB-touching commands are run as `railway run bb ... prod ...`
    - Explicitly warn **not** to print `DATABASE_URL` or paste it into chat/logs.
  - Phases mirroring the core of `create-articles.md` (but scoped):
    - Phase 0: triage backlog on remote (`unmapped_aliases_counts`, `list_unmapped_aliases`, save artifacts under `tmp/`)
    - Phase 1: variant risk grouping on remote (`group-aliases-by-brand ... --json` into `tmp/phase1-variant-groups.json`)
    - Phase 2: list categories on remote (`list_categories`)
    - Phase 3: web research locally (`bb serper-search ...`)
    - Phase 4: create articles on remote (`create_articles.clj prod`), **require `--dry-run` first**, then run without dry-run
    - Phase 5: map aliases on remote (`map_aliases.clj prod`), **require `--dry-run` first**, then run without dry-run
    - Phase 6: verify progress on remote (`report_progress.clj prod` to `tmp/`)
  - Explicitly exclude (or mark “out of scope on remote-db command”):
    - Receipt `review_required` retry REPL flow
    - `delete_unmapped_aliases.clj` noise cleanup
  - Output contract + response-size rules copied/adapted from `create-articles.md`.

### 2) Make bb scripts support `prod` + Railway `DATABASE_URL`
- Update `scripts/bb/articles/db.clj`:
  - Extend `parse-profile` to accept `prod` and `--prod` (still default `:dev`).
  - Add `parse-database-url` helper:
    - Accept `postgres://...`, `postgresql://...`, and `jdbc:postgresql://...` (strip `jdbc:`).
    - Extract `host`, `port` (default 5432), `dbname`, `user`, `password`.
    - Extract `sslmode` from query params when present.
  - Ensure scripts can connect in `:prod` even when only `DATABASE_URL` is present:
    - In `read-config` (or a new internal post-process step), when `profile = :prod` and env `DATABASE_URL` exists, merge parsed values into `(:database config)` so existing scripts keep working unchanged.
  - Update `run-psql` to support SSL:
    - If db map contains `:sslmode`, set env `PGSSLMODE` accordingly.
    - Keep using `PGPASSWORD` env var (do not put password on the command line).

- Update `scripts/bb/expenses/group_aliases_by_brand.clj`:
  - Accept `prod` / `--prod` in arg parsing.
  - Reuse `articles.db` for DB config + querying (preferred):
    - Replace its custom `aero` + `psql` header parsing with `articles.db/query` so `DATABASE_URL` support is centralized.
  - Keep output format/semantics identical (JSON mode and human-readable mode).

- Update usage text in the scripts referenced by the new command to say `[dev|test|prod]` where applicable (so the docs match reality).

## Test Plan
- Unit tests (no real DB, no secrets):
  - Add `clojure.test` coverage for `articles.db/parse-database-url` with dummy URLs:
    - With/without port, with `sslmode=require`, with percent-encoded password.
  - Test `parse-profile` accepts `prod` / `--prod` and preserves existing dev/test behavior.
- Local smoke (dev DB):
  - Run 2–3 existing scripts with `dev` profile to ensure no regressions (e.g. `list_categories`, `unmapped_aliases_counts`).
- Railway smoke (manual, after implementation):
  - `railway run bb scripts/bb/articles/list_categories.clj prod --pretty` returns data and clearly isn’t hitting localhost.
  - Run `create_articles.clj prod --dry-run` and `map_aliases.clj prod --dry-run` with known-safe inputs to confirm connectivity without writes.

## Assumptions
- Railway provides a standard `DATABASE_URL` containing user/password/host/port/dbname, and the DB is reachable from the machine running `railway run`.
- `psql` is available on the operator’s machine (scripts require it).
- The intended “Railway DB” target is the app’s production Postgres, so we standardize on the `prod` profile name for remote runs.