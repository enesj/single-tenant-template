#!/usr/bin/env bb

(ns scripts.bb.articles.list-categories
  "List article categories (and optionally subcategories) from the DB."
  (:require
    [articles.db :as db]))

(defn- usage
  ([] (usage nil))
  ([msg]
   (when msg
     (binding [*out* *err*]
       (println msg)
       (println "")))
   (println "List article categories available in the DB.")
   (println "")
   (println "Usage:")
   (println "  bb scripts/bb/articles/list_categories.clj [dev|test] [--with-subcategories] [--pretty]")
   (println "")
   (println "Output (default):")
   (println "  EDN vector of {:id .. :name .. :description ..}")
   (println "")
   (println "Flags:")
   (println "  --with-subcategories  Include subcategories grouped under each category")
   (println "  --pretty              Pretty-print EDN output")))

(defn- parse-args
  [args]
  (loop [args args
         parsed {:with-subcategories? false :pretty? false}]
    (let [[a b & more] args]
      (cond
        (nil? a) parsed

        (or (= a "--help") (= a "-h"))
        (do (usage) (System/exit 0))

        (= a "--pretty")
        (recur (cons b more) (assoc parsed :pretty? true))

        (= a "--with-subcategories")
        (recur (cons b more) (assoc parsed :with-subcategories? true))

        :else
        (do (usage (str "Unknown arg: " a)) (System/exit 1))))))

(defn -main
  [& args]
  (let [{:keys [profile args]}          (db/parse-profile args)
        {:keys [with-subcategories? pretty?]} (parse-args args)
        config  (db/read-config profile)
        db-cfg  (:database config)]
    (if with-subcategories?
      ;; Return categories with nested subcategories array
      (let [sql
            (str
              "SELECT\n"
              "  c.id,\n"
              "  c.name,\n"
              "  c.description,\n"
              "  COALESCE(\n"
              "    json_agg(\n"
              "      json_build_object('id', sc.id, 'name', sc.name)\n"
              "      ORDER BY sc.name\n"
              "    ) FILTER (WHERE sc.id IS NOT NULL),\n"
              "    '[]'::json\n"
              "  ) AS subcategories\n"
              "FROM categories c\n"
              "LEFT JOIN subcategories sc ON sc.category_id = c.id\n"
              "GROUP BY c.id, c.name, c.description\n"
              "ORDER BY c.name")
            rows (db/query db-cfg sql)]
        (if pretty? (db/pprint-edn rows) (db/prn-edn rows)))
      ;; Default: flat category list
      (let [sql
            (str
              "SELECT id, name, description\n"
              "FROM categories\n"
              "ORDER BY name")
            rows (db/query db-cfg sql)]
        (if pretty? (db/pprint-edn rows) (db/prn-edn rows))))))

(apply -main *command-line-args*)
