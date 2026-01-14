(ns app.domain.frontend.expenses.subs.payers
  (:require [re-frame.core :as rf]))

(def ^:private base-path [:admin :expenses :payers])

(rf/reg-sub
  :expenses/payers
  (fn [db _]
    (get-in db (conj base-path :items))))

;; Used by receipt approval flows to delay form mount until defaults can be applied.
(rf/reg-sub
  :expenses/payers-loading?
  (fn [db _]
    (true? (get-in db (conj base-path :loading?)))))

(rf/reg-sub
  :expenses/payer
  (fn [db [_ payer-id]]
    (get-in db (conj base-path :by-id payer-id))))

(rf/reg-sub
  :expenses/payer-detail-loading?
  (fn [db _]
    (true? (get-in db (conj base-path :detail-loading?)))))

(rf/reg-sub
  :expenses/payers-error
  (fn [db _]
    (get-in db (conj base-path :error))))

(rf/reg-sub
  :expenses/payer-detail-modal-open?
  (fn [db _]
    (true? (get-in db (conj base-path :detail-modal :open?)))))

(rf/reg-sub
  :expenses/payer-detail-modal-id
  (fn [db _]
    (get-in db (conj base-path :detail-modal :entity-id))))
