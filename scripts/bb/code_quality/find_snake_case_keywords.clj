#!/usr/bin/env bb

(ns find-snake-case-keywords
  "Script to find snake_case keywords in Clojure files and sort by occurrence count"
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]))

;; ANSI Color codes for terminal output
(def colors
  {:reset   "\033[0m"
   :bold    "\033[1m"
   :red     "\033[31m"
   :green   "\033[32m"
   :yellow  "\033[33m"
   :blue    "\033[34m"
   :magenta "\033[35m"
   :cyan    "\033[36m"
   :white   "\033[37m"
   :gray    "\033[90m"})

(defn colorize
  "Apply ANSI color codes to text"
  [color text]
  (str (get colors color "") text (:reset colors)))

(defn print-colored
  "Print text with color"
  [color text]
  (println (colorize color text)))

(defn find-cljs-files
  "Recursively find all .cljs files in the given directory"
  [dir]
  (->> (file-seq (io/file dir))
    (filter #(.isFile %))
    (filter #(str/ends-with? (.getName %) ".cljs"))
    (map #(.getAbsolutePath %))))

(defn find-clj-files
  "Recursively find all .clj files in the given directory"
  [dir]
  (->> (file-seq (io/file dir))
    (filter #(.isFile %))
    (filter #(str/ends-with? (.getName %) ".clj"))
    (map #(.getAbsolutePath %))))

(defn extract-keywords
  "Extract all keywords from a string of Clojure code"
  [content]
  (->> content
    (re-seq #":[a-zA-Z][a-zA-Z0-9_-]*")
    (map #(subs % 1)) ; Remove the : prefix
    set))

(defn snake-case?
  "Check if a string is in snake_case format"
  [s]
  (and (re-matches #"[a-z][a-z0-9_]*" s)
    (str/includes? s "_")
    (not (str/starts-with? s "_"))
    (not (str/ends-with? s "_"))))

(defn count-snake-case-keywords
  "Count snake_case keywords in a file"
  [file-path]
  (try
    (let [content (slurp file-path)
          keywords (extract-keywords content)
          snake-keywords (filter snake-case? keywords)]
      {:file file-path
       :count (count snake-keywords)
       :keywords (sort snake-keywords)})
    (catch Exception e
      (println "Error processing" file-path ":" (.getMessage e))
      {:file file-path :count 0 :keywords []})))

(defn relative-path
  "Convert absolute path to relative path from project root"
  [abs-path project-root]
  (str/replace abs-path (str project-root "/") ""))

(defn analyze-files
  "Analyze all Clojure files and return results sorted by snake_case keyword count"
  [project-root file-type]
  (let [files (case file-type
                "cljs" (find-cljs-files (str project-root "/src"))
                "clj" (find-clj-files (str project-root "/src"))
                (do
                  (println "Invalid file type. Use 'clj' or 'cljs'")
                  []))
        results (map count-snake-case-keywords files)
        non-zero-results (filter #(> (:count %) 0) results)
        sorted-results (sort-by :count > non-zero-results)]
    (map #(update % :file relative-path project-root) sorted-results)))

(defn print-summary
  "Print a summary of the analysis"
  [results]
  (let [total-files (count results)
        total-keywords (reduce + (map :count results))]
    (println)
    (print-colored :cyan (str "═══ " (colorize :bold "SNAKE_CASE KEYWORD ANALYSIS SUMMARY") " ═══"))
    (print-colored :green (str "Files with snake_case keywords: " (colorize :bold (str total-files))))
    (print-colored :yellow (str "Total snake_case keywords found: " (colorize :bold (str total-keywords))))
    (print-colored :cyan (str "═" (str/join (repeat 50 "═")) "═"))
    (println)))

(defn print-keyword-list
  "Print just the unique snake_case keywords found across all files"
  [results file-type]
  (let [all-keywords (->> results
                       (mapcat :keywords)
                       (into #{})
                       sort)]
    (print-colored :blue (str "🔍 Snake_case keywords found in ." file-type " files (" (count all-keywords) " unique):"))
    (println)
    (doseq [keyword all-keywords]
      (print-colored :magenta (str ":" keyword)))))

(defn main
  "Main function to run the analysis"
  [& args]
  (let [options (set args)
        non-flag-args (remove #(str/starts-with? % "--") args)
        file-type (or (some #{"clj" "cljs"} non-flag-args) "cljs")
        project-root (or (first (remove #(str/starts-with? % "--")
                                  (remove #{"clj" "cljs"} non-flag-args)))
                       (System/getProperty "user.dir"))
        limit-str (some #(when (str/starts-with? % "--limit=")
                           (subs % 8)) args)
        file-limit (when limit-str
                     (try (Integer/parseInt limit-str)
                       (catch Exception _ nil)))
        _ (print-colored :cyan (str "🔍 Searching for snake_case keywords in ." file-type " files..."))
        _ (print-colored :gray (str "📁 Project root: " project-root))
        results (analyze-files project-root file-type)
        limited-results (if file-limit
                          (take file-limit results)
                          results)]

    (if (empty? results)
      (print-colored :green (str "✅ No snake_case keywords found in ." file-type " files!"))
      (if (contains? options "--list")
        (print-keyword-list results file-type)
        (do
          (print-summary results)
          (when (and file-limit (> (count results) file-limit))
            (print-colored :yellow (str "📊 Showing top " file-limit " files (out of " (count results) " total)")))
          (doseq [{:keys [file count keywords]} limited-results]
            (println (str (colorize :blue "📄 ")
                       (colorize :white file)
                       " "
                       (colorize :yellow (str "(" count " keywords)"))))
            (when (seq keywords)
              (println (str "   "
                         (colorize :gray "Keywords: ")
                         (colorize :magenta (str/join ", " keywords)))))
            (println)))))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply main *command-line-args*))
