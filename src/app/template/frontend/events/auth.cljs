(ns app.template.frontend.events.auth
  "Authentication and session management events for the template/frontend layer.

   Handles user authentication flow, session management, and logout functionality."
  (:require
    [app.admin.frontend.adapters.users :as admin-users-adapter]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; ========================================================================
;; Authentication Status Events
;; ========================================================================

;; Event to fetch authentication status from the backend
(rf/reg-event-fx
  ::fetch-auth-status
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (assoc-in db [:session :loading?] true)
     :http-xhrio (http/api-request
                   {:method :get
                    :uri "/auth/status"
                    :on-success [::fetch-auth-status-success]
                    :on-failure [::fetch-auth-status-failure]})}))

;; Handle successful auth status retrieval
(rf/reg-event-fx
  ::fetch-auth-status-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [authenticated? (get response :authenticated false)
          session-valid? (get response :session-valid true) ; default to true for legacy sessions
          legacy-session? (get response :legacy-session false)
          provider (get response :provider)
          tokens (get response :tokens)
          user (get response :user)
          tenant (get response :tenant)
          permissions (get response :permissions)
          current-page (get-in db [:ui :current-page])
          user-role (:role user)]

      ;; Log authentication details
      (when user
        (if legacy-session?
          (log/debug "Legacy user session:" (:name user))
          (log/debug "Multi-tenant user session:" (:full-name user) "tenant:" (:name tenant) "role:" user-role)))

      (let [updated-db (-> db
                         ;; Clear loading state
                         (assoc-in [:session :loading?] false)

                         ;; Set authentication status
                         (assoc-in [:session :authenticated?] authenticated?)
                         (assoc-in [:session :session-valid?] session-valid?)
                         (assoc-in [:session :legacy-session?] legacy-session?)

                         ;; Handle legacy OAuth tokens (backward compatibility)
                         (assoc-in [:session :oauth2/access-tokens] tokens)
                         (assoc-in [:session :provider] provider)

                         ;; Set user information (updated format for multi-tenant)
                         (assoc-in [:session :user] user)

                         ;; Set tenant information (new for multi-tenant)
                         (assoc-in [:session :tenant] tenant)

                         ;; Set user permissions (new for multi-tenant)
                         (assoc-in [:session :permissions] permissions)

                         ;; Clear any previous errors
                         (update :session dissoc :error))
            ;; Determine redirect based on role
            redirect-path (cond
                            ;; Unassigned users go to waiting room
                            (= user-role "unassigned") "/waiting-room"
                            ;; Members and above go to expense dashboard
                            (contains? #{"member" "admin" "owner"} user-role) "/dashboard"
                            ;; Viewers or other roles go to entities
                            :else "/entities")
            base-effects (cond-> {:db updated-db}
                           (and authenticated? (= current-page :login))
                           (assoc :redirect redirect-path))]

        ;; Mirror the resolved session user into the shared template
        ;; entity store so FK table columns pointing at :users can
        ;; resolve labels via list-view + select-options.
        (cond-> base-effects
          user (update :fx (fnil conj [])
                 [:dispatch [::admin-users-adapter/sync-users-to-template [user]]]))))))

;; Handle failure to fetch auth status
(rf/reg-event-db
  ::fetch-auth-status-failure
  common-interceptors
  (fn [db _]
    (log/error "Failed to fetch auth status")
    (-> db
      (assoc-in [:session :loading?] false)
      (assoc-in [:session :authenticated?] false)
      (assoc-in [:session :error] "Failed to fetch authentication status"))))

;; ========================================================================
;; Page Initialization Events
;; ========================================================================

;; Page initialization events for login and logout
(rf/reg-event-fx
  :page/init-login
  common-interceptors
  (fn [{:keys [db]} _]
    (let [current-auth-status (get-in db [:session :authenticated?])
          loading? (get-in db [:session :loading?])]
      ;; If already authenticated, redirect to entities page
      (if (and current-auth-status (not loading?))
        {:db (assoc-in db [:ui :current-page] :login)
         :redirect "/entities"}
        ;; Otherwise, fetch auth status and show login page
        {:db (-> db
               (assoc-in [:ui :current-page] :login))
         :fx [[:dispatch [::fetch-auth-status]]]}))))

(rf/reg-event-fx
  :page/init-logout
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (-> db
           (assoc-in [:ui :current-page] :logout))
     :fx [[:dispatch [::fetch-auth-status]]]}))

;; Page initialization for registration
(rf/reg-event-fx
  :page/init-register
  common-interceptors
  (fn [{:keys [db]} _]
    (let [current-auth-status (get-in db [:session :authenticated?])
          loading? (get-in db [:session :loading?])]
      ;; If already authenticated, redirect to entities page
      (if (and current-auth-status (not loading?))
        {:db (assoc-in db [:ui :current-page] :register)
         :redirect "/entities"}
        ;; Otherwise, clear auth state and show registration page
        {:db (-> db
               (assoc-in [:ui :current-page] :register))
         :fx [[:dispatch [::clear-auth-state]]]}))))

;; Page initialization for email verification
(rf/reg-event-fx
  :page/init-verify-email
  common-interceptors
  (fn [{:keys [db]} [_ token]]
    (let [loading? (get-in db [:session :loading?])]
      ;; If not loading, process verification
      (if-not loading?
        {:db (assoc-in db [:ui :current-page] :verify-email)
         :fx [(when token
                [:dispatch [::verify-email token]])]}
        ;; Show loading state
        {:db (assoc-in db [:session :loading?] true)}))))

;; ========================================================================
;; Logout Events
;; ========================================================================

;; Handle logout action
(rf/reg-event-fx
  ::logout
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (-> db
           ;; Clear local session state immediately
           (assoc-in [:session :authenticated?] false)
           (assoc-in [:session :user] nil)
           (assoc-in [:session :tenant] nil)
           (assoc-in [:session :permissions] nil)
           (update :session dissoc :oauth2/access-tokens :provider))
     ;; Call backend logout to clear server session
     :http-xhrio (http/api-request
                   {:method :post
                    :uri "/auth/logout"
                    :on-success [::logout-success]
                    :on-failure [::logout-failure]})}))

;; Handle successful logout
(rf/reg-event-fx
  ::logout-success
  common-interceptors
  (fn [{:keys [db]} _]
    (log/info "Logout successful, redirecting to home")
    {:db db
     :redirect "/"}))

;; Handle logout failure
(rf/reg-event-fx
  ::logout-failure
  common-interceptors
  (fn [{:keys [db]} _]
    (log/error "Logout failed, but clearing local session anyway")
    {:db db
     :redirect "/"}))

;; ========================================================================
;; Effects
;; ========================================================================

;; ========================================================================
;; Email/Password Registration Events
;; ========================================================================

;; === Email/Password Registration (map-based, single definition) ===
(rf/reg-event-fx
  ::register-user
  common-interceptors
  (fn [{:keys [db]} [{:keys [email full-name password confirm-password]}]]
    ;; Derive an effective password in case the primary field didn't bind correctly
    (let [effective-password (if (seq password) password confirm-password)
          ;; Normalize keys to the exact wire names the backend expects
          payload {:email email
                   :full-name full-name
                   :password effective-password}]
      ;; Debug: log payload going into register-user
      (js/console.log
        (str "register-user event payload "
          (js/JSON.stringify (clj->js payload))))
      {:db (assoc-in db [:session :loading?] true)
       :http-xhrio (http/post-request
                     {:uri "/api/v1/auth/register"
                      :params payload
                      :on-success [::register-user-success]
                      :on-failure [::register-user-failure]})})))

(rf/reg-event-fx
  ::register-user-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [success? (get response :success false)
          message (get response :message "Registration successful")
          verification-required? (get response :verification-required false)
          user (get response :user)]
      (if success?
        {:db (-> db
               (assoc-in [:session :loading?] false)
               (assoc-in [:session :registered?] true)
               (assoc-in [:session :verification-required?] verification-required?)
               (assoc-in [:session :user] user)
               (assoc-in [:session :registration-message] message)
               (update :session dissoc :error))
         :fx [[:dispatch
               [:app.template.frontend.events.messages/show-success
                "Registration Successful"
                message]]]}
        {:db (-> db
               (assoc-in [:session :loading?] false)
               (assoc-in [:session :error] (get response :error "Registration failed")))
         :fx [[:dispatch
               [:app.template.frontend.events.messages/show-error
                "Registration Failed"
                (get response :error "Please try again.")]]]}))))

(rf/reg-event-fx
  ::register-user-failure
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [resp (:response response)
          ;; Prefer field-specific validation messages when present
          field-error (or (get-in resp [:details :email 0])
                        (get-in resp [:details :password 0])
                        (get-in resp [:details :full-name 0])
                        (get-in resp [:details :full_name 0]))
          error-message (or field-error (http/extract-error-message response))]
      (log/error "User registration failed:" error-message)
      {:db (-> db
             (assoc-in [:session :loading?] false)
             (assoc-in [:session :error] error-message))
       :fx [[:dispatch
             [:app.template.frontend.events.messages/show-error
              "Registration Failed"
              error-message]]]})))

;; ========================================================================
;; Email/Password Login Events
;; ========================================================================

;; Event to handle user login with email and password
(rf/reg-event-fx
  ::login-with-password
  common-interceptors
  (fn [{:keys [db]} [email password]]
    {:db (assoc-in db [:session :loading?] true)
     :http-xhrio (http/api-request
                   {:method :post
                    :uri "/api/v1/auth/login"
                    :params {:email email :password password}
                    :on-success [::login-with-password-success]
                    :on-failure [::login-with-password-failure]})}))

;; Handle successful email/password login
(rf/reg-event-fx
  ::login-with-password-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [success? (get response :success false)
          user (get response :user)
          message (get response :message "Login successful")]

      (if success?
        (do
          (log/info "Email/password login successful for user:" (:email user))
          ;; Trigger auth status refresh to get complete session data
          {:db (-> db
                 (assoc-in [:session :loading?] false)
                 (assoc-in [:session :login-success?] true)
                 (assoc-in [:session :login-message] message)
                 ;; Clear any previous errors
                 (update :session dissoc :error))
           :fx [[:dispatch [::fetch-auth-status]
                 [:dispatch [:app.template.frontend.events.messages/show-success]
                  message
                  "Welcome back!"]]]})
        (do
          (log/warn "Email/password login failed:" response)
          {:db (-> db
                 (assoc-in [:session :loading?] false)
                 (assoc-in [:session :error] (get response :error "Invalid email or password")))
           :fx [[:dispatch [:app.template.frontend.events.messages/show-error]
                 "Login Failed"
                 (get response :error "Invalid email or password")]]})))))

;; Handle email/password login failure
(rf/reg-event-fx
  ::login-with-password-failure
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [resp (:response response)
          field-error (or (get-in resp [:details :email 0])
                        (get-in resp [:details :password 0]))
          error-message (or field-error (http/extract-error-message response))]
      (log/error "Email/password login failed:" error-message)
      {:db (-> db
             (assoc-in [:session :loading?] false)
             (assoc-in [:session :error] error-message))
       :fx [[:dispatch [:app.template.frontend.events.messages/show-error]
             "Login Failed"
             error-message]]})))

;; ========================================================================
;; Email Verification Events
;; ========================================================================

;; Event to handle email verification (from link in email)
(rf/reg-event-fx
  ::verify-email
  common-interceptors
  (fn [{:keys [db]} [_ token]]
    {:db (assoc-in db [:session :loading?] true)
     :http-xhrio (http/api-request
                   {:method :get
                    :uri (str "/api/v1/auth/verify-email?token=" token)
                    :on-success [::verify-email-success]
                    :on-failure [::verify-email-failure]})}))

;; Handle successful email verification
(rf/reg-event-fx
  ::verify-email-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [success? (get response :success false)
          message (get response :message "Email verification successful")]

      (if success?
        (do
          (log/info "Email verification successful")
          {:db (-> db
                 (assoc-in [:session :loading?] false)
                 (assoc-in [:session :email-verified?] true)
                 (assoc-in [:session :verification-message] message)
                 ;; Clear any previous errors
                 (update :session dissoc :error))
           :fx [[:dispatch [:app.template.frontend.events.messages/show-success]
                 "Email Verified"
                 message]]})
        (do
          (log/warn "Email verification failed:" response)
          {:db (-> db
                 (assoc-in [:session :loading?] false)
                 (assoc-in [:session :error] (get response :error "Verification failed")))
           :fx [[:dispatch [:app.template.frontend.events.messages/show-error]
                 "Verification Failed"
                 (get response :error "Invalid or expired verification link")]]})))))

;; Handle email verification failure
(rf/reg-event-fx
  ::verify-email-failure
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [error-message (http/extract-error-message response)]
      (log/error "Email verification failed:" error-message)
      {:db (-> db
             (assoc-in [:session :loading?] false)
             (assoc-in [:session :error] error-message))
       :fx [[:dispatch [:app.template.frontend.events.messages/show-error]
             "Verification Failed"
             error-message]]})))

;; ========================================================================
;; Utility Events
;; ========================================================================

;; Event to clear authentication state (for form resets)
(rf/reg-event-db
  ::clear-auth-state
  common-interceptors
  (fn [db _]
    (-> db
      (assoc-in [:session :loading?] false)
      (update :session dissoc
        :error
        :registration-message
        :verification-message
        :login-message
        :registered?
        :verification-required?))))

;; Event to set authentication error (for form validation)
(rf/reg-event-db
  ::set-auth-error
  common-interceptors
  (fn [db [_ error-message]]
    (assoc-in db [:session :error] error-message)))

;; Helper effect handler for redirects
(rf/reg-fx
 :redirect
  (fn [path]
    (set! (.-href js/window.location) path)))

;; ========================================================================
;; Password Reset Events (Forgot Password Flow)
;; ========================================================================

;; Page initialization for forgot password
(rf/reg-event-fx
  :page/init-forgot-password
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (-> db
           (assoc-in [:ui :current-page] :forgot-password)
           (assoc-in [:password-reset :loading?] false)
           (assoc-in [:password-reset :success?] false)
           (update :password-reset dissoc :error))}))

;; Page initialization for reset password (with token)
(rf/reg-event-fx
  :page/init-reset-password
  common-interceptors
  (fn [{:keys [db]} [token]]
    (let [;; If token is not passed as argument, try to get it from URL
          url-token (when (and (exists? js/window) (exists? js/URLSearchParams))
                      (-> js/window .-location .-search
                        (js/URLSearchParams.)
                        (.get "token")))
          effective-token (or token url-token)]
      {:db (-> db
             (assoc-in [:ui :current-page] :reset-password)
             (assoc-in [:password-reset :token] effective-token)
             (assoc-in [:password-reset :loading?] false)
             (assoc-in [:password-reset :token-verified?] false)
             (update :password-reset dissoc :error :success?))
       :fx [(when effective-token
              [:dispatch [::verify-reset-token effective-token]])]})))

;; Page initialization for change password (authenticated users)
(rf/reg-event-fx
  :page/init-change-password
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (-> db
           (assoc-in [:ui :current-page] :change-password)
           (update :change-password dissoc :loading? :error :success? :message))}))

;; Event to request password reset (forgot password form submission)
(rf/reg-event-fx
  ::request-password-reset
  common-interceptors
  (fn [{:keys [db]} [email]]
    (log/info "Password reset event - email param:" email "type:" (type email))
    (let [request-params {:email email}]
      (log/info "Password reset request params:" request-params)
      {:db (-> db
             (assoc-in [:password-reset :loading?] true)
             (update :password-reset dissoc :error :success?))
       :http-xhrio (http/api-request
                     {:method :post
                      :uri "/api/v1/auth/forgot-password"
                      :params request-params
                      :on-success [::request-password-reset-success]
                      :on-failure [::request-password-reset-failure]})})))

;; Handle successful password reset request
(rf/reg-event-fx
  ::request-password-reset-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [message (get response :message "Password reset instructions sent")]
      (log/info "Password reset request successful")
      {:db (-> db
             (assoc-in [:password-reset :loading?] false)
             (assoc-in [:password-reset :success?] true)
             (assoc-in [:password-reset :message] message)
             (update :password-reset dissoc :error))
       :fx [[:dispatch
             [:app.template.frontend.events.messages/show-success
              "Email Sent"
              message]]]})))

;; Handle password reset request failure
(rf/reg-event-fx
  ::request-password-reset-failure
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [error-message (http/extract-error-message response)]
      (log/error "Password reset request failed:" error-message)
      {:db (-> db
             (assoc-in [:password-reset :loading?] false)
             (assoc-in [:password-reset :error] error-message))
       :fx [[:dispatch
             [:app.template.frontend.events.messages/show-error
              "Request Failed"
              error-message]]]})))

;; Event to verify reset token
(rf/reg-event-fx
  ::verify-reset-token
  common-interceptors
  (fn [{:keys [db]} [token]]
    {:db (-> db
           (assoc-in [:password-reset :loading?] true)
           (update :password-reset dissoc :error))
     :http-xhrio (http/api-request
                   {:method :get
                    :uri (str "/api/v1/auth/verify-reset-token?token=" token)
                    :on-success [::verify-reset-token-success]
                    :on-failure [::verify-reset-token-failure]})}))

;; Handle successful token verification
(rf/reg-event-fx
  ::verify-reset-token-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [valid? (get response :valid false)]
      (if valid?
        {:db (-> db
               (assoc-in [:password-reset :loading?] false)
               (assoc-in [:password-reset :token-verified?] true)
               (update :password-reset dissoc :error))}
        {:db (-> db
               (assoc-in [:password-reset :loading?] false)
               (assoc-in [:password-reset :token-verified?] false)
               (assoc-in [:password-reset :error] "Invalid or expired reset link"))
         :fx [[:dispatch
               [:app.template.frontend.events.messages/show-error
                "Invalid Link"
                "This password reset link is invalid or has expired."]]]}))))

;; Handle token verification failure
(rf/reg-event-fx
  ::verify-reset-token-failure
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [error-message (http/extract-error-message response)]
      (log/error "Reset token verification failed:" error-message)
      {:db (-> db
             (assoc-in [:password-reset :loading?] false)
             (assoc-in [:password-reset :token-verified?] false)
             (assoc-in [:password-reset :error] error-message))
       :fx [[:dispatch
             [:app.template.frontend.events.messages/show-error
              "Invalid Link"
              "This password reset link is invalid or has expired."]]]})))

;; Event to reset password with token
(rf/reg-event-fx
  ::reset-password-with-token
  common-interceptors
  (fn [{:keys [db]} [token new-password]]
    {:db (-> db
           (assoc-in [:password-reset :loading?] true)
           (update :password-reset dissoc :error :success?))
     :http-xhrio (http/api-request
                   {:method :post
                    :uri "/api/v1/auth/reset-password"
                    :params {:token token :new-password new-password}
                    :on-success [::reset-password-with-token-success]
                    :on-failure [::reset-password-with-token-failure]})}))

;; Handle successful password reset
(rf/reg-event-fx
  ::reset-password-with-token-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [success? (get response :success false)
          message (get response :message "Password reset successful")]
      (if success?
        (do
          (log/info "Password reset successful")
          {:db (-> db
                 (assoc-in [:password-reset :loading?] false)
                 (assoc-in [:password-reset :success?] true)
                 (assoc-in [:password-reset :message] message)
                 (update :password-reset dissoc :error))
           :fx [[:dispatch
                 [:app.template.frontend.events.messages/show-success
                  "Password Reset"
                  message]]]})
        {:db (-> db
               (assoc-in [:password-reset :loading?] false)
               (assoc-in [:password-reset :error] (get response :error "Password reset failed")))
         :fx [[:dispatch
               [:app.template.frontend.events.messages/show-error
                "Reset Failed"
                (get response :error "Please try again.")]]]}))))

;; Handle password reset failure
(rf/reg-event-fx
  ::reset-password-with-token-failure
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [error-message (http/extract-error-message response)]
      (log/error "Password reset failed:" error-message)
      {:db (-> db
             (assoc-in [:password-reset :loading?] false)
             (assoc-in [:password-reset :error] error-message))
       :fx [[:dispatch
             [:app.template.frontend.events.messages/show-error
              "Reset Failed"
              error-message]]]})))

;; ========================================================================
;; Change Password Events (Authenticated Users)
;; ========================================================================

;; Event to change password for authenticated user
(rf/reg-event-fx
  ::change-password
  common-interceptors
  (fn [{:keys [db]} [_ current-password new-password]]
    {:db (-> db
           (assoc-in [:change-password :loading?] true)
           (update :change-password dissoc :error :success?))
     :http-xhrio (http/api-request
                   {:method :post
                    :uri "/api/v1/auth/change-password"
                    :params {:current-password current-password
                             :new-password new-password}
                    :on-success [::change-password-success]
                    :on-failure [::change-password-failure]})}))

;; Handle successful password change
(rf/reg-event-fx
  ::change-password-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [success? (get response :success false)
          message (get response :message "Password changed successfully")]
      (if success?
        (do
          (log/info "Password change successful")
          {:db (-> db
                 (assoc-in [:change-password :loading?] false)
                 (assoc-in [:change-password :success?] true)
                 (assoc-in [:change-password :message] message)
                 (update :change-password dissoc :error))
           :fx [[:dispatch
                 [:app.template.frontend.events.messages/show-success
                  "Password Changed"
                  message]]]})
        {:db (-> db
               (assoc-in [:change-password :loading?] false)
               (assoc-in [:change-password :error] (get response :error "Password change failed")))
         :fx [[:dispatch
               [:app.template.frontend.events.messages/show-error
                "Change Failed"
                (get response :error "Please try again.")]]]}))))

;; Handle password change failure
(rf/reg-event-fx
  ::change-password-failure
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [resp (:response response)
          field-error (or (get-in resp [:details :current-password 0])
                        (get-in resp [:details :new-password 0]))
          error-message (or field-error (http/extract-error-message response))]
      (log/error "Password change failed:" error-message)
      {:db (-> db
             (assoc-in [:change-password :loading?] false)
             (assoc-in [:change-password :error] error-message))
       :fx [[:dispatch
             [:app.template.frontend.events.messages/show-error
              "Change Failed"
              error-message]]]})))

;; Event to clear change password state
(rf/reg-event-db
  ::clear-change-password-state
  common-interceptors
  (fn [db _]
    (update db :change-password dissoc :loading? :error :success? :message)))

;; Event to clear password reset state
(rf/reg-event-db
  ::clear-password-reset-state
  common-interceptors
  (fn [db _]
    (dissoc db :password-reset)))
