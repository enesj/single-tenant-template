(ns app.shared.adapters.database
  "Convert PG objects/keys; call before JSON responses."
  (:require
    [app.shared.adapters.normalization :as norm]
    [cheshire.core :as json]
    [clojure.string :as str]
    [clojure.walk :as walk]
    [taoensso.timbre :as log])
  (:import
    (org.postgresql.jdbc PgArray)
    (org.postgresql.util PGobject)))

(defn convert-pg-objects
  "Convert PostgreSQL objects to JSON-serializable Clojure data structures.

  Handles:
  - PgArray -> Clojure vector
  - PGobject(json/jsonb) -> parsed Clojure data

  Other values are returned unchanged."
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

(defn to-app
  "Convert DB rows/results to API-friendly maps.

  Applies:
  - `convert-pg-objects` (PGobject/PgArray → JSON-safe Clojure values)
  - `norm/convert-db-keys->app-keys` (snake_case → kebab-case keywords)

  Intended for backend handlers returning data to the frontend."
  [data]
  (-> data
    convert-pg-objects
    norm/convert-db-keys->app-keys))
