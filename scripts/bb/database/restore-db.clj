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

(defn confirm-restore [env backup-file]
  (println (str "📥 About to RESTORE " (str/upper-case env) " database from backup"))
  (println (str "📁 Backup file: " backup-file))
  (let [file-size (.length (java.io.File. backup-file))
        size-mb (/ file-size 1024.0 1024.0)]
    (println (str "📊 Backup size: " (format "%.2f" size-mb) " MB")))
  (println)
  (print "Type 'RESTORE' to confirm this operation: ")
  (flush)
  (let [response (read-line)]
    (= "RESTORE" (str/trim response))))

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
                (println "🎉 Restore completed successfully!"))
              (println "⚠️  Could not verify restore (but restore completed)"))))
        (do
          (println "❌ Backup restore failed!")
          (when-not (str/blank? (:err result))
            (println "Error:" (:err result)))
          (throw (Exception. "Backup restore failed")))))))

(defn restore-database [env backup-file]
  (let [db-config (get-db-config (keyword env))]

    (println (str "🔄 Starting RESTORE of " (str/upper-case env) " database..."))
    (println (str "📁 Source: " backup-file))
    (println (str "🎯 Target: " (:dbname db-config) " on " (:host db-config) ":" (:port db-config)))
    (println)

    (try
      ;; Restore from backup
      (restore-from-backup env backup-file db-config)

      (println)
      (println "🎉 RESTORE COMPLETED SUCCESSFULLY!")
      (println (str "✅ " (str/upper-case env) " database has been restored from backup"))

      (catch Exception e
        (println (str "❌ Restore failed: " (.getMessage e)))
        (System/exit 1)))))

(defn -main [& args]
  (if (= 2 (count args))
    (let [[env backup-file] args]
      (if (#{"dev" "test"} env)
        (if (file-exists? backup-file)
          (if (confirm-restore env backup-file)
            (try
              (restore-database env backup-file)
              (catch Exception e
                (println (str "❌ Error during restore: " (.getMessage e)))
                (when-let [cause (.getCause e)]
                  (println (str "   Caused by: " (.getMessage cause))))
                (System/exit 1)))
            (println "❌ Restore cancelled by user"))
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
          (println "Usage: clj -M scripts/restore-db.clj [dev|test] backup_file.sql")
          (System/exit 1))))
    (do
      (println "Usage: clj -M scripts/restore-db.clj [dev|test] backup_file.sql")
      (println "📥 This restores a database from a backup file")
      (println)
      (println "  dev         Restore to development database")
      (println "  test        Restore to test database")
      (println "  backup_file Path to SQL backup file")
      (println)
      (println "Examples:")
      (println "  clj -M scripts/restore-db.clj dev backups/backup_dev_2025-06-27_19-58-16.sql")
      (println "  clj -M scripts/restore-db.clj test backups/backup_dev_2025-06-27_19-58-16.sql")
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
