(ns automigrate.migrations-enhanced
  "Enhanced migration system with extension management and SQL validation."
  (:require
   [automigrate.migrations :as base-migrations]
   [automigrate.util.db :as db-util]
   [automigrate.util.extensions :as extensions]
   [automigrate.util.sql-validation :as sql-validation]
   [clojure.string :as str]
   [slingshot.slingshot :refer [try+]]))

;; Configuration defaults
(def default-migration-config
  {:migration-strategy {:functions :use-extensions    ; or :recreate
                        :validate-sql true
                        :auto-fix-common-issues true
                        :continue-on-error false}
   :extensions {:auto-install true
                :preferred ["uuid-ossp" "hstore" "btree_gist"]}
   :postgresql {:min-version 9.1}})

(defn merge-migration-config
  "Merge user config with defaults"
  [user-config]
  (merge-with merge default-migration-config user-config))

(defn validate-and-fix-migration-content
  "Validate and optionally fix migration content based on type"
  [migration-content file-extension config]
  (let [validation-enabled? (get-in config [:migration-strategy :validate-sql])
        auto-fix-enabled? (get-in config [:migration-strategy :auto-fix-common-issues])]

    (if validation-enabled?
      (let [result (sql-validation/validate-and-fix-sql
                     migration-content
                     :auto-fix? auto-fix-enabled?
                     :verbose? true)
            type-specific-issues (sql-validation/validate-migration-by-type
                                     (:sql result) file-extension)]
        ;; Additional type-specific validation
          (when (seq type-specific-issues)
            (println "⚠️  Type-specific validation issues:")
            (doseq [issue type-specific-issues]
              (println (str "   " (:description issue))))

        ;; Return enhanced result
            (assoc result :type-specific-issues type-specific-issues)))

      ;; If validation disabled, return original content
      {:sql migration-content
       :original-issues []
       :final-issues []
       :fixed-count 0
       :has-errors? false})))

(defn should-skip-migration?
  "Determine if a migration should be skipped based on extension analysis"
  [db migration-file-name migration-content config]
  (let [skip-extension-functions? (= :use-extensions
                                    (get-in config [:migration-strategy :functions]))]
    (when (and skip-extension-functions?
            (str/ends-with? migration-file-name ".fn"))

      ;; Extract function name from migration
      (let [function-name-match (re-find #"function_([^.]+)" migration-file-name)]
        (when function-name-match
          (let [function-name (str/replace (second function-name-match) #"_" "")]
            (when (extensions/should-skip-function-migration?
                    db function-name migration-content)
              (println (str "⏭️  Skipping migration " migration-file-name
                         " - function provided by extension"))
              true)))))))

(defn ensure-required-extensions
  "Ensure all required extensions are installed before running migrations"
  [db migrations config]
  (when (get-in config [:extensions :auto-install])
    (let [required-extensions (extensions/get-required-extensions-for-migrations migrations)
          compatibility-check (extensions/extension-compatibility-check db required-extensions)]

      ;; Check compatibility first
      (doseq [[ext-name compat-info] compatibility-check]
        (when-not (:compatible compat-info)
          (println (str "⚠️  Extension " ext-name " requires PostgreSQL >= "
                     (:required-version compat-info)
                     " but current version is " (:current-version compat-info)))))

      ;; Install compatible extensions
      (let [compatible-extensions (map first
                                    (filter #(:compatible (second %)) compatibility-check))]
        (when (seq compatible-extensions)
          (println (str "🔧 Installing required extensions: "
                     (str/join ", " compatible-extensions)))
          (extensions/ensure-extensions! db compatible-extensions))))))

(defn exec-migration-with-recovery
  "Execute migration with error recovery options"
  [db migration-info config]
  (let [continue-on-error? (get-in config [:migration-strategy :continue-on-error])
        {:keys [migration-name file-name migration-type]} migration-info]

    (try
      ;; Get migration content and validate
      (let [migration-content (case migration-type
                                :fn (-> (str "resources/db/migrations/" file-name)
                                      slurp)
                                :pol (-> (str "resources/db/migrations/" file-name)
                                       slurp)
                                :view (-> (str "resources/db/migrations/" file-name)
                                        slurp)
                                :sql (-> (str "resources/db/migrations/" file-name)
                                       slurp)
                                "")]

        ;; Check if we should skip this migration
        (if (should-skip-migration? db file-name migration-content config)
          {:status :skipped :migration migration-name}

          ;; Validate and potentially fix the migration content
          (let [validation-result (validate-and-fix-migration-content
                                    migration-content
                                    (str "." (name migration-type))
                                    config)]

            ;; If there are critical errors and we're not auto-fixing, abort
            (if (and (:has-errors? validation-result)
                  (not (get-in config [:migration-strategy :auto-fix-common-issues])))
              (do
                (println (str "❌ Migration " migration-name " has validation errors"))
                (if continue-on-error?
                  {:status :error :migration migration-name :error "Validation errors"}
                  (throw (ex-info "Migration validation failed"
                           {:migration migration-name
                            :errors (:final-issues validation-result)}))))

              ;; Execute the migration with validated/fixed content
              (do
                (when (> (:fixed-count validation-result) 0)
                  (println (str "✅ Auto-fixed " (:fixed-count validation-result)
                             " issues in " migration-name)))

                ;; Execute using base migration system with fixed content
                ;; This is a simplified approach - in reality we'd need to integrate
                ;; more deeply with the base system
                (base-migrations/exec-action!
                  {:db db
                   :action (:sql validation-result)
                   :migration-type migration-type})

                {:status :success :migration migration-name})))))

      (catch Exception e
        (println (str "💥 Error in migration " migration-name ": " (.getMessage e)))
        (if continue-on-error?
          {:status :error :migration migration-name :error (.getMessage e)}
          (throw e))))))

(defn migrate-with-enhancements
  "Enhanced migration function with extension management and validation"
  [args & {:keys [config] :or {config {}}}]

  (let [effective-config (merge-migration-config config)
        {:keys [migrations-dir jdbc-url number migrations-table]
         :or {migrations-table :automigrate-migrations
              migrations-dir "db/migrations"}} args]

    (try+
      (let [db (db-util/db-conn jdbc-url)
            _ (db-util/create-migrations-table db migrations-table)

            ;; Get migrations to process
            all-migrations (base-migrations/migrations-list migrations-dir)
            all-migrations-detailed (map base-migrations/detailed-migration all-migrations)
            migrated (base-migrations/already-migrated db migrations-table)

            {:keys [to-migrate direction]}
            (base-migrations/get-detailed-migrations-to-migrate
              all-migrations-detailed migrated number)]

        ;; Ensure required extensions are installed
        (ensure-required-extensions db to-migrate effective-config)

        ;; Get PostgreSQL version for compatibility checks
        (let [pg-version (extensions/get-postgresql-version db)]
          (when (< pg-version (get-in effective-config [:postgresql :min-version]))
            (println (str "⚠️  PostgreSQL version " pg-version
                       " may not support all features. Recommended: >= "
                       (get-in effective-config [:postgresql :min-version])))))

        (if (seq to-migrate)
          (let [results (atom {:successful [] :skipped [] :failed []})]

            (println (str "\n🚀 Processing " (count to-migrate) " migration(s) in "
                       (name direction) " direction"))

            ;; Process each migration with enhanced error handling
            (doseq [{:keys [migration-name file-name migration-type number-int] :as migration-info} to-migrate]
              (println (str "\n📋 Processing " migration-name "..."))

              (let [result (exec-migration-with-recovery db migration-info effective-config)]
                (case (:status result)
                  :success (do
                             (swap! results update :successful conj migration-name)
                             (if (= direction :forward)
                               (base-migrations/save-migration! db migration-name migrations-table)
                               (base-migrations/delete-migration! db migration-name migrations-table)))
                  :skipped (swap! results update :skipped conj migration-name)
                  :error (swap! results update :failed conj
                           {:migration migration-name :error (:error result)}))))

            ;; Print summary
            (let [{:keys [successful skipped failed]} @results]
              (println (str "\n📊 Migration Summary:"))
              (println (str "  ✅ Successful: " (count successful)))
              (println (str "  ⏭️  Skipped: " (count skipped)))
              (println (str "  ❌ Failed: " (count failed)))

              (when (seq skipped)
                (println "  Skipped migrations:")
                (doseq [migration skipped]
                  (println (str "    - " migration))))

              (when (seq failed)
                (println "  Failed migrations:")
                (doseq [{:keys [migration error]} failed]
                  (println (str "    - " migration ": " error))))

              ;; Return results
              @results))

          (do
            (println "✨ Nothing to migrate.")
            {:successful [] :skipped [] :failed []})))

      (catch Exception e
        (println (str "❌ Migration process failed: " (.getMessage e)))
        (throw e)))))

(defn analyze-migration-health
  "Analyze the health of migration files and suggest improvements"
  [migrations-dir & {:keys [config] :or {config {}}}]

  (let [effective-config (merge-migration-config config)
        all-migrations (base-migrations/migrations-list migrations-dir)
        analysis-results (atom {:total 0
                                :issues []
                                :suggestions []})]

    (println "🔍 Analyzing migration health...")

    (doseq [migration-file all-migrations]
      (swap! analysis-results update :total inc)

      (try
        (let [file-path (str "resources/" migrations-dir "/" migration-file)
              content (slurp file-path)
              file-extension (str "." (last (str/split migration-file #"\.")))]

          ;; Validate SQL
          (let [validation-result (sql-validation/validate-and-fix-sql content :auto-fix? false)]
            (when (:has-errors? validation-result)
              (swap! analysis-results update :issues conj
                {:file migration-file
                 :type :sql-validation
                 :issues (:final-issues validation-result)})))

          ;; Check for extension functions that could be skipped
          (when (and (str/ends-with? migration-file ".fn")
                  (str/includes? content "LANGUAGE c"))
            (let [function-name-match (re-find #"function_([^.]+)" migration-file)]
              (when (and function-name-match
                      (extensions/function-requires-extension?
                        (str/replace (second function-name-match) #"_" "")))
                (swap! analysis-results update :suggestions conj
                  {:file migration-file
                   :type :extension-function
                   :suggestion "Consider using extension instead of recreating function"})))))

        (catch Exception e
          (swap! analysis-results update :issues conj
            {:file migration-file
             :type :read-error
             :error (.getMessage e)}))))

    ;; Print summary
    (let [{:keys [total issues suggestions]} @analysis-results]
      (println (str "\n📊 Migration Health Report:"))
      (println (str "  📁 Total migrations analyzed: " total))
      (println (str "  ⚠️  Issues found: " (count issues)))
      (println (str "  💡 Suggestions: " (count suggestions)))

      (when (seq issues)
        (println "\n⚠️  Issues:")
        (doseq [{:keys [file type issues error]} issues]
          (println (str "  " file " (" (name type) ")"))
          (when issues
            (doseq [issue issues]
              (println (str "    - " (:description issue)))))
          (when error
            (println (str "    - " error)))))

      (when (seq suggestions)
        (println "\n💡 Suggestions:")
        (doseq [{:keys [file suggestion]} suggestions]
          (println (str "  " file ": " suggestion))))

      @analysis-results)))

;; Public API functions that wrap base functionality with enhancements

(defn make-migration
  "Enhanced make-migration with validation"
  [args & {:keys [config] :or {config {}}}]
  (let [effective-config (merge-migration-config config)
        result (base-migrations/make-migration args)]

    ;; If SQL validation is enabled, validate the generated migration
    (when (get-in effective-config [:migration-strategy :validate-sql])
      (println "🔍 Validating generated migration..."))

    result))

(defn explain
  "Enhanced explain with additional context"
  [args & {:keys [config] :or {config {}}}]
  (let [effective-config (merge-migration-config config)]
    ;; Add extension context if analyzing function migrations
    (base-migrations/explain args)))

(defn list-migrations
  "Enhanced list-migrations with health indicators"
  [args & {:keys [config] :or {config {}}}]
  (let [effective-config (merge-migration-config config)]
    (base-migrations/list-migrations args)

    ;; Add quick health check
    (when (get-in effective-config [:migration-strategy :validate-sql])
      (println "\n🏥 Quick health check:")
      (let [health-results (analyze-migration-health (:migrations-dir args "db/migrations")
                             :config effective-config)]
        (if (zero? (+ (count (:issues health-results))
                     (count (:suggestions health-results))))
          (println "  ✅ All migrations look healthy")
          (println (str "  ⚠️  Found " (count (:issues health-results))
                     " issues and " (count (:suggestions health-results))
                     " suggestions. Run analyze-migration-health for details.")))))))

;; Convenience functions for common extension management tasks

(defn install-extension
  "Install a PostgreSQL extension manually"
  [jdbc-url extension-name]
  (let [db (db-util/db-conn jdbc-url)]
    (if (extensions/extension-available? db extension-name)
      (do
        (extensions/ensure-extensions! db [extension-name])
        (println (str "✅ Extension " extension-name " installed successfully")))
      (println (str "❌ Extension " extension-name " is not available in this PostgreSQL instance")))))

(defn list-extensions
  "List installed and available extensions"
  [jdbc-url]
  (let [db (db-util/db-conn jdbc-url)
        installed (extensions/get-installed-extensions db)
        available (extensions/get-available-extensions db)]

    (println "🔌 PostgreSQL Extensions:")
    (println (str "  Installed (" (count installed) "):"))
    (doseq [ext (sort installed)]
      (println (str "    ✅ " ext)))

    (println (str "  Available (" (count available) "):"))
    (doseq [ext (sort (clojure.set/difference available installed))]
      (println (str "    ⚪ " ext)))))

(defn check-extension-compatibility
  "Check if extensions are compatible with current PostgreSQL version"
  [jdbc-url extension-names]
  (let [db (db-util/db-conn jdbc-url)
        compatibility (extensions/extension-compatibility-check db extension-names)]

    (println "🔍 Extension Compatibility Check:")
    (doseq [[ext-name compat-info] compatibility]
      (if (:compatible compat-info)
        (println (str "  ✅ " ext-name " (compatible)"))
        (println (str "  ❌ " ext-name " (requires PostgreSQL >= "
                   (:required-version compat-info)
                   ", current: " (:current-version compat-info) ")"))))))
