---
description: "Read backend + shadow-cljs logs from the dev monitoring output"
tags: ["logs", "debugging", "server", "shadow-cljs", "development"]
---

# system-logs

Use this when someone asks “what do the logs say?” (compile errors, runtime exceptions, auth/db issues).

## Fast path
- One-shot read: `./scripts/sh/monitoring/read_output.sh`
- Follow live: `./scripts/sh/monitoring/read_output.sh -f` (Ctrl+C)
- Quick error scan: `./scripts/sh/monitoring/read_output.sh | grep -iE "error|exception|failed|fatal" -n`

## Need fresh server/compile logs?
Restart the system over nREPL (app hot-reloads; no manual restart needed otherwise):
- `clj-nrepl-eval --discover-ports` (pick the Clojure/server port, often `7888`)
- `clj-nrepl-eval -p <port> "(require 'system.core :reload) (system.core/restart-system)"`

## If nothing shows up
- Check monitoring status: `[ -f /tmp/active_command.txt ] && echo active || echo inactive`
- If inactive, start a monitoring run (e.g. `./scripts/sh/monitoring/monitor_terminal.sh '<cmd>'`) and then re-run `read_output.sh`.
