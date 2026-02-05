(ns app.domain.frontend.expenses.events.user-expenses.categories
  "User-facing categories list + CRUD (admin/owner only).

  These events call /api/v1/expenses/categories and sync results into the
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

;; ---------------------------------------------------------------------------
;; Categories
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-categories
  common-interceptors
  (fn [{:keys [db]} [params]]
    (let [request-params (merge {:limit 500 :offset 0} (when (map? params) params))]
      {:db (begin-entity-load db :categories)
       :http-xhrio (x/xhrio db
                     {:method :get
                      :uri endpoints/categories-endpoint
                      :params request-params
                      :on-success [:user-expenses/fetch-categories-success]
                      :on-failure [:user-expenses/fetch-categories-failure]})})))

(rf/reg-event-fx
  :user-expenses/fetch-categories-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [categories (vec (or (:data response) []))]
      {:db (finish-entity-load db :categories nil)
       :dispatch [::expenses-sync/sync-categories categories]})))

(rf/reg-event-db
  :user-expenses/fetch-categories-failure
  common-interceptors
  (fn [db [error]]
    (finish-entity-load db :categories error)))

(rf/reg-event-fx
  :user-expenses/create-category-modal
  common-interceptors
  (fn [{:keys [db]} [form-data on-success]]
    {:db (-> db
           (assoc-in [:user-expenses :form :loading?] true)
           (assoc-in [:user-expenses :form :error] nil))
     :http-xhrio (x/xhrio db
                   {:method :post
                    :uri endpoints/categories-endpoint
                    :params (or form-data {})
                    :on-success [:user-expenses/create-category-modal-success on-success]
                    :on-failure [:user-expenses/create-category-modal-failure]})}))

(rf/reg-event-fx
  :user-expenses/create-category-modal-success
  common-interceptors
  (fn [{:keys [db]} [on-success response]]
    (let [category (:data response)
          category-id (:id category)
          highlight-id (some-> category-id str)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] false)
             (assoc-in [:user-expenses :form :error] nil)
             (cond-> highlight-id
               (crud-success/track-recently-created :categories highlight-id)))
       :dispatch-n [[:user-expenses/fetch-categories]]
       :fx [(when on-success
              [:dispatch-later {:ms 100
                                :dispatch [:user-expenses/call-modal-callback on-success category]}])]})))

(rf/reg-event-db
  :user-expenses/create-category-modal-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to create category" {:error error})
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))

(rf/reg-event-fx
  :user-expenses/update-category-modal
  common-interceptors
  (fn [{:keys [db]} [category-id form-data on-success]]
    (let [category-id-str (some-> category-id str)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] true)
             (assoc-in [:user-expenses :form :error] nil))
       :http-xhrio (x/xhrio db
                     {:method :put
                      :uri (str endpoints/categories-endpoint "/" category-id-str)
                      :params (or form-data {})
                      :on-success [:user-expenses/update-category-modal-success category-id-str on-success]
                      :on-failure [:user-expenses/update-category-modal-failure]})})))

(rf/reg-event-fx
  :user-expenses/update-category-modal-success
  common-interceptors
  (fn [{:keys [db]} [category-id on-success _response]]
    (let [highlight-id (some-> category-id str)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] false)
             (assoc-in [:user-expenses :form :error] nil)
             (cond-> (seq highlight-id)
               (crud-success/track-recently-updated :categories highlight-id)))
       :dispatch-n [[:user-expenses/fetch-categories]]
       :fx [(when on-success
              [:dispatch-later {:ms 100
                                :dispatch [:user-expenses/call-modal-callback on-success]}])]})))

(rf/reg-event-db
  :user-expenses/update-category-modal-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to update category" {:error error})
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))

(rf/reg-event-fx
  :user-expenses/delete-category
  common-interceptors
  (fn [{:keys [db]} [category-id]]
    (let [category-id-str (some-> category-id str)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] true)
             (assoc-in [:user-expenses :form :error] nil))
       :http-xhrio (x/xhrio db
                     {:method :delete
                      :uri (str endpoints/categories-endpoint "/batch")
                      :params {:ids [category-id-str]}
                      :on-success [:user-expenses/delete-category-success]
                      :on-failure [:user-expenses/delete-category-failure]})})))

(rf/reg-event-fx
  :user-expenses/delete-category-success
  common-interceptors
  (fn [{:keys [db]} [_response]]
    {:db (assoc-in db [:user-expenses :form :loading?] false)
     :dispatch [:user-expenses/fetch-categories]}))

(rf/reg-event-db
  :user-expenses/delete-category-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to delete category" {:error error})
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))
