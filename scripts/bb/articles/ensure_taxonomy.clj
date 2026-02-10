#!/usr/bin/env bb

(ns scripts.bb.articles.ensure-taxonomy
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
   (println "Ensure taxonomy rows exist (manufacturers, categories, subcategories) and print their IDs.")
   (println "")
   (println "Usage:")
   (println "  bb scripts/bb/articles/ensure_taxonomy.clj [dev|test] [options]")
   (println "")
   (println "Options:")
   (println "  --manufacturer-name NAME")
   (println "  --manufacturer-key KEY             (optional; defaults to normalized(NAME))")
   (println "  --category-name NAME")
   (println "  --category-description TEXT        (optional)")
   (println "  --subcategory-name NAME")
   (println "  --subcategory-description TEXT     (optional)")
   (println "")
   (println "  --update-manufacturer-name         (allow updating display_name on conflict)")
   (println "  --update-category-description      (allow updating description on conflict)")
   (println "  --update-subcategory-description   (allow updating description on conflict)")
   (println "")
   (println "  --dry-run                          (no DB writes)")
   (println "  --pretty                           (pretty EDN output)")
   (println "")
   (println "Notes:")
   (println "  - If --subcategory-name is provided, --category-name is required.")
   (println "  - By default existing taxonomy values are preserved on conflicts.")
   (println "  - Output is EDN: {:manufacturer .. :category .. :subcategory ..}.")))

(defn- parse-args
  [args]
  (loop [args args
         parsed {:dry-run? false
                 :pretty? false
                 :update-manufacturer-name? false
                 :update-category-description? false
                 :update-subcategory-description? false}]
    (let [[a b & more] args]
      (cond
        (nil? a) parsed

        (or (= a "--help") (= a "-h"))
        (do (usage) (System/exit 0))

        (= a "--dry-run")
        (recur (cons b more) (assoc parsed :dry-run? true))

        (= a "--pretty")
        (recur (cons b more) (assoc parsed :pretty? true))

        (= a "--update-manufacturer-name")
        (recur (cons b more) (assoc parsed :update-manufacturer-name? true))

        (= a "--update-category-description")
        (recur (cons b more) (assoc parsed :update-category-description? true))

        (= a "--update-subcategory-description")
        (recur (cons b more) (assoc parsed :update-subcategory-description? true))

        (= a "--manufacturer-name")
        (recur more (assoc parsed :manufacturer-name b))

        (= a "--manufacturer-key")
        (recur more (assoc parsed :manufacturer-key b))

        (= a "--category-name")
        (recur more (assoc parsed :category-name b))

        (= a "--category-description")
        (recur more (assoc parsed :category-description b))

        (= a "--subcategory-name")
        (recur more (assoc parsed :subcategory-name b))

        (= a "--subcategory-description")
        (recur more (assoc parsed :subcategory-description b))

        :else
        (do
          (usage (str "Unknown arg: " a))
          (System/exit 1))))))

(defn- ensure-manufacturer!
  [db {:keys [manufacturer-name manufacturer-key update-manufacturer-name?]}]
  (when manufacturer-name
    (let [k (some-> (or manufacturer-key (db/normalize-key manufacturer-name)) str str/trim)
          _ (when (str/blank? k)
              (throw (ex-info "Manufacturer normalized key cannot be blank"
                       {:manufacturer-name manufacturer-name
                        :manufacturer-key manufacturer-key})))
          id (db/uuid)
          inserted (db/query1
                     db
                     (str
                       "INSERT INTO manufacturers (id, display_name, normalized_key)\n"
                       "VALUES (" (db/sql-literal id) ", " (db/sql-literal manufacturer-name) ", " (db/sql-literal k) ")\n"
                       "ON CONFLICT (normalized_key)\n"
                       (if update-manufacturer-name?
                         "DO UPDATE SET display_name = EXCLUDED.display_name\n"
                         "DO NOTHING\n")
                       "RETURNING id, display_name, normalized_key"))]
      (or inserted
        (db/query1
          db
          (str
            "SELECT id, display_name, normalized_key\n"
            "FROM manufacturers\n"
            "WHERE normalized_key = " (db/sql-literal k) "\n"
            "LIMIT 1"))))))

(defn- ensure-category!
  [db {:keys [category-name category-description update-category-description?]}]
  (when category-name
    (let [id (db/uuid)
          inserted (db/query1
                     db
                     (str
                       "INSERT INTO categories (id, name, description)\n"
                       "VALUES (" (db/sql-literal id) ", " (db/sql-literal category-name) ", " (db/sql-literal category-description) ")\n"
                       "ON CONFLICT (name)\n"
                       (if update-category-description?
                         "DO UPDATE SET description = COALESCE(EXCLUDED.description, categories.description)\n"
                         "DO NOTHING\n")
                       "RETURNING id, name, description"))]
      (or inserted
        (db/query1
          db
          (str
            "SELECT id, name, description\n"
            "FROM categories\n"
            "WHERE name = " (db/sql-literal category-name) "\n"
            "LIMIT 1"))))))

(defn- ensure-subcategory!
  [db {:keys [category-name subcategory-name subcategory-description update-subcategory-description?]}]
  (when subcategory-name
    (let [category-id
          (:id
           (db/query1
             db
             (str
               "SELECT id\n"
               "FROM categories\n"
               "WHERE name = " (db/sql-literal category-name) "\n"
               "LIMIT 1")))
          _ (when-not category-id
              (throw
                (ex-info
                  "Category not found (did you pass --category-name / ensure it first)?"
                  {:category-name category-name})))
          id (db/uuid)
          inserted (db/query1
                     db
                     (str
                       "INSERT INTO subcategories (id, category_id, name, description)\n"
                       "VALUES (" (db/sql-literal id) ", " (db/sql-literal category-id) ", " (db/sql-literal subcategory-name) ", " (db/sql-literal subcategory-description) ")\n"
                       "ON CONFLICT (category_id, name)\n"
                       (if update-subcategory-description?
                         "DO UPDATE SET description = COALESCE(EXCLUDED.description, subcategories.description)\n"
                         "DO NOTHING\n")
                       "RETURNING id, category_id, name, description"))]
      (or inserted
        (db/query1
          db
          (str
            "SELECT id, category_id, name, description\n"
            "FROM subcategories\n"
            "WHERE category_id = " (db/sql-literal category-id) "\n"
            "  AND name = " (db/sql-literal subcategory-name) "\n"
            "LIMIT 1"))))))

(defn -main
  [& args]
  (let [{:keys [profile args]} (db/parse-profile args)
        opts (parse-args args)
        {:keys [dry-run? pretty? manufacturer-name category-name subcategory-name
                update-manufacturer-name? update-category-description? update-subcategory-description?]} opts
        _ (when-not (or manufacturer-name category-name subcategory-name)
            (usage "At least one of --manufacturer-name, --category-name, or --subcategory-name is required.")
            (System/exit 1))
        _ (when (and subcategory-name (not category-name))
            (usage "--subcategory-name requires --category-name")
            (System/exit 1))
        manufacturer-normalized-key (when manufacturer-name
                                      (some-> (or (:manufacturer-key opts)
                                                (db/normalize-key manufacturer-name))
                                        str
                                        str/trim))
        _ (when (and manufacturer-name (str/blank? manufacturer-normalized-key))
            (usage "Manufacturer normalized key cannot be blank")
            (System/exit 1))
        config (db/read-config profile)
        db (:database config)
        planned {:manufacturer (when manufacturer-name
                                 {:display_name manufacturer-name
                                  :normalized_key manufacturer-normalized-key})
                 :category (when category-name
                             {:name category-name
                              :description (:category-description opts)})
                 :subcategory (when subcategory-name
                                {:category_name category-name
                                 :name subcategory-name
                                 :description (:subcategory-description opts)})
                 :update_existing {:manufacturer_name update-manufacturer-name?
                                   :category_description update-category-description?
                                   :subcategory_description update-subcategory-description?}}]
    (if dry-run?
      (if pretty?
        (db/pprint-edn {:dry_run true :planned planned})
        (db/prn-edn {:dry_run true :planned planned}))
      (let [result {:manufacturer (ensure-manufacturer! db opts)
                    :category (ensure-category! db opts)
                    :subcategory (ensure-subcategory! db opts)}]
        (if pretty?
          (db/pprint-edn result)
          (db/prn-edn result))))))

(apply -main *command-line-args*)
