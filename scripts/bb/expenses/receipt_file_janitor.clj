#!/usr/bin/env clj

(ns receipt-file-janitor
  (:require
    [app.domain.backend.expenses.services.receipts.janitor :as janitor]
    [app.template.backend.migrations.simple-repl :as mig]
    [clojure.pprint :as pprint]
    [next.jdbc :as jdbc]))

(def ^:private default-sleep-seconds
  (* 24 60 60))

(defn- usage
  ([] (usage nil))
  ([msg]
   (when msg
     (binding [*out* *err*]
       (println msg)
       (println)))
   (println "Purge finalized receipt files and delete orphaned files under upload/stripes.")
   (println)
   (println "Usage:")
   (println "  bb receipt-file-janitor [dev|test|prod] [options]")
   (println)
   (println "Options:")
   (println "  --dry-run                Show what would be deleted without deleting anything")
   (println "  --loop                   Run continuously instead of one-shot")
   (println "  --older-than-days N      Retention window for posted receipts (default 60)")
   (println "  --limit N                Max receipt purge candidates per pass (default 200)")
   (println "  --orphan-limit N         Max orphaned files to delete per pass (default 200)")
   (println "  --storage-base-dir PATH  Receipt storage directory (default upload/stripes)")
   (println "  --sleep-seconds N        Loop delay in seconds (default 86400)")
   (println "  --skip-orphans           Disable orphaned-file cleanup")
   (println "  --help                   Show this help")
   (println)
   (println "Examples:")
   (println "  bb receipt-file-janitor dev --dry-run")
   (println "  bb receipt-file-janitor dev --older-than-days 30 --limit 100")
   (println "  bb receipt-file-janitor prod --dry-run --skip-orphans")
   (println "  bb receipt-file-janitor prod --loop --sleep-seconds 86400")))

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
                 :loop? false
                 :dry-run? false
                 :delete-orphans? true
                 :older-than-days 60
                 :limit 200
                 :orphan-limit 200
                 :storage-base-dir "upload/stripes"
                 :sleep-seconds default-sleep-seconds}]
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

        (= a "--dry-run")
        (recur (cons b more) (assoc parsed :dry-run? true))

        (= a "--loop")
        (recur (cons b more) (assoc parsed :loop? true))

        (= a "--skip-orphans")
        (recur (cons b more) (assoc parsed :delete-orphans? false))

        (= a "--older-than-days")
        (do
          (when-not b
            (throw (ex-info "Missing value for --older-than-days" {})))
          (recur more (assoc parsed :older-than-days (parse-long! "--older-than-days" b))))

        (= a "--limit")
        (do
          (when-not b
            (throw (ex-info "Missing value for --limit" {})))
          (recur more (assoc parsed :limit (parse-long! "--limit" b))))

        (= a "--orphan-limit")
        (do
          (when-not b
            (throw (ex-info "Missing value for --orphan-limit" {})))
          (recur more (assoc parsed :orphan-limit (parse-long! "--orphan-limit" b))))

        (= a "--storage-base-dir")
        (do
          (when-not b
            (throw (ex-info "Missing value for --storage-base-dir" {})))
          (recur more (assoc parsed :storage-base-dir b)))

        (= a "--sleep-seconds")
        (do
          (when-not b
            (throw (ex-info "Missing value for --sleep-seconds" {})))
          (recur more (assoc parsed :sleep-seconds (parse-long! "--sleep-seconds" b))))

        :else
        (throw (ex-info (str "Unknown arg: " a) {:arg a}))))))

(defn- datasource-for-profile
  [profile]
  (jdbc/get-datasource {:jdbcUrl (mig/get-jdbc-url profile)}))

(defn- run-once!
  [db {:keys [profile] :as opts}]
  (println (str "Running receipt-file-janitor"
             " profile=" (name profile)
             " dry-run=" (:dry-run? opts)
             " older-than-days=" (:older-than-days opts)
             " limit=" (:limit opts)
             " orphan-limit=" (:orphan-limit opts)
             " delete-orphans?=" (:delete-orphans? opts)
             " storage-base-dir=" (:storage-base-dir opts)))
  (let [result (janitor/run-janitor! db (dissoc opts :profile :loop? :sleep-seconds))]
    (pprint/pprint result)
    (flush)
    result))

(defn- run-loop!
  [db {:keys [sleep-seconds] :as opts}]
  (loop []
    (run-once! db opts)
    (println (str "Sleeping for " sleep-seconds " seconds before next janitor pass..."))
    (flush)
    (Thread/sleep (* 1000 sleep-seconds))
    (recur)))

(defn -main
  [& args]
  (try
    (let [{:keys [profile loop?] :as opts} (parse-args args)
          db (datasource-for-profile profile)]
      (if loop?
        (run-loop! db opts)
        (run-once! db opts)))
    (catch clojure.lang.ExceptionInfo e
      (binding [*out* *err*]
        (usage (or (.getMessage e) "receipt-file-janitor failed")))
      (System/exit 1))
    (catch Exception e
      (binding [*out* *err*]
        (println "receipt-file-janitor failed:")
        (println (.getMessage e)))
      (System/exit 1))))

(apply -main *command-line-args*)