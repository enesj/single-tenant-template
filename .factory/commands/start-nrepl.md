---
description: "Start an nREPL server for Clojure code evaluation via clojure-mcp tools"
metadata:
  tags: ["nrepl", "repl", "clojure", "development", "evaluation"]
---

# start-nrepl

Start (or reuse) an nREPL server for this project so you can evaluate code with `clojure-mcp` tools.

---

## 1) Prefer existing servers

Check for already-running nREPL servers:

```bash
bb list-nrepl-ports
```

Or use clojure-mcp:

```clojure
(clojure-mcp.repl-tools/list-nrepl-ports)
```

If a server is already running for this directory, reuse its port.

---

## 2) Start a new server (deps.edn)

This repo provides a deps alias:

```bash
clojure -M:nrepl
```

By default it starts on port **7888** (see `deps.edn`).

---

## 3) Verify the connection

Evaluate a simple expression via clojure-mcp:

```clojure
(+ 1 2 3)
```

Or from command line:

```bash
bb eval "(+ 1 2 3)"
```

If you started a server on a different port, specify it when evaluating.

---

## Key facts

- Default port: **7888** (configurable in `deps.edn`)
- The server stays running in the background
- Multiple sessions can connect to the same nREPL
- Use `clojure-mcp` eval tools for REPL-driven development

---

## Related

- `create-articles` — Uses REPL for receipt re-processing via `run-by-ids!`
- `create-articles-remote-db` — Production DB operations (no REPL receipt retry)
