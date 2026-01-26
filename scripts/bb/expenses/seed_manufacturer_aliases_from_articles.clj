#!/usr/bin/env clj

(ns scripts.bb.expenses.seed-manufacturer-aliases-from-articles
  (:require
    [aero.core :as aero]
    [app.domain.backend.expenses.services.service-configs :as configs]
    [clojure.string :as str]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.time Instant]
    [java.util UUID]))

(def ^:private min-normalized-length
  2)

(def ^:private stop-tokens
  "Tokens that commonly appear at the end of receipt labels but are not manufacturers."
  #{"KO" "E" "T" "KG" "G" "GR" "L" "ML" "CM" "MM" "M" "D" "NAR" "ENTRY" "POPUST"})

(defn- usage []
  (println "Usage:")
  (println "  clj -M scripts/bb/expenses/seed_manufacturer_aliases_from_articles.clj [dev|test] [--apply] [--limit N]")
  (println "")
  (println "Default is dry-run (no DB writes).")
  (println "")
  (println "Options:")
  (println "  --apply      Apply changes (insert/update manufacturer_aliases and articles)")
  (println "  --limit N    Limit number of articles processed (default: no limit)")
  (println "  --help       Show this message"))

(defn- parse-pos-int [s]
  (when (and s (re-matches #"\d+" s))
    (Long/parseLong s)))

(defn- parse-args [args]
  (loop [args args
         parsed {:profile :dev
                 :apply? false
                 :limit nil}]
    (let [[a b & more] args]
      (cond
        (nil? a) parsed

        (#{"dev" "test"} a)
        (recur (cons b more) (assoc parsed :profile (keyword a)))

        (= a "--help")
        (do (usage) (System/exit 0))

        (= a "--apply")
        (recur (cons b more) (assoc parsed :apply? true))

        (= a "--limit")
        (recur more (assoc parsed :limit (parse-pos-int b)))

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

(defn- clean-token [token]
  (some-> token
    (str/replace #"[,:;\.]+$" "")
    (str/replace #"^\(" "")
    (str/replace #"\)$" "")
    ;; Drop receipt-specific suffixes like "T/KO" -> "T" (which we then ignore).
    (str/replace #"/.*$" "")
    str/trim
    not-empty))

(defn- skip-article-name?
  "Filter out known non-article labels that tend to pollute manufacturer inference." 
  [canonical-name]
  (let [s (some-> canonical-name str str/trim)
        s* (some-> s str/upper-case)]
    (or (str/blank? s)
      (str/ends-with? s ":")
      (and s* (or (str/starts-with? s* "ENTRY")
               (str/starts-with? s* "POPUST")
               (str/includes? s* "POPUST"))))))

(defn- token->candidate? [token]
  (let [t (clean-token token)
        t-upper (some-> t str/upper-case)]
    (and (some? t)
      (>= (count t) 3)
      (not (re-find #"\d" t))
      (not (contains? stop-tokens t-upper))
      ;; Only allow letters + hyphen (unicode-aware).
      (boolean (re-matches #"(?U)[\p{L}][\p{L}-]*" t)))))

(defn- infer-manufacturer-raw-label
  "Heuristic: attempt to infer a manufacturer/brand token from the end of an article name.

  This is intended to build a *review queue* (manufacturer_aliases) when no
  canonical manufacturer dataset exists yet.

  Returns a string or nil."
  [canonical-name]
  (when-not (skip-article-name? canonical-name)
  (let [tokens (->> (str/split (or canonical-name "") #"\s+")
                 (map clean-token)
                 (remove str/blank?))]
    (some (fn [t] (when (token->candidate? t) t)) (reverse tokens)))))

(defn- fetch-articles
  [ds {:keys [limit]}]
  (jdbc/execute!
    ds
    (cond->
      ["SELECT id, canonical_name
        FROM articles
        WHERE canonical_name IS NOT NULL
          AND manufacturer_alias_id IS NULL
        ORDER BY created_at DESC"]
      (some? limit)
      (conj (format "LIMIT %d" (long limit))))
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- build-plan [articles]
  (->> articles
    (keep (fn [{:keys [id canonical_name]}]
            (when-let [raw (infer-manufacturer-raw-label canonical_name)]
              (let [normalized (configs/normalize-manufacturer-key raw)]
                (when (and (some? normalized)
                        (not (str/blank? normalized))
                        (>= (count normalized) min-normalized-length))
                  {:article-id id
                   :canonical-name canonical_name
                   :manufacturer-raw raw
                   :manufacturer-normalized normalized})))))
    vec))

(def ^:private upsert-alias-sql
  "INSERT INTO manufacturer_aliases
     (id, raw_label, raw_label_normalized, manufacturer_id, confidence, created_at, updated_at)
   VALUES (?, ?, ?, NULL, 0, now(), now())
   ON CONFLICT (raw_label_normalized) DO UPDATE
     SET raw_label = EXCLUDED.raw_label,
         updated_at = now()
   RETURNING id")

(def ^:private update-article-sql
  "UPDATE articles
      SET manufacturer = ?,
          manufacturer_alias_id = ?,
          updated_at = now()
    WHERE id = ?
      AND manufacturer_alias_id IS NULL
    RETURNING id")

(defn- apply-plan!
  [ds plan]
  (jdbc/with-transaction [tx ds]
    (let [alias-cache (atom {})
          ensure-alias-id!
          (fn [raw-label normalized]
            (if-let [existing (get @alias-cache normalized)]
              existing
              (let [row (jdbc/execute-one!
                          tx
                          [upsert-alias-sql (UUID/randomUUID) raw-label normalized]
                          {:builder-fn rs/as-unqualified-lower-maps})
                    alias-id (:id row)]
                (swap! alias-cache assoc normalized alias-id)
                alias-id)))
          result
          (reduce
            (fn [acc
                 {:keys [article-id manufacturer-raw manufacturer-normalized]}]
              (let [alias-id (ensure-alias-id! manufacturer-raw manufacturer-normalized)
                    updated (jdbc/execute-one!
                              tx
                              [update-article-sql manufacturer-raw alias-id article-id]
                              {:builder-fn rs/as-unqualified-lower-maps})]
                (cond-> acc
                  updated (update :updated-articles inc))))
            {:updated-articles 0}
            plan)]
      (assoc result :aliases-touched (count @alias-cache)))))

(defn -main [& args]
  (let [{:keys [profile apply?] :as opts} (parse-args args)
        config (aero/read-config "config/base.edn" {:profile profile})
        ds (datasource-from-config config)
        articles (fetch-articles ds opts)
        plan (build-plan articles)
        by-raw (->> plan
                 (group-by :manufacturer-raw)
                 (map (fn [[k v]] {:manufacturer-raw k :occurrence-count (count v)}))
                 (sort-by (juxt (comp - :occurrence-count) :manufacturer-raw)))
        top (take 30 by-raw)]
    (println (str "[" (Instant/now) "]"))
    (println "Seed manufacturer aliases from articles")
    (println "  profile:" (name profile))
    (println "  articles scanned:" (count articles))
    (println "  candidate mappings:" (count plan))
    (println "  distinct manufacturer candidates:" (count by-raw))
    (println "")
    (println "Top candidates (raw label -> occurrences):")
    (doseq [{:keys [manufacturer-raw occurrence-count]} top]
      (println " " manufacturer-raw "->" occurrence-count))
    (println "")
    (if (not apply?)
      (do
        (println "Dry-run only. Re-run with --apply to write changes.")
        (System/exit 0))
      (do
        (println "Applying changes...")
        (let [{:keys [updated-articles aliases-touched]} (apply-plan! ds plan)]
          (println "✅ Done")
          (println "  updated articles:" updated-articles)
          (println "  aliases touched:" aliases-touched)
          (System/exit 0))))))

(apply -main *command-line-args*)
