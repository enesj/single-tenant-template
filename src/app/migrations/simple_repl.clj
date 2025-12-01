(ns app.migrations.simple-repl
  "REPL-friendly utilities for database migrations (schema + extended).

   What this namespace provides:
   - Config-aware JDBC URL helpers
   - Schema migration from hierarchical models (template/shared/domain)
   - Extended migrations from EDN (functions, triggers, policies, views)
   - Status/list/explain commands
   - Duplicate-number checks and migration summaries
   - One-time DB→EDN sync helpers for existing DBs
   - Safe regeneration helpers for resolving duplicate numbers

   Usage (quick):
   (require '[app.migrations.simple-repl :as mig])
   (mig/make-all-migrations!)
   (mig/migrate!)
   (mig/status)

   See the comment block at the bottom for scenarios."
  (:require
    [aero.core :as aero]
    [app.backend.admin-setup :as setup]
    [app.migrations.hierarchical-models :as hierarchical-models]
    [app.shared.model-customizations :as model-cust]
    [automigrate.core :as am]
    [automigrate.generation.extended :as gen-ext]
    [automigrate.util.db :as db-util]
    [clojure.java.io :as io]
    [clojure.pprint :as pprint]
    [clojure.string :as str]))

(defn get-jdbc-url
  "Return JDBC URL for profile (default :dev) from config. Falls back to env."
  ([] (get-jdbc-url :dev))
  ([profile]
   (let [file (io/file "config/base.edn")
         cfg  (cond
                (.exists file) (aero/read-config file {:profile profile})
                (io/resource "config/base.edn") (aero/read-config (io/resource "config/base.edn") {:profile profile})
                (io/resource "base.edn") (aero/read-config (io/resource "base.edn") {:profile profile})
                :else nil)
         env (System/getenv "DATABASE_URL")]
     (or (get-in cfg [:database :jdbc-url])
       (when cfg
         (format "jdbc:postgresql://%s:%s/%s?user=%s&password=%s"
           (get-in cfg [:database :host] "localhost")
           (get-in cfg [:database :port] 5432)
           (get-in cfg [:database :dbname])
           (get-in cfg [:database :user])
           (get-in cfg [:database :password])))
       env))))

(def ^:private resources-dir "resources")
(def ^:private migrations-dir "db/migrations")
(def ^:private project-dir (System/getProperty "user.dir"))
(def ^:private db-dir (str project-dir "/resources/db"))
(def ^:private models-file (str db-dir "/models.edn"))

(defn make-extended-migrations!
  "Generate extended migration files (functions, triggers, policies, views) from EDN files."
  ([]
   (gen-ext/generate-extended-migrations-from-edn!
     {:resources-dir resources-dir
      :migrations-dir migrations-dir})
   (println "Extended migrations generated in resources/db/migrations")))

(defn sync-db-to-edn!
  "One-time DB→EDN sync. Extracts functions, triggers, policies, views from DB
   and writes EDN files under `resources/db/{functions,triggers,policies,views}`.
   Use to capture the current DB state into source-controlled EDN."
  ([] (sync-db-to-edn! :dev))
  ([profile]
   (let [jdbc-url (get-jdbc-url profile)
         db (db-util/db-conn jdbc-url)]
     (println "Syncing database objects to EDN files...")
     (let [summary (gen-ext/sync-db-to-edn-files! {:db db :resources-dir resources-dir})]
       (println (format (str "\nSynced to EDN files:\n"
                          "  Functions: %d\n  Triggers: %d\n  Policies: %d\n  Views: %d\n  Total: %d")
                  (:functions summary)
                  (:triggers summary)
                  (:policies summary)
                  (:views summary)
                  (:total summary)))))))

(defn merge-hierarchical-models!
  "Merge template/domain/shared models into consolidated `resources/db/models.edn`."
  []
  (let [_ (io/make-parents (str db-dir "/.keep"))
        merged-raw (hierarchical-models/read-hierarchical-edn db-dir "models.edn")
        ;; Strip admin configuration for clean separation
        merged (model-cust/strip-all-admin-config merged-raw)]
    (when (.exists (io/file models-file))
      (.delete (io/file models-file)))
    (with-open [w (io/writer models-file)]
      (binding [*print-length* nil *print-level* nil *out* w]
        (pprint/pprint merged)))
    (println (format "Consolidated models written to %s (%d models)"
               models-file (count merged)))
    merged))

(defn make-schema-migration!
  "Generate schema migration from consolidated models with function-default preprocessing."
  [& {:keys [name]}]
  (let [models-file-path (str resources-dir "/db/models.edn")
        temp-models-file (str resources-dir "/db/models_processed.edn")
        preprocessor (requiring-resolve 'app.migrations.function-defaults/load-and-preprocess-models)]
    (try
      (let [processed (preprocessor models-file-path)]
        (spit temp-models-file (with-out-str (pprint/pprint processed))))
      (am/make {:models-file "db/models_processed.edn"
                :resources-dir resources-dir
                :name (or name "schema")})
      (finally
        (when (.exists (io/file temp-models-file))
          (.delete (io/file temp-models-file)))))))

(defn make-all-migrations!
  "End-to-end generation: merge models → schema migration → extended migrations."
  []
  (merge-hierarchical-models!)
  (make-schema-migration!)
  (Thread/sleep 300)
  (make-extended-migrations!))

(defn prune-migrations-to-base!
  "Dangerous: remove all migration files after base (0001_schema.edn and 0002_enable_hstore_extension.sql) to avoid
   duplicate numbers before regenerating extended migrations. Use only when you intend to fully regenerate.
   Returns list of removed files."
  []
  (let [dir (io/file (str resources-dir "/" migrations-dir))
        files (->> (.listFiles dir)
                (filter #(.isFile %))
                (map #(.getName %)))
        keep? (fn [n]
                (or (str/starts-with? n "0001_schema.edn")
                  (str/ends-with? n "_enable_hstore_extension.sql")))
        to-del (remove keep? files)]
    (doseq [f to-del]
      (io/delete-file (str resources-dir "/" migrations-dir "/" f) true))
    to-del))

(defn regenerate-extended-migrations-clean!
  "Prune extended migrations to base, then regenerate extended migrations from EDN."
  []
  (let [removed (prune-migrations-to-base!)]
    (when (seq removed)
      (println (format "Removed %d migration(s) beyond base." (count removed))))
    (make-extended-migrations!)))

(defn check-duplicate-migrations
  "Check for duplicate migration numbers and print them."
  []
  (let [migrations-dir "resources/db/migrations"
        files (.listFiles (io/file migrations-dir))
        file-names (map #(.getName %) files)
        numbers (keep #(when-let [match (re-find #"^(\d+)_" %)]
                         [(Integer/parseInt (second match)) %])
                  file-names)
        grouped (group-by first numbers)
        duplicates (filter #(> (count (val %)) 1) grouped)]
    (if (empty? duplicates)
      (println "No duplicate migration numbers found.")
      (do
        (println "Found duplicate migration numbers:")
        (doseq [[num files] duplicates]
          (println (format "Number %04d is used by:" num))
          (doseq [[_ filename] files]
            (println (str "  - " filename))))))))

(defn next-migration-number
  "Return the next migration number based on files in resources/db/migrations."
  []
  (let [files (map #(.getName %) (.listFiles (io/file (str resources-dir "/" migrations-dir))))
        numbers (keep #(when-let [m (re-find #"^(\d+)_" %)]
                         (Integer/parseInt (second m))) files)]
    (inc (or (apply max numbers) 0))))

(defn migration-summary
  "Print a summary of migrations by type."
  []
  (let [files (map #(.getName %) (.listFiles (io/file (str resources-dir "/" migrations-dir))))
        by-type (group-by #(cond
                             (str/ends-with? % ".edn") :schema
                             (str/ends-with? % ".sql") :sql
                             (str/ends-with? % ".fn") :function
                             (str/ends-with? % ".trg") :trigger
                             (str/ends-with? % ".pol") :policy
                             (str/ends-with? % ".view") :view
                             :else :other)
                  files)]
    (println "Migration Summary:")
    (doseq [[type files] (sort by-type)]
      (println (format "  %-10s %3d" (name type) (count files))))
    (println (format "  %-10s %3d" "TOTAL" (count files)))))

(defn migrate!
  "Run all pending migrations for the given profile (default :dev)."
  ([] (migrate! :dev))
  ([profile]
   (am/migrate {:jdbc-url (get-jdbc-url profile)})))

(defn status
  "Show migration status for the given profile (default :dev)."
  ([] (status :dev))
  ([profile]
   (am/list {:jdbc-url (get-jdbc-url profile)})))

(defn migrate-to!
  "Migrate to specific version (use 0 to rollback all)."
  ([version] (migrate-to! :dev version))
  ([profile version]
   (am/migrate {:jdbc-url (get-jdbc-url profile)
                :number version})))

(defn explain
  "Show SQL for a specific migration number. direction: :forward or :backward"
  [number & {:keys [direction]
             :or {direction :forward}}]
  (am/explain {:number number :direction direction :format :sql}))

;; Convenience aliases
(def m! migrate!)
(def s status)
(def make! make-all-migrations!)

(comment
  ;; Scenarios
  ;; =========
  ;; 1) Fresh start with empty DB (dev)

  ;; - Clean DB externally (recommended):
  ;;     bb clean-db --dev
  ;; - Generate migrations from source:
  ;;     (make-all-migrations!)3
  ;; - Apply migrations:
  ;;     (migrate!)
  ;; - Verify:
  ;;     (status)
  (make-all-migrations!)
  (migrate!)

;; 2) Resolve duplicate migration numbers (regenerate extended only)

  ;; - Keep base schema; prune extended migrations:
  ;;     (regenerate-extended-migrations-clean!)
  ;; - Apply (if needed):
  ;;     (migrate!)
  ;; - Check:
  ;;     (check-duplicate-migrations)
  (regenerate-extended-migrations-clean!)

;; 3) Normal development loop

  ;; - Edit EDN under resources/db/{template,shared,domain}
  ;; - Generate:
  ;;     (make-all-migrations!)
  ;; - Apply:
  ;;     (migrate!)
  ;; - Inspect or explain:
  ;;     (status)
  ;;     (explain 42)
  (make-all-migrations!)
  (migrate!)

;;
  ;; 4) One-time capture of existing DB objects to EDN
  ;; - Useful when onboarding an existing DB:
  ;;     (sync-db-to-edn!)
  ;; - Then commit EDN and generate extended migrations as needed.
  ;;
  ;; 5) Migrate to a specific version
  ;; - Roll back all:
  ;;     (migrate-to! 0)
  (migrate-to! 0)
  ;; - Migrate up to N (inclusive):
  ;;     (migrate-to! 10)
  (migrate-to! 10)

  ;; Quick setup - creates admin user and test data
  (setup/setup-all!)

  ;; Or step by step:
  ;; Create admin user only
  (setup/setup-test-admin!)

  ;; Create test data only
  (setup/create-test-data!))
  ;;
  ;; Notes
  ;; - RLS enablement: define `enable_rls_on_tenant_tables` in
  ;;   `resources/db/shared/policies.edn`. The extended generator creates a .pol
  ;;   migration to run the ALTER TABLE ... ENABLE/DISABLE statements.
  ;; - Avoid manual edits inside `resources/db/migrations/`. Use these utilities
  ;;   to regenerate when needed. Pruning is intentionally explicit.
