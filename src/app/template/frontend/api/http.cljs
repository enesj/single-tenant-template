(ns app.template.frontend.api.http
  "HTTP request utilities for consistent API communication.
   Provides request builders and common configurations."
  (:require
    [ajax.core :as ajax]
    [app.shared.http :as shared-http]
    [app.shared.http.core :as shared-http-core]
    [app.template.frontend.api :as api]
    [clojure.string :as str]
    [re-frame.db :as rf-db]
    #_[taoensso.timbre :as log]))

;;; -------------------------
;;; Request Formats
;;; -------------------------

(def json-request-format
  "Standard JSON request format"
  (ajax/json-request-format))

(def json-response-format
  "Standard JSON response format with keyword keys"
  (ajax/json-response-format {:keywords? true}))

#_(def transit-request-format
    "Transit request format for rich data types"
    (ajax/transit-request-format))

#_(def transit-response-format
    "Transit response format with keyword keys"
    (ajax/transit-response-format {:keywords? true}))

;;; -------------------------
;;; Core Request Builder
;;; -------------------------

(defn api-request
  "Build a standard API request configuration.
   Provides sensible defaults for format, response-format, and timeout."
  [{:keys [method uri params body format response-format on-success on-failure timeout headers]
    :or {format json-request-format
         response-format json-response-format
         timeout 8000}
    :as _config}]
  (shared-http-core/build-xhrio-request
    {:method method
     :uri uri
     :params params
     :body body
     :format format
     :response-format response-format
     :timeout timeout
     :headers headers
     :on-success on-success
     :on-failure on-failure}))

;;; -------------------------
;;; HTTP Method Helpers
;;; -------------------------

(defn get-request
  "Create a GET request configuration"
  [{:keys [_uri _on-success _on-failure] :as opts}]
  (api-request (assoc opts :method :get)))

(defn post-request
  "Create a POST request configuration"
  [{:keys [_uri params _on-success _on-failure] :as opts}]
  (api-request (assoc opts :method :post :params params)))

(defn put-request
  "Create a PUT request configuration"
  [{:keys [_uri params _on-success _on-failure] :as opts}]
  (api-request (assoc opts :method :put :params params)))

(defn delete-request
  "Create a DELETE request configuration"
  [{:keys [_uri _on-success _on-failure] :as opts}]
  (api-request (assoc opts :method :delete)))

#_(defn patch-request
    "Create a PATCH request configuration"
    [{:keys [_uri params _on-success _on-failure] :as opts}]
    (api-request (assoc opts :method :patch :params params)))

;;; -------------------------
;;; Entity CRUD Operations
;;; -------------------------

(defn get-entities
  "Get all entities of a given type"
  [{:keys [entity-name on-success on-failure]}]
  (get-request
    {:uri (api/entity-endpoint entity-name)
     :on-success on-success
     :on-failure on-failure}))

(defn get-entity
  "Get a single entity by ID"
  [{:keys [entity-name id on-success on-failure]}]
  (get-request
    {:uri (api/entity-endpoint entity-name id)
     :on-success on-success
     :on-failure on-failure}))

(defn- admin-token-from-storage
  "Return the admin token from app-db or localStorage.

  NOTE: This helper is only used by the explicit *-admin request builders below.
  Public/template requests must not rely on admin auth state."
  []
  (or (try (:admin/token @rf-db/app-db) (catch :default _ nil))
    (try (when (exists? js/localStorage)
           (.getItem js/localStorage "admin-token"))
      (catch :default _ nil))))

(defn- admin-entity-endpoint
  "Admin entity CRUD endpoint.

  Admin entity routes are mounted under /admin/api/entities/*"
  ([entity-name]
   (str "/admin/api/entities/" entity-name))
  ([entity-name id]
   (str "/admin/api/entities/" entity-name "/" id)))

(defn create-entity-public
  "Create an entity via the public/template API endpoint (versioned /api/*).

  This is the correct choice for non-admin routes even if a stale admin token exists."
  [{:keys [entity-name data on-success on-failure]}]
  (post-request
    {:uri (api/entity-endpoint entity-name)
     :params data
     :on-success on-success
     :on-failure on-failure}))

(defn update-entity-public
  "Update an entity via the public/template API endpoint (versioned /api/*)."
  [{:keys [entity-name id data on-success on-failure]}]
  (put-request
    {:uri (api/entity-endpoint entity-name id)
     :params data
     :on-success on-success
     :on-failure on-failure}))

(defn delete-entity-public
  "Delete an entity via the public/template API endpoint (versioned /api/*)."
  [{:keys [entity-name id on-success on-failure]}]
  (delete-request
    {:uri (api/entity-endpoint entity-name id)
     :on-success on-success
     :on-failure on-failure}))

(defn create-entity-admin
  "Create an entity via the admin API endpoint (/admin/api/entities/*).

  Use this only when you are explicitly performing an admin operation."
  [{:keys [entity-name data on-success on-failure token]}]
  (let [admin-token (or token (admin-token-from-storage))
        headers (when admin-token {"x-admin-token" admin-token})]
    (post-request
      (cond-> {:uri (admin-entity-endpoint entity-name)
               :params data
               :on-success on-success
               :on-failure on-failure}
        headers (assoc :headers headers)))))

(defn update-entity-admin
  "Update an entity via the admin API endpoint (/admin/api/entities/*)."
  [{:keys [entity-name id data on-success on-failure token]}]
  (let [admin-token (or token (admin-token-from-storage))
        headers (when admin-token {"x-admin-token" admin-token})]
    (put-request
      (cond-> {:uri (admin-entity-endpoint entity-name id)
               :params data
               :on-success on-success
               :on-failure on-failure}
        headers (assoc :headers headers)))))

(defn delete-entity-admin
  "Delete an entity via the admin API endpoint (/admin/api/entities/*)."
  [{:keys [entity-name id on-success on-failure token]}]
  (let [admin-token (or token (admin-token-from-storage))
        headers (when admin-token {"x-admin-token" admin-token})]
    (delete-request
      (cond-> {:uri (admin-entity-endpoint entity-name id)
               :on-success on-success
               :on-failure on-failure}
        headers (assoc :headers headers)))))

(defn create-entity
  "Backward-compatible wrapper.

  This function now always targets the public/template API endpoint.
  Use `create-entity-admin` explicitly for admin operations."
  [opts]
  (create-entity-public opts))

(defn update-entity
  "Backward-compatible wrapper.

  This function now always targets the public/template API endpoint.
  Use `update-entity-admin` explicitly for admin operations."
  [opts]
  (update-entity-public opts))

(defn delete-entity
  "Backward-compatible wrapper.

  This function now always targets the public/template API endpoint.
  Use `delete-entity-admin` explicitly for admin operations."
  [opts]
  (delete-entity-public opts))

(defn batch-update-entities
  "Batch update multiple entities"
  [{:keys [entity-name item-ids values on-success on-failure]}]
  (let [entity-key (keyword (str/replace entity-name "-" "_"))
        params {entity-key (mapv #(assoc values :id %) item-ids)}]
    (post-request
      {:uri (api/batch-endpoint entity-name "update")
       :params params
       :timeout 8000
       :on-success on-success
       :on-failure on-failure})))

;; Legacy function names for compatibility
#_(def entity-get-all get-entities)
#_(def entity-get-by-id get-entity)
#_(def entity-create create-entity)
#_(def entity-update update-entity)
#_(def entity-delete delete-entity)
#_(def entity-batch-update batch-update-entities)

;;; -------------------------
;;; Error Handling
;;; -------------------------

(defn extract-error-message
  "Extract error message from various response formats - delegates to shared utilities"
  [response]
  (shared-http/extract-error-message response))

#_(defn log-request-error
    "Log request errors with context"
    [context response]
    (log/error (str "Request failed - " context ": ")
      (extract-error-message response)))

;;; -------------------------
;;; Request Interceptors
;;; -------------------------

#_(defn with-loading-state
    "Add loading state management to request - Loading is now handled by fetch events"
    [_entity-type config]
  ;; Loading state is now managed by fetch-entities/fetch-success/fetch-failure events
  ;; This function is kept for backwards compatibility but is effectively a no-op
    config)

#_(defn with-error-handling
    "Add error handling to request"
    [entity-type config]
    (update config :on-failure
      #(vec (concat [:app.template.frontend.events.list.crud/fetch-failure entity-type]
              (if (vector? %) % [%])))))

#_(defn with-error-state
    "Add error state management to request"
    [request error-path]
    (-> request
      (update :on-failure #(into [[:db/assoc-in error-path (extract-error-message %)]] %))))

#_(defn add-auth-header
    "Add authorization header to request"
    [request token]
    (assoc-in request [:headers "Authorization"] (str "Bearer " token)))

#_(defn add-csrf-token
    "Add CSRF token to request headers"
    [request token]
    (assoc-in request [:headers "X-CSRF-Token"] token))
