(ns app.admin.frontend.subs.dashboard
  (:require
    [re-frame.core :as rf]))

(rf/reg-sub
  :admin/dashboard-stats
  (fn [db _]
    (:admin/dashboard-stats db)))

(rf/reg-sub
  :admin/dashboard-loading?
  (fn [db _]
    (:admin/dashboard-loading? db)))

(rf/reg-sub
  :admin/dashboard-error
  (fn [db _]
    (:admin/dashboard-error db)))

(rf/reg-sub
  :admin/recent-activity
  (fn [db _]
    (:admin/recent-activity db)))

;; Advanced Dashboard Subscriptions
(rf/reg-sub
  :admin/computed-tenant-metrics
  (fn [db _]
    (:admin/computed-tenant-metrics db)))

(rf/reg-sub
  :admin/computed-user-metrics
  (fn [db _]
    (:admin/computed-user-metrics db)))

(rf/reg-sub
  :admin/high-risk-alerts
  (fn [db _]
    (:admin/high-risk-alerts db)))

(rf/reg-sub
  :admin/enhanced-tenant-spec
  (fn [db _]
    (:admin/enhanced-tenant-spec db)))

(rf/reg-sub
  :admin/enhanced-user-spec
  (fn [db _]
    (:admin/enhanced-user-spec db)))

(rf/reg-sub
  :admin/current-user-role
  (fn [db _]
    (:admin/current-user-role db)))
