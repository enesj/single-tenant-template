(ns app.template.backend.migrations.alignment.utils
  "Common utilities for alignment checks."
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]))

(def ^:const default-migrations-dir "resources/db/migrations")
(def ^:const default-db-root "resources/db")

(def internal-tables
  "Tables that exist for migration bookkeeping and should not be compared to models."
  #{"automigrate_migrations"})

(defn now-iso []
  (-> (java.time.OffsetDateTime/now) (.toString)))

(defn normalize-ident
  "Normalize a keyword/string identifier into the DB-friendly form."
  [x]
  (-> (cond
        (keyword? x) (name x)
        (string? x) x
        :else (str x))
    (str/replace "-" "_")
    (str/lower-case)))

(defn read-edn-file
  "Read an EDN file that may contain comments. Returns {} if it doesn't exist."
  [path]
  (let [f (io/file path)]
    (if (.exists f)
      (read-string (slurp f))
      {})))

(defn discover-domain-subdirs
  "Return the names of domain subdirectories under resources/db/domain."
  [db-root]
  (let [base (io/file (str db-root "/domain"))]
    (if (and (.exists base) (.isDirectory base))
      (->> (.listFiles base)
        (filter identity)
        (filter #(.isDirectory %))
        (map #(.getName %))
        (remove #(str/starts-with? % "."))
        (sort))
      [])))

(defn read-hierarchical-edn
  "Merge template + domain (direct + subdirs) + shared for a given file-name.

  Merge order: template < domain < shared (shared wins)."
  [db-root file-name]
  (let [template-path (str db-root "/template/" file-name)
        shared-path (str db-root "/shared/" file-name)
        domain-direct-path (str db-root "/domain/" file-name)
        domain-subdirs (discover-domain-subdirs db-root)
        domain-subdir-data
        (reduce
          (fn [acc d]
            (merge acc (read-edn-file (str db-root "/domain/" d "/" file-name))))
          {}
          domain-subdirs)]
    (merge
      (read-edn-file template-path)
      (read-edn-file domain-direct-path)
      domain-subdir-data
      (read-edn-file shared-path))))

(defn q
  "Execute a query and return results as unqualified lower-case maps."
  [db sqlvec]
  (jdbc/execute! db sqlvec {:builder-fn rs/as-unqualified-lower-maps}))
