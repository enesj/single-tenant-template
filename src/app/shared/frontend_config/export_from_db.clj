(ns app.shared.frontend-config.export-from-db
  "Promote DB-backed frontend config snapshots into source-controlled EDN defaults.

  Dry-run by default. Use --apply to write changes."
  (:require
    [app.shared.frontend-config.discovery :as discovery]
    [app.shared.frontend-config.export :as export]
    [app.shared.frontend-config.io :as frontend-config-io]
    [app.template.backend.migrations.simple-repl :as simple-repl]
    [app.template.backend.routes.admin.settings-io :as settings-io]
    [clojure.string :as str]
    [next.jdbc :as jdbc]))

(defn- die!
  ([msg] (die! msg 2))
  ([msg code]
   (binding [*out* *err*]
     (println msg))
   (System/exit code)))

(defn- require-arg
  [flag value]
  (when (or (nil? value) (str/blank? value))
    (die! (str flag " requires a value"))))

(defn- safe-profile
  [s]
  (let [s (str/trim s)]
    (when-not (re-matches #"[A-Za-z0-9_-]+" s)
      (die! (str "Invalid profile: " s)))
    (keyword s)))

(defn- parse-args
  [args]
  (loop [args args
         opts {:profile :dev
               :only []
               :skip []
               :apply? false}]
    (if (empty? args)
      opts
      (let [[a & more] args]
        (case a
          "--profile" (let [[v & more2] more]
                        (require-arg "--profile" v)
                        (recur more2 (assoc opts :profile (safe-profile v))))
          "--only" (let [[v & more2] more]
                     (require-arg "--only" v)
                     (recur more2 (update opts :only conj v)))
          "--skip" (let [[v & more2] more]
                     (require-arg "--skip" v)
                     (recur more2 (update opts :skip conj v)))
          "--apply" (recur more (assoc opts :apply? true))
          (die! (str "Unknown arg: " a)))))))

(defn- result-label
  [{:keys [scope domain kind]}]
  (str (name scope)
    (when domain (str "/" domain))
    " "
    (name kind)
    ".edn"))

(defn- current-file-data
  [path]
  (frontend-config-io/read-edn-or-empty
    path
    {:log-message "Failed to read current EDN during export"
     :log-context {:path path}}))

(defn- runtime-config
  [db]
  {:admin {:view-options (settings-io/read-view-options db)
           :form-fields (settings-io/read-form-fields db)
           :table-columns (settings-io/read-table-columns db)}
   :user {:entities (settings-io/read-user-entities db)
          :view-options (settings-io/read-user-view-options db)
          :form-fields (settings-io/read-user-form-fields db)
          :table-columns (settings-io/read-user-table-columns db)}})

(defn- decorate-plan
  [plan]
  (mapv (fn [{:keys [path data validation] :as item}]
          (let [current (current-file-data path)]
            (assoc item
              :current current
              :changed? (not= current data)
              :valid? (:valid? validation))))
    plan))

(defn- print-plan!
  [plan]
  (doseq [{:keys [path changed? valid? validation] :as item} plan]
    (println (if changed? "✗" "✓")
      (result-label item)
      (if changed? "would update" "no changes")
      (str "(" path ")"))
    (when-not valid?
      (println "  invalid export data:" (pr-str (dissoc validation :valid?))))))

(defn- apply-plan!
  [plan]
  (doseq [{:keys [path data changed?]} plan
          :when changed?]
    (frontend-config-io/write-edn-pretty! path data)
    (println "  wrote" path)))

(defn -main
  [& args]
  (println "=== Export frontend config from DB (dry-run by default) ===")
  (let [{:keys [profile only skip apply?]} (parse-args args)
        ds (jdbc/get-datasource {:jdbcUrl (simple-repl/get-jdbc-url profile)})
        bundles (-> (discovery/config-bundles {:only only :skip skip})
                  discovery/load-bundles)
        plan (-> (export/export-plan bundles (runtime-config ds))
               decorate-plan)
        invalid (filter (comp not :valid?) plan)
        changed (filter :changed? plan)]
    (when (empty? bundles)
      (die! "No frontend config bundles found"))
    (print-plan! plan)
    (when (seq invalid)
      (die! (str "\nExport blocked: " (count invalid) " invalid target(s).")))
    (cond
      (and apply? (seq changed))
      (do
        (println "\n=== Applying export ===")
        (apply-plan! plan)
        (println "\n✅ Export applied."))

      (and apply? (empty? changed))
      (println "\n✅ No changes needed.")

      (seq changed)
      (die! "\nExport plan has changes. Re-run with --apply to write files.")

      :else
      (println "\n✅ No changes needed."))))
