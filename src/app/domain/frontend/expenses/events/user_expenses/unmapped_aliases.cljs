(ns app.domain.frontend.expenses.events.user-expenses.unmapped-aliases
  "User-facing unmapped aliases list events.

  These events call GET /api/v1/expenses/articles/unmapped-aliases and sync the
  results into the shared template entity store so tenant routes can reuse the
  same list-view UI as admin while remaining read-only."
  (:require
    [app.domain.frontend.expenses.admin.adapters.sync :as expenses-sync]
    [app.domain.frontend.expenses.events.user-expenses.endpoints :as endpoints]
    [app.domain.frontend.expenses.events.user-expenses.list-support :as list-support]
    [app.shared.pagination :as pagination]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [re-frame.core :as rf]))

(rf/reg-event-fx
  :user-expenses/refresh-unmapped-aliases-list
  common-interceptors
  (fn [{:keys [db]} [_ opts]]
    {:dispatch [:user-expenses/fetch-unmapped-aliases
                (merge (list-support/build-list-request-params db :unmapped-aliases pagination/default-page-size)
                  (when (map? opts) opts))]}))

(rf/reg-event-fx
  :user-expenses/fetch-unmapped-aliases
  common-interceptors
  (fn [{:keys [db]} [_ params]]
    (list-support/entity-list-fetch-fx db
      {:entity-key :unmapped-aliases
       :default-request-params {:limit 50 :offset 0}
       :params params
       :uri endpoints/articles-unmapped-aliases-endpoint
       :on-success [:user-expenses/fetch-unmapped-aliases-success]
       :on-failure [:user-expenses/fetch-unmapped-aliases-failure]})))

(rf/reg-event-fx
  :user-expenses/fetch-unmapped-aliases-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (list-support/entity-list-success-fx db :unmapped-aliases response ::expenses-sync/sync-unmapped-aliases)))

(rf/reg-event-db
  :user-expenses/fetch-unmapped-aliases-failure
  common-interceptors
  (fn [db [error]]
    (list-support/finish-entity-load db :unmapped-aliases error)))