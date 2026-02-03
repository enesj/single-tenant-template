# Dead Code Report (Feb 3, 2026)

Scope: review for dead/unused code introduced by today’s changes.

## Committed changes (commit `3584e58`)

### Confirmed dead code (unreferenced in `src/`)

These symbols exist only as definitions and have no call sites in application code.

- `src/app/template/frontend/shared/bridges/crud.cljs:183` `default-delete-failure`
  - Evidence: only occurrence in `src/` is the `defn` line; delete flows now run via `:batch-delete` and `default-batch-delete-*`.
- `src/app/template/frontend/shared/bridges/crud.cljs:217` `default-delete-request`
  - Evidence: only occurrence in `src/` is the `defn` line; `delete-entity` now routes through batch delete (`batch-delete-entities`).

### Effectively dead (only reachable from dead code above)

These functions still exist, but no code path reaches them anymore because the only remaining call sites are inside `default-delete-request` (which is dead).

- `src/app/template/frontend/api/http.cljs:129` `delete-entity-public`
- `src/app/template/frontend/api/http.cljs:163` `delete-entity-admin`
- `src/app/template/frontend/api/http.cljs:190` `delete-entity`
  - Note: tests may still reference these functions directly; this section is about runtime reachability from current UI/event flows.

### Notes / recommendations

- If you want to keep backwards compatibility, consider marking the legacy single-delete helpers as deprecated (docstring) rather than deleting immediately.
- If you prefer cleanup, remove `default-delete-request` and `default-delete-failure`, then decide whether to keep or delete `delete-entity*` in `src/app/template/frontend/api/http.cljs` (and update tests accordingly).

## Pending review

- Staged/uncommitted changes (GLM OCR integration + OCR settings changes).
- New/untracked files (GLM OCR namespaces + migration `0039_schema.edn` + `worktrees/`).

