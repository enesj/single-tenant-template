#!/usr/bin/env bb

(ns scripts.bb.database.seed-bh-cities
  (:require
    [aero.core :as aero]
    [clojure.java.shell :as shell]
    [clojure.string :as str]))

(defn get-db-config [profile]
  (let [config (aero/read-config "config/base.edn" {:profile profile})]
    (:database config)))

(defn run-seed [profile]
  (let [db (get-db-config profile)
        seed-file "resources/db/seeds/bh_cities.sql"
        _ (when-not (.exists (java.io.File. seed-file))
            (println "❌ Seed file not found:" seed-file)
            (System/exit 1))
        psql-cmd ["psql"
                  "-h" (:host db)
                  "-p" (str (:port db))
                  "-U" (:user db)
                  "-d" (:dbname db)
                  "-f" seed-file]
        env-vars {"PGPASSWORD" (:password db)}]

    (println (format "🚀 Seeding Bosnian cities into %s database (%s)..." 
                     (name profile) (:dbname db)))
    
    (let [result (apply shell/sh (concat psql-cmd [:env env-vars]))]
      (if (= 0 (:exit result))
        (do
          (println "✅ Seeding completed successfully!")
          (println (:out result)))
        (do
          (println "❌ Seeding failed!")
          (binding [*out* *err*]
            (println (:err result)))
          (System/exit 1))))))

(defn -main [& args]
  (let [env (or (first args) "dev")
        profile (keyword env)]
    (if (#{:dev :test} profile)
      (run-seed profile)
      (do
        (println "❌ Invalid environment. Use: dev or test")
        (System/exit 1)))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
