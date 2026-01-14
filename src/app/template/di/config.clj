(ns app.template.di.config
  "Dependency injection configuration for template infrastructure services.
   Central orchestration point for service lifecycle and container management."
  (:require
    [app.shared.model-naming :as model-naming]
    [app.template.di.container :as container]
    [app.template.di.factories :as factories]
    [app.template.di.registry :as registry]
    [app.template.protocols :as template-proto]
    [taoensso.timbre :as log]))

;; =============================================================================
;; Re-exports and Aliases for Backward Compatibility
;; =============================================================================

(def create-database-adapter factories/create-database-adapter)
(def create-authentication-service factories/create-authentication-service)
(def create-auth-routes factories/create-auth-routes)
(def create-oauth-routes factories/create-oauth-routes)
(def create-metadata-service factories/create-metadata-service)
(def create-type-casting-service factories/create-type-casting-service)
(def create-validation-service factories/create-validation-service)
(def create-query-builder factories/create-query-builder)
(def create-crud-service factories/create-crud-service)
(def create-crud-routes factories/create-crud-routes)
(def create-email-service factories/create-email-service)

(def register-template-services! registry/register-template-services!)

;; =============================================================================
;; High-level Orchestration
;; =============================================================================

(defn create-template-services
  "Create all template infrastructure services"
  [config db-connection metadata]
  (log/info "Creating template infrastructure services...")

  (let [app-models (model-naming/app-models metadata)
        db-adapter (factories/create-database-adapter db-connection)
        email-service (factories/create-email-service config)
        auth-service (factories/create-authentication-service config db-adapter app-models email-service)
        crud-service (factories/create-crud-service db-adapter app-models)
        auth-routes (factories/create-auth-routes auth-service)
        oauth-routes (factories/create-oauth-routes auth-service)]

    (log/info "Template services initialized successfully")

    {:auth-service auth-service
     :crud-service crud-service
     :email-service email-service
     :auth-routes auth-routes
     :oauth-routes oauth-routes
     :db-adapter db-adapter
     :app-models app-models
     :metadata-service (factories/create-metadata-service app-models)
     :type-casting-service (factories/create-type-casting-service app-models)
     :validation-service (factories/create-validation-service app-models db-adapter)
     :query-builder (factories/create-query-builder app-models)
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
                        (registry/register-template-services! config db-connection metadata domain-config)
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

;; =============================================================================
;; Service Lifecycle Management
;; =============================================================================

(defn stop-services!
  "Stop all services"
  [services]
  (when services
    (when-let [di-container (:di-container services)]
      (try
        (container/shutdown-system! di-container)
        (catch Exception e
          (log/warn e "Failed to stop DI container services"))))
    (cleanup-services! services)))

;; =============================================================================
;; Configuration Helpers (Getters)
;; =============================================================================

(defn get-auth-service [services] (:auth-service services))
(defn get-crud-service [services] (:crud-service services))
(defn get-metadata-service [services] (:metadata-service services))
(defn get-validation-service [services] (:validation-service services))
(defn get-auth-routes [services] (:auth-routes services))
(defn get-oauth-routes [services] (:oauth-routes services))
(defn get-crud-routes [services] (:crud-routes services))
(defn get-db-adapter [services] (:db-adapter services))
(defn get-config [services] (:config services))
(defn get-models-data [services] (:models-data services))
