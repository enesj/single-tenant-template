(ns app.template.di.registry
  "Registration logic for template services in the DI container."
  (:require
    [app.shared.model-naming :as model-naming]
    [app.template.backend.auth.service :as auth-service]
    [app.template.backend.crud.service :as crud-service]
    [app.template.backend.db.adapter :as db-adapter]
    [app.template.backend.email.service :as email-svc]
    [app.template.backend.metadata.service :as metadata-service]
    [app.template.backend.routes.auth :as auth-routes]
    [app.template.backend.routes.crud :as crud-routes]
    [app.template.backend.routes.oauth :as oauth-routes]
    [app.template.backend.routes.password-reset :as password-routes]
    [app.template.di.container :as container]
    [app.template.di.factories :as factories]
    [taoensso.timbre :as log]))

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
           (fn [_] (factories/create-email-service config))))

       (container/register-service!
         (container/create-simple-service
           :auth-service
           (fn [c]
             (let [db-adapter (container/get-service c :db-adapter)
                   email-service (container/get-service c :email-service)]
               (log/debug "Creating auth service...")
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
                   base-url (email-svc/create-base-url config)]
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
