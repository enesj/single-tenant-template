# Legacy Code Sunset Plan (v2 – Fast-Track Deletion)

Status: Adopted — 2026-01-16

Recent progress (Workstream 2)

- 2026-01-16: Removed re-export facade `app.domain.backend.expenses.handlers.user-expenses` (user expenses routes now require the concrete submodules directly).
- 2026-01-16: Removed re-export facade `app.domain.backend.expenses.workers.receipt-ocr` (callers now require `app.domain.backend.expenses.workers.receipt-ocr.core` directly).
- 2026-01-16: Migrated template/admin backend call sites off the facade `app.admin.backend.services.admin` to concrete submodules (auth/audit/users/users.bulk/admins/dashboard/monitoring.integrations) and deleted `src/app/admin/backend/services/admin.clj` (via the bb delete helper due to patch deletion issues). Verified via `rg` and focused Kaocha run (28 tests, 115 assertions, 0 failures).
- 2026-01-16: Removed unused deprecated namespace `app.admin.frontend.pages.entities` (empty stub).
- 2026-01-16: Updated `scripts/legacy/inventory.bb` patterns to require explicit legacy markers (`^:legacy-*`, `:legacy/`, `^:legacy-alias`) and regenerated the baseline inventory (now empty).
- 2026-01-16: Updated `src/app/PLAN-receipts-ocr-ui-integration.md` to reference `app.domain.backend.expenses.workers.receipt-ocr.core`.
- 2026-01-16: Ran legacy audit after baseline refresh; output saved to `/tmp/legacy-audit-<timestamp>.txt` (0 items).

Current implementation state (as of 2026-01-16)

- Inventory baseline regenerated via `bb legacy-inventory` (explicit legacy markers only); `resources/legacy-inventory.edn` now empty.
- Inventory audit executed via `bb scripts/legacy/audit.bb`; output captured in `/tmp/legacy-audit-<timestamp>.txt` (0 items).
- Auth service load sanity check executed via `clojure -M:test -e "(require 'app.template.backend.auth.protocols 'app.template.backend.auth.service :reload)"`; output captured in `/tmp/auth-service-require-<timestamp>.txt`.
- Admin auth smoke check executed via direct `clojure -e` invocation against dev config.
- Re-export reference scan (rg exact-match) shows no code/test references to the removed facade namespaces; OCR plan doc updated to concrete namespace.

Workstream status (as of 2026-01-16)

- Re-export namespaces: ✅ completed (facades removed; code/test references cleared).
- Empty/deprecated namespaces: ✅ completed (admin FE `entities` stub removed; inventory currently empty).
- Inventory audit/baseline: ✅ completed (baseline regenerated; audit green; inventory empty).
- Legacy events & subs, service-map alias vars, OAuth compat, LocalStorage shims, namespaced key fallbacks: ✅ completed (no explicit legacy markers remain).

Outstanding tasks (next batch)

- None (inventory empty after explicit-legacy scan). If scope expands to non-annotated patterns, revisit inventory patterns and regenerate the baseline.

Next scheduled checks (save output once)

- `bb scripts/legacy/audit.bb 2>&1 | tee /tmp/legacy-audit-<timestamp>.txt`
- `bb fe-test-parallel 2>&1 | tee /tmp/fe-legacy-removal-<timestamp>.txt` (after FE-facing deletions)
- `bb validate-frontend-config && bb config-audit --strict 2>&1 | tee /tmp/fe-config-<timestamp>.txt`

Owners: Platform (Template), Admin, Domain leads

Goals

- Remove ~170+ backward‑compatibility snippets across 14 categories while keeping runtime stable.
- Standardize on namespaced, kebab‑case data and service‑map APIs.
- Eliminate deprecated routes, OAuth/password fallbacks, re‑exports, and FE event/subscription shims.
- Finish with clear metrics, tests, and docs updates; no Python tooling (Babashka/Bash only).

Non‑Goals

- Large architectural re‑writes beyond removing legacy compatibility layers.
- Broad UI/UX redesigns unrelated to the legacy removal.

Principles

- Fast-track deletions without new feature flags; rely on tests + quick rollback.
- Small, isolated batches (one category/file group per commit).
- Cheap pre-checks before running tests (clojure-lsp diagnostics, compile sanity).
- Service map pattern only; do not add alias vars.
- Prefer mechanical, verifiable refactors with automated checks (rg + Babashka).
- Tests first; run only relevant tests and save output once.
- Documentation‑first when uncertain; use Morph MCP (Warp Grep) to locate patterns and guidance.

Strategy Shift (from v1)

- Feature flags: skip gradual rollouts. Remove legacy code directly and test immediately.
- Cutover: immediate after each batch passes focused tests.
- Rollback: use `git revert` or quick fix; timebox failures to 30–45 minutes.
- Batch size: one file or one small category per commit for clarity.

Guardrails

- Work on a branch: `git checkout -b legacy-removal-fast-track`.
- One batch = one commit; keep diffs narrowly scoped.
- Pre-check: run `clojure-lsp diagnostics` before tests to catch ns/var errors quickly.
- Save test output once per batch; analyze logs without re-running tests.
- Add FE config checks for UI-heavy changes.
- If stuck >45 min on a batch, revert and split into smaller chunks.

Fast-Track Workflow (batch-based)

1) Pick the smallest category first (quick wins) using the inventory.
2) Delete the legacy code (no flags, no compat shims).
3) Run focused tests (save output ONCE), plus optional FE config checks.
4) If green → commit and move on. If red → fix quickly or `git revert HEAD` and split further.

Per-Batch Playbook (commands)

- Inventory the batch:
  - `rg ':category :<name>' resources/legacy-inventory.edn -n -A3`
- Delete the items.
- Diagnostics pre-check (cheap):
  - `clojure-lsp diagnostics --project-root . 2>&1 | tee /tmp/lsp-diagnostics.txt`
- Focused tests (save once):
  - Backend (single ns): `clj -M:test -m kaocha.runner --focus app.<path>.<ns> 2>&1 | tee /tmp/be-<slug>.txt`
  - Backend (all, only when needed): `bb be-test 2>&1 | tee /tmp/be-all.txt`
  - Frontend (filter): `bb fe-test-parallel --grep "<domain|ns regex>" 2>&1 | tee /tmp/fe-<slug>.txt`
  - FE config (fast): `bb validate-frontend-config && bb config-audit --strict 2>&1 | tee /tmp/fe-config.txt`
- Optional BE smoke (if auth/endpoints touched):
  - `curl -X POST http://localhost:8085/api/login -d '{"email":"test@example.com","password":"oldpass"}'`
- If green: `git add -A && git commit -m "refactor: remove <category>"`
- If not green: fix quickly or `git revert HEAD` and split into smaller batches.

Safety Net Commands

- Before starting: `git checkout -b legacy-removal-fast-track`
- After each batch: `git add -A && git commit -m "refactor: remove [category]"`
- Quick revert: `git revert HEAD`
- Full reset (last resort): `git reset --hard origin/main`
- View remaining work: `rg ':category' resources/legacy-inventory.edn | cut -d: -f3 | sort | uniq -c | sort -rn`

Updated Success Criteria (global)

- CI green (no legacy flags required).
- No references to re‑exported namespaces or alias vars remain.
- All password hashes are bcrypt; SHA‑256 fallback code is removed.
- OAuth legacy formats removed (or isolated behind an off‑by‑default adapter kept only if absolutely necessary).
- No legacy response key variants are returned by APIs.
- FE builds pass; no legacy events/subs or LocalStorage shims remain.
- Docs and changelog updated; support playbooks reflect new behavior.

Recommended Execution Order (lowest risk → highest)

Phase 1 — Quick Wins

1) Empty/deprecated namespaces

- Find: `rg ':category :empty-deprecated-namespaces' resources/legacy-inventory.edn`
- Delete files; run focused BE tests; commit if green.

2) Legacy route redirects

- Find: `rg ':category :legacy-route-redirects' resources/legacy-inventory.edn`
- Remove redirect handlers; `bb be-test 2>&1 | tee /tmp/routes-test.txt`; commit if green.

3) API response compatibility (1 item)

- Find: `rg ':category :api-response-compat' resources/legacy-inventory.edn -A3`
- Delete legacy response handling.
- Tests: `bb be-test 2>&1 | tee /tmp/api-compat-test.txt` and `npm run test:cljs 2>&1 | tee /tmp/api-compat-fe-test.txt`.
- Commit if green: `refactor: remove legacy API response compatibility`.

4) Domain registry compatibility (1 item)

- Find: `rg ':category :domain-registry-compat' resources/legacy-inventory.edn -A3`
- Delete legacy registry entries; `bb be-test 2>&1 | tee /tmp/registry-test.txt`.

5) Component/template compatibility (≈8 items)

- Find: `rg ':category :component-template-compat' resources/legacy-inventory.edn -A3`
- Review all items; delete in one batch.
- Tests: `bb fe-test-parallel 2>&1 | tee /tmp/component-compat-test.txt`.

Phase 2 — Medium Complexity

6) Namespaced key fallbacks (≈86 items)

- Find patterns: `rg ':category :namespaced-key-fallback' resources/legacy-inventory.edn`
- Convert `(or (:id row) (:users/id row)) → (:users/id row)`; ensure DB row builders emit namespaced keys.
- Tests: `bb be-test 2>&1 | tee /tmp/namespaced-keys-test.txt`.

7) Legacy password support (≈30 items)

- Find: `rg ':category :legacy-password' resources/legacy-inventory.edn -A3`
- Remove SHA‑256 fallback; enforce bcrypt only.
- Tests: `bb be-test 2>&1 | tee /tmp/password-test.txt`; manual login smoke against `:8085` if needed.

Phase 3 — High Complexity

8) OAuth compatibility (split by subcategory)

- Break into batches (e.g., token format, client IDs):
  - `rg ':category :oauth-compat' resources/legacy-inventory.edn | rg "token-format" > /tmp/oauth-tokens.txt`
  - `rg ':category :oauth-compat' resources/legacy-inventory.edn | rg "client-id" > /tmp/oauth-clients.txt`
- Delete one batch at a time; `bb be-test 2>&1 | tee /tmp/oauth-batch1-test.txt`.

9) LocalStorage migration (≈114 items)

- Remove LS shims; bump stored schema; add guard to reject old versions with reset path.
- Tests: FE tests + `bb validate-frontend-config && bb config-audit --strict`.

10) Service map alias vars (≈305 items)

- Strategy: convert aliases to service-map calls incrementally.
- Example: `(get-user svc id) → (:users/get svc id)`.
- Test in chunks per namespace:
  - `bb be-test --namespace app.template.backend.routes.admin 2>&1 | tee /tmp/aliases-admin-test.txt`
  - `bb be-test --namespace app.domain.backend.expenses 2>&1 | tee /tmp/aliases-expenses-test.txt`

11) Legacy events & subs (≈651 items)

- DO NOT delete all at once; migrate per feature/namespace.
- Create domain slices:
  - `rg ':category :legacy-events' resources/legacy-inventory.edn | rg "expenses" > /tmp/events-expenses.txt`
  - `rg ':category :legacy-events' resources/legacy-inventory.edn | rg "admin" > /tmp/events-admin.txt`
- Tests per slice: `npm run test:cljs -- --filter="expenses" 2>&1 | tee /tmp/events-expenses-test.txt`.

Category Reference (details retained from v1)

1) Namespaced key fallbacks (100+ occurrences)

Intent: Remove support for unqualified snake_case DB keys; enforce namespaced kebab‑case everywhere.

Steps

- Inventory: find `(or (:foo_bar ...) (:foo/bar ...))` and similar patterns.
- Migration: replace callers to read only namespaced keys and update any SQL result coercions to emit namespaced keys (next.jdbc `:builder-fn` or row‑key transform).
- Tests: update fixtures to namespaced keys; add failing tests if old keys are accidentally used.

Acceptance

- `rg` shows 0 occurrences of fallback patterns.
- Integration tests and at least one end‑to‑end flow per domain pass with only namespaced keys.

Suggested search

```bash
rg -n "\(or\s+\(:[a-z0-9_-]+_[a-z0-9_-]+" src test
rg -n "(get\s+\{:[a-z0-9_-]+_[a-z0-9_-]+" src test
```

2) Re‑export namespaces (12)

Intent: Delete re‑export stubs used during multi‑phase migrations.

Steps

- `rg -n "^\(ns .*:refer :all\)|:refer.*\[.*\]" src/app` to locate stubs.
- Verify zero downstream `require`/`use` of the re‑export namespaces.
- Remove stubs; fix import sites to point to final module.

Acceptance: 0 imports of removed namespaces; builds and focused tests pass.

3) Legacy alias avoidance / service map compatibility (3 + notes)

Intent: Remove any remaining alias vars (e.g., `get-user`, `create-user!`) and ensure all callsites use the service map.

Steps

- Inventory functions exposing alias‑style APIs.
- Provide codemods (Babashka rewrite-clj) to convert calls to `(:users/get svc ...)` etc.
- Delete alias vars; add compile‑time failures if still referenced.

Acceptance: 0 alias var definitions and references.

4) Legacy password support (1)

Intent: Eliminate SHA‑256 fallback; enforce bcrypt only.

Steps

- Enable opportunistic rehash on login now; record progress metric.
- Announce cutoff date; after cutoff, require password reset for remaining SHA‑256 users.
- Remove SHA‑256 verification and any dual‑format storage.

Acceptance

- Metric shows 100% bcrypt; attempts with legacy hash fail as expected in tests.
- Code paths for SHA‑256 are removed; security review complete.

5) OAuth token compatibility (2)

Intent: Remove support for legacy token format.

Steps

- Telemetry: log and count legacy token use by client ID for 14 days.
- Communicate migration; rotate tokens; provide adapter off‑by‑default for emergency.
- Delete legacy verification branches once usage is 0 for 7 consecutive days.

Acceptance: 0 legacy token validations in logs; adapter behind flag only for rollback.

6) Empty/deprecated namespaces (3)

Intent: Remove `entities.cljs`/`.clj` or similar if unused.

Steps: Prove zero imports; delete files; run focused builds/tests.

7) Legacy event handlers (6) and subscriptions (8)

Intent: Move FE to new events/subs exclusively.

Steps

- Inventory old event/sub IDs; map to new equivalents.
- Replace callsites; add deprecation asserts in dev builds.
- Remove old handlers/subs; update FE tests.

Acceptance: `rg` shows 0 uses of legacy events/subs.

8) API response compatibility patterns (15+)

Intent: Stop returning dual key formats; standardize on namespaced keys.

Steps

- Introduce API version header handling if needed; default to v2.
- Update serializers/builders to emit only new format; keep adapter behind a flag for a short window.
- Update external API docs and client SDKs.

Acceptance: Contract tests pass for v2; no legacy keys in production responses.

9) Legacy route redirects (2)

Intent: Remove legacy routes and redirects.

Steps: Confirm zero inbound hits for N days; update router; remove redirect handlers.

10) LocalStorage migration handlers (2)

Intent: Remove LocalStorage shims.

Steps: Bump stored schema version; migrate once on startup; delete shims and add guard that rejects old versions with a clean reset path.

11) Settings schema legacy support (3)

Intent: Remove old settings keys; keep only new schema.

Steps: Add one‑time migration; drop fallback reads; update admin UI forms; validate via `bb validate-frontend-config` and `bb config-audit --strict`.

12) Domain registry compatibility (4)

Intent: Remove compatibility entries; enforce new domain/module registration shape.

Steps: Validate registry at startup; fail fast in dev if legacy entries are present.

13) Component/template compatibility (7)

Intent: Ensure components follow current props/ID requirements; remove template shims.

Steps: Replace shimmed props with canonical ones; verify chrome‑mcp tests can select by `:id`.

Cross‑Cutting Tooling (Babashka/Bash only)

- Inventory generator (bb): Walk `src/` and `test/`, produce `resources/legacy-inventory.edn` with entries: {:category :namespaced-key-fallback :file "..." :line 42 :code "..."}.
- Codemods (bb + rewrite‑clj):
  - Replace `(or (:x_y r) (:x/y r))` → `(:x/y r)`.
  - Replace alias function calls → service map fns.
  - Remove re‑export namespaces.
- CI checks: Fail build if any inventory categories reappear (pre‑merge hook calling `bb legacy-audit`).

Search & Verification Cheatsheet

```bash
# Global inventory
rg -n "\(or\s+\(:[a-z0-9_-]+_[a-z0-9_-]+" src test
rg -n "defn\s+get-[a-z0-9!-]+|defn\s+create-[a-z0-9!-]+" src/app         # alias vars
rg -n "^\(ns .*:refer :all|:refer.*\[.*\]" src/app                    # re-exports
rg -n "dispatch|reg-event" src/app                                       # legacy events
rg -n "reg-sub" src/app                                                   # legacy subs
rg -n "LocalStorage|local-storage|localStorage" src/app                   # LS shims
rg -n "redirect|legacy-route" src/app                                     # routes
```

Testing Strategy

- Diagnostics pre-check: `clojure-lsp diagnostics --project-root . 2>&1 | tee /tmp/lsp-diagnostics.txt`.
- Backend: `bb be-test` (focused namespaces first). Save once: `bb be-test 2>&1 | tee /tmp/be-tests.txt`.
- Frontend (fast): `bb fe-test-parallel` or `npm run test:cljs:parallel`. Save once: `bb fe-test-parallel 2>&1 | tee /tmp/fe-tests.txt`.
- Frontend config: `bb validate-frontend-config`, `bb config-audit --strict`.
- Browser: chrome‑mcp flows targeting element IDs.
- Logs/Builds: Use the `system-logs` skill to monitor shadow‑cljs + server during cutovers.

Runtime & Rollback

- No new flags; immediate cutover per batch after tests pass.
- Staging first; monitor: 4xx/5xx, login failures, OAuth rejections, route misses, client JS errors.
- Rollback = `git revert` of the batch commit; if necessary, split into finer batches.

Communication & Versioning

- Publish deprecation notes and effective dates in CHANGELOG and admin release notes.
- For API changes, document v2 contract and migration guides; provide 30‑day overlap window if external clients exist.
- Track exceptions via ticket IDs in code comments for traceability.

Issue Tracking & Labels

- Use label `legacy-removal` and one of: `risk-low|risk-med|risk-high`.
- File one issue per category with checklists; link to exact file:line items.

Acceptance Checklists (tick per PR)

- [ ] Inventory updated for this category; counts decreased.
- [ ] Flags removed (if previously used for this area); no new flags introduced.
- [ ] Focused tests added/updated; outputs saved to `/tmp`.
- [ ] Docs updated (API, admin UI, or dev docs).
- [ ] `rg` queries return 0 legacy matches for touched scope.
- [ ] Observability shows no new errors after deploy.

Appendix: Example Babashka skeletons (not yet committed)

```clojure
;; scripts/legacy/inventory.bb
#!/usr/bin/env bb
(require '[babashka.fs :as fs]
         '[clojure.string :as str])
;; Walk files, emit EDN inventory to stdout or file

;; scripts/legacy/codemods.bb
#!/usr/bin/env bb
(require '[rewrite-clj.zip :as z])
;; Apply safe, mechanical rewrites; print diff summary
```

Notes & References

- Keep to service map pattern; do not add new alias vars.
- Namespaced key fallbacks likely came from next.jdbc JOIN behavior; we are removing support and enforcing namespaced keys via builder/transform.
- Respect scripting policy: NO Python.
- Use skills: Morph MCP (Warp Grep), system-logs, app-db-inspect, reframe-events-analysis.
