(ns app.domain.frontend.expenses.events.user-expenses.expense-categories
  "User-facing expense categories list + CRUD (admin/owner only).

  These events call /api/v1/expenses/expense-categories and sync results into the
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

(defn- date->iso-str
  [value]
  (cond
    (instance? js/Date value) (.toISOString value)
    (string? value) value
    :else (str value)))

(defn- normalize-filter-value
  [value]
  (cond
    (and (map? value)
      (or (contains? value :from)
        (contains? value :to)
        (contains? value "from")
        (contains? value "to")
        (contains? value :min)
        (contains? value :max)
        (contains? value "min")
        (contains? value "max")))
    value

    (map? value)
    (or (some-> (get value :value) normalize-filter-value)
      (some-> (get value "value") normalize-filter-value))

    (keyword? value) (name value)
    (string? value) (some-> value clojure.string/trim not-empty)

    (vector? value)
    (let [items (->> value
                  (map normalize-filter-value)
                  (remove nil?)
                  vec)]
      (when (seq items)
        (if (= 1 (count items))
          (first items)
          items)))

    (sequential? value)
    (let [items (->> value
                  (map normalize-filter-value)
                  (remove nil?)
                  vec)]
      (when (seq items)
        (if (= 1 (count items))
          (first items)
          items)))

    :else value))

(defn- flatten-ui-filters
  [filters]
  (reduce-kv
    (fn [acc k v]
      (let [normalized (normalize-filter-value v)
            field-key (keyword (name k))
            field-name (name field-key)
            range-from (when (map? normalized)
                         (or (get normalized :from)
                           (get normalized "from")))
            range-to (when (map? normalized)
                       (or (get normalized :to)
                         (get normalized "to")))
            range-min (when (map? normalized)
                        (or (get normalized :min)
                          (get normalized "min")))
            range-max (when (map? normalized)
                        (or (get normalized :max)
                          (get normalized "max")))]
        (cond
          (nil? normalized)
          acc

          (or (some? range-from) (some? range-to))
          (cond-> acc
            (some? range-from) (assoc (keyword (str field-name "-from")) (date->iso-str range-from))
            (some? range-to) (assoc (keyword (str field-name "-to")) (date->iso-str range-to)))

          (or (some? range-min) (some? range-max))
          (cond-> acc
            (some? range-min) (assoc (keyword (str field-name "-min")) range-min)
            (some? range-max) (assoc (keyword (str field-name "-max")) range-max))

          :else
          (assoc acc field-key normalized))))
    {}
    (or filters {})))

(defn- current-expense-categories-page-params
  [db]
  (let [entity-key :expense-categories
        per-page (paths/resolved-list-per-page db entity-key 100)
        current-page (paths/resolved-list-current-page db entity-key)
        sort-config (or (get-in db [:ui :lists entity-key :sort]) {})
        order-dir (let [direction (:direction sort-config)]
                    (when (contains? #{:asc :desc "asc" "desc"} direction)
                      (name (keyword direction))))
        order-field (when-let [f (:field sort-config)] (name f))
        filters (flatten-ui-filters (or (get-in db [:ui :lists entity-key :filters]) {}))]
    (cond-> (merge {:limit per-page
                    :offset (* (max 0 (dec current-page)) per-page)}
              filters)
      (some? order-dir) (assoc :order-dir order-dir)
      (some? order-field) (assoc :order-by order-field))))

;; ---------------------------------------------------------------------------
;; Expense Categories
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/refresh-expense-categories-list
  common-interceptors
  (fn [{:keys [db]} [opts]]
    {:dispatch [:user-expenses/fetch-expense-categories (merge (current-expense-categories-page-params db)
                                                          (when (map? opts) opts))]}))

(rf/reg-event-fx
  :user-expenses/fetch-expense-categories
  common-interceptors
  (fn [{:keys [db]} [params]]
    (let [request-params (merge {:limit 100 :offset 0} (when (map? params) params))]
      {:db (begin-entity-load db :expense-categories)
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
                     (finish-entity-load :expense-categories nil)
                     (assoc-in (paths/list-total-items :expense-categories) total)
                     (assoc-in [:user-expenses :expense-categories :items] expense-categories)
                     (assoc-in [:user-expenses :expense-categories :loading?] false)
                     (assoc-in [:user-expenses :expense-categories :error] nil))
             (map? date-highlights)
             (assoc-in (conj (paths/list-ui-state :expense-categories) :date-highlights) date-highlights))
       :dispatch [::expenses-sync/sync-expense-categories expense-categories]})))

(rf/reg-event-db
  :user-expenses/fetch-expense-categories-failure
  common-interceptors
  (fn [db [error]]
    (-> db
      (finish-entity-load :expense-categories error)
      (assoc-in [:user-expenses :expense-categories :loading?] false)
      (assoc-in [:user-expenses :expense-categories :error] (http/extract-error-message error)))))

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
                    :params (or form-data {})
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
                      :params (or form-data {})
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
