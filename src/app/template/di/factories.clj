(ns app.template.di.factories
  "Discrete factory functions for template infrastructure services."
  (:require
    [app.template.backend.auth.service :as auth-service]
    [app.template.backend.crud.service :as crud-service]
    [app.template.backend.db.adapter :as db-adapter]
    [app.template.backend.email.service :as email-service]
    [app.template.backend.metadata.service :as metadata-service]
    [app.template.backend.routes.crud :as crud-routes]))

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
  "Create email service based on application configuration.

   IMPORTANT: `app.template.backend.email.service/create-email-service` expects a
   config map that includes both the email settings (e.g. `:type`, `:smtp`) and
   enough app context to build links (e.g. `:webserver`/`:https`)."
  [config]
  (let [email-config (get config :email {:type :console})
        ;; Merge app context so email links (verify/reset/etc.) use the correct
        ;; host/port for the active profile (e.g. :test uses 8086).
        effective-config (merge email-config
                           (select-keys config [:webserver :https :base-url]))]
    (email-service/create-email-service effective-config)))
