---
description: Continue debugging wk1/wk2 missing styles in multi-instance dev runner
---

You are working in `/Users/enes/Projects/single-tenant-template` (Clojure backend + shadow-cljs + PostCSS/Tailwind).

## Goal
Fix the regression where **worktrees wk1 (8086) and wk2 (8087) load pages but are unstyled** (CSS not applied), while main (8085) is styled.

## Current multi-instance setup (already implemented)
- Git worktrees:
  - main repo: `/Users/enes/Projects/single-tenant-template`
  - wk1: `/Users/enes/Projects/single-tenant-template/worktrees/wk1`
  - wk2: `/Users/enes/Projects/single-tenant-template/worktrees/wk2`
- Backend ports:
  - main: 8085
  - wk1: 8086
  - wk2: 8087
- shadow-cljs:
  - main shadow is started **inside** `clojure -M:dev` (integrated); do NOT start an extra `shadow-cljs watch` for main.
  - wk1 and wk2 run their own `shadow.cljs.devtools.cli watch app admin` processes.
  - shadow ports are env-driven via `#shadow/env` in `shadow-cljs.edn`:
    - main defaults: 9630/8777/9095
    - wk1: 9631/8778/9096
    - wk2: 9632/8779/9097

## Known files involved
- Runner scripts:
  - `scripts/sh/development/run-app.sh` (+ copies under worktrees)
  - `scripts/sh/development/stop-app.sh`
  - `scripts/sh/development/restart-wt.sh`
- Dev entry:
  - `dev/core.clj` (main) watches `:app` and `:admin`
- Assets handling for worktrees:
  - `ensure_worktree_public_assets` copies `index.html`/`admin.html` into each worktree `resources/public/` and symlinks stable assets (e.g., `resources/public/assets`, `favicon.ico`) from main.
  - PostCSS watcher is started by the runner (intended: one watcher), with PID/logs under `tmp/`.

## Symptom
In wk1/wk2, visiting `/admin/dashboard` shows a mostly plain HTML page (no Tailwind/CSS). Main is styled.

## Hypotheses to test (in order)
1. **CSS file requested by HTML returns 404** on wk1/wk2.
2. Worktree `resources/public/assets` symlink points somewhere wrong (broken symlink) or points to a location that does not contain the built CSS.
3. PostCSS output is generated only in main repo but wk1/wk2 serve from their own `resources/public/` and the expected path isn’t available.
4. Static file middleware refuses to follow symlinks or is rooted differently for worktrees.

## Quick verification checklist
1. Start wk1 + wk2 (or all):
   - `bb run-app all` (or `bb run-app wk1` and `bb run-app wk2`)
2. Inspect what CSS URL the admin page expects:
   - `curl -s http://localhost:8086/admin/dashboard | head -n 80`
   - `curl -s http://localhost:8087/admin/dashboard | head -n 80`
   - Look for `<link rel="stylesheet" href="...">`
3. Probe CSS URL directly and compare main vs wk1/wk2:
   - `curl -I http://localhost:8085/<css-path>`
   - `curl -I http://localhost:8086/<css-path>`
   - `curl -I http://localhost:8087/<css-path>`
4. Confirm the file exists on disk for each repo:
   - `ls -la resources/public/assets` in main
   - `ls -la worktrees/wk1/resources/public/assets`
   - `ls -la worktrees/wk2/resources/public/assets`
   - `find ... -maxdepth 3 -type f | grep -E 'css|style'`

## Expected fix directions
- Ensure the CSS build output is written to a location that wk1 and wk2 can serve.
  - Either: run PostCSS watcher per-instance into each worktree’s `resources/public/assets/...`.
  - Or: keep a single PostCSS output in main and make wk1/wk2 serve that path reliably (symlink + server static root must follow it).
- Keep changes small and focused; do not reintroduce the main shadow "already running" double-start.

## Deliverables
- A code change that makes wk1/wk2 styled again.
- Verification steps (curl HTTP status for CSS and a quick browser check).
