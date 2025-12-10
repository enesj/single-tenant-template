(ns app.domain.expenses.routes.routes-factory
  "Factory functions for generating standard CRUD routes for expenses domain entities.

   This namespace provides generic route generators that eliminate code duplication
   across the expenses domain routes while maintaining flexibility for entity-specific
   behavior through configuration."
  (:require
    [app.backend.routes.admin.utils :as utils]
    [app.shared.adapters.database :as db-adapter]))

;; =============================================================================
;; Data Transformation Utilities
;; =============================================================================

(defn to-app
  "Convert DB rows to API-friendly maps."
  [data]
  (-> data
    db-adapter/convert-pg-objects
    db-adapter/convert-db-keys->app-keys))

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
               {:ns ns-sym :sym sym :target target-sym})))
    resolved))

;; =============================================================================
;; Generic Handler Builders
;; =============================================================================

(defn build-list-handler
  "Builds a generic list handler for an entity."
  [{:keys [service entity-key entity-plural default-limit default-order-by
           custom-query-params transform-response]}]
  (fn [db]
    (utils/with-error-handling
      (fn [request]
        (let [qp (:query-params request)
              query-params (merge {:limit (utils/parse-int-param qp :limit default-limit)
                                   :offset (utils/parse-int-param qp :offset 0)
                                   :order-by (keyword (or (:order-by qp) default-order-by))
                                   :order-dir (keyword (or (:order-dir qp) "asc"))}
                             (when custom-query-params
                               (custom-query-params qp)))
              list-fn (resolve-fn service (symbol (str "list-" (name entity-plural))))
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
              count-params (merge {}
                             (when custom-count-params
                               (custom-count-params qp)))
              count-fn (resolve-fn service (symbol (str "count-" (name entity-plural))))
              total (count-fn db count-params)]
          (utils/success-response {:total total})))
      (str "Failed to count " (name entity-plural)))))

(defn build-create-handler
  "Builds a generic create handler for an entity."
  [{:keys [service entity-key entity-plural required-fields custom-validation
           transform-request transform-response]}]
  (fn [db]
    (utils/with-error-handling
      (fn [request]
        (let [body (-> (:body request)
                     (cond-> transform-request transform-request))
              validation-errors (when custom-validation
                                  (custom-validation body))]
          (if validation-errors
            (utils/error-response validation-errors :status 400)
            (if (some #(empty? (get body %)) required-fields)
              (utils/error-response (str (clojure.string/join ", " required-fields) " are required") :status 400)
              (let [create-fn (resolve-fn service (symbol (str "create-" (name entity-key) "!")))
                    entity (create-fn db body)
                    response-key (or (:response-key transform-response) entity-key)
                    response-data (if (:transform transform-response)
                                    ((:transform transform-response) entity)
                                    (to-app entity))]
                (utils/success-response {response-key response-data}))))))
      (str "Failed to create " (name entity-key)))))

(defn build-get-handler
  "Builds a generic get handler for an entity."
  [{:keys [service entity-key custom-get-fn transform-response]}]
  (fn [db]
    (utils/with-error-handling
      (fn [request]
        (if-let [id (utils/parse-uuid-custom (get-in request [:path-params :id]))]
          (let [get-fn (or custom-get-fn
                         (resolve-fn service (symbol (str "get-" (name entity-key)))))
                entity (get-fn db id)]
            (if entity
              (let [response-key (or (:response-key transform-response) entity-key)
                    response-data (if (:transform transform-response)
                                    ((:transform transform-response) entity)
                                    (to-app entity))]
                (utils/success-response {response-key response-data}))
              (utils/error-response (str (clojure.string/capitalize (name entity-key)) " not found") :status 404)))
          (utils/error-response "Invalid id" :status 400)))
      (str "Failed to fetch " (name entity-key)))))

(defn build-update-handler
  "Builds a generic update handler for an entity."
  [{:keys [service entity-key entity-plural transform-request transform-response]}]
  (fn [db]
    (utils/with-error-handling
      (fn [request]
        (if-let [id (utils/parse-uuid-custom (get-in request [:path-params :id]))]
          (let [body (-> (:body request)
                       (cond-> transform-request transform-request))
                update-fn (resolve-fn service (symbol (str "update-" (name entity-key) "!")))
                updated (update-fn db id body)]
            (if updated
              (let [response-key (or (:response-key transform-response) entity-key)
                    response-data (if (:transform transform-response)
                                    ((:transform transform-response) updated)
                                    (to-app updated))]
                (utils/success-response {response-key response-data}))
              (utils/error-response (str (clojure.string/capitalize (name entity-key)) " not found") :status 404)))
          (utils/error-response "Invalid id" :status 400)))
      (str "Failed to update " (name entity-key)))))

(defn build-delete-handler
  "Builds a generic delete handler for an entity."
  [{:keys [service entity-key entity-plural custom-delete-fn delete-response-type]}]
  (fn [db]
    (utils/with-error-handling
      (fn [request]
        (if-let [id (utils/parse-uuid-custom (get-in request [:path-params :id]))]
          (let [delete-fn (or custom-delete-fn
                            (resolve-fn service (symbol (str "delete-" (name entity-key) "!"))))
                deleted (delete-fn db id)]
            (if deleted
              (utils/success-response
                (case delete-response-type
                  :entity {(keyword entity-key) (to-app deleted)}
                  :deleted {:deleted true}
                  {:deleted true}))
              (utils/error-response (str (clojure.string/capitalize (name entity-key)) " not found or in use") :status 404)))
          (utils/error-response "Invalid id" :status 400)))
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
                          (resolve-fn service (symbol (str "search-" (name entity-plural)))))
              results (search-fn db query {:limit limit})
              response-key (or (:response-key transform-response) entity-plural)
              response-data (if (:transform transform-response)
                              ((:transform transform-response) results)
                              (to-app results))]
          (utils/success-response {response-key response-data})))
      (str "Failed to search " (name entity-plural)))))

;; =============================================================================
;; Custom Handler Builders
;; =============================================================================

(defn build-custom-handler
  "Builds a custom handler with specified logic."
  [{:keys [handler-fn error-message]}]
  (fn [db]
    (utils/with-error-handling
      handler-fn
      (or error-message "Failed to process request"))))

;; =============================================================================
;; Route Builder Functions
;; =============================================================================

(defn build-standard-routes
  "Builds standard CRUD routes for an entity."
  [db config]
  (let [handlers (:handlers config)
        route-path (str "/" (:route-segment config))]
    [route-path
     ["" {:get ((:list handlers) db)
          :post ((:create handlers) db)}]
     ["/:id" {:get ((:get handlers) db)
              :put ((:update handlers) db)
              :delete ((:delete handlers) db)}]]))

(defn build-extended-routes
  "Builds CRUD routes with additional endpoints like count, search, etc."
  [db config]
  (let [handlers (:handlers config)
        additional-routes (:additional-routes config)
        route-path (str "/" (:route-segment config))
        base-routes ["" {:get ((:list handlers) db)
                         :post ((:create handlers) db)}]
        id-routes ["/:id" {:get ((:get handlers) db)
                           :put ((:update handlers) db)
                           :delete ((:delete handlers) db)}]]
    (into [route-path]
      (cond-> [base-routes id-routes]
        (:count handlers) (conj ["/count" {:get ((:count handlers) db)}])
        (:search handlers) (conj ["/search" {:get ((:search handlers) db)}])
        true (into (map #(vector % ((:handler %) db)) additional-routes))))))

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
   - :has-search? - whether to include search endpoint (default: false)"
  [config]
  ;; Validate required configuration
  (when-not (:entity-key config)
    (throw (ex-info "entity-key is required" config)))
  (when-not (:entity-plural config)
    (throw (ex-info "entity-plural is required" config)))
  (when-not (:service config)
    (throw (ex-info "service is required" config)))

  ;; NOTE: We do NOT pre-load the service namespace here because it causes issues
  ;; during hot-reloading when namespaces are in a transitional state.
  ;; Instead, we rely on requiring-resolve in resolve-fn which is called lazily
  ;; when requests are actually handled.

  ;; Build standard handlers - resolve-fn uses requiring-resolve for lazy loading
  ;; The :has-count? and :has-search? flags must be explicitly set in config
  ;; to enable those endpoints - we don't auto-detect anymore to avoid reload issues.
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