(ns app.backend.routes.entities
  (:require
    [taoensso.timbre :as log]))

;; ============================================================================
;; Field Validation Routes (using service container)
;; ============================================================================

;; ============================================================================
;; Legacy code removed - validation now handled by template CRUD routes
;; ============================================================================

;; ============================================================================
;; Service-Based Entity Routes
;; ============================================================================

;; Note: service-entity-routes function removed - now using modern template CRUD routes directly

;; ============================================================================
;; Main Route Functions
;; ============================================================================

(defn entities-routes-logger [md]
  (log/info "Generating entities routes for keys:" (keys md)))

(defn entities-routes
  "Generate entity routes using service container with modern template CRUD routes.

   The modern routes use {entity} path parameters and handle all entities generically,
   so we only need to create one set of routes that work for all entities."
  ([db md]
   (entities-routes db md nil))
  ([_db md service-container]
   (entities-routes-logger md)
   (if-not service-container
     (do
       (log/error "Service container is required but not provided!")
       (throw (ex-info "Service container is required for entity routes" {})))
     ;; With modern template CRUD routes, we get the routes from the DI container
     ;; to avoid duplicating route creation
     (let [crud-routes (:crud-routes service-container)]
       (log/info "🔧 Using modern template CRUD routes from DI container")
       (log/info "🔧 Available entities:" (keys md))
       crud-routes))))

(defn generate-frontend-entity-routes
  "Generate frontend routes for available entities from models-data"
  [md]
  (let [entity-keys (keys md)]
    (mapcat (fn [entity-key]
              (let [entity-name (name entity-key)]
                [[(str "/entities/" entity-name) {:get {:handler :render-page}}]
                 [(str "/entities/" entity-name "/") {:get {:handler :render-page}}]
                 [(str "/entities/" entity-name "/add") {:get {:handler :render-page}}]
                 [(str "/entities/" entity-name "/update/:item-id") {:get {:handler :render-page}}]]))
      entity-keys)))
