#!/usr/bin/env bb

(ns scripts.bb.articles.list-review-required-receipts
  "List receipts in review_required status with mismatch diagnosis."
  (:require
    [articles.db :as db]))

(defn- usage
  ([] (usage nil))
  ([msg]
   (when msg
     (binding [*out* *err*]
       (println msg)
       (println "")))
   (println "List receipts in review_required status with lines-total mismatch diagnosis.")
   (println "")
   (println "Usage:")
   (println "  bb scripts/bb/articles/list_review_required_receipts.clj [dev|test] [--full] [--pretty]")
   (println "")
   (println "Output fields (default):")
   (println "  id, original_filename, supplier_guess, total_amount_guess,")
   (println "  supplier_alias_id, supplier_id, item_count, items_sum, mismatch")
   (println "")
   (println "Flags:")
   (println "  --full    Also include raw items JSON and parsed_markdown (verbose)")
   (println "  --pretty  Pretty-print EDN output")))

(defn- parse-args
  [args]
  (loop [args args
         parsed {:full? false :pretty? false}]
    (let [[a b & more] args]
      (cond
        (nil? a) parsed

        (or (= a "--help") (= a "-h"))
        (do (usage) (System/exit 0))

        (= a "--pretty")
        (recur (cons b more) (assoc parsed :pretty? true))

        (= a "--full")
        (recur (cons b more) (assoc parsed :full? true))

        :else
        (do (usage (str "Unknown arg: " a)) (System/exit 1))))))

;; items may use either "line_total" (OCR pipeline) or "line-total" (save-review!)
(def ^:private items-sum-subquery
  "(SELECT COALESCE(SUM(
      COALESCE((item->>'line_total')::numeric,
               (item->>'line-total')::numeric, 0)), 0)
    FROM jsonb_array_elements(
      COALESCE(r.raw_extract_json->'extraction'->'items', '[]'::jsonb)
    ) AS item)")

(defn -main
  [& args]
  (let [{:keys [profile args]} (db/parse-profile args)
        {:keys [full? pretty?]} (parse-args args)
        config  (db/read-config profile)
        db-cfg  (:database config)
        sql
        (str
          "SELECT\n"
          "  r.id,\n"
          "  r.original_filename,\n"
          "  r.supplier_guess,\n"
          "  r.total_amount_guess,\n"
          "  r.supplier_alias_id,\n"
          "  sa.supplier_id,\n"
          "  COALESCE(jsonb_array_length(\n"
          "    r.raw_extract_json->'extraction'->'items'), 0) AS item_count,\n"
          "  ROUND((" items-sum-subquery ")::numeric, 2) AS items_sum,\n"
          "  ABS(r.total_amount_guess - (" items-sum-subquery ")) > 0.01 AS mismatch"
          (when full?
            (str
              ",\n  r.raw_extract_json->'extraction'->'items' AS items,\n"
              "  r.parsed_markdown"))
          "\nFROM receipts r\n"
          "LEFT JOIN supplier_aliases sa ON sa.id = r.supplier_alias_id\n"
          "WHERE r.status = 'review_required'\n"
          "ORDER BY r.created_at")
        rows (db/query db-cfg sql)]
    (if pretty?
      (db/pprint-edn rows)
      (db/prn-edn rows))))

(apply -main *command-line-args*)
