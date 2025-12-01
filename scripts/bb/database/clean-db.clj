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

(defn confirm-clean-database [env]
  (println (str "⚠️  DANGER: This will COMPLETELY DROP the " (str/upper-case env) " database!"))
  (println (str "🗑️  ALL DATA in " (str/upper-case env) " database will be PERMANENTLY LOST!"))
  (println)
  (print "Type 'DROP DATABASE' to confirm this dangerous operation: ")
  (flush)
  (let [response (read-line)]
    (= "DROP DATABASE" (str/trim response))))

(defn drop-database [env db-config]
  (println (str "🗑️  Dropping " (str/upper-case env) " database: " (:dbname db-config)))

  ;; First terminate all connections to the target database (using superuser)
  (let [terminate-cmd ["/opt/homebrew/bin/psql"
                       "-h" (:host db-config)
                       "-p" (str (:port db-config))
                       "-U" "enes"  ;; Use superuser for database operations
                       "-d" "postgres"
                       "-c" (str "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '" (:dbname db-config) "' AND pid <> pg_backend_pid();")]]

    (println "🔌 Terminating existing connections...")
    (apply shell/sh terminate-cmd))

  ;; Drop the database (using superuser)
  (let [drop-cmd ["/opt/homebrew/bin/psql"
                  "-h" (:host db-config)
                  "-p" (str (:port db-config))
                  "-U" "enes"  ;; Use superuser for database operations
                  "-d" "postgres"
                  "-c" (str "DROP DATABASE IF EXISTS \"" (:dbname db-config) "\";")]]

    (println (str "🗑️  Executing: DROP DATABASE " (:dbname db-config)))
    (let [result (apply shell/sh drop-cmd)]
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
                    "-U" "enes"  ;; Use superuser for database operations
                    "-d" "postgres"
                    "-c" (str "CREATE DATABASE \"" (:dbname db-config) "\" OWNER \"" (:user db-config) "\";")]]

    (println (str "🏗️  Executing: CREATE DATABASE " (:dbname db-config)))
    (let [result (apply shell/sh create-cmd)]
      (if (= 0 (:exit result))
        (println "✅ Database created successfully")
        (do
          (println "❌ Failed to create database!")
          (when-not (str/blank? (:err result))
            (println "Error:" (:err result)))
          (throw (Exception. "Database creation failed")))))))

(defn clean-database [env]
  (let [db-config (get-db-config (keyword env))]

    (println (str "🔄 Starting CLEAN of " (str/upper-case env) " database..."))
    (println (str "🎯 Target: " (:dbname db-config) " on " (:host db-config) ":" (:port db-config)))
    (println)

    (try
      ;; Step 1: Drop existing database
      (drop-database env db-config)

      ;; Step 2: Create new empty database
      (create-database env db-config)


      (println)
      (println "🎉 DATABASE CLEAN COMPLETED SUCCESSFULLY!")
      (println (str "✅ " (str/upper-case env) " database has been dropped and recreated as empty"))

      (catch Exception e
        (println (str "❌ Database clean failed: " (.getMessage e)))
        (System/exit 1)))))

(defn -main [& args]
  (if (= 1 (count args))
    (let [[env] args]
      (if (#{"dev" "test"} env)
        (if (confirm-clean-database env)
          (try
            (clean-database env)
            (catch Exception e
              (println (str "❌ Error during database clean: " (.getMessage e)))
              (when-let [cause (.getCause e)]
                (println (str "   Caused by: " (.getMessage cause))))
              (System/exit 1)))
          (println "❌ Database clean cancelled by user"))
        (do
          (println (str "❌ Invalid environment: " env))
          (println "Usage: clj -M scripts/clean-db.clj [dev|test]")
          (System/exit 1))))
    (do
      (println "Usage: clj -M scripts/clean-db.clj [dev|test]")
      (println "⚠️  WARNING: This COMPLETELY DROPS and recreates the target database!")
      (println)
      (println "  dev         Clean development database")
      (println "  test        Clean test database")
      (println)
      (println "Examples:")
      (println "  clj -M scripts/clean-db.clj dev")
      (println "  clj -M scripts/clean-db.clj test")
      (System/exit 1))))

(apply -main *command-line-args*)
