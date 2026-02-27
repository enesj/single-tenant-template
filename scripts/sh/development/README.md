# Development Scripts

Scripts for running and managing the application during development.

## Scripts

### `run-app.sh`
Primary script for starting the development environment. Handles cleanup of existing processes and starts the application with monitoring.

**Usage:**
```bash
bb run-app
# or: ./scripts/sh/development/run-app.sh
```

**Features:**
- Checks whether the app is already running on port 8085
- Starts Docker services (`docker compose up -d`) for Postgres (dev + test)
- Waits for Postgres on `localhost:55432` before booting the app
- Starts the PostCSS watcher (`npm run develop`) and tails logs in `/tmp/postcss_watcher.log`
- Runs `clojure -M:dev` under `scripts/sh/monitoring/monitor_terminal.sh` for readable output

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
