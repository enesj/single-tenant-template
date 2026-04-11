(ns app.migrate
  "Standalone migration runner for production deployments.
   Invoked by Railway's preDeployCommand before the app starts:
     java -cp app.jar clojure.main -m app.migrate"
  (:require [automigrate.execution.core :as am-exec]
    [automigrate.files.management :as am-files]
    [automigrate.schema.actions :as am-schema-actions]
    [automigrate.status.tracking :as am-status]
    [automigrate.util.db :as am-db]
    [clojure.string :as str]
    [next.jdbc :as jdbc]))

(def ^:private migrations-dir "db/migrations")
(def ^:private migrations-table :automigrate-migrations)

(defn- normalize-jdbc-url
  "Normalize DATABASE_URL into the JDBC form expected by migration tooling."
  [database-url]
  (some-> database-url
    str/trim
    not-empty
    (str/replace #"^postgresql://" "jdbc:postgresql://")
    (str/replace #"^postgres://" "jdbc:postgresql://")))

(defn- redact-jdbc-url
  [jdbc-url]
  (some-> jdbc-url
    (str/replace #"://[^@]+@" "://***@")))

(defn- parse-target-number
  [arg]
  (when (some? arg)
    (try
      (Integer/parseInt (str/trim arg))
      (catch NumberFormatException _
        (throw (ex-info "Target migration number must be an integer." {:arg arg}))))))

(defn- resolve-jdbc-url
  "Resolve JDBC URL from DATABASE_URL env var."
  []
  (normalize-jdbc-url (System/getenv "DATABASE_URL")))

(defn- migration-plan
  [db number]
  (let [all-migrations (->> (am-files/migrations-list migrations-dir)
                         (mapv am-status/detailed-migration))
        migrated (am-status/already-migrated db migrations-table)
        plan (am-status/get-detailed-migrations-to-migrate all-migrations migrated number)]
    (if (map? plan)
      (-> plan
        (update :to-migrate #(vec (or % [])))
        (assoc :all-migrations all-migrations))
      {:all-migrations all-migrations
       :direction :forward
       :to-migrate []})))

(defn- execute-migration!
  [db all-migrations direction {:keys [file-name migration-name migration-type number-int]}]
  (println (if (= direction :forward)
             (str "Applying " migration-name "...")
             (str "Reverting " migration-name "...")))
  (jdbc/with-transaction [tx db]
    (let [actions (am-schema-actions/migration->actions {:file-name file-name
                                                         :migrations-dir migrations-dir
                                                         :migration-type migration-type
                                                         :number-int number-int
                                                         :direction direction
                                                         :all-migrations all-migrations})]
      (am-exec/exec-actions! {:db tx
                              :actions actions
                              :migration-type migration-type})
      (if (= direction :forward)
        (am-exec/save-migration! tx migration-name migrations-table)
        (am-exec/delete-migration! tx migration-name migrations-table)))))

(defn- run-migrations!
  [{:keys [jdbc-url number]}]
  (let [db (am-db/db-conn jdbc-url)
        _ (am-exec/create-migration-table! db migrations-table)
        {:keys [all-migrations to-migrate direction]} (migration-plan db number)]
    (if (seq to-migrate)
      (doseq [migration to-migrate]
        (execute-migration! db all-migrations direction migration))
      (println "Nothing to migrate."))
    {:applied (count to-migrate)
     :direction direction}))

(defn- exit!
  "Exit the JVM with the given status code."
  [code]
  (System/exit code))

(defn -main
  [& [target-number-arg]]
  (let [jdbc-url (resolve-jdbc-url)
        number (parse-target-number target-number-arg)]
    (when-not jdbc-url
      (binding [*out* *err*]
        (println "ERROR: DATABASE_URL env var is required for migrations"))
      (exit! 1))
    (println "Running migrations against" (redact-jdbc-url jdbc-url) "...")
    (try
      (run-migrations! {:jdbc-url jdbc-url
                        :number number})
      (println "Migrations complete.")
      (catch Throwable t
        (binding [*out* *err*]
          (println "ERROR: Migration failed.")
          (println (or (ex-message t) (str t)))
          (.printStackTrace t *err*))
        (exit! 1)))))
