(ns app.migrations.hierarchical-models
  "Fixed version of hierarchical model reading to avoid nil pointer exception"
  (:require
    [clojure.java.io :as io]))

(defn read-consolidated-edn
  "Safely read an EDN file, returning empty map if file doesn't exist.
   Handles comments in EDN files."
  [file-path]
  (try
    (let [file (io/file file-path)]
      (if (.exists file)
        (let [content (slurp file)]
          ;; Use read-string which handles comments better than edn/read
          (read-string content))
        {}))
    (catch Exception e
      (println (str "Error reading " file-path ": " (.getMessage e)))
      {})))

(defn read-hierarchical-edn
  "Read EDN files from hierarchical structure (template/domain/*/shared) and merge them.
   Supports both direct domain files (domain/models.edn) and subdirectory-based organization (domain/*/models.edn).
   Fixed version that handles nil file objects properly."
  [base-path file-name]
  (let [template-path (str base-path "/template/" file-name)
        shared-path (str base-path "/shared/" file-name)

        ;; Check for direct domain file (e.g., domain/models.edn)
        domain-direct-path (str base-path "/domain/" file-name)
        domain-direct-data (read-consolidated-edn domain-direct-path)

        ;; Dynamically discover all domain subdirectories
        domain-base-path (str base-path "/domain")
        domain-dirs (when (.exists (io/file domain-base-path))
                      (let [base-file (io/file domain-base-path)]
                        (when (.isDirectory base-file)
                          (let [files (.listFiles base-file)]
                            (when (and files (pos? (count files)))
                              (->> files
                                (filter #(and % (.isDirectory %)))
                                (map #(.getName %))
                                (filter some?)))))))

        ;; Read template and shared data
        template-data (read-consolidated-edn template-path)
        shared-data (read-consolidated-edn shared-path)

        ;; Read all domain subdirectory data
        domain-subdir-data (when domain-dirs
                             (reduce (fn [acc domain-name]
                                       (let [domain-path (str domain-base-path "/" domain-name "/" file-name)
                                             data (read-consolidated-edn domain-path)]
                                         (merge acc data)))
                               {}
                               domain-dirs))
        
        ;; Merge direct domain file with subdirectory data
        domain-data (merge domain-direct-data domain-subdir-data)]

    (println "Domain direct file:" (if (seq domain-direct-data) domain-direct-path "not found"))
    (println "Domain subdirectories found:" (or domain-dirs []))
    (merge template-data domain-data shared-data)))
