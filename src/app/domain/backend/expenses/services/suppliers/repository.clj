(ns app.domain.backend.expenses.services.suppliers.repository
  (:require
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]))

(def ^:private supplier-order-by
  {:display-name :display_name
   :display_name :display_name
   :normalized-key :normalized_key
   :normalized_key :normalized_key
   :address :address
   :created-at :created_at
   :created_at :created_at
   :updated-at :updated_at
   :updated_at :updated_at})

(defn- suppliers-where
  [{:keys [search]}]
  (let [search-filter (when (seq search)
                        [:or
                         [:ilike :display_name (str "%" search "%")]
                         [:ilike :normalized_key (str "%" search "%")]])
        filters (->> [search-filter]
                  (remove nil?))]
    (when (seq filters)
      (into [:and] filters))))

(defn list-suppliers
  [db {:keys [limit offset order-by order-dir search]
       :or {limit 50 offset 0 order-dir :asc}}]
  (let [order-column (get supplier-order-by order-by :display_name)
        order-direction (if (= :asc order-dir) :asc :desc)
        where (suppliers-where {:search search})]
    (jdbc/execute!
      db
      (sql/format
        (cond-> {:select [:*]
                 :from [:suppliers]
                 :limit limit
                 :offset offset
                 :order-by [[order-column order-direction]]}
          where (assoc :where where)))
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn count-suppliers
  [db {:keys [search]}]
  (let [where (suppliers-where {:search search})]
    (:total
     (jdbc/execute-one!
       db
       (sql/format
         (cond-> {:select [[[:count :*] :total]]
                  :from [:suppliers]}
           where (assoc :where where)))
       {:builder-fn rs/as-unqualified-lower-maps}))))

(defn delete-supplier!
  [db supplier-id]
  (jdbc/execute-one!
    db
    (sql/format {:delete-from :suppliers
                 :where [:= :id supplier-id]
                 :returning [:*]})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn find-by-normalized-key
  [db normalized-key]
  (when normalized-key
    (jdbc/execute-one!
      db
      (sql/format {:select [:*]
                   :from [:suppliers]
                   :where [:= :normalized_key normalized-key]
                   :limit 1})
      {:builder-fn rs/as-unqualified-lower-maps})))
