#!/usr/bin/env bb

(ns scripts.bb.expenses.list-unmapped-article-aliases
  (:require
    [aero.core :as aero]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]
    [honey.sql :as sql]
    [clojure.string :as str]
    [clojure.data.json :as json]))

(defn- datasource-from-config [config]
  (let [{:keys [host port dbname user password]} (:database config)]
    (jdbc/get-datasource {:dbtype "postgresql"
                          :host host
                          :port port
                          :dbname dbname
                          :user user
                          :password password})))

(defn -main [& args]
  (let [profile (or (some-> args first keyword) :dev)
        config (aero/read-config "config/base.edn" {:profile profile})
        ds (datasource-from-config config)

        ;; Get receipts with extracted data to find article aliases
        receipts-with-data
        (jdbc/execute! ds
          (sql/format {:select [:r.id
                                :r.supplier_guess
                                :r.raw_extract_json]
                       :from [[:receipts :r]]
                       :where [:not [:= :r.raw_extract_json nil]]
                       :limit 20})
          {:builder-fn rs/as-unqualified-lower-maps})]

    (println "Found" (count receipts-with-data) "receipts with extracted data:\n")
    ;; Extract all unique article aliases from receipts
    (let [all-aliases
          (->> receipts-with-data
            (mapcat (fn [{:keys [id supplier_guess raw_extract_json]}]
                      (when raw_extract_json
                        (try
                          (let [json-str (str raw_extract_json)
                                extracted (json/read-str json-str :key-fn keyword)
                                items (get-in extracted [:extraction :items])]
                            (map (fn [{:keys [raw_label]}]
                                   {:raw_label raw_label
                                    :supplier supplier_guess})
                              items))
                          (catch Exception e
                            [])))))
            (remove nil?)
            (distinct)
            (sort-by :raw_label))]
      (println "Found" (count all-aliases) "unique article aliases:\n")
      (doseq [{:keys [raw_label supplier]} all-aliases]
        (println (str "- " raw_label " (" supplier ")"))))))

(apply -main *command-line-args*)
