---
name: clojure-eval
description: "REPL-first Clojure/ClojureScript exploration and evaluation workflow (nREPL + clj-nrepl-eval). Use when exploring code, verifying behavior changes, or debugging failing paths by evaluating real code in the running system."
---

# clojure-eval

Use this when you need to explore the codebase, verify behavior changes, or debug a failing path by evaluating real code in the running system.

This repo is **REPL-first**:
- Prefer evaluating in the existing REPL over guessing.
- Don’t spawn new REPLs unless you truly can’t find a running one.

See `examples.md` for extra copy/paste patterns.

## Fast path: connect + sanity check

```bash
clj-nrepl-eval --discover-ports
clj-nrepl-eval -p <PORT> "(+ 1 2)"
```

Tip: This repo has a `:nrepl` deps alias (see `deps.edn`); default port is typically **7888**, but discovering ports is more reliable.

## The tight reload loop (Clojure / JVM)

```clojure
(require 'my.ns :reload)

;; Run the smallest thing that proves your change.
(my.ns/some-fn {:example true})
```

## Exploration primitives (cheap; do early)

These are fast, low-risk ways to learn what’s available before deeper changes:

- `(doc some-symbol)`
- `(dir some.ns)`
- `(apropos "string")`
- `(find-doc "query string")`
- `(source some-symbol)`

## ClojureScript (shadow-cljs) notes

If you’re evaluating frontend code, you must be in the CLJS runtime.

```clojure
;; First select the build.
(shadow.cljs.devtools.api/nrepl-select :app)   ;; or :admin

;; Then reload/eval as usual.
(require 'app.some.cljs-ns :reload)
```

If `(shadow.cljs.devtools.api/nrepl-select ...)` fails, you’re likely connected to the wrong nREPL port. Use `clj-nrepl-eval --discover-ports` and pick the shadow-cljs nREPL.

## Validation discipline

For behavior changes / non-trivial work, follow the repo’s REPL validation checklist (see `.github/copilot-instructions.md` “REPL validation checklist”). Minimum edge cases to consider: happy path, `nil`, empty collections, invalid/boundary inputs.

If you run tests, run the smallest focused set and **save the output once** with `tee` (don’t re-run just to grep).

```bash
mkdir -p tmp
bb be-test 2>&1 | tee tmp/be-test.txt
npm run test:cljs 2>&1 | tee tmp/fe-test.txt
```

## Troubleshooting

- **`FileNotFoundException` requiring `.cljs` namespaces**
  - You’re in JVM REPL mode. Switch to CLJS by selecting a shadow build (see above).

- **“Could not locate …” for a namespace you just edited**
  - Require with `:reload` and verify the namespace ↔ file path match.

- **State got weird across evals**
  - Use `clj-nrepl-eval --reset-session ...` to clear the session, then reload the namespaces you touched.
