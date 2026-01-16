(ns app.shared.adapters.normalization
  "Shared normalization utilities for converting DB/result keys and shaping data for APIs.

  Notes:
  - This namespace is CLJC (works in both Clojure and ClojureScript).
  - JVM-only conversion of PostgreSQL JDBC objects (PGobject/PgArray) lives in
    `app.shared.adapters.database`."
  (:require
    [app.shared.model-naming :as model-naming]
    [app.shared.string :as string]
    [clojure.string :as str]
    #?(:clj [clojure.walk :as walk]
       :cljs [cljs.walk :as walk])))

(defn convert-db-keys->app-keys
  "Convert snake_case database keys to kebab-case for frontend consumption.

  Recursively walks through data structures and converts keys using
  `app.shared.model-naming/db-keyword->app`."
  [data]
  (cond
    (nil? data) nil

    (map? data)
    (into {}
      (map (fn [[k v]]
             [(model-naming/db-keyword->app k) (convert-db-keys->app-keys v)]))
      data)

    (vector? data)
    (mapv convert-db-keys->app-keys data)

    (seq? data)
    (map convert-db-keys->app-keys data)

    :else data))

(defn app-keyword->camel
  "Convert a kebab-case keyword into camelCase, dropping any namespace."
  [k]
  (if (keyword? k)
    (keyword (string/camel-case (name k)))
    k))

(defn convert-app-keys->camel-keys
  "Convert kebab-case application keys into camelCase for JSON responses."
  [data]
  (when data
    (walk/postwalk
      (fn [x]
        (if (map? x)
          (into (empty x)
            (map (fn [[k v]] [(app-keyword->camel k) v]) x))
          x))
      data)))

(defn db-keyword->app-with-aliases
  "Enhanced keyword normalization for admin services that handles table aliases,
  prefixes, and namespaced keywords from JOIN queries.

  Args:
  - k: keyword or string-like identifier
  - prefixes: collection of string prefixes to strip from the final key name
  - namespaces: set of namespaces (strings) that should be treated as eligible for
    prefix stripping"
  [k prefixes namespaces]
  (when k
    (let [base-name (-> (name k)
                        ;; Strip SQL table aliases (e.g. \"u.id\" -> \"id\")
                      (as-> s (if (and s (str/includes? s "."))
                                (subs s (inc (str/last-index-of s ".")))
                                s))
                        ;; Convert snake_case to kebab-case
                      (str/replace "_" "-"))
          ;; Remove entity prefixes from the name
          base-name (reduce (fn [acc prefix]
                              (if (str/starts-with? acc prefix)
                                (subs acc (count prefix))
                                acc))
                      base-name
                      prefixes)
          ;; Also check namespaced keywords for prefix removal
          base-name (if (and (namespace k) (namespaces (namespace k)))
                      (reduce (fn [acc prefix]
                                (if (str/starts-with? acc prefix)
                                  (subs acc (count prefix))
                                  acc))
                        base-name
                        prefixes)
                      base-name)]
      (keyword base-name))))

(defn normalize-admin-result
  "Comprehensive normalization for admin database results.

  The `config` map supports:
  - :prefixes (vector of strings)
  - :namespaces (set of strings)
  - :id-fields (set of keywords)
  - :transforms (map of keyword -> (fn [v] ...))"
  [data config]
  (when data
    (let [{:keys [prefixes namespaces id-fields transforms]
           :or {prefixes [] namespaces #{} id-fields #{} transforms {}}} config

          normalize-single (fn [item]
                             (when item
                               (let [normalized (into {}
                                                  (keep (fn [[k v]]
                                                          (when-let [k* (db-keyword->app-with-aliases k prefixes namespaces)]
                                                            [k* v])))
                                                  item)]
                                 (reduce-kv
                                   (fn [acc k v]
                                     (cond
                                       ;; Convert ID fields to strings
                                       (id-fields k)
                                       (assoc acc k (some-> v str))

                                       ;; Apply custom transformations
                                       (transforms k)
                                       (assoc acc k ((transforms k) v))

                                       :else
                                       (assoc acc k v)))
                                   {}
                                   normalized))))]

      (cond
        (map? data) (normalize-single data)
        (vector? data) (mapv normalize-single data)
        (seq? data) (map normalize-single data)
        :else data))))
