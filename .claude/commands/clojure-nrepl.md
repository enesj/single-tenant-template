# Clojure REPL evaluation (nREPL)

Use the `clj-nrepl-eval` command to evaluate Clojure code against a running nREPL server.

## Discover nREPL servers

- `clj-nrepl-eval --discover-ports`

## Evaluate code

- `clj-nrepl-eval -p <PORT> "(+ 1 2 3)"`
- Always use `:reload` when requiring namespaces:
  - `clj-nrepl-eval -p <PORT> "(require 'my.ns :reload)"`

## Helpful options

- `--connected-ports` (show previously used sessions)
- `--reset-session` (clear REPL session state)
- `--timeout <ms>` (override default timeout)

Notes:
- This project has a `:nrepl` deps alias (see `deps.edn`). Default port is **7888**.
