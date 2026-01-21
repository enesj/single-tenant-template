(ns app.domain.frontend.expenses.subs.suppliers
  (:require [re-frame.core :as rf]))

(def ^:private base-path [:admin :expenses :suppliers])
(def ^:private inline-create-path (conj base-path :inline-create))
(def ^:private delete-path (conj base-path :delete))

(rf/reg-sub
  :expenses/supplier-delete-loading?
  (fn [db _]
    (true? (get-in db (conj delete-path :loading?)))))

(rf/reg-sub
  :expenses/suppliers
  (fn [db _]
    (get-in db (conj base-path :items))))

(rf/reg-sub
  :expenses/supplier-inline-create-loading?
  (fn [db _]
    (true? (get-in db (conj inline-create-path :loading?)))))

(rf/reg-sub
  :expenses/supplier-inline-create-error
  (fn [db _]
    (get-in db (conj inline-create-path :error))))

(rf/reg-sub
  :expenses/supplier-inline-create-last-created
  (fn [db _]
    (get-in db (conj inline-create-path :last-created))))

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
