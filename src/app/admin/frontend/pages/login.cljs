(ns app.admin.frontend.pages.login
  (:require
    [app.admin.frontend.events.auth]
    [app.admin.frontend.subs.auth]
    [app.template.frontend.components.auth :refer [auth-form-container auth-form-header
                                                   auth-error-alert auth-form-field
                                                   auth-submit-button auth-form-footer]]
    [re-frame.core :as rf]
    [reitit.frontend.easy :as rfe]
    [uix.core :refer [$ defui use-state]]
    [uix.re-frame :refer [use-subscribe]]))

(defui admin-login-form []
  (let [loading? (use-subscribe [:admin/login-loading?])
        error (use-subscribe [:admin/login-error])
        [email set-email!] (use-state "")
        [password set-password!] (use-state "")]

    ($ auth-form-container
      ;; Form header
      ($ auth-form-header {:title "Admin Panel"
                           :subtitle "Sign in to access the admin dashboard"})

      ;; Error alert
      ($ auth-error-alert {:error error})

      ;; Login form
      ($ :form {:id "admin-login-form"
                :on-submit (fn [e]
                             (.preventDefault e)
                             (rf/dispatch [:admin/login email password]))}

        ;; Email field
        ($ auth-form-field {:label "Email Address"
                            :type "email"
                            :placeholder "admin@company.com"
                            :value email
                            :field-id "admin-login-email"
                            :container-class "mb-4 lg:mb-6 2xl:mb-10"
                            :on-change #(set-email! (.. % -target -value))})

        ;; Password field
        ($ auth-form-field {:label "Password"
                            :type "password"
                            :placeholder "Enter your password"
                            :value password
                            :field-id "admin-login-password"
                            :container-class "mb-4"
                            :on-change #(set-password! (.. % -target -value))})

        ;; Forgot password link
        ($ :div {:class "text-right mb-6"}
          ($ :a {:href (rfe/href :admin-forgot-password)
                 :class "text-sm text-primary hover:text-primary-focus hover:underline transition-colors"}
            "Forgot password?"))

        ;; Submit button
        ($ auth-submit-button {:loading? loading?
                               :text "Sign In"
                               :button-id "admin-login-submit-btn"}))

      ;; Footer
      ($ auth-form-footer))))

(defui admin-login-page []
  ($ admin-login-form))
