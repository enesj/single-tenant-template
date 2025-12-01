(ns code-quality.fix-misplaced-docstrings
  "Utility to automatically correct clj-kondo \"Misplaced docstring.\" warnings by moving docstrings to their proper position."
  (:require
   [clojure.data.json :as json]
   [clojure.java.shell :refer [sh]]
   [clojure.string :as str])
  (:gen-class))

(def misplaced-msg "Misplaced docstring.")

(defn misplaced-docstring? [p]
  (= misplaced-msg (:message p)))

(defn group-by-file [problems]
  (->> problems (filter misplaced-docstring?) (group-by :filename)))

(defn find-function-boundaries
  "Find the start and end of a function definition starting from start-line"
  [lines start-line]
  (let [start-idx (dec start-line)
        ; Find the line with (defn or (defn-
        defn-line-idx (loop [i start-idx]
                        (if (or (< i 0) (>= i (count lines)))
                          nil
                          (let [line (nth lines i)]
                            (if (re-find #"^\s*\(\s*defn-?\s+" line)
                              i
                              (recur (dec i))))))
        ; Find matching closing paren
        closing-idx (when defn-line-idx
                      (loop [i defn-line-idx
                             paren-count 0]
                        (if (>= i (count lines))
                          (dec (count lines))
                          (let [line (nth lines i)
                                opens (count (re-seq #"\(" line))
                                closes (count (re-seq #"\)" line))
                                new-count (+ paren-count opens (- closes))]
                            (if (and (> i defn-line-idx) (= new-count 0))
                              i
                              (recur (inc i) new-count))))))]
    (when (and defn-line-idx closing-idx)
      {:start defn-line-idx :end closing-idx})))

(defn extract-function-parts
  "Extract parts of a function definition"
  [lines start end]
  (let [func-lines (subvec (vec lines) start (inc end))
        full-text (str/join "\n" func-lines)
        ; Pattern to match: (defn name [args] "docstring" body)
        pattern #"(?s)^(\s*\(\s*defn-?\s+)([^\s\[\]]+)(\s+)(\[[^\]]*\])(\s+)(\"[^\"]*\")(.*)$"]
    (when-let [match (re-find pattern full-text)]
      (let [[_ defn-part name-part space1 args-part space2 docstring-part rest-part] match]
        {:defn-part defn-part
         :name-part name-part
         :space1 space1
         :args-part args-part
         :space2 space2
         :docstring-part docstring-part
         :rest-part rest-part
         :start start
         :end end}))))

(defn fix-function-docstring
  "Rearrange function parts to move docstring before args"
  [parts]
  (let [{:keys [defn-part name-part _space1 args-part _space2 docstring-part rest-part]} parts]
    (str defn-part name-part "\n  " docstring-part "\n  " args-part rest-part)))

(defn fix-misplaced-docstring-in-lines
  "Fix a single misplaced docstring in the given lines"
  [lines problematic-line]
  (when-let [boundaries (find-function-boundaries lines problematic-line)]
    (when-let [parts (extract-function-parts lines (:start boundaries) (:end boundaries))]
      (let [fixed-function (fix-function-docstring parts)
            fixed-lines (str/split-lines fixed-function)
            before-lines (subvec (vec lines) 0 (:start boundaries))
            after-lines (subvec (vec lines) (inc (:end boundaries)))]
        (vec (concat before-lines fixed-lines after-lines))))))

(defn fix-file!
  "Fix misplaced docstrings in a file by moving them to correct positions"
  [fpath rows]
  (try
    (let [original-lines (str/split-lines (slurp fpath))]
      (loop [lines original-lines
             remaining-rows (sort > rows)                   ; Process from bottom to top to preserve line numbers
             fixed-count 0]
        (if (empty? remaining-rows)
          (do
            (spit fpath (str/join "\n" lines))
            (println "Successfully moved" fixed-count "docstrings in" fpath))
          (let [row (first remaining-rows)
                fixed-lines (fix-misplaced-docstring-in-lines lines row)]
            (if fixed-lines
              (recur fixed-lines (rest remaining-rows) (inc fixed-count))
              (do
                (println "Could not fix docstring at line" row "in" fpath "- using comment fallback")
                ; Fallback to commenting out the line
                (let [line-idx (dec row)
                      old-line (nth lines line-idx)
                      commented-line (if-let [[_ indent txt] (re-matches #"(\s*)\"(.*)\"" old-line)]
                                       (str indent "; " txt)
                                       old-line)
                      new-lines (assoc (vec lines) line-idx commented-line)]
                  (recur new-lines (rest remaining-rows) fixed-count))))))))
    (catch Exception e
      (println "Error processing" fpath ":" (.getMessage e)))))

;; -----------------------------------------------------------------------------
;; CLI
;; -----------------------------------------------------------------------------

(defn parse-line [line]
  (when-let [[_ file row _sev msg] (re-matches #"(.+?):(\d+):\d+:\s+(\w+):\s+(.*)" line)]
    {:filename file :row (Integer/parseInt row) :message msg}))

(defn run-clj-kondo
  "Run clj-kondo on `paths` and return parsed JSON report map.
  Attempts the `clj-kondo` binary first, then falls back to
  `clj -M -m clj-kondo.main` using the dependency on the classpath."
  [paths]
  (letfn [(run* [cmd]
            (let [{:keys [out err]} (apply sh cmd)]
              (when (seq err) (binding [*out* *err*] (println err)))
              (->> (str/split-lines out)
                (map parse-line)
                (remove nil?))))]
    (or (seq (run* (concat ["clj-kondo" "--lint"] paths)))
      (run* (concat ["clj" "-M" "-m" "clj-kondo.main" "--lint"] paths)))))

(defn -main
  "Usage:
     clj -M -m fix-misplaced-docstrings [<paths> ...]
     clj -M -m fix-misplaced-docstrings <report.json>
  If one argument ending in `.json` is supplied, it is treated as a pre-generated
  clj-kondo JSON report. Otherwise, the arguments are treated as paths to lint.
  When no arguments are provided, `src` and `test` are linted."
  [& args]
  (let [args (vec args)
        {:keys [report paths]} (if (and (= 1 (count args)) (str/ends-with? (first args) ".json"))
                                 {:report (-> (first args) slurp (json/read-str :key-fn keyword))}
                                 {:paths (if (seq args) args ["src" "test" "cli-tools"])})
        problems (or report (run-clj-kondo paths))
        by-file (group-by-file problems)]
    (doseq [[fp entries] by-file]
      (println "Fixing" fp (count entries) "docstrings")
      (fix-file! fp (map :row entries)))))

;; Allow running under Babashka (bb fix_misplaced_docstrings.clj report.json)
(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
