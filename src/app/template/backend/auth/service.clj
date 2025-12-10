(ns app.template.backend.auth.service
  "Template authentication service implementation"
  (:require
    [app.template.backend.auth.protocols :as auth-protocols]
    [app.template.backend.auth.email-verification :as email-verification]
    [app.template.backend.db.protocols :as db-protocols]
    [app.template.protocols :as core-protocols]
    [app.template.shared.auth :as shared-auth]
    [app.shared.patterns :as patterns]
    [buddy.hashers :as hashers]
    [clojure.string :as str]
    [java-time :as time]
    [taoensso.timbre :as log])
  (:import
    [java.security SecureRandom]
    [java.util Base64]))

;; ============================================================================
;; Utility Functions (must be defined first)
;; ============================================================================

(defn create-session-token
  "Create a secure session token"
  []
  (let [random (SecureRandom.)
        bytes (byte-array 32)]
    (.nextBytes random bytes)
    (.encodeToString (Base64/getEncoder) bytes)))

(defn normalize-oauth-user-data
  "Normalize OAuth user data from different providers"
  [oauth-data provider]
  (case provider
    :google {:email (:email oauth-data)
             :full-name (:name oauth-data)
             :avatar-url (:picture oauth-data)
             :provider-user-id (:sub oauth-data)}
    :github {:email (:email oauth-data)
             :full-name (:name oauth-data)
             :avatar-url (:avatar_url oauth-data)
             :provider-user-id (str (:id oauth-data))}
    {}))

;; ============================================================================
;; Session Manager Implementation
;; ============================================================================

(defrecord SessionManagerImpl [db]
  auth-protocols/SessionManager
  (create-session [_this _user-id _tenant-id duration]
    (let [token (create-session-token)
          expires-at (time/plus (time/local-date-time) duration)]
      {:token token
       :expires-at expires-at}))

  (validate-session [_this _token]
    ;; Implementation would check session storage
    {:valid? false
     :user-id nil
     :expires-at nil})

  (refresh-session [_this token]
    ;; Implementation would refresh session expiration
    {:token token
     :expires-at (time/plus (time/local-date-time) (time/hours 24))})

  (invalidate-session [_this _token]
    ;; Implementation would remove session from storage
    {:success? true})

  (cleanup-expired-sessions [_this]
    ;; Implementation would clean up expired sessions
    {:removed 0}))

;; ============================================================================
;; OAuth Provider Implementations
;; ============================================================================

(defrecord GoogleOAuthProvider [client-id client-secret]
  auth-protocols/OAuthProvider
  (get-auth-url [_this redirect-uri]
    (str "https://accounts.google.com/o/oauth2/v2/auth"
      "?client_id=" client-id
      "&redirect_uri=" redirect-uri
      "&response_type=code"
      "&scope=openid%20email%20profile"))

  (exchange-code [_this _code _redirect-uri]
    ;; Implementation would exchange code for token
    {:access-token "mock-token"
     :refresh-token "mock-refresh"
     :expires-in 3600})

  (get-user-info [_this _access-token]
    ;; Implementation would fetch user info from Google
    {:email "mock@example.com"
     :name "Mock User"
     :avatar-url nil
     :provider-id "mock-id"}))

(defrecord GitHubOAuthProvider [client-id client-secret]
  auth-protocols/OAuthProvider
  (get-auth-url [_this redirect-uri]
    (str "https://github.com/login/oauth/authorize"
      "?client_id=" client-id
      "&redirect_uri=" redirect-uri
      "&scope=user:email"))

  (exchange-code [_this _code _redirect-uri]
    ;; Implementation would exchange code for token
    {:access-token "mock-token"
     :refresh-token nil
     :expires-in 3600})

  (get-user-info [_this _access-token]
    ;; Implementation would fetch user info from GitHub
    {:email "mock@example.com"
     :name "Mock User"
     :avatar-url nil
     :provider-id "mock-id"}))

;; ============================================================================
;; Password Manager Implementation
;; ============================================================================

(defrecord PasswordManagerImpl []
  auth-protocols/PasswordManager
  (hash-password [_this password]
    (hashers/derive password {:alg :bcrypt+sha512 :iterations 12}))

  (verify-password [_this password hash]
    (hashers/check password hash))

  (generate-reset-token [_this _user-id]
    {:token (create-session-token)
     :expires-at (time/plus (time/local-date-time) (time/hours 1))})

  (verify-reset-token [_this _token]
    {:valid? false
     :user-id nil}))

;; Database user normalization helper
(defn db-user->plain
  "Convert database user record to plain format for frontend"
  [user-record]
  (into {}
    (map (fn [[k v]] [(keyword (name k)) v]) user-record)))

;; Email/Password user registration function
(defn register-user-with-password!
  "Create a new user with email/password and trigger email verification.
   Returns {:user <user-map> :verification-token <token>}"
  [auth-service {:keys [email full-name password]}]
  (let [{:keys [db metadata password-manager email-service]} auth-service
        now (time/local-date-time)
        ;; Normalize incoming values
        email (some-> email str/trim)
        full-name (some-> full-name str/trim)]
    (log/info "register-user input" {:email email :full-name full-name})

    ;; 0) Validate required fields in a single place (service layer)
    (when (or (str/blank? email)
            (str/blank? full-name)
            (str/blank? password))
      (throw (ex-info "Missing required fields"
               {:type :validation-error
                :errors (merge {}
                          (when (str/blank? email)
                            {:email ["Email is required"]})
                          (when (str/blank? full-name)
                            {:full-name ["Full name is required"]})
                          (when (str/blank? password)
                            {:password ["Password is required"]}))})))

    ;; 0a) Validate email format before hitting the database so we never
    ;; rely on the users_email_check constraint for user-visible errors.
    (when-not (or (patterns/valid-email? email)
                (patterns/valid-email-simple? email))
      (throw (ex-info "Invalid email format"
               {:type :validation-error
                :errors {:email ["Please enter a valid email address"]}})))

    ;; 1) Check for existing user
    (when (db-protocols/exists? db :users :email email)
      (throw (ex-info "Email already registered"
               {:type :validation-error
                :errors {:email ["This email is already registered"]}})))

    ;; 2) Password strength validation
    (when (< (count password) 10)
      (throw (ex-info "Weak password"
               {:type :validation-error
                :errors {:password ["Password must be at least 10 characters"]}})))

    ;; 3) Hash password and create user
    (let [password-hash (auth-protocols/hash-password password-manager password)
          user-id (java.util.UUID/randomUUID)
          user-record (db-protocols/create db metadata :users
                        {:id user-id
                         :email email
                         :full_name full-name
                         :password_hash password-hash
                         :role shared-auth/role-member
                         :status "active"
                         :auth_provider "password"
                         :provider_user_id nil
                         :created_at now
                         :updated_at now})
          user-plain (db-user->plain user-record)
          verification-token (email-verification/create-verification-token!
                               db
                               user-id)]

      ;; 5) Send verification email
      (when email-service
        (try
          (email-verification/send-verification-email
            email-service
            user-plain
            verification-token)
          (catch Exception e
            (log/warn "Failed to send verification email for" email ":" (.getMessage e)))))

      {:user user-plain
       :verification-required true
       :verification-token verification-token})))

;; Email/Password user authentication function
(defn login-with-password
  "Authenticate a user with email/password.
   Returns {:user <user-map>} or throws ex-info."
  [auth-service {:keys [email password]}]
  (let [{:keys [db password-manager]} auth-service
        user-record (db-protocols/find-by-field db :users :email email)]

    (when-not user-record
      (throw (ex-info "Invalid credentials"
               {:type :validation-error
                :errors {:email ["Invalid email or password"]}})))

    ;; Verify password - handle both namespaced and non-namespaced keys
    (let [user (db-user->plain user-record)
          password-hash (or (:password_hash user-record)
                          (:users/password_hash user-record))
          user-status (or (:status user-record)
                        (:users/status user-record))]

      (when-not password-hash
        (throw (ex-info "Invalid credentials"
                 {:type :validation-error
                  :errors {:password ["Invalid email or password"]}})))

      (when-not (auth-protocols/verify-password password-manager password password-hash)
        (throw (ex-info "Invalid credentials"
                 {:type :validation-error
                  :errors {:password ["Invalid email or password"]}})))

      ;; Status checks
      (when (not= "active" user-status)
        (throw (ex-info "User is not active"
                 {:type :forbidden
                  :status user-status})))

      {:user user})))

;; ============================================================================
;; High-Level Authentication Service
;; ============================================================================

(defrecord AuthenticationService [db metadata oauth-providers session-manager password-manager email-service]
  core-protocols/BusinessService
  (initialize [this]
    (log/info "Initializing authentication service")
    this)

  (cleanup [this]
    (log/info "Cleaning up authentication service")
    (auth-protocols/cleanup-expired-sessions session-manager)
    this))

(defn process-oauth-callback
  "Single-tenant OAuth callback processing.

   In the single-tenant template we don't create tenants or run an onboarding
   flow. We simply upsert a user in the :users table based on the provider
   email and return session data the frontend can use.
   
   Security: Prevents OAuth from overwriting existing password-based accounts.
   If a user registered with email/password, they cannot login via OAuth with
   the same email unless they explicitly link their account."
  [auth-service oauth-data provider]
  (try
    (let [{:keys [db metadata password-manager]} auth-service
          user-email (:email oauth-data)]
      (when-not user-email
        (throw (ex-info "Email not provided by OAuth provider"
                 {:provider provider
                  :oauth-data oauth-data})))

      (let [normalized (normalize-oauth-user-data oauth-data provider)
            existing-user (db-protocols/find-by-field db :users :email user-email)
            now (time/local-date-time)
            new-user? (nil? existing-user)

            ;; Security check: Prevent OAuth from overwriting password-based accounts
            ;; Handle both namespaced and non-namespaced keys
            existing-auth-provider (or (:auth_provider existing-user)
                                     (:users/auth_provider existing-user))
            _ (when (and existing-user
                      (= "password" existing-auth-provider))
                (log/warn "OAuth login blocked for password-based account:" user-email)
                (throw (ex-info "Email already registered with password authentication"
                         {:type :account-conflict
                          :provider provider
                          :email user-email
                          :message "This email is already registered with password authentication. Please log in with your password instead."})))

            ;; Ensure we always have some password hash value even though
            ;; OAuth users do not log in with a local password.
            placeholder-password (auth-protocols/hash-password
                                   password-manager
                                   (str "oauth-" (name provider) "-user"))
            user-record (if existing-user
                          ;; Update existing OAuth user (only if auth_provider is already OAuth)
                          (let [user-id (or (:id existing-user)
                                          (:users/id existing-user))
                                update-data {:full_name (:full-name normalized)
                                             :avatar_url (:avatar-url normalized)
                                             :auth_provider (name provider)
                                             :provider_user_id (:provider-user-id normalized)
                                             :updated_at now}]
                            (db-protocols/update-record db metadata :users user-id update-data)
                            (db-protocols/find-by-id db :users user-id))
                          ;; Create new OAuth user
                          (let [user-id (java.util.UUID/randomUUID)
                                create-data {:id user-id
                                             :email user-email
                                             :full_name (:full-name normalized)
                                             :avatar_url (:avatar-url normalized)
                                             :password_hash placeholder-password
                                             :role shared-auth/role-admin    ;; highest available user role in this starter
                                             :status "active"
                                             :auth_provider (name provider)
                                             :provider_user_id (:provider-user-id normalized)
                                             :created_at now
                                             :updated_at now}]
                            (db-protocols/create db metadata :users create-data)))
            ;; Convert any namespaced keys (e.g. :users/email) to plain keys
            user-plain (into {}
                         (map (fn [[k v]]
                                [(keyword (name k)) v])
                           user-record))]

        {:user user-plain
         :authenticated true
         :provider provider
         ;; Keep these keys for compatibility with template frontend, but
         ;; they no longer drive any tenant-specific onboarding flow.
         :is-new-signup new-user?
         :onboarding-completed true
         :onboarding-step nil}))
    (catch Exception e
      (log/error "Error processing OAuth callback (single-tenant):" (.getMessage e))
      (throw e))))

;; ============================================================================
;; Factory Functions
;; ============================================================================

(defn create-authentication-service
  "Create authentication service with all dependencies"
  [db metadata config email-service]
  (let [oauth-providers {:google (->GoogleOAuthProvider
                                   (:google-client-id config)
                                   (:google-client-secret config))
                         :github (->GitHubOAuthProvider
                                   (:github-client-id config)
                                   (:github-client-secret config))}
        session-manager (->SessionManagerImpl db)
        password-manager (->PasswordManagerImpl)]
    (->AuthenticationService db metadata oauth-providers session-manager password-manager email-service)))
