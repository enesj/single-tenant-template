(ns app.template.backend.routes.auth
  "Auth HTTP routes; keep request/response shape consistent."
  (:require
    [app.shared.auth :as shared-auth]
    [app.shared.data :as shared-data]
    [app.shared.date :as shared-date]
    [app.template.backend.auth.service :as auth-service]
    [app.template.backend.auth.tenant :as tenant-auth]
    [app.template.backend.routes.utils :as route-utils :refer [get-service-container]]
    [app.template.backend.services.monitoring.login-events :as login-monitoring]
    [app.shared.http :as http :refer [json-response]]
    [cheshire.core :as json]
    [clojure.walk :as walk]
    [ring.util.response :as response]
    [taoensso.timbre :as log]))

;; Auth Routes
;;
;; This namespace provides route handlers for authentication.
;; It uses the AuthenticationService to perform the actual logic.

(def logout-handler
  "Handle logout by clearing *user* auth from the session.

  IMPORTANT: We keep other session keys (e.g. :admin-token) intact so that
  signing out of the user UI does not implicitly sign out of the admin UI."
  (fn [req]
    (log/info "Processing logout request")
    (let [existing-session (or (:session req) {})
          new-session (-> existing-session
                        (dissoc :auth-session :user :tenant :tenant-id :user-id))]
      (-> (response/response (json/generate-string {:success true}))
        (response/content-type "application/json")
        (assoc :session (when (seq new-session) new-session))))))

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

(defn register-handler
  "Handler for user registration"
  [auth-service]
  (fn [req]
    (route-utils/with-error-handling "user-registration"
      (let [{:keys [email full-name password]} (:body-params req)
            existing-session (or (:session req) {})]
        (log/info "Processing registration request for:" email)
        ;; Note: We log the keys of the request, but be careful not to log the password!
        (log/info "Request keys:" (keys req))
        (log/info "Body params keys:" (keys (:body-params req)))

        ;; Use a more robust check for password presence
        (when (or (empty? email) (empty? password))
          (log/warn "Missing email or password in registration request")
          (http/bad-request-response "Email and password are required"))

        ;; Debug params if needed
        (log/debug "Email present:" (not (empty? email)))
        (log/debug "Password present:" (not (empty? password)))

        ;; Call registration service (handles validation, email verification and email sending)
        (let [{:keys [db]} (get-service-container req)
              config (get-in req [:service-container :config])
              result (auth-service/register-user-with-password!
                       auth-service
                       {:email email
                        :full-name full-name
                        :password password})
              {:keys [user verification-required]} result
              sanitized-user (sanitize-for-serialization user)]

          (if verification-required
            ;; User created but needs email verification — no tenant provisioning yet
            (let [new-session (assoc existing-session :auth-session {:user sanitized-user})]
              (-> (json-response
                    {:success true
                     :verification-required true
                     :message "Registration successful. Please check your email for verification."})
                ;; IMPORTANT: merge with existing session (e.g. :admin-token) instead of overwriting.
                (assoc :session new-session)))

            ;; User registered successfully without verification — provision tenant now
            (let [tenant-ctx   (tenant-auth/resolve-tenant-context db config user
                                 {:client-ip (:remote-addr req)})
                  auth-session (sanitize-for-serialization
                                 (tenant-auth/build-auth-session {:user sanitized-user} tenant-ctx))
                  new-session  (assoc existing-session :auth-session auth-session)]
              (-> (json-response {:success true
                                  :verification-required false
                                  :user sanitized-user
                                  :tenant (sanitize-for-serialization (:tenant auth-session))})
                (assoc :session new-session)))))))))

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
            ua (get headers "user-agent")
            existing-session (or (:session req) {})]

        ;; Validate required fields
        (when (or (empty? email) (empty? password))
          (http/bad-request-response "Email and password are required"))

        ;; Attempt authentication - wrap in try/catch since it throws on failure
        (try
          (let [auth-result (auth-service/login-with-password
                              auth-service {:email email :password password})
                user-raw  (:user auth-result)
                user-safe (sanitize-for-serialization user-raw)
                user-id   (:id user-raw)
                ;; Resolve tenant context (provision / auto-set / selection)
                config       (get-in req [:service-container :config])
                tenant-ctx   (tenant-auth/resolve-tenant-context db config user-raw
                               {:client-ip (:remote-addr req)})
                auth-session (sanitize-for-serialization
                               (tenant-auth/build-auth-session {:user user-safe} tenant-ctx))
                new-session  (assoc existing-session :auth-session auth-session)
                ;; Build response body with tenant info
                response-body (cond-> {:success true :user user-safe}
                                (:tenant auth-session)
                                (assoc :tenant (sanitize-for-serialization (:tenant auth-session)))

                                (:tenant-selection-required auth-session)
                                (assoc :tenant-selection-required true
                                  :available-tenants (sanitize-for-serialization
                                                       (:available-tenants auth-session))))]

            ;; Record successful login
            (login-monitoring/record-login-event! db
              {:principal-type :user
               :principal-id user-id
               :success true
               :reason nil
               :ip ip
               :user-agent ua})

            ;; Return success response with session
            (-> (json-response response-body)
              ;; IMPORTANT: merge with existing session (e.g. :admin-token) instead of overwriting.
              (assoc :session new-session)))

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
                :validation-error (http/unauthorized-response "Invalid email or password")
                :forbidden (http/forbidden-response "Account is not active")
                ;; Default error
                (http/unauthorized-response "Invalid email or password")))))))))

(defn auth-status-handler
  "Handle authentication status check"
  [req]
  (route-utils/with-error-handling "auth-status"
    (let [;; Check for our new auth session first
          auth-session (get-in req [:session :auth-session])
          user (:user auth-session)
          provider (or (:provider auth-session)
                     (:auth_provider user)
                     (:auth-provider user))]
      (cond
        ;; New session format (from our auth service)
        auth-session
        (let [safe-user (dissoc user :password_hash :password-hash :users/password_hash)
              body (cond-> {:authenticated true
                            :session-valid (not (shared-date/session-expired? auth-session))
                            :user safe-user
                            :tenant (:tenant auth-session)
                            :permissions (shared-auth/get-user-permissions user)}
                     provider
                     (assoc :provider (if (keyword? provider) (name provider) (str provider)))

                     (:membership auth-session)
                     (assoc :membership-role (get-in auth-session [:membership :role]))

                     (:tenant-selection-required auth-session)
                     (assoc :tenant-selection-required true
                       :available-tenants (:available-tenants auth-session)))]
          {:status 200
           :headers {"Content-Type" "application/json"}
           :body (json/generate-string body)})

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
                session (auth-service/process-oauth-callback auth-service test-oauth-data :test)
                user (sanitize-for-serialization (:user session))
                existing-session (or (:session req) {})
                new-session (assoc existing-session :auth-session {:user user})]

            (log/info "Test authentication session created for user:" (:email (:user session)))

            {:status 200
             :headers {"Content-Type" "application/json"}
             :body (json/generate-string
                     {:success true
                      :message "Test authentication session created"
                      :session {:user user}})
             ;; IMPORTANT: merge with existing session (e.g. :admin-token).
             :session new-session}))))))

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
