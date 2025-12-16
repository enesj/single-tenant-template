# Frontend Code Cleanup - Unused Functions and Namespaces

**Date:** 2025-12-16
**Prompt Focus:** Identify and safely remove unused frontend functions and namespaces
**Target:** Frontend ClojureScript code (admin and template modules)

## Context Snapshot

- **Repository:** Single-tenant SaaS template built with Clojure/ClojureScript
- **Frontend Architecture:** Re-frame + UIx, Shadow-CLJS builds
- **Build Targets:** `:app` (public shell) and `:admin` (admin console)
- **Frontend Paths:**
  - Admin frontend: `src/app/admin/frontend/`
  - Template frontend: `src/app/template/frontend/`
  - Domain frontend: `src/app/domain/frontend/`
  - Tests: `test/app/*/frontend/`
- **Entry Points:**
  - Admin: `app.admin.frontend.core/init`
  - App: `app.template.frontend.core/init`

## Task Focus

Your goal is to identify and safely remove all unused frontend functions and namespaces from the ClojureScript codebase. This includes:

1. **Discovery Phase:** Find all potentially unused functions and namespaces
2. **Verification Phase:** Confirm code is truly unused (not referenced via dynamic calls, metadata, or build-time includes)
3. **Safe Removal Phase:** Remove unused code with proper backup and testing

## Code Map

### Key Frontend Namespaces

#### Admin Module (`src/app/admin/frontend/`)
- `events/` - Re-frame event handlers
- `subs/` - Re-frame subscriptions
- `components/` - UI components
- `pages/` - Page-level components
- `utils/` - Utility functions
- `auth/` - Authentication logic
- `renderers/` - Custom renderers

#### Template Module (`src/app/template/frontend/`)
- `events/` - Core Re-frame events
- `components/` - Shared UI components
- `pages/` - Shared page components
- `utils/` - Shared utilities
- `hooks/` - React hooks
- `api/` - HTTP client utilities

#### Domain Module (`src/app/domain/frontend/`)
- `expenses/` - Expenses domain frontend code

### Build Configuration
- Shadow-CLJS config: `shadow-cljs.edn`
- Foreign libs configuration in build file
- Preload configurations: `app.template.frontend.preload.*`

## Commands to Run

### Development
```bash
# Start admin build for hot-reload
npm run watch:admin

# Run frontend tests (primary validation)
npm run test:cljs

# Run tests in watch mode during cleanup
npm run test:cljs:watch

# Validate frontend configs (if any EDN configs)
bb validate-frontend-config
```

### Testing Commands
```bash
# Save test output before cleanup
npm run test:cljs 2>&1 | tee /tmp/fe-test-before.txt

# After each cleanup batch
npm run test:cljs 2>&1 | tee /tmp/fe-test-after.txt

# Compare results
diff /tmp/fe-test-before.txt /tmp/fe-test-after.txt
```

### Backup Commands
```bash
# Create backup before changes
cp -r src/app/ backup-frontend-$(date +%H%M%S)/

# Or file-by-file
cp src/app/admin/frontend/components/unused.cljs backup/unused.cljs.bak
```

## Gotchas

### 1. Dynamic References
- Namespaces might be loaded dynamically via `require` in runtime
- Functions referenced by keywords in event dispatching
- Components used via string-based routing

### 2. Build-time Includes
- Preload configurations in `shadow-cljs.edn`
- Foreign libs and externs
- All namespaces mentioned in build configurations

### 3. Meta-Programming
- Macros that expand to use functions
- Protocol implementations
- Multimethod implementations

### 4. Inter-Namespace Dependencies
- Template components used by admin
- Shared utilities across modules
- Event handlers crossing namespace boundaries

### 5. Test-Only Code
- Functions existing only for testing
- Mock components in test directories
- Test utilities and helpers

## Implementation Plan

### Phase 1: Comprehensive Analysis
1. **Build a complete function and namespace inventory**
   - Parse all `.cljs` files for `defn` and `ns` forms
   - Extract all function names with their namespaces
   - Track function arity and metadata

2. **Build a reference map**
   - Find all direct function calls
   - Find all namespace requires
   - Track dynamic references (keywords, strings)
   - Include references in tests

3. **Identify potentially unused code**
   - Functions with zero references
   - Namespaces with no references from other namespaces
   - Create initial candidate list

### Phase 2: Verification Process
For each candidate:

1. **Check dynamic references**
   - Search for function name as keyword
   - Check string-based references in routing
   - Look for meta-programming usage

2. **Verify build configuration**
   - Check if namespace in `shadow-cljs.edn`
   - Verify preload configurations
   - Check foreign libs dependencies

3. **Cross-reference with tests**
   - Check if function/namespace used in tests
   - Verify if it's test-only code that should remain

4. **Manual review for edge cases**
   - Protocol implementations
   - Multimethod dispatch functions
   - Callback functions passed by reference

### Phase 3: Safe Removal Workflow

For each verified unused code:

1. **Create backup**
   ```bash
   cp src/path/to/unused.cljs src/path/to/unused.cljs.bak
   ```

2. **Remove unused functions/namespaces**
   - Remove `defn` forms
   - Remove `:require` entries for unused namespaces
   - Remove entire files if completely unused

3. **Run tests immediately**
   ```bash
   npm run test:cljs
   ```
   - If tests fail: restore from backup and investigate

4. **Run build verification**
   ```bash
   npm run build:admin
   ```
   - Ensure no build errors

5. **Verify application runs**
   - Check admin console loads
   - Verify key functionality works

6. **Document changes**
   - Add to removal log
   - Note any dependencies removed

### Phase 4: Final Cleanup

1. **Remove all backup files** after user confirmation
2. **Update documentation** if needed
3. **Run full test suite** one final time

## Checklist

- [ ] Build complete function/namespace inventory
- [ ] Create comprehensive reference map
- [ ] Generate initial list of unused code candidates
- [ ] Save list to `UNUSED-FUNCTIONS.md` in repo root
- [ ] Verify each candidate for dynamic references
- [ ] Check build configurations for indirect references
- [ ] Review test coverage for each candidate
- [ ] Create backup workflow ready
- [ ] Plan batch removal (small batches recommended)
- [ ] Prepare rollback procedure for each batch

## Safety Measures

1. **Always backup before changes**
2. **Test after each removal batch**
3. **Keep backups until final approval**
4. **Document all changes for review**
5. **Use version control to track changes**

## Output Files

- `UNUSED-FUNCTIONS.md` - Initial list of candidates
- `REMOVED-CODE-LOG.md` - Track all successful removals
- Backup files with `.bak` extension
- Test output files for before/after comparison

## Tools to Use

- **Chrome-MCP (Critical):** Essential for browser-based testing and verification of component functionality after removals
  - Navigate to admin UI at `http://localhost:8085/admin`
  - Test interactive elements to ensure they still work
  - Verify DOM elements are present and functional
  - Check console for JavaScript errors
  - Validate component IDs are still accessible for testing

- **ClojureScript eval:** Use MCP tool for runtime verification
  - Evaluate function existence in running application
  - Check namespace loading status
  - Verify event handlers are registered

- **App-DB Inspect:** Use skill for state verification
  - Check re-frame state after code removal
  - Verify subscriptions are working
  - Ensure no dangling references in app-db

- **System Logs:** Use skill for build/runtime monitoring
  - Monitor Shadow-CLJS compilation
  - Check for startup errors after changes
  - Verify no missing dependencies

- **Grep/rg:** For searching references
- **Shadow-CLJS:** For build verification
- **Git diff:** For reviewing changes

Remember: When in doubt, keep the code. It's better to have unused code than to break functionality.