(ns app.backend.ensure-enums
  "Utility to ensure all required enum types exist in the database"
  (:require
    [clojure.string :as str]
    [next.jdbc :as jdbc]
    [taoensso.timbre :as log]))

(defn get-all-enum-types
  "Extract all enum type definitions from models"
  [models-data]
  (reduce-kv
    (fn [acc _entity-key entity-def]
      (if-let [types (:types entity-def)]
        (into acc
          (keep (fn [type-def]
                  (let [[type-name type-kind type-data] type-def]
                    (when (= type-kind :enum)
                      {:name (name type-name)
                       :sql-name (str/replace (name type-name) "-" "_")
                       :choices (:choices type-data)})))
            types))
        acc))
    []
    models-data))

(defn enum-type-exists?
  "Check if an enum type exists in the database"
  [db type-name]
  (let [result (jdbc/execute-one! db
                 ["SELECT 1 FROM pg_type WHERE typname = ?" type-name])]
    (boolean result)))

(defn create-enum-type
  "Create an enum type if it doesn't exist"
  [db type-name choices]
  (when-not (enum-type-exists? db type-name)
    (let [choices-str (str/join ", " (map #(str "'" % "'") choices))
          sql (format "CREATE TYPE %s AS ENUM (%s)" type-name choices-str)]
      (jdbc/execute! db [sql]))))

(defn ensure-all-enum-types
  "Ensure all enum types from models exist in the database"
  [db md]
  (let [enum-types (get-all-enum-types md)]
    (doseq [{:keys [sql-name choices]} enum-types]
      (try
        (create-enum-type db sql-name choices)
        (catch Exception e
          (log/warn "Could not create enum type" sql-name ":" (.getMessage e)))))))
