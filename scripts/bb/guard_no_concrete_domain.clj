#!/usr/bin/env bb
;; Guard script to prevent concrete domain coupling in template/admin/shared
;;
;; This script fails if template/admin/shared contain concrete domain references
;; (e.g., "expenses", "expense", ":expenses" patterns).
;;
;; Usage:
;;   bb scripts/bb/guard_no_concrete_domain.clj
;;   bb guard-domain-coupling  ; if task is wired in bb.edn

(ns guard-no-concrete-domain
  (:require
    [babashka.fs :as fs]
    [clojure.java.io :as io]
    [clojure.string :as str]))

(def ^:private source-dirs
  "Directories to check for concrete domain coupling"
  ["src/app/template"
   "src/app/admin"
   "src/app/shared"])

(def ^:private file-extensions
  "File extensions to check"
  #{".clj" ".cljs" ".cljc" ".edn"})

(def ^:private concrete-domain-patterns
  "Patterns that indicate concrete domain coupling.
   Each pattern has a regex and description."
  [{:pattern #"app\.domain\.\w+\.expenses"
    :description "Direct require of domain expenses namespace"
    :severity :error}
   {:pattern #":expenses(?!\-)"
    :description "Keyword :expenses (entity or config key)"
    :severity :warning}
   {:pattern #":expense/"
    :description "Namespaced keyword with expense prefix"
    :severity :warning}
   {:pattern #":receipts(?!\-)"
    :description "Keyword :receipts (expenses domain entity)"
    :severity :warning}
   {:pattern #":suppliers(?!\-)"
    :description "Keyword :suppliers (expenses domain entity)"
    :severity :warning}
   {:pattern #":payers(?!\-)"
    :description "Keyword :payers (expenses domain entity)"
    :severity :warning}
   {:pattern #":articles(?!\-)"
    :description "Keyword :articles (expenses domain entity)"
    :severity :warning}
   {:pattern #":article-aliases"
    :description "Keyword :article-aliases (expenses domain entity)"
    :severity :warning}
   {:pattern #":price-observations"
    :description "Keyword :price-observations (expenses domain entity)"
    :severity :warning}])

(def ^:private allowlist
  "Patterns to allow (false positives from legitimate uses)"
  [;; Domain registry imports are allowed
   #"app\.domain\.frontend\.registry"
   #"app\.domain\.backend\.registry"
   ;; Generic component that accepts any entity
   #"entity-key"
   #"entity-name"
   ;; Test files
   #"_test\.clj"
   ;; Comments and documentation
   #"^\s*;;"
   #"^\s*\""
   ;; Enumeration/dispatch cases (like :expenses-admin or :expenses-user)
   #":expenses-admin"
   #":expenses-user"
   ;; Domain groups in template (legitimate - they come from registry)
   #":admin-domain-groups"
   #":user-domain-groups"])

(def ^:private bootstrap-files
  "Files that are allowed to directly require domain namespaces.
   These are bootstrap/composition points that wire up the domain."
  #{"src/app/admin/frontend/routes.cljs"
    "src/app/admin/frontend/config/preload.cljs"})

(defn- should-skip-file?
  "Check if a file should be skipped based on path patterns."
  [file-path]
  (or
    ;; Skip test files
    (str/includes? file-path "_test.clj")
    ;; Skip generated files
    (str/includes? file-path "/target/")
    ;; Skip bootstrap files that legitimately wire up domain
    (some #(str/ends-with? file-path %) bootstrap-files)))

(defn- is-allowlisted?
  "Check if a line matches any allowlist pattern."
  [line]
  (some #(re-find % line) allowlist))

(defn- check-file
  "Check a single file for concrete domain patterns.
   Returns a vector of violations."
  [file-path]
  (when-not (should-skip-file? file-path)
    (try
      (let [content (slurp file-path)
            lines (str/split-lines content)]
        (for [[line-num line] (map-indexed #(vector (inc %1) %2) lines)
              {:keys [pattern description severity]} concrete-domain-patterns
              :when (and (re-find pattern line)
                      (not (is-allowlisted? line)))]
          {:file file-path
           :line line-num
           :pattern (str pattern)
           :description description
           :severity severity
           :content (str/trim line)}))
      (catch Exception e
        (println "Warning: Could not read file" file-path (.getMessage e))
        nil))))

(defn- get-clojure-files
  "Get all Clojure/ClojureScript files in a directory recursively."
  [dir]
  (when (fs/exists? dir)
    (->> (file-seq (io/file dir))
      (filter #(.isFile %))
      (map str)
      (filter #(some (fn [ext] (str/ends-with? % ext)) file-extensions)))))

(defn- format-violation
  "Format a single violation for output."
  [{:keys [file line description content severity]}]
  (format "[%s] %s:%d - %s\n    %s"
    (name severity)
    file
    line
    description
    (subs content 0 (min 80 (count content)))))

(defn run-guard
  "Run the guard check and return results."
  []
  (let [all-files (mapcat get-clojure-files source-dirs)
        violations (mapcat check-file all-files)
        errors (filter #(= :error (:severity %)) violations)
        warnings (filter #(= :warning (:severity %)) violations)]
    {:errors errors
     :warnings warnings
     :total-files (count all-files)}))

(defn -main
  "Main entry point for the guard script."
  [& args]
  (let [strict? (some #{"--strict"} args)
        {:keys [errors warnings total-files]} (run-guard)]
    (println (format "Checked %d files in template/admin/shared" total-files))
    (println)

    (when (seq warnings)
      (println (format "Found %d warning(s):" (count warnings)))
      (doseq [v warnings]
        (println (format-violation v)))
      (println))

    (when (seq errors)
      (println (format "Found %d error(s):" (count errors)))
      (doseq [v errors]
        (println (format-violation v)))
      (println))

    (cond
      (seq errors)
      (do
        (println "FAILED: Concrete domain coupling detected (errors)")
        (System/exit 1))

      (and strict? (seq warnings))
      (do
        (println "FAILED: Concrete domain coupling detected (strict mode)")
        (System/exit 1))

      (seq warnings)
      (do
        (println "PASSED with warnings")
        (System/exit 0))

      :else
      (do
        (println "PASSED: No concrete domain coupling detected")
        (System/exit 0)))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
