(ns app.domain.frontend.expenses.subs.user-expenses
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
  :user-expenses/by-supplier
  (fn [db _]
    (get-in db [:user-expenses :by-supplier :data])))

(rf/reg-sub
  :user-expenses/by-supplier-loading?
  (fn [db _]
    (get-in db [:user-expenses :by-supplier :loading?])))

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

(rf/reg-sub
  :user-expenses/payers-loading?
  (fn [db _]
    (true? (get-in db [:user-expenses :payers :loading?]))))

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
  :user-expenses/upload-batch
  (fn [db _]
    (get-in db [:user-expenses :upload :batch])))

(rf/reg-sub
  :user-expenses/recent-receipts
  (fn [db _]
    (get-in db [:user-expenses :receipts :items])))

;; Receipts inbox
(rf/reg-sub
  :user-expenses/receipts
  (fn [db _]
    (get-in db [:user-expenses :receipts :items])))

(rf/reg-sub
  :user-expenses/receipts-loading?
  (fn [db _]
    (get-in db [:user-expenses :receipts :loading?])))

(rf/reg-sub
  :user-expenses/receipts-error
  (fn [db _]
    (get-in db [:user-expenses :receipts :error])))

(rf/reg-sub
  :user-expenses/receipt
  (fn [db [_ receipt-id]]
    (get-in db [:user-expenses :receipts :by-id receipt-id])))

(rf/reg-sub
  :user-expenses/receipt-detail-loading?
  (fn [db _]
    (get-in db [:user-expenses :receipts :detail-loading?])))

(rf/reg-sub
  :user-expenses/receipt-action-loading?
  (fn [db _]
    (get-in db [:user-expenses :receipts :action-loading?])))

(rf/reg-sub
  :user-expenses/receipt-detail-modal-open?
  (fn [db _]
    (true? (get-in db [:user-expenses :receipts :detail-modal :open?]))))

(rf/reg-sub
  :user-expenses/receipt-detail-modal-id
  (fn [db _]
    (get-in db [:user-expenses :receipts :detail-modal :entity-id])))

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
