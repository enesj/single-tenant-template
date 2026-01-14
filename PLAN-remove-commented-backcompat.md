# Plan: Remove Commented Code (reader-discard) and Backward-Compatibility Code

## Goals
- Remove all commented-out code (reader-discard) and refactor-era backward-compatibility code from the repo.
- Preserve current runtime behavior for the intended (post-refactor) APIs and flows.
- Keep the codebase consistent with repo conventions and docs.

## Non-Goals
- No feature work or behavior changes beyond cleanup.
- No Python scripts (Babashka/Bash only).

## Phase 0: Prep and Scope
- [ ] Confirm the exact scope with stakeholders (entire repo vs specific packages/modules).
- [ ] Define a cutoff for “backward compatibility” (e.g., anything labeled legacy/v1/compat after refactor date).
- [ ] Decide if any compatibility surfaces must remain for external integrations.

## Phase 1: Inventory and Classification
- [ ] Build an inventory of commented-out code (reader-discard) and compatibility surfaces.
- [ ] Maintain a tracking table (file/line, type, note, risk, action).
- [ ] Tag each item with category and risk:
  - **Category**: commented-out code, deprecated endpoint/handler, shim/adapter, legacy config, backward-compatible data shape, old namespace alias, old API route, migration helper.
  - **Risk**: low (unused), medium (internal call sites), high (external API or data contract).

Real-world examples to look for (patterns):
- Commented-out code:
  - `(comment (defn old-handler ...))`
  - `(comment (let [legacy-user ...] ...))`
- Backward compatibility shims/adapters:
  - Functions named `legacy-*`, `compat-*`, `*_v1` or `*_v2` kept for old call sites.
  - Route handlers that accept old params or return old response shapes.
- Legacy configs:
  - Old EDN keys kept around for fallback reads.
- Admin panel examples (common in this repo layout):
  - Reader-discarded blocks in `src/app/admin/frontend/pages/*.cljs` after UI refactors.
  - Legacy admin API handlers under `src/app/admin/backend/handlers/` or `services/`.
  - Old settings EDN keys in `src/app/admin/frontend/config/*.edn` or `src/app/domain/**/config/*.edn`.
- Template/domain examples:
  - Template shared adapters in `src/app/template/frontend/adapters/` kept for old shapes.
  - Domain-specific compatibility code in `src/app/domain/**/backend/` services.

Initial scan hits (Warp Grep):
- `src/app/template/protocols.clj` — multiple reader-discarded protocol methods (e.g., `initialize`, session helpers, CRUD helpers).
- `src/app/template/backend/routes.clj` — reader-macro based unused-var suppression on `app-routes`.
- `src/app/template/backend/auth/service.clj` — reader-discarded `initialize` method in `AuthenticationService`.
- `test/app/template/frontend/auto_test_data.cljs` — “Re-exports for Backward Compatibility” section plus reader-macro based unused-var ignores.
- `scripts/bb/code_quality/check_unused_reframe/comment_out.clj` — inserts the reader-discard macro to comment-out re-frame registrations (decide if this script remains).

Suggested inventory commands (Bash/Babashka only):
```bash
# Commented-out code
rg --glob '!vendor/**' "#\_" src test resources config scripts cli-tools

# Backward-compat patterns (tune as needed)
rg --glob '!vendor/**' -n "\b(legacy|deprecated|backward|compat|compatibility|old|v1|v2|shim|fallback|migrate|temp)\b" \
  src test resources config scripts cli-tools

# Deprecated routes / handlers
rg --glob '!vendor/**' -n "/admin/api|/api" src

# Legacy namespaces/aliases
rg --glob '!vendor/**' -n "legacy|deprecated" src
```

- [ ] Capture results in a tracking table (file/line, type, note, risk, action).

## Phase 2: Call-Site and Behavior Analysis
- [ ] For each item, find call sites (or confirm it is truly dead code).
- [ ] Identify any external/public API contracts or data formats affected.
- [ ] Map any compatibility shims to their intended replacements.

Real-world examples to verify:
- Old route kept “just in case”:
  - `/admin/api/users-v1` or `/admin/api/legacy-users`
- Old response key mapping:
  - `{:user_name ...}` returning snake_case for older UI/clients
- Deprecated namespace alias:
  - `(require '[app.admin.frontend.legacy.users :as legacy-users])`
- Admin UI compatibility:
  - Component props accepting both `:entity-spec` and a legacy `:spec` key.
  - Re-frame events handling both `:admin/old-event` and `:admin/new-event`.
- Backend service compatibility:
  - Service functions normalizing both `:user_id` and `:user-id`.
  - API handlers accepting `page_size` and `per-page` simultaneously.

Suggested follow-up commands:
```bash
# Find usages of specific symbols or routes
rg --glob '!vendor/**' -n "<symbol-or-route>" src test

# Check for EDN config or migration references
rg --glob '!vendor/**' -n "<config-key-or-migration-helper>" resources config
```

## Phase 3: Removal Plan (Ordered by Risk)
- [ ] **Low-risk**: delete commented-out code blocks and unused functions
  - Remove reader-discarded blocks and any now-dead requires/imports.
- [ ] **Medium-risk**: remove internal-only compatibility shims
  - Update all internal call sites to the new APIs.
- [ ] **High-risk**: external-facing compatibility
  - Verify no clients depend on old routes/params/response shape.
  - If unsure, add temporary logging to confirm zero usage before removal.

Concrete examples by risk:
- Low-risk:
  - Reader-discarded blocks around old UI markup in `src/app/admin/frontend/pages/dashboard.cljs`.
  - Dead helpers in `src/app/shared/**` not referenced anywhere (confirm with `rg`).
- Medium-risk:
  - Adapter functions in `src/app/template/frontend/adapters/` that map old keys.
  - Legacy config resolvers in `src/app/template/frontend/settings/`.
- High-risk:
  - `/admin/api/*` routes that accept old params.
  - Response mappers that emit old keys for external integrations.

## Phase 4: Implementation (Phase-by-Phase)
- [ ] Remove commented-out code in small batches to keep diffs reviewable.
- [ ] Remove compatibility layers and update all call sites.
- [ ] Delete dead routes/handlers and their related tests.
- [ ] Clean unused requires, aliases, and doc comments.
- [ ] Update docs if they mention legacy paths or compatibility modes.

## Tooling Analysis (Implementation)
- **Morph MCP (Warp Grep)**: Primary discovery tool for locating patterns across the codebase and docs (inventory + call-site analysis). Use for broad searches before edits.
- **clojure-mcp**:
  - `mcp__clojure-mcp__clojure_eval` for backend behavior checks (e.g., confirm updated functions still work).
  - `mcp__clojure-mcp__clojurescript_eval` for frontend behavior checks when removing compat shims.
  - `mcp__clojure-mcp__clojure_edit` for safe structural edits to Clojure files.
- **Bash/Babashka**: Use `rg` for inventory and follow-up searches (no Python).

## Phase 5: Verification
- [ ] Targeted tests only (no full suite). Save output once and reuse.
- [ ] Run relevant FE/BE checks based on touched areas.

Suggested commands:
```bash
# Frontend tests (save output once)
# npm run test:cljs 2>&1 | tee /tmp/fe-test.txt

# Backend tests (save output once)
# bb be-test 2>&1 | tee /tmp/be-test.txt
```

- [ ] Manual smoke checks for any removed routes or UI paths.
- [ ] Confirm no regressions in admin panel and domain features.

## Phase 6: Cleanup and Follow-ups
- [ ] Remove any temporary logging added for usage verification.
- [ ] Update or remove stale documentation references.
- [ ] Ensure no reader-discard remains (re-run inventory command).

## Deliverables
- A PR with deletions and call-site updates.
- Updated docs where needed.
- Test evidence for touched areas.

## Open Questions
- What refactor date/commit defines “backward compatibility” for this cleanup?
- Are there any external integrations that still rely on legacy routes or response shapes?
- Should we preserve any compatibility for a deprecation window?
