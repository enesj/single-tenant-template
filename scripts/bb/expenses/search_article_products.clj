#!/usr/bin/env bb

(ns scripts.bb.expenses.search-article-products
  (:require
    [aero.core :as aero]
    [clojure.data.json :as json]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]))

(defn- datasource-from-config [config]
  (let [{:keys [host port dbname user password]} (:database config)]
    (jdbc/get-datasource {:dbtype "postgresql"
                          :host host
                          :port port
                          :dbname dbname
                          :user user
                          :password password})))

(def article-aliases-to-search
  "List of article aliases to search for on the web.
   Each entry: {:raw_label string :supplier string}"
  [])

(defn -main [& args]
  (let [profile (or (some-> args first keyword) :dev)
        config (aero/read-config "config/base.edn" {:profile profile})
        ds (datasource-from-config config)

        ;; Get receipts with extracted data
        receipts-with-data
        (jdbc/execute! ds
          (sql/format {:select [:r.id
                                :r.supplier_guess
                                :r.raw_extract_json]
                       :from [[:receipts :r]]
                       :where [:not [:= :r.raw_extract_json nil]]
                       :limit 20})
          {:builder-fn rs/as-unqualified-lower-maps})

        ;; Extract all unique article aliases
        all-aliases
        (->> receipts-with-data
          (mapcat (fn [{:keys [supplier_guess raw_extract_json]}]
                    (when raw_extract_json
                      (try
                        (let [json-str (str raw_extract_json)
                              extracted (json/read-str json-str :key-fn keyword)
                              items (get-in extracted [:extraction :items])]
                          (map (fn [{:keys [raw_label]}]
                                 {:raw_label raw_label
                                  :supplier supplier_guess})
                            items))
                        (catch Exception _e
                          [])))))
          (remove nil?)
          (distinct)
          (sort-by :raw_label))]

    (println "Found" (count all-aliases) "unique article aliases:\n")
    (println "Article aliases ready for web search:")
    (println "=")
    (doseq [{:keys [raw_label supplier]} all-aliases]
      (println (str "- Alias: \"" raw_label "\""))
      (println (str "  Supplier: " supplier))
      (println ""))))

(apply -main *command-line-args*)
