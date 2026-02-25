---
name: remove-unused-vars
description: Safely remove unused public vars from the codebase in tested batches
---

# Remove Unused Public Vars

Execute a safe, batched removal of unused public vars identified by clojure-lsp.

## Workflow

### Phase 1: Gather Diagnostics

1. Run the diagnostics script:
   ```bash
   bb scripts/bb/code_quality/unused_public_var.clj
   ```

2. Read the output file at `tmp/unused_public_var.txt`.

3. Parse and categorize the unused vars by:
   - **Namespace group**: `app.domain.backend`, `app.domain.frontend`, `app.admin`, `system`, `test`, `dev`, `scripts`
   - **Type**: `var` (function/def) vs `keyword` (re-frame subscription/event)
   - **Risk level**:
     - Low: test helpers, dev utilities, isolated handlers
     - Medium: domain services, admin events/subs
     - High: core routes, auth handlers, shared utilities

4. Capture session guardrails before edits:
   - Create a `tmp/` note with the current diagnostics snapshot.
   - Flag candidates that are likely used indirectly (especially vars commonly redefined in tests via `with-redefs`).

### Phase 2: Create Batch Plan

Group vars into logical batches (5-10 items per batch):

1. **Batch 1: Test/Dev utilities** - Lowest risk, easy to verify
   - `test/app/backend/test_helpers.clj` functions
   - `test/app/backend/test_repl.clj` functions
   - `dev/` and `dev/system/` utilities

2. **Batch 2: Admin frontend events/subs** - Medium risk
   - Unused re-frame events and subscriptions in `app.admin.frontend.events.*`
   - Unused re-frame events and subscriptions in `app.admin.frontend.subs.*`

3. **Batch 3: Domain frontend events/subs** - Medium risk
   - Unused re-frame events and subscriptions in `app.domain.frontend.events.*`
   - Unused re-frame events and subscriptions in `app.domain.frontend.subs.*`

4. **Batch 4: Domain backend handlers** - Medium risk
   - Unused handler functions in `app.domain.backend.expenses.handlers.*`

5. **Batch 5: Domain backend services** - Higher risk
   - Unused service functions in `app.domain.backend.expenses.services.*`
   - Unused integrations in `app.domain.backend.expenses.integrations.*`

6. **Batch 6: System and misc** - Variable risk
   - `system/state.clj`
   - `src/build_hooks.clj`
   - Scripts under `scripts/bb/`

### Phase 3: Execute Batches

For each batch:

1. **Pre-flight check**:
   - Verify the var is truly unused via `grep` search across **both** `src/` and `test/`
   - Search for test indirection patterns: `with-redefs`, `requiring-resolve`, `resolve`, var-quote (`#'ns/var`)
   - Check for dynamic usage patterns (e.g., re-frame keyword construction)
   - Note any vars that might be used via indirection

2. **Remove the var**:
   - Use `clojure-mcp` structural edits for `.clj`/`.cljs`/`.cljc` files
   - For re-frame keywords: remove the entire `reg-event-fx`/`reg-sub` block
   - For functions: remove the entire `defn`/`def` form

3. **Validate**:
   - Lint touched files first:
     ```bash
     clj-kondo --lint <touched-file-or-dir>
     ```
   - Run focused tests related to the namespace:
     ```bash
     bb be-test --grep "namespace-pattern"
     ```
   - For frontend changes, run:
     ```bash
     bb fe-test-parallel --grep "namespace-pattern"
     ```
   - Save test output to `tmp/` for review

4. **Handle failures before next batch**:
   - If validation fails with unresolved vars in tests, update tests to current module boundaries (requires + `with-redefs` targets), then re-run.
   - Do not continue to the next batch until failures are understood as either:
     - fixed in this batch, or
     - explicitly documented as pre-existing and out of scope.

5. **Commit checkpoint** (optional):
   - After successful batch validation, create a checkpoint commit:
     ```bash
     git add -A && git commit -m "chore: remove unused vars batch N"
     ```

6. **Proceed to next batch** only if validation passes.

### Phase 4: Final Gate

After all batches:

1. Run full relevant suite(s) once (save output with `tee`):
   ```bash
   mkdir -p tmp
   bb be-test 2>&1 | tee tmp/backend-test-unused-vars-$(date +%H%M%S).txt
   bb fe-test-parallel 2>&1 | tee tmp/frontend-test-unused-vars-$(date +%H%M%S).txt
   ```
2. If one side was untouched, you may skip that side but state why.

## Safety Rules

- **Never remove vars from**: `vendor/`, `src/app/template/`, `src/app/shared/` (already excluded by script)
- **Preserve metadata**: If a var has a docstring explaining purpose, document why it was removed
- **Check protocol implementations**: Protocol method removals require checking all extenders
- **Re-frame keyword patterns**: Be cautious of dynamically constructed keywords like `(keyword "admin" (str entity "-loading?"))`
- **Test fixtures**: Some test helper vars may be used via `use-fixtures` - verify before removal
- **Namespace stubs**: If removals empty a namespace file, keep a minimal `ns` form unless you also remove every require/site that depends on that namespace path
- **No silent breakage**: A green focused test is not enough; complete final gate before declaring cleanup complete

## Output Format

After each batch, report:

```
## Batch N Complete

**Removed**: 
- `namespace/var-name` (type)
- ...

**Tests**: PASSED / FAILED
**Output**: tmp/batch-N-test-output.txt
**Follow-up fixes**: none / brief note (e.g. "updated stale test `with-redefs` target")

**Next**: Batch N+1 description
```

## Arguments

This prompt takes no arguments. It reads fresh diagnostics each run.

## Example Invocation

```
Run the remove-unused-vars prompt to clean up unused public vars.
```
