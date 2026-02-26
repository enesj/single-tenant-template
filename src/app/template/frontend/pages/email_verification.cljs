(ns app.template.frontend.pages.email-verification
  "Email verification UI pages"
  (:require
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.icons :as icons]
    [uix.core :refer [$ defui]]))

(defui email-verified-page
  "Page shown after email verification attempt"
  []
  (let [url-params (js/URLSearchParams. js/window.location.search)
        success? (.get url-params "success")
        error-type (.get url-params "error")]

    ($ :div.min-h-screen.bg-base-200.flex.items-center.justify-center
      ($ :div.ds-card.w-full.max-w-md.bg-base-100.shadow-xl
        ($ :div.ds-card-body.text-center
          (if success?
               ;; Success state
            ($ :div
              ($ icons/check-circle {:class "w-16 h-16 text-success mx-auto mb-4"})
              ($ :h2.ds-card-title.justify-center.text-success "Email Verified!")
              ($ :p.text-base-content.opacity-75.mb-6
                "Your email has been successfully verified. You now have full access to all features.")
              ($ :div.ds-card-actions.justify-center
                ($ button
                  {:btn-type :primary
                   :id "btn-continue-to-app"
                   :on-click #(set! js/window.location.href "/")}
                  "Continue to App")))

               ;; Error state
            ($ :div
              ($ icons/exclamation-triangle {:class "w-16 h-16 text-error mx-auto mb-4"})
              ($ :h2.ds-card-title.justify-center.text-error "Verification Failed")
              ($ :p.text-base-content.opacity-75.mb-6
                (case error-type
                  "token-not-found" "The verification link is invalid."
                  "token-expired" "The verification link has expired."
                  "token-already-used" "This verification link has already been used."
                  "too-many-attempts" "Too many verification attempts. Please request a new link."
                  "database-error" "A technical error occurred. Please try again."
                  "An error occurred during verification."))
              ($ :div.ds-card-actions.justify-center.gap-2
                ($ button
                  {:btn-type :outline
                   :id "btn-back-to-login"
                   :on-click #(set! js/window.location.href "/login")}
                  "Back to Login")
                (when (contains? #{"token-expired" "token-not-found"} error-type)
                  ($ button
                    {:btn-type :primary
                     :id "btn-request-new-verification"
                     :on-click #(set! js/window.location.href "/login")}
                    "Request New Link"))))))))))

;; Re-frame events and subscriptions for email verification

;; Subscriptions


