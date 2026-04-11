(ns app.admin.frontend.subs.receipts-detail
  "Subscriptions for admin receipt detail modal state."
  (:require
    [re-frame.core :as rf]))

(def ^:private base-path [:admin :receipts :detail])

(rf/reg-sub
  :admin/receipt-detail
  (fn [db [_ receipt-id]]
    (get-in db (conj base-path :by-id (some-> receipt-id str)))))

(rf/reg-sub
  :admin/receipt-detail-loading?
  (fn [db _]
    (boolean (get-in db (conj base-path :loading?) false))))

(rf/reg-sub
  :admin/receipt-action-loading?
  (fn [db _]
    (boolean (get-in db (conj base-path :action-loading?) false))))

(rf/reg-sub
  :admin/receipt-form-error
  (fn [db _]
    (get-in db [:admin :receipts :form :error])))

(rf/reg-sub
  :admin/receipt-form-loading?
  (fn [db _]
    (boolean (get-in db [:admin :receipts :form :loading?] false))))

(rf/reg-sub
  :admin/receipt-detail-error
  (fn [db _]
    (get-in db (conj base-path :error))))