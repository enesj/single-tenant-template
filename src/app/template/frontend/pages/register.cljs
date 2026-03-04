(ns app.template.frontend.pages.register
  "User registration page with email/password authentication"
  (:require
    [app.template.frontend.components.auth :refer [auth-form-container
                                                   auth-form-header
                                                   auth-error-alert
                                                   auth-form-field
                                                   auth-submit-button
                                                   auth-form-footer]]
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.icons :refer [google-icon github-icon]]
    [app.template.frontend.events.auth :as auth-events]
    [app.template.frontend.i18n :refer [use-t]]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-state]]
    [uix.re-frame :refer [use-subscribe]]))

(defui registration-page
  "User registration form component"
  []
  (let [t (use-t)
        auth-status (use-subscribe [:auth-status])
        loading? (get-in auth-status [:loading?] false)
        error (:error auth-status)
        registered? (get-in auth-status [:session :registered?] false)
        registration-message (get-in auth-status [:session :registration-message])
        verification-required? (get-in auth-status [:session :verification-required?] false)

        ;; Form state
        [email set-email!] (use-state "")
        [full-name set-full-name!] (use-state "")
        [password set-password!] (use-state "")
        [confirm-password set-confirm-password!] (use-state "")
        [form-errors set-form-errors!] (use-state {})

        validate-form
        (fn [email* full-name* password* confirm-password*]
          (let [email-val (str/trim (or email* ""))
                full-name-val (str/trim (or full-name* ""))
                errors (cond-> {}
                         (str/blank? email-val) (assoc :email (t :validation/email-required))
                         (str/blank? full-name-val) (assoc :full-name (t :validation/full-name-required))
                         (< (count full-name-val) 2) (assoc :full-name (t :validation/full-name-min))
                         (str/blank? password*) (assoc :password (t :validation/password-required))
                         (< (count (or password* "")) 10) (assoc :password (t :validation/password-min))
                         (not= password* confirm-password*) (assoc :confirm-password (t :validation/passwords-no-match)))]
            (js/console.log "registration-validate"
              (clj->js {:email email-val
                        :full-name full-name-val
                        :password-present (boolean (seq password*))
                        :confirm-password-present (boolean (seq confirm-password*))
                        :errors errors}))
            errors))

        handle-submit
        (fn [e]
          (.preventDefault e)
          (let [email* email
                full-name* full-name
                password* password
                confirm-password* confirm-password
                validation-errors (validate-form email* full-name* password* confirm-password*)]
            (if (empty? validation-errors)
              (do
                (set-email! (str/trim (or email* "")))
                (set-full-name! (str/trim (or full-name* "")))
                (set-password! password*)
                (set-confirm-password! confirm-password*)
                (set-form-errors! {})
                (js/console.log
                  (str "registration-submit "
                    (js/JSON.stringify
                      #js {:email email*
                           :fullName full-name*
                           :password password*
                           :confirmPassword confirm-password*})))
                (rf/dispatch [::auth-events/register-user
                              {:email email*
                               :full-name full-name*
                               :password password*
                               :confirm-password confirm-password*}]))
              (set-form-errors! validation-errors))))]

    ;; If already registered, show success message
    (if registered?
      ($ auth-form-container
        ($ auth-form-header {:title (t :register/success-title)
                             :subtitle (t :register/success-subtitle)})

        ;; Success message
        (when verification-required?
          ($ :div {:class "ds-alert ds-alert-success mb-6"}
            ($ :svg {:class "w-6 h-6" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
              ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                        :d "M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"}))
            ($ :div
              ($ :h3 {:class "font-semibold text-lg mb-2"} (t :register/check-email))
              ($ :p {:class "text-sm"} registration-message))))

        (when (not verification-required?)
          ($ :div {:class "ds-alert ds-alert-success mb-6"}
            ($ :div
              ($ :h3 {:class "font-semibold text-lg mb-2"} (t :register/complete))
              ($ :p {:class "text-sm"} registration-message))))

        ;; Action buttons
        ($ :div {:class "space-y-4"}
          ($ button {:btn-type :primary
                     :class "w-full"
                     :id "registration-continue-btn"
                     :on-click #(set! (.-href js/window.location) "/login")}
            (t :register/continue-login))
          ($ button {:btn-type :outline
                     :class "w-full"
                     :id "registration-home-btn"
                     :on-click #(set! (.-href js/window.location) "/")}
            (t :register/go-home))))

      ;; Registration form
      ($ auth-form-container
        ($ auth-form-header {:title (t :register/title)
                             :subtitle (t :register/subtitle)})

        ;; Error alert
        ($ auth-error-alert {:error error})

        ;; Registration form
        ($ :form {:id "registration-form"
                  :on-submit handle-submit}

          ;; Email field
          ($ auth-form-field {:label (t :common/email-address)
                              :type "email"
                              :placeholder (t :common/email-placeholder)
                              :value email
                              :field-id "registration-email"
                              :required true
                              :on-change #(set-email! (.. % -target -value))})

          ;; Email error
          (when (:email form-errors)
            ($ :p {:class "ds-label-text-alt text-error mb-4"}
              (:email form-errors)))

          ;; Full name field
          ($ auth-form-field {:label (t :common/full-name)
                              :type "text"
                              :placeholder (t :common/full-name-placeholder)
                              :value full-name
                              :field-id "registration-full-name"
                              :required true
                              :on-change #(set-full-name! (.. % -target -value))})

          ;; Full name error
          (when (:full-name form-errors)
            ($ :p {:class "ds-label-text-alt text-error mb-4"}
              (:full-name form-errors)))

          ;; Password field
          ($ auth-form-field {:label (t :common/password)
                              :type "password"
                              :placeholder (t :register/password-placeholder)
                              :value password
                              :field-id "registration-password"
                              :required true
                              :on-change #(set-password! (.. % -target -value))})

          ;; Password error
          (when (:password form-errors)
            ($ :p {:class "ds-label-text-alt text-error mb-4"}
              (:password form-errors)))

          ;; Confirm password field
          ($ auth-form-field {:label (t :register/confirm-password)
                              :type "password"
                              :placeholder (t :register/confirm-password-placeholder)
                              :value confirm-password
                              :field-id "registration-confirm-password"
                              :required true
                              :on-change #(set-confirm-password! (.. % -target -value))})

          ;; Confirm password error
          (when (:confirm-password form-errors)
            ($ :p {:class "ds-label-text-alt text-error mb-4"}
              (:confirm-password form-errors)))

          ;; Submit button
          ($ auth-submit-button {:loading? loading?
                                 :disabled? (or loading?
                                              (empty? email)
                                              (empty? full-name)
                                              (empty? password)
                                              (empty? confirm-password))
                                 :text (t :register/submit)
                                 :loading-text (t :register/submitting)
                                 :button-id "registration-submit-btn"}))

        ;; Divider
        ($ :div {:class "divider my-6"}
          ($ :div {:class "divider-text"} (t :common/or)))

        ;; OAuth options
        ($ :div {:class "space-y-4"}
          ($ button {:btn-type :outline
                     :class "w-full"
                     :id "registration-google-btn"
                     :on-click #(set! (.-href js/window.location) "/login/google")}
            ($ google-icon)
            (t :login/continue-google))

          ($ button {:btn-type :outline
                     :class "w-full"
                     :id "registration-github-btn"
                     :on-click #(set! (.-href js/window.location) "/login/github")}
            ($ github-icon)
            (t :login/continue-github)))

        ;; Footer
        ($ auth-form-footer {:message (t :register/have-account)
                             :show-security-badge? true})))))
