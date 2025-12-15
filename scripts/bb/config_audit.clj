#!/usr/bin/env bb

;; Lightweight audit for admin/domain EDN config keys.
;;
;; Goal: make it harder for "looks-configurable but isn't" keys to accumulate.
;;
;; What it does:
;; - Finds EDN config files under src/app/**/frontend/**/config/*.edn
;; - Extracts all nested map keys (as keyword/string key paths)
;; - Checks whether each key appears anywhere in code (src + test), excluding config EDNs
;;
;; Notes:
;; - This is intentionally grep-based and can have false positives/negatives.
;; - Use --strict if you want CI-style failure when some keys appear unused.
;;
;; Usage:
;;   bb scripts/bb/config_audit.clj
;;   bb scripts/bb/config_audit.clj --strict
;;   bb scripts/bb/config_audit.clj --allowlist scripts/bb/config_audit_allowlist.edn
;;
;; Allowlist format:
;; - EDN set of keywords/strings, e.g. #{:computed-fields :formatter "components"}
;; - or EDN map of key -> reason, e.g. {:computed-fields "metadata-only"}

(ns config-audit
  (:require
    [babashka.fs :as fs]
    [babashka.process :as bp]
    [clojure.edn :as edn]
    [clojure.set :as set]
    [clojure.string :as str]))

(defn- die!
  [msg]
  (binding [*out* *err*]
    (println msg))
  (System/exit 2))

(defn- parse-args
  [args]
  (loop [args args
         opts {:strict? false
               :allowlist-path nil}]
    (if (empty? args)
      opts
      (let [[a & more] args]
        (case a
          "--strict" (recur more (assoc opts :strict? true))
          "--allowlist" (let [[p & more2] more]
                           (when-not p
                             (die! "--allowlist requires a path"))
                           (recur more2 (assoc opts :allowlist-path p)))
          (die! (str "Unknown arg: " a)))))))

(defn- read-edn-file
  [path]
  (try
    (edn/read-string (slurp path))
    (catch Exception e
      (die! (str "Failed to read EDN: " path "\n" (.getMessage e))))))

(defn- config-edn-files
  "Return config EDN files we care about. Intentionally narrow." 
  []
  (->> (concat
         ;; Admin-style: src/app/admin/frontend/config/*.edn
         (fs/glob "src" "**/frontend/config/*.edn")
         ;; Domain-style: src/app/domain/frontend/<domain>/config/*.edn (and deeper)
         (fs/glob "src" "**/frontend/**/config/*.edn"))
       (map str)
       distinct
       sort))

(defn- normalize-key
  [k]
  (cond
    (keyword? k) k
    (string? k) k
    :else (pr-str k)))

;; Map keys whose values are maps keyed by *dynamic IDs* (field IDs / column IDs).
;;
;; We don't want to treat those dynamic IDs as configuration keys.
;;
;; Example:
;;   {:field-config {:email {:label "Email"}}}
;; Here :email is a field-id, not a config key; the config keys are :label, :type, ...
(def ^:private dynamic-id-keyed-maps
  #{:field-config :computed-fields :column-config})

(defn- extract-key-paths*
  [x path acc]
  (cond
    (map? x)
    (reduce-kv
      (fn [acc k v]
        (let [k* (normalize-key k)
              dynamic-parent? (contains? dynamic-id-keyed-maps (last path))]
          (if dynamic-parent?
            ;; Inside maps like :field-config, keys are dynamic IDs.
            ;; Recurse into the value WITHOUT recording the dynamic ID key.
            (extract-key-paths* v path acc)
            (let [next-path (conj path k*)]
              (extract-key-paths* v next-path (conj acc next-path))))))
      acc
      x)

    (sequential? x)
    (reduce (fn [acc v] (extract-key-paths* v path acc)) acc x)

    :else
    acc))

(defn- extract-key-paths
  [edn-value]
  (->> (extract-key-paths* edn-value [] #{})
       (remove empty?)
       set))

(defn- allowlisted?
  [allowlist k]
  (cond
    (nil? allowlist) false
    (set? allowlist) (contains? allowlist k)
    (map? allowlist) (contains? allowlist k)
    :else false))

(defn- load-allowlist
  [path]
  (when path
    (let [v (read-edn-file path)]
      (cond
        (set? v) v
        (map? v) v
        :else (die! (str "Allowlist must be a set or map, got: " (type v)))))))

(defn- regex-escape
  "Escape a literal string for Rust regex (ripgrep)."
  [s]
  (-> s
      (str/replace "\\" "\\\\")
  (str/replace #"[.\^$|?*+()\[\]{}]" (fn [m] (str "\\" m)))))

(defn- key->queries
  "Return ripgrep queries that likely represent this key in code.

  We intentionally include a destructuring probe because `{:keys [foo]}` does
  not contain `:foo` in source text, so a pure `:foo` search yields false
  unused results.
  "
  [k]
  (cond
    (keyword? k)
    (let [ns-part (namespace k)
          n (name k)
          n-re (regex-escape n)]
      (cond-> [{:pattern (str ":" n) :fixed? true}
               ;; Common destructuring form: (let [{:keys [foo bar]} m] ...)
               {:pattern (str ":keys\\s*\\[[^\\]]*" n-re) :fixed? false}]
        ns-part (conj {:pattern (str ":" ns-part "/" n) :fixed? true})))

    (string? k)
    [{:pattern (pr-str k) :fixed? true}]

    :else
    [{:pattern (pr-str k) :fixed? true}]))

(defn- rg-files-with-matches
  "Return a vector of file paths containing the needle.

  Uses ripgrep if available. Excludes config EDNs so matches represent consumers." 
  [{:keys [pattern fixed?]}]
  (let [base (cond-> ["rg" "--files-with-matches"]
               fixed? (conj "-F"))
        cmd (into base
              [pattern
               "src" "test"
               "--glob" "!**/frontend/**/config/*.edn"
               "--glob" "!**/node_modules/**"
               "--glob" "!**/target/**"
               "--glob" "!**/.shadow-cljs/**"
               "--glob" "!**/.git/**"])]
    (try
      (let [{:keys [exit out err]} @(bp/process cmd {:out :string :err :string})]
        (cond
          (= exit 0) (->> (str/split-lines out) (remove str/blank?) vec)
          (= exit 1) []
          :else (die! (str "ripgrep failed for pattern " (pr-str pattern) "\n" err))))
      (catch Exception e
        (die! (str "Failed to run ripgrep (rg). Is it installed and on PATH?\n"
                   "Error: " (.getMessage e)))))))

(defn- summarize!
  [{:keys [strict? allowlist-path]}]
  (let [allowlist (load-allowlist allowlist-path)
        files (config-edn-files)
        _ (when (empty? files)
            (die! "No config EDN files found under src/**/frontend/**/config/*.edn"))
        file->paths (into {}
                          (for [f files
                                :let [v (read-edn-file f)]]
                            [f (extract-key-paths v)]))
        all-paths (->> (vals file->paths) (apply set/union #{}))
        ;; We audit per-key (not per-path) because the consumer grep is key-based.
        all-keys (->> all-paths (map last) set)
        audited-keys (->> all-keys
                          (remove #(allowlisted? allowlist %))
                          sort)
        results (->> audited-keys
               (map (fn [k]
                  (let [queries (key->queries k)
                      matches (->> queries
                             (mapcat rg-files-with-matches)
                             set)
                      used? (seq matches)]
                    {:key k
                     :queries queries
                     :used? (boolean used?)
                     :match-files (->> matches sort vec)})))
               vec)
        unused (filter (comp not :used?) results)]

    (println "=== Config Audit ===")
    (println "Config files:")
    (doseq [f files]
      (println " -" f))
    (println)

    (println (str "Audited keys: " (count audited-keys)
                  (when allowlist-path (str " (allowlist: " allowlist-path ")"))))
    (println (str "Unused (grep-based): " (count unused)))
    (println)

    (when (seq unused)
      (println "Potentially unused keys (no matches in src/ + test/):")
      (doseq [{:keys [key queries]} unused]
        (println " -" (pr-str key) "queries:" (pr-str (map :pattern queries)))))

    (when strict?
      (when (seq unused)
        (binding [*out* *err*]
          (println (str "\n❌ Config audit failed: " (count unused) " keys appear unused")))
        (System/exit 1)))

    (println "\n✅ Config audit complete")))

(defn -main
  [& args]
  (summarize! (parse-args args)))

(apply -main *command-line-args*)
