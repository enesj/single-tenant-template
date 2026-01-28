(ns app.domain.frontend.expenses.events.user-expenses.recent
  "User expense recent list events."
  (:require
    [app.domain.frontend.expenses.admin.adapters.sync :as expenses-sync]
    [app.domain.frontend.expenses.events.user-expenses.endpoints :as endpoints]
    [app.domain.frontend.expenses.events.user-expenses.xhrio :as x]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; ---------------------------------------------------------------------------
;; Recent expenses (list)
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-recent
  common-interceptors
  (fn [{:keys [db]} [{:keys [limit offset]}]]
    (let [limit* (or limit 5)
          offset* (or offset 0)]
      {:db (-> db
             (assoc-in [:user-expenses :recent :loading?] true)
             (assoc-in [:user-expenses :recent :error] nil)
             (assoc-in [:user-expenses :recent :limit] limit*)
             (assoc-in [:user-expenses :recent :offset] offset*))
       :http-xhrio (x/xhrio db
                     {:method :get
                      :uri endpoints/list-endpoint

                      :params {:limit limit* :offset offset*}
                      :on-success [:user-expenses/fetch-recent-success]
                      :on-failure [:user-expenses/fetch-recent-failure]})})))

(rf/reg-event-fx
  :user-expenses/fetch-recent-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [data (or (:data response) [])
          total (or (:total response) (count data))
          limit (or (:limit response) (get-in db [:user-expenses :recent :limit]))
          offset (or (:offset response) (get-in db [:user-expenses :recent :offset]))]
      {:db (-> db
             (assoc-in [:user-expenses :recent :loading?] false)
             (assoc-in [:user-expenses :recent :error] nil)
             (assoc-in [:user-expenses :recent :items] (vec data))
             (assoc-in [:user-expenses :recent :total] total)
             (assoc-in [:user-expenses :recent :limit] limit)
             (assoc-in [:user-expenses :recent :offset] offset))
       ;; Also sync into the shared template entity store so both admin and
       ;; user table views can depend on the same data source.
       :dispatch [::expenses-sync/sync-expenses data]})))

(rf/reg-event-db
  :user-expenses/fetch-recent-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to fetch recent user expenses" {:error error})
    (-> db
      (assoc-in [:user-expenses :recent :loading?] false)
      (assoc-in [:user-expenses :recent :error] (http/extract-error-message error)))))
