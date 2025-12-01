(ns app.admin.frontend.pages.login
  (:require
    [app.admin.frontend.events.auth]
    [app.admin.frontend.subs.auth]
    [app.template.frontend.components.auth :refer [login-form]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]))

(defui admin-login-form []
  (let [loading? (use-subscribe [:admin/login-loading?])
        error (use-subscribe [:admin/login-error])]

    ($ login-form {:title "Admin Panel"
                   :subtitle "Sign in to access the admin dashboard"
                   :loading? loading?
                   :error error
                   :email-placeholder "admin@company.com"
                   :password-placeholder "Enter your password"
                   :submit-text "Sign In"
                   :on-submit (fn [email password]
                                (rf/dispatch [:admin/login email password]))})))

(defui admin-login-page []
  ($ admin-login-form))
