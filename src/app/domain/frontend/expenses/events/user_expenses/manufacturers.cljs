(ns app.domain.frontend.expenses.events.user-expenses.manufacturers
  "User-facing manufacturers list + CRUD (admin/owner only).

  These events call /api/v1/expenses/manufacturers and sync results into the
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

(rf/reg-event-fx
  :user-expenses/refresh-manufacturers-list
  common-interceptors
  (fn [{:keys [db]} [opts]]
    {:dispatch [:user-expenses/fetch-manufacturers (merge (list-support/build-list-request-params db :manufacturers 100)
                                                     (when (map? opts) opts))]}))

;; ---------------------------------------------------------------------------
;; Manufacturers
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-manufacturers
  common-interceptors
  (fn [{:keys [db]} [params]]
    (list-support/entity-list-fetch-fx db
      {:entity-key :manufacturers
       :default-request-params {:limit 100 :offset 0}
       :params params
       :uri endpoints/manufacturers-endpoint
       :on-success [:user-expenses/fetch-manufacturers-success]
       :on-failure [:user-expenses/fetch-manufacturers-failure]})))

(rf/reg-event-fx
  :user-expenses/fetch-manufacturers-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (list-support/entity-list-success-fx db :manufacturers response ::expenses-sync/sync-manufacturers)))

(rf/reg-event-db
  :user-expenses/fetch-manufacturers-failure
  common-interceptors
  (fn [db [error]]
    (list-support/finish-entity-load db :manufacturers error)))

(rf/reg-event-fx
  :user-expenses/create-manufacturer-modal
  common-interceptors
  (fn [{:keys [db]} [form-data on-success]]
    {:db (-> db
           (assoc-in [:user-expenses :form :loading?] true)
           (assoc-in [:user-expenses :form :error] nil))
     :http-xhrio (x/xhrio db
                   {:method :post
                    :uri endpoints/manufacturers-endpoint
                    :params (or form-data {})
                    :on-success [:user-expenses/create-manufacturer-modal-success on-success]
                    :on-failure [:user-expenses/create-manufacturer-modal-failure]})}))

(rf/reg-event-fx
  :user-expenses/create-manufacturer-modal-success
  common-interceptors
  (fn [{:keys [db]} [on-success response]]
    (let [manufacturer (:data response)
          manufacturer-id (:id manufacturer)
          highlight-id (some-> manufacturer-id str)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] false)
             (assoc-in [:user-expenses :form :error] nil)
             (cond-> highlight-id
               (crud-success/track-recently-created :manufacturers highlight-id)))
       :dispatch-n [[:user-expenses/refresh-manufacturers-list]]
       :fx [(when on-success
              [:dispatch-later {:ms 100
                                :dispatch [:user-expenses/call-modal-callback on-success manufacturer]}])]})))

(rf/reg-event-db
  :user-expenses/create-manufacturer-modal-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to create manufacturer" {:error error})
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))

(rf/reg-event-fx
  :user-expenses/update-manufacturer-modal
  common-interceptors
  (fn [{:keys [db]} [manufacturer-id form-data on-success]]
    (let [manufacturer-id-str (some-> manufacturer-id str)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] true)
             (assoc-in [:user-expenses :form :error] nil))
       :http-xhrio (x/xhrio db
                     {:method :put
                      :uri (str endpoints/manufacturers-endpoint "/" manufacturer-id-str)
                      :params (or form-data {})
                      :on-success [:user-expenses/update-manufacturer-modal-success manufacturer-id-str on-success]
                      :on-failure [:user-expenses/update-manufacturer-modal-failure]})})))

(rf/reg-event-fx
  :user-expenses/update-manufacturer-modal-success
  common-interceptors
  (fn [{:keys [db]} [manufacturer-id on-success _response]]
    (let [highlight-id (some-> manufacturer-id str)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] false)
             (assoc-in [:user-expenses :form :error] nil)
             (cond-> (seq highlight-id)
               (crud-success/track-recently-updated :manufacturers highlight-id)))
       :dispatch-n [[:user-expenses/refresh-manufacturers-list]]
       :fx [(when on-success
              [:dispatch-later {:ms 100
                                :dispatch [:user-expenses/call-modal-callback on-success]}])]})))

(rf/reg-event-db
  :user-expenses/update-manufacturer-modal-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to update manufacturer" {:error error})
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))

(rf/reg-event-fx
  :user-expenses/delete-manufacturer
  common-interceptors
  (fn [{:keys [db]} [manufacturer-id]]
    (let [manufacturer-id-str (some-> manufacturer-id str)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] true)
             (assoc-in [:user-expenses :form :error] nil))
       :http-xhrio (x/xhrio db
                     {:method :delete
                      :uri (str endpoints/manufacturers-endpoint "/batch")
                      :params {:ids [manufacturer-id-str]}
                      :on-success [:user-expenses/delete-manufacturer-success]
                      :on-failure [:user-expenses/delete-manufacturer-failure]})})))

(rf/reg-event-fx
  :user-expenses/delete-manufacturer-success
  common-interceptors
  (fn [{:keys [db]} [_response]]
    {:db (assoc-in db [:user-expenses :form :loading?] false)
     :dispatch [:user-expenses/refresh-manufacturers-list]}))

(rf/reg-event-db
  :user-expenses/delete-manufacturer-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to delete manufacturer" {:error error})
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))