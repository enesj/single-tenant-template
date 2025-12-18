(ns app.domain.frontend.expenses.events.user-expenses.summary
  "User expense summary events."
  (:require
    [app.domain.frontend.expenses.events.user-expenses.endpoints :as endpoints]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

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
                    :uri endpoints/summary-endpoint
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
