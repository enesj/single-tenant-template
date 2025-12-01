(ns app.shared.http
  "Cross-platform HTTP constants and utilities for consistent API communication.
   Provides status codes, content types, and common response patterns
   that work in both Clojure and ClojureScript environments.")

;; -------------------------
;; HTTP Status Codes
;; -------------------------

;; 2xx Success
(def status-ok 200)
(def status-created 201)
(def status-accepted 202)
(def status-no-content 204)

;; 3xx Redirection
(def status-moved-permanently 301)
(def status-found 302)
(def status-not-modified 304)

;; 4xx Client Error
(def status-bad-request 400)
(def status-unauthorized 401)
(def status-forbidden 403)
(def status-not-found 404)
(def status-method-not-allowed 405)
(def status-conflict 409)
(def status-unprocessable-entity 422)
(def status-too-many-requests 429)

;; 5xx Server Error
(def status-internal-server-error 500)
(def status-not-implemented 501)
(def status-bad-gateway 502)
(def status-service-unavailable 503)
(def status-gateway-timeout 504)

;; Status code categories
(defn success-status? [status] (and (>= status 200) (< status 300)))
(defn redirect-status? [status] (and (>= status 300) (< status 400)))
(defn client-error-status? [status] (and (>= status 400) (< status 500)))
(defn server-error-status? [status] (and (>= status 500) (< status 600)))
(defn error-status? [status] (>= status 400))

;; -------------------------
;; Content Types
;; -------------------------

(def content-type-json "application/json")
(def content-type-json-utf8 "application/json; charset=utf-8")
(def content-type-html "text/html")
(def content-type-html-utf8 "text/html; charset=utf-8")
(def content-type-text "text/plain")
(def content-type-text-utf8 "text/plain; charset=utf-8")
(def content-type-xml "application/xml")
(def content-type-form-encoded "application/x-www-form-urlencoded")
(def content-type-multipart "multipart/form-data")
(def content-type-octet-stream "application/octet-stream")

;; Prometheus metrics content type
(def content-type-prometheus "text/plain; version=0.0.4; charset=utf-8")

;; -------------------------
;; Common HTTP Headers
;; -------------------------

(def header-content-type "Content-Type")
(def header-accept "Accept")
(def header-authorization "Authorization")
(def header-user-agent "User-Agent")
(def header-x-requested-with "X-Requested-With")
(def header-x-csrf-token "X-CSRFToken")
(def header-cache-control "Cache-Control")
(def header-location "Location")

;; -------------------------
;; HTTP Methods
;; -------------------------

(def method-get :get)
(def method-post :post)
(def method-put :put)
(def method-patch :patch)
(def method-delete :delete)
(def method-head :head)
(def method-options :options)

;; -------------------------
;; Standard Response Structures
;; -------------------------

(defn success-response
  "Create a successful response with optional data"
  ([data]
   {:status status-ok
    :body data})
  ([status data]
   {:status status
    :body data}))

(defn json-response
  "Create a JSON response with proper headers"
  ([data]
   (json-response status-ok data))
  ([status data]
   {:status status
    :headers {header-content-type content-type-json-utf8}
    :body data}))

(defn created-response
  "Create a 201 Created response"
  [data]
  {:status status-created
   :headers {header-content-type content-type-json-utf8}
   :body data})

(defn no-content-response
  "Create a 204 No Content response"
  []
  {:status status-no-content})

;; -------------------------
;; Error Response Structures
;; -------------------------

(defn error-response
  "Create an error response with status and message"
  ([message]
   (error-response status-internal-server-error message))
  ([status message]
   {:status status
    :headers {header-content-type content-type-json-utf8}
    :body {:error message}})
  ([status message details]
   {:status status
    :headers {header-content-type content-type-json-utf8}
    :body {:error message
           :details details}}))

(defn bad-request-response
  "Create a 400 Bad Request response"
  ([message]
   (error-response status-bad-request message))
  ([message details]
   (error-response status-bad-request message details)))

(defn unauthorized-response
  "Create a 401 Unauthorized response"
  ([]
   (unauthorized-response "Unauthorized"))
  ([message]
   (error-response status-unauthorized message)))

(defn forbidden-response
  "Create a 403 Forbidden response"
  ([]
   (forbidden-response "Forbidden"))
  ([message]
   (error-response status-forbidden message)))

(defn not-found-response
  "Create a 404 Not Found response"
  ([]
   (not-found-response "Resource not found"))
  ([message]
   (error-response status-not-found message)))

(defn conflict-response
  "Create a 409 Conflict response"
  ([message]
   (error-response status-conflict message))
  ([message details]
   (error-response status-conflict message details)))

(defn unprocessable-entity-response
  "Create a 422 Unprocessable Entity response (validation errors)"
  ([message]
   (error-response status-unprocessable-entity message))
  ([message validation-errors]
   (error-response status-unprocessable-entity message {:validation-errors validation-errors})))

(defn internal-server-error-response
  "Create a 500 Internal Server Error response"
  ([]
   (internal-server-error-response "Internal server error"))
  ([message]
   (error-response status-internal-server-error message)))

;; -------------------------
;; Request/Response Utilities
;; -------------------------

(defn extract-error-message
  "Extract error message from various response formats (primarily for frontend)"
  [response]
  (or (:error response)
    (get-in response [:response :error])
    (get-in response [:body :error])
    (:status-text response)
    (:message response)
    "An error occurred"))

(defn get-status
  "Get status code from response"
  [response]
  (or (:status response)
    (get-in response [:response :status])
    status-internal-server-error))

(defn success?
  "Check if response indicates success"
  [response]
  (success-status? (get-status response)))

(defn error?
  "Check if response indicates an error"
  [response]
  (error-status? (get-status response)))

;; -------------------------
;; Content Type Utilities
;; -------------------------

(defn json-content-type?
  "Check if content type is JSON"
  [content-type]
  (when content-type
    (or (.contains content-type "application/json")
      (.contains content-type "application/vnd.api+json"))))

(defn html-content-type?
  "Check if content type is HTML"
  [content-type]
  (when content-type
    (.contains content-type "text/html")))

(defn text-content-type?
  "Check if content type is plain text"
  [content-type]
  (when content-type
    (.contains content-type "text/plain")))

;; -------------------------
;; API Response Builders
;; -------------------------

(defn api-success
  "Build a standard API success response"
  ([data]
   (json-response status-ok {:success true :data data}))
  ([data meta]
   (json-response status-ok {:success true :data data :meta meta})))

(defn api-error
  "Build a standard API error response"
  ([message]
   (api-error status-internal-server-error message))
  ([status message]
   (json-response status {:success false :error message}))
  ([status message details]
   (json-response status {:success false :error message :details details})))

(defn api-validation-error
  "Build a validation error response"
  [validation-errors]
  (json-response status-unprocessable-entity
    {:success false
     :error "Validation failed"
     :validation-errors validation-errors}))

;; -------------------------
;; Health Check Response
;; -------------------------

(defn health-check-response
  "Standard health check response"
  ([]
   (health-check-response {}))
  ([additional-data]
   (json-response status-ok
     (merge {:status "ok"
             :timestamp #?(:clj (System/currentTimeMillis)
                           :cljs (.getTime (js/Date.)))
             :database "connected"}
       additional-data))))

;; -------------------------
;; CSRF Token Response
;; -------------------------

(defn csrf-token-response
  "Standard CSRF token response"
  [token]
  (json-response status-ok {:csrf-token token}))

;; -------------------------
;; Common Error Messages
;; -------------------------

(def error-messages
  {:unauthorized "Authentication required"
   :forbidden "Access denied"
   :not-found "Resource not found"
   :validation-failed "Validation failed"
   :conflict "Resource already exists"
   :internal-error "Internal server error"
   :bad-request "Invalid request"
   :rate-limited "Too many requests"
   :service-unavailable "Service temporarily unavailable"})
