(ns app.domain.backend.expenses.routes.routes-factory
  "Factory functions for generating standard CRUD routes for expenses domain entities.

   This namespace provides generic route generators that eliminate code duplication
   across the expenses domain routes while maintaining flexibility for entity-specific
   behavior through configuration."
  (:require
    [app.template.backend.routes.admin.utils :as utils]
    [app.shared.adapters.database :as shared-db]
    [cheshire.core :as json]
    [clojure.string :as str]
    [taoensso.timbre :as log]))

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

(defn build-list-handler
  "Builds a generic list handler for an entity."
  [{:keys [service _entity-key entity-plural default-limit default-order-by
           custom-query-params transform-response]}]
  (fn [db]
    (utils/with-error-handling
      (fn [request]
        (let [qp (:query-params request)
              custom-params (when custom-query-params (custom-query-params qp))
              query-params (merge {:limit (utils/parse-int-param qp :limit default-limit)
                                   :offset (utils/parse-int-param qp :offset 0)
                                   :order-by (keyword (or (get-param qp :order-by) default-order-by))
                                   :order-dir (keyword (or (get-param qp :order-dir) "asc"))}
                             custom-params)
              list-fn (resolve-service-op-fn service
                        (symbol (str "list-" (name entity-plural)))
                        :list)
              results (list-fn db query-params)
              response-key (or (:response-key transform-response) entity-plural)
              response-data (if (:transform transform-response)
                              ((:transform transform-response) results)
                              (to-app results))]
          (utils/success-response {response-key response-data})))
      (str "Failed to list " (name entity-plural)))))

(defn build-count-handler
  "Builds a generic count handler for an entity."
  [{:keys [service entity-plural custom-count-params]}]
  (fn [db]
    (utils/with-error-handling
      (fn [request]
        (let [qp (:query-params request)
              params (if custom-count-params (custom-count-params qp) {})
              count-fn (resolve-service-op-fn service
                         (symbol (str "count-" (name entity-plural)))
                         :count)
              {:keys [total]} (count-fn db params)]
          (utils/success-response {:total (or total 0)})))
      (str "Failed to count " (name entity-plural)))))

(defn build-create-handler
  "Builds a generic create handler for an entity."
  [{:keys [service entity-key _entity-plural required-fields custom-validation
           transform-request transform-response]}]
  (fn [db]
    (utils/with-error-handling
      (fn [request]
        (let [body (read-json-body request)
              data (if transform-request (transform-request body) body)
              missing (vec (remove #(contains? data %) (or required-fields [])))]
          (if (seq missing)
            (utils/error-response (str "Missing required fields: " (str/join ", " (map name missing))) :status 400)
            (let [create-fn (resolve-service-op-fn service
                              (symbol (str "create-" (name entity-key) "!"))
                              :create!)
                  data* (if custom-validation (custom-validation data) data)
                  created (create-fn db data*)
                  response-key (or (:response-key transform-response) entity-key)
                  response-data (if (:transform transform-response)
                                  ((:transform transform-response) created)
                                  (to-app created))]
              (utils/success-response {response-key response-data} :status 201)))))
      (str "Failed to create " (name entity-key)))))

(defn build-get-handler
  "Builds a generic get-by-id handler for an entity."
  [{:keys [service entity-key custom-get-fn transform-response]}]
  (fn [db]
    (utils/with-error-handling
      (fn [request]
        (let [id (utils/parse-uuid-custom (get-in request [:path-params :id]))
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
  [{:keys [service entity-key _entity-plural transform-request transform-response]}]
  (fn [db]
    (utils/with-error-handling
      (fn [request]
        (let [id (utils/parse-uuid-custom (get-in request [:path-params :id]))
              body (read-json-body request)
              data (if transform-request (transform-request body) body)
              update-fn (resolve-service-op-fn service (symbol (str "update-" (name entity-key) "!")) :update!)
              updated (when id (update-fn db id data))]
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
  [{:keys [service entity-key _entity-plural custom-delete-fn delete-response-type]}]
  (fn [db]
    (utils/with-error-handling
      (fn [request]
        (let [id (utils/parse-uuid-custom (get-in request [:path-params :id]))
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
        by-id ["/:id" {:get ((:get handlers) db)
                       :put ((:update handlers) db)
                       :delete ((:delete handlers) db)}]
        header (if route-meta [route-path route-meta] [route-path])]
    (into header [base by-id])))

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
        id-routes ["/:id" {:get ((:get handlers) db)
                           :put ((:update handlers) db)
                           :delete ((:delete handlers) db)}]
        header (if route-meta [route-path route-meta] [route-path])
        routes (cond-> [base-routes id-routes]
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
   - :custom-query-params - function to transform query parameters
   - :custom-handlers - map of custom handler functions
   - :additional-routes - vector of additional route definitions
   - :has-count? - whether to include count endpoint (default: false)
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
  (let [handlers {:list (build-list-handler config)
                  :count (when (:has-count? config)
                           (build-count-handler config))
                  :create (build-create-handler config)
                  :get (build-get-handler config)
                  :update (build-update-handler config)
                  :delete (build-delete-handler config)
                  :search (when (:has-search? config)
                            (build-search-handler config))}
        ;; Add custom handlers
        handlers* (merge handlers (:custom-handlers config))]
    (assoc config :handlers handlers*)))