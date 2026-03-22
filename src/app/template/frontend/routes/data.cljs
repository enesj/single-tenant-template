(ns app.template.frontend.routes.data
  "Route data composition (template + domain + admin); keep registry integration here."
  (:require
    [app.template.frontend.routes.controllers :as controllers]
    [app.admin.frontend.routes :as admin-routes]
    ;; Domain registry for user-facing domain routes
    [app.domain.frontend.registry :as domain-registry]
    [re-frame.core :as rf]))

(def app-routes
  "Unified vector of all application routes"
  (into []
    (concat
      [[""
        {:name :home
         :view :home
         :controllers (controllers/make-simple-controller :page/init-home)}]

       ["/home"
        {:name :home-explicit
         :view :home
         :controllers (controllers/make-simple-controller :page/init-home)}]

       ["/login"
        {:name :login
         :view :login
         :controllers (controllers/make-simple-controller :page/init-login)}]

       ["/register"
        {:name :register
         :view :register
         :controllers (controllers/make-simple-controller :page/init-register)}]

       ["/verify-email"
        {:name :verify-email
         :view :verify-email
         :controllers (controllers/make-simple-controller :page/init-verify-email)}]

       ["/logout"
        {:name :logout
         :view :logout
         :controllers (controllers/make-simple-controller :page/init-logout)}]

       ["/forgot-password"
        {:name :forgot-password
         :view :forgot-password
         :controllers (controllers/make-simple-controller :page/init-forgot-password)}]

       ["/reset-password"
        {:name :reset-password
         :view :reset-password
         :controllers (controllers/make-simple-controller :page/init-reset-password)}]

       ["/change-password"
        {:name :change-password
         :view :change-password
         :controllers (controllers/make-simple-controller :page/init-change-password)}]

       ["/about"
        {:name :about
         :view :about
         :controllers (controllers/make-simple-controller :page/init-about)}]

       ["/about/"
        {:name :about-slash
         :view :about
         :controllers (controllers/make-simple-controller :page/init-about)}]

       ["/verify-email-success"
        {:name :verify-email-success
         :view :verify-email-success
         :controllers (controllers/make-simple-controller :page/init-verify-email-success)}]

       ["/email-verified"
        {:name :email-verified
         :view :email-verified}]

       ["/tenant-select"
        {:name :tenant-select
         :view :tenant-select
         :controllers (controllers/make-simple-controller :page/init-tenant-select)}]

       ["/tenant/members"
        {:name :tenant-members
         :view :tenant-members
         :controllers (controllers/user-guarded-start :page/init-tenant-members)}]

       ["/invitation/accept"
        {:name :invitation-accept
         :view :invitation-accept
         :controllers [{:start (fn [match]
                                 (let [token (get-in match [:query-params :token])]
                                   (when token
                                     (rf/dispatch [:app.template.frontend.events.tenant/accept-invitation-init token]))))}]}]

       ["/tenant/impersonation"
        {:name :tenant-impersonation
         :view :tenant-impersonation
         :controllers (controllers/user-guarded-start :page/init-tenant-impersonation)}]

       ["/onboarding"
        {:name :onboarding
         :view :onboarding
         :controllers (controllers/user-guarded-start :page/init-onboarding)}]]
      ;; Domain user routes from registry (decoupled from template)
      (domain-registry/all-user-routes)
      admin-routes/admin-routes)))
