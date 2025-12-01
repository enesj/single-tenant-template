(ns app.backend.routes.admin.utils
  "Shared utilities for admin API routes"
  (:require
    [cheshire.core :as json]
    [clojure.string :as str]
    [ring.util.response :as response]
    [taoensso.timbre :as log]))

;; Response Generation Utilities

(defn json-response
  "Generate a JSON response with optional status"
  [data & {:keys [status] :or {status 200}}]
  (-> (response/response (json/generate-string data))
    (response/content-type "application/json")
    (response/status status)))

(defn error-response
  "Generate a JSON error response. Optionally include :details in the body so
  callers can surface context (e.g., suggestions) to the frontend."
  [message & {:keys [status details] :or {status 500}}]
  (json-response (cond-> {:error message}
                   details (assoc :details details))
    :status status))

(defn success-response
  "Generate a JSON success response"
  [& [data]]
  (json-response (merge {:success true} (or data {}))))

;; Request Context Extraction

(defn extract-request-context
  "Extract common request context (IP, user agent, admin)"
  [request]
  {:ip-address (or (get-in request [:headers "x-forwarded-for"])
                 (get-in request [:headers "x-real-ip"])
                 (:remote-addr request))
   :user-agent (get-in request [:headers "user-agent"])
   :admin (:admin request)})

(defn get-admin-id
  "Extract admin ID from request"
  [request]
  (-> request :admin :id))

;; UUID Parsing & Validation

(defn parse-uuid-custom
  "Parse a string to UUID, returns nil if invalid"
  [s]
  (when s
    (try
      (java.util.UUID/fromString s)
      (catch IllegalArgumentException _
        nil))))

(defn parse-and-validate-uuid
  "Parse and validate UUID with descriptive error"
  [id-str param-name]
  (if id-str
    (if-let [uuid (parse-uuid-custom id-str)]
      {:success true :uuid uuid}
      {:success false :error (str "Invalid " param-name)})
    {:success false :error (str "Missing " param-name)}))

(defn extract-uuid-param
  "Extract and validate UUID from request parameters"
  [request param-key]
  (let [id-str (or (get-in request [:path-params param-key])
                 (get-in request [:params param-key]))]
    (parse-uuid-custom id-str)))

;; Parameter Extraction & Validation

(defn parse-int-param
  "Parse integer parameter with default value"
  [params key default-val]
  (if-let [val (get params key)]
    (try
      (Integer/parseInt val)
      (catch NumberFormatException _
        default-val))
    default-val))

(defn parse-boolean-param
  "Parse boolean parameter"
  [params key]
  (when-let [val (get params key)]
    (Boolean/parseBoolean val)))

(defn extract-pagination-params
  "Extract pagination parameters from request"
  [params & {:keys [default-limit default-offset]
             :or {default-limit 50 default-offset 0}}]
  {:limit (parse-int-param params :limit default-limit)
   :offset (parse-int-param params :offset default-offset)})

;; Error Handling Middleware

(defn with-error-handling
  "Wrap handler with standardized error handling"
  [handler-fn error-message]
  (fn [request]
    (try
      (handler-fn request)
      (catch Exception e
        (log/error e error-message {:message (.getMessage e) :data (ex-data e)})
        (let [data (ex-data e)]
          (if (= 400 (:status data))
            (error-response (.getMessage e) :status 400)
            (error-response error-message :status 500)))))))

(defn with-validation-error-handling
  "Handle validation errors with detailed field information"
  [handler-fn error-message]
  (fn [request]
    (try
      (handler-fn request)
      (catch Exception e
        (let [data (ex-data e)]
          (if (= 400 (:status data))
            (do
              (log/warn "Validation error:" data)
              (json-response {:error (.getMessage e)
                              :field (:field data)
                              :allowed (:allowed data)
                              :value (:value data)}
                :status 400))
            (do
              (log/error e error-message)
              (error-response error-message :status 500))))))))

;; Common Request Patterns

(defn handle-uuid-request
  "Handle request that requires a valid UUID parameter"
  [request param-key handler-fn]
  (if-let [uuid (extract-uuid-param request param-key)]
    (handler-fn uuid request)
    (error-response (str "Invalid " (name param-key)) :status 400)))

(defn handle-uuid-body-request
  "Handle request with UUID parameter and request body"
  [request param-key handler-fn]
  (if-let [uuid (extract-uuid-param request param-key)]
    (let [body (:body request)
          context (extract-request-context request)]
      (handler-fn uuid body context request))
    (error-response (str "Invalid " (name param-key)) :status 400)))

;; Middleware Utilities

(defn wrap-json-body-parsing
  "Middleware to parse JSON body if needed"
  [handler]
  (when (nil? handler)
    (log/error "🚨 CRITICAL ERROR: wrap-json-body-parsing called with nil handler!")
    (throw (IllegalArgumentException. "Handler cannot be nil")))
  (fn [request]
    (let [content-type (get-in request [:headers "content-type"])
          body (:body request)]
      (if (and content-type
            (str/includes? content-type "application/json")
            body)
        (try
          (let [parsed-body (cond
                              ;; Body already parsed by Ring middleware
                              (map? body) body
                              ;; Raw body - need to parse
                              :else
                              (let [body-str (if (string? body) body (slurp body))]
                                (if (empty? body-str)
                                  {}
                                  (json/parse-string body-str true))))]
            (handler (assoc request :body parsed-body)))
          (catch Exception e
            (log/error e "Failed to parse JSON body")
            (error-response "Invalid JSON" :status 400)))
        (handler request)))))

;; Logging Utilities

(defn log-admin-action
  "Log admin action to audit_logs table and application logs"
  [action admin-id entity-type entity-id details]
  ;; Log to application logs for immediate debugging
  (log/info "Admin action:"
    {:action action
     :admin-id admin-id
     :entity-type entity-type
     :entity-id entity-id
     :details details})

  ;; Store in audit_logs table for compliance and persistence
  (try
    (when-let [db (requiring-resolve 'system.state/db)]
      (let [audit-service (requiring-resolve 'app.backend.services.admin/log-audit!)]
        (when (and @db audit-service)
          (audit-service @db
            {:admin_id admin-id
             :action action
             :entity-type (str entity-type)
             :entity-id (when entity-id (java.util.UUID/fromString (str entity-id)))
             :changes details
             :ip-address nil
             :user-agent nil}))))
    (catch Exception e
      (log/warn "Failed to persist audit log to database:" (.getMessage e)))))

(defn get-client-ip
  "Extract client IP address from request, considering proxy headers"
  [request]
  (or (get-in request [:headers "x-forwarded-for"])
    (get-in request [:headers "x-real-ip"])
    (get-in request [:remote-addr])
    "unknown"))

(defn log-admin-action-with-context
  "Log admin action to audit_logs table with full request context"
  [action admin-id entity-type entity-id details ip-address user-agent]
  ;; Log to application logs for immediate debugging
  (log/info "Admin action with context:"
    {:action action
     :admin-id admin-id
     :entity-type entity-type
     :entity-id entity-id
     :details details
     :ip-address ip-address
     :user-agent user-agent})

  ;; Store in audit_logs table for compliance and persistence
  (try
    (when-let [db (requiring-resolve 'system.state/db)]
      (let [audit-service (requiring-resolve 'app.backend.services.admin/log-audit!)]
        (when (and @db audit-service)
          (audit-service @db
            {:admin_id admin-id
             :action action
             :entity-type (str entity-type)
             :entity-id (when entity-id (java.util.UUID/fromString (str entity-id)))
             :changes details
             :ip-address ip-address
             :user-agent user-agent}))))
    (catch Exception e
      (log/warn "Failed to persist audit log to database:" (.getMessage e)))))
