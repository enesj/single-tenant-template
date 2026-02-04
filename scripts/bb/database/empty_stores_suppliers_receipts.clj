#!/usr/bin/env clj

(ns scripts.bb.database.empty-stores-suppliers-receipts
  "Empty supplier/store/receipt tables.

  Tables targeted (requested):
  - suppliers
  - supplier_aliases
  - stores
  - store_aliases
  - receipts

  Important:
  - `expenses.supplier_id` has an FK with `ON DELETE RESTRICT` + NOT NULL.
    That means suppliers cannot be deleted while expenses exist.
  - Therefore this script ALSO deletes:
    - expense_items
    - expenses

  Safety:
  - Default is dry-run (no DB writes)
  - Requires --apply to perform deletion
  - Prompts for a confirmation phrase unless --yes is provided

  Usage:
    clj -M scripts/bb/database/empty_stores_suppliers_receipts.clj [--dev|--test|dev|test] [--apply] [--yes]"
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
   (println "  clj -M scripts/bb/database/empty_stores_suppliers_receipts.clj [--dev|--test|dev|test] [--apply] [--yes]")
   (println "")
   (println "Default is dry-run (no DB writes).")))

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
       "  (SELECT count(*) FROM suppliers) AS suppliers,\n"
       "  (SELECT count(*) FROM supplier_aliases) AS supplier_aliases,\n"
       "  (SELECT count(*) FROM stores) AS stores,\n"
       "  (SELECT count(*) FROM store_aliases) AS store_aliases,\n"
       "  (SELECT count(*) FROM receipts) AS receipts,\n"
       "  (SELECT count(*) FROM expenses) AS expenses,\n"
       "  (SELECT count(*) FROM expense_items) AS expense_items")]
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- update-count
  [result]
  (or (get result :next.jdbc/update-count)
    (get result :update-count)
    0))

(defn- confirm!
  [{:keys [profile dbname]}]
  (println (str "⚠️  DANGER: This will DELETE ALL rows from suppliers/stores/aliases/receipts in the " (name profile) " database!"))
  (println "   It will also DELETE expenses + expense_items (FK dependency).")
  (println (str "🎯 Target DB: " dbname))
  (println "")
  (print "Type 'EMPTY RECEIPTS DOMAIN' to confirm: ")
  (flush)
  (= "EMPTY RECEIPTS DOMAIN" (str/trim (read-line))))

(defn -main
  [& args]
  (let [{:keys [profile apply? yes?]} (parse-args args)
        config (aero/read-config "config/base.edn" {:profile profile})
        ds (datasource-from-config config)
        dbname (get-in config [:database :dbname])
        before (stats ds)]

    (println (str "[" (Instant/now) "]"))
    (println "Empty suppliers/stores/aliases/receipts (and dependent expenses)")
    (println "  profile:" (name profile))
    (println "  dbname:  " dbname)
    (println "  dry-run?:" (not apply?))
    (println "")
    (println "Before:")
    (println "  suppliers:" (:suppliers before))
    (println "  supplier_aliases:" (:supplier_aliases before))
    (println "  stores:" (:stores before))
    (println "  store_aliases:" (:store_aliases before))
    (println "  receipts:" (:receipts before))
    (println "  expenses:" (:expenses before))
    (println "  expense_items:" (:expense_items before))
    (println "")

    (when-not apply?
      (println "Dry-run only. Re-run with --apply to delete all rows from these tables.")
      (System/exit 0))

    (when-not (or yes? (confirm! {:profile profile :dbname dbname}))
      (println "❌ Cancelled.")
      (System/exit 1))

    (jdbc/with-transaction [tx ds]
      (println "Deleting expense_items...")
      (println "✅ deleted expense_items:" (update-count (jdbc/execute-one! tx ["DELETE FROM expense_items"])))

      (println "Deleting expenses...")
      (println "✅ deleted expenses:" (update-count (jdbc/execute-one! tx ["DELETE FROM expenses"])))

      (println "Deleting receipts...")
      (println "✅ deleted receipts:" (update-count (jdbc/execute-one! tx ["DELETE FROM receipts"])))

      (println "Deleting store_aliases...")
      (println "✅ deleted store_aliases:" (update-count (jdbc/execute-one! tx ["DELETE FROM store_aliases"])))

      (println "Deleting stores...")
      (println "✅ deleted stores:" (update-count (jdbc/execute-one! tx ["DELETE FROM stores"])))

      (println "Deleting supplier_aliases...")
      (println "✅ deleted supplier_aliases:" (update-count (jdbc/execute-one! tx ["DELETE FROM supplier_aliases"])))

      (println "Deleting suppliers...")
      (println "✅ deleted suppliers:" (update-count (jdbc/execute-one! tx ["DELETE FROM suppliers"]))))

    (let [after (stats ds)]
      (println "")
      (println "After:")
      (println "  suppliers:" (:suppliers after))
      (println "  supplier_aliases:" (:supplier_aliases after))
      (println "  stores:" (:stores after))
      (println "  store_aliases:" (:store_aliases after))
      (println "  receipts:" (:receipts after))
      (println "  expenses:" (:expenses after))
      (println "  expense_items:" (:expense_items after))
      (System/exit 0))))

(apply -main *command-line-args*)
