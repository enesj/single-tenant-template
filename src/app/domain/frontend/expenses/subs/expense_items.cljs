(ns app.domain.frontend.expenses.subs.expense-items
  "Subscriptions for expense items — expand feature."
  (:require [re-frame.core :as rf]))

;; Items fetched for a specific expense (expand feature)
(rf/reg-sub
  ::items-for-expense
  (fn [db [_ expense-id]]
    (get-in db [:expense-items :by-expense expense-id :items] [])))

(rf/reg-sub
  ::loading-for-expense?
  (fn [db [_ expense-id]]
    (boolean (get-in db [:expense-items :by-expense expense-id :loading?]))))
