#!/usr/bin/env bb

(ns scripts.bb.articles.delete-unmapped-aliases
  (:require
    [clojure.string :as str]
    [articles.db :as db]))

(defn- usage
  ([] (usage nil))
  ([msg]
   (when msg
     (binding [*out* *err*]
       (println msg)
       (println "")))
   (println "Delete unmapped article_aliases by raw_label (OCR noise cleanup).")
   (println "")
   (println "Safety:")
   (println "  - Default is dry-run (no DB writes)")
   (println "  - Requires --apply to delete")
   (println "  - Prompts for confirmation phrase unless --yes is provided")
   (println "")
   (println "Usage:")
   (println "  bb scripts/bb/articles/delete_unmapped_aliases.clj [dev|test] --raw-label TEXT [--raw-label TEXT ...] [--supplier DISPLAY_NAME] [--apply] [--yes]")
   (println "")
   (println "Options:")
   (println "  --raw-label TEXT           (repeatable; required)")
   (println "  --supplier DISPLAY_NAME    (optional; only delete for a single supplier)")
   (println "  --apply                    (perform deletion)")
   (println "  --yes                      (skip confirmation prompt)")
   (println "  --pretty                   (pretty EDN output)")
   (println "")
   (println "Notes:")
   (println "  - This script uses `psql` (no JDBC).")))

(defn- parse-args
  [args]
  (loop [args args
         parsed {:apply? false
                 :yes? false
                 :pretty? false
                 :raw-labels []}]
    (let [[a b & more] args]
      (cond
        (nil? a) parsed

        (or (= a "--help") (= a "-h"))
        (do (usage) (System/exit 0))

        (= a "--apply")
        (recur (cons b more) (assoc parsed :apply? true))

        (or (= a "--yes") (= a "--force"))
        (recur (cons b more) (assoc parsed :yes? true))

        (= a "--pretty")
        (recur (cons b more) (assoc parsed :pretty? true))

        (= a "--supplier")
        (recur more (assoc parsed :supplier-display-name b))

        (= a "--raw-label")
        (recur more (update parsed :raw-labels conj b))

        :else
        (do
          (usage (str "Unknown arg: " a))
          (System/exit 1))))))

(defn- confirm!
  [{:keys [profile dbname]}]
  (println (str "⚠️  DANGER: This will DELETE rows from article_aliases in the " (name profile) " database."))
  (println (str "🎯 Target DB: " dbname))
  (println "")
  (print "Type 'DELETE UNMAPPED ARTICLE ALIASES' to confirm: ")
  (flush)
  (= "DELETE UNMAPPED ARTICLE ALIASES" (str/trim (read-line))))

(defn- resolve-supplier-id!
  [db supplier-display-name]
  (when supplier-display-name
    (:id
     (db/query1
       db
       (str
         "SELECT id\n"
         "FROM suppliers\n"
         "WHERE display_name = " (db/sql-literal supplier-display-name) "\n"
         "LIMIT 1")))))

(defn- where-sql
  [{:keys [raw-labels supplier-id]}]
  (str
    "WHERE article_id IS NULL\n"
    "  AND raw_label IN " (db/sql-in-list raw-labels) "\n"
    (when supplier-id
      (str "  AND supplier_id = " (db/sql-literal supplier-id) "\n"))))

(defn -main
  [& args]
  (let [{:keys [profile args]} (db/parse-profile args)
        {:keys [apply? yes? pretty? raw-labels supplier-display-name]} (parse-args args)
        raw-labels (vec (distinct raw-labels))
        _ (when (empty? raw-labels)
            (usage "At least one --raw-label is required")
            (System/exit 1))
        config (db/read-config profile)
        db (:database config)
        dbname (:dbname db)
        supplier-id (resolve-supplier-id! db supplier-display-name)
        _ (when (and supplier-display-name (nil? supplier-id))
            (throw (ex-info "Supplier not found" {:supplier-display-name supplier-display-name})))
        where (where-sql {:raw-labels raw-labels :supplier-id supplier-id})
        count-row (db/query1
                    db
                    (str
                      "SELECT COUNT(*) AS count\n"
                      "FROM article_aliases\n"
                      where))
        to-delete (:count count-row)
        planned {:profile profile
                 :dbname dbname
                 :supplier_display_name supplier-display-name
                 :supplier_id supplier-id
                 :raw_labels raw-labels
                 :would_delete to-delete}
        result
        (if-not apply?
          {:dry_run true
           :planned planned}
          (do
            (when-not (or yes? (confirm! {:profile profile :dbname dbname}))
              (throw (ex-info "Cancelled" {})))
            (let [deleted-rows (db/query
                                 db
                                 (str
                                   "DELETE FROM article_aliases\n"
                                   where
                                   "RETURNING id"))]
              {:dry_run false
               :planned planned
               :deleted (count deleted-rows)})))]
    (if pretty?
      (db/pprint-edn result)
      (db/prn-edn result))))

(apply -main *command-line-args*)
