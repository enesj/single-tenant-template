#!/usr/bin/env bb

(ns list-tenants
  "Simple script to list all tenants in the database"
  (:require
   [babashka.process :as process]))

;; Configuration with environment variable support
(def db-config
  {:host (or (System/getenv "DB_HOST") "localhost")
   :port (Integer/parseInt (or (System/getenv "DB_PORT") "5432"))
   :database (or (System/getenv "DB_NAME") "bookkeeping")
   :user (or (System/getenv "DB_USER") "user")
   :password (System/getenv "DB_PASSWORD")})

(defn run-query [sql]
  (let [env (if (:password db-config)
              {"PGPASSWORD" (:password db-config)}
              {})]
    (try
      (let [result (process/shell {:out :string :err :string :extra-env env}
                     "psql" "-h" (:host db-config)
                     "-p" (str (:port db-config))
                     "-U" (:user db-config)
                     "-d" (:database db-config)
                     "-c" sql)]
        (if (zero? (:exit result))
          {:success true :output (:out result)}
          {:success false :error (:err result)}))
      (catch Exception e
        {:success false :error (.getMessage e)}))))

(defn list-tenants []
  (println "🏢 Listing all tenants in the database...")
  (println "==========================================")

  (let [result (run-query "
    SELECT
      name,
      slug,
      status,
      subscription_tier,
      subscription_status,
      billing_email,
      created_at
    FROM tenants
    ORDER BY created_at DESC;")]

    (if (:success result)
      (do
        (println (:output result))
        (println "\n✅ Query completed successfully"))
      (do
        (println "❌ Error executing query:")
        (println (:error result))
        (System/exit 1)))))

(defn main []
  (list-tenants))

;; Entry point for babashka
(when (= *file* (System/getProperty "babashka.file"))
  (main))
