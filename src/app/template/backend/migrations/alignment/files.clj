(ns app.template.backend.migrations.alignment.files
  "Migration file parsing and DB migration tracking."
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
    [app.template.backend.migrations.alignment.utils :as utils]))

(defn list-migration-files
  [dir]
  (let [dir (io/file dir)]
    (when-not (.exists dir)
      (throw (ex-info "Migrations directory not found" {:dir (.getPath dir)})))
    (->> (file-seq dir)
      (filter #(.isFile %))
      (map #(.getName %))
      (remove #(or (str/blank? %) (= % ".DS_Store")))
      (sort))))

(defn parse-migration-filename
  "Parse a migration filename like `0001_schema.edn`.

  Returns a map like:
    {:file 0001_schema.edn :number 1 :name 0001_schema :ext edn}

  If unparseable, returns:
    {:file <name> :unparseable true}"
  [file]
  (if-let [[_ n base ext] (re-matches #"^(\d+)_([^.]+)\.(.+)$" file)]
    (let [number (Integer/parseInt n)]
      {:file file
       :number number
       :name (str n "_" base)
       :ext ext})
    {:file file :unparseable true}))

(defn migration-file-report
  [{:keys [migrations-dir]}]
  (let [files (list-migration-files migrations-dir)
        parsed (mapv parse-migration-filename files)
        unparseable (->> parsed (filter :unparseable) (map :file) sort)
        by-number (group-by :number (remove :unparseable parsed))
        duplicates (->> by-number
                     (filter (fn [[_ xs]] (> (count xs) 1)))
                     (into (sorted-map)))
        names (->> parsed
                (remove :unparseable)
                (map :name)
                (set))]
    {:files files
     :parsed parsed
     :names names
     :duplicates duplicates
     :unparseable unparseable}))

(defn db-applied-migrations
  "Return the set of migration names applied in DB.

  If the tracking table does not exist, returns an empty set."
  [db]
  (try
    (->> (utils/q db ["SELECT name FROM automigrate_migrations ORDER BY created_at ASC"])
      (map :name)
      (set))
    (catch Exception e
      (let [msg (ex-message e)]
        (if (re-find #"relation .+ does not exist" msg)
          #{}
          (throw e))))))
