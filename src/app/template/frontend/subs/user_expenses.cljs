(ns app.template.frontend.subs.user-expenses
  "Subscriptions for user-facing expense dashboard data."
  (:require
    [re-frame.core :as rf]))

;; Summary
(rf/reg-sub
  :user-expenses/summary
  (fn [db _]
    (get-in db [:user-expenses :summary :data])))

(rf/reg-sub
  :user-expenses/summary-loading?
  (fn [db _]
    (get-in db [:user-expenses :summary :loading?])))

(rf/reg-sub
  :user-expenses/summary-error
  (fn [db _]
    (get-in db [:user-expenses :summary :error])))

;; Recent expenses list
(rf/reg-sub
  :user-expenses/recent
  (fn [db _]
    (get-in db [:user-expenses :recent :items])))

(rf/reg-sub
  :user-expenses/recent-loading?
  (fn [db _]
    (get-in db [:user-expenses :recent :loading?])))

(rf/reg-sub
  :user-expenses/recent-error
  (fn [db _]
    (get-in db [:user-expenses :recent :error])))

;; Aggregations
(rf/reg-sub
  :user-expenses/by-month
  (fn [db _]
    (get-in db [:user-expenses :by-month :data])))

(rf/reg-sub
  :user-expenses/by-month-loading?
  (fn [db _]
    (get-in db [:user-expenses :by-month :loading?])))

(rf/reg-sub
  :user-expenses/by-month-error
  (fn [db _]
    (get-in db [:user-expenses :by-month :error])))

(rf/reg-sub
  :user-expenses/by-supplier
  (fn [db _]
    (get-in db [:user-expenses :by-supplier :data])))

(rf/reg-sub
  :user-expenses/by-supplier-loading?
  (fn [db _]
    (get-in db [:user-expenses :by-supplier :loading?])))

(rf/reg-sub
  :user-expenses/by-supplier-error
  (fn [db _]
    (get-in db [:user-expenses :by-supplier :error])))
