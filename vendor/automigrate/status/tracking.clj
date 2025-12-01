(ns automigrate.status.tracking
  "Migration status, listing, and progress tracking"
  (:require [automigrate.files.management :as files]
    [automigrate.execution.core :as exec]
    [automigrate.util.db :as db-util]
    [automigrate.errors :as errors]
    [automigrate.util.file :as file-util]
    [clojure.spec.alpha :as s]
    [slingshot.slingshot :refer [throw+ try+]]))

(def ^:private LIST-SIGN-COMPLETED "x")

(defn already-migrated
  "Get names of previously migrated migrations from db."
  [db migrations-table]
  (try
    (->> {:select [:name]
          :from [migrations-table]
          :order-by [:created-at]}
      (db-util/exec! db)
      (map :name))
    (catch Exception e
      (let [msg (ex-message e)
            table-exists-err-pattern #"relation .+ does not exist"]
        (if (re-find table-exists-err-pattern msg)
          (throw+ {:type ::no-migrations-table
                   :message "Migrations table does not exist."})
          (throw+ {:type ::unexpected-db-error
                   :data (or (ex-message e) (str e))
                   :message "Unexpected db error."}))))))

(defn- get-already-migrated-migrations
  [db migrations-table]
  (try+
    (set (already-migrated db migrations-table))
    (catch [:type ::no-migrations-table]
      ; There is no migrated migrations if table doesn't exist.
      [])))

(defn list-migrations
  "Print migration list with status."
  [{:keys [jdbc-url migrations-dir migrations-table]
    :or {migrations-table :automigrate-migrations
         migrations-dir "db/migrations"}}]
  ; TODO: reduce duplication with `migrate` fn!
  (try+
    (let [migration-names (files/migrations-list migrations-dir)
          db (db-util/db-conn jdbc-url)
          migrated (set (get-already-migrated-migrations db migrations-table))]
      (if (seq migration-names)
        (do
          (println "Existing migrations:")
          (doseq [file-name migration-names
                  :let [migration-name (files/get-migration-name-from-filename file-name)
                        sign (if (contains? migrated migration-name)
                               LIST-SIGN-COMPLETED
                               " ")]]
            (println (format "[%s] %s" sign file-name))))
        (println "Migrations not found.")))
    (catch [:type ::s/invalid] e
      (file-util/prn-err e))
    (catch [:type :automigrate.files.management/duplicated-migration-numbers] e
      (-> e
        (errors/custom-error->error-report)
        (file-util/prn-err)))
    (catch [:type ::no-migrations-table] e
      (-> e
        (errors/custom-error->error-report)
        (file-util/prn-err)))
    (catch [:type ::unexpected-db-error] e
      (-> e
        (errors/custom-error->error-report)
        (file-util/prn-err)))))

(defn detailed-migration
  "Return detailed info for each migration file."
  [file-name]
  {:file-name file-name
   :migration-name (files/get-migration-name-from-filename file-name)
   :number-int (files/get-migration-number file-name)
   :migration-type (files/get-migration-type file-name)})

(defn- current-migration-number
  "Return current migration name."
  [migrated]
  (if (seq migrated)
    (let [res (->> (last migrated)
                (files/get-migration-number))]
      res)
    0))

(defn get-detailed-migrations-to-migrate
  "Return migrations to migrate and migration direction."
  [all-migrations migrated target-number]
  (if-not (seq all-migrations)
    {}
    (let [all-numbers (set (map :number-int all-migrations))
          last-number (apply max all-numbers)
          target-number* (or target-number last-number)
          current-number (current-migration-number migrated)
          direction (if (> target-number* current-number)
                      :forward
                      :backward)]
      (when-not (or (contains? all-numbers target-number*)
                  (= 0 target-number*))
        (throw+ {:type ::invalid-target-migration-number
                 :number target-number*
                 :message "Invalid target migration number."}))
      (if (= target-number* current-number)
        []
        (condp contains? direction
          #{:forward} {:to-migrate (->> all-migrations
                                     (drop-while #(>= current-number (:number-int %)))
                                     (take-while #(>= target-number* (:number-int %))))
                       :direction direction}
          #{:backward} {:to-migrate (->> all-migrations
                                      (drop-while #(>= target-number* (:number-int %)))
                                      (take-while #(>= current-number (:number-int %)))
                                      (sort-by :number-int >))
                        :direction direction})))))

(defn migration-status-summary
  "Get a summary of migration status"
  [db migrations-table migrations-dir]
  (let [migration-files (files/migrations-list migrations-dir)
        migrated (set (get-already-migrated-migrations db migrations-table))
        total (count migration-files)
        applied (count (filter #(contains? migrated (files/get-migration-name-from-filename %)) migration-files))
        pending (- total applied)]
    {:total total
     :applied applied
     :pending pending
     :up-to-date (zero? pending)}))

(defn get-last-migration
  "Get the last applied migration"
  [db migrations-table]
  (first (db-util/exec! db
           {:select [:name :created-at]
            :from [migrations-table]
            :order-by [[:created-at :desc]]
            :limit 1})))

(defn validate-migration-sequence
  "Validate that migrations have been applied in the correct sequence"
  [db migrations-table migrations-dir]
  (let [migration-files (files/migrations-list migrations-dir)
        migrated-names (set (get-already-migrated-migrations db migrations-table))
        applied-migrations (filter #(contains? migrated-names (files/get-migration-name-from-filename %)) migration-files)
        applied-numbers (map files/get-migration-number applied-migrations)
        expected-sequence (range 1 (inc (count applied-numbers)))]
    {:valid (= applied-numbers expected-sequence)
     :applied applied-numbers
     :expected expected-sequence}))

(defn rollback-to-migration
  "Rollback to a specific migration (mark later ones as not applied)"
  [db migrations-table target-migration-number migrations-dir]
  (let [migration-files (files/migrations-list migrations-dir)
        migrations-to-remove (filter #(> (files/get-migration-number %) target-migration-number) migration-files)
        migration-names (map files/get-migration-name-from-filename migrations-to-remove)]
    (doseq [migration-name migration-names]
      (exec/delete-migration! db migration-name migrations-table))))

(defn get-migration-history
  "Get complete migration history"
  [db migrations-table]
  (db-util/exec! db
    {:select [:name :created-at]
     :from [migrations-table]
     :order-by [[:created-at :asc]]}))
