(ns app.domain.frontend.expenses.events.user-expenses.categories
  "User-facing categories list + CRUD (admin/owner only).

  These events call /api/v1/expenses/categories and sync results into the
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
  :user-expenses/refresh-categories-list
  common-interceptors
  (fn [{:keys [db]} [opts]]
    {:dispatch [:user-expenses/fetch-categories (merge (list-support/build-list-request-params db :categories 100)
                                                  (when (map? opts) opts))]}))

;; ---------------------------------------------------------------------------
;; Categories
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-categories
  common-interceptors
  (fn [{:keys [db]} [params]]
    (list-support/entity-list-fetch-fx db
      {:entity-key :categories
       :default-request-params {:limit 100 :offset 0}
       :params params
       :uri endpoints/categories-endpoint
       :on-success [:user-expenses/fetch-categories-success]
       :on-failure [:user-expenses/fetch-categories-failure]})))

(rf/reg-event-fx
  :user-expenses/fetch-categories-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (list-support/entity-list-success-fx db :categories response ::expenses-sync/sync-categories)))

(rf/reg-event-db
  :user-expenses/fetch-categories-failure
  common-interceptors
  (fn [db [error]]
    (list-support/finish-entity-load db :categories error)))

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
       :dispatch-n [[:user-expenses/refresh-categories-list]]
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
       :dispatch-n [[:user-expenses/refresh-categories-list]]
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
     :dispatch [:user-expenses/refresh-categories-list]}))

(rf/reg-event-db
  :user-expenses/delete-category-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to delete category" {:error error})
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))