(ns app.domain.backend.expenses.routes.routes-factory
  "Factory functions for generating standard CRUD routes for expenses domain entities.

   This namespace provides generic route generators that eliminate code duplication
   across the expenses domain routes while maintaining flexibility for entity-specific
   behavior through configuration."
  (:require
    [app.shared.adapters.database :as shared-db]
    [app.shared.model-naming :as model-naming]
    [app.template.backend.routes.admin.utils :as utils]
    [cheshire.core :as json]
    [clojure.string :as str]
    [clojure.walk :as walk]
    [taoensso.timbre :as log])
  (:import
    [java.time Instant LocalDate OffsetDateTime]
    [java.util UUID]))

;; =============================================================================
;; Data Transformation Utilities
;; =============================================================================

(defn to-app
  "Convert DB rows to API-friendly maps."
  [data]
  (shared-db/to-app data))

(defn resolve-fn
  "Require and resolve a var in the given namespace. Accepts namespaced or bare symbols.
   Uses requiring-resolve which handles namespace loading automatically."
  [ns-sym sym]
  (let [target-sym (if (namespace sym)
                     sym
                     (symbol (name ns-sym) (name sym)))
        resolved (requiring-resolve target-sym)]
    (when-not resolved
      (throw (ex-info (str "Could not resolve " target-sym " in namespace " ns-sym)
               {:ns ns-sym :sym sym :target-sym target-sym})))
    resolved))

(defn- maybe-resolve-fn
  "Like resolve-fn, but returns nil when the target var doesn't exist.

  NOTE: still throws if the namespace itself can't be loaded."
  [ns-sym sym]
  (let [target-sym (if (namespace sym)
                     sym
                     (symbol (name ns-sym) (name sym)))]
    (requiring-resolve target-sym)))

(defn- resolve-service-map
  "Resolve the `service` var in a service namespace and return its value.

  Service maps are produced by `services-factory/build-entity-service` and include
  keys like :list/:get/:create!/:update!/:delete!/:count and sometimes :search."
  [service-ns]
  (when-let [service-var (maybe-resolve-fn service-ns 'service)]
    (let [m (var-get service-var)]
      (when (map? m)
        m))))

(defn- resolve-service-op-fn
  "Resolve a handler/service function for a CRUD op.

  Resolution order:
  1) Prefer an explicitly named var (e.g., wrappers/overrides like delete-expense-item!)
  2) Fallback to the `service` map op (allows removing legacy alias vars)

  Throws if neither exists."
  [service-ns legacy-sym service-op]
  (or (maybe-resolve-fn service-ns legacy-sym)
    (when-let [service-map (resolve-service-map service-ns)]
      (get service-map service-op))
    (throw (ex-info (str "Could not resolve " legacy-sym " or service op " service-op
                      " in namespace " service-ns)
             {:ns service-ns
              :legacy-sym legacy-sym
              :service-op service-op}))))

(defn read-json-body
  "Return a parsed request body map.

  Domain admin routes under /admin/api/expenses do not currently run the same JSON
  middleware as the template admin routes, so `:body` may be a raw InputStream.

  This helper supports all of:
  - pre-parsed maps under `:body`
  - Ring JSON middleware under `:body-params`
  - reitit coercion under `:parameters :body`
  - raw JSON request bodies (InputStream/String)"
  [request]
  (let [body (:body request)]
    (cond
      (map? body) body
      (map? (:body-params request)) (:body-params request)
      (map? (get-in request [:parameters :body])) (get-in request [:parameters :body])
      (nil? body) {}
      :else (try
              (json/parse-string (slurp body) true)
              (catch Exception e
                (log/warn e "Failed to parse JSON request body" {:uri (:uri request)})
                {})))))

;; =============================================================================
;; Generic Handler Builders
;; =============================================================================

(defn- get-param
  "Get param from map, trying both keyword and string keys."
  [m k]
  (or (get m k) (get m (if (keyword? k) (name k) (keyword k)))))

(defn- normalize-map-keys->app
  "Normalize the keys of a request/params map into canonical app keywords.

  This is boundary-only normalization:
  - Accepts keyword or string keys.
  - Converts snake_case to kebab-case via `model-naming/ensure-app-keyword`.

  NOTE: If both snake_case and kebab-case versions are provided, the last one
  in iteration order wins."
  [m]
  (when m
    (into (empty m)
      (map (fn [[k v]]
             [(model-naming/ensure-app-keyword k) v]))
      m)))

(defn- normalize-keys-deep->app
  "Recursively normalize map keys into canonical app keywords."
  [data]
  (walk/postwalk
    (fn [v]
      (if (map? v)
        (into (empty v)
          (map (fn [[k vv]]
                 [(model-naming/ensure-app-keyword k) vv]))
          v)
        v))
    (or data {})))

(defn- to-db-deep
  "Recursively convert app keyword keys to DB (snake_case) keyword keys.

  Also performs lightweight boundary coercions so DB writes use correct types.
  In particular, UUID foreign keys often arrive from the frontend as strings
  (e.g. select inputs). When the DB column is UUID, passing a string causes a
  PostgreSQL type error.

  Coercion rules:
  - For keys named \"id\" or ending with \"_id\", if the value is a string:
    - blank string => nil
    - UUID string => java.util.UUID
    - otherwise => leave as-is

  This is best-effort and intentionally conservative: it only changes values
  when it can confidently coerce them."
  [data]
  (walk/postwalk
    (fn [v]
      (if (map? v)
        (into (empty v)
          (map (fn [[k vv]]
                 (let [k* (if (keyword? k)
                            (model-naming/app-keyword->db k)
                            k)
                       k-name (when (keyword? k*) (name k*))
                       vv* (if (and k-name
                                 (or (= k-name "id")
                                   (str/ends-with? k-name "_id"))
                                 (string? vv))
                             (let [s (str/trim vv)]
                               (cond
                                 (str/blank? s) nil
                                 :else (try
                                         (UUID/fromString s)
                                         (catch Exception _
                                           vv))))
                             vv)]
                   [k* vv*])))
          v)
        v))
    (or data {})))

(defn- order-by->app
  "Normalize an order-by value (snake/kebab/string/keyword) into an app keyword."
  [order-by default-order-by]
  (let [v (or order-by default-order-by)]
    (model-naming/ensure-app-keyword v)))

(defn- normalize-count-result
  "Normalize a count function's return value to a number.

  Count functions may return either {:total N} (map) or N (number)."
  [result]
  (cond
    (map? result) (:total result)
    (number? result) result
    :else nil))

(defn- parse-instant-param
  "Lenient parse of a date/time string into java.time.Instant.
   Returns nil for nil, blank, or unparseable input."
  [raw]
  (when-not (str/blank? (str (or raw "")))
    (or (when (instance? Instant raw) raw)
      (try (Instant/parse (str raw)) (catch Exception _ nil))
      (try (-> (OffsetDateTime/parse (str raw)) .toInstant) (catch Exception _ nil))
      (try (-> (LocalDate/parse (str raw)) (.atStartOfDay java.time.ZoneOffset/UTC) .toInstant) (catch Exception _ nil)))))

(defn- extract-date-range-filters
  "Extract date range filters from query params using an allowlist.

  `date-range-columns` is a map of {app-keyword sql-column}, e.g.:
    {:created-at :created_at, :purchased-at :e.purchased_at}

  Detects params matching `<field>-from` / `<field>-to` and returns a vector
  of HoneySQL WHERE clauses like [[:>= :created_at #inst \"2026-03-01T...\"]].

  Values are parsed to java.time.Instant so JDBC binds them as timestamptz.
  Params must match allowlisted column names — arbitrary columns are rejected."
  ([qp date-range-columns]
   (extract-date-range-filters qp date-range-columns nil))
  ([qp date-range-columns exclude-field]
   (when (seq date-range-columns)
     (reduce-kv
       (fn [acc field-key sql-col]
         (if (= field-key exclude-field)
           acc
           (let [field-name (name field-key)
                 from-val (parse-instant-param
                            (or (get qp (keyword (str field-name "-from")))
                              (get qp (str field-name "-from"))))
                 to-val (parse-instant-param
                          (or (get qp (keyword (str field-name "-to")))
                            (get qp (str field-name "-to"))))]
             (cond-> acc
               (some? from-val) (conj [:>= sql-col from-val])
               (some? to-val) (conj [:<= sql-col to-val])))))
       []
       date-range-columns))))

(defn- extract-highlight-request
  [qp date-range-columns]
  (let [field-key (some-> (get-param qp :highlight-date-field) model-naming/ensure-app-keyword)
        timezone (some-> (get-param qp :highlight-timezone) str str/trim not-empty)
        sql-column (get date-range-columns field-key)]
    (when (and field-key timezone sql-column)
      {:field field-key
       :timezone timezone
       :sql-column sql-column})))

(defn- coerce-filter-value
  "Coerce a raw query-param value according to its declared type."
  [qp param-key type]
  (let [raw (get qp param-key)]
    (case type
      :uuid (utils/parse-uuid-custom raw)
      :boolean (utils/parse-boolean-param qp param-key)
      :keyword (some-> raw keyword)
      :int (utils/parse-int-param qp param-key nil)
      ;; :string (default) — pass through
      raw)))

(defn- build-filter-params-fn
  "Generate a query-param extraction function from a declarative filter-params spec.

  Accepts either:
  - A vector of keywords: each is extracted as a string (passthrough).
    e.g. [:search :category-name]
  - A map of {param-key type}: type is :string, :uuid, :boolean, :keyword, :int.
    e.g. {:search :string, :supplier-id :uuid, :unmapped-only :boolean}"
  [filter-params]
  (cond
    (vector? filter-params)
    (fn [qp]
      (reduce (fn [acc k] (assoc acc k (get qp k))) {} filter-params))

    (map? filter-params)
    (fn [qp]
      (reduce-kv
        (fn [acc param-key type]
          (let [type (or type :string)]
            (assoc acc param-key (coerce-filter-value qp param-key type))))
        {}
        filter-params))

    :else nil))

(defn- resolve-query-params-fn
  "Resolve the effective query-params extraction function from config.

  Priority: explicit :custom-query-params > generated from :filter-params > nil."
  [{:keys [custom-query-params filter-params]}]
  (or custom-query-params
    (build-filter-params-fn filter-params)))

(defn build-list-handler
  "Builds a generic list handler for an entity.

  The handler includes :total in the response by default (server-side pagination).
  Set :has-count? false explicitly to disable.

  Filters can be specified declaratively via :filter-params (a vector of keywords
  or a map of {param type}) instead of writing a :custom-query-params function.

  Optional :date-range-columns — a map of {app-keyword sql-column} that enables
  date range filtering via `<field>-from` / `<field>-to` query params."
  [{:keys [service _entity-key entity-plural default-limit default-order-by
           custom-count-params transform-response
           date-range-columns] :as config}]
  (let [has-count? (get config :has-count? true)
        query-params-fn (resolve-query-params-fn config)
        count-params-fn (or custom-count-params query-params-fn)]
    (fn [db]
      (utils/with-error-handling
        (fn [request]
          (let [qp (or (normalize-map-keys->app (:query-params request)) {})
                highlight-request (extract-highlight-request qp date-range-columns)
                date-filters (extract-date-range-filters qp date-range-columns)
                highlight-date-filters (extract-date-range-filters qp date-range-columns (:field highlight-request))
                custom-params (when query-params-fn (query-params-fn qp))
                query-params (cond-> (merge {:limit (utils/parse-int-param qp :limit default-limit)
                                             :offset (utils/parse-int-param qp :offset 0)
                                             :order-by (order-by->app (get-param qp :order-by) default-order-by)
                                             :order-dir (keyword (or (get-param qp :order-dir) "asc"))}
                                       custom-params)
                               (seq date-filters)
                               (update :extra-filters (fn [existing]
                                                        (into (vec (or existing [])) date-filters))))
                list-fn (resolve-service-op-fn service
                          (symbol (str "list-" (name entity-plural)))
                          :list)
                results (list-fn db query-params)
                response-key (or (:response-key transform-response) entity-plural)
                response-data (if (:transform transform-response)
                                ((:transform transform-response) results)
                                (to-app results))
                total (when has-count?
                        (try
                          (let [base-count-params (if count-params-fn (count-params-fn qp) {})
                                count-params (if (seq date-filters)
                                               (update base-count-params :extra-filters
                                                 (fn [existing]
                                                   (into (vec (or existing [])) date-filters)))
                                               base-count-params)
                                count-fn (resolve-service-op-fn service
                                           (symbol (str "count-" (name entity-plural)))
                                           :count)]
                            (normalize-count-result (count-fn db count-params)))
                          (catch Exception e
                            (log/warn e "Failed to get count for" (name entity-plural))
                            nil)))
                date-highlights (when highlight-request
                                  (try
                                    (let [highlight-fn (resolve-service-op-fn service
                                                         (symbol (str "list-" (name entity-plural) "-highlight-days"))
                                                         :highlight-days)
                                          highlight-params (cond-> (merge custom-params
                                                                     {:highlight-column (:sql-column highlight-request)
                                                                      :highlight-timezone (:timezone highlight-request)})
                                                             (seq highlight-date-filters)
                                                             (update :extra-filters (fn [existing]
                                                                                      (into (vec (or existing [])) highlight-date-filters))))]
                                      {(:field highlight-request)
                                       (vec (or (highlight-fn db highlight-params) []))})
                                    (catch Exception e
                                      (log/warn e "Failed to get date highlights for" (name entity-plural))
                                      nil)))
                response (cond-> {response-key response-data}
                           (some? total) (assoc :total total)
                           (map? date-highlights) (assoc :date-highlights date-highlights))]
            (utils/success-response response)))
        (str "Failed to list " (name entity-plural))))))

(defn build-count-handler
  "Builds a generic count handler for an entity."
  [{:keys [service entity-plural custom-count-params] :as config}]
  (let [count-params-fn (or custom-count-params (resolve-query-params-fn config))]
    (fn [db]
      (utils/with-error-handling
        (fn [request]
          (let [qp (or (normalize-map-keys->app (:query-params request)) {})
                params (if count-params-fn (count-params-fn qp) {})
                count-fn (resolve-service-op-fn service
                           (symbol (str "count-" (name entity-plural)))
                           :count)
                {:keys [total]} (count-fn db params)]
            (utils/success-response {:total (or total 0)})))
        (str "Failed to count " (name entity-plural))))))

(defn build-create-handler
  "Builds a generic create handler for an entity."
  [{:keys [service entity-key _entity-plural required-fields custom-validation
           transform-request transform-response]}]
  (fn [db]
    (utils/with-error-handling
      (fn [request]
        (let [body (normalize-keys-deep->app (read-json-body request))
              data (if transform-request (transform-request body) body)
              missing (vec (remove #(contains? data %) (or required-fields [])))]
          (if (seq missing)
            (utils/error-response (str "Missing required fields: " (str/join ", " (map name missing))) :status 400)
            (let [create-fn (resolve-service-op-fn service
                              (symbol (str "create-" (name entity-key) "!"))
                              :create!)
                  data* (if custom-validation (custom-validation data) data)
                  created (create-fn db (to-db-deep data*))
                  response-key (or (:response-key transform-response) entity-key)
                  response-data (if (:transform transform-response)
                                  ((:transform transform-response) created)
                                  (to-app created))]
              (utils/success-response {response-key response-data} :status 201)))))
      (str "Failed to create " (name entity-key)))))

(defn build-get-handler
  "Builds a generic get-by-id handler for an entity."
  [{:keys [service entity-key custom-get-fn transform-response parse-id]}]
  (fn [db]
    (utils/with-error-handling
      (fn [request]
        (let [raw-id (get-in request [:path-params :id])
              id (if parse-id
                   (parse-id raw-id)
                   (utils/parse-uuid-custom raw-id))
              get-fn (or (when custom-get-fn (resolve-fn service custom-get-fn))
                       (resolve-service-op-fn service (symbol (str "get-" (name entity-key))) :get))
              result (when id (get-fn db id))]
          (if result
            (let [response-key (or (:response-key transform-response) entity-key)
                  response-data (if (:transform transform-response)
                                  ((:transform transform-response) result)
                                  (to-app result))]
              (utils/success-response {response-key response-data}))
            (utils/error-response (str (str/capitalize (name entity-key)) " not found") :status 404))))
      (str "Failed to get " (name entity-key)))))

(defn build-update-handler
  "Builds a generic update handler for an entity."
  [{:keys [service entity-key _entity-plural transform-request transform-response parse-id]}]
  (fn [db]
    (utils/with-error-handling
      (fn [request]
        (let [raw-id (get-in request [:path-params :id])
              id (if parse-id
                   (parse-id raw-id)
                   (utils/parse-uuid-custom raw-id))
              body (normalize-keys-deep->app (read-json-body request))
              data (if transform-request (transform-request body) body)
              update-fn (resolve-service-op-fn service (symbol (str "update-" (name entity-key) "!")) :update!)
              updated (when id (update-fn db id (to-db-deep data)))]
          (if updated
            (let [response-key (or (:response-key transform-response) entity-key)
                  response-data (if (:transform transform-response)
                                  ((:transform transform-response) updated)
                                  (to-app updated))]
              (utils/success-response {response-key response-data}))
            (utils/error-response (str (str/capitalize (name entity-key)) " not found") :status 404))))
      (str "Failed to update " (name entity-key)))))

(defn build-delete-handler
  "Builds a generic delete handler for an entity."
  [{:keys [service entity-key _entity-plural custom-delete-fn delete-response-type parse-id]}]
  (fn [db]
    (utils/with-error-handling
      (fn [request]
        (let [raw-id (get-in request [:path-params :id])
              id (if parse-id
                   (parse-id raw-id)
                   (utils/parse-uuid-custom raw-id))
              delete-fn (or (when custom-delete-fn (resolve-fn service custom-delete-fn))
                          (resolve-service-op-fn service (symbol (str "delete-" (name entity-key) "!")) :delete!))
              deleted (when id (delete-fn db id))]
          (if deleted
            (case delete-response-type
              :return-deleted (utils/success-response {(keyword (name entity-key)) (to-app deleted)})
              :deleted (utils/success-response {:deleted true})
              (utils/success-response {:deleted true}))
            (utils/error-response (str (clojure.string/capitalize (name entity-key)) " not found or in use") :status 404))))
      (str "Failed to delete " (name entity-key)))))

(defn build-batch-delete-handler
  "Builds a generic batch delete handler for an entity.

  Expects JSON body like:
  {:ids [<uuid-str> ...]}"
  [{:keys [service entity-key custom-delete-fn parse-id]}]
  (fn [db]
    (utils/with-error-handling
      (fn [request]
        (let [body (normalize-keys-deep->app (read-json-body request))
              raw-ids (or (:ids body) [])
              ids (if parse-id
                    (mapv parse-id raw-ids)
                    (->> raw-ids (map utils/parse-uuid-custom) (filter some?) vec))
              delete-fn (or (when custom-delete-fn (resolve-fn service custom-delete-fn))
                          (resolve-service-op-fn service (symbol (str "delete-" (name entity-key) "!")) :delete!))]
          (cond
            (empty? raw-ids)
            (utils/error-response "No ids provided" :status 400)

            (empty? ids)
            (utils/error-response "One or more ids are invalid" :status 400)

            :else
            (let [deleted-ids (atom [])
                  errors (atom [])]
              (doseq [id ids]
                (try
                  (if (delete-fn db id)
                    (swap! deleted-ids conj (str id))
                    (swap! errors conj {:id (str id)
                                        :error "not found or in use"}))
                  (catch Exception e
                    (swap! errors conj {:id (str id)
                                        :error (.getMessage e)}))))
              (utils/success-response
                {:data {:deleted-count (count @deleted-ids)
                        :deleted-ids (vec @deleted-ids)
                        :errors (vec @errors)}})))))
      (str "Failed to batch delete " (name entity-key)))))

(defn build-search-handler
  "Builds a generic search handler for an entity."
  [{:keys [service entity-plural query-param-name search-fn-name transform-response]}]
  (fn [db]
    (utils/with-error-handling
      (fn [request]
        (let [qp (:query-params request)
              query (get qp query-param-name)
              limit (utils/parse-int-param qp :limit 10)
              search-fn (or (when search-fn-name (resolve-fn service search-fn-name))
                          (resolve-service-op-fn service
                            (symbol (str "search-" (name entity-plural)))
                            :search))
              results (search-fn db query {:limit limit})
              response-key (or (:response-key transform-response) entity-plural)
              response-data (if (:transform transform-response)
                              ((:transform transform-response) results)
                              (to-app results))]
          (utils/success-response {response-key response-data})))
      (str "Failed to search " (name entity-plural)))))

;; =============================================================================
;; Route Builder Functions
;; =============================================================================

(defn build-standard-routes
  "Builds standard CRUD routes for an entity. Honors optional :route-middleware."
  [db config]
  (let [handlers (:handlers config)
        route-path (str "/" (:route-segment config))
        route-meta (when (seq (:route-middleware config))
                     {:middleware (:route-middleware config)})
        base ["" {:get ((:list handlers) db)
                  :post ((:create handlers) db)}]
        batch ["/batch" {:conflicting true
                         :delete ((:batch-delete handlers) db)}]
        by-id ["/:id" {:get ((:get handlers) db)
                       :put ((:update handlers) db)}]
        header (if route-meta [route-path route-meta] [route-path])]
    (into header [base batch by-id])))

(defn build-extended-routes
  "Builds CRUD routes with additional endpoints like count, search, etc. Honors optional :route-middleware."
  [db config]
  (let [handlers (:handlers config)
        additional-routes (:additional-routes config)
        route-path (str "/" (:route-segment config))
        route-meta (when (seq (:route-middleware config))
                     {:middleware (:route-middleware config)})
        base-routes ["" {:get ((:list handlers) db)
                         :post ((:create handlers) db)}]
        batch-routes ["/batch" {:conflicting true
                                :delete ((:batch-delete handlers) db)}]
        id-routes ["/:id" {:get ((:get handlers) db)
                           :put ((:update handlers) db)}]
        header (if route-meta [route-path route-meta] [route-path])
        routes (cond-> [base-routes batch-routes id-routes]
                 (:count handlers) (conj ["/count" {:get ((:count handlers) db)}])
                 (:search handlers) (conj ["/search" {:get ((:search handlers) db)}])
                 true (into (map #(vector % ((:handler %) db)) additional-routes)))]
    (into header routes)))

;; =============================================================================
;; Main Registration Function
;; =============================================================================

(defn register-entity-routes!
  "Registers all standard CRUD routes for an entity based on its configuration.

   Configuration options:
   - :entity-key - singular entity name (e.g., :supplier)
   - :entity-plural - plural entity name (e.g., :suppliers)
   - :service - the service namespace
   - :route-segment - URL path segment
   - :required-fields - vector of required fields for creation
   - :default-limit - default pagination limit
   - :default-order-by - default sort field
   - :custom-validation - function for custom validation logic
   - :filter-params - declarative filter spec: vector of keywords (all string) or
     map of {param-key type} where type is :string, :uuid, :boolean, :keyword, :int
   - :custom-query-params - function to transform query parameters (overrides :filter-params)
   - :custom-handlers - map of custom handler functions
   - :additional-routes - vector of additional route definitions
   - :has-count? - whether to include count endpoint (default: true)
   - :has-search? - whether to include search endpoint (default: false)
   - :route-middleware - vector of reitit-compatible middleware fns applied at the route root"
  [config]
  ;; Validate required configuration
  (when-not (:entity-key config)
    (throw (ex-info "entity-key is required" config)))
  (when-not (:entity-plural config)
    (throw (ex-info "entity-plural is required" config)))
  (when-not (:service config)
    (throw (ex-info "service is required" config)))

  ;; Build standard handlers - resolve-fn uses requiring-resolve for lazy loading
  (let [has-count? (get config :has-count? true)
        handlers {:list (build-list-handler config)
                  :count (when has-count?
                           (build-count-handler config))
                  :create (build-create-handler config)
                  :get (build-get-handler config)
                  :update (build-update-handler config)
                  :delete (build-delete-handler config)
                  :batch-delete (build-batch-delete-handler config)
                  :search (when (:has-search? config)
                            (build-search-handler config))}
        ;; Add custom handlers
        handlers* (merge handlers (:custom-handlers config))]
    (assoc config :handlers handlers*)))