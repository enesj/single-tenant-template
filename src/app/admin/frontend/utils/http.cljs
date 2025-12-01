(ns app.admin.frontend.utils.http
  "Centralized HTTP utilities for admin frontend operations.

   Provides consistent request configuration, authentication, error handling,
   and timeout management for all admin API calls."
  (:require
    [ajax.core :as ajax]
    [taoensso.timbre :as log]))

;; ============================================================================
;; Constants and Configuration
;; ============================================================================

(def ^:private default-timeout 10000) ; 10 seconds
(def ^:private default-headers {"Content-Type" "application/json"})

;; ============================================================================
;; Request Builders
;; ============================================================================

(defn admin-request
  "Creates a standardized HTTP request configuration for admin API calls.

   Automatically includes:
   - Admin token authentication from localStorage or db
   - Proper JSON request/response formatting
   - Timeout protection
   - Standard error handling

   Options:
   - method: HTTP method (:get, :post, :put, :delete, :patch)
   - uri: Request URI (should start with /admin/api/)
   - params: Request parameters (for POST/PUT body or GET query params)
   - headers: Additional headers (merged with defaults)
   - timeout: Custom timeout in milliseconds (default: 10000ms)
   - on-success: Success event vector
   - on-failure: Failure event vector
   - token: Override token (otherwise fetched automatically)"
  [{:keys [method uri params headers timeout on-success on-failure token]
    :or {headers {}
         timeout default-timeout}}]
  (let [admin-token (or token
                      (.getItem js/localStorage "admin-token"))
        final-headers (cond-> (merge default-headers headers)
                        admin-token (assoc "x-admin-token" admin-token))]

    (when-not admin-token
      (log/warn "Admin request without token:" uri))

    (cond-> {:method method
             :uri uri
             :headers final-headers
             :format (ajax/json-request-format)
             :response-format (ajax/json-response-format {:keywords? true})
             :timeout timeout
             :on-success on-success
             :on-failure on-failure}
      params (assoc :params params))))

;; ============================================================================
;; HTTP Method Helpers
;; ============================================================================

(defn admin-get
  "GET request to admin API endpoint"
  [opts]
  (admin-request (assoc opts :method :get)))

(defn admin-post
  "POST request to admin API endpoint"
  [opts]
  (admin-request (assoc opts :method :post)))

(defn admin-put
  "PUT request to admin API endpoint"
  [opts]
  (admin-request (assoc opts :method :put)))

(defn admin-delete
  "DELETE request to admin API endpoint"
  [opts]
  (admin-request (assoc opts :method :delete)))

(defn admin-patch
  "PATCH request to admin API endpoint"
  [opts]
  (admin-request (assoc opts :method :patch)))

;; ============================================================================
;; Specialized Request Builders
;; ============================================================================

(defn auth-request
  "Authentication request (login/logout) - doesn't require existing token"
  [{:keys [method uri params on-success on-failure]
    :or {method :post}}]
  {:method method
   :uri uri
   :params params
   :format (ajax/json-request-format)
   :response-format (ajax/json-response-format {:keywords? true})
   :timeout default-timeout
   :on-success on-success
   :on-failure on-failure})

(defn dashboard-request
  "Dashboard data request with enhanced error handling"
  [{:keys [uri on-success on-failure]
    :or {uri "/admin/api/dashboard"}}]
  (admin-get {:uri uri
              :on-success on-success
              :on-failure on-failure}))

(defn entity-request
  "CRUD request for admin entity management"
  [{:keys [method entity-type id params on-success on-failure]}]
  (let [base-uri (str "/admin/api/entities/" (name entity-type))
        uri (if id (str base-uri "/" id) base-uri)]
    (admin-request {:method method
                    :uri uri
                    :params params
                    :on-success on-success
                    :on-failure on-failure})))

(defn bulk-operation-request
  "Bulk operation request for multiple entities"
  [{:keys [entity-type operation params on-success on-failure]}]
  (admin-post {:uri (str "/admin/api/" (name entity-type) "/bulk/" (name operation))
               :params params
               :on-success on-success
               :on-failure on-failure}))

(defn export-request
  "Export data request with longer timeout for large datasets"
  [{:keys [entity-type params on-success on-failure]}]
  (admin-post {:uri (str "/admin/api/" (name entity-type) "/export")
               :params params
               :timeout 30000 ; 30 seconds for exports
               :on-success on-success
               :on-failure on-failure}))

;; ============================================================================
;; Error Handling Utilities
;; ============================================================================

(defn extract-error-message
  "Extract user-friendly error message from API response"
  [error-response]
  (or (get-in error-response [:response :error])
    (get-in error-response [:response :message])
    (:error error-response)
    (:message error-response)
    "An unexpected error occurred"))

(defn log-request-error
  "Log request error with context for debugging"
  [context error-response]
  (log/error "Admin API request failed:"
    {:context context
     :error (extract-error-message error-response)
     :status (get-in error-response [:response :status])
     :uri (get-in error-response [:uri])}))

;; ============================================================================
;; Re-frame Integration Helpers
;; ============================================================================

(defn with-loading-state
  "Wrap request with loading state management"
  [request loading-path]
  (assoc request
    :db-before-loading loading-path
    :db-after-success loading-path
    :db-after-failure loading-path))

(defn create-standard-handlers
  "Create standard success/failure event handlers for common patterns"
  [base-event-name]
  {:on-success [base-event-name :success]
   :on-failure [base-event-name :failure]})
