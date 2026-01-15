#!/usr/bin/env bb

(ns analyze-longest-files
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]))

(defn clojure-file?
  "Check if a file has a Clojure extension (.clj, .cljs, .cljc)."
  [file]
  (let [name (.getName file)]
    (or (str/ends-with? name ".clj")
      (str/ends-with? name ".cljs")
      (str/ends-with? name ".cljc"))))

(defn count-lines
  "Count the number of lines in a file."
  [file]
  (try
    (with-open [reader (io/reader file)]
      (count (line-seq reader)))
    (catch Exception e
      (println (str "Error reading file " (.getPath file) ": " (.getMessage e)))
      0)))

(defn analyze-file
  "Create a map with file info and line count."
  [file]
  (let [line-count (count-lines file)
        path (.getPath file)
        size (.length file)]
    {:path path
     :lines line-count
     :size-kb (Math/round (/ size 1024.0))
     :name (.getName file)}))

(defn find-clojure-files
  "Recursively find all Clojure files, excluding specified directories."
  [root-dir exclusions]
  (->> (file-seq (io/file root-dir))
    (filter #(.isFile %))
    (filter clojure-file?)
    (remove (fn [file]
              (let [path (.getPath file)]
                (some #(str/includes? path %) exclusions))))))

(defn format-file-info
  "Format file information for display."
  [{:keys [path lines size-kb]}]
  (format "%4d lines | %4d KB | %s" lines size-kb path))

(defn -main
  "Print a quick report of the top 10 longest Clojure source files."
  [& _args]
  (println "🔍 Analyzing Clojure files (.clj, .cljs, .cljc) in project...")
  (let [root-dir "."
        exclusions #{"/.git/" "/node_modules/" "/target/" "/.shadow-cljs/" "/.cpcache/" "/out/" "/tmp/"}]

    (println "📁 Root directory:" (.getCanonicalPath (io/file root-dir)))
    (println "🚫 Excluding directories with:" (str/join ", " exclusions))
    (println)

    (let [files (find-clojure-files root-dir exclusions)
          analyzed-files (->> files
                           (map analyze-file)
                           (sort-by :lines >)
                           (take 10))
          total-files (count files)
          total-lines (reduce + (map :lines analyzed-files))]

      (println (format "Found %d total Clojure files, analyzing..." total-files))
      (println (format "📊 Found %d Clojure files total" total-files))
      (println (format "📏 Top 10 longest files (total: %d lines):" total-lines))
      (println (str/join "" (repeat 70 "─")))

      (doseq [[idx file-info] (map-indexed vector analyzed-files)]
        (println (format "%2d. %s" (inc idx) (format-file-info file-info))))

      (println)
      (println "✨ Analysis complete!"))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
