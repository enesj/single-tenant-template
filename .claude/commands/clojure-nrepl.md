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

## Troubleshooting

- **Connection refused / no ports discovered**
  - The nREPL server probably isn’t running. In this repo, the common case is “the app isn’t running”. Start it (see `bb run-app`) and try `clj-nrepl-eval --discover-ports` again.

- **Evaluating works, but `require` can’t find your namespace** (`FileNotFoundException`, “Could not locate …”)
  - Double-check the namespace + file path match.
  - If you just edited the file, require with `:reload`: `(require 'my.ns :reload)`.

- **You’re in the wrong runtime (Clojure vs ClojureScript)**
  - Symptom: trying to `require` `.cljs` code (or use browser globals) from a JVM REPL.
  - Fix: connect to the shadow-cljs nREPL port (it will show up in `--discover-ports`), then select a build:
    - `(shadow.cljs.devtools.api/nrepl-select :app)` or `:admin`

- **Timeouts / long-running evals**
  - Increase timeout: `--timeout <ms>`.
  - If the REPL session got into a weird state, try `--reset-session`.
