#!/usr/bin/env clj

(require '[clojure.java.shell :as shell]
  '[aero.core :as aero]
  '[clojure.string :as str])

(defn get-db-config [profile]
  (let [config (aero/read-config "config/base.edn" {:profile profile})
        db-config (:database config)]
    {:host (:host db-config)
     :port (:port db-config)
     :dbname (:dbname db-config)
     :user (:user db-config)
     :password (:password db-config)}))

(defn file-exists? [filepath]
  (.exists (java.io.File. filepath)))

(defn confirm-clean-restore [env backup-file]
  (println (str "⚠️  DANGER: This will COMPLETELY DROP and RECREATE the " (str/upper-case env) " database!"))
  (println (str "🗑️  ALL DATA in " (str/upper-case env) " database will be PERMANENTLY LOST!"))
  (println (str "📁 Backup file: " backup-file))
  (let [file-size (.length (java.io.File. backup-file))
        size-mb (/ file-size 1024.0 1024.0)]
    (println (str "📊 Backup size: " (format "%.2f" size-mb) " MB")))
  (println)
  (print "Type 'DROP DATABASE' to confirm this dangerous operation: ")
  (flush)
  (let [response (read-line)]
    (= "DROP DATABASE" (str/trim response))))

(defn drop-database [env db-config]
  (println (str "🗑️  Dropping " (str/upper-case env) " database: " (:dbname db-config)))

  ;; First terminate all connections to the target database
  (let [terminate-cmd ["/opt/homebrew/bin/psql"
                       "-h" (:host db-config)
                       "-p" (str (:port db-config))
                       "-U" (:user db-config)
                       "-d" "postgres"
                       "-c" (str "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '" (:dbname db-config) "' AND pid <> pg_backend_pid();")]
        env-vars {"PGPASSWORD" (:password db-config)}]

    (println "🔌 Terminating existing connections...")
    (apply shell/sh (concat terminate-cmd [:env env-vars])))

  ;; Drop the database
  (let [drop-cmd ["/opt/homebrew/bin/psql"
                  "-h" (:host db-config)
                  "-p" (str (:port db-config))
                  "-U" (:user db-config)
                  "-d" "postgres"
                  "-c" (str "DROP DATABASE IF EXISTS \"" (:dbname db-config) "\";")]
        env-vars {"PGPASSWORD" (:password db-config)}]

    (println (str "🗑️  Executing: DROP DATABASE " (:dbname db-config)))
    (let [result (apply shell/sh (concat drop-cmd [:env env-vars]))]
      (if (= 0 (:exit result))
        (println "✅ Database dropped successfully")
        (do
          (println "❌ Failed to drop database!")
          (when-not (str/blank? (:err result))
            (println "Error:" (:err result)))
          (throw (Exception. "Database drop failed")))))))

(defn create-database [env db-config]
  (println (str "🏗️  Creating new " (str/upper-case env) " database: " (:dbname db-config)))

  (let [create-cmd ["/opt/homebrew/bin/psql"
                    "-h" (:host db-config)
                    "-p" (str (:port db-config))
                    "-U" (:user db-config)
                    "-d" "postgres"
                    "-c" (str "CREATE DATABASE \"" (:dbname db-config) "\";")]
        env-vars {"PGPASSWORD" (:password db-config)}]

    (println (str "🏗️  Executing: CREATE DATABASE " (:dbname db-config)))
    (let [result (apply shell/sh (concat create-cmd [:env env-vars]))]
      (if (= 0 (:exit result))
        (println "✅ Database created successfully")
        (do
          (println "❌ Failed to create database!")
          (when-not (str/blank? (:err result))
            (println "Error:" (:err result)))
          (throw (Exception. "Database creation failed")))))))

(defn restore-from-backup [env backup-file db-config]
  (println (str "📥 Restoring from backup: " backup-file))

  (let [psql-cmd ["/opt/homebrew/bin/psql"
                  "-h" (:host db-config)
                  "-p" (str (:port db-config))
                  "-U" (:user db-config)
                  "-d" (:dbname db-config)
                  "--set" "ON_ERROR_STOP=on"
                  "-f" backup-file]
        env-vars {"PGPASSWORD" (:password db-config)}]

    (println (str "🚀 Executing restore..."))
    (let [result (apply shell/sh (concat psql-cmd [:env env-vars]))]
      (if (= 0 (:exit result))
        (do
          (println "✅ Backup restored successfully!")

          ;; Verify restore
          (let [verify-cmd ["/opt/homebrew/bin/psql"
                            "-h" (:host db-config)
                            "-p" (str (:port db-config))
                            "-U" (:user db-config)
                            "-d" (:dbname db-config)
                            "-t" "-c" "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';"]
                verify-result (apply shell/sh (concat verify-cmd [:env env-vars]))]
            (if (= 0 (:exit verify-result))
              (let [table-count (str/trim (:out verify-result))]
                (println (str "📊 Database contains " table-count " tables"))
                (println "🎉 Clean restore completed successfully!"))
              (println "⚠️  Could not verify restore (but restore completed)"))))
        (do
          (println "❌ Backup restore failed!")
          (when-not (str/blank? (:err result))
            (println "Error:" (:err result)))
          (throw (Exception. "Backup restore failed")))))))

(defn clean-restore-database [env backup-file]
  (let [db-config (get-db-config (keyword env))]

    (println (str "🔄 Starting CLEAN RESTORE of " (str/upper-case env) " database..."))
    (println (str "📁 Source: " backup-file))
    (println (str "🎯 Target: " (:dbname db-config) " on " (:host db-config) ":" (:port db-config)))
    (println)

    (try
      ;; Step 1: Drop existing database
      (drop-database env db-config)

      ;; Step 2: Create new empty database
      (create-database env db-config)

      ;; Step 3: Restore from backup
      (restore-from-backup env backup-file db-config)

      (println)
      (println "🎉 CLEAN RESTORE COMPLETED SUCCESSFULLY!")
      (println (str "✅ " (str/upper-case env) " database has been completely replaced with backup data"))

      (catch Exception e
        (println (str "❌ Clean restore failed: " (.getMessage e)))
        (System/exit 1)))))

(defn -main [& args]
  (if (= 2 (count args))
    (let [[env backup-file] args]
      (if (#{"dev" "test"} env)
        (if (file-exists? backup-file)
          (if (confirm-clean-restore env backup-file)
            (try
              (clean-restore-database env backup-file)
              (catch Exception e
                (println (str "❌ Error during clean restore: " (.getMessage e)))
                (when-let [cause (.getCause e)]
                  (println (str "   Caused by: " (.getMessage cause))))
                (System/exit 1)))
            (println "❌ Clean restore cancelled by user"))
          (do
            (println (str "❌ Backup file not found: " backup-file))
            (println "Available backup files:")
            (let [backups-dir (java.io.File. "backups")]
              (if (.exists backups-dir)
                (doseq [file (.listFiles backups-dir)
                        :when (and (.isFile file) (.endsWith (.getName file) ".sql"))]
                  (println (str "  - backups/" (.getName file))))
                (println "  No backups directory found")))
            (System/exit 1)))
        (do
          (println (str "❌ Invalid environment: " env))
          (println "Usage: clj -M scripts/clean_restore_db.clj [dev|test] backup_file.sql")
          (System/exit 1))))
    (do
      (println "Usage: clj -M scripts/clean_restore_db.clj [dev|test] backup_file.sql")
      (println "⚠️  WARNING: This COMPLETELY DROPS the target database before restore!")
      (println)
      (println "  dev         Clean restore to development database")
      (println "  test        Clean restore to test database")
      (println "  backup_file Path to SQL backup file")
      (println)
      (println "Examples:")
      (println "  clj -M scripts/clean_restore_db.clj dev backups/backup_dev_2025-06-27_19-58-16.sql")
      (println "  clj -M scripts/clean_restore_db.clj test backups/backup_dev_2025-06-27_19-58-16.sql")
      (println)
      (println "Available backup files:")
      (let [backups-dir (java.io.File. "backups")]
        (if (.exists backups-dir)
          (doseq [file (.listFiles backups-dir)
                  :when (and (.isFile file) (.endsWith (.getName file) ".sql"))]
            (println (str "  - backups/" (.getName file))))
          (println "  No backups directory found")))
      (System/exit 1))))

(apply -main *command-line-args*)
