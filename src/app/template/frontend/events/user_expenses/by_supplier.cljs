(ns app.template.frontend.events.user-expenses.by-supplier
  (:require
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [app.template.frontend.events.user-expenses.endpoints :as endpoints]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; ---------------------------------------------------------------------------
;; Spending by supplier (future use / leaderboard)
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-by-supplier
  common-interceptors
  (fn [{:keys [db]} [{:keys [limit from to]}]]
    (let [limit* (or limit 5)]
      {:db (-> db
             (assoc-in [:user-expenses :by-supplier :loading?] true)
             (assoc-in [:user-expenses :by-supplier :error] nil))
       :http-xhrio (http/api-request
                     {:method :get
                      :uri endpoints/by-supplier-endpoint
                      :params (cond-> {:limit limit*}
                                from (assoc :from from)
                                to (assoc :to to))
                      :on-success [:user-expenses/fetch-by-supplier-success]
                      :on-failure [:user-expenses/fetch-by-supplier-failure]})})))

(rf/reg-event-db
  :user-expenses/fetch-by-supplier-success
  common-interceptors
  (fn [db [response]]
    (-> db
      (assoc-in [:user-expenses :by-supplier :loading?] false)
      (assoc-in [:user-expenses :by-supplier :error] nil)
      (assoc-in [:user-expenses :by-supplier :data] (:data response)))))

(rf/reg-event-db
  :user-expenses/fetch-by-supplier-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to fetch spending by supplier" {:error error})
    (-> db
      (assoc-in [:user-expenses :by-supplier :loading?] false)
      (assoc-in [:user-expenses :by-supplier :error] (http/extract-error-message error)))))

