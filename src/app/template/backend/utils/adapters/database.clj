(ns app.template.backend.utils.adapters.database
  "Shared database adapter utilities. 
   Acts as an aggregator for normalization and persistence logic."
  (:require
    [app.template.backend.utils.adapters.normalization :as norm]
    [app.template.backend.utils.adapters.persistence :as persist]
    [cheshire.core :as json]
    [clojure.string :as str]
    [clojure.walk :as walk]
    [taoensso.timbre :as log])
  (:import
    (org.postgresql.jdbc PgArray)
    (org.postgresql.util PGobject)))

;; ============================================================================
;; Low-level PostgreSQL Object Conversion
;; ============================================================================

(defn convert-pg-objects
  "Convert PostgreSQL objects to JSON-serializable Clojure data structures."
  [data]
  (walk/postwalk
    (fn [x]
      (cond
        (instance? PgArray x)
        (try
          (vec (.getArray x))
          (catch Exception e
            (log/warn "Failed to convert PgArray, using string representation:" (.getMessage e))
            (let [array-str (.toString x)]
              (if (and (> (count array-str) 2)
                    (= (first array-str) \{)
                    (= (last array-str) \}))
                (-> array-str
                  (subs 1 (dec (count array-str)))
                  (str/split #",")
                  (->> (mapv str/trim)))
                []))))

        (instance? PGobject x)
        (try
          (let [type (.getType x)
                value (.getValue x)]
            (case type
              "json" (json/parse-string value true)
              "jsonb" (json/parse-string value true)
              value))
          (catch Exception e
            (log/warn "Failed to convert PGobject, using string value:" (.getMessage e))
            (.getValue x)))

        :else x))
    data))

;; ============================================================================
;; Re-exports for Backward Compatibility
;; ============================================================================

;; Normalization
(def convert-db-keys->app-keys norm/convert-db-keys->app-keys)
(def app-keyword->camel norm/app-keyword->camel)
(def convert-app-keys->camel-keys norm/convert-app-keys->camel-keys)
(def db-keyword->app-with-aliases norm/db-keyword->app-with-aliases)
(def normalize-admin-result norm/normalize-admin-result)

;; Persistence
(def with-admin-transaction persist/with-admin-transaction)

(defn execute-admin-query
  "Wrapped version of execute-admin-query that provides the internal normalization fn."
  [db query normalization-config & [options]]
  (persist/execute-admin-query db query 
    (fn [raw-result]
      (-> raw-result
        convert-pg-objects
        (norm/normalize-admin-result normalization-config)))
    options))
