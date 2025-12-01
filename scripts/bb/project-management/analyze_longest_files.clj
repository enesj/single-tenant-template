#!/usr/bin/env bb

(require '[clojure.java.io :as io]
  '[clojure.string :as str])

(defn clojure-file? [file]
  "Check if a file has a Clojure extension (.clj, .cljs, .cljc)"
  (let [name (.getName file)]
    (or (str/ends-with? name ".clj")
      (str/ends-with? name ".cljs")
      (str/ends-with? name ".cljc"))))

(defn count-lines [file]
  "Count the number of lines in a file"
  (try
    (with-open [reader (io/reader file)]
      (count (line-seq reader)))
    (catch Exception e
      (println (str "Error reading file " (.getPath file) ": " (.getMessage e)))
      0)))

(defn analyze-file [file]
  "Create a map with file info and line count"
  (let [line-count (count-lines file)
        path (.getPath file)
        size (.length file)]
    {:path path
     :lines line-count
     :size-kb (Math/round (/ size 1024.0))
     :name (.getName file)}))

(defn find-clojure-files [root-dir exclusions]
  "Recursively find all Clojure files, excluding specified directories"
  (->> (file-seq (io/file root-dir))
    (filter #(.isFile %))
    (filter clojure-file?)
    (remove (fn [file]
              (let [path (.getPath file)]
                (some #(str/includes? path %) exclusions))))))

(defn format-file-info [{:keys [path lines size-kb name]}]
  "Format file information for display"
  (format "%4d lines | %4d KB | %s" lines size-kb path))

(println "🔍 Analyzing Clojure files (.clj, .cljs, .cljc) in project...")

(let [root-dir "."
      exclusions #{"/.git/" "/node_modules/" "/target/" "/.shadow-cljs/" "/.cpcache/" "/out/" "/tmp/"}

      _ (println "📁 Root directory:" (.getCanonicalPath (io/file root-dir)))
      _ (println "🚫 Excluding directories with:" (str/join ", " exclusions))
      _ (println)

      files (find-clojure-files root-dir exclusions)
      _ (println (format "Found %d total Clojure files, analyzing..." (count files)))

      analyzed-files (->> files
                       (map analyze-file)
                       (sort-by :lines >)
                       (take 10))

      total-files (count files)
      total-lines (reduce + (map :lines analyzed-files))]

  (println (format "📊 Found %d Clojure files total" total-files))
  (println (format "📏 Top 10 longest files (total: %d lines):" total-lines))
  (println (str/join "" (repeat 70 "─")))

  (doseq [[idx file-info] (map-indexed vector analyzed-files)]
    (println (format "%2d. %s" (inc idx) (format-file-info file-info))))

  (println)
  (println "✨ Analysis complete!"))
