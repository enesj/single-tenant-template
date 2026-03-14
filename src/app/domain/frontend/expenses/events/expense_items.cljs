(ns app.domain.frontend.expenses.events.expense-items
  "Expense items domain events — generated factory events plus
   per-expense fetch for the list-page expand feature."
  (:require
    [app.domain.frontend.expenses.events.entity-configs :as configs]
    [app.domain.frontend.expenses.events.events-factory :as factory]
    [app.domain.frontend.expenses.events.user-expenses.endpoints :as endpoints]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

(factory/register-entity-events! configs/expense-items-config)

;; =============================================================================
;; Per-expense item fetch (list-page expand feature)
;; =============================================================================

(rf/reg-event-fx
  ::fetch-items-for-expense
  common-interceptors
  (fn [{:keys [db]} [expense-id]]
    ;; Skip fetch if items are already cached for this expense.
    (if (get-in db [:expense-items :by-expense expense-id :items])
      {}
      {:db (assoc-in db [:expense-items :by-expense expense-id :loading?] true)
       :http-xhrio (http/api-request
                     {:method :get
                      :uri endpoints/expense-items-endpoint
                      :params {:expense_id (str expense-id) :limit 500}
                      :on-success [::fetch-items-for-expense-success expense-id]
                      :on-failure [::fetch-items-for-expense-failure expense-id]})})))

(rf/reg-event-db
  ::fetch-items-for-expense-success
  common-interceptors
  (fn [db [expense-id response]]
    (assoc-in db [:expense-items :by-expense expense-id]
              {:items (vec (or (:data response) []))
               :loading? false})))

(rf/reg-event-db
  ::fetch-items-for-expense-failure
  common-interceptors
  (fn [db [expense-id error]]
    (log/warn "Failed to fetch items for expense" {:expense-id expense-id :error error})
    (assoc-in db [:expense-items :by-expense expense-id :loading?] false)))
