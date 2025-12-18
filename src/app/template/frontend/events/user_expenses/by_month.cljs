(ns app.template.frontend.events.user-expenses.by-month
  (:require
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [app.template.frontend.events.user-expenses.endpoints :as endpoints]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; ---------------------------------------------------------------------------
;; Spending by month (for dashboard chart/stats)
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-by-month
  common-interceptors
  (fn [{:keys [db]} [{:keys [months-back]}]]
    (let [months* (or months-back 6)]
      {:db (-> db
             (assoc-in [:user-expenses :by-month :loading?] true)
             (assoc-in [:user-expenses :by-month :error] nil))
       :http-xhrio (http/api-request
                     {:method :get
                      :uri endpoints/by-month-endpoint
                      :params {:months_back months*}
                      :on-success [:user-expenses/fetch-by-month-success]
                      :on-failure [:user-expenses/fetch-by-month-failure]})})))

(rf/reg-event-db
  :user-expenses/fetch-by-month-success
  common-interceptors
  (fn [db [response]]
    (-> db
      (assoc-in [:user-expenses :by-month :loading?] false)
      (assoc-in [:user-expenses :by-month :error] nil)
      (assoc-in [:user-expenses :by-month :data] (:data response)))))

(rf/reg-event-db
  :user-expenses/fetch-by-month-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to fetch spending by month" {:error error})
    (-> db
      (assoc-in [:user-expenses :by-month :loading?] false)
      (assoc-in [:user-expenses :by-month :error] (http/extract-error-message error)))))

