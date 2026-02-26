(ns code-quality.unused-public-var
  "Extract unused-related diagnostics from clojure-lsp and save to file.

  Captures: unused-public-var, unused-namespace, unused-referred-var, unused-private-var

  Usage:
    bb scripts/bb/code_quality/unused_public_var.clj [options]

  Options:
    --output, -o <path>  Output file path (default: tmp/unused_public_var.txt)
    --domain             Include src/app/domain/ results
    --template           Include src/app/template/ results
    --admin              Include src/app/admin/ results
    --shared             Include src/app/shared/ results
    --help               Show this help

  Default behavior (no filter flags):
    Includes domain and admin, excludes template and shared.
    Vendor is always excluded.

  Output format:
    One diagnostic per line: file:line:col: severity: [diagnostic-type] message"
  (:require
    [babashka.fs :as fs]
    [babashka.process :as p]
    [clojure.string :as str]))

(defn parse-args
  [args]
  (loop [opts {:output "tmp/unused_public_var.txt"
               :domain false
               :template false
               :admin false
               :shared false}
         remaining args]
    (if (empty? remaining)
      opts
      (let [[arg next-arg] remaining]
        (cond
          (or (= arg "--output") (= arg "-o"))
          (recur (assoc opts :output next-arg) (drop 2 remaining))

          (= arg "--domain")
          (recur (assoc opts :domain true) (drop 1 remaining))

          (= arg "--template")
          (recur (assoc opts :template true) (drop 1 remaining))

          (= arg "--admin")
          (recur (assoc opts :admin true) (drop 1 remaining))

          (= arg "--shared")
          (recur (assoc opts :shared true) (drop 1 remaining))

          (= arg "--help")
          (do
            (println "Usage: bb scripts/bb/code_quality/unused_public_var.clj [options]")
            (println "")
            (println "Options:")
            (println "  --output, -o <path>  Output file path (default: tmp/unused_public_var.txt)")
            (println "  --domain             Include src/app/domain/ results")
            (println "  --template           Include src/app/template/ results")
            (println "  --admin              Include src/app/admin/ results")
            (println "  --shared             Include src/app/shared/ results")
            (println "  --help               Show this help")
            (println "")
            (println "Default (no filter flags): includes domain and admin, excludes template and shared.")
            (println "Vendor is always excluded.")
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

(defn domain-path?
  "Check if a file path is in src/app/domain/."
  [line]
  (str/starts-with? line "src/app/domain/"))

(defn template-path?
  "Check if a file path is in src/app/template/."
  [line]
  (str/starts-with? line "src/app/template/"))

(defn admin-path?
  "Check if a file path is in src/app/admin/."
  [line]
  (str/starts-with? line "src/app/admin/"))

(defn shared-path?
  "Check if a file path is in src/app/shared."
  [line]
  (str/starts-with? line "src/app/shared/"))

(defn make-path-filter
  "Create a filter function based on selected path flags.
   Returns true if the line should be included in results."
  [{:keys [domain template admin shared]}]
  (let [;; If no filter flags specified, use default: domain + admin
        use-default? (not (or domain template admin shared))
        include-domain? (or domain use-default?)
        include-admin? (or admin use-default?)
        include-template? template
        include-shared? shared]
    (fn [line]
      (and
        ;; Always exclude vendor
        (not (vendor-path? line))
        ;; Include based on selected paths
        (or
          (and include-domain? (domain-path? line))
          (and include-admin? (admin-path? line))
          (and include-template? (template-path? line))
          (and include-shared? (shared-path? line)))))))

(defn unused-diagnostic?
  "Check if a line contains any unused-related diagnostic."
  [line]
  (or (str/includes? line "[clojure-lsp/unused-public-var]")
      (str/includes? line "[unused-namespace]")
      (str/includes? line "[unused-referred-var]")
      (str/includes? line "[unused-private-var]")))

(defn filter-unused-public-var
  "Filter diagnostics for unused-related entries based on selected paths."
  [output opts]
  (let [path-filter (make-path-filter opts)]
    (->> (str/split-lines output)
      (filter unused-diagnostic?)
      (filter path-filter)
      (map str/trim)
      (filter (complement str/blank?)))))

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
      (let [filtered (filter-unused-public-var raw-output opts)
            result (save-results filtered output-path)]
        (println (format "Found %d unused diagnostics (public-var, namespace, referred-var, private-var)" (:count result)))
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