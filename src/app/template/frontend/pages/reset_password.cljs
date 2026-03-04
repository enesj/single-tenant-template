(ns app.template.frontend.pages.reset-password
  "User reset password page (with token from email)"
  (:require
    [app.template.frontend.components.auth :refer [auth-form-container
                                                   auth-form-header
                                                   auth-form-field
                                                   auth-submit-button
                                                   auth-error-alert]]
    [app.template.frontend.events.auth :as auth-events]
    [app.template.frontend.i18n :refer [use-t]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-state use-effect]]
    [uix.re-frame :refer [use-subscribe]]))

(defui reset-password-page
  []
  (let [t (use-t)
        [password set-password!] (use-state "")
        [confirm-password set-confirm-password!] (use-state "")
        [form-errors set-form-errors!] (use-state {})

        ;; Get token from URL
        token (-> js/window .-location .-search
                (js/URLSearchParams.)
                (.get "token"))

        loading? (use-subscribe [:password-reset/loading?])
        error (use-subscribe [:password-reset/error])
        success? (use-subscribe [:password-reset/success?])
        token-valid? (use-subscribe [:password-reset/token-verified?])

        validate-form
        (fn []
          (cond-> {}
            (< (count password) 10)
            (assoc :password (t :validation/password-min))

            (not= password confirm-password)
            (assoc :confirm-password (t :validation/passwords-no-match))))

        handle-submit
        (fn [e]
          (.preventDefault e)
          (let [errors (validate-form)]
            (if (empty? errors)
              (do
                (set-form-errors! {})
                (rf/dispatch [::auth-events/reset-password-with-token token password]))
              (set-form-errors! errors))))]

    ;; Verify token on mount
    (use-effect
      (fn []
        (when (and token (seq token))
          (rf/dispatch [::auth-events/verify-reset-token token]))
        js/undefined)
      [token])

    ($ :div {:class "min-h-screen flex items-center justify-center bg-base-200"}
      ($ :div {:class "max-w-md w-full"}
        ($ auth-form-container
          (cond
            ;; No token provided
            (or (nil? token) (not (seq token)))
            ($ :div {:class "text-center p-4"}
              ($ :div {:class "text-error text-6xl mb-4"} "⚠")
              ($ :h2 {:class "text-2xl font-bold mb-4"} (t :reset-password/missing-token-title))
              ($ :p {:class "text-base-content/70 mb-6"} (t :reset-password/missing-token-desc))
              ($ :a {:href "/forgot-password" :class "ds-btn ds-btn-primary"}
                (t :reset-password/request-new-link)))

            ;; Checking token (loading state)
            (and loading? (not token-valid?) (not error))
            ($ :div {:class "text-center p-8"}
              ($ :div {:class "ds-loading ds-loading-spinner ds-loading-lg"})
              ($ :p {:class "mt-4 text-base-content/70"} (t :reset-password/verifying)))

            ;; Token invalid
            (and (not loading?) error (not token-valid?))
            ($ :div {:class "text-center p-4"}
              ($ :div {:class "w-20 h-20 mx-auto mb-6 bg-error/10 rounded-full flex items-center justify-center"}
                ($ :svg {:class "w-10 h-10 text-error" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
                  ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                            :d "M6 18L18 6M6 6l12 12"})))
              ($ :h2 {:class "text-2xl font-bold mb-4"} (t :reset-password/invalid-title))
              ($ :p {:class "text-base-content/70 mb-2"}
                (or error (t :reset-password/invalid-default)))
              ($ :p {:class "text-base-content/70 mb-6"} (t :reset-password/invalid-request-new))
              ($ :a {:href "/forgot-password" :class "ds-btn ds-btn-primary"}
                (t :reset-password/request-new-link)))

            ;; Password reset success
            success?
            ($ :div {:class "text-center p-4"}
              ($ :div {:class "w-20 h-20 mx-auto mb-6 bg-success/10 rounded-full flex items-center justify-center"}
                ($ :svg {:class "w-10 h-10 text-success" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
                  ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                            :d "M5 13l4 4L19 7"})))
              ($ :h2 {:class "text-2xl font-bold mb-4"} (t :reset-password/success-title))
              ($ :p {:class "text-base-content/70 mb-6"} (t :reset-password/success-desc))
              ($ :a {:href "/login" :class "ds-btn ds-btn-primary"} (t :common/sign-in)))

            ;; Reset form (token valid)
            :else
            ($ :<>
              ($ auth-form-header
                {:title (t :reset-password/form-title)
                 :subtitle (t :reset-password/form-subtitle)
                 :icon-path "M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z"})

              ($ auth-error-alert {:error error})

              ($ :form {:id "reset-password-form"
                        :on-submit handle-submit}

                ($ auth-form-field {:label (t :reset-password/new-password)
                                    :type "password"
                                    :placeholder (t :reset-password/new-password-ph)
                                    :value password
                                    :field-id "reset-password"
                                    :required true
                                    :on-change #(set-password! (.. % -target -value))})

                (when (:password form-errors)
                  ($ :p {:class "text-error text-sm mb-4"} (:password form-errors)))

                ($ auth-form-field {:label (t :reset-password/confirm-password)
                                    :type "password"
                                    :placeholder (t :reset-password/confirm-password-ph)
                                    :value confirm-password
                                    :field-id "reset-confirm-password"
                                    :required true
                                    :on-change #(set-confirm-password! (.. % -target -value))})

                (when (:confirm-password form-errors)
                  ($ :p {:class "text-error text-sm mb-4"} (:confirm-password form-errors)))

                ($ auth-submit-button {:loading? loading?
                                       :text (t :reset-password/submit)
                                       :loading-text (t :reset-password/submitting)
                                       :button-id "reset-submit-btn"
                                       :icon-path "M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z"})))))))))
