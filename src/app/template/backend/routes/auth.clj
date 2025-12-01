(ns app.template.backend.routes.auth
  "Template authentication routes using service-oriented architecture"
  (:require
    [app.backend.services.monitoring.login-events :as login-monitoring]
    [app.template.shared.auth :as shared-auth]
    [app.shared.data :as shared-data]
    [app.shared.date :as shared-date]
    [app.template.backend.auth.service :as auth-service]
    [app.template.backend.routes.utils :as route-utils]
    [cheshire.core :as json]
    [clojure.walk :as walk]
    [ring.util.response :as response]
    [taoensso.timbre :as log]))

;; Forward declaration to satisfy usages above its definition
(declare sanitize-for-serialization)

;; Utility functions for error responses
(defn error-response
  [message status]
  {:status status
   :headers {"Content-Type" "application/json"}
   :body (cheshire.core/generate-string {:error message})})

;; Utility functions for JSON responses
(defn json-response
  [body & [session-data]]
  (let [response {:status 200
                   :headers {"Content-Type" "application/json"}
                   :body (cheshire.core/generate-string body)}]
    (if session-data
      (assoc response :session session-data)
      response)))

(defn get-service-container
  "Extract the template service container from the Ring request.
   The backend webserver middleware associates it under :service-container."
  [req]
  (:service-container req))

(defn logout-handler
  "Handle user logout by clearing session"
  [_]
  (-> (response/redirect "/about")
    (assoc :session nil)))

;; NEW: User registration endpoint
(defn register-handler
  [auth-service]
  (fn [req]
    (route-utils/with-error-handling "user-register"
      (let [{:keys [db]} (get-service-container req)
            ;; Support both camelCase and kebab-case from frontend
            {:keys [email full-name fullName password]} (:body-params req)
            ;; Canonicalize names
            email (or email (get (:body-params req) :email))
            full-name (or full-name fullName (get (:body-params req) :full_name))
            password password]
        (log/info "register-handler body-params" (:body-params req))

        ;; Call registration service (handles validation, email verification and email sending)
        (let [result (auth-service/register-user-with-password!
                       auth-service
                       {:email email
                        :full-name full-name
                        :password password})
              {:keys [user verification-required]} result
              sanitized-user (sanitize-for-serialization user)]

          (if verification-required
            ;; User created but needs email verification
            (-> (json-response
                  {:success true
                   :verification-required true
                   :message "Registration successful. Please check your email for verification."})
                ;; Store only EDN-serializable data in Ring session cookie
                (assoc-in [:session :auth-session] {:user sanitized-user}))

            ;; User registered successfully without verification requirement
            (-> (json-response {:success true
                                :verification-required false
                                :user sanitized-user})
                (assoc-in [:session :auth-session] {:user sanitized-user}))))))))

;; NEW: Email/password login endpoint
(defn login-handler
  [auth-service]
  (fn [req]
    (route-utils/with-error-handling "user-login"
      (let [{:keys [db]} (get-service-container req)
            {:keys [email password]} (:body-params req)
            ;; Ring already provides remote-addr and headers on the request
            remote-addr (:remote-addr req)
            headers (:headers req)
            ip (or remote-addr (get headers "x-forwarded-for"))
            ua (get headers "user-agent")]

        ;; Validate required fields
        (when (or (empty? email) (empty? password))
          (error-response "Email and password are required" 400))

        ;; Attempt authentication - wrap in try/catch since it throws on failure
        (try
          (let [auth-result (auth-service/login-with-password
                              auth-service {:email email :password password})
                user-safe (sanitize-for-serialization (:user auth-result))
                user-id (:id (:user auth-result))]

            ;; Record successful login
            (login-monitoring/record-login-event! db
              {:principal-type :user
               :principal-id user-id
               :success true
               :reason nil
               :ip ip
               :user-agent ua})

            ;; Return success response with session
            (-> (json-response {:success true :user user-safe})
              ;; Store only serializable user data in session (Ring cookie store requirement)
              (assoc-in [:session :auth-session] {:user user-safe})))
          
          (catch clojure.lang.ExceptionInfo e
            ;; Handle authentication failure
            (let [ex-data (ex-data e)]
              (login-monitoring/record-login-event! db
                {:principal-type :user
                 :principal-id nil
                 :success false
                 :reason "invalid_credentials"
                 :ip ip
                 :user-agent ua})
              
              ;; Return appropriate error based on exception type
              (case (:type ex-data)
                :validation-error (error-response "Invalid email or password" 401)
                :forbidden (error-response "Account is not active" 403)
                ;; Default error
                (error-response "Invalid email or password" 401)))))))))

(defn- sanitize-for-serialization
  "Helper function to sanitize objects for JSON/EDN serialization"
  [obj]
  (walk/postwalk
    (fn [x]
      (cond
        ;; Handle all UUID types
        (instance? java.util.UUID x) (str x)
        ;; Handle all time/date types
        (instance? java.time.LocalDateTime x) (str x)
        (instance? java.time.ZonedDateTime x) (str x)
        (instance? java.time.OffsetDateTime x) (str x)
        (instance? java.time.Instant x) (str x)
        (instance? java.time.LocalDate x) (str x)
        (instance? java.time.LocalTime x) (str x)
        ;; Handle SQL types
        (instance? java.sql.Timestamp x) (str x)
        (instance? java.sql.Date x) (str x)
        (instance? java.sql.Time x) (str x)
        ;; Handle numeric types that might not serialize
        (instance? java.math.BigDecimal x) (str x)
        (instance? java.math.BigInteger x) (str x)
        :else x))
    obj))

(defn auth-status-handler
  "Handle authentication status check"
  [req]
  (route-utils/with-error-handling "auth-status"
    (let [;; Check for our new auth session first
          auth-session (get-in req [:session :auth-session])

          ;; Fall back to old OAuth tokens for backward compatibility
          session-tokens (get-in req [:session :ring.middleware.oauth2/access-tokens])
          req-tokens (get-in req [:ring.middleware.oauth2/access-tokens])
          oauth-tokens (or session-tokens req-tokens)]

      (cond
        ;; New session format (from our auth service)
        auth-session
        {:status 200
         :headers {"Content-Type" "application/json"}
         :body (json/generate-string
                 {:authenticated true
                  :session-valid (not (shared-date/session-expired? auth-session))
                  :user (:user auth-session)
                  :tenant (:tenant auth-session)
                  :permissions (shared-auth/get-user-permissions (:user auth-session))})}

        ;; Legacy OAuth format (for backward compatibility)
        oauth-tokens
        (let [provider (-> oauth-tokens keys first)]
          {:status 200
           :headers {"Content-Type" "application/json"}
           :body (json/generate-string
                   {:authenticated true
                    :legacy-session true
                    :provider (when provider (name provider))
                    :user nil})})

        ;; Not authenticated
        :else
        {:status 200
         :headers {"Content-Type" "application/json"}
         :body (json/generate-string
                 {:authenticated false})}))))

(defn test-auth-handler
  "Handler for POST /api/v1/test/auth - Creates test authentication session"
  [auth-service]
  (fn [req]
    (route-utils/with-error-handling "test-auth"
      (let [test-mode? (or (= "development" (System/getProperty "app.environment"))
                         (= (shared-data/get-server-port (:service-container req))
                           (:server-port req)))]
        (if-not test-mode?
          {:status 403
           :headers {"Content-Type" "application/json"}
           :body (json/generate-string {:error "Test authentication only available in development mode"})}

          (let [test-oauth-data {:email "test@example.com"
                                 :name "Test User"
                                 :hd nil}
                session (auth-service/process-oauth-callback auth-service test-oauth-data :test)]

            (log/info "Test authentication session created for user:" (:email (:user session)))

            {:status 200
             :headers {"Content-Type" "application/json"}
             :body (json/generate-string
                     {:success true
                      :message "Test authentication session created"
                      :session {:user (sanitize-for-serialization (:user session))}})
             ;; Store ONLY serializable user data in session (Ring cookie store requirement)
             :session {:auth-session {:user (sanitize-for-serialization (:user session))}}}))))))

(defn create-auth-routes
  "Create authentication routes that use the authentication service"
  [auth-service]
  {:logout-handler logout-handler
   :auth-status-handler auth-status-handler
   :test-auth-handler (test-auth-handler auth-service)
   ;; NEW: User registration endpoint
   :register-handler (register-handler auth-service)
   ;; NEW: Email/password login endpoint
   :login-handler (login-handler auth-service)})
