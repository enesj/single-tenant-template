#!/usr/bin/env clj

(ns privacy-subject-backfill
  (:require
    [app.template.backend.migrations.simple-repl :as mig]
    [app.template.backend.security.privacy-subject-backfill :as backfill]
    [clojure.pprint :as pprint]
    [clojure.string :as str]
    [next.jdbc :as jdbc]))

(defn- usage
  ([] (usage nil))
  ([msg]
   (when msg
     (binding [*out* *err*]
       (println msg)
       (println)))
   (println "Backfill privacy subject refs for operational expense/receipt ownership.")
   (println)
   (println "Usage:")
   (println "  bb privacy-subject-backfill [dev|test|prod] [options]")
   (println)
   (println "Options:")
   (println "  --apply       Perform writes. Default is dry-run.")
   (println "  --cutover     Also null direct users.id links after subject refs exist.")
   (println "  --limit N     Process at most N candidates per table.")
   (println "  --yes         Skip confirmation for --apply.")
   (println "  --pretty      Pretty-print the EDN result.")
   (println "  --help        Show this help.")
   (println)
   (println "Safety:")
   (println "  - Dry-run is the default.")
   (println "  - Subject refs are computed in application code with PRIVACY_SUBJECT_KEY_B64.")
   (println "  - For prod-like targets, configure a stable PRIVACY_SUBJECT_KEY_B64 before applying.")
   (println "  - Use --cutover only after reviewing a dry-run report.")))

(defn- parse-long!
  [label s]
  (try
    (Long/parseLong (str s))
    (catch Exception _
      (throw (ex-info (str "Invalid value for " label)
               {:label label
                :value s})))))

(defn- normalize-profile
  [s]
  (let [profile (keyword (str s))]
    (when-not (contains? #{:dev :test :prod} profile)
      (throw (ex-info "Profile must be one of dev, test, or prod"
               {:value s})))
    profile))

(defn- parse-args
  [args]
  (loop [args args
         parsed {:profile :dev
                 :apply? false
                 :cutover? false
                 :yes? false
                 :pretty? false}]
    (let [[a b & more] args]
      (cond
        (nil? a)
        parsed

        (or (= a "--help") (= a "-h"))
        (do
          (usage)
          (System/exit 0))

        (contains? #{"dev" "test" "prod"} a)
        (recur (cons b more) (assoc parsed :profile (normalize-profile a)))

        (= a "--apply")
        (recur (cons b more) (assoc parsed :apply? true))

        (= a "--cutover")
        (recur (cons b more) (assoc parsed :cutover? true))

        (or (= a "--yes") (= a "--force"))
        (recur (cons b more) (assoc parsed :yes? true))

        (= a "--pretty")
        (recur (cons b more) (assoc parsed :pretty? true))

        (= a "--limit")
        (do
          (when-not b
            (throw (ex-info "Missing value for --limit" {})))
          (recur more (assoc parsed :limit (parse-long! "--limit" b))))

        :else
        (throw (ex-info (str "Unknown arg: " a) {:arg a}))))))

(defn- datasource-for-profile
  [profile]
  (jdbc/get-datasource {:jdbcUrl (mig/get-jdbc-url profile)}))

(defn- confirm!
  [{:keys [profile cutover?]}]
  (println "⚠️  This will update operational expense/receipt ownership rows.")
  (println (str "🎯 Target profile: " (name profile)))
  (println (str "🔐 Cutover direct users.id links: " (boolean cutover?)))
  (println)
  (print "Type 'BACKFILL PRIVACY SUBJECTS' to confirm: ")
  (flush)
  (= "BACKFILL PRIVACY SUBJECTS" (str/trim (read-line))))

(defn- print-result!
  [{:keys [pretty?]} result]
  (if pretty?
    (pprint/pprint result)
    (prn result))
  (flush))

(defn -main
  [& args]
  (try
    (let [{:keys [profile apply? cutover? yes? limit] :as opts} (parse-args args)
          _ (System/setProperty "app.environment" (name profile))
          db (datasource-for-profile profile)
          dry-run? (not apply?)]
      (println (str "Running privacy-subject-backfill"
                 " profile=" (name profile)
                 " dry-run=" dry-run?
                 " cutover=" (boolean cutover?)
                 " limit=" (or limit "none")))
      (when (and apply? (not (or yes? (confirm! opts))))
        (println "❌ Cancelled.")
        (System/exit 1))
      (let [result (backfill/backfill-privacy-subjects!
                     db
                     (cond-> {:dry-run? dry-run?
                              :cutover? cutover?}
                       limit (assoc :limit limit)))]
        (print-result! opts result)))
    (catch clojure.lang.ExceptionInfo e
      (binding [*out* *err*]
        (usage (or (.getMessage e) "privacy-subject-backfill failed")))
      (System/exit 1))
    (catch Exception e
      (binding [*out* *err*]
        (println "privacy-subject-backfill failed:")
        (println (.getMessage e)))
      (System/exit 1))))

(apply -main *command-line-args*)
