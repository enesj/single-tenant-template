#!/usr/bin/env clj

(ns scripts.bb.expenses.promote-manufacturer-aliases-to-manufacturers
  (:require
    [aero.core :as aero]
    [clojure.string :as str]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.time Instant]
    [java.util UUID]))

(def ^:private default-confidence
  "Confidence to assign when auto-linking an unmapped alias to a newly created manufacturer."
  25)

(defn- usage []
  (println "Usage:")
  (println "  clj -M scripts/bb/expenses/promote_manufacturer_aliases_to_manufacturers.clj [dev|test] [--apply]")
  (println "")
  (println "Default is dry-run (no DB writes).")
  (println "")
  (println "Behavior:")
  (println "  - For each manufacturer_aliases row with manufacturer_id IS NULL:")
  (println "      • upsert a manufacturers row keyed by normalized_key = raw_label_normalized")
  (println "      • set manufacturer_aliases.manufacturer_id (does not overwrite existing mappings)")
  (println "  - Then backfill articles.manufacturer_id (+ articles.manufacturer convenience string)")
  (println "      from articles.manufacturer_alias_id -> manufacturer_aliases.manufacturer_id"))

(defn- parse-args [args]
  (loop [args args
         parsed {:profile :dev
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

(defn- stats [ds]
  (jdbc/execute-one!
    ds
    ["SELECT
        (SELECT count(*) FROM manufacturers) AS manufacturers,
        (SELECT count(*) FROM manufacturer_aliases) AS manufacturer_aliases,
        (SELECT count(*) FROM manufacturer_aliases WHERE manufacturer_id IS NULL) AS manufacturer_aliases_unmapped,
        (SELECT count(*) FROM articles) AS articles,
        (SELECT count(*) FROM articles WHERE manufacturer_alias_id IS NOT NULL) AS articles_with_alias,
        (SELECT count(*) FROM articles WHERE manufacturer_id IS NOT NULL) AS articles_with_manufacturer_id"]
    {:builder-fn rs/as-unqualified-lower-maps}))

(def ^:private fetch-unmapped-aliases-sql
  "SELECT id, raw_label, raw_label_normalized
     FROM manufacturer_aliases
    WHERE manufacturer_id IS NULL
      AND raw_label_normalized IS NOT NULL
    ORDER BY created_at ASC")

(def ^:private upsert-manufacturer-sql
  "INSERT INTO manufacturers (id, display_name, normalized_key, created_at, updated_at)
   VALUES (?, ?, ?, now(), now())
   ON CONFLICT (normalized_key) DO UPDATE
     SET updated_at = now()
   RETURNING id")

(def ^:private map-alias-sql
  "UPDATE manufacturer_aliases
      SET manufacturer_id = ?,
          confidence = ?,
          updated_at = now()
    WHERE id = ?
      AND manufacturer_id IS NULL
    RETURNING id")

(def ^:private backfill-articles-sql
  "UPDATE articles a
      SET manufacturer_id = ma.manufacturer_id,
          manufacturer = m.display_name,
          updated_at = now()
     FROM manufacturer_aliases ma
     JOIN manufacturers m ON m.id = ma.manufacturer_id
    WHERE a.manufacturer_alias_id = ma.id
      AND a.manufacturer_id IS NULL
      AND ma.manufacturer_id IS NOT NULL")

(defn- safe-display-name [raw-label raw-label-normalized]
  (let [s (some-> raw-label str str/trim not-empty)]
    (or s raw-label-normalized)))

(defn- update-count [result]
  (or (get result :next.jdbc/update-count)
      (get result :update-count)
      0))

(defn- promote!
  [ds]
  (jdbc/with-transaction [tx ds]
    (let [aliases (jdbc/execute!
                    tx
                    [fetch-unmapped-aliases-sql]
                    {:builder-fn rs/as-unqualified-lower-maps})
          promoted
          (reduce
            (fn [acc {:keys [id raw_label raw_label_normalized]}]
              (let [normalized (some-> raw_label_normalized str/trim not-empty)]
                (if (str/blank? normalized)
                  acc
                  (let [display (safe-display-name raw_label normalized)
                        manufacturer-id (:id (jdbc/execute-one!
                                               tx
                                               [upsert-manufacturer-sql (UUID/randomUUID) display normalized]
                                               {:builder-fn rs/as-unqualified-lower-maps}))
                        mapped (jdbc/execute-one!
                                 tx
                                 [map-alias-sql manufacturer-id default-confidence id]
                                 {:builder-fn rs/as-unqualified-lower-maps})]
                    (cond-> acc
                      manufacturer-id (update :manufacturers-created inc)
                      mapped (update :aliases-mapped inc))))))
            {:manufacturers-created 0
             :aliases-mapped 0}
            aliases)
          backfilled-result (jdbc/execute-one!
                             tx
                             [backfill-articles-sql])]
      (assoc promoted :articles-backfilled (update-count backfilled-result)))))

(defn -main [& args]
  (let [{:keys [profile apply?]} (parse-args args)
        config (aero/read-config "config/base.edn" {:profile profile})
        ds (datasource-from-config config)
        before (stats ds)]
    (println (str "[" (Instant/now) "]"))
    (println "Promote manufacturer_aliases -> manufacturers")
    (println "  profile:" (name profile))
    (println "  dry-run?:" (not apply?))
    (println "")
    (println "Before:")
    (println "  manufacturers:" (:manufacturers before))
    (println "  manufacturer_aliases:" (:manufacturer_aliases before)
      "(unmapped:" (:manufacturer_aliases_unmapped before) ")")
    (println "  articles:" (:articles before)
      "(with alias:" (:articles_with_alias before)
      ", with manufacturer_id:" (:articles_with_manufacturer_id before) ")")
    (println "")

    (if (not apply?)
      (do
        (println "Dry-run only. Re-run with --apply to write changes.")
        (System/exit 0))
      (do
        (println "Applying changes...")
        (let [result (promote! ds)
              after (stats ds)]
          (println "✅ Done")
          (println "  manufacturers upserted:" (:manufacturers-created result))
          (println "  aliases mapped:" (:aliases-mapped result)
            "(confidence:" default-confidence ")")
          (println "  articles backfilled:" (:articles-backfilled result))
          (println "")
          (println "After:")
          (println "  manufacturers:" (:manufacturers after))
          (println "  manufacturer_aliases:" (:manufacturer_aliases after)
            "(unmapped:" (:manufacturer_aliases_unmapped after) ")")
          (println "  articles:" (:articles after)
            "(with alias:" (:articles_with_alias after)
            ", with manufacturer_id:" (:articles_with_manufacturer_id after) ")")
          (System/exit 0))))))

(apply -main *command-line-args*)
