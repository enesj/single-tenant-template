# Legacy Code Sunset Plan (v1)

Status: Draft for review — 2026-01-16

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

- Incremental, reversible changes behind flags when risk is non‑trivial.
- Service map pattern only; do not add alias vars.
- Prefer mechanical, verifiable refactors with automated checks (rg + Babashka).
- Tests first; run only relevant tests and save output once.
- Documentation‑first when uncertain; use Morph MCP (Warp Grep) to locate patterns and guidance.

Phases & Timeline (suggested)

1) Inventory & Baseline (Week 1)

- Reconfirm counts with Warp Grep and `rg`; export machine‑readable inventory (EDN) per category.
- Establish dashboards: compile success, test pass rates, 4xx/5xx, auth failures, OAuth usage, bcrypt adoption.
- Decide per‑category risk level + gating criteria.

2) Deprecation Markers & Flags (Week 2)

- Add `^:deprecated` metadata or comment tags to legacy points plus TODO ticket IDs.
- Introduce config flags where needed (e.g., `:legacy.oauth.enabled`, `:legacy.passwords.sha256-enabled`, `:api.responses.legacy-keys`).
- Add startup log summary of any legacy flags still enabled.

3) Migrations (Weeks 2–5)

- Execute workstreams (below) in parallel where safe; merge small PRs with targeted tests.
- Keep changes behind flags until each category’s acceptance criteria are met.

4) Cutover (Week 6)

- Turn off legacy flags one by one in staging → production.
- Monitor logs/dashboards; revert flag if regressions are detected.

5) Cleanup & Hard Removal (Week 7)

- Remove dead code paths, flags, and re‑exports once all gates pass.
- Update docs, examples, and onboarding materials.

Definition of Done (global)

- CI green with legacy flags disabled.
- No references to re‑exported namespaces or alias vars remain.
- All password hashes are bcrypt; SHA‑256 fallback code is removed.
- OAuth legacy formats are removed or isolated behind a dedicated, off‑by‑default adapter.
- No legacy response key variants are returned by APIs.
- FE builds pass; no legacy events/subs or LocalStorage shims remain.
- Docs and changelog updated; support playbooks reflect new behavior.

Workstreams (by category)

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

- Backend: `bb be-test` (focused namespaces first). Save once: `bb be-test 2>&1 | tee /tmp/be-tests.txt`.
- Frontend (fast): `bb fe-test-parallel` or `npm run test:cljs:parallel`. Save once: `bb fe-test-parallel 2>&1 | tee /tmp/fe-tests.txt`.
- Frontend config: `bb validate-frontend-config`, `bb config-audit --strict`.
- Browser: chrome‑mcp flows targeting element IDs.
- Logs/Builds: Use the `system-logs` skill to monitor shadow‑cljs + server during cutovers.

Runtime & Rollback

- Feature flags per risky area with default safe values.
- Staging cutover first; monitor: 4xx/5xx, login failures, OAuth rejections, route misses, client JS errors.
- Rollback = re‑enable flag + revert PR if necessary.

Communication & Versioning

- Publish deprecation notes and effective dates in CHANGELOG and admin release notes.
- For API changes, document v2 contract and migration guides; provide 30‑day overlap window if external clients exist.
- Track exceptions via ticket IDs in code comments for traceability.

Issue Tracking & Labels

- Use label `legacy-removal` and one of: `risk-low|risk-med|risk-high`.
- File one issue per category with checklists; link to exact file:line items.

Acceptance Checklists (tick per PR)

- [ ] Inventory updated for this category; counts decreased.
- [ ] Flag added (if needed) and defaulted safe.
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

