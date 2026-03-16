(ns app.template.backend.routes.auth
  "Auth HTTP routes; keep request/response shape consistent."
  (:require
    [app.shared.auth :as shared-auth]
    [app.shared.data :as shared-data]
    [app.shared.date :as shared-date]
    [app.template.backend.auth.service :as auth-service]
    [app.template.backend.auth.tenant :as tenant-auth]
    [app.template.backend.routes.utils :as route-utils]
    [app.template.backend.services.monitoring.login-events :as login-monitoring]
    [app.template.backend.services.tenant :as tenant-svc]
    [cheshire.core :as json]
    [clojure.walk :as walk]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]
    [ring.util.response :as response]
    [taoensso.timbre :as log]))

;; Auth Routes
;;
;; This namespace provides route handlers for authentication.
;; It uses the AuthenticationService to perform the actual logic.

(def logout-handler
  "Handle logout by clearing the entire session.

  Previous approach preserved non-auth keys (e.g. :admin-token), but this caused
  cookie-store serialization issues where stale auth data persisted despite logout.
  A full session clear is safer — admin sessions can be re-established independently."
  (fn [req]
    (log/info "Processing logout request")
    (-> (response/response (json/generate-string {:success true}))
      (response/content-type "application/json")
      (assoc :session nil))))

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

(defn- clear-user-auth-session
  "Remove user auth keys from session while preserving other session state."
  [session]
  (let [existing-session (or session {})
        new-session (-> existing-session
                      (dissoc :auth-session :user :tenant :tenant-id :user-id))]
    (when (seq new-session) new-session)))

(defn- normalize-status
  [value]
  (when (some? value)
    (cond
      (keyword? value) (name value)
      :else (str value))))

(defn- ->uuid
  [value]
  (cond
    (instance? java.util.UUID value) value
    (string? value) (try
                      (java.util.UUID/fromString value)
                      (catch Exception _ nil))
    :else nil))

(defn- resolve-db
  "Best-effort DB connection for request-time session validation."
  [req]
  (or (get-in req [:service-container :db])
    (some-> (get-in req [:service-container :db-adapter]) :connection)))

(defn- fetch-user-status
  [db user-id]
  (when (and db user-id)
    (when-let [u-id (->uuid user-id)]
      (let [opts {:builder-fn rs/as-unqualified-maps}
            row  (jdbc/execute-one! db
                   (sql/format {:select [:status]
                                :from   [:users]
                                :where  [:= :id u-id]})
                   opts)]
        (normalize-status (:status row))))))

(defn- fetch-tenant-status
  [db tenant-id]
  (when (and db tenant-id)
    (when-let [t-id (->uuid tenant-id)]
      (let [opts {:builder-fn rs/as-unqualified-maps}
            row  (jdbc/execute-one! db
                   (sql/format {:select [:status]
                                :from   [:tenants]
                                :where  [:= :id t-id]})
                   opts)]
        (normalize-status (:status row))))))

(defn- fetch-owner-user-status
  [db tenant-id]
  (when (and db tenant-id)
    (when-let [t-id (->uuid tenant-id)]
      (let [opts {:builder-fn rs/as-unqualified-maps}
            row  (jdbc/execute-one! db
                   (sql/format {:select [[:u.status :owner_user_status]]
                                :from   [[:tenant_memberships :tm]]
                                :join   [[:users :u] [:= :tm.user_id :u.id]]
                                :where  [:and
                                         [:= :tm.tenant_id t-id]
                                         [:= :tm.status [:cast "active" :membership_status]]
                                         [:= :tm.role [:cast "owner" :membership_role]]]
                                :limit  1})
                   opts)]
        (normalize-status (:owner_user_status row))))))

(defn- invalid-auth-session-message
  "Return nil when the auth session is valid, otherwise an error message."
  [db {:keys [user tenant]}]
  (let [user-id (->uuid (or (:id user) (:users/id user)))
        tenant-id (->uuid (or (:id tenant) (:tenants/id tenant)))
        user-status (fetch-user-status db user-id)]
    (cond
      (not= "active" user-status)
      "Account is not active"

      tenant-id
      (let [tenant-status (fetch-tenant-status db tenant-id)]
        (cond
          (not= "active" tenant-status)
          "Tenant is not active"

          (nil? (tenant-svc/get-membership db tenant-id user-id))
          "You are not an active member of this tenant"

          (not= "active" (fetch-owner-user-status db tenant-id))
          "Tenant owner account is not active"

          :else nil))

      :else
      nil)))

(defn- fetch-user-record
  [db user-id]
  (when (and db user-id)
    (when-let [u-id (->uuid user-id)]
      (let [opts {:builder-fn rs/as-unqualified-maps}
            row  (jdbc/execute-one! db
                   (sql/format {:select [:id
                                         :email
                                         :full_name
                                         :status
                                         :auth_provider
                                         :email_verified
                                         :avatar_url
                                         :created_at
                                         :updated_at]
                                :from   [:users]
                                :where  [:= :id u-id]})
                   opts)]
        (some->> row
          (map (fn [[k v]] [(keyword (name k)) v]))
          (into {}))))))

(defn- incomplete-auth-session?
  [{:keys [user tenant membership tenant-selection-required no-tenant]}]
  (and user
    (nil? tenant)
    (nil? membership)
    (not tenant-selection-required)
    (not no-tenant)))

(defn- build-auth-status-body
  [auth-session]
  (let [user     (:user auth-session)
        provider (or (:provider auth-session)
                   (:auth_provider user)
                   (:auth-provider user))
        safe-user (dissoc user :password_hash :password-hash :users/password_hash)]
    (cond-> {:authenticated true
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
        :available-tenants (:available-tenants auth-session))

      (:no-tenant auth-session)
      (assoc :no-tenant true))))

(defn- repair-incomplete-auth-session
  [req db auth-session]
  (when (and db (incomplete-auth-session? auth-session))
    (let [session-user (get auth-session :user)
          user-id      (or (:id session-user) (:users/id session-user))
          fresh-user   (fetch-user-record db user-id)]
      (cond
        (nil? fresh-user)
        {:action :clear
         :reason :missing-user
         :body {:authenticated false}}

        (not (:email_verified fresh-user))
        {:action :clear
         :reason :verification-pending
         :body {:authenticated false
                :verification-required true}}

        :else
        (let [config            (get-in req [:service-container :config])
              sanitized-user    (sanitize-for-serialization fresh-user)
              tenant-ctx        (tenant-auth/resolve-tenant-context db config fresh-user)
              repaired-session  (sanitize-for-serialization
                                  (tenant-auth/build-auth-session
                                    {:user sanitized-user}
                                    tenant-ctx))]
          {:action :repair
           :reason :verified-user-session-refreshed
           :auth-session repaired-session})))))

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
        (if (or (empty? email) (empty? password))
          (do
            (log/warn "Missing email or password in registration request")
            ;; IMPORTANT: don't set Content-Type here; let reitit/muuntaja encode the body.
            (response/bad-request {:error "Email and password are required"}))

          (do
            ;; Debug params if needed
            (log/debug "Email present:" (not (empty? email)))
            (log/debug "Password present:" (not (empty? password)))

            ;; Call registration service (handles validation, email verification and email sending)
            (let [db (resolve-db req)
                  config (get-in req [:service-container :config])
                  result (auth-service/register-user-with-password! auth-service
                           {:email email
                            :full-name full-name
                            :password password})
                  {:keys [user verification-required]} result
                  sanitized-user (sanitize-for-serialization user)
                  ;; Resolve tenant context — returns :no-tenant for newly registered users.
                  ;; Workspace is provisioned later when the user verifies their email.
                  tenant-ctx (tenant-auth/resolve-tenant-context db config user
                               {:client-ip (:remote-addr req)})
                  auth-session (sanitize-for-serialization
                                 (tenant-auth/build-auth-session {:user sanitized-user} tenant-ctx))
                  new-session (assoc existing-session :auth-session auth-session)
                  response-body (cond-> {:success true
                                         :verification-required (boolean verification-required)
                                         :user sanitized-user}
                                  (:tenant auth-session)
                                  (assoc :tenant (sanitize-for-serialization (:tenant auth-session)))

                                  (:tenant-selection-required auth-session)
                                  (assoc :tenant-selection-required true
                                    :available-tenants (sanitize-for-serialization
                                                         (:available-tenants auth-session)))

                                  verification-required
                                  (assoc :message "Registration successful. Please check your email for verification."))]
              (-> (response/response response-body)
                ;; IMPORTANT: merge with existing session (e.g. :admin-token) instead of overwriting.
                (assoc :session new-session)))))))))

;; NEW: Email/password login endpoint
(defn login-handler
  [auth-service]
  (fn [req]
    (route-utils/with-error-handling "user-login"
      (let [db (resolve-db req)
            {:keys [email password]} (:body-params req)
            ;; Ring already provides remote-addr and headers on the request
            remote-addr (:remote-addr req)
            headers (:headers req)
            ip (or remote-addr (get headers "x-forwarded-for"))
            ua (get headers "user-agent")
            existing-session (or (:session req) {})]

        ;; Validate required fields
        (if (or (empty? email) (empty? password))
          (response/bad-request {:error "Email and password are required"})

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

              ;; Return success response with session.
              ;; IMPORTANT: don't set Content-Type here; let reitit/muuntaja encode the body.
              (-> (response/response response-body)
                ;; IMPORTANT: merge with existing session (e.g. :admin-token) instead of overwriting.
                (assoc :session new-session)))

            (catch clojure.lang.ExceptionInfo e
              ;; Handle authentication failure
              (let [ex-data (ex-data e)
                    error-type (:type ex-data)
                    message (case error-type
                              :forbidden "Account is not active"
                              "Invalid email or password")
                    status (case error-type
                             :forbidden 403
                             401)
                    reason (case error-type
                             :forbidden "account_inactive"
                             "invalid_credentials")]

                (login-monitoring/record-login-event! db
                  {:principal-type :user
                   :principal-id nil
                   :success false
                   :reason reason
                   :ip ip
                   :user-agent ua})

                (response/status
                  (response/response {:error message})
                  status)))))))))

(defn auth-status-handler
  "Handle authentication status check"
  [req]
  (route-utils/with-error-handling "auth-status"
    (let [;; Check for our new auth session first
          auth-session (get-in req [:session :auth-session])
          user (:user auth-session)
          db (resolve-db req)
          invalid-message (when (and auth-session db)
                            (invalid-auth-session-message db auth-session))
          repair-result (when (and auth-session db)
                          (repair-incomplete-auth-session req db auth-session))
          effective-auth-session (or (:auth-session repair-result) auth-session)]
      (cond
        ;; Auth session exists but is no longer valid (user/tenant/membership status changed)
        (and auth-session invalid-message)
        (do
          (log/warn "Auth session invalid; clearing session"
            {:reason invalid-message
             :user-id (or (:id user) (:users/id user))
             :tenant-id (or (get-in auth-session [:tenant :id])
                          (get-in auth-session [:tenant :tenants/id]))})
          {:status 200
           :headers {"Content-Type" "application/json"}
           :body (json/generate-string {:authenticated false})
           :session (clear-user-auth-session (:session req))})

        (= :clear (:action repair-result))
        (do
          (log/info "Clearing incomplete auth session"
            {:reason (:reason repair-result)
             :user-id (or (:id user) (:users/id user))})
          {:status 200
           :headers {"Content-Type" "application/json"}
           :body (json/generate-string (:body repair-result))
           :session (clear-user-auth-session (:session req))})

        ;; New session format (from our auth service)
        effective-auth-session
        (do
          (when (= :repair (:action repair-result))
            (log/info "Repaired incomplete auth session"
              {:reason (:reason repair-result)
               :user-id (or (get-in effective-auth-session [:user :id])
                          (get-in effective-auth-session [:user :users/id]))
               :tenant-id (or (get-in effective-auth-session [:tenant :id])
                            (get-in effective-auth-session [:tenant :tenants/id]))}))
          (cond-> {:status 200
                   :headers {"Content-Type" "application/json"}
                   :body (json/generate-string
                           (build-auth-status-body effective-auth-session))}
            (= :repair (:action repair-result))
            (assoc :session (assoc (or (:session req) {}) :auth-session effective-auth-session))))

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
