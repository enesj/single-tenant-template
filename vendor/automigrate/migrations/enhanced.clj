(ns automigrate.migrations.enhanced
  "Enhanced migration support for consolidated EDN files"
  (:require
   [automigrate.util.file :as file-util]
   [clojure.java.io :as io]
   [clojure.string :as str]))

(defn read-consolidated-edn-file
  "Read a consolidated EDN file and convert to the format expected by automigrate"
  [file-path]
  (try
    (when (.exists (io/file file-path))
      (let [content (slurp file-path)
            data (read-string content)]
        ;; Convert from map format to vector of maps with :name key
        (mapv (fn [[name migration]]
                (assoc migration :name (str name)))
          data)))
    (catch Exception e
      (println "Error reading consolidated file:" file-path (.getMessage e))
      [])))

(defn read-edn-files-enhanced
  "Read EDN files from either a directory of individual files or a consolidated file"
  [base-path type-name]
  (let [dir-path (file-util/join-path base-path (str "db/" type-name))
        consolidated-file-path (file-util/join-path base-path (str "db/" type-name ".edn"))
        ;; First check if consolidated file exists
        consolidated-data (read-consolidated-edn-file consolidated-file-path)]
    (if (seq consolidated-data)
      (do
        (println (format "Using consolidated %s.edn file with %d entries" type-name (count consolidated-data)))
        consolidated-data)
      ;; Fall back to reading individual files from directory
      (let [dir (io/file dir-path)]
        (if (.exists dir)
          (let [files (let [files (.listFiles dir)]
                        (if files
                          (->> files
                            (filter identity)  ; Remove any null files first
                            (filter #(and % (str/ends-with? (.getName %) ".edn")))
                            (map slurp)
                            (map read-string)
                            (vec))
                          []))]
            (println (format "Using %d individual files from %s/ directory" (count files) type-name))
            files)
          [])))))

(defn generate-extended-migrations-from-edn-enhanced!
  "Enhanced version that supports both individual files and consolidated EDN files"
  [{:keys [resources-dir migrations-dir]}]
  ;; Monkey-patch the original function to use our enhanced reader
  (let [original-fn (resolve 'automigrate.migrations/read-edn-files)]
    (with-redefs [automigrate.migrations/read-edn-files
                  (fn [dir-path]
                    ;; Extract the type from the path (e.g., "db/functions" -> "functions")
                    (if-let [type-match (re-find #"db/(.+)$" dir-path)]
                      (let [type-name (second type-match)
                            base-path (str/replace dir-path #"/db/.+$" "")]
                        (read-edn-files-enhanced base-path type-name))
                      []))]
      ;; Call the original function
      ((resolve 'automigrate.migrations/generate-extended-migrations-from-edn!)
       {:resources-dir resources-dir
        :migrations-dir migrations-dir}))))
