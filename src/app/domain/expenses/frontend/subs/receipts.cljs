(ns app.domain.expenses.frontend.subs.receipts
  (:require [re-frame.core :as rf]))

(def ^:private base-path [:admin :expenses :receipts])

(rf/reg-sub
  :expenses/receipts
  (fn [db _]
    (get-in db (conj base-path :items))))

(rf/reg-sub
  :expenses/receipts-loading?
  (fn [db _]
    (true? (get-in db (conj base-path :loading?)))))

(rf/reg-sub
  :expenses/receipts-error
  (fn [db _]
    (get-in db (conj base-path :error))))

(rf/reg-sub
  :expenses/receipt
  (fn [db [_ receipt-id]]
    (get-in db (conj base-path :by-id receipt-id))))

(rf/reg-sub
  :expenses/receipt-detail-loading?
  (fn [db _]
    (true? (get-in db (conj base-path :detail-loading?)))))
