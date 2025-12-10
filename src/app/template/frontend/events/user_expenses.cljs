(ns app.template.frontend.events.user-expenses
  "User-facing expense dashboard events.
   Fetches expense summary, recent expenses, and aggregates for the user scope."
  (:require
    [app.template.frontend.api :as api]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

(def summary-endpoint (api/versioned-endpoint "/expenses/summary"))
(def list-endpoint (api/versioned-endpoint "/expenses"))
(def by-month-endpoint (api/versioned-endpoint "/expenses/by-month"))
(def by-supplier-endpoint (api/versioned-endpoint "/expenses/by-supplier"))

;; ---------------------------------------------------------------------------
;; Dashboard bootstrap
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/init-dashboard
  common-interceptors
  (fn [{:keys [_db]} _]
    {:dispatch-n [[:user-expenses/fetch-summary]
                  [:user-expenses/fetch-by-month {:months-back 6}]
                  [:user-expenses/fetch-recent {:limit 5}]]}))

;; ---------------------------------------------------------------------------
;; Summary
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-summary
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (-> db
           (assoc-in [:user-expenses :summary :loading?] true)
           (assoc-in [:user-expenses :summary :error] nil))
     :http-xhrio (http/api-request
                   {:method :get
                    :uri summary-endpoint
                    :on-success [:user-expenses/fetch-summary-success]
                    :on-failure [:user-expenses/fetch-summary-failure]})}))

(rf/reg-event-db
  :user-expenses/fetch-summary-success
  common-interceptors
  (fn [db [response]]
    (-> db
      (assoc-in [:user-expenses :summary :loading?] false)
      (assoc-in [:user-expenses :summary :error] nil)
      (assoc-in [:user-expenses :summary :data] (:data response)))))

(rf/reg-event-db
  :user-expenses/fetch-summary-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to fetch user expense summary" {:error error})
    (-> db
      (assoc-in [:user-expenses :summary :loading?] false)
      (assoc-in [:user-expenses :summary :error] (http/extract-error-message error)))))

;; ---------------------------------------------------------------------------
;; Recent expenses (list)
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-recent
  common-interceptors
  (fn [{:keys [db]} [_ {:keys [limit offset]}]]
    (let [limit* (or limit 5)
          offset* (or offset 0)]
      {:db (-> db
             (assoc-in [:user-expenses :recent :loading?] true)
             (assoc-in [:user-expenses :recent :error] nil))
       :http-xhrio (http/api-request
                     {:method :get
                      :uri list-endpoint
                      :params {:limit limit* :offset offset*}
                      :on-success [:user-expenses/fetch-recent-success]
                      :on-failure [:user-expenses/fetch-recent-failure]})})))

(rf/reg-event-db
  :user-expenses/fetch-recent-success
  common-interceptors
  (fn [db [response]]
    (let [{:keys [data total limit offset]} response]
      (-> db
        (assoc-in [:user-expenses :recent :loading?] false)
        (assoc-in [:user-expenses :recent :error] nil)
        (assoc-in [:user-expenses :recent :items] (vec data))
        (assoc-in [:user-expenses :recent :total] total)
        (assoc-in [:user-expenses :recent :limit] limit)
        (assoc-in [:user-expenses :recent :offset] offset)))))

(rf/reg-event-db
  :user-expenses/fetch-recent-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to fetch recent user expenses" {:error error})
    (-> db
      (assoc-in [:user-expenses :recent :loading?] false)
      (assoc-in [:user-expenses :recent :error] (http/extract-error-message error)))))

;; ---------------------------------------------------------------------------
;; Spending by month (for dashboard chart/stats)
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-by-month
  common-interceptors
  (fn [{:keys [db]} [_ {:keys [months-back]}]]
    (let [months* (or months-back 6)]
      {:db (-> db
             (assoc-in [:user-expenses :by-month :loading?] true)
             (assoc-in [:user-expenses :by-month :error] nil))
       :http-xhrio (http/api-request
                     {:method :get
                      :uri by-month-endpoint
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

;; ---------------------------------------------------------------------------
;; Spending by supplier (future use / leaderboard)
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-by-supplier
  common-interceptors
  (fn [{:keys [db]} [_ {:keys [limit from to]}]]
    (let [limit* (or limit 5)]
      {:db (-> db
             (assoc-in [:user-expenses :by-supplier :loading?] true)
             (assoc-in [:user-expenses :by-supplier :error] nil))
       :http-xhrio (http/api-request
                     {:method :get
                      :uri by-supplier-endpoint
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
