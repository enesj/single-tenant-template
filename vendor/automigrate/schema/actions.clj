(ns automigrate.schema.actions
  "Migration to actions transformation and related utilities."
  (:require
    [automigrate.schema :as schema]
    [automigrate.schema.diffing :as diffing]
    [automigrate.util.file :as file-util]
    [clojure.java.io :as io]
    [clojure.string :as str]))

;; Define constants for extensions and directions
(def ^:private AUTO-MIGRATION-EXT :edn)
(def ^:private SQL-MIGRATION-EXT :sql)
(def ^:private FORWARD-DIRECTION :forward)
(def ^:private BACKWARD-DIRECTION :backward)
(def ^:private function-migration-ext :fn)
(def ^:private trigger-migration-ext :trg)
(def ^:private policy-migration-ext :pol)
(def ^:private view-migration-ext :view)
(def ^:private FORWARD-MIGRATION-DELIMITER "-- FORWARD")
(def ^:private BACKWARD-MIGRATION-DELIMITER "-- BACKWARD")

(defn- ->file
  [file-name migrations-dir]
  (file-util/join-path migrations-dir file-name))

(defn- get-forward-sql-migration
  [migration]
  (-> (str/split migration (re-pattern BACKWARD-MIGRATION-DELIMITER))
    (first)
    (str/replace (re-pattern FORWARD-MIGRATION-DELIMITER) "")
    (vector)))

(defn- get-backward-sql-migration
  [migration]
  (-> (str/split migration (re-pattern BACKWARD-MIGRATION-DELIMITER))
    (last)
    (vector)))

;; Migration->Actions Multimethod
(defmulti migration->actions (juxt :migration-type :direction))

;; EDN migrations
(defmethod migration->actions [AUTO-MIGRATION-EXT FORWARD-DIRECTION]
  [{:keys [file-name migrations-dir]}]
  (let [migration-file-path (file-util/join-path migrations-dir file-name)]
    (-> migration-file-path (io/resource) (file-util/read-edn))))

(defmethod migration->actions [AUTO-MIGRATION-EXT BACKWARD-DIRECTION]
  [{:keys [migrations-dir number-int all-migrations]}]
  (try
    (let [migrations-from (->> all-migrations
                            (take-while #(<= (:number-int %) number-int))
                            (filterv #(= AUTO-MIGRATION-EXT (:migration-type %)))
                            (mapv #(-> % :file-name (->file migrations-dir))))
          schema-from (schema/current-db-schema migrations-from)
          migrations-to (butlast migrations-from)
          schema-to (schema/current-db-schema migrations-to)]
      (diffing/make-migration* schema-from schema-to))
    (catch Exception e
      (throw e))))

;; SQL migrations
(defmethod migration->actions [SQL-MIGRATION-EXT FORWARD-DIRECTION]
  [{:keys [file-name migrations-dir]}]
  (-> (file-util/join-path migrations-dir file-name)
    (io/resource)
    (slurp)
    (get-forward-sql-migration)))

(defmethod migration->actions [SQL-MIGRATION-EXT BACKWARD-DIRECTION]
  [{:keys [file-name migrations-dir]}]
  (-> (file-util/join-path migrations-dir file-name)
    (io/resource)
    (slurp)
    (get-backward-sql-migration)))

;; Function migrations
(defmethod migration->actions [function-migration-ext FORWARD-DIRECTION]
  [{:keys [file-name migrations-dir]}]
  (-> (file-util/join-path migrations-dir file-name)
    (io/resource)
    (slurp)
    (get-forward-sql-migration)))

(defmethod migration->actions [function-migration-ext BACKWARD-DIRECTION]
  [{:keys [file-name migrations-dir]}]
  (-> (file-util/join-path migrations-dir file-name)
    (io/resource)
    (slurp)
    (get-backward-sql-migration)))

;; Trigger migrations
(defmethod migration->actions [trigger-migration-ext FORWARD-DIRECTION]
  [{:keys [file-name migrations-dir]}]
  (-> (file-util/join-path migrations-dir file-name)
    (io/resource)
    (slurp)
    (get-forward-sql-migration)))

(defmethod migration->actions [trigger-migration-ext BACKWARD-DIRECTION]
  [{:keys [file-name migrations-dir]}]
  (-> (file-util/join-path migrations-dir file-name)
    (io/resource)
    (slurp)
    (get-backward-sql-migration)))

;; Policy migrations
(defmethod migration->actions [policy-migration-ext FORWARD-DIRECTION]
  [{:keys [file-name migrations-dir]}]
  (-> (file-util/join-path migrations-dir file-name)
    (io/resource)
    (slurp)
    (get-forward-sql-migration)))

(defmethod migration->actions [policy-migration-ext BACKWARD-DIRECTION]
  [{:keys [file-name migrations-dir]}]
  (-> (file-util/join-path migrations-dir file-name)
    (io/resource)
    (slurp)
    (get-backward-sql-migration)))

;; View migrations
(defmethod migration->actions [view-migration-ext FORWARD-DIRECTION]
  [{:keys [file-name migrations-dir]}]
  (-> (file-util/join-path migrations-dir file-name)
    (io/resource)
    (slurp)
    (get-forward-sql-migration)))

(defmethod migration->actions [view-migration-ext BACKWARD-DIRECTION]
  [{:keys [file-name migrations-dir]}]
  (-> (file-util/join-path migrations-dir file-name)
    (io/resource)
    (slurp)
    (get-backward-sql-migration)))
