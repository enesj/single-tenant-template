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

;; Pagination info for recent list
(rf/reg-sub
  :user-expenses/recent-total
  (fn [db _]
    (get-in db [:user-expenses :recent :total])))

(rf/reg-sub
  :user-expenses/recent-limit
  (fn [db _]
    (get-in db [:user-expenses :recent :limit])))

(rf/reg-sub
  :user-expenses/recent-offset
  (fn [db _]
    (get-in db [:user-expenses :recent :offset])))

;; Current expense detail
(rf/reg-sub
  :user-expenses/current-expense
  (fn [db _]
    (get-in db [:user-expenses :current-expense :data])))

(rf/reg-sub
  :user-expenses/current-expense-loading?
  (fn [db _]
    (get-in db [:user-expenses :current-expense :loading?])))

(rf/reg-sub
  :user-expenses/current-expense-error
  (fn [db _]
    (get-in db [:user-expenses :current-expense :error])))

;; Form state
(rf/reg-sub
  :user-expenses/form-loading?
  (fn [db _]
    (get-in db [:user-expenses :form :loading?])))

(rf/reg-sub
  :user-expenses/form-error
  (fn [db _]
    (get-in db [:user-expenses :form :error])))

;; Suppliers and payers for forms
(rf/reg-sub
  :user-expenses/suppliers
  (fn [db _]
    (get-in db [:user-expenses :suppliers :items])))

(rf/reg-sub
  :user-expenses/payers
  (fn [db _]
    (get-in db [:user-expenses :payers :items])))

;; Upload state
(rf/reg-sub
  :user-expenses/upload-loading?
  (fn [db _]
    (get-in db [:user-expenses :upload :loading?])))

(rf/reg-sub
  :user-expenses/upload-error
  (fn [db _]
    (get-in db [:user-expenses :upload :error])))

(rf/reg-sub
  :user-expenses/recent-receipts
  (fn [db _]
    (get-in db [:user-expenses :receipts :items])))

;; Settings
(rf/reg-sub
  :user-expenses/settings
  (fn [db _]
    (get-in db [:user-expenses :settings :data])))

(rf/reg-sub
  :user-expenses/settings-loading?
  (fn [db _]
    (get-in db [:user-expenses :settings :loading?])))

(rf/reg-sub
  :user-expenses/settings-saving?
  (fn [db _]
    (get-in db [:user-expenses :settings :saving?])))
