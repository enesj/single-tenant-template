#!/usr/bin/env bb

(ns code-quality.format-edn
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.pprint :as pprint]))

(defn format-edn-file
  "Read EDN file, parse it, and write it back with pretty printing"
  [file-path]
  (try
    (let [content (slurp file-path)
          parsed (edn/read-string content)]
      (with-out-str
        (pprint/pprint parsed)))
    (catch Exception e
      (println "Error parsing EDN file:" (.getMessage e))
      (System/exit 1))))

(defn main [& args]
  (if (empty? args)
    (do
      (println "Usage: bb scripts/format_edn.clj <file-path>")
      (println "Example: bb scripts/format_edn.clj some-file.edn")
      (System/exit 1))
    (let [file-path (first args)]
      (if (.exists (io/file file-path))
        (let [formatted-content (format-edn-file file-path)]
          (spit file-path formatted-content)
          (println (str "✅ Formatted " file-path " with pretty print")))
        (do
          (println (str "❌ File not found: " file-path))
          (System/exit 1))))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply main *command-line-args*))
