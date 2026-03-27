(ns app.domain.backend.expenses.services.services-factory
  "Generic service factory for expenses domain entities."
  (:require
    [app.shared.query-builders :as shared-qb]
    [clojure.string :as str]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.time ZoneId]
    [java.util UUID]))

(defn- apply-hook
  "Call an optional hook with db-aware args when supported.

  Some configs use hook arities without `db` (legacy), others need `db` to
  perform lookups/side effects. We support both by trying the db-aware arity
  first and falling back."
  [hook primary-args fallback-args]
  (when hook
    (try
      (apply hook primary-args)
      (catch clojure.lang.ArityException _
        (apply hook fallback-args)))))

;; ============================================================================
;; Generic Query Builders
;; ============================================================================

(declare build-where-clause)

(defn- tenant-id-column
  "Return the qualified tenant_id column for a config.
   Uses table-alias when present (e.g. :p.tenant_id), otherwise :tenant_id."
  [{:keys [table-alias]}]
  (if table-alias
    (keyword (name table-alias) "tenant_id")
    :tenant_id))

(defn- inject-tenant-filter
  "Prepend a tenant_id filter to a seq of base-filters when config is tenant-scoped
   and tenant-id is non-nil."
  [{:keys [tenant-scoped?] :as config} tenant-id base-filters]
  (if (and tenant-scoped? tenant-id)
    (into [[:= (tenant-id-column config) tenant-id]] base-filters)
    base-filters))

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
  "Build a generic list function for an entity.
   When config has :tenant-scoped? true, accepts :tenant-id in opts to scope by tenant.
   Accepts :extra-filters in opts — a seq of HoneySQL WHERE clauses to append."
  [{:keys [table-name primary-key joins select-fields allowed-order-by search-fields
           table-alias base-filters text-filter-columns]
    :as config}]
  (fn list-entity
    [db {:keys [limit offset order-by order-dir search tenant-id extra-filters]
         :as opts
         :or {limit 50 offset 0 order-dir :asc}}]
    (let [default-order-by (get config :default-order-by primary-key)
          effective-filters (into (vec (inject-tenant-filter config tenant-id base-filters))
                              (or extra-filters []))
          base-query (build-query-with-filters
                       {:table-name table-name
                        :primary-key primary-key
                        :joins joins
                        :select-fields select-fields
                        :allowed-order-by allowed-order-by
                        :default-order-by default-order-by
                        :table-alias table-alias
                        :base-filters effective-filters}
                       {:limit limit
                        :offset offset
                        :order-by order-by
                        :order-dir order-dir})
          final-query (cond-> base-query
                        (seq text-filter-columns)
                        (shared-qb/apply-text-filters text-filter-columns opts)

                        (and search search-fields)
                        (apply-search-filter search-fields search))]
      (jdbc/execute! db (sql/format final-query)
        {:builder-fn rs/as-unqualified-lower-maps}))))

(defn build-get-function
  "Build a generic get-by-id function for an entity.
   When config has :tenant-scoped? true, accepts optional third arg opts map
   with :tenant-id to scope by tenant."
  [{:keys [table-name primary-key joins select-fields table-alias base-filters]
    :as config}]
  (fn get-entity
    ([db id] (get-entity db id nil))
    ([db id opts]
     (let [tenant-id (:tenant-id opts)
           effective-filters (inject-tenant-filter config tenant-id base-filters)
           query (-> (build-base-query {:table-name table-name
                                        :primary-key primary-key
                                        :joins joins
                                        :select-fields select-fields
                                        :table-alias table-alias
                                        :base-filters effective-filters})
                   (apply-id-filter table-name table-alias id))]
       (jdbc/execute-one! db (sql/format query)
         {:builder-fn rs/as-unqualified-lower-maps})))))

(defn build-create-function
  "Build a generic create function for an entity.
   When config has :tenant-scoped? true, the caller must include :tenant_id in data."
  [{:keys [table-name required-fields field-transformers before-insert tenant-scoped?]
    :or {field-transformers {}}}]
  (fn create-entity!
    [db data]
    ;; Validate tenant_id for tenant-scoped entities
    (when (and tenant-scoped? (nil? (:tenant_id data)))
      (throw (ex-info "tenant_id is required for tenant-scoped entity"
               {:entity table-name})))

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

          ;; Apply before-insert hook (db-aware when supported)
          processed-data (or (apply-hook before-insert
                               [db final-data]
                               [final-data])
                           final-data)
          create-data (dissoc processed-data :updated_at :updated-at)]

      (jdbc/execute-one! db
        (sql/format {:insert-into (keyword table-name)
                     :values [create-data]
                     :returning [:*]})
        {:builder-fn rs/as-unqualified-lower-maps}))))

(defn build-update-function
  "Build a generic update function for an entity.
   When config has :tenant-scoped? true, accepts optional fourth arg opts map
   with :tenant-id to add tenant_id to WHERE clause."
  [{:keys [table-name field-transformers before-update tenant-scoped?]
    :or {field-transformers {}}}]
  (fn update-entity!
    ([db id updates] (update-entity! db id updates nil))
    ([db id updates opts]
     (let [tenant-id (:tenant-id opts)
           ;; Apply field transformers
           transformed-updates (reduce-kv
                                 (fn [acc k v]
                                   (if-let [transformer (get field-transformers k)]
                                     (assoc acc k (transformer v))
                                     acc))
                                 updates
                                 updates)

           ;; Apply before-update hook (db-aware when supported)
           processed-updates (or (apply-hook before-update
                                   [db id transformed-updates]
                                   [id transformed-updates])
                               transformed-updates)
           update-data (dissoc processed-updates :updated_at :updated-at :tenant_id)
           where-clause (if (and tenant-scoped? tenant-id)
                          [:and [:= :id id] [:= :tenant_id tenant-id]]
                          [:= :id id])]

       (when (seq update-data)
         (jdbc/execute-one! db
           (sql/format {:update (keyword table-name)
                        :set update-data
                        :where where-clause
                        :returning [:*]})
           {:builder-fn rs/as-unqualified-lower-maps}))))))

(defn build-delete-function
  "Build a generic delete function for an entity.
   When config has :tenant-scoped? true, accepts optional third arg opts map
   with :tenant-id to scope the DELETE."
  [{:keys [table-name tenant-scoped?]}]
  (fn delete-entity!
    ([db id] (delete-entity! db id nil))
    ([db id opts]
     (let [tenant-id (:tenant-id opts)
           where-clause (if (and tenant-scoped? tenant-id)
                          [:and [:= :id id] [:= :tenant_id tenant-id]]
                          [:= :id id])]
       (pos? (::jdbc/update-count
              (jdbc/execute-one! db
                (sql/format {:delete-from (keyword table-name)
                             :where where-clause}))))))))

(defn build-count-function
  "Build a generic count function for an entity.
   When config has :tenant-scoped? true, accepts :tenant-id in opts map.
   Accepts :extra-filters in opts — a seq of HoneySQL WHERE clauses to append."
  [{:keys [table-name search-fields joins table-alias base-filters text-filter-columns]
    :as config}]
  (fn count-entity
    [db & [opts]]
    (let [search (cond
                   (nil? opts) nil
                   (map? opts) (:search opts)
                   (string? opts) opts
                   :else (throw (ex-info "opts must be a map, string, or nil" {:opts opts})))
          tenant-id (when (map? opts) (:tenant-id opts))
          extra-filters (when (map? opts) (:extra-filters opts))
          effective-filters (into (vec (inject-tenant-filter config tenant-id base-filters))
                              (or extra-filters []))
          where (build-where-clause effective-filters)
          base-query (cond-> {:select [[[:count :*] :total]]
                              :from (if table-alias
                                      [[(keyword table-name) table-alias]]
                                      [(keyword table-name)])}
                       joins (assoc :left-join joins)
                       where (assoc :where where))
          final-query (cond-> base-query
                        (seq text-filter-columns)
                        (shared-qb/apply-text-filters text-filter-columns opts)

                        (and search search-fields)
                        (apply-search-filter search-fields search))]
      (:total
       (jdbc/execute-one! db
         (sql/format final-query)
         {:builder-fn rs/as-unqualified-lower-maps})))))

(defn build-search-function
  "Build a generic search function for autocomplete.
   When config has :tenant-scoped? true, accepts :tenant-id in opts map."
  [{:keys [table-name search-fields order-by-field default-limit joins table-alias base-filters]
    :as config
    :or {order-by-field :display_name default-limit 10}}]
  (fn search-entity
    [db query {:keys [limit tenant-id] :or {limit default-limit}}]
    (when (and query (>= (count query) 2))
      (let [effective-filters (inject-tenant-filter config tenant-id base-filters)
            where (build-where-clause effective-filters)
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

(defn- sql-ident->str
  [ident]
  (cond
    (keyword? ident)
    (if-let [ns-part (namespace ident)]
      (str ns-part "." (name ident))
      (name ident))

    (string? ident)
    ident

    :else
    (str ident)))

(defn- safe-timezone-id
  [timezone]
  (when-not (str/blank? (str (or timezone "")))
    (try
      (.getId (ZoneId/of (str timezone)))
      (catch Exception _
        nil))))

(defn- local-day-expression
  [timezone sql-column]
  (let [tz-id (safe-timezone-id timezone)
        sql-col (sql-ident->str sql-column)]
    (when (and tz-id (not (str/blank? sql-col)))
      (str "to_char(timezone('"
        (str/replace tz-id "'" "''")
        "', "
        sql-col
        ")::date, 'YYYY-MM-DD')"))))

(defn build-highlight-days-function
  "Build a generic function that returns distinct local-day keys for a timestamp column.
   The caller provides :highlight-column and :highlight-timezone in opts."
  [{:keys [table-name search-fields joins table-alias base-filters]
    :as config}]
  (fn highlight-days
    [db {:keys [search tenant-id extra-filters highlight-column highlight-timezone]}]
    (when-let [day-expr (local-day-expression highlight-timezone highlight-column)]
      (let [effective-filters (into (vec (inject-tenant-filter config tenant-id base-filters))
                                (or extra-filters []))
            where (build-where-clause effective-filters)
            base-query (cond-> {:select [[[:raw day-expr] :day]]
                                :from (if table-alias
                                        [[(keyword table-name) table-alias]]
                                        [(keyword table-name)])}
                         joins (assoc :left-join joins)
                         where (assoc :where where))
            final-query (cond-> (apply-search-filter base-query search-fields search)
                          true (assoc :group-by [[:raw day-expr]])
                          true (assoc :order-by [[[:raw day-expr] :asc]]))]
        (->> (jdbc/execute! db (sql/format final-query)
               {:builder-fn rs/as-unqualified-lower-maps})
          (keep :day)
          vec)))))

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
                     :count (build-count-function config)
                     :highlight-days (build-highlight-days-function config)}]

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
