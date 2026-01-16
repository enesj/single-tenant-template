#!/usr/bin/env bb
;; Legacy Inventory Scanner (Babashka)
;; Purpose: scan src/ and test/ for known legacy patterns and emit EDN.
;; Usage:
;;   bb scripts/legacy/inventory.bb            ; prints EDN to stdout
;;   OUT=resources/legacy-inventory.edn bb scripts/legacy/inventory.bb
;;   bb scripts/legacy/inventory.bb --out resources/legacy-inventory.edn

(ns scripts.legacy.inventory
  (:require
   [clojure.string :as str]
   [clojure.java.shell :as sh]
   [babashka.fs :as fs]))

(defn parse-args [args]
  (let [env-out (System/getenv "OUT")]
    (loop [m {:quiet false} xs args]
      (if-let [a (first xs)]
        (cond
          (or (= a "--out") (= a "-o")) (recur (assoc m :out (second xs)) (nnext xs))
          (or (= a "--quiet") (= a "-q")) (recur (assoc m :quiet true) (next xs))
          :else (recur m (next xs)))
        (cond-> m
          (and (nil? (:out m)) env-out) (assoc :out env-out))))))

(def categories
  {
   :namespaced-key-fallback ["\\(or\\s+\\(:[A-Za-z0-9_-]+_[A-Za-z0-9_-]+" "(get\\s+\\{:[A-Za-z0-9_-]+_[A-Za-z0-9_-]+"]
   :re-export-namespaces    ["^\\(ns\\s+[^\\s\\)]+\\s+.*\\(:refer\\s+:all\\)"]
   :service-map-alias-vars  ["(?m)^\\s*\\(defn\\s+(get-[A-Za-z0-9!-]+|create-[A-Za-z0-9!-]+)\\b"]
  :legacy-password         ["(?i)password.*sha-?256" "(?i)sha-?256.*password"]
   :oauth-compat            ["(?i)oauth" "legacy.*oauth" "token-format"]
   :legacy-events           ["reg-event-db" "reg-event-fx"]
   :legacy-subs             ["(?m)^\\s*re-frame.core/reg-sub|(?m)^\\s*re-frame/reg-sub|(?m)reg-sub\\s"]
   :api-response-compat     ["compat.*response|dual.*key|legacy.*response"]
  ;; Match concrete legacy route compatibility (admin settings URLs).
  ;; Intentionally does NOT match generic redirects like OAuth.
  :legacy-route-redirects  ["\"/admin/settings\"" "\"/admin/amin-settings\"" "\"/amin-settings\""]
   :localstorage-migration  ["localStorage|LocalStorage|local-storage.*migrat"]
  ;; Match underscore-key fallback handling in settings handlers (a concrete legacy compatibility pattern).
  :settings-schema-legacy  ["\\(:entity_name\\b" "\\(:setting_key\\b" "\\(:setting_value\\b"]
   :domain-registry-compat  ["domain.*registry.*(legacy|compat)"]
   :component-template-compat ["compatibility.*component|template.*compat"]
   })

(def file-globs
  { :empty-deprecated-namespaces ["**/admin/frontend/pages/entities.cljs"] })

(defn rg* [& args]
  (let [{:keys [exit out err]} (apply sh/sh "rg" args)]
    {:exit exit :out out :err err}))

(defn parse-rg-lines [s]
  ;; rg -n yields: path:line:match
  (->> (str/split-lines (or s ""))
       (keep (fn [line]
               (let [[file ln match] (str/split line #":" 3)]
                 (when (and file ln match)
                   {:file file
                    :line (try (Long/parseLong ln) (catch Exception _ 0))
                    :match (str/trim match)
                    ;; Canonical field name used by the plan/docs. Kept in sync
                    ;; with :match so the audit key remains stable.
                    :code (str/trim match)}))))))

(defn run-pattern [category pattern]
  (let [{:keys [exit out]} (rg* "-n" "-S" "--color" "never" "--no-heading" pattern "src" "test")
        rows (when (zero? exit) (parse-rg-lines out))]
    (map (fn [m] (assoc m :category category :pattern pattern)) rows)))

(defn run-file-glob [category globs]
  (let [{:keys [exit out]} (apply rg* "--files" (concat ["src" "test"] (map #(str "-g" "=" %) globs)))
        files (when (zero? exit) (str/split-lines out))]
    (map (fn [f]
       {:category category
    :file f
    :line 0
    :match "<file>"
    :code "<file>"
    :pattern (str (str/join "," globs))})
  files)))

(defn inventory []
  (let [content-matches (for [[cat pats] categories
                              pat pats
                              m (run-pattern cat pat)] m)
        file-matches    (for [[cat globs] file-globs
                              m (run-file-glob cat globs)] m)
        all             (vec (concat content-matches file-matches))]
    all))

(defn -main [& args]
  (let [{:keys [out quiet]} (parse-args (vec args))
        inv (inventory)
        edn-str (with-out-str (prn inv))]
    ;; Default behavior: print EDN to stdout (used by audit tooling).
    ;; When generating a baseline file, callers can pass --quiet to avoid
    ;; streaming thousands of lines to the terminal.
    (when (or (nil? out) (not quiet))
      (println edn-str))
    (when out
      (fs/create-dirs (fs/parent out))
  ;; Keep output as pure EDN (no leading comments) so other tools can parse it via clojure.edn.
  (spit out edn-str))))

(apply -main *command-line-args*)

