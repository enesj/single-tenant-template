#!/usr/bin/env bb

(ns code-quality.fix-str-warnings
  (:require
   [clojure.java.shell :refer [sh]]
   [clojure.string :as str]))

(defn parse-warning-line [line]
  (when-let [[_ file line-num warning] (re-matches #"([^:]+):(\d+):\d+: info: (Single argument to str already is a string.*)" line)]
    {:file file
     :line-num (Integer/parseInt line-num)
     :warning warning}))

(defn get-str-warnings []
  (let [result (sh "bb" "lint")
        output (:out result)
        lines  (str/split-lines output)]
    (->> lines
      (keep parse-warning-line)
      (filter #(str/includes? (:warning %) "Single argument to str already is a string")))))

(defn fix-warning [{:keys [file line-num]}]
  (println "Fixing" file "at line" line-num)
  (let [content (slurp file)
        lines (str/split-lines content)
        target-line (dec line-num) ;; 0-based index
        line-content (nth lines target-line)
        fixed-content (-> line-content
                        (str/replace #"\(str\s+\"([^\"]+)\"\)" "\"$1\"")
                        (str/replace #"\(str\s+\(([^()]+)\)\)" "($1)")
                        (str/replace #"\(str\s+([^\s()]+)\)" "$1"))
        new-lines (assoc lines target-line fixed-content)
        new-content (str/join "\n" new-lines)]
    (spit file new-content)))

(defn -main []
  (println "Finding redundant str calls in codebase...")
  (let [warnings (get-str-warnings)]
    (if (seq warnings)
      (do
        (println "Found" (count warnings) "redundant str calls. Fixing...")
        (run! fix-warning warnings)
        (println "Done! Fixed" (count warnings) "occurrences."))
      (println "No redundant str calls found."))))

(-main)
