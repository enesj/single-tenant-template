(ns app.template.frontend.api.http
  "HTTP request utilities for consistent API communication.
   Provides request builders and common configurations."
  (:require
    [ajax.core :as ajax]
    [app.shared.http :as http]
    [app.template.frontend.api :as api]
    [re-frame.db :as rf-db]
    [clojure.string :as str]
    [taoensso.timbre :as log]))

;;; -------------------------
;;; Request Formats
;;; -------------------------

(def json-request-format
  "Standard JSON request format"
  (ajax/json-request-format))

(def json-response-format
  "Standard JSON response format with keyword keys"
  (ajax/json-response-format {:keywords? true}))

(def transit-request-format
  "Transit request format for rich data types"
  (ajax/transit-request-format))

(def transit-response-format
  "Transit response format with keyword keys"
  (ajax/transit-response-format {:keywords? true}))

;;; -------------------------
;;; Core Request Builder
;;; -------------------------

(defn api-request
  "Build a standard API request configuration.
   Provides sensible defaults for format, response-format, and timeout."
  [{:keys [method uri params format response-format on-success on-failure timeout headers]
    :or {format json-request-format
         response-format json-response-format
         timeout 8000}
    :as config}]
  (cond-> {:method method
           :uri uri
           :format format
           :response-format response-format
           :on-success on-success
           :on-failure on-failure
           :timeout timeout}
    params (assoc :params params)
    headers (assoc :headers headers)))

;; Alias for backwards compatibility
(def build-request api-request)

;;; -------------------------
;;; HTTP Method Helpers
;;; -------------------------

(defn get-request
  "Create a GET request configuration"
  [{:keys [uri on-success on-failure] :as opts}]
  (api-request (assoc opts :method :get)))

(defn post-request
  "Create a POST request configuration"
  [{:keys [uri params on-success on-failure] :as opts}]
  (api-request (assoc opts :method :post :params params)))

(defn put-request
  "Create a PUT request configuration"
  [{:keys [uri params on-success on-failure] :as opts}]
  (api-request (assoc opts :method :put :params params)))

(defn delete-request
  "Create a DELETE request configuration"
  [{:keys [uri on-success on-failure] :as opts}]
  (api-request (assoc opts :method :delete)))

(defn patch-request
  "Create a PATCH request configuration"
  [{:keys [uri params on-success on-failure] :as opts}]
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

(defn create-entity
  "Create a new entity"
  [{:keys [entity-name data on-success on-failure]}]
  (let [db (try @rf-db/app-db (catch :default _ nil))
        admin-token (or (:admin/token db)
                      (try (when (exists? js/localStorage)
                             (.getItem js/localStorage "admin-token"))
                        (catch :default _ nil)))
        pathname (when (exists? js/window)
                   (some-> js/window .-location .-pathname))
        hostname (when (exists? js/window)
                   (some-> js/window .-location .-hostname))
        admin-uri (str "/admin/api/" entity-name)
        uri (if (and admin-token (or (and pathname (str/includes? pathname "/admin"))
                                   (and hostname (str/includes? (str/lower-case hostname) "admin"))))
              admin-uri
              (api/entity-endpoint entity-name))]
    (post-request
      {:uri uri
       :params data
       :on-success on-success
       :on-failure on-failure})))

(defn update-entity
  "Update an existing entity"
  [{:keys [entity-name id data on-success on-failure]}]
  (let [db (try @rf-db/app-db (catch :default _ nil))
        admin-token (or (:admin/token db)
                      (try (when (exists? js/localStorage)
                             (.getItem js/localStorage "admin-token"))
                        (catch :default _ nil)))
        pathname (when (exists? js/window)
                   (some-> js/window .-location .-pathname))
        hostname (when (exists? js/window)
                   (some-> js/window .-location .-hostname))
        admin-uri (str "/admin/api/" entity-name "/" id)
        uri (if (and admin-token (or (and pathname (str/includes? pathname "/admin"))
                                   (and hostname (str/includes? (str/lower-case hostname) "admin"))))
              admin-uri
              (api/entity-endpoint entity-name id))]
    (put-request
      {:uri uri
       :params data
       :on-success on-success
       :on-failure on-failure})))

(defn delete-entity
  "Delete an entity"
  [{:keys [entity-name id on-success on-failure]}]
  (delete-request
    {:uri (api/entity-endpoint entity-name id)
     :on-success on-success
     :on-failure on-failure}))

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
(def entity-get-all get-entities)
(def entity-get-by-id get-entity)
(def entity-create create-entity)
(def entity-update update-entity)
(def entity-delete delete-entity)
(def entity-batch-update batch-update-entities)

;;; -------------------------
;;; Error Handling
;;; -------------------------

(defn extract-error-message
  "Extract error message from various response formats - delegates to shared utilities"
  [response]
  (http/extract-error-message response))

(defn log-request-error
  "Log request errors with context"
  [context response]
  (log/error (str "Request failed - " context ": ")
    (extract-error-message response)))

;;; -------------------------
;;; Request Interceptors
;;; -------------------------

(defn with-loading-state
  "Add loading state management to request - Loading is now handled by fetch events"
  [_entity-type config]
  ;; Loading state is now managed by fetch-entities/fetch-success/fetch-failure events
  ;; This function is kept for backwards compatibility but is effectively a no-op
  config)

(defn with-error-handling
  "Add error handling to request"
  [entity-type config]
  (update config :on-failure
    #(vec (concat [:app.template.frontend.events.list.crud/fetch-failure entity-type]
            (if (vector? %) % [%])))))

(defn with-error-state
  "Add error state management to request"
  [request error-path]
  (-> request
    (update :on-failure #(into [[:db/assoc-in error-path (extract-error-message %)]] %))))

(defn add-auth-header
  "Add authorization header to request"
  [request token]
  (assoc-in request [:headers "Authorization"] (str "Bearer " token)))

(defn add-csrf-token
  "Add CSRF token to request headers"
  [request token]
  (assoc-in request [:headers "X-CSRF-Token"] token))
