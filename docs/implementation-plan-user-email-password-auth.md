# User Email/Password Authentication Implementation Plan

## Executive Summary

This document outlines the implementation of user email/password registration and authentication alongside the existing OAuth system. This provides users with choice in authentication methods while maintaining security best practices.

## Current State Analysis

### ✅ Existing Infrastructure
- **OAuth Authentication**: Fully implemented via `src/app/template/backend/routes/oauth.clj` and `src/app/template/backend/auth/service.clj`
- **Admin Authentication**: Separate email/password system via `src/app/backend/routes/admin/auth.clj`
- **Email Verification**: Complete infrastructure in `src/app/template/backend/auth/email_verification.clj`
- **Database Schema**: `users` table supports email/password fields and OAuth provider data
- **Frontend**: Template login page with OAuth button at `src/app/template/frontend/pages/login.cljs`

### ❌ Missing Components
- **User Registration**: No endpoint for users to register with email/password
- **User Email/Password Login**: No login endpoint for regular users (only admins)
- **Frontend Registration Form**: No user registration UI component
- **Email Verification Integration**: Email verification exists but not integrated with user registration
- **Progressive Access**: No mechanism to limit unverified user access

## Implementation Plan

### Phase 1: Backend Authentication Service Enhancement

#### 1.1 Strengthen Password Manager Implementation

**File**: `src/app/template/backend/auth/service.clj`

```clojure
;; Add buddy hashers dependency
(ns app.template.backend.auth.service
  (:require
    ...
    [buddy.hashers :as hashers]
    [app.template.backend.auth.protocols :as auth-protocols]
    ...))

;; Replace PasswordManagerImpl with secure implementation
(defrecord PasswordManagerImpl []
  auth-protocols/PasswordManager
  (hash-password [this password]
    (hashers/derive password {:alg :bcrypt+sha512 :iterations 12}))

  (verify-password [this password hash]
    (hashers/check password hash)))

  (generate-reset-token [this _user-id]
    {:token (create-session-token)
     :expires-at (time/plus (time/local-date-time) (time/hours 1))})

  (verify-reset-token [this _token]
    {:valid? false :user-id nil}))
```

#### 1.2 Add User Registration Functions

**File**: `src/app/template/backend/auth/service.clj`

```clojure
;; Database user normalization helper
(defn db-user->plain [user-record]
  "Convert database user record to plain format for frontend"
  (into {}
    (map (fn [[k v]] [(keyword (name k)) v]) user-record)))

;; Email/Password user registration function
(defn register-user-with-password!
  "Create a new user with email/password and trigger email verification.
   Returns {:user <user-map> :verification-token <token>}"
  [^AuthenticationService auth-service {:keys [db metadata password-manager email-service]}]
  (let [{:keys [email full-name password]} auth-service]
        now (time/local-date-time)]

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
                         :role "member"
                         :status "active"
                         :auth_provider "password"
                         :provider_user_id nil
                         :created_at now
                         :updated_at now})]
          user-plain (db-user->plain user-record)]

      ;; 4) Create email verification token
      (let [verification-token (create-verification-token! db nil user-id)]

        ;; 5) Send verification email
        (when email-service
          (try
            (auth-protocols/send-verification-email
              email-service
              user-plain
              verification-token)
            (catch Exception e
              (log/warn "Failed to send verification email for" email ":" (.getMessage e)))))

      {:user user-plain
       :verification-required true})))

;; Email/Password user authentication function
(defn login-with-password
  "Authenticate a user with email/password.
   Returns {:user <user-map>} or throws ex-info."
  [^AuthenticationService auth-service {:keys [db password-manager email-service]}]
  (let [{:keys [email password]} auth-service
        now (time/local-date-time)]

    ;; Find user by email
    (let [user-record (db-protocols/find-by-field db :users :email email)]
      (when-not user-record
        (throw (ex-info "Invalid credentials"
                   {:type :validation-error
                    :errors {:email ["Invalid email or password"]}})))

      ;; Verify password
      (let [user (db-user->plain user-record)
            password-hash (:password_hash user-record)]
        (when-not (auth-protocols/verify-password password-manager password password-hash)
          (throw (ex-info "Invalid credentials"
                       {:type :validation-error
                        :errors {:password ["Invalid email or password"]}})))

        ;; Status checks
        (when-not= "active" (:status user-record)
          (throw (ex-info "User is not active"
                       {:type :forbidden
                        :status (:status user-record)})))

        {:user user})))
```

### Phase 2: API Routes Implementation

#### 2.1 User Authentication Routes

**File**: `src/app/template/backend/routes/auth.clj`

```clojure
;; Add new routes to existing create-auth-routes function
(defn create-auth-routes
  "Create authentication routes that use the authentication service"
  [auth-service]
  {:logout-handler logout-handler
   :auth-status-handler auth-status-handler
   :test-auth-handler (test-auth-handler auth-service)

   ;; NEW: User registration endpoint
   :register-handler
   (fn [req]
     (route-utils/with-error-handling "user-register"
       (let [{:keys [db email-service]} (get-service-container req)
             {:keys [email full-name password]} (:body-params req)
             full-name (or full-name fullName)]

         ;; Validate required fields
         (when (or (empty? email) (empty? full-name) (empty? password))
           (utils/error-response "Email, full name, and password are required" :status 400))

         ;; Call registration service
         (let [result (register-user-with-password!
                    auth-service
                    {:db db
                     :email-service email-service
                     :email email
                     :full-name full-name
                     :password password})]

           (if (:verification-required result)
             ;; User created but needs email verification
             (let [token (:verification-token result)]
               (when email-service
                 (try
                   (email-protocols/send-verification-email
                     email-service
                     (:user result)
                     token)
                   (catch Exception e
                     (log/warn "Failed to send verification email:" (.getMessage e)))))

               (-> (utils/json-response {:success true :message "Registration successful. Please check your email for verification."})
                   (assoc-in [:session :auth-session] {:user (:user result)})))

             ;; User registered successfully
             (-> (utils/json-response {:success true :user (:user result)})
                 (assoc-in [:session :auth-session] {:user (:user result)})))))

   ;; NEW: Email/password login endpoint
   :login-handler
   (fn [req]
     (route-utils/with-error-handling "user-login"
       (let [{:keys [db]} (get-service-container req)
             {:keys [email password]} (:body-params req)
             {:keys [remote-addr headers]} (utils/extract-request-context req)
             ip (or remote-addr (get headers "x-forwarded-for"))
             ua (get headers "user-agent")]

         ;; Validate required fields
         (when (or (empty? email) (empty? password))
           (utils/error-response "Email and password are required" :status 400))

         ;; Attempt authentication
         (let [auth-result (login-with-password auth-service {:email email :password password})]

           ;; Handle failed authentication
           (when (instance? clojure.lang.ExceptionInfo auth-result)
             (login-monitoring/record-login-event! db
               {:principal-type :user
                :principal-id (:id (:user auth-result))
                :success false
                :reason "invalid_credentials"
                :ip ip
                :user-agent ua})
             (utils/error-response "Invalid email or password" :status 401))

         ;; Record successful login
         (let [user-id (:id (:user auth-result))]
           (login-monitoring/record-login-event! db
             {:principal-type :user
                :principal-id user-id
                :success true
                :reason nil
                :ip ip
                :user-agent ua})

         (-> (utils/json-response {:success true :user (:user auth-result)})
             (assoc-in [:session :auth-session] {:user (:user auth-result)})))))}))
```

#### 2.2 Email Verification Routes Integration

**File**: `src/app/template/backend/routes/auth.clj` - add to create-auth-routes

```clojure
;; Add email verification handlers to auth routes
:verify-email-handler (email-verification/verify-email-handler db email-service)
:resend-verification-handler (email-verification/resend-verification-handler db email-service)
:verification-status-handler (email-verification/verification-status-handler db)
```

#### 2.3 API Versioning

**File**: `src/app/backend/routes/api.clj`

Add to versioned API routes:

```clojure
["/api"
 ["/v1" (api/create-versioned-api-routes db md service-container "v1"
  ;; Add auth routes to versioned API
  (concat
    ["/auth/register" {:post {:handler (fn [req]
                                          (let [services (:service-container req)
                                                auth-service (get services :auth-service)
                                                db (get services :db-adapter)
                                                email-service (get services :email-service)]
                                            ((:register-handler (create-auth-routes auth-service)) req)))}}]
    ["/auth/login" {:post {:handler (fn [req]
                                        (let [services (:service-container req)
                                              auth-service (get services :auth-service)
                                              db (get services :db-adapter)]
                                          ((:login-handler (create-auth-routes auth-service)) req)))}}]
    ["/auth/verification-status" {:get {:handler (fn [req]
                                                     (let [services (:service-container req)
                                                           db (get services :db-adapter)]
                                                       ((:verification-status-handler (create-auth-routes auth-service)) req)))}}]
    ["/auth/resend-verification" {:post {:handler (fn [req]
                                                      (let [services (:service-container req)
                                                            db (get services :db-adapter)]
                                                        ((:resend-verification-handler (create-auth-routes auth-service)) req)))}}]]
```

### Phase 3: Database Schema Updates

#### 3.1 Users Table Enhancement

**File**: `resources/db/models.edn`

```edn
:users
 {:fields
  [;; Existing fields...
   [:email_verified :boolean {:null false :default false}]
   [:email_verified_at :timestamptz]
   [:email_verification_status :text {:null false :default "unverified"}]],
  :indexes
  [;; Existing indexes...
   [:idx_users_email_verified :btree {:fields [:email_verified]}]
   [:idx_users_email_verification_status :btree {:fields [:email_verification_status]}]]}
```

Run migrations:
```clojure
(mig/make-all-migrations!)
(mig/migrate!)
```

### Phase 4: Frontend Implementation

#### 4.1 Enhanced Login Page

**File**: `src/app/template/frontend/pages/login.cljs`

```clojure
;; Enhanced not-authenticated section
($ :div {:class "min-h-screen flex items-center justify-center bg-base-200"}
  ($ :div {:class "max-w-md w-full bg-base-100 shadow-xl rounded-lg p-8"}
    ($ :div {:class "text-center"}
      ;; Header
      ($ :div {:class "mb-8"}
        ($ :h1 {:class "text-3xl font-bold text-base-content mb-2"} "Welcome")
        ($ :p {:class "text-base-content/70"} "Choose how to sign in to your account"))

      ;; Loading state
      (if loading?
        ($ :div {:class "flex flex-col items-center space-y-4"}
          ($ :div {:class "ds-loading ds-loading-spinner ds-loading-lg"})
          ($ :p {:class "text-base-content/70"} "Checking authentication..."))

        ;; Login options
        ($ :div {:class "space-y-4"}
          ;; Google OAuth button (existing)
          ($ button {:btn-type :outline
                     :class "ds-btn-lg w-full"
                     :id "login-google-btn"
                     :on-click #(set! (.-href js/window.location) "/login/google")}
            ($ google-icon)
            "Continue with Google")

          ;; Divider
          ($ :div {:class "flex items-center"}
            ($ :div {:class "flex-1 border-t border-base-300"})
            ($ :span {:class "px-3 text-xs text-base-content/60"} "OR")
            ($ :div {:class "flex-1 border-t border-base-300"}))

          ;; Email/password login form
          ($ login-form
            {:title "Sign In"
             :subtitle "Use your email and password"
             :loading? login-loading?
             :error login-error
             :email-placeholder "you@example.com"
             :password-placeholder "Enter your password"
             :submit-text "Sign In"
             :on-submit (fn [email password]
                          (rf/dispatch [:template.auth/login-with-password email password])})

          ;; Registration link
          ($ :div {:class "mt-6 text-center"}
            ($ :p {:class "text-sm text-base-content/70"}
              "Don't have an account? "
              ($ :a {:href "/register" :class "ds-link ds-link-primary"} "Create one"))))))))))))
```

#### 4.2 User Registration Page

**New File**: `src/app/template/frontend/pages/register.cljs`

```clojure
(ns app.template.frontend.pages.register
  (:require
    [app.template.frontend.components.auth :refer [auth-form-container auth-form-header auth-form-field auth-submit-button auth-form-footer]]
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.events.auth :as auth-events]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-state]]
    [uix.re-frame :refer [use-subscribe]]))

(defui register-page
  []
  (let [loading? (use-subscribe [:register-loading?])
        error (use-subscribe [:register-error])
        success? (use-subscribe [:register-success?])]

    ($ auth-form-container
      ;; Registration header
      ($ auth-form-header
        {:title "Create Account"
         :subtitle "Join our platform today"
         :icon-path "M18 9v2a2 1 0-1 9v2a2 2h2a1 0-2 9v2a2 4H14c1A9 1 0 1 0 1h2a1 0 9v2a2 2h2a1 0-2 9v2a2 4c3a12c8.272 1.094 2.53L18 7L12 2 53L18 7L12 2 53L18 18 7l-4-4-2L17 3 2.5L12 23z"})

      ;; Error alert
      ($ auth-error-alert {:error error})

      ;; Registration form
      ($ :form {:id "register-form"
                :on-submit (fn [e]
                             (.preventDefault e)
                             (when (and (:email email) (:password password) (:confirm-password confirm-password))
                               (rf/dispatch [:template.auth/register
                                          {:email (:email email)
                                           :full-name (:full-name email)
                                           :password password
                                           :confirm-password confirm-password}])))

                ;; Email field
        ($ auth-form-field {:label "Email Address"
                            :type "email"
                            :placeholder "you@example.com"
                            :value (:email email)
                            :field-id "register-email"
                            :on-change #(rf/dispatch [:template.auth/set-field :email %])})

                ;; Full name field
        ($ auth-form-field {:label "Full Name"
                            :type "text"
                            :placeholder "Enter your full name"
                            :value (:full-name email)
                            :field-id "register-full-name"
                            :on-change #(rf/dispatch [:template.auth/set-field :full-name %])})

                ;; Password field
        ($ auth-form-field {:label "Password"
                            :type "password"
                            :placeholder "Enter your password (min. 10 characters)"
                            :value (:password password)
                            :field-id "register-password"
                            :on-change #(rf/dispatch [:template.auth/set-field :password %])})

                ;; Confirm password field
        ($ auth-form-field {:label "Confirm Password"
                            :type "password"
                            :placeholder "Confirm your password"
                            :value (:confirm-password confirm-password)
                            :field-id "register-confirm-password"
                            :on-change #(rf/dispatch [:template.auth/set-field :confirm-password %])})

                ;; Submit button
        ($ auth-submit-button {:loading? loading?
                               :text "Create Account"
                               :loading-text "Creating Account..."
                               :button-id "register-submit-btn"})

      ;; Success message
      (when success?
        ($ :div {:class "text-center p-6 bg-success/10 rounded-lg"}
          ($ :h2 {:class "text-2xl font-bold text-success mb-4"} "Account Created!")
          ($ :p {:class "text-base-content mb-6"}
            "Please check your email to verify your account. "
            "You'll need to verify your email before signing in.")
          ($ button {:btn-type :primary
                     :class "w-full"
                     :on-click #(set! (.-href js/window.location) "/login")}
            "Continue to Login")))))
```

#### 4.3 Authentication Events

**File**: `src/app/template/frontend/events/auth.cljs`

```clojure
(ns app.template.frontend.events.auth
  (:require
    [re-frame.core :as rf]
    [app.template.frontend.http :as http]))

;; Form field management
(rf/reg-event-db
 ::set-field
 common-interceptors
 (fn [db [_event [_ field value]]]
   (assoc-in db field value)))

;; Email/password registration
(rf/reg-event-fx
 ::register
 common-interceptors
 (fn [{:keys [db]} [_event {:keys [email full-name password confirm-password]}]]
   (let [db (-> db
         (assoc-in [:session :register-loading?] true)
         (assoc-in [:session :register-error] nil))]

     (cond
       ;; Validation
       (not= password confirm-password)
       (let [new-db (-> db
                     (assoc-in [:session :register-loading?] false)
                     (assoc-in [:session :register-error] "Passwords do not match"))]
         (http/xhrio :http-fx
           {:method :post
            :uri "/api/v1/auth/register"
            :params {:email email :full-name full-name :password password}
            :on-success [::register-success]
            :on-failure [::register-failure]})

       ;; Password strength
       (< (count password) 10)
       (let [new-db (-> db
                     (assoc-in [:session :register-loading?] false)
                     (assoc-in [:session :register-error] "Password must be at least 10 characters"))]
         (http/xhrio :http-fx
           {:method :post
            :uri "/api/v1/auth/register"
            :params {:email email :full-name full-name :password password}
            :on-success [::register-success]
            :on-failure [::register-failure]})

       ;; Submit registration
       :else
       (let [new-db (-> db
                     (assoc-in [:session :register-loading?] true)
                     (assoc-in [:session :register-error] nil))]
         (http/xhrio :http-fx
           {:method :post
            :uri "/api/v1/auth/register"
            :params {:email email :full-name full-name :password password}
            :on-success [::register-success]
            :on-failure [::register-failure]})))))

(rf/reg-event-fx
 ::register-success
 common-interceptors
 (fn [{:keys [db]} [_event resp]]
   (let [db (-> db
             (assoc-in [:session :register-loading?] false)
             (assoc-in [:session :register-success?] true))]
     ;; Check if verification required
     (if (:verification-required resp)
       ;; Show success but redirect to verification sent page
       (set! (.-href js/window.location) "/verify-email-sent")
       ;; Else successful registration, redirect to login
       (set! (.-href js/window.location) "/login"))))

(rf/reg-event-fx
 ::register-failure
 common-interceptors
 (fn [{:keys [db]} [_event error]]
   (let [db (-> db
             (assoc-in [:session :register-loading?] false)
             (assoc-in [:session :register-error] (http/extract-error-message error)))]
     ;; Log error for debugging
     (js/console.error "Registration failed:" error)))

;; Email/password login
(rf/reg-event-fx
 ::login-with-password
 common-interceptors
 (fn [{:keys [db]} [_event email password]]
   (let [db (-> db
             (assoc-in [:session :login-loading?] true)
             (assoc-in [:session :login-error] nil))]
     (http/xhrio :http-fx
       {:method :post
        :uri "/api/v1/auth/login"
        :params {:email email :password password}
        :on-success [::login-success]
        :on-failure [::login-failure]})))

(rf/reg-event-fx
 ::login-success
 common-interceptors
 (fn [{:keys [db]} [_event resp]]
   (let [db (-> db
             (assoc-in [:session :login-loading?] false)
             (assoc-in [:session :login-error] nil)
             ;; Store user data in session for auth status check
             (assoc-in [:session :auth-session] {:user (:user resp)
                                          :verification-required (:verification-required resp)})]
     ;; Update auth status
     (rf/dispatch [:template.auth/fetch-auth-status])))

(rf/reg-event-fx
 ::login-failure
 common-interceptors
 (fn [{:keys [db]} [_event error]]
   (let [db (-> db
             (assoc-in [:session :login-loading?] false)
             (assoc-in [:session :login-error] (http/extract-error-message error))])))

;; Email verification status check
(rf/reg-event-fx
 ::fetch-verification-status
 common-interceptors
 (fn [{:keys [db]} [_event]]
   (let [db (-> db
             (assoc-in [:session :verification-loading?] true)
             (assoc-in [:session :verification-error] nil))]
     (http/xhrio :http-fx
       {:method :get
        :uri "/api/v1/auth/verification-status"
        :on-success [::verification-status-success]
        :on-failure [::verification-status-failure]})))

(rf/reg-event-fx
 ::verification-status-success
 common-interceptors
 (fn [{:keys [db]} [_event resp]]
   (let [db (-> db
             (assoc-in [:session :verification-loading?] false)
             (assoc-in [:session :verification-status] (:email-verified resp))])))

(rf/reg-event-fx
 ::verification-status-failure
 common-interceptors
 (fn [{:keys [db]} [_event error]]
   (let [db (-> db
             (assoc-in [:session :verification-loading?] false)
             (assoc-in [:session :verification-error] (http/extract-error-message error))]))

;; Resend verification email
(rf/reg-event-fx
 ::resend-verification
 common-interceptors
 (fn [{:keys [db]} [_event]]
   (let [db (-> db
             (assoc-in [:session :resend-loading?] true)
             (assoc-in [:session :resend-error] nil))]
     (http/xhrio :http-fx
       {:method :post
        :uri "/api/v1/auth/resend-verification"
        :on-success [::resend-verification-success]
        :on-failure [::resend-verification-failure]})))

(rf/reg-event-fx
 ::resend-verification-success
 common-interceptors
 (fn [{:keys [db]} [_event resp]]
   (let [db (-> db
             (assoc-in [:session :resend-loading?] false)
             (assoc-in [:session :resend-success?] true)))))

(rf/reg-event-fx
 ::resend-verification-failure
 common-interceptors
 (fn [{:keys [db]} [_event error]]
   (let [db (-> db
             (assoc-in [:session :resend-loading?] false)
             (assoc-in [:session :resend-error] (http/extract-error-message error))]))
```

#### 4.4 Email Verification Pages

**New File**: `src/app/template/frontend/pages/verify_email_sent.cljs`

```clojure
(ns app.template.frontend.pages.verify-email-sent
  (:require
    [app.template.frontend.components.auth :refer [auth-form-container]]
    [app.template.frontend.events.auth :as auth-events]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]))

(defui verify-email-sent-page
  []
  (let [loading? (use-subscribe [:resend-loading?])
        error (use-subscribe [:resend-error])
        success? (use-subscribe [:resend-success?])]

    ($ auth-form-container
      ;; Email sent message
      ($ :div {:class "text-center p-8"}
        ($ :div {:class "w-20 h-20 mx-auto mb-6 bg-primary/10 rounded-full flex items-center justify-center"}
          ($ :svg {:class "w-10 h-10 text-primary"} :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
            ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                      :d "M3 8l7a3 .3 1 0 1 0 7a3 .3 1h2a1 0-2 7a3 .3 1v4a1 0 4c1a12c8.272 1.094 2.53L18 7L12 2 53L18 7L12 2 53L18 18 7l-4-4-2L17 3 2.5L12 23z"}))))
        ($ :h3 {:class "text-xl font-semibold text-primary mb-4"} "Verification Email Sent")
        ($ :p {:class "text-base-content/70 mb-6"}
          "We've sent a verification link to your email address. "
          "Please check your inbox and click the link to activate your account.")

      ;; Resend form
      ($ :form {:id "resend-form"
                :on-submit (fn [e]
                             (.preventDefault e)
                             (rf/dispatch [:template.auth/resend-verification]))}

        ($ auth-submit-button
          {:loading? loading?
           :text "Resend Verification Email"
           :loading-text "Sending..."
           :button-id "resend-btn"})

      ;; Success message
      (when success?
        ($ :div {:class "text-center p-6 bg-success/10 rounded-lg"}
          ($ :h3 {:class "text-xl font-semibold text-success mb-4"} "Email Resent!")
          ($ :p {:class "text-base-content"} "Another verification email has been sent to your address."))))))
```

### Phase 5: Security Enhancements

#### 5.1 Rate Limiting Implementation

**File**: `src/app/backend/services/monitoring/login_events.clj` - add rate limiting functions

```clojure
;; Rate limiting helpers
(defn count-recent-failed-logins
  "Count failed login attempts for a user or IP within time window"
  [db {:keys [principal-type principal-id since success?]}]
  (->> (db-protocols/execute! db
     "SELECT COUNT(*) as count
      FROM login_events
      WHERE principal_type = ?
        AND principal_id = ?
        AND success = ?
        AND created_at > ?"
     [principal-type principal-id false since])
    :count))

(defn is-rate-limited?
  "Check if user/IP has exceeded rate limits"
  [db {:keys [principal-type principal-id]}]
  (let [failed-count (count-recent-failed-logins db {:principal-type principal-type :principal-id :since (time/minus (time/instant) (time/minutes 15) :success? false})]
    (> failed-count 5)))
```

#### 5.2 CSRF Protection Updates

**File**: `src/app/backend/routes.clj` - ensure CSRF is properly configured

```clojure
;; In site-config, ensure anti-forgery is enabled for API routes
site-config (-> site-defaults
               (assoc-in [:security :anti-forgery] true)  ; Enable CSRF for production
               ;; Existing secure cookie settings...
```

#### 5.3 Authentication Middleware

**New File**: `src/app/backend/middleware/user_auth.clj`

```clojure
(ns app.backend.middleware.user-auth
  "Middleware to enforce user authentication requirements"
  (:require
    [app.template.backend.auth.email-verification :as email-verify]
    [reitit.ring :as ring]))

(defn wrap-user-email-verified
  "Middleware to ensure user email is verified before accessing protected routes"
  [handler]
  (fn [request]
    (let [auth-session (get-in request [:session :auth-session])
          user (:user auth-session)]
      (cond
        ;; No authenticated user
        (nil? user)
        (handler request)

        ;; User not email verified
        (not (:email_verified user))
        {:status 403
         :headers {"Content-Type" "application/json"}
         :body "{\"error\": \"Email verification required\", \"verification_required\": true}"}

        ;; User verified, proceed
        :else
        (handler request)))))

(defn wrap-progressive-user-access
  "Middleware to allow limited access for unverified users"
  [handler]
  (fn [request]
    (let [auth-session (get-in request [:session :auth-session])
          user (:user auth-session)]
      (if (:email_verified user)
        ;; Verified users get full access
        (handler request)
        ;; Unverified users get limited access
        (let [uri (:uri request)]
          (if (or (.startsWith uri "/api/v1/auth")
                    (.startsWith uri "/auth/")
                    (.startsWith uri "/register"))
            ;; Allow auth routes without verification
            (handler request)
            ;; Block other API routes
            {:status 403
             :headers {"Content-Type" "application/json"}
             :body "{\"error\": \"Email verification required for this action\"}"})))))
```

### Phase 6: Testing Strategy

#### 6.1 Backend Tests

**New File**: `test/app/template/backend/auth_user_registration_test.clj`

```clojure
(ns app.template.backend.auth-user-registration-test
  (:require
    [clojure.test :refer [deftest testing is]]
    [app.template.backend.auth.service :as auth-service]
    [app.template.backend.auth.protocols :as auth-protocols]))

(deftest user-registration-test
  "Test user registration with email/password"
  [auth-service]

  (testing "user registration"
    (let [password-manager (->auth-service :password-manager)]

      ;; Test email validation
      (is (thrown-with-msg?
           #(register-user-with-password! auth-service
              {:email "existing@example.com"
               :full-name "Test User"
               :password "short"})
           #"This email is already registered"))

      ;; Test password validation
      (is (thrown-with-msg?
           #(register-user-with-password! auth-service
              {:email "new@example.com"
               :full-name "Test User"
               :password "123"})
           #"Password must be at least 10 characters"))

      ;; Test successful registration
      (let [result (register-user-with-password! auth-service
                    {:email "newuser@example.com"
                     :full-name "Test User"
                     :password "validpassword123"})]
        (is (= true (:success result)))))))
```

#### 6.2 Frontend Tests

**New File**: `test/app/template/frontend/auth_components_test.cljs`

```clojure
(ns app.template.frontend.auth-components-test
  (:require
    [app.template.frontend.components.auth :as auth-comp]
    [app.template.frontend.events.auth :as auth-events]
    [uix.core :as uix]))

(deftest auth-form-test
  "Test auth form component"
  []

  ;; Test form validation
  (uix/render
    [$ auth-comp.auth-form
      {:title "Test Form"
       :subtitle "Test validation"
       :loading? false
       :error "Test error message"
       :email-placeholder "test@example.com"
       :password-placeholder "Enter password"
       :submit-text "Submit"
       :on-submit (fn [email password]
                    (js/console.log "Form submitted with:" email password))}])))
```

## Implementation Order

1. **Phase 1**: Backend service enhancements (Password Manager, user registration functions)
2. **Phase 2**: API routes (registration, login, verification endpoints)
3. **Phase 3**: Database schema updates (add email verification fields)
4. **Phase 4**: Frontend components (registration page, enhanced login, verification pages)
5. **Phase 5**: Security enhancements (rate limiting, CSRF protection, middleware)
6. **Phase 6**: Testing (backend and frontend test coverage)

## Security Checklist

- [ ] Password hashing with bcrypt+sha512
- [ ] Minimum password length (10 characters)
- [ ] Email uniqueness validation
- [ ] CSRF protection enabled
- [ ] Rate limiting implemented (5 attempts per 15 minutes)
- [ ] Progressive access for unverified users
- [ ] Email verification tokens with expiration
- [ ] Comprehensive audit logging
- [ ] Secure session management
- [ ] Input validation and sanitization

## Migration Strategy

1. Run database migrations to add new fields
2. Update existing users to set `email_verified = true` for OAuth users
3. Test new authentication flows
4. Deploy gradually with feature flags if needed

## Monitoring Requirements

- Track registration conversion rates
- Monitor email verification success/failure rates
- Alert on suspicious login patterns
- Track user verification completion rates
- Monitor API endpoint response times

This implementation provides a complete, secure user registration and authentication system that integrates seamlessly with the existing OAuth infrastructure while maintaining security best practices.