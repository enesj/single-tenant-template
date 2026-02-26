(ns app.domain.backend.expenses.services.stores.repo
  (:require
    [app.domain.backend.expenses.services.service-configs :as configs]
    [clojure.string :as str]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]))

(defn get-store
  [db store-id]
  (jdbc/execute-one!
    db
    (sql/format {:select [[:s.*] [:c.name :city]]
                 :from [[:stores :s]]
                 :left-join [[:cities :c] [:= :c.id :s.city_id]]
                 :where [:= :s.id store-id]
                 :limit 1})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn find-by-supplier-and-normalized-key
  [db supplier-id normalized-key]
  (jdbc/execute-one!
    db
    (sql/format {:select [:*]
                 :from [:stores]
                 :where [:and
                         [:= :supplier_id supplier-id]
                         [:= :normalized_key normalized-key]]
                 :limit 1})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn place-id->normalized-key
  [place-id]
  (when-let [place-id* (some-> place-id str str/trim not-empty)]
    (configs/normalize-store-key (str "place " place-id*))))

(defn list-stores-for-supplier
  [db supplier-id]
  (jdbc/execute!
    db
    (sql/format {:select [[:s.id :id]
                          [:s.normalized_key :normalized_key]
                          [:s.display_name :display_name]
                          [:s.address :address]
                          [:c.name :city]]
                 :from [[:stores :s]]
                 :left-join [[:cities :c] [:= :c.id :s.city_id]]
                 :where [:= :s.supplier_id supplier-id]})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- store-match-key [{:keys [normalized_key display_name address]}]
  (let [normalized* (some-> normalized_key str str/trim not-empty)
        address* (some-> address str str/trim not-empty)
        address-key (some-> address* configs/normalize-store-key)
        pj-or-places-key? (and (seq normalized*)
                            (or (str/starts-with? normalized* "pj-")
                              (str/starts-with? normalized* "place-")))
        display-name* (some-> display_name str str/trim not-empty)
        label (->> [display-name* address*]
                (remove str/blank?)
                (str/join " "))
        derived (some-> label not-empty configs/normalize-store-key)]
    (cond
      (and pj-or-places-key? (seq address-key))
      address-key

      (seq normalized*)
      normalized*

      (seq address-key)
      address-key

      (seq derived)
      derived

      :else
      nil)))

(defn store-candidates-for-supplier
  [db supplier-id]
  (mapv (fn [{:keys [id] :as row}]
          {:id id
           :key (store-match-key row)})
    (list-stores-for-supplier db supplier-id)))
