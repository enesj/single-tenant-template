(ns app.domain.frontend.expenses.subs.unmapped-items
  (:require
    [app.template.frontend.db.paths :as paths]
    [re-frame.core :as rf]))

(def ^:private base-path [:admin :expenses :unmapped-items])

(rf/reg-sub
  :expenses/unmapped-items
  (fn [db _]
    (get-in db (conj base-path :items))))

(rf/reg-sub
  :expenses/unmapped-items-loading?
  (fn [db _]
    (true? (get-in db (conj base-path :loading?)))))

(rf/reg-sub
  :expenses/unmapped-items-error
  (fn [db _]
    (get-in db (conj base-path :error))))

(rf/reg-sub
  :expenses/unmapped-items-lookups-loading?
  (fn [db _]
    (true? (get-in db (conj base-path :lookups :loading?)))))

(rf/reg-sub
  :expenses/unmapped-items-lookups-error
  (fn [db _]
    (get-in db (conj base-path :lookups :error))))

(rf/reg-sub
  :expenses/unmapped-items-lookups-suppliers
  (fn [db _]
    (or (get-in db (conj base-path :lookups :suppliers)) [])))

(rf/reg-sub
  :expenses/unmapped-items-lookups-articles
  (fn [db _]
    (or (get-in db (conj base-path :lookups :articles)) [])))

(rf/reg-sub
  :expenses/unmapped-items-supplier-filter
  (fn [db _]
    (get-in db (conj base-path :filters :supplier-id))))

(rf/reg-sub
  :expenses/unmapped-items-current-page
  (fn [db _]
    (or (get-in db (paths/list-current-page :unmapped-items)) 1)))

(rf/reg-sub
  :expenses/unmapped-items-per-page
  (fn [db _]
    (or (get-in db (paths/list-per-page :unmapped-items)) 50)))

(rf/reg-sub
  :expenses/unmapped-items-total-items
  (fn [db _]
    (or (get-in db (paths/list-total-items :unmapped-items))
      (count (or (get-in db (conj base-path :items)) [])))))

(rf/reg-sub
  :expenses/unmapped-items-selected-ids
  (fn [db _]
    (or (get-in db (conj base-path :selection :item-ids)) #{})))

(rf/reg-sub
  :expenses/unmapped-items-map-modal-open?
  (fn [db _]
    (true? (get-in db (conj base-path :map-modal :open?)))))

(rf/reg-sub
  :expenses/unmapped-items-map-modal-working?
  (fn [db _]
    (true? (get-in db (conj base-path :map-modal :working?)))))

(rf/reg-sub
  :expenses/unmapped-items-map-modal-error
  (fn [db _]
    (get-in db (conj base-path :map-modal :error))))

(rf/reg-sub
  :expenses/unmapped-items-map-modal-progress
  (fn [db _]
    (get-in db (conj base-path :map-modal :progress))))
