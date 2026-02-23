# Development Scripts

Scripts for running and managing the application during development.

## Scripts

### `run-app.sh`

Primary script for starting the development environment. Handles cleanup of existing processes and starts the application with monitoring.

**Usage:**

```bash
bb run-app
bb run-app main
bb run-app wk1
bb run-app wk2
bb run-app all
# or: ./scripts/sh/development/run-app.sh
# or: ./scripts/sh/development/run-app.sh --all
```

**Features:**

- Default mode starts only the app mapped to the current folder (`main`, `wk1`, or `wk2`)
- Explicit target mode starts only the requested instance (`main`, `wk1`, or `wk2`) regardless of current folder
- `all` / `--all` mode starts main + worktree app ports (`8085`, `8086`, `8087`)
- Starts Docker services (`docker compose up -d`) for Postgres (dev + test)
- Waits for Postgres on `localhost:55432` before booting the app
- Starts the PostCSS watcher (`npm run develop`) and writes logs to `tmp/postcss_watcher.log`
- Ensures `worktrees/wk1` and `worktrees/wk2` app instances are started on ports `8086` and `8087`
- Runs main repo app (`8085`) via `clojure -M:dev` under `scripts/sh/monitoring/monitor_terminal.sh` for readable output
- Ensures Shadow is running on **unique ports** so you can run all 3 at once:
  - `main`: started by `clojure -M:dev` (watches `:app` + `:admin`)
  - `wk1`/`wk2`: started via `shadow-cljs watch app admin`

**Shadow ports (per instance):**

The dev scripts set these automatically via env vars read in `shadow-cljs.edn` (`#shadow/env`).

| Instance | Shadow server/UI (`SHADOW_HTTP_PORT`) | Shadow nREPL (`SHADOW_NREPL_PORT`) | CLJS test HTTP (`SHADOW_TEST_HTTP_PORT`) | Devtools HTTP (`SHADOW_DEVTOOLS_HTTP_PORT`) |
| --- | ---: | ---: | ---: | ---: |
| `main` | `9630` | `8777` | `9095` | `9650` |
| `wk1` | `9631` | `8778` | `9096` | `9651` |
| `wk2` | `9632` | `8779` | `9097` | `9652` |

**Worktree static assets:**

- Worktrees keep their own `resources/public/` directory (so JS build output doesn’t collide across branches).
- The script will copy `index.html` + `admin.html` from the main repo if missing.
- The script symlinks `resources/public/assets` (CSS/images) and `favicon.ico` from the main repo to avoid duplication.

### `stop-app.sh`

Stops all local app instances and helper processes started by `run-app.sh`.

**Usage:**

```bash
bb stop-app
# or: bash ./scripts/sh/development/stop-app.sh
```

**Features:**

- Stops listeners on ports `8085`, `8086`, and `8087`
- Stops worktree PID-file managed app processes (`wk1`, `wk2`)
- Stops PostCSS watcher via `tmp/postcss_watcher.pid`
- Stops `shadow-cljs` watchers (main + wk1 + wk2) via their pid files when present
- Cleans up monitor metadata (`/tmp/active_command.txt`) when present

### `restart-wt.sh`

Restarts a single worktree app instance by name.

**Usage:**

```bash
bb restart-wt wk1
bb restart-wt wk2
# or: bash ./scripts/sh/development/restart-wt.sh wk2
```

**Features:**

- Validates worktree name (`wk1` or `wk2`)
- Ensures Docker/Postgres readiness before restart
- Ensures `shadow-cljs` watch is running for `:app` and `:admin` builds
- Stops existing process on the mapped worktree port (`wk1` -> `8086`, `wk2` -> `8087`)
- Starts only the selected worktree app and writes logs under `worktrees/<wt>/tmp/`

### `kill-java.sh`

Terminates all Java processes except clojure-mcp to clean up development environment.

**Usage:**

```bash
./kill-java.sh
```

**Features:**

- Finds all Java processes (excluding clojure-mcp)
- Safely terminates each process
- Provides feedback on termination status
- Useful for cleaning up stuck development processes

### `sync-codex-mcp.sh`

Workaround helper that copies the project’s `clojure-mcp` MCP registration into the global Codex config.

**Usage:**

```bash
./sync-codex-mcp.sh
```

**Features:**

- Extracts the `[mcp_servers.clojure-mcp]` block from `.codex/codex.toml`
- Backs up `~/.codex/config.toml` before writing
- Removes any existing global definition and replaces it with the project-scoped command
- Helps keep Codex working while project-level MCP configs remain unsupported
