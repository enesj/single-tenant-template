(ns code-quality.unused-public-var
  "Extract unused-public-var diagnostics from clojure-lsp and save to file.

  Usage:
    bb scripts/bb/code_quality/unused_public_var.clj [--output <path>]

  Options:
    --output, -o   Output file path (default: tmp/unused_public_var.txt)

  Output format:
    One diagnostic per line: file:line:col: severity: [clojure-lsp/unused-public-var] message"
  (:require
    [babashka.fs :as fs]
    [babashka.process :as p]
    [clojure.string :as str]))

(defn parse-args
  [args]
  (loop [opts {:output "tmp/unused_public_var.txt"}
         remaining args]
    (if (empty? remaining)
      opts
      (let [[arg next-arg] remaining]
        (cond
          (or (= arg "--output") (= arg "-o"))
          (recur (assoc opts :output next-arg) (drop 2 remaining))

          (= arg "--help")
          (do
            (println "Usage: bb scripts/bb/code_quality/unused_public_var.clj [options]")
            (println "")
            (println "Options:")
            (println "  --output, -o <path>  Output file path (default: tmp/unused_public_var.txt)")
            (println "  --help               Show this help")
            (System/exit 0))

          :else
          (recur opts (drop 1 remaining)))))))

(defn run-clojure-lsp-diagnostics
  "Run clojure-lsp diagnostics --raw and return output."
  []
  (let [result (p/shell {:out :string :err :string :continue true}
                        "clojure-lsp" "diagnostics" "--raw")]
    (if (or (zero? (:exit result)) (seq (:out result)))
      (:out result)
      (do
        (binding [*out* *err*]
          (println "clojure-lsp diagnostics failed:" (:err result)))
        nil))))

(defn vendor-path?
  "Check if a file path is in the vendor directory."
  [line]
  (str/starts-with? line "vendor/"))

(defn template-path?
  "Check if a file path is in src/app/template/."
  [line]
  (str/starts-with? line "src/app/template/"))

(defn shared-path?
  "Check if a file path is in src/app/shared."
  [line]
  (str/starts-with? line "src/app/shared/"))

(defn excluded-path?
  "Check if a file path should be excluded (vendor, template, or shared)."
  [line]
  (or (vendor-path? line)
      (template-path? line)
      (shared-path? line)))

(defn filter-unused-public-var
  "Filter diagnostics for only unused-public-var entries, excluding vendor, template, and shared folders."
  [output]
  (->> (str/split-lines output)
    (filter (fn [line]
              (str/includes? line "[clojure-lsp/unused-public-var]")))
    (remove excluded-path?)
    (map str/trim)
    (filter (complement str/blank?))))

(defn save-results
  "Save filtered results to file."
  [results output-path]
  (let [dir (fs/parent output-path)]
    (when (and dir (not (fs/exists? dir)))
      (fs/create-dirs dir)))
  (if (seq results)
    (do
      (spit output-path (str/join "\n" results))
      {:count (count results)
       :path output-path
       :success true})
    (do
      (spit output-path "")
      {:count 0
       :path output-path
       :success true})))

(defn -main
  [& args]
  (let [opts (parse-args args)
        output-path (:output opts)]
    (println "Running clojure-lsp diagnostics...")
    (if-let [raw-output (run-clojure-lsp-diagnostics)]
      (let [filtered (filter-unused-public-var raw-output)
            result (save-results filtered output-path)]
        (println (format "Found %d unused-public-var diagnostics (vendor/template/shared excluded)" (:count result)))
        (println (format "Saved to: %s" (:path result)))
        (when (seq filtered)
          (println "\n--- Preview (first 10) ---")
          (doseq [line (take 10 filtered)]
            (println line))
          (when (> (count filtered) 10)
            (println (format "... and %d more" (- (count filtered) 10))))))
      (do
        (println "Failed to get diagnostics from clojure-lsp")
        (System/exit 1)))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))