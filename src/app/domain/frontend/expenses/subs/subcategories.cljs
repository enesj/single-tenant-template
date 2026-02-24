(ns app.domain.frontend.expenses.subs.subcategories
  (:require
    [app.domain.frontend.expenses.subs.related-records-factory :as rr-subs]
    [re-frame.core :as rf]))

(def ^:private base-path [:admin :expenses :subcategories])

(rf/reg-sub
  :expenses/subcategories
  (fn [db _]
    (get-in db (conj base-path :items))))

(rf/reg-sub
  :expenses/subcategory
  (fn [db [_ subcategory-id]]
    (get-in db (conj base-path :by-id subcategory-id))))

(rf/reg-sub
  :expenses/subcategory-detail-loading?
  (fn [db _]
    (true? (get-in db (conj base-path :detail-loading?)))))

(rf/reg-sub
  :expenses/subcategories-error
  (fn [db _]
    (get-in db (conj base-path :error))))

(rf/reg-sub
  :expenses/subcategory-detail-modal-open?
  (fn [db _]
    (true? (get-in db (conj base-path :detail-modal :open?)))))

(rf/reg-sub
  :expenses/subcategory-detail-modal-id
  (fn [db _]
    (get-in db (conj base-path :detail-modal :entity-id))))

;; Related records modal subs
(rr-subs/register-related-records-subs!
  {:entity-singular "subcategory"
   :base-path base-path})
