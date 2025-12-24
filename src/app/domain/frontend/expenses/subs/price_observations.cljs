(ns app.domain.frontend.expenses.subs.price-observations
  (:require [re-frame.core :as rf]))

(def ^:private base-path [:admin :expenses :price-observations])

(rf/reg-sub
  :expenses/price-observations
  (fn [db _]
    (get-in db (conj base-path :items))))

(rf/reg-sub
  :expenses/price-observations-error
  (fn [db _]
    (get-in db (conj base-path :error))))

(rf/reg-sub
  :expenses/price-observation
  (fn [db [_ obs-id]]
    (get-in db (conj base-path :by-id obs-id))))

(rf/reg-sub
  :expenses/price-observation-detail-loading?
  (fn [db _]
    (true? (get-in db (conj base-path :detail-loading?)))))

(rf/reg-sub
  :expenses/price-observation-detail-modal-open?
  (fn [db _]
    (true? (get-in db (conj base-path :detail-modal :open?)))))

(rf/reg-sub
  :expenses/price-observation-detail-modal-id
  (fn [db _]
    (get-in db (conj base-path :detail-modal :entity-id))))
