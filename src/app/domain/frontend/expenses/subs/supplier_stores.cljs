(ns app.domain.frontend.expenses.subs.supplier-stores
  "Subscriptions for supplier stores — expand feature."
  (:require [re-frame.core :as rf]))

(rf/reg-sub
  ::stores-for-supplier
  (fn [db [_ supplier-id]]
    (get-in db [:supplier-stores :by-supplier supplier-id :stores] [])))

(rf/reg-sub
  ::loading-for-supplier?
  (fn [db [_ supplier-id]]
    (boolean (get-in db [:supplier-stores :by-supplier supplier-id :loading?]))))
