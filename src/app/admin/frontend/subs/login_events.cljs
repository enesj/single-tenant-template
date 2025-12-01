(ns app.admin.frontend.subs.login-events
  "Admin subscriptions for global login events table"
  (:require
    [re-frame.core :as rf]))

(rf/reg-sub
  :admin/login-events-loading?
  (fn [db _]
    (get-in db [:admin :login-events :loading?] false)))

(rf/reg-sub
  :admin/login-events-error
  (fn [db _]
    (get-in db [:admin :login-events :error])))

;; Template compatibility aliases so use-entity-state can use :login-events

(rf/reg-sub
  :admin/login-events-logs-loading?
  :<- [:admin/login-events-loading?]
  (fn [loading? _]
    loading?))

(rf/reg-sub
  :admin/login-events-logs-error
  :<- [:admin/login-events-error]
  (fn [error _]
    error))
