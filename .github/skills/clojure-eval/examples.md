# Examples: clj-nrepl-eval

## Discover servers

- `clj-nrepl-eval --discover-ports`

## Reload and call a function

- `clj-nrepl-eval -p 7888 "(require 'my.ns :reload)"`
- `clj-nrepl-eval -p 7888 "(my.ns/my-fn {:x 1})"`

## Run tests

- `clj-nrepl-eval -p 7888 "(require 'clojure.test)"`
- `clj-nrepl-eval -p 7888 "(require 'my.ns-test :reload)"`
- `clj-nrepl-eval -p 7888 "(clojure.test/run-tests 'my.ns-test)"`

## If you hit delimiter problems

Don’t manually chase parentheses.

- Run: `clj-paren-repair path/to/file.clj`
- Then re-run `require ... :reload`.
