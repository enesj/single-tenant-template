(ns app.template.backend.migrations.alignment
  "Facade namespace for migration/schema alignment checks.

  Historically this namespace re-exported many internals from submodules.
  We now keep a small, stable surface area here and expect internal callers to
  require the specific submodule namespaces.

  Primary entrypoints:
  - report: build an alignment report map
  - diff?: whether the report contains differences
  - print-report!: pretty-print a report

  CLI exit codes are implemented in `app.template.backend.migrations.alignment.report`.
  (The Babashka task `bb check-migrations` calls
  `app.template.backend.migrations.alignment.report/exit-code-for-profile`.)"
  (:require
    [app.template.backend.migrations.alignment.report :as report]))

(def report report/report)
(def diff? report/diff?)
(def print-report! report/print-report!)
