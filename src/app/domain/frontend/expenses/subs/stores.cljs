(ns app.domain.frontend.expenses.subs.stores
  (:require
    [app.domain.frontend.expenses.subs.related-records-factory :as rr-subs]
    [re-frame.core :as rf]))

(def ^:private base-path [:admin :expenses :stores])

(rf/reg-sub
  :expenses/stores
  (fn [db _]
    (get-in db (conj base-path :items))))

(rf/reg-sub
  :expenses/store
  (fn [db [_ store-id]]
    (get-in db (conj base-path :by-id store-id))))

(rf/reg-sub
  :expenses/store-detail-loading?
  (fn [db _]
    (true? (get-in db (conj base-path :detail-loading?)))))

(rf/reg-sub
  :expenses/stores-error
  (fn [db _]
    (get-in db (conj base-path :error))))

(rf/reg-sub
  :expenses/store-detail-modal-open?
  (fn [db _]
    (true? (get-in db (conj base-path :detail-modal :open?)))))

(rf/reg-sub
  :expenses/store-detail-modal-id
  (fn [db _]
    (get-in db (conj base-path :detail-modal :entity-id))))

;; Related records modal subs
(rr-subs/register-related-records-subs!
  {:entity-singular "store"
   :base-path base-path})
