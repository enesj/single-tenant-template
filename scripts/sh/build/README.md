# Build Scripts

Scripts for building, linting, and preparing the application for production.

## Scripts

### `build-production.sh`
Complete production build pipeline that:
- Stops running applications
- Cleans previous builds
- Installs Node.js dependencies
- Builds production CSS
- Compiles ClojureScript for production
- Runs backend and frontend tests
- Checks for security vulnerabilities
- Runs linting and formatting checks
- Creates the uberjar

**Usage:**
```bash
./build-production.sh
```

### `clean-cljs-cache.sh`
Removes all Shadow-CLJS build artifacts and compiled JavaScript output for a fresh compilation.

**Usage:**
```bash
./clean-cljs-cache.sh
```

**Cleans:**
- `.shadow-cljs` directory (compilation & macro cache)
- `resources/public/assets/js` (generated frontend bundle)
- `target` directory (misc build output)

### `lint.sh`
Runs clj-kondo linting on the source code with specific configuration for error detection.

**Usage:**
```bash
./lint.sh
```

### `validate-commit-msg.sh`
Validates commit messages according to conventional commits format. Used by git commit-msg hook.

**Usage:**
```bash
./validate-commit-msg.sh <commit-message-file>
```

**Format:** `<type>(<scope>): <subject>`
- Types: feat, fix, docs, style, refactor, perf, test, build, ci, chore, revert
- Subject: max 50 chars, imperative present tense
