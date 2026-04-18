(ns app.template.backend.routes.api
  (:require
    [app.domain.backend.registry :as domain-registry]
    [app.shared.frontend-config.io :as frontend-config-io]
    [app.shared.frontend-config.template-user :as template-user]
    [app.shared.specs.entities :as entities-spec]
    [app.shared.specs.form-fields :as form-fields-spec]
    [app.shared.specs.table-columns :as table-columns-spec]
    [app.shared.specs.view-options :as view-options-spec]
    [app.template.backend.email.service :as email-svc]
    [app.template.backend.middleware.user :as user-middleware]
    [app.template.backend.routes.admin.settings-io :as settings-io]
    [app.template.backend.routes.entities :as entities]
    [app.template.backend.routes.onboarding :as onboarding-routes]
    [app.template.backend.routes.tenant :as tenant-routes]
    [app.template.backend.routes.utils :as route-utils]
    [app.admin.backend.services.admin.dashboard :as admin-dashboard]
    [app.template.backend.services.monitoring.login-events :as login-monitoring]
    [app.template.backend.services.tenant :as tenant-svc]
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

(def ^:private domain-ui-config-validators
  "Validators for each config type, used when loading file-backed multi-domain UI config."
  {:entities entities-spec/validate-user-entities
   :view-options view-options-spec/validate-view-options-strict
   :form-fields form-fields-spec/validate-form-fields-strict
   :table-columns table-columns-spec/validate-table-columns-strict})

(defn- safe-read-edn-file
  "Read an EDN file from disk. Returns {} when missing/unreadable.

   This remains only for the multi-domain response shape, where runtime user
   config is still stored as a flat single-domain snapshot."
  [k path]
  (let [validate-fn (get domain-ui-config-validators k)]
    (frontend-config-io/read-edn-or-empty+validate
      {:config-key k
       :path path
       :validate-fn validate-fn
       :log-message "Failed to read domain UI config EDN"
       :log-context {:scope :domain-ui-config}})))

(defn- load-config-path-map
  [paths]
  (into {}
    (map (fn [[k path]]
           [k (safe-read-edn-file k path)]))
    (or paths {})))

(defn- single-domain-runtime-ui-config
  [db]
  {:entities (settings-io/read-user-entities db)
   :view-options (settings-io/read-user-view-options db)
   :form-fields (settings-io/read-user-form-fields db)
   :table-columns (settings-io/read-user-table-columns db)})

(defn- load-domain-ui-config
  "Load user-facing UI config.

   Single-domain mode reads the DB-backed runtime store directly.
   Multi-domain mode preserves the existing nested response shape using the
   source-controlled domain bundles until runtime config becomes domain-aware."
  [db]
  (let [all-paths (domain-registry/get-ui-config-paths)]
    (if (<= (count all-paths) 1)
      (single-domain-runtime-ui-config db)
      (do
        (log/warn "Multi-domain /api/v1/config still uses file-backed user UI config"
          {:domains (keys all-paths)})
        (assoc
          (into {}
            (map (fn [[domain-id paths]]
                   [domain-id (load-config-path-map paths)]))
            all-paths)
          :template
          (load-config-path-map template-user/paths))))))

(defn- compile-schema
  "A custom schema compiler that ignores the second argument (options)
  and returns the schema as-is. This prevents `mu/closed-schema` from
  being applied, which is the default behavior in Reitit."
  [schema _]
  schema)

(defn- make-malli-coercion
  "Create a fresh Malli coercion instance.

  Why: during dev reloads, `clojure.tools.namespace` can reload dependency
  namespaces like `malli.core` without reloading `reitit.coercion.malli`.
  The default reitit transformer providers capture transformer instances at load
  time, which can become invalid after such reloads.

  We therefore build the transformer instances *fresh* for each router build and
  pass them in explicitly, avoiding cached providers."
  []
  (let [strip (mt/strip-extra-keys-transformer)
        defaults (mt/default-value-transformer {})
        json (mt/json-transformer)
        string (mt/string-transformer)
        base (fn [& xs] (apply mt/transformer (remove nil? xs)))]
    (malli-coercion/create
      {:compile compile-schema
       :transformers {:body {:default (base strip defaults)
                             :formats {"application/json" (base strip json defaults)}}
                      :string {:default (base strip string defaults)}
                      :response {:default (base strip defaults)
                                 :formats {"application/json" (base strip json defaults)}}}})))

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

(defn- parse-int-param
  [value default]
  (cond
    (integer? value) value
    (string? value) (try
                      (Integer/parseInt value)
                      (catch NumberFormatException _
                        default))
    :else default))

(defn- tenant-member->user-entity-row
  [member]
  {:id (or (:user_id member) (:user-id member))
   :full_name (or (:user_full_name member) (:user-full-name member))
   :email (or (:user_email member) (:user-email member))
   :status (or (:user_status member) (:user-status member))
   :membership_role (or (:role member)
                      (:tenant_memberships/role member)
                      (:membership-role member))
   :membership_status (or (:status member)
                        (:tenant_memberships/status member)
                        (:membership-status member))})

(defn- protected-users-list-handler
  [db]
  (fn [request]
    (route-utils/with-error-handling "protected-users-list"
      (let [tenant-id (or (get-in request [:session :auth-session :tenant :id])
                        (get-in request [:session :auth-session :tenant :tenants/id])
                        (get-in request [:session :tenant-id]))
            user-id (or (get-in request [:session :auth-session :user :id])
                      (get-in request [:session :user :id]))
            _ (when-not tenant-id
                (throw (ex-info "No active tenant"
                         {:type :validation-error
                          :errors {:tenant ["No active tenant in session"]}})))
            actor-membership (tenant-svc/get-membership db tenant-id user-id)
            _ (when-not actor-membership
                (throw (ex-info "Not a member of this tenant"
                         {:type :forbidden
                          :errors {:tenant ["You are not a member of this tenant"]}})))
            actor-role (let [role (or (:role actor-membership)
                                    (:tenant_memberships/role actor-membership))]
                         (cond
                           (keyword? role) (name role)
                           (some? role) (str role)
                           :else nil))
            include-suspended? (contains? #{"owner" "admin"} actor-role)
            limit (parse-int-param (or (get-in request [:query-params :limit])
                                     (get-in request [:params :limit])) nil)
            offset (parse-int-param (or (get-in request [:query-params :offset])
                                      (get-in request [:params :offset])) 0)
            users (cond->> (tenant-svc/get-tenant-members db tenant-id {:include-suspended? include-suspended?})
                    true (map tenant-member->user-entity-row)
                    true (drop offset)
                    limit (take limit)
                    true vec)]
        (route-utils/success-response users)))))

(defn create-versioned-api-routes
  "Create versioned API routes with the given database, models-data, and
   version prefix like \"/v1\"."
  [db md service-container version]
  (let [;; Extract app-config for domain routes that need it
        app-config (:config service-container)
        ;; Collect user API routes from all enabled domains
        domain-user-routes (domain-registry/all-user-api-routes
                             db user-middleware/wrap-user-authentication app-config)]
    [""
     {:coercion (make-malli-coercion)
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

     ["/config" {:get {:handler (fn [_req]
                                  (try
                                    (let [safe-models-data (or md {})
                                          ;; Process models to include validation specs
                                          processed-models (when (seq safe-models-data)
                                                             ((requiring-resolve 'app.shared.validation.metadata/process-models-for-frontend)
                                                              safe-models-data))
                                          frontend-config {:entity-configs {}
                                                           :models-data safe-models-data
                                                           :validation-specs processed-models
                                                           ;; User-facing UI config (DB-backed).
                                                           :domain-ui-config (load-domain-ui-config db)}]
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

     ;; Tenant management routes (switch, members, invitations, settings, name)
     (tenant-routes/tenant-routes
       db user-middleware/wrap-user-authentication
       (get service-container :email-service)
       (email-svc/create-base-url app-config)
       app-config)

     ;; Onboarding routes (progress, step completion, skip, dismiss)
     (onboarding-routes/onboarding-routes
       db user-middleware/wrap-user-authentication)

     ;; User profile routes (Phase 2 — settings hierarchy)
     ;; Lazy-resolved from domain to avoid template→domain compile-time coupling
     (let [get-profile (requiring-resolve 'app.domain.backend.expenses.handlers.user-expenses.profile/get-profile-handler)
           update-defaults (requiring-resolve 'app.domain.backend.expenses.handlers.user-expenses.profile/update-profile-defaults-handler)
           export-expenses (requiring-resolve 'app.domain.backend.expenses.handlers.user-expenses.settings/export-expenses-handler)
           delete-all-expenses (requiring-resolve 'app.domain.backend.expenses.handlers.user-expenses.settings/delete-all-expenses-handler)]
       ["/profile"
        {:middleware [user-middleware/wrap-user-authentication]}
        ["" {:get {:handler (get-profile db)}}]
        ["/defaults" {:put {:handler (update-defaults db)}}]
        ["/export" {:get {:handler (export-expenses db)}}]
        ["/all" {:delete {:handler (delete-all-expenses db)}}]])

     ;; Domain user API routes (from registry)
     ;; Each domain provides routes under its own path prefix (e.g., /expenses)
     (into [] (filter some? domain-user-routes))

     ;; Generic entity CRUD routes - but we need to handle users specially
     ;; Transform ["/:entity" config & subroutes] to ["/entities/:entity" config & subroutes]
     (let [entity-routes (entities/entities-routes db md service-container)
           [_entity-path route-config & subroutes] (first entity-routes)
           list-route (some #(when (= "" (first %)) %) subroutes)
           original-get-handler (get-in list-route [1 :get :handler])
           users-get-handler (protected-users-list-handler db)
           custom-get-handler (fn [request]
                                (let [entity (or (get-in request [:path-params :entity])
                                               (get-in request [:parameters :path :entity]))
                                      entity-name (cond
                                                    (keyword? entity) (name entity)
                                                    (string? entity) entity
                                                    (some? entity) (str entity)
                                                    :else nil)]
                                  (if (= entity-name "users")
                                    (users-get-handler request)
                                    (original-get-handler request))))
           ;; Protect all generic CRUD endpoints under /entities/:entity
           updated-route-config (-> route-config
                                  (update :middleware (fnil conj []) #(user-middleware/wrap-user-authentication %))
                                  (update :middleware (fnil conj []) #(user-middleware/wrap-entities-authorization %)))
           updated-subroutes (mapv (fn [subroute]
                                     (if (= "" (first subroute))
                                       (assoc-in subroute [1 :get :handler] custom-get-handler)
                                       subroute))
                               subroutes)]
       ;; Replace "/:entity" with "/entities/:entity" in the path and use updated config
       (into ["/entities/:entity" updated-route-config] updated-subroutes))]))

