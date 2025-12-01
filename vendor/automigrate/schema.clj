(ns automigrate.schema
  "Module for generating db schema from migrations."
  (:require
   [automigrate.actions :as actions]
   [automigrate.models :as models]
   [automigrate.util.file :as file-util]
   [automigrate.util.map :as map-util]
   [automigrate.util.model :as model-util]
   [automigrate.util.spec :as spec-util]
   [clojure.java.io :as io]))

(defn- load-migrations-from-files
  [migrations-files]
  (println "🐛 DEBUG SCHEMA 1: load-migrations-from-files called with" (count migrations-files) "files")
  (println "🐛 DEBUG SCHEMA 2: files:" migrations-files)
  (try
    (let [result (map (fn [file-ref]
                        (println "🐛 DEBUG SCHEMA 3: processing file-ref:" file-ref)
                        (try
                          (if (string? file-ref)
                            (let [resource (io/resource file-ref)
                                  _ (println "🐛 DEBUG SCHEMA 4: got resource:" resource)]
                              (if resource
                                (do
                                  (println "🐛 DEBUG SCHEMA 5: reading EDN from resource")
                                  (file-util/read-edn resource))
                                (do
                                  (println "🐛 DEBUG SCHEMA 6: resource not found, returning empty vector")
                                  [])))
                            (do
                              (println "🐛 DEBUG SCHEMA 7: reading EDN from file-ref directly")
                              (file-util/read-edn file-ref)))
                          (catch Exception e
                            (println "🐛 DEBUG SCHEMA 8: Exception processing file-ref:" file-ref)
                            (println "  Exception type:" (type e))
                            (println "  Exception message:" (.getMessage e))
                            (println "  Stack trace:")
                            (.printStackTrace e)
                            (throw e))))
                   migrations-files)]
      (println "🐛 DEBUG SCHEMA 9: load-migrations-from-files completed successfully")
      result)
    (catch Exception e
      (println "🐛 DEBUG SCHEMA 10: Exception in load-migrations-from-files:")
      (println "  Exception type:" (type e))
      (println "  Exception message:" (.getMessage e))
      (println "  Stack trace:")
      (.printStackTrace e)
      (throw e))))

(defmulti apply-action-to-schema
  "Apply migrating action to schema in memory to reproduce current db state."
  (fn [_schema action]
    (:action action)))

(defmethod apply-action-to-schema actions/CREATE-TABLE-ACTION
  [schema action]
  (assoc-in schema [(:model-name action) :fields] (:fields action)))

(defmethod apply-action-to-schema actions/ADD-COLUMN-ACTION
  [schema action]
  (assoc-in schema [(:model-name action) :fields (:field-name action)]
            (:options action)))

(defmethod apply-action-to-schema actions/ALTER-COLUMN-ACTION
  [schema action]
  (let [model-name (:model-name action)
        field-name (:field-name action)
        changes-to-add (model-util/changes-to-add (:changes action))
        changes-to-drop (model-util/changes-to-drop (:changes action))
        dissoc-actions-fn (fn [schema]
                            (apply map-util/dissoc-in
                              schema
                              [model-name :fields field-name]
                              changes-to-drop))]

    (-> schema
      (update-in [model-name :fields field-name] merge changes-to-add)
      (dissoc-actions-fn))))

(defmethod apply-action-to-schema actions/DROP-COLUMN-ACTION
  [schema action]
  (map-util/dissoc-in schema [(:model-name action) :fields] (:field-name action)))

(defmethod apply-action-to-schema actions/DROP-TABLE-ACTION
  [schema action]
  (dissoc schema (:model-name action)))

(defmethod apply-action-to-schema actions/CREATE-INDEX-ACTION
  [schema action]
  (assoc-in schema [(:model-name action) :indexes (:index-name action)]
            (:options action)))

(defmethod apply-action-to-schema actions/DROP-INDEX-ACTION
  [schema action]
  (let [action-name (:model-name action)
        result (map-util/dissoc-in
                 schema
                 [action-name :indexes]
                 (:index-name action))]
    (if (seq (get-in result [action-name :indexes]))
      result
      (map-util/dissoc-in result [action-name] :indexes))))

(defmethod apply-action-to-schema actions/ALTER-INDEX-ACTION
  [schema action]
  (assoc-in schema [(:model-name action) :indexes (:index-name action)]
            (:options action)))

(defmethod apply-action-to-schema actions/CREATE-TYPE-ACTION
  [schema action]
  (assoc-in schema [(:model-name action) :types (:type-name action)]
            (:options action)))

(defmethod apply-action-to-schema actions/ALTER-TYPE-ACTION
  [schema action]
  (assoc-in schema [(:model-name action) :types (:type-name action)]
            (:options action)))

(defmethod apply-action-to-schema actions/DROP-TYPE-ACTION
  [schema action]

  (let [action-name (:model-name action)
        result (map-util/dissoc-in
                 schema
                 [action-name :types]
                 (:type-name action))]
    (if (seq (get-in result [action-name :types]))
      result
      (map-util/dissoc-in result [action-name] :types))))

(defn- actions->internal-models
  [actions]
  ; Throws spec exception if not valid.
  (println "🐛 actions->internal-models:" actions)
  (actions/validate-actions! actions)
  (->> actions
    (reduce apply-action-to-schema {})
    (spec-util/conform ::models/internal-models)))

(defn current-db-schema
  "Return map of models derived from existing migrations."
  [migrations-files]
  (println "🐛 DEBUG SCHEMA 11: current-db-schema called with" (count migrations-files) "files")
  (try
    (let [_ (println "🐛 DEBUG SCHEMA 12: loading migrations from files")
          actions (flatten (load-migrations-from-files migrations-files))
          _ (println "🐛 DEBUG SCHEMA 13: got" (count actions) "actions")
          _ (println "🐛 DEBUG SCHEMA 14: converting actions to internal models")]
      (println "🐛 DEBUG SCHEMA 15: calling actions->internal-models")
      (try
        (let [result (actions->internal-models actions)]
          (println "🐛 DEBUG SCHEMA 16: actions->internal-models completed successfully")
          result)
        (catch Exception e
          (println "🐛 DEBUG SCHEMA 17: Exception in actions->internal-models:")
          (println "  Exception type:" (type e))
          (println "  Exception message:" (.getMessage e))
          (println "  Stack trace:")
          (.printStackTrace e)
          (throw e))))
    (catch Exception e
      (println "🐛 DEBUG SCHEMA 18: Exception in current-db-schema:")
      (println "  Exception type:" (type e))
      (println "  Exception message:" (.getMessage e))
      (println "  Stack trace:")
      (.printStackTrace e)
      (throw e))))
