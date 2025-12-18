(ns app.template.backend.metadata.service.query-builder
  (:require
    [app.shared.model-naming :as model-naming]
    [app.template.backend.crud.protocols :as crud-protocols]
    [app.template.backend.metadata.service.metadata-service :as ms]
    [clojure.string :as str]))

(defn- safe-sql-name
  [k]
  (-> (name k) (str/replace "-" "_")))

(defn- normalize-order-token
  [entity-def token]
  (if (keyword? token)
    (let [token-name (name token)]
      (if (str/includes? token-name ".")
        token
        (or (ms/db-field-key entity-def token) token)))
    token))

(defn- normalize-order-entry
  [entity-def entry]
  (cond
    (keyword? entry) (normalize-order-token entity-def entry)
    (vector? entry)
    (let [[field & rest] entry]
      (if (keyword? field)
        (into [(normalize-order-token entity-def field)] rest)
        entry))
    :else entry))

(defn- normalize-order-by
  [models entity-key order-by]
  (let [entity-def (ms/entity-definition* models entity-key)]
    (cond
      (nil? order-by) nil
      (keyword? order-by) [(normalize-order-token entity-def order-by)]
      (vector? order-by) (mapv #(normalize-order-entry entity-def %) order-by)
      (sequential? order-by) (map #(normalize-order-entry entity-def %) order-by)
      :else order-by)))

(defn- build-foreign-key-joins
  "Add foreign key joins to a query based on entity metadata."
  [query-builder entity-key base-query]
  (let [models (:models query-builder)
        foreign-keys (crud-protocols/get-foreign-keys query-builder entity-key)
        db-entity (or (ms/db-entity-key models entity-key) entity-key)
        table-name (safe-sql-name db-entity)]
    (if (empty? foreign-keys)
      base-query
      (let [select-fields (concat
                            [:*]
                            (mapcat (fn [fk]
                                      (let [field (:field fk)
                                            db-field (:db/field fk)
                                            foreign-table (:foreign-table fk)
                                            db-foreign-table (:db/foreign-table fk)
                                            alias-name (safe-sql-name (or db-field field))
                                            table-alias (keyword (str (safe-sql-name (or db-foreign-table foreign-table)) "_join"))]
                                        [[(keyword (str (name table-alias) ".id")) (keyword (str alias-name "_id"))]
                                         [(keyword (str (name table-alias) ".name")) (keyword (str alias-name "_name"))]]))
                               foreign-keys))

            join-clauses (mapcat (fn [fk]
                                   (let [field (:field fk)
                                         db-field (:db/field fk)
                                         foreign-table (:foreign-table fk)
                                         db-foreign-table (:db/foreign-table fk)
                                         foreign-field (:foreign-field fk)
                                         db-foreign-field (:db/foreign-field fk)
                                         join-table-name (keyword (safe-sql-name (or db-foreign-table foreign-table)))
                                         table-alias (keyword (str (name join-table-name) "_join"))
                                         join-table (keyword (str table-name "." (safe-sql-name (or db-field field))))
                                         join-with (keyword (str (name table-alias) "." (safe-sql-name (or db-foreign-field foreign-field))))]
                                     [[join-table-name table-alias]
                                      [:= join-table join-with]]))
                           foreign-keys)]

        (-> base-query
          (assoc :select select-fields)
          (assoc :left-join join-clauses))))))

(defrecord TemplateQueryBuilder [models]
  crud-protocols/QueryBuilder

  (build-select-query [this entity-key opts]
    (let [db-table (or (ms/db-entity-key models entity-key) entity-key)
          from-table (keyword (safe-sql-name db-table))
          raw-filters (:filters opts)
          filters (when (map? raw-filters)
                    (model-naming/app-filters->db models entity-key raw-filters))
          where-clause (when (seq filters)
                         (let [preds (map (fn [[field value]]
                                            [:= field value])
                                        filters)]
                           (if (= 1 (count preds))
                             (first preds)
                             (into [:and] preds))))
          order-by (normalize-order-by models entity-key (:order-by opts))
          base-query (cond-> {:select [:*]
                              :from [from-table]}
                       where-clause (assoc :where where-clause)
                       order-by (assoc :order-by order-by)
                       (:limit opts) (assoc :limit (:limit opts))
                       (:offset opts) (assoc :offset (:offset opts)))
          final-query (if (:include-joins? opts)
                        (build-foreign-key-joins this entity-key base-query)
                        base-query)]
      final-query))

  (build-insert-query [_ entity-key data]
    (let [db-table (or (ms/db-entity-key models entity-key) entity-key)
          db-data (model-naming/app-map->db models entity-key data)]
      {:insert-into [(keyword (safe-sql-name db-table))]
       :values [(into {} (remove (comp nil? val)) db-data)]
       :returning [:*]}))

  (build-update-query [_ entity-key item-id data]
    (let [db-table (or (ms/db-entity-key models entity-key) entity-key)
          db-data (model-naming/app-map->db models entity-key data)]
      {:update [(keyword (safe-sql-name db-table))]
       :set (into {} (remove (comp nil? val)) db-data)
       :where [:= :id [:cast item-id :uuid]]
       :returning [:*]}))

  (build-delete-query [_ entity-key item-id]
    (let [db-table (or (ms/db-entity-key models entity-key) entity-key)]
      {:delete-from [(keyword (safe-sql-name db-table))]
       :where [:= :id [:cast item-id :uuid]]
       :returning [:id]})))
