(ns app.template.frontend.components.auth
  (:require
    [app.template.frontend.events.auth :as auth-events]
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.icons :refer [default-provider-icon
                                                    github-icon google-icon]]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-state]]
    [uix.re-frame :refer [use-subscribe]]))

(defui auth-component
  []
  (let [auth-status (use-subscribe [:auth-status])
        current-user (use-subscribe [:current-user])
        current-tenant (use-subscribe [:current-tenant])
        is-owner? (use-subscribe [:is-tenant-owner?])]

    (if (and auth-status (:authenticated auth-status))
      ;; User is authenticated
      ($ :div {:class "flex justify-between items-center space-x-2"}
        ;; Provider logo + user info container
        ($ :div {:class "flex items-center space-x-2"}
          ;; Provider Logo
          (let [provider (when (:provider auth-status) (name (:provider auth-status)))]
            (case provider
              "google" ($ google-icon)
              "github" ($ github-icon)
              ;; Default (fallback for other providers)
              ($ default-provider-icon)))

          ;; User info - updated for multi-tenant
          ($ :div {:class "flex flex-col"}
            ;; User name/initials - FIXED to ensure string output
            ($ :span {:class "font-medium text-sm"}
              (let [user current-user]
                (str                                        ; ENSURE STRING OUTPUT
                  (if user
                    ;; Multi-tenant session format
                    (if-let [full-name (:full-name user)]
                      ;; Display initials from full name
                      (let [name-parts (str/split full-name #"\s+")
                            first-initial (first (first name-parts))
                            last-initial (when (> (count name-parts) 1)
                                           (first (last name-parts)))
                            initials (str first-initial (when last-initial last-initial))]
                        initials)
                      ;; Fallback to email prefix
                      (first (str/split (str (:email user)) #"@")))
                    ;; Legacy session format
                    (let [legacy-user (:user auth-status)]
                      (if legacy-user
                        (let [given-name (:given-name legacy-user)
                              family-name (:family-name legacy-user)
                              first-initial (when given-name (first given-name))
                              last-initial (when family-name (first family-name))
                              initials (str first-initial (when last-initial last-initial))]
                          (if (empty? initials)
                            (let [provider (name (:provider auth-status))]
                              (case provider
                                "google" "G"
                                "github" "GH"
                                (first provider)))
                            initials))
                        "U"))))))

            ;; Tenant info for multi-tenant sessions - FIXED to ensure string output
            (when (and current-tenant (not (:legacy-session auth-status)))
              ($ :span {:class "text-xs text-gray-600"}
                (str (:name current-tenant)))))             ; ENSURE STRING OUTPUT

          ;; Role badge for multi-tenant - FIXED to ensure string output
          (when (and current-user (not (:legacy-session auth-status)))
            ($ :span {:class (str "ds-badge ds-badge-sm "
                               (if is-owner? "ds-badge-primary" "ds-badge-secondary"))}
              (str (name (:role current-user))))))          ; ENSURE STRING OUTPUT

        ;; Sign Out Button - FIXED event dispatch
        ($ button {:btn-type :error
                   :class "ds-btn-sm"
                   :id "auth-sign-out-btn"
                   :on-click #(rf/dispatch [::auth-events/logout])}
          "Sign Out"))

      ;; User is not authenticated
      ($ :div {:class "flex items-center space-x-2"}
        ($ button {:btn-type :primary
                   :class "ds-btn-sm"
                   :id "auth-sign-in-btn"
                   :on-click #(set! (.-href js/window.location) "/login/google")}
          "Sign In")))))

(defui auth-form-container
  "Centered auth form container with gradient background.

   Props:
   - :children - Form content to display
   - :max-width - Container max width class (default: 'max-w-md lg:max-w-lg xl:max-w-xl 2xl:max-w-3xl')
   - :bg-gradient - Background gradient classes (default: 'from-base-200 to-base-300')
   - :container-class - Additional classes for outer container"
  [{:keys [children max-width bg-gradient container-class]
    :or {max-width "max-w-md"
         bg-gradient "from-base-200 to-base-300"
         container-class ""}}]
  ($ :div {:class (str "min-h-screen bg-gradient-to-br " bg-gradient
                    " flex items-center justify-center p-4 lg:p-8 2xl:p-16 " container-class)}
    ($ :div {:class (str "w-full " max-width)}
      ;; Main form card
      ($ :div {:class "bg-base-100 shadow-2xl border border-base-300 rounded-2xl overflow-hidden"}
        ($ :div {:class "p-6 lg:p-8 2xl:p-12 text-left"}
          children)))))

(defui auth-form-header
  "Auth form header with icon, title, and subtitle.

   Props:
   - :title - Main title text (default: 'Admin Panel')
   - :subtitle - Subtitle text (default: 'Sign in to access the admin dashboard')
   - :icon-path - SVG path for the icon (default: lock icon)
   - :icon-gradient - Icon background gradient (default: 'from-primary to-secondary')
   - :container-class - Additional classes for the container"
  [{:keys [title subtitle icon-path icon-gradient container-class]
    :or {title "Admin Panel"
         subtitle "Sign in to access the admin dashboard"
         icon-path "M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"
         icon-gradient "from-primary to-secondary"
         container-class ""}}]
  ($ :div {:class (str "text-center mb-6 lg:mb-8 2xl:mb-10 " container-class)}
    ;; Avatar with icon
    ($ :div {:class "ds-avatar mb-4 lg:mb-6 2xl:mb-8"}
      ($ :div {:class (str "w-12 h-12 lg:w-16 lg:h-16 xl:w-20 xl:h-20 2xl:w-24 2xl:h-24 rounded-full bg-gradient-to-br "
                        icon-gradient " shadow-lg flex items-center justify-center")}
        ($ :svg {:class "w-6 h-6 lg:w-8 lg:h-8 xl:w-10 xl:h-10 2xl:w-12 2xl:h-12 text-white" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
          ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d icon-path}))))
    ;; Title and subtitle
    ($ :h1 {:class "text-2xl lg:text-3xl xl:text-4xl 2xl:text-5xl font-bold text-base-content mb-2 lg:mb-3 2xl:mb-4 text-left"} title)
    ($ :p {:class "text-sm lg:text-base xl:text-lg 2xl:text-xl text-base-content/70 text-left"} subtitle)))

(defui auth-error-alert
  "Auth form error alert component.

   Props:
   - :error - Error message to display
   - :icon-path - SVG path for error icon (default: warning triangle)
   - :container-class - Additional classes for the container"
  [{:keys [error icon-path container-class]
    :or {icon-path "M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L3.732 16c-.77.833.192 2.5 1.732 2.5z"
         container-class ""}}]
  (when error
    ($ :div {:class (str "flex items-start gap-3 p-3 mb-4 text-sm "
                      "bg-error/10 text-error border border-error/20 rounded-lg " 
                      container-class)}
      ($ :svg {:class "w-5 h-5 flex-shrink-0 mt-0.5" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
        ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d icon-path}))
      ($ :span error))))

(defui auth-form-field
  "Auth form field with label and input.

   Props:
   - :label - Field label text
   - :type - Input type (default: 'text')
   - :placeholder - Input placeholder
   - :value - Input value
   - :required - Whether field is required (default: false)
   - :on-change - Change handler function
   - :input-class - Additional classes for input
   - :container-class - Additional classes for field container"
  [{:keys [label type placeholder value required on-change input-class container-class field-id]
    :or {type "text"
         required false
         container-class ""}}]
  ($ :div {:class (str "mb-4 " container-class)}
    ($ :label {:class "flex items-center gap-1 mb-1.5 cursor-pointer"
               :for field-id}
      ($ :span {:class "text-sm font-semibold text-base-content/90"} label)
      (when required
        ($ :span {:class "text-error text-sm font-bold"} "*")))
    ($ :input {:type type
              :placeholder placeholder
              :class (str "w-full px-4 py-2.5 "
                       "text-base text-base-content "
                       "bg-white "
                       "border border-base-300 rounded-lg "
                       "focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary "
                       "hover:border-base-400 "
                       "transition-all duration-200 "
                       "placeholder:text-base-content/40 " 
                       input-class)
              :value value
              :required required
              :id field-id
              :on-change on-change})))

(defui auth-submit-button
  "Auth form submit button with loading state.

   Props:
   - :loading? - Whether button is in loading state
   - :disabled? - Whether button is disabled
   - :text - Button text when not loading (default: 'Sign In')
   - :loading-text - Button text when loading (default: 'Signing in...')
   - :icon-path - SVG path for button icon
   - :button-class - Additional classes for button"
  [{:keys [loading? disabled? text loading-text icon-path button-class button-id]
    :or {text "Sign In"
         loading-text "Signing in..."
         icon-path "M11 16l-4-4m0 0l4-4m-4 4h14m-5 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013 3v1"
         button-class ""}}]
  ($ button {:type "submit"
             :btn-type :primary
             :loading loading?
             :disabled (or loading? disabled?)
             :class (str "w-full py-2.5 px-4 "
                      "text-base font-semibold text-white "
                      "bg-primary hover:bg-primary-focus "
                      "rounded-lg "
                      "shadow-sm hover:shadow-md active:shadow-sm "
                      "transition-all duration-200 "
                      "disabled:opacity-50 disabled:cursor-not-allowed "
                      "focus:outline-none focus:ring-2 focus:ring-primary/20 "
                      button-class)
             :id button-id}
    (if loading?
      ($ :span {:class "flex items-center justify-center gap-2"}
        ($ :span {:class "ds-loading ds-loading-spinner ds-loading-sm"})
        loading-text)
      ($ :span {:class "flex items-center justify-center gap-2"}
        (when icon-path
          ($ :svg {:class "w-5 h-5" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
            ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d icon-path})))
        text))))

(defui auth-form-footer
  "Auth form footer with security messaging.

   Props:
   - :message - Footer message (default: 'Secure admin access • Protected by authentication')
   - :show-security-badge? - Whether to show security badge (default: true)
   - :security-badge-text - Security badge text (default: 'Secure Connection')
   - :container-class - Additional classes for the container"
  [{:keys [message show-security-badge? security-badge-text container-class]
    :or {message "Secure admin access • Protected by authentication"
         show-security-badge? true
         security-badge-text "Secure Connection"
         container-class ""}}]
  ($ :div {:class container-class}
    ;; Footer message
    ($ :div {:class "text-center mt-6 lg:mt-8 2xl:mt-12 pt-4 lg:pt-6 2xl:pt-8 border-t border-base-300"}
      ($ :p {:class "text-xs lg:text-sm 2xl:text-lg text-base-content/60"} message))

    ;; Security badge
    (when show-security-badge?
      ($ :div {:class "text-center mt-6 lg:mt-8 2xl:mt-12"}
        ($ :div {:class "ds-badge ds-badge-ghost ds-badge-sm lg:ds-badge-md 2xl:ds-badge-lg"}
          ($ :svg {:class "w-3 h-3 lg:w-4 lg:h-4 2xl:w-6 2xl:h-6 mr-1 lg:mr-2 2xl:mr-3" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
            ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                      :d "M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"}))
          ($ :span {:class "text-xs lg:text-sm 2xl:text-base"} security-badge-text))))))

(defui login-form
  "Complete login form using auth template components.

   Props:
   - :title - Form title (default: 'Admin Panel')
   - :subtitle - Form subtitle
   - :on-submit - Form submission handler (receives email and password)
   - :loading? - Whether form is in loading state
   - :error - Error message to display
   - :email-placeholder - Email field placeholder (default: 'admin@company.com')
   - :password-placeholder - Password field placeholder (default: 'Enter your password')
   - :submit-text - Submit button text (default: 'Sign In')"
  [{:keys [title subtitle on-submit loading? error email-placeholder password-placeholder submit-text]
    :or {email-placeholder "admin@company.com"
         password-placeholder "Enter your password"
         submit-text "Sign In"}}]
  (let [[email set-email!] (use-state "")
        [password set-password!] (use-state "")]

    ($ auth-form-container
      ;; Form header
      ($ auth-form-header {:title title :subtitle subtitle})

      ;; Error alert
      ($ auth-error-alert {:error error})

      ;; Login form
      ($ :form {:id "admin-login-form"
                :on-submit (fn [e]
                             (.preventDefault e)
                             (when on-submit
                               (on-submit email password)))}

        ;; Email field
        ($ auth-form-field {:label "Email Address"
                            :type "email"
                            :placeholder email-placeholder
                            :value email
                            :field-id "admin-login-email"
                            :container-class "mb-4 lg:mb-6 2xl:mb-10"
                            :on-change #(set-email! (.. % -target -value))})

        ;; Password field
        ($ auth-form-field {:label "Password"
                            :type "password"
                            :placeholder password-placeholder
                            :value password
                            :field-id "admin-login-password"
                            :container-class "mb-6 lg:mb-8 2xl:mb-12"
                            :on-change #(set-password! (.. % -target -value))})

        ;; Submit button
        ($ auth-submit-button {:loading? loading?
                               :text submit-text
                               :button-id "admin-login-submit-btn"}))

      ;; Footer
      ($ auth-form-footer))))
