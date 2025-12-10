#!/usr/bin/env bb

(require '[babashka.fs :as fs])
(require '[babashka.process :as process])
(require '[clojure.string :as str])

;; Load all the functions from the original script
(load-file "scripts/repair-lints.clj")

(defn get-file-type [file-path]
  "Determine if this is a Clojure or ClojureScript file"
  (cond
    (str/ends-with? file-path ".cljs") :clojurescript
    (str/ends-with? file-path ".cljc") :clojure  ; Treat cljc as clojure for validation
    (str/ends-with? file-path ".clj") :clojure
    :else :unknown))

(defn validate-clojure-file [file-path]
  "Validate a .clj file by trying to load it with clojure"
  (try
    (println "  🔍 Validating Clojure file:" file-path)
    (let [{:keys [exit]} (process/sh ["clj" "-e" (str "(load-file \"" file-path "\")")]
                                     {:out :inherit :err :inherit})]
      (if (= 0 exit)
        (do
          (println "  ✅ Clojure file valid:" file-path)
          true)
        (do
          (println "  ❌ Clojure file validation failed:" file-path)
          false)))
    (catch Exception e
      (println "  ❌ Clojure validation error:" file-path "-" (.getMessage e))
      false)))

(defn validate-clojurescript-file [file-path]
  "Validate a .cljs file by compiling with shadow-cljs"
  (try
    (println "  🔍 Validating ClojureScript file:" file-path)
    ;; Try to compile just this file with shadow-cljs
    (let [{:keys [exit out err]} (process/sh ["npx" "shadow-cljs" "compile" "--file" file-path]
                                             {:out :string :err :string})]
      (if (= 0 exit)
        (do
          (println "  ✅ ClojureScript file valid:" file-path)
          true)
        (do
          (println "  ❌ ClojureScript file validation failed:" file-path)
          (println "  Error output:" err)
          false)))
    (catch Exception e
      (println "  ❌ ClojureScript validation error:" file-path "-" (.getMessage e))
      false)))

(defn validate-file-with-kondo [file-path]
  "Validate a file with clj-kondo after changes"
  (try
    (println "  🔍 Running clj-kondo check on:" file-path)
    (let [{:keys [exit out err]} (process/sh ["clj-kondo" "--cache" "false" "--lint" file-path]
                                             {:out :string :err :string})]
      ;; Count errors and warnings
      (let [output (str out err)
            error-count (count (re-seq #"error:" output))
            warning-count (count (re-seq #"warning:" output))]
        (println (str "  📊 clj-kondo results: " error-count " errors, " warning-count " warnings"))
        ;; Allow warnings but not errors
        (= 0 error-count)))
    (catch Exception e
      (println "  ❌ clj-kondo validation error:" file-path "-" (.getMessage e))
      false)))

(defn safe-process-file [file entries]
  "Process a single file with comprehensive validation"
  (let [path (fs/file file)]
    (if-not (fs/exists? path)
      (binding [*out* *err*]
        (println "Skipping" file "(not found)"))
      (let [file-type (get-file-type file)]
        (when (= file-type :unknown)
          (println "  ⚠️  Unknown file type, skipping:" file)
          (println))

        (println "\n🔄 Processing file:" file " (type:" file-type ")")

        ;; Create backup
        (let [backup-path (str file ".backup." (System/currentTimeMillis))]
          (fs/copy path backup-path)
          (println "  💾 Created backup:" backup-path)

          ;; Track if any changes were made
          (let [original-content (slurp file)
                had-changes? (atom false)]

            ;; Apply the original logic for unused bindings
            (when (some #(= :unused-binding (:type %)) entries)
              (println "  🔧 Applying unused binding fixes...")
              (apply-warnings-to-file file entries)
              (reset! had-changes? true))

            ;; Apply clean-ns for namespace issues if needed
            (when (some #(= :clean-ns (:type %)) entries)
              (println "  🧹 Applying clojure-lsp clean-ns...")
              (run-clean-ns [file])
              (reset! had-changes? true))

            ;; Only validate if changes were made
            (when @had-changes?
              ;; Step 1: Basic syntax check with kondo
              (if (not (validate-file-with-kondo file))
                (do
                  (println "  ❌ clj-kondo validation failed - reverting changes")
                  (fs/copy backup-path path)
                  (fs/deleteIfExists backup-path)
                  false)

                ;; Step 2: Compiler-specific validation
                (let [compiler-valid? (case file-type
                                        :clojure (validate-clojure-file file)
                                        :clojurescript (validate-clojurescript-file file)
                                        true)]  ; For other types, just trust kondo

                  (if (not compiler-valid?)
                    (do
                      (println "  ❌ Compiler validation failed - reverting changes")
                      (fs/copy backup-path path)
                      (fs/deleteIfExists backup-path)
                      false)
                    (do
                      (println "  ✅ File passed all validations - keeping changes")
                      (fs/deleteIfExists backup-path)
                      true)))))))))))

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

;; Main execution - process files one by one with validation
(let [{:keys [out err exit]} (process/sh lint-command {:out :string :err :string :check false})
      output (str out (when (seq err) err)]
  (print output)
  (flush)
  (let [warnings (->> (str/split-lines output)
                      (keep parse-warning))
        unused-binding-warnings (filter #(= :unused-binding (:type %)) warnings)
        clean-ns-warnings (filter #(= :clean-ns (:type %)) warnings)]

    (println "\n🛡️  Starting ultra-safe processing with individual file validation")

    ;; Process unused bindings one file at a time
    (let [files-with-unused-bindings (group-by :file unused-binding-warnings)]
      (doseq [[file entries] files-with-unused-bindings]
        (safe-process-file file entries)))

    ;; Process clean-ns files one at a time
    (let [clean-ns-files (->> clean-ns-warnings
                              (map :file)
                              distinct
                              vec)]
      (doseq [file clean-ns-files]
        (safe-process-file file clean-ns-warnings)))

    (println "\n🎉 Processing complete!")
    (System/exit exit)))