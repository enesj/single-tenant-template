(ns app.template.backend.migrations.alignment
  "Check whether DB state is aligned with migration files and source-of-truth EDN.

  This is intended for REPL/CI use.

  What it checks:
  - Migration files under resources/db/migrations are all applied in DB
  - DB-applied migrations that do not exist as files
  - Duplicate migration numbers
  - Basic schema diff between hierarchical models.edn and actual DB (tables/columns/index names/enums)
  - Existence of extended DB objects declared in hierarchical EDN (functions/triggers/views/policies)

  Exit semantics (helper):
  - 0: aligned
  - 1: differences
  - 2: error (DB connection/config/other)

  This file re-exports all public functions from submodules for backward compatibility:
  - utils: Common utilities
  - files: Migration file parsing and DB tracking
  - schema: Schema expectation building
  - fetchers: DB fetchers and comparers
  - report: Report generation and printing"
  (:require
    [app.template.backend.migrations.alignment.utils :as utils]
    [app.template.backend.migrations.alignment.files :as files]
    [app.template.backend.migrations.alignment.schema :as schema]
    [app.template.backend.migrations.alignment.fetchers :as fetchers]
    [app.template.backend.migrations.alignment.report :as report]))

;; =============================================================================
;; Re-exports from utils.clj
;; =============================================================================

(def default-migrations-dir utils/default-migrations-dir)
(def default-db-root utils/default-db-root)
(def internal-tables utils/internal-tables)
(def now-iso utils/now-iso)
(def normalize-ident utils/normalize-ident)
(def read-edn-file utils/read-edn-file)
(def discover-domain-subdirs utils/discover-domain-subdirs)
(def read-hierarchical-edn utils/read-hierarchical-edn)
(def q utils/q)

;; =============================================================================
;; Re-exports from files.clj
;; =============================================================================

(def list-migration-files files/list-migration-files)
(def parse-migration-filename files/parse-migration-filename)
(def migration-file-report files/migration-file-report)
(def db-applied-migrations files/db-applied-migrations)

;; =============================================================================
;; Re-exports from schema.clj
;; =============================================================================

(def sql-type->expected schema/sql-type->expected)
(def expected-nullable? schema/expected-nullable?)
(def models->expected schema/models->expected)

;; =============================================================================
;; Re-exports from fetchers.clj
;; =============================================================================

(def fetch-tables fetchers/fetch-tables)
(def fetch-columns fetchers/fetch-columns)
(def fetch-indexes fetchers/fetch-indexes)
(def fetch-enums fetchers/fetch-enums)
(def compare-tables fetchers/compare-tables)
(def compare-columns fetchers/compare-columns)
(def compare-indexes fetchers/compare-indexes)
(def compare-enums fetchers/compare-enums)
(def extract-sql-object-name fetchers/extract-sql-object-name)
(def expected-extended-object-names fetchers/expected-extended-object-names)
(def fetch-functions fetchers/fetch-functions)
(def fetch-triggers fetchers/fetch-triggers)
(def fetch-views fetchers/fetch-views)
(def fetch-policies fetchers/fetch-policies)

;; =============================================================================
;; Re-exports from report.clj
;; =============================================================================

(def report report/report)
(def diff? report/diff?)
(def exit-code report/exit-code)
(def print-report! report/print-report!)
(def report-for-profile report/report-for-profile)
(def exit-code-for-profile report/exit-code-for-profile)
