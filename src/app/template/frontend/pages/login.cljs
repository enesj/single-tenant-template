(ns app.template.frontend.pages.login
  (:require
    [app.template.frontend.components.auth :refer [auth-error-alert
                                                   auth-form-field
                                                   auth-submit-button]]
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.icons :refer [google-icon]]
    [app.template.frontend.events.auth :as auth-events]
    [app.template.frontend.i18n :refer [use-t]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-state]]
    [uix.re-frame :refer [use-subscribe]]))

(defui login-page
  []
  (let [t (use-t)
        auth-status (use-subscribe [:auth-status])
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
                            (empty? email) (assoc :email (t :validation/email-required))
                            (not (re-matches #".+@.+\..+" email)) (assoc :email (t :validation/email-invalid))
                            (empty? password) (assoc :password (t :validation/password-required)))))

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
            ($ :h2 {:class "text-2xl font-bold text-base-content mb-2"} (t :login/welcome-back))
            ($ :p {:class "text-base-content/70"} (t :login/already-signed-in))

            ($ :div {:class "space-y-4"}
                ;; User info display
              (when-let [user (:user auth-status)]
                ($ :div {:class "bg-base-200 rounded-lg p-4"}
                  ($ :div
                    ($ :p {:class "font-medium"}
                      (or (:full-name user) (:name user) (:email user)))
                    (when-let [email (:email user)]
                      ($ :p {:class "text-sm text-base-content/70"} email))
                    (when-let [role (:role user)]
                      ($ :p {:class "text-xs text-base-content/50"}
                        (str (t :common/role) ": " role))))))

              ;; Tenant info for multi-tenant sessions
              (when tenant
                ($ :div {:class "bg-primary/10 rounded-lg p-4"}
                  ($ :h4 {:class "font-medium text-primary mb-1"} (t :common/organization))
                  ($ :p {:class "text-sm"} (:name tenant))))

              ;; Action buttons
              ($ :div {:class "flex space-x-3"}
                ($ button {:btn-type :primary
                           :class "flex-1"
                           :id "login-continue-btn"
                           :on-click #(set! (.-href js/window.location) "/")}
                  (t :login/continue-to-app))
                ($ button {:btn-type :outline
                           :class "flex-1"
                           :id "login-sign-out-btn"
                           :on-click #(rf/dispatch [:app.template.frontend.events.auth/logout])}
                  (t :common/sign-out)))))))

      ;; Not authenticated - show login options
      ($ :div {:class "min-h-screen flex items-center justify-center bg-base-200"}
        ($ :div {:class "max-w-md w-full bg-base-100 shadow-xl rounded-lg p-8"}
          ($ :div {:class "text-center"}
            ;; Header
            ($ :div {:class "mb-8"}
              ($ :h1 {:class "text-3xl font-bold text-base-content mb-2"} (t :login/welcome))
              ($ :p {:class "text-base-content/70"} (t :login/subtitle)))

            ;; Loading state
            (if loading?
              ($ :div {:class "flex flex-col items-center space-y-4"}
                ($ :div {:class "ds-loading ds-loading-spinner ds-loading-lg"})
                ($ :p {:class "text-base-content/70"} (t :login/checking-auth)))

              ;; Login options
              ($ :div {:class "space-y-4"}
                ;; Email/Password Login Form
                ($ :form {:id "login-form"
                          :on-submit handle-email-password-submit
                          :class "bg-base-100 rounded-lg p-6 mb-6 border border-base-300"}
                  ($ :h3 {:class "text-lg font-semibold mb-4"} (t :login/sign-in-with-email))

                  ;; Server-side authentication error
                  ($ auth-error-alert {:error (:error auth-status)})

                  ;; Email field
                  ($ auth-form-field {:label (t :common/email-address)
                                      :type "email"
                                      :placeholder (t :common/email-placeholder)
                                      :value email
                                      :field-id "login-email"
                                      :required true
                                      :on-change #(set-email! (.. % -target -value))})

                  ;; Email error
                  (when (:email form-errors)
                    ($ :p {:class "ds-label-text-alt text-error mb-4"}
                      (:email form-errors)))

                  ;; Password field
                  ($ auth-form-field {:label (t :common/password)
                                      :type "password"
                                      :placeholder (t :common/password-placeholder)
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
                      (t :login/forgot-password)))

                  ;; Submit button
                  ($ auth-submit-button {:loading? loading?
                                         :disabled? loading?
                                         :text (t :login/sign-in)
                                         :loading-text (t :login/signing-in)
                                         :button-id "login-submit-btn"}))

                ;; Divider
                ($ :div {:class "divider my-6"}
                  ($ :div {:class "divider-text"} (t :common/or)))

                ;; Google OAuth button
                ($ button {:btn-type :outline
                           :class "ds-btn-lg w-full"
                           :id "login-google-btn"
                           :on-click #(set! (.-href js/window.location) "/login/google")}
                  ($ google-icon)
                  (t :login/continue-google))

                ;; Help text
                ($ :div {:class "mt-6 text-center"}
                  ($ :p {:class "text-sm text-base-content/70"}
                    (t :login/agree-text))
                  ($ :div {:class "flex justify-center space-x-1 text-sm"}
                    ($ :a {:href "/terms" :class "ds-link ds-link-primary"} (t :common/terms))
                    ($ :span {:class "text-base-content/50"} (t :common/and))
                    ($ :a {:href "/privacy" :class "ds-link ds-link-primary"} (t :common/privacy)))
                  ($ :p {:class "text-sm text-base-content/70 mt-4"}
                    (t :login/no-account) " "
                    ($ :a {:href "/register" :class "ds-link ds-link-primary"}
                      (t :login/sign-up))))))))))))
