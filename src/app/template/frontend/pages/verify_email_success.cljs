(ns app.template.frontend.pages.verify-email-success
  "Email verification success page component"
  (:require
    [app.template.frontend.components.auth :refer [auth-form-container
                                                   auth-form-header
                                                   auth-submit-button
                                                   auth-form-footer]]
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.icons :refer [google-icon]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]))


(defui verify-email-success-page
  []
  (let [auth-status (use-subscribe [:auth-status])
        loading? (get-in auth-status [:loading?] false)
        verification-message (get-in auth-status [:session :verification-message] "Email verification successful!")]

    ($ auth-form-container
      ;; Success message
      ($ auth-form-header {:title "Email Verified!"
                           :subtitle "Your email address has been successfully verified."})

      ;; Success alert
      ($ :div {:class "ds-alert ds-alert-success mb-6"}
        ($ :svg {:class "w-6 h-6" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
          ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                    :d "M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"}))
        ($ :div
          ($ :h3 {:class "font-semibold text-lg mb-2"} "Verification Complete!")
          ($ :p {:class "text-sm"} verification-message)))

      ;; Action buttons
      ($ :div {:class "space-y-4"}
        ($ button {:btn-type :primary
                   :class "w-full"
                   :id "verification-login-btn"
                   :on-click #(set! (.-href js/window.location) "/login")}
          "Sign In to Your Account")

        ($ button {:btn-type :outline
                   :class "w-full"
                   :id "verification-home-btn"
                   :on-click #(set! (.-href js/window.location) "/")}
          "Go to Homepage"))

      ;; Footer
      ($ auth-form-footer {:message "Your account is now ready to use"
                           :show-security-badge? true}))))
