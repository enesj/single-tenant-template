(ns app.domain.frontend.expenses.events.user-expenses.stores
  "User-facing stores + store-aliases list + CRUD (admin/owner only).

  These events call /api/v1/expenses/stores and /api/v1/expenses/store-aliases and
  sync results into the shared template entity store so list-view can render them."
  (:require
    [ajax.core :as ajax]
    [app.domain.frontend.expenses.admin.adapters.sync :as expenses-sync]
    [app.domain.frontend.expenses.events.user-expenses.endpoints :as endpoints]
    [app.domain.frontend.expenses.events.user-expenses.xhrio :as x]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.shared.crud.success :as crud-success]
    [re-frame.core :as rf]))

(defn- begin-entity-load
  [db entity-type]
  (-> db
    (assoc-in (paths/entity-loading? entity-type) true)
    (assoc-in (paths/entity-error entity-type) nil)))

(defn- finish-entity-load
  [db entity-type error]
  (-> db
    (assoc-in (paths/entity-loading? entity-type) false)
    (assoc-in (paths/entity-error entity-type) (when error (http/extract-error-message error)))))

(defn- ->id-str
  [id]
  (some-> id str))

(defn- ->id-strs
  [ids]
  (->> ids
    (map ->id-str)
    (remove nil?)
    vec))

;; ---------------------------------------------------------------------------
;; Stores
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-stores
  common-interceptors
  (fn [{:keys [db]} [params]]
    (let [request-params (merge {:limit 200 :offset 0} (when (map? params) params))]
      {:db (begin-entity-load db :stores)
       :http-xhrio (x/xhrio db
                     {:method :get
                      :uri endpoints/stores-endpoint
                      :params request-params
                      :on-success [:user-expenses/fetch-stores-success]
                      :on-failure [:user-expenses/fetch-stores-failure]})})))

(rf/reg-event-fx
  :user-expenses/fetch-stores-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [stores (vec (or (:data response) []))]
      {:db (finish-entity-load db :stores nil)
       :dispatch [::expenses-sync/sync-stores stores]})))

(rf/reg-event-db
  :user-expenses/fetch-stores-failure
  common-interceptors
  (fn [db [error]]
    (finish-entity-load db :stores error)))

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
       :dispatch-n [[:user-expenses/fetch-stores]]
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
          {:keys [display_name display-name address place_id place-id]} (or form-data {})
          payload (cond-> {}
                    (some? display-name) (assoc :display-name display-name)
                    (some? display_name) (assoc :display-name display_name)
                    (some? address) (assoc :address address)
                    (some? place-id) (assoc :place-id place-id)
                    (some? place_id) (assoc :place-id place_id))]
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
       :dispatch-n [[:user-expenses/fetch-stores]]
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
  :user-expenses/delete-stores
  common-interceptors
  (fn [{:keys [db]} [store-ids]]
    (let [ids (->id-strs store-ids)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] true)
             (assoc-in [:user-expenses :form :error] nil))
       :http-xhrio (x/xhrio db
                     {:method :delete
                      :uri (str endpoints/stores-endpoint "/batch")
                      :params {:ids ids}
                      :on-success [:user-expenses/delete-store-success]
                      :on-failure [:user-expenses/delete-store-failure]})})))

(rf/reg-event-fx
  :user-expenses/delete-store-success
  common-interceptors
  (fn [{:keys [db]} [_response]]
    {:db (assoc-in db [:user-expenses :form :loading?] false)
     :dispatch [:user-expenses/fetch-stores]}))

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
    (let [request-params (merge {:limit 200 :offset 0} (when (map? params) params))]
      {:db (begin-entity-load db :store-aliases)
       :http-xhrio (x/xhrio db
                     {:method :get
                      :uri endpoints/store-aliases-endpoint
                      :params request-params
                      :on-success [:user-expenses/fetch-store-aliases-success]
                      :on-failure [:user-expenses/fetch-store-aliases-failure]})})))

(rf/reg-event-fx
  :user-expenses/fetch-store-aliases-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [aliases (vec (or (:data response) []))]
      {:db (finish-entity-load db :store-aliases nil)
       :dispatch [::expenses-sync/sync-store-aliases aliases]})))

(rf/reg-event-db
  :user-expenses/fetch-store-aliases-failure
  common-interceptors
  (fn [db [error]]
    (finish-entity-load db :store-aliases error)))

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
       :dispatch-n [[:user-expenses/fetch-store-aliases]]
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
  :user-expenses/delete-store-aliases
  common-interceptors
  (fn [{:keys [db]} [store-alias-ids]]
    (let [ids (->id-strs store-alias-ids)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] true)
             (assoc-in [:user-expenses :form :error] nil))
       :http-xhrio (x/xhrio db
                     {:method :delete
                      :uri (str endpoints/store-aliases-endpoint "/batch")
                      :params {:ids ids}
                      :on-success [:user-expenses/delete-store-alias-success]
                      :on-failure [:user-expenses/delete-store-alias-failure]})})))

(rf/reg-event-fx
  :user-expenses/delete-store-alias-success
  common-interceptors
  (fn [{:keys [db]} [_response]]
    {:db (assoc-in db [:user-expenses :form :loading?] false)
     :dispatch [:user-expenses/fetch-store-aliases]}))

(rf/reg-event-db
  :user-expenses/delete-store-alias-failure
  common-interceptors
  (fn [db [error]]
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))
