(ns app.domain.frontend.expenses.events.user-expenses.expense-categories
  "User-facing expense categories list + CRUD (admin/owner only).

  These events call /api/v1/expenses/expense-categories and sync results into the
  shared template entity store so list-view can render them."
  (:require
    [app.domain.frontend.expenses.admin.adapters.sync :as expenses-sync]
    [app.domain.frontend.expenses.events.user-expenses.endpoints :as endpoints]
    [app.domain.frontend.expenses.events.user-expenses.list-support :as list-support]
    [app.domain.frontend.expenses.events.user-expenses.xhrio :as x]
    [app.shared.adapters.normalization :as normalization]
    [app.shared.model-naming :as model-naming]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.shared.crud.success :as crud-success]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; ---------------------------------------------------------------------------
;; Expense Categories
;; ---------------------------------------------------------------------------

(defn- prepare-expense-category-payload
  "Normalize expense-category form values to the API's snake_case payload.

  The dynamic form specs use app/kebab-case keys, while older callers and
  fallback specs may still produce snake_case keys. When both styles are
  present, prefer the app-style checkbox value that reflects the user's latest
  interaction."
  [form-data]
  (let [form-data (or form-data {})
        has-exclude? (or (contains? form-data :exclude-from-reports)
                       (contains? form-data :exclude_from_reports))
        exclude? (cond
                   (contains? form-data :exclude-from-reports)
                   (boolean (:exclude-from-reports form-data))

                   (contains? form-data :exclude_from_reports)
                   (boolean (:exclude_from_reports form-data))

                   :else false)
        has-default? (or (contains? form-data :is-default)
                       (contains? form-data :is_default))
        is-default? (cond
                      (contains? form-data :is-default)
                      (boolean (:is-default form-data))

                      (contains? form-data :is_default)
                      (boolean (:is_default form-data))

                      :else false)
        app-form-data (cond-> (normalization/convert-db-keys->app-keys form-data)
                        has-exclude?
                        (assoc :exclude-from-reports exclude?)

                        has-default?
                        (assoc :is-default is-default?))]
    (model-naming/app-map-keys->db app-form-data)))

(rf/reg-event-fx
  :user-expenses/refresh-expense-categories-list
  common-interceptors
  (fn [{:keys [db]} [opts]]
    {:dispatch [:user-expenses/fetch-expense-categories (merge (list-support/build-list-request-params db :expense-categories 100)
                                                          (when (map? opts) opts))]}))

(rf/reg-event-fx
  :user-expenses/fetch-expense-categories
  common-interceptors
  (fn [{:keys [db]} [params]]
    (let [request-params (merge {:limit 100 :offset 0} (when (map? params) params))]
      {:db (-> db
             (list-support/begin-entity-load :expense-categories)
             (list-support/begin-loading [:user-expenses :expense-categories :loading?]
               [:user-expenses :expense-categories :error]))
       :http-xhrio (x/xhrio db
                     {:method :get
                      :uri endpoints/expense-categories-endpoint
                      :params request-params
                      :on-success [:user-expenses/fetch-expense-categories-success]
                      :on-failure [:user-expenses/fetch-expense-categories-failure]})})))

(rf/reg-event-fx
  :user-expenses/fetch-expense-categories-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [expense-categories (vec (or (:data response) []))
          total (or (:total response) (count expense-categories))
          date-highlights (:date-highlights response)]
      {:db (cond-> (-> db
                     (list-support/finish-entity-load :expense-categories nil)
                     (list-support/finish-loading [:user-expenses :expense-categories :loading?]
                       [:user-expenses :expense-categories :error]
                       nil)
                     (assoc-in (paths/list-total-items :expense-categories) total)
                     (assoc-in [:user-expenses :expense-categories :items] expense-categories))
             (map? date-highlights)
             (assoc-in (conj (paths/list-ui-state :expense-categories) :date-highlights) date-highlights))
       :dispatch [::expenses-sync/sync-expense-categories expense-categories]})))

(rf/reg-event-db
  :user-expenses/fetch-expense-categories-failure
  common-interceptors
  (fn [db [error]]
    (-> db
      (list-support/finish-entity-load :expense-categories error)
      (list-support/finish-loading [:user-expenses :expense-categories :loading?]
        [:user-expenses :expense-categories :error]
        error))))

(rf/reg-event-fx
  :user-expenses/create-expense-category-modal
  common-interceptors
  (fn [{:keys [db]} [form-data on-success]]
    {:db (-> db
           (assoc-in [:user-expenses :form :loading?] true)
           (assoc-in [:user-expenses :form :error] nil))
     :http-xhrio (x/xhrio db
                   {:method :post
                    :uri endpoints/expense-categories-endpoint
                    :params (prepare-expense-category-payload form-data)
                    :on-success [:user-expenses/create-expense-category-modal-success on-success]
                    :on-failure [:user-expenses/create-expense-category-modal-failure]})}))

(rf/reg-event-fx
  :user-expenses/create-expense-category-modal-success
  common-interceptors
  (fn [{:keys [db]} [on-success response]]
    (let [expense-category (:data response)
          expense-category-id (:id expense-category)
          highlight-id (some-> expense-category-id str)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] false)
             (assoc-in [:user-expenses :form :error] nil)
             (cond-> highlight-id
               (crud-success/track-recently-created :expense-categories highlight-id)))
       :dispatch-n [[:user-expenses/refresh-expense-categories-list]]
       :fx [(when on-success
              [:dispatch-later {:ms 100
                                :dispatch [:user-expenses/call-modal-callback on-success expense-category]}])]})))

(rf/reg-event-db
  :user-expenses/create-expense-category-modal-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to create expense category" {:error error})
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))

(rf/reg-event-fx
  :user-expenses/update-expense-category-modal
  common-interceptors
  (fn [{:keys [db]} [expense-category-id form-data on-success]]
    (let [expense-category-id-str (some-> expense-category-id str)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] true)
             (assoc-in [:user-expenses :form :error] nil))
       :http-xhrio (x/xhrio db
                     {:method :put
                      :uri (str endpoints/expense-categories-endpoint "/" expense-category-id-str)
                      :params (prepare-expense-category-payload form-data)
                      :on-success [:user-expenses/update-expense-category-modal-success expense-category-id-str on-success]
                      :on-failure [:user-expenses/update-expense-category-modal-failure]})})))

(rf/reg-event-fx
  :user-expenses/update-expense-category-modal-success
  common-interceptors
  (fn [{:keys [db]} [expense-category-id on-success _response]]
    (let [highlight-id (some-> expense-category-id str)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] false)
             (assoc-in [:user-expenses :form :error] nil)
             (cond-> (seq highlight-id)
               (crud-success/track-recently-updated :expense-categories highlight-id)))
       :dispatch-n [[:user-expenses/refresh-expense-categories-list]]
       :fx [(when on-success
              [:dispatch-later {:ms 100
                                :dispatch [:user-expenses/call-modal-callback on-success]}])]})))

(rf/reg-event-db
  :user-expenses/update-expense-category-modal-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to update expense category" {:error error})
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))

(rf/reg-event-fx
  :user-expenses/delete-expense-category
  common-interceptors
  (fn [{:keys [db]} [expense-category-id]]
    (let [expense-category-id-str (some-> expense-category-id str)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] true)
             (assoc-in [:user-expenses :form :error] nil))
       :http-xhrio (x/xhrio db
                     {:method :delete
                      :uri (str endpoints/expense-categories-endpoint "/batch")
                      :params {:ids [expense-category-id-str]}
                      :on-success [:user-expenses/delete-expense-category-success]
                      :on-failure [:user-expenses/delete-expense-category-failure]})})))

(rf/reg-event-fx
  :user-expenses/delete-expense-category-success
  common-interceptors
  (fn [{:keys [db]} [_response]]
    {:db (assoc-in db [:user-expenses :form :loading?] false)
     :dispatch [:user-expenses/refresh-expense-categories-list]}))

(rf/reg-event-db
  :user-expenses/delete-expense-category-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to delete expense category" {:error error})
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))