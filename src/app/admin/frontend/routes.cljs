(ns app.admin.frontend.routes
  "Admin routes; update when adding admin pages."
  (:require
    [app.admin.frontend.pages.admins :as admins]
    [app.admin.frontend.pages.domain.expenses.article-aliases :as article-aliases]
    [app.admin.frontend.pages.audit :as audit]
    [app.admin.frontend.pages.domain.expenses.articles :as articles]
    [app.admin.frontend.pages.domain.expenses.categories :as categories]
    [app.admin.frontend.pages.domain.expenses.cities :as cities]
    [app.admin.frontend.pages.dashboard :as dashboard]
    [app.admin.frontend.pages.forgot-password :as forgot-password]
    [app.admin.frontend.pages.login :as login]
    [app.admin.frontend.pages.login-events :as login-events]
    [app.admin.frontend.pages.domain.expenses.manufacturers :as manufacturers]
    [app.admin.frontend.pages.domain.expenses.price-observations :as price-observations]
    [app.admin.frontend.pages.reset-password :as reset-password]
    [app.admin.frontend.pages.domain.expenses.store-aliases :as store-aliases]
    [app.admin.frontend.pages.domain.expenses.stores :as stores]
    [app.admin.frontend.pages.domain.expenses.subcategories :as subcategories]
    [app.admin.frontend.pages.domain.expenses.supplier-aliases :as supplier-aliases]
    [app.admin.frontend.pages.domain.expenses.suppliers :as suppliers]
    [app.admin.frontend.pages.unified-settings.page :as unified-settings-page]
    [app.admin.frontend.pages.domain.expenses.unmapped-aliases :as unmapped-aliases]
    [app.admin.frontend.pages.users :as users]
    [re-frame.core :as rf]))

(defn guarded-start
  "Creates a controller that runs events only after admin auth is valid.
   Accepts either a single event vector, a vector of event vectors, or a function
   that receives `params` and returns a vector of event vectors."
  [events-or-fn]
  {:start (fn [params]
            (let [events (cond
                           (fn? events-or-fn) (or (events-or-fn params) [])
                           (and (sequential? events-or-fn)
                             (sequential? (first events-or-fn))) events-or-fn
                           (sequential? events-or-fn) [events-or-fn]
                           (nil? events-or-fn) []
                           :else [events-or-fn])]
              (rf/dispatch [:admin/check-auth-protected events])))})

(def admin-routes
  "Admin panel routes (single-tenant). Domain routes are NOT included here."
  (let [base-child-routes
        [;; Login
         ["/login"
          {:name :admin-login
           :view login/admin-login-page
           :controllers [{:start (fn [_] (rf/dispatch [:admin/init-login]))}]}]

         ;; Forgot Password (public)
         ["/forgot-password"
          {:name :admin-forgot-password
           :view forgot-password/admin-forgot-password-page
           :controllers [{:start (fn [_] (rf/dispatch [:admin/init-forgot-password]))}]}]

         ;; Reset Password (public, with token)
         ["/reset-password"
          {:name :admin-reset-password
           :view reset-password/admin-reset-password-page
           :controllers [{:start (fn [params]
                                   (when-let [token (get-in params [:query :token])]
                                     (rf/dispatch [:admin/verify-reset-token token])))}]}]

         ;; Dashboard (default)
         [""
          {:name :admin-dashboard
           :view dashboard/admin-dashboard-page
           :controllers [(guarded-start [:admin/load-dashboard])]}]

         ["/dashboard"
          {:name :admin-dashboard-alt
           :view dashboard/admin-dashboard-page
           :controllers [(guarded-start [:admin/load-dashboard])]}]

         ;; Users
         ["/users"
          {:name :admin-users
           :view users/admin-users-page
           :controllers [{:start (fn [params]
                                   ((:start (guarded-start [[:admin/load-users]])) params))}]}]

         ;; Articles (embedded user route; keeps admin shell visible)
         ["/articles"
          {:name :admin-articles
           :view articles/admin-articles-page
           :controllers [(guarded-start nil)]}]

         ["/article-aliases"
          {:name :admin-article-aliases
           :view article-aliases/admin-article-aliases-page
           :controllers [(guarded-start nil)]}]

         ["/suppliers"
          {:name :admin-suppliers
           :view suppliers/admin-suppliers-page
           :controllers [(guarded-start nil)]}]

         ["/stores"
          {:name :admin-stores
           :view stores/admin-stores-page
           :controllers [(guarded-start nil)]}]

         ["/supplier-aliases"
          {:name :admin-supplier-aliases
           :view supplier-aliases/admin-supplier-aliases-page
           :controllers [(guarded-start nil)]}]

         ["/store-aliases"
          {:name :admin-store-aliases
           :view store-aliases/admin-store-aliases-page
           :controllers [(guarded-start nil)]}]

         ["/categories"
          {:name :admin-categories
           :view categories/admin-categories-page
           :controllers [(guarded-start nil)]}]

         ["/cities"
          {:name :admin-cities
           :view cities/admin-cities-page
           :controllers [(guarded-start nil)]}]

         ["/subcategories"
          {:name :admin-subcategories
           :view subcategories/admin-subcategories-page
           :controllers [(guarded-start nil)]}]

         ["/manufacturers"
          {:name :admin-manufacturers
           :view manufacturers/admin-manufacturers-page
           :controllers [(guarded-start nil)]}]

         ["/price-observations"
          {:name :admin-price-observations
           :view price-observations/admin-price-observations-page
           :controllers [(guarded-start nil)]}]

         ["/unmapped-aliases"
          {:name :admin-unmapped-aliases
           :view unmapped-aliases/admin-unmapped-aliases-page
           :controllers [(guarded-start nil)]}]

         ;; Audit Logs
         ["/audit"
          {:name :admin-audit
           :view audit/admin-audit-page
           :controllers [{:start (fn [params]
                                   ((:start (guarded-start [[:admin/load-audit-logs]])) params))}]}]

         ;; Login Events
         ["/login-events"
          {:name :admin-login-events
           :view login-events/admin-login-events-page
           :controllers [{:start (fn [params]
                                   ((:start (guarded-start [[:admin/load-login-events]])) params))}]}]

         ;; Admin Management (owner only)
         ["/admins"
          {:name :admin-admins
           :view admins/admin-admins-page
           :controllers [{:start (fn [params]
                                   ((:start (guarded-start [[:admin/load-admins]])) params))}]}]

         ;; Admin Settings (hardcoded display settings)
         ["/admin-settings"
          {:name :admin-admin-settings
           :view unified-settings-page/admin-settings-page
           :controllers [(guarded-start nil)]}]

         ;; User Settings (domain-owned user UI config)
         ["/user-settings"
          {:name :admin-user-settings
           :view unified-settings-page/user-settings-page
           :controllers [(guarded-start nil)]}]]]

    [["/admin" base-child-routes]]))
