(ns app.domain.frontend.expenses.subs.expense-items
  (:require
    [re-frame.core :as rf]))

(def ^:private base-path [:admin :expenses :expense-items])

(rf/reg-sub
  :expenses/expense-items
  (fn [db _]
    (get-in db (conj base-path :items))))

(rf/reg-sub
  :expenses/expense-item
  (fn [db [_ item-id]]
    (get-in db (conj base-path :by-id item-id))))

(rf/reg-sub
  :expenses/expense-item-detail-loading?
  (fn [db _]
    (true? (get-in db (conj base-path :detail-loading?)))))

(rf/reg-sub
  :expenses/expense-items-error
  (fn [db _]
    (get-in db (conj base-path :error))))

(rf/reg-sub
  :expenses/expense-item-detail-modal-open?
  (fn [db _]
    (true? (get-in db (conj base-path :detail-modal :open?)))))

(rf/reg-sub
  :expenses/expense-item-detail-modal-id
  (fn [db _]
    (get-in db (conj base-path :detail-modal :entity-id))))
