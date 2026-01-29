---
description: "Read backend + shadow-cljs logs from the dev monitoring output"
tags: ["logs", "debugging", "server", "shadow-cljs", "development"]
---

# system-logs

Use this when someone asks “what do the logs say?” (compile errors, runtime exceptions, auth/db issues).

## Fast path
- One-shot read last logs: `./scripts/sh/monitoring/read_output.sh`
- Quick error scan: `./scripts/sh/monitoring/read_output.sh | grep -iE "error|exception|failed|fatal" -n`

## Need fresh server/compile logs?
Restart the system using the `mcp__clojure-mcp__clojure_eval` MCP tool:
```clojure
(require 'system.core :reload)
(system.core/restart-system)
```
.
