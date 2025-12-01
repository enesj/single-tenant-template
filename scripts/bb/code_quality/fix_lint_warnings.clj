(ns code-quality.fix-lint-warnings
  "Script to automatically fix lint warnings using clojure-lsp clean-ns"
  (:require
   [clojure.java.io :as io]
   [clojure.java.shell :as shell]
   [clojure.string :as str]))

(defn run-lint
  "Run bb lint and return the output"
  ([]
   (run-lint nil))
  ([file-path]
   (let [command (if file-path
                   ["bb" "lint" file-path]
                   ["bb" "lint"])
         result (apply shell/sh command)]
     (if (zero? (:exit result))
       (:out result)
       (str (:out result) "\n" (:err result))))))

(defn parse-lint-output [lint-output]
  "Parse lint output to extract file paths and warning types"
  (let [lines (str/split-lines lint-output)
        warnings (atom [])]
    (doseq [line lines]
      (when-let [match (re-find #"^([^:]+):(\d+):(\d+):\s+(warning|error):\s+(.+)$" line)]
        (let [[_ file line-num col type message] match]
          (swap! warnings conj {:file file
                                :line (Integer/parseInt line-num)
                                :col (Integer/parseInt col)
                                :type type
                                :message message
                                :raw-line line}))))
    @warnings))

(defn create-backup
  "Create a simple backup file"
  [file-path]
  (let [backup-file (str file-path ".backup." (System/currentTimeMillis))]
    (io/copy (io/file file-path) (io/file backup-file))
    (println (str "   💾 Created backup: " backup-file))
    backup-file))

(defn clean-ns-with-clojure-lsp
  "Use clojure-lsp clean-ns to fix namespace issues in a file"
  [file-path dry-run?]
  (let [command (if dry-run?
                  ["clojure-lsp" "clean-ns" "--dry" "--filenames" file-path]
                  ["clojure-lsp" "clean-ns" "--filenames" file-path])
        result (apply shell/sh command)]
    {:exit-code (:exit result)
     :stdout (:out result)
     :stderr (:err result)
     :success? (zero? (:exit result))}))

(defn fix-unused-binding-in-line [line unused-binding]
  "Fix a specific unused binding in a line by prefixing with _"
  (try
    (let [escaped-binding (java.util.regex.Pattern/quote unused-binding)]
      (cond
        ;; Skip if already prefixed with _
        (str/includes? line (str "_" unused-binding))
        line

        ;; Fix function parameter bindings [param] - single param in brackets
        (re-find (re-pattern (str "\\[\\s*" escaped-binding "\\s*\\]")) line)
        (str/replace line (re-pattern (str "\\[\\s*" escaped-binding "\\s*\\]"))
          (str "[_" unused-binding "]"))

        ;; Fix function parameter bindings [param other-param] - first param
        (re-find (re-pattern (str "\\[\\s*" escaped-binding "\\s+")) line)
        (str/replace line (re-pattern (str "\\[\\s*" escaped-binding "\\s+"))
          (str "[_" unused-binding " "))

        ;; Fix function parameter bindings [other-param param] - last param
        (re-find (re-pattern (str "\\s+" escaped-binding "\\s*\\]")) line)
        (str/replace line (re-pattern (str "\\s+" escaped-binding "\\s*\\]"))
          (str " _" unused-binding "]"))

        ;; Fix function parameter bindings [other-param param another-param] - middle param
        (re-find (re-pattern (str "\\s+" escaped-binding "\\s+")) line)
        (str/replace line (re-pattern (str "\\s+" escaped-binding "\\s+"))
          (str " _" unused-binding " "))

        ;; Fix let bindings - be more specific about context
        (and (re-find (re-pattern (str "\\b" escaped-binding "\\s+")) line)
          (or (str/includes? line "let [")
            (str/includes? line "when-let")
            (str/includes? line "for [")))
        (str/replace line (re-pattern (str "\\b" escaped-binding "\\s+"))
          (str "_" unused-binding " "))

        :else line))
    (catch Exception e
      (println "Error fixing unused binding in line:" (.getMessage e))
      line)))

(defn fix-unused-bindings-in-file
  "Fix unused bindings in a specific file"
  [file-path unused-binding-warnings automatic?]
  (when (seq unused-binding-warnings)
    (let [original-content (slurp file-path)
          lines (str/split-lines original-content)
          fixed-lines (reduce (fn [current-lines warning]
                                (let [line-idx (dec (:line warning))
                                      binding-name (last (str/split (:message warning) #" "))
                                      current-line (get current-lines line-idx)]
                                  (if current-line
                                    (let [fixed-line (fix-unused-binding-in-line current-line binding-name)]
                                      (when (and (not= current-line fixed-line) (not automatic?))
                                        (println (str "   Line " (:line warning) ":"))
                                        (println (str "     - " current-line))
                                        (println (str "     + " fixed-line)))
                                      (assoc current-lines line-idx fixed-line))
                                    current-lines)))
                        (vec lines)
                        unused-binding-warnings)
          fixed-content (str/join "\\n" fixed-lines)]

      (when (not= original-content fixed-content)
        (if automatic?
          (do
            (spit file-path fixed-content)
            (println (str "   ✅ Fixed " (count unused-binding-warnings) " unused bindings automatically")))
          (do
            (print "   Apply unused binding fixes? (y/N): ")
            (flush)
            (when (= (str/lower-case (str/trim (or (read-line) "n"))) "y")
              (spit file-path fixed-content)
              (println (str "   ✅ Fixed " (count unused-binding-warnings) " unused bindings")))))))))

(defn fix-file-warnings
  "Fix warnings in a single file using clojure-lsp and custom unused binding fixes"
  [file-path file-warnings automatic?]
  (println (str "\\n🔄 Processing " (count file-warnings) " warnings in: " file-path))

  (when-not (.exists (io/file file-path))
    (println "   ❌ File does not exist")
    (System/exit 1))

  ;; Create backup
  (let [backup-file (create-backup file-path)]

    ;; Separate unused bindings from namespace issues
    (let [unused-binding-warnings (filter #(str/includes? (:message %) "unused binding") file-warnings)
          namespace-warnings (filter #(or (str/includes? (:message %) "is required but never used")
                                        (str/includes? (:message %) "is referred but never used")
                                        (str/includes? (:message %) "Unused import")
                                        (str/includes? (:message %) "duplicate require")) file-warnings)]

      ;; Fix namespace issues using clojure-lsp clean-ns
      (when (seq namespace-warnings)
        (println (str "   🧹 Using clojure-lsp clean-ns for " (count namespace-warnings) " namespace issues"))
        (if automatic?
          (let [result (clean-ns-with-clojure-lsp file-path false)]
            (if (:success? result)
              (println "   ✅ clojure-lsp clean-ns completed successfully")
              (do
                (println "   ❌ clojure-lsp clean-ns failed:")
                (when (not (str/blank? (:stderr result)))
                  (println (str "   Error: " (:stderr result)))))))
          (do
            ;; Show what clean-ns would do
            (let [dry-result (clean-ns-with-clojure-lsp file-path true)]
              (when (not (str/blank? (:stdout dry-result)))
                (println "   Preview of changes clojure-lsp clean-ns would make:")
                (println (:stdout dry-result)))
              (print "   Apply clojure-lsp clean-ns? (y/N): ")
              (flush)
              (when (= (str/lower-case (str/trim (or (read-line) "n"))) "y")
                (let [result (clean-ns-with-clojure-lsp file-path false)]
                  (if (:success? result)
                    (println "   ✅ clojure-lsp clean-ns completed successfully")
                    (do
                      (println "   ❌ clojure-lsp clean-ns failed:")
                      (when (not (str/blank? (:stderr result)))
                        (println (str "   Error: " (:stderr result))))))))))))

      ;; Fix unused bindings with our custom logic (clojure-lsp doesn't handle this)
      (when (seq unused-binding-warnings)
        (println (str "   🔧 Fixing " (count unused-binding-warnings) " unused bindings"))
        (fix-unused-bindings-in-file file-path unused-binding-warnings automatic?))

      ;; Validate the changes
      (let [post-lint-result (run-lint file-path)
            post-warnings (parse-lint-output post-lint-result)
            post-errors (filter #(= (:type %) "error") post-warnings)]

        (if (empty? post-errors)
          (do
            (println (str "   ✅ File processed successfully - no errors introduced"))
            ;; Clean up backup
            (.delete (io/file backup-file))
            {:success true :warnings-before (count file-warnings) :warnings-after (count post-warnings)})
          (do
            (println "   ❌ Errors introduced - reverting changes")
            (io/copy (io/file backup-file) (io/file file-path))
            (.delete (io/file backup-file))
            {:success false :message "Errors introduced - reverted"}))))))

(defn process-warnings-automatically
  "Process all warnings automatically using clojure-lsp clean-ns and custom unused binding fixes"
  []
  (println "🤖 Running automatic lint fixing with clojure-lsp clean-ns...")
  (let [lint-output (run-lint)
        warnings (parse-lint-output lint-output)]

    (if (empty? warnings)
      (println "✅ No lint warnings found!")
      (let [warnings-by-file (group-by :file warnings)
            total-files (count warnings-by-file)]

        (println (str "📋 Found " (count warnings) " warnings in " total-files " files"))

        (let [results (atom {:total-files 0 :fixed-files 0 :total-warnings-before 0 :total-warnings-after 0})]
          (doseq [[file-path file-warnings] warnings-by-file]
            (let [result (fix-file-warnings file-path file-warnings true)]
              (swap! results update :total-files inc)
              (when (:success result)
                (swap! results update :fixed-files inc))
              (swap! results update :total-warnings-before + (:warnings-before result 0))
              (swap! results update :total-warnings-after + (:warnings-after result 0))))

          (let [final-results @results]
            (println (str "\\n🎉 Processing complete!"))
            (println (str "   Files processed: " (:total-files final-results)))
            (println (str "   Files fixed: " (:fixed-files final-results)))
            (println (str "   Warnings before: " (:total-warnings-before final-results)))
            (println (str "   Warnings after: " (:total-warnings-after final-results)))
            (println (str "   Warnings fixed: " (- (:total-warnings-before final-results) (:total-warnings-after final-results))))))))))

(defn process-warnings-interactively
  "Process lint warnings interactively using clojure-lsp clean-ns and custom unused binding fixes"
  []
  (let [lint-output (run-lint)
        warnings (parse-lint-output lint-output)]

    (if (empty? warnings)
      (println "✅ No lint warnings found!")
      (let [warnings-by-file (group-by :file warnings)]
        (println (str "📋 Found " (count warnings) " warnings in " (count warnings-by-file) " files"))
        (println "\\n🚀 Starting interactive lint fix process using clojure-lsp clean-ns...")

        (doseq [[idx [file-path file-warnings]] (map-indexed vector warnings-by-file)]
          (println (str "\\n[" (inc idx) "/" (count warnings-by-file) "] " file-path " (" (count file-warnings) " warnings)"))
          (fix-file-warnings file-path file-warnings false))

        (println "\\n🎉 Interactive processing complete!")
        (println "💡 Run 'bb lint' again to see remaining issues")))))

(defn main []
  "Main function to fix lint warnings"
  (println "🧹 Lint Warning Fixer using clojure-lsp clean-ns")
  (println "This script uses clojure-lsp clean-ns for namespace issues and custom logic for unused bindings.")
  (println "")

  (let [interactive-mode (some #(= % "--interactive") *command-line-args*)]
    (if interactive-mode
      (process-warnings-interactively)
      (process-warnings-automatically))))

(when (= *file* (System/getProperty "babashka.file"))
  (main))
