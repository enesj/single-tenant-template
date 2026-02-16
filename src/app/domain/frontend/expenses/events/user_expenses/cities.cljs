(ns app.domain.frontend.expenses.events.user-expenses.cities
  "User-facing cities list + CRUD (admin/owner only).

  These events call /api/v1/expenses/cities and sync results into the
  shared template entity store so list-view can render them."
  (:require
    [app.domain.frontend.expenses.admin.adapters.sync :as expenses-sync]
    [app.domain.frontend.expenses.events.user-expenses.endpoints :as endpoints]
    [app.domain.frontend.expenses.events.user-expenses.xhrio :as x]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [app.template.frontend.db.paths :as paths]
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

(defn- parse-pos-int
  [value]
  (cond
    (number? value) (when (pos? value) (long value))
    (string? value) (let [n (js/parseInt value 10)]
                      (when (and (number? n) (not (js/isNaN n)) (pos? n))
                        (long n)))
    :else nil))

(defn- current-cities-page-params
  [db]
  (let [entity-key :cities
        per-page (or (parse-pos-int (get-in db (paths/list-per-page entity-key)))
                   (parse-pos-int (get-in db (conj (paths/list-ui-state entity-key) :per-page)))
                   (parse-pos-int (get-in db (conj (paths/list-ui-state entity-key) :pagination :per-page)))
                   10)
        current-page (or (parse-pos-int (get-in db (paths/list-current-page entity-key)))
                       (parse-pos-int (get-in db (conj (paths/list-ui-state entity-key) :current-page)))
                       (parse-pos-int (get-in db (conj (paths/list-ui-state entity-key) :pagination :current-page)))
                       1)]
    {:limit per-page
     :offset (* (max 0 (dec current-page)) per-page)}))

;; ---------------------------------------------------------------------------
;; Cities
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/refresh-cities-list
  common-interceptors
  (fn [{:keys [db]} _]
    {:dispatch [:user-expenses/fetch-cities (current-cities-page-params db)]}))

(rf/reg-event-fx
  :user-expenses/fetch-cities
  common-interceptors
  (fn [{:keys [db]} [params]]
    (let [request-params (merge {:limit 500 :offset 0} (when (map? params) params))]
      {:db (begin-entity-load db :cities)
       :http-xhrio (x/xhrio db
                     {:method :get
                      :uri endpoints/cities-endpoint
                      :params request-params
                      :on-success [:user-expenses/fetch-cities-success]
                      :on-failure [:user-expenses/fetch-cities-failure]})})))

(rf/reg-event-fx
  :user-expenses/fetch-cities-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [cities (vec (or (:data response) []))
          total (or (:total response) (count cities))]
      {:db (-> (finish-entity-load db :cities nil)
             (assoc-in (paths/list-total-items :cities) total))
       :dispatch [::expenses-sync/sync-cities cities]})))

(rf/reg-event-db
  :user-expenses/fetch-cities-failure
  common-interceptors
  (fn [db [error]]
    (finish-entity-load db :cities error)))

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