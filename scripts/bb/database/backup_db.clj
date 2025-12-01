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

(defn generate-backup-filename [env]
  (let [timestamp (.format (java.time.LocalDateTime/now)
                    (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd_HH-mm-ss"))]
    (str "backup_" env "_" timestamp ".sql")))

(defn create-backup [env]
  (let [db-config (get-db-config (keyword env))
        backup-filename (generate-backup-filename env)
        backup-path (str "backups/" backup-filename)]

    (println (str "🔄 Creating backup of " (str/upper-case env) " database..."))
    (println (str "📁 Backup file: " backup-path))

    ;; Create backups directory if it doesn't exist
    (.mkdirs (java.io.File. "backups"))

    ;; Build pg_dump command
    (let [pg-dump-cmd ["/opt/homebrew/bin/pg_dump"
                       "-h" (:host db-config)
                       "-p" (str (:port db-config))
                       "-U" (:user db-config)
                       "-d" (:dbname db-config)
                       "--verbose"
                       "--clean"
                       "--if-exists"
                       "--create"
                       "--format=plain"
                       "-f" backup-path]
          env-vars {"PGPASSWORD" (:password db-config)}]

      (println (str "🚀 Running: " (str/join " " (take 8 pg-dump-cmd)) " ... -f " backup-path))

      (let [result (apply shell/sh (concat pg-dump-cmd [:env env-vars]))]
        (if (= 0 (:exit result))
          (do
            (println "✅ Backup completed successfully!")
            (let [file-size (.length (java.io.File. backup-path))
                  size-mb (/ file-size 1024.0 1024.0)]
              (println (str "📊 Backup size: " (format "%.2f" size-mb) " MB"))
              (println (str "📍 Location: " (.getAbsolutePath (java.io.File. backup-path))))))
          (do
            (println "❌ Backup failed!")
            (when-not (str/blank? (:err result))
              (println "Error output:")
              (println (:err result)))
            (System/exit 1)))))))

(defn -main [& args]
  (if-let [env (first args)]
    (if (#{"dev" "test"} env)
      (try
        (create-backup env)
        (catch Exception e
          (println (str "❌ Error creating backup: " (.getMessage e)))
          (when-let [cause (.getCause e)]
            (println (str "   Caused by: " (.getMessage cause))))
          (System/exit 1)))
      (do
        (println (str "❌ Invalid environment: " env))
        (println "Usage: clj -M scripts/backup_db.clj [dev|test]")
        (System/exit 1)))
    (do
      (println "Usage: clj -M scripts/backup_db.clj [dev|test]")
      (println "  dev   Backup development database")
      (println "  test  Backup test database")
      (System/exit 1))))

(apply -main *command-line-args*)
