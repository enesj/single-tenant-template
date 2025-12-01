(ns automigrate.migrations
  "Legacy compatibility layer for existing code"
  (:require
   [automigrate.db.introspection :as db-intro]
   [automigrate.execution.core :as exec]
   [automigrate.files.management :as files]
   [automigrate.generation.core :as core]
   [automigrate.generation.extended :as gen-ext]
   [automigrate.schema.diffing :as diffing]
   [automigrate.status.tracking :as status]))

;; Print deprecation notice
(println "⚠️  WARNING: automigrate.migrations is deprecated.")
(println "   Please migrate to automigrate.generation.core for the main API.")
(println "   This compatibility layer will be removed in a future version.")

;; Main public API - delegate to new namespaces
(def make-migration core/make-migration)

(defn explain-migration
  "Explain migration by number - show what SQL would be executed"
  [args]
  (let [number (:number args)
        direction (:direction args :forward)
        format (:format args :sql)
        _ (assert number "Migration number is required")]
    (println "Explaining migration" number "in" direction "direction...")

    ;; Get detailed migration info using status tracking
    (let [migrations-dir "db/migrations"
          migration-files (files/migrations-list migrations-dir)
          all-migrations-detailed (map status/detailed-migration migration-files)
          migration-to-explain (first (filter #(= (:number-int %) number) all-migrations-detailed))]

      (if migration-to-explain
        (let [{:keys [file-name migration-type]} migration-to-explain
              ;; Map string migration types to keyword types for multimethod dispatch
              migration-type-kw (case migration-type
                                  "function" :fn
                                  "trigger" :trg
                                  "policy" :pol
                                  "view" :view
                                  "sql" :sql
                                  "edn" :edn
                                  nil :sql  ; Handle nil case as SQL
                                  migration-type)

              ;; Read the actual migration file content
              migration-content (try
                                  (some-> (str "db/migrations/" file-name)
                                    clojure.java.io/resource
                                    slurp)
                                  (catch Exception e
                                    (println "Error reading migration file:" (.getMessage e))
                                    nil))

              ;; Process the content based on migration type
              actions (if migration-content
                        (cond
                          (= migration-type-kw :fn) [migration-content]
                          (= migration-type-kw :sql) (let [parts (clojure.string/split migration-content #"-- BACKWARD")]
                                                       [(first parts)])
                          :else [migration-content])
                        ["-- Migration content not available --"])]

          (if (= format :sql)
            ;; Show SQL format
            (do
              (println "SQL for migration" file-name ":")
              (println "=====================================")
              (doseq [action actions]
                (println action))
              (println "====================================="))
            ;; Show human-readable format
            (do
              (println "Migration" file-name "contains" (count actions) "actions:")
              (println "=====================================")
              (doseq [action actions]
                (println " -" action))
              (println "====================================="))))

        (do
          (println "Migration number" number "not found. Available migrations:")
          (doseq [mig all-migrations-detailed]
            (println "  [" (:number-int mig) "] " (:file-name mig))))))))

(def explain explain-migration)
(def migrate core/migrate!)
(def list-migrations status/list-migrations)

;; File management functions
(def migrations-list files/migrations-list)
(def read-consolidated-edn files/read-consolidated-edn)
(def read-hierarchical-edn files/read-hierarchical-edn)
(def read-models-hierarchical files/read-models-hierarchical)

;; Database introspection functions
(def get-db-functions db-intro/get-db-functions)
(def get-db-triggers db-intro/get-db-triggers)
(def get-db-policies db-intro/get-db-policies)
(def get-db-views db-intro/get-db-views)
(def policy-command->sql db-intro/policy-command->sql)

;; Execution functions
(def exec-actions! exec/exec-actions!)
(def already-migrated status/already-migrated)
(def save-migration! exec/save-migration!)
(def delete-migration! exec/delete-migration!)

;; Status tracking
(def migration-status-summary status/migration-status-summary)

;; Extended migration functions
(def generate-extended-migrations-from-edn! gen-ext/generate-extended-migrations-from-edn!)
(def generate-extended-migrations-from-db! gen-ext/generate-extended-migrations-from-db!)
(def sync-db-to-edn-files! gen-ext/sync-db-to-edn-files!)

;; Schema diffing functions
(def make-migration* diffing/make-migration*)

;; File management helper functions
(def get-migration-number files/get-migration-number)
(def get-migration-type files/get-migration-type)
(def get-migration-name files/get-migration-name)
(def filter-new-edn-items files/filter-new-edn-items)
(def find-orphaned-migrations files/find-orphaned-migrations)

;; Constants that may be used by external code
(def EMPTY-SQL-MIGRATION-TYPE core/EMPTY-SQL-MIGRATION-TYPE)

;; Legacy wrapper functions to maintain exact API compatibility
(defn detailed-migration
  "Return detailed info for each migration file."
  [file-name]
  (status/detailed-migration file-name))

(defn get-detailed-migrations-to-migrate
  "Return migrations to migrate and migration direction."
  [all-migrations migrated target-number]
  (status/get-detailed-migrations-to-migrate all-migrations migrated target-number))

;; Migration execution wrapper to match the exact signature
(defn exec-action-wrapper
  [{:keys [db action migration-type]}]
  (exec/exec-action! {:db db
                      :action action
                      :migration-type migration-type}))

;; Backward compatibility for specific workflow functions
(defn create-migrations-dir!
  "Create migrations root dir if it does not exist."
  [migrations-dir]
  (when-not (.isDirectory (clojure.java.io/file migrations-dir))
    (.mkdir (java.io.File. migrations-dir))))

;; SQL migration parsing functions
(defn get-forward-sql-migration
  [migration]
  (exec/get-forward-sql-migration migration))

(defn get-backward-sql-migration
  [migration]
  (exec/get-backward-sql-migration migration))

;; Constants used by external code
(def ^{:deprecated "Use automigrate.generation.core instead"}
  RESOURCES-DIR "resources")
(def ^{:deprecated "Use automigrate.generation.core instead"}
  MODELS-FILE "db/models.edn")
(def ^{:deprecated "Use automigrate.generation.core instead"}
  MIGRATIONS-DIR "db/migrations")
(def ^{:deprecated "Use automigrate.generation.core instead"}
  MIGRATIONS-TABLE :automigrate-migrations)
(def ^{:deprecated "Use automigrate.generation.core instead"}
  FORWARD-DIRECTION :forward)
(def ^{:deprecated "Use automigrate.generation.core instead"}
  BACKWARD-DIRECTION :backward)

;; Delegate migration->actions to the new diffing namespace
(def migration->actions diffing/migration->actions)
