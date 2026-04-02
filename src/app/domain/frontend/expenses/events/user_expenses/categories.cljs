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

(defn- flatten-ui-filters
  "Flatten shared-list UI filter values into flat backend query params.
   Date ranges {:from x :to y} → <field>-from / <field>-to.
   Number ranges {:min x :max y} → <field>-min / <field>-max.
   Select maps {:value v :label _} → scalar v.
   Nil/empty string values are omitted."
  [filters]
  (let [lookup-map-value (fn [m k]
                           (if (contains? m k)
                             (get m k)
                             (get m (name k))))
        normalize-select-item (fn [item]
                                (cond
                                  (map? item) (lookup-map-value item :value)
                                  (keyword? item) (name item)
                                  :else item))]
    (reduce-kv
      (fn [acc k v]
        (cond
          (nil? v) acc
          (and (string? v) (empty? v)) acc

          ;; Date range → <field>-from / <field>-to
          (and (map? v)
            (or (contains? v :from) (contains? v "from")
              (contains? v :to) (contains? v "to")))
          (let [field-name (name k)
                from-val (lookup-map-value v :from)
                to-val (lookup-map-value v :to)
                ->query-value (fn [x]
                                (cond
                                  (instance? js/Date x) (.toISOString x)
                                  (some? x) (str x)
                                  :else nil))]
            (cond-> acc
              (some? from-val) (assoc (keyword (str field-name "-from")) (->query-value from-val))
              (some? to-val) (assoc (keyword (str field-name "-to")) (->query-value to-val))))

          ;; Number range → <field>-min / <field>-max
          (and (map? v)
            (or (contains? v :min) (contains? v "min")
              (contains? v :max) (contains? v "max")))
          (let [field-name (name k)
                min-val (lookup-map-value v :min)
                max-val (lookup-map-value v :max)]
            (cond-> acc
              (some? min-val) (assoc (keyword (str field-name "-min")) min-val)
              (some? max-val) (assoc (keyword (str field-name "-max")) max-val)))

          ;; Multi-select values → scalar for one selection, vector for many
          (vector? v)
          (let [selected-values (->> v
                                  (map normalize-select-item)
                                  (filter some?)
                                  vec)]
            (cond
              (empty? selected-values) acc
              (= 1 (count selected-values)) (assoc acc (keyword (name k)) (first selected-values))
              :else (assoc acc (keyword (name k)) selected-values)))

          ;; Select → extract :value
          (and (map? v) (or (contains? v :value) (contains? v "value")))
          (let [selected-value (lookup-map-value v :value)]
            (if (some? selected-value)
              (assoc acc (keyword (name k)) selected-value)
              acc))

          ;; Scalar (string, number, boolean) → pass through
          :else (assoc acc (keyword (name k)) v)))
      {}
      filters)))

(defn- current-list-page-params
  [db entity-key default-limit]
  (let [per-page (paths/resolved-list-per-page db entity-key default-limit)
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

(rf/reg-event-fx
  :user-expenses/refresh-categories-list
  common-interceptors
  (fn [{:keys [db]} [opts]]
    {:dispatch [:user-expenses/fetch-categories (merge (current-list-page-params db :categories 100)
                                                  (when (map? opts) opts))]}))

;; ---------------------------------------------------------------------------
;; Categories
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-categories
  common-interceptors
  (fn [{:keys [db]} [params]]
    (let [request-params (merge {:limit 100 :offset 0} (when (map? params) params))]
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
    (let [categories (vec (or (:data response) []))
          total (or (:total response) (count categories))
          date-highlights (:date-highlights response)]
      {:db (cond-> (-> (finish-entity-load db :categories nil)
                     (assoc-in (paths/list-total-items :categories) total))
             (map? date-highlights)
             (assoc-in (conj (paths/list-ui-state :categories) :date-highlights) date-highlights))
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
