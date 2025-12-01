(ns app.template.frontend.components.change-password
  "Change password form component for authenticated users"
  (:require
    [app.template.frontend.components.auth :refer [auth-form-field
                                                   auth-submit-button
                                                   auth-error-alert]]
    [app.template.frontend.events.auth :as auth-events]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-state use-effect use-callback]]
    [uix.re-frame :refer [use-subscribe]]))

(defui change-password-form
  "Change password form for authenticated users.
   
   Can be embedded in a settings page or used standalone."
  []
  (let [[current-password set-current-password!] (use-state "")
        [new-password set-new-password!] (use-state "")
        [confirm-password set-confirm-password!] (use-state "")
        [form-errors set-form-errors!] (use-state {})

        loading? (use-subscribe [:change-password/loading?])
        error (use-subscribe [:change-password/error])
        success? (use-subscribe [:change-password/success?])

        validate-form
        (fn []
          (cond-> {}
            (empty? current-password)
            (assoc :current-password "Current password is required")

            (< (count new-password) 10)
            (assoc :new-password "Password must be at least 10 characters")

            (not= new-password confirm-password)
            (assoc :confirm-password "Passwords do not match")))

        reset-form!
        (use-callback
          (fn []
            (set-current-password! "")
            (set-new-password! "")
            (set-confirm-password! "")
            (set-form-errors! {}))
          [])

        handle-submit
        (fn [e]
          (.preventDefault e)
          (let [errors (validate-form)]
            (if (empty? errors)
              (do
                (set-form-errors! {})
                (rf/dispatch [::auth-events/change-password
                              current-password new-password]))
              (set-form-errors! errors))))]

    ;; Reset form on success
    (use-effect
      (fn []
        (when success?
          (reset-form!))
        js/undefined)
      [reset-form! success?])

    ($ :div {:class "ds-card bg-base-100 shadow-xl"}
      ($ :div {:class "ds-card-body"}
        ($ :h2 {:class "ds-card-title mb-4"} "Change Password")

        ;; Success message
        (when success?
          ($ :div {:class "ds-alert ds-alert-success mb-4"}
            ($ :svg {:class "stroke-current shrink-0 h-6 w-6" :fill "none" :viewBox "0 0 24 24"}
              ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                        :d "M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"}))
            ($ :span "Password changed successfully!")))

        ;; Error message
        ($ auth-error-alert {:error error})

        ($ :form {:on-submit handle-submit}

          ;; Current password
          ($ auth-form-field {:label "Current Password"
                              :type "password"
                              :placeholder "Enter your current password"
                              :value current-password
                              :field-id "current-password"
                              :required true
                              :on-change #(set-current-password! (.. % -target -value))})

          (when (:current-password form-errors)
            ($ :p {:class "text-error text-sm mb-4"} (:current-password form-errors)))

          ;; New password
          ($ auth-form-field {:label "New Password"
                              :type "password"
                              :placeholder "Enter new password (min. 10 characters)"
                              :value new-password
                              :field-id "new-password"
                              :required true
                              :on-change #(set-new-password! (.. % -target -value))})

          (when (:new-password form-errors)
            ($ :p {:class "text-error text-sm mb-4"} (:new-password form-errors)))

          ;; Confirm new password
          ($ auth-form-field {:label "Confirm New Password"
                              :type "password"
                              :placeholder "Confirm your new password"
                              :value confirm-password
                              :field-id "confirm-new-password"
                              :required true
                              :on-change #(set-confirm-password! (.. % -target -value))})

          (when (:confirm-password form-errors)
            ($ :p {:class "text-error text-sm mb-4"} (:confirm-password form-errors)))

          ;; Submit button
          ($ :div {:class "ds-card-actions justify-end mt-4"}
            ($ auth-submit-button {:loading? loading?
                                   :text "Change Password"
                                   :loading-text "Changing..."
                                   :button-id "change-password-btn"
                                   :icon-path "M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z"})))))))

(defui change-password-page
  "Standalone change password page for authenticated users"
  []
  (let [auth-status (use-subscribe [:auth-status])]
    (if (:authenticated auth-status)
      ($ :div {:class "min-h-screen bg-base-200 py-8"}
        ($ :div {:class "max-w-md mx-auto px-4"}
          ($ :div {:class "mb-6"}
            ($ :a {:href "/" :class "ds-link ds-link-primary text-sm"}
              "← Back to Home"))
          ($ change-password-form)))

      ;; Not authenticated - redirect to login
      ($ :div {:class "min-h-screen flex items-center justify-center bg-base-200"}
        ($ :div {:class "text-center"}
          ($ :h2 {:class "text-2xl font-bold mb-4"} "Authentication Required")
          ($ :p {:class "text-base-content/70 mb-6"}
            "Please sign in to change your password.")
          ($ :a {:href "/login" :class "ds-btn ds-btn-primary"} "Sign In"))))))
