(ns app.domain.frontend.expenses.events.user-expenses.expenses
  "User-facing expenses list events.

  These events call GET /api/v1/expenses with pagination + server-side sorting.
  Results are synced into the shared template entity store so list-view can
  render in :server pagination mode."
  (:require
    [app.domain.frontend.expenses.admin.adapters.sync :as expenses-sync]
    [app.domain.frontend.expenses.events.user-expenses.endpoints :as endpoints]
    [app.domain.frontend.expenses.events.user-expenses.list-support :as list-support]
    [app.domain.frontend.expenses.events.source-filter :as source-filter]
    [app.shared.pagination :as pagination]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [re-frame.core :as rf]))

(rf/reg-event-fx
  :user-expenses/refresh-expenses-list
  common-interceptors
  (fn [{:keys [db]} [opts]]
    (let [source-param (source-filter/source-filter->param (source-filter/current-state db))
          base-params (list-support/build-list-request-params db :expenses pagination/default-page-size)
          caller-opts (when (map? opts) opts)]
      {:dispatch [:user-expenses/fetch-expenses
                  (cond-> (merge base-params caller-opts)
                    source-param (assoc :source source-param))]})))

(rf/reg-event-fx
  :user-expenses/fetch-expenses
  common-interceptors
  (fn [{:keys [db]} [params]]
    (list-support/entity-list-fetch-fx db
      {:entity-key :expenses
       :default-request-params {:limit 25 :offset 0}
       :params params
       :uri endpoints/list-endpoint
       :on-success [:user-expenses/fetch-expenses-success]
       :on-failure [:user-expenses/fetch-expenses-failure]})))

(rf/reg-event-fx
  :user-expenses/fetch-expenses-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (list-support/entity-list-success-fx db :expenses response ::expenses-sync/sync-expenses)))

(rf/reg-event-db
  :user-expenses/fetch-expenses-failure
  common-interceptors
  (fn [db [error]]
    (list-support/finish-entity-load db :expenses error)))