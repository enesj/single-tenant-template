(ns app.template.frontend.pages.email-verification
  "Email verification UI pages"
  (:require
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.icons :as icons]
    [app.template.frontend.i18n :refer [use-t]]
    [uix.core :refer [$ defui]]))

(defui email-verified-page
  "Page shown after email verification attempt or pending verification"
  []
  (let [t (use-t)
        url-params (js/URLSearchParams. js/window.location.search)
        success? (.get url-params "success")
        pending? (.get url-params "pending")
        error-type (.get url-params "error")]

    ($ :div.min-h-screen.bg-base-200.flex.items-center.justify-center
      ($ :div.ds-card.w-full.max-w-md.bg-base-100.shadow-xl
        ($ :div.ds-card-body.text-center
          (cond
            ;; Pending state — user just signed up (OAuth or email), needs to verify
            pending?
            ($ :div
              ($ icons/mail {:class "w-16 h-16 text-info mx-auto mb-4"})
              ($ :h2.ds-card-title.justify-center.text-info (t :email-verification/pending-title))
              ($ :p.text-base-content.opacity-75.mb-6
                (t :email-verification/pending-desc))
              ($ :div.ds-card-actions.justify-center
                ($ button
                  {:btn-type :outline
                   :id "btn-pending-to-login"
                   :on-click #(set! js/window.location.href "/login")}
                  (t :email-verification/back-to-login))))

            ;; Success state — email verified, workspace created
            success?
            ($ :div
              ($ icons/check-circle {:class "w-16 h-16 text-success mx-auto mb-4"})
              ($ :h2.ds-card-title.justify-center.text-success (t :email-verification/verified-title))
              ($ :p.text-base-content.opacity-75.mb-6
                (t :email-verification/verified-desc))
              ($ :div.ds-card-actions.justify-center
                ($ button
                  {:btn-type :primary
                   :id "btn-continue-to-app"
                   :on-click #(set! js/window.location.href "/login")}
                  (t :email-verification/sign-in))))

            ;; Error state
            :else
            ($ :div
              ($ icons/exclamation-triangle {:class "w-16 h-16 text-error mx-auto mb-4"})
              ($ :h2.ds-card-title.justify-center.text-error (t :email-verification/failed-title))
              ($ :p.text-base-content.opacity-75.mb-6
                (case error-type
                  "token-not-found" (t :email-verification/err-not-found)
                  "token-expired" (t :email-verification/err-expired)
                  "token-already-used" (t :email-verification/err-already-used)
                  "too-many-attempts" (t :email-verification/err-too-many)
                  "database-error" (t :email-verification/err-db)
                  "email-send-failed" (t :email-verification/err-email-send-failed)
                  "workspace-provisioning-failed" (t :email-verification/err-workspace-provisioning)
                  (t :email-verification/err-default)))
              ($ :div.ds-card-actions.justify-center.gap-2
                ($ button
                  {:btn-type :outline
                   :id "btn-back-to-login"
                   :on-click #(set! js/window.location.href "/login")}
                  (t :email-verification/back-to-login))
                (when (contains? #{"token-expired" "token-not-found" "email-send-failed"} error-type)
                  ($ button
                    {:btn-type :primary
                     :id "btn-request-new-verification"
                     :on-click #(set! js/window.location.href "/login")}
                    (t :email-verification/request-new-link)))))))))))

;; Re-frame events and subscriptions for email verification

;; Subscriptions


