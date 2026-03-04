#!/usr/bin/env clj

(ns scripts.bb.database.seed-bh-cities
  (:require
    [aero.core :as aero]
    [next.jdbc :as jdbc]))

(defn get-db-config [profile]
  (let [config (aero/read-config "config/base.edn" {:profile profile})]
    (:database config)))

(defn run-seed [profile]
  (let [db (get-db-config profile)
        seed-file "resources/db/seeds/bh_cities.sql"
        _ (when-not (.exists (java.io.File. seed-file))
            (println "❌ Seed file not found:" seed-file)
            (System/exit 1))
        sql (slurp seed-file)
        ds (jdbc/get-datasource {:dbtype "postgresql"
                                 :host (:host db)
                                 :port (:port db)
                                 :dbname (:dbname db)
                                 :user (:user db)
                                 :password (:password db)})]

    (println (format "🚀 Seeding Bosnian cities into %s database (%s)..."
               (name profile) (:dbname db)))

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
    (if (#{:dev :test} profile)
      (run-seed profile)
      (do
        (println "❌ Invalid environment. Use: dev or test")
        (System/exit 1)))))

(apply -main *command-line-args*)
