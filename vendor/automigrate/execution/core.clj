(ns automigrate.execution.core
  "Migration execution, action running, and transaction management"
  (:require
   [automigrate.actions :as actions]
   [automigrate.sql :as sql]
   [automigrate.util.db :as db-util]
   [automigrate.util.spec :as spec-util]
   [clojure.string :as str]))

(def ^:private AUTO-MIGRATION-EXT :edn)
(def ^:private SQL-MIGRATION-EXT :sql)
(def ^:private function-migration-ext :fn)
(def ^:private trigger-migration-ext :trg)
(def ^:private policy-migration-ext :pol)
(def ^:private view-migration-ext :view)

(defmulti exec-action! :migration-type)

(defn- action->honeysql
  [action]
  (spec-util/conform ::sql/->sql action))

(defmethod exec-action! AUTO-MIGRATION-EXT
  [{:keys [db action]}]
  ;; Handle idempotency for index creation: skip if index already exists
  (if (= (:action action) actions/CREATE-INDEX-ACTION)
    (let [idx (:index-name action)]
      (when-not (db-util/index-exists? db idx)
        (let [formatted (action->honeysql action)]
          (if (sequential? formatted)
            (doseq [sub formatted]
              (db-util/exec! db sub))
            (db-util/exec! db formatted)))))
    (let [formatted-action (action->honeysql action)]
      (if (sequential? formatted-action)
        (doseq [sub-action formatted-action]
          (db-util/exec! db sub-action))
        (db-util/exec! db formatted-action)))))

(defmethod exec-action! SQL-MIGRATION-EXT
  [{:keys [db action]}]
  (db-util/exec-raw! db action))

(defmethod exec-action! function-migration-ext
  [{:keys [db action]}]
  (db-util/exec-raw! db action))

(defmethod exec-action! trigger-migration-ext
  [{:keys [db action]}]
  (db-util/exec-raw! db action))

(defmethod exec-action! policy-migration-ext
  [{:keys [db action]}]
  (db-util/exec-raw! db action))

(defmethod exec-action! view-migration-ext
  [{:keys [db action]}]
  (db-util/exec-raw! db action))

(defn exec-actions!
  "Perform list of actions on a database."
  [{:keys [db actions migration-type]}]
  (doseq [action actions]
    (exec-action! {:db db
                   :action action
                   :migration-type migration-type})))

(defn save-migration!
  "Save migration to db after applying it."
  [db migration-name migrations-table]
  (->> {:insert-into migrations-table
        :values [{:name migration-name}]}
    (db-util/exec! db))
  (println (str migration-name " successfully applied.")))

(defn delete-migration!
  "Delete reverted migration from db."
  [db migration-name migrations-table]
  (->> {:delete-from migrations-table
        :where [:= :name migration-name]}
    (db-util/exec! db))
  (println (str migration-name " successfully reverted.")))

(defn already-migrated?
  "Check if migration has already been applied"
  [db migrations-table migration-name]
  (let [result (first (db-util/exec! db
                        {:select [:%count.*]
                         :from [migrations-table]
                         :where [:= :name migration-name]}))]
    (-> result :count (> 0))))

(defn create-migration-table!
  "Create schema_migrations table if it doesn't exist"
  [db migrations-table]
  (db-util/create-migrations-table db migrations-table))

(defn migrate
  "Execute migration with proper error handling and logging"
  [db migration-data migrations-table]
  (try
    (exec-actions! {:db db
                    :actions (:actions migration-data)
                    :migration-type (:migration-type migration-data)})
    (save-migration! db (:name migration-data) migrations-table)
    {:success true
     :migration-name (:name migration-data)}
    (catch Exception e
      {:success false
       :error (.getMessage e)
       :migration-name (:name migration-data)})))

(defn get-forward-sql-migration
  [migration]
  (-> (str/split migration #"-- BACKWARD")
    (first)
    (str/replace #"-- FORWARD" "")
    (vector)))

(defn get-backward-sql-migration
  [migration]
  (-> (str/split migration #"-- BACKWARD")
    (last)
    (vector)))
