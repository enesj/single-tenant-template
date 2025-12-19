#!/usr/bin/env bb

(ns sync-frontend-config
  (:require
    [app.shared.frontend-config.core :as fc]
    [rewrite-clj.zip :as z]))

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
               :schema-path nil
               :apply? false}]
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
          "--apply" (recur more (assoc opts :apply? true))
          (die! (str "Unknown arg: " a)))))))

(defn- result-label
  [{:keys [scope domain kind]}]
  (str (name scope)
    (when domain (str "/" domain))
    " "
    (name kind)
    ".edn"))

(defn- print-summary!
  [{:keys [path summary unknown-entities has-changes?] :as patch}]
  (let [label (result-label patch)]
    (if has-changes?
      (println "✗" label (str "(" path ")"))
      (println "✓" label "no changes" (str "(" path ")")))
    (when (seq unknown-entities)
      (println "  unknown entities:" (pr-str unknown-entities)))
    (doseq [[entity info] (sort-by (comp str key) summary)]
      (println "  entity" (pr-str entity))
      (when (seq (:add-available info))
        (println "    add to :available-columns:" (pr-str (:add-available info))))
      (when (seq (:removed info))
        (println "    quarantine fields:" (pr-str (:removed info)))))))

(defn- update-entity
  [zloc entity updates]
  (if-let [entity-loc (z/find-value zloc z/next entity)]
    (let [value-loc (z/right entity-loc)
          updated-loc (reduce (fn [loc [k v]]
                                (z/assoc loc k v))
                        value-loc
                        updates)
          root-node (z/root updated-loc)]
      (z/of-node root-node))
    zloc))

(defn- apply-file-updates!
  [path updates-by-entity]
  (when (seq updates-by-entity)
    (let [content (slurp path)
          zloc (z/of-string content)
          zloc* (reduce (fn [loc [entity updates]]
                          (update-entity loc entity updates))
                  zloc
                  updates-by-entity)
          new-content (z/root-string zloc*)]
      (spit path new-content))))

(defn -main
  [& args]
  (println "=== Sync frontend config (dry-run by default) ===")
  (let [{:keys [only skip allowlist-path schema-path apply?]} (parse-args args)
        schema (fc/models-index (or schema-path "resources/db/models.edn"))
        bundles (fc/config-bundles {:only only :skip skip})]
    (when (empty? bundles)
      (die! "No frontend config bundles found"))
    (let [bundles* (fc/load-bundles bundles)
          allowlist (when allowlist-path (fc/read-edn-file allowlist-path))
          patches (fc/plan-sync bundles* schema allowlist)
          changes? (some :has-changes? patches)
          errors? (some (comp seq :unknown-entities) patches)
          _ (doseq [patch patches]
              (print-summary! patch))]
      (cond
        errors?
        (die! "\nSync plan found unknown entities. Fix config or schema alignment first.")

        (and (not apply?) changes?)
        (die! "\nSync plan has changes. Re-run with --apply to write updates.")

        (and apply? changes?)
        (do
          (doseq [{:keys [path updates]} patches
                  :when (seq updates)]
            (apply-file-updates! path updates))
          (println "\n✅ Applied sync updates."))

        (and apply? (not changes?))
        (println "\n✅ No changes needed.")

        :else
        (println "\n✅ No changes needed.")))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
