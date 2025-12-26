#!/usr/bin/env bb

(ns clean-and-init-dev-db
  "Clean and reinitialize development database"
  (:require
   [babashka.process :as process]
   [clojure.string :as str]))

(def db-config
  {:host "localhost"
   :port 5432
   :database "bookkeeping"
   :user "user"
   :superuser "enes"})

(defn run-sql [sql & {:keys [as-superuser?]}]
  (let [user (if as-superuser? (:superuser db-config) (:user db-config))]
    (println "Running: psql -h" (:host db-config) "-p" (:port db-config) "-U" user "-d" (:database db-config) "-c" "<SQL>")
    (let [result (process/shell {:out :string :err :string}
                   "/opt/homebrew/bin/psql" "-h" (:host db-config)
                   "-p" (str (:port db-config))
                   "-U" user
                   "-d" (:database db-config)
                   "-c" sql)]
      (if (zero? (:exit result))
        (do
          (println "✅ Success")
          (when (not-empty (:out result))
            (println (:out result))))
        (do
          (println "❌ Error:")
          (println (:err result))
          (throw (ex-info "SQL execution failed" {:result result})))))))

(defn clean-database []
  (println "\n🧹 Cleaning existing database data...")

  ;; Delete all data from tables in dependency order (preserving schema)
  (run-sql "
    DELETE FROM price_observations;
    DELETE FROM article_aliases;
    DELETE FROM expense_items;
    DELETE FROM expenses;
    DELETE FROM receipts;
    DELETE FROM articles;
    DELETE FROM payers;
    DELETE FROM suppliers;
    DELETE FROM audit_logs;
    DELETE FROM login_events;
    DELETE FROM email_verification_tokens;
    DELETE FROM password_reset_tokens;
    DELETE FROM admin_sessions;
    DELETE FROM admins;
    DELETE FROM users;
  " :as-superuser? true))

(defn init-dev-data []
  (println "\n🔧 Seeding minimum development data...")
  (let [result (process/shell {:out :string :err :string}
                 "bb" "scripts/bb/database/seed_admin.clj" "dev")]
    (if (zero? (:exit result))
      (do
        (println "✅ Development seeding completed")
        (println (:out result)))
      (do
        (println "❌ Development seeding failed:")
        (println (:err result))
        (throw (ex-info "Development seeding failed" {:result result}))))))

(defn main []
  (println "🚀 Clean and seed development database...")
  (println "⚠️  This will TRUNCATE ALL DATA in the bookkeeping database (schema preserved)!")
  (println "⚠️  Make sure you have already applied migrations via REPL (mig/migrate!)")

  (print "Are you sure you want to continue? (y/N): ")
  (flush)
  (let [response (read-line)]
    (if (= (str/lower-case response) "y")
      (do
        (clean-database)
        (init-dev-data)
        (println "\n🎉 Database cleaned and seeded successfully!")
        (println "You can now start your application: ./scripts/run-app.sh"))
      (println "Operation cancelled."))))

(when (= *file* (System/getProperty "babashka.file"))
  (main))
