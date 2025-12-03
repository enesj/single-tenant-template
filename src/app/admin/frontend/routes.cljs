(ns app.admin.frontend.routes
  (:require
    [app.admin.frontend.pages.admins :as admins]
    [app.admin.frontend.pages.dashboard :as dashboard]
    [app.admin.frontend.pages.forgot-password :as forgot-password]
    [app.admin.frontend.pages.login :as login]
    [app.admin.frontend.pages.login-events :as login-events]
    [app.admin.frontend.pages.reset-password :as reset-password]
    [app.admin.frontend.pages.settings :as settings]
    [app.admin.frontend.pages.users :as users]
    [app.admin.frontend.pages.audit :as audit]
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
  "Admin panel routes (single-tenant)"
  [["/admin"
    ;; Login
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

    ;; Settings Overview (Hardcoded display settings)
    ["/settings"
     {:name :admin-settings
      :view settings/admin-settings-page
      :controllers [(guarded-start nil)]}]]])
