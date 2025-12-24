(ns app.domain.frontend.expenses.subs.suppliers
  (:require [re-frame.core :as rf]))

(def ^:private base-path [:admin :expenses :suppliers])

(rf/reg-sub
  :expenses/suppliers
  (fn [db _]
    (get-in db (conj base-path :items))))

#_(rf/reg-sub
    :expenses/suppliers-loading?
    (fn [db _]
      (true? (get-in db (conj base-path :loading?)))))

#_(rf/reg-sub
    :expenses/suppliers-error
    (fn [db _]
      (get-in db (conj base-path :error))))

(rf/reg-sub
  :expenses/supplier
  (fn [db [_ supplier-id]]
    (get-in db (conj base-path :by-id supplier-id))))

(rf/reg-sub
  :expenses/supplier-detail-loading?
  (fn [db _]
    (true? (get-in db (conj base-path :detail-loading?)))))

(rf/reg-sub
  :expenses/suppliers-error
  (fn [db _]
    (get-in db (conj base-path :error))))

(rf/reg-sub
  :expenses/supplier-detail-modal-open?
  (fn [db _]
    (true? (get-in db (conj base-path :detail-modal :open?)))))

(rf/reg-sub
  :expenses/supplier-detail-modal-id
  (fn [db _]
    (get-in db (conj base-path :detail-modal :entity-id))))
