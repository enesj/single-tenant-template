(ns app.backend.routes.api
  (:require
    [app.backend.middleware.database-context :as db-context]
    [app.backend.middleware.user :as user-middleware]
    [app.backend.routes.entities :as entities]
    [app.backend.services.admin.dashboard :as admin-dashboard]
    [app.backend.services.monitoring.login-events :as login-monitoring]
    [app.shared.http :as http]
    [cheshire.core :as json]
    [java-time.api :as time]
    [malli.transform :as mt]
    [muuntaja.core :as m]
    [reitit.coercion.malli :as malli-coercion]
    [reitit.ring.coercion :as coercion]
    [reitit.ring.middleware.muuntaja :as muuntaja]
    [reitit.ring.middleware.parameters :as parameters]
    [ring.util.response :as response]
    [taoensso.timbre :as log]))

(def custom-string-transformer
  (mt/transformer
    {:name :string
     :decoders {:string (fn [value]
                          (cond
                            (string? value) value
                            (number? value) (str value)
                            (keyword? value) (name value)
                            :else value))}}))

(defn- compile-schema
  "A custom schema compiler that ignores the second argument (options)
  and returns the schema as-is. This prevents `mu/closed-schema` from
  being applied, which is the default behavior in Reitit."
  [schema _]
  schema)

(def custom-malli-coercion
  (malli-coercion/create
    {:compile compile-schema
     :transformers {:body {:default mt/default-value-transformer
                           :formats {"application/json" mt/default-value-transformer}}
                    :string {:default mt/default-value-transformer}
                    :response {:default mt/default-value-transformer}}}))

(defn wrap-exception-handling
  "Middleware to handle exceptions and return appropriate responses."
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (log/error e "Unhandled exception in API handler")
        {:status 500
         :headers {"Content-Type" "application/json"}
         :body (json/generate-string {:error "Internal server error"})}))))

(defn api-version-middleware
  "Middleware to add API version header to responses."
  [version]
  (fn [handler]
    (fn [request]
      (let [response (handler request)]
        (assoc-in response [:headers "X-API-Version"] version)))))

(defn- get-simple-metrics-summary
  "Get a simple metrics summary for the API based on login_events and audit_logs.

   Returns a map that is safe to JSON-encode directly."
  [db]
  (let [now (time/instant)
        day-ago (time/minus now (time/days 1))
        ;; Dashboard-level stats (admins, sessions, recent audit activity)
        dashboard-stats (try
                          (admin-dashboard/get-dashboard-stats db)
                          (catch Exception e
                            (log/warn e "Failed to load dashboard stats for /metrics")
                            {:total-admins 0
                             :active-sessions 0
                             :recent-activity 0
                             :recent-events []}))
        ;; Login metrics over last 24h
        total-logins-24h (try
                           (login-monitoring/count-recent-login-events db
                             {:since day-ago})
                           (catch Exception e
                             (log/warn e "Failed to compute total login events for /metrics")
                             0))
        admin-logins-24h (try
                           (login-monitoring/count-recent-login-events db
                             {:since day-ago :principal-type :admin})
                           (catch Exception _ 0))
        user-logins-24h (try
                          (login-monitoring/count-recent-login-events db
                            {:since day-ago :principal-type :user})
                          (catch Exception _ 0))
        failed-logins-24h (try
                            (login-monitoring/count-recent-login-events db
                              {:since day-ago :success? false})
                            (catch Exception _ 0))]
    {:status "ok"
     :timestamp (System/currentTimeMillis)
     :version "1.0.0"
     :dashboard dashboard-stats
     :login-metrics {:last-24h {:total total-logins-24h
                                :failed failed-logins-24h
                                :by-principal-type {:admin admin-logins-24h
                                                    :user user-logins-24h}}}}))

(defn create-versioned-api-routes
  "Create versioned API routes with the given database, models-data, and
   version prefix like \"/v1\"."
  [db md service-container version]
  [""
   {:coercion custom-malli-coercion
    :muuntaja m/instance
    :middleware [parameters/parameters-middleware
                 muuntaja/format-negotiate-middleware
                 muuntaja/format-response-middleware
                 muuntaja/format-request-middleware
                 coercion/coerce-response-middleware
                 coercion/coerce-request-middleware
                 wrap-exception-handling
                 (api-version-middleware version)]}

   ;; IMPORTANT: Specific routes must come BEFORE wildcard routes in Reitit
   ;; to avoid conflicts. All specific paths like /config or /metrics
   ;; must be defined before the generic /:entity patterns.

   ["/config" {:get {:handler (fn [req]
                                (try
                                  (log/info "=== CONFIG ENDPOINT CALLED ===")
                                  (let [_req-service-container (:service-container req)
                                        safe-models-data (or md {})
                                        ;; Process models to include validation specs
                                        processed-models (when (seq safe-models-data)
                                                           ((requiring-resolve 'app.shared.validation.metadata/process-models-for-frontend) safe-models-data))
                                        frontend-config {:entity-configs {}
                                                         :models-data safe-models-data
                                                         :validation-specs processed-models}]
                                    (log/info "Config endpoint success with models-data keys:" (when safe-models-data (keys safe-models-data)))
                                    (log/info "Config endpoint validation specs keys:" (when processed-models (keys processed-models)))
                                    (response/response frontend-config))
                                  (catch Exception e
                                    (log/error e "ERROR in config handler")
                                    {:status 500
                                     :headers {"Content-Type" "application/json"}
                                     :body "{\"error\":\"Handler failed\"}"})))}}]

   ["/metrics" {:get {:handler (fn [_]
                                 {:status http/status-ok
                                  :headers {http/header-content-type http/content-type-json}
                                  :body (json/generate-string (get-simple-metrics-summary db))})}}]

   ;; Authentication and email verification routes
   ["/auth"
    ;; Email/password registration - used by SPA /register
    ["/register" {:post {:handler (fn [req]
                                    (if-let [register-handler (get-in service-container [:auth-routes :register-handler])]
                                      ;; Handler expects :service-container on request (added by webserver middleware)
                                      (register-handler req)
                                      {:status 500
                                       :headers {"Content-Type" "application/json"}
                                       :body (json/generate-string {:error "Auth registration handler not available"})}))}}]

    ;; Email/password login - used by SPA /login
    ["/login" {:post {:handler (fn [req]
                                 (if-let [login-handler (get-in service-container [:auth-routes :login-handler])]
                                   (login-handler req)
                                   {:status 500
                                    :headers {"Content-Type" "application/json"}
                                    :body (json/generate-string {:error "Auth login handler not available"})}))}}]

    ;; Password reset routes
    ["/forgot-password" {:post {:handler (fn [req]
                                           (if-let [handler (get-in service-container [:password-routes :forgot-password-handler])]
                                             (handler req)
                                             {:status 500
                                              :headers {"Content-Type" "application/json"}
                                              :body (json/generate-string {:error "Forgot password handler not available"})}))}}]

    ["/verify-reset-token" {:get {:handler (fn [req]
                                             (if-let [handler (get-in service-container [:password-routes :verify-reset-token-handler])]
                                               (handler req)
                                               {:status 500
                                                :headers {"Content-Type" "application/json"}
                                                :body (json/generate-string {:error "Verify reset token handler not available"})}))}}]

    ["/reset-password" {:post {:handler (fn [req]
                                          (if-let [handler (get-in service-container [:password-routes :reset-password-handler])]
                                            (handler req)
                                            {:status 500
                                             :headers {"Content-Type" "application/json"}
                                             :body (json/generate-string {:error "Reset password handler not available"})}))}}]

    ["/change-password" {:post {:middleware [#(user-middleware/wrap-user-authentication %)]
                                :handler (fn [req]
                                           (if-let [handler (get-in service-container [:password-routes :change-password-handler])]
                                             (handler req)
                                             {:status 500
                                              :headers {"Content-Type" "application/json"}
                                              :body (json/generate-string {:error "Change password handler not available"})}))}}]

    ;; Email verification JSON helpers
    ["/verification-status" {:get {:handler (fn [req]
                                              (when-let [handler-fn (requiring-resolve 'app.template.backend.routes.email-verification/verification-status-handler)]
                                                ((handler-fn db) req)))}}]
    ["/resend-verification" {:post {:handler (fn [req]
                                               (let [email-service (get service-container :email-service)]
                                                 (when-let [handler-fn (requiring-resolve 'app.template.backend.routes.email-verification/resend-verification-handler)]
                                                   ((handler-fn db email-service) req))))}}]]

   ;; Generic entity CRUD routes - but we need to handle users specially
   ;; Transform ["/:entity" config & subroutes] to ["/entities/:entity" config & suboutes]
   (let [entity-routes (entities/entities-routes db md service-container)
         [_entity-path route-config & subroutes] (first entity-routes)
         ;; Create a custom handler that checks for users and delegates appropriately
         custom-get-handler (fn [request]
                              (let [entity (get-in request [:parameters :path :entity])]
                                (if (= entity "users")
                                  ;; Delegate to admin API for users
                                  (let [admin-handler (requiring-resolve 'app.backend.routes.admin-api/list-users-handler)]
                                    ((admin-handler db) request))
                                  ;; Use original handler for other entities
                                  (let [original-get-handler (get-in route-config ["" :get :handler])]
                                    (original-get-handler request)))))
         ;; Update the route config to use our custom handler and require user auth for all CRUD ops
         updated-route-config (-> route-config
                               ;; Protect all generic CRUD endpoints under /entities/:entity
                                (update :middleware (fnil conj []) #(user-middleware/wrap-user-authentication %))
                                (update :middleware (fnil conj []) #(user-middleware/wrap-entities-authorization %))
                                (assoc-in ["" :get :handler] custom-get-handler))]
     ;; Replace "/:entity" with "/entities/:entity" in the path and use updated config
     (into ["/entities/:entity" updated-route-config] subroutes))])

(defn create-api-routes
  "Create API routes with version prefix"
  [db md service-container]
  ["/api" {}
   ["/v1" (create-versioned-api-routes db md service-container "v1")]])
