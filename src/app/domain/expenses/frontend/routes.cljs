(ns app.domain.expenses.frontend.routes
  "Expense domain routes - merged into admin router"
  (:require
    [app.domain.expenses.frontend.events.expenses :as expenses-events]
    [app.domain.expenses.frontend.events.payers :as payers-events]
    [app.domain.expenses.frontend.events.receipts :as receipts-events]
    [app.domain.expenses.frontend.events.suppliers :as suppliers-events]
    [app.domain.expenses.frontend.pages.expense-detail :as expense-detail]
    [app.domain.expenses.frontend.pages.expense-list :as expense-list]
    [app.domain.expenses.frontend.pages.payers :as payers]
    [app.domain.expenses.frontend.pages.receipts :as receipts]
    [app.domain.expenses.frontend.pages.suppliers :as suppliers]
    [re-frame.core :as rf]))

(defn- guarded-start
  "Creates a controller start fn that runs events after admin auth is confirmed.

   Accepts:
   - A single event vector
   - A vector of event vectors
   - A function of params returning a vector of event vectors"
  [events-or-fn]
  {:start (fn [params]
            (let [events (cond
                           (fn? events-or-fn) (or (events-or-fn params) [])
                           (and (sequential? events-or-fn)
                                (sequential? (first events-or-fn))) events-or-fn
                           (sequential? events-or-fn) [events-or-fn]
                           (nil? events-or-fn) []
                           :else [events-or-fn])]
              (rf/dispatch [:admin/check-auth-protected events])))} )

(defn routes
  "Routes under /admin for expenses domain."
  []
  [;; Expenses list
   ["/expenses"
    {:name :admin-expenses
     :view expense-list/admin-expense-list-page
     :controllers [(guarded-start [[::expenses-events/load-list {:limit 25}]])]}]
   ["/expenses/:id"
    {:name :admin-expense-detail
     :view expense-detail/admin-expense-detail-page
     :controllers [(guarded-start (fn [params]
                                    (when-let [entry-id (get-in params [:path-params :id])]
                                      [[::expenses-events/load-detail entry-id]])))]}]
   ;; Receipts inbox
   ["/receipts"
    {:name :admin-receipts
     :view receipts/admin-receipts-page
     :controllers [(guarded-start [[::receipts-events/load-list {:limit 25}]])]}]
   ;; Suppliers
   ["/suppliers"
    {:name :admin-suppliers
     :view suppliers/admin-suppliers-page
     :controllers [(guarded-start [[::suppliers-events/load {:limit 50}]])]}]
   ;; Payers
   ["/payers"
    {:name :admin-payers
     :view payers/admin-payers-page
     :controllers [(guarded-start [[::payers-events/load {}]])]}]])
