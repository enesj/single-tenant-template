(ns app.migrate
  "Standalone migration runner for production deployments.
   Invoked by Railway's preDeployCommand before the app starts:
     java -cp app.jar clojure.main -m app.migrate"
  (:require [automigrate.core :as am]
    [clojure.string :as str]))

(defn- resolve-jdbc-url
  "Resolve JDBC URL from DATABASE_URL env var.
  Normalizes both postgres:// and postgresql:// to jdbc:postgresql://."
  []
  (some-> (System/getenv "DATABASE_URL") str/trim not-empty
    (str/replace #"^postgresql://" "jdbc:postgresql://")
    (str/replace #"^postgres://" "jdbc:postgresql://")))

(defn -main [& _args]
  (let [jdbc-url (resolve-jdbc-url)]
    (when-not jdbc-url
      (println "ERROR: DATABASE_URL env var is required for migrations")
      (System/exit 1))
    (println "Running migrations against" (str/replace jdbc-url #"://[^@]+@" "://***@") "...")
    (am/migrate {:jdbc-url jdbc-url})
    (println "Migrations complete.")))
