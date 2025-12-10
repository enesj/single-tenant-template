#!/usr/bin/env bb

(require '[babashka.fs :as fs])
(require '[babashka.process :as process])
(require '[clojure.string :as str])

;; Load all the functions from the original script
(load-file "scripts/repair-lints.clj")

(defn check-clojure-syntax [file-path]
  "Check if a Clojure file has valid syntax using clojure.core/read-string"
  (try
    (let [content (slurp file-path)]
      ;; Try to read the first form to check basic syntax
      (clojure.core/read-string content)
      true)
    (catch Exception e
      (println "Syntax error in" file-path ":" (.getMessage e))
      false)))

(defn safe-apply-warnings-to-file [file entries]
  "Apply warnings with additional safety checks"
  (let [path (fs/file file)]
    (if-not (fs/exists? path)
      (binding [*out* *err*]
        (println "Skipping" file "(not found)"))
      (do
        ;; Create backup
        (let [backup-path (str file ".backup." (System/currentTimeMillis))]
          (fs/copy path backup-path)
          (println "Created backup:" backup-path)

          ;; Apply the original logic
          (apply-warnings-to-file file entries)

          ;; Check if the modified file still has valid syntax
          (if (check-clojure-syntax file)
            (do
              (println "✅ File syntax is valid after changes")
              ;; Clean up backup if successful
              (fs/deleteIfExists backup-path)
              true)
            (do
              (println "❌ File syntax became invalid - reverting changes")
              ;; Restore from backup
              (fs/copy backup-path path)
              (fs/deleteIfExists backup-path)
              false))))))

(defn safe-run-clean-ns [files]
  "Run clojure-lsp clean-ns with error handling"
  (try
    (let [file-arg (str/join "," files)
          cmd ["clojure-lsp" "clean-ns" "--filenames" file-arg]
          {:keys [exit]} (process/sh cmd {:out :inherit :err :inherit :check false})]
      (when (not= 0 exit)
        (binding [*out* *err*]
          (println "clojure-lsp clean-ns failed with exit code" exit)))
      (= 0 exit))
    (catch java.io.IOException _
      (binding [*out* *err*]
        (println "clojure-lsp executable not found; skipping clean-ns for" (str/join ", " files)))
      false)))

;; Main execution - same as original but with safety checks
(let [{:keys [out err exit]} (process/sh lint-command {:out :string :err :string :check false})
      output (str out (when (seq err) err)]
  (print output)
  (flush)
  (let [warnings (->> (str/split-lines output)
                      (keep parse-warning))
        unused-binding-warnings (filter #(= :unused-binding (:type %)) warnings)
        clean-ns-files (->> warnings
                            (filter #(= :clean-ns (:type %)))
                            (map :file)
                            distinct
                            vec)]

    ;; Process unused bindings with safety checks
    (doseq [[file entries] (group-by :file unused-binding-warnings)]
      (println "\nProcessing unused bindings in:" file)
      (safe-apply-warnings-to-file file entries))

    ;; Process clean-ns files with safety checks
    (when (seq clean-ns-files)
      (println "\nRunning clojure-lsp clean-ns on:" (str/join ", " clean-ns-files))
      (flush)
      (safe-run-clean-ns clean-ns-files))

    (System/exit exit)))