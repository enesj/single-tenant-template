(ns app.shared.http
  "HTTP response helpers/status codes; keep shapes stable."
  (:require
    [clojure.string :as str]
    #?(:clj [cheshire.core :as json])))

;; -------------------------
;; HTTP Status Codes
;; -------------------------

;; 2xx Success
(def status-ok 200)

;; 3xx Redirection

;; 4xx Client Error
(def status-bad-request 400)
(def status-unauthorized 401)
(def status-forbidden 403)

;; 5xx Server Error
(def status-internal-server-error 500)

;; -------------------------
;; Content Types
;; -------------------------

(def content-type-json "application/json")
(def content-type-json-utf8 "application/json; charset=utf-8")

;; -------------------------
;; Common HTTP Headers
;; -------------------------

(def header-content-type "Content-Type")

;; App-specific auth header used by the admin UI + backend.
(def header-x-admin-token "x-admin-token")

;; -------------------------
;; HTTP Methods
;; -------------------------

(defn json-response
  "Create a JSON response with proper headers"
  ([data]
   (json-response status-ok data))
  ([status data]
   {:status status
    :headers {header-content-type content-type-json-utf8}
    :body data}))

#?(:clj
   (defn encode-json-body
     "Encode a Ring response map's :body as a JSON string.

     Useful for routes that do not use reitit/muuntaja response middleware and
     therefore must return a string body for JSON responses.

     - Preserves existing headers (and status)
     - Ensures Content-Type is set (defaults to application/json; charset=utf-8)
     - No-op when :body is already a string"
     [{:keys [headers body] :as response}]
     (let [content-type (or (get headers header-content-type)
                          content-type-json-utf8)]
       (-> response
         (update :headers #(assoc (or % {}) header-content-type content-type))
         (cond-> (not (string? body))
           (assoc :body (json/generate-string body)))))))

#?(:clj
   (defn json-string-response
     "Create a JSON response whose :body is a JSON string.

     Prefer `json-response` in routes that have response encoding middleware
     (e.g. reitit/muuntaja)."
     ([data]
      (json-string-response status-ok data))
     ([status data]
      (encode-json-body (json-response status data)))))

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

#?(:clj
   (defn error-string-response
     "Create an error response whose :body is a JSON string.

     Mirrors `error-response` arities, but encodes the body for routes without
     response encoding middleware."
     ([message]
      (encode-json-body (error-response message)))
     ([status message]
      (encode-json-body (error-response status message)))
     ([status message details]
      (encode-json-body (error-response status message details)))))

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

;; -------------------------
;; Request/Response Utilities
;; -------------------------

(defn extract-error-message
  "Extract error message from various response formats (primarily for frontend).

    Arity:
    - (extract-error-message response)
    - (extract-error-message response default-message)"
  ([response]
   (extract-error-message response "An error occurred"))
  ([response default-message]
   (or (:error response)
     (get-in response [:response :error])
     (get-in response [:response :message])
     (get-in response [:body :error])
     (get-in response [:body :message])
     (:status-text response)
     (:message response)
     default-message)))

;; =============================================================================
;; Public API helpers (docs/shared/http-utilities.md)
;; =============================================================================

(defn- get-status
  "Get a best-effort status code from various response shapes."
  [response]
  (or (:status response)
    (get-in response [:response :status])
    status-internal-server-error))

(defn success?
  "True when the response status is in the 2xx range."
  [response]
  (let [status (get-status response)]
    (and (number? status) (<= 200 status 299))))

(defn client-error?
  "True when the response status is in the 4xx range."
  [response]
  (let [status (get-status response)]
    (and (number? status) (<= 400 status 499))))

(defn server-error?
  "True when the response status is in the 5xx range."
  [response]
  (let [status (get-status response)]
    (and (number? status) (<= 500 status 599))))

(defn create-json-headers
  "Create a headers map that declares JSON (UTF-8)."
  ([]
   {header-content-type content-type-json-utf8})
  ([headers]
   (merge (create-json-headers) (or headers {}))))

(defn create-auth-headers
  "Create auth headers for a given token.

  Defaults to the app's admin token header (x-admin-token), but supports
  overriding the header name for callers that use Authorization/Bearer.

  Arity:
  - (create-auth-headers token)
  - (create-auth-headers token header-name)"
  ([token]
   (create-auth-headers token header-x-admin-token))
  ([token header-name]
   (if (some? token)
     {(or header-name header-x-admin-token) token}
     {})))

(defn merge-headers
  "Nil-safe merge for header maps."
  ([a b]
   (merge (or a {}) (or b {})))
  ([a b & more]
   (apply merge (or a {}) (or b {}) more)))

(defn is-json?
  "Check whether a content-type (or response) indicates JSON.

  Arity:
  - (is-json? content-type-string)
  - (is-json? response-map)  ; checks response headers"
  [x]
  (let [content-type (if (string? x)
                       x
                       (or (get-in x [:headers header-content-type])
                         (get-in x [:response :headers header-content-type])
                         (get-in x [:response :headers (keyword header-content-type)])))]
    (when (string? content-type)
      (let [ct (str/lower-case content-type)]
        (or (str/includes? ct "application/json")
          (str/includes? ct "application/vnd.api+json"))))))

(defn create-success-response
  "Alias for `json-response` (stable doc-level API)."
  ([data]
   (json-response data))
  ([status data]
   (json-response status data)))

(defn create-error-response
  "Alias for `error-response` (stable doc-level API)."
  ([message]
   (error-response message))
  ([status message]
   (error-response status message))
  ([status message details]
   (error-response status message details)))
