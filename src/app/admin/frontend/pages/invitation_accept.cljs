(ns app.admin.frontend.pages.invitation-accept
  "Public admin invitation accept page.

   Flow:
   1. Extract token from URL query params
   2. Validate token server-side (shows invitation details or error)
   3. Collect full_name + password from the invitee
   4. On accept: create admin account, auto-login, redirect to dashboard"
  (:require
    [app.admin.frontend.events.admin-invitations]
    [app.template.frontend.components.auth
     :refer [auth-form-container
             auth-form-header
             auth-form-field
             auth-submit-button
             auth-error-alert]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-state use-effect]]
    [uix.re-frame :refer [use-subscribe]]))

(defui admin-invitation-accept-page
  []
  (let [[full-name set-full-name!] (use-state "")
        [password set-password!] (use-state "")
        [confirm-password set-confirm-password!] (use-state "")
        [form-errors set-form-errors!] (use-state {})

        ;; Get token from URL
        token (-> js/window .-location .-search
                (js/URLSearchParams.)
                (.get "token"))

        accept-state (use-subscribe [:admin/invitation-accept])
        loading? (:loading? accept-state)
        valid? (:valid? accept-state)
        email (:email accept-state)
        role (:role accept-state)
        inviter-name (:inviter-name accept-state)
        error (:error accept-state)
        accepting? (:accepting? accept-state)
        accepted? (:accepted? accept-state)
        accept-error (:accept-error accept-state)

        validate-form
        (fn []
          (cond-> {}
            (empty? full-name)
            (assoc :full-name "Full name is required")

            (< (count password) 10)
            (assoc :password "Password must be at least 10 characters")

            (not= password confirm-password)
            (assoc :confirm-password "Passwords do not match")))

        handle-submit
        (fn [e]
          (.preventDefault e)
          (let [errors (validate-form)]
            (if (empty? errors)
              (do
                (set-form-errors! {})
                (rf/dispatch [:admin/accept-invitation
                              {:token token
                               :full-name full-name
                               :password password}]))
              (set-form-errors! errors))))]

    ;; Validate token on mount
    (use-effect
      (fn []
        (when (and token (seq token))
          (rf/dispatch [:admin/validate-invitation-token token]))
        js/undefined)
      [token])

    ($ auth-form-container
      (cond
        ;; No token provided
        (or (nil? token) (empty? token))
        ($ :div {:class "text-center p-4"}
          ($ :div {:class "text-error text-6xl mb-4"} "!")
          ($ :h2 {:class "text-2xl font-bold mb-4"} "Missing Invitation Token")
          ($ :p {:class "text-base-content/70 mb-6"}
            "No invitation token was provided. Please use the link from your email.")
          ($ :a {:href "/admin/login" :class "ds-btn ds-btn-primary"} "Go to Login"))

        ;; Checking token (loading state)
        (and loading? (nil? valid?))
        ($ :div {:class "text-center p-8"}
          ($ :div {:class "ds-loading ds-loading-spinner ds-loading-lg"})
          ($ :p {:class "mt-4 text-base-content/70"} "Verifying invitation..."))

        ;; Token invalid or expired
        (and (not loading?) (not valid?) (some? valid?))
        ($ :div {:class "text-center p-4"}
          ($ :div {:class "w-20 h-20 mx-auto mb-6 bg-error/10 rounded-full flex items-center justify-center"}
            ($ :svg {:class "w-10 h-10 text-error" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
              ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                        :d "M6 18L18 6M6 6l12 12"})))
          ($ :h2 {:class "text-2xl font-bold mb-4"} "Invalid or Expired Invitation")
          ($ :p {:class "text-base-content/70 mb-2"}
            (or error "This invitation link is invalid or has expired."))
          ($ :p {:class "text-base-content/70 mb-6"}
            "Please contact the platform owner for a new invitation.")
          ($ :a {:href "/admin/login" :class "ds-btn ds-btn-primary"} "Go to Login"))

        ;; Successfully accepted
        accepted?
        ($ :div {:class "text-center p-4"}
          ($ :div {:class "w-20 h-20 mx-auto mb-6 bg-success/10 rounded-full flex items-center justify-center"}
            ($ :svg {:class "w-10 h-10 text-success" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
              ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                        :d "M5 13l4 4L19 7"})))
          ($ :h2 {:class "text-2xl font-bold mb-4"} "Welcome!")
          ($ :p {:class "text-base-content/70 mb-6"}
            "Your admin account has been created. Redirecting to dashboard..."))

        ;; Accept form (token valid)
        :else
        ($ :<>
          ($ auth-form-header
            {:title "Accept Admin Invitation"
             :subtitle (str "You've been invited by " inviter-name)
             :icon-path "M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z"})

          ;; Invitation details
          ($ :div {:class "mb-6 p-4 bg-base-200 rounded-lg"}
            ($ :div {:class "flex items-center gap-2 mb-2"}
              ($ :span {:class "text-sm font-medium text-base-content/70"} "Email:")
              ($ :span {:class "text-sm font-semibold"} email))
            ($ :div {:class "flex items-center gap-2"}
              ($ :span {:class "text-sm font-medium text-base-content/70"} "Role:")
              ($ :span {:class "ds-badge ds-badge-primary ds-badge-sm"} role)))

          ($ auth-error-alert {:error accept-error})

          ($ :form {:id "admin-accept-invitation-form"
                    :on-submit handle-submit}

            ($ auth-form-field {:label "Full Name"
                                :type "text"
                                :placeholder "Enter your full name"
                                :value full-name
                                :field-id "admin-accept-full-name"
                                :required true
                                :on-change #(set-full-name! (.. % -target -value))})

            (when (:full-name form-errors)
              ($ :p {:class "text-error text-sm mb-4"} (:full-name form-errors)))

            ($ auth-form-field {:label "Password"
                                :type "password"
                                :placeholder "Enter password (min. 10 characters)"
                                :value password
                                :field-id "admin-accept-password"
                                :required true
                                :on-change #(set-password! (.. % -target -value))})

            (when (:password form-errors)
              ($ :p {:class "text-error text-sm mb-4"} (:password form-errors)))

            ($ auth-form-field {:label "Confirm Password"
                                :type "password"
                                :placeholder "Confirm your password"
                                :value confirm-password
                                :field-id "admin-accept-confirm-password"
                                :required true
                                :on-change #(set-confirm-password! (.. % -target -value))})

            (when (:confirm-password form-errors)
              ($ :p {:class "text-error text-sm mb-4"} (:confirm-password form-errors)))

            ($ auth-submit-button {:loading? accepting?
                                   :text "Create Account & Sign In"
                                   :loading-text "Creating account..."
                                   :button-id "admin-accept-submit-btn"
                                   :icon-path "M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z"})))))))
