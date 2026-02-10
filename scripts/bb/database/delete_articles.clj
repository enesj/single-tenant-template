#!/usr/bin/env clj

(ns scripts.bb.database.delete-articles
  "Delete all rows from articles and related taxonomy tables while preserving article_aliases.

  Safety:
  - Default is dry-run (no DB writes)
  - Requires --apply to perform deletion
  - Prompts for a confirmation phrase unless --yes is provided

  Usage:
    bb delete-articles [--dev|--test|dev|test] [--apply] [--yes]

  Notes:
  - Clears article_aliases.article_id before deleting articles
  - This preserves article_aliases rows without changing schema at runtime
  - Deletes all rows from: articles, manufacturers, categories, subcategories
  - Deleting from `articles` will cascade to price_observations
  - expense_items.article_id will be set to NULL"
  (:require
    [aero.core :as aero]
    [clojure.string :as str]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.time Instant]))

(defn- usage
  ([] (usage nil))
  ([msg]
   (when msg
     (binding [*out* *err*]
       (println msg)
       (println "")))
   (println "Usage:")
   (println "  bb delete-articles [--dev|--test|dev|test] [--apply] [--yes]")
   (println "")
   (println "Default is dry-run (no DB writes).")
   (println "")
   (println "Examples:")
   (println "  bb delete-articles --dev")
   (println "  bb delete-articles --dev --apply")
   (println "  bb delete-articles test --apply --yes")))

(defn- parse-args
  [args]
  (loop [args args
         parsed {:profile :dev
                 :apply? false
                 :yes? false}]
    (let [[a b & more] args]
      (cond
        (nil? a) parsed

        (#{"dev" "test"} a)
        (recur (cons b more) (assoc parsed :profile (keyword a)))

        (= a "--dev")
        (recur (cons b more) (assoc parsed :profile :dev))

        (= a "--test")
        (recur (cons b more) (assoc parsed :profile :test))

        (= a "--apply")
        (recur (cons b more) (assoc parsed :apply? true))

        (or (= a "--yes") (= a "--force"))
        (recur (cons b more) (assoc parsed :yes? true))

        (or (= a "--help") (= a "-h"))
        (do (usage) (System/exit 0))

        :else
        (do
          (usage (str "Unknown arg: " a))
          (System/exit 1))))))

(defn- datasource-from-config
  [config]
  (let [{:keys [host port dbname user password]} (:database config)]
    (jdbc/get-datasource {:dbtype "postgresql"
                          :host host
                          :port port
                          :dbname dbname
                          :user user
                          :password password})))

(defn- stats
  [ds]
  (jdbc/execute-one!
    ds
    [(str
       "SELECT\n"
       "  (SELECT count(*) FROM articles) AS articles,\n"
       "  (SELECT count(*) FROM manufacturers) AS manufacturers,\n"
       "  (SELECT count(*) FROM categories) AS categories,\n"
       "  (SELECT count(*) FROM subcategories) AS subcategories,\n"
       "  (SELECT count(*) FROM article_aliases) AS article_aliases,\n"
       "  (SELECT count(*) FROM article_aliases WHERE article_id IS NOT NULL) AS article_aliases_mapped,\n"
       "  (SELECT count(*) FROM price_observations) AS price_observations,\n"
       "  (SELECT count(*) FROM expense_items WHERE article_id IS NOT NULL) AS expense_items_with_article_id")]
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- update-count
  [result]
  (or (get result :next.jdbc/update-count)
    (get result :update-count)
    0))

(defn- confirm!
  [{:keys [profile dbname]}]
  (println (str "⚠️  DANGER: This will DELETE ALL rows from articles, manufacturers, categories, and subcategories in the " (name profile) " database!"))
  (println (str "🎯 Target DB: " dbname))
  (println "")
  (print "Type 'DELETE ARTICLES TAXONOMY' to confirm: ")
  (flush)
  (= "DELETE ARTICLES TAXONOMY" (str/trim (read-line))))

(defn -main
  [& args]
  (let [{:keys [profile apply? yes?]} (parse-args args)
        config (aero/read-config "config/base.edn" {:profile profile})
        ds (datasource-from-config config)
        dbname (get-in config [:database :dbname])
        before (stats ds)]

    (println (str "[" (Instant/now) "]"))
    (println "Delete articles and taxonomy tables (preserve article_aliases)")
    (println "  profile:" (name profile))
    (println "  dbname:  " dbname)
    (println "  dry-run?:" (not apply?))
    (println "")
    (println "Before:")
    (println "  articles:" (:articles before))
    (println "  manufacturers:" (:manufacturers before))
    (println "  categories:" (:categories before))
    (println "  subcategories:" (:subcategories before))
    (println "  article_aliases:" (:article_aliases before))
    (println "  article_aliases (mapped):" (:article_aliases_mapped before))
    (println "  price_observations:" (:price_observations before))
    (println "  expense_items (with article_id):" (:expense_items_with_article_id before))
    (println "")

    (when-not apply?
      (println "Dry-run only. Re-run with --apply to clear alias mappings and delete articles/taxonomy tables.")
      (System/exit 0))

    (when-not (or yes? (confirm! {:profile profile :dbname dbname}))
      (println "❌ Cancelled.")
      (System/exit 1))

    (println "Clearing article_aliases.article_id values...")
    (let [cleared-mappings
          (update-count
            (jdbc/execute-one!
              ds
              ["UPDATE article_aliases SET article_id = NULL WHERE article_id IS NOT NULL"]))]
      (println "✅ Cleared article_aliases mappings:" cleared-mappings))

    (println "Deleting articles and taxonomy tables...")
    (let [deleted-articles (update-count (jdbc/execute-one! ds ["DELETE FROM articles"]))
          deleted-subcategories (update-count (jdbc/execute-one! ds ["DELETE FROM subcategories"]))
          deleted-manufacturers (update-count (jdbc/execute-one! ds ["DELETE FROM manufacturers"]))
          after (stats ds)]
      (println "✅ Done")
      (println "  deleted articles:" deleted-articles)
      (println "  deleted subcategories:" deleted-subcategories)
      (println "  deleted categories:" deleted-categories)
      (println "  deleted manufacturers:" deleted-manufacturers)
      (println "")
      (println "After:")
      (println "  articles:" (:articles after))
      (println "  manufacturers:" (:manufacturers after))
      (println "  categories:" (:categories after))
      (println "  subcategories:" (:subcategories after))
      (println "  article_aliases:" (:article_aliases after))
      (println "  article_aliases (mapped):" (:article_aliases_mapped after))
      (println "  price_observations:" (:price_observations after))
      (println "  expense_items (with article_id):" (:expense_items_with_article_id after))
      (println "")
      (println "💡 Article aliases remain preserved with article_id cleared.")
      (System/exit 0))))

(apply -main *command-line-args*)
