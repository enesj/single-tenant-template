(ns automigrate.generation.core
  "High-level migration generation and coordination - Main API"
  (:require [automigrate.db.introspection :as db-intro]
    [automigrate.files.management :as files]
    [automigrate.generation.extended :as gen-ext]
    [automigrate.schema.diffing :as diffing]
    [automigrate.execution.core :as exec]
    [automigrate.status.tracking :as status]
    [automigrate.schema :as schema]
    [automigrate.util.db :as db-util]
    [automigrate.util.file :as file-util]
    [automigrate.errors :as errors]
    [automigrate.actions :as actions]
    [clojure.java.io :as io]
    [clojure.pprint :as pprint]
    [clojure.string :as str]
    [clojure.spec.alpha :as s]
    [next.jdbc :as jdbc]
    [slingshot.slingshot :refer [throw+ try+]]))

(def ^:private RESOURCES-DIR "resources")
(def ^:private MODELS-FILE "db/models.edn")
(def ^:private MIGRATIONS-DIR "db/migrations")
(def ^:private MIGRATIONS-TABLE :automigrate-migrations)
(def ^:private AUTO-MIGRATION-PREFIX "auto")
(def ^:private AUTO-MIGRATION-POSTFIX "etc")
(def ^:private AUTO-MIGRATION-EXT :edn)
(def ^:private SQL-MIGRATION-EXT :sql)
(def EMPTY-SQL-MIGRATION-TYPE :empty-sql)
(def ^:private FORWARD-DIRECTION :forward)
(def ^:private BACKWARD-DIRECTION :backward)

(defn- read-models
  "Read and validate models from file or hierarchical structure."
  [models-file]
  (println "Read models" models-file (io/resource models-file))
  (if (= models-file MODELS-FILE)
    ;; Check if consolidated models file exists first
    (if (io/resource models-file)
      (do
        (println "Using consolidated models file:" models-file)
        (->> models-file
          (io/resource)
          (file-util/read-edn)
          (automigrate.models/->internal-models)))
      ;; Fallback to hierarchical structure if consolidated doesn't exist
      (let [db-path "db"]
        (println "Using hierarchical model structure from" db-path)
        (files/read-models-hierarchical db-path)))
    ;; Fallback to single file for custom paths
    (->> models-file
      (io/resource)
      (file-util/read-edn)
      (automigrate.models/->internal-models))))

(defn- extract-item-name
  [action]
  (condp contains? (:action action)
    #{actions/CREATE-TABLE-ACTION
      actions/DROP-TABLE-ACTION} (:model-name action)
    #{actions/ADD-COLUMN-ACTION
      actions/DROP-COLUMN-ACTION
      actions/ALTER-COLUMN-ACTION} (:field-name action)
    #{actions/CREATE-INDEX-ACTION
      actions/DROP-INDEX-ACTION
      actions/ALTER-INDEX-ACTION} (:index-name action)
    #{actions/CREATE-TYPE-ACTION
      actions/DROP-TYPE-ACTION
      actions/ALTER-TYPE-ACTION} (:type-name action)))

(defn- get-action-description-vec-basic
  [action]
  (let [action-name (-> action :action name (str/replace #"-" "_") (str/split #"_"))
        item-name (-> action extract-item-name name (str/replace #"-" "_"))]
    (conj action-name item-name)))

(defn- get-action-description-vec-with-table
  [action preposition]
  (let [action-desc-basic (get-action-description-vec-basic action)
        model-name (-> action :model-name name (str/replace #"-" "_"))]
    (conj action-desc-basic preposition model-name)))

(defn- get-action-description-vec
  [action]
  (condp contains? (:action action)
    #{actions/ADD-COLUMN-ACTION} (get-action-description-vec-with-table action "to")
    #{actions/ALTER-COLUMN-ACTION} (get-action-description-vec-with-table action "in")
    #{actions/DROP-COLUMN-ACTION} (get-action-description-vec-with-table action "from")

    #{actions/CREATE-INDEX-ACTION
      actions/ALTER-INDEX-ACTION
      actions/DROP-INDEX-ACTION} (get-action-description-vec-with-table action "on")

    ; default
    (get-action-description-vec-basic action)))

(defn- get-next-migration-name-auto
  [actions]
  (let [first-action (first actions)
        action-desc-vec (get-action-description-vec first-action)
        action-desc-vec* (cond-> (concat [AUTO-MIGRATION-PREFIX] action-desc-vec)
                           (> (count actions) 1) (concat [AUTO-MIGRATION-POSTFIX]))]
    (str/join "_" action-desc-vec*)))

(defn- get-next-migration-name
  "Return given custom name with underscores or first action name."
  [actions custom-name]
  (let [migration-name (or custom-name (get-next-migration-name-auto actions))]
    (str/replace migration-name #"-" "_")))

(defn- get-next-migration-file-path
  "Return next migration file name based on existing migrations."
  [{:keys [migration-type resources-dir migrations-dir next-migration-name]}]
  (let [migration-names (files/migrations-list migrations-dir)
        migration-number (files/next-migration-number migration-names)
        migration-file-name (str migration-number "_" next-migration-name)
        migration-file-with-ext (str migration-file-name "." (name migration-type))]
    (file-util/join-path resources-dir migrations-dir migration-file-with-ext)))

(defn- make-next-migration
  "Return actions for next migration."
  [{:keys [models-file migrations-dir]}]
  (let [auto-migration-files (->> (file-util/list-files migrations-dir)
                               (filter files/auto-migration?)
                               (sort-by file-util/file-url->file-name))
        new-schema (read-models models-file)]
    ;; If we have no .edn migration files but models exist, force generation of initial schema
    (if (and (empty? auto-migration-files) (not-empty new-schema))
      ;; Force creation of initial schema migration by using empty old schema
      (do
        (println "No .edn migration files found but models exist - forcing initial schema migration")
        (println "New schema contains" (count new-schema) "models:" (keys new-schema))
        (let [result (diffing/make-migration* {} new-schema)]
          (println "make-migration* result:" (count result) "actions")
          (println "First few actions:" (take 3 result))
          (-> result
            (flatten)
            (seq))))
      ;; Normal behavior: compare existing migrations to new models
      (let [old-schema (schema/current-db-schema auto-migration-files)]
        (-> (diffing/make-migration* old-schema new-schema)
          (flatten)
          (seq))))))

(defn- get-action-name-verbose
  [action]
  (->> action
    (get-action-description-vec)
    (cons "  -")
    (str/join " ")))

(defn- print-action-names
  [actions]
  (let [action-names (mapv get-action-name-verbose actions)]
    (file-util/safe-println (cons "Actions:" action-names) "")))

(defn- create-migrations-dir!
  "Create migrations root dir if it does not exist."
  [migrations-dir]
  (when-not (.isDirectory (io/file migrations-dir))
    (.mkdir (java.io.File. migrations-dir))))

(defmulti make-migration :type)

(defmethod make-migration :default
  ; Make new migration based on models definition automatically.
  [{:keys [models-file migrations-dir resources-dir :generate-all]
    :or {models-file MODELS-FILE
         migrations-dir MIGRATIONS-DIR
         resources-dir RESOURCES-DIR}
    custom-migration-name :name
    generate-all? :generate-all}]

  (try+
    ; Create migrations dir if it doesn't exist
    (create-migrations-dir!
      (file-util/join-path resources-dir migrations-dir))
    (println "Make migration" [models-file migrations-dir resources-dir])

    ;; First, create schema migration if there are changes
    (let [schema-created?
          (if-let [next-migration (make-next-migration {:models-file models-file
                                                        :migrations-dir migrations-dir})]
            (let [next-migration-name (get-next-migration-name next-migration custom-migration-name)
                  _ (println "next-migration" next-migration next-migration-name)
                  migration-file-name-full-path (get-next-migration-file-path
                                                  {:migration-type AUTO-MIGRATION-EXT
                                                   :resources-dir resources-dir
                                                   :migrations-dir migrations-dir
                                                   :next-migration-name next-migration-name})]
              (println "Migration file path:" migration-file-name-full-path)
              (spit migration-file-name-full-path
                (with-out-str
                  (pprint/pprint next-migration)))
              ; Print all actions from migration in human-readable format
              (print-action-names next-migration)
              true)
            (do
              (println "There are no changes in models.")
              false))]

      ;; If generate-all is requested, generate extended migrations
      (when generate-all?
        (println "\nGenerating extended migrations from EDN files...")
        (let [summary (gen-ext/generate-extended-migrations-from-edn!
                        {:migrations-dir migrations-dir
                         :resources-dir resources-dir})]
          (println (format "\nGenerated extended migrations:\n                             Functions: %d\n                             Triggers: %d\n                             Policies: %d\n                             Views: %d\n                             Total: %d"
                     (:functions summary)
                     (:triggers summary)
                     (:policies summary)
                     (:views summary)
                     (:total summary))))

        ;; Return summary of what was created
        {:schema-migration-created schema-created?
         :extended-migrations-created true}))

    (catch [:type ::s/invalid] e
      (file-util/prn-err e))
    (catch #(contains? #{:automigrate.models/missing-referenced-model
                         :automigrate.models/missing-referenced-field
                         :automigrate.models/referenced-field-is-not-unique
                         :automigrate.models/fk-fields-have-different-types} (:type %)) e
      (-> e
        (errors/custom-error->error-report)
        (file-util/prn-err)))
    (catch [:reason :weavejester.dependency/circular-dependency] e
      (-> {:title "MIGRATION ERROR"
           :message (format (str "Circular dependency between two migration actions: \n  %s\nand\n  %s\n\n"
                              "Please split actions by different migrations.")
                      (pr-str (:dependency e))
                      (pr-str (:node e)))}
        (errors/custom-error->error-report)
        (file-util/prn-err)))
    (catch java.io.FileNotFoundException e
      (-> {:title "ERROR"
           :message (format "Missing file:\n\n  %s" (ex-message e))}
        (errors/custom-error->error-report)
        (file-util/prn-err)))
    (catch IllegalArgumentException e
      (-> {:title "ERROR"
           :message (str (format "%s\n\nMissing resource file error. " (ex-message e))
                      "Please, check, if models.edn exists and resources dir\n"
                      "is included to source paths in `deps.edn` or `project.clj`.")}
        (errors/custom-error->error-report)
        (file-util/prn-err)))))

(defmethod make-migration EMPTY-SQL-MIGRATION-TYPE
  ; Make new migrations based on models definitions automatically.
  [{:keys [migrations-dir resources-dir]
    :or {migrations-dir MIGRATIONS-DIR
         resources-dir RESOURCES-DIR}
    next-migration-name :name}]
  (try+
    (when (empty? next-migration-name)
      (throw+ {:type ::missing-migration-name
               :message "Missing migration name."}))
    (let [migrations-dir-resource (file-util/join-path resources-dir migrations-dir)
          _ (create-migrations-dir! migrations-dir-resource)
          next-migration-name* (str/replace next-migration-name #"-" "_")
          migration-file-name-full-path (get-next-migration-file-path
                                          {:migration-type SQL-MIGRATION-EXT
                                           :resources-dir resources-dir
                                           :migrations-dir migrations-dir
                                           :next-migration-name next-migration-name*})
          sql-migration-template (format "-- FORWARD\n\n\n-- BACKWARD\n")]
      (spit migration-file-name-full-path sql-migration-template)
      (println (str "Created migration: " migration-file-name-full-path)))
    (catch [:type ::s/invalid] e
      (file-util/prn-err e))
    (catch #(contains? #{::missing-migration-name
                         :automigrate.files.management/duplicated-migration-numbers} (:type %)) e
      (-> e
        (errors/custom-error->error-report)
        (file-util/prn-err)))))

(defn migrate!
  "Run migration on a db."
  [{:keys [migrations-dir jdbc-url number migrations-table]
    :or {migrations-table MIGRATIONS-TABLE
         migrations-dir MIGRATIONS-DIR}}]
  (try+
    (let [db (db-util/db-conn jdbc-url)
          _ (exec/create-migration-table! db migrations-table)
          migrated (status/already-migrated db migrations-table)
          all-migrations (files/migrations-list migrations-dir)
          all-migrations-detailed (map status/detailed-migration all-migrations)
          {:keys [to-migrate direction]}
          (status/get-detailed-migrations-to-migrate all-migrations-detailed migrated number)]
      (if (seq to-migrate)
        (doseq [{:keys [migration-name file-name migration-type number-int]} to-migrate]
          (condp = direction
            FORWARD-DIRECTION (println (str "Applying " migration-name "..."))
            BACKWARD-DIRECTION (println (str "Reverting " migration-name "...")))
          (jdbc/with-transaction [tx db]
            (let [actions (diffing/migration->actions {:file-name file-name
                                                       :migrations-dir migrations-dir
                                                       :migration-type migration-type
                                                       :number-int number-int
                                                       :direction direction
                                                       :all-migrations all-migrations-detailed})]
              (exec/exec-actions! {:db tx
                                   :actions actions
                                   :migration-type migration-type})
              (if (= direction FORWARD-DIRECTION)
                (exec/save-migration! db migration-name migrations-table)
                (exec/delete-migration! db migration-name migrations-table)))))
        (println "Nothing to migrate.")))
    (catch [:type ::s/invalid] e
      (file-util/prn-err e))
    (catch #(contains? #{:automigrate.files.management/duplicated-migration-numbers
                         ::invalid-target-migration-number} (:type %)) e
      (-> e
        (errors/custom-error->error-report)
        (file-util/prn-err)))))

(defn explain
  "Explain what migrations would be generated without executing them"
  [old-schema new-schema & {:keys [db extended?]
                            :or {extended? true}}]
  (let [;; Core schema diffing
        core-diff (diffing/make-migration* old-schema new-schema)

        ;; Extended migrations (functions, triggers, etc.) if requested
        extended-diff (when (and extended? db)
                        (gen-ext/generate-extended-migrations-from-db!
                          {:db db}))

        ;; Combine all migration actions
        all-actions (concat core-diff
                      (when extended-diff
                        (apply concat (vals extended-diff))))]

    {:total-actions (count all-actions)
     :core-changes core-diff
     :extended-changes extended-diff
     :actions all-actions}))

(defn list-migrations
  "List all migrations with their status"
  [jdbc-url migrations-dir]
  (status/list-migrations {:jdbc-url jdbc-url
                           :migrations-dir migrations-dir}))

(defn migration-status
  "Get current migration status summary"
  [jdbc-url migrations-dir]
  (let [db (db-util/db-conn jdbc-url)]
    (status/migration-status-summary db MIGRATIONS-TABLE migrations-dir)))

(defn setup-migration-system!
  "Initialize the migration system by creating required tables"
  [jdbc-url]
  (let [db (db-util/db-conn jdbc-url)]
    (exec/create-migration-table! db MIGRATIONS-TABLE)))

;; Legacy compatibility - maintain the original API
(def ^:deprecated migrations-list files/migrations-list)
(def ^:deprecated get-db-functions db-intro/get-db-functions)
(def ^:deprecated get-db-policies db-intro/get-db-policies)
