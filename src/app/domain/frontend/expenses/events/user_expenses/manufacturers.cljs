(ns app.domain.frontend.expenses.events.user-expenses.manufacturers
  "User-facing manufacturers list + CRUD (admin/owner only).

  These events call /api/v1/expenses/manufacturers and sync results into the
  shared template entity store so list-view can render them."
  (:require

    [app.domain.frontend.expenses.admin.adapters.sync :as expenses-sync]
    [app.domain.frontend.expenses.events.user-expenses.endpoints :as endpoints]
    [app.domain.frontend.expenses.events.user-expenses.xhrio :as x]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.events.list.filter-serialization :as filter-serialization]
    [app.template.frontend.shared.crud.success :as crud-success]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

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

(defn- current-list-page-params
  [db entity-key default-limit]
  (let [per-page (paths/resolved-list-per-page db entity-key default-limit)
        current-page (paths/resolved-list-current-page db entity-key)
        sort-config (or (get-in db [:ui :lists entity-key :sort]) {})
        order-dir (let [direction (:direction sort-config)]
                    (when (contains? #{:asc :desc "asc" "desc"} direction)
                      (name (keyword direction))))
        order-field (when-let [f (:field sort-config)] (name f))
        filters (filter-serialization/flatten-ui-filters (or (get-in db [:ui :lists entity-key :filters]) {}))]
    (cond-> (merge {:limit per-page
                    :offset (* (max 0 (dec current-page)) per-page)}
              filters)
      (some? order-dir) (assoc :order-dir order-dir)
      (some? order-field) (assoc :order-by order-field))))

(rf/reg-event-fx
  :user-expenses/refresh-manufacturers-list
  common-interceptors
  (fn [{:keys [db]} [opts]]
    {:dispatch [:user-expenses/fetch-manufacturers (merge (current-list-page-params db :manufacturers 100)
                                                     (when (map? opts) opts))]}))

;; ---------------------------------------------------------------------------
;; Manufacturers
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-manufacturers
  common-interceptors
  (fn [{:keys [db]} [params]]
    (let [request-params (merge {:limit 100 :offset 0} (when (map? params) params))]
      {:db (begin-entity-load db :manufacturers)
       :http-xhrio (x/xhrio db
                     {:method :get
                      :uri endpoints/manufacturers-endpoint
                      :params request-params
                      :on-success [:user-expenses/fetch-manufacturers-success]
                      :on-failure [:user-expenses/fetch-manufacturers-failure]})})))

(rf/reg-event-fx
  :user-expenses/fetch-manufacturers-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [manufacturers (vec (or (:data response) []))
          total (or (:total response) (count manufacturers))
          date-highlights (:date-highlights response)]
      {:db (cond-> (-> (finish-entity-load db :manufacturers nil)
                     (assoc-in (paths/list-total-items :manufacturers) total))
             (map? date-highlights)
             (assoc-in (conj (paths/list-ui-state :manufacturers) :date-highlights) date-highlights))
       :dispatch [::expenses-sync/sync-manufacturers manufacturers]})))

(rf/reg-event-db
  :user-expenses/fetch-manufacturers-failure
  common-interceptors
  (fn [db [error]]
    (finish-entity-load db :manufacturers error)))

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
