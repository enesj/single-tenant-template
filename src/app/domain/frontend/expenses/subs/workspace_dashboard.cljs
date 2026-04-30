(ns app.domain.frontend.expenses.subs.workspace-dashboard
  "Subscriptions for workspace dashboard widget data."
  (:require
    [re-frame.core :as rf]))

;; Loading / error state
(rf/reg-sub
  :workspace-dashboard/loading?
  (fn [db _]
    (get-in db [:workspace-dashboard :loading?] false)))

(rf/reg-sub
  :workspace-dashboard/error
  (fn [db _]
    (get-in db [:workspace-dashboard :error])))

;; Raw data (used as signal for derived subs)
(rf/reg-sub
  :workspace-dashboard/data
  (fn [db _]
    (get-in db [:workspace-dashboard :data])))

;; Summary cards
(rf/reg-sub
  :workspace-dashboard/last-30-days
  :<- [:workspace-dashboard/data]
  (fn [data _]
    (:last-30-days data)))

(rf/reg-sub
  :workspace-dashboard/last-6-months
  :<- [:workspace-dashboard/data]
  (fn [data _]
    (:last-6-months data)))

;; Monthly trend
(rf/reg-sub
  :workspace-dashboard/monthly-trend
  :<- [:workspace-dashboard/data]
  (fn [data _]
    (:monthly-trend data)))

;; Top rankings
(rf/reg-sub
  :workspace-dashboard/top-suppliers
  :<- [:workspace-dashboard/data]
  (fn [data _]
    (:top-suppliers data)))

(rf/reg-sub
  :workspace-dashboard/top-stores
  :<- [:workspace-dashboard/data]
  (fn [data _]
    (:top-stores data)))

(rf/reg-sub
  :workspace-dashboard/top-articles
  :<- [:workspace-dashboard/data]
  (fn [data _]
    (:top-articles data)))

;; Insights
(rf/reg-sub
  :workspace-dashboard/price-changes
  :<- [:workspace-dashboard/data]
  (fn [data _]
    (:price-changes data)))

(rf/reg-sub
  :workspace-dashboard/category-breakdown
  :<- [:workspace-dashboard/data]
  (fn [data _]
    (:category-breakdown data)))

(rf/reg-sub
  :workspace-dashboard/biggest-expense
  :<- [:workspace-dashboard/data]
  (fn [data _]
    (:biggest-expense data)))

(rf/reg-sub
  :workspace-dashboard/averages
  :<- [:workspace-dashboard/data]
  (fn [data _]
    (:averages data)))

;; Team & operational
(rf/reg-sub
  :workspace-dashboard/team
  :<- [:workspace-dashboard/data]
  (fn [data _]
    (:team data)))

(rf/reg-sub
  :workspace-dashboard/unmapped-alias-count
  :<- [:workspace-dashboard/data]
  (fn [data _]
    (:unmapped-alias-count data)))
