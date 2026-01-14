(ns app.domain.backend.expenses.services.services-factory
  "Generic service factory for expenses domain entities."
  (:require
    [app.shared.query-builders :as shared-qb]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.util UUID]))

;; ============================================================================
;; Generic Query Builders
;; ============================================================================

(declare build-where-clause)

(defn build-base-query
  "Build a base query with joins for an entity."
  [{:keys [table-name _primary-key joins select-fields table-alias base-filters]}]
  (let [base-select (or select-fields [:*])
        from-target (if table-alias
                      [[(keyword table-name) table-alias]]
                      [(keyword table-name)])
        where (build-where-clause base-filters)]
    (cond-> {:select base-select
             :from from-target}
      joins (assoc :left-join joins)
      where (assoc :where where))))

(defn build-where-clause
  "Build where clause from filters."
  [base-filters]
  (when (seq base-filters)
    (into [:and] base-filters)))

(defn build-query-with-filters
  "Build complete query with filters, ordering, and pagination."
  [{:keys [table-name primary-key joins select-fields allowed-order-by table-alias base-filters]
    :as config}
   {:keys [limit offset order-by order-dir]
    :or {limit 50 offset 0 order-dir :asc}}]
  (let [default-order-by (get config :default-order-by primary-key)
        order-by-col (or order-by default-order-by)
        base-query (build-base-query {:table-name table-name
                                      :primary-key primary-key
                                      :joins joins
                                      :select-fields select-fields
                                      :table-alias table-alias
                                      :base-filters base-filters})
        order-column (get allowed-order-by order-by-col default-order-by)
        order-direction (shared-qb/normalize-order-direction order-dir {:default :asc})]
    (-> base-query
      (shared-qb/apply-pagination {:limit limit :offset offset}
        {:default-limit 50
         :default-offset 0})
      (shared-qb/apply-order-by order-column order-direction))))

(defn apply-search-filter
  "Apply search filter to query if search term provided."
  [query search-fields search-term]
  (if (and search-term (seq search-fields))
    (shared-qb/apply-search-where query search-fields (str "%" search-term "%"))
    query))

(defn apply-id-filter
  "Apply ID filter to query."
  [query table-name table-alias id]
  (let [id-col (if table-alias
                 (keyword (name table-alias) "id")
                 (keyword (str table-name ".id")))
        id-filter [:= id-col id]]
    (update query :where (fn [existing]
                           (if existing
                             [:and existing id-filter]
                             id-filter)))))

;; ============================================================================
;; Generic CRUD Operations
;; ============================================================================

(defn build-list-function
  "Build a generic list function for an entity."
  [{:keys [table-name primary-key joins select-fields allowed-order-by search-fields table-alias base-filters]
    :as config}]
  (fn list-entity
    [db {:keys [limit offset order-by order-dir search]
         :or {limit 50 offset 0 order-dir :asc}}]
    (let [default-order-by (get config :default-order-by primary-key)
          base-query (build-query-with-filters
                       {:table-name table-name
                        :primary-key primary-key
                        :joins joins
                        :select-fields select-fields
                        :allowed-order-by allowed-order-by
                        :default-order-by default-order-by
                        :table-alias table-alias
                        :base-filters base-filters}
                       {:limit limit
                        :offset offset
                        :order-by order-by
                        :order-dir order-dir})
          final-query (if (and search search-fields)
                        (apply-search-filter base-query search-fields search)
                        base-query)]
      (jdbc/execute! db (sql/format final-query)
        {:builder-fn rs/as-unqualified-lower-maps}))))

(defn build-get-function
  "Build a generic get-by-id function for an entity."
  [{:keys [table-name primary-key joins select-fields table-alias base-filters]}]
  (fn get-entity
    [db id]
    (let [query (-> (build-base-query {:table-name table-name
                                       :primary-key primary-key
                                       :joins joins
                                       :select-fields select-fields
                                       :table-alias table-alias
                                       :base-filters base-filters})
                  (apply-id-filter table-name table-alias id))]
      (jdbc/execute-one! db (sql/format query)
        {:builder-fn rs/as-unqualified-lower-maps}))))

(defn build-create-function
  "Build a generic create function for an entity."
  [{:keys [table-name required-fields field-transformers before-insert]
    :or {field-transformers {}}}]
  (fn create-entity!
    [db data]
    ;; Validate required fields
    (doseq [field required-fields]
      (when-not (get data field)
        (throw (ex-info (str (name field) " is required")
                 {:entity table-name :missing-field field}))))

    ;; Apply field transformers
    (let [transformed-data (reduce-kv
                             (fn [acc k v]
                               (if-let [transformer (get field-transformers k)]
                                 (assoc acc k (transformer v))
                                 acc))
                             data
                             data)

          ;; Add generated ID if not present
          final-data (assoc transformed-data
                       :id (or (:id transformed-data) (UUID/randomUUID)))

          ;; Apply before-insert hook
          processed-data (if before-insert
                           (before-insert final-data)
                           final-data)]

      (jdbc/execute-one! db
        (sql/format {:insert-into (keyword table-name)
                     :values [processed-data]
                     :returning [:*]})
        {:builder-fn rs/as-unqualified-lower-maps}))))

(defn build-update-function
  "Build a generic update function for an entity."
  [{:keys [table-name field-transformers before-update]
    :or {field-transformers {}}}]
  (fn update-entity!
    [db id updates]
    (let [;; Apply field transformers
          transformed-updates (reduce-kv
                                (fn [acc k v]
                                  (if-let [transformer (get field-transformers k)]
                                    (assoc acc k (transformer v))
                                    acc))
                                updates
                                updates)

          ;; Apply before-update hook
          processed-updates (if before-update
                              (before-update id transformed-updates)
                              transformed-updates)]

      (when (seq processed-updates)
        (jdbc/execute-one! db
          (sql/format {:update (keyword table-name)
                       :set processed-updates
                       :where [:= :id id]
                       :returning [:*]})
          {:builder-fn rs/as-unqualified-lower-maps})))))

(defn build-delete-function
  "Build a generic delete function for an entity."
  [{:keys [table-name]}]
  (fn delete-entity!
    [db id]
    (pos? (::jdbc/update-count
           (jdbc/execute-one! db
             (sql/format {:delete-from (keyword table-name)
                          :where [:= :id id]}))))))

(defn build-count-function
  "Build a generic count function for an entity."
  [{:keys [table-name search-fields joins table-alias base-filters]}]
  (fn count-entity
    [db & [opts]]
    (let [search (cond
                   (map? opts) (:search opts)
                   (string? opts) opts
                   :else nil)
          where (build-where-clause base-filters)
          base-query (cond-> {:select [[[:count :*] :total]]
                              :from (if table-alias
                                      [[(keyword table-name) table-alias]]
                                      [(keyword table-name)])}
                       joins (assoc :left-join joins)
                       where (assoc :where where))
          final-query (apply-search-filter base-query search-fields search)]
      (:total
       (jdbc/execute-one! db
         (sql/format final-query)
         {:builder-fn rs/as-unqualified-lower-maps})))))

(defn build-search-function
  "Build a generic search function for autocomplete."
  [{:keys [table-name search-fields order-by-field default-limit joins table-alias base-filters]
    :or {order-by-field :display_name default-limit 10}}]
  (fn search-entity
    [db query {:keys [limit] :or {limit default-limit}}]
    (when (and query (>= (count query) 2))
      (let [where (build-where-clause base-filters)
            base-query (cond-> {:select [:*]
                                :from (if table-alias
                                        [[(keyword table-name) table-alias]]
                                        [(keyword table-name)])}
                         joins (assoc :left-join joins)
                         where (assoc :where where))
            query (-> base-query
                    (apply-search-filter search-fields query)
                    (assoc :order-by [[order-by-field :asc]]
                      :limit limit))]
        (jdbc/execute! db (sql/format query)
          {:builder-fn rs/as-unqualified-lower-maps})))))

;; ============================================================================
;; Service Builder
;; ============================================================================

(defn build-entity-service
  "Build complete service for an entity with all CRUD operations."
  [config]
  (let [service-map {:list (build-list-function config)
                     :get (build-get-function config)
                     :create! (build-create-function config)
                     :update! (build-update-function config)
                     :delete! (build-delete-function config)
                     :count (build-count-function config)}]

    ;; Add search function if search fields specified
    (if (:search-fields config)
      (assoc service-map :search (build-search-function config))
      service-map)))

(defn register-entity-service!
  "Register entity service and return config for validation."
  [config]
  ;; Validate required config keys
  (when-not (:table-name config)
    (throw (ex-info "table-name is required" {:config config})))
  (when-not (:primary-key config)
    (throw (ex-info "primary-key is required" {:config config})))

  ;; Log service registration
  (println "✓ Registered service for table:" (:table-name config))

  config)
