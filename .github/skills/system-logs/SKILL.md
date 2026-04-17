---
name: system-logs
description: "Read backend and shadow-cljs logs from the dev monitoring output. Use when checking compile errors, runtime exceptions, auth issues, or DB failures."
---

# system-logs

Use this when someone asks “what do the logs say?” (compile errors, runtime exceptions, auth/db issues).

## Fast path
- One-shot read last logs: `./scripts/sh/monitoring/read_output.sh`
- Quick error scan: `./scripts/sh/monitoring/read_output.sh | grep -iE "error|exception|failed|fatal" -n`

## Need fresh server/compile logs?
Restart the system via nREPL using `clj-nrepl-eval`:

```bash
clj-nrepl-eval --discover-ports
clj-nrepl-eval -p <BACKEND_PORT> "(require 'system.core :reload) (system.core/restart-system)"
```
