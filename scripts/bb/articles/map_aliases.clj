#!/usr/bin/env bb

(ns scripts.bb.articles.map-aliases
  (:require
    [articles.db :as db]))

(defn- usage
  ([] (usage nil))
  ([msg]
   (when msg
     (binding [*out* *err*]
       (println msg)
       (println "")))
   (println "Map one or more article_aliases rows to an article by setting article_aliases.article_id.")
   (println "")
   (println "Usage:")
   (println "  bb scripts/bb/articles/map_aliases.clj [dev|test] (alias selector) (article selector) [--dry-run] [--allow-reassign] [--pretty]")
   (println "")
   (println "Alias selector (choose exactly one mode):")
   (println "  --alias-id UUID                (repeat for batch updates)")
   (println "  --raw-label TEXT --supplier DISPLAY_NAME")
   (println "")
   (println "Article selector (exactly one):")
   (println "  --article-id UUID")
   (println "  --article-key NORMALIZED_KEY")
   (println "  --canonical-name TEXT")
   (println "")
   (println "Options:")
   (println "  --allow-reassign   Allow remapping aliases that already have article_id")
   (println "")
   (println "Output:")
   (println "  EDN: {:updated <n> :requested <n> :rows [...] :article_id <uuid> :not_updated_alias_ids [...]}")
   (println "")
   (println "Notes:")
   (println "  - Default behavior only updates currently unmapped aliases (article_id IS NULL).")
   (println "  - In batch mode, --alias-id may be repeated.")
   (println "  - This script uses `psql` (no JDBC).")))

(defn- parse-args
  [args]
  (loop [args args
         parsed {:dry-run? false
                 :allow-reassign? false
                 :pretty? false
                 :alias-ids []}]
    (let [[a b & more] args]
      (cond
        (nil? a) parsed

        (or (= a "--help") (= a "-h"))
        (do (usage) (System/exit 0))

        (= a "--dry-run")
        (recur (cons b more) (assoc parsed :dry-run? true))

        (= a "--allow-reassign")
        (recur (cons b more) (assoc parsed :allow-reassign? true))

        (= a "--pretty")
        (recur (cons b more) (assoc parsed :pretty? true))

        (= a "--alias-id")
        (recur more (update parsed :alias-ids conj b))

        (= a "--raw-label")
        (recur more (assoc parsed :raw-label b))

        (= a "--supplier")
        (recur more (assoc parsed :supplier-display-name b))

        (= a "--article-id")
        (recur more (assoc parsed :article-id b))

        (= a "--article-key")
        (recur more (assoc parsed :article-key b))

        (= a "--canonical-name")
        (recur more (assoc parsed :canonical-name b))

        :else
        (do
          (usage (str "Unknown arg: " a))
          (System/exit 1))))))

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

(defn- resolve-article-id!
  [db {:keys [article-id article-key canonical-name]}]
  (cond
    article-id article-id

    article-key
    (:id
     (db/query1
       db
       (str
         "SELECT id\n"
         "FROM articles\n"
         "WHERE normalized_key = " (db/sql-literal article-key) "\n"
         "LIMIT 1")))

    canonical-name
    (:id
     (db/query1
       db
       (str
         "SELECT id\n"
         "FROM articles\n"
         "WHERE canonical_name = " (db/sql-literal canonical-name) "\n"
         "LIMIT 1")))

    :else
    nil))

(defn- update-by-alias-ids!
  [db alias-ids article-id allow-reassign?]
  (db/query
    db
    (str
      "UPDATE article_aliases\n"
      "SET article_id = " (db/sql-literal article-id) "\n"
      "WHERE id IN " (db/sql-in-list alias-ids) "\n"
      (when-not allow-reassign?
        "  AND article_id IS NULL\n")
      "RETURNING id, raw_label, supplier_id, article_id")))

(defn- update-by-raw-label-and-supplier!
  [db {:keys [raw-label supplier-id article-id allow-reassign?]}]
  (db/query
    db
    (str
      "UPDATE article_aliases\n"
      "SET article_id = " (db/sql-literal article-id) "\n"
      "WHERE raw_label = " (db/sql-literal raw-label) "\n"
      "  AND supplier_id = " (db/sql-literal supplier-id) "\n"
      (when-not allow-reassign?
        "  AND article_id IS NULL\n")
      "RETURNING id, raw_label, supplier_id, article_id")))

(defn- alias-selector-count
  [{:keys [alias-ids raw-label supplier-display-name]}]
  (+ (if (seq alias-ids) 1 0)
    (if (and raw-label supplier-display-name) 1 0)))

(defn- article-selector-count
  [{:keys [article-id article-key canonical-name]}]
  (+ (if article-id 1 0)
    (if article-key 1 0)
    (if canonical-name 1 0)))

(defn -main
  [& args]
  (let [{:keys [profile args]} (db/parse-profile args)
        opts (parse-args args)
        {:keys [dry-run? pretty? allow-reassign? raw-label supplier-display-name]} opts
        alias-ids (->> (:alias-ids opts)
                    (remove nil?)
                    distinct
                    vec)
        _ (when (or (and raw-label (not supplier-display-name))
                  (and supplier-display-name (not raw-label)))
            (usage "--raw-label and --supplier must be provided together")
            (System/exit 1))
        alias-selector-count* (alias-selector-count (assoc opts :alias-ids alias-ids))
        article-selector-count* (article-selector-count opts)
        _ (when (not= alias-selector-count* 1)
            (usage "Choose exactly one alias selector: one or more --alias-id OR (--raw-label and --supplier)")
            (System/exit 1))
        _ (when (not= article-selector-count* 1)
            (usage "Choose exactly one article selector: --article-id, --article-key, or --canonical-name")
            (System/exit 1))
        config (db/read-config profile)
        db (:database config)
        planned {:alias (if (seq alias-ids)
                          {:alias_ids alias-ids}
                          {:raw_label raw-label
                           :supplier_display_name supplier-display-name})
                 :article (select-keys opts [:article-id :article-key :canonical-name])
                 :allow_reassign allow-reassign?}]
    (if dry-run?
      (if pretty?
        (db/pprint-edn {:dry_run true :planned planned})
        (db/prn-edn {:dry_run true :planned planned}))
      (let [supplier-id (when (and (empty? alias-ids) supplier-display-name)
                          (resolve-supplier-id! db supplier-display-name))
            _ (when (and (empty? alias-ids) (nil? supplier-id))
                (throw (ex-info "Supplier not found" {:supplier-display-name supplier-display-name})))
            resolved-article-id (resolve-article-id! db opts)
            _ (when-not resolved-article-id
                (throw (ex-info
                         "Article not found (provide --article-id, --article-key, or --canonical-name)"
                         {:opts (select-keys opts [:article-id :article-key :canonical-name])})))
            rows (if (seq alias-ids)
                   (update-by-alias-ids! db alias-ids resolved-article-id allow-reassign?)
                   (update-by-raw-label-and-supplier!
                     db
                     {:raw-label raw-label
                      :supplier-id supplier-id
                      :article-id resolved-article-id
                      :allow-reassign? allow-reassign?}))
            updated-alias-id-set (set (map :id rows))
            not-updated-alias-ids (if (seq alias-ids)
                                    (->> alias-ids
                                      (remove updated-alias-id-set)
                                      vec)
                                    [])
            _ (when (empty? rows)
                (throw (ex-info (if allow-reassign?
                                  "No alias rows updated"
                                  "No alias rows updated. Alias may already be mapped; rerun with --allow-reassign to override.")
                         {:alias-ids alias-ids
                          :raw-label raw-label
                          :supplier-id supplier-id
                          :allow-reassign? allow-reassign?})))
            result (cond-> {:updated (count rows)
                            :requested (if (seq alias-ids) (count alias-ids) 1)
                            :article_id resolved-article-id
                            :rows rows}
                     (seq not-updated-alias-ids)
                     (assoc :not_updated_alias_ids not-updated-alias-ids))]
        (if pretty?
          (db/pprint-edn result)
          (db/prn-edn result))))))

(apply -main *command-line-args*)
