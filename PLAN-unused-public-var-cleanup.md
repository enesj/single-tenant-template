# PLAN: `unused-public-var` cleanup (app code only)

## Goal
Reduce `clojure-lsp` diagnostics for `clojure-lsp/unused-public-var` in **`src/app/**`** by either:

- removing truly unused public vars/keywords, or
- reducing visibility (public → private) when the code is only used internally.

This plan is intentionally **documentation-only** (no code changes), and is scoped to application code per request.

## Scope

- **In scope (changes):** `src/app/**/*.{clj,cljc,cljs}`
- **Allowed (changes, only if needed):** `dev/**` as the destination when relocating dev-only helpers out of `src/app/**` (Batch 2)
- **Out of scope (changes):** `vendor/**`, `scripts/**`, `test/**`, `resources/**`
- **Still search for references in:** `resources/**`, `config/**`, `docs/**` before deleting/privatizing anything that might be dynamically referenced

## Baseline (from structured diagnostics)

Baseline source: `clojure-lsp diagnostics` captured locally for `src/app/**`.

Recommended command (avoids post-filtering by scoping analysis to `src/app`):

```bash
clojure-lsp --version
clojure-lsp diagnostics --filenames src/app --output '{:format :edn}' > /tmp/lsp-diagnostics-src-app.edn
```

When regenerating the baseline, record:

- the `clojure-lsp --version` output, and
- the timestamp for `/tmp/lsp-diagnostics-src-app.edn` (so future diffs are comparable).

- `src/app` files with at least one `unused-public-var`: **144**
- `src/app` `clojure-lsp/unused-public-var` diagnostics: **533**
  - “Unused public var …”: **439**
  - “Unused public keyword …”: **94**

Breakdown by top-level area:

- `src/app/template`: **228**
- `src/app/domain`: **132**
- `src/app/admin`: **99**
- `src/app/shared`: **74**

Breakdown by file type:

- `.cljs`: **288**
- `.clj`: **173**
- `.cljc`: **72**

### Highest-volume files (≥ 5 findings)

These are the best “batch anchors”: fixing them yields the biggest reduction with the fewest file touches.

- 33  `src/app/template/backend/migrations/alignment.clj`
- 15  `src/app/shared/patterns.cljc`
- 15  `src/app/template/di/config.clj`
- 14  `src/app/domain/backend/expenses/services/receipts.clj`
- 14  `src/app/domain/frontend/expenses/admin/subs.cljs`
- 13  `src/app/template/backend/crud/protocols.clj`
- 12  `src/app/shared/schemas/primitives.cljc`
- 11  `src/app/domain/frontend/expenses/adapters.cljs`
- 11  `src/app/template/backend/utils/model_customizations.clj`
- 10  `src/app/template/backend/migrations/simple_repl.clj`
-  9  `src/app/template/frontend/dev/repl_tracing.cljs`
-  9  `src/app/template/backend/auth/protocols.clj`
-  9  `src/app/admin/frontend/components/shared_utils.cljs`
-  8  `src/app/shared/date.cljc`
-  8  `src/app/template/frontend/utils/shared.cljs`
-  8  `src/app/admin/frontend/components/settings_views.cljs`
-  8  `src/app/domain/frontend/expenses/components/user_expense_form.cljs`
-  8  `src/app/admin/frontend/config/loader.cljs`
-  7  `src/app/admin/frontend/subs/config.cljs`
-  7  `src/app/domain/frontend/expenses/components/expense_form.cljs`
-  7  `src/app/domain/frontend/expenses/subs/user_expenses.cljs`
-  7  `src/app/template/frontend/components/advanced_fields.cljs`
-  7  `src/app/template/frontend/components/shared_utils.cljs`
-  7  `src/app/domain/frontend/expenses/admin/components/entity_actions.cljs`
-  7  `src/app/domain/frontend/expenses/admin/components/detail_modals.cljs`
-  6  `src/app/admin/frontend/specs/generic.cljs`
-  6  `src/app/admin/backend/services/admin/users.clj`
-  6  `src/app/template/frontend/db/db.cljs`
-  6  `src/app/shared/field_metadata.cljc`
-  6  `src/app/template/frontend/hooks/display_settings.cljs`
-  6  `src/app/admin/frontend/events/users/bulk_operations.cljs`
-  5  `src/app/template/frontend/pages/email_verification.cljs`
-  5  `src/app/template/frontend/components/states.cljs`
-  5  `src/app/admin/frontend/subs/audit.cljs`
-  5  `src/app/domain/backend/expenses/handlers/user_expenses.clj`
-  5  `src/app/template/frontend/components/dropdown.cljs`
-  5  `src/app/admin/frontend/events/settings/view_options.cljs`

### Files with “Unused public keyword …” (keyword registries: re-frame + specs)

These are often `rf/reg-sub` / `rf/reg-event-*` keyword IDs *or* spec/schema registry keys. They’re often either:

- dead registrations (safe to remove), or
- dynamically referenced (false positives) and should be kept.

Top offenders:

- 14  `src/app/domain/frontend/expenses/admin/subs.cljs`
-  9  `src/app/template/backend/crud/protocols.clj` (spec keywords, not re-frame)
-  7  `src/app/admin/frontend/subs/config.cljs`
-  7  `src/app/domain/frontend/expenses/subs/user_expenses.cljs`
-  6  `src/app/admin/frontend/events/users/bulk_operations.cljs`
-  5  `src/app/admin/frontend/subs/audit.cljs`
-  5  `src/app/admin/frontend/events/settings/view_options.cljs`

The full list of keyword-bearing files (33 total) is a good late-batch target.

## Decision criteria (what’s safe to remove vs keep)

Treat each diagnostic as a *candidate*, not a mandate. `unused-public-var` often indicates “not used from other namespaces”, which can mean one of four things:

1) truly unused code
2) internal-only code that should be private
3) intentional API surface (protocols/spec catalogs)
4) dynamic usage that static analysis misses

Use this decision table for **every** finding:

| Finding shape | Common in | How to validate | Recommended action | Risk | Tests |
|---|---|---|---|---:|---|
| Public alias/re-export `(def x other/x)` | “index” namespaces / compatibility layers | Search for `ns/x` usage across repo; also check docs/EDN references | If no internal usages: remove alias. If file is *only* re-exports: delete namespace file. | Low | FE or BE depending on file |
| Public helper used only within its namespace | utilities/components/services | `clojure-lsp references` or ripgrep shows only local references | Change to private (`defn-`, `def ^:private`, etc.) rather than deleting | Low–Med | FE/BE depending |
| Public entrypoints used by tools/runtime (`-main`, `init!`, hooks) | app wiring, startup, adapters | Validate via runtime call sites, reflection, config, docs | Keep public; if linter is wrong, prefer adding a local ignore + comment explaining why | Med | Typically both if shared |
| Protocol method vars flagged unused | `defprotocol` namespaces | Check whether protocol is implemented and called; method vars are invoked via protocol dispatch, not always referenced directly | Keep; only remove if *protocol itself* is dead and has no implementations/usages | High | BE (and FE if `.cljc`) |
| Schema/spec keyword catalogs flagged unused | `s/def` / Malli schema libs | Check if schemas are referenced via registry/map or by var name; check if used in validation codepaths | Keep if part of “catalog” semantics; otherwise remove | Med–High | Both if `.cljc` |
| “Unused public keyword ':x/y'” | re-frame subs/events (mostly) | ripgrep for the literal keyword outside its registration; also search for stringified variants | If no usage: remove the registration + supporting code; if dynamic usage: keep (and consider suppressing at the registration form) | Med | FE |

### Notes on suppressions

This repo already uses `^{:clojure-lsp/ignore [:clojure-lsp/unused-public-var]}` in a few places, which sets precedent for “keep but document why” when static analysis can’t see runtime usage.

Examples (preferred patterns):

- On a var definition (`def`, `defn`, `defui`, etc.):

  ```clojure
  (defn ^{:clojure-lsp/ignore [:clojure-lsp/unused-public-var]} foo [...])
  ```

- On a top-level form (useful for re-frame registrations that don’t introduce a var):

  ```clojure
  ^{:clojure-lsp/ignore [:clojure-lsp/unused-public-var]}
  (rf/reg-sub :some/sub ...)
  ```

Use suppressions only when:

- the var/keyword is genuinely needed, and
- you can leave a short comment stating the dynamic usage mechanism.

## Batching strategy

Batches are grouped by *why the warning exists*, because the remediation is similar.

General rule: **start low-risk/high-volume**, then move toward higher-risk areas.

After each batch:

1. re-run `clojure-lsp diagnostics` (prefer `--filenames <touched files/dirs>` to keep it fast), and
2. run the minimum tests that cover the touched platform.
3. optional: run `clojure-lsp clean-ns --filenames <touched files>` to remove namespace noise introduced by deletions/privatization.

## Batch 0 — safety rails (no functional change)

Purpose: make it easy to verify progress and avoid accidental behavior regressions.

- Capture a baseline `unused-public-var` count for `src/app`.
- Ensure tests are runnable and output is saved (per repo testing discipline).
- If you hit delimiter/paren issues while editing, use `clj-paren-repair <files>` instead of hand-fixing.

Tests to run (baseline):

- Frontend: `bb fe-test-parallel 2>&1 | tee /tmp/fe-test-unused-public-var-baseline.txt`
- Backend: `bb be-test 2>&1 | tee /tmp/be-test-unused-public-var-baseline.txt`

## Batch 1 — re-export/compat alias cleanup (highest ROI)

Why this works: Many of the largest offenders are *pure re-export namespaces* (or mostly re-exports). If an alias is truly unused, removing it shouldn’t affect behavior.

### Batch 1A — backend re-export wrappers (BE)

Target files (representative; add similar ones as discovered):

- `src/app/template/backend/migrations/alignment.clj`
- `src/app/domain/backend/expenses/services/receipts.clj`

Work:

- Remove unused `(def … other-ns/…)` aliases.
- If a namespace is *only* re-exports (docstring + requires + alias defs), delete the file entirely.

Verify:

- `bb be-test 2>&1 | tee /tmp/be-test-unused-public-var-batch-1a.txt`

### Batch 1B — frontend re-export wrappers (FE)

Target files:

- `src/app/template/frontend/components/dropdown.cljs`
- `src/app/admin/frontend/components/shared_utils.cljs`
- `src/app/admin/frontend/components/settings_views.cljs`
- `src/app/domain/frontend/expenses/adapters.cljs`
- `src/app/template/frontend/db/db.cljs`
- `src/app/template/frontend/components/shared_utils.cljs`

Work:

- Same as 1A: remove unused alias defs; delete “index” namespaces when they have no remaining purpose.

Verify:

- `bb fe-test-parallel 2>&1 | tee /tmp/fe-test-unused-public-var-batch-1b.txt`

### Batch 1C — shared re-export wrappers (`.cljc`) (FE+BE)

Target files:

- `src/app/shared/patterns.cljc`

Work:

- Remove unused re-export defs.
- If this is intended as a stable “public facade”, decide explicitly:
  - either keep it and accept/suppress the warnings, or
  - remove facade exports and have callers use the more specific namespaces.

Verify:

- `bb fe-test-parallel 2>&1 | tee /tmp/fe-test-unused-public-var-batch-1c-fe.txt`
- `bb be-test 2>&1 | tee /tmp/be-test-unused-public-var-batch-1c-be.txt`

## Batch 2 — dev/REPL tooling living under `src/app` (cleanup or relocate)

These are often genuinely unused from production code but valuable for humans.

Target files:

- `src/app/template/backend/migrations/simple_repl.clj`
- `src/app/template/frontend/dev/repl_tracing.cljs`

Work options (choose one per file):

1) Keep under `src/app`, but add clear “dev-only” docs + suppression metadata.
2) Move to `dev/**` (preferred for long-term correctness if nothing in prod depends on it).
3) Remove if it’s obsolete.

Verify:

- If BE-only: `bb be-test 2>&1 | tee /tmp/be-test-unused-public-var-batch-2.txt`
- If FE-only: `bb fe-test-parallel 2>&1 | tee /tmp/fe-test-unused-public-var-batch-2.txt`

## Batch 3 — “real” utilities/components that are public by accident

These are usually functions/components that should have been private.

Target files (start with highest counts):

- `src/app/template/frontend/utils/shared.cljs`
- `src/app/shared/date.cljc`
- `src/app/shared/field_metadata.cljc`
- `src/app/template/di/config.clj`
- `src/app/template/backend/utils/model_customizations.clj`
- `src/app/admin/backend/services/admin/users.clj`
- `src/app/admin/frontend/config/loader.cljs`

Work:

- For each flagged var, decide:
  - delete (if unused everywhere), or
  - make private (if only internal callers exist).

Verify:

- `.cljs`: `bb fe-test-parallel 2>&1 | tee /tmp/fe-test-unused-public-var-batch-3.txt`
- `.clj`: `bb be-test 2>&1 | tee /tmp/be-test-unused-public-var-batch-3.txt`
- `.cljc`: run both (use distinct output files):
  - `bb fe-test-parallel 2>&1 | tee /tmp/fe-test-unused-public-var-batch-3-cljc.txt`
  - `bb be-test 2>&1 | tee /tmp/be-test-unused-public-var-batch-3-cljc.txt`

## Batch 4 — UI component libraries (unused exports)

These are typically `defui` / component defs exported but never referenced.

Target files:

- `src/app/template/frontend/components/advanced_fields.cljs`
- `src/app/template/frontend/components/states.cljs`
- `src/app/domain/frontend/expenses/components/user_expense_form.cljs`
- `src/app/domain/frontend/expenses/components/expense_form.cljs`
- `src/app/domain/frontend/expenses/admin/components/entity_actions.cljs`
- `src/app/domain/frontend/expenses/admin/components/detail_modals.cljs`

Work:

- Remove truly unused components.
- If a component is only used within the same file, make it private.

Verify:

- `bb fe-test-parallel 2>&1 | tee /tmp/fe-test-unused-public-var-batch-4.txt`

## Batch 5 — keyword IDs (re-frame subs/events) and other dynamic registries

This batch is medium-risk because removing “unused” subscriptions/events can subtly break flows that are driven by config or dynamic dispatch.

Approach per keyword:

- Search for the literal keyword across the repo (outside the registration).
- If it is only present at the registration site, remove the registration and any supporting code.
- If it is used dynamically (e.g., constructed keywords, config-driven dispatch), keep it and consider suppressing the diagnostic at the registration form.

Suggested sub-batches (so regressions are easier to localize):

- 5A: `src/app/domain/frontend/expenses/admin/subs.cljs` (largest)
- 5B: `src/app/domain/frontend/expenses/subs/user_expenses.cljs`
- 5C: admin settings/config events and subs:
  - `src/app/admin/frontend/subs/config.cljs`
  - `src/app/admin/frontend/events/settings/view_options.cljs`
  - `src/app/admin/frontend/events/unified_settings.cljs`
  - `src/app/admin/frontend/events/users/bulk_operations.cljs`
  - `src/app/admin/frontend/subs/audit.cljs`

Verify after each sub-batch:

- 5A: `bb fe-test-parallel 2>&1 | tee /tmp/fe-test-unused-public-var-batch-5a.txt`
- 5B: `bb fe-test-parallel 2>&1 | tee /tmp/fe-test-unused-public-var-batch-5b.txt`
- 5C: `bb fe-test-parallel 2>&1 | tee /tmp/fe-test-unused-public-var-batch-5c.txt`

## Batch 6 — protocols + schema/spec catalogs (last)

These frequently appear as `unused-public-var` even when they are conceptually used (e.g., protocol dispatch, spec instrumentation, dynamic schema registry).

Target files:

- `src/app/template/backend/crud/protocols.clj`
- `src/app/template/backend/auth/protocols.clj`
- `src/app/template/protocols.clj`
- `src/app/shared/schemas/primitives.cljc`

Work:

- Remove only when you can prove the protocol/schema is not used:
  - no implementations,
  - no instrumentation/config references,
  - no runtime usage.
- Otherwise, keep and (sparingly) suppress with a comment explaining why.

Verify:

- `.clj`: `bb be-test 2>&1 | tee /tmp/be-test-unused-public-var-batch-6.txt`
- `.cljc`: run both (use distinct output files):
  - `bb fe-test-parallel 2>&1 | tee /tmp/fe-test-unused-public-var-batch-6-cljc.txt`
  - `bb be-test 2>&1 | tee /tmp/be-test-unused-public-var-batch-6-cljc.txt`

## Completion criteria

This cleanup is “done” when:

- `clojure-lsp` no longer reports `unused-public-var` for the `src/app/**` areas touched (or remaining findings are explicitly justified with suppressions), and
- FE and BE tests pass for every batch.
