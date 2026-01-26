#!/usr/bin/env clj

(ns scripts.bb.expenses.import-manufacturers
  (:require
    [aero.core :as aero]
    [app.domain.backend.expenses.services.service-configs :as configs]
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.time Instant]
    [java.util UUID]))

(def ^:private min-normalized-length
  2)

(defn- usage []
  (println "Usage:")
  (println "  clj -M scripts/bb/expenses/import_manufacturers.clj [dev|test] [options]")
  (println "")
  (println "Options:")
  (println "  --file PATH   Default resources/import/manufacturers.edn")
  (println "  --apply       Apply changes (default is dry-run)")
  (println "  --help        Show this message"))

(defn- parse-args [args]
  (loop [args args
         parsed {:profile :dev
                 :file "resources/import/manufacturers.edn"
                 :apply? false}]
    (let [[a b & more] args]
      (cond
        (nil? a) parsed

        (#{"dev" "test"} a)
        (recur (cons b more) (assoc parsed :profile (keyword a)))

        (= a "--help")
        (do (usage) (System/exit 0))

        (= a "--apply")
        (recur (cons b more) (assoc parsed :apply? true))

        (= a "--file")
        (recur more (assoc parsed :file (some-> b str/trim not-empty)))

        :else
        (do
          (println "Unknown arg:" a)
          (usage)
          (System/exit 1))))))

(defn- datasource-from-config [config]
  (let [{:keys [host port dbname user password]} (:database config)]
    (jdbc/get-datasource {:dbtype "postgresql"
                          :host host
                          :port port
                          :dbname dbname
                          :user user
                          :password password})))

(defn- read-dataset [path]
  (let [f (io/file path)]
    (when-not (.exists f)
      (throw (ex-info "Dataset file does not exist" {:path path})))
    (with-open [r (io/reader f)]
      (edn/read {:eof nil} r))))

(defn- normalize-or-throw [label]
  (let [label* (some-> label str str/trim not-empty)
        normalized (when label* (configs/normalize-manufacturer-key label*))]
    (when (or (str/blank? label*)
            (str/blank? normalized)
            (< (count normalized) min-normalized-length))
      (throw (ex-info "Invalid manufacturer label (normalizes to blank/too short)"
               {:label label*
                :normalized normalized})))
    {:label label*
     :normalized normalized}))

(def ^:private upsert-manufacturer-sql
  "INSERT INTO manufacturers (id, display_name, normalized_key, created_at, updated_at)
   VALUES (?, ?, ?, now(), now())
   ON CONFLICT (normalized_key) DO UPDATE
     SET display_name = EXCLUDED.display_name,
         updated_at = now()
   RETURNING id")

(def ^:private upsert-alias-sql
  "INSERT INTO manufacturer_aliases
     (id, raw_label, raw_label_normalized, manufacturer_id, confidence, created_at, updated_at)
   VALUES (?, ?, ?, ?, ?, now(), now())
   ON CONFLICT (raw_label_normalized) DO UPDATE
     SET raw_label = EXCLUDED.raw_label,
         manufacturer_id = COALESCE(manufacturer_aliases.manufacturer_id, EXCLUDED.manufacturer_id),
         confidence = CASE
                        WHEN manufacturer_aliases.manufacturer_id IS NULL
                          THEN EXCLUDED.confidence
                        ELSE manufacturer_aliases.confidence
                      END,
         updated_at = now()
   RETURNING id")

(defn- apply-import!
  [ds dataset]
  (jdbc/with-transaction [tx ds]
    (reduce
      (fn [{:keys [manufacturers aliases] :as acc}
           {:keys [display-name display_name aliases] :as row}]
        (let [display (or display-name display_name)
              {:keys [label normalized]} (normalize-or-throw display)
              manufacturer-id (:id (jdbc/execute-one!
                                     tx
                                     [upsert-manufacturer-sql (UUID/randomUUID) label normalized]
                                     {:builder-fn rs/as-unqualified-lower-maps}))
              aliases* (->> (or aliases [])
                         (map (fn [a]
                                (let [{:keys [label normalized]} (normalize-or-throw a)]
                                  {:raw label :normalized normalized})))
                         distinct)]
          (doseq [{:keys [raw normalized]} aliases*]
            (jdbc/execute-one!
              tx
              [upsert-alias-sql (UUID/randomUUID) raw normalized manufacturer-id 100]
              {:builder-fn rs/as-unqualified-lower-maps}))
          (-> acc
            (update :manufacturers inc)
            (update :aliases + (count aliases*)))))
      {:manufacturers 0 :aliases 0}
      dataset)))

(defn -main [& args]
  (let [{:keys [profile file apply?]} (parse-args args)
        config (aero/read-config "config/base.edn" {:profile profile})
        ds (datasource-from-config config)
        dataset (read-dataset file)]
    (when-not (vector? dataset)
      (throw (ex-info "Dataset must be a vector" {:file file :type (type dataset)})))
    (println (str "[" (Instant/now) "]"))
    (println "Import manufacturers")
    (println "  profile:" (name profile))
    (println "  file:" file)
    (println "  rows:" (count dataset))
    (println "")
    (if (not apply?)
      (do
        (println "Dry-run only. Re-run with --apply to write changes.")
        (System/exit 0))
      (do
        (println "Applying changes...")
        (let [{:keys [manufacturers aliases]} (apply-import! ds dataset)]
          (println "✅ Done")
          (println "  manufacturers upserted:" manufacturers)
          (println "  aliases upserted:" aliases)
          (System/exit 0))))))

(apply -main *command-line-args*)
