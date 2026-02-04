#!/usr/bin/env clj

(ns scripts.bb.database.delete-stores
  "Delete all rows from the stores table and detach them from store_aliases.

  Safety:
  - Default is dry-run (no DB writes)
  - Requires --apply to perform deletion
  - Prompts for a confirmation phrase unless --yes is provided

  Usage:
    bb delete-stores [--dev|--test|dev|test] [--apply] [--yes]

  Notes:
  - Sets store_aliases.store_id = NULL before deleting stores
  - Deleting from `stores` will set NULL in referencing tables (e.g. expenses.store_id)
    due to FK `ON DELETE SET NULL`"
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
   (println "  bb delete-stores [--dev|--test|dev|test] [--apply] [--yes]")
   (println "")
   (println "Default is dry-run (no DB writes).")
   (println "")
   (println "Examples:")
   (println "  bb delete-stores --dev")
   (println "  bb delete-stores --dev --apply")
   (println "  bb delete-stores test --apply --yes")))

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
       "  (SELECT count(*) FROM stores) AS stores,\n"
       "  (SELECT count(*) FROM store_aliases) AS store_aliases,\n"
       "  (SELECT count(*) FROM store_aliases WHERE store_id IS NOT NULL) AS store_aliases_mapped,\n"
       "  (SELECT count(*) FROM expenses WHERE store_id IS NOT NULL) AS expenses_with_store_id")]
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- update-count
  [result]
  (or (get result :next.jdbc/update-count)
      (get result :update-count)
      0))

(defn- confirm!
  [{:keys [profile dbname]}]
  (println (str "⚠️  DANGER: This will DELETE ALL rows from stores in the " (name profile) " database!"))
  (println "   It will also unmap store_aliases by setting store_id = NULL.")
  (println (str "🎯 Target DB: " dbname))
  (println "")
  (print "Type 'DELETE STORES' to confirm: ")
  (flush)
  (= "DELETE STORES" (str/trim (read-line))))

(defn -main
  [& args]
  (let [{:keys [profile apply? yes?]} (parse-args args)
        config (aero/read-config "config/base.edn" {:profile profile})
        ds (datasource-from-config config)
        dbname (get-in config [:database :dbname])
        before (stats ds)]
    (println (str "[" (Instant/now) "]"))
    (println "Delete stores (unmap store_aliases)")
    (println "  profile:" (name profile))
    (println "  dbname:  " dbname)
    (println "  dry-run?:" (not apply?))
    (println "")
    (println "Before:")
    (println "  stores:" (:stores before))
    (println "  store_aliases:" (:store_aliases before))
    (println "  store_aliases (mapped):" (:store_aliases_mapped before))
    (println "  expenses (with store_id):" (:expenses_with_store_id before))
    (println "")

    (when-not apply?
      (println "Dry-run only. Re-run with --apply to unmap aliases and delete all stores.")
      (System/exit 0))

    (when-not (or yes? (confirm! {:profile profile :dbname dbname}))
      (println "❌ Cancelled.")
      (System/exit 1))

    (println "Unmapping store_aliases (setting store_id = NULL)...")
    (let [result (jdbc/execute-one! ds ["UPDATE store_aliases SET store_id = NULL WHERE store_id IS NOT NULL"])
          unmapped (update-count result)]
      (println "✅ Unmapped store_aliases:" unmapped))

    (println "Deleting stores...")
    (let [result (jdbc/execute-one! ds ["DELETE FROM stores"])
          deleted (update-count result)
          after (stats ds)]
      (println "✅ Done")
      (println "  deleted stores:" deleted)
      (println "")
      (println "After:")
      (println "  stores:" (:stores after))
      (println "  store_aliases:" (:store_aliases after))
      (println "  store_aliases (mapped):" (:store_aliases_mapped after))
      (println "  expenses (with store_id):" (:expenses_with_store_id after))
      (System/exit 0))))

(apply -main *command-line-args*)

