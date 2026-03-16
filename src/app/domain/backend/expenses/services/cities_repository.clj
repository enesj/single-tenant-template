(ns app.domain.backend.expenses.services.cities-repository
  "Database access for city lookup and upsert operations."
  (:require
    [app.domain.backend.expenses.services.cities-normalize :as normalize]
    [clojure.string :as str]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]))

(defn find-city-by-normalized-key
  [db normalized-key]
  (when-let [key* (some-> normalized-key str str/trim not-empty)]
    (jdbc/execute-one!
      db
      (sql/format {:select [:*]
                   :from [:cities]
                   :where [:= :normalized_key key*]
                   :limit 1})
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn find-city-by-country-and-zip
  [db country zip]
  (let [country* (or (some-> country str str/trim not-empty)
                   normalize/default-country)
        zip* (normalize/normalize-zip zip)]
    (when zip*
      (jdbc/execute-one!
        db
        (sql/format {:select [:id :name :country :zip]
                     :from [:cities]
                     :where [:and
                             [:= :country country*]
                             [:= :zip zip*]]
                     :limit 1})
        {:builder-fn rs/as-unqualified-lower-maps}))))

(defn find-city-id-by-candidate
  [db candidate]
  (some (fn [normalized-key]
          (some-> (find-city-by-normalized-key db normalized-key)
            :id))
    (normalize/candidate-normalized-keys candidate)))

(defn find-city-by-zip-prefix
  "Find a city by progressively shorter ZIP prefixes (4 → 3 → 2 digits).
   Returns the first city whose ZIP starts with the longest matching prefix."
  [db zip]
  (when-let [zip* (normalize/normalize-zip zip)]
    (some (fn [prefix-len]
            (let [prefix (subs zip* 0 prefix-len)]
              (jdbc/execute-one!
                db
                (sql/format {:select [:id :name :zip]
                             :from [:cities]
                             :where [:like :zip (str prefix "%")]
                             :order-by [[:zip :asc]]
                             :limit 1})
                {:builder-fn rs/as-unqualified-lower-maps})))
      [4 3 2])))

(defn ensure-city-by-country-and-zip!
  [db country zip city-name]
  (let [country* (or (some-> country str str/trim not-empty)
                   normalize/default-country)
        zip* (normalize/normalize-zip zip)
        city-name* (some-> city-name str str/trim not-empty)
        normalized* (some-> city-name* normalize/normalize-city-key)]
    (when (and zip* city-name* normalized*)
      (or
        (some-> (find-city-by-country-and-zip db country* zip*) :id)
        (when-let [by-key (find-city-by-normalized-key db normalized*)]
          (let [existing-country (some-> (:country by-key) str str/trim not-empty)
                existing-zip (some-> (:zip by-key) normalize/normalize-zip)
                compatible? (and (or (not (seq existing-country))
                                   (= existing-country country*))
                              (or (not (seq existing-zip))
                                (= existing-zip zip*)))
                id (:id by-key)]
            (when (and compatible? id)
              (some-> (jdbc/execute-one!
                        db
                        (sql/format {:update :cities
                                     :set {:country country*
                                           :zip zip*}
                                     :where [:= :id id]
                                     :returning [:id]})
                        {:builder-fn rs/as-unqualified-lower-maps})
                :id))))
        (some-> (jdbc/execute-one!
                  db
                  (sql/format {:insert-into :cities
                               :values [{:name city-name*
                                         :normalized_key normalized*
                                         :country country*
                                         :zip zip*
                                         :created_at [:now]}]
                               :returning [:id]})
                  {:builder-fn rs/as-unqualified-lower-maps})
          :id)))))
