(ns app.domain.frontend.expenses.subs.manufacturers
  (:require
    [app.domain.frontend.expenses.subs.related-records-factory :as rr-subs]
    [re-frame.core :as rf]))

(def ^:private base-path [:admin :expenses :manufacturers])

(rf/reg-sub
  :expenses/manufacturers
  (fn [db _]
    (get-in db (conj base-path :items))))

(rf/reg-sub
  :expenses/manufacturer
  (fn [db [_ manufacturer-id]]
    (get-in db (conj base-path :by-id manufacturer-id))))

(rf/reg-sub
  :expenses/manufacturer-detail-loading?
  (fn [db _]
    (true? (get-in db (conj base-path :detail-loading?)))))

(rf/reg-sub
  :expenses/manufacturers-error
  (fn [db _]
    (get-in db (conj base-path :error))))

(rf/reg-sub
  :expenses/manufacturer-detail-modal-open?
  (fn [db _]
    (true? (get-in db (conj base-path :detail-modal :open?)))))

(rf/reg-sub
  :expenses/manufacturer-detail-modal-id
  (fn [db _]
    (get-in db (conj base-path :detail-modal :entity-id))))

;; Related records modal subs
(rr-subs/register-related-records-subs!
  {:entity-singular "manufacturer"
   :base-path base-path})
