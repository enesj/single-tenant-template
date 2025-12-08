(ns app.domain.expenses.frontend.subs.expenses
  (:require [re-frame.core :as rf]))

(def ^:private base-path [:admin :expenses :entries])

(rf/reg-sub
  :expenses/entries
  (fn [db _]
    (get-in db (conj base-path :items))))

(rf/reg-sub
  :expenses/entries-loading?
  (fn [db _]
    (true? (get-in db (conj base-path :loading?)))))

(rf/reg-sub
  :expenses/entries-error
  (fn [db _]
    (get-in db (conj base-path :error))))

(rf/reg-sub
  :expenses/entry
  (fn [db [_ entry-id]]
    (get-in db (conj base-path :by-id entry-id))))

(rf/reg-sub
  :expenses/entry-detail-loading?
  (fn [db _]
    (true? (get-in db (conj base-path :detail-loading?)))))
