(ns app.domain.frontend.expenses.events.user-expenses.workspace-dashboard
  "Events for the redesigned workspace dashboard.
   Single API fetch that populates all dashboard widget data."
  (:require
    [app.domain.frontend.expenses.events.user-expenses.endpoints :as endpoints]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

(rf/reg-event-fx
  :workspace-dashboard/init
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (-> db
           (assoc-in [:workspace-dashboard :loading?] true)
           (assoc-in [:workspace-dashboard :error] nil))
     :http-xhrio (http/api-request
                   {:method :get
                    :uri endpoints/dashboard-endpoint
                    :on-success [:workspace-dashboard/loaded]
                    :on-failure [:workspace-dashboard/load-failed]})}))

(rf/reg-event-db
  :workspace-dashboard/loaded
  common-interceptors
  (fn [db [response]]
    (let [data (:data response)]
      (-> db
        (assoc-in [:workspace-dashboard :loading?] false)
        (assoc-in [:workspace-dashboard :error] nil)
        (assoc-in [:workspace-dashboard :data] data)))))

(rf/reg-event-db
  :workspace-dashboard/load-failed
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to load workspace dashboard" {:error error})
    (-> db
      (assoc-in [:workspace-dashboard :loading?] false)
      (assoc-in [:workspace-dashboard :error]
        (http/extract-error-message error)))))
