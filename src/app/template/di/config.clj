(ns app.template.di.config
  "Dependency injection configuration for template infrastructure services"
  (:require
    [app.shared.model-naming :as model-naming]
    [app.template.backend.auth.service :as auth-service]
    [app.template.backend.crud.service :as crud-service]
    [app.template.backend.db.adapter :as db-adapter]
    [app.template.backend.email.service :as email-service]
    [app.template.backend.metadata.service :as metadata-service]
    [app.template.backend.routes.auth :as auth-routes]
    [app.template.backend.routes.crud :as crud-routes]
    [app.template.backend.routes.oauth :as oauth-routes]
    [app.template.backend.routes.password-reset :as password-routes]
    [app.template.di.container :as container]
    [app.template.protocols :as template-proto]
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [taoensso.timbre :as log]))

(defn create-database-adapter
  "Create database adapter for template services"
  ([db-connection]
   (db-adapter/create-postgres-adapter db-connection))
  ([db-connection domain-config]
   (let [relationship-mappings (get-in domain-config [:relationship-mappings])]
     (db-adapter/create-postgres-adapter db-connection relationship-mappings))))

(defn create-authentication-service
  "Create authentication service with all dependencies"
  [config db-connection metadata email-service]
  (let [db-adapter (create-database-adapter db-connection)]
    (auth-service/create-authentication-service db-adapter metadata config email-service)))

(defn create-auth-routes
  "Create authentication routes"
  [auth-service]
  (auth-routes/create-auth-routes auth-service))

(defn create-oauth-routes
  "Create OAuth routes"
  [auth-service]
  (oauth-routes/create-oauth-routes auth-service))

(defn create-metadata-service
  "Create metadata service for model introspection"
  [metadata]
  (metadata-service/create-metadata-service metadata))

(defn create-type-casting-service
  "Create type casting service"
  [metadata]
  (metadata-service/create-type-casting-service metadata))

(defn create-validation-service
  "Create validation service"
  [metadata db-adapter]
  (metadata-service/create-validation-service metadata db-adapter))

(defn create-query-builder
  "Create query builder service"
  [metadata]
  (metadata-service/create-query-builder metadata))

(defn create-crud-service
  "Create CRUD service with all dependencies"
  [db-adapter metadata]
  (let [metadata-svc (create-metadata-service metadata)
        type-casting-svc (create-type-casting-service metadata)
        validation-svc (create-validation-service metadata db-adapter)
        query-builder-svc (create-query-builder metadata)]

    (crud-service/create-crud-service
      db-adapter metadata-svc validation-svc
      type-casting-svc query-builder-svc)))

(defn create-crud-routes
  "Create CRUD routes"
  [crud-service]
  (crud-routes/crud-routes crud-service))

(defn create-email-service
  "Create email service based on configuration"
  [config]
  (let [email-config (get config :email {:type :console})]
    (email-service/create-email-service email-config)))

(defn create-template-services
  "Create all template infrastructure services"
  [config db-connection metadata]
  (log/info "Creating template infrastructure services...")

  (let [app-models (model-naming/app-models metadata)
        db-adapter (create-database-adapter db-connection)
        email-service (create-email-service config)
        auth-service (create-authentication-service config db-adapter app-models email-service)
        crud-service (create-crud-service db-adapter app-models)
        auth-routes (create-auth-routes auth-service)
        oauth-routes (create-oauth-routes auth-service)]

    (log/info "Template services initialized successfully")

    {:auth-service auth-service
     :crud-service crud-service
     :email-service email-service
     :auth-routes auth-routes
     :oauth-routes oauth-routes
     :db-adapter db-adapter
     :app-models app-models
     :metadata-service (create-metadata-service app-models)
     :type-casting-service (create-type-casting-service app-models)
     :validation-service (create-validation-service app-models db-adapter)
     :query-builder (create-query-builder app-models)
     ;; Placeholder for future template services
     :tenant-service nil
     :user-service nil
     :invitation-service nil}))

(defn create-domain-services
  "Create domain-specific services using template services"
  [_template-services _config _db-connection _metadata]
  (log/info "Creating domain services...")

  ;; For now, just return empty placeholders
  ;; These will be implemented in future phases
  {:property-service nil
   :booking-service nil
   :transaction-service nil
   :reporting-service nil})

(defn create-application-services
  "Create high-level application services"
  [_template-services _domain-services _config]
  (log/info "Creating application services...")

  ;; For now, just return empty placeholders
  {:hosting-app-service nil})

(defn cleanup-services!
  "Cleanup all services"
  [services]
  (log/info "Cleaning up services...")

  ;; Cleanup template services
  (when-let [auth-service (:auth-service services)]
    (template-proto/cleanup auth-service))

  ;; TODO: Cleanup other services as they are created

  (log/info "Services cleanup complete"))

(defn register-template-services!
  "Register all template services with the DI container"
  ([di-container config db-connection metadata]
   (register-template-services! di-container config db-connection metadata nil))
  ([di-container config db-connection metadata domain-config]
   (log/debug "Registering template services with db-connection:" (type db-connection))
   (when (nil? db-connection)
     (log/error "Database connection is nil when registering template services!"))
   (let [app-models (model-naming/app-models metadata)]
     (-> di-container
       (container/register-service!
         (container/create-simple-service
           :db-adapter
           (fn [_]
             (log/debug "Creating database adapter with connection:" (type db-connection))
             (when (nil? db-connection)
               (log/error "Database connection is nil when creating adapter!"))
             (let [relationship-mappings (get-in domain-config [:relationship-mappings])]
               (db-adapter/->PostgresAdapter db-connection relationship-mappings)))))

       (container/register-service!
         (container/create-simple-service
           :metadata-service
           (fn [_] (metadata-service/create-metadata-service app-models))))

       (container/register-service!
         (container/create-simple-service
           :type-casting-service
           (fn [_] (metadata-service/create-type-casting-service app-models))))

       (container/register-service!
         (container/create-simple-service
           :validation-service
           (fn [c]
             (let [db-adapter (container/get-service c :db-adapter)]
               (metadata-service/create-validation-service app-models db-adapter)))
           :deps #{:db-adapter}))

       (container/register-service!
         (container/create-simple-service
           :query-builder
           (fn [_] (metadata-service/create-query-builder app-models))))

       (container/register-service!
         (container/create-simple-service
           :crud-service
           (fn [c]
             (crud-service/create-crud-service
               (container/get-service c :db-adapter)
               (container/get-service c :metadata-service)
               (container/get-service c :validation-service)
               (container/get-service c :type-casting-service)
               (container/get-service c :query-builder)))
           :deps #{:db-adapter :metadata-service :validation-service
                   :type-casting-service :query-builder}))

       (container/register-service!
         (container/create-simple-service
           :email-service
           (fn [_] (create-email-service config))))

       (container/register-service!
         (container/create-simple-service
           :auth-service
           (fn [c]
             (let [all-services (container/list-services c)
                   service-statuses (container/service-statuses c)
                   db-adapter (container/get-service c :db-adapter)
                   email-service (container/get-service c :email-service)]
               (log/debug "Creating auth service:")
               (log/debug "  All services:" all-services)
               (log/debug "  Service statuses:" service-statuses)
               (log/debug "  Database adapter:" (type db-adapter))
               (log/debug "  Email service:" (type email-service))
               (when (nil? db-adapter)
                 (log/error "Database adapter is nil when creating auth service!")
                 (log/error "Available services:" all-services)
                 (log/error "Service statuses:" service-statuses))
               (auth-service/create-authentication-service
                 db-adapter app-models config email-service)))
           :deps #{:db-adapter :email-service}))

       (container/register-service!
         (container/create-simple-service
           :auth-routes
           (fn [c]
             (auth-routes/create-auth-routes
               (container/get-service c :auth-service)))
           :deps #{:auth-service}))

       (container/register-service!
         (container/create-simple-service
           :oauth-routes
           (fn [c]
             (oauth-routes/create-oauth-routes
               (container/get-service c :auth-service)))
           :deps #{:auth-service}))

       (container/register-service!
         (container/create-simple-service
           :password-routes
           (fn [c]
             (let [db-adapter (container/get-service c :db-adapter)
                   email-service (container/get-service c :email-service)
                   base-url (get config :base-url "http://localhost:8085")]
               (password-routes/create-password-reset-routes
                 db-adapter email-service base-url)))
           :deps #{:db-adapter :email-service}))

       (container/register-service!
         (container/create-simple-service
           :crud-routes
           (fn [c]
             (crud-routes/crud-routes
               (container/get-service c :crud-service)))
           :deps #{:crud-service}))))))

(defn wire-services
  "Wire all services with dependency injection"
  [config db-connection metadata]
  (log/info "Wiring services with dependency injection...")

  (let [template-services (create-template-services config db-connection metadata)
        domain-services (create-domain-services template-services config db-connection metadata)
        app-services (create-application-services template-services domain-services config)

        ;; Merge all services
        all-services (merge template-services domain-services app-services)]

    (log/info "Service wiring complete")
    all-services))

(defn create-service-container
  "Create and boot a DI container with all services"
  ([config db-connection metadata]
   (create-service-container config db-connection metadata nil))
  ([config db-connection metadata domain-config]
   (log/info "Creating service container with DI...")

   (let [app-models (model-naming/app-models metadata)
         di-container (-> (container/create-container config)
                        (register-template-services! config db-connection metadata domain-config)
                        container/initialize-services!
                        container/start-services!)]

     ;; Return a map structure compatible with existing code
     {:di-container di-container
      :config config
      :models-data metadata
      :app-models app-models
      :auth-service (container/get-service di-container :auth-service)
      :crud-service (container/get-service di-container :crud-service)
      :email-service (container/get-service di-container :email-service)
      :auth-routes (container/get-service di-container :auth-routes)
      :oauth-routes (container/get-service di-container :oauth-routes)
      :password-routes (container/get-service di-container :password-routes)
      :crud-routes (container/get-service di-container :crud-routes)
      :db-adapter (container/get-service di-container :db-adapter)
      :metadata-service (container/get-service di-container :metadata-service)
      :type-casting-service (container/get-service di-container :type-casting-service)
      :validation-service (container/get-service di-container :validation-service)
      :query-builder (container/get-service di-container :query-builder)
      :get-service (partial container/get-service di-container)
      :get-service! (partial container/get-service! di-container)
      :service-status (partial container/service-status di-container)
      :service-statuses #(container/service-statuses di-container)
      :tenant-service nil
      :user-service nil
      :invitation-service nil
      :property-service nil
      :booking-service nil
      :transaction-service nil
      :reporting-service nil
      :hosting-app-service nil})))

;; Service lifecycle management

(defn stop-services!
  "Stop all services"
  [services]
  (when services
    (cleanup-services! services)))

;; Configuration helpers

(defn get-auth-service
  "Get authentication service from service container"
  [services]
  (:auth-service services))

(defn get-crud-service
  "Get CRUD service from service container"
  [services]
  (:crud-service services))

(defn get-metadata-service
  "Get metadata service from service container"
  [services]
  (:metadata-service services))

(defn get-validation-service
  "Get validation service from service container"
  [services]
  (:validation-service services))

(defn get-auth-routes
  "Get authentication routes from service container"
  [services]
  (:auth-routes services))

(defn get-oauth-routes
  "Get OAuth routes from service container"
  [services]
  (:oauth-routes services))

(defn get-crud-routes
  "Get CRUD routes from service container"
  [services]
  (:crud-routes services))

(defn get-db-adapter
  "Get database adapter from service container"
  [services]
  (:db-adapter services))

(defn get-config
  "Get configuration from service container"
  [services]
  (:config services))

(defn get-models-data
  "Get models data from service container"
  [services]
  (:models-data services))

(comment

;; =============================================================================
;; Usage Examples
;; =============================================================================

;; These examples are executable and can be evaluated in a REPL.
;; Note: In a real REPL session, you would need to require the necessary namespaces first.

;; Helper functions for examples (simulate external dependencies)
  (defn load-config []
    {:app-name "Test App"
     :email {:type :console}
     :database {:host "localhost"
                :port 5432
                :dbname "test_db"
                :user "test_user"
                :password "test_pass"}})

  (defn load-metadata []
   ;; Load real metadata from the models.edn file
    (try
      (let [models-resource (io/resource "db/models.edn")]
        (if models-resource
          (-> models-resource slurp edn/read-string)
         ;; Fallback: try direct file path
          (let [models-file (io/file "resources/db/models.edn")]
            (if (.exists models-file)
              (-> models-file slurp edn/read-string)
              (do
                (println "Warning: Could not find models.edn file!")
                {:tables {:users {:columns {:id {:type :uuid :primary-key true}}
                                  :email {:type :string :required true}
                                  :name {:type :string}}}})))))
      (catch Exception e
        (println "Error loading metadata:" (.getMessage e))
       ;; Fallback to minimal mock data
        {:tables {:users {:columns {:id {:type :uuid :primary-key true}}
                          :email {:type :string :required true}
                          :name {:type :string}}}})))

  (defn process-request [services]
    (println "Processing request with services:" (keys services)))

  (defn validation-errors []
    {:email ["Invalid email format"]})

;; Mock CustomService for example
  (defrecord CustomService [db-adapter metadata-service validation-service])

;; Example 1: Basic service container creation
  (defn initialize-app []
    (let [config (load-config)
          db-connection {:connection "mock-db-connection"} ; In real code: (jdbc/get-connection (:database config))
          metadata (load-metadata)]
      (create-service-container config db-connection metadata)))

;; Example 2: Using services directly
  (defn handle-user-request [services {:keys [entity-type entity-id credentials context]}]
    (let [auth-svc (get-auth-service services)
          crud-svc (get-crud-service services)]
    ;; Authenticate before fetching the entity from the CRUD service
      (when (:success? (template-proto/authenticate auth-svc credentials))
        (template-proto/get-entity crud-svc entity-type entity-id context))))

;; Example 3: Using DI container for dependency resolution
  (defn create-custom-service [di-container]
    (let [db-adapter (container/get-service di-container :db-adapter)
          metadata-service (container/get-service di-container :metadata-service)
          validation-service (container/get-service di-container :validation-service)]
     ;; Create service with injected dependencies
      (->CustomService db-adapter metadata-service validation-service)))

;; Example 4: Service lifecycle management
  (defn start-and-stop-services []
    (let [config (load-config)
          db-connection {:connection "mock-db-connection"}
          metadata (load-metadata)
          services (create-service-container config db-connection metadata)]
      (try
       ;; Use services
        (process-request services)
        (finally
         ;; Cleanup
          (stop-services! services)))))

;; Example 5: Configuration-based email service
  (defn setup-email-service [config]
    (let [email-service (create-email-service config)
          send-fn (or (:send email-service)
                    (:send-email email-service))]
    ;; Uses :console type by default, or configured SMTP settings
      (when send-fn
        (send-fn {:to "user@example.com"
                  :subject "Welcome!"
                  :body "Welcome to our platform!"}))))

;; Example 6: CRUD service with validation and metadata
  (defn create-user-with-validation [services context user-data]
    (let [crud-service (get-crud-service services)
          validation-service (get-validation-service services)
          {:keys [valid? errors validated-data] :as validation-result}
          (template-proto/validate-entity validation-service :users user-data context)]
      (if valid?
        (template-proto/create-entity crud-service :users (or validated-data user-data) context)
        (throw (ex-info "Validation failed"
                 {:errors (or errors (:errors validation-result))})))))

;; Example 7: Authentication flow
  (defn authenticate-user [services credentials]
    (let [auth-service (get-auth-service services)]
      (template-proto/authenticate auth-service credentials)))

;; Example 8: Database adapter with relationship mappings
  (defn create-db-adapter-with-relationships [db-connection domain-config]
    (create-database-adapter db-connection domain-config)))
;; Automatically handles relationships defined in domain-config
