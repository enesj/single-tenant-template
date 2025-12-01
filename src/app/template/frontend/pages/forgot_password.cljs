(ns app.template.frontend.pages.forgot-password
  "User forgot password page"
  (:require
    [app.template.frontend.components.auth :refer [auth-form-header
                                                   auth-submit-button
                                                   auth-error-alert]]
    [app.template.frontend.events.auth :as auth-events]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]
    [uix.core :refer [$ defui use-state use-callback use-ref]]
    [uix.re-frame :refer [use-subscribe]]))

(defui forgot-password-page
  []
  (let [email-ref (use-ref nil)
        [form-errors set-form-errors!] (use-state {})
        loading? (use-subscribe [:password-reset/loading?])
        error (use-subscribe [:password-reset/error])
        success? (use-subscribe [:password-reset/success?])

        validate-form
        (use-callback
          (fn [email-val]
            (cond-> {}
              (empty? email-val)
              (assoc :email "Email is required")

              (and (not (empty? email-val))
                (not (re-matches #".+@.+\..+" email-val)))
              (assoc :email "Please enter a valid email address")))
          [])

        handle-submit
        (use-callback
          (fn [e]
            (.preventDefault e)
            ;; Read email from ref (uncontrolled input)
            (let [current-email (when @email-ref (.-value @email-ref))
                  _ (log/info "Form submit - email from ref:" current-email)
                  validation-errors (validate-form current-email)]
              (log/info "Form submit - validation-errors:" validation-errors)
              (if (empty? validation-errors)
                (do
                  (set-form-errors! {})
                  (log/info "Dispatching request-password-reset with email:" current-email)
                  (rf/dispatch [::auth-events/request-password-reset current-email]))
                (set-form-errors! validation-errors))))
          [validate-form])]

    ($ :div {:class "min-h-screen flex items-center justify-center bg-base-200"}
      ($ :div {:class "max-w-md w-full bg-base-100 shadow-xl rounded-lg p-8"}
        (if success?
          ;; Success message
          ($ :div {:class "text-center"}
            ($ :div {:class "w-20 h-20 mx-auto mb-6 bg-success/10 rounded-full flex items-center justify-center"}
              ($ :svg {:class "w-10 h-10 text-success" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
                ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                          :d "M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"})))
            ($ :h2 {:class "text-2xl font-bold mb-4"} "Check Your Email")
            ($ :p {:class "text-base-content/70 mb-6"}
              "If an account exists with this email, you'll receive password reset instructions.")
            ($ :a {:href "/login" :class "ds-btn ds-btn-primary"} "Back to Login"))

          ;; Forgot password form
          ($ :div {:class "text-center"}
            ($ auth-form-header
              {:title "Forgot Password"
               :subtitle "Enter your email to receive reset instructions"
               :icon-path "M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z"})

            ($ auth-error-alert {:error error})

            ($ :form {:id "forgot-password-form"
                      :on-submit handle-submit}

              ;; Uncontrolled email input (no :value binding)
              ($ :div {:class "mb-4"}
                ($ :label {:class "block text-sm font-medium text-base-content mb-2"
                           :for "forgot-email"}
                  "Email Address")
                ($ :input {:ref email-ref
                           :id "forgot-email"
                           :type "email"
                           :placeholder "Enter your email address"
                           :required true
                           :class "w-full px-4 py-2.5 text-base text-base-content bg-white border border-base-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary hover:border-base-400 transition-all duration-200 placeholder:text-base-content/40"}))

              (when (:email form-errors)
                ($ :p {:class "text-error text-sm mb-4"} (:email form-errors)))

              ($ auth-submit-button {:loading? loading?
                                     :text "Send Reset Link"
                                     :loading-text "Sending..."
                                     :button-id "forgot-submit-btn"
                                     :icon-path "M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"}))

            ($ :div {:class "text-center mt-6"}
              ($ :a {:href "/login" :class "ds-link ds-link-primary text-sm"}
                "← Back to Login"))))))))
