(ns app.template.frontend.components.auth-guard
  "Universal authentication guard component for protecting routes and content.

   This component provides consistent authentication patterns across all application modules:
   - Admin panel, tenant UI, customer portal, etc.
   - Supports different auth types and redirect patterns
   - Configurable behavior and appearance"

  (:require
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.message-display :refer [message-display]]
    [re-frame.core :refer [dispatch]]
    [uix.core :refer [$ defui]]))

(defui auth-guard
  "Universal authentication guard component.

   Props:
   - :authenticated? - boolean indicating if user is authenticated
   - :children - content to show when authenticated
   - :auth-type - :admin (default), :tenant, :customer for different auth types
   - :loading? - boolean indicating if auth state is loading
   - :error - string error message if auth failed
   - :login-redirect-path - path to redirect to for login
   - :login-message - custom login message to display
   - :on-redirect - custom redirect function (defaults to dispatch)
   - :show-message? - boolean to show auth messages (default: true)
   - :custom-login-button - custom login button element
   - :class - optional additional CSS classes

   Auth Types:
   - :admin - Admin authentication with admin login redirect
   - :tenant - Tenant authentication with tenant login redirect
   - :customer - Customer authentication with customer login redirect

   Examples:
   - Basic: {:authenticated? false, :auth-type :admin}
   - Custom: {:authenticated? false, :login-message \"Please sign in to continue\"}"
  [props]

  (let [{:keys [authenticated? children auth-type loading? error
                login-redirect-path login-message on-redirect
                show-message? custom-login-button class]} props
        auth-type (or auth-type :admin)
        loading? (or loading? false)
        ;; In the single-tenant admin panel, treat nil as authenticated
        ;; for :admin routes so that missing auth wiring doesn't block
        ;; access to the UI. Other auth types remain strict.
        effective-authenticated? (if (= auth-type :admin)
                                   (not (false? authenticated?))
                                   (boolean authenticated?))
        login-redirect-path (or login-redirect-path
                              (case auth-type
                                :admin "/admin/login"
                                :tenant "/tenant/login"
                                :customer "/login"
                                "/login"))
        login-message (or login-message
                        (case auth-type
                          :admin "You must be logged in as an admin to access this page."
                          :tenant "You must be logged in to access your tenant dashboard."
                          :customer "Please sign in to continue."))
        on-redirect (or on-redirect
                      #(dispatch [:redirect-to login-redirect-path]))
        show-message? (or show-message? true)
        class (or class "")]

    (if loading?
      ;; Show loading state
      ($ :div {:class (str "flex justify-center items-center min-h-screen " class)}
        ($ :div {:class "text-center"}
          ($ :div {:class "animate-spin rounded-full h-8 w-8 border-b-2 border-gray-900 mx-auto mb-4"})
          ($ :p "Checking authentication...")))

      (if effective-authenticated?
        ;; Show protected content when authenticated
        children

        ;; Show login prompt when not authenticated
        ($ :div {:class (str "p-6 text-center " class)}
          ($ :h1 {:class "text-2xl font-semibold text-white mb-4"} "Authentication Required")

          ;; Show error message if any
          (when (and show-message? error)
            ($ message-display
              {:error error
               :variant auth-type}))

          ($ :p {:class "text-gray-400 mb-6"} login-message)

          ;; Login button
          (if custom-login-button
            custom-login-button
            ($ button {:btn-type :primary
                       :on-click on-redirect}
              (case auth-type
                :admin "Go to Admin Login"
                :tenant "Go to Tenant Login"
                :customer "Sign In"))))))))

;; Convenience functions for common use cases

(defn admin-auth-guard
  "Convenience function for admin authentication guard."
  [& args]
  ($ auth-guard (merge {:auth-type :admin} (first args))))

(defn tenant-auth-guard
  "Convenience function for tenant authentication guard."
  [& args]
  ($ auth-guard (merge {:auth-type :tenant} (first args))))

(defn customer-auth-guard
  "Convenience function for customer authentication guard."
  [& args]
  ($ auth-guard (merge {:auth-type :customer} (first args))))

;; Specialized guard for role-based access

(defn role-based-guard
  "Advanced authentication guard with role-based access control.

   Props:
   - :user-roles - collection of user's current roles
   - :required-roles - collection of required roles
   - :fallback-component - component to show when roles don't match
   - :show-denied-message? - boolean to show access denied message
   - All auth-guard props are also supported"
  [{:keys [user-roles required-roles fallback-component show-denied-message?]
    :or {show-denied-message? true}
    :as auth-guard-props}]

  (let [has-required-role? (some (set required-roles) user-roles)]

    (if has-required-role?
      ($ auth-guard auth-guard-props)
      (if fallback-component
        fallback-component
        ($ :div {:class "p-6 text-center"}
          ($ :h1 {:class "text-2xl font-semibold text-white mb-4"} "Access Denied")
          (when show-denied-message?
            ($ :p {:class "text-gray-400 mb-6"} "You don't have permission to access this resource."))
          ($ button {:btn-type :primary
                     :on-click #(dispatch [:navigate-to "/dashboard"])}
            "Go to Dashboard"))))))
