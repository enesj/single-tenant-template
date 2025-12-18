(ns app.template.frontend.routes.data
  "Route data generation for the template application.
   Defines the structure and parameters for various route groups."
  (:require
    [app.template.frontend.routes.controllers :as controllers]
    [app.admin.frontend.routes :as admin-routes]
    ;; Domain registry for user-facing domain routes
    [app.domain.frontend.registry :as domain-registry]))

(defn generate-entity-routes
  "Generate generic entity routes that accept entity name as parameter"
  []
  [["/entities/:entity-name/add"
    {:name :entity-add
     :view :entity-detail
     :parameters {:path {:entity-name string?}}
     :controllers (controllers/make-entity-controller :add)}]

   ["/entities/:entity-name"
    {:name :entity-detail
     :view :entity-detail
     :parameters {:path {:entity-name string?}}
     :controllers (controllers/make-entity-controller :detail)}]

   ["/entities/:entity-name/update/:item-id"
    {:name :entity-update
     :view :entity-detail
     :parameters {:path {:entity-name string? :item-id string?}}
     :controllers (controllers/make-entity-controller :update)}]])

(defn generate-app-routes
  "Generate tenant application routes that redirect to entity functionality.
   These provide more intuitive URLs for property hosting workflows."
  []
  [;; App dashboard - redirect to home for now
   ["/app"
    {:name :app-home
     :view :home
     :controllers (controllers/make-simple-controller :page/init-home)}]

   ["/app/dashboard"
    {:name :app-dashboard
     :view :home
     :controllers (controllers/make-simple-controller :page/init-home)}]

   ;; Property management (redirects to entities/properties)
   ["/app/properties"
    {:name :app-properties
     :view :entity-detail
     :controllers (controllers/make-redirect-controller "properties" :detail)}]

   ["/app/properties/add"
    {:name :app-properties-add
     :view :entity-detail
     :controllers (controllers/make-redirect-controller "properties" :add)}]

   ["/app/properties/update/:property-id"
    {:name :app-properties-update
     :view :entity-detail
     :parameters {:path {:property-id string?}}
     :controllers (controllers/make-redirect-controller "properties" :update)}]

   ;; Financial management (redirects to entities/transactions_v2)
   ["/app/transactions"
    {:name :app-transactions
     :view :entity-detail
     :controllers (controllers/make-redirect-controller "transactions_v2" :detail)}]

   ["/app/transactions/add"
    {:name :app-transactions-add
     :view :entity-detail
     :controllers (controllers/make-redirect-controller "transactions_v2" :add)}]

   ["/app/transactions/update/:transaction-id"
    {:name :app-transactions-update
     :view :entity-detail
     :parameters {:path {:transaction-id string?}}
     :controllers (controllers/make-redirect-controller "transactions_v2" :update)}]

   ;; Financials alias for transactions
   ["/app/financials"
    {:name :app-financials
     :view :entity-detail
     :controllers (controllers/make-redirect-controller "transactions_v2" :detail)}]

   ;; Reports and analytics (redirects to entities/cohost_balances)
   ["/app/reports"
    {:name :app-reports
     :view :entity-detail
     :controllers (controllers/make-redirect-controller "cohost_balances" :detail)}]

   ;; Co-host management (redirects to entities/property_cohosts)
   ["/app/cohosts"
    {:name :app-cohosts
     :view :entity-detail
     :controllers (controllers/make-redirect-controller "property_cohosts" :detail)}]

   ["/app/cohosts/add"
    {:name :app-cohosts-add
     :view :entity-detail
     :controllers (controllers/make-redirect-controller "property_cohosts" :add)}]

   ["/app/cohosts/update/:cohost-id"
    {:name :app-cohosts-update
     :view :entity-detail
     :parameters {:path {:cohost-id string?}}
     :controllers (controllers/make-redirect-controller "property_cohosts" :update)}]

   ;; User management (redirects to entities/users)
   ["/app/users"
    {:name :app-users
     :view :entity-detail
     :controllers (controllers/make-redirect-controller "users" :detail)}]

   ["/app/users/add"
    {:name :app-users-add
     :view :entity-detail
     :controllers (controllers/make-redirect-controller "users" :add)}]

   ["/app/users/update/:user-id"
    {:name :app-users-update
     :view :entity-detail
     :parameters {:path {:user-id string?}}
     :controllers (controllers/make-redirect-controller "users" :update)}]

   ;; Invitations management (redirects to entities/invitations)
   ["/app/invitations"
    {:name :app-invitations
     :view :entity-detail
     :controllers (controllers/make-redirect-controller "invitations" :detail)}]

   ["/app/invitations/add"
    {:name :app-invitations-add
     :view :entity-detail
     :controllers (controllers/make-redirect-controller "invitations" :add)}]])

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

       ["/onboarding"
        {:name :onboarding
         :view :onboarding
         :controllers (controllers/make-simple-controller :page/init-onboarding)}]

       ["/subscription"
        {:name :subscription
         :view :subscription
         :controllers (controllers/make-simple-controller :page/init-subscription)}]

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

       ["/entities"
        {:name :entities
         :view :entities
         :controllers (controllers/make-simple-controller :page/init-entities)}]

       ["/entities/"
        {:name :entities-slash
         :view :entities
         :controllers (controllers/make-simple-controller :page/init-entities)}]]
      (generate-app-routes)
      ;; Domain user routes from registry (decoupled from template)
      (domain-registry/all-user-routes)
      (generate-entity-routes)
      admin-routes/admin-routes)))
