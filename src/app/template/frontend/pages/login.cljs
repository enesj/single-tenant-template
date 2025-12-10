(ns app.template.frontend.pages.login
  (:require
    [app.template.frontend.components.auth :refer [auth-error-alert
                            auth-form-field
                            auth-submit-button]]
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.icons :refer [google-icon]]
    [app.template.frontend.events.auth :as auth-events]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-state]]
    [uix.re-frame :refer [use-subscribe]]))

(defui login-page
  []
  (let [auth-status (use-subscribe [:auth-status])
        loading? (get-in auth-status [:loading?] false)
        tenant (use-subscribe [:current-tenant])

        ;; Email/password login form state
        [email set-email!] (use-state "")
        [password set-password!] (use-state "")
        [form-errors set-form-errors!] (use-state {})

        ;; Form validation and submission
        validate-form (fn []
                        (let [errors {}]
                          (cond-> errors
                            (empty? email) (assoc :email "Email is required")
                            (not (re-matches #".+@.+\..+" email)) (assoc :email "Please enter a valid email address")
                            (empty? password) (assoc :password "Password is required"))))

        handle-email-password-submit (fn [e]
                                       (.preventDefault e)
                                       (let [validation-errors (validate-form)]
                                         (if (empty? validation-errors)
                                           (do
                                             (set-form-errors! {})
                                             (rf/dispatch [::auth-events/login-with-password email password]))
                                           (set-form-errors! validation-errors))))]

    ;; If already authenticated, show success message
    (if (:authenticated auth-status)
      ($ :div {:class "min-h-screen flex items-center justify-center bg-base-200"}
        ($ :div {:class "max-w-md w-full bg-base-100 shadow-xl rounded-lg p-8"}
          ($ :div {:class "text-center"}
            ($ :div {:class "mb-6"}
              ($ :div {:class "w-16 h-16 bg-success rounded-full flex items-center justify-center mx-auto mb-4"}
                ($ :svg {:class "w-8 h-8 text-success-content" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
                  ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"}
                    :d "M5 13l4 4L19 7"))))
            ($ :h2 {:class "text-2xl font-bold text-base-content mb-2"} "Welcome Back!")
            ($ :p {:class "text-base-content/70"} "You are already signed in.")

            ($ :div {:class "space-y-4"}
                ;; User info display
              (when-let [user (:user auth-status)]
                ($ :div {:class "bg-base-200 rounded-lg p-4"}
                  (if (:legacy-session auth-status)
                    ;; Legacy session display
                    ($ :div
                      ($ :p {:class "font-medium"} (:name user))
                      ($ :p {:class "text-sm text-base-content/70"} (:email user))
                      (when (:provider auth-status)
                        ($ :p {:class "text-xs text-base-content/50"}
                          (str "via " (name (:provider auth-status))))))
                    ;; Multi-tenant session display
                    ($ :div
                      ($ :p {:class "font-medium"} (:full-name user))
                      ($ :p {:class "text-sm text-base-content/70"} (:email user))
                      ($ :p {:class "text-xs text-base-content/50"}
                        (str "Role: " (:role user)))))))

              ;; Tenant info for multi-tenant sessions
              (when tenant
                ($ :div {:class "bg-primary/10 rounded-lg p-4"}
                  ($ :h4 {:class "font-medium text-primary mb-1"} "Organization")
                  ($ :p {:class "text-sm"} (:name tenant))
                  ($ :p {:class "text-xs text-base-content/50"}
                    (str "Plan: " (:subscription-tier tenant)))))

              ;; Action buttons
              ($ :div {:class "flex space-x-3"}
                ($ button {:btn-type :primary
                           :class "flex-1"
                           :id "login-continue-btn"
                           :on-click #(set! (.-href js/window.location) "/")}
                  "Continue to App")
                ($ button {:btn-type :outline
                           :class "flex-1"
                           :id "login-sign-out-btn"
                           :on-click #(rf/dispatch [:app.template.frontend.events.auth/logout])}
                  "Sign Out"))))))

      ;; Not authenticated - show login options
      ($ :div {:class "min-h-screen flex items-center justify-center bg-base-200"}
        ($ :div {:class "max-w-md w-full bg-base-100 shadow-xl rounded-lg p-8"}
          ($ :div {:class "text-center"}
            ;; Header
            ($ :div {:class "mb-8"}
              ($ :h1 {:class "text-3xl font-bold text-base-content mb-2"} "Welcome")
              ($ :p {:class "text-base-content/70"} "Sign in to your account to continue"))

            ;; Loading state
            (if loading?
              ($ :div {:class "flex flex-col items-center space-y-4"}
                ($ :div {:class "ds-loading ds-loading-spinner ds-loading-lg"})
                ($ :p {:class "text-base-content/70"} "Checking authentication..."))

              ;; Login options
              ($ :div {:class "space-y-4"}
                ;; Email/Password Login Form
                ($ :form {:id "login-form"
                          :on-submit handle-email-password-submit
                          :class "bg-base-100 rounded-lg p-6 mb-6 border border-base-300"}
                  ($ :h3 {:class "text-lg font-semibold mb-4"} "Sign in with Email")

                  ;; Server-side authentication error
                  ($ auth-error-alert {:error (:error auth-status)})

                  ;; Email field
                  ($ auth-form-field {:label "Email Address"
                                      :type "email"
                                      :placeholder "Enter your email address"
                                      :value email
                                      :field-id "login-email"
                                      :required true
                                      :on-change #(set-email! (.. % -target -value))})

                  ;; Email error
                  (when (:email form-errors)
                    ($ :p {:class "ds-label-text-alt text-error mb-4"}
                      (:email form-errors)))

                  ;; Password field
                  ($ auth-form-field {:label "Password"
                                      :type "password"
                                      :placeholder "Enter your password"
                                      :value password
                                      :field-id "login-password"
                                      :required true
                                      :on-change #(set-password! (.. % -target -value))})

                  ;; Password error
                  (when (:password form-errors)
                    ($ :p {:class "ds-label-text-alt text-error mb-4"}
                      (:password form-errors)))

                  ;; Forgot password link
                  ($ :div {:class "text-right mb-4"}
                    ($ :a {:href "/forgot-password" :class "ds-link ds-link-primary text-sm"}
                      "Forgot password?"))

                  ;; Submit button
                  ($ auth-submit-button {:loading? loading?
                                         :disabled? loading?
                                         :text "Sign In"
                                         :loading-text "Signing in..."
                                         :button-id "login-submit-btn"}))

                ;; Divider
                ($ :div {:class "divider my-6"}
                  ($ :div {:class "divider-text"} "OR"))

                ;; Google OAuth button
                ($ button {:btn-type :outline
                           :class "ds-btn-lg w-full"
                           :id "login-google-btn"
                           :on-click #(set! (.-href js/window.location) "/login/google")}
                  ($ google-icon)
                  "Continue with Google")

                ;; Help text
                ($ :div {:class "mt-6 text-center"}
                  ($ :p {:class "text-sm text-base-content/70"}
                    "By signing in, you agree to our")
                  ($ :div {:class "flex justify-center space-x-1 text-sm"}
                    ($ :a {:href "/terms" :class "ds-link ds-link-primary"} "Terms of Service")
                    ($ :span {:class "text-base-content/50"} "and")
                    ($ :a {:href "/privacy" :class "ds-link ds-link-primary"} "Privacy Policy"))
                  ($ :p {:class "text-sm text-base-content/70 mt-4"}
                    "Don't have an account? "
                    ($ :a {:href "/register" :class "ds-link ds-link-primary"}
                      "Sign up")))))))))))

;; Helper component for authentication loading state
(defui auth-loading-component
  []
  ($ :div {:class "min-h-screen flex items-center justify-center bg-base-200"}
    ($ :div {:class "max-w-md w-full bg-base-100 shadow-xl rounded-lg p-8"}
      ($ :div {:class "text-center space-y-4"}
        ($ :div {:class "ds-loading ds-loading-spinner ds-loading-lg text-primary"})
        ($ :h2 {:class "text-xl font-semibold"} "Authenticating...")
        ($ :p {:class "text-base-content/70"} "Please wait while we sign you in")))))
