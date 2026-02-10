#!/usr/bin/env bb

(ns scripts.bb.articles.report-progress
  (:require
    [articles.db :as db]))

(defn- usage
  ([] (usage nil))
  ([msg]
   (when msg
     (binding [*out* *err*]
       (println msg)
       (println "")))
   (println "Report progress for article mapping + taxonomy population.")
   (println "")
   (println "Usage:")
   (println "  bb scripts/bb/articles/report_progress.clj [dev|test] [--coverage-only] [--limit N] [--pretty]")
   (println "")
   (println "Options:")
   (println "  --coverage-only    Only print coverage stats")
   (println "  --limit N          Limit for grouped breakdowns (default 100)")
   (println "  --pretty           Pretty EDN output")
   (println "")
   (println "Output:")
   (println "  EDN map with keys: :coverage, :by-category, :by-manufacturer")))

(defn- parse-args
  [args]
  (loop [args args
         parsed {:coverage-only? false
                 :pretty? false
                 :limit 100}]
    (let [[a b & more] args]
      (cond
        (nil? a) parsed

        (or (= a "--help") (= a "-h"))
        (do (usage) (System/exit 0))

        (= a "--pretty")
        (recur (cons b more) (assoc parsed :pretty? true))

        (= a "--coverage-only")
        (recur (cons b more) (assoc parsed :coverage-only? true))

        (= a "--limit")
        (let [n (db/parse-long b)]
          (when-not n
            (usage (str "Invalid --limit: " b))
            (System/exit 1))
          (recur more (assoc parsed :limit n)))

        :else
        (do
          (usage (str "Unknown arg: " a))
          (System/exit 1))))))

(defn- coverage!
  [db]
  (db/query1
    db
    (str
      "WITH stats AS (\n"
      "  SELECT\n"
      "    COUNT(DISTINCT aa.id) AS total_aliases,\n"
      "    COUNT(DISTINCT CASE WHEN aa.article_id IS NOT NULL THEN aa.id END) AS mapped_aliases,\n"
      "    COUNT(DISTINCT a.id) AS total_articles\n"
      "  FROM article_aliases aa\n"
      "  LEFT JOIN articles a ON a.id = aa.article_id\n"
      ")\n"
      "SELECT\n"
      "  total_aliases,\n"
      "  mapped_aliases,\n"
      "  total_articles,\n"
      "  ROUND(100.0 * mapped_aliases / NULLIF(total_aliases, 0), 1) AS coverage_percent\n"
      "FROM stats")))

(defn- by-category!
  [db limit]
  (db/query
    db
    (str
      "SELECT\n"
      "  COALESCE(c.name, 'Uncategorized') AS category,\n"
      "  COALESCE(s.name, 'Uncategorized') AS subcategory,\n"
      "  COUNT(*) AS article_count\n"
      "FROM articles a\n"
      "LEFT JOIN subcategories s ON s.id = a.subcategory_id\n"
      "LEFT JOIN categories c ON c.id = s.category_id\n"
      "GROUP BY c.name, s.name\n"
      "ORDER BY article_count DESC, category, subcategory\n"
      "LIMIT " limit)))

(defn- by-manufacturer!
  [db limit]
  (db/query
    db
    (str
      "SELECT\n"
      "  COALESCE(m.display_name, 'Generic') AS manufacturer,\n"
      "  COUNT(*) AS article_count\n"
      "FROM articles a\n"
      "LEFT JOIN manufacturers m ON m.id = a.manufacturer_id\n"
      "GROUP BY m.display_name\n"
      "ORDER BY article_count DESC, manufacturer\n"
      "LIMIT " limit)))

(defn -main
  [& args]
  (let [{:keys [profile args]} (db/parse-profile args)
        {:keys [coverage-only? pretty? limit]} (parse-args args)
        config (db/read-config profile)
        db (:database config)
        result (if coverage-only?
                 {:coverage (coverage! db)}
                 {:coverage (coverage! db)
                  :by-category (by-category! db limit)
                  :by-manufacturer (by-manufacturer! db limit)})]
    (if pretty?
      (db/pprint-edn result)
      (db/prn-edn result))))

(apply -main *command-line-args*)
