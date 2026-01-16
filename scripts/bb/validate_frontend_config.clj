#!/usr/bin/env bb

(ns validate-frontend-config
  (:require
    [app.shared.frontend-config.discovery :as discovery]
    [app.shared.frontend-config.schema :as schema]
    [app.shared.frontend-config.validation :as validation]))

(defn- die!
  [msg]
  (binding [*out* *err*]
    (println msg))
  (System/exit 2))

(defn- parse-args
  [args]
  (loop [args args
         opts {:only []
               :skip []
               :allowlist-path nil
               :schema-path nil}]
    (if (empty? args)
      opts
      (let [[a & more] args]
        (case a
          "--only" (let [[d & more2] more]
                     (when-not d
                       (die! "--only requires a domain name"))
                     (recur more2 (update opts :only conj d)))
          "--skip" (let [[d & more2] more]
                     (when-not d
                       (die! "--skip requires a domain name"))
                     (recur more2 (update opts :skip conj d)))
          "--allowlist" (let [[p & more2] more]
                          (when-not p
                            (die! "--allowlist requires a path"))
                          (recur more2 (assoc opts :allowlist-path p)))
          "--schema" (let [[p & more2] more]
                       (when-not p
                         (die! "--schema requires a path"))
                       (recur more2 (assoc opts :schema-path p)))
          (die! (str "Unknown arg: " a)))))))

(defn- result-label
  [{:keys [scope domain kind]}]
  (str (name scope)
    (when domain (str "/" domain))
    " "
    (name kind)
    ".edn"))

(defn- print-semantic!
  [{:keys [semantic]}]
  (when (seq (:unknown-entities semantic))
    (println "  unknown entities:" (pr-str (:unknown-entities semantic))))
  (when (seq (:unknown-fields semantic))
    (println "  unknown fields:" (pr-str (:unknown-fields semantic))))
  (when (seq (:missing-fields semantic))
    (println "  missing DB fields (info):" (pr-str (:missing-fields semantic)))))

(defn- print-result!
  [{:keys [label path valid? errors warnings semantic] :as result}]
  (let [label* (or label (result-label result))]
    (if valid?
      (println "✓" label* "valid" (str "(" path ")"))
      (do
        (println "✗" label* "INVALID" (str "(" path ")"))
        (when (seq errors)
          (println "  errors:" (pr-str errors)))
        (when (seq warnings)
          (println "  warnings:" (pr-str warnings)))))
    (print-semantic! {:semantic semantic})))

(defn -main
  [& args]
  (println "=== Validating frontend config EDNs ===")
  (let [{:keys [only skip allowlist-path schema-path]} (parse-args args)
        schema-index (schema/models-index (or schema-path "resources/db/models.edn"))
        bundles (discovery/config-bundles {:only only :skip skip})]
    (when (empty? bundles)
      (die! "No frontend config bundles found"))
    (let [bundles* (discovery/load-bundles bundles)
          allowlist (when allowlist-path (discovery/read-edn-file allowlist-path))
          results (validation/validate-bundles bundles* schema-index allowlist)
          _ (doseq [res results]
              (print-result! (assoc res :label (result-label res))))
          invalid (filter (comp not :valid?) results)]
      (if (seq invalid)
        (die! (str "\nConfig validation failed (" (count invalid) " invalid file(s))."))
        (println "\n✅ All frontend config EDNs are valid.")))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
