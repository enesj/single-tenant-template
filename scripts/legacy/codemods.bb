#!/usr/bin/env bb
;; Legacy Codemods (Babashka)
;; Purpose: mechanical, reversible rewrites to remove legacy patterns.
;; Status: scaffold — implement ops incrementally. No Python; Babashka only.
;;
;; Usage examples:
;;   bb scripts/legacy/codemods.bb --op namespaced-key-fallback --dry-run
;;   bb scripts/legacy/codemods.bb --op remove-reexports
;;   bb scripts/legacy/codemods.bb --op aliases-to-service --targets src:test --dry-run
;;
(ns scripts.legacy.codemods
  (:require [babashka.fs :as fs]
    [clojure.string :as str]
    [clojure.edn :as edn]
    [clojure.java.shell :as sh]))

(defn parse-args [args]
  (loop [m {:targets ["src" "test"] :dry-run false}
         xs args]
    (if-let [a (first xs)]
      (cond
        (or (= a "--op") (= a "-o")) (recur (assoc m :op (second xs)) (nnext xs))
        (= a "--targets")               (recur (assoc m :targets (str/split (second xs) #":")) (nnext xs))
        (= a "--dry-run")               (recur (assoc m :dry-run true) (next xs))
        :else                            (recur m (next xs)))
      (cond-> m
        (nil? (:op m)) (assoc :op "help")))))

(defn rg* [& args]
  (let [{:keys [exit out err]} (apply sh/sh "rg" args)]
    {:exit exit :out out :err err}))

(defmulti run-op (fn [{:keys [op]}] (keyword op)))

(defmethod run-op :help [m]
  (println "Codemods ops:\n"
    "  namespaced-key-fallback  -> replace (or (:x_y r) (:x/y r)) with (:x/y r)\n"
    "  remove-reexports         -> delete re-export ns stubs\n"
    "  aliases-to-service       -> replace alias fns with service-map calls\n")
  (println "Flags:\n  --targets src:test  --dry-run")
  0)

(defn preview-changes [label diff-lines]
  (println (str "=== " label " ==="))
  (doseq [l diff-lines] (println l))
  (println "=== end ==="))

(defmethod run-op :namespaced-key-fallback [{:keys [targets dry-run]}]
  ;; Preview: find likely sites
  (let [{:keys [exit out]} (apply rg* "-n" "-S" "--color" "never" "--no-heading"
                             "\\(or\\s+\\(:[A-Za-z0-9_-]+_[A-Za-z0-9_-]+" targets)]
    (when (pos? (count out))
      (preview-changes "namespaced-key-fallback candidates" (str/split-lines out)))
    (if dry-run
      0
      (do
        (println "NOTE: transformation not yet implemented — scaffold only.")
        1))))

(defmethod run-op :remove-reexports [{:keys [targets dry-run]}]
  (let [{:keys [out]} (apply rg* "-n" "--pcre2" "--no-heading"
                        "^\\(ns\\s+[^\\s\\)]+\\s+.*\\(:refer\\s+:all\\)" targets)]
    (when (pos? (count out))
      (preview-changes "re-export ns candidates" (str/split-lines out)))
    (if dry-run
      0
      (do (println "NOTE: auto-removal not yet implemented — scaffold only.") 1))))

(defmethod run-op :aliases-to-service [{:keys [targets dry-run]}]
  (let [{:keys [out]} (apply rg* "-n" "--pcre2" "--no-heading"
                        "(?m)^\\s*\\(defn\\s+(get-[A-Za-z0-9!-]+|create-[A-Za-z0-9!-]+)\\b" targets)]
    (when (pos? (count out))
      (preview-changes "alias function candidates" (str/split-lines out)))
    (if dry-run
      0
      (do (println "NOTE: transformation not yet implemented — scaffold only.") 1))))

(defn -main [& args]
  (System/exit (run-op (parse-args (vec args)))))

(apply -main *command-line-args*)
