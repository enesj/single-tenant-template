(ns app.domain.frontend.expenses.events.user-expenses.cities
  "User-facing cities list + CRUD (admin/owner only).

  These events call /api/v1/expenses/cities and sync results into the
  shared template entity store so list-view can render them."
  (:require
    [app.domain.frontend.expenses.admin.adapters.sync :as expenses-sync]
    [app.domain.frontend.expenses.events.user-expenses.endpoints :as endpoints]
    [app.domain.frontend.expenses.events.user-expenses.list-support :as list-support]
    [app.domain.frontend.expenses.events.user-expenses.xhrio :as x]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [app.template.frontend.shared.crud.success :as crud-success]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; ---------------------------------------------------------------------------
;; Cities
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/refresh-cities-list
  common-interceptors
  (fn [{:keys [db]} [opts]]
    {:dispatch [:user-expenses/fetch-cities (merge (list-support/build-list-request-params db :cities 100)
                                              (when (map? opts) opts))]}))

(rf/reg-event-fx
  :user-expenses/fetch-cities
  common-interceptors
  (fn [{:keys [db]} [params]]
    (list-support/entity-list-fetch-fx db
      {:entity-key :cities
       :default-request-params {:limit 100 :offset 0}
       :params params
       :uri endpoints/cities-endpoint
       :on-success [:user-expenses/fetch-cities-success]
       :on-failure [:user-expenses/fetch-cities-failure]})))

(rf/reg-event-fx
  :user-expenses/fetch-cities-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (list-support/entity-list-success-fx db :cities response ::expenses-sync/sync-cities)))

(rf/reg-event-db
  :user-expenses/fetch-cities-failure
  common-interceptors
  (fn [db [error]]
    (list-support/finish-entity-load db :cities error)))

(rf/reg-event-fx
  :user-expenses/create-city-modal
  common-interceptors
  (fn [{:keys [db]} [form-data on-success]]
    {:db (-> db
           (assoc-in [:user-expenses :form :loading?] true)
           (assoc-in [:user-expenses :form :error] nil))
     :http-xhrio (x/xhrio db
                   {:method :post
                    :uri endpoints/cities-endpoint
                    :params (or form-data {})
                    :on-success [:user-expenses/create-city-modal-success on-success]
                    :on-failure [:user-expenses/create-city-modal-failure]})}))

(rf/reg-event-fx
  :user-expenses/create-city-modal-success
  common-interceptors
  (fn [{:keys [db]} [on-success response]]
    (let [city (:data response)
          city-id (:id city)
          highlight-id (some-> city-id str)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] false)
             (assoc-in [:user-expenses :form :error] nil)
             (cond-> highlight-id
               (crud-success/track-recently-created :cities highlight-id)))
       :dispatch-n [[:user-expenses/refresh-cities-list]]
       :fx [(when on-success
              [:dispatch-later {:ms 100
                                :dispatch [:user-expenses/call-modal-callback on-success city]}])]})))

(rf/reg-event-db
  :user-expenses/create-city-modal-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to create city" {:error error})
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))

(rf/reg-event-fx
  :user-expenses/update-city-modal
  common-interceptors
  (fn [{:keys [db]} [city-id form-data on-success]]
    (let [city-id-str (some-> city-id str)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] true)
             (assoc-in [:user-expenses :form :error] nil))
       :http-xhrio (x/xhrio db
                     {:method :put
                      :uri (str endpoints/cities-endpoint "/" city-id-str)
                      :params (or form-data {})
                      :on-success [:user-expenses/update-city-modal-success city-id-str on-success]
                      :on-failure [:user-expenses/update-city-modal-failure]})})))

(rf/reg-event-fx
  :user-expenses/update-city-modal-success
  common-interceptors
  (fn [{:keys [db]} [city-id on-success _response]]
    (let [highlight-id (some-> city-id str)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] false)
             (assoc-in [:user-expenses :form :error] nil)
             (cond-> (seq highlight-id)
               (crud-success/track-recently-updated :cities highlight-id)))
       :dispatch-n [[:user-expenses/refresh-cities-list]]
       :fx [(when on-success
              [:dispatch-later {:ms 100
                                :dispatch [:user-expenses/call-modal-callback on-success]}])]})))

(rf/reg-event-db
  :user-expenses/update-city-modal-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to update city" {:error error})
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))

(rf/reg-event-fx
  :user-expenses/delete-city
  common-interceptors
  (fn [{:keys [db]} [city-id]]
    (let [city-id-str (some-> city-id str)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] true)
             (assoc-in [:user-expenses :form :error] nil))
       :http-xhrio (x/xhrio db
                     {:method :delete
                      :uri (str endpoints/cities-endpoint "/batch")
                      :params {:ids [city-id-str]}
                      :on-success [:user-expenses/delete-city-success]
                      :on-failure [:user-expenses/delete-city-failure]})})))

(rf/reg-event-fx
  :user-expenses/delete-city-success
  common-interceptors
  (fn [{:keys [db]} [_response]]
    {:db (assoc-in db [:user-expenses :form :loading?] false)
     :dispatch [:user-expenses/refresh-cities-list]}))

(rf/reg-event-db
  :user-expenses/delete-city-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to delete city" {:error error})
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))