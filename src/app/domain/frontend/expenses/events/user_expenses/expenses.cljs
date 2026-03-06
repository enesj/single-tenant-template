(ns app.domain.frontend.expenses.events.user-expenses.expenses
  "User-facing expenses list events.

  These events call GET /api/v1/expenses with pagination + server-side sorting.
  Results are synced into the shared template entity store so list-view can
  render in :server pagination mode."
  (:require
    [app.domain.frontend.expenses.admin.adapters.sync :as expenses-sync]
    [app.domain.frontend.expenses.events.user-expenses.endpoints :as endpoints]
    [app.domain.frontend.expenses.events.user-expenses.xhrio :as x]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [app.template.frontend.db.paths :as paths]
    [clojure.string :as str]
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

(defn- parse-pos-int
  [value]
  (cond
    (number? value) (when (pos? value) (long value))
    (string? value) (let [n (js/parseInt value 10)]
                      (when (and (number? n) (not (js/isNaN n)) (pos? n))
                        (long n)))
    :else nil))

(defn- normalize-filter-value
  [value]
  (cond
    (map? value) (or (normalize-filter-value (:value value))
                   (normalize-filter-value (get value "value")))
    (keyword? value) (name value)
    (string? value) (some-> value str/trim not-empty)
    (vector? value) (let [items (->> value
                                  (map normalize-filter-value)
                                  (remove nil?)
                                  vec)]
                      (when (seq items)
                        items))
    (sequential? value) (let [items (->> value
                                      (map normalize-filter-value)
                                      (remove nil?)
                                      vec)]
                          (when (seq items)
                            items))
    :else value))

(defn- normalize-filter-params
  [filters]
  (reduce-kv
    (fn [acc k v]
      (let [normalized (normalize-filter-value v)]
        (if (nil? normalized)
          acc
          (assoc acc k normalized))))
    {}
    (or filters {})))

(defn- current-expenses-page-params
  "Build request params for the current list UI state.

  NOTE: In :server pagination mode, list-view does not apply client filtering.
  We still forward normalized filter params so the backend can (optionally)
  implement filtering for supported keys."
  [db]
  (let [entity-key :expenses
        per-page (paths/resolved-list-per-page db entity-key 25)
        current-page (paths/resolved-list-current-page db entity-key)
        sort-config (or (get-in db (paths/list-sort-config entity-key)) {})
        order-dir (let [direction (:direction sort-config)]
                    (when (contains? #{:asc :desc "asc" "desc"} direction)
                      (name (keyword direction))))
        order-field (when-let [f (:field sort-config)] (name f))
        filters (normalize-filter-params (get-in db (paths/list-filters entity-key)))]
    (cond-> (merge {:limit per-page
                    :offset (* (max 0 (dec current-page)) per-page)}
              filters)
      (some? order-dir) (assoc :order-dir order-dir)
      (some? order-field) (assoc :order-by order-field))))

(rf/reg-event-fx
  :user-expenses/refresh-expenses-list
  common-interceptors
  (fn [{:keys [db]} _]
    {:dispatch [:user-expenses/fetch-expenses (current-expenses-page-params db)]}))

(rf/reg-event-fx
  :user-expenses/fetch-expenses
  common-interceptors
  (fn [{:keys [db]} [params]]
    (let [request-params (merge {:limit 25 :offset 0} (when (map? params) params))]
      {:db (begin-entity-load db :expenses)
       :http-xhrio (x/xhrio db
                     {:method :get
                      :uri endpoints/list-endpoint
                      :params request-params
                      :on-success [:user-expenses/fetch-expenses-success]
                      :on-failure [:user-expenses/fetch-expenses-failure]})})))

(rf/reg-event-fx
  :user-expenses/fetch-expenses-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [expenses (vec (or (:data response) []))
          total (or (:total response) (count expenses))]
      {:db (-> (finish-entity-load db :expenses nil)
             (assoc-in (paths/list-total-items :expenses) total))
       :dispatch [::expenses-sync/sync-expenses expenses]})))

(rf/reg-event-db
  :user-expenses/fetch-expenses-failure
  common-interceptors
  (fn [db [error]]
    (finish-entity-load db :expenses error)))
