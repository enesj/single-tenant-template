#!/usr/bin/env clj

(ns scripts.bb.database.seed-bh-cities
  (:require
    [aero.core :as aero]
    [clojure.string :as str]
    [next.jdbc :as jdbc]))

(defn get-db-config [profile]
  (if (= profile :prod)
    ;; Prefer DATABASE_PUBLIC_URL (reachable from local machine via `railway run`)
    ;; over DATABASE_URL (private Railway network, only reachable from Railway services).
    {:jdbc-url (some-> (or (System/getenv "DATABASE_PUBLIC_URL")
                         (System/getenv "DATABASE_URL"))
                 str/trim not-empty
                 (str/replace #"^postgres://" "jdbc:postgresql://")
                 (str/replace #"^postgresql://" "jdbc:postgresql://"))}
    (:database (aero/read-config "config/base.edn" {:profile profile}))))

(defn run-seed [profile]
  (let [db (get-db-config profile)
        seed-file "resources/db/seeds/bh_cities.sql"
        _ (when-not (.exists (java.io.File. seed-file))
            (println "❌ Seed file not found:" seed-file)
            (System/exit 1))
        sql (slurp seed-file)
        ds (jdbc/get-datasource (if (:jdbc-url db)
                                  {:jdbcUrl (:jdbc-url db)}
                                  {:dbtype "postgresql"
                                   :host (:host db)
                                   :port (:port db)
                                   :dbname (:dbname db)
                                   :user (:user db)
                                   :password (:password db)}))]

    (println (format "🚀 Seeding Bosnian cities into %s database (%s)..."
               (name profile) (or (:dbname db) "DATABASE_URL")))

    (try
      ;; The seed file is one large INSERT ... VALUES ... statement.
      ;; It is idempotent (uses ON CONFLICT) so re-running is safe.
      (jdbc/with-transaction [tx ds]
        (jdbc/execute! tx [sql]))

      (let [{:keys [n]} (jdbc/execute-one! ds ["SELECT COUNT(*)::int AS n FROM cities"])]
        (println (format "✓ Seeded BH cities (total in DB: %d)" n))
        (println "Done!"))

      (catch Exception e
        (println "❌ Seeding failed!")
        (println (.getMessage e))
        (System/exit 1)))))

(defn -main [& args]
  (let [env (or (first args) "dev")
        profile (keyword env)]
    (when (and (= profile :prod) (empty? (System/getenv "DATABASE_URL")))
      (println "❌ DATABASE_URL env var is required for prod. Use: railway run clj -M scripts/bb/database/seed_bh_cities.clj prod")
      (System/exit 1))
    (if (#{:dev :test :prod} profile)
      (run-seed profile)
      (do
        (println "❌ Invalid environment. Use: dev, test, or prod")
        (System/exit 1)))))

(apply -main *command-line-args*)
