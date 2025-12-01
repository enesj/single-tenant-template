(ns app.template.frontend.pages.email-verification
  "Email verification UI pages"
  (:require
    [ajax.core :as ajax]
    [app.template.frontend.components.modal-wrapper :refer [modal-wrapper]]
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.icons :as icons]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]))

(defui email-verification-banner
  "Banner component to show when user needs email verification"
  []
  (let [verification-status (use-subscribe [:auth/verification-status])
        needs-verification? (:needs-verification verification-status)
        verification-status-value (:verification-status verification-status)]

    (when needs-verification?
      ($ :div.ds-alert.ds-alert-warning.mb-4 {:id "email-verification-banner"}
        ($ :div.flex.items-center.gap-3
          ($ icons/exclamation-triangle {:class "w-5 h-5"})
          ($ :div.flex-1
            ($ :div.font-medium "Email Verification Required")
            ($ :div.text-sm.opacity-75
              (case verification-status-value
                "unverified" "Please verify your email address to access all features."
                "pending" "We've sent a verification email. Please check your inbox."
                "Please verify your email address.")))
          (when (= "unverified" verification-status-value)
            ($ button
              {:btn-type :warning
               :class "ds-btn-sm"
               :id "btn-send-verification-email"
               :on-click #(rf/dispatch [:auth/send-verification-email])}
              "Send Verification Email"))
          (when (= "pending" verification-status-value)
            ($ button
              {:btn-type :warning
               :class "ds-btn-sm"
               :id "btn-resend-verification-email"
               :on-click #(rf/dispatch [:auth/resend-verification-email])}
              "Resend Email")))))))

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

(defui verification-email-sent-modal
  "Modal shown after verification email is sent"
  []
  (let [show-modal? (use-subscribe [:auth/verification-email-sent-modal])]

    ($ modal-wrapper
      {:visible? show-modal?
       :title "Verification Email Sent"
       :size :medium
       :on-close [:auth/close-verification-email-modal]
       :close-button-id "btn-close-verification-modal-x"}

      ($ :div {:class "text-center"}
        ($ icons/mail {:class "w-16 h-16 text-primary mx-auto mb-4"})
        ($ :p {:class "text-base-content opacity-75 mb-6"}
          "We've sent a verification email to your address. Please check your inbox and click the verification link.")
        ($ :div {:class "text-sm text-base-content opacity-60 mb-6"}
          "Didn't receive the email? Check your spam folder or try resending.")
        ($ :div {:class "ds-modal-action justify-center gap-2"}
          ($ button
            {:btn-type :outline
             :id "btn-close-verification-modal"
             :on-click #(rf/dispatch [:auth/close-verification-email-modal])}
            "Close")
          ($ button
            {:btn-type :primary
             :id "btn-resend-from-modal"
             :on-click #(rf/dispatch [:auth/resend-verification-email])}
            "Resend Email"))))))

(defui verification-settings-card
  "Card component for email verification settings"
  []
  (let [user (use-subscribe [:auth/current-user])
        verification-status (use-subscribe [:auth/verification-status])
        loading? (use-subscribe [:auth/verification-loading])]

    ($ :div.ds-card.bg-base-100.shadow-lg {:id "verification-settings-card"}
      ($ :div.ds-card-body
        ($ :h3.ds-card-title.text-lg "Email Verification")

        ($ :div.flex.items-center.gap-3.mb-4
          (if (:email-verified verification-status)
            ($ :div.flex.items-center.gap-2.text-success
              ($ icons/check-circle {:class "w-5 h-5"})
              ($ :span "Verified"))
            ($ :div.flex.items-center.gap-2.text-warning
              ($ icons/exclamation-triangle {:class "w-5 h-5"})
              ($ :span "Not Verified")))

          ($ :div.text-sm.opacity-75 (:email user)))

        ($ :div.text-sm.text-base-content.opacity-75.mb-4
          (if (:email-verified verification-status)
            "Your email address has been verified and you have full access to all features."
            "Please verify your email address to ensure account security and access to all features."))

        (when (not (:email-verified verification-status))
          ($ :div.ds-card-actions
            ($ button
              {:btn-type :primary
               :id "btn-send-verification-settings"
               :disabled loading?
               :on-click #(rf/dispatch [:auth/send-verification-email])}
              (if loading? "Sending..." "Send Verification Email"))))))))

;; Re-frame events and subscriptions for email verification
(rf/reg-event-fx
  :auth/send-verification-email
  (fn [{:keys [db]} _]
    {:http-xhrio {:method :post
                  :uri "/api/v1/auth/resend-verification"
                  :format (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [:auth/verification-email-sent]
                  :on-failure [:auth/verification-email-failed]}
     :db (assoc-in db [:auth :verification-loading] true)}))

(rf/reg-event-fx
  :auth/resend-verification-email
  (fn [_cofx _]
    (rf/dispatch [:auth/send-verification-email])))

(rf/reg-event-db
  :auth/verification-email-sent
  (fn [db [_ _response]]
    (-> db
      (assoc-in [:auth :verification-loading] false)
      (assoc-in [:auth :verification-email-sent-modal] true)
      (assoc-in [:auth :user :email-verification-status] "pending"))))

(rf/reg-event-db
  :auth/verification-email-failed
  (fn [db [_ error]]
    (-> db
      (assoc-in [:auth :verification-loading] false)
      (assoc-in [:auth :verification-error] (:response error)))))

(rf/reg-event-db
  :auth/close-verification-email-modal
  (fn [db _]
    (assoc-in db [:auth :verification-email-sent-modal] false)))

(rf/reg-event-fx
  :auth/fetch-verification-status
  (fn [_ _]
    {:http-xhrio {:method :get
                  :uri "/api/v1/auth/verification-status"
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [:auth/verification-status-loaded]
                  :on-failure [:auth/verification-status-failed]}}))

(rf/reg-event-db
  :auth/verification-status-loaded
  (fn [db [_ status]]
    (assoc-in db [:auth :verification-status] status)))

(rf/reg-event-db
  :auth/verification-status-failed
  (fn [db [_ error]]
    (assoc-in db [:auth :verification-error] (:response error))))

;; Subscriptions
(rf/reg-sub
  :auth/verification-status
  (fn [db _]
    (get-in db [:auth :verification-status]
      {:email-verified false
       :verification-status "unverified"
       :needs-verification true})))

(rf/reg-sub
  :auth/verification-loading
  (fn [db _]
    (get-in db [:auth :verification-loading] false)))

(rf/reg-sub
  :auth/verification-email-sent-modal
  (fn [db _]
    (get-in db [:auth :verification-email-sent-modal] false)))

(rf/reg-sub
  :auth/verification-error
  (fn [db _]
    (get-in db [:auth :verification-error])))
