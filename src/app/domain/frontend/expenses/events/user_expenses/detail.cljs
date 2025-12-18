(ns app.domain.frontend.expenses.events.user-expenses.detail
  "User expense detail events."
  (:require
    [app.domain.frontend.expenses.admin.adapters.sync :as expenses-sync]
    [app.domain.frontend.expenses.events.user-expenses.endpoints :as endpoints]
    [app.domain.frontend.expenses.events.user-expenses.xhrio :as x]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; ---------------------------------------------------------------------------
;; Single expense detail
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-expense
  common-interceptors
  (fn [{:keys [db]} [expense-id]]
    {:db (-> db
           (assoc-in [:user-expenses :current-expense :loading?] true)
           (assoc-in [:user-expenses :current-expense :error] nil))
     :http-xhrio (x/xhrio db
                   {:method :get
                    :uri (str endpoints/expense-detail-endpoint "/" expense-id)
                    :admin-uri (str endpoints/admin-expenses-endpoint "/" expense-id)
                    :on-success [:user-expenses/fetch-expense-success]
                    :on-failure [:user-expenses/fetch-expense-failure]})}))

(rf/reg-event-fx
  :user-expenses/fetch-expense-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [expense (or (:data response) (:expense response))
          updated-db (-> db
                       (assoc-in [:user-expenses :current-expense :loading?] false)
                       (assoc-in [:user-expenses :current-expense :error] nil)
                       (assoc-in [:user-expenses :current-expense :data] expense))]
      (cond-> {:db updated-db}
        expense (assoc :dispatch [::expenses-sync/upsert-expenses [expense]])))))

(rf/reg-event-db
  :user-expenses/fetch-expense-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to fetch expense detail" {:error error})
    (-> db
      (assoc-in [:user-expenses :current-expense :loading?] false)
      (assoc-in [:user-expenses :current-expense :error] (http/extract-error-message error)))))
