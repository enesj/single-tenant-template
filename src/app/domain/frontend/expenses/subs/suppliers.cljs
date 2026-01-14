(ns app.domain.frontend.expenses.subs.suppliers
  (:require [re-frame.core :as rf]))

(def ^:private base-path [:admin :expenses :suppliers])
(def ^:private inline-create-path (conj base-path :inline-create))
(def ^:private archive-path (conj base-path :archive))
(def ^:private purge-path (conj base-path :purge))

(rf/reg-sub
  :expenses/suppliers-include-archived?
  (fn [db _]
    (true? (get-in db (conj base-path :include-archived?)))))

(rf/reg-sub
  :expenses/supplier-archive-loading?
  (fn [db _]
    (true? (get-in db (conj archive-path :loading?)))))

(rf/reg-sub
  :expenses/supplier-purge-loading?
  (fn [db _]
    (true? (get-in db (conj purge-path :loading?)))))

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
