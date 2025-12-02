---
description: "Read and analyze server logs and shadow-cljs logs using monitoring system"
tags: ["logs", "monitoring", "debugging", "development", "server", "shadow-cljs"]
---

# system-logs

Compact guide for reading backend + shadow-cljs logs from the monitoring system.

## Use When
- User asks for build/compile status, runtime errors, auth/db issues, or “what just happened”.

## Prereqs
- Monitoring script exists: `./scripts/sh/monitoring/read_output.sh`
- Logs live at `/tmp/command_output_{RUN_ID}.log` (pointers `.pos/.last`).

## Fast Path
1) Check status: `[ -f /tmp/active_command.txt ] && echo active || echo inactive`
2) Read once: `./scripts/sh/monitoring/read_output.sh`
3) Live follow: `./scripts/sh/monitoring/read_output.sh -f` (Ctrl+C to stop)
4) Need fresh BE/FE compile logs? Restart system over nREPL (app already running):
   - `clj-nrepl-eval --discover-ports` → pick the `clj` port (often 7888).
   - `clj-nrepl-eval -p <port> "(require 'system.core :reload) (system.core/restart-system)"`
   - Add `--with-sudo` if `ps` is blocked.

## Minimal Workflow
- **Target**: server | compile | auth | db | perf | recent | errors.
- **Read**: `./scripts/sh/monitoring/read_output.sh` (or `-f`).
- **Filter**: pipe to `grep -i "error|exception|failed|fatal"`, `"compil|build|warning"`, `"auth|login|token|unauth"`, or `tail -50` for recency.
- **Summarize**: list findings and next actions.

## If Monitoring Missing
- Ask teammate to start monitoring run (e.g., via `monitor_terminal.sh '<cmd>'`) then rerun read_output.

## Output Template
```
## Log Analysis
- Source/time: ...
- Errors/warnings: ...
- Perf/DB: ...
- Recent activity: ...
- Next actions: 1) ... 2) ...
```

## Tips
- Use `grep -C 3` for context.
- Hot-reload/build issues are near the end of the file.
