#!/usr/bin/env bb

(ns scripts.bb.articles.list-aliases-from-receipts
  (:require
    [articles.db :as db]))

(defn- usage
  ([] (usage nil))
  ([msg]
   (when msg
     (binding [*out* *err*]
       (println msg)
       (println "")))
   (println "Extract distinct raw_label strings from receipts.raw_extract_json.")
   (println "")
   (println "Usage:")
   (println "  bb scripts/bb/articles/list_aliases_from_receipts.clj [dev|test] [--limit N] [--pretty]")
   (println "")
   (println "Notes:")
   (println "  - This is a fallback. Preferred source of truth is `article_aliases`.")
   (println "  - Output is EDN (vector of {:raw_label .. :supplier_guess ..}).")))

(defn- parse-args
  [args]
  (loop [args args
         parsed {:limit 500
                 :pretty? false}]
    (let [[a b & more] args]
      (cond
        (nil? a) parsed

        (or (= a "--help") (= a "-h"))
        (do (usage) (System/exit 0))

        (= a "--pretty")
        (recur (cons b more) (assoc parsed :pretty? true))

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

(defn -main
  [& args]
  (let [{:keys [profile args]} (db/parse-profile args)
        {:keys [limit pretty?]} (parse-args args)
        config (db/read-config profile)
        db (:database config)
        sql (str
              "WITH x AS (\n"
              "  SELECT\n"
              "    jsonb_array_elements_text(\n"
              "      jsonb_path_query_array(raw_extract_json, '$.extraction.items[*].raw_label')\n"
              "    ) AS raw_label,\n"
              "    supplier_guess\n"
              "  FROM receipts\n"
              "  WHERE raw_extract_json IS NOT NULL\n"
              ")\n"
              "SELECT raw_label, supplier_guess\n"
              "FROM x\n"
              "GROUP BY raw_label, supplier_guess\n"
              "ORDER BY raw_label\n"
              "LIMIT " limit)
        rows (db/query db sql)]
    (if pretty?
      (db/pprint-edn rows)
      (db/prn-edn rows))))

(apply -main *command-line-args*)
