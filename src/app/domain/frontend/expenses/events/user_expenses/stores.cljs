(ns app.domain.frontend.expenses.events.user-expenses.stores
  "User-facing stores + store-aliases list + CRUD (admin/owner only).

  These events call /api/v1/expenses/stores and /api/v1/expenses/store-aliases and
  sync results into the shared template entity store so list-view can render them."
  (:require
    [app.domain.frontend.expenses.admin.adapters.sync :as expenses-sync]
    [app.domain.frontend.expenses.events.user-expenses.endpoints :as endpoints]
    [app.domain.frontend.expenses.events.user-expenses.list-support :as list-support]
    [app.domain.frontend.expenses.events.user-expenses.xhrio :as x]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [app.template.frontend.shared.crud.success :as crud-success]
    [re-frame.core :as rf]))

(defn- ->id-str
  [id]
  (some-> id str))

(rf/reg-event-fx
  :user-expenses/refresh-stores-list
  common-interceptors
  (fn [{:keys [db]} [opts]]
    {:dispatch [:user-expenses/fetch-stores (merge (list-support/build-list-request-params db :stores 200)
                                              (when (map? opts) opts))]}))

(rf/reg-event-fx
  :user-expenses/refresh-store-aliases-list
  common-interceptors
  (fn [{:keys [db]} [opts]]
    {:dispatch [:user-expenses/fetch-store-aliases (merge (list-support/build-list-request-params db :store-aliases 200)
                                                     (when (map? opts) opts))]}))

;; ---------------------------------------------------------------------------
;; Stores
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-stores
  common-interceptors
  (fn [{:keys [db]} [params]]
    (list-support/entity-list-fetch-fx db
      {:entity-key :stores
       :default-request-params {:limit 200 :offset 0}
       :params params
       :uri endpoints/stores-endpoint
       :on-success [:user-expenses/fetch-stores-success]
       :on-failure [:user-expenses/fetch-stores-failure]})))

(rf/reg-event-fx
  :user-expenses/fetch-stores-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (list-support/entity-list-success-fx db :stores response ::expenses-sync/sync-stores)))

(rf/reg-event-db
  :user-expenses/fetch-stores-failure
  common-interceptors
  (fn [db [error]]
    (list-support/finish-entity-load db :stores error)))

(rf/reg-event-fx
  :user-expenses/create-store-modal
  common-interceptors
  (fn [{:keys [db]} [form-data on-success]]
    {:db (-> db
           (assoc-in [:user-expenses :form :loading?] true)
           (assoc-in [:user-expenses :form :error] nil))
     :http-xhrio (x/xhrio db
                   {:method :post
                    :uri endpoints/stores-endpoint
                    :params (or form-data {})
                    :on-success [:user-expenses/create-store-modal-success on-success]
                    :on-failure [:user-expenses/create-store-modal-failure]})}))

(rf/reg-event-fx
  :user-expenses/create-store-modal-success
  common-interceptors
  (fn [{:keys [db]} [on-success response]]
    (let [store (:data response)
          store-id (:id store)
          highlight-id (some-> store-id str)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] false)
             (assoc-in [:user-expenses :form :error] nil)
             (cond-> highlight-id
               (crud-success/track-recently-created :stores highlight-id)))
       :dispatch-n [[:user-expenses/refresh-stores-list]]
       :fx [(when on-success
              [:dispatch-later {:ms 100
                                :dispatch [:user-expenses/call-modal-callback on-success store]}])]})))

(rf/reg-event-db
  :user-expenses/create-store-modal-failure
  common-interceptors
  (fn [db [error]]
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))

(rf/reg-event-fx
  :user-expenses/update-store-modal
  common-interceptors
  (fn [{:keys [db]} [store-id form-data on-success]]
    (let [store-id-str (->id-str store-id)
          {:keys [display_name display-name address]} (or form-data {})
          payload (cond-> {}
                    (some? display-name) (assoc :display-name display-name)
                    (some? display_name) (assoc :display-name display_name)
                    (some? address) (assoc :address address))]

      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] true)
             (assoc-in [:user-expenses :form :error] nil))
       :http-xhrio (x/xhrio db
                     {:method :put
                      :uri (str endpoints/stores-endpoint "/" store-id-str)
                      :params payload
                      :on-success [:user-expenses/update-store-modal-success store-id-str on-success]
                      :on-failure [:user-expenses/update-store-modal-failure]})})))

(rf/reg-event-fx
  :user-expenses/update-store-modal-success
  common-interceptors
  (fn [{:keys [db]} [store-id on-success _response]]
    (let [highlight-id (some-> store-id str)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] false)
             (assoc-in [:user-expenses :form :error] nil)
             (cond-> (seq highlight-id)
               (crud-success/track-recently-updated :stores highlight-id)))
       :dispatch-n [[:user-expenses/refresh-stores-list]]
       :fx [(when on-success
              [:dispatch-later {:ms 100
                                :dispatch [:user-expenses/call-modal-callback on-success]}])]})))

(rf/reg-event-db
  :user-expenses/update-store-modal-failure
  common-interceptors
  (fn [db [error]]
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))

(rf/reg-event-fx
  :user-expenses/delete-store
  common-interceptors
  (fn [{:keys [db]} [store-id]]
    (let [store-id-str (->id-str store-id)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] true)
             (assoc-in [:user-expenses :form :error] nil))
       :http-xhrio (x/xhrio db
                     {:method :delete
                      :uri (str endpoints/stores-endpoint "/batch")
                      :params {:ids [store-id-str]}
                      :on-success [:user-expenses/delete-store-success]
                      :on-failure [:user-expenses/delete-store-failure]})})))

(rf/reg-event-fx
  :user-expenses/delete-store-success
  common-interceptors
  (fn [{:keys [db]} [_response]]
    {:db (assoc-in db [:user-expenses :form :loading?] false)
     :dispatch [:user-expenses/refresh-stores-list]}))

(rf/reg-event-db
  :user-expenses/delete-store-failure
  common-interceptors
  (fn [db [error]]
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))

;; ---------------------------------------------------------------------------
;; Store aliases
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-store-aliases
  common-interceptors
  (fn [{:keys [db]} [params]]
    (list-support/entity-list-fetch-fx db
      {:entity-key :store-aliases
       :default-request-params {:limit 200 :offset 0}
       :params params
       :uri endpoints/store-aliases-endpoint
       :on-success [:user-expenses/fetch-store-aliases-success]
       :on-failure [:user-expenses/fetch-store-aliases-failure]})))

(rf/reg-event-fx
  :user-expenses/fetch-store-aliases-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (list-support/entity-list-success-fx db :store-aliases response ::expenses-sync/sync-store-aliases)))

(rf/reg-event-db
  :user-expenses/fetch-store-aliases-failure
  common-interceptors
  (fn [db [error]]
    (list-support/finish-entity-load db :store-aliases error)))

(rf/reg-event-fx
  :user-expenses/update-store-alias-modal
  common-interceptors
  (fn [{:keys [db]} [store-alias-id form-data on-success]]
    (let [store-alias-id-str (->id-str store-alias-id)
          {:keys [raw_label raw-label
                  raw_label_normalized raw-label-normalized
                  store_id store-id
                  confidence]} (or form-data {})
          payload (cond-> {}
                    (some? raw-label) (assoc :raw-label raw-label)
                    (some? raw_label) (assoc :raw-label raw_label)
                    (some? raw-label-normalized) (assoc :raw-label-normalized raw-label-normalized)
                    (some? raw_label_normalized) (assoc :raw-label-normalized raw_label_normalized)
                    (some? store-id) (assoc :store-id store-id)
                    (some? store_id) (assoc :store-id store_id)
                    (some? confidence) (assoc :confidence confidence))]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] true)
             (assoc-in [:user-expenses :form :error] nil))
       :http-xhrio (x/xhrio db
                     {:method :put
                      :uri (str endpoints/store-aliases-endpoint "/" store-alias-id-str)
                      :params payload
                      :on-success [:user-expenses/update-store-alias-modal-success store-alias-id-str on-success]
                      :on-failure [:user-expenses/update-store-alias-modal-failure]})})))

(rf/reg-event-fx
  :user-expenses/update-store-alias-modal-success
  common-interceptors
  (fn [{:keys [db]} [store-alias-id on-success _response]]
    (let [highlight-id (some-> store-alias-id str)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] false)
             (assoc-in [:user-expenses :form :error] nil)
             (cond-> (seq highlight-id)
               (crud-success/track-recently-updated :store-aliases highlight-id)))
       :dispatch-n [[:user-expenses/refresh-store-aliases-list]]
       :fx [(when on-success
              [:dispatch-later {:ms 100
                                :dispatch [:user-expenses/call-modal-callback on-success]}])]})))

(rf/reg-event-db
  :user-expenses/update-store-alias-modal-failure
  common-interceptors
  (fn [db [error]]
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))

(rf/reg-event-fx
  :user-expenses/delete-store-alias
  common-interceptors
  (fn [{:keys [db]} [store-alias-id]]
    (let [store-alias-id-str (->id-str store-alias-id)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] true)
             (assoc-in [:user-expenses :form :error] nil))
       :http-xhrio (x/xhrio db
                     {:method :delete
                      :uri (str endpoints/store-aliases-endpoint "/batch")
                      :params {:ids [store-alias-id-str]}
                      :on-success [:user-expenses/delete-store-alias-success]
                      :on-failure [:user-expenses/delete-store-alias-failure]})})))

(rf/reg-event-fx
  :user-expenses/delete-store-alias-success
  common-interceptors
  (fn [{:keys [db]} [_response]]
    {:db (assoc-in db [:user-expenses :form :loading?] false)
     :dispatch [:user-expenses/refresh-store-aliases-list]}))

(rf/reg-event-db
  :user-expenses/delete-store-alias-failure
  common-interceptors
  (fn [db [error]]
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))

(rf/reg-event-fx
  :user-expenses/batch-delete-store-aliases
  common-interceptors
  (fn [{:keys [db]} [ids]]
    (let [id-strs (mapv ->id-str ids)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] true)
             (assoc-in [:user-expenses :form :error] nil))
       :http-xhrio (x/xhrio db
                     {:method :delete
                      :uri (str endpoints/store-aliases-endpoint "/batch")
                      :params {:ids id-strs}
                      :on-success [:user-expenses/delete-store-alias-success]
                      :on-failure [:user-expenses/delete-store-alias-failure]})})))

